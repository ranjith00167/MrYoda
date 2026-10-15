package com.mryoda.diagnostics.api.report;

import com.mryoda.diagnostics.api.utils.LoggerUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.UnaryOperator;

/**
 * Polls the report API until every expected test has verified evidence in its PDF, or the
 * configured attempts / timeout run out.
 *
 * <p>Responsibilities are split: HTTP is behind {@link ReportApiClient}/{@link PdfDownloader},
 * text extraction in {@link PdfTextExtractor}, verification in {@link PdfResultMatcher}. This
 * class only orchestrates, so it is fully unit-testable with fakes and a fake clock.
 */
public class ReportPoller {

    /** Pause between attempts; replaceable so tests do not really sleep. */
    public interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    /** Cached extraction for one PDF object key. */
    private static final class CachedPdf {
        final boolean header;
        final PdfExtraction extraction;
        int attemptsUnchanged;

        CachedPdf(boolean header, PdfExtraction extraction) {
            this.header = header;
            this.extraction = extraction;
        }
    }

    private final ReportApiClient apiClient;
    private final PdfDownloader downloader;
    private final PdfTextExtractor extractor;
    private final PdfResultMatcher matcher;
    private final PollConfig config;
    private final Sleeper sleeper;
    private final LongSupplier clock;
    private final Consumer<String> log;

    public ReportPoller(ReportApiClient apiClient, PdfDownloader downloader, PollConfig config) {
        this(apiClient, downloader, config, Thread::sleep, System::currentTimeMillis,
                line -> LoggerUtil.info(LogRedactor.redact(line)));
    }

    public ReportPoller(ReportApiClient apiClient, PdfDownloader downloader, PollConfig config,
                        Sleeper sleeper, LongSupplier clock, Consumer<String> log) {
        this.apiClient = apiClient;
        this.downloader = downloader;
        this.config = config;
        this.extractor = new PdfTextExtractor(config.getMinPdfChars());
        this.matcher = new PdfResultMatcher();
        this.sleeper = sleeper;
        this.clock = clock;
        this.log = line -> log.accept(LogRedactor.redact(line));
    }

    /**
     * @param expected tests to verify; when empty they are derived from the API response
     */
    public PollOutcome poll(String visitNumber, List<ExpectedTest> expected) {
        return poll(visitNumber, expected, derived -> derived);
    }

    /**
     * @param enricher applied once to tests derived from the API (e.g. to attach expected values
     *                 known to the caller); ignored when {@code expected} is non-empty
     */
    public PollOutcome poll(String visitNumber, List<ExpectedTest> expected,
                            UnaryOperator<List<ExpectedTest>> enricher) {
        long start = clock.getAsLong();
        long deadline = start + config.getOverallTimeoutMs();
        List<ExpectedTest> tests = new ArrayList<>(expected == null ? new ArrayList<ExpectedTest>() : expected);
        Map<String, CachedPdf> cache = new LinkedHashMap<>();

        ReportEvaluation evaluation = null;
        ReportSnapshot snapshot = null;
        String lastBody = null;
        int lastStatus = 0;
        String lastProblem = "no attempt made";
        long newestVersion = -1;
        int attempt = 0;
        boolean saw404 = false;
        boolean sawUsableResponse = false;
        boolean timedOut = false;

        log.accept("[" + visitNumber + "] Polling report: max " + config.getMaxAttempts() + " attempts, every "
                + config.getIntervalMs() + "ms, timeout " + config.getOverallTimeoutMs() + "ms");

        while (attempt < config.getMaxAttempts()) {
            if (clock.getAsLong() >= deadline) {
                timedOut = true;
                break;
            }
            attempt++;
            ReportApiClient.ApiResponse response = fetchWithTransientRetries(visitNumber, attempt, deadline);
            lastStatus = response.getStatus();
            log.accept("[" + visitNumber + "] attempt " + attempt + "/" + config.getMaxAttempts() + " HTTP "
                    + response.getStatus() + " in " + response.getElapsedMs() + "ms");

            if (response.getStatus() == 404) {
                saw404 = true;
                lastProblem = "HTTP 404 - report not published yet (lab result entry/approval must finish first)";
                log.accept("[" + visitNumber + "]   pending: " + lastProblem);
            } else if (response.getStatus() != 200) {
                lastProblem = "HTTP " + response.getStatus() + " from report API";
                log.accept("[" + visitNumber + "]   unsuccessful response: " + lastProblem);
            } else {
                ReportSnapshot parsed = ReportSnapshot.parse(response.getBody());
                if (!parsed.isSuccess() || parsed.getEntries().isEmpty()) {
                    lastProblem = parsed.isSuccess()
                            ? "200 but 'data.tests' is empty"
                            : "200 but success=false (" + parsed.getMessage() + ")";
                    log.accept("[" + visitNumber + "]   unusable response: " + lastProblem);
                } else if (parsed.version() >= 0 && newestVersion >= 0 && parsed.version() < newestVersion) {
                    lastProblem = "stale response (version " + parsed.version() + " older than " + newestVersion + ")";
                    log.accept("[" + visitNumber + "]   ignored: " + lastProblem);
                } else {
                    sawUsableResponse = true;
                    saw404 = false;
                    newestVersion = Math.max(newestVersion, parsed.version());
                    snapshot = parsed;
                    lastBody = response.getBody();
                    if (tests.isEmpty()) {
                        tests = enricher.apply(parsed.toExpectedTests());
                        log.accept("[" + visitNumber + "]   expected tests derived from API: " + tests);
                    }
                    evaluation = evaluate(visitNumber, attempt, parsed, tests, cache);
                    logEvaluation(visitNumber, evaluation);
                    if (isDone(evaluation)) {
                        return outcome(visitNumber, PollOutcome.Status.COMPLETE, evaluation, snapshot, lastBody,
                                attempt, lastStatus, start, cache, "all expected results verified");
                    }
                    if (evaluation.pendingCount() == 0 && evaluation.failedCount() > 0
                            && parsed.nonHeaderCount() == parsed.getEntries().size()) {
                        // Everything is final on the API side and values are wrong: waiting cannot fix it.
                        return outcome(visitNumber, PollOutcome.Status.VALUE_MISMATCH, evaluation, snapshot,
                                lastBody, attempt, lastStatus, start, cache, evaluation.summary());
                    }
                    lastProblem = evaluation.summary();
                }
            }

            if (attempt >= config.getMaxAttempts()) {
                break;
            }
            long remaining = deadline - clock.getAsLong();
            if (remaining <= 0) {
                timedOut = true;
                break;
            }
            try {
                sleeper.sleep(Math.min(config.getIntervalMs(), remaining));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return outcome(visitNumber, PollOutcome.Status.INTERRUPTED, evaluation, snapshot, lastBody,
                        attempt, lastStatus, start, cache, "interrupted while waiting");
            }
        }

        PollOutcome.Status status;
        if (evaluation != null && evaluation.failedCount() > 0 && evaluation.pendingCount() == 0) {
            status = PollOutcome.Status.VALUE_MISMATCH;
        } else if (!sawUsableResponse) {
            status = saw404 ? PollOutcome.Status.NOT_PUBLISHED : PollOutcome.Status.API_ERROR;
        } else {
            status = timedOut ? PollOutcome.Status.INCOMPLETE_TIMEOUT : PollOutcome.Status.INCOMPLETE_MAX_ATTEMPTS;
        }
        log.accept("[" + visitNumber + "] Polling ended: " + status + " after " + attempt + " attempt(s) - "
                + lastProblem);
        return outcome(visitNumber, status, evaluation, snapshot, lastBody, attempt, lastStatus, start, cache,
                lastProblem);
    }

    // ------------------------------------------------------------------ HTTP

    private ReportApiClient.ApiResponse fetchWithTransientRetries(String visit, int attempt, long deadline) {
        ReportApiClient.ApiResponse response = apiClient.getReport(visit);
        int retry = 0;
        while (config.isTransient(response.getStatus()) && retry < config.getTransientRetries()) {
            long backoff = config.getTransientBackoffMs() * (1L << retry);
            if (clock.getAsLong() + backoff >= deadline) {
                break;
            }
            retry++;
            log.accept("[" + visit + "]   transient HTTP " + response.getStatus() + " (attempt " + attempt
                    + "), retry " + retry + "/" + config.getTransientRetries() + " in " + backoff + "ms");
            try {
                sleeper.sleep(backoff);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            response = apiClient.getReport(visit);
        }
        return response;
    }

    // ------------------------------------------------------------ evaluation

    private ReportEvaluation evaluate(String visit, int attempt, ReportSnapshot snapshot,
                                      List<ExpectedTest> tests, Map<String, CachedPdf> cache) {
        refreshPdfs(visit, snapshot, cache);
        Map<String, String> textByKey = new HashMap<>();
        StringBuilder combined = new StringBuilder();
        for (Map.Entry<String, CachedPdf> e : cache.entrySet()) {
            if (e.getValue().extraction.isOk()) {
                textByKey.put(e.getKey(), e.getValue().extraction.getText());
                combined.append(e.getValue().extraction.getText()).append('\n');
            }
        }
        String all = combined.toString();
        ReportEvaluation evaluation = matcher.evaluate(
                test -> textFor(test, snapshot, textByKey, all),
                tests,
                snapshot::completionFor);
        return config.isAcceptPdfOnly() ? evaluation : demotePdfOnly(evaluation);
    }

    /** Text authoritative for a test: its own PDF(s), else the combined text of every PDF read. */
    private String textFor(ExpectedTest test, ReportSnapshot snapshot, Map<String, String> textByKey, String all) {
        StringBuilder own = new StringBuilder();
        for (ReportSnapshot.Entry entry : snapshot.entriesFor(test)) {
            String text = textByKey.get(entry.getKey());
            if (text != null) {
                own.append(text).append('\n');
            }
        }
        return own.length() > 0 ? own.toString() : all;
    }

    private ReportEvaluation demotePdfOnly(ReportEvaluation evaluation) {
        List<TestResult> adjusted = new ArrayList<>();
        for (TestResult r : evaluation.getResults()) {
            if (r.getState() == ResultState.PDF_ONLY) {
                adjusted.add(new TestResult(r.getTest(), ResultState.NAME_ONLY, r.getValue(), r.getUnit(),
                        "value in PDF but API still header-only and report.acceptPdfOnly=false"));
            } else {
                adjusted.add(r);
            }
        }
        return new ReportEvaluation(adjusted);
    }

    private boolean isDone(ReportEvaluation evaluation) {
        return evaluation.isComplete();
    }

    // ------------------------------------------------------------------ PDFs

    private void refreshPdfs(String visit, ReportSnapshot snapshot, Map<String, CachedPdf> cache) {
        Map<String, ReportSnapshot.Entry> byKey = new LinkedHashMap<>();
        for (ReportSnapshot.Entry e : snapshot.getEntries()) {
            if (e.getUrl() != null && e.getKey() != null && !byKey.containsKey(e.getKey())) {
                byKey.put(e.getKey(), e);
            }
        }
        for (Map.Entry<String, ReportSnapshot.Entry> e : byKey.entrySet()) {
            String key = e.getKey();
            ReportSnapshot.Entry entry = e.getValue();
            CachedPdf cached = cache.get(key);
            boolean reuse = cached != null && cached.extraction.isOk() && cached.header == entry.isHeader();
            if (reuse) {
                cached.attemptsUnchanged++;
                if (cached.attemptsUnchanged % config.getStaleRecheckEvery() != 0) {
                    log.accept("[" + visit + "]   PDF " + shortKey(key) + " unchanged - reusing extracted text");
                    continue;
                }
                log.accept("[" + visit + "]   PDF " + shortKey(key) + " unchanged for "
                        + cached.attemptsUnchanged + " attempts - re-checking in case it was overwritten");
            }
            PdfExtraction extraction = download(visit, entry);
            CachedPdf fresh = new CachedPdf(entry.isHeader(), extraction);
            if (reuse) {
                fresh.attemptsUnchanged = cached.attemptsUnchanged;
            }
            cache.put(key, fresh);
        }
        // Full-report PDF, when the API publishes one, is extra evidence for tests without their own PDF.
        String full = snapshot.getFullDownloadUrl();
        if (full != null) {
            String fullKey = LogRedactor.stripQuery(full);
            if (!cache.containsKey(fullKey)) {
                PdfExtraction ex = downloadUrl(visit, fullKey, full);
                cache.put(fullKey, new CachedPdf(false, ex));
            }
        }
    }

    private PdfExtraction download(String visit, ReportSnapshot.Entry entry) {
        return downloadUrl(visit, entry.getKey(), entry.getUrl());
    }

    private PdfExtraction downloadUrl(String visit, String key, String url) {
        PdfDownloader.DownloadResult dl = downloader.download(url);
        if (!dl.isSuccess()) {
            String why = dl.getError() != null ? dl.getError() : "HTTP " + dl.getStatus();
            log.accept("[" + visit + "]   PDF " + shortKey(key) + " download failed: " + why);
            return PdfExtraction.failed(PdfExtraction.Status.NO_BYTES, "download failed: " + why);
        }
        PdfExtraction ex = extractor.extract(dl.getBytes());
        log.accept("[" + visit + "]   PDF " + shortKey(key) + " " + dl.getBytes().length + " bytes in "
                + dl.getElapsedMs() + "ms -> " + ex.getStatus() + " (" + ex.getDetail() + ")");
        return ex;
    }

    private static String shortKey(String key) {
        if (key == null) {
            return "?";
        }
        int slash = key.lastIndexOf('/');
        return slash >= 0 ? key.substring(slash + 1) : key;
    }

    // --------------------------------------------------------------- logging

    private void logEvaluation(String visit, ReportEvaluation evaluation) {
        log.accept("[" + visit + "]   sync: " + evaluation.summary());
        for (TestResult r : evaluation.getResults()) {
            log.accept("[" + visit + "]     " + r.getState() + " | " + r.getTest()
                    + (r.getValue() != null ? " | value=" + r.getValue() + (r.getUnit() != null ? " " + r.getUnit() : "") : "")
                    + " | " + r.getReason());
        }
    }

    private PollOutcome outcome(String visit, PollOutcome.Status status, ReportEvaluation evaluation,
                                ReportSnapshot snapshot, String body, int attempts, int lastStatus, long start,
                                Map<String, CachedPdf> cache, String detail) {
        Map<String, String> texts = new LinkedHashMap<>();
        for (Map.Entry<String, CachedPdf> e : cache.entrySet()) {
            if (e.getValue().extraction.isOk()) {
                texts.put(e.getKey(), e.getValue().extraction.getText());
            }
        }
        return new PollOutcome(visit, status, evaluation, snapshot, body, attempts, lastStatus,
                clock.getAsLong() - start, texts, detail);
    }
}

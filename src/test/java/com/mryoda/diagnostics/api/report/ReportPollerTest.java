package com.mryoda.diagnostics.api.report;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** Poller behaviour with a fake API, fake downloads and a fake clock - no network, no sleeping. */
public class ReportPollerTest {

    private static final String VISIT = "VIS-TEST-1";
    private static final String KEY_A = "PDF/VIS-TEST-1/VIS-TEST-1_TST001_1792045330_Header.pdf";
    private static final String KEY_B = "PDF/VIS-TEST-1/VIS-TEST-1_TST002_1792045330_Header.pdf";
    private static final String KEY_NEW = "PDF/VIS-TEST-1/VIS-TEST-1_TST001_1792045999_Result.pdf";
    private static final String KEY_OLD = "PDF/VIS-TEST-1/VIS-TEST-1_TST001_1700000000_Result.pdf";

    // --------------------------------------------------------------- fakes

    private static final class Fakes {
        final Deque<ReportApiClient.ApiResponse> responses = new ArrayDeque<>();
        final Map<String, byte[]> pdfs = new HashMap<>();
        final Map<String, Integer> downloads = new HashMap<>();
        final List<String> log = new ArrayList<>();
        final List<Long> sleeps = new ArrayList<>();
        final AtomicLong now = new AtomicLong(1_000_000L);
        int apiCalls;
        ReportApiClient.ApiResponse last;

        void respond(int status, String body) {
            responses.add(new ReportApiClient.ApiResponse(status, body, 5));
        }

        ReportPoller poller(PollConfig config) {
            ReportApiClient client = visit -> {
                apiCalls++;
                if (!responses.isEmpty()) {
                    last = responses.poll();
                }
                return last;
            };
            PdfDownloader downloader = url -> {
                String key = LogRedactor.stripQuery(url).replace("https://reports.example.test/", "");
                downloads.merge(key, 1, Integer::sum);
                byte[] bytes = pdfs.get(key);
                return bytes == null
                        ? new PdfDownloader.DownloadResult(404, null, "HTTP 404", 1)
                        : new PdfDownloader.DownloadResult(200, bytes, null, 1);
            };
            return new ReportPoller(client, downloader, config,
                    millis -> {
                        sleeps.add(millis);
                        now.addAndGet(millis);
                    },
                    now::get, log::add);
        }
    }

    private static PollConfig.Builder config() {
        return PollConfig.builder().intervalMs(15_000).maxAttempts(5).overallTimeoutMs(10 * 60_000L)
                .transientRetries(2).transientBackoffMs(2_000).minPdfChars(20);
    }

    private static ExpectedTest clottingTime() {
        return ExpectedTest.builder("CLOTTING TIME").code("TST001").build();
    }

    private static byte[] resultPdf() {
        return PdfFixtures.pdf("LAB REPORT VISIT VIS-TEST-1", "Investigation|Result|Unit|Reference",
                "CLOTTING TIME|105|Min|5 - 10 Min");
    }

    private static byte[] headerOnlyPdf() {
        return PdfFixtures.pdf("LAB REPORT VISIT VIS-TEST-1", "Investigation|Result|Unit|Reference", "CLOTTING TIME");
    }

    private static String headerOnlyReport() {
        return PdfFixtures.report(VISIT, PdfFixtures.entry("CLOTTING TIME", "TST001", "HAEMATOLOGY", KEY_A, true));
    }

    private static String completedReport() {
        return PdfFixtures.report(VISIT, PdfFixtures.entry("CLOTTING TIME", "TST001", "HAEMATOLOGY", KEY_A, false));
    }

    // --------------------------------------------------------------- tests

    @Test(description = "404 (not published) then a completed report with a value => COMPLETE")
    public void completesOnceResultAppears() {
        Fakes f = new Fakes();
        f.respond(404, "{\"status\":404,\"success\":false,\"msg\":\"No report found for the given visit number\"}");
        f.respond(404, "{\"status\":404,\"success\":false,\"msg\":\"No report found for the given visit number\"}");
        f.respond(200, completedReport());
        f.pdfs.put(KEY_A, resultPdf());

        PollOutcome out = f.poller(config().build()).poll(VISIT, Collections.singletonList(clottingTime()));

        Assert.assertEquals(out.getStatus(), PollOutcome.Status.COMPLETE, out.getDetail());
        Assert.assertEquals(out.getAttempts(), 3);
        Assert.assertEquals(f.sleeps, Arrays.asList(15_000L, 15_000L));
        TestResult r = out.getEvaluation().getResults().get(0);
        Assert.assertEquals(r.getState(), ResultState.SYNCED);
        Assert.assertEquals(r.getValue(), "105");
        Assert.assertTrue(out.combinedPdfText().contains("CLOTTING TIME"));
    }

    @Test(description = "Regression: API says isHeader:true but the PDF already holds approved results")
    public void apiHeaderFlagButPdfHasResultsCompletesOnFirstResponse() {
        Fakes f = new Fakes();
        f.respond(404, "{\"status\":404,\"success\":false,\"msg\":\"No report found for the given visit number\"}");
        f.respond(200, PdfFixtures.report(VISIT,
                PdfFixtures.entry("CBC(COMPLETE BLOOD COUNT)", "TST010", "HAEMATOLOGY", KEY_A, true),
                PdfFixtures.entry("CLOTTING TIME", "TST001", "HAEMATOLOGY", KEY_B, true)));
        f.pdfs.put(KEY_A, PdfFixtures.pdf(
                "Visit ID : VIS-TEST-1|UHID/MR No : UH.0000000001",
                "Test Name|Result|Unit|Biological Ref. Range|Method",
                "*CBC(COMPLETE BLOOD COUNT)",
                "Sample Type : WHOLE BLOOD BOTTLE",
                "HAEMOGLOBIN (HB)|15.0|g/dl|13.0 - 17.0",
                "TOTAL LEUCOCYTE COUNT|7,000|cells/ml|4000 - 11000",
                "*** End Of Report ***"));
        f.pdfs.put(KEY_B, PdfFixtures.pdf(
                "Visit ID : VIS-TEST-1|UHID/MR No : UH.0000000001",
                "Test Name|Result|Unit|Biological Ref. Range|Method",
                "*CLOTTING TIME",
                "Sample Type : CSF",
                "CLOTTING TIME|5|min|3 - 8",
                "*** End Of Report ***"));

        PollOutcome out = f.poller(config().build()).poll(VISIT, Collections.<ExpectedTest>emptyList());

        Assert.assertEquals(out.getStatus(), PollOutcome.Status.COMPLETE, out.getDetail());
        Assert.assertEquals(out.getAttempts(), 2, "complete on the first 200 - no further polling");
        Assert.assertEquals(out.getEvaluation().count(ResultState.PDF_ONLY), 2);
        Assert.assertEquals(out.getEvaluation().getResults().get(0).getValue(), "15.0");
        Assert.assertEquals(out.getEvaluation().getResults().get(1).getValue(), "5");
    }

    @Test(description = "Header-only report is never reported as synced; reason is explained")
    public void headerOnlyIsNeverFabricatedAsComplete() {
        Fakes f = new Fakes();
        f.respond(200, headerOnlyReport());
        f.pdfs.put(KEY_A, headerOnlyPdf());

        PollOutcome out = f.poller(config().maxAttempts(4).build())
                .poll(VISIT, Collections.singletonList(clottingTime()));

        Assert.assertEquals(out.getStatus(), PollOutcome.Status.INCOMPLETE_MAX_ATTEMPTS);
        Assert.assertEquals(out.getAttempts(), 4);
        Assert.assertEquals(out.getEvaluation().completedCount(), 0);
        Assert.assertEquals(out.getEvaluation().pendingCount(), 1);
        TestResult r = out.getEvaluation().getResults().get(0);
        Assert.assertEquals(r.getState(), ResultState.NAME_ONLY);
        Assert.assertTrue(r.getReason().contains("no result value"), r.getReason());
        // unchanged PDF is not re-processed every attempt: download, reuse, reuse, re-check on 3rd repeat
        Assert.assertEquals(f.downloads.get(KEY_A).intValue(), 2, "stale PDF re-downloaded too often");
    }

    @Test(description = "Transient 503s are retried with backoff inside one attempt")
    public void transientErrorsAreRetried() {
        Fakes f = new Fakes();
        f.respond(503, "<html>503</html>");
        f.respond(503, "<html>503</html>");
        f.respond(200, completedReport());
        f.pdfs.put(KEY_A, resultPdf());

        PollOutcome out = f.poller(config().build()).poll(VISIT, Collections.singletonList(clottingTime()));

        Assert.assertEquals(out.getStatus(), PollOutcome.Status.COMPLETE);
        Assert.assertEquals(out.getAttempts(), 1);
        Assert.assertEquals(f.apiCalls, 3);
        Assert.assertEquals(f.sleeps, Arrays.asList(2_000L, 4_000L), "exponential backoff");
        Assert.assertTrue(f.log.stream().anyMatch(l -> l.contains("transient HTTP 503")), f.log.toString());
    }

    @Test(description = "Persistent 503 is bounded: attempts x (1 + retries) calls, then API_ERROR")
    public void repeatedTransientFailureIsBounded() {
        Fakes f = new Fakes();
        f.respond(503, "<html>503</html>");

        PollOutcome out = f.poller(config().maxAttempts(2).transientRetries(2).build())
                .poll(VISIT, Collections.singletonList(clottingTime()));

        Assert.assertEquals(out.getStatus(), PollOutcome.Status.API_ERROR);
        Assert.assertEquals(f.apiCalls, 6);
        Assert.assertEquals(out.getLastHttpStatus(), 503);
    }

    @Test(description = "404 on every attempt => NOT_PUBLISHED with a lab-side explanation")
    public void neverPublishedIsDistinguishedFromApiError() {
        Fakes f = new Fakes();
        f.respond(404, "{\"status\":404,\"success\":false,\"msg\":\"No report found for the given visit number\"}");

        PollOutcome out = f.poller(config().maxAttempts(3).build())
                .poll(VISIT, Collections.singletonList(clottingTime()));

        Assert.assertEquals(out.getStatus(), PollOutcome.Status.NOT_PUBLISHED);
        Assert.assertEquals(out.getAttempts(), 3);
        Assert.assertTrue(out.getDetail().contains("404"), out.getDetail());
        Assert.assertNull(out.getSnapshot());
    }

    @Test(description = "Overall timeout stops polling before max attempts")
    public void overallTimeoutStopsPolling() {
        Fakes f = new Fakes();
        f.respond(200, headerOnlyReport());
        f.pdfs.put(KEY_A, headerOnlyPdf());

        PollOutcome out = f.poller(config().intervalMs(60_000).maxAttempts(50).overallTimeoutMs(100_000).build())
                .poll(VISIT, Collections.singletonList(clottingTime()));

        Assert.assertEquals(out.getStatus(), PollOutcome.Status.INCOMPLETE_TIMEOUT);
        Assert.assertEquals(out.getAttempts(), 2);
        Assert.assertEquals(f.sleeps, Arrays.asList(60_000L, 40_000L), "last sleep is capped to remaining time");
    }

    @Test(description = "A response older than one already seen is ignored (stale), and not downloaded")
    public void staleResponseIsIgnored() {
        Fakes f = new Fakes();
        f.respond(200, PdfFixtures.report(VISIT,
                PdfFixtures.entry("CLOTTING TIME", "TST001", "HAEMATOLOGY", KEY_NEW, true)));
        f.respond(200, PdfFixtures.report(VISIT,
                PdfFixtures.entry("CLOTTING TIME", "TST001", "HAEMATOLOGY", KEY_OLD, false)));
        f.respond(200, PdfFixtures.report(VISIT,
                PdfFixtures.entry("CLOTTING TIME", "TST001", "HAEMATOLOGY", KEY_NEW, true)));
        f.pdfs.put(KEY_NEW, headerOnlyPdf());
        f.pdfs.put(KEY_OLD, resultPdf());

        PollOutcome out = f.poller(config().maxAttempts(3).build())
                .poll(VISIT, Collections.singletonList(clottingTime()));

        Assert.assertNotEquals(out.getStatus(), PollOutcome.Status.COMPLETE);
        Assert.assertNull(f.downloads.get(KEY_OLD), "stale PDF must not be downloaded or trusted");
        Assert.assertTrue(f.log.stream().anyMatch(l -> l.contains("stale response")), f.log.toString());
    }

    @Test(description = "Corrupt PDF is a pending reason, never a crash and never a pass")
    public void malformedPdfIsReported() {
        Fakes f = new Fakes();
        f.respond(200, completedReport());
        f.pdfs.put(KEY_A, "%PDF-1.4\n garbage that is not a pdf".getBytes(StandardCharsets.US_ASCII));

        PollOutcome out = f.poller(config().maxAttempts(2).build())
                .poll(VISIT, Collections.singletonList(clottingTime()));

        Assert.assertFalse(out.isComplete());
        Assert.assertEquals(out.getEvaluation().getResults().get(0).getState(), ResultState.NOT_FOUND);
        Assert.assertTrue(f.log.stream().anyMatch(l -> l.contains("MALFORMED")), f.log.toString());
    }

    @Test(description = "200 with success=false is an unsuccessful response; secrets are redacted in logs")
    public void unsuccessfulBodyHandledAndRedacted() {
        Fakes f = new Fakes();
        f.respond(200, "{\"status\":200,\"success\":false,\"msg\":\"auth failed token=abc123SECRET\"}");

        PollOutcome out = f.poller(config().maxAttempts(2).build())
                .poll(VISIT, Collections.singletonList(clottingTime()));

        Assert.assertEquals(out.getStatus(), PollOutcome.Status.API_ERROR);
        String all = String.join("\n", f.log);
        Assert.assertTrue(all.contains("success=false"), all);
        Assert.assertFalse(all.contains("abc123SECRET"), "secret leaked into logs: " + all);
    }

    @Test(description = "report.acceptPdfOnly=false requires the API entry to be completed too")
    public void pdfOnlyEvidenceIsConfigurable() {
        Fakes accept = new Fakes();
        accept.respond(200, headerOnlyReport());
        accept.pdfs.put(KEY_A, resultPdf());
        PollOutcome accepted = accept.poller(config().acceptPdfOnly(true).build())
                .poll(VISIT, Collections.singletonList(clottingTime()));
        Assert.assertEquals(accepted.getStatus(), PollOutcome.Status.COMPLETE);
        Assert.assertEquals(accepted.getEvaluation().getResults().get(0).getState(), ResultState.PDF_ONLY);

        Fakes strict = new Fakes();
        strict.respond(200, headerOnlyReport());
        strict.pdfs.put(KEY_A, resultPdf());
        PollOutcome rejected = strict.poller(config().acceptPdfOnly(false).maxAttempts(2).build())
                .poll(VISIT, Collections.singletonList(clottingTime()));
        Assert.assertFalse(rejected.isComplete());
        Assert.assertEquals(rejected.getEvaluation().completedCount(), 0);
    }

    @Test(description = "Expected tests are derived from the API response when none are supplied")
    public void derivesExpectedTestsFromApi() {
        Fakes f = new Fakes();
        f.respond(200, PdfFixtures.report(VISIT,
                PdfFixtures.entry("CLOTTING TIME", "TST001", "HAEMATOLOGY", KEY_A, false),
                PdfFixtures.entry("HEMOGLOBIN", "TST002", "HAEMATOLOGY", KEY_B, false)));
        f.pdfs.put(KEY_A, resultPdf());
        f.pdfs.put(KEY_B, PdfFixtures.pdf("LAB REPORT VISIT VIS-TEST-1", "HEMOGLOBIN|13.5|g/dL|12 - 16"));

        PollOutcome out = f.poller(config().build()).poll(VISIT, Collections.<ExpectedTest>emptyList());

        Assert.assertEquals(out.getStatus(), PollOutcome.Status.COMPLETE, out.getDetail());
        Assert.assertEquals(out.getEvaluation().total(), 2);
        Assert.assertEquals(out.getEvaluation().getResults().get(1).getValue(), "13.5");
    }

    @Test(description = "Each test is verified against its own PDF, not another test's")
    public void perTestEvidenceIsIndependent() {
        Fakes f = new Fakes();
        f.respond(200, PdfFixtures.report(VISIT,
                PdfFixtures.entry("CLOTTING TIME", "TST001", "HAEMATOLOGY", KEY_A, false),
                PdfFixtures.entry("HEMOGLOBIN", "TST002", "HAEMATOLOGY", KEY_B, false)));
        f.pdfs.put(KEY_A, headerOnlyPdf());                       // CLOTTING TIME: name only
        f.pdfs.put(KEY_B, PdfFixtures.pdf("LAB REPORT VISIT VIS-TEST-1",
                "HEMOGLOBIN|13.5|g/dL|12 - 16", "CLOTTING TIME|105|Min|5 - 10 Min")); // value in the OTHER pdf

        PollOutcome out = f.poller(config().maxAttempts(1).build()).poll(VISIT,
                Arrays.asList(clottingTime(), ExpectedTest.builder("HEMOGLOBIN").code("TST002").build()));

        Assert.assertFalse(out.isComplete());
        Assert.assertEquals(out.getEvaluation().getResults().get(0).getState(), ResultState.NAME_ONLY);
        Assert.assertEquals(out.getEvaluation().getResults().get(1).getState(), ResultState.SYNCED);
        Assert.assertEquals(out.getEvaluation().completedCount(), 1);
        Assert.assertEquals(out.getEvaluation().pendingCount(), 1);
    }
}

package com.mryoda.diagnostics.api.report;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Final result of polling one visit. */
public final class PollOutcome {

    public enum Status {
        /** Every expected test has verified evidence. */
        COMPLETE,
        /** Some tests verified, others still pending when attempts ran out. */
        INCOMPLETE_MAX_ATTEMPTS,
        /** Overall timeout reached before all tests were verified. */
        INCOMPLETE_TIMEOUT,
        /** API kept answering 404 "no report" - lab approval/sync never produced a report. */
        NOT_PUBLISHED,
        /** API never gave a usable response (5xx, bad JSON, success=false). */
        API_ERROR,
        /** All values found but at least one differs from its expected business value. */
        VALUE_MISMATCH,
        INTERRUPTED
    }

    private final String visitNumber;
    private final Status status;
    private final ReportEvaluation evaluation;
    private final ReportSnapshot snapshot;
    private final String lastResponseBody;
    private final int attempts;
    private final int lastHttpStatus;
    private final long elapsedMs;
    private final Map<String, String> pdfTextByKey;
    private final String detail;

    PollOutcome(String visitNumber, Status status, ReportEvaluation evaluation, ReportSnapshot snapshot,
                String lastResponseBody, int attempts, int lastHttpStatus, long elapsedMs,
                Map<String, String> pdfTextByKey, String detail) {
        this.visitNumber = visitNumber;
        this.status = status;
        this.evaluation = evaluation == null ? ReportEvaluation.empty() : evaluation;
        this.snapshot = snapshot;
        this.lastResponseBody = lastResponseBody;
        this.attempts = attempts;
        this.lastHttpStatus = lastHttpStatus;
        this.elapsedMs = elapsedMs;
        this.pdfTextByKey = Collections.unmodifiableMap(new LinkedHashMap<>(pdfTextByKey));
        this.detail = detail;
    }

    public String getVisitNumber() {
        return visitNumber;
    }

    public Status getStatus() {
        return status;
    }

    public boolean isComplete() {
        return status == Status.COMPLETE;
    }

    public ReportEvaluation getEvaluation() {
        return evaluation;
    }

    /** Last successfully parsed API response, or null if none was ever parsed. */
    public ReportSnapshot getSnapshot() {
        return snapshot;
    }

    /** Raw body of the last HTTP 200 response (for callers that need the original JSON). */
    public String getLastResponseBody() {
        return lastResponseBody;
    }

    public int getAttempts() {
        return attempts;
    }

    public int getLastHttpStatus() {
        return lastHttpStatus;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    /** Extracted text per PDF object key (query string removed). */
    public Map<String, String> getPdfTextByKey() {
        return pdfTextByKey;
    }

    /** All extracted PDF texts joined; empty when no PDF could be read. */
    public String combinedPdfText() {
        return String.join("\n", pdfTextByKey.values());
    }

    public String getDetail() {
        return detail;
    }
}

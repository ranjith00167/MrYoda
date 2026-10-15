package com.mryoda.diagnostics.api.report;

/** Fetches the report metadata for a visit. Implementations must not throw for HTTP errors. */
public interface ReportApiClient {

    /** Result of one HTTP call. */
    final class ApiResponse {
        private final int status;
        private final String body;
        private final long elapsedMs;

        public ApiResponse(int status, String body, long elapsedMs) {
            this.status = status;
            this.body = body;
            this.elapsedMs = elapsedMs;
        }

        public int getStatus() {
            return status;
        }

        public String getBody() {
            return body;
        }

        public long getElapsedMs() {
            return elapsedMs;
        }
    }

    ApiResponse getReport(String visitNumber);
}

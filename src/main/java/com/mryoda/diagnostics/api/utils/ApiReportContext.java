package com.mryoda.diagnostics.api.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Thread-safe context for capturing API request/response details
 * to be consumed by the Extent Report listener.
 * Each test method gets its own list of API call records.
 */
public class ApiReportContext {

    private static final ThreadLocal<List<ApiCallRecord>> callRecords = ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<Integer> nextExpectedStatus = ThreadLocal.withInitial(() -> 200);
    // Range support: -1 means "not set" (use exact match instead)
    private static final ThreadLocal<int[]> nextExpectedStatusRange = ThreadLocal.withInitial(() -> new int[]{-1, -1});
    // Extra per-test detail lines rendered in the Extent Report details panel
    private static final ThreadLocal<List<String>> extraDetails = ThreadLocal.withInitial(ArrayList::new);

    public static void clear() {
        callRecords.get().clear();
        extraDetails.get().clear();
    }

    /**
     * Add an HTML-formatted detail line to be rendered in the Extent Report
     * under the VALIDATION DETAILS section for the current test.
     * Example: addExtraDetail("<b>Content-Type:</b> application/json; charset=UTF-8")
     */
    public static void addExtraDetail(String htmlDetail) {
        extraDetails.get().add(htmlDetail);
    }

    public static List<String> getExtraDetails() {
        return new ArrayList<>(extraDetails.get());
    }

    /** Use for positive tests: expects an exact HTTP status code (e.g. 200). */
    public static void setExpectedStatus(int status) {
        nextExpectedStatus.set(status);
        nextExpectedStatusRange.set(new int[]{-1, -1}); // clear any range
    }

    /**
     * Use for negative / security tests: any status code in [min, max] is acceptable.
     * Example: setExpectedStatusRange(400, 499) → shows "4xx" in the Extent Report
     * and marks green for any 400-499 response.
     */
    public static void setExpectedStatusRange(int min, int max) {
        nextExpectedStatusRange.set(new int[]{min, max});
        nextExpectedStatus.set(min); // keep backward-compat value
    }

    public static int getAndResetExpectedStatus() {
        int status = nextExpectedStatus.get();
        nextExpectedStatus.set(200);
        return status;
    }

    /** Returns int[]{min, max}. Both are -1 when no range was set (fall back to exact match). */
    public static int[] getAndResetExpectedStatusRange() {
        int[] range = nextExpectedStatusRange.get();
        nextExpectedStatusRange.set(new int[]{-1, -1});
        return range;
    }

    public static void addRecord(ApiCallRecord record) {
        callRecords.get().add(record);
    }

    public static List<ApiCallRecord> getRecords() {
        return new ArrayList<>(callRecords.get());
    }

    public static class ApiCallRecord {
        private final String method;
        private final String endpoint;
        private final String requestBody;
        private final int statusCode;
        private final String responseBody;
        private final long responseTimeMs;
        private final String timestamp;
        private final int expectedStatus;
        private final int expectedMin;  // -1 = not a range
        private final int expectedMax;  // -1 = not a range
        private final String description;

        /** Legacy constructor — exact status code match (positive tests). */
        public ApiCallRecord(String method, String endpoint, String requestBody,
                             int statusCode, String responseBody, long responseTimeMs,
                             int expectedStatus, String description) {
            this(method, endpoint, requestBody, statusCode, responseBody,
                 responseTimeMs, expectedStatus, -1, -1, description);
        }

        /** Full constructor — supports both exact-match and range-based expected status. */
        public ApiCallRecord(String method, String endpoint, String requestBody,
                             int statusCode, String responseBody, long responseTimeMs,
                             int expectedStatus, int expectedMin, int expectedMax,
                             String description) {
            this.method = method;
            this.endpoint = endpoint;
            this.requestBody = requestBody;
            this.statusCode = statusCode;
            this.responseBody = responseBody;
            this.responseTimeMs = responseTimeMs;
            this.expectedStatus = expectedStatus;
            this.expectedMin = expectedMin;
            this.expectedMax = expectedMax;
            this.description = description;
            this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("h:mm:ss a"));
        }

        public String getMethod() { return method; }
        public String getEndpoint() { return endpoint; }
        public String getRequestBody() { return requestBody; }
        public int getStatusCode() { return statusCode; }
        public String getResponseBody() { return responseBody; }
        public long getResponseTimeMs() { return responseTimeMs; }
        public String getTimestamp() { return timestamp; }
        public int getExpectedStatus() { return expectedStatus; }
        public String getDescription() { return description; }

        /**
         * Human-readable expected status label shown in the Extent Report.
         * Range case  → "4xx (400–499)"
         * Exact case  → "200"
         */
        public String getExpectedStatusLabel() {
            if (expectedMin >= 0 && expectedMax >= 0) {
                int century = expectedMin / 100;
                return century + "xx (" + expectedMin + "-" + expectedMax + ")";
            }
            return String.valueOf(expectedStatus);
        }

        /**
         * True when the actual status satisfies the expected condition.
         * Range:  expectedMin <= statusCode <= expectedMax
         * Exact:  statusCode == expectedStatus  (or any 2xx when expectedStatus == 0)
         */
        public boolean isStatusInExpectedRange() {
            if (expectedMin >= 0 && expectedMax >= 0) {
                return statusCode >= expectedMin && statusCode <= expectedMax;
            }
            return statusCode == expectedStatus
                    || (expectedStatus == 0 && statusCode >= 200 && statusCode < 300);
        }

        /** @deprecated Use isStatusInExpectedRange() */
        public boolean isPassed() {
            return isStatusInExpectedRange();
        }
    }
}

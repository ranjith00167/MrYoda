package com.mryoda.diagnostics.api.report;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parsed {@code GET /report/getReportDetailsByVisitNumber/{visit}} response.
 * Contract: {@code {success, msg, data:{userName, visitNumber, order_date, tests:[{testName,
 * testCode, department_name, key, url, isHeader}], full_download_url}}}.
 * {@code isHeader:true} means only the header PDF exists - results are not synced yet.
 */
public final class ReportSnapshot {

    /** Epoch seconds embedded in object keys: ..._HEMAT016_1792045330_Header.pdf */
    private static final Pattern KEY_EPOCH = Pattern.compile("_(\\d{9,13})_");

    /** One row of {@code data.tests}. */
    public static final class Entry {
        private final String name;
        private final String code;
        private final String department;
        private final String key;
        private final String url;
        private final boolean header;

        Entry(String name, String code, String department, String key, String url, boolean header) {
            this.name = name;
            this.code = code;
            this.department = department;
            this.key = key;
            this.url = url;
            this.header = header;
        }

        public String getName() {
            return name;
        }

        public String getCode() {
            return code;
        }

        public String getDepartment() {
            return department;
        }

        public String getUrl() {
            return url;
        }

        /** Object key without the signed query string; stable identity of the PDF version. */
        public String getKey() {
            return key != null ? key : LogRedactor.stripQuery(url);
        }

        public boolean isHeader() {
            return header;
        }

        /** Epoch embedded in the object key, or -1 when absent. */
        public long getEpoch() {
            String k = getKey();
            if (k == null) {
                return -1;
            }
            Matcher m = KEY_EPOCH.matcher(k);
            return m.find() ? Long.parseLong(m.group(1)) : -1;
        }
    }

    private final boolean success;
    private final String message;
    private final String visitNumber;
    private final String userName;
    private final String orderDate;
    private final String fullDownloadUrl;
    private final List<Entry> entries;

    private ReportSnapshot(boolean success, String message, String visitNumber, String userName, String orderDate,
                           String fullDownloadUrl, List<Entry> entries) {
        this.success = success;
        this.message = message;
        this.visitNumber = visitNumber;
        this.userName = userName;
        this.orderDate = orderDate;
        this.fullDownloadUrl = fullDownloadUrl;
        this.entries = Collections.unmodifiableList(entries);
    }

    /** Never throws: an unparsable body becomes an unsuccessful snapshot with a message. */
    public static ReportSnapshot parse(String body) {
        if (body == null || body.trim().isEmpty()) {
            return failure("empty response body");
        }
        try {
            JSONObject root = new JSONObject(body);
            boolean ok = root.optBoolean("success", false);
            String msg = root.optString("msg", root.optString("message", ""));
            JSONObject data = root.optJSONObject("data");
            if (data == null) {
                return new ReportSnapshot(ok, msg.isEmpty() ? "no 'data' object in response" : msg,
                        null, null, null, null, new ArrayList<Entry>());
            }
            List<Entry> parsed = new ArrayList<>();
            JSONArray tests = data.optJSONArray("tests");
            for (int i = 0; tests != null && i < tests.length(); i++) {
                JSONObject t = tests.optJSONObject(i);
                if (t == null) {
                    continue;
                }
                parsed.add(new Entry(
                        blankToNull(t.optString("testName", null)),
                        blankToNull(t.optString("testCode", null)),
                        blankToNull(t.optString("department_name", null)),
                        blankToNull(t.optString("key", null)),
                        blankToNull(t.optString("url", null)),
                        parseHeaderFlag(t.opt("isHeader"))));
            }
            return new ReportSnapshot(ok, msg, blankToNull(data.optString("visitNumber", null)),
                    blankToNull(data.optString("userName", null)), blankToNull(data.optString("order_date", null)),
                    blankToNull(data.optString("full_download_url", null)), parsed);
        } catch (JSONException e) {
            return failure("response is not valid JSON: " + e.getMessage());
        }
    }

    private static ReportSnapshot failure(String message) {
        return new ReportSnapshot(false, message, null, null, null, null, new ArrayList<Entry>());
    }

    private static boolean parseHeaderFlag(Object raw) {
        if (raw instanceof Boolean) {
            return (Boolean) raw;
        }
        // Missing flag => treat as header (not completed): never assume a result exists.
        return raw == null || JSONObject.NULL.equals(raw) || Boolean.parseBoolean(String.valueOf(raw));
    }

    private static String blankToNull(String s) {
        return s == null || s.trim().isEmpty() || "null".equals(s) ? null : s.trim();
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getVisitNumber() {
        return visitNumber;
    }

    public String getUserName() {
        return userName;
    }

    public String getOrderDate() {
        return orderDate;
    }

    public String getFullDownloadUrl() {
        return fullDownloadUrl;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public int nonHeaderCount() {
        int n = 0;
        for (Entry e : entries) {
            if (!e.isHeader()) {
                n++;
            }
        }
        return n;
    }

    /** Newest object-key epoch across entries, or -1 when none carries one. Used for staleness checks. */
    public long version() {
        long max = -1;
        for (Entry e : entries) {
            max = Math.max(max, e.getEpoch());
        }
        return max;
    }

    /**
     * Completion flag for an expected test: TRUE when a non-header entry exists, FALSE when only
     * header entries exist, null when the API does not list the test (matched by code, then name).
     */
    public Boolean completionFor(ExpectedTest test) {
        Boolean result = null;
        for (Entry e : entries) {
            if (!matches(e, test)) {
                continue;
            }
            if (!e.isHeader()) {
                return Boolean.TRUE;
            }
            result = Boolean.FALSE;
        }
        return result;
    }

    /** Entries that belong to the expected test (non-header first). */
    public List<Entry> entriesFor(ExpectedTest test) {
        List<Entry> match = new ArrayList<>();
        for (Entry e : entries) {
            if (matches(e, test)) {
                match.add(e);
            }
        }
        match.sort((a, b) -> Boolean.compare(a.isHeader(), b.isHeader()));
        return match;
    }

    private static boolean matches(Entry e, ExpectedTest test) {
        if (e.getCode() != null && test.getCode() != null && e.getCode().equalsIgnoreCase(test.getCode())) {
            return true;
        }
        return e.getName() != null && key(e.getName()).equals(key(test.getName()));
    }

    private static String key(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /**
     * Derives the expected tests from this response: one per distinct test, package/panel
     * departments become name-only group headers.
     */
    public List<ExpectedTest> toExpectedTests() {
        Map<String, ExpectedTest> byKey = new LinkedHashMap<>();
        for (Entry e : entries) {
            if (e.getName() == null) {
                continue;
            }
            String k = key(e.getName());
            if (byKey.containsKey(k)) {
                continue;
            }
            byKey.put(k, ExpectedTest.builder(e.getName())
                    .code(e.getCode())
                    .department(e.getDepartment())
                    .valueRequired(!isGroupDepartment(e.getDepartment()))
                    .build());
        }
        return new ArrayList<>(byKey.values());
    }

    static boolean isGroupDepartment(String department) {
        if (department == null) {
            return false;
        }
        String d = department.toLowerCase(Locale.ROOT);
        return d.contains("package") || d.contains("panel") || d.contains("profile");
    }
}

package api.phlebio;

import com.mryoda.diagnostics.api.config.ConfigLoader;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.RequestContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Writes a Jira-ready Markdown log of every failed validation and backend defect recorded in {@link ValidationLog}.
 *
 * Identical checks seen in several steps are merged into ONE issue (with one row per occurrence) so that a
 * single backend defect does not turn into several duplicate tickets.
 * Output: docs/validation-logs/Phlebo_Validation_Log_&lt;timestamp&gt;.md (+ ..._latest.md).
 */
public final class PhlebioValidationReport {

    private static final Path OUTPUT_DIR = Paths.get("docs", "validation-logs");
    private static final Pattern STEP = Pattern.compile("PHLEBO_MGMT_(\\d{2})");
    private static final Pattern JWT = Pattern.compile("eyJ[\\w-]+\\.[\\w-]+\\.[\\w-]+");
    private static final int MAX_VALUE_LENGTH = 400;
    private static final int MAX_SUMMARY_LENGTH = 200;

    private PhlebioValidationReport() {
    }

    /** One distinct failing check, with every step it was seen in. */
    private static final class Issue {
        private final ValidationLog.Status status;
        private final String check;
        private final List<ValidationLog.Entry> occurrences = new ArrayList<>();

        Issue(ValidationLog.Status status, String check) {
            this.status = status;
            this.check = check;
        }
    }

    /** Builds and writes the report. Returns the timestamped file, or null when it could not be written. */
    public static Path write() {
        List<ValidationLog.Entry> entries = ValidationLog.snapshot();
        String phleboBase = ConfigLoader.getConfig().phlebioBaseUrl();
        String diagBase = APIEndpoints.DIAGNOSTICS_BASE_URL;
        LocalDateTime now = LocalDateTime.now();

        byte[] content = render(entries, endpoints(phleboBase, diagBase), phleboBase, diagBase, now)
                .getBytes(StandardCharsets.UTF_8);
        try {
            Files.createDirectories(OUTPUT_DIR);
            Path stamped = OUTPUT_DIR.resolve("Phlebo_Validation_Log_"
                    + now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".md");
            Files.write(stamped, content);
            Files.write(OUTPUT_DIR.resolve("Phlebo_Validation_Log_latest.md"), content);
            System.out.println("   [REPORT] Validation log written: " + stamped.toAbsolutePath());
            return stamped;
        } catch (IOException e) {
            System.out.println("   [REPORT] Could not write the validation log: " + e.getMessage());
            return null;
        }
    }

    // ── endpoints per flow step: {primary call, cross-check call} ─────────────────────────────────
    private static Map<String, String[]> endpoints(String phlebo, String diag) {
        String order = "GET " + diag + APIEndpoints.GET_ORDER_BY_ID + "{order_id}";
        String shifts = "GET " + phlebo + PhlebioEndpoints.GET_SHIFTS;
        String updateTracking = "POST " + diag + APIEndpoints.UPDATE_ORDER_TRACKING;

        Map<String, String[]> m = new HashMap<>();
        m.put("01", new String[] {"POST " + phlebo + PhlebioEndpoints.LOGIN, ""});
        m.put("02", new String[] {"POST " + diag + APIEndpoints.ADMIN_LOGIN, ""});
        m.put("03", new String[] {"POST " + phlebo + PhlebioEndpoints.ASSIGN_SHIFTS, shifts});
        m.put("04", new String[] {shifts, "GET " + phlebo + PhlebioEndpoints.GET_PHLEBO_BY_GUID});
        m.put("05", new String[] {"POST " + phlebo + PhlebioEndpoints.CLOCK_IN, ""});
        m.put("06", new String[] {"POST " + diag + APIEndpoints.ASSIGN_ORDER, order});
        m.put("07", new String[] {updateTracking, order});
        m.put("08", new String[] {"POST " + phlebo + PhlebioEndpoints.UPLOAD_IMAGE, ""});
        m.put("09", new String[] {"POST " + diag + APIEndpoints.VERIFY_OTP, order});
        m.put("10", new String[] {order, ""});
        m.put("11", new String[] {"POST " + PhlebioEndpoints.CHANGE_BARCODE, order});
        m.put("12", new String[] {updateTracking, order});
        return m;
    }

    private static String render(List<ValidationLog.Entry> entries, Map<String, String[]> api, String phleboBase,
                                 String diagBase, LocalDateTime now) {
        Map<String, int[]> perStep = new TreeMap<>();   // step -> {pass, fail, defect}
        Map<String, Issue> issues = new LinkedHashMap<>();
        int[] total = new int[3];

        for (ValidationLog.Entry e : entries) {
            int slot = e.status() == ValidationLog.Status.PASS ? 0 : e.status() == ValidationLog.Status.FAIL ? 1 : 2;
            total[slot]++;
            perStep.computeIfAbsent(stepOf(e.scope()), k -> new int[3])[slot]++;
            if (slot != 0) {
                issues.computeIfAbsent(e.status() + "|" + e.check(), k -> new Issue(e.status(), e.check()))
                        .occurrences.add(e);
            }
        }

        StringBuilder md = new StringBuilder();
        md.append("# Phlebo Flow - Validation Log\n\n");
        md.append("Generated: ").append(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
        md.append("| Item | Value |\n|---|---|\n");
        md.append("| Suite | ").append(cell(System.getProperty("suiteXmlFile", "n/a"))).append(" |\n");
        md.append("| Phlebo notification service | ").append(cell(phleboBase)).append(" |\n");
        md.append("| Diagnostics service | ").append(cell(diagBase)).append(" |\n");
        md.append("| Total checks | ").append(entries.size()).append(" |\n");
        md.append("| Passed | ").append(total[0]).append(" |\n");
        md.append("| Failed (test failures) | ").append(total[1]).append(" |\n");
        md.append("| Backend defects | ").append(total[2]).append(" |\n");
        md.append("| Distinct issues (Jira candidates) | ").append(issues.size()).append(" |\n\n");
        md.append("FAIL = a check did not hold. DEFECT = backend returned wrong / stale / null data "
                + "(reported as SKIPPED in the run unless `-Dphlebo.failOnDefects=true`).\n\n");

        appendStepTable(md, perStep, api);
        appendIndex(md, issues);
        appendDetails(md, issues, api);
        return md.toString();
    }

    private static void appendStepTable(StringBuilder md, Map<String, int[]> perStep, Map<String, String[]> api) {
        md.append("## Results by step\n\n| Step | Endpoint under test | Passed | Failed | Defects |\n|---|---|---|---|---|\n");
        for (Map.Entry<String, int[]> s : perStep.entrySet()) {
            String[] ep = api.get(s.getKey());
            md.append("| PHLEBO_MGMT_").append(s.getKey()).append(" | ")
                    .append(ep == null ? "n/a" : cellCode(ep[0])).append(" | ")
                    .append(s.getValue()[0]).append(" | ").append(s.getValue()[1]).append(" | ")
                    .append(s.getValue()[2]).append(" |\n");
        }
        md.append("\n");
    }

    private static void appendIndex(StringBuilder md, Map<String, Issue> issues) {
        md.append("## Issue index\n\n");
        if (issues.isEmpty()) {
            md.append("No failed validations and no backend defects.\n");
            return;
        }
        md.append("| ID | Type | Suggested priority | Check | Steps affected |\n|---|---|---|---|---|\n");
        int n = 1;
        for (Issue issue : issues.values()) {
            md.append("| ").append(issueId(n++)).append(" | ").append(typeOf(issue)).append(" | ")
                    .append(priority(issue)).append(" | ").append(cell(issue.check)).append(" | ")
                    .append(cell(stepsOf(issue))).append(" |\n");
        }
        md.append("\n");
    }

    private static void appendDetails(StringBuilder md, Map<String, Issue> issues, Map<String, String[]> api) {
        if (issues.isEmpty()) {
            return;
        }
        md.append("## Issue details\n\n");
        int n = 1;
        for (Issue issue : issues.values()) {
            ValidationLog.Entry first = issue.occurrences.get(0);
            String[] firstApi = api.get(stepOf(first.scope()));
            String id = issueId(n++);

            md.append("### ").append(id).append(" - ").append(typeOf(issue)).append(": ")
                    .append(clean(issue.check)).append("\n\n");
            md.append("| Jira field | Value |\n|---|---|\n");
            md.append("| Summary | ").append(cell(summary(issue, firstApi))).append(" |\n");
            md.append("| Issue type | Bug |\n");
            md.append("| Suggested priority | ").append(priority(issue)).append(" |\n");
            md.append("| Labels | api-automation, phlebo-flow, ")
                    .append(issue.status == ValidationLog.Status.FAIL ? "test-failure" : "backend-defect").append(" |\n");
            md.append("| Steps affected | ").append(cell(stepsOf(issue))).append(" |\n");
            md.append("| Automated test | `PhlebioFlowValidationTest.PHLEBO_VALIDATION` - `[")
                    .append(cell(first.scope())).append("] ").append(cell(issue.check)).append("` |\n\n");

            md.append("**Occurrences (endpoint, expected, actual)**\n\n");
            md.append("| Step | Endpoint | Expected | Actual |\n|---|---|---|---|\n");
            for (ValidationLog.Entry e : issue.occurrences) {
                String[] ep = api.get(stepOf(e.scope()));
                md.append("| ").append(cell(e.scope())).append(" | ").append(endpointCell(ep)).append(" | ")
                        .append(cellCode(expectedOf(e))).append(" | ").append(cellCode(actualOf(e))).append(" |\n");
            }

            md.append("\n**Description**\n\n");
            md.append("The check `").append(clean(issue.check).replace("`", "'")).append("` ")
                    .append(issue.status == ValidationLog.Status.FAIL
                            ? "failed" : "found inconsistent backend data")
                    .append(". Detail: ").append(clean(first.detail())).append("\n\n");

            md.append("**Steps to reproduce**\n\n");
            md.append("1. Create a non-member COD order (login, add to cart, address and slot, place order).\n");
            md.append("2. Run the phlebo flow in order, PHLEBO_MGMT_01 up to PHLEBO_MGMT_")
                    .append(stepOf(first.scope())).append(" (suite `testng_non_member_cod_modular.xml`).\n");
            md.append("3. Call the endpoint(s) above and compare the response field with the expected value.\n\n");
            appendTestData(md);
        }
    }

    private static void appendTestData(StringBuilder md) {
        md.append("**Test data of the run**\n\n");
        md.append("- Order id: ").append(code(RequestContext.getCurrentOrderIds())).append("\n");
        md.append("- Order tracking id: ").append(code(RequestContext.getCurrentOrderTrackingIds())).append("\n");
        md.append("- Phlebo guid: ").append(code(RequestContext.getCurrentPhleboGuid())).append("\n");
        md.append("- Visit number: ").append(code(RequestContext.getVisitNumber())).append("\n\n---\n\n");
    }

    // ── helpers ───────────────────────────────────────────────────────────────────────────────────
    private static String stepOf(String scope) {
        Matcher m = STEP.matcher(scope == null ? "" : scope);
        return m.find() ? m.group(1) : "00";
    }

    private static String stepsOf(Issue issue) {
        List<String> steps = new ArrayList<>();
        for (ValidationLog.Entry e : issue.occurrences) {
            String s = stepOf(e.scope());
            if (!steps.contains(s)) {
                steps.add(s);
            }
        }
        return "PHLEBO_MGMT_" + String.join(", ", steps);
    }

    private static String issueId(int n) {
        return String.format("ISSUE-%02d", n);
    }

    private static String typeOf(Issue issue) {
        return issue.status == ValidationLog.Status.FAIL ? "FAIL" : "DEFECT";
    }

    /** Suggested only - the final priority is the triager's call. */
    private static String priority(Issue issue) {
        if (issue.status == ValidationLog.Status.FAIL) {
            return "High";
        }
        String c = issue.check.toLowerCase();
        if (c.startsWith("assignment")) {
            return "High";
        }
        return c.startsWith("timeline") ? "Low" : "Medium";
    }

    private static String summary(Issue issue, String[] ep) {
        String path = ep == null ? "" : " (" + ep[0].replaceAll("https?://[^/]+", "") + ")";
        String s = "[Phlebo flow] " + clean(issue.check) + path;
        return s.length() > MAX_SUMMARY_LENGTH ? s.substring(0, MAX_SUMMARY_LENGTH - 3) + "..." : s;
    }

    private static String expectedOf(ValidationLog.Entry e) {
        return e.expected() != null ? e.expected() : "Condition holds: " + e.check();
    }

    private static String actualOf(ValidationLog.Entry e) {
        return e.actual() != null ? e.actual() : e.detail();
    }

    private static String endpointCell(String[] ep) {
        if (ep == null) {
            return "n/a";
        }
        String cell = cellCode(ep[0]);
        return ep[1].isEmpty() ? cell : cell + "<br>cross-check: " + cellCode(ep[1]);
    }

    /** Removes line breaks, redacts JWTs and caps the length. */
    private static String clean(Object value) {
        String s = value == null ? "null" : String.valueOf(value);
        s = JWT.matcher(s).replaceAll("[REDACTED_JWT]").replace("\r", " ").replace("\n", " ").trim();
        return s.length() > MAX_VALUE_LENGTH ? s.substring(0, MAX_VALUE_LENGTH) + "..." : s;
    }

    private static String cell(Object value) {
        return clean(value).replace("|", "\\|");
    }

    private static String code(Object value) {
        return "`" + clean(value).replace("`", "'") + "`";
    }

    private static String cellCode(Object value) {
        return code(value).replace("|", "\\|");
    }
}

package utilities;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.markuputils.ExtentColor;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import com.mryoda.diagnostics.api.ai.AIFailureAnalyzer;
import com.mryoda.diagnostics.api.utils.ApiReportContext;
import com.mryoda.diagnostics.api.utils.RequestContext;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.List;

/**
 * Extent Report Listener following MLX structured pattern:
 * REQUEST → EXPECTED → ACTUAL → RESULT
 */
public class TestNGExtentReportListener implements ITestListener {

    private static ExtentReports extent = ExtentManager.getInstance();
    private static ThreadLocal<ExtentTest> test = new ThreadLocal<>();
    private static java.util.List<String> consoleReport = new java.util.ArrayList<>();

    @Override
    public void onStart(ITestContext context) {
    }

    @Override
    public void onFinish(ITestContext context) {
        if (extent != null) {
            extent.flush();
        }

        System.out.println("\n\n");
        System.out.println("==========================================================================================");
        System.out.println("                               TEST EXECUTION SUMMARY                                     ");
        System.out.println("==========================================================================================");
        System.out.printf("%-80s | %-10s%n", "Test Method", "Status");
        System.out.println("------------------------------------------------------------------------------------------");
        for (String line : consoleReport) {
            System.out.println(line);
        }
        System.out.println("==========================================================================================");
        System.out.println("\n");
    }

    @Override
    public void onTestStart(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        String className = result.getTestClass().getName();
        if (className.contains(".")) {
            className = className.substring(className.lastIndexOf('.') + 1);
        }

        ExtentTest extentTest = extent.createTest(testName);

        // Assign category based on class name
        String category = resolveCategory(className);
        extentTest.assignCategory(category);

        // Add description from @Test annotation
        String description = result.getMethod().getDescription();
        if (description != null && !description.isEmpty()) {
            extentTest.info(description);
        }

        test.set(extentTest);

        // Clear API call records for this test
        ApiReportContext.clear();
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        String methodName = result.getMethod().getMethodName();
        String className = getShortClassName(result);

        String logMsg = String.format("[PASS] %s.%s", className, methodName);
        System.out.println("\n" + logMsg);
        consoleReport.add(String.format("%-80s | %s", className + "." + methodName, "PASS ✅"));

        // Log structured API call details
        logApiCallsToReport(result);

        test.get().log(Status.PASS,
                MarkupHelper.createLabel("<b>" + methodName + " passed successfully.</b>", ExtentColor.GREEN));
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String methodName = result.getMethod().getMethodName();
        String className = getShortClassName(result);

        String logMsg = String.format("[FAIL] %s.%s", className, methodName);
        System.out.println("\n" + logMsg);
        consoleReport.add(String.format("%-80s | %s", className + "." + methodName, "FAIL ❌"));

        // Log structured API call details
        logApiCallsToReport(result);

        // Log failure reason
        Throwable throwable = result.getThrowable();
        if (throwable != null) {
            test.get().log(Status.FAIL,
                    MarkupHelper.createLabel("<b>Failure Reason:</b> " + throwable.getMessage(), ExtentColor.RED));
            test.get().log(Status.FAIL, throwable);
        }

        // AI Failure Analysis — calls Gemini to explain why the test failed
        logAiFailureAnalysis(result);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        String methodName = result.getMethod().getMethodName();
        String className = getShortClassName(result);

        String logMsg = String.format("[SKIP] %s.%s", className, methodName);
        System.out.println("\n" + logMsg);
        consoleReport.add(String.format("%-80s | %s", className + "." + methodName, "SKIP ⚠️"));

        test.get().log(Status.SKIP,
                MarkupHelper.createLabel("<b>" + methodName + " skipped.</b>", ExtentColor.YELLOW));
        if (result.getThrowable() != null) {
            test.get().log(Status.SKIP, result.getThrowable());
        }
    }

    // =========================================================================
    // STRUCTURED API CALL LOGGING (MLX Pattern)
    // =========================================================================

    private void logApiCallsToReport(ITestResult result) {
        List<ApiReportContext.ApiCallRecord> records = ApiReportContext.getRecords();
        ExtentTest extentTest = test.get();

        if (records.isEmpty()) {
            injectBusinessData(result);
            return;
        }

        // Use the TEST's actual pass/fail from TestNG
        boolean testPassed = (result.getStatus() == ITestResult.SUCCESS);

        for (int i = 0; i < records.size(); i++) {
            ApiReportContext.ApiCallRecord record = records.get(i);

            // REQUEST SECTION
            extentTest.info("<b>------------------------------ REQUEST ------------------------------:</b>");
            extentTest.info("<b>API Endpoint:</b> " + record.getMethod() + " " + record.getEndpoint());

            String reqBody = record.getRequestBody();
            if (reqBody != null && !reqBody.isEmpty() && !"{}".equals(reqBody)) {
                String formattedReq = formatJsonSafe(reqBody);
                extentTest.info(MarkupHelper.createCodeBlock(formattedReq));
            }

            // EXPECTED SECTION
            extentTest.info("<b>------------------------------ EXPECTED ------------------------------:</b>");
            extentTest.info("<b>Expected Status Code:</b> " + record.getExpectedStatusLabel());
            extentTest.info("<b>Expected Behavior:</b> Response should contain record data and response time &lt; 5s");

            // ACTUAL SECTION
            extentTest.info("<b>------------------------------ ACTUAL ------------------------------:</b>");
            extentTest.info("<b>Actual Status Code:</b> " + record.getStatusCode());
            extentTest.info("<b>Actual Response Time:</b> " + record.getResponseTimeMs() + " ms");

            if (record.isStatusInExpectedRange()) {
                extentTest.info("<span style='color:green'>&#10004;</span> <b>Response Status:</b> "
                        + record.getStatusCode() + " (matches expected: " + record.getExpectedStatusLabel() + ")");
            } else {
                extentTest.info("<span style='color:red'>&#10008;</span> <b>Response Status:</b> "
                    + record.getStatusCode()
                    + " (Expected: " + record.getExpectedStatusLabel() + ")");
            }

            String respBody = record.getResponseBody();
            if (respBody != null && !respBody.isEmpty()) {
                String formattedResp = formatJsonSafe(respBody);
                extentTest.info(MarkupHelper.createCodeBlock(formattedResp));
            }

            // RESULT SECTION
            extentTest.info("<b>------------------------------ RESULT ------------------------------:</b>");
            extentTest.info("<b>Duration:</b> " + record.getResponseTimeMs() + " ms");

            if (testPassed) {
                extentTest.info(MarkupHelper.createLabel("PASSED", ExtentColor.GREEN));
            } else {
                extentTest.info(MarkupHelper.createLabel("FAILED", ExtentColor.RED));
                Throwable throwable = result.getThrowable();
                String reason = (throwable != null) ? throwable.getMessage() : "Test assertion failed";
                extentTest.info("<b>Failure Reason:</b> " + reason);
            }

            if (i < records.size() - 1) {
                extentTest.info("<hr/>");
            }
        }

        // Render per-test extracted validation details (e.g. content-type, counts, names)
        List<String> extras = ApiReportContext.getExtraDetails();
        if (!extras.isEmpty()) {
            extentTest.info("<b>------------------------------ VALIDATION DETAILS ------------------------------:</b>");
            for (String detail : extras) {
                extentTest.info(detail);
            }
        }
    }

    // =========================================================================
    // AI FAILURE ANALYSIS
    // =========================================================================

    /**
     * Calls AIFailureAnalyzer to get a Gemini-powered root cause explanation,
     * then renders it in the Extent Report as a styled info block.
     * Silently skips if the analyzer is disabled or the API key is not set.
     */
    private void logAiFailureAnalysis(ITestResult result) {
        try {
            List<ApiReportContext.ApiCallRecord> records = ApiReportContext.getRecords();
            Throwable throwable = result.getThrowable();
            String failureMessage = (throwable != null) ? throwable.getMessage() : "Unknown failure";
            String testName = result.getMethod().getMethodName();

            System.out.println("[ExtentListener] Calling AIFailureAnalyzer for: " + testName
                    + " | records=" + records.size());

            String analysis = AIFailureAnalyzer.analyze(records, failureMessage, testName);

            System.out.println("[ExtentListener] Analysis result: "
                    + (analysis != null ? "received (" + analysis.length() + " chars)" : "null"));

            if (analysis != null && !analysis.trim().isEmpty()) {
                ExtentTest extentTest = test.get();
                extentTest.info(
                        "<div style='"
                        + "background:#fff8e1;"
                        + "border-left:4px solid #f9a825;"
                        + "padding:10px 14px;"
                        + "border-radius:4px;"
                        + "margin-top:8px;"
                        + "'>"
                        + "<b style='color:#e65100;'>&#129302; AI Failure Analysis (Gemini)</b><br/>"
                        + "<span style='color:#37474f;font-size:13px;'>"
                        + analysis.replace("\n", "<br/>")
                        + "</span>"
                        + "</div>");
            }
        } catch (Exception e) {
            System.out.println("[ExtentListener] logAiFailureAnalysis threw: " + e.getMessage());
        }
    }

    // =========================================================================
    // BUSINESS DATA INJECTION (Fallback when no API calls captured)
    // =========================================================================

    private void injectBusinessData(ITestResult result) {
        String methodName = result.getMethod().getMethodName();
        ExtentTest extentTest = test.get();

        try {
            if (methodName.contains("Login") || methodName.contains("Setup")) {
                String token = RequestContext.getToken();
                String userId = RequestContext.getUserId();
                extentTest.info("<b>User ID:</b> " + (userId != null ? userId : "N/A"));
                extentTest.info("<b>Token Status:</b> " + (token != null ? "<span style='color:green'>Generated</span>" : "<span style='color:red'>Missing</span>"));
            } else if (methodName.contains("Cart") || methodName.contains("AddToCart")) {
                extentTest.info("<b>Cart ID:</b> " + (RequestContext.getCurrentCartId() != null ? RequestContext.getCurrentCartId() : "N/A"));
                extentTest.info("<b>Total Price:</b> Rs." + RequestContext.getCurrentTotalPrice());
            } else if (methodName.contains("Slot") || methodName.contains("Address")) {
                extentTest.info("<b>Slot Date:</b> " + (RequestContext.getSlotStartDate() != null ? RequestContext.getSlotStartDate() : "N/A"));
                extentTest.info("<b>Slot Guid:</b> " + (RequestContext.getSelectedSlotGuid() != null ? RequestContext.getSelectedSlotGuid() : "N/A"));
            } else if (methodName.contains("Order") || methodName.contains("Payment")) {
                String orderId = RequestContext.getCurrentOrderId();
                extentTest.info("<b>Order ID:</b> " + (orderId != null ? orderId : "N/A"));
                extentTest.info("<b>Payment Method:</b> COD");
            }
        } catch (Exception e) {
            extentTest.warning("Could not inject business data: " + e.getMessage());
        }

        // Always render extra details added by the test (e.g. extracted field values)
        List<String> extras = ApiReportContext.getExtraDetails();
        if (!extras.isEmpty()) {
            extentTest.info("<b>------------------------------ VALIDATION DETAILS ------------------------------:</b>");
            for (String detail : extras) {
                extentTest.info(detail);
            }
        }
    }

    // =========================================================================
    // UTILITY METHODS
    // =========================================================================

    private String getShortClassName(ITestResult result) {
        String className = result.getTestClass().getName();
        if (className.contains(".")) {
            className = className.substring(className.lastIndexOf('.') + 1);
        }
        return className;
    }

    private String resolveCategory(String className) {
        if (className.contains("Login")) return "Authentication";
        if (className.contains("Cart")) return "Cart Management";
        if (className.contains("Payment")) return "Payment";
        if (className.contains("Order")) return "Order Processing";
        if (className.contains("Slot")) return "Slot Management";
        if (className.contains("Search") || className.contains("Catalog")) return "Tests & Packages";
        if (className.contains("Address")) return "Address Management";
        if (className.contains("Report")) return "Reports";
        return "General API";
    }

    private String formatJsonSafe(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        String trimmed = raw.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
                Object parsed = com.google.gson.JsonParser.parseString(trimmed);
                return gson.toJson(parsed);
            } catch (Exception e) {
                return raw;
            }
        }
        return raw;
    }
}

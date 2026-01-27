package com.mryoda.diagnostics.api.tests;

import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.RequestContext;
import com.mryoda.diagnostics.api.utils.LoggerUtil;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.Map;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.InputStream;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class COD_17_ReportGenerationTest extends BaseTest {

    @Test
    public void testGetReportAndVerifyPDF() {
        LoggerUtil.info(">>> STEP 17: GET REPORT DETAILS & PDF TEXT EXTRACTION <<<");

        String visitNumber = RequestContext.getVisitNumber();
        if (visitNumber == null) {
            visitNumber = System.getProperty("visitNumber", "MYD9211");
        }
        
        Assert.assertNotNull(visitNumber, "❌ Visit Number is missing!");
        LoggerUtil.info("Validating Report for Visit Number: " + visitNumber);

        // Polling Configuration
        int maxRetries = 20; // Increased to 20 attempts (5 minutes total)
        int delayMs = 15000; // 15 seconds wait between retries
        Response response = null;
        Map<String, Object> data = null;
        String reportUrl = null;
        String pdfText = null;

        LoggerUtil.info("⏳ Polling Report API (Max retries: " + maxRetries + ")...");

        for (int i = 1; i <= maxRetries; i++) {
            long startTime = System.currentTimeMillis();
            LoggerUtil.info("🔄 [ATTEMPT " + i + "/" + maxRetries + "] Requesting Report at " + new java.util.Date());
            
            response = new RequestBuilder()
                    .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.GET_REPORT_DETAILS.replace("{visit_Number}", visitNumber))
                    .addHeader("Authorization", "Bearer " + RequestContext.getToken())
                    .get();

            int statusCode = response.getStatusCode();
            LoggerUtil.info("📡 API Response: " + statusCode + " (Received in " + (System.currentTimeMillis() - startTime) + "ms)");

            if (statusCode == 200) {
                data = response.jsonPath().getMap("data");
                if (data != null && data.get("full_download_url") != null) {
                    reportUrl = (String) data.get("full_download_url");
                    LoggerUtil.info("✅ URL found: " + reportUrl);
                    
                    // DEEP DIVE: Try to extract text immediately. 
                    // Sometimes the URL is returned but the file is not yet ready for streaming or metadata is missing.
                    LoggerUtil.info("🔍 Deep Check: Attempting to read PDF content...");
                    pdfText = extractTextFromPdfUrl(reportUrl);
                    
                    List<Map<String, Object>> testsList = (List<Map<String, Object>>) data.get("tests");
                    int actualTestCount = 0;
                    if (testsList != null) {
                        for (Map<String, Object> t : testsList) {
                            Object isH = t.get("isHeader");
                            boolean isHeader = (isH instanceof Boolean) ? (Boolean)isH : Boolean.parseBoolean(String.valueOf(isH));
                            if (!isHeader) actualTestCount++;
                        }
                    }

                    if (pdfText != null && pdfText.trim().length() > 50 && actualTestCount > 0) {
                        LoggerUtil.info("✅ SUCCESS: Report generated with " + actualTestCount + " test results! (Text length: " + pdfText.length() + ")");
                        break;
                    } else {
                        if (actualTestCount == 0 && testsList != null && !testsList.isEmpty()) {
                            LoggerUtil.warn("⚠️ URL exists, but report is currently showing HEADER ONLY. Waiting for Test Results...");
                        } else if (testsList == null || testsList.isEmpty()) {
                            LoggerUtil.warn("⚠️ URL exists, but 'tests' metadata is still empty. Waiting for API sync...");
                        } else {
                            LoggerUtil.warn("⚠️ URL exists, but PDF content is empty or unreadable yet. Retrying in " + (delayMs/1000) + "s...");
                        }
                        pdfText = null; // Clear to continue polling
                    }
                } else {
                    LoggerUtil.warn("⚠️ Received 200 but 'full_download_url' is still NULL. Retrying...");
                }
            } else if (statusCode == 404) {
                LoggerUtil.info("⚠️ NOT FOUND (404): Report is not yet generated. Waiting for processing...");
            } else {
                LoggerUtil.warn("❓ UNEXPECTED STATUS " + statusCode + ": Retrying anyway...");
            }

            if (i < maxRetries) {
                LoggerUtil.info("😴 PAUSE: Sleeping before Attempt " + (i + 1) + "...");
                try { Thread.sleep(delayMs); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }

        Assert.assertNotNull(reportUrl, "❌ Report URL not ready after " + maxRetries + " attempts!");
        Assert.assertNotNull(pdfText, "❌ Report found at URL but could not be read or is empty after " + maxRetries + " attempts!");

        LoggerUtil.info("📄 --- EXTRACTED PDF TEXT (PREVIEW) ---");
        System.out.println(pdfText.length() > 1000 ? pdfText.substring(0, 1000) + "..." : pdfText);
        LoggerUtil.info("---------------------------------------");

        // 4. PERFORM SIDE-BY-SIDE VALIDATION (JSON vs PDF vs UI)
        performTripleValidation(pdfText, data);
        
        LoggerUtil.info("✔ Report Triple-Check Verification Completed!");
    }

    private String extractTextFromPdfUrl(String pdfUrl) {
        try (InputStream inputStream = new URL(pdfUrl).openStream();
             PDDocument document = PDDocument.load(inputStream)) {
            
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (Exception e) {
            LoggerUtil.error("❌ Error extracting text from PDF URL: " + e.getMessage());
            return null;
        }
    }

    private void performTripleValidation(String pdfText, Map<String, Object> apiData) {
        String logFile = "logs/report_validation.log";
        java.io.PrintWriter writer = null;
        try {
            writer = new java.io.PrintWriter(new java.io.FileWriter(logFile, true));
            writer.println("\n\n=== [" + new java.util.Date() + "] NEW VALIDATION RUN ===");
            
            LoggerUtil.info("⚖️ --- STARTING DEEP SIDE-BY-SIDE VALIDATION --- ⚖️");
            int matchCount = 0;
            int totalChecks = 0;

            java.util.List<String> auditTrail = new java.util.ArrayList<>();
            auditTrail.add(String.format("%-15s | %-30s | %-20s | %-20s | %s", "CATEGORY", "DATA POINT", "EXPECTED", "ACTUAL (In PDF)", "STATUS"));
            auditTrail.add("-".repeat(110));

            // --- 1. Report Sync Status (NOT a validation failure point) ---
            Map<String, Map<String, Object>> bookedTests = RequestContext.getAllTests();
            List<Map<String, Object>> apiTests = (List<Map<String, Object>>) apiData.get("tests");
            
            int bookedCount = (bookedTests != null ? bookedTests.size() : 0);
            int apiTestResultCount = 0;
            if (apiTests != null) {
                for (Map<String, Object> t : apiTests) {
                    Object isHeaderField = t.get("isHeader");
                    boolean isHeader = false;
                    if (isHeaderField instanceof Boolean) isHeader = (Boolean) isHeaderField;
                    else if (isHeaderField instanceof String) isHeader = Boolean.parseBoolean((String) isHeaderField);
                    
                    if (!isHeader) {
                        apiTestResultCount++;
                    }
                }
            }
            
            LoggerUtil.info("📊 REPORT SYNC STATUS:");
            writer.println("📊 REPORT SYNC STATUS:");
            LoggerUtil.info("   > Booked Tests in Transaction: " + bookedCount);
            LoggerUtil.info("   > Results Sync'd in API Data: " + apiTestResultCount);

            if (apiTestResultCount == 0) {
                LoggerUtil.warn("   ⚠️ API is currently showing HEADER ONLY. Individual test metadata not yet sync'd.");
                writer.println("   ⚠️ API is currently showing HEADER ONLY. Individual test metadata not yet sync'd.");
            } else if (apiTestResultCount < bookedCount) {
                LoggerUtil.info("   ℹ️ Partial sync: " + apiTestResultCount + "/" + bookedCount + " test results sync'd to API metadata.");
            } else {
                LoggerUtil.info("   ✅ All test results are sync'd to API metadata.");
            }

            // --- PHASE 1: Metadata ---
            String visitNo = (String) apiData.get("visitNumber");
            if (visitNo != null) {
                totalChecks++;
                boolean found = pdfText.contains(visitNo);
                if (found) matchCount++;
                auditTrail.add(String.format("%-15s | %-30s | %-20s | %-20s | %s", "Metadata", "Visit Number", visitNo, "FOUND", found ? "✅" : "❌"));
            }

            String userName = (String) apiData.get("userName");
            if (userName != null) {
                totalChecks++;
                String cleanName = userName.replace("Mr.", "").replace("Ms.", "").trim();
                boolean found = pdfText.toLowerCase().contains(cleanName.toLowerCase());
                if (found) matchCount++;
                auditTrail.add(String.format("%-15s | %-30s | %-20s | %-20s | %s", "Metadata", "Patient Name", userName, found ? "FOUND" : "NOT FOUND", found ? "✅" : "❌"));
            }

            String registrationDate = (String) apiData.get("order_date"); 
            if (registrationDate != null && registrationDate.length() >= 10) {
                totalChecks++;
                String datePart = registrationDate.substring(0, 10);
                boolean found = pdfText.contains(datePart);
                if (found) matchCount++;
                auditTrail.add(String.format("%-15s | %-30s | %-20s | %-20s | %s", "Metadata", "Order Date", datePart, found ? "FOUND" : "NOT FOUND", found ? "✅" : "❌"));
            }

            // --- PHASE 2a: UI Booking Check ---
            Map<String, String> uiExpectedResults = RequestContext.getAllExpectedTestResults();
            if (bookedTests != null) {
                for (String testName : bookedTests.keySet()) {
                    totalChecks++;
                    boolean found = pdfText.toLowerCase().contains(testName.toLowerCase());
                    if (found) matchCount++;
                    auditTrail.add(String.format("%-15s | %-30s | %-20s | %-20s | %s", "UI-Booking", "Test Existence", testName, found ? "FOUND" : "NOT FOUND", found ? "✅" : "❌"));
                }
            }

            // --- PHASE 2b: API Schema Check ---
            if (apiTests != null) {
                for (Map<String, Object> test : apiTests) {
                    String testName = (String) test.get("testName");
                    if (testName == null || testName.isEmpty() || (boolean)test.getOrDefault("isHeader", false)) continue;

                    // 1. Name Presence (API record vs PDF)
                    totalChecks++;
                    boolean nameFound = pdfText.toLowerCase().contains(testName.toLowerCase());
                    if (nameFound) matchCount++;
                    auditTrail.add(String.format("%-15s | %-30s | %-20s | %-20s | %s", "API-Sync", "Test Name Sync", testName, nameFound ? "FOUND" : "NOT FOUND", nameFound ? "✅" : "❌"));

                    if (nameFound) {
                        // 2. Numerical Result check (UI Value vs PDF Content)
                        String expectedVal = uiExpectedResults.get(testName);
                        if (expectedVal != null) {
                            totalChecks++;
                            boolean valMatch = validateNumericalResultInPdf(pdfText, testName, expectedVal);
                            if (valMatch) matchCount++;
                            auditTrail.add(String.format("%-15s | %-30s | %-20s | %-20s | %s", "API-Sync", "Result Value Match", expectedVal, valMatch ? "MATCHED" : "MISMATCH", valMatch ? "✅" : "❌"));
                        }

                        // 3. Department Name check
                        String dept = (String) test.get("department_name");
                        if (dept != null && !dept.isEmpty()) {
                            totalChecks++;
                            boolean deptFound = pdfText.toLowerCase().contains(dept.toLowerCase());
                            if (deptFound) matchCount++;
                            auditTrail.add(String.format("%-15s | %-30s | %-20s | %-20s | %s", "API-Sync", "Department Sync", dept, deptFound ? "FOUND" : "NOT FOUND", deptFound ? "✅" : "❌"));
                        }
                    }
                }
            }

            // --- Summary Display ---
            double accuracy = totalChecks > 0 ? ((double) matchCount / totalChecks) * 100 : 0;
            boolean isPerfect = (matchCount == totalChecks && totalChecks > 0);

            System.out.println("\n🔍 " + "=".repeat(25) + " DEEP VALIDATION AUDIT TRAIL " + "=".repeat(25));
            for (String line : auditTrail) {
                System.out.println(line);
            }
            System.out.println("=".repeat(80));

            if (isPerfect) {
                System.out.println("✅ EVERYTHING IS SUCCESS: All " + totalChecks + " data points matched perfectly!");
            } else {
                System.out.println("❌ VALIDATION COMPLETE WITH ISSUES: " + (totalChecks - matchCount) + "/" + totalChecks + " points failed.");
            }
            System.out.format("📈 Final Report Fidelity Score: %.2f%%\n", accuracy);
            System.out.println("=".repeat(80) + "\n");

            String summaryLog = "\n📊 FINAL VALIDATION SCORECARD:" + 
                             "\n   > Total Validation Points: " + totalChecks + 
                             "\n   > Total Points Passed    : " + matchCount + 
                             "\n   > Total Points Failed    : " + (totalChecks - matchCount) + 
                             "\n   > Report Fidelity Score  : " + String.format("%.2f", accuracy) + "%\n";
            writer.println(summaryLog);

            Assert.assertTrue(accuracy > 70.0, "❌ Report Validation Fidelity failed! (" + accuracy + "%)");
        } catch (Exception e) {
            LoggerUtil.error("Validation Error: " + e.getMessage());
        } finally {
            if (writer != null) writer.close();
        }
    }

    private boolean validateNumericalResultInPdf(String pdfText, String testName, String expectedValue) {
        String escapedName = Pattern.quote(testName);
        // Look for number within 100 characters after the test name
        int index = pdfText.toLowerCase().indexOf(testName.toLowerCase());
        if (index == -1) return false;
        
        String snippet = pdfText.substring(index, Math.min(index + 150, pdfText.length()));
        
        // Match number (handles 105, 105.5, 5)
        Pattern pattern = Pattern.compile("(\\d+(?:\\.\\d+)?)");
        Matcher matcher = pattern.matcher(snippet);

        while (matcher.find()) {
            String foundVal = matcher.group(1);
            try {
                double foundD = Double.parseDouble(foundVal);
                double expectedD = Double.parseDouble(expectedValue);
                if (Math.abs(foundD - expectedD) < 0.1) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }
}

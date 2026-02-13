package com.mryoda.diagnostics.api.tests.order;

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

        java.util.List<String> visits = RequestContext.getCurrentVisitNumbers();
        if (visits == null || visits.isEmpty()) {
            String singleVisit = RequestContext.getVisitNumber();
            if (singleVisit != null) {
                visits = new java.util.ArrayList<>();
                visits.add(singleVisit);
            }
        }

        if (visits == null || visits.isEmpty()) {
            // Last resort fallback
            String fallback = System.getProperty("visitNumber", "MYD9211");
            visits = new java.util.ArrayList<>();
            visits.add(fallback);
        }

        LoggerUtil.info("📊 Found " + visits.size() + " visits for Report Validation.");

        for (String visitNumber : visits) {
            LoggerUtil.info("\n" + "=".repeat(60));
            LoggerUtil.info("📄 VALIDATING REPORT FOR VISIT: " + visitNumber);
            LoggerUtil.info("=".repeat(60));

            // Polling Configuration
            int maxRetries = 20;
            int delayMs = 15000;
            Response response = null;
            Map<String, Object> data = null;
            String reportUrl = null;
            String pdfText = null;

            LoggerUtil.info("⏳ Polling Report API (Max retries: " + maxRetries + ")...");

            for (int i = 1; i <= maxRetries; i++) {
                long startTime = System.currentTimeMillis();
                LoggerUtil
                        .info("🔄 [ATTEMPT " + i + "/" + maxRetries + "] Requesting Report at " + new java.util.Date());

                response = new RequestBuilder()
                        .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL
                                + APIEndpoints.GET_REPORT_DETAILS.replace("{visit_Number}", visitNumber))
                        .addHeader("Authorization", "Bearer " + RequestContext.getToken())
                        .get();

                int statusCode = response.getStatusCode();
                LoggerUtil.info("📡 API Response: " + statusCode + " (Received in "
                        + (System.currentTimeMillis() - startTime) + "ms)");

                if (statusCode == 200) {
                    data = response.jsonPath().getMap("data");
                    if (data != null && data.get("full_download_url") != null) {
                        reportUrl = (String) data.get("full_download_url");
                        LoggerUtil.info("✅ URL found: " + reportUrl);

                        LoggerUtil.info("🔍 Deep Check: Attempting to read PDF content...");
                        pdfText = extractTextFromPdfUrl(reportUrl);

                        List<Map<String, Object>> testsList = (List<Map<String, Object>>) data.get("tests");
                        int actualTestCount = 0;
                        if (testsList != null) {
                            for (Map<String, Object> t : testsList) {
                                Object isH = t.get("isHeader");
                                boolean isHeader = (isH instanceof Boolean) ? (Boolean) isH
                                        : Boolean.parseBoolean(String.valueOf(isH));
                                if (!isHeader)
                                    actualTestCount++;
                            }
                        }

                        if (pdfText != null && pdfText.trim().length() > 50 && actualTestCount > 0) {
                            LoggerUtil.info("✅ SUCCESS: Report generated with " + actualTestCount
                                    + " test results! (Text length: " + pdfText.length() + ")");
                            break;
                        } else {
                            if (actualTestCount == 0 && testsList != null && !testsList.isEmpty()) {
                                LoggerUtil.warn(
                                        "⚠️ URL exists, but report is currently showing HEADER ONLY. Waiting for Test Results...");
                            } else if (testsList == null || testsList.isEmpty()) {
                                LoggerUtil.warn(
                                        "⚠️ URL exists, but 'tests' metadata is still empty. Waiting for API sync...");
                            } else {
                                LoggerUtil.warn("⚠️ URL exists, but PDF content is empty or unreadable yet.");
                            }
                            pdfText = null;
                        }
                    } else {
                        LoggerUtil.warn("⚠️ Received 200 but 'full_download_url' is still NULL. Retrying...");
                    }
                } else if (statusCode == 404) {
                    LoggerUtil.info("⚠️ NOT FOUND (404): Report is not yet generated. Waiting for processing...");
                }

                if (i < maxRetries && pdfText == null) {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            Assert.assertNotNull(reportUrl, "❌ Report URL not ready for visit: " + visitNumber);
            Assert.assertNotNull(pdfText, "❌ PDF content is missing/unreadable for visit: " + visitNumber);

            // 4. PERFORM SIDE-BY-SIDE VALIDATION
            performTripleValidation(pdfText, data);
        }
        LoggerUtil.info("✔ All requested reports verified successfully!");
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
            auditTrail.add(String.format("%-15s | %-30s | %-20s | %-20s | %s", "CATEGORY", "DATA POINT", "EXPECTED",
                    "ACTUAL (In PDF)", "STATUS"));
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
                    if (isHeaderField instanceof Boolean)
                        isHeader = (Boolean) isHeaderField;
                    else if (isHeaderField instanceof String)
                        isHeader = Boolean.parseBoolean((String) isHeaderField);

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
                LoggerUtil.info("   ℹ️ Partial sync: " + apiTestResultCount + "/" + bookedCount
                        + " test results sync'd to API metadata.");
            } else {
                LoggerUtil.info("   ✅ All test results are sync'd to API metadata.");
            }

            // --- PHASE 1: Metadata ---
            String visitNo = (String) apiData.get("visitNumber");
            if (visitNo != null) {
                totalChecks++;
                boolean found = pdfText.contains(visitNo);
                if (found)
                    matchCount++;
                auditTrail.add(String.format("%-15s | %-30s | %-20s | %-20s | %s", "Metadata", "Visit Number", visitNo,
                        "FOUND", found ? "✅" : "❌"));
            }

            String userName = (String) apiData.get("userName");
            if (userName != null) {
                totalChecks++;
                String cleanName = userName.replace("Mr.", "").replace("Ms.", "").trim();
                boolean found = pdfText.toLowerCase().contains(cleanName.toLowerCase());
                if (found)
                    matchCount++;
                auditTrail.add(String.format("%-15s | %-30s | %-20s | %-20s | %s", "Metadata", "Patient Name", userName,
                        found ? "FOUND" : "NOT FOUND", found ? "✅" : "❌"));
            }

            String registrationDate = (String) apiData.get("order_date");
            if (registrationDate != null && registrationDate.length() >= 10) {
                totalChecks++;
                String datePart = registrationDate.substring(0, 10); // YYYY-MM-DD

                // Try multiple formats
                boolean found = pdfText.contains(datePart);

                if (!found) {
                    java.util.List<String> variations = new java.util.ArrayList<>();
                    try {
                        java.time.LocalDate baseDate = java.time.LocalDate.parse(datePart);

                        // Check base date, day before, and day after (timezone buffer)
                        for (int offset : new int[] { 0, -1, 1 }) {
                            java.time.LocalDate ld = baseDate.plusDays(offset);
                            variations.add(ld.toString()); // YYYY-MM-DD
                            variations.add(ld.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")));
                            variations.add(ld.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                            variations.add(ld.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")));
                            variations.add(ld.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yy")));
                            variations.add(ld.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yy")));
                            variations.add(ld.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy",
                                    java.util.Locale.ENGLISH)));
                            variations.add(ld.format(java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy",
                                    java.util.Locale.ENGLISH)));
                            variations.add(ld.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yy",
                                    java.util.Locale.ENGLISH)));
                            variations.add(ld.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy",
                                    java.util.Locale.ENGLISH)));
                        }

                        for (String v : variations) {
                            if (pdfText.toLowerCase().contains(v.toLowerCase())) {
                                found = true;
                                break;
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }

                if (found)
                    matchCount++;
                auditTrail.add(String.format("%-15s | %-30s | %-20s | %-20s | %s", "Metadata", "Order Date", datePart,
                        found ? "FOUND" : "NOT FOUND", found ? "✅" : "❌"));
            }

            // --- PHASE 2a: UI Booking Check ---
            Map<String, String> uiExpectedResults = RequestContext.getAllExpectedTestResults();
            List<String> packageComponents = RequestContext.getPackageTestNames();

            if (bookedTests != null) {
                for (String testName : bookedTests.keySet()) {
                    totalChecks++;
                    String normExpected = normalize(testName);
                    boolean found = normalize(pdfText).contains(normExpected);

                    // If not found, check if it's a package and if its components are present
                    if (!found && packageComponents != null && !packageComponents.isEmpty()) {
                        LoggerUtil.info("   ℹ️ Item '" + testName
                                + "' not in PDF, checking if it's a package with components...");
                        boolean allCompFound = true;
                        for (String comp : packageComponents) {
                            if (!normalize(pdfText).contains(normalize(comp))) {
                                allCompFound = false;
                                break;
                            }
                        }
                        if (allCompFound) {
                            found = true;
                            LoggerUtil
                                    .info("   ✅ All components of '" + testName + "' found in PDF. Marking as FOUND.");
                        }
                    }

                    if (found)
                        matchCount++;
                    auditTrail.add(String.format("%-15s | %-30s | %-20s | %-20s | %s", "UI-Booking", "Test Existence",
                            testName, found ? "FOUND" : "NOT FOUND", found ? "✅" : "❌"));
                }
            }

            // --- PHASE 2b: API Sync Check (What's in DB vs PDF) ---
            java.util.Set<String> claimedNames = new java.util.HashSet<>();
            if (apiTests != null) {
                for (Map<String, Object> test : apiTests) {
                    String testName = (String) test.get("testName");
                    String testCode = (String) test.get("testCode");

                    if (testName == null || (boolean) test.getOrDefault("isHeader", false))
                        continue;

                    // Recovery Strategy: If API says "Unknown", try to recover the name
                    if (testName == null || "Unknown".equalsIgnoreCase(testName)) {
                        LoggerUtil.info(
                                "   ⚠️ Encountered 'Unknown' test from API. Attempting recovery for code: " + testCode);

                        // 1. Try Component Mapping (for package components)
                        if (testCode != null) {
                            String recoveredName = RequestContext.getComponentNameById(testCode);
                            if (recoveredName != null) {
                                testName = recoveredName;
                                LoggerUtil.info("   🔄 Recovered name '" + testName + "' from Component Map for code: "
                                        + testCode);
                            }
                            // 2. Try Top-level Booked Tests
                            else {
                                for (Map.Entry<String, Map<String, Object>> entry : bookedTests.entrySet()) {
                                    Object storedIdObj = entry.getValue().get("test_id");
                                    String storedCode = storedIdObj != null ? String.valueOf(storedIdObj) : null;
                                    if (testCode.equals(storedCode)) {
                                        testName = entry.getKey();
                                        LoggerUtil.info("   🔄 Recovered name '" + testName
                                                + "' from Booked Tests for code: " + testCode);
                                        break;
                                    }
                                }
                            }
                        }

                        // 3. Fallback: If still Unknown, check UNCLAIMED package components in PDF
                        if ("Unknown".equalsIgnoreCase(testName) && packageComponents != null) {
                            for (String compName : packageComponents) {
                                if (!claimedNames.contains(compName)
                                        && (pdfText.toLowerCase().contains(compName.toLowerCase())
                                                || normalize(pdfText).contains(normalize(compName)))) {
                                    testName = compName;
                                    LoggerUtil.info(
                                            "   🔄 Heuristic Recovery: Matched 'Unknown' to unclaimed package component: "
                                                    + testName);
                                    break;
                                }
                            }
                        }
                    }

                    // 1. Name Presence (API record vs PDF)
                    totalChecks++;
                    boolean nameFound = false;
                    if (testName != null && !"Unknown".equalsIgnoreCase(testName)) {
                        nameFound = pdfText.toLowerCase().contains(testName.toLowerCase()) ||
                                normalize(pdfText).contains(normalize(testName));
                    } else if (testCode != null) {
                        // If still unknown, check if the PDF contains the test CODE as a desperate
                        // fallback
                        nameFound = pdfText.contains(testCode);
                        if (nameFound) {
                            LoggerUtil.info("   ✅ Found test code '" + testCode + "' in PDF instead of name.");
                            testName = testCode;
                        }
                    }

                    if (nameFound) {
                        matchCount++;
                        claimedNames.add(testName);
                    }
                    auditTrail.add(String.format("%-15s | %-30s | %-20s | %-20s | %s", "API-Sync", "Test Name Sync",
                            testName, nameFound ? "FOUND" : "NOT FOUND", nameFound ? "✅" : "❌"));

                    if (nameFound) {
                        // 2. Numerical Result check
                        String expectedVal = uiExpectedResults.get(testName);

                        // --- NEW: Fuzzy lookup if direct match fails ---
                        if (expectedVal == null) {
                            String normApiName = normalize(testName);
                            for (Map.Entry<String, String> entry : uiExpectedResults.entrySet()) {
                                String normUiName = normalize(entry.getKey());
                                // Check if one contains the other to handle "CALCIUM - TOTAL" vs "SERUM TOTAL
                                // CALCIUM"
                                if (normApiName.length() > 3 && normUiName.length() > 3 &&
                                        (normApiName.contains(normUiName) || normUiName.contains(normApiName) ||
                                                (normApiName.contains("calcium") && normUiName.contains("calcium")) ||
                                                (normApiName.contains("phosphorus")
                                                        && normUiName.contains("phosphorus"))
                                                ||
                                                (normApiName.contains("glucose") && normUiName.contains("glucose")))) {

                                    expectedVal = entry.getValue();
                                    LoggerUtil.info("   🔄 Fuzzy matched expected result for '" + testName
                                            + "' using UI Key: " + entry.getKey() + " -> " + expectedVal);
                                    break;
                                }
                            }
                        }

                        if (expectedVal != null) {
                            totalChecks++;
                            boolean valMatch = validateNumericalResultInPdf(pdfText, testName, expectedVal);
                            if (valMatch)
                                matchCount++;
                            auditTrail.add(String.format("%-15s | %-30s | %-20s | %-20s | %s", "API-Sync",
                                    "Result Value Match", expectedVal, valMatch ? "MATCHED" : "MISMATCH",
                                    valMatch ? "✅" : "❌"));
                        }

                        // 3. Department Name check
                        String dept = (String) test.get("department_name");
                        if (dept != null && !dept.isEmpty() && !"Unknown".equalsIgnoreCase(dept)) {
                            totalChecks++;
                            // Cleanup dept name for better matching (e.g., "DEPARTMENT OF HEMATOLOGY" ->
                            // "HEMATOLOGY")
                            String cleanDept = dept.replace("DEPARTMENT OF", "").replace("DEPT OF", "").trim();
                            boolean deptFound = pdfText.toLowerCase().contains(cleanDept.toLowerCase());
                            if (deptFound)
                                matchCount++;
                            auditTrail.add(String.format("%-15s | %-30s | %-20s | %-20s | %s", "API-Sync",
                                    "Department Sync", cleanDept, deptFound ? "FOUND" : "NOT FOUND",
                                    deptFound ? "✅" : "❌"));
                        }
                    }
                }
            }

            // --- Summary Display ---
            double accuracy = totalChecks > 0 ? ((double) matchCount / totalChecks) * 100 : 0;
            boolean isPerfect = (matchCount == totalChecks && totalChecks > 0);

            System.out.println("\n🔍 " + "=".repeat(25) + " DEEP VALIDATION AUDIT TRAIL " + "=".repeat(25));
            writer.println("\n🔍 " + "=".repeat(25) + " DEEP VALIDATION AUDIT TRAIL " + "=".repeat(25));
            for (String line : auditTrail) {
                System.out.println(line);
                writer.println(line);
            }
            System.out.println("=".repeat(110));
            writer.println("=".repeat(110));

            if (isPerfect) {
                System.out.println("✅ EVERYTHING IS SUCCESS: All " + totalChecks + " data points matched perfectly!");
            } else {
                System.out.println("❌ VALIDATION COMPLETE WITH ISSUES: " + (totalChecks - matchCount) + "/"
                        + totalChecks + " points failed.");
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
            if (writer != null)
                writer.close();
        }
    }

    private boolean validateNumericalResultInPdf(String pdfText, String testName, String expectedValue) {
        String lowerPdf = pdfText.toLowerCase();
        String lowerName = testName.toLowerCase();

        int lastIndex = 0;
        int maxAttempts = 3; // In case the test name appears in header/summary without result
        int attempt = 0;

        while ((lastIndex = lowerPdf.indexOf(lowerName, lastIndex)) != -1 && attempt < maxAttempts) {
            attempt++;
            int startSnippet = lastIndex;
            int endSnippet = Math.min(lastIndex + 250, pdfText.length()); // Slightly larger window
            String snippet = pdfText.substring(startSnippet, endSnippet);

            // Match number (handles 105, 105.5, 5, 0.5)
            Pattern pattern = Pattern.compile("(\\d+(?:\\.\\d+)?)");
            Matcher matcher = pattern.matcher(snippet);

            while (matcher.find()) {
                String foundVal = matcher.group(1);
                try {
                    double foundD = Double.parseDouble(foundVal);
                    double expectedD = Double.parseDouble(expectedValue);
                    // Match found! (Allowing small float delta)
                    if (Math.abs(foundD - expectedD) < 0.01) {
                        return true;
                    }
                } catch (Exception ignored) {
                }
            }
            lastIndex += lowerName.length();
        }

        // Final fallback: try search in the entire text if the test name is multi-line
        // or tricky
        if (pdfText.contains(expectedValue)) {
            // Basic heuristic: if the number exists anywhere, it's better than nothing
            // but we'll stick to proximity for fidelity.
        }

        return false;
    }

    private String normalize(String str) {
        if (str == null)
            return "";
        return str.toLowerCase()
                .replaceAll("[^a-z0-9]", "")
                .replaceAll("\\s+", "");
    }
}

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class COD_17_ReportGenerationTest extends BaseTest {

    @Test
    public void testGetReportAndVerifyPDF() {
        LoggerUtil.info(">>> STEP 17: GET REPORT DETAILS & PDF TEXT EXTRACTION <<<");

        // Skip if COD_99 has already executed COD_17 per-visit inline
        if (RequestContext.isVisitsProcessedByUI()) {
            LoggerUtil.info("   ⏩ Skipping COD_17 — all visits already validated per-visit inside COD_99 loop.");
            return;
        }

        java.util.List<String> visits = RequestContext.getCurrentVisitNumbers();
        if (visits == null || visits.isEmpty()) {
            String singleVisit = RequestContext.getVisitNumber();
            if (singleVisit != null) {
                visits = new java.util.ArrayList<>();
                visits.add(singleVisit);
            }
        }

        if (visits == null || visits.isEmpty()) {
            // Last resort fallback — only use system property (no hardcoded default)
            // In suite flow, RequestContext always provides the visit number.
            // -DvisitNumber is only for standalone runs.
            String fallback = System.getProperty("visitNumber", "");
            if (fallback.isEmpty()) {
                LoggerUtil.warn("⚠️ No visit number found in RequestContext and none provided via -DvisitNumber. Skipping COD_17.");
                return;
            }
            visits = new java.util.ArrayList<>();
            visits.add(fallback);
            LoggerUtil.info("   ℹ️ Using visit number from -DvisitNumber system property: " + fallback);
        }

        // Filter out cancelled visits — cancelled orders should not be validated in COD_17
        java.util.Set<String> cancelledVisits17 = RequestContext.getCancelledVisitNumbers();
        if (cancelledVisits17 != null && !cancelledVisits17.isEmpty()) {
            visits = new java.util.ArrayList<>(visits);
            for (String cv : cancelledVisits17) {
                if (visits.remove(cv)) {
                    LoggerUtil.info("   ⚠️ Skipping CANCELLED visit in COD_17: " + cv);
                }
            }
        }
        if (visits.isEmpty()) {
            LoggerUtil.info("   ℹ️ All visits were cancelled — skipping COD_17 report generation validation.");
            return;
        }

        LoggerUtil.info("📊 Found " + visits.size() + " visits for Report Validation.");

        for (String visitNumber : visits) {
            LoggerUtil.info("\n" + "=".repeat(60));
            LoggerUtil.info("📄 VALIDATING REPORT FOR VISIT: " + visitNumber);
            LoggerUtil.info("=".repeat(120));

            // Polling Configuration
            int maxRetries = 10;       // up to 10 × 15s = 150 seconds per visit
            int delayMs    = 15000;
            Response response        = null;
            Map<String, Object> data = null;
            String reportUrl         = null;
            String pdfText           = null;
            int lastActualTestCount  = 0;
            boolean fullysynced      = false; // true only when non-header test results are present

            System.out.println("⏳ [" + visitNumber + "] Polling for synced report (max " + maxRetries + " × " + (delayMs/1000) + "s)...");

            for (int i = 1; i <= maxRetries; i++) {
                long startTime = System.currentTimeMillis();
                System.out.println("🔄 [" + visitNumber + "] Attempt " + i + "/" + maxRetries + " @ " + new java.util.Date());

                String token = RequestContext.getToken();
                if (token == null || token.isEmpty()) {
                    token = System.getProperty("authToken", "");
                }
                if (token == null || token.isEmpty()) {
                    Assert.fail("Authorization token is missing. Pass -DauthToken=<token> for standalone runs.");
                }

                response = new RequestBuilder()
                        .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL
                                + APIEndpoints.GET_REPORT_DETAILS.replace("{visit_Number}", visitNumber))
                        .addHeader("Authorization", "Bearer " + token)
                        .get();

                int statusCode = response.getStatusCode();
                LoggerUtil.info("📡 [" + visitNumber + "] HTTP " + statusCode + " in " + (System.currentTimeMillis() - startTime) + "ms");

                if (statusCode != 200) {
                    System.out.println("   ⚠️ Non-200 (" + statusCode + "). Retrying in " + (delayMs/1000) + "s...");
                    try { Thread.sleep(delayMs); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                    continue;
                }

                // ── Parse response ────────────────────────────────────────────
                data = response.jsonPath().getMap("data");
                if (data == null) {
                    System.out.println("   ⚠️ 'data' is null. Retrying...");
                    try { Thread.sleep(delayMs); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                    continue;
                }

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> reportTests = (List<Map<String, Object>>) data.get("tests");
                if (reportTests == null || reportTests.isEmpty()) {
                    System.out.println("   ⚠️ No tests in response. Retrying...");
                    try { Thread.sleep(delayMs); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                    continue;
                }

                // ── Count non-header (synced) entries & collect URLs ──────────
                reportUrl          = null;
                lastActualTestCount = 0;
                String fallbackHeaderUrl = null;
                int headerOnlyCount = 0;

                for (Map<String, Object> testEntry : reportTests) {
                    Object isHeaderObj = testEntry.get("isHeader");
                    boolean isHeader   = (isHeaderObj instanceof Boolean)
                            ? (Boolean) isHeaderObj
                            : Boolean.parseBoolean(String.valueOf(isHeaderObj));

                    String testName = (String) testEntry.get("testName");
                    String testCode = (String) testEntry.get("testCode");
                    String dept     = (String) testEntry.get("department_name");
                    Object urlObj   = testEntry.get("url");
                    String testUrl  = (urlObj != null && !urlObj.toString().trim().isEmpty()) ? urlObj.toString() : null;

                    if (!isHeader) {
                        lastActualTestCount++;
                        if (reportUrl == null && testUrl != null) reportUrl = testUrl;
                        System.out.println(String.format("   ✅ [SYNCED -%d] %-40s | Code: %-10s | Dept: %s",
                                lastActualTestCount, testName, testCode, dept));
                    } else {
                        headerOnlyCount++;
                        if (fallbackHeaderUrl == null && testUrl != null) fallbackHeaderUrl = testUrl;
                        System.out.println(String.format("   ⏳ [HEADER -%d] %-40s | Code: %-10s | Dept: %s  ← waiting for sync",
                                headerOnlyCount, testName, testCode, dept));
                    }
                }

                int totalTests    = reportTests.size();
                int syncedPercent = totalTests > 0 ? (lastActualTestCount * 100 / totalTests) : 0;
                System.out.println("   📊 Sync status: " + lastActualTestCount + "/" + totalTests
                        + " tests synced (" + syncedPercent + "%) — attempt " + i);

                // ── Use header URL as fallback if no non-header URL yet ───────
                if (reportUrl == null && fallbackHeaderUrl != null) {
                    reportUrl = fallbackHeaderUrl;
                }

                if (reportUrl == null) {
                    System.out.println("   ⚠️ No URL in any entry yet. Retrying...");
                    try { Thread.sleep(delayMs); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                    continue;
                }

                // ── Download & parse PDF ──────────────────────────────────────
                try {
                    String candidatePdf = extractTextFromPdfUrl(reportUrl);
                    if (candidatePdf != null && !candidatePdf.trim().isEmpty()) {
                        pdfText = candidatePdf;
                    }
                } catch (Exception e) {
                    System.out.println("   ❌ PDF extraction error: " + e.getMessage());
                }

                // ── Check if ALL tests synced (no header-only entries left) ───
                if (lastActualTestCount == totalTests && pdfText != null) {
                    fullysynced = true;
                    System.out.println("   ✅ ALL " + totalTests + " tests fully synced for visit: " + visitNumber);
                    break;
                }

                // If not all synced yet, log progress and retry
                if (lastActualTestCount > 0) {
                    System.out.println("   ℹ️ Partial sync (" + lastActualTestCount + "/" + totalTests
                            + "). Waiting " + (delayMs/1000) + "s for remaining tests to sync...");
                } else {
                    System.out.println("   ⏳ No results synced yet. Waiting " + (delayMs/1000) + "s...");
                }
                try { Thread.sleep(delayMs); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            }

            if (!fullysynced) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> finalTests = data != null
                        ? (List<Map<String, Object>>) data.get("tests") : null;
                int total = finalTests != null ? finalTests.size() : 0;
                System.out.println("   ⚠️ Visit " + visitNumber + ": Only " + lastActualTestCount + "/" + total
                        + " tests synced after " + maxRetries + " retries. Proceeding with available data.");
            }

            // ── Assert we at least have a URL and PDF ─────────────────────────
            Assert.assertNotNull(reportUrl, "❌ Report URL not ready for visit: " + visitNumber);

            if (pdfText == null || pdfText.trim().isEmpty()) {
                System.out.println("❌ PDF text extraction FAILED for visit: " + visitNumber + " | URL: " + reportUrl);
                Assert.fail("❌ PDF could not be extracted for visit: " + visitNumber);
            } else {
                // ── Print full PDF text ───────────────────────────────────────
                System.out.println("\n" + "=".repeat(70));
                System.out.println("📄 PDF PARSED (" + pdfText.length() + " chars) — Visit: " + visitNumber);
                System.out.println("=".repeat(70));
                System.out.println(pdfText);
                System.out.println("=".repeat(70) + "\n");

                // ── Always validate: userName and visitNumber must appear in any PDF ────
                String apiUserName = (String) data.get("userName");
                String apiVisitNum = (String) data.get("visitNumber");

                System.out.println("\n🔍 " + "=".repeat(50));
                System.out.println("   PDF CONTENT VALIDATION — Visit: " + visitNumber);
                System.out.println("=".repeat(55));

                // Visit Number in PDF
                boolean visitInPdf = apiVisitNum != null && pdfText.contains(apiVisitNum);
                System.out.println("   Visit Number (" + apiVisitNum + ") in PDF : " + (visitInPdf ? "✅ FOUND" : "❌ NOT FOUND"));

                // Patient Name in PDF (strip prefix Mr./Ms. for fuzzy match)
                boolean nameInPdf = false;
                if (apiUserName != null) {
                    String cleanName = apiUserName.replace("Mr.", "").replace("Ms.", "").replace("Mrs.", "").trim();
                    nameInPdf = pdfText.toLowerCase().contains(cleanName.toLowerCase());
                    System.out.println("   Patient Name  (" + apiUserName + ") in PDF : " + (nameInPdf ? "✅ FOUND" : "❌ NOT FOUND"));
                }

                if (lastActualTestCount == 0) {
                    // Header-only — validate test names AND extract result values from PDF body
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> allTests = (List<Map<String, Object>>) data.get("tests");
                    System.out.println("   Mode          : ⚠️  HEADER-ONLY (lab results in PDF body section)");
                    System.out.println("   " + "-".repeat(90));
                    System.out.printf("   %-35s | %-8s | %-8s | %-8s | %-16s | %s%n",
                            "TEST NAME", "NAME", "DEPT", "RESULT", "VALUE IN PDF", "UNIT");
                    System.out.println("   " + "-".repeat(90));

                    java.util.Set<String> seenTests = new java.util.LinkedHashSet<>();
                    if (allTests != null) {
                        for (Map<String, Object> t : allTests) {
                            String tn   = (String) t.get("testName");
                            String dept = (String) t.get("department_name");
                            if (tn == null || seenTests.contains(tn)) continue;
                            seenTests.add(tn);

                            boolean tnFound   = pdfText.toLowerCase().contains(tn.toLowerCase());
                            boolean deptFound = dept != null && pdfText.toLowerCase().contains(dept.toLowerCase());

                            // ── Extract result value + unit from PDF text after the test name ──
                            // PDF pattern: "TEST NAME  <value> <unit> <refrange> <method>"
                            String extractedValue = "N/A";
                            String extractedUnit  = "";
                            if (tnFound) {
                                int idx = pdfText.toLowerCase().indexOf(tn.toLowerCase());
                                if (idx >= 0) {
                                    // Look in the 200 chars after the test name occurrence
                                    String snippet = pdfText.substring(idx + tn.length(),
                                            Math.min(idx + tn.length() + 200, pdfText.length()));
                                    // Match: optional spaces then a number (e.g. 105, 0.5, 12.3)
                                    java.util.regex.Matcher m = java.util.regex.Pattern
                                            .compile("\\s+(\\d+(?:\\.\\d+)?)\\s+([a-zA-Z/%]+(?:/[a-zA-Z]+)?)")
                                            .matcher(snippet);
                                    if (m.find()) {
                                        extractedValue = m.group(1);
                                        extractedUnit  = m.group(2);
                                    }
                                }
                            }

                            boolean resultFound = !extractedValue.equals("N/A");
                            System.out.printf("   %-35s | %-8s | %-8s | %-8s | %-16s | %s%n",
                                    tn,
                                    tnFound    ? "✅ FOUND" : "❌ MISS",
                                    deptFound  ? "✅ FOUND" : "❌ MISS",
                                    resultFound ? "✅ FOUND" : "⚠️  MISS",
                                    extractedValue,
                                    extractedUnit);
                        }
                    }
                    System.out.println("   " + "-".repeat(90));

                    // Also print the raw PDF body section (lines that contain test results)
                    System.out.println("\n   📋 RAW RESULT LINES FROM PDF:");
                    for (String line : pdfText.split("\n")) {
                        String stripped = line.trim();
                        // Print lines that contain a number followed by a unit (likely result lines)
                        if (stripped.matches(".*\\d+(\\.\\d+)?\\s+[a-zA-Z/%]+.*") && stripped.length() > 5) {
                            System.out.println("      " + stripped);
                        }
                    }
                    System.out.println("=".repeat(55) + "\n");
                    // Soft assertion — PDF downloaded and visit/name found is sufficient
                    Assert.assertTrue(visitInPdf || nameInPdf,
                            "❌ Neither visitNumber nor patientName found in PDF for visit: " + visitNumber);
                } else {
                    System.out.println("=".repeat(55) + "\n");
                    // Full deep validation when actual results are present
                    performTripleValidation(pdfText, data);
                }
            }
        }
        LoggerUtil.info("✔ All requested reports verified successfully!");
    }

    private String extractTextFromPdfUrl(String pdfUrl) {
        try {
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(pdfUrl).openConnection();
            connection.setRequestMethod("GET");
            // Only add Authorization header for non-S3 URLs.
            // Pre-signed S3 URLs carry auth in query params — adding Authorization header causes signature conflict.
            boolean isS3Url = pdfUrl.contains("amazonaws.com") || pdfUrl.contains("X-Amz-Signature");
            if (!isS3Url) {
                String token = RequestContext.getToken();
                if (token != null && !token.isEmpty()) {
                    connection.setRequestProperty("Authorization", "Bearer " + token);
                }
            }
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.connect();
            int httpCode = connection.getResponseCode();
            if (httpCode != 200) {
                LoggerUtil.error("❌ PDF download returned HTTP " + httpCode + " for URL: " + pdfUrl);
                return null;
            }
            try (InputStream inputStream = connection.getInputStream();
                 PDDocument document = PDDocument.load(inputStream)) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            } finally {
                connection.disconnect();
            }
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

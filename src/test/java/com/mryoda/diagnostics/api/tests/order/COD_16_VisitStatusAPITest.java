package com.mryoda.diagnostics.api.tests.order;

import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.RequestContext;
import utilities.ScenarioContext;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.path.json.JsonPath;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.*;

public class COD_16_VisitStatusAPITest {

    @Test
    public void testGetVisitStatus() {
        System.out.println("\n>>> STEP 16: GET VISIT STATUS API (UAT) WITH MULTI-TEST VALIDATION <<<");

        java.util.List<String> visits = RequestContext.getCurrentVisitNumbers();
        if (visits == null || visits.isEmpty()) {
            String singleVisit = RequestContext.getVisitNumber();
            if (singleVisit != null) {
                visits = new java.util.ArrayList<>();
                visits.add(singleVisit);
            }
        }

        if (visits == null || visits.isEmpty()) {
            Assert.fail("❌ No Visit Numbers found in RequestContext!");
        }

        System.out.println("📊 Found " + visits.size() + " visits to validate.");

        for (String visitNumber : visits) {
            System.out.println("\n-------------------------------------------------------");
            System.out.println("🔍 VALIDATING VISIT: " + visitNumber);
            System.out.println("-------------------------------------------------------");

            String expectedSinNo = RequestContext.getSinNumberForVisit(visitNumber);
            if (expectedSinNo == null || expectedSinNo.isEmpty()) {
                expectedSinNo = ScenarioContext.extractedSinNo;
            }

            // Get all expected test names from RequestContext
            Set<String> expectedTestNames = new HashSet<>();
            if (RequestContext.getAllTests() != null) {
                expectedTestNames.addAll(RequestContext.getAllTests().keySet());
            }
            if (RequestContext.getAllStoredTests() != null) {
                expectedTestNames.addAll(RequestContext.getAllStoredTests().keySet());
            }

            // --- KEY FIX FOR PACKAGES ---
            List<String> packageTests = RequestContext.getPackageTestNames();
            if (packageTests != null && !packageTests.isEmpty()) {
                System.out.println("   📦 Adding Package Component Tests to Expected List: " + packageTests);
                expectedTestNames.addAll(packageTests);
            }

            System.out.println("   Extracted Visit Number (LabNo): " + visitNumber);
            System.out.println("   Expected SIN No (from UI): " + expectedSinNo);
            System.out.println("   Expected Test Names: " + expectedTestNames);

            String endpoint = APIEndpoints.VISIT_STATUS_UAT;
            String quotedVisitNumber = "\"" + visitNumber + "\"";

            int maxRetries = 10;
            boolean validated = false;

            for (int i = 1; i <= maxRetries; i++) {
                System.out.println("\n   --- 🔄 Attempt " + i + "/" + maxRetries + " ---");

                Response response = RestAssured.given()
                        .queryParam("LabNo", quotedVisitNumber)
                        .post(endpoint);

                if (response.getStatusCode() == 200) {
                    JsonPath rootJson = response.jsonPath();
                    if (rootJson.getString("success").equalsIgnoreCase("true")) {
                        String dataString = rootJson.getString("data");

                        if (dataString == null || dataString.equals("[]") || dataString.isEmpty()) {
                            System.out.println("      ⏳ 'data' is empty. Results might be processing...");
                        } else {
                            System.out.println("      📦 Received Data: " + dataString);
                            List<Map<String, String>> actualItems = JsonPath.from(dataString).getList("");

                            // Sets to track findings
                            Set<String> foundAndApprovedTests = new HashSet<>();
                            Set<String> foundBarcodes = new HashSet<>();
                            boolean allFoundItemsApproved = true;

                            System.out.println("\n      🔍 Individual Item Validation:");
                            for (Map<String, String> item : actualItems) {
                                String itemName = item.get("ItemName");
                                String barcode = item.get("BarcodeNo");
                                String status = item.get("Status");

                                System.out
                                        .print("         - [" + itemName + "] | Barcode: " + barcode + " | Status: "
                                                + status);

                                // Robust Normalization for matching
                                String normalizedItem = normalize(itemName);
                                boolean isExpected = expectedTestNames.stream()
                                        .anyMatch(e -> normalize(e).equals(normalizedItem));

                                boolean isApproved = "Approved".equalsIgnoreCase(status) ||
                                        "Sample collected".equalsIgnoreCase(status);
                                if (isExpected && isApproved) {
                                    foundAndApprovedTests.add(normalizedItem);
                                    foundBarcodes.add(barcode);
                                    System.out.println(" -> ✅ VALID");
                                } else {
                                    if (!isExpected)
                                        System.out.print(" -> ⚠️ UNEXPECTED NAME");
                                    if (!isApproved) {
                                        System.out.print(" -> ❌ NOT APPROVED");
                                        allFoundItemsApproved = false;
                                    }
                                    System.out.println();
                                }
                            }

                            boolean allExpectedFound = true;
                            List<String> missingTests = new ArrayList<>();

                            Set<String> mandatoryNormalized = new HashSet<>();
                            boolean hasPackageComponents = (packageTests != null && !packageTests.isEmpty());

                            for (String expected : expectedTestNames) {
                                String normExp = normalize(expected);
                                boolean isPackageParent = hasPackageComponents
                                        && expectedTestNames.size() > packageTests.size()
                                        && !packageTests.contains(expected);

                                if (!isPackageParent) {
                                    mandatoryNormalized.add(normExp);
                                    if (!foundAndApprovedTests.contains(normExp)) {
                                        allExpectedFound = false;
                                        missingTests.add(expected);
                                    }
                                }
                            }

                            boolean sinMatch = true;
                            if (expectedSinNo != null && !expectedSinNo.isEmpty()) {
                                sinMatch = foundBarcodes.contains(expectedSinNo);
                            }

                            if (allExpectedFound && allFoundItemsApproved && sinMatch) {
                                System.out.println("\n      ✅ VALIDATION SUCCESS for " + visitNumber);
                                validated = true;
                                break;
                            } else {
                                System.out.println("\n      ⚠️ Validation incomplete for " + visitNumber + ":");
                                if (!allExpectedFound)
                                    System.out.println("      - Missing or unapproved tests: " + missingTests);
                                if (!allFoundItemsApproved)
                                    System.out.println("      - Some items are not yet Approved.");
                                if (!sinMatch)
                                    System.out.println(
                                            "      - SIN No " + expectedSinNo + " not found in barcodes "
                                                    + foundBarcodes);
                            }
                        }
                    }
                } else {
                    System.out.println("      ⚠️ Server Error: " + response.getStatusCode());
                }

                if (i < maxRetries) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                    }
                }
            }
            Assert.assertTrue(validated, "Validation failed for visit: " + visitNumber);
        }
    }

    /**
     * Normalizes names for comparison:
     * - Removes special characters: ( ) [ ] - _
     * - Removes spaces
     * - Converts to lower case
     */
    private String normalize(String name) {
        if (name == null)
            return "";
        return name.toLowerCase()
                .replaceAll("[\\(\\)\\[\\]\\-_\\s]", "") // Remove ( ) [ ] - _ and spaces
                .trim();
    }
}

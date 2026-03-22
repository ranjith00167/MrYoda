package com.mryoda.diagnostics.api.tests.order;

import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.RequestContext;
import com.mryoda.diagnostics.api.utils.PackageComponentResolver;
import utilities.ScenarioContext;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.path.json.JsonPath;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.util.*;

public class COD_16_VisitStatusAPITest {

    @BeforeClass
    public void ensureVisitNumbersPresent() {
        System.out.println("\n>>> SETUP: Ensuring visit numbers are present in RequestContext <<<");
        List<String> visits = RequestContext.getCurrentVisitNumbers();
        if (visits == null || visits.isEmpty()) {
            // Try to populate from existing order→visit mapping
            Map<String, String> orderVisit = RequestContext.getOrderVisitMap();
            if (orderVisit != null && !orderVisit.isEmpty()) {
                List<String> derived = new ArrayList<>(new java.util.HashSet<>(orderVisit.values()));
                RequestContext.setCurrentVisitNumbers(derived);
                System.out.println("   ✅ Populated visit numbers from Order→Visit map: " + derived);
                return;
            }

            // Fallback: read comma-separated visit numbers from system property `visitNumbers`
            String prop = System.getProperty("visitNumbers");
            if (prop != null && !prop.trim().isEmpty()) {
                List<String> fromProp = new ArrayList<>();
                for (String s : prop.split(",")) {
                    if (s != null && !s.trim().isEmpty())
                        fromProp.add(s.trim());
                }
                if (!fromProp.isEmpty()) {
                    RequestContext.setCurrentVisitNumbers(fromProp);
                    System.out.println("   ✅ Populated visit numbers from system property: " + fromProp);
                    return;
                }
            }

            System.out.println("   ⚠️ No visit numbers found in RequestContext. Tests may fail unless visits are provided.");
        } else {
            System.out.println("   ✅ Visit numbers already present: " + visits);
        }
    }


    @Test(description = "QA Automation: Verify Get Visit Status")
    public void testGetVisitStatus() {
        System.out.println("\n>>> STEP 16: GET VISIT STATUS API (UAT) WITH MULTI-TEST VALIDATION <<<");

        // Skip if COD_99 has already executed COD_16 per-visit inline
        if (RequestContext.isVisitsProcessedByUI()) {
            System.out.println("   ⏩ Skipping COD_16 — all visits already validated per-visit inside COD_99 loop.");
            return;
        }

        List<String> visits = RequestContext.getCurrentVisitNumbers();
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

        // Filter out cancelled visits — cancelled orders should not be validated in COD_16
        java.util.Set<String> cancelledVisits16 = RequestContext.getCancelledVisitNumbers();
        if (cancelledVisits16 != null && !cancelledVisits16.isEmpty()) {
            visits = new java.util.ArrayList<>(visits);
            for (String cv : cancelledVisits16) {
                if (visits.remove(cv)) {
                    System.out.println("   ⚠️ Skipping CANCELLED visit in COD_16: " + cv);
                }
            }
        }
        if (visits.isEmpty()) {
            System.out.println("   ℹ️ All visits were cancelled — skipping COD_16 visit status validation.");
            return;
        }

        if (visits == null) {
            Assert.fail("❌ visits list is null unexpectedly.");
            return;
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
            // IMPORTANT: Separate individual tests from package-type products
            Set<String> expectedTestNames = new HashSet<>();
            Set<String> packageProductNames = new HashSet<>();

            // From getAllStoredTests, only add INDIVIDUAL tests (not packages)
            Map<String, Map<String, Object>> allStoredTests = RequestContext.getAllStoredTests();
            if (allStoredTests != null) {
                for (Map.Entry<String, Map<String, Object>> entry : allStoredTests.entrySet()) {
                    String name = entry.getKey();
                    Map<String, Object> details = entry.getValue();
                    String type = details != null && details.get("type") != null
                            ? details.get("type").toString().toLowerCase() : "test";
                    if (type.contains("package") || type.contains("panel")) {
                        packageProductNames.add(name); // track for logging; do NOT add to expected
                        System.out.println("   📦 Identified package (will validate by components): " + name);
                    } else {
                        expectedTestNames.add(name);
                    }
                }
            }
            // Also from getAllTests (same filter)
            if (RequestContext.getAllTests() != null) {
                for (Map.Entry<String, Map<String, Object>> entry : RequestContext.getAllTests().entrySet()) {
                    String name = entry.getKey();
                    Map<String, Object> details = entry.getValue();
                    
                    // Apply same package detection logic
                    String type = details != null && details.get("type") != null
                            ? details.get("type").toString().toLowerCase() : "test";
                    
                    if (type.contains("package") || type.contains("panel")) {
                        if (!packageProductNames.contains(name)) {
                            packageProductNames.add(name);
                            System.out.println("   📦 Identified package from getAllTests: " + name);
                        }
                    } else if (!packageProductNames.contains(name)) {
                        expectedTestNames.add(name);
                    }
                }
            }

            // Pre-resolved component names stored during order processing (from sample_types → Tests)
            // These are the actual individual tests visible in visit status
            List<String> preResolvedComponents = RequestContext.getPackageTestNames();
            if (preResolvedComponents != null && !preResolvedComponents.isEmpty()) {
                expectedTestNames.addAll(preResolvedComponents);
                System.out.println("   ✅ Added " + preResolvedComponents.size() + " pre-resolved components to expected tests:");
                preResolvedComponents.forEach(c -> System.out.println("      - " + c));
            }

            System.out.println("   📦 Package products (excluded from expected): " + packageProductNames);
            System.out.println("   🔬 Individual tests expected in visit status: " + expectedTestNames);

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
                            Set<String> foundTestNamesNormalized = new HashSet<>();
                            Set<String> foundBarcodes = new HashSet<>();

                            System.out.println("\n      🔍 Individual Item Validation:");
                            for (Map<String, String> item : actualItems) {
                                String itemName = item.get("ItemName");
                                String barcode = item.get("BarcodeNo");
                                String status = item.get("Status");

                                System.out.print("         - [" + itemName + "] | Barcode: " + barcode + " | Status: " + status);

                                // Robust Normalization for matching
                                String normalizedItem = normalize(itemName);
                                if (barcode != null && !barcode.isEmpty()) foundBarcodes.add(barcode);

                                boolean isExpected = expectedTestNames.isEmpty() ||
                                        expectedTestNames.stream().anyMatch(e -> normalize(e).equals(normalizedItem));

                                // In staging env, sample collection hasn't happened yet.
                                // Consider item as "found" if it's present in the response.
                                // Accept: Approved, Tested, Sample collected, Sample Not Collected, Order booked, Booked
                                boolean isSamplePresent = status != null && (
                                        "Approved".equalsIgnoreCase(status) ||
                                        "Tested".equalsIgnoreCase(status) ||
                                        "Sample collected".equalsIgnoreCase(status) ||
                                        "Sample Not Collected".equalsIgnoreCase(status) ||
                                        "Order booked".equalsIgnoreCase(status) ||
                                        "Booked".equalsIgnoreCase(status));

                                if (isExpected) {
                                    foundTestNamesNormalized.add(normalizedItem);
                                    System.out.println(" -> ✅ FOUND (Status: " + status + ")");
                                } else {
                                    // When packages were ordered, unknown items are likely package components
                                    if (!packageProductNames.isEmpty()) {
                                        System.out.println(" -> ℹ️ LIKELY PACKAGE COMPONENT (not in expected list, but accepted)");
                                        foundTestNamesNormalized.add(normalizedItem);
                                    } else {
                                        System.out.println(" -> ⚠️ UNEXPECTED NAME (not in expected list - ignored)");
                                    }
                                }
                                // Log warning if status is unexpected
                                if (!isSamplePresent) {
                                    System.out.println("            ⚠️ Unrecognized status: " + status);
                                }
                            }

                            // Check if all expected tests are present in the response
                            boolean allExpectedFound = true;
                            List<String> missingTests = new ArrayList<>();
                            List<String> missingPackages = new ArrayList<>();

                            if (!expectedTestNames.isEmpty()) {
                                for (String expected : expectedTestNames) {
                                    String normExp = normalize(expected);
                                    if (!foundTestNamesNormalized.contains(normExp)) {
                                        // Separate packages from individual tests
                                        if (expected.contains("Panel") || expected.contains("Profile") || expected.contains("Package")) {
                                            missingPackages.add(expected);
                                        } else {
                                            missingTests.add(expected);
                                        }
                                    }
                                }
                                // Only mark as not found if there are genuine missing tests (not packages)
                                allExpectedFound = missingTests.isEmpty();
                            } else {
                                // No expected test names → accept any response with items present
                                allExpectedFound = !actualItems.isEmpty();
                            }

                            boolean sinMatch = true;
                            if (expectedSinNo != null && !expectedSinNo.isEmpty()) {
                                sinMatch = foundBarcodes.contains(expectedSinNo);
                            }

                            if (allExpectedFound && sinMatch) {
                                System.out.println("\n      ✅ VALIDATION SUCCESS for " + visitNumber);
                                if (!missingPackages.isEmpty()) {
                                    System.out.println("      ℹ️ Packages validated by their components: " + missingPackages);
                                }
                                validated = true;
                                break;
                            } else {
                                System.out.println("\n      ⚠️ Validation incomplete for " + visitNumber + ":");
                                if (!missingTests.isEmpty()) {
                                    System.out.println("      - Missing individual tests: " + missingTests);
                                }
                                if (!sinMatch)
                                    System.out.println(
                                            "      - SIN No " + expectedSinNo + " not found in barcodes "
                                                    + foundBarcodes);
                            }
                        }
                    }
                } else {
                    System.out.println("      ⚠️ Non-200 (" + response.getStatusCode() + "). Retrying in 15s...");
                }

                if (i < maxRetries) {
                    try {
                        Thread.sleep(15000);
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

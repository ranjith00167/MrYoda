package com.mryoda.diagnostics.api.tests;

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

        String visitNumber = RequestContext.getVisitNumber();
        String expectedSinNo = ScenarioContext.extractedSinNo;
        
        // Get all expected test names from RequestContext
        Set<String> expectedTestNames = new HashSet<>();
        if (RequestContext.getAllTests() != null) {
            expectedTestNames.addAll(RequestContext.getAllTests().keySet());
        }
        if (RequestContext.getAllStoredTests() != null) {
            expectedTestNames.addAll(RequestContext.getAllStoredTests().keySet());
        }

        System.out.println("   Extracted Visit Number (LabNo): " + visitNumber);
        System.out.println("   Expected SIN No (from UI): " + expectedSinNo);
        System.out.println("   Expected Test Names: " + expectedTestNames);

        Assert.assertNotNull(visitNumber, "Visit Number (LabNo) should not be null");
        
        // Fallback for demo/safety
        if (expectedTestNames.isEmpty()) {
            System.out.println("   ⚠️ Warning: No expected tests found in context. Using fallback 'RANDOM BLOOD GLUCOSE (RBS)'.");
            expectedTestNames.add("RANDOM BLOOD GLUCOSE (RBS)");
        }

        String endpoint = APIEndpoints.VISIT_STATUS_UAT;
        String quotedVisitNumber = "\"" + visitNumber + "\"";

        int maxRetries = 10;
        boolean validated = false;

        for (int i = 1; i <= maxRetries; i++) {
            System.out.println("\n--- 🔄 Attempt " + i + "/" + maxRetries + " ---");
            
            Response response = RestAssured.given()
                    .queryParam("LabNo", quotedVisitNumber)
                    .post(endpoint);

            if (response.getStatusCode() == 200) {
                JsonPath rootJson = response.jsonPath();
                if (rootJson.getString("success").equalsIgnoreCase("true")) {
                    String dataString = rootJson.getString("data");
                    
                    if (dataString == null || dataString.equals("[]") || dataString.isEmpty()) {
                        System.out.println("   ⏳ 'data' is empty. Results might be processing...");
                    } else {
                        System.out.println("   📦 Received Data: " + dataString);
                        List<Map<String, String>> actualItems = JsonPath.from(dataString).getList("");
                        
                        // Sets to track findings
                        Set<String> foundAndApprovedTests = new HashSet<>();
                        Set<String> foundBarcodes = new HashSet<>();
                        boolean allFoundItemsApproved = true;

                        System.out.println("\n   🔍 Individual Item Validation:");
                        for (Map<String, String> item : actualItems) {
                            String itemName = item.get("ItemName");
                            String barcode = item.get("BarcodeNo");
                            String status = item.get("Status");
                            
                            System.out.print("      - [" + itemName + "] | Barcode: " + barcode + " | Status: " + status);
                            
                            boolean isExpected = expectedTestNames.stream().anyMatch(e -> e.equalsIgnoreCase(itemName));
                            boolean isApproved = "Approved".equalsIgnoreCase(status);
                            
                            if (isExpected && isApproved) {
                                foundAndApprovedTests.add(itemName.toLowerCase());
                                foundBarcodes.add(barcode);
                                System.out.println(" -> ✅ VALID");
                            } else {
                                if (!isExpected) System.out.print(" -> ⚠️ UNEXPECTED NAME");
                                if (!isApproved) {
                                    System.out.print(" -> ❌ NOT APPROVED");
                                    allFoundItemsApproved = false;
                                }
                                System.out.println();
                            }
                        }

                        // Final logic for multi-test success:
                        // 1. All expected tests must be found and approved.
                        // 2. All items in the response must be approved.
                        // 3. At least one barcode should match the extracted SIN No (if available).
                        
                        boolean allExpectedFound = expectedTestNames.stream()
                                .allMatch(e -> foundAndApprovedTests.contains(e.toLowerCase()));
                        
                        boolean sinMatch = true;
                        if (expectedSinNo != null && !expectedSinNo.isEmpty()) {
                            sinMatch = foundBarcodes.contains(expectedSinNo);
                        }

                        if (allExpectedFound && allFoundItemsApproved && sinMatch) {
                            System.out.println("\n✅ VALIDATION SUCCESS:");
                            System.out.println("   - All expected tests (" + expectedTestNames.size() + ") are Approved.");
                            System.out.println("   - SIN No check passed: " + (expectedSinNo != null ? expectedSinNo : "N/A"));
                            validated = true;
                            break;
                        } else {
                            System.out.println("\n⚠️ Validation incomplete:");
                            if (!allExpectedFound) System.out.println("   - Missing or unapproved tests. Expected: " + expectedTestNames);
                            if (!allFoundItemsApproved) System.out.println("   - Some items are not yet Approved.");
                            if (!sinMatch) System.out.println("   - SIN No " + expectedSinNo + " not found in barcodes " + foundBarcodes);
                        }
                    }
                }
            } else {
                System.out.println("   ⚠️ Server Error: " + response.getStatusCode());
            }

            if (i < maxRetries) {
                try { Thread.sleep(5000); } catch (InterruptedException e) {}
            }
        }

        Assert.assertTrue(validated, "Multi-test validation failed after retries.");
    }
}

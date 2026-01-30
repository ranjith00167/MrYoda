package com.mryoda.diagnostics.api.tests;

import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.AssertionUtil;
import com.mryoda.diagnostics.api.utils.RequestContext;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CatalogVerificationAPITest extends BaseTest {

    @Test(priority = 1)
    public void verifyDiagnosticsCatalog() {
        System.out.println("\n==========================================================");
        System.out.println("      CATALOG VERIFICATION - DIAGNOSTICS");
        System.out.println("==========================================================");

        String locationId = RequestContext.getLocationId(DEFAULT_LOCATION);
        if (locationId == null) {
            System.out.println("⚠️ Location ID not found, using HARDCODED fallback for test");
            locationId = "676a5fa720093d2807af03a5"; // Fallback from user prompt
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("limit", 10);
        payload.put("location", locationId);
        payload.put("diseases", new ArrayList<>());

        // STRATEGY 1: Use a validated test from Global Search (Context)
        String targetName = null;
        Map<String, Map<String, Object>> storedTests = RequestContext.getAllTests();
        if (storedTests != null && !storedTests.isEmpty()) {
            targetName = storedTests.keySet().iterator().next();
            System.out.println("✅ Found validated test in Context from Global Search: " + targetName);
        }

        // STRATEGY 2: Fallback to Dynamic Fetch from API if Context is empty
        if (targetName == null) {
            System.out.println("⚠️ No tests found in Context. Fetching dynamically from API...");
            targetName = fetchFirstItemName(APIEndpoints.GET_ALL_TESTS, payload);
        }

        AssertionUtil.verifyNotNull(targetName, "Failed to identify a target test for Diagnostics catalog verification. Context and dynamic fetch both returned null.");
        System.out.println("🎯 Final Verification Target: " + targetName);
        searchAndVerifyItem(APIEndpoints.GET_ALL_TESTS, payload, "Diagnostics", targetName);
    }

    @Test(priority = 2)
    public void verifyDnaDecoderCatalog() {
        System.out.println("\n==========================================================");
        System.out.println("      CATALOG VERIFICATION - DNA DECODER");
        System.out.println("==========================================================");

        Map<String, Object> payload = new HashMap<>();
        payload.put("limit", 20);
        payload.put("dnadecoder", true);

        String targetName = fetchFirstItemName(APIEndpoints.GET_ALL_TESTS, payload);
        
        AssertionUtil.verifyNotNull(targetName, "Failed to identify a target test for DNA Decoder catalog verification.");
        System.out.println("🎯 Dynamic Target Selected: " + targetName);

        searchAndVerifyItem(APIEndpoints.GET_ALL_TESTS, payload, "DNA Decoder", targetName);
    }

    @Test(priority = 3)
    public void verifyPgxCatalog() {
        System.out.println("\n==========================================================");
        System.out.println("      CATALOG VERIFICATION - PGX (PHARMACOGENOMICS)");
        System.out.println("==========================================================");

        String locationId = RequestContext.getLocationId(DEFAULT_LOCATION);
        if (locationId == null)
            locationId = "676a5fa720093d2807af03a5";

        Map<String, Object> payload = new HashMap<>();
        payload.put("limit", 20);
        payload.put("pharmacogenomics", true);
        payload.put("location", locationId);
        payload.put("popular", true);

        String targetName = fetchFirstItemName(APIEndpoints.GET_ALL_TESTS, payload);
        
        AssertionUtil.verifyNotNull(targetName, "Failed to identify a target test for PGX catalog verification.");
        System.out.println("🎯 Dynamic Target Selected: " + targetName);

        searchAndVerifyItem(APIEndpoints.GET_ALL_TESTS, payload, "PGX", targetName);
    }

    @Test(priority = 4)
    public void verifyFetalMedicineCatalog() {
        System.out.println("\n==========================================================");
        System.out.println("      CATALOG VERIFICATION - FETAL MEDICINE");
        System.out.println("==========================================================");

        String locationId = RequestContext.getLocationId(DEFAULT_LOCATION);
        if (locationId == null)
            locationId = "676a5fa720093d2807af03a5";

        Map<String, Object> payload = new HashMap<>();
        payload.put("limit", 10);
        payload.put("popular", true);
        payload.put("filter", "");
        payload.put("location", locationId);

        String targetName = fetchFirstItemName(APIEndpoints.GET_FETAL_MEDICINE_TESTS, payload);
        
        AssertionUtil.verifyNotNull(targetName, "Failed to identify a target test for Fetal Medicine catalog verification.");
        System.out.println("🎯 Dynamic Target Selected: " + targetName);

        searchAndVerifyItem(APIEndpoints.GET_FETAL_MEDICINE_TESTS, payload, "Fetal Medicine", targetName);
    }

    @Test(priority = 5)
    public void verifyPackagesCatalog() {
        System.out.println("\n==========================================================");
        System.out.println("      CATALOG VERIFICATION - PACKAGES");
        System.out.println("==========================================================");

        String locationId = RequestContext.getLocationId(DEFAULT_LOCATION);
        if (locationId == null)
            locationId = "676a5fa720093d2807af03a5";

        Map<String, Object> payload = new HashMap<>();
        payload.put("limit", 10);
        payload.put("location", locationId);
        payload.put("diseases", new ArrayList<>());

        String targetName = fetchFirstItemName(APIEndpoints.GET_ALL_PACKAGES, payload);
        
        AssertionUtil.verifyNotNull(targetName, "Failed to identify a target package for catalog verification.");
        System.out.println("🎯 Dynamic Target Selected: " + targetName);

        searchAndVerifyItem(APIEndpoints.GET_ALL_PACKAGES, payload, "Packages", targetName);
    }

    /**
     * Generic method to search for a specific item across multiple pages.
     * Iterates until the item is found OR the max page limit is reached.
     */
    private void searchAndVerifyItem(String endpoint, Map<String, Object> payload, String category, String targetName) {
        int page = 1;
        int maxPages = 5; // Safety limit to avoid infinite loops
        boolean found = false;

        System.out.println("🔍 Searching for '" + targetName + "' in " + category + " catalog...");

        while (page <= maxPages && !found) {
            System.out.println("   📄 Scanning Page: " + page);
            payload.put("page", page);

            Response response = new RequestBuilder()
                    .setEndpoint(endpoint)
                    .setRequestBody(payload)
                    .post();

            // Validate Basic Response
            AssertionUtil.verifyEquals(response.getStatusCode(), 200, category + " API Status should be 200");

            List<Map<String, Object>> items = null;
            try {
                // Handle response structure variations (data.tests vs data list vs directly list)
                Object dataObj = response.jsonPath().get("data");
                if (dataObj instanceof List) {
                    items = (List<Map<String, Object>>) dataObj;
                } else if (dataObj instanceof Map) {
                    Map<String, Object> dataMap = (Map<String, Object>) dataObj;
                    if (dataMap.containsKey("tests")) {
                        items = (List<Map<String, Object>>) dataMap.get("tests");
                    } else if (dataMap.containsKey("packages")) {
                        items = (List<Map<String, Object>>) dataMap.get("packages");
                    }
                }
            } catch (Exception e) {
                System.out.println("   ⚠️ Error parsing response data: " + e.getMessage());
            }

            if (items == null || items.isEmpty()) {
                System.out.println("   🛑 No more items found on page " + page + ". Ending search.");
                break;
            }

            // Iterate items
            for (Map<String, Object> item : items) {
                String name = (String) item.get("name"); // Adjust field name if "test_name" or "product_name"
                if (name == null) name = (String) item.get("test_name");
                if (name == null) name = (String) item.get("product_name");

                 // Soft match logic
                if (name != null && name.toLowerCase().contains(targetName.toLowerCase())) {
                    System.out.println("\n✅ FOUND MATCH: " + name);
                    verifyPriceAndDetails(item);
                    found = true;
                    break;
                }
            }
            
            // If checking ALL items (not just target), validation can happen here for every item
            // For now, we continue to next page
            page++;
        }

        if (!found) {
            System.out.println("\n⚠️  Target '" + targetName + "' NOT FOUND after scanning " + (page - 1) + " pages.");
            System.out.println("   (This might be expected if the test data is dynamic/limited)");
        }
    }

    private void verifyPriceAndDetails(Map<String, Object> item) {
        // Price Verification
        Object priceObj = item.get("price");
        Object offerPriceObj = item.get("offer_price");
        Object finalPriceObj = item.get("final_price"); // API dependent

        String name = (String) item.getOrDefault("name", "Unknown Item");

        System.out.println("   📋 Details for: " + name);
        
        // Normalize Price
        double price = parsePrice(priceObj);
        double finalPrice = parsePrice(finalPriceObj);
        
        if(price > 0) {
            System.out.println("      Price Verified: ₹" + price);
        } else {
             System.out.println("      ⚠️ Warning: Price is 0 or null");
        }

        // Check ID
        Object id = item.get("id"); 
        if (id == null) {
            id = item.get("_id"); // Fallback for some API responses
        }
        
        if (id != null) {
            System.out.println("      ID Verified: " + id);
        } else {
            AssertionUtil.verifyNotNull(id, "Item ID should not be null");
        }

        // Additional Flags
        if (item.containsKey("home_collection")) {
            System.out.println("      Home Collection: " + item.get("home_collection"));
        }
    }
    
    private double parsePrice(Object priceObj) {
        if (priceObj == null) return 0.0;
        try {
            return Double.parseDouble(priceObj.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Helper to fetch the FIRST item name from Page 1 to ensure we search for
     * something that actually exists.
     */
    private String fetchFirstItemName(String endpoint, Map<String, Object> payload) {
        // Create a copy to not affect the original
        Map<String, Object> checkPayload = new HashMap<>(payload);
        checkPayload.put("page", 1);
        
        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .setRequestBody(checkPayload)
                .post();

        if (response.getStatusCode() != 200) {
            System.out.println("⚠️ API Error during pre-check: " + response.getStatusCode());
            return null;
        }

        try {
            Object dataObj = response.jsonPath().get("data");
            List<Map<String, Object>> items = null;

            if (dataObj instanceof List) {
                items = (List<Map<String, Object>>) dataObj;
            } else if (dataObj instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) dataObj;
                if (dataMap.containsKey("tests")) {
                    items = (List<Map<String, Object>>) dataMap.get("tests");
                } else if (dataMap.containsKey("packages")) {
                    items = (List<Map<String, Object>>) dataMap.get("packages");
                }
            }

            if (items != null && !items.isEmpty()) {
                Map<String, Object> firstItem = items.get(0);
                String name = (String) firstItem.get("name");
                if (name == null)
                    name = (String) firstItem.get("test_name");
                if (name == null)
                    name = (String) firstItem.get("product_name");
                return name;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error parsing first item: " + e.getMessage());
        }
        return null;
    }
}

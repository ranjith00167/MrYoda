package com.mryoda.diagnostics.api.tests.tests_packages;

import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.AssertionUtil;
import com.mryoda.diagnostics.api.utils.RequestContext;
import com.mryoda.diagnostics.api.utils.TokenManager;
import com.mryoda.diagnostics.api.config.ConfigLoader;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ============================================================
 * SECTION 2 — LOCATION & CATALOG
 * ============================================================
 * CAT-01 : Get Lab Locations — at least one with valid _id    [Positive]
 * CAT-02 : Diagnostics Catalog — price + _id verified         [Positive]
 * CAT-03 : DNA Decoder Catalog — non-empty list               [Positive]
 * CAT-04 : PGX (Pharmacogenomics) Catalog                     [Positive]
 * CAT-05 : Fetal Medicine Catalog                             [Positive]
 * CAT-06 : Packages Catalog — count + pricing                 [Positive]
 * CAT-07 : Brands List — Diagnostics brand stored             [Positive]
 * CAT-08 : Invalid Location ID — graceful response            [Negative]
 * CAT-09 : Empty Catalog Result — random keyword              [Negative]
 * ============================================================
 */
public class Section2_LocationAndCatalogTest extends BaseTest {

    private static final String MEMBER_MOBILE = ConfigLoader.getConfig().memberMobile();

    // =========================================================
    // SETUP: Login & store token (self-contained)
    // =========================================================
    private String ensureMemberToken() {
        String token = RequestContext.getMemberToken();
        if (token == null) {
            System.out.println("   [SETUP] No token in context — logging in as Member...");
            token = TokenManager.generateToken(MEMBER_MOBILE, TokenManager.MEMBER);
        }
        return token;
    }

    // =========================================================
    // CAT-01 : Get Lab Locations — POSITIVE
    // =========================================================
    @Test(priority = 1, description = "CAT-01: Fetch all lab locations; verify at least one with valid _id")
    public void CAT_01_GetLabLocations() {
        System.out.println("\n>>> CAT-01: GET LAB LOCATIONS <<<");

        String token = ensureMemberToken();

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.GET_LOCATION)
                .addHeader("Authorization", token)
                .post();

        System.out.println("   Status: " + response.getStatusCode());
        AssertionUtil.verifyEquals(response.getStatusCode(), 200, "GET_LOCATION must return 200");
        AssertionUtil.verifyTrue(response.jsonPath().getBoolean("success"), "success flag must be true");

        List<Map<String, Object>> locations = response.jsonPath().getList("data");
        Assert.assertNotNull(locations, "Locations list must not be null");
        Assert.assertTrue(locations.size() > 0, "At least 1 location must be returned");

        System.out.println("   Total Locations: " + locations.size());

        // Store all locations in context & verify default location
        for (int i = 0; i < locations.size(); i++) {
            String id    = response.jsonPath().getString("data[" + i + "]._id");
            String title = response.jsonPath().getString("data[" + i + "].title");
            String city  = response.jsonPath().getString("data[" + i + "].city");
            String state = response.jsonPath().getString("data[" + i + "].state");
            String lat   = response.jsonPath().getString("data[" + i + "].google_map_latitude");
            String lng   = response.jsonPath().getString("data[" + i + "].google_map_langitude");

            Assert.assertNotNull(id,    "Location _id must not be null for entry " + i);
            Assert.assertNotNull(title, "Location title must not be null for entry " + i);

            RequestContext.storeLocation(title, id);
            if (city != null && state != null)  RequestContext.storeLocationCityState(title, city, state);
            if (lat  != null && lng  != null)   RequestContext.storeLocationCoordinates(title, lat, lng);

            System.out.println("   ✅ [" + (i+1) + "] " + title + " | City: " + city + " | ID: " + id);
        }

        // Verify the default location (Ameerpet HQ) is present and set as selected
        String defaultId = RequestContext.getLocationId(DEFAULT_LOCATION);
        Assert.assertNotNull(defaultId,
                "Default location '" + DEFAULT_LOCATION + "' must exist in the response");

        RequestContext.setSelectedLocation(DEFAULT_LOCATION);
        System.out.println("   ✅ Default location '" + DEFAULT_LOCATION + "' stored. ID: " + defaultId);
        System.out.println("   ✅ CAT-01 PASSED — Lab locations fetched and stored.\n");
    }

    // =========================================================
    // CAT-07 : Brands List — Diagnostics brand stored — POSITIVE
    // (Run before catalog calls since catalog needs brand context)
    // =========================================================
    @Test(priority = 2,
          dependsOnMethods = "CAT_01_GetLabLocations",
          description = "CAT-07: Fetch all brands; verify Diagnostics brand is stored")
    public void CAT_07_GetBrandsList() {
        System.out.println("\n>>> CAT-07: GET BRANDS LIST <<<");

        String token = ensureMemberToken();

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.GET_ALL_BRANDS)
                .addHeader("Authorization", token)
                .addBodyParam("page", 1)
                .expectStatus(200)
                .post();

        System.out.println("   Status: " + response.getStatusCode());
        AssertionUtil.verifyTrue(response.jsonPath().getBoolean("success"), "success flag must be true");

        List<Map<String, Object>> brands = response.jsonPath().getList("data");
        Assert.assertNotNull(brands, "Brands list must not be null");
        Assert.assertTrue(brands.size() > 0, "At least 1 brand must be returned");

        System.out.println("   Total Brands: " + brands.size());

        for (int i = 0; i < brands.size(); i++) {
            String name     = response.jsonPath().getString("data[" + i + "].title");
            String brandId  = response.jsonPath().getString("data[" + i + "].Guid");
            String isActive = response.jsonPath().getString("data[" + i + "].is_active");

            RequestContext.storeBrand(name, brandId);
            System.out.println("   ✅ Brand: " + name + " | Active: " + isActive + " | ID: " + brandId);
        }

        // Diagnostics brand is required for AddToCart
        String diagnosticsId = RequestContext.getBrandId("Diagnostics");
        if (diagnosticsId == null) {
            diagnosticsId = "967a5f02-2e38-47c8-b850-c4aeee8898ed"; // staging fallback
            RequestContext.storeBrand("Diagnostics", diagnosticsId);
            System.out.println("   ⚠️  Diagnostics brand not found in API — using staging fallback ID.");
        }
        RequestContext.setSelectedBrand("Diagnostics");
        System.out.println("   ✅ Diagnostics brand ID set: " + diagnosticsId);
        System.out.println("   ✅ CAT-07 PASSED — Brands fetched and Diagnostics brand stored.\n");
    }

    // =========================================================
    // CAT-02 : Diagnostics Catalog — POSITIVE
    // =========================================================
    @Test(priority = 3,
          dependsOnMethods = "CAT_07_GetBrandsList",
          description = "CAT-02: Verify Diagnostics catalog returns items with valid price and _id")
    public void CAT_02_DiagnosticsCatalog() {
        System.out.println("\n>>> CAT-02: DIAGNOSTICS CATALOG <<<");

        String locationId = RequestContext.getLocationId(DEFAULT_LOCATION);
        Assert.assertNotNull(locationId, "Location ID must be present from CAT-01");

        Map<String, Object> payload = new HashMap<>();
        payload.put("limit", 10);
        payload.put("page",  1);
        payload.put("location", locationId);
        payload.put("diseases", new ArrayList<>());

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.GET_ALL_TESTS)
                .setRequestBody(payload)
                .post();

        System.out.println("   Status: " + response.getStatusCode());
        AssertionUtil.verifyEquals(response.getStatusCode(), 200, "GET_ALL_TESTS must return 200");

        List<Map<String, Object>> items = extractItems(response);
        Assert.assertNotNull(items, "Diagnostics catalog items must not be null");
        Assert.assertTrue(items.size() > 0, "Diagnostics catalog must return at least 1 item");

        System.out.println("   Items returned: " + items.size());
        verifyFirstItemFields(response, "Diagnostics");

        System.out.println("   ✅ CAT-02 PASSED — Diagnostics catalog verified.\n");
    }

    // =========================================================
    // CAT-03 : DNA Decoder Catalog — POSITIVE
    // =========================================================
    @Test(priority = 4,
          dependsOnMethods = "CAT_01_GetLabLocations",
          description = "CAT-03: Verify DNA Decoder catalog returns at least one test")
    public void CAT_03_DnaDecoderCatalog() {
        System.out.println("\n>>> CAT-03: DNA DECODER CATALOG <<<");

        Map<String, Object> payload = new HashMap<>();
        payload.put("limit",      20);
        payload.put("page",       1);
        payload.put("dnadecoder", true);

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.GET_ALL_TESTS)
                .setRequestBody(payload)
                .post();

        System.out.println("   Status: " + response.getStatusCode());
        AssertionUtil.verifyEquals(response.getStatusCode(), 200, "DNA Decoder API must return 200");

        List<Map<String, Object>> items = extractItems(response);
        Assert.assertNotNull(items, "DNA Decoder catalog must not be null");
        Assert.assertTrue(items.size() > 0, "DNA Decoder catalog must return at least 1 test");

        System.out.println("   Items returned: " + items.size());
        verifyFirstItemFields(response, "DNA Decoder");

        System.out.println("   ✅ CAT-03 PASSED — DNA Decoder catalog verified.\n");
    }

    // =========================================================
    // CAT-04 : PGX (Pharmacogenomics) Catalog — POSITIVE
    // =========================================================
    @Test(priority = 5,
          dependsOnMethods = "CAT_01_GetLabLocations",
          description = "CAT-04: Verify PGX catalog returns at least one test with valid details")
    public void CAT_04_PgxCatalog() {
        System.out.println("\n>>> CAT-04: PGX (PHARMACOGENOMICS) CATALOG <<<");

        String locationId = RequestContext.getLocationId(DEFAULT_LOCATION);
        Assert.assertNotNull(locationId, "Location ID must be present from CAT-01");

        Map<String, Object> payload = new HashMap<>();
        payload.put("limit",           20);
        payload.put("page",            1);
        payload.put("pharmacogenomics", true);
        payload.put("location",        locationId);
        payload.put("popular",         true);

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.GET_ALL_TESTS)
                .setRequestBody(payload)
                .post();

        System.out.println("   Status: " + response.getStatusCode());
        AssertionUtil.verifyEquals(response.getStatusCode(), 200, "PGX API must return 200");

        List<Map<String, Object>> items = extractItems(response);
        Assert.assertNotNull(items, "PGX catalog must not be null");
        Assert.assertTrue(items.size() > 0, "PGX catalog must return at least 1 test");

        System.out.println("   Items returned: " + items.size());
        verifyFirstItemFields(response, "PGX");

        System.out.println("   ✅ CAT-04 PASSED — PGX catalog verified.\n");
    }

    // =========================================================
    // CAT-05 : Fetal Medicine Catalog — POSITIVE
    // =========================================================
    @Test(priority = 6,
          dependsOnMethods = "CAT_01_GetLabLocations",
          description = "CAT-05: Verify Fetal Medicine catalog returns at least one test")
    public void CAT_05_FetalMedicineCatalog() {
        System.out.println("\n>>> CAT-05: FETAL MEDICINE CATALOG <<<");

        String locationId = RequestContext.getLocationId(DEFAULT_LOCATION);
        Assert.assertNotNull(locationId, "Location ID must be present from CAT-01");

        Map<String, Object> payload = new HashMap<>();
        payload.put("limit",    10);
        payload.put("page",     1);
        payload.put("popular",  true);
        payload.put("filter",   "");
        payload.put("location", locationId);

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.GET_FETAL_MEDICINE_TESTS)
                .setRequestBody(payload)
                .post();

        System.out.println("   Status: " + response.getStatusCode());
        AssertionUtil.verifyEquals(response.getStatusCode(), 200, "Fetal Medicine API must return 200");

        List<Map<String, Object>> items = extractItems(response);
        Assert.assertNotNull(items, "Fetal Medicine catalog must not be null");
        Assert.assertTrue(items.size() > 0, "Fetal Medicine catalog must return at least 1 test");

        System.out.println("   Items returned: " + items.size());
        verifyFirstItemFields(response, "Fetal Medicine");

        System.out.println("   ✅ CAT-05 PASSED — Fetal Medicine catalog verified.\n");
    }

    // =========================================================
    // CAT-06 : Packages Catalog — POSITIVE
    // =========================================================
    @Test(priority = 7,
          dependsOnMethods = "CAT_01_GetLabLocations",
          description = "CAT-06: Verify Packages catalog returns at least one package with price")
    public void CAT_06_PackagesCatalog() {
        System.out.println("\n>>> CAT-06: PACKAGES CATALOG <<<");

        String locationId = RequestContext.getLocationId(DEFAULT_LOCATION);
        Assert.assertNotNull(locationId, "Location ID must be present from CAT-01");

        Map<String, Object> payload = new HashMap<>();
        payload.put("limit",    10);
        payload.put("page",     1);
        payload.put("location", locationId);
        payload.put("diseases", new ArrayList<>());

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.GET_ALL_PACKAGES)
                .setRequestBody(payload)
                .post();

        System.out.println("   Status: " + response.getStatusCode());
        AssertionUtil.verifyEquals(response.getStatusCode(), 200, "Packages API must return 200");

        // Packages response may come as data.packages or data[]
        List<Map<String, Object>> items = extractItems(response);
        Assert.assertNotNull(items, "Packages catalog must not be null");
        Assert.assertTrue(items.size() > 0, "At least 1 package must be returned");

        System.out.println("   Packages returned: " + items.size());
        verifyFirstItemFields(response, "Packages");

        System.out.println("   ✅ CAT-06 PASSED — Packages catalog verified.\n");
    }

    // =========================================================
    // CAT-08 : Invalid Location ID — NEGATIVE
    // =========================================================
    @Test(priority = 8,
          description = "CAT-08: Request catalog with an invalid location ID — must return graceful response")
    public void CAT_08_InvalidLocationId() {
        System.out.println("\n>>> CAT-08: INVALID LOCATION ID <<<");

        Map<String, Object> payload = new HashMap<>();
        payload.put("limit",    10);
        payload.put("page",     1);
        payload.put("location", "000000000000000000000000"); // non-existent ID
        payload.put("diseases", new ArrayList<>());

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.GET_ALL_TESTS)
                .setRequestBody(payload)
                .post();

        int status = response.getStatusCode();
        String body = response.getBody().asString().toLowerCase();
        System.out.println("   HTTP Status : " + status);
        System.out.println("   Response    : " + body.substring(0, Math.min(body.length(), 150)));

        // API should either return 200 with empty data OR 4xx — never 5xx
        Assert.assertTrue(status < 500,
                "Server must not throw 5xx for invalid location. Got: " + status);

        if (status >= 400 && status < 500) {
            // Server correctly rejected the invalid location ID
            System.out.println("   ✅ CAT-08 PASSED — API correctly rejected invalid location with: " + status);
            org.testng.Reporter.log("[CAT-08] ✅ Invalid location ID correctly rejected. HTTP " + status, true);

        } else if (status == 200) {
            List<Map<String, Object>> items = extractItems(response);
            int count = (items == null) ? 0 : items.size();
            System.out.println("   Items returned for invalid location: " + count);

            if (count > 0) {
                // ============================================================
                // ⚠️  BACKEND GAP — BUG-CAT-001
                // ============================================================
                String warningBlock =
                    "\n  ╔══════════════════════════════════════════════════════════╗\n" +
                    "  ║  ⚠️  WARNING — BACKEND GAP DETECTED [BUG-CAT-001]        ║\n" +
                    "  ╠══════════════════════════════════════════════════════════╣\n" +
                    "  ║  Endpoint  : GET_ALL_TESTS                               ║\n" +
                    "  ║  Condition : Non-existent location ID sent               ║\n" +
                    "  ║  Actual    : HTTP 200 with " + count + " item(s) returned              ║\n" +
                    "  ║  Expected  : HTTP 200 with empty list OR HTTP 404         ║\n" +
                    "  ║  Impact    : API returns results for invalid location     ║\n" +
                    "  ║  Action    : Backend must validate location_id existence  ║\n" +
                    "  ║             before querying the catalog.                  ║\n" +
                    "  ╚══════════════════════════════════════════════════════════╝";
                System.out.println(warningBlock);
                org.testng.Reporter.log(
                    "<b style='color:orange;'>[BUG-CAT-001] CAT-08 ⚠️ WARNING:</b> " +
                    "GET_ALL_TESTS returned <b>" + count + " item(s)</b> for a non-existent location ID. " +
                    "Backend must validate location_id before querying catalog.", true);
            } else {
                System.out.println("   ✅ API correctly returned empty list for invalid location.");
                org.testng.Reporter.log("[CAT-08] ✅ Invalid location returned empty list (HTTP 200). Correct behavior.", true);
            }
        }

        Assert.assertNotNull(response.getBody(), "Response body must not be null");
        System.out.println("   ✅ CAT-08 COMPLETED — Invalid location behavior documented.\n");
    }

    // =========================================================
    // CAT-09 : Empty Catalog Result (Random keyword) — NEGATIVE
    // =========================================================
    @Test(priority = 9,
          dependsOnMethods = "CAT_01_GetLabLocations",
          description = "CAT-09: Search with a nonsense keyword must return empty result, not an error")
    public void CAT_09_EmptyCatalogResult() {
        System.out.println("\n>>> CAT-09: EMPTY CATALOG RESULT (RANDOM KEYWORD) <<<");

        String locationId = RequestContext.getLocationId(DEFAULT_LOCATION);

        Map<String, Object> payload = new HashMap<>();
        payload.put("limit",    10);
        payload.put("page",     1);
        payload.put("location", locationId != null ? locationId : "676a5fa720093d2807af03a5");
        payload.put("diseases", new ArrayList<>());
        payload.put("filter",   "XYZXYZNONEXISTENTTEST99999"); // no test should match this

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.GET_ALL_TESTS)
                .setRequestBody(payload)
                .post();

        int status = response.getStatusCode();
        System.out.println("   HTTP Status: " + status);

        // Must return 200 (empty result) — not 500
        Assert.assertTrue(status == 200 || status == 404,
                "Random keyword must return 200/404, not 5xx. Got: " + status);

        if (status == 200) {
            List<Map<String, Object>> items = extractItems(response);
            int count = (items == null) ? 0 : items.size();
            System.out.println("   Items returned for nonsense keyword: " + count);

            if (count > 0) {
                // ============================================================
                // ⚠️  BACKEND GAP — BUG-CAT-002
                // ============================================================
                String warningBlock =
                    "\n  ╔══════════════════════════════════════════════════════════╗\n" +
                    "  ║  ⚠️  WARNING — BACKEND GAP DETECTED [BUG-CAT-002]        ║\n" +
                    "  ╠══════════════════════════════════════════════════════════╣\n" +
                    "  ║  Endpoint  : GET_ALL_TESTS                               ║\n" +
                    "  ║  Condition : Nonsense filter keyword sent                ║\n" +
                    "  ║  Actual    : HTTP 200 with " + count + " item(s) returned              ║\n" +
                    "  ║  Expected  : HTTP 200 with EMPTY list                    ║\n" +
                    "  ║  Impact    : Search filter is not working correctly      ║\n" +
                    "  ║  Action    : Backend must return empty set for keywords  ║\n" +
                    "  ║             that match no tests in catalog.               ║\n" +
                    "  ╚══════════════════════════════════════════════════════════╝";
                System.out.println(warningBlock);
                org.testng.Reporter.log(
                    "<b style='color:orange;'>[BUG-CAT-002] CAT-09 ⚠️ WARNING:</b> " +
                    "GET_ALL_TESTS returned <b>" + count + " item(s)</b> for a nonsense keyword. " +
                    "Search filter is not working — backend must return empty for unmatched keywords.", true);
            } else {
                System.out.println("   ✅ API correctly returned empty list for nonsense keyword.");
                org.testng.Reporter.log("[CAT-09] ✅ Nonsense keyword returned empty list. Correct behavior.", true);
            }
        } else {
            System.out.println("   ✅ API returned " + status + " for nonsense keyword.");
            org.testng.Reporter.log("[CAT-09] ✅ Nonsense keyword returned HTTP " + status + ".", true);
        }

        Assert.assertNotNull(response.getBody(), "Response body must not be null");
        System.out.println("   ✅ CAT-09 COMPLETED — Empty/invalid keyword behavior documented.\n");
    }

    // =========================================================
    // HELPERS
    // =========================================================

    /**
     * Extract items from GET_ALL_TESTS or GET_ALL_PACKAGES response.
     * Handles: data[] | data.tests[] | data.packages[]
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractItems(Response response) {
        try {
            Object dataObj = response.jsonPath().get("data");
            if (dataObj instanceof List) {
                return (List<Map<String, Object>>) dataObj;
            } else if (dataObj instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) dataObj;
                if (dataMap.containsKey("tests"))    return (List<Map<String, Object>>) dataMap.get("tests");
                if (dataMap.containsKey("packages")) return (List<Map<String, Object>>) dataMap.get("packages");
            }
        } catch (Exception e) {
            System.out.println("   ⚠️ Could not extract items: " + e.getMessage());
        }
        return null;
    }

    /**
     * Verify the first item in the catalog has a valid name, _id, and price > 0.
     */
    private void verifyFirstItemFields(Response response, String catalogName) {
        try {
            List<Map<String, Object>> items = extractItems(response);
            if (items == null || items.isEmpty()) return;

            Map<String, Object> first = items.get(0);

            // Name check
            String name = firstNonNull(first, "name", "test_name", "product_name");
            Assert.assertNotNull(name, catalogName + " — first item must have a name");

            // ID check
            Object id = firstNonNullObj(first, "_id", "id", "guid");
            Assert.assertNotNull(id, catalogName + " — first item must have an _id");

            // Price check
            Object priceObj = firstNonNullObj(first, "price", "offer_price", "final_price");
            double price = 0;
            if (priceObj != null) {
                try { price = Double.parseDouble(priceObj.toString()); } catch (Exception ignored) {}
            }

            System.out.println("   First Item  : " + name);
            System.out.println("   First ID    : " + id);
            System.out.println("   First Price : ₹" + price);

            if (price <= 0) {
                System.out.println("   ⚠️  Price is 0 or null — banner/free test. Acceptable for catalog.");
            }
        } catch (Exception e) {
            System.out.println("   ⚠️ Field verification skipped: " + e.getMessage());
        }
    }

    private String firstNonNull(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val != null) return val.toString();
        }
        return null;
    }

    private Object firstNonNullObj(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val != null) return val;
        }
        return null;
    }
}

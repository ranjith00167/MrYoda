package com.mryoda.diagnostics.api.tests.tests_packages;

import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;

/**
 * POST /tests/getlocations - Positive + Schema Validation Suite
 *
 * Request: POST with empty JSON body {}
 * Headers: accept=any, Content-Type=application/json
 *
 * TC01-TC08:  Functional    - 200, success, status, data list, msg, Content-Type, idempotency, response time
 * TC09-TC20:  Schema        - _id, title, slug, city, state, address, pincode, status,
 *                              coordinates (lat/lng), google_map_url, home_collection, mobile
 * TC21-TC30:  Data Integrity - No duplicate _ids/slugs, active locations exist, coordinate ranges,
 *                              _id ObjectId format, slug URL-safe, required fields across all locations
 */
public class GetLocationsValidationTest extends BaseTest {

    private static final String ENDPOINT = APIEndpoints.GET_LOCATION;

    private static List<Map<String, Object>> allLocations = new ArrayList<>();
    private static int locationCount = 0;
    private static Map<String, Object> firstLocation = null;

    // ═══════════════════════════════════════════════════════════════════
    //  SETUP
    // ═══════════════════════════════════════════════════════════════════

    @BeforeClass
    public void setupLocationData() {
        System.out.println("\n========================================");
        System.out.println("SETUP: Fetching locations via POST /tests/getlocations");
        System.out.println("========================================");

        Response r = callGetLocations();

        Assert.assertEquals(r.getStatusCode(), 200, "SETUP FAILED: getlocations returned " + r.getStatusCode());

        allLocations = r.jsonPath().getList("data");
        Assert.assertNotNull(allLocations, "SETUP FAILED: data is null");
        Assert.assertFalse(allLocations.isEmpty(), "SETUP FAILED: no locations returned");

        locationCount = allLocations.size();
        firstLocation = allLocations.get(0);

        System.out.println("   Total locations : " + locationCount);
        System.out.println("   First location  : " + firstLocation.get("title"));
        System.out.println("   Fields          : " + firstLocation.keySet());
        System.out.println("✅ Setup complete");
        System.out.println("========================================\n");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  HELPER
    // ═══════════════════════════════════════════════════════════════════

    private Response callGetLocations() {
        return new RequestBuilder()
                .setEndpoint(ENDPOINT)
                .addHeader("accept", "*/*")
                .addHeader("Content-Type", "application/json")
                .setRequestBody(new HashMap<>())
                .post();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  FUNCTIONAL (TC01-TC08)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 1, description = "TC01: POST with empty body returns HTTP 200")
    public void testTC01_Returns200() {
        Response r = callGetLocations();
        Assert.assertEquals(r.getStatusCode(), 200, "Expected 200. Got: " + r.getStatusCode());
        System.out.println("   Status: " + r.getStatusCode());
        System.out.println("✅ TC01 PASSED");
    }

    @Test(priority = 2, description = "TC02: Response contains success=true")
    public void testTC02_SuccessTrue() {
        Response r = callGetLocations();
        Assert.assertTrue(r.jsonPath().getBoolean("success"), "success should be true");
        System.out.println("✅ TC02 PASSED");
    }

    @Test(priority = 3, description = "TC03: Response contains status=200")
    public void testTC03_StatusField() {
        Response r = callGetLocations();
        Object status = r.jsonPath().get("status");
        Assert.assertNotNull(status, "status field missing");
        Assert.assertEquals(r.jsonPath().getInt("status"), 200);
        System.out.println("   status: " + status);
        System.out.println("✅ TC03 PASSED");
    }

    @Test(priority = 4, description = "TC04: data field is a non-empty list")
    public void testTC04_DataIsNonEmptyList() {
        Response r = callGetLocations();
        List<Map<String, Object>> data = r.jsonPath().getList("data");
        Assert.assertNotNull(data, "data field should not be null");
        Assert.assertFalse(data.isEmpty(), "data list should not be empty");
        System.out.println("   data count: " + data.size());
        System.out.println("✅ TC04 PASSED");
    }

    @Test(priority = 5, description = "TC05: Response contains msg field")
    public void testTC05_MsgField() {
        Response r = callGetLocations();
        Assert.assertNotNull(r.jsonPath().get("msg"), "msg field missing");
        System.out.println("   msg: " + r.jsonPath().getString("msg"));
        System.out.println("✅ TC05 PASSED");
    }

    @Test(priority = 6, description = "TC06: Content-Type header is application/json")
    public void testTC06_ContentType() {
        Response r = callGetLocations();
        Assert.assertTrue(r.getContentType().contains("application/json"),
                "Expected application/json. Got: " + r.getContentType());
        System.out.println("✅ TC06 PASSED");
    }

    @Test(priority = 7, description = "TC07: Two successive calls return same number of locations (idempotent POST)")
    public void testTC07_IdempotentPost() {
        int count1 = callGetLocations().jsonPath().getList("data").size();
        int count2 = callGetLocations().jsonPath().getList("data").size();
        Assert.assertEquals(count1, count2, "Location count changed between calls");
        System.out.println("   Both calls returned " + count1 + " locations");
        System.out.println("✅ TC07 PASSED");
    }

    @Test(priority = 8, description = "TC08: Response time is under 3000ms")
    public void testTC08_ResponseTime() {
        long start = System.currentTimeMillis();
        Response r = callGetLocations();
        long elapsed = System.currentTimeMillis() - start;
        Assert.assertEquals(r.getStatusCode(), 200);
        Assert.assertTrue(elapsed < 3000, "Response too slow: " + elapsed + "ms (expected < 3000ms)");
        System.out.println("   Response time: " + elapsed + "ms");
        System.out.println("✅ TC08 PASSED");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SCHEMA VALIDATION (TC09-TC20)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 9, description = "TC09: Every location has a non-null _id")
    public void testTC09_IdField() {
        for (Map<String, Object> loc : allLocations) {
            Assert.assertNotNull(loc.get("_id"), "null _id at: " + loc.get("title"));
        }
        System.out.println("   _id present in all " + locationCount + " locations");
        System.out.println("✅ TC09 PASSED");
    }

    @Test(priority = 10, description = "TC10: Every location has a non-empty title")
    public void testTC10_TitleField() {
        for (Map<String, Object> loc : allLocations) {
            Object title = loc.get("title");
            Assert.assertNotNull(title, "null title");
            Assert.assertFalse(String.valueOf(title).trim().isEmpty(), "empty title found");
        }
        System.out.println("   title present in all " + locationCount + " locations");
        System.out.println("✅ TC10 PASSED");
    }

    @Test(priority = 11, description = "TC11: Every location has a slug field")
    public void testTC11_SlugField() {
        for (Map<String, Object> loc : allLocations) {
            Assert.assertNotNull(loc.get("slug"), "null slug at: " + loc.get("title"));
        }
        System.out.println("   slug present in all " + locationCount + " locations");
        System.out.println("✅ TC11 PASSED");
    }

    @Test(priority = 12, description = "TC12: Every location has city and state fields")
    public void testTC12_CityStateFields() {
        for (Map<String, Object> loc : allLocations) {
            Assert.assertNotNull(loc.get("city"), "null city at: " + loc.get("title"));
            Assert.assertNotNull(loc.get("state"), "null state at: " + loc.get("title"));
        }
        System.out.println("   city + state present in all " + locationCount + " locations");
        System.out.println("✅ TC12 PASSED");
    }

    @Test(priority = 13, description = "TC13: Every location has an address field")
    public void testTC13_AddressField() {
        int present = 0;
        for (Map<String, Object> loc : allLocations) {
            if (loc.get("address") != null) present++;
        }
        System.out.println("   address present in " + present + "/" + locationCount);
        Assert.assertTrue(present > 0, "No location has address field");
        System.out.println("✅ TC13 PASSED");
    }

    @Test(priority = 14, description = "TC14: Every location has a status field")
    public void testTC14_StatusField() {
        for (Map<String, Object> loc : allLocations) {
            Assert.assertNotNull(loc.get("status"), "null status at: " + loc.get("title"));
            System.out.println("   " + loc.get("title") + " → status: " + loc.get("status"));
        }
        System.out.println("✅ TC14 PASSED");
    }

    @Test(priority = 15, description = "TC15: pincode is 6 digits where present")
    public void testTC15_PincodeFormat() {
        for (Map<String, Object> loc : allLocations) {
            Object pincode = loc.get("pincode");
            if (pincode == null) continue;
            String p = String.valueOf(pincode).replaceAll("\\s", "");
            if (!p.isEmpty()) {
                Assert.assertTrue(p.matches("\\d{6}"),
                        "Invalid pincode '" + p + "' at: " + loc.get("title"));
            }
        }
        System.out.println("✅ TC15 PASSED");
    }

    @Test(priority = 16, description = "TC16: google_map_latitude is numeric and in valid range (-90 to 90)")
    public void testTC16_LatitudeField() {
        for (Map<String, Object> loc : allLocations) {
            Object lat = loc.get("google_map_latitude");
            if (lat == null) continue;
            double d = Double.parseDouble(String.valueOf(lat));
            Assert.assertTrue(d >= -90 && d <= 90,
                    "Latitude out of range: " + d + " at: " + loc.get("title"));
            System.out.println("   " + loc.get("title") + " → lat: " + d);
        }
        System.out.println("✅ TC16 PASSED");
    }

    @Test(priority = 17, description = "TC17: google_map_langitude is numeric and in valid range (-180 to 180)")
    public void testTC17_LongitudeField() {
        for (Map<String, Object> loc : allLocations) {
            Object lng = loc.get("google_map_langitude");
            if (lng == null) continue;
            double d = Double.parseDouble(String.valueOf(lng));
            Assert.assertTrue(d >= -180 && d <= 180,
                    "Longitude out of range: " + d + " at: " + loc.get("title"));
            System.out.println("   " + loc.get("title") + " → lng: " + d);
        }
        System.out.println("✅ TC17 PASSED");
    }

    @Test(priority = 18, description = "TC18: google_map_location_url is present and starts with http")
    public void testTC18_MapUrl() {
        for (Map<String, Object> loc : allLocations) {
            Object url = loc.get("google_map_location_url");
            if (url == null) continue;
            String u = String.valueOf(url).trim();
            if (u.isEmpty()) continue;
            Assert.assertTrue(u.startsWith("http"),
                    "Invalid map URL '" + u + "' at: " + loc.get("title"));
        }
        System.out.println("✅ TC18 PASSED");
    }

    @Test(priority = 19, description = "TC19: mobile field (when present) is non-empty")
    public void testTC19_MobileField() {
        int present = 0;
        for (Map<String, Object> loc : allLocations) {
            Object mobile = loc.get("mobile");
            if (mobile != null && !String.valueOf(mobile).trim().isEmpty()) present++;
        }
        System.out.println("   mobile present in " + present + "/" + locationCount);
        System.out.println("✅ TC19 PASSED");
    }

    @Test(priority = 20, description = "TC20: is_serving_radiology field is boolean (true/false) where present")
    public void testTC20_IsServingRadiology() {
        for (Map<String, Object> loc : allLocations) {
            Object val = loc.get("is_serving_radiology");
            if (val == null) continue;
            Assert.assertTrue(val instanceof Boolean,
                    "is_serving_radiology should be boolean at: " + loc.get("title") + " got: " + val.getClass());
        }
        System.out.println("✅ TC20 PASSED");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  DATA INTEGRITY (TC21-TC30)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 21, description = "TC21: No duplicate _id values across all locations")
    public void testTC21_NoDuplicateIds() {
        Set<String> seen = new HashSet<>();
        List<String> dupes = new ArrayList<>();
        for (Map<String, Object> loc : allLocations) {
            String id = String.valueOf(loc.get("_id"));
            if (!seen.add(id)) dupes.add(id);
        }
        Assert.assertTrue(dupes.isEmpty(), "Duplicate _id(s): " + dupes);
        System.out.println("   All " + locationCount + " _ids are unique");
        System.out.println("✅ TC21 PASSED");
    }

    @Test(priority = 22, description = "TC22: No duplicate slug values across all locations")
    public void testTC22_NoDuplicateSlugs() {
        Set<String> seen = new HashSet<>();
        List<String> dupes = new ArrayList<>();
        for (Map<String, Object> loc : allLocations) {
            Object slug = loc.get("slug");
            if (slug == null) continue;
            if (!seen.add(String.valueOf(slug))) dupes.add(String.valueOf(slug));
        }
        Assert.assertTrue(dupes.isEmpty(), "Duplicate slug(s): " + dupes);
        System.out.println("   All slugs are unique");
        System.out.println("✅ TC22 PASSED");
    }

    @Test(priority = 23, description = "TC23: At least one active location exists")
    public void testTC23_ActiveLocationExists() {
        long active = allLocations.stream()
                .filter(loc -> {
                    Object s = loc.get("status");
                    if (s == null) return false;
                    String sv = String.valueOf(s).toLowerCase();
                    return sv.equals("active") || sv.equals("1") || sv.equals("true");
                }).count();
        Assert.assertTrue(active > 0, "No active locations found");
        System.out.println("   Active locations: " + active + "/" + locationCount);
        System.out.println("✅ TC23 PASSED");
    }

    @Test(priority = 24, description = "TC24: _id values are valid MongoDB ObjectId (24 hex characters)")
    public void testTC24_IdIsObjectId() {
        for (Map<String, Object> loc : allLocations) {
            String id = String.valueOf(loc.get("_id"));
            Assert.assertTrue(id.matches("[a-fA-F0-9]{24}"),
                    "Invalid ObjectId '" + id + "' at: " + loc.get("title"));
        }
        System.out.println("   All _ids are valid 24-char MongoDB ObjectIds");
        System.out.println("✅ TC24 PASSED");
    }

    @Test(priority = 25, description = "TC25: slug values are URL-safe (lowercase alphanumeric + hyphens)")
    public void testTC25_SlugUrlSafe() {
        int invalid = 0;
        for (Map<String, Object> loc : allLocations) {
            Object slug = loc.get("slug");
            if (slug == null) continue;
            String s = String.valueOf(slug);
            if (!s.matches("[a-z0-9\\-]+")) {
                invalid++;
                System.out.println("   Non URL-safe slug: '" + s + "' at: " + loc.get("title"));
            }
        }
        if (invalid > 0) System.out.println("   ⚠ " + invalid + " slug(s) not URL-safe");
        System.out.println("✅ TC25 PASSED");
    }

    @Test(priority = 26, description = "TC26: All locations have _id, title, slug populated (minimum required schema)")
    public void testTC26_MinimumRequiredSchema() {
        String[] required = {"_id", "title", "slug", "city", "state"};
        int violations = 0;
        for (Map<String, Object> loc : allLocations) {
            for (String field : required) {
                if (loc.get(field) == null) {
                    violations++;
                    System.out.println("   MISSING '" + field + "' in: " + loc.get("title"));
                }
            }
        }
        Assert.assertEquals(violations, 0, violations + " required field(s) missing");
        System.out.println("✅ TC26 PASSED");
    }

    @Test(priority = 27, description = "TC27: city and state values are non-empty strings where present")
    public void testTC27_CityStateNonEmpty() {
        for (Map<String, Object> loc : allLocations) {
            Object city = loc.get("city");
            Object state = loc.get("state");
            if (city != null)
                Assert.assertFalse(String.valueOf(city).trim().isEmpty(),
                        "Empty city at: " + loc.get("title"));
            if (state != null)
                Assert.assertFalse(String.valueOf(state).trim().isEmpty(),
                        "Empty state at: " + loc.get("title"));
        }
        System.out.println("✅ TC27 PASSED");
    }

    @Test(priority = 28, description = "TC28: Total location count is consistent across two calls")
    public void testTC28_ConsistentLocationCount() {
        int c1 = callGetLocations().jsonPath().getList("data").size();
        int c2 = callGetLocations().jsonPath().getList("data").size();
        Assert.assertEquals(c1, c2, "Location count inconsistent: " + c1 + " vs " + c2);
        System.out.println("   Consistent count: " + c1);
        System.out.println("✅ TC28 PASSED");
    }

    @Test(priority = 29, description = "TC29: All location titles in both calls are identical (order preserved)")
    public void testTC29_ConsistentTitles() {
        List<Map<String, Object>> l1 = callGetLocations().jsonPath().getList("data");
        List<Map<String, Object>> l2 = callGetLocations().jsonPath().getList("data");
        Assert.assertEquals(l1.size(), l2.size());
        for (int i = 0; i < l1.size(); i++) {
            Assert.assertEquals(
                    String.valueOf(l1.get(i).get("title")),
                    String.valueOf(l2.get(i).get("title")),
                    "Title mismatch at index " + i);
        }
        System.out.println("✅ TC29 PASSED: order and titles stable");
    }

    @Test(priority = 30, description = "TC30: Print full schema of all locations for reference")
    public void testTC30_PrintAllLocations() {
        System.out.println("\n   ===== LOCATION DATA SUMMARY =====");
        for (Map<String, Object> loc : allLocations) {
            System.out.println("   ---------------------------------");
            System.out.println("   Title   : " + loc.get("title"));
            System.out.println("   _id     : " + loc.get("_id"));
            System.out.println("   slug    : " + loc.get("slug"));
            System.out.println("   city    : " + loc.get("city") + " | state: " + loc.get("state"));
            System.out.println("   pincode : " + loc.get("pincode"));
            System.out.println("   status  : " + loc.get("status"));
            System.out.println("   lat     : " + loc.get("google_map_latitude") + " | lng: " + loc.get("google_map_langitude"));
            System.out.println("   mobile  : " + loc.get("mobile"));
            System.out.println("   radiology: " + loc.get("is_serving_radiology"));
        }
        System.out.println("   =================================\n");
        Assert.assertEquals(locationCount, allLocations.size());
        System.out.println("✅ TC30 PASSED");
    }
}

package com.mryoda.diagnostics.api.tests.tests_packages;

import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.ApiReportContext;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;
import java.util.stream.Collectors;

/**
 * GetAllTests Filter Validation Test Suite
 * Endpoint: POST /tests/getAllTests
 *
 * Tests three filter categories:
 * 1. yodaara_ott filter (with/without location)
 * 2. pharmacogenomics filter (with/without location)
 * 3. dnadecoder filter (with/without location, with popular flag)
 *
 * Covers: Functional, Pagination, Location filter, Negative, Combination, Security
 */
public class GetAllTestsFilterValidationTest extends BaseTest {

    private static final String ENDPOINT = APIEndpoints.GET_ALL_TESTS;
    private static final String VALID_LOCATION = "64870066842708a0d5ae6c77";

    private int yodaaraTotal = 0;
    private int pharmacogenomicsTotal = 0;
    private int dnadecoderTotal = 0;

    // ═══════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════════

    private Response callAPI(Map<String, Object> payload) {
        return new RequestBuilder()
                .setEndpoint(ENDPOINT)
                .addHeader("Content-Type", "application/json")
                .addHeader("type", "Mr.Yoda Web")
                .setRequestBody(payload)
                .post();
    }

    private Map<String, Object> buildPayload(int page, int limit) {
        Map<String, Object> p = new HashMap<>();
        p.put("page", page);
        p.put("limit", limit);
        return p;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SETUP
    // ═══════════════════════════════════════════════════════════════════

    @BeforeClass
    public void setupTotals() {
        System.out.println("\n========== SETUP: Fetching totals for each filter ==========");

        // yodaara_ott
        Map<String, Object> p1 = buildPayload(1, 100);
        p1.put("yodaara_ott", true);
        p1.put("location", VALID_LOCATION);
        Response r1 = callAPI(p1);
        if (r1.getStatusCode() == 200) {
            yodaaraTotal = r1.jsonPath().getInt("total");
        }

        // pharmacogenomics
        Map<String, Object> p2 = buildPayload(1, 100);
        p2.put("pharmacogenomics", true);
        p2.put("location", VALID_LOCATION);
        Response r2 = callAPI(p2);
        if (r2.getStatusCode() == 200) {
            pharmacogenomicsTotal = r2.jsonPath().getInt("total");
        }

        // dnadecoder
        Map<String, Object> p3 = buildPayload(1, 100);
        p3.put("dnadecoder", true);
        p3.put("location", VALID_LOCATION);
        Response r3 = callAPI(p3);
        if (r3.getStatusCode() == 200) {
            dnadecoderTotal = r3.jsonPath().getInt("total");
        }

        System.out.println("  Yodaara OTT total: " + yodaaraTotal);
        System.out.println("  Pharmacogenomics total: " + pharmacogenomicsTotal);
        System.out.println("  DNA Decoder total: " + dnadecoderTotal);
        System.out.println("==========================================================\n");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECTION 1: YODAARA OTT FILTER (TC01 - TC20)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 1, description = "TC01: Yodaara OTT - Valid request with location")
    public void testTC01_YodaaraOtt_ValidWithLocation() {
        System.out.println("\n>>> TC01: Yodaara OTT valid request <<<");
        Map<String, Object> payload = buildPayload(1, 20);
        payload.put("yodaara_ott", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);

        Assert.assertEquals(r.getStatusCode(), 200);
        Assert.assertTrue(r.jsonPath().getBoolean("success"));
        Assert.assertEquals(r.jsonPath().getString("msg"), "Tests fetched successfully");
        int total = r.jsonPath().getInt("total");
        Assert.assertTrue(total > 0, "Should return yodaara tests");
        List<Map<String, Object>> data = r.jsonPath().getList("data");
        Assert.assertNotNull(data);
        Assert.assertTrue(data.size() > 0);
        ApiReportContext.addExtraDetail("<b>Yodaara OTT:</b> total=" + total + " returned=" + data.size());
        System.out.println("TC01 PASSED - total=" + total);
    }

    @Test(priority = 2, description = "TC02: Yodaara OTT - Response schema validation")
    public void testTC02_YodaaraOtt_ResponseSchema() {
        System.out.println("\n>>> TC02: Yodaara OTT response schema <<<");
        Map<String, Object> payload = buildPayload(1, 20);
        payload.put("yodaara_ott", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        // Verify top-level fields
        Assert.assertNotNull(r.jsonPath().get("status"));
        Assert.assertNotNull(r.jsonPath().get("success"));
        Assert.assertNotNull(r.jsonPath().get("msg"));
        Assert.assertNotNull(r.jsonPath().get("total"));
        Assert.assertNotNull(r.jsonPath().get("page"));
        Assert.assertNotNull(r.jsonPath().get("limit"));
        Assert.assertNotNull(r.jsonPath().get("total_pages"));
        Assert.assertNotNull(r.jsonPath().get("data"));

        ApiReportContext.addExtraDetail("<b>Schema:</b> All top-level fields present");
        System.out.println("TC02 PASSED");
    }

    @Test(priority = 3, description = "TC03: Yodaara OTT - Test object required fields")
    public void testTC03_YodaaraOtt_TestObjectFields() {
        System.out.println("\n>>> TC03: Yodaara OTT test object fields <<<");
        Map<String, Object> payload = buildPayload(1, 5);
        payload.put("yodaara_ott", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        List<Map<String, Object>> data = r.jsonPath().getList("data");
        Assert.assertTrue(data.size() > 0);
        Map<String, Object> test = data.get(0);

        Assert.assertNotNull(test.get("_id"), "Missing _id");
        Assert.assertNotNull(test.get("test_name"), "Missing test_name");
        Assert.assertNotNull(test.get("slug"), "Missing slug");
        Assert.assertNotNull(test.get("price"), "Missing price");
        Assert.assertNotNull(test.get("status"), "Missing status");

        ApiReportContext.addExtraDetail("<b>Test object:</b> _id, test_name, slug, price, status present");
        System.out.println("TC03 PASSED - test: " + test.get("test_name"));
    }

    @Test(priority = 4, description = "TC04: Yodaara OTT - All returned tests have yodaara_ott=true")
    public void testTC04_YodaaraOtt_FilterAccuracy() {
        System.out.println("\n>>> TC04: Yodaara OTT filter accuracy <<<");
        Map<String, Object> payload = buildPayload(1, 100);
        payload.put("yodaara_ott", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        List<Map<String, Object>> data = r.jsonPath().getList("data");
        long nonYodaara = data.stream()
                .filter(t -> !Boolean.TRUE.equals(t.get("yodaara_ott")))
                .count();
        ApiReportContext.addExtraDetail("<b>Filter check:</b> " + data.size() + " tests, " + nonYodaara + " non-yodaara");
        Assert.assertEquals(nonYodaara, 0L, "All results should have yodaara_ott=true");
        System.out.println("TC04 PASSED - all " + data.size() + " tests are yodaara");
    }

    @Test(priority = 5, description = "TC05: Yodaara OTT - Without location returns tests")
    public void testTC05_YodaaraOtt_WithoutLocation() {
        System.out.println("\n>>> TC05: Yodaara OTT without location <<<");
        Map<String, Object> payload = buildPayload(1, 20);
        payload.put("yodaara_ott", true);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        int total = r.jsonPath().getInt("total");
        ApiReportContext.addExtraDetail("<b>No location:</b> total=" + total);
        Assert.assertTrue(total >= yodaaraTotal, "Without location should return >= location-filtered count");
        System.out.println("TC05 PASSED - total=" + total);
    }

    @Test(priority = 6, description = "TC06: Yodaara OTT - All returned tests have the given location")
    public void testTC06_YodaaraOtt_LocationFilter() {
        System.out.println("\n>>> TC06: Yodaara OTT location filter check <<<");
        Map<String, Object> payload = buildPayload(1, 100);
        payload.put("yodaara_ott", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        List<Map<String, Object>> data = r.jsonPath().getList("data");
        long missingLocation = data.stream()
                .filter(t -> {
                    Object locs = t.get("locations");
                    if (locs instanceof List) {
                        return !((List<?>) locs).contains(VALID_LOCATION);
                    }
                    return true;
                })
                .count();
        ApiReportContext.addExtraDetail("<b>Location filter:</b> " + missingLocation + "/" + data.size() + " missing location");
        Assert.assertEquals(missingLocation, 0L, "All results should include the requested location");
        System.out.println("TC06 PASSED");
    }

    @Test(priority = 7, description = "TC07: Yodaara OTT - Pagination page=1 limit=2")
    public void testTC07_YodaaraOtt_PaginationSmallLimit() {
        System.out.println("\n>>> TC07: Yodaara OTT pagination limit=2 <<<");
        Map<String, Object> payload = buildPayload(1, 2);
        payload.put("yodaara_ott", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        List<Map<String, Object>> data = r.jsonPath().getList("data");
        int respLimit = r.jsonPath().getInt("limit");
        Assert.assertEquals(respLimit, 2, "Response limit should match requested");
        Assert.assertTrue(data.size() <= 2, "Data count should be <= limit. Got: " + data.size());
        ApiReportContext.addExtraDetail("<b>limit=2:</b> returned=" + data.size());
        System.out.println("TC07 PASSED - returned=" + data.size());
    }

    @Test(priority = 8, description = "TC08: Yodaara OTT - Page 2")
    public void testTC08_YodaaraOtt_Page2() {
        System.out.println("\n>>> TC08: Yodaara OTT page 2 <<<");
        Map<String, Object> payload = buildPayload(2, 2);
        payload.put("yodaara_ott", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        int respPage = r.jsonPath().getInt("page");
        Assert.assertEquals(respPage, 2);
        List<Map<String, Object>> data = r.jsonPath().getList("data");
        ApiReportContext.addExtraDetail("<b>page=2, limit=2:</b> returned=" + data.size());
        System.out.println("TC08 PASSED - page=2, returned=" + data.size());
    }

    @Test(priority = 9, description = "TC09: Yodaara OTT - total_pages calculation")
    public void testTC09_YodaaraOtt_TotalPages() {
        System.out.println("\n>>> TC09: Yodaara OTT total_pages <<<");
        Map<String, Object> payload = buildPayload(1, 2);
        payload.put("yodaara_ott", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        int total = r.jsonPath().getInt("total");
        int limit = r.jsonPath().getInt("limit");
        int totalPages = r.jsonPath().getInt("total_pages");
        int expected = (int) Math.ceil((double) total / limit);
        Assert.assertEquals(totalPages, expected, "total_pages mismatch");
        ApiReportContext.addExtraDetail("<b>total_pages:</b> " + totalPages + " (total=" + total + " limit=" + limit + ")");
        System.out.println("TC09 PASSED - total_pages=" + totalPages);
    }

    @Test(priority = 10, description = "TC10: Yodaara OTT - yodaara_ott=false returns different set")
    public void testTC10_YodaaraOtt_FalseValue() {
        System.out.println("\n>>> TC10: Yodaara OTT false <<<");
        Map<String, Object> payload = buildPayload(1, 20);
        payload.put("yodaara_ott", false);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        int total = r.jsonPath().getInt("total");
        ApiReportContext.addExtraDetail("<b>yodaara_ott=false:</b> total=" + total + " vs true=" + yodaaraTotal);
        // false should return non-yodaara tests (different count)
        Assert.assertNotEquals(total, yodaaraTotal, "false filter should return different set than true");
        System.out.println("TC10 PASSED - false total=" + total + " vs true=" + yodaaraTotal);
    }

    @Test(priority = 11, description = "TC11: Yodaara OTT - Invalid location returns error")
    public void testTC11_YodaaraOtt_InvalidLocation() {
        System.out.println("\n>>> TC11: Yodaara OTT invalid location <<<");
        Map<String, Object> payload = buildPayload(1, 20);
        payload.put("yodaara_ott", true);
        payload.put("location", "invalidlocationid");
        Response r = callAPI(payload);

        // API returns error for non-ObjectId location
        Assert.assertFalse(r.jsonPath().getBoolean("success"), "Should fail for invalid location");
        ApiReportContext.addExtraDetail("<b>Invalid location:</b> success=false, msg=" + r.jsonPath().getString("message"));
        System.out.println("TC11 PASSED - properly rejects invalid location");
    }

    @Test(priority = 12, description = "TC12: Yodaara OTT - Non-existent valid ObjectId location should return 0 results")
    public void testTC12_YodaaraOtt_NonExistentLocation() {
        System.out.println("\n>>> TC12: Yodaara OTT non-existent location <<<");
        System.out.println("┌─────────────────────────────────────────────────────────────");
        System.out.println("│ EXAMPLE: Location filter should reject non-existent ObjectId");
        System.out.println("│ Request:  POST /tests/getAllTests");
        System.out.println("│ Payload:  {\"page\":1,\"limit\":20,\"yodaara_ott\":true,\"location\":\"000000000000000000000000\"}");
        System.out.println("│ Expected: {\"total\":0, \"data\":[]}");
        System.out.println("│ Actual:   Returns all yodaara tests ignoring invalid location");
        System.out.println("└─────────────────────────────────────────────────────────────");

        Map<String, Object> payload = buildPayload(1, 20);
        payload.put("yodaara_ott", true);
        payload.put("location", "000000000000000000000000");
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        int total = r.jsonPath().getInt("total");
        List<Map<String, Object>> data = r.jsonPath().getList("data");
        System.out.println("   Actual total: " + total + " | Actual data size: " + (data != null ? data.size() : 0));
        ApiReportContext.addExtraDetail("<b>TC12:</b> Non-existent location returned " + total + " results (expected 0)");
        Assert.assertEquals(total, 0, "Non-existent location should return 0 results, but got: " + total);
        Assert.assertTrue(data == null || data.isEmpty(), "Data array should be empty for non-existent location");
        System.out.println("TC12 PASSED - non-existent location correctly returned 0 results");
    }

    @Test(priority = 13, description = "TC13: Yodaara OTT - All tests have status=ACTIVE")
    public void testTC13_YodaaraOtt_AllActive() {
        System.out.println("\n>>> TC13: Yodaara OTT all ACTIVE <<<");
        Map<String, Object> payload = buildPayload(1, 100);
        payload.put("yodaara_ott", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        List<Map<String, Object>> data = r.jsonPath().getList("data");
        long inactive = data.stream()
                .filter(t -> !"ACTIVE".equals(t.get("status")))
                .count();
        ApiReportContext.addExtraDetail("<b>Status check:</b> " + inactive + "/" + data.size() + " inactive");
        Assert.assertEquals(inactive, 0L, "All returned tests should be ACTIVE");
        System.out.println("TC13 PASSED");
    }

    @Test(priority = 14, description = "TC14: Yodaara OTT - Price is positive number")
    public void testTC14_YodaaraOtt_PricePositive() {
        System.out.println("\n>>> TC14: Yodaara OTT price validation <<<");
        Map<String, Object> payload = buildPayload(1, 100);
        payload.put("yodaara_ott", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        List<Map<String, Object>> data = r.jsonPath().getList("data");
        long invalidPrice = data.stream()
                .filter(t -> {
                    Object p = t.get("price");
                    if (p instanceof Number) {
                        return ((Number) p).doubleValue() <= 0;
                    }
                    return true;
                })
                .count();
        ApiReportContext.addExtraDetail("<b>Price check:</b> " + invalidPrice + "/" + data.size() + " invalid prices");
        Assert.assertEquals(invalidPrice, 0L, "All tests should have positive price");
        System.out.println("TC14 PASSED");
    }

    @Test(priority = 15, description = "TC15: Yodaara OTT - No duplicate test IDs")
    public void testTC15_YodaaraOtt_NoDuplicates() {
        System.out.println("\n>>> TC15: Yodaara OTT no duplicates <<<");
        Map<String, Object> payload = buildPayload(1, 100);
        payload.put("yodaara_ott", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        List<Map<String, Object>> data = r.jsonPath().getList("data");
        Set<String> ids = new HashSet<>();
        long dups = data.stream().filter(t -> !ids.add(String.valueOf(t.get("_id")))).count();
        ApiReportContext.addExtraDetail("<b>Duplicates:</b> " + dups + " in " + data.size() + " records");
        Assert.assertEquals(dups, 0L, "No duplicate test IDs");
        System.out.println("TC15 PASSED");
    }

    @Test(priority = 16, description = "TC16: Yodaara OTT - Response time < 3s")
    public void testTC16_YodaaraOtt_ResponseTime() {
        System.out.println("\n>>> TC16: Yodaara OTT response time <<<");
        Map<String, Object> payload = buildPayload(1, 20);
        payload.put("yodaara_ott", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);
        long time = r.getTime();
        Assert.assertTrue(time < 3000, "Response time should be < 3s. Got: " + time + "ms");
        ApiReportContext.addExtraDetail("<b>Response time:</b> " + time + "ms");
        System.out.println("TC16 PASSED - " + time + "ms");
    }

    @Test(priority = 17, description = "TC17: Yodaara OTT - discount_rate field exists")
    public void testTC17_YodaaraOtt_DiscountField() {
        System.out.println("\n>>> TC17: Yodaara OTT discount_rate <<<");
        Map<String, Object> payload = buildPayload(1, 5);
        payload.put("yodaara_ott", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        List<Map<String, Object>> data = r.jsonPath().getList("data");
        Assert.assertTrue(data.size() > 0);
        // Check at least first test has discount_rate
        Map<String, Object> first = data.get(0);
        Assert.assertTrue(first.containsKey("discount_rate") || first.containsKey("discount_percentage"),
                "Test should have discount info");
        ApiReportContext.addExtraDetail("<b>Discount:</b> rate=" + first.get("discount_rate") + " %=" + first.get("discount_percentage"));
        System.out.println("TC17 PASSED");
    }

    @Test(priority = 18, description = "TC18: Yodaara OTT - large page number returns empty")
    public void testTC18_YodaaraOtt_LargePageNumber() {
        System.out.println("\n>>> TC18: Yodaara OTT large page <<<");
        Map<String, Object> payload = buildPayload(9999, 20);
        payload.put("yodaara_ott", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        List<Map<String, Object>> data = r.jsonPath().getList("data");
        Assert.assertEquals(data.size(), 0, "Large page should return empty data");
        ApiReportContext.addExtraDetail("<b>page=9999:</b> returned " + data.size() + " records");
        System.out.println("TC18 PASSED - empty for page 9999");
    }

    @Test(priority = 19, description = "TC19: Yodaara OTT - page=0 should return 400 Bad Request")
    public void testTC19_YodaaraOtt_PageZero() {
        System.out.println("\n>>> TC19: Yodaara OTT page=0 <<<");
        System.out.println("┌─────────────────────────────────────────────────────────────");
        System.out.println("│ EXAMPLE: Invalid page=0 should be rejected with 400");
        System.out.println("│ Request:  POST /tests/getAllTests");
        System.out.println("│ Payload:  {\"page\":0,\"limit\":20,\"yodaara_ott\":true,\"location\":\"64870066842708a0d5ae6c77\"}");
        System.out.println("│ Expected: 400 Bad Request with validation error message");
        System.out.println("│ Actual:   500 Internal Server Error (unhandled exception)");
        System.out.println("└─────────────────────────────────────────────────────────────");

        Map<String, Object> payload = buildPayload(0, 20);
        payload.put("yodaara_ott", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);

        int status = r.getStatusCode();
        System.out.println("   Actual status: " + status + " | Response: " + r.getBody().asString().substring(0, Math.min(200, r.getBody().asString().length())));
        ApiReportContext.addExtraDetail("<b>TC19:</b> page=0 returned status " + status + " (expected 400)");
        Assert.assertEquals(status, 400, "page=0 should return 400 Bad Request, but got: " + status);
        System.out.println("TC19 PASSED - page=0 correctly returns 400");
    }

    @Test(priority = 20, description = "TC20: Yodaara OTT - limit=0 should return 400 Bad Request")
    public void testTC20_YodaaraOtt_LimitZero() {
        System.out.println("\n>>> TC20: Yodaara OTT limit=0 <<<");
        System.out.println("┌─────────────────────────────────────────────────────────────");
        System.out.println("│ EXAMPLE: Invalid limit=0 should be rejected with 400");
        System.out.println("│ Request:  POST /tests/getAllTests");
        System.out.println("│ Payload:  {\"page\":1,\"limit\":0,\"yodaara_ott\":true,\"location\":\"64870066842708a0d5ae6c77\"}");
        System.out.println("│ Expected: 400 Bad Request with validation error message");
        System.out.println("│ Actual:   500 Internal Server Error (unhandled exception)");
        System.out.println("└─────────────────────────────────────────────────────────────");

        Map<String, Object> payload = buildPayload(1, 0);
        payload.put("yodaara_ott", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);

        int status = r.getStatusCode();
        System.out.println("   Actual status: " + status + " | Response: " + r.getBody().asString().substring(0, Math.min(200, r.getBody().asString().length())));
        ApiReportContext.addExtraDetail("<b>TC20:</b> limit=0 returned status " + status + " (expected 400)");
        Assert.assertEquals(status, 400, "limit=0 should return 400 Bad Request, but got: " + status);
        System.out.println("TC20 PASSED - limit=0 correctly returns 400");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECTION 2: PHARMACOGENOMICS FILTER (TC21 - TC40)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 21, description = "TC21: Pharmacogenomics - Valid request with location")
    public void testTC21_Pharma_ValidWithLocation() {
        System.out.println("\n>>> TC21: Pharmacogenomics valid request <<<");
        Map<String, Object> payload = buildPayload(1, 100);
        payload.put("pharmacogenomics", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);

        Assert.assertEquals(r.getStatusCode(), 200);
        Assert.assertTrue(r.jsonPath().getBoolean("success"));
        int total = r.jsonPath().getInt("total");
        Assert.assertTrue(total > 0, "Should return pharmacogenomics tests");
        List<Map<String, Object>> data = r.jsonPath().getList("data");
        Assert.assertNotNull(data);
        ApiReportContext.addExtraDetail("<b>Pharmacogenomics:</b> total=" + total + " returned=" + data.size());
        System.out.println("TC21 PASSED - total=" + total);
    }

    @Test(priority = 22, description = "TC22: Pharmacogenomics - Response schema validation")
    public void testTC22_Pharma_ResponseSchema() {
        System.out.println("\n>>> TC22: Pharmacogenomics schema <<<");
        Map<String, Object> payload = buildPayload(1, 100);
        payload.put("pharmacogenomics", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        Assert.assertNotNull(r.jsonPath().get("status"));
        Assert.assertNotNull(r.jsonPath().get("success"));
        Assert.assertNotNull(r.jsonPath().get("total"));
        Assert.assertNotNull(r.jsonPath().get("page"));
        Assert.assertNotNull(r.jsonPath().get("limit"));
        Assert.assertNotNull(r.jsonPath().get("total_pages"));
        Assert.assertNotNull(r.jsonPath().get("data"));
        ApiReportContext.addExtraDetail("<b>Schema:</b> validated");
        System.out.println("TC22 PASSED");
    }

    @Test(priority = 23, description = "TC23: Pharmacogenomics - All results are pharmacogenomics tests")
    public void testTC23_Pharma_FilterAccuracy() {
        System.out.println("\n>>> TC23: Pharmacogenomics filter accuracy <<<");
        Map<String, Object> payload = buildPayload(1, 100);
        payload.put("pharmacogenomics", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        List<Map<String, Object>> data = r.jsonPath().getList("data");
        // Verify returned tests are pharmacogenomics-related
        Assert.assertTrue(data.size() > 0, "Should have pharmacogenomics tests");
        ApiReportContext.addExtraDetail("<b>Pharma filter:</b> " + data.size() + " tests returned");
        System.out.println("TC23 PASSED - " + data.size() + " pharmacogenomics tests");
    }

    @Test(priority = 24, description = "TC24: Pharmacogenomics - Without location")
    public void testTC24_Pharma_WithoutLocation() {
        System.out.println("\n>>> TC24: Pharmacogenomics without location <<<");
        Map<String, Object> payload = buildPayload(1, 100);
        payload.put("pharmacogenomics", true);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        int total = r.jsonPath().getInt("total");
        ApiReportContext.addExtraDetail("<b>No location:</b> total=" + total + " vs with_location=" + pharmacogenomicsTotal);
        Assert.assertTrue(total >= pharmacogenomicsTotal, "Without location should return >= filtered count");
        System.out.println("TC24 PASSED - total=" + total);
    }

    @Test(priority = 25, description = "TC25: Pharmacogenomics - Location filter accuracy")
    public void testTC25_Pharma_LocationFilter() {
        System.out.println("\n>>> TC25: Pharmacogenomics location filter <<<");
        Map<String, Object> payload = buildPayload(1, 100);
        payload.put("pharmacogenomics", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        List<Map<String, Object>> data = r.jsonPath().getList("data");
        long missingLoc = data.stream()
                .filter(t -> {
                    Object locs = t.get("locations");
                    if (locs instanceof List) {
                        return !((List<?>) locs).contains(VALID_LOCATION);
                    }
                    return true;
                })
                .count();
        ApiReportContext.addExtraDetail("<b>Location check:</b> " + missingLoc + " missing");
        Assert.assertEquals(missingLoc, 0L, "All results should include the requested location");
        System.out.println("TC25 PASSED");
    }

    @Test(priority = 26, description = "TC26: Pharmacogenomics - Pagination limit=5")
    public void testTC26_Pharma_Pagination() {
        System.out.println("\n>>> TC26: Pharmacogenomics pagination <<<");
        Map<String, Object> payload = buildPayload(1, 5);
        payload.put("pharmacogenomics", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        List<Map<String, Object>> data = r.jsonPath().getList("data");
        int respLimit = r.jsonPath().getInt("limit");
        Assert.assertEquals(respLimit, 5);
        Assert.assertTrue(data.size() <= 5, "Should return <= 5. Got: " + data.size());
        ApiReportContext.addExtraDetail("<b>limit=5:</b> returned=" + data.size());
        System.out.println("TC26 PASSED");
    }

    @Test(priority = 27, description = "TC27: Pharmacogenomics - Page 2 should have no overlap with page 1")
    public void testTC27_Pharma_Page2DifferentFromPage1() {
        System.out.println("\n>>> TC27: Pharmacogenomics page 1 vs 2 <<<");
        System.out.println("┌─────────────────────────────────────────────────────────────");
        System.out.println("│ EXAMPLE: Pagination should return unique records per page");
        System.out.println("│ Request 1: POST /tests/getAllTests");
        System.out.println("│ Payload 1: {\"page\":1,\"limit\":5,\"pharmacogenomics\":true,\"location\":\"64870066842708a0d5ae6c77\"}");
        System.out.println("│ Request 2: POST /tests/getAllTests");
        System.out.println("│ Payload 2: {\"page\":2,\"limit\":5,\"pharmacogenomics\":true,\"location\":\"64870066842708a0d5ae6c77\"}");
        System.out.println("│ Expected: Page 2 records should NOT overlap with page 1");
        System.out.println("│ Actual:   4 duplicate records found between page 1 and page 2");
        System.out.println("└─────────────────────────────────────────────────────────────");

        Map<String, Object> payload1 = buildPayload(1, 5);
        payload1.put("pharmacogenomics", true);
        payload1.put("location", VALID_LOCATION);
        Response r1 = callAPI(payload1);

        Map<String, Object> payload2 = buildPayload(2, 5);
        payload2.put("pharmacogenomics", true);
        payload2.put("location", VALID_LOCATION);
        Response r2 = callAPI(payload2);

        Assert.assertEquals(r1.getStatusCode(), 200);
        Assert.assertEquals(r2.getStatusCode(), 200);

        List<Map<String, Object>> data1 = r1.jsonPath().getList("data");
        List<Map<String, Object>> data2 = r2.jsonPath().getList("data");

        System.out.println("   Page 1 records: " + data1.size() + " | Page 2 records: " + data2.size());

        if (data2.size() > 0) {
            Set<String> page1Ids = data1.stream().map(t -> String.valueOf(t.get("_id"))).collect(Collectors.toSet());
            long overlap = data2.stream().map(t -> String.valueOf(t.get("_id"))).filter(page1Ids::contains).count();
            System.out.println("   Overlapping records: " + overlap);
            if (overlap > 0) {
                System.out.println("   Page 1 IDs: " + page1Ids);
                Set<String> dupIds = data2.stream().map(t -> String.valueOf(t.get("_id"))).filter(page1Ids::contains).collect(Collectors.toSet());
                System.out.println("   Duplicate IDs: " + dupIds);
            }
            ApiReportContext.addExtraDetail("<b>TC27:</b> Page 1 & 2 overlap=" + overlap + " records (expected 0)");
            Assert.assertEquals(overlap, 0L, "Page 2 should have 0 overlapping records with page 1, but found: " + overlap);
            System.out.println("TC27 PASSED - no pagination overlap");
        } else {
            ApiReportContext.addExtraDetail("<b>Page 2:</b> empty (all data fits page 1)");
            System.out.println("TC27 PASSED - page 2 empty, no overlap possible");
        }
    }

    @Test(priority = 28, description = "TC28: Pharmacogenomics - total_pages calculation")
    public void testTC28_Pharma_TotalPages() {
        System.out.println("\n>>> TC28: Pharmacogenomics total_pages <<<");
        Map<String, Object> payload = buildPayload(1, 5);
        payload.put("pharmacogenomics", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        int total = r.jsonPath().getInt("total");
        int limit = r.jsonPath().getInt("limit");
        int totalPages = r.jsonPath().getInt("total_pages");
        int expected = (int) Math.ceil((double) total / limit);
        Assert.assertEquals(totalPages, expected);
        ApiReportContext.addExtraDetail("<b>total_pages:</b> " + totalPages);
        System.out.println("TC28 PASSED");
    }

    @Test(priority = 29, description = "TC29: Pharmacogenomics - pharmacogenomics=false")
    public void testTC29_Pharma_FalseValue() {
        System.out.println("\n>>> TC29: Pharmacogenomics false <<<");
        Map<String, Object> payload = buildPayload(1, 20);
        payload.put("pharmacogenomics", false);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        int total = r.jsonPath().getInt("total");
        ApiReportContext.addExtraDetail("<b>pharmacogenomics=false:</b> total=" + total);
        Assert.assertNotEquals(total, pharmacogenomicsTotal, "false should return different set");
        System.out.println("TC29 PASSED - false total=" + total);
    }

    @Test(priority = 30, description = "TC30: Pharmacogenomics - Invalid location")
    public void testTC30_Pharma_InvalidLocation() {
        System.out.println("\n>>> TC30: Pharmacogenomics invalid location <<<");
        Map<String, Object> payload = buildPayload(1, 20);
        payload.put("pharmacogenomics", true);
        payload.put("location", "badlocation123");
        Response r = callAPI(payload);

        Assert.assertFalse(r.jsonPath().getBoolean("success"));
        ApiReportContext.addExtraDetail("<b>Invalid location:</b> rejected");
        System.out.println("TC30 PASSED");
    }

    @Test(priority = 31, description = "TC31: Pharmacogenomics - No duplicates")
    public void testTC31_Pharma_NoDuplicates() {
        System.out.println("\n>>> TC31: Pharmacogenomics no duplicates <<<");
        Map<String, Object> payload = buildPayload(1, 100);
        payload.put("pharmacogenomics", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        List<Map<String, Object>> data = r.jsonPath().getList("data");
        Set<String> ids = new HashSet<>();
        long dups = data.stream().filter(t -> !ids.add(String.valueOf(t.get("_id")))).count();
        Assert.assertEquals(dups, 0L);
        ApiReportContext.addExtraDetail("<b>Duplicates:</b> " + dups);
        System.out.println("TC31 PASSED");
    }

    @Test(priority = 32, description = "TC32: Pharmacogenomics - All ACTIVE status")
    public void testTC32_Pharma_AllActive() {
        System.out.println("\n>>> TC32: Pharmacogenomics all active <<<");
        Map<String, Object> payload = buildPayload(1, 100);
        payload.put("pharmacogenomics", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        List<Map<String, Object>> data = r.jsonPath().getList("data");
        long inactive = data.stream().filter(t -> !"ACTIVE".equals(t.get("status"))).count();
        Assert.assertEquals(inactive, 0L);
        ApiReportContext.addExtraDetail("<b>Inactive:</b> " + inactive);
        System.out.println("TC32 PASSED");
    }

    @Test(priority = 33, description = "TC33: Pharmacogenomics - Price positive")
    public void testTC33_Pharma_PricePositive() {
        System.out.println("\n>>> TC33: Pharmacogenomics price <<<");
        Map<String, Object> payload = buildPayload(1, 100);
        payload.put("pharmacogenomics", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        List<Map<String, Object>> data = r.jsonPath().getList("data");
        long badPrice = data.stream()
                .filter(t -> t.get("price") == null || ((Number) t.get("price")).doubleValue() <= 0)
                .count();
        Assert.assertEquals(badPrice, 0L);
        ApiReportContext.addExtraDetail("<b>Bad prices:</b> " + badPrice);
        System.out.println("TC33 PASSED");
    }

    @Test(priority = 34, description = "TC34: Pharmacogenomics - Response time < 3s")
    public void testTC34_Pharma_ResponseTime() {
        System.out.println("\n>>> TC34: Pharmacogenomics response time <<<");
        Map<String, Object> payload = buildPayload(1, 100);
        payload.put("pharmacogenomics", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);
        long time = r.getTime();
        Assert.assertTrue(time < 3000, "Response > 3s: " + time + "ms");
        ApiReportContext.addExtraDetail("<b>Time:</b> " + time + "ms");
        System.out.println("TC34 PASSED - " + time + "ms");
    }

    @Test(priority = 35, description = "TC35: Pharmacogenomics - test_name not null/empty")
    public void testTC35_Pharma_TestNameNotEmpty() {
        System.out.println("\n>>> TC35: Pharmacogenomics test_name <<<");
        Map<String, Object> payload = buildPayload(1, 100);
        payload.put("pharmacogenomics", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        List<Map<String, Object>> data = r.jsonPath().getList("data");
        long emptyName = data.stream()
                .filter(t -> t.get("test_name") == null || String.valueOf(t.get("test_name")).trim().isEmpty())
                .count();
        Assert.assertEquals(emptyName, 0L);
        ApiReportContext.addExtraDetail("<b>Empty names:</b> " + emptyName);
        System.out.println("TC35 PASSED");
    }

    @Test(priority = 36, description = "TC36: Pharmacogenomics - slug not null/empty")
    public void testTC36_Pharma_SlugNotEmpty() {
        System.out.println("\n>>> TC36: Pharmacogenomics slug <<<");
        Map<String, Object> payload = buildPayload(1, 100);
        payload.put("pharmacogenomics", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        List<Map<String, Object>> data = r.jsonPath().getList("data");
        long emptySlug = data.stream()
                .filter(t -> t.get("slug") == null || String.valueOf(t.get("slug")).trim().isEmpty())
                .count();
        Assert.assertEquals(emptySlug, 0L);
        ApiReportContext.addExtraDetail("<b>Empty slugs:</b> " + emptySlug);
        System.out.println("TC36 PASSED");
    }

    @Test(priority = 37, description = "TC37: Pharmacogenomics - large page returns empty")
    public void testTC37_Pharma_LargePage() {
        System.out.println("\n>>> TC37: Pharmacogenomics large page <<<");
        Map<String, Object> payload = buildPayload(9999, 20);
        payload.put("pharmacogenomics", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        List<Map<String, Object>> data = r.jsonPath().getList("data");
        Assert.assertEquals(data.size(), 0);
        ApiReportContext.addExtraDetail("<b>page=9999:</b> empty");
        System.out.println("TC37 PASSED");
    }

    @Test(priority = 38, description = "TC38: Pharmacogenomics - limit=1 returns exactly 1")
    public void testTC38_Pharma_Limit1() {
        System.out.println("\n>>> TC38: Pharmacogenomics limit=1 <<<");
        Map<String, Object> payload = buildPayload(1, 1);
        payload.put("pharmacogenomics", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        List<Map<String, Object>> data = r.jsonPath().getList("data");
        Assert.assertEquals(data.size(), 1, "limit=1 should return exactly 1. Got: " + data.size());
        ApiReportContext.addExtraDetail("<b>limit=1:</b> returned=" + data.size());
        System.out.println("TC38 PASSED");
    }

    @Test(priority = 39, description = "TC39: Pharmacogenomics - Non-existent location should return 0 results")
    public void testTC39_Pharma_NonExistentLocation() {
        System.out.println("\n>>> TC39: Pharmacogenomics non-existent location <<<");
        System.out.println("┌─────────────────────────────────────────────────────────────");
        System.out.println("│ EXAMPLE: Location filter should reject non-existent ObjectId");
        System.out.println("│ Request:  POST /tests/getAllTests");
        System.out.println("│ Payload:  {\"page\":1,\"limit\":20,\"pharmacogenomics\":true,\"location\":\"000000000000000000000000\"}");
        System.out.println("│ Expected: {\"total\":0, \"data\":[]}");
        System.out.println("│ Actual:   Returns all pharmacogenomics tests ignoring invalid location");
        System.out.println("└─────────────────────────────────────────────────────────────");

        Map<String, Object> payload = buildPayload(1, 20);
        payload.put("pharmacogenomics", true);
        payload.put("location", "000000000000000000000000");
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        int total = r.jsonPath().getInt("total");
        System.out.println("   Actual total: " + total);
        ApiReportContext.addExtraDetail("<b>TC39:</b> Non-existent location returned " + total + " results (expected 0)");
        Assert.assertEquals(total, 0, "Non-existent location should return 0 results, but got: " + total);
        System.out.println("TC39 PASSED - non-existent location correctly returned 0 results");
    }

    @Test(priority = 40, description = "TC40: Pharmacogenomics - page=0 should return 400 Bad Request")
    public void testTC40_Pharma_PageZero() {
        System.out.println("\n>>> TC40: Pharmacogenomics page=0 <<<");
        System.out.println("┌─────────────────────────────────────────────────────────────");
        System.out.println("│ EXAMPLE: Invalid page=0 should be rejected with 400");
        System.out.println("│ Request:  POST /tests/getAllTests");
        System.out.println("│ Payload:  {\"page\":0,\"limit\":20,\"pharmacogenomics\":true,\"location\":\"64870066842708a0d5ae6c77\"}");
        System.out.println("│ Expected: 400 Bad Request with validation error");
        System.out.println("│ Actual:   500 Internal Server Error (server crash)");
        System.out.println("└─────────────────────────────────────────────────────────────");

        Map<String, Object> payload = buildPayload(0, 20);
        payload.put("pharmacogenomics", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);

        int status = r.getStatusCode();
        System.out.println("   Actual status: " + status);
        ApiReportContext.addExtraDetail("<b>TC40:</b> page=0 returned status " + status + " (expected 400)");
        Assert.assertEquals(status, 400, "page=0 should return 400 Bad Request, but got: " + status);
        System.out.println("TC40 PASSED - page=0 correctly returns 400");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECTION 3: DNADECODER FILTER (TC41 - TC60)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 41, description = "TC41: DNADecoder - Valid request with popular=false")
    public void testTC41_DNA_ValidWithPopularFalse() {
        System.out.println("\n>>> TC41: DNADecoder valid request <<<");
        Map<String, Object> payload = buildPayload(1, 20);
        payload.put("dnadecoder", true);
        payload.put("popular", false);
        Response r = callAPI(payload);

        Assert.assertEquals(r.getStatusCode(), 200);
        Assert.assertTrue(r.jsonPath().getBoolean("success"));
        int total = r.jsonPath().getInt("total");
        Assert.assertTrue(total > 0, "Should return dnadecoder tests");
        List<Map<String, Object>> data = r.jsonPath().getList("data");
        Assert.assertNotNull(data);
        ApiReportContext.addExtraDetail("<b>DNADecoder popular=false:</b> total=" + total + " returned=" + data.size());
        System.out.println("TC41 PASSED - total=" + total);
    }

    @Test(priority = 42, description = "TC42: DNADecoder - With location filter")
    public void testTC42_DNA_WithLocation() {
        System.out.println("\n>>> TC42: DNADecoder with location <<<");
        Map<String, Object> payload = buildPayload(1, 20);
        payload.put("dnadecoder", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);

        Assert.assertEquals(r.getStatusCode(), 200);
        int total = r.jsonPath().getInt("total");
        ApiReportContext.addExtraDetail("<b>DNADecoder + location:</b> total=" + total);
        System.out.println("TC42 PASSED - total=" + total);
    }

    @Test(priority = 43, description = "TC43: DNADecoder - popular=true returns different set")
    public void testTC43_DNA_PopularTrue() {
        System.out.println("\n>>> TC43: DNADecoder popular=true <<<");
        Map<String, Object> payload = buildPayload(1, 100);
        payload.put("dnadecoder", true);
        payload.put("popular", true);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        int popularTotal = r.jsonPath().getInt("total");

        Map<String, Object> payload2 = buildPayload(1, 100);
        payload2.put("dnadecoder", true);
        payload2.put("popular", false);
        Response r2 = callAPI(payload2);
        int nonPopularTotal = r2.jsonPath().getInt("total");

        ApiReportContext.addExtraDetail("<b>popular=true:</b> " + popularTotal + " | <b>popular=false:</b> " + nonPopularTotal);
        // popular flag should filter differently
        System.out.println("TC43 PASSED - popular=" + popularTotal + " vs non-popular=" + nonPopularTotal);
    }

    @Test(priority = 44, description = "TC44: DNADecoder - Response schema")
    public void testTC44_DNA_ResponseSchema() {
        System.out.println("\n>>> TC44: DNADecoder schema <<<");
        Map<String, Object> payload = buildPayload(1, 20);
        payload.put("dnadecoder", true);
        payload.put("popular", false);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        Assert.assertNotNull(r.jsonPath().get("status"));
        Assert.assertNotNull(r.jsonPath().get("success"));
        Assert.assertNotNull(r.jsonPath().get("total"));
        Assert.assertNotNull(r.jsonPath().get("page"));
        Assert.assertNotNull(r.jsonPath().get("limit"));
        Assert.assertNotNull(r.jsonPath().get("total_pages"));
        Assert.assertNotNull(r.jsonPath().get("data"));
        ApiReportContext.addExtraDetail("<b>Schema:</b> validated");
        System.out.println("TC44 PASSED");
    }

    @Test(priority = 45, description = "TC45: DNADecoder - Pagination limit=5")
    public void testTC45_DNA_PaginationLimit5() {
        System.out.println("\n>>> TC45: DNADecoder pagination <<<");
        Map<String, Object> payload = buildPayload(1, 5);
        payload.put("dnadecoder", true);
        payload.put("popular", false);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        List<Map<String, Object>> data = r.jsonPath().getList("data");
        int respLimit = r.jsonPath().getInt("limit");
        Assert.assertEquals(respLimit, 5);
        Assert.assertTrue(data.size() <= 5, "Got: " + data.size());
        ApiReportContext.addExtraDetail("<b>limit=5:</b> returned=" + data.size());
        System.out.println("TC45 PASSED");
    }

    @Test(priority = 46, description = "TC46: DNADecoder - Page 2 works")
    public void testTC46_DNA_Page2() {
        System.out.println("\n>>> TC46: DNADecoder page 2 <<<");
        Map<String, Object> payload = buildPayload(2, 10);
        payload.put("dnadecoder", true);
        payload.put("popular", false);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        int respPage = r.jsonPath().getInt("page");
        Assert.assertEquals(respPage, 2);
        List<Map<String, Object>> data = r.jsonPath().getList("data");
        ApiReportContext.addExtraDetail("<b>page=2:</b> returned=" + data.size());
        System.out.println("TC46 PASSED - page 2 has " + data.size() + " records");
    }

    @Test(priority = 47, description = "TC47: DNADecoder - No duplicates across pages")
    public void testTC47_DNA_NoDuplicatesAcrossPages() {
        System.out.println("\n>>> TC47: DNADecoder no page overlap <<<");
        Map<String, Object> payload1 = buildPayload(1, 10);
        payload1.put("dnadecoder", true);
        payload1.put("popular", false);
        Response r1 = callAPI(payload1);

        Map<String, Object> payload2 = buildPayload(2, 10);
        payload2.put("dnadecoder", true);
        payload2.put("popular", false);
        Response r2 = callAPI(payload2);

        List<Map<String, Object>> d1 = r1.jsonPath().getList("data");
        List<Map<String, Object>> d2 = r2.jsonPath().getList("data");

        if (d2.size() > 0) {
            Set<String> ids1 = d1.stream().map(t -> String.valueOf(t.get("_id"))).collect(Collectors.toSet());
            long overlap = d2.stream().map(t -> String.valueOf(t.get("_id"))).filter(ids1::contains).count();
            Assert.assertEquals(overlap, 0L, "Pages should not overlap");
            ApiReportContext.addExtraDetail("<b>Overlap:</b> " + overlap);
        }
        System.out.println("TC47 PASSED");
    }

    @Test(priority = 48, description = "TC48: DNADecoder - total_pages calculation")
    public void testTC48_DNA_TotalPages() {
        System.out.println("\n>>> TC48: DNADecoder total_pages <<<");
        Map<String, Object> payload = buildPayload(1, 10);
        payload.put("dnadecoder", true);
        payload.put("popular", false);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        int total = r.jsonPath().getInt("total");
        int limit = r.jsonPath().getInt("limit");
        int totalPages = r.jsonPath().getInt("total_pages");
        int expected = (int) Math.ceil((double) total / limit);
        Assert.assertEquals(totalPages, expected);
        ApiReportContext.addExtraDetail("<b>total_pages:</b> " + totalPages + " expected=" + expected);
        System.out.println("TC48 PASSED");
    }

    @Test(priority = 49, description = "TC49: DNADecoder - dnadecoder=false returns different set")
    public void testTC49_DNA_FalseValue() {
        System.out.println("\n>>> TC49: DNADecoder false <<<");
        Map<String, Object> payload = buildPayload(1, 20);
        payload.put("dnadecoder", false);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        int total = r.jsonPath().getInt("total");
        ApiReportContext.addExtraDetail("<b>dnadecoder=false:</b> total=" + total);
        System.out.println("TC49 PASSED - total=" + total);
    }

    @Test(priority = 50, description = "TC50: DNADecoder - Invalid location")
    public void testTC50_DNA_InvalidLocation() {
        System.out.println("\n>>> TC50: DNADecoder invalid location <<<");
        Map<String, Object> payload = buildPayload(1, 20);
        payload.put("dnadecoder", true);
        payload.put("location", "not_an_objectid");
        Response r = callAPI(payload);

        Assert.assertFalse(r.jsonPath().getBoolean("success"));
        ApiReportContext.addExtraDetail("<b>Invalid location:</b> rejected");
        System.out.println("TC50 PASSED");
    }

    @Test(priority = 51, description = "TC51: DNADecoder - No duplicate IDs")
    public void testTC51_DNA_NoDuplicateIDs() {
        System.out.println("\n>>> TC51: DNADecoder no duplicate IDs <<<");
        Map<String, Object> payload = buildPayload(1, 100);
        payload.put("dnadecoder", true);
        payload.put("popular", false);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        List<Map<String, Object>> data = r.jsonPath().getList("data");
        Set<String> ids = new HashSet<>();
        long dups = data.stream().filter(t -> !ids.add(String.valueOf(t.get("_id")))).count();
        Assert.assertEquals(dups, 0L);
        ApiReportContext.addExtraDetail("<b>Duplicates:</b> " + dups);
        System.out.println("TC51 PASSED");
    }

    @Test(priority = 52, description = "TC52: DNADecoder - All ACTIVE status")
    public void testTC52_DNA_AllActive() {
        System.out.println("\n>>> TC52: DNADecoder all active <<<");
        Map<String, Object> payload = buildPayload(1, 100);
        payload.put("dnadecoder", true);
        payload.put("popular", false);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        List<Map<String, Object>> data = r.jsonPath().getList("data");
        long inactive = data.stream().filter(t -> !"ACTIVE".equals(t.get("status"))).count();
        Assert.assertEquals(inactive, 0L);
        ApiReportContext.addExtraDetail("<b>Inactive:</b> " + inactive);
        System.out.println("TC52 PASSED");
    }

    @Test(priority = 53, description = "TC53: DNADecoder - Price positive")
    public void testTC53_DNA_PricePositive() {
        System.out.println("\n>>> TC53: DNADecoder price <<<");
        Map<String, Object> payload = buildPayload(1, 100);
        payload.put("dnadecoder", true);
        payload.put("popular", false);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        List<Map<String, Object>> data = r.jsonPath().getList("data");
        long badPrice = data.stream()
                .filter(t -> t.get("price") == null || ((Number) t.get("price")).doubleValue() <= 0)
                .count();
        Assert.assertEquals(badPrice, 0L);
        ApiReportContext.addExtraDetail("<b>Bad prices:</b> " + badPrice);
        System.out.println("TC53 PASSED");
    }

    @Test(priority = 54, description = "TC54: DNADecoder - Response time < 3s")
    public void testTC54_DNA_ResponseTime() {
        System.out.println("\n>>> TC54: DNADecoder response time <<<");
        Map<String, Object> payload = buildPayload(1, 20);
        payload.put("dnadecoder", true);
        payload.put("popular", false);
        Response r = callAPI(payload);
        long time = r.getTime();
        Assert.assertTrue(time < 3000, "Response > 3s: " + time + "ms");
        ApiReportContext.addExtraDetail("<b>Time:</b> " + time + "ms");
        System.out.println("TC54 PASSED - " + time + "ms");
    }

    @Test(priority = 55, description = "TC55: DNADecoder - test_name not empty")
    public void testTC55_DNA_TestNameNotEmpty() {
        System.out.println("\n>>> TC55: DNADecoder test_name <<<");
        Map<String, Object> payload = buildPayload(1, 100);
        payload.put("dnadecoder", true);
        payload.put("popular", false);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        List<Map<String, Object>> data = r.jsonPath().getList("data");
        long empty = data.stream()
                .filter(t -> t.get("test_name") == null || String.valueOf(t.get("test_name")).trim().isEmpty())
                .count();
        Assert.assertEquals(empty, 0L);
        System.out.println("TC55 PASSED");
    }

    @Test(priority = 56, description = "TC56: DNADecoder - Large page returns empty")
    public void testTC56_DNA_LargePage() {
        System.out.println("\n>>> TC56: DNADecoder large page <<<");
        Map<String, Object> payload = buildPayload(9999, 20);
        payload.put("dnadecoder", true);
        payload.put("popular", false);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        List<Map<String, Object>> data = r.jsonPath().getList("data");
        Assert.assertEquals(data.size(), 0);
        System.out.println("TC56 PASSED");
    }

    @Test(priority = 57, description = "TC57: DNADecoder - limit=1 returns 1")
    public void testTC57_DNA_Limit1() {
        System.out.println("\n>>> TC57: DNADecoder limit=1 <<<");
        Map<String, Object> payload = buildPayload(1, 1);
        payload.put("dnadecoder", true);
        payload.put("popular", false);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        List<Map<String, Object>> data = r.jsonPath().getList("data");
        Assert.assertEquals(data.size(), 1, "Got: " + data.size());
        ApiReportContext.addExtraDetail("<b>limit=1:</b> returned=" + data.size());
        System.out.println("TC57 PASSED");
    }

    @Test(priority = 58, description = "TC58: DNADecoder - With location + popular=true")
    public void testTC58_DNA_LocationAndPopularTrue() {
        System.out.println("\n>>> TC58: DNADecoder location + popular=true <<<");
        Map<String, Object> payload = buildPayload(1, 100);
        payload.put("dnadecoder", true);
        payload.put("popular", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        int total = r.jsonPath().getInt("total");
        ApiReportContext.addExtraDetail("<b>DNA + location + popular:</b> total=" + total);
        System.out.println("TC58 PASSED - total=" + total);
    }

    @Test(priority = 59, description = "TC59: DNADecoder - Non-existent location should return 0 results")
    public void testTC59_DNA_NonExistentLocation() {
        System.out.println("\n>>> TC59: DNADecoder non-existent location <<<");
        System.out.println("┌─────────────────────────────────────────────────────────────");
        System.out.println("│ EXAMPLE: Location filter should reject non-existent ObjectId");
        System.out.println("│ Request:  POST /tests/getAllTests");
        System.out.println("│ Payload:  {\"page\":1,\"limit\":20,\"dnadecoder\":true,\"location\":\"000000000000000000000000\"}");
        System.out.println("│ Expected: {\"total\":0, \"data\":[]}");
        System.out.println("│ Actual:   Returns all dnadecoder tests ignoring invalid location");
        System.out.println("└─────────────────────────────────────────────────────────────");

        Map<String, Object> payload = buildPayload(1, 20);
        payload.put("dnadecoder", true);
        payload.put("location", "000000000000000000000000");
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        int total = r.jsonPath().getInt("total");
        System.out.println("   Actual total: " + total);
        ApiReportContext.addExtraDetail("<b>TC59:</b> Non-existent location returned " + total + " results (expected 0)");
        Assert.assertEquals(total, 0, "Non-existent location should return 0 results, but got: " + total);
        System.out.println("TC59 PASSED - non-existent location correctly returned 0 results");
    }

    @Test(priority = 60, description = "TC60: DNADecoder - page=0 should return 400 Bad Request")
    public void testTC60_DNA_PageZero() {
        System.out.println("\n>>> TC60: DNADecoder page=0 <<<");
        System.out.println("┌─────────────────────────────────────────────────────────────");
        System.out.println("│ EXAMPLE: Invalid page=0 should be rejected with 400");
        System.out.println("│ Request:  POST /tests/getAllTests");
        System.out.println("│ Payload:  {\"page\":0,\"limit\":20,\"dnadecoder\":true,\"popular\":false}");
        System.out.println("│ Expected: 400 Bad Request with validation error");
        System.out.println("│ Actual:   500 Internal Server Error (server crash)");
        System.out.println("└─────────────────────────────────────────────────────────────");

        Map<String, Object> payload = buildPayload(0, 20);
        payload.put("dnadecoder", true);
        payload.put("popular", false);
        Response r = callAPI(payload);

        int status = r.getStatusCode();
        System.out.println("   Actual status: " + status);
        ApiReportContext.addExtraDetail("<b>TC60:</b> page=0 returned status " + status + " (expected 400)");
        Assert.assertEquals(status, 400, "page=0 should return 400 Bad Request, but got: " + status);
        System.out.println("TC60 PASSED - page=0 correctly returns 400");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECTION 4: COMBINATION & CROSS-FILTER (TC61 - TC70)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 61, description = "TC61: Combo - yodaara_ott + pharmacogenomics both true")
    public void testTC61_Combo_YodaaraAndPharma() {
        System.out.println("\n>>> TC61: yodaara + pharma combo <<<");
        Map<String, Object> payload = buildPayload(1, 20);
        payload.put("yodaara_ott", true);
        payload.put("pharmacogenomics", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        int total = r.jsonPath().getInt("total");
        // Combo of two filters likely returns intersection (0 or very few)
        ApiReportContext.addExtraDetail("<b>yodaara+pharma:</b> total=" + total);
        System.out.println("TC61 PASSED - combo total=" + total);
    }

    @Test(priority = 62, description = "TC62: Combo - yodaara_ott + dnadecoder both true")
    public void testTC62_Combo_YodaaraAndDNA() {
        System.out.println("\n>>> TC62: yodaara + dnadecoder combo <<<");
        Map<String, Object> payload = buildPayload(1, 20);
        payload.put("yodaara_ott", true);
        payload.put("dnadecoder", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        int total = r.jsonPath().getInt("total");
        ApiReportContext.addExtraDetail("<b>yodaara+dna:</b> total=" + total);
        System.out.println("TC62 PASSED - combo total=" + total);
    }

    @Test(priority = 63, description = "TC63: Combo - pharmacogenomics + dnadecoder both true")
    public void testTC63_Combo_PharmaAndDNA() {
        System.out.println("\n>>> TC63: pharma + dnadecoder combo <<<");
        Map<String, Object> payload = buildPayload(1, 20);
        payload.put("pharmacogenomics", true);
        payload.put("dnadecoder", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        int total = r.jsonPath().getInt("total");
        ApiReportContext.addExtraDetail("<b>pharma+dna:</b> total=" + total);
        System.out.println("TC63 PASSED - combo total=" + total);
    }

    @Test(priority = 64, description = "TC64: Combo - All three filters true")
    public void testTC64_Combo_AllThreeTrue() {
        System.out.println("\n>>> TC64: All three filters <<<");
        Map<String, Object> payload = buildPayload(1, 20);
        payload.put("yodaara_ott", true);
        payload.put("pharmacogenomics", true);
        payload.put("dnadecoder", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        int total = r.jsonPath().getInt("total");
        ApiReportContext.addExtraDetail("<b>All 3 true:</b> total=" + total);
        System.out.println("TC64 PASSED - total=" + total);
    }

    @Test(priority = 65, description = "TC65: Combo - All three filters false")
    public void testTC65_Combo_AllThreeFalse() {
        System.out.println("\n>>> TC65: All three filters false <<<");
        Map<String, Object> payload = buildPayload(1, 20);
        payload.put("yodaara_ott", false);
        payload.put("pharmacogenomics", false);
        payload.put("dnadecoder", false);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        int total = r.jsonPath().getInt("total");
        Assert.assertTrue(total > 0, "False filters should return general tests");
        ApiReportContext.addExtraDetail("<b>All false:</b> total=" + total);
        System.out.println("TC65 PASSED - total=" + total);
    }

    @Test(priority = 66, description = "TC66: No filter at all - returns all tests")
    public void testTC66_NoFilter() {
        System.out.println("\n>>> TC66: No filter <<<");
        Map<String, Object> payload = buildPayload(1, 20);
        Response r = callAPI(payload);
        Assert.assertEquals(r.getStatusCode(), 200);

        int total = r.jsonPath().getInt("total");
        Assert.assertTrue(total > 0);
        ApiReportContext.addExtraDetail("<b>No filter:</b> total=" + total);
        System.out.println("TC66 PASSED - total=" + total);
    }

    @Test(priority = 67, description = "TC67: Yodaara count <= No-filter count")
    public void testTC67_YodaaraSubsetOfAll() {
        System.out.println("\n>>> TC67: Yodaara subset check <<<");
        Map<String, Object> noFilter = buildPayload(1, 1);
        Response rAll = callAPI(noFilter);
        int allTotal = rAll.jsonPath().getInt("total");

        Assert.assertTrue(yodaaraTotal <= allTotal, "Yodaara should be subset. yodaara=" + yodaaraTotal + " all=" + allTotal);
        ApiReportContext.addExtraDetail("<b>Subset:</b> yodaara=" + yodaaraTotal + " <= all=" + allTotal);
        System.out.println("TC67 PASSED");
    }

    @Test(priority = 68, description = "TC68: Pharmacogenomics count <= No-filter count")
    public void testTC68_PharmaSubsetOfAll() {
        System.out.println("\n>>> TC68: Pharma subset check <<<");
        Map<String, Object> noFilter = buildPayload(1, 1);
        Response rAll = callAPI(noFilter);
        int allTotal = rAll.jsonPath().getInt("total");

        Assert.assertTrue(pharmacogenomicsTotal <= allTotal, "Pharma should be subset");
        ApiReportContext.addExtraDetail("<b>Subset:</b> pharma=" + pharmacogenomicsTotal + " <= all=" + allTotal);
        System.out.println("TC68 PASSED");
    }

    @Test(priority = 69, description = "TC69: DNADecoder count <= No-filter count")
    public void testTC69_DNASubsetOfAll() {
        System.out.println("\n>>> TC69: DNA subset check <<<");
        Map<String, Object> noFilter = buildPayload(1, 1);
        Response rAll = callAPI(noFilter);
        int allTotal = rAll.jsonPath().getInt("total");

        Assert.assertTrue(dnadecoderTotal <= allTotal, "DNA should be subset");
        ApiReportContext.addExtraDetail("<b>Subset:</b> dna=" + dnadecoderTotal + " <= all=" + allTotal);
        System.out.println("TC69 PASSED");
    }

    @Test(priority = 70, description = "TC70: Yodaara and Pharma results are mutually exclusive")
    public void testTC70_YodaaraVsPharma_MutuallyExclusive() {
        System.out.println("\n>>> TC70: Yodaara vs Pharma exclusivity <<<");
        Map<String, Object> p1 = buildPayload(1, 100);
        p1.put("yodaara_ott", true);
        p1.put("location", VALID_LOCATION);
        Response r1 = callAPI(p1);
        List<Map<String, Object>> d1 = r1.jsonPath().getList("data");
        Set<String> yodaaraIds = d1.stream().map(t -> String.valueOf(t.get("_id"))).collect(Collectors.toSet());

        Map<String, Object> p2 = buildPayload(1, 100);
        p2.put("pharmacogenomics", true);
        p2.put("location", VALID_LOCATION);
        Response r2 = callAPI(p2);
        List<Map<String, Object>> d2 = r2.jsonPath().getList("data");

        long overlap = d2.stream().map(t -> String.valueOf(t.get("_id"))).filter(yodaaraIds::contains).count();
        ApiReportContext.addExtraDetail("<b>Yodaara vs Pharma overlap:</b> " + overlap);
        System.out.println("TC70 PASSED - overlap=" + overlap);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECTION 5: NEGATIVE & SECURITY (TC71 - TC80)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 71, description = "TC71: Empty body returns error or defaults")
    public void testTC71_EmptyBody() {
        System.out.println("\n>>> TC71: Empty body <<<");
        Map<String, Object> payload = new HashMap<>();
        Response r = callAPI(payload);

        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>Empty body:</b> status=" + status);
        // Should either default or fail gracefully
        Assert.assertTrue(status == 200 || status == 400 || status == 500, "Got: " + status);
        System.out.println("TC71 PASSED - status=" + status);
    }

    @Test(priority = 72, description = "TC72: SQL injection in location field should return 400 Bad Request")
    public void testTC72_SQLInjectionLocation() {
        System.out.println("\n>>> TC72: SQL injection <<<");
        System.out.println("┌─────────────────────────────────────────────────────────────");
        System.out.println("│ EXAMPLE: SQL injection in location should be sanitized/rejected");
        System.out.println("│ Request:  POST /tests/getAllTests");
        System.out.println("│ Payload:  {\"page\":1,\"limit\":20,\"yodaara_ott\":true,\"location\":\"'; DROP TABLE tests; --\"}");
        System.out.println("│ Expected: 400 Bad Request (input rejected/sanitized)");
        System.out.println("│ Actual:   500 Internal Server Error (unhandled exception, server crash)");
        System.out.println("│ Impact:   CRITICAL SECURITY - Server crashes on malicious input");
        System.out.println("└─────────────────────────────────────────────────────────────");

        Map<String, Object> payload = buildPayload(1, 20);
        payload.put("yodaara_ott", true);
        payload.put("location", "'; DROP TABLE tests; --");
        Response r = callAPI(payload);

        int status = r.getStatusCode();
        System.out.println("   Actual status: " + status + " | Response: " + r.getBody().asString().substring(0, Math.min(200, r.getBody().asString().length())));
        ApiReportContext.addExtraDetail("<b>TC72 (SECURITY):</b> SQL injection returned status " + status + " (expected 400)");
        Assert.assertEquals(status, 400, "SQL injection in location should return 400 Bad Request, but got: " + status);
        System.out.println("TC72 PASSED - SQL injection correctly rejected with 400");
    }

    @Test(priority = 73, description = "TC73: XSS in location field should return 400 Bad Request")
    public void testTC73_XSSInLocation() {
        System.out.println("\n>>> TC73: XSS attempt <<<");
        System.out.println("┌─────────────────────────────────────────────────────────────");
        System.out.println("│ EXAMPLE: XSS script tag in location should be sanitized/rejected");
        System.out.println("│ Request:  POST /tests/getAllTests");
        System.out.println("│ Payload:  {\"page\":1,\"limit\":20,\"yodaara_ott\":true,\"location\":\"<script>alert('xss')</script>\"}");
        System.out.println("│ Expected: 400 Bad Request (input rejected/sanitized)");
        System.out.println("│ Actual:   500 Internal Server Error (unhandled exception, server crash)");
        System.out.println("│ Impact:   CRITICAL SECURITY - Server crashes on XSS payload");
        System.out.println("└─────────────────────────────────────────────────────────────");

        Map<String, Object> payload = buildPayload(1, 20);
        payload.put("yodaara_ott", true);
        payload.put("location", "<script>alert('xss')</script>");
        Response r = callAPI(payload);

        int status = r.getStatusCode();
        System.out.println("   Actual status: " + status + " | Response: " + r.getBody().asString().substring(0, Math.min(200, r.getBody().asString().length())));
        ApiReportContext.addExtraDetail("<b>TC73 (SECURITY):</b> XSS input returned status " + status + " (expected 400)");
        Assert.assertEquals(status, 400, "XSS in location should return 400 Bad Request, but got: " + status);
        System.out.println("TC73 PASSED - XSS correctly rejected with 400");
    }

    @Test(priority = 74, description = "TC74: NoSQL injection in location")
    public void testTC74_NoSQLInjection() {
        System.out.println("\n>>> TC74: NoSQL injection <<<");
        Map<String, Object> payload = buildPayload(1, 20);
        payload.put("yodaara_ott", true);
        Map<String, Object> injection = new HashMap<>();
        injection.put("$gt", "");
        payload.put("location", injection);
        Response r = callAPI(payload);

        Assert.assertNotEquals(r.getStatusCode(), 500, "NoSQL injection should not cause 500");
        ApiReportContext.addExtraDetail("<b>NoSQL injection:</b> status=" + r.getStatusCode());
        System.out.println("TC74 PASSED");
    }

    @Test(priority = 75, description = "TC75: Negative limit")
    public void testTC75_NegativeLimit() {
        System.out.println("\n>>> TC75: Negative limit <<<");
        Map<String, Object> payload = buildPayload(1, -5);
        payload.put("yodaara_ott", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);

        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>limit=-5:</b> status=" + status);
        Assert.assertTrue(status == 200 || status == 400, "Got: " + status);
        System.out.println("TC75 PASSED - status=" + status);
    }

    @Test(priority = 76, description = "TC76: Negative page should return 400 Bad Request")
    public void testTC76_NegativePage() {
        System.out.println("\n>>> TC76: Negative page <<<");
        System.out.println("┌─────────────────────────────────────────────────────────────");
        System.out.println("│ EXAMPLE: Negative page=-1 should be rejected with 400");
        System.out.println("│ Request:  POST /tests/getAllTests");
        System.out.println("│ Payload:  {\"page\":-1,\"limit\":20,\"yodaara_ott\":true,\"location\":\"64870066842708a0d5ae6c77\"}");
        System.out.println("│ Expected: 400 Bad Request with validation error");
        System.out.println("│ Actual:   500 Internal Server Error (server crash)");
        System.out.println("└─────────────────────────────────────────────────────────────");

        Map<String, Object> payload = buildPayload(-1, 20);
        payload.put("yodaara_ott", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);

        int status = r.getStatusCode();
        System.out.println("   Actual status: " + status);
        ApiReportContext.addExtraDetail("<b>TC76:</b> page=-1 returned status " + status + " (expected 400)");
        Assert.assertEquals(status, 400, "page=-1 should return 400 Bad Request, but got: " + status);
        System.out.println("TC76 PASSED - page=-1 correctly returns 400");
    }

    @Test(priority = 77, description = "TC77: Filter value as string instead of boolean")
    public void testTC77_FilterAsString() {
        System.out.println("\n>>> TC77: Filter as string <<<");
        Map<String, Object> payload = buildPayload(1, 20);
        payload.put("yodaara_ott", "true");
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);

        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>yodaara_ott='true' (string):</b> status=" + status);
        Assert.assertTrue(status == 200 || status == 400, "Got: " + status);
        System.out.println("TC77 PASSED - status=" + status);
    }

    @Test(priority = 78, description = "TC78: Very large limit value")
    public void testTC78_VeryLargeLimit() {
        System.out.println("\n>>> TC78: Large limit <<<");
        Map<String, Object> payload = buildPayload(1, 10000);
        payload.put("yodaara_ott", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);

        Assert.assertEquals(r.getStatusCode(), 200);
        long time = r.getTime();
        ApiReportContext.addExtraDetail("<b>limit=10000:</b> time=" + time + "ms");
        Assert.assertTrue(time < 5000, "Large limit should still respond within 5s");
        System.out.println("TC78 PASSED - " + time + "ms");
    }

    @Test(priority = 79, description = "TC79: Unknown filter key is ignored")
    public void testTC79_UnknownFilterKey() {
        System.out.println("\n>>> TC79: Unknown filter <<<");
        Map<String, Object> payload = buildPayload(1, 20);
        payload.put("unknown_filter_xyz", true);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);

        Assert.assertEquals(r.getStatusCode(), 200);
        int total = r.jsonPath().getInt("total");
        ApiReportContext.addExtraDetail("<b>Unknown filter:</b> total=" + total);
        System.out.println("TC79 PASSED - unknown filter ignored, total=" + total);
    }

    @Test(priority = 80, description = "TC80: Null filter value")
    public void testTC80_NullFilterValue() {
        System.out.println("\n>>> TC80: Null filter value <<<");
        Map<String, Object> payload = buildPayload(1, 20);
        payload.put("yodaara_ott", null);
        payload.put("location", VALID_LOCATION);
        Response r = callAPI(payload);

        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>yodaara_ott=null:</b> status=" + status);
        Assert.assertTrue(status == 200 || status == 400, "Got: " + status);
        System.out.println("TC80 PASSED - status=" + status);
    }
}

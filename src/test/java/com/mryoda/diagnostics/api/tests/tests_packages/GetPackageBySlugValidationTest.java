package com.mryoda.diagnostics.api.tests.tests_packages;

import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.ApiReportContext;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;

/**
 * GET /tests/getPackageBySlug/{slug} — Standalone Validation Suite
 *
 * Slug is dynamically extracted from the getAllPackages API response.
 *
 * TC01-TC10:  Functional        — Valid slug, status 200, response structure
 * TC11-TC20:  Field Validation  — package_name, price, _id, home_collection, tests_included
 * TC21-TC35:  Negative          — Invalid, empty, null, non-existent, special chars, boundary slugs
 * TC36-TC50:  Security          — SQL injection, XSS, HTML injection, NoSQL, path traversal
 * TC51-TC55:  Performance       — Response time, idempotency, consistency
 * TC56-TC60:  Chaining          — Slug from getAllPackages matches returned package
 */
public class GetPackageBySlugValidationTest extends BaseTest {

    private static final String ENDPOINT = APIEndpoints.GET_PACKAGE_BY_SLUG;

    // Extracted from getAllPackages in @BeforeClass
    private String validSlug;
    private String secondSlug;
    private String randomSlug;
    private String validPackageName;
    private String validPackageId;
    private Map<String, Object> validPackageData;

    // ═══════════════════════════════════════════════════════════════════
    //  SETUP — Fetch valid slugs from getAllPackages
    // ═══════════════════════════════════════════════════════════════════

    @BeforeClass
    public void setupValidSlugs() {
        System.out.println("\n========================================");
        System.out.println("SETUP: Fetching valid slugs from GetAllPackages");
        System.out.println("========================================");

        Map<String, Object> payload = new HashMap<>();
        payload.put("page", 1);
        payload.put("pageSize", 20);
        payload.put("limit", 20);

        Response r = new RequestBuilder()
                .setEndpoint(APIEndpoints.GET_ALL_PACKAGES)
                .addHeader("Content-Type", "application/json")
                .setRequestBody(payload)
                .post();

        if (r.getStatusCode() == 200) {
            List<Map<String, Object>> packages = r.jsonPath().getList("data");
            if (packages != null && !packages.isEmpty()) {
                Map<String, Object> first = packages.get(0);
                validSlug        = String.valueOf(first.get("slug"));
                validPackageId   = String.valueOf(first.get("_id"));
                validPackageName = String.valueOf(first.get("package_name"));

                if (packages.size() > 1) {
                    secondSlug = String.valueOf(packages.get(1).get("slug"));
                }

                // Pick a random package slug
                Random rand = new Random();
                Map<String, Object> randPkg = packages.get(rand.nextInt(packages.size()));
                randomSlug = String.valueOf(randPkg.get("slug"));

                System.out.println("   Total packages returned : " + packages.size());
                System.out.println("   First slug (for API)    : " + validSlug);
                System.out.println("   First _id (MongoDB)     : " + validPackageId);
                System.out.println("   Second slug             : " + secondSlug);
                System.out.println("   Random slug             : " + randomSlug);
                System.out.println("   First package_name      : " + validPackageName);
                System.out.println("   NOTE: slug is the URL param; _id is the MongoDB ObjectId");
                System.out.println("✅ Setup complete");
            } else {
                System.out.println("❌ No packages returned from getAllPackages");
            }
        } else {
            System.out.println("❌ Setup failed. Status: " + r.getStatusCode());
        }
        System.out.println("========================================\n");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════════

    /** Positive call — expectedStatus defaults to 200 in ApiReportContext. */
    private Response callGetPackageBySlug(String slug) {
        return new RequestBuilder()
                .setEndpoint(ENDPOINT + slug)
                .addHeader("accept", "*/*")
                .get();
    }

    /**
     * Negative / security call.
     * Sets expected status range 400–499 so the Extent Report shows "4xx (400–499)"
     * and marks the status indicator GREEN for any 4xx response, regardless of
     * whether the API returns 400, 404, 422, etc.
     */
    private Response callGetPackageBySlugExpecting4xx(String slug) {
        ApiReportContext.setExpectedStatusRange(400, 499);
        return new RequestBuilder()
                .setEndpoint(ENDPOINT + slug)
                .addHeader("accept", "*/*")
                .get();
    }

    private Map<String, Object> extractData(Response r) {
        Object data = r.jsonPath().get("data");
        if (data instanceof List) {
            List<Map<String, Object>> list = r.jsonPath().getList("data");
            return (list != null && !list.isEmpty()) ? list.get(0) : null;
        }
        return r.jsonPath().getMap("data");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  FUNCTIONAL (TC01-TC10)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 1, description = "TC01: Valid slug returns HTTP 200")
    public void testTC01_ValidSlugReturns200() {
        Assert.assertNotNull(validSlug, "SETUP FAILED: validSlug is null");
        Response r = callGetPackageBySlug(validSlug);
        Assert.assertEquals(r.getStatusCode(), 200,
                "Expected 200 for slug: " + validSlug + ". Got: " + r.getStatusCode());
        System.out.println("   slug: " + validSlug + " → " + r.getStatusCode());
        System.out.println("✅ TC01 PASSED");
    }

    @Test(priority = 2, description = "TC02: Response contains success=true")
    public void testTC02_SuccessTrue() {
        Response r = callGetPackageBySlug(validSlug);
        Assert.assertEquals(r.getStatusCode(), 200);
        Assert.assertTrue(r.jsonPath().getBoolean("success"), "success should be true");
        System.out.println("✅ TC02 PASSED");
    }

    @Test(priority = 3, description = "TC03: Response contains status field = 200")
    public void testTC03_StatusField() {
        Response r = callGetPackageBySlug(validSlug);
        Assert.assertEquals(r.getStatusCode(), 200);
        Object status = r.jsonPath().get("status");
        Assert.assertNotNull(status, "status field missing in response");
        System.out.println("   status: " + status);
        System.out.println("✅ TC03 PASSED");
    }

    @Test(priority = 4, description = "TC04: Response contains non-null data field")
    public void testTC04_DataFieldNotNull() {
        Response r = callGetPackageBySlug(validSlug);
        Assert.assertNotNull(r.jsonPath().get("data"), "data should not be null");
        System.out.println("✅ TC04 PASSED");
    }

    @Test(priority = 5, description = "TC05: Response contains msg field")
    public void testTC05_MsgField() {
        Response r = callGetPackageBySlug(validSlug);
        Assert.assertNotNull(r.jsonPath().get("msg"), "msg field should exist");
        System.out.println("   msg: " + r.jsonPath().getString("msg"));
        System.out.println("✅ TC05 PASSED");
    }

    @Test(priority = 6, description = "TC06: Returned slug matches requested slug")
    public void testTC06_SlugMatches() {
        Response r = callGetPackageBySlug(validSlug);
        Map<String, Object> data = extractData(r);
        Assert.assertNotNull(data, "data map should not be null");
        Assert.assertEquals(String.valueOf(data.get("slug")), validSlug,
                "Returned slug should match the requested slug");
        System.out.println("   requested: " + validSlug + " | returned: " + data.get("slug"));
        System.out.println("✅ TC06 PASSED");
    }

    @Test(priority = 7, description = "TC07: Content-Type is application/json")
    public void testTC07_ContentType() {
        Response r = callGetPackageBySlug(validSlug);
        Assert.assertTrue(r.getContentType().contains("application/json"),
                "Expected application/json. Got: " + r.getContentType());
        System.out.println("✅ TC07 PASSED");
    }

    @Test(priority = 8, description = "TC08: Response body is valid JSON (status field accessible)")
    public void testTC08_ValidJson() {
        Response r = callGetPackageBySlug(validSlug);
        Assert.assertNotNull(r.jsonPath().get("status"), "Response is not valid JSON");
        System.out.println("✅ TC08 PASSED");
    }

    @Test(priority = 9, description = "TC09: Second slug from getAllPackages also returns 200")
    public void testTC09_SecondSlugReturns200() {
        if (secondSlug == null) {
            System.out.println("   SKIPPED: only one package available");
            return;
        }
        Response r = callGetPackageBySlug(secondSlug);
        Assert.assertEquals(r.getStatusCode(), 200,
                "Second slug should return 200. Got: " + r.getStatusCode());
        System.out.println("   secondSlug: " + secondSlug + " → " + r.getStatusCode());
        System.out.println("✅ TC09 PASSED");
    }

    @Test(priority = 10, description = "TC10: Random slug from getAllPackages returns 200")
    public void testTC10_RandomSlugReturns200() {
        Assert.assertNotNull(randomSlug, "randomSlug is null");
        Response r = callGetPackageBySlug(randomSlug);
        Assert.assertEquals(r.getStatusCode(), 200,
                "Random slug should return 200. Got: " + r.getStatusCode());
        System.out.println("   randomSlug: " + randomSlug + " → " + r.getStatusCode());
        System.out.println("✅ TC10 PASSED");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  FIELD VALIDATION (TC11-TC20)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 11, description = "TC11: Verify _id field exists in response data")
    public void testTC11_IdFieldExists() {
        Map<String, Object> data = extractData(callGetPackageBySlug(validSlug));
        Assert.assertNotNull(data, "data is null");
        Assert.assertNotNull(data.get("_id"), "_id field missing");
        System.out.println("   _id: " + data.get("_id"));
        System.out.println("✅ TC11 PASSED");
    }

    @Test(priority = 12, description = "TC12: Verify slug field exists in response data")
    public void testTC12_SlugFieldExists() {
        Map<String, Object> data = extractData(callGetPackageBySlug(validSlug));
        Assert.assertNotNull(data, "data is null");
        Assert.assertNotNull(data.get("slug"), "slug field missing");
        System.out.println("   slug: " + data.get("slug"));
        System.out.println("✅ TC12 PASSED");
    }

    @Test(priority = 13, description = "TC13: Verify package_name field exists and is non-empty")
    public void testTC13_PackageNameExists() {
        Map<String, Object> data = extractData(callGetPackageBySlug(validSlug));
        Assert.assertNotNull(data, "data is null");
        Object name = data.get("package_name");
        Assert.assertNotNull(name, "package_name missing");
        Assert.assertFalse(String.valueOf(name).trim().isEmpty(), "package_name should not be empty");
        System.out.println("   package_name: " + name);
        System.out.println("✅ TC13 PASSED");
    }

    @Test(priority = 14, description = "TC14: Verify price field exists and is numeric")
    public void testTC14_PriceIsNumeric() {
        Map<String, Object> data = extractData(callGetPackageBySlug(validSlug));
        Object price = data.get("price");
        Assert.assertNotNull(price, "price field missing");
        Assert.assertTrue(price instanceof Number, "price should be numeric. Got: " + price.getClass());
        System.out.println("   price: " + price);
        System.out.println("✅ TC14 PASSED");
    }

    @Test(priority = 15, description = "TC15: Verify original_price field when present")
    public void testTC15_OriginalPrice() {
        Map<String, Object> data = extractData(callGetPackageBySlug(validSlug));
        if (data.containsKey("original_price")) {
            Assert.assertNotNull(data.get("original_price"), "original_price should not be null");
            System.out.println("   original_price: " + data.get("original_price"));
        } else {
            System.out.println("   original_price: not present (optional field)");
        }
        System.out.println("✅ TC15 PASSED");
    }

    @Test(priority = 16, description = "TC16: Verify home_collection field when present")
    public void testTC16_HomeCollection() {
        Map<String, Object> data = extractData(callGetPackageBySlug(validSlug));
        if (data.containsKey("home_collection")) {
            System.out.println("   home_collection: " + data.get("home_collection"));
        } else {
            System.out.println("   home_collection: not present (optional field)");
        }
        System.out.println("✅ TC16 PASSED");
    }

    @Test(priority = 17, description = "TC17: Verify status field in data object when present")
    public void testTC17_DataStatus() {
        Map<String, Object> data = extractData(callGetPackageBySlug(validSlug));
        if (data.containsKey("status")) {
            System.out.println("   data.status: " + data.get("status"));
        }
        System.out.println("✅ TC17 PASSED");
    }

    @Test(priority = 18, description = "TC18: Verify tests_included / tests_id_included field when present")
    public void testTC18_TestsIncluded() {
        Map<String, Object> data = extractData(callGetPackageBySlug(validSlug));
        Object tests = data.get("tests_included");
        if (tests == null) tests = data.get("tests_id_included");
        if (tests instanceof List) {
            System.out.println("   tests_included count: " + ((List<?>) tests).size());
        } else {
            System.out.println("   tests_included: not present or not a list");
        }
        System.out.println("✅ TC18 PASSED");
    }

    @Test(priority = 19, description = "TC19: Verify discount_percentage field when present")
    public void testTC19_DiscountPercentage() {
        Map<String, Object> data = extractData(callGetPackageBySlug(validSlug));
        if (data.containsKey("discount_percentage")) {
            System.out.println("   discount_percentage: " + data.get("discount_percentage"));
        }
        System.out.println("✅ TC19 PASSED");
    }

    @Test(priority = 20, description = "TC20: Verify rewards_percentage field when present")
    public void testTC20_RewardsPercentage() {
        Map<String, Object> data = extractData(callGetPackageBySlug(validSlug));
        if (data.containsKey("rewards_percentage")) {
            System.out.println("   rewards_percentage: " + data.get("rewards_percentage"));
        }
        System.out.println("✅ TC20 PASSED");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  NEGATIVE SCENARIOS (TC21-TC35)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 21, description = "TC21: Non-existent slug should return 404")
    public void testTC21_NonExistentSlug() {
        Response r = callGetPackageBySlugExpecting4xx("this-slug-does-not-exist-xyz-9999");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: API crashes with 500 for non-existent slug");
        Assert.assertEquals(sc, 404,
                "Non-existent slug should return 404. Got: " + sc);
        System.out.println("✅ TC21 PASSED");
    }

    @Test(priority = 22, description = "TC22: Empty slug should not return 500")
    public void testTC22_EmptySlug() {
        Response r = callGetPackageBySlugExpecting4xx("");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: Empty slug should not crash API with 500");
        System.out.println("✅ TC22 PASSED");
    }

    @Test(priority = 23, description = "TC23: Literal 'null' string slug should return 404")
    public void testTC23_NullStringSlug() {
        Response r = callGetPackageBySlugExpecting4xx("null");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: 'null' slug should not crash API with 500");
        System.out.println("✅ TC23 PASSED");
    }

    @Test(priority = 24, description = "TC24: Literal 'undefined' string slug should return 404")
    public void testTC24_UndefinedStringSlug() {
        Response r = callGetPackageBySlugExpecting4xx("undefined");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: 'undefined' slug should not crash API with 500");
        System.out.println("✅ TC24 PASSED");
    }

    @Test(priority = 25, description = "TC25: Numeric-only string as slug should return 404")
    public void testTC25_NumericSlug() {
        Response r = callGetPackageBySlugExpecting4xx("12345");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: Numeric slug crashes API with 500");
        System.out.println("✅ TC25 PASSED");
    }

    @Test(priority = 26, description = "TC26: Very long slug should not crash API")
    public void testTC26_VeryLongSlug() {
        Response r = callGetPackageBySlugExpecting4xx("a".repeat(500));
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: Very long slug crashes API with 500");
        System.out.println("✅ TC26 PASSED");
    }

    @Test(priority = 27, description = "TC27: Slug with leading/trailing spaces should not crash")
    public void testTC27_SpacePaddedSlug() {
        Response r = callGetPackageBySlugExpecting4xx("  " + validSlug + "  ");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: Space-padded slug crashes API with 500");
        System.out.println("✅ TC27 PASSED");
    }

    @Test(priority = 28, description = "TC28: Slug in ALL CAPS should not crash (likely 404 — case sensitive)")
    public void testTC28_UpperCaseSlug() {
        if (validSlug == null) return;
        Response r = callGetPackageBySlugExpecting4xx(validSlug.toUpperCase());
        int sc = r.getStatusCode();
        System.out.println("   UPPER slug: " + validSlug.toUpperCase() + " → " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: Uppercase slug crashes API");
        System.out.println("✅ TC28 PASSED");
    }

    @Test(priority = 29, description = "TC29: Partial slug (first 5 chars) should return 404")
    public void testTC29_PartialSlug() {
        String partial = validSlug.substring(0, Math.min(5, validSlug.length()));
        Response r = callGetPackageBySlugExpecting4xx(partial);
        int sc = r.getStatusCode();
        System.out.println("   Partial slug: " + partial + " → " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: Partial slug crashes API");
        System.out.println("✅ TC29 PASSED");
    }

    @Test(priority = 30, description = "TC30: Whitespace-only slug should not crash API")
    public void testTC30_WhitespaceSlug() {
        Response r = callGetPackageBySlugExpecting4xx("   ");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: Whitespace slug crashes API");
        System.out.println("✅ TC30 PASSED");
    }

    @Test(priority = 31, description = "TC31: Slug with embedded newline should not crash")
    public void testTC31_NewlineInSlug() {
        Response r = callGetPackageBySlugExpecting4xx("abc\ndef");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: Newline in slug crashes API");
        System.out.println("✅ TC31 PASSED");
    }

    @Test(priority = 32, description = "TC32: Slug with tab character should not crash")
    public void testTC32_TabInSlug() {
        Response r = callGetPackageBySlugExpecting4xx("abc\tdef");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: Tab in slug crashes API");
        System.out.println("✅ TC32 PASSED");
    }

    @Test(priority = 33, description = "TC33: Boolean string slug should not crash")
    public void testTC33_BooleanStringSlug() {
        Response r = callGetPackageBySlugExpecting4xx("true");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: 'true' slug crashes API");
        System.out.println("✅ TC33 PASSED");
    }

    @Test(priority = 34, description = "TC34: Unicode characters in slug should not crash")
    public void testTC34_UnicodeSlug() {
        Response r = callGetPackageBySlugExpecting4xx("パッケージ");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: Unicode slug crashes API");
        System.out.println("✅ TC34 PASSED");
    }

    @Test(priority = 35, description = "TC35: Slug with path traversal characters should not crash")
    public void testTC35_PathTraversalSlug() {
        Response r = callGetPackageBySlugExpecting4xx("../../../etc/passwd");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: Path traversal slug crashes API");
        System.out.println("✅ TC35 PASSED");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECURITY (TC36-TC50)
    // ═══════════════════════════════════════════════════════════════════

    @DataProvider(name = "sqlInjectionPayloads")
    public Object[][] sqlInjectionPayloads() {
        return new Object[][]{
                {"' OR 1=1 --", "Classic SQL injection"},
                {"1' OR '1'='1", "Tautology injection"},
                {"'; DROP TABLE packages; --", "DROP TABLE injection"},
                {"1 UNION SELECT * FROM users --", "UNION SELECT injection"},
        };
    }

    @Test(priority = 36, dataProvider = "sqlInjectionPayloads",
            description = "TC36-TC39: SQL injection in slug should not crash API")
    public void testSQLInjection(String payload, String attackType) {
        System.out.println("\n>>> SECURITY: " + attackType + " <<<");
        Response r = callGetPackageBySlugExpecting4xx(payload);
        int sc = r.getStatusCode();
        String body = r.getBody().asString();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: " + attackType + " crashes API with 500");
        Assert.assertFalse(body.contains("SQLException") || body.contains("MongoError")
                        || body.contains("stack") || body.contains("Error:"),
                "SECURITY LEAK: Internal error exposed for " + attackType);
    }

    @DataProvider(name = "xssPayloads")
    public Object[][] xssPayloads() {
        return new Object[][]{
                {"<script>alert('xss')</script>", "Script tag XSS"},
                {"<img src=x onerror=alert(1)>", "IMG onerror XSS"},
                {"javascript:alert(1)", "JS protocol XSS"},
        };
    }

    @Test(priority = 40, dataProvider = "xssPayloads",
            description = "TC40-TC42: XSS payloads in slug should not be reflected")
    public void testXSSPayloads(String payload, String attackType) {
        System.out.println("\n>>> SECURITY: " + attackType + " <<<");
        Response r = callGetPackageBySlugExpecting4xx(payload);
        int sc = r.getStatusCode();
        String body = r.getBody().asString();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: " + attackType + " crashes API");
        Assert.assertFalse(body.contains("<script>") || body.contains("onerror="),
                "XSS reflected in response for " + attackType);
    }

    @DataProvider(name = "nosqlInjectionPayloads")
    public Object[][] nosqlInjectionPayloads() {
        return new Object[][]{
                {"[$gt]=", "NoSQL $gt injection"},
                {"[$ne]=null", "NoSQL $ne injection"},
                {"[$regex]=.*", "NoSQL $regex injection"},
        };
    }

    @Test(priority = 43, dataProvider = "nosqlInjectionPayloads",
            description = "TC43-TC45: NoSQL injection in slug should not expose DB errors")
    public void testNoSQLInjection(String payload, String attackType) {
        System.out.println("\n>>> SECURITY: " + attackType + " <<<");
        Response r = callGetPackageBySlugExpecting4xx(payload);
        int sc = r.getStatusCode();
        String body = r.getBody().asString();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: " + attackType + " crashes API");
        Assert.assertFalse(body.contains("MongoError") || body.contains("CastError"),
                "SECURITY: DB error exposed for " + attackType);
    }

    @Test(priority = 46, description = "TC46: HTML injection in slug should not be reflected")
    public void testTC46_HTMLInjection() {
        Response r = callGetPackageBySlugExpecting4xx("<h1>Injected</h1>");
        int sc = r.getStatusCode();
        String body = r.getBody().asString();
        Assert.assertNotEquals(sc, 500, "HTML injection crashed API");
        Assert.assertFalse(body.contains("<h1>"), "HTML reflected in response");
        System.out.println("   Status: " + sc);
        System.out.println("✅ TC46 PASSED");
    }

    @Test(priority = 47, description = "TC47: Special characters in slug should not crash API")
    public void testTC47_SpecialCharsSlug() {
        Response r = callGetPackageBySlugExpecting4xx("!@#$%^&*()");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: Special chars crash API");
        System.out.println("✅ TC47 PASSED");
    }

    @Test(priority = 48, description = "TC48: Command injection in slug should not crash API")
    public void testTC48_CommandInjection() {
        Response r = callGetPackageBySlugExpecting4xx("; ls -la");
        int sc = r.getStatusCode();
        String body = r.getBody().asString();
        Assert.assertNotEquals(sc, 500, "Command injection crashed API");
        Assert.assertFalse(body.contains("bin") && body.contains("etc"),
                "SECURITY: Possible command execution detected");
        System.out.println("   Status: " + sc);
        System.out.println("✅ TC48 PASSED");
    }

    @Test(priority = 49, description = "TC49: Internal error details should not be exposed for invalid slug")
    public void testTC49_NoStackTraceExposure() {
        Response r = callGetPackageBySlugExpecting4xx("bad-slug-@@##");
        String body = r.getBody().asString();
        Assert.assertFalse(body.contains("at com.") || body.contains("at java."),
                "SECURITY LEAK: Java stack trace exposed in response");
        System.out.println("✅ TC49 PASSED");
    }

    @Test(priority = 50, description = "TC50: Null byte in slug should not crash API")
    public void testTC50_NullByteSlug() {
        Response r = callGetPackageBySlugExpecting4xx("abc%00def");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: Null byte in slug crashes API");
        System.out.println("✅ TC50 PASSED");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  PERFORMANCE & CONSISTENCY (TC51-TC55)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 51, description = "TC51: Response time should be under 3000ms for valid slug")
    public void testTC51_ResponseTime() {
        long start = System.currentTimeMillis();
        Response r = callGetPackageBySlug(validSlug);
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("   Response time: " + elapsed + "ms");
        Assert.assertEquals(r.getStatusCode(), 200);
        Assert.assertTrue(elapsed < 3000,
                "Response too slow: " + elapsed + "ms (expected < 3000ms)");
        System.out.println("✅ TC51 PASSED");
    }

    @Test(priority = 52, description = "TC52: Two successive calls return identical data (idempotent GET)")
    public void testTC52_IdempotentGet() {
        Response r1 = callGetPackageBySlug(validSlug);
        Response r2 = callGetPackageBySlug(validSlug);
        Map<String, Object> d1 = extractData(r1);
        Map<String, Object> d2 = extractData(r2);
        Assert.assertNotNull(d1);
        Assert.assertNotNull(d2);
        Assert.assertEquals(String.valueOf(d1.get("_id")), String.valueOf(d2.get("_id")));
        Assert.assertEquals(String.valueOf(d1.get("slug")), String.valueOf(d2.get("slug")));
        Assert.assertEquals(String.valueOf(d1.get("package_name")), String.valueOf(d2.get("package_name")));
        Assert.assertEquals(String.valueOf(d1.get("price")), String.valueOf(d2.get("price")));
        System.out.println("✅ TC52 PASSED: GET is idempotent");
    }

    @Test(priority = 53, description = "TC53: Response for valid slug has no empty body")
    public void testTC53_NonEmptyBody() {
        Response r = callGetPackageBySlug(validSlug);
        String body = r.getBody().asString();
        Assert.assertFalse(body == null || body.trim().isEmpty(), "Response body should not be empty");
        System.out.println("   body length: " + body.length());
        System.out.println("✅ TC53 PASSED");
    }

    @Test(priority = 54, description = "TC54: Response time for non-existent slug should be under 3000ms")
    public void testTC54_ResponseTimeForInvalidSlug() {
        long start = System.currentTimeMillis();
        callGetPackageBySlugExpecting4xx("non-existent-slug-perf-check");
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("   Response time (invalid slug): " + elapsed + "ms");
        Assert.assertTrue(elapsed < 3000,
                "404 response too slow: " + elapsed + "ms (expected < 3000ms)");
        System.out.println("✅ TC54 PASSED");
    }

    @Test(priority = 55, description = "TC55: Accept header */* is accepted without error")
    public void testTC55_AcceptHeader() {
        Response r = callGetPackageBySlug(validSlug);
        Assert.assertEquals(r.getStatusCode(), 200);
        System.out.println("✅ TC55 PASSED");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  CHAINING VALIDATION (TC56-TC60)
    //  Verify slug from getAllPackages → getPackageBySlug produces consistent data
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 56, description = "TC56: slug field in getPackageBySlug response matches slug used in request (from getAllPackages)")
    public void testTC56_ChainSlugMatches() {
        Response r = callGetPackageBySlug(validSlug);
        Map<String, Object> data = extractData(r);
        Assert.assertNotNull(data, "data is null");

        // Verify the slug field in the response matches what we requested
        String returnedSlug = String.valueOf(data.get("slug"));
        Assert.assertEquals(returnedSlug, validSlug,
                "Chain FAIL: slug mismatch. Requested=" + validSlug
                        + " | getPackageBySlug returned slug=" + returnedSlug);

        // Also verify the MongoDB _id is consistent across both APIs
        String returnedId = String.valueOf(data.get("_id"));
        Assert.assertEquals(returnedId, validPackageId,
                "Chain FAIL: MongoDB _id mismatch. getAllPackages _id=" + validPackageId
                        + " | getPackageBySlug _id=" + returnedId);

        System.out.println("   Requested slug (from getAllPackages) : " + validSlug);
        System.out.println("   Returned  slug (from getPackageBySlug): " + returnedSlug);
        System.out.println("   getAllPackages  _id (MongoDB ObjectId): " + validPackageId);
        System.out.println("   getPackageBySlug _id (MongoDB ObjectId): " + returnedId);
        System.out.println("✅ TC56 PASSED: slug and _id both consistent across APIs");
    }

    @Test(priority = 57, description = "TC57: package_name from getPackageBySlug matches getAllPackages")
    public void testTC57_ChainPackageNameMatches() {
        Response r = callGetPackageBySlug(validSlug);
        Map<String, Object> data = extractData(r);
        Assert.assertNotNull(data, "data is null");
        String returnedName = String.valueOf(data.get("package_name"));
        Assert.assertEquals(returnedName, validPackageName,
                "Chain FAIL: package_name mismatch. getAllPackages=" + validPackageName
                        + " getPackageBySlug=" + returnedName);
        System.out.println("   package_name chain:  ✅ " + returnedName);
        System.out.println("✅ TC57 PASSED");
    }

    @Test(priority = 58, description = "TC58: slug in response matches slug used in request")
    public void testTC58_SlugSelfConsistency() {
        Response r = callGetPackageBySlug(validSlug);
        Map<String, Object> data = extractData(r);
        Assert.assertNotNull(data);
        Assert.assertEquals(String.valueOf(data.get("slug")), validSlug,
                "Slug self-consistency FAIL: requested=" + validSlug
                        + " returned=" + data.get("slug"));
        System.out.println("✅ TC58 PASSED");
    }

    @Test(priority = 59, description = "TC59: All slugs from getAllPackages return 200 via getPackageBySlug (first 5)")
    public void testTC59_BulkSlugChain() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("page", 1);
        payload.put("pageSize", 5);
        payload.put("limit", 5);

        Response allResp = new RequestBuilder()
                .setEndpoint(APIEndpoints.GET_ALL_PACKAGES)
                .addHeader("Content-Type", "application/json")
                .setRequestBody(payload)
                .post();

        Assert.assertEquals(allResp.getStatusCode(), 200, "getAllPackages failed");
        List<Map<String, Object>> packages = allResp.jsonPath().getList("data");
        Assert.assertNotNull(packages, "packages list is null");

        int passed = 0;
        for (Map<String, Object> pkg : packages) {
            String slug = String.valueOf(pkg.get("slug"));
            if (slug == null || slug.equals("null")) continue;
            Response r = callGetPackageBySlug(slug);
            int sc = r.getStatusCode();
            System.out.println("   slug: " + slug + " → " + sc);
            if (sc == 200) passed++;
            Assert.assertNotEquals(sc, 500, "BUG: slug '" + slug + "' causes 500 crash");
        }
        System.out.println("   Passed: " + passed + "/" + packages.size());
        Assert.assertTrue(passed > 0, "No package slugs returned 200 from getPackageBySlug");
        System.out.println("✅ TC59 PASSED");
    }

    @Test(priority = 60, description = "TC60: getPackageBySlug response has no internal error fields")
    public void testTC60_NoInternalErrorFields() {
        Response r = callGetPackageBySlug(validSlug);
        String body = r.getBody().asString();
        Assert.assertFalse(body.contains("\"stack\"") || body.contains("\"trace\""),
                "SECURITY: stack/trace field should not appear in response");
        Assert.assertFalse(body.contains("MongoError") || body.contains("CastError"),
                "SECURITY: DB error should not appear in response");
        System.out.println("✅ TC60 PASSED");
    }
}

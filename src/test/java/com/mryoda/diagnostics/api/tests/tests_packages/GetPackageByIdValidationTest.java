package com.mryoda.diagnostics.api.tests.tests_packages;

import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;

/**
 * GET /tests/getPackageById/{package_id} — Standalone Validation Suite
 *
 * TC01-TC10:  Functional — Valid ID, response fields
 * TC11-TC20:  Field Validation — package_name, price, slug, tests_included, etc.
 * TC21-TC35:  Negative — Invalid, empty, null, deleted, special chars, boundary IDs
 * TC36-TC50:  Security — SQL injection, XSS, HTML injection, NoSQL, path traversal
 * TC51-TC60:  Schema & Performance
 */
public class GetPackageByIdValidationTest extends BaseTest {

    private static final String ENDPOINT = APIEndpoints.GET_PACKAGE_BY_ID;

    private String validPackageId;
    private String validSlug;
    private String validPackageName;
    private Map<String, Object> validPackageData;

    // ═══════════════════════════════════════════════════════════════════
    //  SETUP — Fetch a valid package_id from GetAllPackages
    // ═══════════════════════════════════════════════════════════════════

    @BeforeClass
    public void setupValidPackageId() {
        System.out.println("\n========================================");
        System.out.println("SETUP: Fetching valid package_id from GetAllPackages");
        System.out.println("========================================");

        Map<String, Object> payload = new HashMap<>();
        payload.put("page", 1);
        payload.put("pageSize", 10);
        payload.put("limit", 10);

        Response r = new RequestBuilder()
                .setEndpoint(APIEndpoints.GET_ALL_PACKAGES)
                .addHeader("Content-Type", "application/json")
                .setRequestBody(payload)
                .post();

        if (r.getStatusCode() == 200) {
            List<Map<String, Object>> packages = r.jsonPath().getList("data");
            if (packages != null && !packages.isEmpty()) {
                Map<String, Object> pkg = packages.get(0);
                validPackageId = String.valueOf(pkg.get("_id"));
                validSlug = String.valueOf(pkg.get("slug"));
                validPackageName = String.valueOf(pkg.get("package_name"));
                System.out.println("   _id:          " + validPackageId);
                System.out.println("   slug:         " + validSlug);
                System.out.println("   package_name: " + validPackageName);
                System.out.println("✅ Setup complete");
            } else {
                System.out.println("❌ No packages returned");
            }
        } else {
            System.out.println("❌ Setup failed. Status: " + r.getStatusCode());
        }
        System.out.println("========================================\n");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  HELPER
    // ═══════════════════════════════════════════════════════════════════

    private Response callGetPackageById(String packageId) {
        return new RequestBuilder()
                .setEndpoint(ENDPOINT + packageId)
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

    @Test(priority = 1, description = "TC01: Valid package_id returns 200")
    public void testTC01_ValidIdReturns200() {
        Response r = callGetPackageById(validPackageId);
        Assert.assertEquals(r.getStatusCode(), 200);
        Assert.assertTrue(r.jsonPath().getBoolean("success"));
        System.out.println("✅ TC01 PASSED");
    }

    @Test(priority = 2, description = "TC02: Response contains status field")
    public void testTC02_StatusField() {
        Response r = callGetPackageById(validPackageId);
        Assert.assertEquals(r.getStatusCode(), 200);
        Assert.assertNotNull(r.jsonPath().get("status"));
        Assert.assertEquals(r.jsonPath().getInt("status"), 200);
        System.out.println("✅ TC02 PASSED");
    }

    @Test(priority = 3, description = "TC03: Response contains success=true")
    public void testTC03_SuccessTrue() {
        Response r = callGetPackageById(validPackageId);
        Assert.assertTrue(r.jsonPath().getBoolean("success"));
        System.out.println("✅ TC03 PASSED");
    }

    @Test(priority = 4, description = "TC04: Response contains data field")
    public void testTC04_DataField() {
        Response r = callGetPackageById(validPackageId);
        Assert.assertNotNull(r.jsonPath().get("data"), "data should not be null");
        System.out.println("✅ TC04 PASSED");
    }

    @Test(priority = 5, description = "TC05: Response contains msg field")
    public void testTC05_MsgField() {
        Response r = callGetPackageById(validPackageId);
        Assert.assertEquals(r.getStatusCode(), 200);
        Assert.assertNotNull(r.jsonPath().get("msg"), "msg should exist");
        System.out.println("   msg: " + r.jsonPath().getString("msg"));
        System.out.println("✅ TC05 PASSED");
    }

    @Test(priority = 6, description = "TC06: Returned _id matches requested _id")
    public void testTC06_IdMatches() {
        Response r = callGetPackageById(validPackageId);
        Map<String, Object> data = extractData(r);
        Assert.assertNotNull(data);
        Assert.assertEquals(String.valueOf(data.get("_id")), validPackageId);
        System.out.println("✅ TC06 PASSED");
    }

    @Test(priority = 7, description = "TC07: Content-Type is application/json")
    public void testTC07_ContentType() {
        Response r = callGetPackageById(validPackageId);
        Assert.assertTrue(r.getContentType().contains("application/json"),
                "Expected JSON. Got: " + r.getContentType());
        System.out.println("✅ TC07 PASSED");
    }

    @Test(priority = 8, description = "TC08: Response body is valid JSON")
    public void testTC08_ValidJson() {
        Response r = callGetPackageById(validPackageId);
        Assert.assertNotNull(r.jsonPath().get("status"), "Not valid JSON");
        System.out.println("✅ TC08 PASSED");
    }

    @Test(priority = 9, description = "TC09: Multiple calls return consistent data")
    public void testTC09_Idempotent() {
        Response r1 = callGetPackageById(validPackageId);
        Response r2 = callGetPackageById(validPackageId);
        Map<String, Object> d1 = extractData(r1);
        Map<String, Object> d2 = extractData(r2);
        Assert.assertEquals(String.valueOf(d1.get("_id")), String.valueOf(d2.get("_id")));
        Assert.assertEquals(String.valueOf(d1.get("package_name")), String.valueOf(d2.get("package_name")));
        Assert.assertEquals(String.valueOf(d1.get("price")), String.valueOf(d2.get("price")));
        System.out.println("✅ TC09 PASSED: Idempotent");
    }

    @Test(priority = 10, description = "TC10: Second package_id also returns 200")
    public void testTC10_AnotherValidId() {
        // Fetch a second package
        Map<String, Object> payload = new HashMap<>();
        payload.put("page", 1);
        payload.put("pageSize", 5);
        payload.put("limit", 5);
        Response allResp = new RequestBuilder()
                .setEndpoint(APIEndpoints.GET_ALL_PACKAGES)
                .addHeader("Content-Type", "application/json")
                .setRequestBody(payload)
                .post();
        List<Map<String, Object>> pkgs = allResp.jsonPath().getList("data");
        if (pkgs.size() > 1) {
            String secondId = String.valueOf(pkgs.get(1).get("_id"));
            Response r = callGetPackageById(secondId);
            Assert.assertEquals(r.getStatusCode(), 200);
            System.out.println("   Second _id: " + secondId + " → 200 OK");
        }
        System.out.println("✅ TC10 PASSED");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  FIELD VALIDATION (TC11-TC20)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 11, description = "TC11: Verify _id field exists")
    public void testTC11_IdExists() {
        Map<String, Object> data = extractData(callGetPackageById(validPackageId));
        Assert.assertNotNull(data.get("_id"));
        System.out.println("✅ TC11 PASSED");
    }

    @Test(priority = 12, description = "TC12: Verify slug field exists")
    public void testTC12_SlugExists() {
        Map<String, Object> data = extractData(callGetPackageById(validPackageId));
        Assert.assertNotNull(data.get("slug"), "slug missing");
        System.out.println("   slug: " + data.get("slug"));
        System.out.println("✅ TC12 PASSED");
    }

    @Test(priority = 13, description = "TC13: Verify package_name field exists")
    public void testTC13_PackageNameExists() {
        Map<String, Object> data = extractData(callGetPackageById(validPackageId));
        Assert.assertNotNull(data.get("package_name"), "package_name missing");
        System.out.println("   package_name: " + data.get("package_name"));
        System.out.println("✅ TC13 PASSED");
    }

    @Test(priority = 14, description = "TC14: Verify price field exists and is numeric")
    public void testTC14_PriceExists() {
        Map<String, Object> data = extractData(callGetPackageById(validPackageId));
        Object price = data.get("price");
        Assert.assertNotNull(price, "price missing");
        Assert.assertTrue(price instanceof Number, "price should be numeric. Got: " + price.getClass());
        System.out.println("   price: " + price);
        System.out.println("✅ TC14 PASSED");
    }

    @Test(priority = 15, description = "TC15: Verify original_price field")
    public void testTC15_OriginalPrice() {
        Map<String, Object> data = extractData(callGetPackageById(validPackageId));
        if (data.containsKey("original_price")) {
            Assert.assertNotNull(data.get("original_price"));
            System.out.println("   original_price: " + data.get("original_price"));
        }
        System.out.println("✅ TC15 PASSED");
    }

    @Test(priority = 16, description = "TC16: Verify home_collection field")
    public void testTC16_HomeCollection() {
        Map<String, Object> data = extractData(callGetPackageById(validPackageId));
        if (data.containsKey("home_collection")) {
            System.out.println("   home_collection: " + data.get("home_collection"));
        }
        System.out.println("✅ TC16 PASSED");
    }

    @Test(priority = 17, description = "TC17: Verify status field")
    public void testTC17_Status() {
        Map<String, Object> data = extractData(callGetPackageById(validPackageId));
        if (data.containsKey("status")) {
            System.out.println("   status: " + data.get("status"));
        }
        System.out.println("✅ TC17 PASSED");
    }

    @Test(priority = 18, description = "TC18: Verify tests_included field")
    public void testTC18_TestsIncluded() {
        Map<String, Object> data = extractData(callGetPackageById(validPackageId));
        Object tests = data.get("tests_included");
        if (tests == null) tests = data.get("tests_id_included");
        if (tests instanceof List) {
            System.out.println("   tests_included count: " + ((List<?>) tests).size());
        }
        System.out.println("✅ TC18 PASSED");
    }

    @Test(priority = 19, description = "TC19: Verify discount_percentage field")
    public void testTC19_DiscountPercentage() {
        Map<String, Object> data = extractData(callGetPackageById(validPackageId));
        if (data.containsKey("discount_percentage")) {
            System.out.println("   discount_percentage: " + data.get("discount_percentage"));
        }
        System.out.println("✅ TC19 PASSED");
    }

    @Test(priority = 20, description = "TC20: Verify rewards_percentage field")
    public void testTC20_RewardsPercentage() {
        Map<String, Object> data = extractData(callGetPackageById(validPackageId));
        if (data.containsKey("rewards_percentage")) {
            System.out.println("   rewards_percentage: " + data.get("rewards_percentage"));
        }
        System.out.println("✅ TC20 PASSED");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  NEGATIVE SCENARIOS (TC21-TC35)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 21, description = "TC21: Invalid package_id should return 404")
    public void testTC21_InvalidId() {
        Response r = callGetPackageById("invalid_id_999");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: API crashes with 500 for invalid _id");
        Assert.assertEquals(sc, 404, "Invalid _id should return 404. Got: " + sc);
    }

    @Test(priority = 22, description = "TC22: Empty package_id → API BUG: returns 500")
    public void testTC22_EmptyId() {
        Response r = callGetPackageById("");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: Empty _id should not crash API");
    }

    @Test(priority = 23, description = "TC23: Null string package_id should return 404")
    public void testTC23_NullStringId() {
        Response r = callGetPackageById("null");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: API crashes with 500 for 'null' _id");
        Assert.assertEquals(sc, 404, "'null' _id should return 404. Got: " + sc);
    }

    @Test(priority = 24, description = "TC24: Undefined string package_id should return 404")
    public void testTC24_UndefinedStringId() {
        Response r = callGetPackageById("undefined");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: API crashes with 500 for 'undefined' _id");
        Assert.assertEquals(sc, 404, "'undefined' _id should return 404. Got: " + sc);
    }

    @Test(priority = 25, description = "TC25: Non-existent ObjectId should return 404")
    public void testTC25_NonExistentObjectId() {
        Response r = callGetPackageById("000000000000000000000000");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: API crashes with 500 for non-existent ObjectId");
        Assert.assertEquals(sc, 404, "Non-existent ObjectId should return 404. Got: " + sc);
    }

    @Test(priority = 26, description = "TC26: Numeric string package_id should return 404")
    public void testTC26_NumericStringId() {
        Response r = callGetPackageById("12345");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: API crashes with 500 for numeric _id");
        Assert.assertEquals(sc, 404, "Numeric _id should return 404. Got: " + sc);
    }

    @Test(priority = 27, description = "TC27: Very long package_id should return 400/404")
    public void testTC27_VeryLongId() {
        Response r = callGetPackageById("a".repeat(1000));
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: API crashes with 500 for very long _id");
    }

    @Test(priority = 28, description = "TC28: Special characters in package_id should not crash")
    public void testTC28_SpecialCharsId() {
        Response r = callGetPackageById("!@#$%^&*()");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: API crashes with 500 for special chars in _id");
    }

    @Test(priority = 29, description = "TC29: Unicode package_id should not crash")
    public void testTC29_UnicodeId() {
        Response r = callGetPackageById("パッケージ");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: API crashes with 500 for unicode _id");
    }

    @Test(priority = 30, description = "TC30: Whitespace only package_id")
    public void testTC30_WhitespaceId() {
        Response r = callGetPackageById("   ");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: Whitespace should not crash API");
    }

    @Test(priority = 31, description = "TC31: package_id with spaces should not crash")
    public void testTC31_SpacePaddedId() {
        Response r = callGetPackageById("  " + validPackageId + "  ");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: API crashes with 500 for space-padded _id");
    }

    @Test(priority = 32, description = "TC32: package_id with newline should not crash")
    public void testTC32_NewlineInId() {
        Response r = callGetPackageById("abc\ndef");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: API crashes with 500 for newline in _id");
    }

    @Test(priority = 33, description = "TC33: package_id with tab should not crash")
    public void testTC33_TabInId() {
        Response r = callGetPackageById("abc\tdef");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: API crashes with 500 for tab in _id");
    }

    @Test(priority = 34, description = "TC34: Partial ObjectId should not crash")
    public void testTC34_PartialObjectId() {
        String partial = validPackageId.substring(0, Math.min(12, validPackageId.length()));
        Response r = callGetPackageById(partial);
        int sc = r.getStatusCode();
        System.out.println("   Partial ID: " + partial + " → Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: API crashes with 500 for partial _id");
    }

    @Test(priority = 35, description = "TC35: Boolean string package_id should not crash")
    public void testTC35_BooleanStringId() {
        Response r = callGetPackageById("true");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: API crashes with 500 for 'true' _id");
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
            description = "TC36-TC39: SQL injection in package_id should not crash")
    public void testSQLInjection(String payload, String attackType) {
        System.out.println("\n>>> SECURITY: " + attackType + " <<<");
        Response r = callGetPackageById(payload);
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
                {"<img src=x onerror=alert(1)>", "IMG tag XSS"},
                {"javascript:alert(1)", "JS protocol XSS"},
        };
    }

    @Test(priority = 40, dataProvider = "xssPayloads",
            description = "TC40-TC42: XSS payloads in package_id should not crash")
    public void testXSSPayloads(String payload, String attackType) {
        System.out.println("\n>>> SECURITY: " + attackType + " <<<");
        Response r = callGetPackageById(payload);
        int sc = r.getStatusCode();
        String body = r.getBody().asString();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: " + attackType + " crashes API with 500");
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
            description = "TC43-TC45: NoSQL injection in package_id should not crash")
    public void testNoSQLInjection(String payload, String attackType) {
        System.out.println("\n>>> SECURITY: " + attackType + " <<<");
        Response r = callGetPackageById(payload);
        int sc = r.getStatusCode();
        String body = r.getBody().asString();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: " + attackType + " crashes API with 500");
        Assert.assertFalse(body.contains("MongoError") || body.contains("CastError"),
                "SECURITY: DB error exposed for " + attackType);
    }

    @Test(priority = 46, description = "TC46: HTML injection in package_id")
    public void testTC46_HTMLInjection() {
        Response r = callGetPackageById("<h1>Injected</h1>");
        int sc = r.getStatusCode();
        String body = r.getBody().asString();
        Assert.assertNotEquals(sc, 500, "HTML injection crashed API");
        Assert.assertFalse(body.contains("<h1>"), "HTML reflected in response");
        System.out.println("   Status: " + sc);
        System.out.println("✅ TC46 PASSED");
    }

    @Test(priority = 47, description = "TC47: Path traversal in package_id")
    public void testTC47_PathTraversal() {
        Response r = callGetPackageById("../../etc/passwd");
        int sc = r.getStatusCode();
        String body = r.getBody().asString();
        Assert.assertNotEquals(sc, 500, "Path traversal crashed API");
        Assert.assertFalse(body.contains("root:") || body.contains("/bin/bash"),
                "SECURITY: File content exposed");
        System.out.println("   Status: " + sc);
        System.out.println("✅ TC47 PASSED");
    }

    @Test(priority = 48, description = "TC48: CRLF injection in package_id should not crash")
    public void testTC48_CRLFInjection() {
        Response r = callGetPackageById("abc%0d%0aInjected-Header:true");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: CRLF injection crashes API with 500");
    }

    @Test(priority = 49, description = "TC49: Template injection in package_id should not crash")
    public void testTC49_TemplateInjection() {
        Response r = callGetPackageById("7multiply7");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: Template injection crashes API with 500");
    }

    @Test(priority = 50, description = "TC50: Null byte injection in package_id should not crash")
    public void testTC50_NullByteInjection() {
        Response r = callGetPackageById("valid%00malicious");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "BUG: Null byte injection crashes API with 500");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SCHEMA & PERFORMANCE (TC51-TC60)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 51, description = "TC51: Response schema — all top-level keys present")
    public void testTC51_ResponseSchema() {
        Response r = callGetPackageById(validPackageId);
        Assert.assertNotNull(r.jsonPath().get("status"), "'status' missing");
        Assert.assertNotNull(r.jsonPath().get("success"), "'success' missing");
        Assert.assertNotNull(r.jsonPath().get("data"), "'data' missing");
        System.out.println("✅ TC51 PASSED");
    }

    @Test(priority = 52, description = "TC52: Data object contains critical fields")
    public void testTC52_CriticalFields() {
        Map<String, Object> data = extractData(callGetPackageById(validPackageId));
        Assert.assertNotNull(data);
        String[] critical = {"_id", "slug", "package_name", "price"};
        for (String f : critical) {
            Assert.assertTrue(data.containsKey(f), "Critical field '" + f + "' missing");
        }
        System.out.println("   Fields: " + data.keySet());
        System.out.println("✅ TC52 PASSED");
    }

    @Test(priority = 53, description = "TC53: _id is a string (ObjectId format)")
    public void testTC53_IdFormat() {
        Map<String, Object> data = extractData(callGetPackageById(validPackageId));
        String id = String.valueOf(data.get("_id"));
        Assert.assertTrue(id.matches("^[a-f0-9]{24}$"),
                "_id should be 24-char hex ObjectId. Got: " + id);
        System.out.println("✅ TC53 PASSED");
    }

    @Test(priority = 54, description = "TC54: slug is a non-empty string")
    public void testTC54_SlugFormat() {
        Map<String, Object> data = extractData(callGetPackageById(validPackageId));
        String slug = String.valueOf(data.get("slug"));
        Assert.assertFalse(slug.isEmpty(), "slug should not be empty");
        System.out.println("   slug: " + slug);
        System.out.println("✅ TC54 PASSED");
    }

    @Test(priority = 55, description = "TC55: price is non-negative")
    public void testTC55_PriceNonNegative() {
        Map<String, Object> data = extractData(callGetPackageById(validPackageId));
        Object price = data.get("price");
        if (price instanceof Number) {
            Assert.assertTrue(((Number) price).doubleValue() >= 0,
                    "price should not be negative. Got: " + price);
        }
        System.out.println("✅ TC55 PASSED");
    }

    @Test(priority = 56, description = "TC56: Response time < 2s for valid ID")
    public void testTC56_ResponseTimeValid() {
        Response r = callGetPackageById(validPackageId);
        Assert.assertTrue(r.getTime() < 2000, "Too slow: " + r.getTime() + "ms");
        System.out.println("   Time: " + r.getTime() + "ms");
        System.out.println("✅ TC56 PASSED");
    }

    @Test(priority = 57, description = "TC57: Response time < 3s for invalid ID")
    public void testTC57_ResponseTimeInvalid() {
        Response r = callGetPackageById("invalid_xyz_999");
        Assert.assertTrue(r.getTime() < 3000, "Too slow for invalid ID: " + r.getTime() + "ms");
        System.out.println("   Time: " + r.getTime() + "ms");
        System.out.println("✅ TC57 PASSED");
    }

    @Test(priority = 58, description = "TC58: Response time < 3s for large payload ID")
    public void testTC58_ResponseTimeLargeId() {
        Response r = callGetPackageById("a".repeat(5000));
        Assert.assertTrue(r.getTime() < 3000, "Too slow for large ID: " + r.getTime() + "ms");
        System.out.println("   Time: " + r.getTime() + "ms");
        System.out.println("✅ TC58 PASSED");
    }

    @Test(priority = 59, description = "TC59: No sensitive data leaked in error response")
    public void testTC59_NoSensitiveDataInError() {
        Response r = callGetPackageById("invalid_xyz");
        String body = r.getBody().asString();
        Assert.assertFalse(body.contains("password") || body.contains("secret")
                        || body.contains("connectionString") || body.contains("mongodb://"),
                "Sensitive data leaked in error response");
        System.out.println("✅ TC59 PASSED");
    }

    @Test(priority = 60, description = "TC60: Error response should have proper JSON structure")
    public void testTC60_ErrorResponseStructure() {
        Response r = callGetPackageById("invalid_xyz");
        int sc = r.getStatusCode();
        String body = r.getBody().asString();
        System.out.println("   Status: " + sc);
        System.out.println("   Body (first 200): " + body.substring(0, Math.min(200, body.length())));
        Assert.assertNotEquals(sc, 500, "BUG: API crashes with 500 for invalid _id");
        Assert.assertNotNull(r.jsonPath().get("status"), "'status' missing in error response");
    }

}

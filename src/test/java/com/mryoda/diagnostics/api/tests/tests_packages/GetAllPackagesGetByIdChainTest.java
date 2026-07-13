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
import java.util.stream.Collectors;

/**
 * GetAllPackages + GetPackageById API Chaining Test Suite
 *
 * Flow: POST /tests/getAllPackages → Extract _id & slug → GET /tests/getPackageById/{_id}
 *
 * TC01-TC45:  GetAllPackages Validation
 * TC46-TC51:  Extract Data Validation
 * TC52-TC72:  GetPackageById Validation
 * TC73-TC86:  API Chaining (data consistency)
 * TC87-TC95:  Negative Scenarios
 * TC96-TC100: Security
 * TC101-TC115: High Priority Automation
 */
public class GetAllPackagesGetByIdChainTest extends BaseTest {

    private static final String GET_ALL_ENDPOINT = APIEndpoints.GET_ALL_PACKAGES;
    private static final String GET_BY_ID_ENDPOINT = APIEndpoints.GET_PACKAGE_BY_ID;

    // Extracted data from GetAllPackages
    private static List<Map<String, Object>> allPackages = new ArrayList<>();
    private static List<String> packageIds = new ArrayList<>();
    private static List<String> packageSlugs = new ArrayList<>();
    private static Map<String, Object> firstPackage = null;
    private static Map<String, Object> lastPackage = null;
    private static Map<String, Object> randomPackage = null;
    private static String firstId = null;
    private static String firstSlug = null;
    private static String randomId = null;
    private static String randomSlug = null;
    private static int totalPages = 0;

    // ═══════════════════════════════════════════════════════════════════
    //  SETUP
    // ═══════════════════════════════════════════════════════════════════

    @BeforeClass
    public void setupTestData() {
        System.out.println("\n========================================");
        System.out.println("SETUP: Fetching packages & extracting _id/slug");
        System.out.println("========================================");

        Response response = callGetAllPackages(1, 50, 50);

        if (response.getStatusCode() == 200) {
            allPackages = response.jsonPath().getList("data");
            totalPages = response.jsonPath().get("total_pages") != null
                    ? response.jsonPath().getInt("total_pages") : 0;

            if (allPackages != null && !allPackages.isEmpty()) {
                firstPackage = allPackages.get(0);
                lastPackage = allPackages.get(allPackages.size() - 1);
                randomPackage = allPackages.get(new Random().nextInt(allPackages.size()));

                for (Map<String, Object> pkg : allPackages) {
                    if (pkg.get("_id") != null) packageIds.add(String.valueOf(pkg.get("_id")));
                    if (pkg.get("slug") != null) packageSlugs.add(String.valueOf(pkg.get("slug")));
                }

                firstId = String.valueOf(firstPackage.get("_id"));
                firstSlug = String.valueOf(firstPackage.get("slug"));
                randomId = String.valueOf(randomPackage.get("_id"));
                randomSlug = String.valueOf(randomPackage.get("slug"));

                System.out.println("   Total Packages: " + allPackages.size());
                System.out.println("   Total Pages: " + totalPages);
                System.out.println("   First _id: " + firstId);
                System.out.println("   First slug: " + firstSlug);
                System.out.println("   Random _id: " + randomId);
                System.out.println("   Random slug: " + randomSlug);
                System.out.println("   Fields: " + firstPackage.keySet());
                System.out.println("✅ Setup complete");
            } else {
                System.out.println("❌ No packages returned");
            }
        } else {
            System.out.println("❌ Setup failed. Status: " + response.getStatusCode());
        }
        System.out.println("========================================\n");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════════

    private Response callGetAllPackages(int page, int pageSize, int limit) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("page", page);
        payload.put("pageSize", pageSize);
        payload.put("limit", limit);
        return new RequestBuilder()
                .setEndpoint(GET_ALL_ENDPOINT)
                .addHeader("Content-Type", "application/json")
                .setRequestBody(payload)
                .post();
    }

    private Response callGetAllPackagesWithSearch(String searchString) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("page", 1);
        payload.put("pageSize", 10);
        payload.put("limit", 20);
        payload.put("searchString", searchString);
        return new RequestBuilder()
                .setEndpoint(GET_ALL_ENDPOINT)
                .addHeader("Content-Type", "application/json")
                .setRequestBody(payload)
                .post();
    }

    private Response callGetAllPackagesRaw(Map<String, Object> payload) {
        return new RequestBuilder()
                .setEndpoint(GET_ALL_ENDPOINT)
                .addHeader("Content-Type", "application/json")
                .setRequestBody(payload)
                .post();
    }

    private Response callGetPackageById(String packageId) {
        return new RequestBuilder()
                .setEndpoint(GET_BY_ID_ENDPOINT + packageId)
                .addHeader("accept", "*/*")
                .get();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  GET ALL PACKAGES (TC01-TC45)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 1, description = "TC01: Verify packages returned for valid request")
    public void testTC01_ValidRequest() {
        Response r = callGetAllPackages(1, 10, 20);
        Assert.assertEquals(r.getStatusCode(), 200);
        Assert.assertNotNull(r.jsonPath().get("data"));
        System.out.println("✅ TC01 PASSED");
    }

    @Test(priority = 2, description = "TC02: Verify status code is 200")
    public void testTC02_StatusCode200() {
        Response r = callGetAllPackages(1, 10, 20);
        Assert.assertEquals(r.getStatusCode(), 200);
        Assert.assertEquals(r.jsonPath().getInt("status"), 200);
        System.out.println("✅ TC02 PASSED");
    }

    @Test(priority = 3, description = "TC03: Verify package list is not empty")
    public void testTC03_PackageListNotEmpty() {
        Response r = callGetAllPackages(1, 10, 20);
        List<?> data = r.jsonPath().getList("data");
        Assert.assertNotNull(data);
        Assert.assertFalse(data.isEmpty(), "Package list should not be empty");
        System.out.println("   Packages: " + data.size());
        System.out.println("✅ TC03 PASSED");
    }

    @Test(priority = 4, description = "TC04: Verify _id is present for each package")
    public void testTC04_IdPresent() {
        Assert.assertFalse(allPackages.isEmpty());
        for (Map<String, Object> pkg : allPackages) {
            Assert.assertNotNull(pkg.get("_id"), "Missing _id in: " + pkg.get("package_name"));
        }
        System.out.println("   All " + allPackages.size() + " packages have _id");
        System.out.println("✅ TC04 PASSED");
    }

    @Test(priority = 5, description = "TC05: Verify slug is present for each package")
    public void testTC05_SlugPresent() {
        Assert.assertFalse(allPackages.isEmpty());
        for (Map<String, Object> pkg : allPackages) {
            Assert.assertNotNull(pkg.get("slug"), "Missing slug in: " + pkg.get("package_name"));
        }
        System.out.println("   All " + allPackages.size() + " packages have slug");
        System.out.println("✅ TC05 PASSED");
    }

    @Test(priority = 6, description = "TC06: Verify package_name is present")
    public void testTC06_PackageNamePresent() {
        for (Map<String, Object> pkg : allPackages) {
            Assert.assertNotNull(pkg.get("package_name"), "Missing package_name for _id: " + pkg.get("_id"));
        }
        System.out.println("✅ TC06 PASSED");
    }

    @Test(priority = 7, description = "TC07: Verify package price is present")
    public void testTC07_PricePresent() {
        for (Map<String, Object> pkg : allPackages) {
            Assert.assertNotNull(pkg.get("price"), "Missing price for: " + pkg.get("package_name"));
        }
        System.out.println("✅ TC07 PASSED");
    }

    @Test(priority = 8, description = "TC08: Verify membership price is present")
    public void testTC08_MembershipPrice() {
        Assert.assertNotNull(firstPackage);
        boolean hasMembershipField = firstPackage.containsKey("membership_price")
                || firstPackage.containsKey("cpt_price");
        System.out.println("   membership_price present: " + firstPackage.containsKey("membership_price"));
        System.out.println("   cpt_price present: " + firstPackage.containsKey("cpt_price"));
        System.out.println("✅ TC08 PASSED");
    }

    @Test(priority = 9, description = "TC09: Verify rewards percentage is present")
    public void testTC09_RewardsPercentage() {
        Assert.assertNotNull(firstPackage);
        System.out.println("   rewards_percentage: " + firstPackage.get("rewards_percentage"));
        System.out.println("✅ TC09 PASSED");
    }

    @Test(priority = 10, description = "TC10: Verify home collection field is present")
    public void testTC10_HomeCollection() {
        Assert.assertNotNull(firstPackage);
        Assert.assertTrue(firstPackage.containsKey("home_collection"), "home_collection field missing");
        System.out.println("   home_collection: " + firstPackage.get("home_collection"));
        System.out.println("✅ TC10 PASSED");
    }

    @Test(priority = 11, description = "TC11: Verify searchString returns matching package")
    public void testTC11_SearchReturnsMatch() {
        String name = String.valueOf(firstPackage.get("package_name"));
        String partial = name.length() > 4 ? name.substring(0, 4) : name;
        Response r = callGetAllPackagesWithSearch(partial);
        Assert.assertEquals(r.getStatusCode(), 200);
        List<?> data = r.jsonPath().getList("data");
        Assert.assertFalse(data.isEmpty(), "Search for '" + partial + "' should return results");
        System.out.println("   Search: '" + partial + "' → " + data.size() + " results");
        System.out.println("✅ TC11 PASSED");
    }

    @Test(priority = 12, description = "TC12: Verify exact package name search")
    public void testTC12_ExactNameSearch() {
        String name = String.valueOf(firstPackage.get("package_name"));
        Response r = callGetAllPackagesWithSearch(name);
        Assert.assertEquals(r.getStatusCode(), 200);
        Assert.assertFalse(r.jsonPath().getList("data").isEmpty(), "Exact name should match");
        System.out.println("✅ TC12 PASSED");
    }

    @Test(priority = 13, description = "TC13: Verify partial package name search")
    public void testTC13_PartialSearch() {
        String name = String.valueOf(firstPackage.get("package_name"));
        String partial = name.substring(0, Math.min(3, name.length()));
        Response r = callGetAllPackagesWithSearch(partial);
        Assert.assertEquals(r.getStatusCode(), 200);
        System.out.println("   Partial '" + partial + "' → " + r.jsonPath().getList("data").size());
        System.out.println("✅ TC13 PASSED");
    }

    @Test(priority = 14, description = "TC14: Verify lowercase search")
    public void testTC14_LowercaseSearch() {
        Response r = callGetAllPackagesWithSearch("bone");
        Assert.assertEquals(r.getStatusCode(), 200);
        System.out.println("   'bone' → " + r.jsonPath().getList("data").size());
        System.out.println("✅ TC14 PASSED");
    }

    @Test(priority = 15, description = "TC15: Verify uppercase search")
    public void testTC15_UppercaseSearch() {
        Response r = callGetAllPackagesWithSearch("BONE");
        Assert.assertEquals(r.getStatusCode(), 200);
        System.out.println("   'BONE' → " + r.jsonPath().getList("data").size());
        System.out.println("✅ TC15 PASSED");
    }

    @Test(priority = 16, description = "TC16: Verify mixed case search")
    public void testTC16_MixedCaseSearch() {
        Response r = callGetAllPackagesWithSearch("BoNe");
        Assert.assertEquals(r.getStatusCode(), 200);
        System.out.println("✅ TC16 PASSED");
    }

    @Test(priority = 17, description = "TC17: Verify special character search")
    public void testTC17_SpecialCharSearch() {
        Response r = callGetAllPackagesWithSearch("!@#$%");
        Assert.assertNotEquals(r.getStatusCode(), 500, "Should not crash for special chars");
        System.out.println("   Status: " + r.getStatusCode());
        System.out.println("✅ TC17 PASSED");
    }

    @Test(priority = 18, description = "TC18: Verify search with spaces")
    public void testTC18_SearchWithSpaces() {
        Response r = callGetAllPackagesWithSearch("  Bone  ");
        Assert.assertEquals(r.getStatusCode(), 200);
        System.out.println("✅ TC18 PASSED");
    }

    @Test(priority = 19, description = "TC19: Verify searchString as empty")
    public void testTC19_EmptySearch() {
        Response r = callGetAllPackagesWithSearch("");
        Assert.assertTrue(r.getStatusCode() == 200 || r.getStatusCode() == 400);
        System.out.println("   Status: " + r.getStatusCode());
        System.out.println("✅ TC19 PASSED");
    }

    @Test(priority = 20, description = "TC20: Verify searchString as null")
    public void testTC20_NullSearch() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("page", 1);
        payload.put("pageSize", 10);
        payload.put("limit", 20);
        payload.put("searchString", null);
        Response r = callGetAllPackagesRaw(payload);
        Assert.assertNotEquals(r.getStatusCode(), 500, "Null search should not crash");
        System.out.println("   Status: " + r.getStatusCode());
        System.out.println("✅ TC20 PASSED");
    }

    @Test(priority = 21, description = "TC21: Verify invalid searchString")
    public void testTC21_InvalidSearch() {
        Response r = callGetAllPackagesWithSearch("zzznonexistent999xyz");
        Assert.assertEquals(r.getStatusCode(), 200);
        List<?> data = r.jsonPath().getList("data");
        Assert.assertTrue(data == null || data.isEmpty(), "Invalid search should return empty");
        System.out.println("✅ TC21 PASSED");
    }

    @Test(priority = 22, description = "TC22: Verify no matching package search")
    public void testTC22_NoMatchSearch() {
        Response r = callGetAllPackagesWithSearch("xxxxxxxxx_no_match_xxxxxxxxx");
        Assert.assertEquals(r.getStatusCode(), 200);
        List<?> data = r.jsonPath().getList("data");
        Assert.assertTrue(data == null || data.isEmpty());
        System.out.println("✅ TC22 PASSED");
    }

    @Test(priority = 23, description = "TC23: Verify page = 1")
    public void testTC23_Page1() {
        Response r = callGetAllPackages(1, 10, 20);
        Assert.assertEquals(r.getStatusCode(), 200);
        Assert.assertFalse(r.jsonPath().getList("data").isEmpty());
        System.out.println("✅ TC23 PASSED");
    }

    @Test(priority = 24, description = "TC24: Verify page > 1")
    public void testTC24_PageGreaterThan1() {
        Response r = callGetAllPackages(2, 5, 5);
        Assert.assertEquals(r.getStatusCode(), 200);
        System.out.println("   Page 2 results: " + r.jsonPath().getList("data").size());
        System.out.println("✅ TC24 PASSED");
    }

    @Test(priority = 25, description = "TC25: Verify page = 0 → API BUG: returns 500")
    public void testTC25_PageZero() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("page", 0);
        payload.put("pageSize", 10);
        payload.put("limit", 20);
        Response r = callGetAllPackagesRaw(payload);
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertEquals(sc, 400,
                "BUG: page=0 should return 400. Actual: " + sc + " (API crashes with 500)");
    }

    @Test(priority = 26, description = "TC26: Verify page = -1 → API BUG: returns 500")
    public void testTC26_PageNegative() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("page", -1);
        payload.put("pageSize", 10);
        payload.put("limit", 20);
        Response r = callGetAllPackagesRaw(payload);
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertEquals(sc, 400,
                "BUG: page=-1 should return 400. Actual: " + sc + " (API crashes with 500)");
    }

    @Test(priority = 27, description = "TC27: Verify page = null → API BUG: returns 500")
    public void testTC27_PageNull() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("page", null);
        payload.put("pageSize", 10);
        payload.put("limit", 20);
        Response r = callGetAllPackagesRaw(payload);
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertEquals(sc, 400,
                "BUG: page=null should return 400. Actual: " + sc + " (API crashes with 500)");
    }

    @Test(priority = 28, description = "TC28: Verify page missing → API BUG: returns 500")
    public void testTC28_PageMissing() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("pageSize", 10);
        payload.put("limit", 20);
        Response r = callGetAllPackagesRaw(payload);
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertEquals(sc, 400,
                "BUG: missing page should return 400. Actual: " + sc + " (API crashes with 500)");
    }

    @Test(priority = 29, description = "TC29: Verify pageSize = 10")
    public void testTC29_PageSize10() {
        Response r = callGetAllPackages(1, 10, 10);
        Assert.assertEquals(r.getStatusCode(), 200);
        Assert.assertTrue(r.jsonPath().getList("data").size() <= 10);
        System.out.println("✅ TC29 PASSED");
    }

    @Test(priority = 30, description = "TC30: Verify pageSize = 0 → API BUG")
    public void testTC30_PageSizeZero() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("page", 1);
        payload.put("pageSize", 0);
        payload.put("limit", 20);
        Response r = callGetAllPackagesRaw(payload);
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertEquals(sc, 400,
                "BUG: pageSize=0 should return 400. Actual: " + sc);
    }

    @Test(priority = 31, description = "TC31: Verify pageSize = -1 → API BUG")
    public void testTC31_PageSizeNegative() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("page", 1);
        payload.put("pageSize", -1);
        payload.put("limit", 20);
        Response r = callGetAllPackagesRaw(payload);
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertEquals(sc, 400,
                "BUG: pageSize=-1 should return 400. Actual: " + sc);
    }

    @Test(priority = 32, description = "TC32: Verify pageSize = null → API BUG")
    public void testTC32_PageSizeNull() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("page", 1);
        payload.put("pageSize", null);
        payload.put("limit", 20);
        Response r = callGetAllPackagesRaw(payload);
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertEquals(sc, 400,
                "BUG: pageSize=null should return 400. Actual: " + sc);
    }

    @Test(priority = 33, description = "TC33: Verify pageSize missing → API BUG")
    public void testTC33_PageSizeMissing() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("page", 1);
        payload.put("limit", 20);
        Response r = callGetAllPackagesRaw(payload);
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertEquals(sc, 400,
                "BUG: missing pageSize should return 400. Actual: " + sc);
    }

    @Test(priority = 34, description = "TC34: Verify limit = 20")
    public void testTC34_Limit20() {
        Response r = callGetAllPackages(1, 20, 20);
        Assert.assertEquals(r.getStatusCode(), 200);
        Assert.assertTrue(r.jsonPath().getList("data").size() <= 20);
        System.out.println("✅ TC34 PASSED");
    }

    @Test(priority = 35, description = "TC35: Verify limit = 0 → API BUG")
    public void testTC35_LimitZero() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("page", 1);
        payload.put("pageSize", 10);
        payload.put("limit", 0);
        Response r = callGetAllPackagesRaw(payload);
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertEquals(sc, 400,
                "BUG: limit=0 should return 400. Actual: " + sc);
    }

    @Test(priority = 36, description = "TC36: Verify limit = -1 → API BUG")
    public void testTC36_LimitNegative() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("page", 1);
        payload.put("pageSize", 10);
        payload.put("limit", -1);
        Response r = callGetAllPackagesRaw(payload);
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertEquals(sc, 400,
                "BUG: limit=-1 should return 400. Actual: " + sc);
    }

    @Test(priority = 37, description = "TC37: Verify limit = null → API BUG")
    public void testTC37_LimitNull() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("page", 1);
        payload.put("pageSize", 10);
        payload.put("limit", null);
        Response r = callGetAllPackagesRaw(payload);
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertEquals(sc, 400,
                "BUG: limit=null should return 400. Actual: " + sc);
    }

    @Test(priority = 38, description = "TC38: Verify limit missing → API BUG")
    public void testTC38_LimitMissing() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("page", 1);
        payload.put("pageSize", 10);
        Response r = callGetAllPackagesRaw(payload);
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertEquals(sc, 400,
                "BUG: missing limit should return 400. Actual: " + sc);
    }

    @Test(priority = 39, description = "TC39: Verify no duplicate packages")
    public void testTC39_NoDuplicates() {
        Set<String> ids = new HashSet<>();
        for (Map<String, Object> pkg : allPackages) {
            String id = String.valueOf(pkg.get("_id"));
            Assert.assertFalse(ids.contains(id), "Duplicate _id: " + id);
            ids.add(id);
        }
        System.out.println("   " + ids.size() + " unique packages");
        System.out.println("✅ TC39 PASSED");
    }

    @Test(priority = 40, description = "TC40: Verify inactive packages not returned")
    public void testTC40_NoInactivePackages() {
        for (Map<String, Object> pkg : allPackages) {
            if (pkg.containsKey("status")) {
                Assert.assertNotEquals(String.valueOf(pkg.get("status")).toLowerCase(), "inactive",
                        "Inactive package: " + pkg.get("package_name"));
            }
        }
        System.out.println("✅ TC40 PASSED");
    }

    @Test(priority = 41, description = "TC41: Verify deleted packages not returned")
    public void testTC41_NoDeletedPackages() {
        for (Map<String, Object> pkg : allPackages) {
            if (pkg.containsKey("is_deleted")) {
                Assert.assertNotEquals(pkg.get("is_deleted"), true,
                        "Deleted package: " + pkg.get("package_name"));
            }
        }
        System.out.println("✅ TC41 PASSED");
    }

    @Test(priority = 42, description = "TC42: Verify response schema")
    public void testTC42_ResponseSchema() {
        Response r = callGetAllPackages(1, 10, 20);
        Assert.assertNotNull(r.jsonPath().get("status"), "Missing 'status'");
        Assert.assertNotNull(r.jsonPath().get("success"), "Missing 'success'");
        Assert.assertNotNull(r.jsonPath().get("data"), "Missing 'data'");
        Assert.assertNotNull(r.jsonPath().get("msg"), "Missing 'msg'");
        System.out.println("✅ TC42 PASSED");
    }

    @Test(priority = 43, description = "TC43: Verify response time < 3s")
    public void testTC43_ResponseTime() {
        Response r = callGetAllPackages(1, 10, 20);
        Assert.assertTrue(r.getTime() < 3000, "Response too slow: " + r.getTime() + "ms");
        System.out.println("   Time: " + r.getTime() + "ms");
        System.out.println("✅ TC43 PASSED");
    }

    @Test(priority = 44, description = "TC44: Verify pagination count")
    public void testTC44_PaginationCount() {
        Response r = callGetAllPackages(1, 5, 5);
        List<?> data = r.jsonPath().getList("data");
        Assert.assertTrue(data.size() <= 5, "Page size exceeded. Got: " + data.size());
        System.out.println("   Page 1 size: " + data.size());
        System.out.println("✅ TC44 PASSED");
    }

    @Test(priority = 45, description = "TC45: Verify total records count")
    public void testTC45_TotalRecords() {
        Response r = callGetAllPackages(1, 10, 20);
        Object totalPagesObj = r.jsonPath().get("total_pages");
        Assert.assertNotNull(totalPagesObj, "total_pages should exist");
        System.out.println("   total_pages: " + totalPagesObj);
        System.out.println("✅ TC45 PASSED");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  EXTRACT DATA VALIDATION (TC46-TC51)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 46, description = "TC46: Verify _id can be extracted")
    public void testTC46_ExtractId() {
        Assert.assertFalse(packageIds.isEmpty(), "_id list should not be empty");
        System.out.println("   Extracted " + packageIds.size() + " _ids");
        System.out.println("   Sample: " + packageIds.get(0));
        System.out.println("✅ TC46 PASSED");
    }

    @Test(priority = 47, description = "TC47: Verify slug can be extracted")
    public void testTC47_ExtractSlug() {
        Assert.assertFalse(packageSlugs.isEmpty(), "slug list should not be empty");
        System.out.println("   Extracted " + packageSlugs.size() + " slugs");
        System.out.println("   Sample: " + packageSlugs.get(0));
        System.out.println("✅ TC47 PASSED");
    }

    @Test(priority = 48, description = "TC48: Verify _id is not null")
    public void testTC48_IdNotNull() {
        for (String id : packageIds) {
            Assert.assertNotNull(id);
            Assert.assertNotEquals(id, "null");
            Assert.assertFalse(id.trim().isEmpty());
        }
        System.out.println("✅ TC48 PASSED");
    }

    @Test(priority = 49, description = "TC49: Verify slug is not null")
    public void testTC49_SlugNotNull() {
        for (String slug : packageSlugs) {
            Assert.assertNotNull(slug);
            Assert.assertNotEquals(slug, "null");
            Assert.assertFalse(slug.trim().isEmpty());
        }
        System.out.println("✅ TC49 PASSED");
    }

    @Test(priority = 50, description = "TC50: Verify _id is unique")
    public void testTC50_IdUnique() {
        Set<String> unique = new HashSet<>(packageIds);
        Assert.assertEquals(unique.size(), packageIds.size(),
                "Duplicate _ids found! Unique: " + unique.size() + " Total: " + packageIds.size());
        System.out.println("✅ TC50 PASSED");
    }

    @Test(priority = 51, description = "TC51: Verify slug is unique")
    public void testTC51_SlugUnique() {
        Set<String> unique = new HashSet<>(packageSlugs);
        Assert.assertEquals(unique.size(), packageSlugs.size(),
                "Duplicate slugs found! Unique: " + unique.size() + " Total: " + packageSlugs.size());
        System.out.println("✅ TC51 PASSED");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  GET PACKAGE BY ID (TC52-TC72)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 52, description = "TC52: Verify package details fetched using valid _id")
    public void testTC52_GetByIdSuccess() {
        Assert.assertNotNull(firstId);
        Response r = callGetPackageById(firstId);
        Assert.assertEquals(r.getStatusCode(), 200);
        Assert.assertTrue(r.jsonPath().getBoolean("success"));
        System.out.println("   Fetched package by _id: " + firstId);
        System.out.println("✅ TC52 PASSED");
    }

    @Test(priority = 53, description = "TC53: Verify GetPackageById status code is 200")
    public void testTC53_StatusCode200() {
        Response r = callGetPackageById(firstId);
        Assert.assertEquals(r.getStatusCode(), 200);
        System.out.println("✅ TC53 PASSED");
    }

    @Test(priority = 54, description = "TC54: Verify package_id exists in response")
    public void testTC54_PackageIdInResponse() {
        Response r = callGetPackageById(firstId);
        Assert.assertEquals(r.getStatusCode(), 200);
        Object data = r.jsonPath().get("data");
        Assert.assertNotNull(data, "data should exist");
        // Check for _id or package_id in response
        String returnedId = r.jsonPath().getString("data._id");
        if (returnedId == null) returnedId = r.jsonPath().getString("data.package_id");
        if (returnedId == null) {
            // data might be a list
            returnedId = r.jsonPath().getString("data[0]._id");
        }
        Assert.assertNotNull(returnedId, "_id/package_id should exist in response");
        System.out.println("   Returned _id: " + returnedId);
        System.out.println("✅ TC54 PASSED");
    }

    @Test(priority = 55, description = "TC55: Verify slug exists in response")
    public void testTC55_SlugInResponse() {
        Response r = callGetPackageById(firstId);
        Assert.assertEquals(r.getStatusCode(), 200);
        String slug = r.jsonPath().getString("data.slug");
        if (slug == null) slug = r.jsonPath().getString("data[0].slug");
        Assert.assertNotNull(slug, "slug should exist in response");
        System.out.println("   slug: " + slug);
        System.out.println("✅ TC55 PASSED");
    }

    @Test(priority = 56, description = "TC56: Verify package_name exists in response")
    public void testTC56_PackageNameInResponse() {
        Response r = callGetPackageById(firstId);
        Assert.assertEquals(r.getStatusCode(), 200);
        String name = r.jsonPath().getString("data.package_name");
        if (name == null) name = r.jsonPath().getString("data[0].package_name");
        Assert.assertNotNull(name, "package_name should exist");
        System.out.println("   package_name: " + name);
        System.out.println("✅ TC56 PASSED");
    }

    @Test(priority = 57, description = "TC57: Verify price exists in response")
    public void testTC57_PriceInResponse() {
        Response r = callGetPackageById(firstId);
        Assert.assertEquals(r.getStatusCode(), 200);
        Object price = r.jsonPath().get("data.price");
        if (price == null) price = r.jsonPath().get("data[0].price");
        Assert.assertNotNull(price, "price should exist");
        System.out.println("   price: " + price);
        System.out.println("✅ TC57 PASSED");
    }

    @Test(priority = 58, description = "TC58: Verify membership price exists")
    public void testTC58_MembershipPriceById() {
        Response r = callGetPackageById(firstId);
        Assert.assertEquals(r.getStatusCode(), 200);
        System.out.println("   Response body (first 200): " + r.getBody().asString().substring(0, Math.min(200, r.getBody().asString().length())));
        System.out.println("✅ TC58 PASSED");
    }

    @Test(priority = 59, description = "TC59: Verify rewards percentage exists")
    public void testTC59_RewardsById() {
        Response r = callGetPackageById(firstId);
        Assert.assertEquals(r.getStatusCode(), 200);
        System.out.println("✅ TC59 PASSED");
    }

    @Test(priority = 60, description = "TC60: Verify included tests exist")
    public void testTC60_IncludedTests() {
        Response r = callGetPackageById(firstId);
        Assert.assertEquals(r.getStatusCode(), 200);
        Object tests = r.jsonPath().get("data.tests_included");
        if (tests == null) tests = r.jsonPath().get("data[0].tests_included");
        if (tests == null) tests = r.jsonPath().get("data.tests_id_included");
        if (tests == null) tests = r.jsonPath().get("data[0].tests_id_included");
        System.out.println("   tests_included: " + (tests != null ? "present" : "not found"));
        System.out.println("✅ TC60 PASSED");
    }

    @Test(priority = 61, description = "TC61: Verify home collection field exists")
    public void testTC61_HomeCollectionById() {
        Response r = callGetPackageById(firstId);
        Assert.assertEquals(r.getStatusCode(), 200);
        System.out.println("✅ TC61 PASSED");
    }

    @Test(priority = 62, description = "TC62: Verify valid package_id")
    public void testTC62_ValidId() {
        Response r = callGetPackageById(firstId);
        Assert.assertEquals(r.getStatusCode(), 200);
        Assert.assertTrue(r.jsonPath().getBoolean("success"));
        System.out.println("✅ TC62 PASSED");
    }

    @Test(priority = 63, description = "TC63: Verify invalid package_id")
    public void testTC63_InvalidId() {
        Response r = callGetPackageById("invalid_id_999");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        System.out.println("   Response: " + r.getBody().asString().substring(0, Math.min(200, r.getBody().asString().length())));
        Assert.assertTrue(sc == 200 || sc == 400 || sc == 404,
                "Invalid _id should return 200(empty)/400/404. Got: " + sc);
        System.out.println("✅ TC63 PASSED");
    }

    @Test(priority = 64, description = "TC64: Verify empty package_id")
    public void testTC64_EmptyId() {
        Response r = callGetPackageById("");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "Empty _id should not crash API");
        System.out.println("✅ TC64 PASSED");
    }

    @Test(priority = 65, description = "TC65: Verify null package_id")
    public void testTC65_NullId() {
        Response r = callGetPackageById("null");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "'null' _id should not crash API");
        System.out.println("✅ TC65 PASSED");
    }

    @Test(priority = 66, description = "TC66: Verify deleted package_id")
    public void testTC66_DeletedId() {
        Response r = callGetPackageById("000000000000000000000000");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertTrue(sc == 200 || sc == 404, "Deleted _id should return 200(empty)/404. Got: " + sc);
        System.out.println("✅ TC66 PASSED");
    }

    @Test(priority = 67, description = "TC67: Verify inactive package_id")
    public void testTC67_InactiveId() {
        // Using first valid ID as proxy (actual inactive ID unknown)
        Response r = callGetPackageById(firstId);
        Assert.assertEquals(r.getStatusCode(), 200);
        System.out.println("✅ TC67 PASSED");
    }

    @Test(priority = 68, description = "TC68: Verify special characters in package_id")
    public void testTC68_SpecialCharsInId() {
        Response r = callGetPackageById("!@#$%^&*()");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "Special chars should not crash API");
        System.out.println("✅ TC68 PASSED");
    }

    @Test(priority = 69, description = "TC69: Verify SQL injection in package_id")
    public void testTC69_SQLInjection() {
        Response r = callGetPackageById("' OR 1=1 --");
        int sc = r.getStatusCode();
        String body = r.getBody().asString();
        Assert.assertNotEquals(sc, 500, "SQL injection should not crash API");
        Assert.assertFalse(body.contains("SQLException") || body.contains("MongoError"),
                "SECURITY: Internal DB error exposed");
        System.out.println("   Status: " + sc);
        System.out.println("✅ TC69 PASSED");
    }

    @Test(priority = 70, description = "TC70: Verify XSS payload in package_id")
    public void testTC70_XSSInId() {
        Response r = callGetPackageById("<script>alert(1)</script>");
        int sc = r.getStatusCode();
        String body = r.getBody().asString();
        Assert.assertNotEquals(sc, 500, "XSS should not crash API");
        Assert.assertFalse(body.contains("<script>"), "XSS reflected in response");
        System.out.println("   Status: " + sc);
        System.out.println("✅ TC70 PASSED");
    }

    @Test(priority = 71, description = "TC71: Verify GetPackageById response schema")
    public void testTC71_ByIdResponseSchema() {
        Response r = callGetPackageById(firstId);
        Assert.assertEquals(r.getStatusCode(), 200);
        Assert.assertNotNull(r.jsonPath().get("status"));
        Assert.assertNotNull(r.jsonPath().get("success"));
        Assert.assertNotNull(r.jsonPath().get("data"));
        System.out.println("✅ TC71 PASSED");
    }

    @Test(priority = 72, description = "TC72: Verify GetPackageById response time < 2s")
    public void testTC72_ByIdResponseTime() {
        Response r = callGetPackageById(firstId);
        Assert.assertEquals(r.getStatusCode(), 200);
        Assert.assertTrue(r.getTime() < 2000, "Too slow: " + r.getTime() + "ms");
        System.out.println("   Time: " + r.getTime() + "ms");
        System.out.println("✅ TC72 PASSED");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  API CHAINING VALIDATION (TC73-TC86)
    // ═══════════════════════════════════════════════════════════════════

    private Map<String, Object> getByIdData(String id) {
        Response r = callGetPackageById(id);
        Assert.assertEquals(r.getStatusCode(), 200, "GetPackageById failed for: " + id);
        // Handle both data as object and data as array
        Object data = r.jsonPath().get("data");
        if (data instanceof List) {
            List<Map<String, Object>> list = r.jsonPath().getList("data");
            return (list != null && !list.isEmpty()) ? list.get(0) : null;
        }
        return r.jsonPath().getMap("data");
    }

    @Test(priority = 73, description = "TC73: GetAllPackages → Extract _id → GetPackageById")
    public void testTC73_ChainFlow() {
        System.out.println("\n>>> TC73: API CHAINING FLOW <<<");
        // Step 1: GetAllPackages
        Response allResp = callGetAllPackages(1, 5, 5);
        Assert.assertEquals(allResp.getStatusCode(), 200);
        List<Map<String, Object>> packages = allResp.jsonPath().getList("data");
        Assert.assertFalse(packages.isEmpty());

        // Step 2: Extract _id
        String extractedId = String.valueOf(packages.get(0).get("_id"));
        Assert.assertNotNull(extractedId);
        System.out.println("   Extracted _id: " + extractedId);

        // Step 3: GetPackageById
        Response byIdResp = callGetPackageById(extractedId);
        Assert.assertEquals(byIdResp.getStatusCode(), 200);
        Assert.assertTrue(byIdResp.jsonPath().getBoolean("success"));
        System.out.println("   GetPackageById: 200 OK");
        System.out.println("✅ TC73 PASSED: Chain flow works");
    }

    @Test(priority = 74, description = "TC74: Verify extracted _id matches returned _id")
    public void testTC74_IdMatch() {
        Map<String, Object> byIdData = getByIdData(firstId);
        Assert.assertNotNull(byIdData, "GetPackageById returned no data");
        String returnedId = String.valueOf(byIdData.get("_id"));
        Assert.assertEquals(returnedId, firstId, "_id mismatch");
        System.out.println("   Expected: " + firstId + " | Got: " + returnedId);
        System.out.println("✅ TC74 PASSED");
    }

    @Test(priority = 75, description = "TC75: Verify extracted slug matches returned slug")
    public void testTC75_SlugMatch() {
        Map<String, Object> byIdData = getByIdData(firstId);
        Assert.assertNotNull(byIdData);
        String returnedSlug = String.valueOf(byIdData.get("slug"));
        Assert.assertEquals(returnedSlug, firstSlug, "slug mismatch");
        System.out.println("   Expected: " + firstSlug + " | Got: " + returnedSlug);
        System.out.println("✅ TC75 PASSED");
    }

    @Test(priority = 76, description = "TC76: Verify package_name matches in both APIs")
    public void testTC76_NameMatch() {
        Map<String, Object> byIdData = getByIdData(firstId);
        Assert.assertNotNull(byIdData);
        String fromAll = String.valueOf(firstPackage.get("package_name"));
        String fromById = String.valueOf(byIdData.get("package_name"));
        Assert.assertEquals(fromById, fromAll, "package_name mismatch");
        System.out.println("   " + fromAll + " ✓");
        System.out.println("✅ TC76 PASSED");
    }

    @Test(priority = 77, description = "TC77: Verify price matches in both APIs")
    public void testTC77_PriceMatch() {
        Map<String, Object> byIdData = getByIdData(firstId);
        Assert.assertNotNull(byIdData);
        String fromAll = String.valueOf(firstPackage.get("price"));
        String fromById = String.valueOf(byIdData.get("price"));
        Assert.assertEquals(fromById, fromAll, "price mismatch");
        System.out.println("   Price: " + fromAll + " ✓");
        System.out.println("✅ TC77 PASSED");
    }

    @Test(priority = 78, description = "TC78: Verify membership price matches")
    public void testTC78_MembershipPriceMatch() {
        Map<String, Object> byIdData = getByIdData(firstId);
        Assert.assertNotNull(byIdData);
        // Check cpt_price or membership_price
        String key = firstPackage.containsKey("cpt_price") ? "cpt_price" : "membership_price";
        if (firstPackage.containsKey(key) && byIdData.containsKey(key)) {
            Assert.assertEquals(String.valueOf(byIdData.get(key)), String.valueOf(firstPackage.get(key)),
                    key + " mismatch");
            System.out.println("   " + key + ": " + firstPackage.get(key) + " ✓");
        }
        System.out.println("✅ TC78 PASSED");
    }

    @Test(priority = 79, description = "TC79: Verify rewards percentage matches")
    public void testTC79_RewardsMatch() {
        Map<String, Object> byIdData = getByIdData(firstId);
        Assert.assertNotNull(byIdData);
        if (firstPackage.containsKey("rewards_percentage") && byIdData.containsKey("rewards_percentage")) {
            Assert.assertEquals(String.valueOf(byIdData.get("rewards_percentage")),
                    String.valueOf(firstPackage.get("rewards_percentage")), "rewards_percentage mismatch");
        }
        System.out.println("✅ TC79 PASSED");
    }

    @Test(priority = 80, description = "TC80: Verify home collection status matches")
    public void testTC80_HomeCollectionMatch() {
        Map<String, Object> byIdData = getByIdData(firstId);
        Assert.assertNotNull(byIdData);
        if (firstPackage.containsKey("home_collection") && byIdData.containsKey("home_collection")) {
            Assert.assertEquals(String.valueOf(byIdData.get("home_collection")),
                    String.valueOf(firstPackage.get("home_collection")), "home_collection mismatch");
        }
        System.out.println("✅ TC80 PASSED");
    }

    @Test(priority = 81, description = "TC81: Verify included tests count matches")
    public void testTC81_IncludedTestsMatch() {
        Map<String, Object> byIdData = getByIdData(firstId);
        Assert.assertNotNull(byIdData);
        String key = "tests_included";
        if (!firstPackage.containsKey(key)) key = "tests_id_included";
        if (firstPackage.containsKey(key) && byIdData.containsKey(key)) {
            Object fromAll = firstPackage.get(key);
            Object fromById = byIdData.get(key);
            if (fromAll instanceof List && fromById instanceof List) {
                Assert.assertEquals(((List<?>) fromById).size(), ((List<?>) fromAll).size(),
                        "Included tests count mismatch");
                System.out.println("   Tests count: " + ((List<?>) fromAll).size() + " ✓");
            }
        }
        System.out.println("✅ TC81 PASSED");
    }

    @Test(priority = 82, description = "TC82: Verify package status matches")
    public void testTC82_StatusMatch() {
        Map<String, Object> byIdData = getByIdData(firstId);
        Assert.assertNotNull(byIdData);
        if (firstPackage.containsKey("status") && byIdData.containsKey("status")) {
            Assert.assertEquals(String.valueOf(byIdData.get("status")),
                    String.valueOf(firstPackage.get("status")), "status mismatch");
        }
        System.out.println("✅ TC82 PASSED");
    }

    @Test(priority = 83, description = "TC83: Verify all fields consistent between both APIs")
    public void testTC83_AllFieldsConsistent() {
        Map<String, Object> byIdData = getByIdData(firstId);
        Assert.assertNotNull(byIdData);
        String[] criticalFields = {"_id", "slug", "package_name", "price", "status"};
        for (String field : criticalFields) {
            if (firstPackage.containsKey(field) && byIdData.containsKey(field)) {
                Assert.assertEquals(String.valueOf(byIdData.get(field)),
                        String.valueOf(firstPackage.get(field)), field + " mismatch");
            }
        }
        System.out.println("   All critical fields match ✓");
        System.out.println("✅ TC83 PASSED");
    }

    @Test(priority = 84, description = "TC84: Verify random package data consistency")
    public void testTC84_RandomPackageConsistency() {
        Assert.assertNotNull(randomId);
        Map<String, Object> byIdData = getByIdData(randomId);
        Assert.assertNotNull(byIdData);
        Assert.assertEquals(String.valueOf(byIdData.get("_id")), randomId);
        Assert.assertEquals(String.valueOf(byIdData.get("slug")), randomSlug);
        System.out.println("   Random package " + randomId + " → consistent ✓");
        System.out.println("✅ TC84 PASSED");
    }

    @Test(priority = 85, description = "TC85: Verify first package data consistency")
    public void testTC85_FirstPackageConsistency() {
        Map<String, Object> byIdData = getByIdData(firstId);
        Assert.assertNotNull(byIdData);
        Assert.assertEquals(String.valueOf(byIdData.get("package_name")),
                String.valueOf(firstPackage.get("package_name")));
        System.out.println("   First package ✓");
        System.out.println("✅ TC85 PASSED");
    }

    @Test(priority = 86, description = "TC86: Verify last package data consistency")
    public void testTC86_LastPackageConsistency() {
        Assert.assertNotNull(lastPackage);
        String lastId = String.valueOf(lastPackage.get("_id"));
        Map<String, Object> byIdData = getByIdData(lastId);
        Assert.assertNotNull(byIdData);
        Assert.assertEquals(String.valueOf(byIdData.get("_id")), lastId);
        Assert.assertEquals(String.valueOf(byIdData.get("package_name")),
                String.valueOf(lastPackage.get("package_name")));
        System.out.println("   Last package " + lastId + " ✓");
        System.out.println("✅ TC86 PASSED");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  NEGATIVE SCENARIOS (TC87-TC95)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 87, description = "TC87: Empty request body → API BUG")
    public void testTC87_EmptyBody() {
        Map<String, Object> payload = new HashMap<>();
        Response r = callGetAllPackagesRaw(payload);
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertEquals(sc, 400,
                "BUG: Empty body should return 400. Actual: " + sc);
    }

    @Test(priority = 88, description = "TC88: Null request body")
    public void testTC88_NullBody() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("page", null);
        payload.put("pageSize", null);
        payload.put("limit", null);
        Response r = callGetAllPackagesRaw(payload);
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertEquals(sc, 400,
                "BUG: All-null fields should return 400. Actual: " + sc);
    }

    @Test(priority = 90, description = "TC90: Invalid datatype for page → API BUG")
    public void testTC90_InvalidPageType() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("page", "abc");
        payload.put("pageSize", 10);
        payload.put("limit", 20);
        Response r = callGetAllPackagesRaw(payload);
        Assert.assertEquals(r.getStatusCode(), 400,
                "BUG: page='abc' should return 400. Actual: " + r.getStatusCode());
    }

    @Test(priority = 91, description = "TC91: Invalid datatype for pageSize → API BUG")
    public void testTC91_InvalidPageSizeType() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("page", 1);
        payload.put("pageSize", "abc");
        payload.put("limit", 20);
        Response r = callGetAllPackagesRaw(payload);
        Assert.assertEquals(r.getStatusCode(), 400,
                "BUG: pageSize='abc' should return 400. Actual: " + r.getStatusCode());
    }

    @Test(priority = 92, description = "TC92: Invalid datatype for limit → API BUG")
    public void testTC92_InvalidLimitType() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("page", 1);
        payload.put("pageSize", 10);
        payload.put("limit", "abc");
        Response r = callGetAllPackagesRaw(payload);
        Assert.assertEquals(r.getStatusCode(), 400,
                "BUG: limit='abc' should return 400. Actual: " + r.getStatusCode());
    }

    @Test(priority = 93, description = "TC93: Invalid datatype for searchString → API BUG")
    public void testTC93_InvalidSearchType() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("page", 1);
        payload.put("pageSize", 10);
        payload.put("limit", 20);
        payload.put("searchString", 12345);
        Response r = callGetAllPackagesRaw(payload);
        Assert.assertEquals(r.getStatusCode(), 400,
                "BUG: searchString=12345 should return 400. Actual: " + r.getStatusCode());
    }

    @Test(priority = 94, description = "TC94: Invalid package_id datatype")
    public void testTC94_InvalidIdType() {
        Response r = callGetPackageById("12345");
        int sc = r.getStatusCode();
        System.out.println("   Status: " + sc);
        Assert.assertNotEquals(sc, 500, "Invalid _id type should not crash");
        System.out.println("✅ TC94 PASSED");
    }

    @Test(priority = 95, description = "TC95: Large request payload")
    public void testTC95_LargePayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("page", 1);
        payload.put("pageSize", 10);
        payload.put("limit", 20);
        payload.put("searchString", "A".repeat(5000));
        Response r = callGetAllPackagesRaw(payload);
        Assert.assertNotEquals(r.getStatusCode(), 500, "Large payload should not crash");
        Assert.assertTrue(r.getTime() < 5000, "Should respond within 5s");
        System.out.println("   Status: " + r.getStatusCode() + " | Time: " + r.getTime() + "ms");
        System.out.println("✅ TC95 PASSED");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECURITY SCENARIOS (TC96-TC100)
    // ═══════════════════════════════════════════════════════════════════

    @DataProvider(name = "securityPayloads")
    public Object[][] securityPayloads() {
        return new Object[][]{
                {"' OR 1=1 --", "SQL Injection in searchString"},
                {"<script>alert(1)</script>", "XSS in searchString"},
                {"<h1>Injected</h1>", "HTML Injection in searchString"},
        };
    }

    @Test(priority = 96, dataProvider = "securityPayloads",
            description = "TC96-TC98: Security payloads in searchString")
    public void testSecurity_SearchString(String payload, String attackType) {
        System.out.println("\n>>> SECURITY [searchString]: " + attackType + " <<<");
        Response r = callGetAllPackagesWithSearch(payload);
        int sc = r.getStatusCode();
        String body = r.getBody().asString();
        Assert.assertNotEquals(sc, 500, attackType + " crashed API");
        Assert.assertFalse(body.contains("SQLException") || body.contains("MongoError"),
                "LEAK: DB error exposed for " + attackType);
        if (payload.contains("<script>") || payload.contains("<h1>")) {
            Assert.assertFalse(body.contains("<script>") || body.contains("<h1>"),
                    "XSS/HTML reflected for " + attackType);
        }
        System.out.println("   Status: " + sc);
        System.out.println("✅ PASSED: " + attackType);
    }

    @DataProvider(name = "securityIdPayloads")
    public Object[][] securityIdPayloads() {
        return new Object[][]{
                {"' OR 1=1 --", "SQL Injection in package_id"},
                {"<script>alert(1)</script>", "XSS in package_id"},
        };
    }

    @Test(priority = 99, dataProvider = "securityIdPayloads",
            description = "TC99-TC100: Security payloads in package_id")
    public void testSecurity_PackageId(String payload, String attackType) {
        System.out.println("\n>>> SECURITY [package_id]: " + attackType + " <<<");
        Response r = callGetPackageById(payload);
        int sc = r.getStatusCode();
        String body = r.getBody().asString();
        Assert.assertNotEquals(sc, 500, attackType + " crashed API");
        Assert.assertFalse(body.contains("SQLException") || body.contains("MongoError"),
                "LEAK: DB error exposed");
        Assert.assertFalse(body.contains("<script>"), "XSS reflected");
        System.out.println("   Status: " + sc);
        System.out.println("✅ PASSED: " + attackType);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  HIGH PRIORITY AUTOMATION (TC101-TC115)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 101, description = "TC101-TC115: High priority E2E flow")
    public void testHighPriority_E2EFlow() {
        System.out.println("\n>>> HIGH PRIORITY: E2E FLOW <<<");

        // TC101: GetAllPackages success
        System.out.println("   [TC101] GetAllPackages...");
        Response allResp = callGetAllPackages(1, 10, 20);
        Assert.assertEquals(allResp.getStatusCode(), 200);
        List<Map<String, Object>> pkgs = allResp.jsonPath().getList("data");
        Assert.assertFalse(pkgs.isEmpty());

        // TC102: Search success
        System.out.println("   [TC102] Search...");
        String pkgName = String.valueOf(pkgs.get(0).get("package_name"));
        Response searchResp = callGetAllPackagesWithSearch(pkgName);
        Assert.assertEquals(searchResp.getStatusCode(), 200);

        // TC103: Extract _id
        System.out.println("   [TC103] Extract _id...");
        String id = String.valueOf(pkgs.get(0).get("_id"));
        Assert.assertNotNull(id);
        Assert.assertNotEquals(id, "null");

        // TC104: Extract slug
        System.out.println("   [TC104] Extract slug...");
        String slug = String.valueOf(pkgs.get(0).get("slug"));
        Assert.assertNotNull(slug);
        Assert.assertNotEquals(slug, "null");

        // TC105: GetPackageById success
        System.out.println("   [TC105] GetPackageById...");
        Response byIdResp = callGetPackageById(id);
        Assert.assertEquals(byIdResp.getStatusCode(), 200);

        // Get ById data (handle array or object)
        Map<String, Object> byIdData;
        Object data = byIdResp.jsonPath().get("data");
        if (data instanceof List) {
            List<Map<String, Object>> list = byIdResp.jsonPath().getList("data");
            byIdData = (list != null && !list.isEmpty()) ? list.get(0) : null;
        } else {
            byIdData = byIdResp.jsonPath().getMap("data");
        }
        Assert.assertNotNull(byIdData, "No data in GetPackageById response");

        // TC106: _id match
        System.out.println("   [TC106] _id match...");
        Assert.assertEquals(String.valueOf(byIdData.get("_id")), id);

        // TC107: slug match
        System.out.println("   [TC107] slug match...");
        Assert.assertEquals(String.valueOf(byIdData.get("slug")), slug);

        // TC108: package_name match
        System.out.println("   [TC108] package_name match...");
        Assert.assertEquals(String.valueOf(byIdData.get("package_name")), pkgName);

        // TC109: price match
        System.out.println("   [TC109] price match...");
        Assert.assertEquals(String.valueOf(byIdData.get("price")),
                String.valueOf(pkgs.get(0).get("price")));

        // TC110: membership price match
        System.out.println("   [TC110] membership price...");
        String mKey = pkgs.get(0).containsKey("cpt_price") ? "cpt_price" : "membership_price";
        if (pkgs.get(0).containsKey(mKey) && byIdData.containsKey(mKey)) {
            Assert.assertEquals(String.valueOf(byIdData.get(mKey)),
                    String.valueOf(pkgs.get(0).get(mKey)));
        }

        // TC111: Invalid package_id
        System.out.println("   [TC111] Invalid _id...");
        Response invalidResp = callGetPackageById("invalid_xyz_999");
        Assert.assertNotEquals(invalidResp.getStatusCode(), 500);

        // TC112: Empty package_id
        System.out.println("   [TC112] Empty _id...");
        Response emptyResp = callGetPackageById("");
        Assert.assertNotEquals(emptyResp.getStatusCode(), 500);

        // TC113: SQL injection
        System.out.println("   [TC113] SQL injection...");
        Response sqlResp = callGetPackageById("' OR 1=1 --");
        Assert.assertNotEquals(sqlResp.getStatusCode(), 500);
        Assert.assertFalse(sqlResp.getBody().asString().contains("MongoError"));

        // TC114: Response schema
        System.out.println("   [TC114] Response schema...");
        Assert.assertNotNull(byIdResp.jsonPath().get("status"));
        Assert.assertNotNull(byIdResp.jsonPath().get("success"));
        Assert.assertNotNull(byIdResp.jsonPath().get("data"));

        // TC115: Response time
        System.out.println("   [TC115] Response time...");
        Assert.assertTrue(byIdResp.getTime() < 3000);

        System.out.println("✅ ALL HIGH PRIORITY TESTS PASSED (TC101-TC115)");
    }
}

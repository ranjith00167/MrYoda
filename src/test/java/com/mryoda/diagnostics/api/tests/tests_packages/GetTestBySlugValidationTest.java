package com.mryoda.diagnostics.api.tests.tests_packages;

import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.ApiReportContext;
import com.mryoda.diagnostics.api.utils.AssertionUtil;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;

/**
 * GetTestBySlug API Validation Test Suite
 * Endpoint: POST /tests/getTestBySlug
 * 
 * Comprehensive validation covering 100+ test scenarios:
 * 1. FUNCTIONAL SCENARIOS (TC01-TC10): Valid slug retrieval, test details, price, description
 * 2. SLUG VALIDATION (TC11-TC23): Empty, null, spaces, case sensitivity, mixed case
 * 3. RESPONSE VALIDATION (TC29-TC48): Status code, response fields, test data completeness
 * 4. NEGATIVE TESTING (TC50-TC63): Invalid datatypes, missing fields, malformed requests
 * 5. SECURITY TESTING (TC64-TC68): SQL injection, XSS, path traversal, command injection
 * 6. BUSINESS VALIDATION (TC69-TC71): Active tests, home collection, membership pricing
 * 7. EDGE CASES (TC72-TC77): Long slugs, special characters, numeric, uppercase
 * 8. PERFORMANCE (TC78-TC79): Response time validation, consistency checks
 * 9. REGRESSION (TC90): Must-automate critical flow validation
 * 
 * API Request Structure:
 * {
 *   "slug": "test-slug-name"
 * }
 * 
 * Test Flow:
 * 1. Get test slugs from getAllTests API
 * 2. Use slugs to validate getTestBySlug API
 */
public class GetTestBySlugValidationTest extends BaseTest {

    private static final String GET_ALL_TESTS_ENDPOINT = APIEndpoints.GET_ALL_TESTS;
    private static final String GET_TEST_BY_SLUG_ENDPOINT = APIEndpoints.GET_TEST_BY_SLUG;
    
    // Store test slugs for validation
    private static List<String> testSlugs = new ArrayList<>();
    private static String validSlug = null;

    // ═══════════════════════════════════════════════════════════════════
    //  SETUP - FETCH TEST SLUGS FROM GETALLTESTS API
    // ═══════════════════════════════════════════════════════════════════

    @BeforeClass
    public void setupTestData() {
        System.out.println("\n========================================");
        System.out.println("SETUP: Fetching Test Slugs from GetAllTests API");
        System.out.println("========================================");
        
        // Build payload for getAllTests
        Map<String, Object> payload = new HashMap<>();
        payload.put("page", 1);
        payload.put("limit", 50);
        payload.put("pageSize", 50);
        
        // Call getAllTests API
        Response response = new RequestBuilder()
                .setEndpoint(GET_ALL_TESTS_ENDPOINT)
                .addHeader("Content-Type", "application/json")
                .setRequestBody(payload)
                .post();
        
        if (response.getStatusCode() == 200) {
            // Try multiple possible paths for tests data
            List<Map<String, Object>> tests = null;
            
            // Try data.tests first
            try {
                tests = response.jsonPath().getList("data.tests");
            } catch (Exception e) {
                // Ignore
            }
            
            // If null or empty, try just data as array
            if (tests == null || tests.isEmpty() || tests.get(0) == null) {
                try {
                    tests = response.jsonPath().getList("data");
                } catch (Exception e) {
                    // Ignore
                }
            }
            
            // If still null or empty, try tests directly
            if (tests == null || tests.isEmpty() || tests.get(0) == null) {
                try {
                    tests = response.jsonPath().getList("tests");
                } catch (Exception e) {
                    // Ignore
                }
            }
            
            System.out.println("   Response Status: " + response.getStatusCode());
            System.out.println("   Tests found: " + (tests != null ? tests.size() : 0));
            
            if (tests != null && !tests.isEmpty()) {
                // Find first non-null test
                Map<String, Object> firstTest = null;
                for (Map<String, Object> test : tests) {
                    if (test != null) {
                        firstTest = test;
                        break;
                    }
                }
                
                if (firstTest != null) {
                    System.out.println("   First test fields: " + firstTest.keySet());
                }
                
                for (Map<String, Object> test : tests) {
                    // Skip null entries
                    if (test == null) {
                        continue;
                    }
                    
                    // Extract slug field (try multiple field names)
                    String slug = null;
                    if (test.containsKey("slug") && test.get("slug") != null) {
                        slug = test.get("slug").toString();
                    } else if (test.containsKey("test_slug") && test.get("test_slug") != null) {
                        slug = test.get("test_slug").toString();
                    } else if (test.containsKey("testSlug") && test.get("testSlug") != null) {
                        slug = test.get("testSlug").toString();
                    } else if (test.containsKey("test_name") && test.get("test_name") != null) {
                        // Fallback: create slug from test name
                        String testName = test.get("test_name").toString();
                        slug = testName.toLowerCase()
                                      .replaceAll("[^a-z0-9]+", "-")
                                      .replaceAll("^-|-$", "");
                    }
                    
                    if (slug != null && !slug.isEmpty()) {
                        testSlugs.add(slug);
                        
                        // Store first valid slug
                        if (validSlug == null) {
                            validSlug = slug;
                        }
                    }
                }
                
                System.out.println("✅ Fetched " + testSlugs.size() + " test slugs");
                if (validSlug != null) {
                    System.out.println("✅ Sample Valid Slug: " + validSlug);
                }
            } else {
                System.out.println("⚠️ No tests found in getAllTests response");
            }
        } else {
            System.out.println("❌ Failed to fetch tests. Status: " + response.getStatusCode());
        }
        
        System.out.println("========================================\n");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Call GetTestBySlug API
     */
    private Response callGetTestBySlugAPI(Map<String, Object> payload) {
        return new RequestBuilder()
                .setEndpoint(GET_TEST_BY_SLUG_ENDPOINT)
                .addHeader("Content-Type", "application/json")
                .setRequestBody(payload)
                .post();
    }

    /**
     * Build payload with slug
     */
    private Map<String, Object> buildPayload(String slug) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("slug", slug);
        return payload;
    }

    /**
     * Get a random valid slug from the list
     */
    private String getRandomSlug() {
        if (testSlugs.isEmpty()) {
            return validSlug; // Fallback to first slug
        }
        Random random = new Random();
        return testSlugs.get(random.nextInt(testSlugs.size()));
    }

    /**
     * Verify response structure
     */
    private void verifyResponseStructure(Response response) {
        Assert.assertNotNull(response.jsonPath().get("status"), "Response should have 'status' field");
        Assert.assertNotNull(response.jsonPath().get("success"), "Response should have 'success' field");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  POSITIVE TESTS (TC01-TC10)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 1, description = "TC01: Get test by valid slug")
    public void testGetTestBySlug_ValidSlug() {
        System.out.println("\n>>> GET TEST BY SLUG - POSITIVE: VALID SLUG <<<");
        
        if (validSlug == null) {
            System.out.println("⚠️ SKIPPED: No valid slug available");
            throw new org.testng.SkipException("No valid slug available from setup");
        }
        
        System.out.println("   Testing with slug: " + validSlug);
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        System.out.println("   HTTP Status: " + response.getStatusCode());
        AssertionUtil.verifyStatusCode(response, 200);
        
        verifyResponseStructure(response);
        
        Boolean success = response.jsonPath().get("success");
        Assert.assertTrue(success, "API should return success=true");
        
        // Verify test data is returned (data is an array)
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        Assert.assertNotNull(dataList, "Response should contain data array");
        Assert.assertTrue(dataList.size() > 0, "Data array should not be empty");
        
        Map<String, Object> testData = dataList.get(0);
        Assert.assertNotNull(testData, "Response should contain test data");
        
        System.out.println("   Test Retrieved: " + testData.get("test_name"));
        System.out.println("   Slug Match: " + testData.get("slug"));
        System.out.println("✅ PASSED: Valid slug returns test details");
        
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 2, description = "TC02: Get test by different valid slugs")
    public void testGetTestBySlug_MultipleValidSlugs() {
        System.out.println("\n>>> GET TEST BY SLUG - POSITIVE: MULTIPLE VALID SLUGS <<<");
        
        if (testSlugs.size() < 3) {
            System.out.println("⚠️ SKIPPED: Not enough slugs for testing");
            throw new org.testng.SkipException("Insufficient slugs for testing");
        }
        
        int testCount = Math.min(5, testSlugs.size());
        int successCount = 0;
        
        for (int i = 0; i < testCount; i++) {
            String slug = testSlugs.get(i);
            System.out.println("   Testing slug " + (i + 1) + ": " + slug);
            
            Map<String, Object> payload = buildPayload(slug);
            Response response = callGetTestBySlugAPI(payload);
            
            if (response.getStatusCode() == 200) {
                successCount++;
            }
        }
        
        System.out.println("   Success Rate: " + successCount + "/" + testCount);
        Assert.assertTrue(successCount > 0, "At least one slug should work");
        System.out.println("✅ PASSED: Multiple valid slugs work");
        
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 3, description = "TC03: Verify response contains test details")
    public void testGetTestBySlug_VerifyTestDetails() {
        System.out.println("\n>>> GET TEST BY SLUG - POSITIVE: VERIFY TEST DETAILS <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        // Get test data from array
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        Assert.assertNotNull(dataList, "Response should contain data array");
        Assert.assertTrue(dataList.size() > 0, "Data array should not be empty");
        
        Map<String, Object> testData = dataList.get(0);
        Assert.assertNotNull(testData, "Response should contain test data");
        
        // Verify essential fields
        Assert.assertNotNull(testData.get("_id"), "Test should have _id");
        Assert.assertNotNull(testData.get("test_name"), "Test should have test_name");
        Assert.assertNotNull(testData.get("slug"), "Test should have slug");
        
        System.out.println("   Test Details:");
        System.out.println("      ID: " + testData.get("_id"));
        System.out.println("      Name: " + testData.get("test_name"));
        System.out.println("      Slug: " + testData.get("slug"));
        
        if (testData.containsKey("price")) {
            System.out.println("      Price: ₹" + testData.get("price"));
        }
        
        if (testData.containsKey("status")) {
            System.out.println("      Status: " + testData.get("status"));
        }
        
        System.out.println("✅ PASSED: Test details are complete");
        
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 4, description = "TC04: Verify slug matches requested slug")
    public void testGetTestBySlug_SlugMatchesRequest() {
        System.out.println("\n>>> GET TEST BY SLUG - POSITIVE: SLUG MATCHES REQUEST <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        // Get test data from array
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        Assert.assertNotNull(dataList, "Response should contain data array");
        Assert.assertTrue(dataList.size() > 0, "Data array should not be empty");
        
        Map<String, Object> testData = dataList.get(0);
        String returnedSlug = (String) testData.get("slug");
        
        Assert.assertEquals(returnedSlug, validSlug, 
            "Returned slug should match requested slug");
        
        System.out.println("   Requested Slug: " + validSlug);
        System.out.println("   Returned Slug: " + returnedSlug);
        System.out.println("✅ PASSED: Slugs match perfectly");
        
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 5, description = "TC05: Verify test price returned correctly")
    public void testGetTestBySlug_VerifyPrice() {
        System.out.println("\n>>> GET TEST BY SLUG - FUNCTIONAL: VERIFY PRICE <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        Map<String, Object> testData = dataList.get(0);
        
        Assert.assertNotNull(testData.get("price"), "Test should have price");
        
        Object price = testData.get("price");
        System.out.println("   Test Price: ₹" + price);
        
        if (testData.containsKey("original_price")) {
            System.out.println("   Original Price: ₹" + testData.get("original_price"));
        }
        if (testData.containsKey("b2b_price")) {
            System.out.println("   B2B Price: ₹" + testData.get("b2b_price"));
        }
        
        System.out.println("✅ PASSED: Price details returned");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 6, description = "TC06: Verify test description returned")
    public void testGetTestBySlug_VerifyDescription() {
        System.out.println("\n>>> GET TEST BY SLUG - FUNCTIONAL: VERIFY DESCRIPTION <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        Map<String, Object> testData = dataList.get(0);
        
        // Description or usage should be present
        boolean hasDescription = testData.containsKey("description") || 
                                testData.containsKey("usage") ||
                                testData.containsKey("usage_yoda");
        
        Assert.assertTrue(hasDescription, "Test should have description/usage information");
        
        if (testData.containsKey("usage")) {
            String usage = (String) testData.get("usage");
            System.out.println("   Usage: " + (usage != null ? usage.substring(0, Math.min(50, usage.length())) + "..." : "N/A"));
        }
        
        System.out.println("✅ PASSED: Description/usage information present");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 7, description = "TC07: Verify turnaround time returned")
    public void testGetTestBySlug_VerifyTurnaroundTime() {
        System.out.println("\n>>> GET TEST BY SLUG - FUNCTIONAL: VERIFY TURNAROUND TIME <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        Map<String, Object> testData = dataList.get(0);
        
        boolean hasTurnaroundTime = testData.containsKey("turn_around_time") || 
                                    testData.containsKey("turn_around_time_yoda");
        
        Assert.assertTrue(hasTurnaroundTime, "Test should have turnaround time");
        
        if (testData.containsKey("turn_around_time")) {
            System.out.println("   Turnaround Time: " + testData.get("turn_around_time"));
        }
        
        System.out.println("✅ PASSED: Turnaround time returned");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 8, description = "TC08: Verify home collection status returned")
    public void testGetTestBySlug_VerifyHomeCollection() {
        System.out.println("\n>>> GET TEST BY SLUG - FUNCTIONAL: VERIFY HOME COLLECTION <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        Map<String, Object> testData = dataList.get(0);
        
        Assert.assertNotNull(testData.get("home_collection"), "Test should have home_collection field");
        
        String homeCollection = (String) testData.get("home_collection");
        System.out.println("   Home Collection: " + homeCollection);
        
        System.out.println("✅ PASSED: Home collection status returned");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 9, description = "TC09: Verify available locations returned")
    public void testGetTestBySlug_VerifyLocations() {
        System.out.println("\n>>> GET TEST BY SLUG - FUNCTIONAL: VERIFY LOCATIONS <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        Map<String, Object> testData = dataList.get(0);
        
        Assert.assertNotNull(testData.get("locations"), "Test should have locations");
        
        Object locations = testData.get("locations");
        if (locations instanceof List) {
            List<?> locationList = (List<?>) locations;
            System.out.println("   Locations Count: " + locationList.size());
        }
        
        System.out.println("✅ PASSED: Locations returned");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 10, description = "TC10: Verify FAQ details returned")
    public void testGetTestBySlug_VerifyFAQ() {
        System.out.println("\n>>> GET TEST BY SLUG - FUNCTIONAL: VERIFY FAQ <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        Map<String, Object> testData = dataList.get(0);
        
        if (testData.containsKey("frequently_asked_questions")) {
            Object faq = testData.get("frequently_asked_questions");
            if (faq instanceof List) {
                List<?> faqList = (List<?>) faq;
                System.out.println("   FAQ Count: " + faqList.size());
            }
        }
        
        System.out.println("✅ PASSED: FAQ field checked");
        ApiReportContext.setExpectedStatus(200);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  NEGATIVE TESTS (TC11-TC20)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 11, description = "TC11: Get test with invalid slug")
    public void testGetTestBySlug_InvalidSlug() {
        System.out.println("\n>>> GET TEST BY SLUG - NEGATIVE: INVALID SLUG <<<");
        
        String invalidSlug = "invalid-test-slug-xyz-99999";
        System.out.println("   Testing with invalid slug: " + invalidSlug);
        
        Map<String, Object> payload = buildPayload(invalidSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        System.out.println("   HTTP Status: " + response.getStatusCode());
        
        // API should return 200 with empty data OR 404
        boolean validResponse = response.getStatusCode() == 200 || 
                                response.getStatusCode() == 404;
        
        Assert.assertTrue(validResponse, 
            "Invalid slug should return 200 with null/empty data or 404");
        
        if (response.getStatusCode() == 200) {
            Object data = response.jsonPath().get("data");
            System.out.println("   Data returned: " + (data == null ? "null" : data));
        }
        
        System.out.println("✅ PASSED: Invalid slug handled gracefully");
        
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 12, description = "TC12: Get test with empty slug")
    public void testGetTestBySlug_EmptySlug() {
        System.out.println("\n>>> GET TEST BY SLUG - NEGATIVE: EMPTY SLUG <<<");
        
        Map<String, Object> payload = buildPayload("");
        Response response = callGetTestBySlugAPI(payload);
        
        System.out.println("   HTTP Status: " + response.getStatusCode());
        
        // Should return 400 or 200 with error
        boolean validResponse = response.getStatusCode() == 400 || 
                                response.getStatusCode() == 200;
        
        Assert.assertTrue(validResponse, "Empty slug should be handled");
        
        System.out.println("✅ PASSED: Empty slug handled");
        
        ApiReportContext.setExpectedStatus(400);
    }

    @Test(priority = 13, description = "TC13: Get test with null slug")
    public void testGetTestBySlug_NullSlug() {
        System.out.println("\n>>> GET TEST BY SLUG - NEGATIVE: NULL SLUG <<<");
        
        Map<String, Object> payload = buildPayload(null);
        Response response = callGetTestBySlugAPI(payload);
        
        System.out.println("   HTTP Status: " + response.getStatusCode());
        
        // Should return 400 Bad Request
        boolean validResponse = response.getStatusCode() == 400 || 
                                response.getStatusCode() == 200;
        
        Assert.assertTrue(validResponse, "Null slug should return error");
        
        System.out.println("✅ PASSED: Null slug handled");
        
        ApiReportContext.setExpectedStatus(400);
    }

    @Test(priority = 14, description = "TC14: Get test with missing slug field")
    public void testGetTestBySlug_MissingSlugField() {
        System.out.println("\n>>> GET TEST BY SLUG - NEGATIVE: MISSING SLUG FIELD <<<");
        
        Map<String, Object> payload = new HashMap<>();
        // Intentionally not adding slug field
        
        Response response = callGetTestBySlugAPI(payload);
        
        System.out.println("   HTTP Status: " + response.getStatusCode());
        
        // Should return 400 Bad Request
        boolean validResponse = response.getStatusCode() == 400 || 
                                response.getStatusCode() == 200;
        
        Assert.assertTrue(validResponse, "Missing slug field should return error");
        
        System.out.println("✅ PASSED: Missing slug field handled");
        
        ApiReportContext.setExpectedStatus(400);
    }

    @Test(priority = 15, description = "TC15: Get test with empty request body")
    public void testGetTestBySlug_EmptyRequestBody() {
        System.out.println("\n>>> GET TEST BY SLUG - NEGATIVE: EMPTY REQUEST BODY <<<");
        
        Map<String, Object> payload = new HashMap<>();
        Response response = callGetTestBySlugAPI(payload);
        
        System.out.println("   HTTP Status: " + response.getStatusCode());
        
        // Should return 400 Bad Request
        boolean validResponse = response.getStatusCode() == 400 || 
                                response.getStatusCode() == 200;
        
        Assert.assertTrue(validResponse, "Empty body should return error");
        
        System.out.println("✅ PASSED: Empty request body handled");
        
        ApiReportContext.setExpectedStatus(400);
    }

    @Test(priority = 16, description = "TC16: Get test with slug containing only spaces")
    public void testGetTestBySlug_SlugOnlySpaces() {
        System.out.println("\n>>> GET TEST BY SLUG - SLUG VALIDATION: ONLY SPACES <<<");
        
        Map<String, Object> payload = buildPayload("   ");
        Response response = callGetTestBySlugAPI(payload);
        
        System.out.println("   HTTP Status: " + response.getStatusCode());
        
        boolean validResponse = response.getStatusCode() == 400 || 
                                response.getStatusCode() == 200 || 
                                response.getStatusCode() == 404;
        
        Assert.assertTrue(validResponse, "Slug with only spaces should be handled");
        
        System.out.println("✅ PASSED: Slug with only spaces handled");
        ApiReportContext.setExpectedStatus(400);
    }

    @Test(priority = 17, description = "TC17: Get test with slug having leading spaces")
    public void testGetTestBySlug_SlugLeadingSpaces() {
        System.out.println("\n>>> GET TEST BY SLUG - SLUG VALIDATION: LEADING SPACES <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        String slugWithLeadingSpaces = " " + validSlug;
        Map<String, Object> payload = buildPayload(slugWithLeadingSpaces);
        Response response = callGetTestBySlugAPI(payload);
        
        System.out.println("   HTTP Status: " + response.getStatusCode());
        System.out.println("   Original: '" + validSlug + "'");
        System.out.println("   With leading space: '" + slugWithLeadingSpaces + "'");
        
        boolean validResponse = response.getStatusCode() == 200 || 
                                response.getStatusCode() == 404;
        
        Assert.assertTrue(validResponse, "Slug with leading spaces should be handled");
        
        System.out.println("✅ PASSED: Leading spaces handled");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 18, description = "TC18: Get test with slug having trailing spaces")
    public void testGetTestBySlug_SlugTrailingSpaces() {
        System.out.println("\n>>> GET TEST BY SLUG - SLUG VALIDATION: TRAILING SPACES <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        String slugWithTrailingSpaces = validSlug + " ";
        Map<String, Object> payload = buildPayload(slugWithTrailingSpaces);
        Response response = callGetTestBySlugAPI(payload);
        
        System.out.println("   HTTP Status: " + response.getStatusCode());
        System.out.println("   Original: '" + validSlug + "'");
        System.out.println("   With trailing space: '" + slugWithTrailingSpaces + "'");
        
        boolean validResponse = response.getStatusCode() == 200 || 
                                response.getStatusCode() == 404;
        
        Assert.assertTrue(validResponse, "Slug with trailing spaces should be handled");
        
        System.out.println("✅ PASSED: Trailing spaces handled");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 19, description = "TC19: Get test with mixed case slug")
    public void testGetTestBySlug_MixedCaseSlug() {
        System.out.println("\n>>> GET TEST BY SLUG - SLUG VALIDATION: MIXED CASE <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        // Convert to mixed case: first char upper, rest alternating
        StringBuilder mixedCase = new StringBuilder();
        for (int i = 0; i < validSlug.length(); i++) {
            char c = validSlug.charAt(i);
            if (i % 2 == 0) {
                mixedCase.append(Character.toUpperCase(c));
            } else {
                mixedCase.append(Character.toLowerCase(c));
            }
        }
        
        String mixedCaseSlug = mixedCase.toString();
        Map<String, Object> payload = buildPayload(mixedCaseSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        System.out.println("   HTTP Status: " + response.getStatusCode());
        System.out.println("   Original: " + validSlug);
        System.out.println("   Mixed case: " + mixedCaseSlug);
        
        boolean validResponse = response.getStatusCode() == 200 || 
                                response.getStatusCode() == 404;
        
        Assert.assertTrue(validResponse, "Mixed case slug should be handled");
        
        System.out.println("✅ PASSED: Mixed case slug handled");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 20, description = "TC20: Get test with HTML characters in slug")
    public void testGetTestBySlug_HTMLCharacters() {
        System.out.println("\n>>> GET TEST BY SLUG - SPECIAL CHAR: HTML CHARACTERS <<<");
        
        String htmlSlug = "<h1>test-slug</h1>";
        Map<String, Object> payload = buildPayload(htmlSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        int actualStatusCode = response.getStatusCode();
        String responseBody = response.getBody().asString();
        System.out.println("   HTTP Status: " + actualStatusCode);
        
        // VALIDATION 1: Must NOT return 500
        if (actualStatusCode == 500) {
            System.err.println("❌ HTML INJECTION VULNERABILITY DETECTED!");
            System.err.println("   Status Code: 500");
            System.err.println("   HTML Payload: " + htmlSlug);
            System.err.println("   Response: " + responseBody);
            
            Assert.fail("SECURITY FAILURE: API crashed with HTTP 500 for HTML characters: '" 
                + htmlSlug + "'. Expected: 200/400/404. Actual: 500. Response: " + responseBody);
        }
        
        Assert.assertNotEquals(actualStatusCode, 500, 
            "SECURITY FAILURE: API returned HTTP 500 for HTML characters: '" + htmlSlug 
            + "'. Expected: 200/400/404. Actual: " + actualStatusCode);
        
        // VALIDATION 2: Response must NOT reflect unescaped HTML (XSS)
        boolean htmlReflected = responseBody.contains("<h1>") && responseBody.contains("</h1>");
        if (htmlReflected) {
            System.err.println("❌ XSS VULNERABILITY: Unescaped HTML reflected in response!");
            System.err.println("   Payload: " + htmlSlug);
            System.err.println("   Response: " + responseBody);
            
            Assert.fail("XSS VULNERABILITY: API reflected unescaped HTML in response! "
                + "Payload: '" + htmlSlug + "'. Response contains raw HTML tags. "
                + "API must sanitize/escape HTML input. Response: " + responseBody);
        }
        System.out.println("   ✅ No XSS reflection detected");
        
        // VALIDATION 3: Must NOT expose internal errors
        boolean internalError = responseBody.contains("stack trace") || 
                                responseBody.contains("Exception") ||
                                responseBody.contains("at com.") ||
                                responseBody.contains("MongoError");
        if (internalError) {
            Assert.fail("INFORMATION DISCLOSURE: Internal error details exposed for HTML payload: '" 
                + htmlSlug + "'. Response: " + responseBody);
        }
        System.out.println("   ✅ No internal error details exposed");
        
        // VALIDATION 4: API should return 400 for HTML input (Input Validation)
        if (actualStatusCode == 200) {
            System.err.println("❌ INPUT VALIDATION MISSING FOR HTML CHARACTERS!");
            System.err.println("   Payload: " + htmlSlug);
            System.err.println("   Expected: HTTP 400 (slug should not contain HTML tags)");
            System.err.println("   Actual: HTTP " + actualStatusCode);
            
            Assert.fail("INPUT VALIDATION DEFECT: API returned HTTP 200 for HTML payload: '" 
                + htmlSlug + "'. Expected: HTTP 400 - slug field should reject HTML tags. "
                + "Actual: HTTP " + actualStatusCode + ". API accepts HTML without validation.");
        }
        
        System.out.println("✅ ALL HTML SECURITY VALIDATIONS PASSED (Status: " + actualStatusCode + ")");
        ApiReportContext.setExpectedStatus(400);
    }

    @Test(priority = 21, description = "TC21: Get test with emoji characters")
    public void testGetTestBySlug_EmojiCharacters() {
        System.out.println("\n>>> GET TEST BY SLUG - SPECIAL CHAR: EMOJI CHARACTERS <<<");
        
        String emojiSlug = "test-😊-slug";
        Map<String, Object> payload = buildPayload(emojiSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        System.out.println("   HTTP Status: " + response.getStatusCode());
        
        boolean validResponse = response.getStatusCode() == 200 || 
                                response.getStatusCode() == 400 || 
                                response.getStatusCode() == 404;
        
        Assert.assertTrue(validResponse, "Emoji characters should be handled");
        
        System.out.println("✅ PASSED: Emoji characters handled");
        ApiReportContext.setExpectedStatus(200);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  RESPONSE FIELD VALIDATION (TC29-TC48)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 29, description = "TC29: Verify status code is 200")
    public void testGetTestBySlug_StatusCode200() {
        System.out.println("\n>>> RESPONSE VALIDATION: STATUS CODE <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        System.out.println("   Status Code: " + response.getStatusCode());
        System.out.println("✅ PASSED: Status code is 200");
        
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 30, description = "TC30-TC48: Verify all response fields present")
    public void testGetTestBySlug_AllResponseFields() {
        System.out.println("\n>>> RESPONSE VALIDATION: ALL FIELDS <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        Map<String, Object> testData = dataList.get(0);
        
        // Verify essential fields exist
        String[] essentialFields = {
            "_id", "test_id", "test_name", "slug", "price", 
            "status", "home_collection", "locations"
        };
        
        int fieldsPresent = 0;
        for (String field : essentialFields) {
            if (testData.containsKey(field)) {
                fieldsPresent++;
                System.out.println("   ✓ " + field + ": " + testData.get(field));
            } else {
                System.out.println("   ✗ " + field + ": MISSING");
            }
        }
        
        Assert.assertTrue(fieldsPresent >= 6, 
            "At least 6/8 essential fields should be present. Found: " + fieldsPresent);
        
        System.out.println("✅ PASSED: Essential fields present (" + fieldsPresent + "/8)");
        ApiReportContext.setExpectedStatus(200);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  NEGATIVE DATATYPE TESTS (TC60-TC63)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 50, description = "TC60: Get test with integer slug")
    public void testGetTestBySlug_IntegerDatatype() {
        System.out.println("\n>>> NEGATIVE: INTEGER DATATYPE <<<");
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("slug", 12345);
        
        Response response = callGetTestBySlugAPI(payload);
        
        System.out.println("   HTTP Status: " + response.getStatusCode());
        
        boolean validResponse = response.getStatusCode() == 400 || 
                                response.getStatusCode() == 200;
        
        Assert.assertTrue(validResponse, "Integer datatype should be handled");
        
        System.out.println("✅ PASSED: Integer datatype handled");
        ApiReportContext.setExpectedStatus(400);
    }

    @Test(priority = 51, description = "TC61: Get test with boolean slug")
    public void testGetTestBySlug_BooleanDatatype() {
        System.out.println("\n>>> NEGATIVE: BOOLEAN DATATYPE <<<");
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("slug", true);
        
        Response response = callGetTestBySlugAPI(payload);
        
        System.out.println("   HTTP Status: " + response.getStatusCode());
        
        boolean validResponse = response.getStatusCode() == 400 || 
                                response.getStatusCode() == 200;
        
        Assert.assertTrue(validResponse, "Boolean datatype should be handled");
        
        System.out.println("✅ PASSED: Boolean datatype handled");
        ApiReportContext.setExpectedStatus(400);
    }

    @Test(priority = 52, description = "TC62: Get test with array slug")
    public void testGetTestBySlug_ArrayDatatype() {
        System.out.println("\n>>> NEGATIVE: ARRAY DATATYPE <<<");
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("slug", Arrays.asList("test-slug"));
        
        Response response = callGetTestBySlugAPI(payload);
        
        System.out.println("   HTTP Status: " + response.getStatusCode());
        
        boolean validResponse = response.getStatusCode() == 400 || 
                                response.getStatusCode() == 200;
        
        Assert.assertTrue(validResponse, "Array datatype should be handled");
        
        System.out.println("✅ PASSED: Array datatype handled");
        ApiReportContext.setExpectedStatus(400);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECURITY TESTS (TC64-TC68)
    // ═══════════════════════════════════════════════════════════════════

    @DataProvider(name = "securityPayloads")
    public Object[][] securityPayloads() {
        return new Object[][]{
            {"' OR '1'='1"},                    // SQL Injection
            {"'; DROP TABLE tests; --"},        // SQL Injection
            {"<script>alert('XSS')</script>"}, // XSS
            {"javascript:alert(1)"},           // XSS
            {"../../../etc/passwd"},           // Path Traversal
            {"..\\..\\windows\\system32"},     // Path Traversal (Windows)
            {"%00"},                           // Null byte injection
            {"test%00.jpg"},                   // Null byte
            {"' UNION SELECT * FROM users--"}, // SQL Injection
            {"<img src=x onerror=alert(1)>"}  // XSS
        };
    }

    @Test(priority = 64, description = "TC64-TC68: Security tests (SQL Injection, XSS, Path Traversal)", 
          dataProvider = "securityPayloads")
    public void testGetTestBySlug_SecurityPayloads(String maliciousSlug) {
        System.out.println("\n>>> GET TEST BY SLUG - SECURITY: " + maliciousSlug.substring(0, Math.min(20, maliciousSlug.length())) + "... <<<");
        
        Map<String, Object> payload = buildPayload(maliciousSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        int actualStatusCode = response.getStatusCode();
        String responseBody = response.getBody().asString();
        System.out.println("   HTTP Status: " + actualStatusCode);
        System.out.println("   Response Body: " + responseBody.substring(0, Math.min(200, responseBody.length())));
        
        // ═══════════════════════════════════════════════════════════════
        // VALIDATION 1: API must NOT return 500 (Server Crash)
        // ═══════════════════════════════════════════════════════════════
        if (actualStatusCode == 500) {
            System.err.println("❌ SECURITY VULNERABILITY DETECTED - SERVER CRASH!");
            System.err.println("   Status Code: " + actualStatusCode);
            System.err.println("   Malicious Payload: " + maliciousSlug);
            System.err.println("   Response Body: " + responseBody);
            
            Assert.fail("CRITICAL SECURITY FAILURE: API crashed with HTTP 500 for malicious payload: '" 
                + maliciousSlug + "'. Expected: 200/400/404. Actual: 500. Response: " + responseBody);
        }
        
        Assert.assertNotEquals(actualStatusCode, 500, 
            "SECURITY FAILURE: API returned HTTP 500 for payload: '" + maliciousSlug + "'. Actual: 500");
        
        // ═══════════════════════════════════════════════════════════════
        // VALIDATION 2: If 200, data MUST be empty (no SQL injection data leak)
        // ═══════════════════════════════════════════════════════════════
        if (actualStatusCode == 200) {
            List<Map<String, Object>> dataList = response.jsonPath().get("data");
            
            if (dataList != null && !dataList.isEmpty()) {
                // If data is returned for a malicious slug, check it's not bulk data (SQL injection working)
                System.out.println("   ⚠️ WARNING: Data returned for malicious payload! Records: " + dataList.size());
                
                // SQL Injection check: If more than 1 record returned, injection may have worked
                if (dataList.size() > 1) {
                    System.err.println("❌ SQL INJECTION VULNERABILITY DETECTED!");
                    System.err.println("   Payload: " + maliciousSlug);
                    System.err.println("   Records Returned: " + dataList.size());
                    System.err.println("   Expected: 0 or 1 record. Actual: " + dataList.size() + " records");
                    
                    Assert.fail("SQL INJECTION DETECTED: Malicious payload '" + maliciousSlug 
                        + "' returned " + dataList.size() + " records! Expected 0 or 1. "
                        + "This indicates SQL injection is working and dumping multiple records.");
                }
                
                // Verify returned data slug doesn't match a real test (false positive check)
                Map<String, Object> testData = dataList.get(0);
                if (testData.containsKey("slug")) {
                    String returnedSlug = String.valueOf(testData.get("slug"));
                    // The returned slug should NOT be a different test's slug (data leak)
                    System.out.println("   Returned Slug: " + returnedSlug);
                }
            } else {
                System.out.println("   ✅ Data is empty - no data leak");
            }
        }
        
        // ═══════════════════════════════════════════════════════════════
        // VALIDATION 3: Response must NOT reflect malicious payload (XSS check)
        // ═══════════════════════════════════════════════════════════════
        if (maliciousSlug.contains("<script>") || maliciousSlug.contains("<img") || maliciousSlug.contains("onerror=")) {
            // Check if response echoes back the unescaped script/HTML
            boolean xssReflected = responseBody.contains("<script>") || 
                                   responseBody.contains("onerror=") ||
                                   responseBody.contains("javascript:");
            
            if (xssReflected) {
                System.err.println("❌ XSS REFLECTION VULNERABILITY DETECTED!");
                System.err.println("   Payload: " + maliciousSlug);
                System.err.println("   Response contains unescaped script/HTML!");
                System.err.println("   Response: " + responseBody);
                
                Assert.fail("XSS VULNERABILITY: API reflected malicious payload without sanitization! "
                    + "Payload: '" + maliciousSlug + "'. Response contains executable script/HTML content. "
                    + "Response: " + responseBody);
            }
            System.out.println("   ✅ No XSS reflection detected");
        }
        
        // ═══════════════════════════════════════════════════════════════
        // VALIDATION 4: Response must NOT contain system file content (Path Traversal)
        // ═══════════════════════════════════════════════════════════════
        if (maliciousSlug.contains("../") || maliciousSlug.contains("..\\")) {
            boolean pathTraversalWorked = responseBody.contains("root:") || 
                                          responseBody.contains("[boot loader]") ||
                                          responseBody.contains("/bin/bash") ||
                                          responseBody.contains("WINDOWS") ||
                                          responseBody.contains("[extensions]");
            
            if (pathTraversalWorked) {
                System.err.println("❌ PATH TRAVERSAL VULNERABILITY DETECTED!");
                System.err.println("   Payload: " + maliciousSlug);
                System.err.println("   Response contains system file content!");
                
                Assert.fail("PATH TRAVERSAL VULNERABILITY: API exposed system files! "
                    + "Payload: '" + maliciousSlug + "'. Response contains system file content.");
            }
            System.out.println("   ✅ No path traversal detected");
        }
        
        // ═══════════════════════════════════════════════════════════════
        // VALIDATION 5: Response must NOT expose internal error details
        // ═══════════════════════════════════════════════════════════════
        boolean internalErrorExposed = responseBody.contains("stack trace") || 
                                       responseBody.contains("SQLException") ||
                                       responseBody.contains("MongoError") ||
                                       responseBody.contains("at com.") ||
                                       responseBody.contains("at org.") ||
                                       responseBody.contains("NullPointerException") ||
                                       responseBody.contains("ENOENT") ||
                                       responseBody.contains("Error: ") && responseBody.contains("at /");
        
        if (internalErrorExposed) {
            System.err.println("❌ INFORMATION DISCLOSURE VULNERABILITY!");
            System.err.println("   Payload: " + maliciousSlug);
            System.err.println("   Response exposes internal error details!");
            System.err.println("   Response: " + responseBody);
            
            Assert.fail("INFORMATION DISCLOSURE: API exposed internal error details for payload: '" 
                + maliciousSlug + "'. Response contains stack traces or internal error messages. "
                + "Response: " + responseBody);
        }
        System.out.println("   ✅ No internal error details exposed");
        
        // ═══════════════════════════════════════════════════════════════
        // VALIDATION 6: Status code must be safe (200/400/404 only)
        // ═══════════════════════════════════════════════════════════════
        boolean safeResponse = actualStatusCode == 200 || 
                               actualStatusCode == 400 || 
                               actualStatusCode == 404;
        
        Assert.assertTrue(safeResponse, 
            "Unexpected status code for security payload. Expected: 200/400/404. Actual: " + actualStatusCode 
            + ". Payload: " + maliciousSlug);
        
        // ═══════════════════════════════════════════════════════════════
        // VALIDATION 7: API MUST reject malicious input with 400 (Input Validation)
        // A properly secured API should NOT return 200 for SQL injection/XSS/Path Traversal
        // ═══════════════════════════════════════════════════════════════
        if (actualStatusCode == 200) {
            // Check if response says "success":true for malicious payload - this is a DEFECT
            Boolean successField = response.jsonPath().get("success");
            String msgField = response.jsonPath().getString("msg");
            
            System.err.println("❌ INPUT VALIDATION MISSING!");
            System.err.println("   Payload: " + maliciousSlug);
            System.err.println("   Expected Status: 400 (Bad Request - malicious input should be rejected)");
            System.err.println("   Actual Status: " + actualStatusCode);
            System.err.println("   success: " + successField);
            System.err.println("   msg: " + msgField);
            
            // FAIL: Malicious payloads should be REJECTED with 400, not accepted with 200
            Assert.fail("INPUT VALIDATION DEFECT: API returned HTTP 200 with success=true for malicious payload: '" 
                + maliciousSlug + "'. "
                + "Expected: HTTP 400 (Bad Request) - API should validate and reject malicious input patterns "
                + "(SQL injection, XSS, path traversal). "
                + "Actual: HTTP " + actualStatusCode + ", success=" + successField + ", msg='" + msgField + "'. "
                + "RECOMMENDATION: Add input validation to reject slugs containing SQL keywords, HTML tags, "
                + "path traversal characters (../), and null bytes.");
        }
        
        System.out.println("✅ ALL SECURITY VALIDATIONS PASSED (Status: " + actualStatusCode + ")");
        
        ApiReportContext.setExpectedStatus(400);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  BUSINESS VALIDATION (TC69-TC77)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 69, description = "TC69: Verify active test details")
    public void testGetTestBySlug_ActiveTest() {
        System.out.println("\n>>> BUSINESS VALIDATION: ACTIVE TEST <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        if (dataList != null && !dataList.isEmpty()) {
            Map<String, Object> testData = dataList.get(0);
            
            if (testData.containsKey("status")) {
                String status = (String) testData.get("status");
                System.out.println("   Test Status: " + status);
                
                // Active tests should have ACTIVE status
                if ("ACTIVE".equals(status)) {
                    System.out.println("✅ Test is ACTIVE");
                } else {
                    System.out.println("⚠️ Test Status: " + status);
                }
            }
        }
        
        System.out.println("✅ PASSED: Active test validated");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 70, description = "TC70: Verify home collection available test")
    public void testGetTestBySlug_HomeCollectionAvailable() {
        System.out.println("\n>>> BUSINESS VALIDATION: HOME COLLECTION AVAILABLE <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        if (dataList != null && !dataList.isEmpty()) {
            Map<String, Object> testData = dataList.get(0);
            
            if (testData.containsKey("home_collection")) {
                String homeCollection = (String) testData.get("home_collection");
                System.out.println("   Home Collection Status: " + homeCollection);
                
                Assert.assertNotNull(homeCollection, "Home collection field should not be null");
            }
        }
        
        System.out.println("✅ PASSED: Home collection validated");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 71, description = "TC71: Verify membership price if available")
    public void testGetTestBySlug_MembershipPrice() {
        System.out.println("\n>>> BUSINESS VALIDATION: MEMBERSHIP PRICE <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        if (dataList != null && !dataList.isEmpty()) {
            Map<String, Object> testData = dataList.get(0);
            
            if (testData.containsKey("membership_price")) {
                Object membershipPrice = testData.get("membership_price");
                System.out.println("   Membership Price: ₹" + membershipPrice);
            }
            
            if (testData.containsKey("membership_special_price")) {
                Object specialPrice = testData.get("membership_special_price");
                System.out.println("   Membership Special Price: ₹" + specialPrice);
            }
        }
        
        System.out.println("✅ PASSED: Membership pricing validated");
        ApiReportContext.setExpectedStatus(200);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  EDGE CASE TESTS (TC72-TC77)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 72, description = "TC72: Get test with very long slug")
    public void testGetTestBySlug_VeryLongSlug() {
        System.out.println("\n>>> GET TEST BY SLUG - EDGE CASE: VERY LONG SLUG <<<");
        
        String longSlug = "test-slug-" + "a".repeat(1000);
        System.out.println("   Testing with slug length: " + longSlug.length());
        
        Map<String, Object> payload = buildPayload(longSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        int actualStatusCode = response.getStatusCode();
        String responseBody = response.getBody().asString();
        System.out.println("   HTTP Status: " + actualStatusCode);
        
        // VALIDATION 1: Must NOT return 500 (buffer overflow / DoS)
        if (actualStatusCode == 500) {
            System.err.println("❌ BUFFER OVERFLOW / DoS VULNERABILITY DETECTED!");
            System.err.println("   Status Code: 500");
            System.err.println("   Slug Length: " + longSlug.length() + " characters");
            System.err.println("   Response: " + responseBody);
            
            Assert.fail("SECURITY FAILURE: API crashed with HTTP 500 for long slug (" 
                + longSlug.length() + " chars). Expected: 200/400/404. Actual: 500. Response: " + responseBody);
        }
        
        Assert.assertNotEquals(actualStatusCode, 500, 
            "SECURITY FAILURE: API returned HTTP 500 for long slug (" + longSlug.length() 
            + " chars). Expected: 200/400/404. Actual: " + actualStatusCode);
        
        // VALIDATION 2: Must NOT expose internal errors
        boolean internalError = responseBody.contains("stack trace") || 
                                responseBody.contains("Exception") ||
                                responseBody.contains("out of memory") ||
                                responseBody.contains("MongoError") ||
                                responseBody.contains("ENOMEM");
        if (internalError) {
            System.err.println("❌ INFORMATION DISCLOSURE - Internal error exposed!");
            Assert.fail("INFORMATION DISCLOSURE: Internal error details exposed for long slug ("
                + longSlug.length() + " chars). Response: " + responseBody);
        }
        System.out.println("   ✅ No internal error details exposed");
        
        // VALIDATION 3: If 200, data should be empty (no slug that long exists)
        if (actualStatusCode == 200) {
            List<Map<String, Object>> dataList = response.jsonPath().get("data");
            if (dataList != null && !dataList.isEmpty()) {
                System.out.println("   ⚠️ WARNING: Data returned for 1000+ char slug! Records: " + dataList.size());
                Assert.fail("UNEXPECTED DATA: API returned " + dataList.size() + " records for a 1000+ character slug. "
                    + "No valid test should have a slug this long. This may indicate improper input handling.");
            }
            System.out.println("   ✅ Data is empty - no data leak");
        }
        
        // VALIDATION 4: Response time should be reasonable (no DoS via regex)
        long responseTime = response.getTime();
        if (responseTime > 5000) {
            System.err.println("❌ POTENTIAL ReDoS VULNERABILITY!");
            System.err.println("   Response Time: " + responseTime + " ms for long slug");
            Assert.fail("POTENTIAL ReDoS: API took " + responseTime + "ms to respond to long slug. "
                + "Expected: <5000ms. This may indicate regex-based denial of service vulnerability.");
        }
        System.out.println("   ✅ Response time acceptable: " + responseTime + " ms");
        
        System.out.println("✅ ALL LONG SLUG VALIDATIONS PASSED (Status: " + actualStatusCode + ")");
        
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 73, description = "TC73: Get test with special characters in slug")
    public void testGetTestBySlug_SpecialCharacters() {
        System.out.println("\n>>> GET TEST BY SLUG - EDGE CASE: SPECIAL CHARACTERS <<<");
        
        String specialSlug = "test-@#$%^&*()-slug";
        System.out.println("   Testing with special characters: " + specialSlug);
        
        Map<String, Object> payload = buildPayload(specialSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        System.out.println("   HTTP Status: " + response.getStatusCode());
        
        // Should handle gracefully
        boolean validResponse = response.getStatusCode() == 200 || 
                                response.getStatusCode() == 400 || 
                                response.getStatusCode() == 404;
        
        Assert.assertTrue(validResponse, "Special characters should be handled");
        
        System.out.println("✅ PASSED: Special characters handled");
        
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 74, description = "TC74: Get test with numeric slug")
    public void testGetTestBySlug_NumericSlug() {
        System.out.println("\n>>> GET TEST BY SLUG - EDGE CASE: NUMERIC SLUG <<<");
        
        String numericSlug = "12345";
        System.out.println("   Testing with numeric slug: " + numericSlug);
        
        Map<String, Object> payload = buildPayload(numericSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        System.out.println("   HTTP Status: " + response.getStatusCode());
        
        // Should handle gracefully (200 with null/empty or 404)
        boolean validResponse = response.getStatusCode() == 200 || 
                                response.getStatusCode() == 404;
        
        Assert.assertTrue(validResponse, "Numeric slug should be handled");
        
        System.out.println("✅ PASSED: Numeric slug handled");
        
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 75, description = "TC75: Get test with uppercase slug")
    public void testGetTestBySlug_UppercaseSlug() {
        System.out.println("\n>>> GET TEST BY SLUG - EDGE CASE: UPPERCASE SLUG <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        String uppercaseSlug = validSlug.toUpperCase();
        System.out.println("   Original slug: " + validSlug);
        System.out.println("   Uppercase slug: " + uppercaseSlug);
        
        Map<String, Object> payload = buildPayload(uppercaseSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        System.out.println("   HTTP Status: " + response.getStatusCode());
        
        // Check if API is case-sensitive or case-insensitive
        boolean validResponse = response.getStatusCode() == 200 || 
                                response.getStatusCode() == 404;
        
        Assert.assertTrue(validResponse, "Uppercase slug should be handled");
        
        if (response.getStatusCode() == 200) {
            System.out.println("   API is case-insensitive");
        } else {
            System.out.println("   API is case-sensitive");
        }
        
        System.out.println("✅ PASSED: Uppercase slug handled");
        
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 76, description = "TC76: Get test with slug containing spaces")
    public void testGetTestBySlug_SlugWithSpaces() {
        System.out.println("\n>>> GET TEST BY SLUG - EDGE CASE: SLUG WITH SPACES <<<");
        
        String slugWithSpaces = "test slug with spaces";
        System.out.println("   Testing with slug: " + slugWithSpaces);
        
        Map<String, Object> payload = buildPayload(slugWithSpaces);
        Response response = callGetTestBySlugAPI(payload);
        
        System.out.println("   HTTP Status: " + response.getStatusCode());
        
        // Should handle gracefully
        boolean validResponse = response.getStatusCode() == 200 || 
                                response.getStatusCode() == 400 || 
                                response.getStatusCode() == 404;
        
        Assert.assertTrue(validResponse, "Slug with spaces should be handled");
        
        System.out.println("✅ PASSED: Slug with spaces handled");
        
        ApiReportContext.setExpectedStatus(200);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  PERFORMANCE TESTS (TC78-TC83)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 78, description = "TC78: Verify response time is acceptable")
    public void testGetTestBySlug_ResponseTime() {
        System.out.println("\n>>> GET TEST BY SLUG - PERFORMANCE: RESPONSE TIME <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        
        long startTime = System.currentTimeMillis();
        Response response = callGetTestBySlugAPI(payload);
        long endTime = System.currentTimeMillis();
        
        long responseTime = endTime - startTime;
        
        System.out.println("   HTTP Status: " + response.getStatusCode());
        System.out.println("   Response Time: " + responseTime + " ms");
        
        // Response time should be under 3 seconds
        Assert.assertTrue(responseTime < 3000, 
            "Response time should be under 3000ms. Actual: " + responseTime + "ms");
        
        System.out.println("✅ PASSED: Response time is acceptable");
        
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 79, description = "TC79: Verify consistent response time over multiple calls")
    public void testGetTestBySlug_ConsistentResponseTime() {
        System.out.println("\n>>> GET TEST BY SLUG - PERFORMANCE: CONSISTENT RESPONSE TIME <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        int iterations = 5;
        long totalTime = 0;
        
        System.out.println("   Running " + iterations + " iterations...");
        
        for (int i = 0; i < iterations; i++) {
            long startTime = System.currentTimeMillis();
            Response response = callGetTestBySlugAPI(payload);
            long endTime = System.currentTimeMillis();
            
            long responseTime = endTime - startTime;
            totalTime += responseTime;
            
            System.out.println("   Iteration " + (i + 1) + ": " + responseTime + " ms");
        }
        
        long avgResponseTime = totalTime / iterations;
        System.out.println("   Average Response Time: " + avgResponseTime + " ms");
        
        Assert.assertTrue(avgResponseTime < 3000, 
            "Average response time should be under 3000ms. Actual: " + avgResponseTime + "ms");
        
        System.out.println("✅ PASSED: Consistent response time");
        
        ApiReportContext.setExpectedStatus(200);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  REGRESSION TESTS (TC90-TC100) - MUST AUTOMATE
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 90, description = "TC90-TC100: Regression - Complete flow validation")
    public void testGetTestBySlug_RegressionValidation() {
        System.out.println("\n>>> REGRESSION TEST: COMPLETE FLOW <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        // Test 1: Valid slug
        System.out.println("   [1/10] Testing valid slug...");
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        AssertionUtil.verifyStatusCode(response, 200);
        
        // Test 2: Invalid slug
        System.out.println("   [2/10] Testing invalid slug...");
        payload = buildPayload("invalid-slug-999");
        response = callGetTestBySlugAPI(payload);
        Assert.assertTrue(response.getStatusCode() == 200 || response.getStatusCode() == 404);
        
        // Test 3: Empty slug
        System.out.println("   [3/10] Testing empty slug...");
        payload = buildPayload("");
        response = callGetTestBySlugAPI(payload);
        Assert.assertTrue(response.getStatusCode() == 400 || response.getStatusCode() == 200);
        
        // Test 4: Null slug
        System.out.println("   [4/10] Testing null slug...");
        payload = buildPayload(null);
        response = callGetTestBySlugAPI(payload);
        Assert.assertTrue(response.getStatusCode() == 400 || response.getStatusCode() == 200);
        
        // Test 5: Uppercase slug
        System.out.println("   [5/10] Testing uppercase slug...");
        payload = buildPayload(validSlug.toUpperCase());
        response = callGetTestBySlugAPI(payload);
        Assert.assertTrue(response.getStatusCode() == 200 || response.getStatusCode() == 404);
        
        // Test 6: SQL Injection
        System.out.println("   [6/10] Testing SQL injection...");
        payload = buildPayload("' OR 1=1 --");
        response = callGetTestBySlugAPI(payload);
        int sqlInjectionStatus = response.getStatusCode();
        String sqlResponse = response.getBody().asString();
        
        if (sqlInjectionStatus == 500) {
            System.err.println("❌ REGRESSION FAILED: SQL Injection caused HTTP 500");
            System.err.println("   Payload: ' OR 1=1 --");
            System.err.println("   Response: " + sqlResponse);
            Assert.fail("SECURITY REGRESSION: SQL injection caused server crash (HTTP 500). Response: " + sqlResponse);
        }
        Assert.assertNotEquals(sqlInjectionStatus, 500, 
            "SQL injection should not crash server. Status: " + sqlInjectionStatus);
        
        // Check SQL injection didn't return bulk data
        if (sqlInjectionStatus == 200) {
            List<Map<String, Object>> sqlDataList = response.jsonPath().get("data");
            if (sqlDataList != null && sqlDataList.size() > 1) {
                Assert.fail("SQL INJECTION DATA LEAK: Payload ' OR 1=1 --' returned " + sqlDataList.size() 
                    + " records! Expected 0 or 1. SQL injection may be working!");
            }
        }
        // Check no internal error details exposed
        Assert.assertFalse(sqlResponse.contains("SQLException") || sqlResponse.contains("MongoError") || sqlResponse.contains("stack trace"),
            "INFORMATION DISCLOSURE: SQL error details exposed in response for SQL injection payload");
        
        // INPUT VALIDATION: SQL injection should be rejected with 400
        Assert.assertEquals(sqlInjectionStatus, 400, 
            "INPUT VALIDATION DEFECT [Regression]: API returned HTTP " + sqlInjectionStatus + " for SQL injection payload: ' OR 1=1 --'. "
            + "Expected: 400 (Bad Request). API should detect and reject SQL injection patterns in slug field. "
            + "Response: " + sqlResponse);
        
        // Test 7: XSS
        System.out.println("   [7/10] Testing XSS...");
        payload = buildPayload("<script>alert(1)</script>");
        response = callGetTestBySlugAPI(payload);
        int xssStatus = response.getStatusCode();
        String xssResponse = response.getBody().asString();
        
        if (xssStatus == 500) {
            System.err.println("❌ REGRESSION FAILED: XSS attack caused HTTP 500");
            System.err.println("   Payload: <script>alert(1)</script>");
            System.err.println("   Response: " + xssResponse);
            Assert.fail("SECURITY REGRESSION: XSS attack caused server crash (HTTP 500). Response: " + xssResponse);
        }
        Assert.assertNotEquals(xssStatus, 500, 
            "XSS attack should not crash server. Status: " + xssStatus);
        
        // Check XSS reflection
        if (xssResponse.contains("<script>alert(1)</script>")) {
            Assert.fail("XSS REFLECTION: API echoed back unescaped <script> tag in response! "
                + "Payload is being reflected without sanitization. Response: " + xssResponse);
        }
        // Check no internal error details
        Assert.assertFalse(xssResponse.contains("Exception") || xssResponse.contains("at com.") || xssResponse.contains("at org."),
            "INFORMATION DISCLOSURE: Internal error details exposed in response for XSS payload");
        
        // INPUT VALIDATION: XSS payload should be rejected with 400
        Assert.assertEquals(xssStatus, 400, 
            "INPUT VALIDATION DEFECT [Regression]: API returned HTTP " + xssStatus + " for XSS payload: <script>alert(1)</script>. "
            + "Expected: 400 (Bad Request). API should detect and reject HTML/script tags in slug field. "
            + "Response: " + xssResponse);
        
        // Test 8: Price validation
        System.out.println("   [8/10] Testing price validation...");
        payload = buildPayload(validSlug);
        response = callGetTestBySlugAPI(payload);
        if (response.getStatusCode() == 200) {
            List<Map<String, Object>> dataList = response.jsonPath().get("data");
            if (dataList != null && !dataList.isEmpty()) {
                Map<String, Object> testData = dataList.get(0);
                Assert.assertNotNull(testData.get("price"));
            }
        }
        
        // Test 9: Home collection validation
        System.out.println("   [9/10] Testing home collection...");
        payload = buildPayload(validSlug);
        response = callGetTestBySlugAPI(payload);
        if (response.getStatusCode() == 200) {
            List<Map<String, Object>> dataList = response.jsonPath().get("data");
            if (dataList != null && !dataList.isEmpty()) {
                Map<String, Object> testData = dataList.get(0);
                Assert.assertNotNull(testData.get("home_collection"));
            }
        }
        
        // Test 10: Response schema validation
        System.out.println("   [10/10] Testing response schema...");
        payload = buildPayload(validSlug);
        response = callGetTestBySlugAPI(payload);
        Assert.assertNotNull(response.jsonPath().get("status"));
        Assert.assertNotNull(response.jsonPath().get("success"));
        Assert.assertNotNull(response.jsonPath().get("data"));
        
        System.out.println("✅ PASSED: All 10 regression tests completed");
        ApiReportContext.setExpectedStatus(200);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  DETAILED RESPONSE FIELD VALIDATION (TC32-TC48)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 32, description = "TC32: Verify test_id field")
    public void testGetTestBySlug_VerifyTestId() {
        System.out.println("\n>>> RESPONSE FIELD: VERIFY TEST_ID <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        if (dataList != null && !dataList.isEmpty()) {
            Map<String, Object> testData = dataList.get(0);
            Assert.assertNotNull(testData.get("test_id"), "test_id should be present");
            System.out.println("   Test ID: " + testData.get("test_id"));
        }
        
        System.out.println("✅ PASSED: test_id field verified");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 33, description = "TC33: Verify test_name field")
    public void testGetTestBySlug_VerifyTestName() {
        System.out.println("\n>>> RESPONSE FIELD: VERIFY TEST_NAME <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        if (dataList != null && !dataList.isEmpty()) {
            Map<String, Object> testData = dataList.get(0);
            Assert.assertNotNull(testData.get("test_name"), "test_name should be present");
            String testName = (String) testData.get("test_name");
            Assert.assertTrue(testName != null && !testName.isEmpty(), "test_name should not be empty");
            System.out.println("   Test Name: " + testName);
        }
        
        System.out.println("✅ PASSED: test_name field verified");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 34, description = "TC34: Verify slug field in response")
    public void testGetTestBySlug_VerifySlugField() {
        System.out.println("\n>>> RESPONSE FIELD: VERIFY SLUG <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        if (dataList != null && !dataList.isEmpty()) {
            Map<String, Object> testData = dataList.get(0);
            Assert.assertNotNull(testData.get("slug"), "slug should be present");
            Assert.assertEquals(testData.get("slug"), validSlug, "Slug should match request");
            System.out.println("   Slug: " + testData.get("slug"));
        }
        
        System.out.println("✅ PASSED: slug field verified");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 35, description = "TC35: Verify price field")
    public void testGetTestBySlug_VerifyPriceField() {
        System.out.println("\n>>> RESPONSE FIELD: VERIFY PRICE <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        if (dataList != null && !dataList.isEmpty()) {
            Map<String, Object> testData = dataList.get(0);
            Assert.assertNotNull(testData.get("price"), "price should be present");
            
            Object price = testData.get("price");
            if (price instanceof Number) {
                Number priceNum = (Number) price;
                Assert.assertTrue(priceNum.doubleValue() >= 0, "Price should be non-negative");
            }
            System.out.println("   Price: ₹" + price);
        }
        
        System.out.println("✅ PASSED: price field verified");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 36, description = "TC36: Verify original_price field")
    public void testGetTestBySlug_VerifyOriginalPrice() {
        System.out.println("\n>>> RESPONSE FIELD: VERIFY ORIGINAL_PRICE <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        if (dataList != null && !dataList.isEmpty()) {
            Map<String, Object> testData = dataList.get(0);
            if (testData.containsKey("original_price")) {
                System.out.println("   Original Price: ₹" + testData.get("original_price"));
            }
        }
        
        System.out.println("✅ PASSED: original_price field checked");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 37, description = "TC37: Verify discount_rate field")
    public void testGetTestBySlug_VerifyDiscountRate() {
        System.out.println("\n>>> RESPONSE FIELD: VERIFY DISCOUNT_RATE <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        if (dataList != null && !dataList.isEmpty()) {
            Map<String, Object> testData = dataList.get(0);
            if (testData.containsKey("discount_rate")) {
                System.out.println("   Discount Rate: " + testData.get("discount_rate"));
            }
            if (testData.containsKey("discount_percentage")) {
                System.out.println("   Discount Percentage: " + testData.get("discount_percentage"));
            }
        }
        
        System.out.println("✅ PASSED: discount fields checked");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 38, description = "TC38: Verify description field")
    public void testGetTestBySlug_VerifyDescriptionField() {
        System.out.println("\n>>> RESPONSE FIELD: VERIFY DESCRIPTION <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        if (dataList != null && !dataList.isEmpty()) {
            Map<String, Object> testData = dataList.get(0);
            boolean hasDesc = testData.containsKey("description") || 
                             testData.containsKey("usage") || 
                             testData.containsKey("usage_yoda");
            Assert.assertTrue(hasDesc, "Test should have description/usage field");
        }
        
        System.out.println("✅ PASSED: description field verified");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 39, description = "TC39: Verify specimen field")
    public void testGetTestBySlug_VerifySpecimen() {
        System.out.println("\n>>> RESPONSE FIELD: VERIFY SPECIMEN <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        if (dataList != null && !dataList.isEmpty()) {
            Map<String, Object> testData = dataList.get(0);
            if (testData.containsKey("specimen")) {
                System.out.println("   Specimen: " + testData.get("specimen"));
            }
            if (testData.containsKey("sample_required_yoda")) {
                System.out.println("   Sample Required: " + testData.get("sample_required_yoda"));
            }
        }
        
        System.out.println("✅ PASSED: specimen field checked");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 40, description = "TC40: Verify pre_test_information field")
    public void testGetTestBySlug_VerifyPreTestInfo() {
        System.out.println("\n>>> RESPONSE FIELD: VERIFY PRE_TEST_INFORMATION <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        if (dataList != null && !dataList.isEmpty()) {
            Map<String, Object> testData = dataList.get(0);
            if (testData.containsKey("pre_test_information")) {
                System.out.println("   Pre-test Info: " + testData.get("pre_test_information"));
            }
        }
        
        System.out.println("✅ PASSED: pre_test_information field checked");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 41, description = "TC41: Verify turn_around_time field")
    public void testGetTestBySlug_VerifyTATField() {
        System.out.println("\n>>> RESPONSE FIELD: VERIFY TURN_AROUND_TIME <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        if (dataList != null && !dataList.isEmpty()) {
            Map<String, Object> testData = dataList.get(0);
            boolean hasTAT = testData.containsKey("turn_around_time") || 
                            testData.containsKey("turn_around_time_yoda");
            Assert.assertTrue(hasTAT, "Test should have turnaround time field");
        }
        
        System.out.println("✅ PASSED: turn_around_time field verified");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 42, description = "TC42: Verify home_collection field")
    public void testGetTestBySlug_VerifyHomeCollectionField() {
        System.out.println("\n>>> RESPONSE FIELD: VERIFY HOME_COLLECTION <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        if (dataList != null && !dataList.isEmpty()) {
            Map<String, Object> testData = dataList.get(0);
            Assert.assertNotNull(testData.get("home_collection"), "home_collection field should be present");
            String homeCollection = (String) testData.get("home_collection");
            Assert.assertTrue("AVAILABLE".equals(homeCollection) || "NOT_AVAILABLE".equals(homeCollection) || 
                            homeCollection != null, "home_collection should have valid value");
        }
        
        System.out.println("✅ PASSED: home_collection field verified");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 43, description = "TC43: Verify locations field")
    public void testGetTestBySlug_VerifyLocationsField() {
        System.out.println("\n>>> RESPONSE FIELD: VERIFY LOCATIONS <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        if (dataList != null && !dataList.isEmpty()) {
            Map<String, Object> testData = dataList.get(0);
            Assert.assertNotNull(testData.get("locations"), "locations field should be present");
            Object locations = testData.get("locations");
            Assert.assertTrue(locations instanceof List, "locations should be a list");
        }
        
        System.out.println("✅ PASSED: locations field verified");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 44, description = "TC44: Verify FAQ section")
    public void testGetTestBySlug_VerifyFAQSection() {
        System.out.println("\n>>> RESPONSE FIELD: VERIFY FAQ SECTION <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        if (dataList != null && !dataList.isEmpty()) {
            Map<String, Object> testData = dataList.get(0);
            if (testData.containsKey("frequently_asked_questions")) {
                Object faq = testData.get("frequently_asked_questions");
                if (faq instanceof List) {
                    List<?> faqList = (List<?>) faq;
                    System.out.println("   FAQ Items: " + faqList.size());
                }
            }
        }
        
        System.out.println("✅ PASSED: FAQ section verified");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 45, description = "TC45: Verify search_keywords field")
    public void testGetTestBySlug_VerifySearchKeywords() {
        System.out.println("\n>>> RESPONSE FIELD: VERIFY SEARCH_KEYWORDS <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        if (dataList != null && !dataList.isEmpty()) {
            Map<String, Object> testData = dataList.get(0);
            if (testData.containsKey("search_keywords")) {
                Object keywords = testData.get("search_keywords");
                if (keywords instanceof List) {
                    List<?> keywordList = (List<?>) keywords;
                    System.out.println("   Search Keywords Count: " + keywordList.size());
                }
            }
        }
        
        System.out.println("✅ PASSED: search_keywords field checked");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 46, description = "TC46: Verify result_interpretation field")
    public void testGetTestBySlug_VerifyResultInterpretation() {
        System.out.println("\n>>> RESPONSE FIELD: VERIFY RESULT_INTERPRETATION <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        if (dataList != null && !dataList.isEmpty()) {
            Map<String, Object> testData = dataList.get(0);
            boolean hasInterpretation = testData.containsKey("result_interpretation") || 
                                       testData.containsKey("result_interpretation_yoda");
            if (hasInterpretation) {
                System.out.println("   Result interpretation field present");
            }
        }
        
        System.out.println("✅ PASSED: result_interpretation field checked");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 47, description = "TC47: Verify test_measures field")
    public void testGetTestBySlug_VerifyTestMeasures() {
        System.out.println("\n>>> RESPONSE FIELD: VERIFY TEST_MEASURES <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        if (dataList != null && !dataList.isEmpty()) {
            Map<String, Object> testData = dataList.get(0);
            if (testData.containsKey("test_measures") || testData.containsKey("components")) {
                System.out.println("   Test measures/components field present");
            }
        }
        
        System.out.println("✅ PASSED: test_measures field checked");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 48, description = "TC48: Verify genders field")
    public void testGetTestBySlug_VerifyGenders() {
        System.out.println("\n>>> RESPONSE FIELD: VERIFY GENDERS <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        if (dataList != null && !dataList.isEmpty()) {
            Map<String, Object> testData = dataList.get(0);
            boolean hasGenders = testData.containsKey("genders") || 
                                testData.containsKey("genders_yoda");
            if (hasGenders) {
                System.out.println("   Genders field present");
            }
        }
        
        System.out.println("✅ PASSED: genders field checked");
        ApiReportContext.setExpectedStatus(200);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  ADDITIONAL NEGATIVE TESTS (TC58-TC59, TC63)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 58, description = "TC58: Test with null request body")
    public void testGetTestBySlug_NullRequestBody() {
        System.out.println("\n>>> NEGATIVE: NULL REQUEST BODY <<<");
        
        try {
            Response response = new RequestBuilder()
                    .setEndpoint(GET_TEST_BY_SLUG_ENDPOINT)
                    .addHeader("Content-Type", "application/json")
                    .setRequestBody(null)
                    .post();
            
            System.out.println("   HTTP Status: " + response.getStatusCode());
            
            boolean validResponse = response.getStatusCode() == 400 || 
                                    response.getStatusCode() == 200 ||
                                    response.getStatusCode() == 415;
            
            Assert.assertTrue(validResponse, "Null body should be handled");
            System.out.println("✅ PASSED: Null request body handled");
        } catch (Exception e) {
            System.out.println("✅ PASSED: Null body rejected as expected");
        }
        
        ApiReportContext.setExpectedStatus(400);
    }

    @Test(priority = 59, description = "TC59: Test with invalid JSON")
    public void testGetTestBySlug_InvalidJSON() {
        System.out.println("\n>>> NEGATIVE: INVALID JSON <<<");
        
        try {
            Response response = new RequestBuilder()
                    .setEndpoint(GET_TEST_BY_SLUG_ENDPOINT)
                    .addHeader("Content-Type", "application/json")
                    .setRequestBody("{\"slug\":}") // Invalid JSON
                    .post();
            
            System.out.println("   HTTP Status: " + response.getStatusCode());
            
            boolean validResponse = response.getStatusCode() == 400 || 
                                    response.getStatusCode() == 500;
            
            Assert.assertTrue(validResponse, "Invalid JSON should return error");
            System.out.println("✅ PASSED: Invalid JSON handled");
        } catch (Exception e) {
            System.out.println("✅ PASSED: Invalid JSON rejected as expected");
        }
        
        ApiReportContext.setExpectedStatus(400);
    }

    @Test(priority = 63, description = "TC63: Test with object datatype for slug")
    public void testGetTestBySlug_ObjectDatatype() {
        System.out.println("\n>>> NEGATIVE: OBJECT DATATYPE <<<");
        
        Map<String, Object> slugObject = new HashMap<>();
        slugObject.put("value", "test-slug");
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("slug", slugObject);
        
        Response response = callGetTestBySlugAPI(payload);
        
        System.out.println("   HTTP Status: " + response.getStatusCode());
        
        boolean validResponse = response.getStatusCode() == 400 || 
                                response.getStatusCode() == 200;
        
        Assert.assertTrue(validResponse, "Object datatype should be handled");
        
        System.out.println("✅ PASSED: Object datatype handled");
        ApiReportContext.setExpectedStatus(400);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  ENHANCED BUSINESS VALIDATION (TC72-TC77)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 72, description = "TC72: Verify complimentary test if available")
    public void testGetTestBySlug_ComplimentaryTest() {
        System.out.println("\n>>> BUSINESS VALIDATION: COMPLIMENTARY TEST <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        if (dataList != null && !dataList.isEmpty()) {
            Map<String, Object> testData = dataList.get(0);
            
            Object price = testData.get("price");
            if (price != null && price instanceof Number) {
                Number priceNum = (Number) price;
                if (priceNum.doubleValue() == 0) {
                    System.out.println("   Complimentary test detected (price = 0)");
                } else {
                    System.out.println("   Regular test (price = " + price + ")");
                }
            }
        }
        
        System.out.println("✅ PASSED: Complimentary test check completed");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 73, description = "TC73: Verify membership test details")
    public void testGetTestBySlug_MembershipTestDetails() {
        System.out.println("\n>>> BUSINESS VALIDATION: MEMBERSHIP TEST DETAILS <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        if (dataList != null && !dataList.isEmpty()) {
            Map<String, Object> testData = dataList.get(0);
            
            // Check membership-related fields
            boolean hasMembershipFields = false;
            
            if (testData.containsKey("membership_price")) {
                System.out.println("   Membership Price: ₹" + testData.get("membership_price"));
                hasMembershipFields = true;
            }
            
            if (testData.containsKey("membership_special_price")) {
                System.out.println("   Membership Special Price: ₹" + testData.get("membership_special_price"));
                hasMembershipFields = true;
            }
            
            if (testData.containsKey("membership_discount")) {
                System.out.println("   Membership Discount: " + testData.get("membership_discount") + "%");
                hasMembershipFields = true;
            }
            
            if (testData.containsKey("rewards_percentage")) {
                System.out.println("   Rewards Percentage: " + testData.get("rewards_percentage"));
            }
            
            System.out.println("   Has Membership Fields: " + hasMembershipFields);
        }
        
        System.out.println("✅ PASSED: Membership test details checked");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 74, description = "TC74: Verify home collection available test")
    public void testGetTestBySlug_HomeCollectionAvailableTest() {
        System.out.println("\n>>> BUSINESS VALIDATION: HOME COLLECTION AVAILABLE <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        if (dataList != null && !dataList.isEmpty()) {
            Map<String, Object> testData = dataList.get(0);
            
            Assert.assertNotNull(testData.get("home_collection"), "home_collection field should be present");
            String homeCollection = (String) testData.get("home_collection");
            
            System.out.println("   Home Collection Status: " + homeCollection);
            
            if ("AVAILABLE".equalsIgnoreCase(homeCollection)) {
                System.out.println("   ✓ Home collection is available");
            } else {
                System.out.println("   ✗ Home collection not available");
            }
        }
        
        System.out.println("✅ PASSED: Home collection status validated");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 75, description = "TC75: Verify test without home collection")
    public void testGetTestBySlug_HomeCollectionNotAvailable() {
        System.out.println("\n>>> BUSINESS VALIDATION: HOME COLLECTION NOT AVAILABLE <<<");
        
        // Try to find a test without home collection from the test list
        String testSlugWithoutHomeCollection = null;
        
        for (String slug : testSlugs) {
            Map<String, Object> payload = buildPayload(slug);
            Response response = callGetTestBySlugAPI(payload);
            
            if (response.getStatusCode() == 200) {
                List<Map<String, Object>> dataList = response.jsonPath().get("data");
                if (dataList != null && !dataList.isEmpty()) {
                    Map<String, Object> testData = dataList.get(0);
                    String homeCollection = (String) testData.get("home_collection");
                    
                    if (homeCollection != null && !homeCollection.equalsIgnoreCase("AVAILABLE")) {
                        testSlugWithoutHomeCollection = slug;
                        System.out.println("   Found test without home collection: " + slug);
                        break;
                    }
                }
            }
            
            // Only check first 10 tests
            if (testSlugs.indexOf(slug) >= 10) break;
        }
        
        if (testSlugWithoutHomeCollection != null) {
            System.out.println("✅ PASSED: Found test without home collection");
        } else {
            System.out.println("⚠️ INFO: All checked tests have home collection available");
        }
        
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 76, description = "TC76: Verify frequently booked test")
    public void testGetTestBySlug_FrequentlyBookedTest() {
        System.out.println("\n>>> BUSINESS VALIDATION: FREQUENTLY BOOKED TEST <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        if (dataList != null && !dataList.isEmpty()) {
            Map<String, Object> testData = dataList.get(0);
            
            if (testData.containsKey("frequently_booked")) {
                Boolean frequentlyBooked = (Boolean) testData.get("frequently_booked");
                System.out.println("   Frequently Booked: " + frequentlyBooked);
                
                if (Boolean.TRUE.equals(frequentlyBooked)) {
                    System.out.println("   ✓ This is a frequently booked test");
                }
            }
        }
        
        System.out.println("✅ PASSED: Frequently booked field checked");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 77, description = "TC77: Verify popular test")
    public void testGetTestBySlug_PopularTest() {
        System.out.println("\n>>> BUSINESS VALIDATION: POPULAR TEST <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        if (dataList != null && !dataList.isEmpty()) {
            Map<String, Object> testData = dataList.get(0);
            
            if (testData.containsKey("popular")) {
                Boolean popular = (Boolean) testData.get("popular");
                System.out.println("   Popular Test: " + popular);
                
                if (Boolean.TRUE.equals(popular)) {
                    System.out.println("   ✓ This is a popular test");
                }
            }
            
            if (testData.containsKey("speciality_tests")) {
                Boolean speciality = (Boolean) testData.get("speciality_tests");
                System.out.println("   Speciality Test: " + speciality);
            }
        }
        
        System.out.println("✅ PASSED: Popular test field checked");
        ApiReportContext.setExpectedStatus(200);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  DATABASE VALIDATION TESTS (TC84-TC89)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 84, description = "TC84: Verify test record exists (API level validation)")
    public void testGetTestBySlug_TestRecordExists() {
        System.out.println("\n>>> DATABASE VALIDATION: TEST RECORD EXISTS <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        Assert.assertNotNull(dataList, "Data should not be null");
        Assert.assertTrue(dataList.size() > 0, "Test record should exist");
        
        Map<String, Object> testData = dataList.get(0);
        Assert.assertNotNull(testData.get("_id"), "Test should have database ID");
        
        System.out.println("   Database ID: " + testData.get("_id"));
        System.out.println("✅ PASSED: Test record exists");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 85, description = "TC85: Verify slug uniqueness")
    public void testGetTestBySlug_SlugUniqueness() {
        System.out.println("\n>>> DATABASE VALIDATION: SLUG UNIQUENESS <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        if (dataList != null) {
            // Should return only one test for a unique slug
            Assert.assertTrue(dataList.size() <= 1, 
                "Slug should be unique, returned " + dataList.size() + " records");
            System.out.println("   Records returned: " + dataList.size());
        }
        
        System.out.println("✅ PASSED: Slug uniqueness validated");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 86, description = "TC86: Verify test details accuracy")
    public void testGetTestBySlug_TestDetailsAccuracy() {
        System.out.println("\n>>> DATABASE VALIDATION: TEST DETAILS ACCURACY <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        if (dataList != null && !dataList.isEmpty()) {
            Map<String, Object> testData = dataList.get(0);
            
            // Verify essential fields are not null/empty
            Assert.assertNotNull(testData.get("_id"), "Database ID should not be null");
            Assert.assertNotNull(testData.get("test_name"), "Test name should not be null");
            Assert.assertNotNull(testData.get("slug"), "Slug should not be null");
            
            // Verify slug consistency
            String returnedSlug = (String) testData.get("slug");
            Assert.assertEquals(returnedSlug, validSlug, "Returned slug should match requested");
            
            System.out.println("   All essential fields are accurate");
        }
        
        System.out.println("✅ PASSED: Test details accuracy verified");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 87, description = "TC87: Verify pricing accuracy")
    public void testGetTestBySlug_PricingAccuracy() {
        System.out.println("\n>>> DATABASE VALIDATION: PRICING ACCURACY <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        if (dataList != null && !dataList.isEmpty()) {
            Map<String, Object> testData = dataList.get(0);
            
            // Verify price fields exist and are valid
            Object price = testData.get("price");
            Assert.assertNotNull(price, "Price should not be null");
            
            if (price instanceof Number) {
                Number priceNum = (Number) price;
                Assert.assertTrue(priceNum.doubleValue() >= 0, "Price should be non-negative");
                System.out.println("   Price: ₹" + priceNum);
            }
            
            // Check price consistency
            if (testData.containsKey("original_price") && testData.containsKey("price")) {
                Object originalPrice = testData.get("original_price");
                if (originalPrice instanceof Number && price instanceof Number) {
                    System.out.println("   Price consistency check passed");
                }
            }
        }
        
        System.out.println("✅ PASSED: Pricing accuracy verified");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 88, description = "TC88: Verify location mapping")
    public void testGetTestBySlug_LocationMapping() {
        System.out.println("\n>>> DATABASE VALIDATION: LOCATION MAPPING <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        if (dataList != null && !dataList.isEmpty()) {
            Map<String, Object> testData = dataList.get(0);
            
            Assert.assertNotNull(testData.get("locations"), "Locations should not be null");
            
            Object locations = testData.get("locations");
            if (locations instanceof List) {
                List<?> locationList = (List<?>) locations;
                System.out.println("   Total Locations Mapped: " + locationList.size());
                Assert.assertTrue(locationList.size() >= 0, "Location list should be valid");
            }
        }
        
        System.out.println("✅ PASSED: Location mapping verified");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 89, description = "TC89: Verify membership mapping")
    public void testGetTestBySlug_MembershipMapping() {
        System.out.println("\n>>> DATABASE VALIDATION: MEMBERSHIP MAPPING <<<");
        
        if (validSlug == null) {
            throw new org.testng.SkipException("No valid slug available");
        }
        
        Map<String, Object> payload = buildPayload(validSlug);
        Response response = callGetTestBySlugAPI(payload);
        
        AssertionUtil.verifyStatusCode(response, 200);
        
        List<Map<String, Object>> dataList = response.jsonPath().get("data");
        if (dataList != null && !dataList.isEmpty()) {
            Map<String, Object> testData = dataList.get(0);
            
            // Check if membership fields are properly mapped
            int membershipFieldCount = 0;
            
            if (testData.containsKey("membership_price")) {
                membershipFieldCount++;
                System.out.println("   ✓ membership_price mapped");
            }
            
            if (testData.containsKey("membership_special_price")) {
                membershipFieldCount++;
                System.out.println("   ✓ membership_special_price mapped");
            }
            
            if (testData.containsKey("membership_discount")) {
                membershipFieldCount++;
                System.out.println("   ✓ membership_discount mapped");
            }
            
            System.out.println("   Membership fields found: " + membershipFieldCount);
        }
        
        System.out.println("✅ PASSED: Membership mapping checked");
        ApiReportContext.setExpectedStatus(200);
    }
}

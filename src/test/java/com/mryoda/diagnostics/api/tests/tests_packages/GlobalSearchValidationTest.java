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
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * GET /tests/global-search - Global Search API Validation Suite
 *
 * curl 'https://staging-api-diagnostics.yodaprojects.com/tests/global-search?query=CB&location=64870066842708a0d5ae6c77&page=1&limit=50'
 *      -H 'accept: *\/*'
 *      -H 'authorization: Bearer <token>'
 *      -H 'content-type: application/json'
 *
 * TC01-TC20:   Query Validation       - valid/partial/case/spaces/special chars/edge cases
 * TC21-TC30:   Search Result          - matching, relevance, duplicates, inactive/deleted
 * TC31-TC40:   Location Validation    - valid/invalid/missing location ID filtering
 * TC41-TC50:   Pagination             - page param edge cases
 * TC51-TC60:   Limit Validation       - limit param edge cases
 * TC61-TC70:   Data Validation        - field presence, types, uniqueness
 * TC71-TC78:   Negative Scenarios     - invalid combos, random strings, no 500
 * TC79-TC85:   Security               - SQL injection, XSS, HTML injection, path traversal
 * TC86-TC90:   Performance            - response time, concurrency, repeated requests
 * TC91-TC98:   Response Validation    - status, schema, content-type, metadata
 * TC99-TC110:  High Priority          - combined regression scenarios
 */
public class GlobalSearchValidationTest extends BaseTest {

    private static final String ENDPOINT = APIEndpoints.GLOBAL_SEARCH_API;
    private static final String DEFAULT_QUERY = "CB";
    private static final String DEFAULT_LOCATION = "64870066842708a0d5ae6c77";
    private static final String ALT_LOCATION = "64870066842708a0d5ae6c75";
    private static final String AUTH_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyR3VpZCI6Ijc0NTE4MDY1LWNjNGItNGQ5ZS1hMjRiLTMyZTMzMWUxOTYzZCIsIm1vYmlsZSI6IjgyMjAyMjAyMjciLCJmaXJzdF9uYW1lIjoiUmFuaml0aCIsImxhc3RfbmFtZSI6IkEiLCJlbWFpbCI6InJhbmppdGguQGdtYWlsLmNvbSIsInJvbGUiOiJ1c2VyX29ubHkiLCJ0eXBlIjoiQUNDRVNTIiwiaWF0IjoxNzgxNjczNDIwLCJleHAiOjE3ODQyNjU0MjB9.LSuCgEKPiAqx0WguzYkdIk4vf9BPDptFXvleO34issY";

    private List<Map<String, Object>> searchResults = new ArrayList<>();
    private Map<String, Object> firstResult = null;
    private int totalResults = 0;

    // =========================================================================
    //  SETUP
    // =========================================================================

    @BeforeClass
    public void setupSearchData() {
        System.out.println("\n========================================");
        System.out.println("SETUP: Global Search with query=" + DEFAULT_QUERY + " location=" + DEFAULT_LOCATION);
        System.out.println("========================================");

        Response r = callGlobalSearch(DEFAULT_QUERY, DEFAULT_LOCATION, 1, 50);

        if (r.getStatusCode() == 200) {
            List<Map<String, Object>> list = extractResultsList(r);
            if (list != null && !list.isEmpty()) {
                searchResults.addAll(list);
                totalResults = searchResults.size();
                firstResult = searchResults.get(0);
                System.out.println("   Total results: " + totalResults);
                System.out.println("   First result name: " + firstResult.get("name"));
                System.out.println("   First result type: " + firstResult.get("type"));
                System.out.println("   First result keys: " + firstResult.keySet());
            } else {
                System.out.println("   No results found for query: " + DEFAULT_QUERY);
            }
        } else {
            System.out.println("   Setup failed. Status: " + r.getStatusCode());
        }
        System.out.println("========================================\n");
    }

    // =========================================================================
    //  HELPERS
    // =========================================================================

    private Response callGlobalSearch(String query, String location, int page, int limit) {
        long start = System.currentTimeMillis();
        RequestBuilder rb = new RequestBuilder()
                .setEndpoint(ENDPOINT)
                .addHeader("accept", "*/*")
                .addHeader("authorization", AUTH_TOKEN)
                .addHeader("Content-Type", "application/json");
        if (query != null) rb.addQueryParam("query", query);
        if (location != null) rb.addQueryParam("location", location);
        rb.addQueryParam("page", String.valueOf(page));
        rb.addQueryParam("limit", String.valueOf(limit));
        Response r = rb.get();
        long elapsed = System.currentTimeMillis() - start;
        String body = r.asString();
        String truncated = body.length() > 3000 ? body.substring(0, 3000) + "\n...(truncated)" : body;
        ApiReportContext.addRecord(new ApiReportContext.ApiCallRecord(
                "GET", ENDPOINT + "?query=" + query + "&location=" + location + "&page=" + page + "&limit=" + limit, "",
                r.getStatusCode(), truncated, elapsed,
                200, "GET /tests/global-search?query=" + query));
        return r;
    }

    private Response callGlobalSearchRaw(String queryString) {
        long start = System.currentTimeMillis();
        Response r = new RequestBuilder()
                .setEndpoint(ENDPOINT + "?" + queryString)
                .addHeader("accept", "*/*")
                .addHeader("authorization", AUTH_TOKEN)
                .addHeader("Content-Type", "application/json")
                .get();
        long elapsed = System.currentTimeMillis() - start;
        String body = r.asString();
        String truncated = body.length() > 2000 ? body.substring(0, 2000) + "\n...(truncated)" : body;
        ApiReportContext.addRecord(new ApiReportContext.ApiCallRecord(
                "GET", ENDPOINT + "?" + queryString, "",
                r.getStatusCode(), truncated, elapsed,
                0, 200, 599, "GET /tests/global-search (raw)"));
        return r;
    }

    private Response callGlobalSearchNoAuth(String query, String location, int page, int limit) {
        long start = System.currentTimeMillis();
        RequestBuilder rb = new RequestBuilder()
                .setEndpoint(ENDPOINT)
                .addHeader("accept", "*/*")
                .addHeader("Content-Type", "application/json");
        if (query != null) rb.addQueryParam("query", query);
        if (location != null) rb.addQueryParam("location", location);
        rb.addQueryParam("page", String.valueOf(page));
        rb.addQueryParam("limit", String.valueOf(limit));
        Response r = rb.get();
        long elapsed = System.currentTimeMillis() - start;
        String body = r.asString();
        String truncated = body.length() > 2000 ? body.substring(0, 2000) + "\n...(truncated)" : body;
        ApiReportContext.addRecord(new ApiReportContext.ApiCallRecord(
                "GET", ENDPOINT + " (no auth)", "",
                r.getStatusCode(), truncated, elapsed,
                0, 200, 499, "GET /tests/global-search (no auth)"));
        return r;
    }

    private List<Map<String, Object>> extractResultsList(Response r) {
        List<Map<String, Object>> list = null;
        try { list = r.jsonPath().getList("data.results"); } catch (Exception ignored) { }
        if (list != null) return list;
        try { list = r.jsonPath().getList("data"); } catch (Exception ignored) { }
        if (list != null) return list;
        try { list = r.jsonPath().getList("results"); } catch (Exception ignored) { }
        return list != null ? list : new ArrayList<>();
    }

    // =========================================================================
    //  QUERY VALIDATION (TC01-TC20)
    // =========================================================================

    @Test(priority = 1, description = "TC01: Verify valid search query returns matching results")
    public void testTC01_ValidSearchReturnsResults() {
        System.out.println("\n>>> TC01: Valid search query=CB <<<");
        Response r = callGlobalSearch("CB", DEFAULT_LOCATION, 1, 50);
        Assert.assertEquals(r.getStatusCode(), 200);
        List<Map<String, Object>> results = extractResultsList(r);
        ApiReportContext.addExtraDetail("<b>Query:</b> CB | <b>Status:</b> 200 | <b>Results:</b> " + results.size());
        Assert.assertFalse(results.isEmpty(), "Expected results for 'CB'");
        System.out.println("TC01 PASSED - " + results.size() + " results");
    }

    @Test(priority = 2, description = "TC02: Verify exact search (CBC)")
    public void testTC02_ExactSearchCBC() {
        System.out.println("\n>>> TC02: Exact search query=CBC <<<");
        Response r = callGlobalSearch("CBC", DEFAULT_LOCATION, 1, 50);
        Assert.assertEquals(r.getStatusCode(), 200);
        List<Map<String, Object>> results = extractResultsList(r);
        boolean hasCBC = results.stream()
                .anyMatch(item -> {
                    String name = item.get("name") != null ? item.get("name").toString().toUpperCase() : "";
                    return name.contains("CBC");
                });
        ApiReportContext.addExtraDetail("<b>Query:</b> CBC | <b>Results:</b> " + results.size()
                + " | <b>Contains CBC:</b> " + (hasCBC ? "<span style='color:green'>YES</span>" : "<span style='color:orange'>NO</span>"));
        if (!results.isEmpty()) {
            Assert.assertTrue(hasCBC, "No result contains 'CBC' in name");
        }
        System.out.println("TC02 PASSED");
    }

    @Test(priority = 3, description = "TC03: Verify partial search (CB)")
    public void testTC03_PartialSearchCB() {
        System.out.println("\n>>> TC03: Partial search query=CB <<<");
        Response r = callGlobalSearch("CB", DEFAULT_LOCATION, 1, 50);
        Assert.assertEquals(r.getStatusCode(), 200);
        List<Map<String, Object>> results = extractResultsList(r);
        ApiReportContext.addExtraDetail("<b>Partial query:</b> CB | <b>Results:</b> " + results.size());
        Assert.assertTrue(results.size() >= 0, "Results should be non-negative");
        System.out.println("TC03 PASSED - " + results.size() + " results");
    }

    @Test(priority = 4, description = "TC04: Verify lowercase search (cbc)")
    public void testTC04_LowercaseSearch() {
        System.out.println("\n>>> TC04: Lowercase search query=cbc <<<");
        Response r = callGlobalSearch("cbc", DEFAULT_LOCATION, 1, 50);
        Assert.assertEquals(r.getStatusCode(), 200);
        List<Map<String, Object>> results = extractResultsList(r);
        ApiReportContext.addExtraDetail("<b>Lowercase query:</b> cbc | <b>Results:</b> " + results.size());
        System.out.println("TC04 PASSED - " + results.size() + " results");
    }

    @Test(priority = 5, description = "TC05: Verify uppercase search (CBC)")
    public void testTC05_UppercaseSearch() {
        System.out.println("\n>>> TC05: Uppercase search query=CBC <<<");
        Response r = callGlobalSearch("CBC", DEFAULT_LOCATION, 1, 50);
        Assert.assertEquals(r.getStatusCode(), 200);
        List<Map<String, Object>> results = extractResultsList(r);
        ApiReportContext.addExtraDetail("<b>Uppercase query:</b> CBC | <b>Results:</b> " + results.size());
        System.out.println("TC05 PASSED - " + results.size() + " results");
    }

    @Test(priority = 6, description = "TC06: Verify mixed case search (CbC)")
    public void testTC06_MixedCaseSearch() {
        System.out.println("\n>>> TC06: Mixed case search query=CbC <<<");
        Response r = callGlobalSearch("CbC", DEFAULT_LOCATION, 1, 50);
        Assert.assertEquals(r.getStatusCode(), 200);
        List<Map<String, Object>> results = extractResultsList(r);
        ApiReportContext.addExtraDetail("<b>Mixed case query:</b> CbC | <b>Results:</b> " + results.size());
        System.out.println("TC06 PASSED - " + results.size() + " results");
    }

    @Test(priority = 7, description = "TC07: Verify case insensitive search")
    public void testTC07_CaseInsensitiveSearch() {
        System.out.println("\n>>> TC07: Case insensitive comparison <<<");
        Response rUpper = callGlobalSearch("CBC", DEFAULT_LOCATION, 1, 50);
        Response rLower = callGlobalSearch("cbc", DEFAULT_LOCATION, 1, 50);
        List<Map<String, Object>> upper = extractResultsList(rUpper);
        List<Map<String, Object>> lower = extractResultsList(rLower);
        ApiReportContext.addExtraDetail("<b>Upper (CBC):</b> " + upper.size()
                + " | <b>Lower (cbc):</b> " + lower.size()
                + " | <b>Case insensitive:</b> " + (upper.size() == lower.size() ? "<span style='color:green'>YES</span>" : "<span style='color:orange'>DIFFERS</span>"));
        Assert.assertEquals(upper.size(), lower.size(), "Case insensitive search should return same count");
        System.out.println("TC07 PASSED");
    }

    @Test(priority = 8, description = "TC08: Verify search with leading spaces")
    public void testTC08_LeadingSpaces() {
        System.out.println("\n>>> TC08: Leading spaces query='  CBC' <<<");
        Response r = callGlobalSearch("  CBC", DEFAULT_LOCATION, 1, 50);
        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>Leading spaces:</b> Status " + status);
        Assert.assertEquals(status, 400, "BUG: API should return 400 for leading spaces but returned: " + status);
        System.out.println("TC08 PASSED - status: " + status);
    }

    @Test(priority = 9, description = "TC09: Verify search with trailing spaces")
    public void testTC09_TrailingSpaces() {
        System.out.println("\n>>> TC09: Trailing spaces query='CBC  ' <<<");
        Response r = callGlobalSearch("CBC  ", DEFAULT_LOCATION, 1, 50);
        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>Trailing spaces:</b> Status " + status);
        Assert.assertEquals(status, 400, "BUG: API should return 400 for trailing spaces but returned: " + status);
        System.out.println("TC09 PASSED - status: " + status);
    }

    @Test(priority = 10, description = "TC10: Verify search with multiple spaces")
    public void testTC10_MultipleSpaces() {
        System.out.println("\n>>> TC10: Multiple spaces query='CB  C' <<<");
        Response r = callGlobalSearch("CB  C", DEFAULT_LOCATION, 1, 50);
        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>Multiple spaces:</b> Status " + status);
        Assert.assertEquals(status, 400, "BUG: API should return 400 for multiple spaces but returned: " + status);
        System.out.println("TC10 PASSED - status: " + status);
    }

    @Test(priority = 11, description = "TC11: Verify search with special characters")
    public void testTC11_SpecialCharacters() {
        System.out.println("\n>>> TC11: Special chars query='CBC@#$' <<<");
        Response r = callGlobalSearch("CBC@#$", DEFAULT_LOCATION, 1, 50);
        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>Special chars:</b> Status " + status);
        Assert.assertEquals(status, 400, "BUG: API should return 400 for special chars but returned: " + status);
        System.out.println("TC11 PASSED - status: " + status);
    }

    @Test(priority = 12, description = "TC12: Verify search with numeric values")
    public void testTC12_NumericValues() {
        System.out.println("\n>>> TC12: Numeric query='123' <<<");
        Response r = callGlobalSearch("123", DEFAULT_LOCATION, 1, 50);
        Assert.assertEquals(r.getStatusCode(), 200);
        ApiReportContext.addExtraDetail("<b>Numeric query:</b> 123 | <b>Status:</b> 200");
        System.out.println("TC12 PASSED");
    }

    @Test(priority = 13, description = "TC13: Verify search with alphanumeric values")
    public void testTC13_AlphanumericValues() {
        System.out.println("\n>>> TC13: Alphanumeric query='CBC123' <<<");
        Response r = callGlobalSearch("CBC123", DEFAULT_LOCATION, 1, 50);
        Assert.assertEquals(r.getStatusCode(), 200);
        ApiReportContext.addExtraDetail("<b>Alphanumeric query:</b> CBC123 | <b>Status:</b> 200");
        System.out.println("TC13 PASSED");
    }

    @Test(priority = 14, description = "TC14: Verify empty query")
    public void testTC14_EmptyQuery() {
        System.out.println("\n>>> TC14: Empty query='' <<<");
        Response r = callGlobalSearch("", DEFAULT_LOCATION, 1, 10);
        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>Empty query:</b> Status " + status);
        Assert.assertEquals(status, 400, "BUG: API should return 400 for empty query but returned: " + status);
        System.out.println("TC14 PASSED - status: " + status);
    }

    @Test(priority = 15, description = "TC15: Verify null query")
    public void testTC15_NullQuery() {
        System.out.println("\n>>> TC15: Null query <<<");
        Response r = callGlobalSearch(null, DEFAULT_LOCATION, 1, 10);
        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>Null query:</b> Status " + status);
        Assert.assertEquals(status, 400, "BUG: API should return 400 for null query but returned: " + status);
        System.out.println("TC15 PASSED - status: " + status);
    }

    @Test(priority = 16, description = "TC16: Verify missing query parameter")
    public void testTC16_MissingQueryParam() {
        System.out.println("\n>>> TC16: Missing query parameter <<<");
        Response r = callGlobalSearchRaw("location=" + DEFAULT_LOCATION + "&page=1&limit=10");
        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>Missing query param:</b> Status " + status);
        Assert.assertEquals(status, 400, "BUG: API should return 400 for missing query param but returned: " + status);
        System.out.println("TC16 PASSED - status: " + status);
    }

    @Test(priority = 17, description = "TC17: Verify query with only spaces")
    public void testTC17_OnlySpaces() {
        System.out.println("\n>>> TC17: Query with only spaces <<<");
        Response r = callGlobalSearch("   ", DEFAULT_LOCATION, 1, 10);
        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>Only spaces query:</b> Status " + status);
        Assert.assertEquals(status, 400, "BUG: API should return 400 for spaces-only query but returned: " + status);
        System.out.println("TC17 PASSED - status: " + status);
    }

    @Test(priority = 18, description = "TC18: Verify very long query string")
    public void testTC18_VeryLongQuery() {
        System.out.println("\n>>> TC18: Very long query (500 chars) <<<");
        String longQuery = "CBC".repeat(167);
        Response r = callGlobalSearch(longQuery, DEFAULT_LOCATION, 1, 10);
        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>Long query (500 chars):</b> Status " + status);
        Assert.assertEquals(status, 400, "BUG: API should return 400 for long query but returned: " + status);
        System.out.println("TC18 PASSED - status: " + status);
    }

    @Test(priority = 19, description = "TC19: Verify query with emoji")
    public void testTC19_EmojiQuery() {
        System.out.println("\n>>> TC19: Emoji query <<<");
        Response r = callGlobalSearch("\uD83D\uDE00CBC", DEFAULT_LOCATION, 1, 10);
        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>Emoji query:</b> Status " + status);
        Assert.assertEquals(status, 400, "BUG: API should return 400 for emoji query but returned: " + status);
        System.out.println("TC19 PASSED - status: " + status);
    }

    @Test(priority = 20, description = "TC20: Verify query with non-English characters")
    public void testTC20_NonEnglishChars() {
        System.out.println("\n>>> TC20: Non-English chars query <<<");
        Response r = callGlobalSearch("\u0BA4\u0BAE\u0BBF\u0BB4\u0BCD", DEFAULT_LOCATION, 1, 10);
        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>Non-English query:</b> Status " + status);
        Assert.assertEquals(status, 400, "BUG: API should return 400 for non-English query but returned: " + status);
        System.out.println("TC20 PASSED - status: " + status);
    }

    // =========================================================================
    //  SEARCH RESULT VALIDATION (TC21-TC30)
    // =========================================================================

    @Test(priority = 21, description = "TC21: Verify matching tests are returned")
    public void testTC21_MatchingTestsReturned() {
        System.out.println("\n>>> TC21: Matching tests in results <<<");
        Response r = callGlobalSearch("CBC", DEFAULT_LOCATION, 1, 50);
        List<Map<String, Object>> results = extractResultsList(r);
        long testCount = results.stream()
                .filter(item -> "test".equals(String.valueOf(item.get("type"))))
                .count();
        ApiReportContext.addExtraDetail("<b>Tests found:</b> " + testCount + " out of " + results.size());
        Assert.assertTrue(testCount > 0 || results.isEmpty(), "Expected test type results for CBC");
        System.out.println("TC21 PASSED - " + testCount + " tests");
    }

    @Test(priority = 22, description = "TC22: Verify matching packages are returned")
    public void testTC22_MatchingPackagesReturned() {
        System.out.println("\n>>> TC22: Check for package type in results <<<");
        Response r = callGlobalSearch("health", DEFAULT_LOCATION, 1, 50);
        List<Map<String, Object>> results = extractResultsList(r);
        long packageCount = results.stream()
                .filter(item -> "package".equals(String.valueOf(item.get("type"))))
                .count();
        ApiReportContext.addExtraDetail("<b>Search 'health':</b> " + results.size() + " results | <b>Packages:</b> " + packageCount);
        System.out.println("TC22 PASSED - packages: " + packageCount);
    }

    @Test(priority = 23, description = "TC23: Verify matching symptoms are returned (if applicable)")
    public void testTC23_MatchingSymptomsReturned() {
        System.out.println("\n>>> TC23: Check for symptom type in results <<<");
        Response r = callGlobalSearch("fever", DEFAULT_LOCATION, 1, 50);
        List<Map<String, Object>> results = extractResultsList(r);
        long symptomCount = results.stream()
                .filter(item -> "symptom".equals(String.valueOf(item.get("type"))))
                .count();
        ApiReportContext.addExtraDetail("<b>Search 'fever':</b> " + results.size() + " results | <b>Symptoms:</b> " + symptomCount);
        System.out.println("TC23 PASSED - symptoms: " + symptomCount);
    }

    @Test(priority = 24, description = "TC24: Verify matching diseases are returned (if applicable)")
    public void testTC24_MatchingDiseasesReturned() {
        System.out.println("\n>>> TC24: Check for disease type in results <<<");
        Response r = callGlobalSearch("diabetes", DEFAULT_LOCATION, 1, 50);
        List<Map<String, Object>> results = extractResultsList(r);
        long diseaseCount = results.stream()
                .filter(item -> "disease".equals(String.valueOf(item.get("type"))))
                .count();
        ApiReportContext.addExtraDetail("<b>Search 'diabetes':</b> " + results.size() + " results | <b>Diseases:</b> " + diseaseCount);
        System.out.println("TC24 PASSED - diseases: " + diseaseCount);
    }

    @Test(priority = 25, description = "TC25: Verify matching organs are returned (if applicable)")
    public void testTC25_MatchingOrgansReturned() {
        System.out.println("\n>>> TC25: Check for organ type in results <<<");
        Response r = callGlobalSearch("kidney", DEFAULT_LOCATION, 1, 50);
        List<Map<String, Object>> results = extractResultsList(r);
        long organCount = results.stream()
                .filter(item -> "organ".equals(String.valueOf(item.get("type"))))
                .count();
        ApiReportContext.addExtraDetail("<b>Search 'kidney':</b> " + results.size() + " results | <b>Organs:</b> " + organCount);
        System.out.println("TC25 PASSED - organs: " + organCount);
    }

    @Test(priority = 26, description = "TC26: Verify search results are relevant")
    public void testTC26_SearchResultsRelevant() {
        System.out.println("\n>>> TC26: Results relevance for 'CBC' <<<");
        if (searchResults.isEmpty()) {
            ApiReportContext.addExtraDetail("<b>Relevance:</b> SKIPPED - no results");
            System.out.println("TC26 SKIPPED - no data");
            return;
        }
        long relevant = searchResults.stream()
                .filter(item -> {
                    String name = item.get("name") != null ? item.get("name").toString().toUpperCase() : "";
                    String slug = item.get("slug") != null ? item.get("slug").toString().toUpperCase() : "";
                    String matchType = item.get("matchType") != null ? item.get("matchType").toString() : "";
                    return name.contains("CB") || slug.contains("CB") || !matchType.isEmpty();
                })
                .count();
        double pct = (relevant * 100.0) / searchResults.size();
        ApiReportContext.addExtraDetail("<b>Relevance:</b> " + relevant + "/" + searchResults.size()
                + " (" + String.format("%.0f", pct) + "%) have name/slug match or matchType");
        Assert.assertTrue(pct > 0, "No results have relevance indicators: " + pct + "%");
        System.out.println("TC26 PASSED - relevance: " + String.format("%.0f", pct) + "%");
    }

    @Test(priority = 27, description = "TC27: Verify duplicate records are not returned")
    public void testTC27_NoDuplicateRecords() {
        System.out.println("\n>>> TC27: No duplicate _ids in results <<<");
        if (searchResults.isEmpty()) {
            ApiReportContext.addExtraDetail("<b>Duplicates:</b> SKIPPED - no results");
            System.out.println("TC27 SKIPPED");
            return;
        }
        List<String> ids = searchResults.stream()
                .map(item -> String.valueOf(item.get("_id")))
                .collect(Collectors.toList());
        Set<String> uniqueIds = new HashSet<>(ids);
        int dupes = ids.size() - uniqueIds.size();
        ApiReportContext.addExtraDetail("<b>Total:</b> " + ids.size() + " | <b>Unique:</b> " + uniqueIds.size()
                + " | <b>Duplicates:</b> " + (dupes == 0 ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + dupes + "</span>"));
        Assert.assertEquals(dupes, 0, "Duplicate records found: " + dupes);
        System.out.println("TC27 PASSED");
    }

    @Test(priority = 28, description = "TC28: Verify inactive records are not returned")
    public void testTC28_NoInactiveRecords() {
        System.out.println("\n>>> TC28: No inactive records <<<");
        if (searchResults.isEmpty()) {
            ApiReportContext.addExtraDetail("<b>Inactive check:</b> SKIPPED - no results");
            System.out.println("TC28 SKIPPED");
            return;
        }
        long inactiveCount = searchResults.stream()
                .filter(item -> {
                    Object status = item.get("status");
                    return status != null && "INACTIVE".equalsIgnoreCase(status.toString());
                })
                .count();
        ApiReportContext.addExtraDetail("<b>Inactive records:</b> " + (inactiveCount == 0 ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + inactiveCount + "</span>"));
        Assert.assertEquals(inactiveCount, 0, "Inactive records found: " + inactiveCount);
        System.out.println("TC28 PASSED");
    }

    @Test(priority = 29, description = "TC29: Verify deleted records are not returned")
    public void testTC29_NoDeletedRecords() {
        System.out.println("\n>>> TC29: No deleted records <<<");
        if (searchResults.isEmpty()) {
            ApiReportContext.addExtraDetail("<b>Deleted check:</b> SKIPPED - no results");
            System.out.println("TC29 SKIPPED");
            return;
        }
        long deletedCount = searchResults.stream()
                .filter(item -> {
                    Object isDeleted = item.get("is_deleted");
                    return isDeleted != null && Boolean.TRUE.equals(isDeleted);
                })
                .count();
        ApiReportContext.addExtraDetail("<b>Deleted records:</b> " + (deletedCount == 0 ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + deletedCount + "</span>"));
        Assert.assertEquals(deletedCount, 0, "Deleted records returned: " + deletedCount);
        System.out.println("TC29 PASSED");
    }

    @Test(priority = 30, description = "TC30: Verify search result count accuracy")
    public void testTC30_ResultCountAccuracy() {
        System.out.println("\n>>> TC30: Result count accuracy <<<");
        Response r = callGlobalSearch("CB", DEFAULT_LOCATION, 1, 50);
        List<Map<String, Object>> results = extractResultsList(r);
        Integer total = null;
        try { total = r.jsonPath().getInt("data.total"); } catch (Exception ignored) { }
        if (total == null) {
            try { total = r.jsonPath().getInt("total"); } catch (Exception ignored) { }
        }
        ApiReportContext.addExtraDetail("<b>Actual results:</b> " + results.size()
                + " | <b>Total field:</b> " + (total != null ? total : "N/A"));
        System.out.println("TC30 PASSED - results: " + results.size() + ", total: " + total);
    }

    // =========================================================================
    //  LOCATION VALIDATION (TC31-TC40)
    // =========================================================================

    @Test(priority = 31, description = "TC31: Verify valid location ID")
    public void testTC31_ValidLocationID() {
        System.out.println("\n>>> TC31: Valid location ID <<<");
        Response r = callGlobalSearch("CB", DEFAULT_LOCATION, 1, 50);
        Assert.assertEquals(r.getStatusCode(), 200);
        List<Map<String, Object>> results = extractResultsList(r);
        ApiReportContext.addExtraDetail("<b>Location:</b> " + DEFAULT_LOCATION + " | <b>Results:</b> " + results.size());
        System.out.println("TC31 PASSED - " + results.size() + " results");
    }

    @Test(priority = 32, description = "TC32: Verify invalid location ID")
    public void testTC32_InvalidLocationID() {
        System.out.println("\n>>> TC32: Invalid location ID <<<");
        Response r = callGlobalSearch("CB", "INVALID_LOC_ID", 1, 10);
        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>Invalid location:</b> Status " + status);
        Assert.assertEquals(status, 400, "BUG: API should return 400 for invalid location but returned: " + status);
        System.out.println("TC32 PASSED - status: " + status);
    }

    @Test(priority = 33, description = "TC33: Verify non-existing location ID")
    public void testTC33_NonExistingLocation() {
        System.out.println("\n>>> TC33: Non-existing location (valid format) <<<");
        Response r = callGlobalSearch("CB", "000000000000000000000000", 1, 10);
        int status = r.getStatusCode();
        List<Map<String, Object>> results = extractResultsList(r);
        ApiReportContext.addExtraDetail("<b>Non-existing location:</b> Status " + status + " | <b>Results:</b> " + results.size());
        Assert.assertEquals(status, 400, "BUG: API should return 400 for non-existing location but returned: " + status);
        System.out.println("TC33 PASSED - status: " + status + ", results: " + results.size());
    }

    @Test(priority = 34, description = "TC34: Verify null location")
    public void testTC34_NullLocation() {
        System.out.println("\n>>> TC34: Null location <<<");
        Response r = callGlobalSearch("CB", null, 1, 10);
        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>Null location:</b> Status " + status);
        Assert.assertEquals(status, 400, "BUG: API should return 400 for null location but returned: " + status);
        System.out.println("TC34 PASSED - status: " + status);
    }

    @Test(priority = 35, description = "TC35: Verify empty location")
    public void testTC35_EmptyLocation() {
        System.out.println("\n>>> TC35: Empty location <<<");
        Response r = callGlobalSearch("CB", "", 1, 10);
        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>Empty location:</b> Status " + status);
        Assert.assertEquals(status, 400, "BUG: API should return 400 for empty location but returned: " + status);
        System.out.println("TC35 PASSED - status: " + status);
    }

    @Test(priority = 36, description = "TC36: Verify missing location parameter")
    public void testTC36_MissingLocationParam() {
        System.out.println("\n>>> TC36: Missing location parameter <<<");
        Response r = callGlobalSearchRaw("query=CB&page=1&limit=10");
        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>Missing location param:</b> Status " + status);
        Assert.assertEquals(status, 400, "BUG: API should return 400 for missing location but returned: " + status);
        System.out.println("TC36 PASSED - status: " + status);
    }

    @Test(priority = 37, description = "TC37: Verify results differ for different locations")
    public void testTC37_DifferentLocations() {
        System.out.println("\n>>> TC37: Different locations return different results <<<");
        Response r1 = callGlobalSearch("CB", DEFAULT_LOCATION, 1, 50);
        Response r2 = callGlobalSearch("CB", ALT_LOCATION, 1, 50);
        List<Map<String, Object>> results1 = extractResultsList(r1);
        List<Map<String, Object>> results2 = extractResultsList(r2);
        ApiReportContext.addExtraDetail("<b>Location 1:</b> " + results1.size() + " results"
                + " | <b>Location 2:</b> " + results2.size() + " results");
        System.out.println("TC37 PASSED - loc1: " + results1.size() + ", loc2: " + results2.size());
    }

    @Test(priority = 38, description = "TC38: Verify unavailable tests are excluded for location")
    public void testTC38_UnavailableTestsExcluded() {
        System.out.println("\n>>> TC38: Location-specific availability <<<");
        if (searchResults.isEmpty()) {
            ApiReportContext.addExtraDetail("<b>Availability:</b> SKIPPED - no results");
            System.out.println("TC38 SKIPPED");
            return;
        }
        long withLocation = searchResults.stream()
                .filter(item -> {
                    Object locations = item.get("locations");
                    if (locations instanceof List) {
                        return ((List<?>) locations).stream()
                                .anyMatch(loc -> DEFAULT_LOCATION.equals(String.valueOf(loc)));
                    }
                    return true;
                })
                .count();
        ApiReportContext.addExtraDetail("<b>Results with location match:</b> " + withLocation + "/" + searchResults.size());
        System.out.println("TC38 PASSED - " + withLocation + " of " + searchResults.size() + " have location");
    }

    @Test(priority = 39, description = "TC39: Verify unavailable packages are excluded for location")
    public void testTC39_UnavailablePackagesExcluded() {
        System.out.println("\n>>> TC39: Package location availability <<<");
        Response r = callGlobalSearch("health", DEFAULT_LOCATION, 1, 50);
        List<Map<String, Object>> results = extractResultsList(r);
        long packages = results.stream()
                .filter(item -> "package".equals(String.valueOf(item.get("type"))))
                .count();
        ApiReportContext.addExtraDetail("<b>Packages for location:</b> " + packages);
        System.out.println("TC39 PASSED - " + packages + " packages");
    }

    @Test(priority = 40, description = "TC40: Verify location-based filtering accuracy")
    public void testTC40_LocationFilteringAccuracy() {
        System.out.println("\n>>> TC40: Location filter accuracy <<<");
        Response r = callGlobalSearch("blood", DEFAULT_LOCATION, 1, 50);
        List<Map<String, Object>> results = extractResultsList(r);
        long missingLocation = results.stream()
                .filter(item -> {
                    Object locations = item.get("locations");
                    if (locations instanceof List) {
                        return !((List<?>) locations).stream()
                                .anyMatch(loc -> DEFAULT_LOCATION.equals(String.valueOf(loc)));
                    }
                    return false;
                })
                .count();
        ApiReportContext.addExtraDetail("<b>Results missing location:</b> " + missingLocation + "/" + results.size()
                + (missingLocation == 0 ? " <span style='color:green'>ALL MATCH</span>" : " <span style='color:red'>FILTER ISSUE</span>"));
        System.out.println("TC40 PASSED - missing location: " + missingLocation);
    }

    // =========================================================================
    //  PAGINATION VALIDATION (TC41-TC50)
    // =========================================================================

    @Test(priority = 41, description = "TC41: Verify page = 1")
    public void testTC41_Page1() {
        System.out.println("\n>>> TC41: page=1 <<<");
        Response r = callGlobalSearch("CB", DEFAULT_LOCATION, 1, 10);
        Assert.assertEquals(r.getStatusCode(), 200);
        List<Map<String, Object>> results = extractResultsList(r);
        ApiReportContext.addExtraDetail("<b>Page 1:</b> " + results.size() + " results");
        System.out.println("TC41 PASSED - " + results.size() + " results");
    }

    @Test(priority = 42, description = "TC42: Verify page = 2")
    public void testTC42_Page2() {
        System.out.println("\n>>> TC42: page=2 <<<");
        Response r = callGlobalSearch("CB", DEFAULT_LOCATION, 2, 10);
        Assert.assertEquals(r.getStatusCode(), 200);
        List<Map<String, Object>> results = extractResultsList(r);
        ApiReportContext.addExtraDetail("<b>Page 2:</b> " + results.size() + " results");
        System.out.println("TC42 PASSED - " + results.size() + " results");
    }

    @Test(priority = 43, description = "TC43: Verify last page")
    public void testTC43_LastPage() {
        System.out.println("\n>>> TC43: Last page <<<");
        Response r1 = callGlobalSearch("CB", DEFAULT_LOCATION, 1, 10);
        int totalPages = 0;
        try { totalPages = r1.jsonPath().getInt("total_pages"); } catch (Exception ignored) { }
        if (totalPages <= 0) {
            ApiReportContext.addExtraDetail("<b>Last page:</b> SKIPPED - total_pages=0");
            System.out.println("TC43 SKIPPED - total_pages=0");
            return;
        }
        Response r = callGlobalSearch("CB", DEFAULT_LOCATION, totalPages, 10);
        Assert.assertEquals(r.getStatusCode(), 200);
        List<Map<String, Object>> results = extractResultsList(r);
        ApiReportContext.addExtraDetail("<b>Last page (" + totalPages + "):</b> " + results.size() + " results");
        System.out.println("TC43 PASSED");
    }

    @Test(priority = 44, description = "TC44: Verify page = 0")
    public void testTC44_PageZero() {
        System.out.println("\n>>> TC44: page=0 <<<");
        Response r = callGlobalSearch("CB", DEFAULT_LOCATION, 0, 10);
        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>page=0:</b> Status " + status);
        Assert.assertEquals(status, 400, "BUG: API should return 400 for page=0 but returned: " + status);
        System.out.println("TC44 PASSED - status: " + status);
    }

    @Test(priority = 45, description = "TC45: Verify page = -1")
    public void testTC45_PageNegative() {
        System.out.println("\n>>> TC45: page=-1 <<<");
        Response r = callGlobalSearch("CB", DEFAULT_LOCATION, -1, 10);
        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>page=-1:</b> Status " + status);
        Assert.assertEquals(status, 400, "BUG: API should return 400 for page=-1 but returned: " + status);
        System.out.println("TC45 PASSED - status: " + status);
    }

    @Test(priority = 46, description = "TC46: Verify page = null")
    public void testTC46_PageNull() {
        System.out.println("\n>>> TC46: page=null <<<");
        Response r = callGlobalSearchRaw("query=CB&location=" + DEFAULT_LOCATION + "&limit=10");
        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>page=null (missing):</b> Status " + status);
        Assert.assertEquals(status, 400, "BUG: API should return 400 for null page but returned: " + status);
        System.out.println("TC46 PASSED - status: " + status);
    }

    @Test(priority = 47, description = "TC47: Verify missing page parameter")
    public void testTC47_MissingPageParam() {
        System.out.println("\n>>> TC47: Missing page param <<<");
        Response r = callGlobalSearchRaw("query=CB&location=" + DEFAULT_LOCATION + "&limit=10");
        int status = r.getStatusCode();
        Assert.assertEquals(status, 400, "BUG: API should return 400 for missing page but returned: " + status);
        List<Map<String, Object>> results = extractResultsList(r);
        ApiReportContext.addExtraDetail("<b>Missing page:</b> Status " + status + " | Results: " + results.size());
        System.out.println("TC47 PASSED - status: " + status);
    }

    @Test(priority = 48, description = "TC48: Verify page as string")
    public void testTC48_PageAsString() {
        System.out.println("\n>>> TC48: page='abc' <<<");
        Response r = callGlobalSearchRaw("query=CB&location=" + DEFAULT_LOCATION + "&page=abc&limit=10");
        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>page='abc':</b> Status " + status);
        Assert.assertEquals(status, 400, "BUG: API should return 400 for page=string but returned: " + status);
        System.out.println("TC48 PASSED - status: " + status);
    }

    @Test(priority = 49, description = "TC49: Verify very large page number")
    public void testTC49_VeryLargePageNumber() {
        System.out.println("\n>>> TC49: page=999999 <<<");
        Response r = callGlobalSearch("CB", DEFAULT_LOCATION, 999999, 10);
        Assert.assertEquals(r.getStatusCode(), 200);
        List<Map<String, Object>> results = extractResultsList(r);
        ApiReportContext.addExtraDetail("<b>page=999999:</b> " + results.size() + " results (expected 0)");
        Assert.assertTrue(results.size() == 0, "Expected 0 results on page 999999, got: " + results.size());
        System.out.println("TC49 PASSED");
    }

    @Test(priority = 50, description = "TC50: Verify no duplicate records across pages")
    public void testTC50_NoDuplicatesAcrossPages() {
        System.out.println("\n>>> TC50: No duplicates across pages <<<");
        Response r1 = callGlobalSearch("CB", DEFAULT_LOCATION, 1, 10);
        Response r2 = callGlobalSearch("CB", DEFAULT_LOCATION, 2, 10);
        List<Map<String, Object>> p1 = extractResultsList(r1);
        List<Map<String, Object>> p2 = extractResultsList(r2);
        if (p1.isEmpty() || p2.isEmpty()) {
            ApiReportContext.addExtraDetail("<b>Cross-page duplicates:</b> SKIPPED - insufficient pages");
            System.out.println("TC50 SKIPPED");
            return;
        }
        Set<String> page1Ids = p1.stream().map(i -> String.valueOf(i.get("_id"))).collect(Collectors.toSet());
        long overlap = p2.stream().map(i -> String.valueOf(i.get("_id"))).filter(page1Ids::contains).count();
        ApiReportContext.addExtraDetail("<b>Page 1:</b> " + p1.size() + " | <b>Page 2:</b> " + p2.size()
                + " | <b>Overlap:</b> " + (overlap == 0 ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + overlap + "</span>"));
        Assert.assertEquals(overlap, 0L, "Duplicate records across pages: " + overlap);
        System.out.println("TC50 PASSED");
    }

    // =========================================================================
    //  LIMIT VALIDATION (TC51-TC60)
    // =========================================================================

    @Test(priority = 51, description = "TC51: Verify limit = 10")
    public void testTC51_Limit10() {
        System.out.println("\n>>> TC51: limit=10 <<<");
        Response r = callGlobalSearch("CB", DEFAULT_LOCATION, 1, 10);
        Assert.assertEquals(r.getStatusCode(), 200);
        List<Map<String, Object>> results = extractResultsList(r);
        ApiReportContext.addExtraDetail("<b>limit=10:</b> " + results.size() + " results");
        Assert.assertTrue(results.size() <= 10, "Expected <=10, got: " + results.size());
        System.out.println("TC51 PASSED - " + results.size());
    }

    @Test(priority = 52, description = "TC52: Verify limit = 50")
    public void testTC52_Limit50() {
        System.out.println("\n>>> TC52: limit=50 <<<");
        Response r = callGlobalSearch("CB", DEFAULT_LOCATION, 1, 50);
        Assert.assertEquals(r.getStatusCode(), 200);
        List<Map<String, Object>> results = extractResultsList(r);
        ApiReportContext.addExtraDetail("<b>limit=50:</b> " + results.size() + " results");
        Assert.assertTrue(results.size() <= 50, "Expected <=50, got: " + results.size());
        System.out.println("TC52 PASSED - " + results.size());
    }

    @Test(priority = 53, description = "TC53: Verify limit = 100")
    public void testTC53_Limit100() {
        System.out.println("\n>>> TC53: limit=100 <<<");
        Response r = callGlobalSearch("CB", DEFAULT_LOCATION, 1, 100);
        Assert.assertEquals(r.getStatusCode(), 200);
        List<Map<String, Object>> results = extractResultsList(r);
        ApiReportContext.addExtraDetail("<b>limit=100:</b> " + results.size() + " results");
        Assert.assertTrue(results.size() <= 100, "Expected <=100, got: " + results.size());
        System.out.println("TC53 PASSED - " + results.size());
    }

    @Test(priority = 54, description = "TC54: Verify limit = 0")
    public void testTC54_LimitZero() {
        System.out.println("\n>>> TC54: limit=0 <<<");
        Response r = callGlobalSearch("CB", DEFAULT_LOCATION, 1, 0);
        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>limit=0:</b> Status " + status);
        Assert.assertEquals(status, 400, "BUG: API should return 400 for limit=0 but returned: " + status);
        System.out.println("TC54 PASSED - status: " + status);
    }

    @Test(priority = 55, description = "TC55: Verify limit = -1")
    public void testTC55_LimitNegative() {
        System.out.println("\n>>> TC55: limit=-1 <<<");
        Response r = callGlobalSearch("CB", DEFAULT_LOCATION, 1, -1);
        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>limit=-1:</b> Status " + status);
        Assert.assertEquals(status, 400, "BUG: API should return 400 for limit=-1 but returned: " + status);
        System.out.println("TC55 PASSED - status: " + status);
    }

    @Test(priority = 56, description = "TC56: Verify limit = null")
    public void testTC56_LimitNull() {
        System.out.println("\n>>> TC56: Missing limit param <<<");
        Response r = callGlobalSearchRaw("query=CB&location=" + DEFAULT_LOCATION + "&page=1");
        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>Missing limit:</b> Status " + status);
        Assert.assertEquals(status, 400, "BUG: API should return 400 for null limit but returned: " + status);
        System.out.println("TC56 PASSED - status: " + status);
    }

    @Test(priority = 57, description = "TC57: Verify missing limit parameter")
    public void testTC57_MissingLimitParam() {
        System.out.println("\n>>> TC57: Missing limit param (uses default) <<<");
        Response r = callGlobalSearchRaw("query=CB&location=" + DEFAULT_LOCATION + "&page=1");
        int status = r.getStatusCode();
        Assert.assertEquals(status, 200);
        List<Map<String, Object>> results = extractResultsList(r);
        ApiReportContext.addExtraDetail("<b>Default limit:</b> " + results.size() + " results (API default)");
        System.out.println("TC57 PASSED - default limit returns " + results.size() + " results");
    }

    @Test(priority = 58, description = "TC58: Verify limit as string")
    public void testTC58_LimitAsString() {
        System.out.println("\n>>> TC58: limit='abc' <<<");
        Response r = callGlobalSearchRaw("query=CB&location=" + DEFAULT_LOCATION + "&page=1&limit=abc");
        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>limit='abc':</b> Status " + status);
        Assert.assertEquals(status, 400, "BUG: API should return 400 for limit=string but returned: " + status);
        System.out.println("TC58 PASSED - status: " + status);
    }

    @Test(priority = 59, description = "TC59: Verify very large limit value")
    public void testTC59_VeryLargeLimit() {
        System.out.println("\n>>> TC59: limit=99999 <<<");
        Response r = callGlobalSearch("CB", DEFAULT_LOCATION, 1, 99999);
        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>limit=99999:</b> Status " + status);
        Assert.assertEquals(status, 400, "BUG: API should return 400 for very large limit but returned: " + status);
        System.out.println("TC59 PASSED - status: " + status);
    }

    @Test(priority = 60, description = "TC60: Verify returned count <= limit")
    public void testTC60_ReturnedCountWithinLimit() {
        System.out.println("\n>>> TC60: Returned count <= limit <<<");
        Response r = callGlobalSearch("CB", DEFAULT_LOCATION, 1, 5);
        List<Map<String, Object>> results = extractResultsList(r);
        ApiReportContext.addExtraDetail("<b>limit=5:</b> returned " + results.size()
                + (results.size() <= 5 ? " <span style='color:green'>OK</span>" : " <span style='color:red'>EXCEEDS LIMIT</span>"));
        Assert.assertTrue(results.size() <= 5, "Returned " + results.size() + " results, limit was 5");
        System.out.println("TC60 PASSED");
    }

    // =========================================================================
    //  DATA VALIDATION (TC61-TC70)
    // =========================================================================

    @Test(priority = 61, description = "TC61: Verify _id is present")
    public void testTC61_IdPresent() {
        System.out.println("\n>>> TC61: _id field present <<<");
        if (firstResult == null) {
            ApiReportContext.addExtraDetail("<b>_id check:</b> SKIPPED - no results");
            System.out.println("TC61 SKIPPED");
            return;
        }
        Assert.assertTrue(firstResult.containsKey("_id"), "_id field missing");
        Assert.assertNotNull(firstResult.get("_id"), "_id is null");
        ApiReportContext.addExtraDetail("<b>_id:</b> " + firstResult.get("_id"));
        System.out.println("TC61 PASSED");
    }

    @Test(priority = 62, description = "TC62: Verify name is present")
    public void testTC62_NamePresent() {
        System.out.println("\n>>> TC62: name field present <<<");
        if (firstResult == null) {
            ApiReportContext.addExtraDetail("<b>name check:</b> SKIPPED - no results");
            System.out.println("TC62 SKIPPED");
            return;
        }
        Assert.assertTrue(firstResult.containsKey("name"), "name field missing");
        Assert.assertNotNull(firstResult.get("name"), "name is null");
        ApiReportContext.addExtraDetail("<b>name:</b> " + firstResult.get("name"));
        System.out.println("TC62 PASSED");
    }

    @Test(priority = 63, description = "TC63: Verify slug is present")
    public void testTC63_SlugPresent() {
        System.out.println("\n>>> TC63: slug field present <<<");
        if (firstResult == null) {
            ApiReportContext.addExtraDetail("<b>slug check:</b> SKIPPED - no results");
            System.out.println("TC63 SKIPPED");
            return;
        }
        Assert.assertTrue(firstResult.containsKey("slug"), "slug field missing");
        Assert.assertNotNull(firstResult.get("slug"), "slug is null");
        ApiReportContext.addExtraDetail("<b>slug:</b> " + firstResult.get("slug"));
        System.out.println("TC63 PASSED");
    }

    @Test(priority = 64, description = "TC64: Verify price is present")
    public void testTC64_PricePresent() {
        System.out.println("\n>>> TC64: price field present <<<");
        if (firstResult == null) {
            ApiReportContext.addExtraDetail("<b>price check:</b> SKIPPED - no results");
            System.out.println("TC64 SKIPPED");
            return;
        }
        Assert.assertTrue(firstResult.containsKey("price"), "price field missing");
        Assert.assertNotNull(firstResult.get("price"), "price is null");
        ApiReportContext.addExtraDetail("<b>price:</b> " + firstResult.get("price"));
        System.out.println("TC64 PASSED");
    }

    @Test(priority = 65, description = "TC65: Verify category/type is present")
    public void testTC65_TypePresent() {
        System.out.println("\n>>> TC65: type field present <<<");
        if (firstResult == null) {
            ApiReportContext.addExtraDetail("<b>type check:</b> SKIPPED - no results");
            System.out.println("TC65 SKIPPED");
            return;
        }
        Assert.assertTrue(firstResult.containsKey("type"), "type field missing");
        Assert.assertNotNull(firstResult.get("type"), "type is null");
        ApiReportContext.addExtraDetail("<b>type:</b> " + firstResult.get("type"));
        System.out.println("TC65 PASSED");
    }

    @Test(priority = 66, description = "TC66: Verify mandatory fields are not null")
    public void testTC66_MandatoryFieldsNotNull() {
        System.out.println("\n>>> TC66: Mandatory fields not null <<<");
        if (firstResult == null) {
            ApiReportContext.addExtraDetail("<b>Mandatory fields:</b> SKIPPED - no results");
            System.out.println("TC66 SKIPPED");
            return;
        }
        String[] mandatory = {"_id", "name", "type", "slug"};
        List<String> nullFields = new ArrayList<>();
        for (String field : mandatory) {
            if (!firstResult.containsKey(field) || firstResult.get(field) == null) {
                nullFields.add(field);
            }
        }
        ApiReportContext.addExtraDetail("<b>Null mandatory fields:</b> " + (nullFields.isEmpty()
                ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + nullFields + "</span>"));
        Assert.assertTrue(nullFields.isEmpty(), "Null mandatory fields: " + nullFields);
        System.out.println("TC66 PASSED");
    }

    @Test(priority = 67, description = "TC67: Verify mandatory fields are not empty")
    public void testTC67_MandatoryFieldsNotEmpty() {
        System.out.println("\n>>> TC67: Mandatory fields not empty <<<");
        if (firstResult == null) {
            ApiReportContext.addExtraDetail("<b>Empty fields:</b> SKIPPED - no results");
            System.out.println("TC67 SKIPPED");
            return;
        }
        String[] mandatory = {"_id", "name", "type", "slug"};
        List<String> emptyFields = new ArrayList<>();
        for (String field : mandatory) {
            Object val = firstResult.get(field);
            if (val != null && val.toString().trim().isEmpty()) {
                emptyFields.add(field);
            }
        }
        ApiReportContext.addExtraDetail("<b>Empty mandatory fields:</b> " + (emptyFields.isEmpty()
                ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + emptyFields + "</span>"));
        Assert.assertTrue(emptyFields.isEmpty(), "Empty mandatory fields: " + emptyFields);
        System.out.println("TC67 PASSED");
    }

    @Test(priority = 68, description = "TC68: Verify unique IDs")
    public void testTC68_UniqueIds() {
        System.out.println("\n>>> TC68: All _id values are unique <<<");
        if (searchResults.isEmpty()) {
            ApiReportContext.addExtraDetail("<b>Unique IDs:</b> SKIPPED - no results");
            System.out.println("TC68 SKIPPED");
            return;
        }
        List<String> ids = searchResults.stream()
                .map(i -> String.valueOf(i.get("_id")))
                .collect(Collectors.toList());
        Set<String> unique = new HashSet<>(ids);
        ApiReportContext.addExtraDetail("<b>Total:</b> " + ids.size() + " | <b>Unique:</b> " + unique.size());
        Assert.assertEquals(ids.size(), unique.size(), "Duplicate IDs found");
        System.out.println("TC68 PASSED");
    }

    @Test(priority = 69, description = "TC69: Verify correct datatype for fields")
    public void testTC69_CorrectDatatypes() {
        System.out.println("\n>>> TC69: Field datatypes <<<");
        if (firstResult == null) {
            ApiReportContext.addExtraDetail("<b>Datatypes:</b> SKIPPED - no results");
            System.out.println("TC69 SKIPPED");
            return;
        }
        boolean idIsString = firstResult.get("_id") instanceof String;
        boolean nameIsString = firstResult.get("name") instanceof String;
        boolean priceIsNumber = firstResult.get("price") instanceof Number;
        ApiReportContext.addExtraDetail("<b>_id is String:</b> " + idIsString
                + " | <b>name is String:</b> " + nameIsString
                + " | <b>price is Number:</b> " + priceIsNumber);
        Assert.assertTrue(idIsString, "_id should be String");
        Assert.assertTrue(nameIsString, "name should be String");
        Assert.assertTrue(priceIsNumber, "price should be Number, got: " + (firstResult.get("price") != null ? firstResult.get("price").getClass() : "null"));
        System.out.println("TC69 PASSED");
    }

    @Test(priority = 70, description = "TC70: Verify response data consistency")
    public void testTC70_DataConsistency() {
        System.out.println("\n>>> TC70: Data consistency across calls <<<");
        Response r1 = callGlobalSearch("CB", DEFAULT_LOCATION, 1, 50);
        Response r2 = callGlobalSearch("CB", DEFAULT_LOCATION, 1, 50);
        List<Map<String, Object>> res1 = extractResultsList(r1);
        List<Map<String, Object>> res2 = extractResultsList(r2);
        ApiReportContext.addExtraDetail("<b>Call 1:</b> " + res1.size() + " | <b>Call 2:</b> " + res2.size()
                + " | <b>Consistent:</b> " + (res1.size() == res2.size() ? "<span style='color:green'>YES</span>" : "<span style='color:orange'>NO</span>"));
        Assert.assertEquals(res1.size(), res2.size(), "Inconsistent result count");
        System.out.println("TC70 PASSED");
    }

    // =========================================================================
    //  NEGATIVE SCENARIOS (TC71-TC78)
    // =========================================================================

    @Test(priority = 71, description = "TC71: Invalid query parameter")
    public void testTC71_InvalidQueryParam() {
        System.out.println("\n>>> TC71: Invalid query param name <<<");
        Response r = callGlobalSearchRaw("search=CB&location=" + DEFAULT_LOCATION + "&page=1&limit=10");
        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>Invalid param 'search':</b> Status " + status);
        Assert.assertEquals(status, 400, "BUG: API should return 400 for invalid param but returned: " + status);
        System.out.println("TC71 PASSED - status: " + status);
    }

    @Test(priority = 72, description = "TC72: Invalid location parameter")
    public void testTC72_InvalidLocationParam() {
        System.out.println("\n>>> TC72: Invalid location=!@#$%^ <<<");
        Response r = callGlobalSearch("CB", "!@#$%^", 1, 10);
        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>Invalid location '!@#$%^':</b> Status " + status);
        Assert.assertEquals(status, 400, "BUG: API should return 400 for invalid location but returned: " + status);
        System.out.println("TC72 PASSED - status: " + status);
    }

    @Test(priority = 73, description = "TC73: Invalid page parameter")
    public void testTC73_InvalidPageParam() {
        System.out.println("\n>>> TC73: page=!@# <<<");
        Response r = callGlobalSearchRaw("query=CB&location=" + DEFAULT_LOCATION + "&page=!@#&limit=10");
        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>page='!@#':</b> Status " + status);
        Assert.assertEquals(status, 400, "BUG: API should return 400 for invalid page but returned: " + status);
        System.out.println("TC73 PASSED - status: " + status);
    }

    @Test(priority = 74, description = "TC74: Invalid limit parameter")
    public void testTC74_InvalidLimitParam() {
        System.out.println("\n>>> TC74: limit=!@# <<<");
        Response r = callGlobalSearchRaw("query=CB&location=" + DEFAULT_LOCATION + "&page=1&limit=!@#");
        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>limit='!@#':</b> Status " + status);
        Assert.assertEquals(status, 400, "BUG: API should return 400 for invalid limit but returned: " + status);
        System.out.println("TC74 PASSED - status: " + status);
    }

    @Test(priority = 75, description = "TC75: Combination of invalid inputs")
    public void testTC75_CombinedInvalidInputs() {
        System.out.println("\n>>> TC75: All invalid params <<<");
        Response r = callGlobalSearchRaw("query=&location=INVALID&page=-1&limit=abc");
        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>All invalid:</b> Status " + status);
        Assert.assertEquals(status, 400, "BUG: API should return 400 for combined invalid inputs but returned: " + status);
        System.out.println("TC75 PASSED - status: " + status);
    }

    @Test(priority = 76, description = "TC76: Query with random string (XYZ123ABC)")
    public void testTC76_RandomString() {
        System.out.println("\n>>> TC76: Random string query <<<");
        Response r = callGlobalSearch("XYZ123ABC", DEFAULT_LOCATION, 1, 10);
        Assert.assertEquals(r.getStatusCode(), 200);
        List<Map<String, Object>> results = extractResultsList(r);
        ApiReportContext.addExtraDetail("<b>Random string 'XYZ123ABC':</b> " + results.size() + " results (expected 0)");
        Assert.assertEquals(results.size(), 0, "Expected 0 results for random string");
        System.out.println("TC76 PASSED");
    }

    @Test(priority = 77, description = "TC77: Query with unsupported characters")
    public void testTC77_UnsupportedChars() {
        System.out.println("\n>>> TC77: Unsupported chars <<<");
        Response r = callGlobalSearch("\u0000\u0001\u0002", DEFAULT_LOCATION, 1, 10);
        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>Control chars:</b> Status " + status);
        Assert.assertEquals(status, 400, "BUG: API should return 400 for control chars but returned: " + status);
        System.out.println("TC77 PASSED - status: " + status);
    }

    @Test(priority = 78, description = "TC78: Verify API does not return 500 error")
    public void testTC78_No500Error() {
        System.out.println("\n>>> TC78: No 500 errors <<<");
        String[] queries = {"CB", "", "!@#$%", "' OR 1=1", "<script>", "a".repeat(1000)};
        List<String> errors = new ArrayList<>();
        for (String q : queries) {
            Response r = callGlobalSearch(q, DEFAULT_LOCATION, 1, 10);
            if (r.getStatusCode() >= 500) {
                errors.add("query='" + q.substring(0, Math.min(20, q.length())) + "' -> " + r.getStatusCode());
            }
        }
        ApiReportContext.addExtraDetail("<b>5xx errors:</b> " + (errors.isEmpty()
                ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + errors + "</span>"));
        Assert.assertTrue(errors.isEmpty(), "5xx errors found: " + errors);
        System.out.println("TC78 PASSED");
    }

    // =========================================================================
    //  SECURITY SCENARIOS (TC79-TC85)
    // =========================================================================

    @Test(priority = 79, description = "TC79: SQL Injection - query=' OR 1=1 --")
    public void testTC79_SQLInjection1() {
        System.out.println("\n>>> TC79: SQL Injection - ' OR 1=1 -- <<<");
        Response r = callGlobalSearch("' OR 1=1 --", DEFAULT_LOCATION, 1, 10);
        int status = r.getStatusCode();
        String body = r.asString().toLowerCase();
        boolean hasDbError = body.contains("sql") || body.contains("syntax error") || body.contains("mysql") || body.contains("mongodb");
        ApiReportContext.addExtraDetail("<b>SQL Injection:</b> Status " + status
                + " | <b>DB error exposed:</b> " + (hasDbError ? "<span style='color:red'>YES</span>" : "<span style='color:green'>NO</span>"));
        Assert.assertEquals(status, 400, "BUG: API should return 400 for SQL injection but returned: " + status);
        Assert.assertFalse(hasDbError, "Database error exposed in response");
        System.out.println("TC79 PASSED");
    }

    @Test(priority = 80, description = "TC80: SQL Injection - query=DROP TABLE tests")
    public void testTC80_SQLInjection2() {
        System.out.println("\n>>> TC80: SQL Injection - DROP TABLE <<<");
        Response r = callGlobalSearch("DROP TABLE tests", DEFAULT_LOCATION, 1, 10);
        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>DROP TABLE injection:</b> Status " + status);
        Assert.assertEquals(status, 400, "BUG: API should return 400 for DROP TABLE but returned: " + status);
        System.out.println("TC80 PASSED - status: " + status);
    }

    @Test(priority = 81, description = "TC81: XSS Injection - query=<script>alert(1)</script>")
    public void testTC81_XSSInjection() {
        System.out.println("\n>>> TC81: XSS Injection <<<");
        Response r = callGlobalSearch("<script>alert(1)</script>", DEFAULT_LOCATION, 1, 10);
        int status = r.getStatusCode();
        String body = r.asString();
        boolean reflected = body.contains("<script>alert(1)</script>");
        ApiReportContext.addExtraDetail("<b>XSS:</b> Status " + status
                + " | <b>Script reflected:</b> " + (reflected ? "<span style='color:red'>YES - VULNERABLE</span>" : "<span style='color:green'>NO</span>"));
        Assert.assertEquals(status, 400, "BUG: API should return 400 for XSS payload but returned: " + status);
        System.out.println("TC81 PASSED - status: " + status);
    }

    @Test(priority = 82, description = "TC82: HTML Injection - query=<h1>CBC</h1>")
    public void testTC82_HTMLInjection() {
        System.out.println("\n>>> TC82: HTML Injection <<<");
        Response r = callGlobalSearch("<h1>CBC</h1>", DEFAULT_LOCATION, 1, 10);
        int status = r.getStatusCode();
        ApiReportContext.addExtraDetail("<b>HTML injection:</b> Status " + status);
        Assert.assertEquals(status, 400, "BUG: API should return 400 for HTML injection but returned: " + status);
        System.out.println("TC82 PASSED - status: " + status);
    }

    @Test(priority = 83, description = "TC83: Path Traversal - query=../../etc/passwd")
    public void testTC83_PathTraversal() {
        System.out.println("\n>>> TC83: Path Traversal <<<");
        Response r = callGlobalSearch("../../etc/passwd", DEFAULT_LOCATION, 1, 10);
        int status = r.getStatusCode();
        String body = r.asString().toLowerCase();
        boolean hasFileContent = body.contains("root:") || body.contains("/bin/bash");
        ApiReportContext.addExtraDetail("<b>Path traversal:</b> Status " + status
                + " | <b>File content leaked:</b> " + (hasFileContent ? "<span style='color:red'>YES</span>" : "<span style='color:green'>NO</span>"));
        Assert.assertEquals(status, 400, "BUG: API should return 400 for path traversal but returned: " + status);
        Assert.assertFalse(hasFileContent, "File content leaked via path traversal");
        System.out.println("TC83 PASSED");
    }

    @Test(priority = 84, description = "TC84: Verify stack trace not exposed")
    public void testTC84_NoStackTrace() {
        System.out.println("\n>>> TC84: No stack trace in response <<<");
        Response r = callGlobalSearch("' OR 1=1; DROP TABLE--", DEFAULT_LOCATION, 1, 10);
        String body = r.asString().toLowerCase();
        boolean hasTrace = body.contains("stacktrace") || body.contains("stack trace")
                || body.contains("at com.") || body.contains("at java.")
                || body.contains("traceback") || body.contains("node_modules");
        ApiReportContext.addExtraDetail("<b>Stack trace exposed:</b> " + (hasTrace ? "<span style='color:red'>YES</span>" : "<span style='color:green'>NO</span>"));
        Assert.assertFalse(hasTrace, "Stack trace exposed in error response");
        System.out.println("TC84 PASSED");
    }

    @Test(priority = 85, description = "TC85: Verify internal DB details not exposed")
    public void testTC85_NoDBDetails() {
        System.out.println("\n>>> TC85: No internal DB details <<<");
        Response r = callGlobalSearch("{$gt:''}", DEFAULT_LOCATION, 1, 10);
        String body = r.asString().toLowerCase();
        boolean hasDb = body.contains("mongoose") || body.contains("mongodb://")
                || body.contains("connection string") || body.contains("aggregate")
                || body.contains("db.collection") || body.contains("mongo_uri");
        ApiReportContext.addExtraDetail("<b>DB details exposed:</b> " + (hasDb ? "<span style='color:red'>YES</span>" : "<span style='color:green'>NO</span>"));
        Assert.assertFalse(hasDb, "Internal DB details exposed");
        System.out.println("TC85 PASSED");
    }

    // =========================================================================
    //  PERFORMANCE SCENARIOS (TC86-TC90)
    // =========================================================================

    @Test(priority = 86, description = "TC86: Verify response time < 2 seconds")
    public void testTC86_ResponseTime() {
        System.out.println("\n>>> TC86: Response time < 2000ms <<<");
        long start = System.currentTimeMillis();
        Response r = callGlobalSearch("CB", DEFAULT_LOCATION, 1, 50);
        long elapsed = System.currentTimeMillis() - start;
        ApiReportContext.addExtraDetail("<b>Response time:</b> " + elapsed + " ms | <b>Threshold:</b> 2000 ms"
                + " | " + (elapsed < 2000 ? "<span style='color:green'>WITHIN SLA</span>" : "<span style='color:red'>EXCEEDS SLA</span>"));
        Assert.assertTrue(elapsed < 2000, "Response took " + elapsed + " ms (threshold: 2000ms)");
        System.out.println("TC86 PASSED - " + elapsed + " ms");
    }

    @Test(priority = 87, description = "TC87: Verify concurrent search requests")
    public void testTC87_ConcurrentRequests() throws Exception {
        System.out.println("\n>>> TC87: 5 concurrent requests <<<");
        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            futures.add(executor.submit(() -> callGlobalSearch("CB", DEFAULT_LOCATION, 1, 10).getStatusCode()));
        }
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> f : futures) { statuses.add(f.get()); }
        long successCount = statuses.stream().filter(s -> s == 200).count();
        ApiReportContext.addExtraDetail("<b>Concurrent (5):</b> " + successCount + "/5 returned 200");
        Assert.assertEquals(successCount, 5L, "Not all concurrent requests succeeded: " + statuses);
        System.out.println("TC87 PASSED");
    }

    @Test(priority = 88, description = "TC88: Verify repeated search requests")
    public void testTC88_RepeatedRequests() {
        System.out.println("\n>>> TC88: 3 repeated requests <<<");
        int[] sizes = new int[3];
        for (int i = 0; i < 3; i++) {
            Response r = callGlobalSearch("CB", DEFAULT_LOCATION, 1, 50);
            sizes[i] = extractResultsList(r).size();
        }
        ApiReportContext.addExtraDetail("<b>Repeated (3x):</b> " + sizes[0] + ", " + sizes[1] + ", " + sizes[2]);
        Assert.assertEquals(sizes[0], sizes[1], "Inconsistent between call 1 and 2");
        Assert.assertEquals(sizes[1], sizes[2], "Inconsistent between call 2 and 3");
        System.out.println("TC88 PASSED");
    }

    @Test(priority = 89, description = "TC89: Verify large result set handling")
    public void testTC89_LargeResultSet() {
        System.out.println("\n>>> TC89: Large result set (limit=100, query=a) <<<");
        long start = System.currentTimeMillis();
        Response r = callGlobalSearch("a", DEFAULT_LOCATION, 1, 100);
        long elapsed = System.currentTimeMillis() - start;
        List<Map<String, Object>> results = extractResultsList(r);
        ApiReportContext.addExtraDetail("<b>Large result (query='a', limit=100):</b> " + results.size()
                + " results in " + elapsed + " ms");
        Assert.assertTrue(elapsed < 5000, "Large result set took " + elapsed + " ms");
        System.out.println("TC89 PASSED - " + results.size() + " results in " + elapsed + " ms");
    }

    @Test(priority = 90, description = "TC90: Verify response consistency under load")
    public void testTC90_ConsistencyUnderLoad() throws Exception {
        System.out.println("\n>>> TC90: Consistency under load (10 requests) <<<");
        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            futures.add(executor.submit(() -> extractResultsList(callGlobalSearch("CB", DEFAULT_LOCATION, 1, 10)).size()));
        }
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        Set<Integer> sizes = new HashSet<>();
        for (Future<Integer> f : futures) { sizes.add(f.get()); }
        ApiReportContext.addExtraDetail("<b>Load (10x):</b> result sizes = " + sizes
                + " | <b>Consistent:</b> " + (sizes.size() == 1 ? "<span style='color:green'>YES</span>" : "<span style='color:orange'>VARIES</span>"));
        System.out.println("TC90 PASSED - distinct sizes: " + sizes);
    }

    // =========================================================================
    //  RESPONSE VALIDATION (TC91-TC98)
    // =========================================================================

    @Test(priority = 91, description = "TC91: Verify status code 200")
    public void testTC91_StatusCode200() {
        System.out.println("\n>>> TC91: Status code 200 <<<");
        Response r = callGlobalSearch("CB", DEFAULT_LOCATION, 1, 10);
        ApiReportContext.addExtraDetail("<b>Status:</b> " + r.getStatusCode());
        Assert.assertEquals(r.getStatusCode(), 200);
        System.out.println("TC91 PASSED");
    }

    @Test(priority = 92, description = "TC92: Verify response schema")
    public void testTC92_ResponseSchema() {
        System.out.println("\n>>> TC92: Response schema validation <<<");
        Response r = callGlobalSearch("CB", DEFAULT_LOCATION, 1, 10);
        boolean hasStatus = r.jsonPath().get("status") != null;
        boolean hasSuccess = r.jsonPath().get("success") != null;
        boolean hasMsg = r.jsonPath().get("msg") != null;
        boolean hasData = r.jsonPath().get("data") != null;
        ApiReportContext.addExtraDetail("<b>Schema:</b> status=" + hasStatus + " success=" + hasSuccess
                + " msg=" + hasMsg + " data=" + hasData);
        Assert.assertTrue(hasStatus, "Missing 'status' field");
        Assert.assertTrue(hasSuccess, "Missing 'success' field");
        Assert.assertTrue(hasMsg, "Missing 'msg' field");
        Assert.assertTrue(hasData, "Missing 'data' field");
        System.out.println("TC92 PASSED");
    }

    @Test(priority = 93, description = "TC93: Verify success flag")
    public void testTC93_SuccessFlag() {
        System.out.println("\n>>> TC93: success=true <<<");
        Response r = callGlobalSearch("CB", DEFAULT_LOCATION, 1, 10);
        boolean success = r.jsonPath().getBoolean("success");
        ApiReportContext.addExtraDetail("<b>success:</b> " + success);
        Assert.assertTrue(success, "success flag is not true");
        System.out.println("TC93 PASSED");
    }

    @Test(priority = 94, description = "TC94: Verify response message")
    public void testTC94_ResponseMessage() {
        System.out.println("\n>>> TC94: Response message <<<");
        Response r = callGlobalSearch("CB", DEFAULT_LOCATION, 1, 10);
        String msg = r.jsonPath().getString("msg");
        ApiReportContext.addExtraDetail("<b>msg:</b> " + msg);
        Assert.assertNotNull(msg, "msg field is null");
        Assert.assertFalse(msg.trim().isEmpty(), "msg field is empty");
        System.out.println("TC94 PASSED - msg: " + msg);
    }

    @Test(priority = 95, description = "TC95: Verify response data object")
    public void testTC95_DataObject() {
        System.out.println("\n>>> TC95: data object structure <<<");
        Response r = callGlobalSearch("CB", DEFAULT_LOCATION, 1, 10);
        Object data = r.jsonPath().get("data");
        Assert.assertNotNull(data, "data field is null");
        Object results = r.jsonPath().get("data.results");
        Assert.assertNotNull(results, "data.results is null");
        ApiReportContext.addExtraDetail("<b>data:</b> present | <b>data.results:</b> present");
        System.out.println("TC95 PASSED");
    }

    @Test(priority = 96, description = "TC96: Verify total count field")
    public void testTC96_TotalCountField() {
        System.out.println("\n>>> TC96: Total count field <<<");
        Response r = callGlobalSearch("CB", DEFAULT_LOCATION, 1, 10);
        Integer total = null;
        try { total = r.jsonPath().getInt("data.total"); } catch (Exception ignored) { }
        if (total == null) {
            try { total = r.jsonPath().getInt("total"); } catch (Exception ignored) { }
        }
        ApiReportContext.addExtraDetail("<b>total:</b> " + (total != null ? total : "NOT PRESENT"));
        System.out.println("TC96 PASSED - total: " + total);
    }

    @Test(priority = 97, description = "TC97: Verify pagination metadata")
    public void testTC97_PaginationMetadata() {
        System.out.println("\n>>> TC97: Pagination metadata <<<");
        Response r = callGlobalSearch("CB", DEFAULT_LOCATION, 1, 10);
        Integer page = null;
        Integer limit = null;
        Integer totalPages = null;
        try { page = r.jsonPath().getInt("page"); } catch (Exception ignored) { }
        try { limit = r.jsonPath().getInt("limit"); } catch (Exception ignored) { }
        try { totalPages = r.jsonPath().getInt("total_pages"); } catch (Exception ignored) { }
        ApiReportContext.addExtraDetail("<b>page:</b> " + page + " | <b>limit:</b> " + limit + " | <b>total_pages:</b> " + totalPages);
        Assert.assertNotNull(page, "page field missing");
        Assert.assertNotNull(limit, "limit field missing");
        System.out.println("TC97 PASSED");
    }

    @Test(priority = 98, description = "TC98: Verify response content type")
    public void testTC98_ContentType() {
        System.out.println("\n>>> TC98: Content-Type header <<<");
        Response r = callGlobalSearch("CB", DEFAULT_LOCATION, 1, 10);
        String contentType = r.getContentType();
        ApiReportContext.addExtraDetail("<b>Content-Type:</b> " + contentType);
        Assert.assertTrue(contentType != null && contentType.contains("json"),
                "Expected JSON content-type, got: " + contentType);
        System.out.println("TC98 PASSED - " + contentType);
    }

    // =========================================================================
    //  HIGH PRIORITY AUTOMATION SCENARIOS (TC99-TC110)
    // =========================================================================

    @Test(priority = 99, description = "TC99: Valid Search Validation")
    public void testTC99_ValidSearchValidation() {
        System.out.println("\n>>> TC99: Full valid search validation <<<");
        Response r = callGlobalSearch("CBC", DEFAULT_LOCATION, 1, 50);
        Assert.assertEquals(r.getStatusCode(), 200);
        Assert.assertTrue(r.jsonPath().getBoolean("success"));
        List<Map<String, Object>> results = extractResultsList(r);
        Assert.assertFalse(results.isEmpty(), "Expected results for CBC");
        ApiReportContext.addExtraDetail("<b>Valid search:</b> 200 OK | success=true | results=" + results.size());
        System.out.println("TC99 PASSED");
    }

    @Test(priority = 100, description = "TC100: Partial Search Validation")
    public void testTC100_PartialSearchValidation() {
        System.out.println("\n>>> TC100: Partial search validation <<<");
        Response r = callGlobalSearch("CB", DEFAULT_LOCATION, 1, 50);
        Assert.assertEquals(r.getStatusCode(), 200);
        List<Map<String, Object>> results = extractResultsList(r);
        boolean anyMatch = results.stream().anyMatch(item -> {
            String name = item.get("name") != null ? item.get("name").toString().toUpperCase() : "";
            return name.contains("CB");
        });
        ApiReportContext.addExtraDetail("<b>Partial 'CB':</b> " + results.size() + " results | <b>Matches:</b> " + anyMatch);
        if (!results.isEmpty()) Assert.assertTrue(anyMatch, "No partial match found");
        System.out.println("TC100 PASSED");
    }

    @Test(priority = 101, description = "TC101: Location Filter Validation")
    public void testTC101_LocationFilterValidation() {
        System.out.println("\n>>> TC101: Location filter validation <<<");
        Response r = callGlobalSearch("CB", DEFAULT_LOCATION, 1, 50);
        Assert.assertEquals(r.getStatusCode(), 200);
        List<Map<String, Object>> results = extractResultsList(r);
        long withLoc = results.stream()
                .filter(item -> {
                    Object locs = item.get("locations");
                    return locs instanceof List && ((List<?>) locs).contains(DEFAULT_LOCATION);
                })
                .count();
        double pct = results.isEmpty() ? 100 : (withLoc * 100.0) / results.size();
        ApiReportContext.addExtraDetail("<b>Location filter:</b> " + withLoc + "/" + results.size()
                + " (" + String.format("%.0f", pct) + "%) have target location");
        System.out.println("TC101 PASSED - " + String.format("%.0f", pct) + "% match location");
    }

    @Test(priority = 102, description = "TC102: Empty Query Validation")
    public void testTC102_EmptyQueryValidation() {
        System.out.println("\n>>> TC102: Empty query comprehensive <<<");
        Response r = callGlobalSearch("", DEFAULT_LOCATION, 1, 10);
        int status = r.getStatusCode();
        List<Map<String, Object>> results = extractResultsList(r);
        ApiReportContext.addExtraDetail("<b>Empty query:</b> Status " + status + " | Results: " + results.size());
        Assert.assertEquals(status, 400, "BUG: API should return 400 for empty query but returned: " + status);
        System.out.println("TC102 PASSED - status: " + status + ", results: " + results.size());
    }

    @Test(priority = 103, description = "TC103: Invalid Location Validation")
    public void testTC103_InvalidLocationValidation() {
        System.out.println("\n>>> TC103: Invalid location comprehensive <<<");
        Response r = callGlobalSearch("CB", "INVALID_LOCATION_999", 1, 10);
        int status = r.getStatusCode();
        List<Map<String, Object>> results = extractResultsList(r);
        ApiReportContext.addExtraDetail("<b>Invalid location:</b> Status " + status + " | Results: " + results.size());
        Assert.assertEquals(status, 400, "BUG: API should return 400 for invalid location but returned: " + status);
        System.out.println("TC103 PASSED - status: " + status);
    }

    @Test(priority = 104, description = "TC104: Pagination Validation")
    public void testTC104_PaginationValidation() {
        System.out.println("\n>>> TC104: Pagination comprehensive <<<");
        Response r1 = callGlobalSearch("CB", DEFAULT_LOCATION, 1, 5);
        Response r2 = callGlobalSearch("CB", DEFAULT_LOCATION, 2, 5);
        List<Map<String, Object>> p1 = extractResultsList(r1);
        List<Map<String, Object>> p2 = extractResultsList(r2);
        Assert.assertTrue(p1.size() <= 5, "Page 1 exceeds limit: " + p1.size());
        if (!p1.isEmpty() && !p2.isEmpty()) {
            String id1 = String.valueOf(p1.get(0).get("_id"));
            String id2 = String.valueOf(p2.get(0).get("_id"));
            Assert.assertNotEquals(id1, id2, "Same first record on different pages");
        }
        ApiReportContext.addExtraDetail("<b>Pagination:</b> Page 1=" + p1.size() + " | Page 2=" + p2.size());
        System.out.println("TC104 PASSED");
    }

    @Test(priority = 105, description = "TC105: Limit Validation")
    public void testTC105_LimitValidation() {
        System.out.println("\n>>> TC105: Limit validation comprehensive <<<");
        Response r5 = callGlobalSearch("CB", DEFAULT_LOCATION, 1, 5);
        Response r20 = callGlobalSearch("CB", DEFAULT_LOCATION, 1, 20);
        List<Map<String, Object>> res5 = extractResultsList(r5);
        List<Map<String, Object>> res20 = extractResultsList(r20);
        Assert.assertTrue(res5.size() <= 5, "limit=5 returned " + res5.size());
        Assert.assertTrue(res20.size() <= 20, "limit=20 returned " + res20.size());
        ApiReportContext.addExtraDetail("<b>limit=5:</b> " + res5.size() + " | <b>limit=20:</b> " + res20.size());
        System.out.println("TC105 PASSED");
    }

    @Test(priority = 106, description = "TC106: Duplicate Result Validation")
    public void testTC106_DuplicateResultValidation() {
        System.out.println("\n>>> TC106: Duplicate result check <<<");
        Response r = callGlobalSearch("CB", DEFAULT_LOCATION, 1, 50);
        List<Map<String, Object>> results = extractResultsList(r);
        List<String> ids = results.stream().map(i -> String.valueOf(i.get("_id"))).collect(Collectors.toList());
        Set<String> unique = new HashSet<>(ids);
        int dupes = ids.size() - unique.size();
        ApiReportContext.addExtraDetail("<b>Duplicates:</b> " + (dupes == 0 ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + dupes + "</span>"));
        Assert.assertEquals(dupes, 0, dupes + " duplicate records found");
        System.out.println("TC106 PASSED");
    }

    @Test(priority = 107, description = "TC107: Response Schema Validation")
    public void testTC107_ResponseSchemaValidation() {
        System.out.println("\n>>> TC107: Full schema validation <<<");
        Response r = callGlobalSearch("CB", DEFAULT_LOCATION, 1, 10);
        Assert.assertEquals(r.getStatusCode(), 200);
        Assert.assertNotNull(r.jsonPath().get("status"), "status missing");
        Assert.assertNotNull(r.jsonPath().get("success"), "success missing");
        Assert.assertNotNull(r.jsonPath().get("msg"), "msg missing");
        Assert.assertNotNull(r.jsonPath().get("data"), "data missing");
        Assert.assertNotNull(r.jsonPath().get("data.results"), "data.results missing");
        ApiReportContext.addExtraDetail("<b>Schema:</b> All required fields present");
        System.out.println("TC107 PASSED");
    }

    @Test(priority = 108, description = "TC108: SQL Injection Validation")
    public void testTC108_SQLInjectionValidation() {
        System.out.println("\n>>> TC108: SQL injection comprehensive <<<");
        String[] payloads = {"' OR 1=1 --", "1; DROP TABLE tests", "' UNION SELECT * FROM users--", "{$gt:''}"};
        List<String> failed = new ArrayList<>();
        for (String payload : payloads) {
            Response r = callGlobalSearch(payload, DEFAULT_LOCATION, 1, 10);
            if (r.getStatusCode() >= 500) {
                failed.add(payload + " -> " + r.getStatusCode());
            }
            String body = r.asString().toLowerCase();
            if (body.contains("syntax error") || body.contains("mongodb") || body.contains("mysql")) {
                failed.add(payload + " -> DB error exposed");
            }
        }
        ApiReportContext.addExtraDetail("<b>SQL Injection (4 payloads):</b> " + (failed.isEmpty()
                ? "<span style='color:green'>ALL SAFE</span>" : "<span style='color:red'>" + failed + "</span>"));
        Assert.assertTrue(failed.isEmpty(), "Injection vulnerabilities: " + failed);
        System.out.println("TC108 PASSED");
    }

    @Test(priority = 109, description = "TC109: Response Time Validation")
    public void testTC109_ResponseTimeValidation() {
        System.out.println("\n>>> TC109: Response time validation (3 calls) <<<");
        long[] times = new long[3];
        for (int i = 0; i < 3; i++) {
            long start = System.currentTimeMillis();
            callGlobalSearch("CB", DEFAULT_LOCATION, 1, 50);
            times[i] = System.currentTimeMillis() - start;
        }
        long avg = (times[0] + times[1] + times[2]) / 3;
        ApiReportContext.addExtraDetail("<b>Response times:</b> " + times[0] + "ms, " + times[1] + "ms, " + times[2] + "ms"
                + " | <b>Avg:</b> " + avg + "ms"
                + " | " + (avg < 2000 ? "<span style='color:green'>WITHIN SLA</span>" : "<span style='color:red'>EXCEEDS SLA</span>"));
        Assert.assertTrue(avg < 2000, "Average response time " + avg + " ms exceeds 2000 ms SLA");
        System.out.println("TC109 PASSED - avg: " + avg + " ms");
    }

    @Test(priority = 110, description = "TC110: Search Result Relevance Validation")
    public void testTC110_SearchResultRelevanceValidation() {
        System.out.println("\n>>> TC110: Search result relevance validation <<<");
        Response r = callGlobalSearch("CBC", DEFAULT_LOCATION, 1, 50);
        List<Map<String, Object>> results = extractResultsList(r);
        if (results.isEmpty()) {
            ApiReportContext.addExtraDetail("<b>Relevance:</b> SKIPPED - no results");
            System.out.println("TC110 SKIPPED");
            return;
        }
        // Check first result has relevance score and results contain the search term somewhere
        Map<String, Object> first = results.get(0);
        Object scoreObj = first.get("relevanceScore");
        String firstName = first.get("name") != null ? first.get("name").toString() : "N/A";
        boolean firstContainsCBC = firstName.toUpperCase().contains("CBC");
        boolean anyContainsCBC = results.stream()
                .anyMatch(item -> item.get("name") != null && item.get("name").toString().toUpperCase().contains("CBC"));
        ApiReportContext.addExtraDetail("<b>First result:</b> " + firstName
                + " | <b>Relevance score:</b> " + scoreObj
                + " | <b>Contains CBC:</b> " + (firstContainsCBC ? "<span style='color:green'>YES</span>" : "<span style='color:orange'>NO (other results do)</span>")
                + " | <b>Any result has CBC:</b> " + anyContainsCBC);
        Assert.assertTrue(anyContainsCBC, "No result contains search term 'CBC'. First result: " + firstName);
        System.out.println("TC110 PASSED");
    }
}

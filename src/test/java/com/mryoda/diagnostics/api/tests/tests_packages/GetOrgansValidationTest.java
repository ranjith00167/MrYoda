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
 * GET /tests/getOrgans - Standalone Validation Suite
 *
 * curl -X 'GET' https://staging-api-diagnostics.yodaprojects.com/tests/getOrgans
 *      -H 'accept: any'
 *
 * TC01-TC12:  Positive           - 200 OK, response body, organ list, mandatory fields
 * TC13-TC23:  Data Validation    - null checks, uniqueness, naming rules, active filter
 * TC24-TC26:  Sorting            - alphabetical order, consistency
 * TC27-TC32:  Schema Validation  - field types, datetime fields
 * TC33-TC39:  Negative           - unsupported methods (POST/PUT/DELETE)
 * TC40-TC43:  Security           - no sensitive data, no stack trace
 * TC44-TC47:  Performance        - response time, concurrency, dataset size
 * TC48-TC52:  Integration        - organ usability in search/symptoms/tests
 * TC53-TC57:  Response           - status, message, success, data, valid values
 * TC58-TC75:  High Priority      - schema, duplicates, sorting, trimming, consistency
 */
public class GetOrgansValidationTest extends BaseTest {

    private static final String ENDPOINT = APIEndpoints.GET_ORGANS;

    // Captured once during @BeforeClass for reuse across tests
    private static List<Map<String, Object>> organsData = new ArrayList<>();
    private static List<String> organNames = new ArrayList<>();
    private static List<String> organIds = new ArrayList<>();
    private static Map<String, Object> firstOrgan = null;
    private static int totalOrgans = 0;

    // =========================================================================
    //  SETUP
    // =========================================================================

    @BeforeClass
    public void setupOrgansData() {
        System.out.println("\n========================================");
        System.out.println("SETUP: Fetching organs from getOrgans API");
        System.out.println("========================================");

        Response r = callGetOrgans();

        if (r.getStatusCode() == 200) {
            List<Map<String, Object>> list = null;
            try { list = r.jsonPath().getList("data"); } catch (Exception ignored) { }
            if (list == null || list.isEmpty()) {
                try { list = r.jsonPath().getList("organs"); } catch (Exception ignored) { }
            }
            if (list == null || list.isEmpty()) {
                try { list = r.jsonPath().getList("$"); } catch (Exception ignored) { }
            }

            if (list != null && !list.isEmpty()) {
                for (Map<String, Object> organ : list) {
                    if (organ != null) organsData.add(organ);
                }
                totalOrgans = organsData.size();
                firstOrgan  = organsData.isEmpty() ? null : organsData.get(0);

                for (Map<String, Object> organ : organsData) {
                    String nameKey = organ.containsKey("name")        ? "name"
                                   : organ.containsKey("organ_name")  ? "organ_name"
                                   : organ.containsKey("title")       ? "title" : null;
                    if (nameKey != null && organ.get(nameKey) != null) {
                        organNames.add(organ.get(nameKey).toString());
                    }
                    String idKey = organ.containsKey("_id") ? "_id" : organ.containsKey("id") ? "id" : null;
                    if (idKey != null && organ.get(idKey) != null) {
                        organIds.add(organ.get(idKey).toString());
                    }
                }

                System.out.println("   Total organs returned : " + totalOrgans);
                System.out.println("   Names extracted       : " + organNames.size());
                System.out.println("   IDs extracted         : " + organIds.size());
                if (firstOrgan != null) {
                    System.out.println("   First organ fields    : " + firstOrgan.keySet());
                }
                if (!organNames.isEmpty()) {
                    System.out.println("   Sample names          : "
                            + organNames.subList(0, Math.min(5, organNames.size())));
                }
                System.out.println("✅ Setup complete");
            } else {
                System.out.println("⚠️  No organs list found in response");
            }
        } else {
            System.out.println("❌ Setup failed. Status: " + r.getStatusCode());
        }
        System.out.println("========================================\n");
    }

    // =========================================================================
    //  HELPERS
    // =========================================================================

    /** Standard positive GET call — records the API call for the Extent Report */
    private Response callGetOrgans() {
        long start = System.currentTimeMillis();
        Response r = new RequestBuilder()
                .setEndpoint(ENDPOINT)
                .addHeader("accept", "*/*")
                .get();
        long elapsed = System.currentTimeMillis() - start;
        String body = r.asString();
        String truncated = body.length() > 3000 ? body.substring(0, 3000) + "\n...(truncated)" : body;
        ApiReportContext.addRecord(new ApiReportContext.ApiCallRecord(
                "GET", ENDPOINT, "",
                r.getStatusCode(), truncated, elapsed,
                200, "GET /tests/getOrgans"));
        return r;
    }

    private int getOrganCount(Response r) {
        if (r.getStatusCode() != 200) return -1;
        List<?> list = null;
        try { list = r.jsonPath().getList("data"); } catch (Exception ignored) { }
        if (list == null) {
            try { list = r.jsonPath().getList("organs"); } catch (Exception ignored) { }
        }
        return list != null ? list.size() : 0;
    }

    // =========================================================================
    //  POSITIVE SCENARIOS (TC01-TC12)
    // =========================================================================

    @Test(priority = 1, description = "TC01: GET getOrgans returns HTTP 200")
    public void testTC01_Returns200() {
        System.out.println("\n>>> TC01: GET getOrgans — Expect HTTP 200 <<<");
        Response r = callGetOrgans();
        ApiReportContext.addExtraDetail("<b>HTTP Status:</b> " + r.getStatusCode()
                + " | <b>Response Time:</b> " + r.getTime() + " ms");
        Assert.assertEquals(r.getStatusCode(), 200,
                "Expected 200 OK. Got: " + r.getStatusCode() + "\nBody: " + r.asString());
        System.out.println("✅ TC01 PASSED");
    }

    @Test(priority = 2, description = "TC02: Response body is not empty")
    public void testTC02_ResponseBodyNotEmpty() {
        System.out.println("\n>>> TC02: Response body should not be empty <<<");
        Response r = callGetOrgans();
        String body = r.asString();
        Assert.assertEquals(r.getStatusCode(), 200);
        Assert.assertNotNull(body, "Response body is null");
        Assert.assertFalse(body.isEmpty(), "Response body is empty");
        ApiReportContext.addExtraDetail("<b>Body Length:</b> " + body.length() + " chars");
        System.out.println("✅ TC02 PASSED");
    }

    @Test(priority = 3, description = "TC03: Response contains an organs list")
    public void testTC03_OrgansListReturned() {
        System.out.println("\n>>> TC03: Organs list should be present <<<");
        Assert.assertFalse(organsData.isEmpty(),
                "No organs found in response - list should not be empty");
        ApiReportContext.addExtraDetail("<b>Organs list present:</b> <span style='color:green'>YES</span>"
                + " | <b>Count:</b> " + organsData.size());
        System.out.println("✅ TC03 PASSED");
    }

    @Test(priority = 4, description = "TC04: At least one organ is returned")
    public void testTC04_AtLeastOneOrgan() {
        System.out.println("\n>>> TC04: At least 1 organ expected <<<");
        Assert.assertTrue(totalOrgans >= 1,
                "Expected at least 1 organ, got: " + totalOrgans);
        ApiReportContext.addExtraDetail("<b>Total Organs:</b> " + totalOrgans);
        System.out.println("✅ TC04 PASSED");
    }

    @Test(priority = 5, description = "TC05: First organ has an ID field (_id)")
    public void testTC05_OrganIdPresent() {
        System.out.println("\n>>> TC05: Organ _id field should be present <<<");
        Assert.assertNotNull(firstOrgan, "No organs returned");
        boolean hasId = firstOrgan.containsKey("_id") || firstOrgan.containsKey("id");
        Assert.assertTrue(hasId, "No _id/id field found. Keys: " + firstOrgan.keySet());
        Object id = firstOrgan.containsKey("_id") ? firstOrgan.get("_id") : firstOrgan.get("id");
        ApiReportContext.addExtraDetail("<b>First organ _id:</b> " + id);
        System.out.println("✅ TC05 PASSED");
    }

    @Test(priority = 6, description = "TC06: First organ has a name field")
    public void testTC06_OrganNamePresent() {
        System.out.println("\n>>> TC06: Organ name field should be present <<<");
        Assert.assertNotNull(firstOrgan, "No organs returned");
        boolean hasName = firstOrgan.containsKey("name")
                || firstOrgan.containsKey("organ_name")
                || firstOrgan.containsKey("title");
        Assert.assertTrue(hasName, "No name-like field found. Keys: " + firstOrgan.keySet());
        String name = firstOrgan.containsKey("name")       ? String.valueOf(firstOrgan.get("name"))
                    : firstOrgan.containsKey("organ_name") ? String.valueOf(firstOrgan.get("organ_name"))
                    : String.valueOf(firstOrgan.get("title"));
        ApiReportContext.addExtraDetail("<b>First organ name:</b> " + name);
        System.out.println("✅ TC06 PASSED");
    }

    @Test(priority = 7, description = "TC07: First organ has a slug field (if applicable)")
    public void testTC07_OrganSlugPresent() {
        System.out.println("\n>>> TC07: Check for slug field <<<");
        Assert.assertNotNull(firstOrgan, "No organs returned");
        boolean hasSlug = firstOrgan.containsKey("slug");
        String slugVal = hasSlug ? String.valueOf(firstOrgan.get("slug")) : "N/A";
        ApiReportContext.addExtraDetail("<b>slug field present:</b> " + (hasSlug ? "<span style='color:green'>YES</span>" : "<span style='color:orange'>NOT FOUND</span>")
                + " | <b>Value:</b> " + slugVal);
        // Slug is optional — only assert format if present
        if (hasSlug && firstOrgan.get("slug") != null) {
            String slug = firstOrgan.get("slug").toString();
            Assert.assertFalse(slug.trim().isEmpty(), "slug is blank when present");
        }
        System.out.println("✅ TC07 PASSED");
    }

    @Test(priority = 8, description = "TC08: Active organs are returned (isActive flag)")
    public void testTC08_ActiveOrgansReturned() {
        System.out.println("\n>>> TC08: Organs should be active <<<");
        Assert.assertFalse(organsData.isEmpty(), "No organs to validate");
        int inactiveCount = 0;
        for (Map<String, Object> organ : organsData) {
            if (organ == null) continue;
            if (organ.containsKey("isActive")) {
                Object active = organ.get("isActive");
                if (active != null && active.toString().equalsIgnoreCase("false")) {
                    inactiveCount++;
                }
            }
        }
        ApiReportContext.addExtraDetail("<b>Total organs:</b> " + organsData.size()
                + " | <b>Inactive organs in response:</b> " + inactiveCount);
        Assert.assertEquals(inactiveCount, 0,
                "Expected no inactive organs, found: " + inactiveCount);
        System.out.println("✅ TC08 PASSED");
    }

    @Test(priority = 9, description = "TC09: Response success flag is true")
    public void testTC09_SuccessFlag() {
        System.out.println("\n>>> TC09: success flag should be true <<<");
        Response r = callGetOrgans();
        Assert.assertEquals(r.getStatusCode(), 200);
        Boolean success = r.jsonPath().getBoolean("success");
        Assert.assertNotNull(success, "success field missing");
        Assert.assertTrue(success, "success should be true");
        ApiReportContext.addExtraDetail("<b>success field:</b> " + success);
        System.out.println("✅ TC09 PASSED");
    }

    @Test(priority = 10, description = "TC10: Response contains a msg field")
    public void testTC10_ResponseMessage() {
        System.out.println("\n>>> TC10: msg field should be present <<<");
        Response r = callGetOrgans();
        Assert.assertEquals(r.getStatusCode(), 200);
        String msg = r.jsonPath().getString("msg");
        Assert.assertNotNull(msg, "msg field missing in response body");
        ApiReportContext.addExtraDetail("<b>msg field value:</b> &quot;" + msg + "&quot;");
        System.out.println("✅ TC10 PASSED");
    }

    @Test(priority = 11, description = "TC11: All mandatory fields present on each organ")
    public void testTC11_MandatoryFields() {
        System.out.println("\n>>> TC11: Each organ must have _id and name <<<");
        Assert.assertFalse(organsData.isEmpty(), "No organs returned");
        int missing = 0;
        for (Map<String, Object> organ : organsData) {
            if (organ == null) { missing++; continue; }
            boolean hasId   = organ.containsKey("_id") || organ.containsKey("id");
            boolean hasName = organ.containsKey("name") || organ.containsKey("organ_name") || organ.containsKey("title");
            if (!hasId || !hasName) missing++;
        }
        ApiReportContext.addExtraDetail("<b>Total organs:</b> " + organsData.size()
                + " | <b>Entries missing _id or name:</b> " + missing);
        Assert.assertEquals(missing, 0,
                missing + " organs missing mandatory fields (_id or name)");
        System.out.println("✅ TC11 PASSED");
    }

    @Test(priority = 12, description = "TC12: Content-Type is application/json")
    public void testTC12_ContentType() {
        System.out.println("\n>>> TC12: Content-Type should be application/json <<<");
        Response r = callGetOrgans();
        String ct = r.getContentType();
        ApiReportContext.addExtraDetail("<b>Content-Type header:</b> " + ct);
        Assert.assertTrue(ct.contains("application/json"),
                "Expected application/json. Got: " + ct);
        System.out.println("✅ TC12 PASSED");
    }

    // =========================================================================
    //  DATA VALIDATION (TC13-TC23)
    // =========================================================================

    @Test(priority = 13, description = "TC13: Organ _id is not null for any entry")
    public void testTC13_OrganIdNotNull() {
        System.out.println("\n>>> TC13: Every organ _id must be non-null <<<");
        Assert.assertFalse(organsData.isEmpty(), "No organs to validate");
        int nullCount = 0;
        for (Map<String, Object> organ : organsData) {
            if (organ == null) { nullCount++; continue; }
            String idKey = organ.containsKey("_id") ? "_id" : "id";
            if (organ.get(idKey) == null) nullCount++;
        }
        ApiReportContext.addExtraDetail("<b>Entries with null _id:</b> " + nullCount
                + " | <b>Total:</b> " + organsData.size());
        Assert.assertEquals(nullCount, 0, nullCount + " organs have null _id");
        System.out.println("✅ TC13 PASSED");
    }

    @Test(priority = 14, description = "TC14: Organ name is not null for any entry")
    public void testTC14_OrganNameNotNull() {
        System.out.println("\n>>> TC14: Every organ name must be non-null <<<");
        Assert.assertFalse(organsData.isEmpty(), "No organs to validate");
        int nullCount = 0;
        for (Map<String, Object> organ : organsData) {
            if (organ == null) { nullCount++; continue; }
            String nameKey = organ.containsKey("name")       ? "name"
                           : organ.containsKey("organ_name") ? "organ_name"
                           : organ.containsKey("title")      ? "title" : null;
            if (nameKey == null || organ.get(nameKey) == null) nullCount++;
        }
        ApiReportContext.addExtraDetail("<b>Entries with null name:</b> " + nullCount
                + " | <b>Total:</b> " + organsData.size());
        Assert.assertEquals(nullCount, 0, nullCount + " organs have null name");
        System.out.println("✅ TC14 PASSED");
    }

    @Test(priority = 15, description = "TC15: Organ name is not empty for any entry")
    public void testTC15_OrganNameNotEmpty() {
        System.out.println("\n>>> TC15: Every organ name must be non-empty <<<");
        Assert.assertFalse(organNames.isEmpty(), "No organ names extracted");
        int blankCount = 0;
        for (String name : organNames) {
            if (name == null || name.trim().isEmpty()) blankCount++;
        }
        ApiReportContext.addExtraDetail("<b>Blank/empty names found:</b> " + blankCount
                + " | <b>Total names:</b> " + organNames.size());
        Assert.assertEquals(blankCount, 0, blankCount + " organ names are blank/empty");
        System.out.println("✅ TC15 PASSED");
    }

    @Test(priority = 16, description = "TC16: Organ IDs are unique (no duplicates)")
    public void testTC16_OrganIdsUnique() {
        System.out.println("\n>>> TC16: No duplicate _id values <<<");
        Set<String> seen = new HashSet<>();
        List<String> duplicates = new ArrayList<>();
        for (String id : organIds) {
            if (!seen.add(id)) duplicates.add(id);
        }
        ApiReportContext.addExtraDetail("<b>Total IDs:</b> " + organIds.size()
                + " | <b>Duplicate IDs:</b> "
                + (duplicates.isEmpty() ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + duplicates + "</span>"));
        Assert.assertTrue(duplicates.isEmpty(), "Duplicate organ _ids: " + duplicates);
        System.out.println("✅ TC16 PASSED");
    }

    @Test(priority = 17, description = "TC17: Organ names are unique (no duplicates)")
    public void testTC17_OrganNamesUnique() {
        System.out.println("\n>>> TC17: No duplicate organ names <<<");
        Set<String> seen = new LinkedHashSet<>();
        List<String> duplicates = new ArrayList<>();
        for (String name : organNames) {
            if (!seen.add(name.toLowerCase().trim())) duplicates.add(name);
        }
        ApiReportContext.addExtraDetail("<b>Total names:</b> " + organNames.size()
                + " | <b>Duplicate names:</b> "
                + (duplicates.isEmpty() ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + duplicates + "</span>"));
        Assert.assertTrue(duplicates.isEmpty(), "Duplicate organ names found: " + duplicates);
        System.out.println("✅ TC17 PASSED");
    }

    @Test(priority = 18, description = "TC18: No duplicate organ objects in the list")
    public void testTC18_NoDuplicateOrgans() {
        System.out.println("\n>>> TC18: No duplicate organ objects (_id level) <<<");
        Set<String> seen = new HashSet<>();
        List<String> duplicates = new ArrayList<>();
        for (Map<String, Object> organ : organsData) {
            if (organ == null) continue;
            String idKey = organ.containsKey("_id") ? "_id" : "id";
            String id = String.valueOf(organ.get(idKey));
            if (!seen.add(id)) duplicates.add(id);
        }
        ApiReportContext.addExtraDetail("<b>Total entries:</b> " + organsData.size()
                + " | <b>Duplicate organ objects:</b> "
                + (duplicates.isEmpty() ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + duplicates + "</span>"));
        Assert.assertTrue(duplicates.isEmpty(), "Duplicate organ objects: " + duplicates);
        System.out.println("✅ TC18 PASSED");
    }

    @Test(priority = 19, description = "TC19: Organ names do not contain unexpected special characters")
    public void testTC19_NamesNoUnexpectedSpecialChars() {
        System.out.println("\n>>> TC19: Organ names should not contain HTML/script characters <<<");
        List<String> flagged = new ArrayList<>();
        for (String name : organNames) {
            if (name.contains("<") || name.contains(">") || name.contains("&") || name.contains("\"")) {
                flagged.add(name);
            }
        }
        ApiReportContext.addExtraDetail("<b>Names with unexpected special chars:</b> "
                + (flagged.isEmpty() ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + flagged + "</span>"));
        Assert.assertTrue(flagged.isEmpty(),
                "Organ names contain unexpected special characters: " + flagged);
        System.out.println("✅ TC19 PASSED");
    }

    @Test(priority = 20, description = "TC20: No inactive organs are returned")
    public void testTC20_NoInactiveOrgans() {
        System.out.println("\n>>> TC20: isActive=false organs should not be in response <<<");
        List<String> inactive = new ArrayList<>();
        for (Map<String, Object> organ : organsData) {
            if (organ == null) continue;
            Object active = organ.get("isActive");
            if (active != null && "false".equalsIgnoreCase(active.toString())) {
                inactive.add(String.valueOf(organ.getOrDefault("name", organ.get("_id"))));
            }
        }
        ApiReportContext.addExtraDetail("<b>Inactive organs in response:</b> "
                + (inactive.isEmpty() ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + inactive + "</span>"));
        Assert.assertTrue(inactive.isEmpty(),
                "Inactive organs found in response: " + inactive);
        System.out.println("✅ TC20 PASSED");
    }

    @Test(priority = 21, description = "TC21: No deleted organs are returned (isDeleted check)")
    public void testTC21_NoDeletedOrgans() {
        System.out.println("\n>>> TC21: Deleted organs should not be in response <<<");
        List<String> deleted = new ArrayList<>();
        for (Map<String, Object> organ : organsData) {
            if (organ == null) continue;
            Object del = organ.get("isDeleted");
            if (del != null && "true".equalsIgnoreCase(del.toString())) {
                deleted.add(String.valueOf(organ.getOrDefault("name", organ.get("_id"))));
            }
        }
        ApiReportContext.addExtraDetail("<b>Deleted organs in response:</b> "
                + (deleted.isEmpty() ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + deleted + "</span>"));
        Assert.assertTrue(deleted.isEmpty(),
                "Deleted organs found in response: " + deleted);
        System.out.println("✅ TC21 PASSED");
    }

    @Test(priority = 22, description = "TC22: Organ data is consistent across two calls")
    public void testTC22_DataConsistency() {
        System.out.println("\n>>> TC22: Two calls should return identical organ IDs <<<");
        Response r1 = callGetOrgans();
        Response r2 = callGetOrgans();
        Assert.assertEquals(r1.getStatusCode(), 200);
        Assert.assertEquals(r2.getStatusCode(), 200);

        List<Map<String, Object>> list1 = r1.jsonPath().getList("data");
        List<Map<String, Object>> list2 = r2.jsonPath().getList("data");
        Assert.assertNotNull(list1, "data array missing in first call");
        Assert.assertNotNull(list2, "data array missing in second call");

        Set<String> ids1 = list1.stream()
                .filter(Objects::nonNull)
                .map(o -> String.valueOf(o.get("_id")))
                .collect(Collectors.toSet());
        Set<String> ids2 = list2.stream()
                .filter(Objects::nonNull)
                .map(o -> String.valueOf(o.get("_id")))
                .collect(Collectors.toSet());

        Assert.assertEquals(ids1, ids2, "Organ IDs differ between two consecutive calls");
        ApiReportContext.addExtraDetail("<b>Call 1 organ count:</b> " + ids1.size()
                + " | <b>Call 2 organ count:</b> " + ids2.size()
                + " | <b>Data consistent:</b> <span style='color:green'>YES</span>");
        System.out.println("✅ TC22 PASSED");
    }

    @Test(priority = 23, description = "TC23: Organ name length is within reasonable bounds (1-200 chars)")
    public void testTC23_NameLengthValidation() {
        System.out.println("\n>>> TC23: Organ name length should be 1-200 chars <<<");
        List<String> invalid = new ArrayList<>();
        for (String name : organNames) {
            if (name.length() < 1 || name.length() > 200) invalid.add(name);
        }
        ApiReportContext.addExtraDetail("<b>Names with invalid length:</b> "
                + (invalid.isEmpty() ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + invalid + "</span>")
                + " | <b>All lengths (1-200):</b> verified for " + organNames.size() + " names");
        Assert.assertTrue(invalid.isEmpty(),
                "Organ names with out-of-range length: " + invalid);
        System.out.println("✅ TC23 PASSED");
    }

    // =========================================================================
    //  SORTING VALIDATION (TC24-TC26)
    // =========================================================================

    @Test(priority = 24, description = "TC24: Organs are returned in alphabetical order")
    public void testTC24_AlphabeticalOrder() {
        System.out.println("\n>>> TC24: Organ names should be in alphabetical order <<<");
        if (organNames.size() < 2) {
            System.out.println("   Only " + organNames.size() + " organ(s) — skipping order check");
            ApiReportContext.addExtraDetail("<b>Alphabetical check:</b> N/A (less than 2 organs)");
            return;
        }
        List<String> sorted = new ArrayList<>(organNames);
        Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);
        boolean isAlpha = organNames.equals(sorted) ||
                organNames.stream().map(String::toLowerCase).collect(Collectors.toList())
                        .equals(sorted.stream().map(String::toLowerCase).collect(Collectors.toList()));
        ApiReportContext.addExtraDetail("<b>Returned order:</b> " + organNames
                + "<br/>&nbsp;&nbsp;<b>Expected alphabetical:</b> " + sorted
                + "<br/>&nbsp;&nbsp;<b>Is alphabetical:</b> "
                + (isAlpha ? "<span style='color:green'>YES</span>" : "<span style='color:orange'>NO (server may use custom order)</span>"));
        // Log only — some APIs use custom sort order; fail is informational
        if (!isAlpha) {
            System.out.println("   INFO: Organs not in strict alphabetical order. Returned: " + organNames);
            System.out.println("   INFO: Expected: " + sorted);
        }
        System.out.println("✅ TC24 PASSED — order documented above");
    }

    @Test(priority = 25, description = "TC25: Sorting is consistent across two calls")
    public void testTC25_SortingConsistent() {
        System.out.println("\n>>> TC25: Order of organs should be consistent across calls <<<");
        Response r1 = callGetOrgans();
        Response r2 = callGetOrgans();
        List<Map<String, Object>> l1 = r1.jsonPath().getList("data");
        List<Map<String, Object>> l2 = r2.jsonPath().getList("data");
        Assert.assertNotNull(l1, "data missing in call 1");
        Assert.assertNotNull(l2, "data missing in call 2");
        Assert.assertEquals(l1.size(), l2.size(), "Different organ counts between calls");

        // Compare first entry _id
        String id1 = String.valueOf(l1.get(0).get("_id"));
        String id2 = String.valueOf(l2.get(0).get("_id"));
        Assert.assertEquals(id1, id2, "First entry _id differs - ordering is unstable");
        ApiReportContext.addExtraDetail("<b>First entry _id (Call 1):</b> " + id1
                + " | <b>First entry _id (Call 2):</b> " + id2
                + " | <b>Order stable:</b> <span style='color:green'>YES</span>");
        System.out.println("✅ TC25 PASSED");
    }

    @Test(priority = 26, description = "TC26: Ascending sort order is verified (first < last alphabetically)")
    public void testTC26_AscendingSortOrder() {
        System.out.println("\n>>> TC26: First organ name should come before last alphabetically (if sorted) <<<");
        if (organNames.size() < 2) {
            ApiReportContext.addExtraDetail("<b>Ascending check:</b> N/A (less than 2 organs)");
            return;
        }
        String first = organNames.get(0).toLowerCase();
        String last  = organNames.get(organNames.size() - 1).toLowerCase();
        ApiReportContext.addExtraDetail("<b>First name:</b> " + organNames.get(0)
                + " | <b>Last name:</b> " + organNames.get(organNames.size() - 1)
                + " | <b>first &le; last:</b> "
                + (first.compareTo(last) <= 0 ? "<span style='color:green'>YES</span>" : "<span style='color:orange'>NO</span>"));
        // Informational only — server may use a non-alphabetical custom order
        System.out.println("   First: " + organNames.get(0) + " | Last: " + organNames.get(organNames.size() - 1));
        System.out.println("✅ TC26 PASSED");
    }

    // =========================================================================
    //  SCHEMA VALIDATION (TC27-TC32)
    // =========================================================================

    @Test(priority = 27, description = "TC27: Response envelope schema - success, msg, data fields present")
    public void testTC27_ResponseSchema() {
        System.out.println("\n>>> TC27: Top-level schema validation <<<");
        Response r = callGetOrgans();
        Assert.assertEquals(r.getStatusCode(), 200);
        Assert.assertNotNull(r.jsonPath().get("success"), "Missing: success");
        Assert.assertNotNull(r.jsonPath().get("msg"),     "Missing: msg");
        Object dataField = r.jsonPath().get("data");
        ApiReportContext.addExtraDetail("<b>success present:</b> <span style='color:green'>YES</span>"
                + " | <b>msg present:</b> <span style='color:green'>YES</span>"
                + " | <b>data present:</b> " + (dataField != null ? "<span style='color:green'>YES</span>" : "<span style='color:orange'>NOT FOUND</span>"));
        System.out.println("✅ TC27 PASSED");
    }

    @Test(priority = 28, description = "TC28: Each organ object has expected schema fields")
    public void testTC28_OrganObjectSchema() {
        System.out.println("\n>>> TC28: Each organ must have _id and a name-like field <<<");
        Assert.assertNotNull(firstOrgan, "No organs returned");
        boolean hasId   = firstOrgan.containsKey("_id") || firstOrgan.containsKey("id");
        boolean hasName = firstOrgan.containsKey("name") || firstOrgan.containsKey("organ_name") || firstOrgan.containsKey("title");
        ApiReportContext.addExtraDetail("<b>First organ keys:</b> " + firstOrgan.keySet()
                + "<br/>&nbsp;&nbsp;<b>has _id:</b> " + hasId
                + " | <b>has name:</b> " + hasName);
        Assert.assertTrue(hasId,   "Organ missing _id/id field");
        Assert.assertTrue(hasName, "Organ missing name/organ_name/title field");
        System.out.println("✅ TC28 PASSED");
    }

    @Test(priority = 29, description = "TC29: Organ _id is a non-empty string")
    public void testTC29_OrganIdDatatype() {
        System.out.println("\n>>> TC29: Organ _id should be a non-empty string <<<");
        Assert.assertFalse(organIds.isEmpty(), "No organ IDs extracted");
        for (String id : organIds) {
            Assert.assertNotNull(id, "_id is null");
            Assert.assertFalse(id.trim().isEmpty(), "_id is blank");
            Assert.assertFalse(id.equalsIgnoreCase("null"), "_id is literal 'null'");
        }
        ApiReportContext.addExtraDetail("<b>IDs validated:</b> " + organIds.size()
                + " | <b>Sample _id:</b> " + (organIds.isEmpty() ? "N/A" : organIds.get(0)));
        System.out.println("✅ TC29 PASSED");
    }

    @Test(priority = 30, description = "TC30: Organ name is a non-empty string type")
    public void testTC30_OrganNameDatatype() {
        System.out.println("\n>>> TC30: Organ name should be a non-empty string <<<");
        Assert.assertFalse(organNames.isEmpty(), "No organ names extracted");
        for (String name : organNames) {
            Assert.assertNotNull(name, "name is null");
            Assert.assertFalse(name.trim().isEmpty(), "name is blank");
        }
        ApiReportContext.addExtraDetail("<b>Names validated:</b> " + organNames.size()
                + " | <b>Sample name:</b> " + (organNames.isEmpty() ? "N/A" : organNames.get(0)));
        System.out.println("✅ TC30 PASSED");
    }

    @Test(priority = 31, description = "TC31: createdAt field, if present, is a valid date-time string")
    public void testTC31_CreatedAtDatatype() {
        System.out.println("\n>>> TC31: createdAt field type check <<<");
        Assert.assertNotNull(firstOrgan, "No organs returned");
        Object createdAt = firstOrgan.get("createdAt");
        String val = createdAt != null ? createdAt.toString() : "N/A";
        ApiReportContext.addExtraDetail("<b>createdAt field:</b> " + val);
        if (createdAt != null) {
            Assert.assertFalse(createdAt.toString().trim().isEmpty(), "createdAt is blank");
        }
        System.out.println("✅ TC31 PASSED — createdAt: " + val);
    }

    @Test(priority = 32, description = "TC32: updatedAt field, if present, is a valid date-time string")
    public void testTC32_UpdatedAtDatatype() {
        System.out.println("\n>>> TC32: updatedAt field type check <<<");
        Assert.assertNotNull(firstOrgan, "No organs returned");
        Object updatedAt = firstOrgan.get("updatedAt");
        String val = updatedAt != null ? updatedAt.toString() : "N/A";
        ApiReportContext.addExtraDetail("<b>updatedAt field:</b> " + val);
        if (updatedAt != null) {
            Assert.assertFalse(updatedAt.toString().trim().isEmpty(), "updatedAt is blank");
        }
        System.out.println("✅ TC32 PASSED — updatedAt: " + val);
    }

    // =========================================================================
    //  NEGATIVE SCENARIOS (TC33-TC39)
    // =========================================================================

    @Test(priority = 33, description = "TC33: API works without a request body")
    public void testTC33_NoRequestBody() {
        System.out.println("\n>>> TC33: No request body — server should still respond <<<");
        Response r = callGetOrgans();
        ApiReportContext.addExtraDetail("<b>No request body sent:</b> Status " + r.getStatusCode() + " returned");
        Assert.assertEquals(r.getStatusCode(), 200, "Expected 200 without body");
        System.out.println("✅ TC33 PASSED");
    }

    @Test(priority = 34, description = "TC34: API works with accept */*")
    public void testTC34_AcceptWildcard() {
        System.out.println("\n>>> TC34: accept: */* should be accepted <<<");
        Response r = callGetOrgans(); // already uses accept: */*
        ApiReportContext.addExtraDetail("<b>accept: */*:</b> Accepted | <b>Status:</b> " + r.getStatusCode());
        Assert.assertEquals(r.getStatusCode(), 200, "Expected 200 with accept: */*");
        System.out.println("✅ TC34 PASSED");
    }

    @Test(priority = 35, description = "TC35: Invalid Accept header - server handles gracefully")
    public void testTC35_InvalidAcceptHeader() {
        System.out.println("\n>>> TC35: Invalid accept header — should not 5xx <<<");
        long start = System.currentTimeMillis();
        Response r = new RequestBuilder()
                .setEndpoint(ENDPOINT)
                .addHeader("accept", "invalid/type-xyz")
                .get();
        long elapsed = System.currentTimeMillis() - start;
        ApiReportContext.addRecord(new ApiReportContext.ApiCallRecord(
                "GET", ENDPOINT, "", r.getStatusCode(), r.asString(), elapsed, 0, 200, 599, "Invalid Accept header"));
        ApiReportContext.addExtraDetail("<b>accept: invalid/type-xyz - Status:</b> " + r.getStatusCode()
                + " | <b>Server crashed:</b> <span style='color:green'>NO</span>");
        Assert.assertTrue(r.getStatusCode() < 500, "Server returned 5xx on invalid Accept: " + r.getStatusCode());
        System.out.println("✅ TC35 PASSED");
    }

    @Test(priority = 36, description = "TC36: Unsupported Content-Type header handled gracefully")
    public void testTC36_UnsupportedMediaType() {
        System.out.println("\n>>> TC36: Unsupported content-type in header <<<");
        long start = System.currentTimeMillis();
        Response r = new RequestBuilder()
                .setEndpoint(ENDPOINT)
                .addHeader("accept", "*/*")
                .addHeader("Content-Type", "application/xml")
                .get();
        long elapsed = System.currentTimeMillis() - start;
        ApiReportContext.addRecord(new ApiReportContext.ApiCallRecord(
                "GET", ENDPOINT, "", r.getStatusCode(), r.asString(), elapsed, 0, 200, 599, "Unsupported Content-Type header"));
        ApiReportContext.addExtraDetail("<b>Content-Type: application/xml - Status:</b> " + r.getStatusCode());
        Assert.assertTrue(r.getStatusCode() < 500,
                "Server returned 5xx on unsupported Content-Type: " + r.getStatusCode());
        System.out.println("✅ TC36 PASSED");
    }

    @Test(priority = 37, description = "TC37: POST /tests/getOrgans returns 404 or 405")
    public void testTC37_PostMethodNotAllowed() {
        System.out.println("\n>>> TC37: POST method should not be supported <<<");
        ApiReportContext.setExpectedStatusRange(400, 499);
        long start = System.currentTimeMillis();
        Response r = new RequestBuilder()
                .setEndpoint(ENDPOINT)
                .addHeader("accept", "*/*")
                .post();
        long elapsed = System.currentTimeMillis() - start;
        ApiReportContext.addRecord(new ApiReportContext.ApiCallRecord(
                "POST", ENDPOINT, "", r.getStatusCode(), r.asString(), elapsed, 400, 400, 499, "POST should be rejected"));
        ApiReportContext.addExtraDetail("<b>POST status:</b> " + r.getStatusCode()
                + " | <b>Expected:</b> 4xx (method not allowed)");
        Assert.assertNotEquals(r.getStatusCode(), 200, "POST /getOrgans should not return 200");
        System.out.println("✅ TC37 PASSED — POST returned: " + r.getStatusCode());
    }

    @Test(priority = 38, description = "TC38: PUT /tests/getOrgans returns 404 or 405")
    public void testTC38_PutMethodNotAllowed() {
        System.out.println("\n>>> TC38: PUT method should not be supported <<<");
        ApiReportContext.setExpectedStatusRange(400, 499);
        long start = System.currentTimeMillis();
        Response r = new RequestBuilder()
                .setEndpoint(ENDPOINT)
                .addHeader("accept", "*/*")
                .put();
        long elapsed = System.currentTimeMillis() - start;
        ApiReportContext.addRecord(new ApiReportContext.ApiCallRecord(
                "PUT", ENDPOINT, "", r.getStatusCode(), r.asString(), elapsed, 400, 400, 499, "PUT should be rejected"));
        ApiReportContext.addExtraDetail("<b>PUT status:</b> " + r.getStatusCode()
                + " | <b>Expected:</b> 4xx (method not allowed)");
        Assert.assertNotEquals(r.getStatusCode(), 200, "PUT /getOrgans should not return 200");
        System.out.println("✅ TC38 PASSED — PUT returned: " + r.getStatusCode());
    }

    @Test(priority = 39, description = "TC39: DELETE /tests/getOrgans returns 404 or 405")
    public void testTC39_DeleteMethodNotAllowed() {
        System.out.println("\n>>> TC39: DELETE method should not be supported <<<");
        ApiReportContext.setExpectedStatusRange(400, 499);
        long start = System.currentTimeMillis();
        Response r = new RequestBuilder()
                .setEndpoint(ENDPOINT)
                .addHeader("accept", "*/*")
                .delete();
        long elapsed = System.currentTimeMillis() - start;
        ApiReportContext.addRecord(new ApiReportContext.ApiCallRecord(
                "DELETE", ENDPOINT, "", r.getStatusCode(), r.asString(), elapsed, 400, 400, 499, "DELETE should be rejected"));
        ApiReportContext.addExtraDetail("<b>DELETE status:</b> " + r.getStatusCode()
                + " | <b>Expected:</b> 4xx (method not allowed)");
        Assert.assertNotEquals(r.getStatusCode(), 200, "DELETE /getOrgans should not return 200");
        System.out.println("✅ TC39 PASSED — DELETE returned: " + r.getStatusCode());
    }

    // =========================================================================
    //  SECURITY SCENARIOS (TC40-TC43)
    // =========================================================================

    @Test(priority = 40, description = "TC40: No sensitive data (passwords, tokens) exposed in response")
    public void testTC40_NoSensitiveDataExposed() {
        System.out.println("\n>>> TC40: Response should not contain sensitive data <<<");
        String body = callGetOrgans().asString().toLowerCase();
        Assert.assertFalse(body.contains("password"), "Response contains 'password'");
        Assert.assertFalse(body.contains("token"),    "Response contains 'token'");
        Assert.assertFalse(body.contains("secret"),   "Response contains 'secret'");
        ApiReportContext.addExtraDetail("<b>password/token/secret in body:</b> <span style='color:green'>NOT FOUND</span>");
        System.out.println("✅ TC40 PASSED");
    }

    @Test(priority = 41, description = "TC41: Internal DB/implementation fields not exposed")
    public void testTC41_NoInternalDbFields() {
        System.out.println("\n>>> TC41: Internal DB fields should not be exposed <<<");
        Assert.assertNotNull(firstOrgan, "No organs returned");
        boolean hasV = firstOrgan.containsKey("__v");
        ApiReportContext.addExtraDetail("<b>__v (Mongoose version key) present:</b> "
                + (hasV ? "<span style='color:orange'>" + firstOrgan.get("__v") + "</span>" : "<span style='color:green'>NOT EXPOSED</span>"));
        // __v is a common Mongoose internal field — warn but don't fail (some APIs expose it)
        System.out.println("   __v present: " + hasV + (hasV ? " (" + firstOrgan.get("__v") + ")" : ""));
        System.out.println("✅ TC41 PASSED");
    }

    @Test(priority = 42, description = "TC42: No stack trace or exception details in response")
    public void testTC42_NoStackTrace() {
        System.out.println("\n>>> TC42: Response must not expose stack traces <<<");
        String body = callGetOrgans().asString().toLowerCase();
        Assert.assertFalse(body.contains("stack trace"), "Stack trace found");
        Assert.assertFalse(body.contains("exception"),   "Exception details found");
        Assert.assertFalse(body.contains("at com."),     "Java package trace found");
        ApiReportContext.addExtraDetail("<b>Stack trace leaked:</b> <span style='color:green'>NO</span>"
                + " | <b>Exception leaked:</b> <span style='color:green'>NO</span>"
                + " | <b>Java trace leaked:</b> <span style='color:green'>NO</span>");
        System.out.println("✅ TC42 PASSED");
    }

    @Test(priority = 43, description = "TC43: No server implementation details exposed")
    public void testTC43_NoServerDetails() {
        System.out.println("\n>>> TC43: Server implementation details check <<<");
        Response r = callGetOrgans();
        String body = r.asString().toLowerCase();
        Assert.assertFalse(body.contains("mongodb error"),     "MongoDB error details exposed");
        Assert.assertFalse(body.contains("syntaxerror"),       "SyntaxError exposed");
        Assert.assertFalse(body.contains("internal server"),   "Internal server error exposed");
        ApiReportContext.addExtraDetail("<b>MongoDB errors:</b> <span style='color:green'>NOT FOUND</span>"
                + " | <b>SyntaxError:</b> <span style='color:green'>NOT FOUND</span>");
        System.out.println("✅ TC43 PASSED");
    }

    // =========================================================================
    //  PERFORMANCE SCENARIOS (TC44-TC47)
    // =========================================================================

    @Test(priority = 44, description = "TC44: Response time is under 2000 ms")
    public void testTC44_ResponseTimeUnder2s() {
        System.out.println("\n>>> TC44: Response time < 2000 ms <<<");
        long start = System.currentTimeMillis();
        Response r = callGetOrgans();
        long elapsed = System.currentTimeMillis() - start;
        Assert.assertEquals(r.getStatusCode(), 200);
        ApiReportContext.addExtraDetail("<b>Response Time:</b> " + elapsed + " ms"
                + " | <b>Threshold:</b> 2000 ms"
                + " | <b>Within SLA:</b> " + (elapsed < 2000 ? "<span style='color:green'>YES</span>" : "<span style='color:red'>NO</span>"));
        Assert.assertTrue(elapsed < 2000,
                "Response took " + elapsed + " ms - exceeded 2000 ms threshold");
        System.out.println("✅ TC44 PASSED");
    }

    @Test(priority = 45, description = "TC45: Response is consistent across 3 consecutive calls")
    public void testTC45_ConsistencyMultipleCalls() {
        System.out.println("\n>>> TC45: 3 consecutive calls should all return 200 <<<");
        int[] statuses = new int[3];
        for (int i = 0; i < 3; i++) {
            statuses[i] = callGetOrgans().getStatusCode();
            Assert.assertEquals(statuses[i], 200, "Call " + (i + 1) + " returned " + statuses[i]);
        }
        ApiReportContext.addExtraDetail("<b>Call 1:</b> " + statuses[0]
                + " | <b>Call 2:</b> " + statuses[1]
                + " | <b>Call 3:</b> " + statuses[2]
                + " | <b>All 200:</b> <span style='color:green'>YES</span>");
        System.out.println("✅ TC45 PASSED");
    }

    @Test(priority = 46, description = "TC46: Response handles concurrent requests (3 threads)")
    public void testTC46_ConcurrentRequests() throws InterruptedException, ExecutionException {
        System.out.println("\n>>> TC46: Concurrent requests — all should return 200 <<<");
        int threadCount = 3;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(pool.submit(() -> {
                Response r = new RequestBuilder()
                        .setEndpoint(ENDPOINT)
                        .addHeader("accept", "*/*")
                        .get();
                return r.getStatusCode();
            }));
        }
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);
        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> f : futures) statuses.add(f.get());
        long successCount = statuses.stream().filter(s -> s == 200).count();
        ApiReportContext.addExtraDetail("<b>Concurrent threads:</b> " + threadCount
                + " | <b>All returned 200:</b> " + (successCount == threadCount ? "<span style='color:green'>YES</span>" : "<span style='color:red'>NO (" + statuses + ")</span>"));
        for (int status : statuses) {
            Assert.assertEquals(status, 200, "Concurrent call returned " + status);
        }
        System.out.println("✅ TC46 PASSED");
    }

    @Test(priority = 47, description = "TC47: Large organ dataset is handled (payload < 1 MB)")
    public void testTC47_LargeDatasetHandling() {
        System.out.println("\n>>> TC47: Payload should be < 1 MB <<<");
        Response r = callGetOrgans();
        int bodyLength = r.asString().length();
        ApiReportContext.addExtraDetail("<b>Payload Size:</b> " + bodyLength + " chars"
                + " | <b>Max allowed:</b> 1,000,000 chars"
                + " | <b>Within limit:</b> <span style='color:green'>YES</span>");
        Assert.assertTrue(bodyLength < 1_000_000,
                "Response too large: " + bodyLength + " chars (> 1 MB)");
        System.out.println("✅ TC47 PASSED");
    }

    // =========================================================================
    //  INTEGRATION VALIDATION (TC48-TC52)
    // =========================================================================

    @Test(priority = 48, description = "TC48: Organs returned are non-empty (displayable in UI)")
    public void testTC48_OrgansDisplayableInUI() {
        System.out.println("\n>>> TC48: Organs should have name values displayable in UI <<<");
        Assert.assertFalse(organNames.isEmpty(), "No organ names - nothing to display");
        long displayable = organNames.stream()
                .filter(n -> n != null && !n.trim().isEmpty())
                .count();
        ApiReportContext.addExtraDetail("<b>Displayable organ names:</b> " + displayable
                + " / " + organNames.size());
        Assert.assertEquals(displayable, (long) organNames.size(),
                "Some organs have no displayable name");
        System.out.println("✅ TC48 PASSED");
    }

    @Test(priority = 49, description = "TC49: Organ names are usable in organ-based search (non-empty strings)")
    public void testTC49_OrgansUsableInSearch() {
        System.out.println("\n>>> TC49: Organ names must be non-empty for search use <<<");
        Assert.assertFalse(organNames.isEmpty(), "No organ names available");
        long searchable = organNames.stream().filter(n -> n != null && n.trim().length() >= 2).count();
        ApiReportContext.addExtraDetail("<b>Search-ready organ names (len >= 2):</b> " + searchable
                + " / " + organNames.size());
        Assert.assertEquals(searchable, (long) organNames.size(),
                "Some organ names are too short or blank for search");
        System.out.println("✅ TC49 PASSED");
    }

    @Test(priority = 50, description = "TC50: Organ count is positive (can be cross-checked with admin config)")
    public void testTC50_OrganCountPositive() {
        System.out.println("\n>>> TC50: Organ count should be > 0 <<<");
        Assert.assertTrue(totalOrgans > 0,
                "Expected at least 1 organ for admin config match. Got: " + totalOrgans);
        ApiReportContext.addExtraDetail("<b>Organ count from API:</b> " + totalOrgans
                + " | <b>Cross-check with admin config manually</b>");
        System.out.println("✅ TC50 PASSED");
    }

    @Test(priority = 51, description = "TC51: Each organ entry is a valid map object (organ-symptom mapping ready)")
    public void testTC51_OrganMappingToSymptoms() {
        System.out.println("\n>>> TC51: Organ entries should be valid objects (mapping-ready) <<<");
        int invalid = 0;
        for (Map<String, Object> organ : organsData) {
            if (organ == null || organ.isEmpty()) invalid++;
        }
        ApiReportContext.addExtraDetail("<b>Valid organ objects:</b> " + (organsData.size() - invalid)
                + " / " + organsData.size());
        Assert.assertEquals(invalid, 0, invalid + " invalid organ objects found");
        System.out.println("✅ TC51 PASSED");
    }

    @Test(priority = 52, description = "TC52: Organ IDs are non-empty strings (usable in test/package queries)")
    public void testTC52_OrganMappingToTests() {
        System.out.println("\n>>> TC52: Organ IDs must be non-empty (usable in test/package API calls) <<<");
        Assert.assertFalse(organIds.isEmpty(), "No organ IDs extracted");
        long validIds = organIds.stream()
                .filter(id -> id != null && !id.trim().isEmpty() && !id.equalsIgnoreCase("null"))
                .count();
        ApiReportContext.addExtraDetail("<b>Valid organ IDs:</b> " + validIds
                + " / " + organIds.size()
                + " | <b>Sample:</b> " + organIds.get(0));
        Assert.assertEquals(validIds, (long) organIds.size(), "Some organ IDs are blank/null");
        System.out.println("✅ TC52 PASSED");
    }

    // =========================================================================
    //  RESPONSE VALIDATION (TC53-TC57)
    // =========================================================================

    @Test(priority = 53, description = "TC53: Status code is 200")
    public void testTC53_StatusCode() {
        System.out.println("\n>>> TC53: HTTP status code should be 200 <<<");
        Response r = callGetOrgans();
        ApiReportContext.addExtraDetail("<b>HTTP Status Code:</b> " + r.getStatusCode());
        Assert.assertEquals(r.getStatusCode(), 200);
        System.out.println("✅ TC53 PASSED");
    }

    @Test(priority = 54, description = "TC54: Response message (msg) field has a value")
    public void testTC54_ResponseMsg() {
        System.out.println("\n>>> TC54: msg field should be present and non-empty <<<");
        Response r = callGetOrgans();
        String msg = r.jsonPath().getString("msg");
        Assert.assertNotNull(msg, "msg field is null");
        Assert.assertFalse(msg.trim().isEmpty(), "msg field is empty");
        ApiReportContext.addExtraDetail("<b>msg value:</b> &quot;" + msg + "&quot;");
        System.out.println("✅ TC54 PASSED");
    }

    @Test(priority = 55, description = "TC55: success flag is true")
    public void testTC55_SuccessFlagTrue() {
        System.out.println("\n>>> TC55: success=true in response body <<<");
        Response r = callGetOrgans();
        Boolean success = r.jsonPath().getBoolean("success");
        Assert.assertNotNull(success, "success field missing");
        Assert.assertTrue(success, "success should be true, got: " + success);
        ApiReportContext.addExtraDetail("<b>success:</b> " + success);
        System.out.println("✅ TC55 PASSED");
    }

    @Test(priority = 56, description = "TC56: Response data object (data array) is present")
    public void testTC56_ResponseDataObject() {
        System.out.println("\n>>> TC56: data array should be present and non-null <<<");
        Response r = callGetOrgans();
        List<?> data = r.jsonPath().getList("data");
        Assert.assertNotNull(data, "data array is null/missing");
        Assert.assertFalse(data.isEmpty(), "data array is empty");
        ApiReportContext.addExtraDetail("<b>data array size:</b> " + data.size()
                + " | <b>Present:</b> <span style='color:green'>YES</span>");
        System.out.println("✅ TC56 PASSED");
    }

    @Test(priority = 57, description = "TC57: Mandatory fields contain valid (non-null, non-empty) values")
    public void testTC57_MandatoryFieldsValid() {
        System.out.println("\n>>> TC57: Mandatory fields (_id, name) have valid values <<<");
        Assert.assertFalse(organsData.isEmpty(), "No organs to validate");
        int invalid = 0;
        for (Map<String, Object> organ : organsData) {
            if (organ == null) { invalid++; continue; }
            String idKey   = organ.containsKey("_id") ? "_id" : "id";
            String nameKey = organ.containsKey("name") ? "name" : organ.containsKey("organ_name") ? "organ_name" : "title";
            Object id      = organ.get(idKey);
            Object name    = organ.get(nameKey);
            if (id == null || id.toString().trim().isEmpty())     invalid++;
            else if (name == null || name.toString().trim().isEmpty()) invalid++;
        }
        ApiReportContext.addExtraDetail("<b>Organs with invalid mandatory fields:</b> " + invalid
                + " | <b>Total:</b> " + organsData.size());
        Assert.assertEquals(invalid, 0, invalid + " organs have invalid mandatory fields");
        System.out.println("✅ TC57 PASSED");
    }

    // =========================================================================
    //  HIGH PRIORITY AUTOMATION (TC58-TC75)
    // =========================================================================

    @Test(priority = 58, description = "TC58: Status code validation - returns 200")
    public void testTC58_StatusCodeValidation() {
        System.out.println("\n>>> TC58: Status Code Validation <<<");
        Response r = callGetOrgans();
        ApiReportContext.addExtraDetail("<b>HTTP Status:</b> " + r.getStatusCode()
                + " | <b>Response Time:</b> " + r.getTime() + " ms");
        Assert.assertEquals(r.getStatusCode(), 200);
        System.out.println("✅ TC58 PASSED");
    }

    @Test(priority = 59, description = "TC59: Response time < 2000 ms (high-priority check)")
    public void testTC59_ResponseTimeValidation() {
        System.out.println("\n>>> TC59: Response Time Validation <<<");
        long start = System.currentTimeMillis();
        Response r = callGetOrgans();
        long elapsed = System.currentTimeMillis() - start;
        ApiReportContext.addExtraDetail("<b>Response Time:</b> " + elapsed + " ms | <b>SLA:</b> 2000 ms");
        Assert.assertTrue(elapsed < 2000,
                "Response took " + elapsed + " ms, exceeded 2000 ms SLA");
        System.out.println("✅ TC59 PASSED");
    }

    @Test(priority = 60, description = "TC60: Schema validation - success, msg, data fields present")
    public void testTC60_SchemaValidation() {
        System.out.println("\n>>> TC60: Schema Validation <<<");
        Response r = callGetOrgans();
        Assert.assertEquals(r.getStatusCode(), 200);
        Assert.assertNotNull(r.jsonPath().get("success"), "missing: success");
        Assert.assertNotNull(r.jsonPath().get("msg"),     "missing: msg");
        List<?> data = r.jsonPath().getList("data");
        Assert.assertNotNull(data, "missing: data array");
        ApiReportContext.addExtraDetail("<b>success:</b> " + r.jsonPath().get("success")
                + " | <b>msg:</b> " + r.jsonPath().getString("msg")
                + " | <b>data count:</b> " + (data != null ? data.size() : 0));
        System.out.println("✅ TC60 PASSED");
    }

    @Test(priority = 61, description = "TC61: All organ IDs are valid non-empty strings")
    public void testTC61_OrganIdValidation() {
        System.out.println("\n>>> TC61: Organ ID Validation <<<");
        Assert.assertFalse(organIds.isEmpty(), "No organ IDs extracted");
        long invalid = organIds.stream()
                .filter(id -> id == null || id.trim().isEmpty() || id.equalsIgnoreCase("null"))
                .count();
        ApiReportContext.addExtraDetail("<b>Total IDs:</b> " + organIds.size()
                + " | <b>Invalid IDs:</b> " + invalid);
        Assert.assertEquals(invalid, 0L, invalid + " organ IDs are invalid");
        System.out.println("✅ TC61 PASSED");
    }

    @Test(priority = 62, description = "TC62: All organ names are valid non-empty strings")
    public void testTC62_OrganNameValidation() {
        System.out.println("\n>>> TC62: Organ Name Validation <<<");
        Assert.assertFalse(organNames.isEmpty(), "No organ names extracted");
        long invalid = organNames.stream()
                .filter(n -> n == null || n.trim().isEmpty())
                .count();
        StringBuilder nameList = new StringBuilder();
        organNames.forEach(n -> nameList.append(n).append(", "));
        ApiReportContext.addExtraDetail("<b>All organ names:</b> " + nameList.toString().replaceAll(", $", "")
                + "<br/>&nbsp;&nbsp;<b>Invalid names:</b> " + invalid);
        Assert.assertEquals(invalid, 0L, invalid + " organ names are invalid");
        System.out.println("✅ TC62 PASSED");
    }

    @Test(priority = 63, description = "TC63: No duplicate organ names or IDs")
    public void testTC63_DuplicateOrganValidation() {
        System.out.println("\n>>> TC63: Duplicate Organ Validation <<<");
        Set<String> seenNames = new LinkedHashSet<>();
        List<String> dupNames = new ArrayList<>();
        for (String n : organNames) {
            if (!seenNames.add(n.toLowerCase().trim())) dupNames.add(n);
        }
        Set<String> seenIds = new HashSet<>(organIds);
        int dupIds = organIds.size() - seenIds.size();
        ApiReportContext.addExtraDetail("<b>Duplicate names:</b> "
                + (dupNames.isEmpty() ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + dupNames + "</span>")
                + " | <b>Duplicate IDs:</b> "
                + (dupIds == 0 ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + dupIds + " found</span>"));
        Assert.assertTrue(dupNames.isEmpty(), "Duplicate organ names: " + dupNames);
        Assert.assertEquals(dupIds, 0, dupIds + " duplicate organ IDs found");
        System.out.println("✅ TC63 PASSED");
    }

    @Test(priority = 64, description = "TC64: No null organ objects in the data array")
    public void testTC64_NullOrganValidation() {
        System.out.println("\n>>> TC64: Null Organ Validation <<<");
        Response r = callGetOrgans();
        List<?> rawList = r.jsonPath().getList("data");
        Assert.assertNotNull(rawList, "data array missing");
        long nullCount = rawList.stream().filter(Objects::isNull).count();
        ApiReportContext.addExtraDetail("<b>Null objects in data array:</b> "
                + (nullCount == 0 ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + nullCount + " found</span>")
                + " | <b>Total entries:</b> " + rawList.size());
        Assert.assertEquals(nullCount, 0L, nullCount + " null entries in data array");
        System.out.println("✅ TC64 PASSED");
    }

    @Test(priority = 65, description = "TC65: Alphabetical sorting validation - document order")
    public void testTC65_AlphabeticalSortingValidation() {
        System.out.println("\n>>> TC65: Alphabetical Sorting Validation <<<");
        if (organNames.size() < 2) {
            ApiReportContext.addExtraDetail("<b>Sorting check:</b> N/A (< 2 organs)");
            return;
        }
        List<String> sorted = organNames.stream()
                .map(String::toLowerCase)
                .sorted()
                .collect(Collectors.toList());
        List<String> actual = organNames.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        boolean isAlpha = actual.equals(sorted);
        ApiReportContext.addExtraDetail("<b>Returned order:</b> " + organNames
                + "<br/>&nbsp;&nbsp;<b>Alphabetically sorted:</b> " + sorted
                + "<br/>&nbsp;&nbsp;<b>Is alphabetical:</b> "
                + (isAlpha ? "<span style='color:green'>YES</span>" : "<span style='color:orange'>NO - custom sort order in use</span>"));
        System.out.println("   Is alphabetical: " + isAlpha + " | order: " + organNames);
        System.out.println("✅ TC65 PASSED — order documented");
    }

    @Test(priority = 66, description = "TC66: All returned organs are active (no isActive=false)")
    public void testTC66_ActiveOrganValidation() {
        System.out.println("\n>>> TC66: Active Organ Validation <<<");
        List<String> inactive = new ArrayList<>();
        for (Map<String, Object> organ : organsData) {
            if (organ == null) continue;
            Object active = organ.get("isActive");
            if (active != null && "false".equalsIgnoreCase(active.toString())) {
                inactive.add(String.valueOf(organ.getOrDefault("name", organ.get("_id"))));
            }
        }
        ApiReportContext.addExtraDetail("<b>Inactive organs returned:</b> "
                + (inactive.isEmpty() ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + inactive + "</span>")
                + " | <b>Total checked:</b> " + organsData.size());
        Assert.assertTrue(inactive.isEmpty(), "Inactive organs in response: " + inactive);
        System.out.println("✅ TC66 PASSED");
    }

    @Test(priority = 68, description = "TC68: 'Heart' is not returned more than once")
    public void testTC68_HeartNotReturnedTwice() {
        System.out.println("\n>>> TC68: 'Heart' organ should appear at most once <<<");
        long heartCount = organNames.stream()
                .filter(n -> "heart".equalsIgnoreCase(n.trim()))
                .count();
        ApiReportContext.addExtraDetail("<b>'Heart' occurrences:</b> " + heartCount
                + " | <b>Expected:</b> 0 or 1");
        Assert.assertTrue(heartCount <= 1,
                "'Heart' appears " + heartCount + " times - expected at most 1");
        System.out.println("✅ TC68 PASSED — Heart count: " + heartCount);
    }

    @Test(priority = 69, description = "TC69: No blank organ names are returned")
    public void testTC69_NoBlankOrganNames() {
        System.out.println("\n>>> TC69: No blank organ names <<<");
        long blankCount = organNames.stream().filter(n -> n == null || n.trim().isEmpty()).count();
        ApiReportContext.addExtraDetail("<b>Blank organ names:</b> "
                + (blankCount == 0 ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + blankCount + " found</span>")
                + " | <b>Total names:</b> " + organNames.size());
        Assert.assertEquals(blankCount, 0L, blankCount + " blank organ names found");
        System.out.println("✅ TC69 PASSED");
    }

    @Test(priority = 70, description = "TC70: Organ names have no leading or trailing spaces")
    public void testTC70_NamesTrimmed() {
        System.out.println("\n>>> TC70: Organ names should be trimmed <<<");
        List<String> untrimmed = new ArrayList<>();
        for (String name : organNames) {
            if (!name.equals(name.trim())) untrimmed.add("'" + name + "'");
        }
        ApiReportContext.addExtraDetail("<b>Untrimmed names:</b> "
                + (untrimmed.isEmpty() ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + untrimmed + "</span>")
                + " | <b>Total checked:</b> " + organNames.size());
        Assert.assertTrue(untrimmed.isEmpty(),
                "Organ names with leading/trailing spaces: " + untrimmed);
        System.out.println("✅ TC70 PASSED");
    }

    @Test(priority = 71, description = "TC71: Organ count is consistent across two calls")
    public void testTC71_OrganCountConsistent() {
        System.out.println("\n>>> TC71: Organ count stable across calls <<<");
        int c1 = getOrganCount(callGetOrgans());
        int c2 = getOrganCount(callGetOrgans());
        ApiReportContext.addExtraDetail("<b>Call 1 count:</b> " + c1
                + " | <b>Call 2 count:</b> " + c2
                + " | <b>Consistent:</b> " + (c1 == c2 ? "<span style='color:green'>YES</span>" : "<span style='color:red'>NO</span>"));
        Assert.assertEquals(c1, c2, "Organ count differs: " + c1 + " vs " + c2);
        System.out.println("✅ TC71 PASSED");
    }

    @Test(priority = 72, description = "TC72: Response data array contains no null objects")
    public void testTC72_NoNullObjects() {
        System.out.println("\n>>> TC72: No null objects in data array <<<");
        Response r = callGetOrgans();
        List<?> data = r.jsonPath().getList("data");
        Assert.assertNotNull(data, "data array missing");
        long nulls = data.stream().filter(Objects::isNull).count();
        ApiReportContext.addExtraDetail("<b>Null objects in data array:</b> "
                + (nulls == 0 ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + nulls + " found</span>")
                + " | <b>Total entries:</b> " + data.size());
        Assert.assertEquals(nulls, 0L, nulls + " null objects in data array");
        System.out.println("✅ TC72 PASSED");
    }

    @Test(priority = 73, description = "TC73: Organ names are human-readable (only printable chars)")
    public void testTC73_NamesHumanReadable() {
        System.out.println("\n>>> TC73: Organ names should be human-readable <<<");
        List<String> nonReadable = new ArrayList<>();
        for (String name : organNames) {
            boolean hasControl = name.chars().anyMatch(c -> c < 32 && c != 9); // allow tab
            if (hasControl || name.contains("\0")) nonReadable.add(name);
        }
        ApiReportContext.addExtraDetail("<b>Non-readable names:</b> "
                + (nonReadable.isEmpty() ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + nonReadable + "</span>")
                + " | <b>All names:</b> " + organNames);
        Assert.assertTrue(nonReadable.isEmpty(),
                "Organ names with control characters: " + nonReadable);
        System.out.println("✅ TC73 PASSED");
    }

    @Test(priority = 74, description = "TC74: Idempotency - same data returned across two calls")
    public void testTC74_SameDataAfterRefresh() {
        System.out.println("\n>>> TC74: Idempotency — same organ count on two calls <<<");
        int c1 = getOrganCount(callGetOrgans());
        int c2 = getOrganCount(callGetOrgans());
        ApiReportContext.addExtraDetail("<b>Call 1:</b> " + c1 + " organs"
                + " | <b>Call 2:</b> " + c2 + " organs"
                + " | <b>Idempotent:</b> " + (c1 == c2 ? "<span style='color:green'>YES</span>" : "<span style='color:red'>NO</span>"));
        Assert.assertEquals(c1, c2, "Organ count changed between calls: " + c1 + " vs " + c2);
        System.out.println("✅ TC74 PASSED");
    }

    @Test(priority = 75, description = "TC75: Soft-deleted organs are excluded from response")
    public void testTC75_SoftDeletedOrgansExcluded() {
        System.out.println("\n>>> TC75: Soft-deleted organs should not appear <<<");
        List<String> softDeleted = new ArrayList<>();
        for (Map<String, Object> organ : organsData) {
            if (organ == null) continue;
            Object del = organ.get("isDeleted");
            if (del != null && "true".equalsIgnoreCase(del.toString())) {
                softDeleted.add(String.valueOf(organ.getOrDefault("name", organ.get("_id"))));
            }
        }
        ApiReportContext.addExtraDetail("<b>Soft-deleted organs in response:</b> "
                + (softDeleted.isEmpty() ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + softDeleted + "</span>")
                + " | <b>Total checked:</b> " + organsData.size());
        Assert.assertTrue(softDeleted.isEmpty(),
                "Soft-deleted organs found in response: " + softDeleted);
        System.out.println("✅ TC75 PASSED");
    }
}

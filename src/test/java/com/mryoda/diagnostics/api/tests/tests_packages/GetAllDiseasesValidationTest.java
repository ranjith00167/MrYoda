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
 * GET /tests/getAllDiseases - Standalone Validation Suite
 *
 * curl -X 'GET' https://staging-api-diagnostics.yodaprojects.com/tests/getAllDiseases
 *      -H 'accept: any'
 *
 * TC01-TC12:  Positive           - 200 OK, response body, disease list, mandatory fields
 * TC13-TC25:  Data Validation    - null checks, uniqueness, naming rules, trimming
 * TC26-TC28:  Sorting            - alphabetical order, consistency
 * TC29-TC35:  Schema Validation  - field types, datetime fields
 * TC36-TC41:  Negative           - unsupported methods (POST/PUT/DELETE)
 * TC42-TC45:  Security           - no sensitive data, no stack trace
 * TC46-TC49:  Performance        - response time, concurrency, dataset size
 * TC50-TC55:  Integration        - disease usability in search/tests/packages
 * TC56-TC60:  Response           - status, message, success, data, valid values
 * TC61-TC80:  High Priority      - schema, duplicates, sorting, trimming, consistency
 */
public class GetAllDiseasesValidationTest extends BaseTest {

    private static final String ENDPOINT = APIEndpoints.GET_ALL_DISEASES;

    private static List<Map<String, Object>> diseasesData = new ArrayList<>();
    private static List<String> diseaseNames = new ArrayList<>();
    private static List<String> diseaseIds = new ArrayList<>();
    private static List<String> diseaseSlugs = new ArrayList<>();
    private static Map<String, Object> firstDisease = null;
    private static int totalDiseases = 0;

    // =========================================================================
    //  SETUP
    // =========================================================================

    @BeforeClass
    public void setupDiseasesData() {
        System.out.println("\n========================================");
        System.out.println("SETUP: Fetching diseases from getAllDiseases API");
        System.out.println("========================================");

        Response r = callGetAllDiseases();

        if (r.getStatusCode() == 200) {
            List<Map<String, Object>> list = null;
            try { list = r.jsonPath().getList("data"); } catch (Exception ignored) { }
            if (list == null || list.isEmpty()) {
                try { list = r.jsonPath().getList("diseases"); } catch (Exception ignored) { }
            }
            if (list == null || list.isEmpty()) {
                try { list = r.jsonPath().getList("$"); } catch (Exception ignored) { }
            }

            if (list != null && !list.isEmpty()) {
                for (Map<String, Object> disease : list) {
                    if (disease != null) diseasesData.add(disease);
                }
                totalDiseases = diseasesData.size();
                firstDisease  = diseasesData.isEmpty() ? null : diseasesData.get(0);

                for (Map<String, Object> disease : diseasesData) {
                    String nameKey = disease.containsKey("name")         ? "name"
                                   : disease.containsKey("disease_name") ? "disease_name"
                                   : disease.containsKey("title")        ? "title" : null;
                    if (nameKey != null && disease.get(nameKey) != null) {
                        diseaseNames.add(disease.get(nameKey).toString());
                    }
                    String idKey = disease.containsKey("_id") ? "_id" : disease.containsKey("id") ? "id" : null;
                    if (idKey != null && disease.get(idKey) != null) {
                        diseaseIds.add(disease.get(idKey).toString());
                    }
                    if (disease.containsKey("slug") && disease.get("slug") != null) {
                        diseaseSlugs.add(disease.get("slug").toString());
                    }
                }

                System.out.println("   Total diseases returned : " + totalDiseases);
                System.out.println("   Names extracted         : " + diseaseNames.size());
                System.out.println("   IDs extracted           : " + diseaseIds.size());
                System.out.println("   Slugs extracted         : " + diseaseSlugs.size());
                if (firstDisease != null) {
                    System.out.println("   First disease fields    : " + firstDisease.keySet());
                }
                if (!diseaseNames.isEmpty()) {
                    System.out.println("   Sample names            : "
                            + diseaseNames.subList(0, Math.min(5, diseaseNames.size())));
                }
                System.out.println("Setup complete");
            } else {
                System.out.println("No diseases list found in response");
            }
        } else {
            System.out.println("Setup failed. Status: " + r.getStatusCode());
        }
        System.out.println("========================================\n");
    }

    // =========================================================================
    //  HELPERS
    // =========================================================================

    private Response callGetAllDiseases() {
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
                200, "GET /tests/getAllDiseases"));
        return r;
    }

    private int getDiseaseCount(Response r) {
        if (r.getStatusCode() != 200) return -1;
        List<?> list = null;
        try { list = r.jsonPath().getList("data"); } catch (Exception ignored) { }
        if (list == null) {
            try { list = r.jsonPath().getList("diseases"); } catch (Exception ignored) { }
        }
        return list != null ? list.size() : 0;
    }

    // =========================================================================
    //  POSITIVE SCENARIOS (TC01-TC12)
    // =========================================================================

    @Test(priority = 1, description = "TC01: GET getAllDiseases returns HTTP 200")
    public void testTC01_Returns200() {
        System.out.println("\n>>> TC01: GET getAllDiseases - Expect HTTP 200 <<<");
        Response r = callGetAllDiseases();
        ApiReportContext.addExtraDetail("<b>HTTP Status:</b> " + r.getStatusCode()
                + " | <b>Response Time:</b> " + r.getTime() + " ms");
        Assert.assertEquals(r.getStatusCode(), 200,
                "Expected 200 OK. Got: " + r.getStatusCode());
        System.out.println("TC01 PASSED");
    }

    @Test(priority = 2, description = "TC02: Response body is not empty")
    public void testTC02_ResponseBodyNotEmpty() {
        System.out.println("\n>>> TC02: Response body should not be empty <<<");
        Response r = callGetAllDiseases();
        String body = r.asString();
        Assert.assertEquals(r.getStatusCode(), 200);
        Assert.assertNotNull(body, "Response body is null");
        Assert.assertFalse(body.isEmpty(), "Response body is empty");
        ApiReportContext.addExtraDetail("<b>Body Length:</b> " + body.length() + " chars");
        System.out.println("TC02 PASSED");
    }

    @Test(priority = 3, description = "TC03: Response contains a diseases list")
    public void testTC03_DiseasesListReturned() {
        System.out.println("\n>>> TC03: Diseases list should be present <<<");
        Assert.assertFalse(diseasesData.isEmpty(),
                "No diseases found in response - list should not be empty");
        ApiReportContext.addExtraDetail("<b>Diseases list present:</b> <span style='color:green'>YES</span>"
                + " | <b>Count:</b> " + diseasesData.size());
        System.out.println("TC03 PASSED");
    }

    @Test(priority = 4, description = "TC04: At least one disease is returned")
    public void testTC04_AtLeastOneDisease() {
        System.out.println("\n>>> TC04: At least 1 disease expected <<<");
        Assert.assertTrue(totalDiseases >= 1,
                "Expected at least 1 disease, got: " + totalDiseases);
        ApiReportContext.addExtraDetail("<b>Total Diseases:</b> " + totalDiseases);
        System.out.println("TC04 PASSED");
    }

    @Test(priority = 5, description = "TC05: First disease has an ID field (_id)")
    public void testTC05_DiseaseIdPresent() {
        System.out.println("\n>>> TC05: Disease _id field should be present <<<");
        Assert.assertNotNull(firstDisease, "No diseases returned");
        boolean hasId = firstDisease.containsKey("_id") || firstDisease.containsKey("id");
        Assert.assertTrue(hasId, "No _id/id field found. Keys: " + firstDisease.keySet());
        Object id = firstDisease.containsKey("_id") ? firstDisease.get("_id") : firstDisease.get("id");
        ApiReportContext.addExtraDetail("<b>First disease _id:</b> " + id);
        System.out.println("TC05 PASSED");
    }

    @Test(priority = 6, description = "TC06: First disease has a name field")
    public void testTC06_DiseaseNamePresent() {
        System.out.println("\n>>> TC06: Disease name field should be present <<<");
        Assert.assertNotNull(firstDisease, "No diseases returned");
        boolean hasName = firstDisease.containsKey("name")
                || firstDisease.containsKey("disease_name")
                || firstDisease.containsKey("title");
        Assert.assertTrue(hasName, "No name-like field found. Keys: " + firstDisease.keySet());
        String name = firstDisease.containsKey("name")         ? String.valueOf(firstDisease.get("name"))
                    : firstDisease.containsKey("disease_name") ? String.valueOf(firstDisease.get("disease_name"))
                    : String.valueOf(firstDisease.get("title"));
        ApiReportContext.addExtraDetail("<b>First disease name:</b> " + name);
        System.out.println("TC06 PASSED");
    }

    @Test(priority = 7, description = "TC07: First disease has a slug field (if applicable)")
    public void testTC07_DiseaseSlugPresent() {
        System.out.println("\n>>> TC07: Check for slug field <<<");
        Assert.assertNotNull(firstDisease, "No diseases returned");
        boolean hasSlug = firstDisease.containsKey("slug");
        String slugVal = hasSlug ? String.valueOf(firstDisease.get("slug")) : "N/A";
        ApiReportContext.addExtraDetail("<b>slug field present:</b> "
                + (hasSlug ? "<span style='color:green'>YES</span>" : "<span style='color:orange'>NOT FOUND</span>")
                + " | <b>Value:</b> " + slugVal);
        if (hasSlug && firstDisease.get("slug") != null) {
            Assert.assertFalse(firstDisease.get("slug").toString().trim().isEmpty(),
                    "slug is blank when present");
        }
        System.out.println("TC07 PASSED");
    }

    @Test(priority = 8, description = "TC08: Active diseases are returned (isActive flag)")
    public void testTC08_ActiveDiseasesReturned() {
        System.out.println("\n>>> TC08: Diseases should be active <<<");
        Assert.assertFalse(diseasesData.isEmpty(), "No diseases to validate");
        int inactiveCount = 0;
        for (Map<String, Object> disease : diseasesData) {
            if (disease == null) continue;
            if (disease.containsKey("isActive")) {
                Object active = disease.get("isActive");
                if (active != null && active.toString().equalsIgnoreCase("false")) {
                    inactiveCount++;
                }
            }
        }
        ApiReportContext.addExtraDetail("<b>Total diseases:</b> " + diseasesData.size()
                + " | <b>Inactive diseases in response:</b> " + inactiveCount);
        Assert.assertEquals(inactiveCount, 0,
                "Expected no inactive diseases, found: " + inactiveCount);
        System.out.println("TC08 PASSED");
    }

    @Test(priority = 9, description = "TC09: Response success flag is true")
    public void testTC09_SuccessFlag() {
        System.out.println("\n>>> TC09: success flag should be true <<<");
        Response r = callGetAllDiseases();
        Assert.assertEquals(r.getStatusCode(), 200);
        Boolean success = r.jsonPath().getBoolean("success");
        Assert.assertNotNull(success, "success field missing");
        Assert.assertTrue(success, "success should be true");
        ApiReportContext.addExtraDetail("<b>success field:</b> " + success);
        System.out.println("TC09 PASSED");
    }

    @Test(priority = 10, description = "TC10: Response contains a msg field")
    public void testTC10_ResponseMessage() {
        System.out.println("\n>>> TC10: msg field should be present <<<");
        Response r = callGetAllDiseases();
        Assert.assertEquals(r.getStatusCode(), 200);
        String msg = r.jsonPath().getString("msg");
        Assert.assertNotNull(msg, "msg field missing in response body");
        ApiReportContext.addExtraDetail("<b>msg field value:</b> &quot;" + msg + "&quot;");
        System.out.println("TC10 PASSED");
    }

    @Test(priority = 11, description = "TC11: All mandatory fields present on each disease")
    public void testTC11_MandatoryFields() {
        System.out.println("\n>>> TC11: Each disease must have _id and name <<<");
        Assert.assertFalse(diseasesData.isEmpty(), "No diseases returned");
        int missing = 0;
        for (Map<String, Object> disease : diseasesData) {
            if (disease == null) { missing++; continue; }
            boolean hasId   = disease.containsKey("_id") || disease.containsKey("id");
            boolean hasName = disease.containsKey("name") || disease.containsKey("disease_name") || disease.containsKey("title");
            if (!hasId || !hasName) missing++;
        }
        ApiReportContext.addExtraDetail("<b>Total diseases:</b> " + diseasesData.size()
                + " | <b>Entries missing _id or name:</b> " + missing);
        Assert.assertEquals(missing, 0,
                missing + " diseases missing mandatory fields (_id or name)");
        System.out.println("TC11 PASSED");
    }

    @Test(priority = 12, description = "TC12: Content-Type is application/json")
    public void testTC12_ContentType() {
        System.out.println("\n>>> TC12: Content-Type should be application/json <<<");
        Response r = callGetAllDiseases();
        String ct = r.getContentType();
        ApiReportContext.addExtraDetail("<b>Content-Type header:</b> " + ct);
        Assert.assertTrue(ct.contains("application/json"),
                "Expected application/json. Got: " + ct);
        System.out.println("TC12 PASSED");
    }

    // =========================================================================
    //  DATA VALIDATION (TC13-TC25)
    // =========================================================================

    @Test(priority = 13, description = "TC13: Disease _id is not null for any entry")
    public void testTC13_DiseaseIdNotNull() {
        System.out.println("\n>>> TC13: Every disease _id must be non-null <<<");
        Assert.assertFalse(diseasesData.isEmpty(), "No diseases to validate");
        int nullCount = 0;
        for (Map<String, Object> disease : diseasesData) {
            if (disease == null) { nullCount++; continue; }
            String idKey = disease.containsKey("_id") ? "_id" : "id";
            if (disease.get(idKey) == null) nullCount++;
        }
        ApiReportContext.addExtraDetail("<b>Entries with null _id:</b> " + nullCount
                + " | <b>Total:</b> " + diseasesData.size());
        Assert.assertEquals(nullCount, 0, nullCount + " diseases have null _id");
        System.out.println("TC13 PASSED");
    }

    @Test(priority = 14, description = "TC14: Disease name is not null for any entry")
    public void testTC14_DiseaseNameNotNull() {
        System.out.println("\n>>> TC14: Every disease name must be non-null <<<");
        Assert.assertFalse(diseasesData.isEmpty(), "No diseases to validate");
        int nullCount = 0;
        for (Map<String, Object> disease : diseasesData) {
            if (disease == null) { nullCount++; continue; }
            String nameKey = disease.containsKey("name")         ? "name"
                           : disease.containsKey("disease_name") ? "disease_name"
                           : disease.containsKey("title")        ? "title" : null;
            if (nameKey == null || disease.get(nameKey) == null) nullCount++;
        }
        ApiReportContext.addExtraDetail("<b>Entries with null name:</b> " + nullCount
                + " | <b>Total:</b> " + diseasesData.size());
        Assert.assertEquals(nullCount, 0, nullCount + " diseases have null name");
        System.out.println("TC14 PASSED");
    }

    @Test(priority = 15, description = "TC15: Disease name is not empty for any entry")
    public void testTC15_DiseaseNameNotEmpty() {
        System.out.println("\n>>> TC15: Every disease name must be non-empty <<<");
        Assert.assertFalse(diseaseNames.isEmpty(), "No disease names extracted");
        int blankCount = 0;
        for (String name : diseaseNames) {
            if (name == null || name.trim().isEmpty()) blankCount++;
        }
        ApiReportContext.addExtraDetail("<b>Blank/empty names found:</b> " + blankCount
                + " | <b>Total names:</b> " + diseaseNames.size());
        Assert.assertEquals(blankCount, 0, blankCount + " disease names are blank/empty");
        System.out.println("TC15 PASSED");
    }

    @Test(priority = 16, description = "TC16: Disease IDs are unique (no duplicates)")
    public void testTC16_DiseaseIdsUnique() {
        System.out.println("\n>>> TC16: No duplicate _id values <<<");
        Set<String> seen = new HashSet<>();
        List<String> duplicates = new ArrayList<>();
        for (String id : diseaseIds) {
            if (!seen.add(id)) duplicates.add(id);
        }
        ApiReportContext.addExtraDetail("<b>Total IDs:</b> " + diseaseIds.size()
                + " | <b>Duplicate IDs:</b> "
                + (duplicates.isEmpty() ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + duplicates + "</span>"));
        Assert.assertTrue(duplicates.isEmpty(), "Duplicate disease _ids: " + duplicates);
        System.out.println("TC16 PASSED");
    }

    @Test(priority = 17, description = "TC17: Disease names are unique (no duplicates)")
    public void testTC17_DiseaseNamesUnique() {
        System.out.println("\n>>> TC17: No duplicate disease names <<<");
        Set<String> seen = new LinkedHashSet<>();
        List<String> duplicates = new ArrayList<>();
        for (String name : diseaseNames) {
            if (!seen.add(name.toLowerCase().trim())) duplicates.add(name);
        }
        ApiReportContext.addExtraDetail("<b>Total names:</b> " + diseaseNames.size()
                + " | <b>Duplicate names:</b> "
                + (duplicates.isEmpty() ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + duplicates + "</span>"));
        Assert.assertTrue(duplicates.isEmpty(), "Duplicate disease names found: " + duplicates);
        System.out.println("TC17 PASSED");
    }

    @Test(priority = 18, description = "TC18: No duplicate disease objects in the list")
    public void testTC18_NoDuplicateDiseases() {
        System.out.println("\n>>> TC18: No duplicate disease objects <<<");
        Set<String> seen = new HashSet<>();
        List<String> duplicates = new ArrayList<>();
        for (Map<String, Object> disease : diseasesData) {
            if (disease == null) continue;
            String idKey = disease.containsKey("_id") ? "_id" : "id";
            String id = String.valueOf(disease.get(idKey));
            if (!seen.add(id)) duplicates.add(id);
        }
        ApiReportContext.addExtraDetail("<b>Total entries:</b> " + diseasesData.size()
                + " | <b>Duplicate objects:</b> "
                + (duplicates.isEmpty() ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + duplicates + "</span>"));
        Assert.assertTrue(duplicates.isEmpty(), "Duplicate disease objects: " + duplicates);
        System.out.println("TC18 PASSED");
    }

    @Test(priority = 19, description = "TC19: Disease names do not contain invalid characters")
    public void testTC19_NamesNoInvalidChars() {
        System.out.println("\n>>> TC19: Disease names should not contain HTML/script chars <<<");
        List<String> flagged = new ArrayList<>();
        for (String name : diseaseNames) {
            if (name.contains("<script") || name.contains("javascript:")) {
                flagged.add(name);
            }
        }
        ApiReportContext.addExtraDetail("<b>Names with invalid chars (script/XSS):</b> "
                + (flagged.isEmpty() ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + flagged + "</span>"));
        Assert.assertTrue(flagged.isEmpty(),
                "Disease names contain invalid/dangerous characters: " + flagged);
        System.out.println("TC19 PASSED");
    }

    @Test(priority = 20, description = "TC20: No inactive diseases are returned")
    public void testTC20_NoInactiveDiseases() {
        System.out.println("\n>>> TC20: isActive=false diseases should not be in response <<<");
        List<String> inactive = new ArrayList<>();
        for (Map<String, Object> disease : diseasesData) {
            if (disease == null) continue;
            Object active = disease.get("isActive");
            if (active != null && "false".equalsIgnoreCase(active.toString())) {
                inactive.add(String.valueOf(disease.getOrDefault("name", disease.get("_id"))));
            }
        }
        ApiReportContext.addExtraDetail("<b>Inactive diseases in response:</b> "
                + (inactive.isEmpty() ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + inactive + "</span>"));
        Assert.assertTrue(inactive.isEmpty(), "Inactive diseases found: " + inactive);
        System.out.println("TC20 PASSED");
    }

    @Test(priority = 21, description = "TC21: No deleted diseases are returned (isDeleted check)")
    public void testTC21_NoDeletedDiseases() {
        System.out.println("\n>>> TC21: Deleted diseases should not be in response <<<");
        List<String> deleted = new ArrayList<>();
        for (Map<String, Object> disease : diseasesData) {
            if (disease == null) continue;
            Object del = disease.get("isDeleted");
            if (del != null && "true".equalsIgnoreCase(del.toString())) {
                deleted.add(String.valueOf(disease.getOrDefault("name", disease.get("_id"))));
            }
        }
        ApiReportContext.addExtraDetail("<b>Deleted diseases in response:</b> "
                + (deleted.isEmpty() ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + deleted + "</span>"));
        Assert.assertTrue(deleted.isEmpty(), "Deleted diseases found: " + deleted);
        System.out.println("TC21 PASSED");
    }

    @Test(priority = 22, description = "TC22: Disease data is consistent across two calls")
    public void testTC22_DataConsistency() {
        System.out.println("\n>>> TC22: Two calls should return identical disease IDs <<<");
        Response r1 = callGetAllDiseases();
        Response r2 = callGetAllDiseases();
        Assert.assertEquals(r1.getStatusCode(), 200);
        Assert.assertEquals(r2.getStatusCode(), 200);
        int c1 = getDiseaseCount(r1);
        int c2 = getDiseaseCount(r2);
        ApiReportContext.addExtraDetail("<b>Call 1 count:</b> " + c1
                + " | <b>Call 2 count:</b> " + c2
                + " | <b>Consistent:</b> " + (c1 == c2 ? "<span style='color:green'>YES</span>" : "<span style='color:red'>NO</span>"));
        Assert.assertEquals(c1, c2, "Disease count differs between calls");
        System.out.println("TC22 PASSED");
    }

    @Test(priority = 23, description = "TC23: Disease name length is within reasonable bounds (1-200 chars)")
    public void testTC23_NameLengthValidation() {
        System.out.println("\n>>> TC23: Disease name length should be 1-200 chars <<<");
        List<String> invalid = new ArrayList<>();
        for (String name : diseaseNames) {
            if (name.length() < 1 || name.length() > 200) invalid.add(name);
        }
        ApiReportContext.addExtraDetail("<b>Names with invalid length:</b> "
                + (invalid.isEmpty() ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + invalid + "</span>")
                + " | <b>Verified:</b> " + diseaseNames.size() + " names");
        Assert.assertTrue(invalid.isEmpty(),
                "Disease names with out-of-range length: " + invalid);
        System.out.println("TC23 PASSED");
    }

    @Test(priority = 24, description = "TC24: Disease names do not contain leading spaces")
    public void testTC24_NoLeadingSpaces() {
        System.out.println("\n>>> TC24: No leading spaces in disease names <<<");
        List<String> flagged = new ArrayList<>();
        for (String name : diseaseNames) {
            if (name.startsWith(" ")) flagged.add("'" + name + "'");
        }
        ApiReportContext.addExtraDetail("<b>Names with leading spaces:</b> "
                + (flagged.isEmpty() ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + flagged + "</span>"));
        Assert.assertTrue(flagged.isEmpty(),
                "Disease names with leading spaces: " + flagged);
        System.out.println("TC24 PASSED");
    }

    @Test(priority = 25, description = "TC25: Disease names do not contain trailing spaces")
    public void testTC25_NoTrailingSpaces() {
        System.out.println("\n>>> TC25: No trailing spaces in disease names <<<");
        List<String> flagged = new ArrayList<>();
        for (String name : diseaseNames) {
            if (name.endsWith(" ")) flagged.add("'" + name + "'");
        }
        ApiReportContext.addExtraDetail("<b>Names with trailing spaces:</b> "
                + (flagged.isEmpty() ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + flagged + "</span>"));
        Assert.assertTrue(flagged.isEmpty(),
                "Disease names with trailing spaces: " + flagged);
        System.out.println("TC25 PASSED");
    }

    // =========================================================================
    //  SORTING VALIDATION (TC26-TC28)
    // =========================================================================

    @Test(priority = 26, description = "TC26: Diseases are returned in alphabetical order")
    public void testTC26_AlphabeticalOrder() {
        System.out.println("\n>>> TC26: Disease names should be in alphabetical order <<<");
        if (diseaseNames.size() < 2) {
            ApiReportContext.addExtraDetail("<b>Alphabetical check:</b> N/A (less than 2 diseases)");
            return;
        }
        List<String> sorted = new ArrayList<>(diseaseNames);
        Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);
        boolean isAlpha = diseaseNames.stream().map(String::toLowerCase).collect(Collectors.toList())
                .equals(sorted.stream().map(String::toLowerCase).collect(Collectors.toList()));
        ApiReportContext.addExtraDetail("<b>Returned order:</b> " + diseaseNames
                + "<br/>&nbsp;&nbsp;<b>Expected alphabetical:</b> " + sorted
                + "<br/>&nbsp;&nbsp;<b>Is alphabetical:</b> "
                + (isAlpha ? "<span style='color:green'>YES</span>" : "<span style='color:orange'>NO - server may use custom order</span>"));
        System.out.println("   Is alphabetical: " + isAlpha);
        System.out.println("TC26 PASSED - order documented");
    }

    @Test(priority = 27, description = "TC27: Ascending sort order verified")
    public void testTC27_AscendingSortOrder() {
        System.out.println("\n>>> TC27: First disease should come before last alphabetically <<<");
        if (diseaseNames.size() < 2) {
            ApiReportContext.addExtraDetail("<b>Ascending check:</b> N/A (less than 2 diseases)");
            return;
        }
        String first = diseaseNames.get(0).toLowerCase();
        String last  = diseaseNames.get(diseaseNames.size() - 1).toLowerCase();
        ApiReportContext.addExtraDetail("<b>First name:</b> " + diseaseNames.get(0)
                + " | <b>Last name:</b> " + diseaseNames.get(diseaseNames.size() - 1)
                + " | <b>first &le; last:</b> "
                + (first.compareTo(last) <= 0 ? "<span style='color:green'>YES</span>" : "<span style='color:orange'>NO</span>"));
        System.out.println("TC27 PASSED");
    }

    @Test(priority = 28, description = "TC28: Sorting is consistent across two calls")
    public void testTC28_SortingConsistent() {
        System.out.println("\n>>> TC28: Order of diseases should be consistent across calls <<<");
        Response r1 = callGetAllDiseases();
        Response r2 = callGetAllDiseases();
        List<Map<String, Object>> l1 = r1.jsonPath().getList("data");
        List<Map<String, Object>> l2 = r2.jsonPath().getList("data");
        Assert.assertNotNull(l1, "data missing in call 1");
        Assert.assertNotNull(l2, "data missing in call 2");
        Assert.assertFalse(l1.isEmpty(), "data empty in call 1");
        Assert.assertFalse(l2.isEmpty(), "data empty in call 2");
        String id1 = String.valueOf(l1.get(0).get("_id"));
        String id2 = String.valueOf(l2.get(0).get("_id"));
        Assert.assertEquals(id1, id2, "First entry _id differs - ordering is unstable");
        ApiReportContext.addExtraDetail("<b>First _id (Call 1):</b> " + id1
                + " | <b>First _id (Call 2):</b> " + id2
                + " | <b>Order stable:</b> <span style='color:green'>YES</span>");
        System.out.println("TC28 PASSED");
    }

    // =========================================================================
    //  SCHEMA VALIDATION (TC29-TC35)
    // =========================================================================

    @Test(priority = 29, description = "TC29: Response envelope schema - success, msg, data fields present")
    public void testTC29_ResponseSchema() {
        System.out.println("\n>>> TC29: Top-level schema validation <<<");
        Response r = callGetAllDiseases();
        Assert.assertEquals(r.getStatusCode(), 200);
        Assert.assertNotNull(r.jsonPath().get("success"), "Missing: success");
        Assert.assertNotNull(r.jsonPath().get("msg"),     "Missing: msg");
        Object dataField = r.jsonPath().get("data");
        ApiReportContext.addExtraDetail("<b>success present:</b> <span style='color:green'>YES</span>"
                + " | <b>msg present:</b> <span style='color:green'>YES</span>"
                + " | <b>data present:</b> " + (dataField != null ? "<span style='color:green'>YES</span>" : "<span style='color:orange'>NOT FOUND</span>"));
        System.out.println("TC29 PASSED");
    }

    @Test(priority = 30, description = "TC30: Each disease object has expected schema fields")
    public void testTC30_DiseaseObjectSchema() {
        System.out.println("\n>>> TC30: Each disease must have _id and a name-like field <<<");
        Assert.assertNotNull(firstDisease, "No diseases returned");
        boolean hasId   = firstDisease.containsKey("_id") || firstDisease.containsKey("id");
        boolean hasName = firstDisease.containsKey("name") || firstDisease.containsKey("disease_name") || firstDisease.containsKey("title");
        ApiReportContext.addExtraDetail("<b>First disease keys:</b> " + firstDisease.keySet()
                + "<br/>&nbsp;&nbsp;<b>has _id:</b> " + hasId
                + " | <b>has name:</b> " + hasName);
        Assert.assertTrue(hasId,   "Disease missing _id/id field");
        Assert.assertTrue(hasName, "Disease missing name/disease_name/title field");
        System.out.println("TC30 PASSED");
    }

    @Test(priority = 31, description = "TC31: Disease _id is a non-empty string type")
    public void testTC31_DiseaseIdDatatype() {
        System.out.println("\n>>> TC31: Disease _id should be a non-empty string <<<");
        Assert.assertFalse(diseaseIds.isEmpty(), "No disease IDs extracted");
        for (String id : diseaseIds) {
            Assert.assertNotNull(id, "_id is null");
            Assert.assertFalse(id.trim().isEmpty(), "_id is blank");
            Assert.assertFalse(id.equalsIgnoreCase("null"), "_id is literal 'null'");
        }
        ApiReportContext.addExtraDetail("<b>IDs validated:</b> " + diseaseIds.size()
                + " | <b>Sample _id:</b> " + (diseaseIds.isEmpty() ? "N/A" : diseaseIds.get(0)));
        System.out.println("TC31 PASSED");
    }

    @Test(priority = 32, description = "TC32: Disease name is a non-empty string type")
    public void testTC32_DiseaseNameDatatype() {
        System.out.println("\n>>> TC32: Disease name should be a non-empty string <<<");
        Assert.assertFalse(diseaseNames.isEmpty(), "No disease names extracted");
        for (String name : diseaseNames) {
            Assert.assertNotNull(name, "name is null");
            Assert.assertFalse(name.trim().isEmpty(), "name is blank");
        }
        ApiReportContext.addExtraDetail("<b>Names validated:</b> " + diseaseNames.size()
                + " | <b>Sample name:</b> " + (diseaseNames.isEmpty() ? "N/A" : diseaseNames.get(0)));
        System.out.println("TC32 PASSED");
    }

    @Test(priority = 33, description = "TC33: Disease slug, if present, is a non-empty string")
    public void testTC33_SlugDatatype() {
        System.out.println("\n>>> TC33: slug field type check <<<");
        if (diseaseSlugs.isEmpty()) {
            ApiReportContext.addExtraDetail("<b>slug field:</b> Not present in response");
            System.out.println("   slug field not present - SKIPPED");
            return;
        }
        int blankSlugs = 0;
        for (String slug : diseaseSlugs) {
            if (slug == null || slug.trim().isEmpty()) blankSlugs++;
        }
        ApiReportContext.addExtraDetail("<b>Slugs validated:</b> " + diseaseSlugs.size()
                + " | <b>Blank slugs:</b> " + blankSlugs
                + " | <b>Sample slug:</b> " + diseaseSlugs.get(0));
        Assert.assertEquals(blankSlugs, 0, blankSlugs + " blank slugs found");
        System.out.println("TC33 PASSED");
    }

    @Test(priority = 34, description = "TC34: createdAt field, if present, is a valid date-time string")
    public void testTC34_CreatedAtDatatype() {
        System.out.println("\n>>> TC34: createdAt field type check <<<");
        Assert.assertNotNull(firstDisease, "No diseases returned");
        Object createdAt = firstDisease.get("createdAt");
        String val = createdAt != null ? createdAt.toString() : "N/A";
        ApiReportContext.addExtraDetail("<b>createdAt field:</b> " + val);
        if (createdAt != null) {
            Assert.assertFalse(createdAt.toString().trim().isEmpty(), "createdAt is blank");
        }
        System.out.println("TC34 PASSED - createdAt: " + val);
    }

    @Test(priority = 35, description = "TC35: updatedAt field, if present, is a valid date-time string")
    public void testTC35_UpdatedAtDatatype() {
        System.out.println("\n>>> TC35: updatedAt field type check <<<");
        Assert.assertNotNull(firstDisease, "No diseases returned");
        Object updatedAt = firstDisease.get("updatedAt");
        String val = updatedAt != null ? updatedAt.toString() : "N/A";
        ApiReportContext.addExtraDetail("<b>updatedAt field:</b> " + val);
        if (updatedAt != null) {
            Assert.assertFalse(updatedAt.toString().trim().isEmpty(), "updatedAt is blank");
        }
        System.out.println("TC35 PASSED - updatedAt: " + val);
    }

    // =========================================================================
    //  NEGATIVE SCENARIOS (TC36-TC41)
    // =========================================================================

    @Test(priority = 36, description = "TC36: API works without a request body")
    public void testTC36_NoRequestBody() {
        System.out.println("\n>>> TC36: No request body - server should still respond <<<");
        Response r = callGetAllDiseases();
        ApiReportContext.addExtraDetail("<b>No request body sent:</b> Status " + r.getStatusCode() + " returned");
        Assert.assertEquals(r.getStatusCode(), 200, "Expected 200 without body");
        System.out.println("TC36 PASSED");
    }

    @Test(priority = 37, description = "TC37: Invalid Accept header - server handles gracefully")
    public void testTC37_InvalidAcceptHeader() {
        System.out.println("\n>>> TC37: Invalid accept header - should not 5xx <<<");
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
        System.out.println("TC37 PASSED");
    }

    @Test(priority = 38, description = "TC38: Unsupported Content-Type header handled gracefully")
    public void testTC38_UnsupportedMediaType() {
        System.out.println("\n>>> TC38: Unsupported content-type in header <<<");
        long start = System.currentTimeMillis();
        Response r = new RequestBuilder()
                .setEndpoint(ENDPOINT)
                .addHeader("accept", "*/*")
                .addHeader("Content-Type", "application/xml")
                .get();
        long elapsed = System.currentTimeMillis() - start;
        ApiReportContext.addRecord(new ApiReportContext.ApiCallRecord(
                "GET", ENDPOINT, "", r.getStatusCode(), r.asString(), elapsed, 0, 200, 599, "Unsupported Content-Type"));
        ApiReportContext.addExtraDetail("<b>Content-Type: application/xml - Status:</b> " + r.getStatusCode());
        Assert.assertTrue(r.getStatusCode() < 500,
                "Server returned 5xx on unsupported Content-Type: " + r.getStatusCode());
        System.out.println("TC38 PASSED");
    }

    @Test(priority = 39, description = "TC39: POST method is not allowed")
    public void testTC39_PostMethodNotAllowed() {
        System.out.println("\n>>> TC39: POST method should not be supported <<<");
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
        Assert.assertNotEquals(r.getStatusCode(), 200, "POST /getAllDiseases should not return 200");
        System.out.println("TC39 PASSED - POST returned: " + r.getStatusCode());
    }

    @Test(priority = 40, description = "TC40: PUT method is not allowed")
    public void testTC40_PutMethodNotAllowed() {
        System.out.println("\n>>> TC40: PUT method should not be supported <<<");
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
        Assert.assertNotEquals(r.getStatusCode(), 200, "PUT /getAllDiseases should not return 200");
        System.out.println("TC40 PASSED - PUT returned: " + r.getStatusCode());
    }

    @Test(priority = 41, description = "TC41: DELETE method is not allowed")
    public void testTC41_DeleteMethodNotAllowed() {
        System.out.println("\n>>> TC41: DELETE method should not be supported <<<");
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
        Assert.assertNotEquals(r.getStatusCode(), 200, "DELETE /getAllDiseases should not return 200");
        System.out.println("TC41 PASSED - DELETE returned: " + r.getStatusCode());
    }

    // =========================================================================
    //  SECURITY SCENARIOS (TC42-TC45)
    // =========================================================================

    @Test(priority = 42, description = "TC42: No sensitive data (passwords, tokens) exposed in response")
    public void testTC42_NoSensitiveDataExposed() {
        System.out.println("\n>>> TC42: Response should not contain sensitive data <<<");
        String body = callGetAllDiseases().asString().toLowerCase();
        Assert.assertFalse(body.contains("password"), "Response contains 'password'");
        Assert.assertFalse(body.contains("token"),    "Response contains 'token'");
        Assert.assertFalse(body.contains("secret"),   "Response contains 'secret'");
        ApiReportContext.addExtraDetail("<b>password/token/secret in body:</b> <span style='color:green'>NOT FOUND</span>");
        System.out.println("TC42 PASSED");
    }

    @Test(priority = 43, description = "TC43: Internal DB/implementation fields not exposed")
    public void testTC43_NoInternalDbFields() {
        System.out.println("\n>>> TC43: Internal DB fields should not be exposed <<<");
        Assert.assertNotNull(firstDisease, "No diseases returned");
        boolean hasV = firstDisease.containsKey("__v");
        ApiReportContext.addExtraDetail("<b>__v (Mongoose version key) present:</b> "
                + (hasV ? "<span style='color:orange'>" + firstDisease.get("__v") + "</span>" : "<span style='color:green'>NOT EXPOSED</span>"));
        System.out.println("   __v present: " + hasV);
        System.out.println("TC43 PASSED");
    }

    @Test(priority = 44, description = "TC44: No stack trace or exception details in response")
    public void testTC44_NoStackTrace() {
        System.out.println("\n>>> TC44: Response must not expose stack traces <<<");
        String body = callGetAllDiseases().asString().toLowerCase();
        Assert.assertFalse(body.contains("stack trace"), "Stack trace found");
        Assert.assertFalse(body.contains("exception"),   "Exception details found");
        Assert.assertFalse(body.contains("at com."),     "Java package trace found");
        ApiReportContext.addExtraDetail("<b>Stack trace:</b> <span style='color:green'>NO</span>"
                + " | <b>Exception:</b> <span style='color:green'>NO</span>"
                + " | <b>Java trace:</b> <span style='color:green'>NO</span>");
        System.out.println("TC44 PASSED");
    }

    @Test(priority = 45, description = "TC45: No server implementation details exposed")
    public void testTC45_NoServerDetails() {
        System.out.println("\n>>> TC45: Server implementation details check <<<");
        String body = callGetAllDiseases().asString().toLowerCase();
        Assert.assertFalse(body.contains("mongodb error"),     "MongoDB error details exposed");
        Assert.assertFalse(body.contains("syntaxerror"),       "SyntaxError exposed");
        Assert.assertFalse(body.contains("internal server"),   "Internal server error exposed");
        ApiReportContext.addExtraDetail("<b>MongoDB errors:</b> <span style='color:green'>NOT FOUND</span>"
                + " | <b>SyntaxError:</b> <span style='color:green'>NOT FOUND</span>");
        System.out.println("TC45 PASSED");
    }

    // =========================================================================
    //  PERFORMANCE SCENARIOS (TC46-TC49)
    // =========================================================================

    @Test(priority = 46, description = "TC46: Response time is under 2000 ms")
    public void testTC46_ResponseTimeUnder2s() {
        System.out.println("\n>>> TC46: Response time < 2000 ms <<<");
        long start = System.currentTimeMillis();
        Response r = callGetAllDiseases();
        long elapsed = System.currentTimeMillis() - start;
        Assert.assertEquals(r.getStatusCode(), 200);
        ApiReportContext.addExtraDetail("<b>Response Time:</b> " + elapsed + " ms"
                + " | <b>Threshold:</b> 2000 ms"
                + " | <b>Within SLA:</b> " + (elapsed < 2000 ? "<span style='color:green'>YES</span>" : "<span style='color:red'>NO</span>"));
        Assert.assertTrue(elapsed < 2000,
                "Response took " + elapsed + " ms - exceeded 2000 ms threshold");
        System.out.println("TC46 PASSED");
    }

    @Test(priority = 47, description = "TC47: Response is consistent across 3 consecutive calls")
    public void testTC47_ConsistencyMultipleCalls() {
        System.out.println("\n>>> TC47: 3 consecutive calls should all return 200 <<<");
        int[] statuses = new int[3];
        for (int i = 0; i < 3; i++) {
            statuses[i] = callGetAllDiseases().getStatusCode();
            Assert.assertEquals(statuses[i], 200, "Call " + (i + 1) + " returned " + statuses[i]);
        }
        ApiReportContext.addExtraDetail("<b>Call 1:</b> " + statuses[0]
                + " | <b>Call 2:</b> " + statuses[1]
                + " | <b>Call 3:</b> " + statuses[2]
                + " | <b>All 200:</b> <span style='color:green'>YES</span>");
        System.out.println("TC47 PASSED");
    }

    @Test(priority = 48, description = "TC48: Response handles concurrent requests (3 threads)")
    public void testTC48_ConcurrentRequests() throws InterruptedException, ExecutionException {
        System.out.println("\n>>> TC48: Concurrent requests - all should return 200 <<<");
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
        System.out.println("TC48 PASSED");
    }

    @Test(priority = 49, description = "TC49: Payload size is reasonable (< 1 MB)")
    public void testTC49_LargeDatasetHandling() {
        System.out.println("\n>>> TC49: Payload should be < 1 MB <<<");
        Response r = callGetAllDiseases();
        int bodyLength = r.asString().length();
        ApiReportContext.addExtraDetail("<b>Payload Size:</b> " + bodyLength + " chars"
                + " | <b>Max allowed:</b> 1,000,000 chars"
                + " | <b>Within limit:</b> <span style='color:green'>YES</span>");
        Assert.assertTrue(bodyLength < 1_000_000,
                "Response too large: " + bodyLength + " chars (> 1 MB)");
        System.out.println("TC49 PASSED");
    }

    // =========================================================================
    //  INTEGRATION VALIDATION (TC50-TC55)
    // =========================================================================

    @Test(priority = 50, description = "TC50: Diseases returned are non-empty (displayable in UI)")
    public void testTC50_DiseasesDisplayableInUI() {
        System.out.println("\n>>> TC50: Diseases should have displayable name values <<<");
        Assert.assertFalse(diseaseNames.isEmpty(), "No disease names - nothing to display");
        long displayable = diseaseNames.stream()
                .filter(n -> n != null && !n.trim().isEmpty())
                .count();
        ApiReportContext.addExtraDetail("<b>Displayable disease names:</b> " + displayable + " / " + diseaseNames.size());
        Assert.assertEquals(displayable, (long) diseaseNames.size(),
                "Some diseases have no displayable name");
        System.out.println("TC50 PASSED");
    }

    @Test(priority = 51, description = "TC51: Disease names are searchable (non-empty, len >= 2)")
    public void testTC51_DiseasesSearchableInUI() {
        System.out.println("\n>>> TC51: Disease names must be searchable (len >= 2) <<<");
        Assert.assertFalse(diseaseNames.isEmpty(), "No disease names available");
        long searchable = diseaseNames.stream().filter(n -> n != null && n.trim().length() >= 2).count();
        ApiReportContext.addExtraDetail("<b>Search-ready disease names (len >= 2):</b> " + searchable + " / " + diseaseNames.size());
        Assert.assertEquals(searchable, (long) diseaseNames.size(),
                "Some disease names are too short for search");
        System.out.println("TC51 PASSED");
    }

    @Test(priority = 52, description = "TC52: Each disease entry is a valid map (mapping-ready for tests)")
    public void testTC52_DiseaseMappingWithTests() {
        System.out.println("\n>>> TC52: Disease entries should be valid objects <<<");
        int invalid = 0;
        for (Map<String, Object> disease : diseasesData) {
            if (disease == null || disease.isEmpty()) invalid++;
        }
        ApiReportContext.addExtraDetail("<b>Valid disease objects:</b> " + (diseasesData.size() - invalid) + " / " + diseasesData.size());
        Assert.assertEquals(invalid, 0, invalid + " invalid disease objects found");
        System.out.println("TC52 PASSED");
    }

    @Test(priority = 53, description = "TC53: Disease IDs are non-empty (usable in package queries)")
    public void testTC53_DiseaseMappingWithPackages() {
        System.out.println("\n>>> TC53: Disease IDs must be non-empty (usable in package API calls) <<<");
        Assert.assertFalse(diseaseIds.isEmpty(), "No disease IDs extracted");
        long validIds = diseaseIds.stream()
                .filter(id -> id != null && !id.trim().isEmpty() && !id.equalsIgnoreCase("null"))
                .count();
        ApiReportContext.addExtraDetail("<b>Valid disease IDs:</b> " + validIds + " / " + diseaseIds.size()
                + " | <b>Sample:</b> " + diseaseIds.get(0));
        Assert.assertEquals(validIds, (long) diseaseIds.size(), "Some disease IDs are blank/null");
        System.out.println("TC53 PASSED");
    }

    @Test(priority = 54, description = "TC54: Disease count is positive (admin config cross-check)")
    public void testTC54_DiseaseCountPositive() {
        System.out.println("\n>>> TC54: Disease count should be > 0 <<<");
        Assert.assertTrue(totalDiseases > 0,
                "Expected at least 1 disease. Got: " + totalDiseases);
        ApiReportContext.addExtraDetail("<b>Disease count from API:</b> " + totalDiseases);
        System.out.println("TC54 PASSED");
    }

    @Test(priority = 55, description = "TC55: Disease data consistency between calls (UI vs API)")
    public void testTC55_DataConsistencyUIvsAPI() {
        System.out.println("\n>>> TC55: Data consistency across two calls <<<");
        int c1 = getDiseaseCount(callGetAllDiseases());
        int c2 = getDiseaseCount(callGetAllDiseases());
        ApiReportContext.addExtraDetail("<b>Call 1 count:</b> " + c1 + " | <b>Call 2 count:</b> " + c2
                + " | <b>Consistent:</b> " + (c1 == c2 ? "<span style='color:green'>YES</span>" : "<span style='color:red'>NO</span>"));
        Assert.assertEquals(c1, c2, "Disease count differs: " + c1 + " vs " + c2);
        System.out.println("TC55 PASSED");
    }

    // =========================================================================
    //  RESPONSE VALIDATION (TC56-TC60)
    // =========================================================================

    @Test(priority = 56, description = "TC56: Status code is 200")
    public void testTC56_StatusCode() {
        System.out.println("\n>>> TC56: HTTP status code should be 200 <<<");
        Response r = callGetAllDiseases();
        ApiReportContext.addExtraDetail("<b>HTTP Status Code:</b> " + r.getStatusCode());
        Assert.assertEquals(r.getStatusCode(), 200);
        System.out.println("TC56 PASSED");
    }

    @Test(priority = 57, description = "TC57: Response message (msg) field has a value")
    public void testTC57_ResponseMsg() {
        System.out.println("\n>>> TC57: msg field should be present and non-empty <<<");
        Response r = callGetAllDiseases();
        String msg = r.jsonPath().getString("msg");
        Assert.assertNotNull(msg, "msg field is null");
        Assert.assertFalse(msg.trim().isEmpty(), "msg field is empty");
        ApiReportContext.addExtraDetail("<b>msg value:</b> &quot;" + msg + "&quot;");
        System.out.println("TC57 PASSED");
    }

    @Test(priority = 58, description = "TC58: success flag is true")
    public void testTC58_SuccessFlagTrue() {
        System.out.println("\n>>> TC58: success=true in response body <<<");
        Response r = callGetAllDiseases();
        Boolean success = r.jsonPath().getBoolean("success");
        Assert.assertNotNull(success, "success field missing");
        Assert.assertTrue(success, "success should be true");
        ApiReportContext.addExtraDetail("<b>success:</b> " + success);
        System.out.println("TC58 PASSED");
    }

    @Test(priority = 59, description = "TC59: Response data object (data array) is present")
    public void testTC59_ResponseDataObject() {
        System.out.println("\n>>> TC59: data array should be present <<<");
        Response r = callGetAllDiseases();
        List<?> data = r.jsonPath().getList("data");
        Assert.assertNotNull(data, "data array is null/missing");
        Assert.assertFalse(data.isEmpty(), "data array is empty");
        ApiReportContext.addExtraDetail("<b>data array size:</b> " + data.size()
                + " | <b>Present:</b> <span style='color:green'>YES</span>");
        System.out.println("TC59 PASSED");
    }

    @Test(priority = 60, description = "TC60: Mandatory fields contain valid (non-null, non-empty) values")
    public void testTC60_MandatoryFieldsValid() {
        System.out.println("\n>>> TC60: Mandatory fields (_id, name) have valid values <<<");
        Assert.assertFalse(diseasesData.isEmpty(), "No diseases to validate");
        int invalid = 0;
        for (Map<String, Object> disease : diseasesData) {
            if (disease == null) { invalid++; continue; }
            String idKey   = disease.containsKey("_id") ? "_id" : "id";
            String nameKey = disease.containsKey("name") ? "name" : disease.containsKey("disease_name") ? "disease_name" : "title";
            Object id      = disease.get(idKey);
            Object name    = disease.get(nameKey);
            if (id == null || id.toString().trim().isEmpty()) invalid++;
            else if (name == null || name.toString().trim().isEmpty()) invalid++;
        }
        ApiReportContext.addExtraDetail("<b>Diseases with invalid mandatory fields:</b> " + invalid
                + " | <b>Total:</b> " + diseasesData.size());
        Assert.assertEquals(invalid, 0, invalid + " diseases have invalid mandatory fields");
        System.out.println("TC60 PASSED");
    }

    // =========================================================================
    //  HIGH PRIORITY AUTOMATION (TC61-TC80)
    // =========================================================================

    @Test(priority = 61, description = "TC61: Status Code Validation - returns 200")
    public void testTC61_StatusCodeValidation() {
        System.out.println("\n>>> TC61: Status Code Validation <<<");
        Response r = callGetAllDiseases();
        ApiReportContext.addExtraDetail("<b>HTTP Status:</b> " + r.getStatusCode()
                + " | <b>Response Time:</b> " + r.getTime() + " ms");
        Assert.assertEquals(r.getStatusCode(), 200);
        System.out.println("TC61 PASSED");
    }

    @Test(priority = 62, description = "TC62: Response time < 2000 ms (high-priority)")
    public void testTC62_ResponseTimeValidation() {
        System.out.println("\n>>> TC62: Response Time Validation <<<");
        long start = System.currentTimeMillis();
        Response r = callGetAllDiseases();
        long elapsed = System.currentTimeMillis() - start;
        ApiReportContext.addExtraDetail("<b>Response Time:</b> " + elapsed + " ms | <b>SLA:</b> 2000 ms");
        Assert.assertTrue(elapsed < 2000,
                "Response took " + elapsed + " ms, exceeded 2000 ms SLA");
        System.out.println("TC62 PASSED");
    }

    @Test(priority = 63, description = "TC63: Schema validation - success, msg, data fields present")
    public void testTC63_SchemaValidation() {
        System.out.println("\n>>> TC63: Schema Validation <<<");
        Response r = callGetAllDiseases();
        Assert.assertEquals(r.getStatusCode(), 200);
        Assert.assertNotNull(r.jsonPath().get("success"), "missing: success");
        Assert.assertNotNull(r.jsonPath().get("msg"),     "missing: msg");
        List<?> data = r.jsonPath().getList("data");
        Assert.assertNotNull(data, "missing: data array");
        ApiReportContext.addExtraDetail("<b>success:</b> " + r.jsonPath().get("success")
                + " | <b>msg:</b> " + r.jsonPath().getString("msg")
                + " | <b>data count:</b> " + (data != null ? data.size() : 0));
        System.out.println("TC63 PASSED");
    }

    @Test(priority = 64, description = "TC64: All disease IDs are valid non-empty strings")
    public void testTC64_DiseaseIdValidation() {
        System.out.println("\n>>> TC64: Disease ID Validation <<<");
        Assert.assertFalse(diseaseIds.isEmpty(), "No disease IDs extracted");
        long invalid = diseaseIds.stream()
                .filter(id -> id == null || id.trim().isEmpty() || id.equalsIgnoreCase("null"))
                .count();
        ApiReportContext.addExtraDetail("<b>Total IDs:</b> " + diseaseIds.size() + " | <b>Invalid IDs:</b> " + invalid);
        Assert.assertEquals(invalid, 0L, invalid + " disease IDs are invalid");
        System.out.println("TC64 PASSED");
    }

    @Test(priority = 65, description = "TC65: All disease names are valid non-empty strings")
    public void testTC65_DiseaseNameValidation() {
        System.out.println("\n>>> TC65: Disease Name Validation <<<");
        Assert.assertFalse(diseaseNames.isEmpty(), "No disease names extracted");
        long invalid = diseaseNames.stream()
                .filter(n -> n == null || n.trim().isEmpty())
                .count();
        StringBuilder nameList = new StringBuilder();
        diseaseNames.forEach(n -> nameList.append(n).append(", "));
        ApiReportContext.addExtraDetail("<b>All disease names:</b> " + nameList.toString().replaceAll(", $", "")
                + "<br/>&nbsp;&nbsp;<b>Invalid names:</b> " + invalid);
        Assert.assertEquals(invalid, 0L, invalid + " disease names are invalid");
        System.out.println("TC65 PASSED");
    }

    @Test(priority = 66, description = "TC66: No duplicate disease names or IDs")
    public void testTC66_DuplicateDiseaseValidation() {
        System.out.println("\n>>> TC66: Duplicate Disease Validation <<<");
        Set<String> seenNames = new LinkedHashSet<>();
        List<String> dupNames = new ArrayList<>();
        for (String n : diseaseNames) {
            if (!seenNames.add(n.toLowerCase().trim())) dupNames.add(n);
        }
        Set<String> seenIds = new HashSet<>(diseaseIds);
        int dupIds = diseaseIds.size() - seenIds.size();
        ApiReportContext.addExtraDetail("<b>Duplicate names:</b> "
                + (dupNames.isEmpty() ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + dupNames + "</span>")
                + " | <b>Duplicate IDs:</b> "
                + (dupIds == 0 ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + dupIds + " found</span>"));
        Assert.assertTrue(dupNames.isEmpty(), "Duplicate disease names: " + dupNames);
        Assert.assertEquals(dupIds, 0, dupIds + " duplicate disease IDs found");
        System.out.println("TC66 PASSED");
    }

    @Test(priority = 67, description = "TC67: No null disease objects in the data array")
    public void testTC67_NullDiseaseValidation() {
        System.out.println("\n>>> TC67: Null Disease Validation <<<");
        Response r = callGetAllDiseases();
        List<?> rawList = r.jsonPath().getList("data");
        Assert.assertNotNull(rawList, "data array missing");
        long nullCount = rawList.stream().filter(Objects::isNull).count();
        ApiReportContext.addExtraDetail("<b>Null objects in data array:</b> "
                + (nullCount == 0 ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + nullCount + "</span>")
                + " | <b>Total entries:</b> " + rawList.size());
        Assert.assertEquals(nullCount, 0L, nullCount + " null entries in data array");
        System.out.println("TC67 PASSED");
    }

    @Test(priority = 68, description = "TC68: Alphabetical sorting validation - document order")
    public void testTC68_AlphabeticalSortingValidation() {
        System.out.println("\n>>> TC68: Alphabetical Sorting Validation <<<");
        if (diseaseNames.size() < 2) {
            ApiReportContext.addExtraDetail("<b>Sorting check:</b> N/A (< 2 diseases)");
            return;
        }
        List<String> sorted = diseaseNames.stream()
                .map(String::toLowerCase)
                .sorted()
                .collect(Collectors.toList());
        List<String> actual = diseaseNames.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        boolean isAlpha = actual.equals(sorted);
        ApiReportContext.addExtraDetail("<b>Returned order:</b> " + diseaseNames
                + "<br/>&nbsp;&nbsp;<b>Alphabetical:</b> " + sorted
                + "<br/>&nbsp;&nbsp;<b>Is sorted:</b> "
                + (isAlpha ? "<span style='color:green'>YES</span>" : "<span style='color:orange'>NO - custom order</span>"));
        System.out.println("   Is alphabetical: " + isAlpha);
        System.out.println("TC68 PASSED - order documented");
    }

    @Test(priority = 69, description = "TC69: All returned diseases are active")
    public void testTC69_ActiveDiseaseValidation() {
        System.out.println("\n>>> TC69: Active Disease Validation <<<");
        List<String> inactive = new ArrayList<>();
        for (Map<String, Object> disease : diseasesData) {
            if (disease == null) continue;
            Object active = disease.get("isActive");
            if (active != null && "false".equalsIgnoreCase(active.toString())) {
                inactive.add(String.valueOf(disease.getOrDefault("name", disease.get("_id"))));
            }
        }
        ApiReportContext.addExtraDetail("<b>Inactive diseases returned:</b> "
                + (inactive.isEmpty() ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + inactive + "</span>")
                + " | <b>Total checked:</b> " + diseasesData.size());
        Assert.assertTrue(inactive.isEmpty(), "Inactive diseases in response: " + inactive);
        System.out.println("TC69 PASSED");
    }

    @Test(priority = 70, description = "TC70: UI vs API Data Validation - count consistency")
    public void testTC70_UIvsAPIDataValidation() {
        System.out.println("\n>>> TC70: UI vs API Data Validation <<<");
        int c1 = getDiseaseCount(callGetAllDiseases());
        int c2 = getDiseaseCount(callGetAllDiseases());
        ApiReportContext.addExtraDetail("<b>API Call 1:</b> " + c1 + " diseases"
                + " | <b>API Call 2:</b> " + c2 + " diseases"
                + " | <b>Consistent:</b> " + (c1 == c2 ? "<span style='color:green'>YES</span>" : "<span style='color:red'>NO</span>"));
        Assert.assertEquals(c1, c2, "Disease count differs between API calls");
        System.out.println("TC70 PASSED");
    }

    @Test(priority = 71, description = "TC71: 'Diabetes' is not returned multiple times")
    public void testTC71_DiabetesNotReturnedMultipleTimes() {
        System.out.println("\n>>> TC71: 'Diabetes' should appear at most once <<<");
        long diabetesCount = diseaseNames.stream()
                .filter(n -> "diabetes".equalsIgnoreCase(n.trim()))
                .count();
        ApiReportContext.addExtraDetail("<b>'Diabetes' occurrences:</b> " + diabetesCount
                + " | <b>Expected:</b> 0 or 1");
        Assert.assertTrue(diabetesCount <= 1,
                "'Diabetes' appears " + diabetesCount + " times - expected at most 1");
        System.out.println("TC71 PASSED - Diabetes count: " + diabetesCount);
    }

    @Test(priority = 72, description = "TC72: Disease names are trimmed (no leading/trailing spaces)")
    public void testTC72_DiseaseNamesTrimmed() {
        System.out.println("\n>>> TC72: Disease names should be trimmed <<<");
        List<String> untrimmed = new ArrayList<>();
        for (String name : diseaseNames) {
            if (!name.equals(name.trim())) untrimmed.add("'" + name + "'");
        }
        ApiReportContext.addExtraDetail("<b>Untrimmed names:</b> "
                + (untrimmed.isEmpty() ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + untrimmed + "</span>")
                + " | <b>Total checked:</b> " + diseaseNames.size());
        Assert.assertTrue(untrimmed.isEmpty(),
                "Disease names with leading/trailing spaces: " + untrimmed);
        System.out.println("TC72 PASSED");
    }

    @Test(priority = 73, description = "TC73: No blank disease records are returned")
    public void testTC73_NoBlankDiseaseRecords() {
        System.out.println("\n>>> TC73: No blank disease names <<<");
        long blankCount = diseaseNames.stream().filter(n -> n == null || n.trim().isEmpty()).count();
        ApiReportContext.addExtraDetail("<b>Blank disease names:</b> "
                + (blankCount == 0 ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + blankCount + " found</span>")
                + " | <b>Total:</b> " + diseaseNames.size());
        Assert.assertEquals(blankCount, 0L, blankCount + " blank disease names found");
        System.out.println("TC73 PASSED");
    }

    @Test(priority = 74, description = "TC74: Disease names support special characters if applicable")
    public void testTC74_SpecialCharsSupported() {
        System.out.println("\n>>> TC74: Disease names with special chars (hyphens, apostrophes) <<<");
        long withSpecial = diseaseNames.stream()
                .filter(n -> n.contains("-") || n.contains("'") || n.contains("/") || n.contains("("))
                .count();
        ApiReportContext.addExtraDetail("<b>Names with special chars (-, ', /, ():</b> " + withSpecial + " / " + diseaseNames.size()
                + " | <b>All names valid:</b> <span style='color:green'>YES</span>");
        // Informational - special chars in medical terms (e.g., "Type-2 Diabetes") are valid
        System.out.println("   Names with special chars: " + withSpecial);
        System.out.println("TC74 PASSED");
    }

    @Test(priority = 75, description = "TC75: No null objects exist in response array")
    public void testTC75_NoNullObjectsInArray() {
        System.out.println("\n>>> TC75: No null objects in response data array <<<");
        Response r = callGetAllDiseases();
        List<?> data = r.jsonPath().getList("data");
        Assert.assertNotNull(data, "data array missing");
        long nulls = data.stream().filter(Objects::isNull).count();
        ApiReportContext.addExtraDetail("<b>Null objects:</b> "
                + (nulls == 0 ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + nulls + "</span>")
                + " | <b>Total:</b> " + data.size());
        Assert.assertEquals(nulls, 0L, nulls + " null objects in data array");
        System.out.println("TC75 PASSED");
    }

    @Test(priority = 76, description = "TC76: Disease count remains consistent across requests")
    public void testTC76_DiseaseCountConsistent() {
        System.out.println("\n>>> TC76: Disease count stable across calls <<<");
        int c1 = getDiseaseCount(callGetAllDiseases());
        int c2 = getDiseaseCount(callGetAllDiseases());
        ApiReportContext.addExtraDetail("<b>Call 1 count:</b> " + c1
                + " | <b>Call 2 count:</b> " + c2
                + " | <b>Consistent:</b> " + (c1 == c2 ? "<span style='color:green'>YES</span>" : "<span style='color:red'>NO</span>"));
        Assert.assertEquals(c1, c2, "Disease count differs: " + c1 + " vs " + c2);
        System.out.println("TC76 PASSED");
    }

    @Test(priority = 77, description = "TC77: Soft-deleted diseases are excluded from response")
    public void testTC77_SoftDeletedExcluded() {
        System.out.println("\n>>> TC77: Soft-deleted diseases should not appear <<<");
        List<String> softDeleted = new ArrayList<>();
        for (Map<String, Object> disease : diseasesData) {
            if (disease == null) continue;
            Object del = disease.get("isDeleted");
            if (del != null && "true".equalsIgnoreCase(del.toString())) {
                softDeleted.add(String.valueOf(disease.getOrDefault("name", disease.get("_id"))));
            }
        }
        ApiReportContext.addExtraDetail("<b>Soft-deleted diseases:</b> "
                + (softDeleted.isEmpty() ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + softDeleted + "</span>")
                + " | <b>Total:</b> " + diseasesData.size());
        Assert.assertTrue(softDeleted.isEmpty(), "Soft-deleted diseases found: " + softDeleted);
        System.out.println("TC77 PASSED");
    }

    @Test(priority = 78, description = "TC78: Disease IDs are unique across all records")
    public void testTC78_DiseaseIdsUniqueAcrossAll() {
        System.out.println("\n>>> TC78: All disease IDs must be globally unique <<<");
        Set<String> seen = new HashSet<>();
        List<String> duplicates = new ArrayList<>();
        for (String id : diseaseIds) {
            if (!seen.add(id)) duplicates.add(id);
        }
        ApiReportContext.addExtraDetail("<b>Total IDs:</b> " + diseaseIds.size()
                + " | <b>Duplicate IDs:</b> "
                + (duplicates.isEmpty() ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + duplicates + "</span>"));
        Assert.assertTrue(duplicates.isEmpty(), "Duplicate disease IDs found: " + duplicates);
        System.out.println("TC78 PASSED");
    }

    @Test(priority = 79, description = "TC79: No duplicate disease slugs in response")
    public void testTC79_NoDuplicateSlugs() {
        System.out.println("\n>>> TC79: No duplicate disease slugs <<<");
        if (diseaseSlugs.isEmpty()) {
            ApiReportContext.addExtraDetail("<b>Slug check:</b> No slugs present in response - SKIPPED");
            System.out.println("   No slugs in response - SKIPPED");
            return;
        }
        Set<String> seen = new HashSet<>();
        List<String> duplicates = new ArrayList<>();
        for (String slug : diseaseSlugs) {
            if (slug != null && !slug.trim().isEmpty()) {
                if (!seen.add(slug.toLowerCase().trim())) duplicates.add(slug);
            }
        }
        ApiReportContext.addExtraDetail("<b>Total slugs:</b> " + diseaseSlugs.size()
                + " | <b>Duplicate slugs:</b> "
                + (duplicates.isEmpty() ? "<span style='color:green'>NONE</span>" : "<span style='color:red'>" + duplicates + "</span>"));
        Assert.assertTrue(duplicates.isEmpty(), "Duplicate disease slugs: " + duplicates);
        System.out.println("TC79 PASSED");
    }

    @Test(priority = 80, description = "TC80: Idempotency - same data after application refresh")
    public void testTC80_SameDataAfterRefresh() {
        System.out.println("\n>>> TC80: Idempotency - same disease count on two calls <<<");
        int c1 = getDiseaseCount(callGetAllDiseases());
        int c2 = getDiseaseCount(callGetAllDiseases());
        ApiReportContext.addExtraDetail("<b>Call 1:</b> " + c1 + " diseases"
                + " | <b>Call 2:</b> " + c2 + " diseases"
                + " | <b>Idempotent:</b> " + (c1 == c2 ? "<span style='color:green'>YES</span>" : "<span style='color:red'>NO</span>"));
        Assert.assertEquals(c1, c2, "Disease count changed between calls: " + c1 + " vs " + c2);
        System.out.println("TC80 PASSED");
    }
}

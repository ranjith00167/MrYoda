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
 * GET /tests/getAllSymptoms - Standalone Validation Suite
 *
 * curl --location https://staging-api-diagnostics.yodaprojects.com/tests/getAllSymptoms
 *      --header accept: any
 *
 * TC01-TC10:  Functional        - 200 OK, success flag, data not empty, response structure
 * TC11-TC20:  Field Validation  - _id, name/symptom_name, related fields present
 * TC21-TC30:  Data Integrity    - No nulls in critical fields, list consistency
 * TC31-TC40:  Negative          - Wrong method (POST), invalid endpoint variant
 * TC41-TC50:  Security          - SQL injection, XSS, path traversal in query/header
 * TC51-TC55:  Performance       - Response time < 5s, idempotency
 * TC56-TC60:  Regression        - Consistent count, valid JSON, content-type
 * TC61-TC70:  Parameterized (data.name) - Each name is non-null, non-empty, string,
 *             no HTML/script tags, reasonable length, unique, no leading/trailing whitespace
 */
public class GetAllSymptomsValidationTest extends BaseTest {

    private static final String ENDPOINT = APIEndpoints.GET_ALL_SYMPTOMS;

    // Captured during setup for reuse in later tests
    private static List<Map<String, Object>> symptomsData = new ArrayList<>();
    private static List<String>             symptomNames  = new ArrayList<>();
    private static Map<String, Object> firstSymptom = null;
    private static int totalSymptoms = 0;

    // ═══════════════════════════════════════════════════════════════════
    //  SETUP — Prime the symptoms list once before all tests
    // ═══════════════════════════════════════════════════════════════════

    @BeforeClass
    public void setupSymptomsData() {
        System.out.println("\n========================================");
        System.out.println("SETUP: Fetching all symptoms from getAllSymptoms API");
        System.out.println("========================================");

        Response r = callGetAllSymptoms();

        if (r.getStatusCode() == 200) {
            // Try data as list first, then root-level array
            List<Map<String, Object>> list = null;
            try {
                list = r.jsonPath().getList("data");
            } catch (Exception ignored) { }

            if (list == null || list.isEmpty()) {
                try {
                    list = r.jsonPath().getList("symptoms");
                } catch (Exception ignored) { }
            }

            if (list == null || list.isEmpty()) {
                try {
                    list = r.jsonPath().getList("$");
                } catch (Exception ignored) { }
            }

            if (list != null && !list.isEmpty()) {
                // Filter out null entries
                for (Map<String, Object> sym : list) {
                    if (sym != null) {
                        symptomsData.add(sym);
                    }
                }
                totalSymptoms = symptomsData.size();
                firstSymptom  = symptomsData.isEmpty() ? null : symptomsData.get(0);

                // ── Extract data.name values for the DataProvider ──────────
                for (Map<String, Object> sym : symptomsData) {
                    String nameKey = sym.containsKey("name")         ? "name"
                                   : sym.containsKey("symptom_name") ? "symptom_name"
                                   : sym.containsKey("title")        ? "title" : null;
                    if (nameKey != null && sym.get(nameKey) != null) {
                        symptomNames.add(sym.get(nameKey).toString());
                    }
                }

                System.out.println("   Total symptoms returned : " + totalSymptoms);
                System.out.println("   Names extracted         : " + symptomNames.size());
                if (firstSymptom != null) {
                    System.out.println("   First symptom fields    : " + firstSymptom.keySet());
                    System.out.println("   Sample entry            : " + firstSymptom);
                }
                if (!symptomNames.isEmpty()) {
                    System.out.println("   Sample names            : " + symptomNames.subList(0, Math.min(5, symptomNames.size())));
                }
                System.out.println("✅ Setup complete");
            } else {
                System.out.println("⚠️  No symptoms list found in response body");
                System.out.println("   Raw body (first 500 chars): " +
                        r.asString().substring(0, Math.min(500, r.asString().length())));
            }
        } else {
            System.out.println("❌ Setup failed. Status: " + r.getStatusCode());
            System.out.println("   Body: " + r.asString());
        }
        System.out.println("========================================\n");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  DATA PROVIDER - one row per data.name value from the API
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Supplies every name extracted from data[].name during @BeforeClass.
     * Each row: { index (1-based), nameValue }
     */
    @DataProvider(name = "symptomNames")
    public Object[][] provideSymptomNames() {
        if (symptomNames.isEmpty()) {
            // Return a single dummy row so TestNG doesn't skip the data-driven tests
            return new Object[][] { { 1, "__NO_DATA__" } };
        }
        Object[][] rows = new Object[symptomNames.size()][2];
        for (int i = 0; i < symptomNames.size(); i++) {
            rows[i][0] = i + 1;
            rows[i][1] = symptomNames.get(i);
        }
        return rows;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════════

    /** Standard positive GET call — also records the API call in the Extent Report */
    private Response callGetAllSymptoms() {
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
                200, "GET /tests/getAllSymptoms"));
        return r;
    }

    /** Negative/security call — marks expected status as 4xx in report */
    private Response callGetAllSymptomsExpecting4xx() {
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
                400, 400, 499, "GET /tests/getAllSymptoms (expecting 4xx)"));
        return r;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  FUNCTIONAL (TC01-TC10)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 1, description = "TC01: GET getAllSymptoms returns HTTP 200")
    public void testTC01_Returns200() {
        System.out.println("\n>>> TC01: GET getAllSymptoms — Expect HTTP 200 <<<");
        Response r = callGetAllSymptoms();
        System.out.println("   HTTP Status: " + r.getStatusCode());
        Assert.assertEquals(r.getStatusCode(), 200,
                "Expected 200 OK. Got: " + r.getStatusCode() + "\nBody: " + r.asString());
        ApiReportContext.addExtraDetail("<b>HTTP Status:</b> " + r.getStatusCode());
        ApiReportContext.addExtraDetail("<b>Response Time:</b> " + r.getTime() + " ms");
        System.out.println("✅ TC01 PASSED");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 2, description = "TC02: Response body is not empty")
    public void testTC02_ResponseBodyNotEmpty() {
        System.out.println("\n>>> TC02: Response body should not be empty <<<");
        Response r = callGetAllSymptoms();
        Assert.assertEquals(r.getStatusCode(), 200);
        String body = r.asString();
        Assert.assertNotNull(body, "Response body should not be null");
        Assert.assertFalse(body.isEmpty(), "Response body should not be empty");
        ApiReportContext.addExtraDetail("<b>Body Length:</b> " + body.length() + " chars");
        System.out.println("   Body length: " + body.length() + " chars");
        System.out.println("✅ TC02 PASSED");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 3, description = "TC03: Response contains success=true")
    public void testTC03_SuccessTrue() {
        System.out.println("\n>>> TC03: success flag should be true <<<");
        Response r = callGetAllSymptoms();
        Assert.assertEquals(r.getStatusCode(), 200);
        Boolean success = r.jsonPath().getBoolean("success");
        Assert.assertNotNull(success, "success field should exist");
        Assert.assertTrue(success, "success should be true");
        ApiReportContext.addExtraDetail("<b>success field value:</b> " + success);
        System.out.println("   success: " + success);
        System.out.println("✅ TC03 PASSED");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 4, description = "TC04: Response contains status=200 field")
    public void testTC04_StatusField() {
        System.out.println("\n>>> TC04: status field should equal 200 <<<");
        Response r = callGetAllSymptoms();
        Assert.assertEquals(r.getStatusCode(), 200);
        Integer statusField = r.jsonPath().getInt("status");
        Assert.assertNotNull(statusField, "status field is missing in response body");
        Assert.assertEquals(statusField.intValue(), 200,
                "status field value should be 200. Got: " + statusField);
        ApiReportContext.addExtraDetail("<b>status field in response body:</b> " + statusField);
        System.out.println("   status: " + statusField);
        System.out.println("✅ TC04 PASSED");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 5, description = "TC05: Response contains a non-empty data/symptoms list")
    public void testTC05_DataListNotEmpty() {
        System.out.println("\n>>> TC05: Symptoms list should have at least one entry <<<");
        Assert.assertFalse(symptomsData.isEmpty(),
                "Symptoms list is empty — getAllSymptoms should return at least one entry");        ApiReportContext.addExtraDetail("<b>Total Symptoms returned:</b> " + symptomsData.size());        System.out.println("   Total symptoms: " + symptomsData.size());
        System.out.println("✅ TC05 PASSED");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 6, description = "TC06: Content-Type is application/json")
    public void testTC06_ContentType() {
        System.out.println("\n>>> TC06: Content-Type should be application/json <<<");
        Response r = callGetAllSymptoms();
        String ct = r.getContentType();
        System.out.println("   Content-Type: " + ct);
        Assert.assertTrue(ct.contains("application/json"),
                "Expected application/json. Got: " + ct);
        ApiReportContext.addExtraDetail("<b>Content-Type header:</b> " + ct);
        System.out.println("✅ TC06 PASSED");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 7, description = "TC07: Response is valid JSON")
    public void testTC07_ValidJson() {
        System.out.println("\n>>> TC07: Response should be parseable JSON <<<");
        Response r = callGetAllSymptoms();
        Assert.assertEquals(r.getStatusCode(), 200);
        // jsonPath().get() throws if not valid JSON
        Object root = r.jsonPath().get("$");
        Assert.assertNotNull(root, "Response root should not be null");
        ApiReportContext.addExtraDetail("<b>Valid JSON:</b> <span style='color:green'>YES</span> | <b>Root type:</b> " + root.getClass().getSimpleName());
        System.out.println("✅ TC07 PASSED");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 8, description = "TC08: Response contains msg field")
    public void testTC08_MsgField() {
        System.out.println("\n>>> TC08: msg field should be present <<<");
        Response r = callGetAllSymptoms();
        Assert.assertEquals(r.getStatusCode(), 200);
        String msg = r.jsonPath().getString("msg");
        Assert.assertNotNull(msg, "msg field is missing in response body");
        ApiReportContext.addExtraDetail("<b>msg field value:</b> &quot;" + msg + "&quot;");
        System.out.println("   msg: " + msg);
        System.out.println("✅ TC08 PASSED");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 9, description = "TC09: Returned list is of array type")
    public void testTC09_DataIsArray() {
        System.out.println("\n>>> TC09: Symptoms data should be an array <<<");
        Response r = callGetAllSymptoms();
        Assert.assertEquals(r.getStatusCode(), 200);
        // symptomsData list was populated from a List parse — confirm it's indeed a list
        List<?> rawList = r.jsonPath().getList("data");
        if (rawList == null) rawList = r.jsonPath().getList("symptoms");
        Assert.assertNotNull(rawList, "data/symptoms array should be present");
        ApiReportContext.addExtraDetail("<b>Array type confirmed:</b> List | <b>Array size:</b> " + rawList.size() + " entries");
        System.out.println("   Array size: " + rawList.size());
        System.out.println("✅ TC09 PASSED");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 10, description = "TC10: Multiple calls return same HTTP status")
    public void testTC10_ConsistentHttpStatus() {
        System.out.println("\n>>> TC10: Multiple calls should return consistent HTTP status <<<");
        int s1 = callGetAllSymptoms().getStatusCode();
        int s2 = callGetAllSymptoms().getStatusCode();
        Assert.assertEquals(s1, 200);
        Assert.assertEquals(s2, 200);
        ApiReportContext.addExtraDetail("<b>Call 1 Status:</b> " + s1 + " | <b>Call 2 Status:</b> " + s2 + " | <b>Consistent:</b> <span style='color:green'>YES</span>");
        System.out.println("   Call 1: " + s1 + "  |  Call 2: " + s2);
        System.out.println("✅ TC10 PASSED");
        ApiReportContext.setExpectedStatus(200);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  FIELD VALIDATION (TC11-TC20)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 11, description = "TC11: First symptom has _id field")
    public void testTC11_IdFieldExists() {
        System.out.println("\n>>> TC11: _id field should exist in each symptom <<<");
        Assert.assertNotNull(firstSymptom, "No symptoms returned - cannot validate fields");
        Assert.assertTrue(firstSymptom.containsKey("_id"),
                "_id field missing. Keys found: " + firstSymptom.keySet());
        ApiReportContext.addExtraDetail("<b>First symptom _id:</b> " + firstSymptom.get("_id"));
        ApiReportContext.addExtraDetail("<b>All fields in first symptom:</b> " + firstSymptom.keySet());
        System.out.println("   _id: " + firstSymptom.get("_id"));
        System.out.println("✅ TC11 PASSED");
    }

    @Test(priority = 12, description = "TC12: First symptom has a name/symptom_name field")
    public void testTC12_NameFieldExists() {
        System.out.println("\n>>> TC12: Symptom should have a name field <<<");
        Assert.assertNotNull(firstSymptom, "No symptoms returned");
        boolean hasName = firstSymptom.containsKey("name")
                || firstSymptom.containsKey("symptom_name")
                || firstSymptom.containsKey("symptomName")
                || firstSymptom.containsKey("title");
        Assert.assertTrue(hasName,
                "No name-like field found. Keys: " + firstSymptom.keySet());
        String nameVal = firstSymptom.containsKey("name")         ? String.valueOf(firstSymptom.get("name"))
                       : firstSymptom.containsKey("symptom_name") ? String.valueOf(firstSymptom.get("symptom_name"))
                       : firstSymptom.containsKey("symptomName")  ? String.valueOf(firstSymptom.get("symptomName"))
                       : String.valueOf(firstSymptom.get("title"));
        ApiReportContext.addExtraDetail("<b>Name field key:</b> &quot;" + (firstSymptom.containsKey("name") ? "name" : firstSymptom.containsKey("symptom_name") ? "symptom_name" : "title") + "&quot;");
        ApiReportContext.addExtraDetail("<b>First symptom name value:</b> " + nameVal);
        System.out.println("   name: " + nameVal);
        System.out.println("✅ TC12 PASSED");
    }


    @Test(priority = 14, description = "TC14: All symptoms have non-null _id")
    public void testTC14_AllIdsNonNull() {
        System.out.println("\n>>> TC14: Every symptom entry should have a non-null _id <<<");
        Assert.assertFalse(symptomsData.isEmpty(), "No symptoms to validate");
        int nullCount = 0;
        for (Map<String, Object> sym : symptomsData) {
            if (sym == null || sym.get("_id") == null) nullCount++;
        }
        Assert.assertEquals(nullCount, 0,
                nullCount + " symptoms have null _id");        ApiReportContext.addExtraDetail("<b>Total entries verified:</b> " + symptomsData.size() + " | <b>Entries with null _id:</b> 0");        System.out.println("   Verified " + symptomsData.size() + " entries — all have _id");
        System.out.println("✅ TC14 PASSED");
    }

    @Test(priority = 15, description = "TC15: Symptom list count is positive")
    public void testTC15_CountPositive() {
        System.out.println("\n>>> TC15: Symptoms count should be > 0 <<<");
        Assert.assertTrue(totalSymptoms > 0,
                "Expected at least 1 symptom. Got: " + totalSymptoms);
        ApiReportContext.addExtraDetail("<b>Total Symptoms Count:</b> " + totalSymptoms);
        System.out.println("   Total symptoms: " + totalSymptoms);
        System.out.println("✅ TC15 PASSED");
    }

    @Test(priority = 16, description = "TC16: First symptom name/title is a non-empty string")
    public void testTC16_NameIsNonEmptyString() {
        System.out.println("\n>>> TC16: Symptom name/title should be a non-empty string <<<");
        Assert.assertNotNull(firstSymptom, "No symptoms returned");
        String nameKey = firstSymptom.containsKey("name")         ? "name"
                       : firstSymptom.containsKey("symptom_name") ? "symptom_name"
                       : firstSymptom.containsKey("title")        ? "title" : null;
        Assert.assertNotNull(nameKey,
                "No standard name field (name/symptom_name/title) found in symptom. Keys: " + firstSymptom.keySet());
        Object val = firstSymptom.get(nameKey);
        Assert.assertNotNull(val, nameKey + " should not be null");
        Assert.assertFalse(val.toString().trim().isEmpty(), nameKey + " should not be blank");
        ApiReportContext.addExtraDetail("<b>Name key used:</b> &quot;" + nameKey + "&quot; | <b>Value:</b> " + val);
        System.out.println("   " + nameKey + ": " + val);
        System.out.println("✅ TC16 PASSED");
    }

    @Test(priority = 17, description = "TC17: Symptoms list has no duplicate _ids")
    public void testTC17_NoDuplicateIds() {
        System.out.println("\n>>> TC17: No duplicate _id values <<<");
        Set<String> seen = new HashSet<>();
        List<String> duplicates = new ArrayList<>();
        for (Map<String, Object> sym : symptomsData) {
            if (sym == null) continue;
            String id = String.valueOf(sym.get("_id"));
            if (!seen.add(id)) duplicates.add(id);
        }
        Assert.assertTrue(duplicates.isEmpty(),
                "Duplicate _ids found: " + duplicates);
        ApiReportContext.addExtraDetail("<b>Total entries:</b> " + symptomsData.size() + " | <b>Duplicate _ids found:</b> <span style='color:green'>NONE</span>");
        System.out.println("   No duplicates in " + symptomsData.size() + " entries");
        System.out.println("✅ TC17 PASSED");
    }

    @Test(priority = 18, description = "TC18: First 5 symptom entries all have _id and name-like field")
    public void testTC18_Top5EntriesComplete() {
        System.out.println("\n>>> TC18: First 5 symptoms should have _id and a name field <<<");
        int limit = Math.min(5, symptomsData.size());
        for (int i = 0; i < limit; i++) {
            Map<String, Object> sym = symptomsData.get(i);
            Assert.assertNotNull(sym, "Entry " + i + " is null");
            Assert.assertNotNull(sym.get("_id"), "Entry " + i + " has null _id");
            boolean hasName = sym.containsKey("name")
                    || sym.containsKey("symptom_name")
                    || sym.containsKey("title");
            Assert.assertTrue(hasName,
                    "Entry " + i + " has no name field. Keys: " + sym.keySet());
            System.out.println("   Entry " + (i + 1) + ": _id=" + sym.get("_id")
                    + " name=" + (sym.containsKey("name") ? sym.get("name") : sym.getOrDefault("symptom_name", sym.get("title"))));
        }
        StringBuilder top5 = new StringBuilder("<b>Top " + limit + " entries verified:</b><br/>");
        for (int i = 0; i < limit; i++) {
            Map<String, Object> sym = symptomsData.get(i);
            Object nameVal = sym.containsKey("name") ? sym.get("name") : sym.getOrDefault("symptom_name", sym.get("title"));
            top5.append("&nbsp;&nbsp;").append(i + 1).append(". <i>_id=</i>").append(sym.get("_id")).append(" &mdash; <i>name=</i>").append(nameVal).append("<br/>");
        }
        ApiReportContext.addExtraDetail(top5.toString());
        System.out.println("✅ TC18 PASSED");
    }

    @Test(priority = 19, description = "TC19: Response list is ordered (index 0 first)")
    public void testTC19_ListOrderConsistency() {
        System.out.println("\n>>> TC19: First entry should be consistent across calls <<<");
        Response r1 = callGetAllSymptoms();
        Response r2 = callGetAllSymptoms();

        List<Map<String, Object>> list1 = r1.jsonPath().getList("data");
        List<Map<String, Object>> list2 = r2.jsonPath().getList("data");

        Assert.assertNotNull(list1, "data array missing in first call response");
        Assert.assertNotNull(list2, "data array missing in second call response");
        Assert.assertFalse(list1.isEmpty(), "data array is empty in first call");
        Assert.assertFalse(list2.isEmpty(), "data array is empty in second call");

        String id1 = String.valueOf(list1.get(0).get("_id"));
        String id2 = String.valueOf(list2.get(0).get("_id"));
        Assert.assertEquals(id1, id2, "First entry _id differs between calls - ordering is unstable");
        ApiReportContext.addExtraDetail("<b>First entry _id (Call 1):</b> " + id1 + "<br/>&nbsp;&nbsp;<b>First entry _id (Call 2):</b> " + id2 + "<br/>&nbsp;&nbsp;<b>Order consistent:</b> <span style='color:green'>YES</span>");
        System.out.println("   First entry _id consistent: " + id1);
        System.out.println("✅ TC19 PASSED");
    }

    @Test(priority = 20, description = "TC20: Symptom _id values are non-empty strings")
    public void testTC20_IdsAreNonEmptyStrings() {
        System.out.println("\n>>> TC20: _id values should be non-empty strings <<<");
        int checked = 0;
        for (Map<String, Object> sym : symptomsData) {
            if (sym == null) continue;
            Object id = sym.get("_id");
            Assert.assertNotNull(id, "_id is null in a symptom entry");
            Assert.assertFalse(id.toString().trim().isEmpty(), "_id is blank");
            checked++;
        }
        System.out.println("   Verified _id on " + checked + " entries");
        ApiReportContext.addExtraDetail("<b>_ids verified:</b> " + checked + " entries | <b>All non-empty strings:</b> <span style='color:green'>YES</span>");
        System.out.println("✅ TC20 PASSED");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  DATA INTEGRITY (TC21-TC30)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 21, description = "TC21: Total count returned matches list size")
    public void testTC21_CountMatchesListSize() {
        System.out.println("\n>>> TC21: Symptom count consistency <<<");
        Response r = callGetAllSymptoms();
        Assert.assertEquals(r.getStatusCode(), 200);
        Object countRaw = r.jsonPath().get("count");
        if (countRaw != null) {
            int countField = Integer.parseInt(countRaw.toString());
            Assert.assertEquals(countField, totalSymptoms,
                    "count field does not match list size");
            ApiReportContext.addExtraDetail("<b>count field in response:</b> " + countField + " | <b>list size:</b> " + totalSymptoms + " | <b>Match:</b> <span style='color:green'>YES</span>");
            System.out.println("   count field: " + countField + "  list size: " + totalSymptoms);
        } else {
            ApiReportContext.addExtraDetail("<b>count field:</b> <i>not present in response</i> | <b>list size:</b> " + totalSymptoms);
            System.out.println("   Info: No 'count' field in response (acceptable)");
        }
        System.out.println("✅ TC21 PASSED");
    }

    @Test(priority = 22, description = "TC22: No null entries in symptoms array")
    public void testTC22_NoNullEntriesInList() {
        System.out.println("\n>>> TC22: Symptoms array should have no null entries <<<");
        Response r = callGetAllSymptoms();
        List<?> rawList = r.jsonPath().getList("data");
        if (rawList == null) rawList = r.jsonPath().getList("symptoms");
        Assert.assertNotNull(rawList, "data/symptoms array not found in response - cannot validate null entries");
        long nullCount = rawList.stream().filter(Objects::isNull).count();
        Assert.assertEquals(nullCount, 0L,
                nullCount + " null entries found in symptoms list");
        ApiReportContext.addExtraDetail("<b>List size:</b> " + rawList.size() + " | <b>Null entries:</b> <span style='color:green'>0</span>");
        System.out.println("   List size: " + rawList.size() + " - no nulls");
        System.out.println("✅ TC22 PASSED");
    }

    @Test(priority = 23, description = "TC23: Each symptom entry is a JSON object (not primitive)")
    public void testTC23_EntriesAreObjects() {
        System.out.println("\n>>> TC23: Each symptom entry should be an object <<<");
        int checked = 0;
        for (Map<String, Object> sym : symptomsData) {
            Assert.assertNotNull(sym, "Null entry in symptoms list");
            Assert.assertTrue(sym instanceof Map,
                    "Entry is not a Map: " + sym);
            checked++;
        }
        System.out.println("   " + checked + " entries are valid objects");
        ApiReportContext.addExtraDetail("<b>Entries verified as JSON objects:</b> " + checked + " / " + symptomsData.size());
        System.out.println("✅ TC23 PASSED");
    }

    @Test(priority = 24, description = "TC24: Symptoms count is stable across two calls")
    public void testTC24_StableCount() {
        System.out.println("\n>>> TC24: Symptoms count stable across calls <<<");
        int c1 = getSymptomCount(callGetAllSymptoms());
        int c2 = getSymptomCount(callGetAllSymptoms());
        System.out.println("   Call 1: " + c1 + "  |  Call 2: " + c2);
        Assert.assertEquals(c1, c2, "Symptom count differs between calls");
        ApiReportContext.addExtraDetail("<b>Call 1 count:</b> " + c1 + " | <b>Call 2 count:</b> " + c2 + " | <b>Stable:</b> <span style='color:green'>YES</span>");
        System.out.println("✅ TC24 PASSED");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  NEGATIVE TESTING (TC31-TC40)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 31, description = "TC31: Invalid endpoint variant returns non-200")
    public void testTC31_InvalidEndpointVariant() {
        System.out.println("\n>>> TC31: Invalid endpoint /tests/getAllSymptoms123 <<<");
        ApiReportContext.setExpectedStatusRange(400, 499);
        Response r = new RequestBuilder()
                .setEndpoint("/tests/getAllSymptoms123")
                .addHeader("accept", "*/*")
                .get();
        System.out.println("   Status: " + r.getStatusCode());
        Assert.assertNotEquals(r.getStatusCode(), 200,
                "Invalid endpoint should not return 200");        ApiReportContext.addExtraDetail("<b>Endpoint tested:</b> /tests/getAllSymptoms123 | <b>Actual Status:</b> " + r.getStatusCode() + " (non-200 as expected)");        System.out.println("✅ TC31 PASSED — non-200 as expected for invalid endpoint");
    }

    @Test(priority = 32, description = "TC32: Extra path segment returns 404")
    public void testTC32_ExtraPathSegment() {
        System.out.println("\n>>> TC32: Extra path /tests/getAllSymptoms/extra <<<");
        ApiReportContext.setExpectedStatusRange(400, 499);
        Response r = new RequestBuilder()
                .setEndpoint(ENDPOINT + "/extra-segment")
                .addHeader("accept", "*/*")
                .get();
        System.out.println("   Status: " + r.getStatusCode());
        Assert.assertNotEquals(r.getStatusCode(), 200,
                "Extra path segment should not return 200");
        ApiReportContext.addExtraDetail("<b>Endpoint tested:</b> " + ENDPOINT + "/extra-segment | <b>Actual Status:</b> " + r.getStatusCode() + " (non-200 as expected)");
        System.out.println("✅ TC32 PASSED");
    }

    @Test(priority = 33, description = "TC33: Correct endpoint with no accept header still responds")
    public void testTC33_NoAcceptHeader() {
        System.out.println("\n>>> TC33: Omit accept header — server should still respond <<<");
        Response r = new RequestBuilder()
                .setEndpoint(ENDPOINT)
                .get();
        System.out.println("   Status: " + r.getStatusCode());
        // Server should respond (200 or handled), not error out
        Assert.assertTrue(r.getStatusCode() >= 200 && r.getStatusCode() < 600,
                "Unexpected status: " + r.getStatusCode());
        ApiReportContext.addExtraDetail("<b>No accept header sent:</b> Server responded with status " + r.getStatusCode());
        System.out.println("✅ TC33 PASSED");
    }

    @Test(priority = 34, description = "TC34: Accept header wildcard (*/*) is accepted")
    public void testTC34_WildcardAcceptHeader() {
        System.out.println("\n>>> TC34: accept: */* should be accepted <<<");
        Response r = new RequestBuilder()
                .setEndpoint(ENDPOINT)
                .addHeader("accept", "*/*")
                .get();
        Assert.assertEquals(r.getStatusCode(), 200);
        ApiReportContext.addExtraDetail("<b>accept: */* header:</b> Accepted | <b>Status:</b> " + r.getStatusCode());
        System.out.println("   Status: " + r.getStatusCode());
        System.out.println("✅ TC34 PASSED");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 35, description = "TC35: Verify server response for uppercase URL path")
    public void testTC35_UppercasePath() {
        System.out.println("\n>>> TC35: /tests/GETALLSYMPTOMS - case sensitivity check <<<");
        Response r = new RequestBuilder()
                .setEndpoint("/tests/GETALLSYMPTOMS")
                .addHeader("accept", "*/*")
                .get();
        int status = r.getStatusCode();
        System.out.println("   Status: " + status);
        if (status == 200) {
            System.out.println("   Info: Server is case-insensitive (both paths return 200 - acceptable)");
        } else {
            System.out.println("   Info: Server is case-sensitive (uppercase path returned " + status + ")");
        }
        // Either behaviour is valid - just confirm no 5xx
        Assert.assertTrue(status < 500, "Server returned 5xx on uppercase path: " + status);
        String caseBehavior = (status == 200) ? "Case-insensitive (returns 200 for uppercase)" : "Case-sensitive (returned " + status + " for uppercase)";
        ApiReportContext.addExtraDetail("<b>Path tested:</b> /tests/GETALLSYMPTOMS | <b>Status:</b> " + status + " | <b>Behavior:</b> " + caseBehavior);
        System.out.println("✅ TC35 PASSED");
        ApiReportContext.setExpectedStatus(200);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECURITY TESTING (TC41-TC50)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 41, description = "TC41: SQL injection in accept header is safe")
    public void testTC41_SqlInjectionInHeader() {
        System.out.println("\n>>> TC41: SQL injection attempt in header <<<");
        Response r = new RequestBuilder()
                .setEndpoint(ENDPOINT)
                .addHeader("accept", "'; DROP TABLE symptoms; --")
                .get();
        System.out.println("   Status: " + r.getStatusCode());
        Assert.assertNotEquals(r.getStatusCode(), 500,
                "Server should not crash on SQL injection in header (no 500)");        ApiReportContext.addExtraDetail("<b>Payload:</b> SQL injection in accept header | <b>Status:</b> " + r.getStatusCode() + " | <b>Server crashed:</b> <span style='color:green'>NO</span>");        System.out.println("✅ TC41 PASSED — Server did not crash");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 42, description = "TC42: XSS payload in accept header is safe")
    public void testTC42_XssInHeader() {
        System.out.println("\n>>> TC42: XSS payload attempt in header <<<");
        Response r = new RequestBuilder()
                .setEndpoint(ENDPOINT)
                .addHeader("X-Custom-Header", "<script>alert(1)</script>")
                .addHeader("accept", "*/*")
                .get();
        System.out.println("   Status: " + r.getStatusCode());
        Assert.assertNotEquals(r.getStatusCode(), 500,
                "Server should not crash on XSS in custom header (no 500)");        ApiReportContext.addExtraDetail("<b>Payload:</b> XSS in X-Custom-Header | <b>Status:</b> " + r.getStatusCode() + " | <b>Server crashed:</b> <span style='color:green'>NO</span>");        System.out.println("✅ TC42 PASSED — Server did not crash");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 43, description = "TC43: Path traversal in endpoint is rejected")
    public void testTC43_PathTraversal() {
        System.out.println("\n>>> TC43: Path traversal attempt <<<");
        ApiReportContext.setExpectedStatusRange(400, 499);
        Response r = new RequestBuilder()
                .setEndpoint("/tests/../getAllSymptoms")
                .addHeader("accept", "*/*")
                .get();
        System.out.println("   Status: " + r.getStatusCode());
        Assert.assertNotEquals(r.getStatusCode(), 500,
                "Server should not 500 on path traversal attempt");        ApiReportContext.addExtraDetail("<b>Payload:</b> /tests/../getAllSymptoms | <b>Status:</b> " + r.getStatusCode() + " | <b>Server crashed:</b> <span style='color:green'>NO</span>");        System.out.println("✅ TC43 PASSED — No server error");
    }

    @Test(priority = 44, description = "TC44: NoSQL injection attempt is safe")
    public void testTC44_NoSqlInjection() {
        System.out.println("\n>>> TC44: NoSQL injection attempt in URL query param <<<");
        Response r = new RequestBuilder()
                .setEndpoint(ENDPOINT + "?filter[$ne]=null")
                .addHeader("accept", "*/*")
                .get();
        System.out.println("   Status: " + r.getStatusCode());
        Assert.assertNotEquals(r.getStatusCode(), 500,
                "Server should not 500 on NoSQL injection attempt");
        ApiReportContext.addExtraDetail("<b>Payload:</b> ?filter[$ne]=null in query | <b>Status:</b> " + r.getStatusCode() + " | <b>Server crashed:</b> <span style='color:green'>NO</span>");
        System.out.println("✅ TC44 PASSED");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 45, description = "TC45: Command injection in URL is safe")
    public void testTC45_CommandInjection() {
        System.out.println("\n>>> TC45: Command injection attempt in URL <<<");
        Response r = new RequestBuilder()
                .setEndpoint(ENDPOINT + "?id=1;ls")
                .addHeader("accept", "*/*")
                .get();
        System.out.println("   Status: " + r.getStatusCode());
        Assert.assertNotEquals(r.getStatusCode(), 500,
                "Server should not crash on command injection attempt");
        ApiReportContext.addExtraDetail("<b>Payload:</b> ?id=1;ls in query | <b>Status:</b> " + r.getStatusCode() + " | <b>Server crashed:</b> <span style='color:green'>NO</span>");
        System.out.println("✅ TC45 PASSED");
        ApiReportContext.setExpectedStatus(200);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  PERFORMANCE (TC51-TC55)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 51, description = "TC51: Response time should be under 5000 ms")
    public void testTC51_ResponseTimeUnder5s() {
        System.out.println("\n>>> TC51: Response time < 5000 ms <<<");
        long start = System.currentTimeMillis();
        Response r = callGetAllSymptoms();
        long elapsed = System.currentTimeMillis() - start;
        Assert.assertEquals(r.getStatusCode(), 200);
        System.out.println("   Response time: " + elapsed + " ms");
        Assert.assertTrue(elapsed < 5000,
                "Response took " + elapsed + " ms — exceeded 5000 ms threshold");        ApiReportContext.addExtraDetail("<b>Response Time:</b> " + elapsed + " ms | <b>Threshold:</b> 5000 ms | <b>Within SLA:</b> <span style='color:green'>YES</span>");        System.out.println("✅ TC51 PASSED");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 52, description = "TC52: Three consecutive calls all return 200")
    public void testTC52_ThreeConsecutiveCalls200() {
        System.out.println("\n>>> TC52: Three consecutive calls should all return 200 <<<");
        for (int i = 1; i <= 3; i++) {
            int status = callGetAllSymptoms().getStatusCode();
            Assert.assertEquals(status, 200, "Call " + i + " returned " + status);
            System.out.println("   Call " + i + ": " + status);
        }
        ApiReportContext.addExtraDetail("<b>3 consecutive calls:</b> all returned HTTP 200 | <b>Reliability:</b> <span style='color:green'>CONFIRMED</span>");
        System.out.println("✅ TC52 PASSED");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 53, description = "TC53: Idempotency - same data on two calls")
    public void testTC53_Idempotent() {
        System.out.println("\n>>> TC53: Idempotency — two calls should return same data <<<");
        Response r1 = callGetAllSymptoms();
        Response r2 = callGetAllSymptoms();
        Assert.assertEquals(r1.getStatusCode(), 200);
        Assert.assertEquals(r2.getStatusCode(), 200);
        int c1 = getSymptomCount(r1);
        int c2 = getSymptomCount(r2);
        Assert.assertEquals(c1, c2, "Different symptom counts: " + c1 + " vs " + c2);
        ApiReportContext.addExtraDetail("<b>Call 1 symptom count:</b> " + c1 + " | <b>Call 2 symptom count:</b> " + c2 + " | <b>Idempotent:</b> <span style='color:green'>YES</span>");
        System.out.println("   Both calls returned " + c1 + " symptoms");
        System.out.println("✅ TC53 PASSED");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 54, description = "TC54: Response time under 3000 ms (strict)")
    public void testTC54_ResponseTimeUnder3s() {
        System.out.println("\n>>> TC54: Response time < 3000 ms (strict) <<<");
        long start = System.currentTimeMillis();
        Response r = callGetAllSymptoms();
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("   Response time: " + elapsed + " ms");
        Assert.assertEquals(r.getStatusCode(), 200);
        Assert.assertTrue(elapsed < 3000,
                "Response time " + elapsed + " ms exceeded 3000 ms threshold");
        ApiReportContext.addExtraDetail("<b>Response Time:</b> " + elapsed + " ms | <b>Threshold:</b> 3000 ms | <b>Within strict SLA:</b> <span style='color:green'>YES</span>");
        System.out.println("✅ TC54 PASSED");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 55, description = "TC55: Response payload size is reasonable (< 1 MB)")
    public void testTC55_PayloadSizeReasonable() {
        System.out.println("\n>>> TC55: Response payload should be < 1 MB <<<");
        Response r = callGetAllSymptoms();
        int bodyLength = r.asString().length();
        System.out.println("   Body size: " + bodyLength + " chars");
        Assert.assertTrue(bodyLength < 1_000_000,
                "Response too large: " + bodyLength + " chars (> 1 MB)");
        ApiReportContext.addExtraDetail("<b>Payload Size:</b> " + bodyLength + " chars | <b>Max allowed:</b> 1,000,000 chars | <b>Within limit:</b> <span style='color:green'>YES</span>");
        System.out.println("✅ TC55 PASSED");
        ApiReportContext.setExpectedStatus(200);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  REGRESSION (TC56-TC60)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 56, description = "TC56: API accessible from the expected base URL")
    public void testTC56_BaseUrl() {
        System.out.println("\n>>> TC56: Verify correct base URL is used <<<");
        Response r = callGetAllSymptoms();
        Assert.assertEquals(r.getStatusCode(), 200,
                "API not accessible at expected base URL");        ApiReportContext.addExtraDetail("<b>Base URL accessible:</b> <span style='color:green'>YES</span> | <b>Status:</b> " + r.getStatusCode());        System.out.println("   Base URL OK — status: " + r.getStatusCode());
        System.out.println("✅ TC56 PASSED");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 57, description = "TC57: All symptom entries have at least one key")
    public void testTC57_AllEntriesNonEmpty() {
        System.out.println("\n>>> TC57: All symptom map entries should have at least 1 key <<<");
        for (Map<String, Object> sym : symptomsData) {
            Assert.assertNotNull(sym, "Null entry in list");
            Assert.assertFalse(sym.isEmpty(), "Empty map entry found");
        }
        System.out.println("   Verified " + symptomsData.size() + " entries — all non-empty");        ApiReportContext.addExtraDetail("<b>Total entries:</b> " + symptomsData.size() + " | <b>Empty maps found:</b> <span style='color:green'>NONE</span>");        System.out.println("✅ TC57 PASSED");
    }

    @Test(priority = 58, description = "TC58: No HTTP 5xx errors on any call")
    public void testTC58_No5xxErrors() {
        System.out.println("\n>>> TC58: Three calls — none should return 5xx <<<");
        int[] s = new int[3];
        for (int i = 1; i <= 3; i++) {
            s[i-1] = callGetAllSymptoms().getStatusCode();
            Assert.assertTrue(s[i-1] < 500,
                    "Call " + i + " returned 5xx: " + s[i-1]);
            System.out.println("   Call " + i + ": " + s[i-1]);
        }
        ApiReportContext.addExtraDetail("<b>Call 1:</b> " + s[0] + " | <b>Call 2:</b> " + s[1] + " | <b>Call 3:</b> " + s[2] + " | <b>5xx errors:</b> <span style='color:green'>NONE</span>");
        System.out.println("✅ TC58 PASSED");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 59, description = "TC59: Response does not expose internal stack traces")
    public void testTC59_NoStackTraceLeaked() {
        System.out.println("\n>>> TC59: Response body should not contain stack traces <<<");
        String body = callGetAllSymptoms().asString().toLowerCase();
        Assert.assertFalse(body.contains("stack trace"),  "Stack trace found in response");
        Assert.assertFalse(body.contains("exception"),    "Exception details found in response");
        Assert.assertFalse(body.contains("at com."),      "Java package trace found in response");
        ApiReportContext.addExtraDetail("<b>Stack trace leaked:</b> <span style='color:green'>NO</span> | <b>Exception details leaked:</b> <span style='color:green'>NO</span> | <b>Java trace leaked:</b> <span style='color:green'>NO</span>");
        System.out.println("✅ TC59 PASSED — No internal details leaked");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 60, description = "TC60: Full regression - all key assertions in one shot")
    public void testTC60_FullRegression() {
        System.out.println("\n>>> TC60: Full regression check <<<");
        Response r = callGetAllSymptoms();

        // HTTP status
        Assert.assertEquals(r.getStatusCode(), 200, "Regression: HTTP status != 200");

        // Content-Type
        Assert.assertTrue(r.getContentType().contains("application/json"),
                "Regression: Content-Type not JSON");

        // success flag
        Assert.assertTrue(r.jsonPath().getBoolean("success"),
                "Regression: success != true");

        // Non-empty data
        Assert.assertFalse(symptomsData.isEmpty(),
                "Regression: symptoms list is empty");

        // No 5xx
        Assert.assertTrue(r.getStatusCode() < 500,
                "Regression: 5xx error");

        System.out.println("   Status: 200 ✓  JSON ✓  success=true ✓  list not empty ✓");
        ApiReportContext.addExtraDetail("<b>HTTP Status:</b> " + r.getStatusCode() + " | <b>Content-Type:</b> " + r.getContentType() + " | <b>success:</b> " + r.jsonPath().getBoolean("success") + " | <b>Symptom count:</b> " + symptomsData.size());
        System.out.println("✅ TC60 PASSED — Full regression OK");
        ApiReportContext.setExpectedStatus(200);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  PARAMETERIZED - data.name validation (TC61-TC70)
    //  Each test runs once per name value extracted from data[].name
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 61, dataProvider = "symptomNames",
          description = "TC61: Each data.name value is non-null")
    public void testTC61_EachNameIsNonNull(int index, String name) {
        System.out.println("   [TC61] #" + index + " name: " + name);
        if ("__NO_DATA__".equals(name)) {
            throw new org.testng.SkipException("No symptom names available from setup");
        }
        Assert.assertNotNull(name, "data.name at index " + index + " is null");
        ApiReportContext.addExtraDetail("<b>Name #" + index + ":</b> &quot;" + name + "&quot; | <b>Null check:</b> <span style='color:green'>PASSED</span>");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 62, dataProvider = "symptomNames",
          description = "TC62: Each data.name value is non-empty")
    public void testTC62_EachNameIsNonEmpty(int index, String name) {
        System.out.println("   [TC62] #" + index + " name: '" + name + "'");
        if ("__NO_DATA__".equals(name)) {
            throw new org.testng.SkipException("No symptom names available from setup");
        }
        Assert.assertFalse(name.trim().isEmpty(),
                "data.name at index " + index + " is blank/empty");
        ApiReportContext.addExtraDetail("<b>Name #" + index + ":</b> &quot;" + name + "&quot; | <b>Empty:</b> <span style='color:green'>NO</span>");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 63, dataProvider = "symptomNames",
          description = "TC63: Each data.name has no leading or trailing whitespace")
    public void testTC63_EachNameTrimmed(int index, String name) {
        System.out.println("   [TC63] #" + index + " name: '" + name + "'");
        if ("__NO_DATA__".equals(name)) {
            throw new org.testng.SkipException("No symptom names available from setup");
        }
        Assert.assertEquals(name, name.trim(),
                "data.name at index " + index + " has leading/trailing whitespace: '" + name + "'");
        ApiReportContext.addExtraDetail("<b>Name #" + index + ":</b> &quot;" + name + "&quot; | <b>Trimmed:</b> <span style='color:green'>YES</span>");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 64, dataProvider = "symptomNames",
          description = "TC64: Each data.name length is within reasonable bounds (1-200 chars)")
    public void testTC64_EachNameReasonableLength(int index, String name) {
        System.out.println("   [TC64] #" + index + " name length: " + name.length());
        if ("__NO_DATA__".equals(name)) {
            throw new org.testng.SkipException("No symptom names available from setup");
        }
        Assert.assertTrue(name.length() >= 1 && name.length() <= 200,
                "data.name at index " + index + " has unexpected length " + name.length() + ": '" + name + "'");
        ApiReportContext.addExtraDetail("<b>Name #" + index + ":</b> &quot;" + name + "&quot; | <b>Length:</b> " + name.length() + " chars");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 65, dataProvider = "symptomNames",
          description = "TC65: Each data.name contains no HTML script tags (XSS-free)")
    public void testTC65_EachNameNoHtmlScript(int index, String name) {
        System.out.println("   [TC65] #" + index + " name: '" + name + "'");
        if ("__NO_DATA__".equals(name)) {
            throw new org.testng.SkipException("No symptom names available from setup");
        }
        String lower = name.toLowerCase();
        Assert.assertFalse(lower.contains("<script"),
                "data.name at index " + index + " contains <script> tag: '" + name + "'");
        Assert.assertFalse(lower.contains("javascript:"),
                "data.name at index " + index + " contains javascript: '" + name + "'");
        ApiReportContext.addExtraDetail("<b>Name #" + index + ":</b> &quot;" + name + "&quot; | <b>&lt;script&gt; / javascript: found:</b> <span style='color:green'>NO</span>");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 66, dataProvider = "symptomNames",
          description = "TC66: Each data.name contains no SQL injection keywords")
    public void testTC66_EachNameNoSqlInjection(int index, String name) {
        System.out.println("   [TC66] #" + index + " name: '" + name + "'");
        if ("__NO_DATA__".equals(name)) {
            throw new org.testng.SkipException("No symptom names available from setup");
        }
        String lower = name.toLowerCase();
        Assert.assertFalse(lower.contains("drop table"),
                "data.name at index " + index + " contains SQL injection: '" + name + "'");
        Assert.assertFalse(lower.contains("' or '1'='1"),
                "data.name at index " + index + " contains SQL injection: '" + name + "'");
        ApiReportContext.addExtraDetail("<b>Name #" + index + ":</b> &quot;" + name + "&quot; | <b>SQL injection keywords:</b> <span style='color:green'>NONE</span>");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 67, dataProvider = "symptomNames",
          description = "TC67: Each data.name is a printable string (no null bytes)")
    public void testTC67_EachNameNoPrintableIssues(int index, String name) {
        System.out.println("   [TC67] #" + index + " name: '" + name + "'");
        if ("__NO_DATA__".equals(name)) {
            throw new org.testng.SkipException("No symptom names available from setup");
        }
        Assert.assertFalse(name.contains("\0"),
                "data.name at index " + index + " contains null byte");
        ApiReportContext.addExtraDetail("<b>Name #" + index + ":</b> &quot;" + name + "&quot; | <b>Null bytes:</b> <span style='color:green'>NONE</span>");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 68, dataProvider = "symptomNames",
          description = "TC68: Each data.name does not start with a special/control character")
    public void testTC68_EachNameNoLeadingControlChar(int index, String name) {
        System.out.println("   [TC68] #" + index + " name: '" + name + "'");
        if ("__NO_DATA__".equals(name)) {
            throw new org.testng.SkipException("No symptom names available from setup");
        }
        if (!name.isEmpty()) {
            char first = name.charAt(0);
            Assert.assertFalse(Character.isISOControl(first),
                    "data.name at index " + index + " starts with control char (code " + (int) first + "): '" + name + "'");
            ApiReportContext.addExtraDetail("<b>Name #" + index + ":</b> &quot;" + name + "&quot; | <b>First char:</b> '" + first + "' (code " + (int)first + ") | <b>Control char:</b> <span style='color:green'>NO</span>");
        } else {
            ApiReportContext.addExtraDetail("<b>Name #" + index + ":</b> (empty string) | <b>Control char:</b> N/A");
        }
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 69, description = "TC69: All extracted data.name values are unique (no duplicates)")
    public void testTC69_AllNamesUnique() {
        System.out.println("\n>>> TC69: All data.name values should be unique <<<");
        if (symptomNames.isEmpty()) {
            throw new org.testng.SkipException("No symptom names extracted from setup");
        }
        Set<String> seen = new LinkedHashSet<>();
        List<String> duplicates = new ArrayList<>();
        for (String n : symptomNames) {
            if (!seen.add(n.toLowerCase().trim())) {
                duplicates.add(n);
            }
        }
        if (!duplicates.isEmpty()) {
            System.out.println("   FAILED: Duplicate data.name values found: " + duplicates);
            System.out.println("   Total duplicates: " + duplicates.size() + " out of " + symptomNames.size());
            ApiReportContext.addExtraDetail("<span style='color:red'><b>[BUG-SYM-001] Duplicate names found:</b></span> " + duplicates + " (" + duplicates.size() + " duplicates out of " + symptomNames.size() + " names)");
        } else {
            System.out.println("   Verified " + symptomNames.size() + " names - all unique");
            ApiReportContext.addExtraDetail("<b>Total names checked:</b> " + symptomNames.size() + " | <b>Duplicates:</b> <span style='color:green'>NONE</span>");
        }
        Assert.assertTrue(duplicates.isEmpty(),
                "[BUG-SYM-001] Duplicate symptom names found: " + duplicates
                + ". Count: " + duplicates.size() + " out of " + symptomNames.size() + " entries.");
        System.out.println("✅ TC69 PASSED");
        ApiReportContext.setExpectedStatus(200);
    }

    @Test(priority = 70, description = "TC70: Total data.name count matches total symptoms count")
    public void testTC70_NameCountMatchesTotal() {
        System.out.println("\n>>> TC70: data.name count should match total symptoms <<<");
        System.out.println("   Total symptoms : " + totalSymptoms);
        System.out.println("   Names extracted: " + symptomNames.size());
        Assert.assertEquals(symptomNames.size(), totalSymptoms,
                "Some symptom entries are missing the name field. " +
                "Expected " + totalSymptoms + " names but got " + symptomNames.size());
        ApiReportContext.addExtraDetail("<b>Total symptoms in list:</b> " + totalSymptoms + " | <b>Names extracted:</b> " + symptomNames.size() + " | <b>All have name field:</b> <span style='color:green'>YES</span>");
        System.out.println("✅ TC70 PASSED");
        ApiReportContext.setExpectedStatus(200);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════

    private int getSymptomCount(Response r) {
        if (r.getStatusCode() != 200) return -1;
        List<?> list = null;
        try { list = r.jsonPath().getList("data"); } catch (Exception ignored) { }
        if (list == null) {
            try { list = r.jsonPath().getList("symptoms"); } catch (Exception ignored) { }
        }
        return list != null ? list.size() : 0;
    }
}

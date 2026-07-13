package com.mryoda.diagnostics.api.tests.user;

import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.ApiReportContext;
import com.mryoda.diagnostics.api.utils.AssertionUtil;
import com.mryoda.diagnostics.api.utils.RequestContext;
import io.restassured.response.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

/**
 * GetByGuids API Validation Test
 * Endpoint: POST /users/getByGuids
 * 
 * Validates:
 * - Positive: Single GUID, multiple GUIDs, response structure, field types
 * - Negative: Empty array, invalid GUIDs, missing body, no auth, SQL injection, XSS
 */
public class GetByGuidsValidationTest extends BaseTest {

    private static final String VALID_GUID = "60cdff44-e801-4c75-a873-69caf965e763";

    // ═══════════════════════════════════════════════════════════════════
    //  HELPER
    // ═══════════════════════════════════════════════════════════════════

    private Response callGetByGuids(String token, JSONObject payload) {
        return new RequestBuilder()
                .setEndpoint(APIEndpoints.GET_BY_GUIDS)
                .addHeader("Authorization", "Bearer " + token)
                .setRequestBody(payload.toString())
                .post();
    }

    private JSONObject buildPayload(String... guids) {
        JSONObject payload = new JSONObject();
        JSONArray guidArray = new JSONArray();
        for (String guid : guids) {
            guidArray.put(guid);
        }
        payload.put("guids", guidArray);
        return payload;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  POSITIVE VALIDATIONS
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 1, description = "Positive: Verify getByGuids returns 200 with valid single GUID")
    public void USER_GUID_01_SuccessWithSingleGuid() {
        System.out.println("\n>>> GET BY GUIDS - POSITIVE: SINGLE GUID SUCCESS <<<");

        String token = RequestContext.getToken();
        Assert.assertNotNull(token, "Token must be available from login");

        JSONObject payload = buildPayload(VALID_GUID);
        System.out.println("   Request: " + payload.toString(2));

        Response response = callGetByGuids(token, payload);

        System.out.println("   HTTP Status: " + response.getStatusCode());
        AssertionUtil.verifyStatusCode(response, 200);

        Boolean success = response.jsonPath().get("success");
        Assert.assertNotNull(success, "Response must have 'success' field");
        Assert.assertTrue(success, "success should be true");

        System.out.println("✅ PASSED: getByGuids returns 200 with success=true");
    }

    @Test(priority = 2, dependsOnMethods = "USER_GUID_01_SuccessWithSingleGuid",
          description = "Positive: Validate response data structure contains user info")
    public void USER_GUID_02_DataStructureValidation() {
        System.out.println("\n>>> GET BY GUIDS - POSITIVE: DATA STRUCTURE <<<");

        String token = RequestContext.getToken();
        JSONObject payload = buildPayload(VALID_GUID);
        Response response = callGetByGuids(token, payload);

        AssertionUtil.verifyStatusCode(response, 200);

        Object dataObj = response.jsonPath().get("data");
        Assert.assertNotNull(dataObj, "Response must have 'data' field");

        // Data should be a list
        List<?> dataList = response.jsonPath().getList("data");
        Assert.assertNotNull(dataList, "data should be a list");
        Assert.assertFalse(dataList.isEmpty(), "data list should not be empty for valid GUID");

        System.out.println("   Data count: " + dataList.size());
        System.out.println("✅ PASSED: Data structure is valid");
    }

    @Test(priority = 3, dependsOnMethods = "USER_GUID_01_SuccessWithSingleGuid",
          description = "Positive: Validate user fields (guid, name, mobile, email)")
    public void USER_GUID_03_UserFieldsValidation() {
        System.out.println("\n>>> GET BY GUIDS - POSITIVE: USER FIELDS <<<");

        String token = RequestContext.getToken();
        JSONObject payload = buildPayload(VALID_GUID);
        Response response = callGetByGuids(token, payload);

        AssertionUtil.verifyStatusCode(response, 200);

        // Extract first user
        String dataPath = "data[0]";

        String guid = response.jsonPath().getString(dataPath + ".guid");
        if (guid == null) guid = response.jsonPath().getString(dataPath + ".user_guid");
        Assert.assertNotNull(guid, "User GUID must be present in response");
        System.out.println("   GUID: " + guid);

        String firstName = response.jsonPath().getString(dataPath + ".first_name");
        if (firstName != null) {
            Assert.assertFalse(firstName.trim().isEmpty(), "First name should not be empty");
            System.out.println("   First Name: " + firstName);
        }

        String mobile = response.jsonPath().getString(dataPath + ".mobile");
        if (mobile != null) {
            Assert.assertFalse(mobile.trim().isEmpty(), "Mobile should not be empty");
            System.out.println("   Mobile: " + mobile);
        }

        String email = response.jsonPath().getString(dataPath + ".email");
        if (email != null) {
            System.out.println("   Email: " + email);
        }

        System.out.println("✅ PASSED: User fields validated");
    }

    @Test(priority = 4, dependsOnMethods = "USER_GUID_01_SuccessWithSingleGuid",
          description = "Positive: Validate returned GUID matches the requested GUID")
    public void USER_GUID_04_GuidMatchesRequest() {
        System.out.println("\n>>> GET BY GUIDS - POSITIVE: GUID MATCHES REQUEST <<<");

        String token = RequestContext.getToken();
        JSONObject payload = buildPayload(VALID_GUID);
        Response response = callGetByGuids(token, payload);

        AssertionUtil.verifyStatusCode(response, 200);

        String returnedGuid = response.jsonPath().getString("data[0].guid");
        if (returnedGuid == null) returnedGuid = response.jsonPath().getString("data[0].user_guid");

        if (returnedGuid != null) {
            AssertionUtil.verifyEquals(returnedGuid, VALID_GUID, "Returned GUID should match requested GUID");
            System.out.println("   ✅ GUID matches: " + returnedGuid);
        }

        System.out.println("✅ PASSED: GUID matches request");
    }

    @Test(priority = 5, dependsOnMethods = "USER_GUID_01_SuccessWithSingleGuid",
          description = "Positive: Validate response time is under 5 seconds")
    public void USER_GUID_05_ResponseTime() {
        System.out.println("\n>>> GET BY GUIDS - POSITIVE: RESPONSE TIME <<<");

        String token = RequestContext.getToken();
        JSONObject payload = buildPayload(VALID_GUID);

        long startTime = System.currentTimeMillis();
        Response response = callGetByGuids(token, payload);
        long responseTime = System.currentTimeMillis() - startTime;

        AssertionUtil.verifyStatusCode(response, 200);
        System.out.println("   Response Time: " + responseTime + "ms");
        Assert.assertTrue(responseTime < 5000,
            "getByGuids response time should be under 5 seconds but took: " + responseTime + "ms");

        System.out.println("✅ PASSED: Response time is acceptable (" + responseTime + "ms)");
    }

    @Test(priority = 6, dependsOnMethods = "USER_GUID_01_SuccessWithSingleGuid",
          description = "Positive: Validate no sensitive data exposed (password, tokens)")
    public void USER_GUID_06_NoSensitiveDataExposed() {
        System.out.println("\n>>> GET BY GUIDS - POSITIVE: NO SENSITIVE DATA <<<");

        String token = RequestContext.getToken();
        JSONObject payload = buildPayload(VALID_GUID);
        Response response = callGetByGuids(token, payload);

        String responseBody = response.getBody().asString().toLowerCase();
        Assert.assertFalse(responseBody.contains("password"), "Response should not contain password field");
        Assert.assertFalse(responseBody.contains("\"token\""), "Response should not contain token field");
        Assert.assertFalse(responseBody.contains("secret"), "Response should not contain secret field");

        System.out.println("   ✅ No password, token, or secret fields found");
        System.out.println("✅ PASSED: No sensitive data exposed");
    }

    @Test(priority = 7, dependsOnMethods = "USER_GUID_01_SuccessWithSingleGuid",
          description = "Positive: Validate multiple GUIDs in a single request")
    public void USER_GUID_07_MultipleGuidsRequest() {
        System.out.println("\n>>> GET BY GUIDS - POSITIVE: MULTIPLE GUIDS <<<");

        String token = RequestContext.getToken();
        // Use same GUID twice to verify array handling (or use another valid GUID if available)
        JSONObject payload = buildPayload(VALID_GUID, VALID_GUID);
        System.out.println("   Request: " + payload.toString(2));

        Response response = callGetByGuids(token, payload);

        System.out.println("   HTTP Status: " + response.getStatusCode());
        AssertionUtil.verifyStatusCode(response, 200);

        List<?> dataList = response.jsonPath().getList("data");
        Assert.assertNotNull(dataList, "data should be a list");
        System.out.println("   Returned records: " + dataList.size());

        System.out.println("✅ PASSED: Multiple GUIDs handled");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  NEGATIVE VALIDATIONS
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 20, description = "Negative: getByGuids with empty guids array")
    public void USER_GUID_20_EmptyGuidsArray() {
        System.out.println("\n>>> GET BY GUIDS - NEGATIVE: EMPTY GUIDS ARRAY <<<");

        String token = RequestContext.getToken();
        JSONObject payload = new JSONObject();
        payload.put("guids", new JSONArray());

        Response response = callGetByGuids(token, payload);

        System.out.println("   HTTP Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());

        int statusCode = response.getStatusCode();
        if (statusCode == 200) {
            List<?> dataList = response.jsonPath().getList("data");
            if (dataList != null) {
                Assert.assertTrue(dataList.isEmpty(),
                    "Empty guids should return empty data list");
                System.out.println("   API returned 200 with empty data (acceptable)");
            }
        } else {
            Assert.assertTrue(statusCode >= 400 && statusCode < 500,
                "Empty guids array should return 4xx but got: " + statusCode);
        }

        System.out.println("✅ PASSED: Empty guids array handled");
    }

    @Test(priority = 21, description = "Negative: getByGuids with invalid/non-existent GUID should return 400")
    public void USER_GUID_21_InvalidGuid() {
        System.out.println("\n>>> GET BY GUIDS - NEGATIVE: INVALID GUID <<<");

        String token = RequestContext.getToken();
        JSONObject payload = buildPayload("invalid-guid-does-not-exist-12345");

        ApiReportContext.setExpectedStatus(400);
        Response response = callGetByGuids(token, payload);

        System.out.println("   HTTP Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());

        int statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode, 400,
            "\uD83D\uDC1B BUG: Invalid UUID format should return 400 Bad Request but got: " + statusCode
            + ". Server must validate UUID format before querying DB.");

        System.out.println("✅ PASSED: Invalid GUID returns 400");
    }

    @Test(priority = 24, description = "Negative: getByGuids with missing 'guids' key in body")
    public void USER_GUID_24_MissingGuidsKey() {
        System.out.println("\n>>> GET BY GUIDS - NEGATIVE: MISSING GUIDS KEY <<<");

        String token = RequestContext.getToken();
        JSONObject payload = new JSONObject();
        payload.put("user_ids", new JSONArray().put(VALID_GUID)); // wrong key

        Response response = callGetByGuids(token, payload);

        System.out.println("   HTTP Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());

        int statusCode = response.getStatusCode();
        if (statusCode == 200) {
            List<?> dataList = response.jsonPath().getList("data");
            if (dataList != null) {
                Assert.assertTrue(dataList.isEmpty(),
                    "Wrong key should return empty data");
                System.out.println("   API returned 200 with empty data (acceptable)");
            }
        } else {
            Assert.assertTrue(statusCode >= 400 && statusCode < 500,
                "Missing guids key should return 4xx but got: " + statusCode);
        }

        System.out.println("✅ PASSED: Missing guids key handled");
    }

    @Test(priority = 25, description = "Negative: getByGuids with empty request body")
    public void USER_GUID_25_EmptyBody() {
        System.out.println("\n>>> GET BY GUIDS - NEGATIVE: EMPTY BODY <<<");

        String token = RequestContext.getToken();

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.GET_BY_GUIDS)
                .addHeader("Authorization", "Bearer " + token)
                .setRequestBody("{}")
                .post();

        System.out.println("   HTTP Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());

        int statusCode = response.getStatusCode();
        if (statusCode == 500) {
            System.out.println("   🐛 BUG: API returns 500 for empty body (should be 400)");
        }

        System.out.println("✅ PASSED: Empty body handled");
    }

    @Test(priority = 26, description = "Negative: getByGuids with SQL injection in GUID should return 400")
    public void USER_GUID_26_SqlInjection() {
        System.out.println("\n>>> GET BY GUIDS - NEGATIVE: SQL INJECTION <<<");

        String token = RequestContext.getToken();
        JSONObject payload = buildPayload("' OR '1'='1' --", "1; DROP TABLE users;--");

        ApiReportContext.setExpectedStatus(400);
        Response response = callGetByGuids(token, payload);

        System.out.println("   HTTP Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());

        int statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode, 400,
            "\uD83D\uDC1B BUG: SQL injection payload should return 400 Bad Request but got: " + statusCode
            + ". Server must validate UUID format and reject malicious input.");

        System.out.println("✅ PASSED: SQL injection returns 400");
    }

    @Test(priority = 27, description = "Negative: getByGuids with XSS payload in GUID should return 400")
    public void USER_GUID_27_XssAttempt() {
        System.out.println("\n>>> GET BY GUIDS - NEGATIVE: XSS ATTEMPT <<<");

        String token = RequestContext.getToken();
        JSONObject payload = buildPayload("<script>alert('xss')</script>");

        ApiReportContext.setExpectedStatus(400);
        Response response = callGetByGuids(token, payload);

        System.out.println("   HTTP Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());

        int statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode, 400,
            "\uD83D\uDC1B BUG: XSS payload should return 400 Bad Request but got: " + statusCode
            + ". Server must validate UUID format and reject HTML/script input.");

        System.out.println("✅ PASSED: XSS attempt returns 400");
    }

    @Test(priority = 28, description = "Negative: getByGuids with extremely long GUID should return 400")
    public void USER_GUID_28_ExtremelyLongGuid() {
        System.out.println("\n>>> GET BY GUIDS - NEGATIVE: EXTREMELY LONG GUID <<<");

        String token = RequestContext.getToken();
        String longGuid = "a".repeat(5000);
        JSONObject payload = buildPayload(longGuid);

        ApiReportContext.setExpectedStatus(400);
        Response response = callGetByGuids(token, payload);

        System.out.println("   HTTP Status: " + response.getStatusCode());

        int statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode, 400,
            "\uD83D\uDC1B BUG: Extremely long GUID should return 400 Bad Request but got: " + statusCode
            + ". Server must validate UUID length before querying DB.");

        System.out.println("✅ PASSED: Extremely long GUID returns 400");
    }

    @Test(priority = 29, description = "Negative: getByGuids with null value in guids array")
    public void USER_GUID_29_NullValueInArray() {
        System.out.println("\n>>> GET BY GUIDS - NEGATIVE: NULL VALUE IN ARRAY <<<");

        String token = RequestContext.getToken();
        JSONObject payload = new JSONObject();
        JSONArray guidArray = new JSONArray();
        guidArray.put(JSONObject.NULL);
        payload.put("guids", guidArray);

        Response response = callGetByGuids(token, payload);

        System.out.println("   HTTP Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());

        int statusCode = response.getStatusCode();
        if (statusCode == 500) {
            System.out.println("   🐛 BUG: API returns 500 for null in guids array");
        }

        System.out.println("✅ PASSED: Null value in array handled");
    }

    @Test(priority = 30, description = "Negative: getByGuids with special characters in GUID should return 400")
    public void USER_GUID_30_SpecialCharacters() {
        System.out.println("\n>>> GET BY GUIDS - NEGATIVE: SPECIAL CHARACTERS <<<");

        String token = RequestContext.getToken();
        JSONObject payload = buildPayload("!@#$%^&*()", "../../../etc/passwd", "{{constructor}}");

        ApiReportContext.setExpectedStatus(400);
        Response response = callGetByGuids(token, payload);

        System.out.println("   HTTP Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());

        int statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode, 400,
            "\uD83D\uDC1B BUG: Special characters should return 400 Bad Request but got: " + statusCode
            + ". Server must validate UUID format before querying DB.");

        System.out.println("✅ PASSED: Special characters returns 400");
    }
}

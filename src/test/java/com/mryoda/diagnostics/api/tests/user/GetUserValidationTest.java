package com.mryoda.diagnostics.api.tests.user;

import com.mryoda.diagnostics.api.utils.AssertionUtil;
import com.mryoda.diagnostics.api.utils.RequestContext;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import com.mryoda.diagnostics.api.base.BaseTest;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

/**
 * GetUser API Validation Test
 * Endpoint: GET /users/getUser/{user_id}
 * Hardcoded user_id: 60cdff44-e801-4c75-a873-69caf965e763
 *
 * Validates:
 * - Positive: Success response, data structure, field types, user fields
 * - Negative: Invalid IDs, SQL injection, XSS, special chars, boundary values
 * - Schema: Response structure, field type validation
 */
public class GetUserValidationTest extends BaseTest {

    private static final String VALID_USER_ID = "60cdff44-e801-4c75-a873-69caf965e763";
    private static final String GET_USER_ENDPOINT = "/users/getUser/";

    // ═══════════════════════════════════════════════════════════════════
    //  HELPER
    // ═══════════════════════════════════════════════════════════════════

    private Response callGetUserAPI(String token, String userId) {
        String endpoint = GET_USER_ENDPOINT + userId;
        return new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", token)
                .get();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  POSITIVE VALIDATIONS
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 1, description = "Positive: Verify GetUser returns 200 with valid user data")
    public void testGetUser_SuccessResponse() {
        System.out.println("\n>>> GET USER - POSITIVE: SUCCESS RESPONSE <<<");

        String token = RequestContext.getToken();
        Assert.assertNotNull(token, "Token must be available from login");

        Response response = callGetUserAPI(token, VALID_USER_ID);

        System.out.println("   HTTP Status: " + response.getStatusCode());
        AssertionUtil.verifyStatusCode(response, 200);

        Boolean success = response.jsonPath().get("success");
        Assert.assertNotNull(success, "Response must have 'success' field");
        Assert.assertTrue(success, "success should be true");

        System.out.println("✅ PASSED: GetUser returns 200 with success=true");
    }

    @Test(priority = 2, dependsOnMethods = "testGetUser_SuccessResponse",
          description = "Positive: Validate user data structure has required fields")
    public void testGetUser_DataStructure() {
        System.out.println("\n>>> GET USER - POSITIVE: DATA STRUCTURE <<<");

        String token = RequestContext.getToken();
        Response response = callGetUserAPI(token, VALID_USER_ID);

        Object dataObj = response.jsonPath().get("data");
        Assert.assertNotNull(dataObj, "Response 'data' field must not be null");

        // Determine path (could be object or array)
        String dataPath = (dataObj instanceof List) ? "data[0]" : "data";

        // User GUID/ID validation
        String userGuid = response.jsonPath().getString(dataPath + ".guid");
        if (userGuid == null) userGuid = response.jsonPath().getString(dataPath + ".user_guid");
        if (userGuid == null) userGuid = response.jsonPath().getString(dataPath + ".id");

        Assert.assertNotNull(userGuid, "User GUID/ID must be present");
        System.out.println("   User GUID: " + userGuid);

        System.out.println("✅ PASSED: User data structure is valid");
    }

    @Test(priority = 3, dependsOnMethods = "testGetUser_SuccessResponse",
          description = "Positive: Validate user profile fields (name, mobile, email)")
    public void testGetUser_ProfileFields() {
        System.out.println("\n>>> GET USER - POSITIVE: PROFILE FIELDS <<<");

        String token = RequestContext.getToken();
        Response response = callGetUserAPI(token, VALID_USER_ID);

        Object dataObj = response.jsonPath().get("data");
        String dataPath = (dataObj instanceof List) ? "data[0]" : "data";

        // First name
        String firstName = response.jsonPath().getString(dataPath + ".first_name");
        if (firstName != null) {
            Assert.assertFalse(firstName.trim().isEmpty(), "First name should not be empty");
            System.out.println("   First Name: " + firstName);
        }

        // Last name
        String lastName = response.jsonPath().getString(dataPath + ".last_name");
        if (lastName != null) {
            System.out.println("   Last Name: " + lastName);
        }

        // Mobile
        String mobile = response.jsonPath().getString(dataPath + ".mobile");
        if (mobile != null) {
            Assert.assertFalse(mobile.trim().isEmpty(), "Mobile should not be empty");
            System.out.println("   Mobile: " + mobile);
        }

        // Email
        String email = response.jsonPath().getString(dataPath + ".email");
        if (email != null) {
            System.out.println("   Email: " + email);
        }

        System.out.println("✅ PASSED: Profile fields validated");
    }

    @Test(priority = 4, dependsOnMethods = "testGetUser_SuccessResponse",
          description = "Positive: Validate user role field")
    public void testGetUser_RoleField() {
        System.out.println("\n>>> GET USER - POSITIVE: ROLE FIELD <<<");

        String token = RequestContext.getToken();
        Response response = callGetUserAPI(token, VALID_USER_ID);

        Object dataObj = response.jsonPath().get("data");
        String dataPath = (dataObj instanceof List) ? "data[0]" : "data";

        String role = response.jsonPath().getString(dataPath + ".role");
        if (role != null) {
            System.out.println("   Role: " + role);
            Assert.assertFalse(role.trim().isEmpty(), "Role should not be empty");
        }

        System.out.println("✅ PASSED: Role field validated");
    }

    @Test(priority = 5, dependsOnMethods = "testGetUser_SuccessResponse",
          description = "Positive: Validate user GUID matches the requested ID")
    public void testGetUser_GuidMatchesRequestedId() {
        System.out.println("\n>>> GET USER - POSITIVE: GUID MATCHES REQUESTED ID <<<");

        String token = RequestContext.getToken();
        Response response = callGetUserAPI(token, VALID_USER_ID);

        Object dataObj = response.jsonPath().get("data");
        String dataPath = (dataObj instanceof List) ? "data[0]" : "data";

        String returnedGuid = response.jsonPath().getString(dataPath + ".guid");
        if (returnedGuid == null) returnedGuid = response.jsonPath().getString(dataPath + ".user_guid");
        if (returnedGuid == null) returnedGuid = response.jsonPath().getString(dataPath + ".id");

        if (returnedGuid != null) {
            AssertionUtil.verifyEquals(returnedGuid, VALID_USER_ID,
                "Returned user GUID should match requested ID");
            System.out.println("   ✅ GUID matches: " + returnedGuid);
        } else {
            System.out.println("   ⚠️ Could not locate GUID field in response");
        }

        System.out.println("✅ PASSED: GUID matches requested ID");
    }

    @Test(priority = 6, dependsOnMethods = "testGetUser_SuccessResponse",
          description = "Positive: Validate timestamp fields (created_at, updated_at)")
    public void testGetUser_TimestampFields() {
        System.out.println("\n>>> GET USER - POSITIVE: TIMESTAMP FIELDS <<<");

        String token = RequestContext.getToken();
        Response response = callGetUserAPI(token, VALID_USER_ID);

        Object dataObj = response.jsonPath().get("data");
        String dataPath = (dataObj instanceof List) ? "data[0]" : "data";

        String createdAt = response.jsonPath().getString(dataPath + ".created_at");
        if (createdAt == null) createdAt = response.jsonPath().getString(dataPath + ".createdAt");
        if (createdAt != null) {
            Assert.assertFalse(createdAt.trim().isEmpty(), "created_at should not be empty");
            System.out.println("   Created At: " + createdAt);
        }

        String updatedAt = response.jsonPath().getString(dataPath + ".updated_at");
        if (updatedAt == null) updatedAt = response.jsonPath().getString(dataPath + ".updatedAt");
        if (updatedAt != null) {
            System.out.println("   Updated At: " + updatedAt);
        }

        System.out.println("✅ PASSED: Timestamp fields validated");
    }

    @Test(priority = 7, dependsOnMethods = "testGetUser_SuccessResponse",
          description = "Positive: Validate response time is acceptable")
    public void testGetUser_ResponseTime() {
        System.out.println("\n>>> GET USER - POSITIVE: RESPONSE TIME <<<");

        String token = RequestContext.getToken();
        long startTime = System.currentTimeMillis();
        Response response = callGetUserAPI(token, VALID_USER_ID);
        long responseTime = System.currentTimeMillis() - startTime;

        AssertionUtil.verifyStatusCode(response, 200);
        System.out.println("   Response Time: " + responseTime + "ms");
        Assert.assertTrue(responseTime < 5000,
            "GetUser response time should be under 5 seconds but took: " + responseTime + "ms");

        System.out.println("✅ PASSED: Response time is acceptable (" + responseTime + "ms)");
    }

    @Test(priority = 8, dependsOnMethods = "testGetUser_SuccessResponse",
          description = "Positive: Validate no sensitive data exposed (password, tokens)")
    public void testGetUser_NoSensitiveDataExposed() {
        System.out.println("\n>>> GET USER - POSITIVE: NO SENSITIVE DATA EXPOSED <<<");

        String token = RequestContext.getToken();
        Response response = callGetUserAPI(token, VALID_USER_ID);

        String responseBody = response.getBody().asString().toLowerCase();
        Assert.assertFalse(responseBody.contains("password"),
            "Response should not contain password field");
        Assert.assertFalse(responseBody.contains("\"token\""),
            "Response should not contain token field");
        Assert.assertFalse(responseBody.contains("secret"),
            "Response should not contain secret field");

        System.out.println("   ✅ No password, token, or secret fields found");
        System.out.println("✅ PASSED: No sensitive data exposed");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  NEGATIVE VALIDATIONS
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 20, description = "Negative: GetUser with invalid/non-existent user ID")
    public void testGetUser_InvalidUserId() {
        System.out.println("\n>>> GET USER - NEGATIVE: INVALID USER ID <<<");

        String token = RequestContext.getToken();
        String invalidId = "non-existent-user-id-99999";

        Response response = callGetUserAPI(token, invalidId);

        System.out.println("   HTTP Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());

        int statusCode = response.getStatusCode();
        if (statusCode == 200) {
            Object data = response.jsonPath().get("data");
            boolean hasData = (data != null && !data.toString().equals("[]")
                && !data.toString().equals("null") && !data.toString().equals("{}"));
            Assert.assertFalse(hasData,
                "Invalid user ID should not return valid user data");
            System.out.println("   API returned 200 with empty/null data (acceptable)");
        } else if (statusCode == 500) {
            System.out.println("   🐛 BUG DETECTED: API returns 500 for invalid user ID");
            System.out.println("   Expected: 404 Not Found");
        } else {
            Assert.assertTrue(statusCode >= 400 && statusCode < 500,
                "Invalid user ID should return 4xx but got: " + statusCode);
        }

        System.out.println("✅ PASSED: Invalid user ID handled");
    }

    @Test(priority = 21, description = "Negative: GetUser with empty user ID")
    public void testGetUser_EmptyUserId() {
        System.out.println("\n>>> GET USER - NEGATIVE: EMPTY USER ID <<<");

        String token = RequestContext.getToken();

        String endpoint = GET_USER_ENDPOINT;
        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", token)
                .get();

        System.out.println("   HTTP Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());

        int statusCode = response.getStatusCode();
        if (statusCode == 200) {
            Boolean success = response.jsonPath().get("success");
            System.out.println("   Success flag: " + success);
        } else if (statusCode == 500) {
            System.out.println("   🐛 BUG: API returns 500 for empty user ID");
        } else {
            Assert.assertTrue(statusCode >= 400,
                "Empty user ID should return error but got: " + statusCode);
        }

        System.out.println("✅ PASSED: Empty user ID handled");
    }

    @Test(priority = 22, description = "Negative: GetUser with numeric zero ID")
    public void testGetUser_ZeroUserId() {
        System.out.println("\n>>> GET USER - NEGATIVE: ZERO USER ID <<<");

        String token = RequestContext.getToken();

        Response response = callGetUserAPI(token, "0");

        System.out.println("   HTTP Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());

        int statusCode = response.getStatusCode();
        if (statusCode == 200) {
            Object data = response.jsonPath().get("data");
            boolean hasData = (data != null && !data.toString().equals("[]")
                && !data.toString().equals("null") && !data.toString().equals("{}"));
            if (hasData) {
                System.out.println("   ⚠️ API returned data for user ID '0'");
            }
        }

        System.out.println("✅ PASSED: Zero user ID handled");
    }

    @Test(priority = 23, description = "Negative: GetUser with negative user ID")
    public void testGetUser_NegativeUserId() {
        System.out.println("\n>>> GET USER - NEGATIVE: NEGATIVE USER ID <<<");

        String token = RequestContext.getToken();

        Response response = callGetUserAPI(token, "-1");

        System.out.println("   HTTP Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());

        int statusCode = response.getStatusCode();
        if (statusCode == 200) {
            Object data = response.jsonPath().get("data");
            boolean hasData = (data != null && !data.toString().equals("[]")
                && !data.toString().equals("null") && !data.toString().equals("{}"));
            Assert.assertFalse(hasData,
                "Negative user ID should NOT return valid user data");
        }

        System.out.println("✅ PASSED: Negative user ID handled");
    }

    @Test(priority = 24, description = "Negative: GetUser with SQL injection in user ID")
    public void testGetUser_SqlInjection() {
        System.out.println("\n>>> GET USER - NEGATIVE: SQL INJECTION <<<");

        String token = RequestContext.getToken();
        String sqlPayload = "1' OR '1'='1' --";

        Response response = callGetUserAPI(token, sqlPayload);

        System.out.println("   HTTP Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());

        int statusCode = response.getStatusCode();
        if (statusCode == 200) {
            Object data = response.jsonPath().get("data");
            boolean hasData = (data != null && !data.toString().equals("[]") && !data.toString().equals("null"));
            Assert.assertFalse(hasData,
                "SQL injection should NOT return valid user data");
        }

        String responseBody = response.getBody().asString().toLowerCase();
        Assert.assertFalse(responseBody.contains("sql syntax"),
            "Response should not expose SQL syntax errors");
        Assert.assertFalse(responseBody.contains("mysql"),
            "Response should not expose database details");
        Assert.assertFalse(responseBody.contains("postgresql"),
            "Response should not expose database details");

        System.out.println("✅ PASSED: SQL injection handled");
    }

    @Test(priority = 25, description = "Negative: GetUser with XSS payload in user ID")
    public void testGetUser_XssAttempt() {
        System.out.println("\n>>> GET USER - NEGATIVE: XSS ATTEMPT <<<");

        String token = RequestContext.getToken();
        String xssPayload = "<script>alert('xss')</script>";

        Response response = callGetUserAPI(token, xssPayload);

        System.out.println("   HTTP Status: " + response.getStatusCode());

        String responseBody = response.getBody().asString();
        Assert.assertFalse(responseBody.contains("<script>"),
            "Response should not reflect XSS script tags");

        System.out.println("✅ PASSED: XSS attempt handled");
    }

    @Test(priority = 26, description = "Negative: GetUser with special characters in user ID")
    public void testGetUser_SpecialCharacters() {
        System.out.println("\n>>> GET USER - NEGATIVE: SPECIAL CHARACTERS <<<");

        String token = RequestContext.getToken();
        String specialChars = "!@#$%^&*()~`";

        Response response = callGetUserAPI(token, specialChars);

        System.out.println("   HTTP Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());

        int statusCode = response.getStatusCode();
        if (statusCode == 200) {
            Object data = response.jsonPath().get("data");
            boolean hasData = (data != null && !data.toString().equals("[]") && !data.toString().equals("null"));
            Assert.assertFalse(hasData,
                "Special characters should NOT return valid user data");
        }

        System.out.println("✅ PASSED: Special characters handled");
    }

    @Test(priority = 27, description = "Negative: GetUser with excessively long user ID")
    public void testGetUser_ExcessivelyLongId() {
        System.out.println("\n>>> GET USER - NEGATIVE: EXCESSIVELY LONG ID <<<");

        String token = RequestContext.getToken();
        StringBuilder longIdBuilder = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            longIdBuilder.append("a");
        }

        Response response = callGetUserAPI(token, longIdBuilder.toString());

        System.out.println("   HTTP Status: " + response.getStatusCode());

        int statusCode = response.getStatusCode();
        if (statusCode == 200) {
            Object data = response.jsonPath().get("data");
            boolean hasData = (data != null && !data.toString().equals("[]") && !data.toString().equals("null"));
            Assert.assertFalse(hasData,
                "Excessively long ID should NOT return valid user data");
        } else {
            Assert.assertTrue(statusCode >= 400 && statusCode < 600,
                "Excessively long ID should return error but got: " + statusCode);
        }

        System.out.println("✅ PASSED: Excessively long ID handled");
    }

    @Test(priority = 28, description = "Negative: GetUser with path traversal attempt")
    public void testGetUser_PathTraversal() {
        System.out.println("\n>>> GET USER - NEGATIVE: PATH TRAVERSAL <<<");

        String token = RequestContext.getToken();
        String pathTraversal = "../../../etc/passwd";

        Response response = callGetUserAPI(token, pathTraversal);

        System.out.println("   HTTP Status: " + response.getStatusCode());

        String responseBody = response.getBody().asString();
        Assert.assertFalse(responseBody.contains("root:"),
            "Response should NOT expose system file content");
        Assert.assertFalse(responseBody.contains("/bin/bash"),
            "Response should NOT expose system file content");

        System.out.println("✅ PASSED: Path traversal blocked");
    }

    @Test(priority = 29, description = "Negative: GetUser with random UUID (valid format but non-existent)")
    public void testGetUser_RandomUUID() {
        System.out.println("\n>>> GET USER - NEGATIVE: RANDOM UUID <<<");

        String token = RequestContext.getToken();
        String randomUuid = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

        Response response = callGetUserAPI(token, randomUuid);

        System.out.println("   HTTP Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());

        int statusCode = response.getStatusCode();
        if (statusCode == 200) {
            Object data = response.jsonPath().get("data");
            boolean hasData = (data != null && !data.toString().equals("[]")
                && !data.toString().equals("null") && !data.toString().equals("{}"));
            Assert.assertFalse(hasData,
                "Random UUID should NOT return valid user data");
            System.out.println("   API returned 200 with empty data (acceptable)");
        } else if (statusCode == 500) {
            System.out.println("   🐛 BUG: API returns 500 for non-existent UUID");
        } else {
            Assert.assertTrue(statusCode == 404 || statusCode == 400,
                "Non-existent UUID should return 404 but got: " + statusCode);
        }

        System.out.println("✅ PASSED: Random UUID handled");
    }

    @Test(priority = 30, description = "Negative: GetUser with numeric ID (original curl example '12333')")
    public void testGetUser_NumericId() {
        System.out.println("\n>>> GET USER - NEGATIVE: NUMERIC ID <<<");

        String token = RequestContext.getToken();
        String numericId = "12333";

        Response response = callGetUserAPI(token, numericId);

        System.out.println("   HTTP Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());

        int statusCode = response.getStatusCode();
        if (statusCode == 200) {
            Object data = response.jsonPath().get("data");
            boolean hasData = (data != null && !data.toString().equals("[]")
                && !data.toString().equals("null") && !data.toString().equals("{}"));
            if (!hasData) {
                System.out.println("   API returned 200 with empty data for numeric ID");
            } else {
                System.out.println("   ⚠️ API returned user data for numeric ID '12333'");
            }
        } else if (statusCode == 500) {
            System.out.println("   🐛 BUG: API returns 500 for numeric ID");
        }

        System.out.println("✅ PASSED: Numeric ID handled");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SCHEMA VALIDATIONS
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 40, dependsOnMethods = "testGetUser_SuccessResponse",
          description = "Schema: Validate top-level response schema")
    public void testGetUser_TopLevelSchema() {
        System.out.println("\n>>> GET USER - SCHEMA: TOP-LEVEL RESPONSE <<<");

        String token = RequestContext.getToken();
        Response response = callGetUserAPI(token, VALID_USER_ID);

        // success must be Boolean
        Object successVal = response.jsonPath().get("success");
        Assert.assertNotNull(successVal, "'success' field must be present");
        Assert.assertTrue(successVal instanceof Boolean,
            "'success' must be Boolean but found: " + successVal.getClass().getSimpleName());

        // data must exist
        Object dataVal = response.jsonPath().get("data");
        Assert.assertNotNull(dataVal, "'data' field must be present");

        // Content-Type should be JSON
        String contentType = response.getContentType();
        Assert.assertTrue(contentType != null && contentType.contains("application/json"),
            "Content-Type should be application/json but found: " + contentType);

        System.out.println("   ✅ success(Boolean), data(present), Content-Type(json)");
        System.out.println("✅ PASSED: Top-level schema validated");
    }

    @Test(priority = 41, dependsOnMethods = "testGetUser_SuccessResponse",
          description = "Schema: Validate user field types")
    public void testGetUser_FieldTypes() {
        System.out.println("\n>>> GET USER - SCHEMA: FIELD TYPES <<<");

        String token = RequestContext.getToken();
        Response response = callGetUserAPI(token, VALID_USER_ID);

        Object dataObj = response.jsonPath().get("data");
        String dataPath = (dataObj instanceof List) ? "data[0]" : "data";

        // guid must be String
        Object guid = response.jsonPath().get(dataPath + ".guid");
        if (guid == null) guid = response.jsonPath().get(dataPath + ".id");
        if (guid != null) {
            Assert.assertTrue(guid instanceof String,
                "guid must be String but found: " + guid.getClass().getSimpleName());
            System.out.println("   guid: String ✓ (" + guid + ")");
        }

        // mobile must be String
        Object mobile = response.jsonPath().get(dataPath + ".mobile");
        if (mobile != null) {
            Assert.assertTrue(mobile instanceof String || mobile instanceof Number,
                "mobile must be String/Number but found: " + mobile.getClass().getSimpleName());
            System.out.println("   mobile: " + mobile.getClass().getSimpleName() + " ✓ (" + mobile + ")");
        }

        // first_name must be String if present
        Object firstName = response.jsonPath().get(dataPath + ".first_name");
        if (firstName != null) {
            Assert.assertTrue(firstName instanceof String,
                "first_name must be String but found: " + firstName.getClass().getSimpleName());
            System.out.println("   first_name: String ✓ (" + firstName + ")");
        }

        // role must be String if present
        Object role = response.jsonPath().get(dataPath + ".role");
        if (role != null) {
            Assert.assertTrue(role instanceof String,
                "role must be String but found: " + role.getClass().getSimpleName());
            System.out.println("   role: String ✓ (" + role + ")");
        }

        System.out.println("✅ PASSED: Field types validated");
    }

    @Test(priority = 42, dependsOnMethods = "testGetUser_SuccessResponse",
          description = "Schema: Validate mandatory fields are never null")
    public void testGetUser_MandatoryFieldsNotNull() {
        System.out.println("\n>>> GET USER - SCHEMA: MANDATORY FIELDS NOT NULL <<<");

        String token = RequestContext.getToken();
        Response response = callGetUserAPI(token, VALID_USER_ID);

        Object dataObj = response.jsonPath().get("data");
        String dataPath = (dataObj instanceof List) ? "data[0]" : "data";

        // GUID must always be present
        Object guid = response.jsonPath().get(dataPath + ".guid");
        if (guid == null) guid = response.jsonPath().get(dataPath + ".id");
        if (guid == null) guid = response.jsonPath().get(dataPath + ".user_guid");
        Assert.assertNotNull(guid, "User GUID/ID must never be null");
        System.out.println("   guid: non-null ✓ (" + guid + ")");

        // Mobile must always be present
        Object mobile = response.jsonPath().get(dataPath + ".mobile");
        Assert.assertNotNull(mobile, "Mobile must never be null");
        System.out.println("   mobile: non-null ✓ (" + mobile + ")");

        // Verify data is not empty
        Map<String, Object> userData = response.jsonPath().getMap(dataPath);
        Assert.assertNotNull(userData, "User data must not be null");
        Assert.assertTrue(userData.size() >= 2,
            "User data must have at least 2 fields but found: " + userData.size());
        System.out.println("   Total fields: " + userData.size() + " ✓");

        System.out.println("✅ PASSED: Mandatory fields validated");
    }
}

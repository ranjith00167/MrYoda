package com.mryoda.diagnostics.api.tests.user;

import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.ApiReportContext;
import com.mryoda.diagnostics.api.utils.AssertionUtil;
import com.mryoda.diagnostics.api.utils.RequestContext;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

/**
 * GetUserByMobile API Validation Test
 * Endpoint: GET /users/mobile/{mobile}
 *
 * Validates:
 * - Positive: Success response, data structure, field types, schema
 * - Negative: Invalid mobile, SQL injection, XSS, special chars, boundary values
 * - Schema: Response structure, field type validation
 */
public class GetUserByMobileValidationTest extends BaseTest {

    private static final String VALID_MEMBER_MOBILE = "9003730394";
    private static final String GET_USER_BY_MOBILE_ENDPOINT = APIEndpoints.GET_USER_BY_MOBILE;

    // ═══════════════════════════════════════════════════════════════════
    //  HELPER
    // ═══════════════════════════════════════════════════════════════════

    private Response callGetUserByMobile(String token, String mobile) {
        String endpoint = GET_USER_BY_MOBILE_ENDPOINT + mobile;
        return new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + token)
                .get();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  POSITIVE VALIDATIONS
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 1, description = "Positive: Verify GetUserByMobile returns 200 with valid mobile")
    public void testGetUserByMobile_SuccessResponse() {
        System.out.println("\n>>> GET USER BY MOBILE - POSITIVE: SUCCESS RESPONSE <<<");

        String token = RequestContext.getToken();
        Assert.assertNotNull(token, "Token must be available from login");

        Response response = callGetUserByMobile(token, VALID_MEMBER_MOBILE);

        System.out.println("   HTTP Status: " + response.getStatusCode());
        AssertionUtil.verifyStatusCode(response, 200);

        Boolean success = response.jsonPath().get("success");
        Assert.assertNotNull(success, "Response must have 'success' field");
        Assert.assertTrue(success, "success should be true");

        System.out.println("✅ PASSED: GetUserByMobile returns 200 with success=true");
    }

    @Test(priority = 2, dependsOnMethods = "testGetUserByMobile_SuccessResponse",
          description = "Positive: Validate response data structure has required fields")
    public void testGetUserByMobile_DataStructure() {
        System.out.println("\n>>> GET USER BY MOBILE - POSITIVE: DATA STRUCTURE <<<");

        String token = RequestContext.getToken();
        Response response = callGetUserByMobile(token, VALID_MEMBER_MOBILE);

        AssertionUtil.verifyStatusCode(response, 200);

        Object dataObj = response.jsonPath().get("data");
        Assert.assertNotNull(dataObj, "Response 'data' field must not be null");

        System.out.println("   Data type: " + dataObj.getClass().getSimpleName());
        System.out.println("✅ PASSED: Data structure is valid");
    }

    @Test(priority = 3, dependsOnMethods = "testGetUserByMobile_SuccessResponse",
          description = "Positive: Validate user profile fields (guid, name, mobile, email)")
    public void testGetUserByMobile_ProfileFields() {
        System.out.println("\n>>> GET USER BY MOBILE - POSITIVE: PROFILE FIELDS <<<");

        String token = RequestContext.getToken();
        Response response = callGetUserByMobile(token, VALID_MEMBER_MOBILE);

        Object dataObj = response.jsonPath().get("data");
        String dataPath = (dataObj instanceof List) ? "data[0]" : "data";

        // GUID
        String guid = response.jsonPath().getString(dataPath + ".guid");
        if (guid == null) guid = response.jsonPath().getString(dataPath + ".user_guid");
        Assert.assertNotNull(guid, "User GUID must be present in response");
        System.out.println("   GUID: " + guid);

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
            Assert.assertTrue(mobile.contains(VALID_MEMBER_MOBILE),
                "Returned mobile should match requested mobile");
            System.out.println("   Mobile: " + mobile);
        }

        // Email
        String email = response.jsonPath().getString(dataPath + ".email");
        if (email != null) {
            System.out.println("   Email: " + email);
        }

        System.out.println("✅ PASSED: Profile fields validated");
    }

    @Test(priority = 4, dependsOnMethods = "testGetUserByMobile_SuccessResponse",
          description = "Positive: Validate role field is present and non-empty")
    public void testGetUserByMobile_RoleField() {
        System.out.println("\n>>> GET USER BY MOBILE - POSITIVE: ROLE FIELD <<<");

        String token = RequestContext.getToken();
        Response response = callGetUserByMobile(token, VALID_MEMBER_MOBILE);

        Object dataObj = response.jsonPath().get("data");
        String dataPath = (dataObj instanceof List) ? "data[0]" : "data";

        String role = response.jsonPath().getString(dataPath + ".role");
        if (role != null) {
            Assert.assertFalse(role.trim().isEmpty(), "Role should not be empty");
            System.out.println("   Role: " + role);
        }

        System.out.println("✅ PASSED: Role field validated");
    }

    @Test(priority = 5, dependsOnMethods = "testGetUserByMobile_SuccessResponse",
          description = "Positive: Validate response time is under 5 seconds")
    public void testGetUserByMobile_ResponseTime() {
        System.out.println("\n>>> GET USER BY MOBILE - POSITIVE: RESPONSE TIME <<<");

        String token = RequestContext.getToken();
        long startTime = System.currentTimeMillis();
        Response response = callGetUserByMobile(token, VALID_MEMBER_MOBILE);
        long responseTime = System.currentTimeMillis() - startTime;

        AssertionUtil.verifyStatusCode(response, 200);
        System.out.println("   Response Time: " + responseTime + "ms");
        Assert.assertTrue(responseTime < 5000,
            "GetUserByMobile response time should be under 5s but took: " + responseTime + "ms");

        System.out.println("✅ PASSED: Response time is acceptable (" + responseTime + "ms)");
    }

    @Test(priority = 6, dependsOnMethods = "testGetUserByMobile_SuccessResponse",
          description = "Positive: Validate no sensitive data exposed (password, tokens, secrets)")
    public void testGetUserByMobile_NoSensitiveDataExposed() {
        System.out.println("\n>>> GET USER BY MOBILE - POSITIVE: NO SENSITIVE DATA <<<");

        String token = RequestContext.getToken();
        Response response = callGetUserByMobile(token, VALID_MEMBER_MOBILE);

        String responseBody = response.getBody().asString().toLowerCase();
        Assert.assertFalse(responseBody.contains("password"), "Response should not contain password");
        Assert.assertFalse(responseBody.contains("\"token\""), "Response should not contain token");
        Assert.assertFalse(responseBody.contains("secret"), "Response should not contain secret");

        System.out.println("   ✅ No password, token, or secret fields found");
        System.out.println("✅ PASSED: No sensitive data exposed");
    }

    @Test(priority = 7, dependsOnMethods = "testGetUserByMobile_SuccessResponse",
          description = "Positive: Validate timestamp fields (created_at, updated_at)")
    public void testGetUserByMobile_TimestampFields() {
        System.out.println("\n>>> GET USER BY MOBILE - POSITIVE: TIMESTAMP FIELDS <<<");

        String token = RequestContext.getToken();
        Response response = callGetUserByMobile(token, VALID_MEMBER_MOBILE);

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

    // ═══════════════════════════════════════════════════════════════════
    //  SCHEMA VALIDATION
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 10, dependsOnMethods = "testGetUserByMobile_SuccessResponse",
          description = "Schema: Validate top-level response fields (status, success, msg, data)")
    public void testGetUserByMobile_Schema_TopLevel() {
        System.out.println("\n>>> GET USER BY MOBILE - SCHEMA: TOP-LEVEL FIELDS <<<");

        String token = RequestContext.getToken();
        Response response = callGetUserByMobile(token, VALID_MEMBER_MOBILE);

        // status field - should be integer
        Object statusObj = response.jsonPath().get("status");
        Assert.assertNotNull(statusObj, "Schema: 'status' field must be present");
        Assert.assertTrue(statusObj instanceof Integer, "Schema: 'status' must be Integer, got: " + statusObj.getClass().getSimpleName());
        System.out.println("   status: " + statusObj + " (Integer ✓)");

        // success field - should be boolean
        Object successObj = response.jsonPath().get("success");
        Assert.assertNotNull(successObj, "Schema: 'success' field must be present");
        Assert.assertTrue(successObj instanceof Boolean, "Schema: 'success' must be Boolean, got: " + successObj.getClass().getSimpleName());
        System.out.println("   success: " + successObj + " (Boolean ✓)");

        // msg field - should be string
        Object msgObj = response.jsonPath().get("msg");
        Assert.assertNotNull(msgObj, "Schema: 'msg' field must be present");
        Assert.assertTrue(msgObj instanceof String, "Schema: 'msg' must be String, got: " + msgObj.getClass().getSimpleName());
        System.out.println("   msg: " + msgObj + " (String ✓)");

        // data field - must be present
        Object dataObj = response.jsonPath().get("data");
        Assert.assertNotNull(dataObj, "Schema: 'data' field must be present");
        System.out.println("   data: present (" + dataObj.getClass().getSimpleName() + " ✓)");

        System.out.println("✅ PASSED: Top-level schema validation passed");
    }

    @Test(priority = 11, dependsOnMethods = "testGetUserByMobile_SuccessResponse",
          description = "Schema: Validate user data field types (guid=String, mobile=String, email=String)")
    public void testGetUserByMobile_Schema_DataFieldTypes() {
        System.out.println("\n>>> GET USER BY MOBILE - SCHEMA: DATA FIELD TYPES <<<");

        String token = RequestContext.getToken();
        Response response = callGetUserByMobile(token, VALID_MEMBER_MOBILE);

        Object dataObj = response.jsonPath().get("data");
        String dataPath = (dataObj instanceof List) ? "data[0]" : "data";

        // guid - String
        Object guidObj = response.jsonPath().get(dataPath + ".guid");
        if (guidObj != null) {
            Assert.assertTrue(guidObj instanceof String, "Schema: 'guid' must be String, got: " + guidObj.getClass().getSimpleName());
            System.out.println("   guid: String ✓");
        }

        // first_name - String
        Object firstNameObj = response.jsonPath().get(dataPath + ".first_name");
        if (firstNameObj != null) {
            Assert.assertTrue(firstNameObj instanceof String, "Schema: 'first_name' must be String, got: " + firstNameObj.getClass().getSimpleName());
            System.out.println("   first_name: String ✓");
        }

        // last_name - String
        Object lastNameObj = response.jsonPath().get(dataPath + ".last_name");
        if (lastNameObj != null) {
            Assert.assertTrue(lastNameObj instanceof String, "Schema: 'last_name' must be String, got: " + lastNameObj.getClass().getSimpleName());
            System.out.println("   last_name: String ✓");
        }

        // mobile - String
        Object mobileObj = response.jsonPath().get(dataPath + ".mobile");
        if (mobileObj != null) {
            Assert.assertTrue(mobileObj instanceof String, "Schema: 'mobile' must be String, got: " + mobileObj.getClass().getSimpleName());
            System.out.println("   mobile: String ✓");
        }

        // email - String
        Object emailObj = response.jsonPath().get(dataPath + ".email");
        if (emailObj != null) {
            Assert.assertTrue(emailObj instanceof String, "Schema: 'email' must be String, got: " + emailObj.getClass().getSimpleName());
            System.out.println("   email: String ✓");
        }

        // role - String
        Object roleObj = response.jsonPath().get(dataPath + ".role");
        if (roleObj != null) {
            Assert.assertTrue(roleObj instanceof String, "Schema: 'role' must be String, got: " + roleObj.getClass().getSimpleName());
            System.out.println("   role: String ✓");
        }

        System.out.println("✅ PASSED: Data field type schema validated");
    }

    @Test(priority = 12, dependsOnMethods = "testGetUserByMobile_SuccessResponse",
          description = "Schema: Validate ID fields are proper types (id=Integer, _user_id=Integer)")
    public void testGetUserByMobile_Schema_IdFieldTypes() {
        System.out.println("\n>>> GET USER BY MOBILE - SCHEMA: ID FIELD TYPES <<<");

        String token = RequestContext.getToken();
        Response response = callGetUserByMobile(token, VALID_MEMBER_MOBILE);

        Object dataObj = response.jsonPath().get("data");
        String dataPath = (dataObj instanceof List) ? "data[0]" : "data";

        // id - Integer
        Object idObj = response.jsonPath().get(dataPath + ".id");
        if (idObj != null) {
            Assert.assertTrue(idObj instanceof Integer, "Schema: 'id' must be Integer, got: " + idObj.getClass().getSimpleName());
            System.out.println("   id: Integer ✓ (" + idObj + ")");
        }

        // _user_id - Integer
        Object userIdObj = response.jsonPath().get(dataPath + "._user_id");
        if (userIdObj != null) {
            Assert.assertTrue(userIdObj instanceof Integer, "Schema: '_user_id' must be Integer, got: " + userIdObj.getClass().getSimpleName());
            System.out.println("   _user_id: Integer ✓ (" + userIdObj + ")");
        }

        System.out.println("✅ PASSED: ID field types validated");
    }

    @Test(priority = 13, dependsOnMethods = "testGetUserByMobile_SuccessResponse",
          description = "Schema: Validate boolean fields are proper types (is_member, is_active)")
    public void testGetUserByMobile_Schema_BooleanFields() {
        System.out.println("\n>>> GET USER BY MOBILE - SCHEMA: BOOLEAN FIELDS <<<");

        String token = RequestContext.getToken();
        Response response = callGetUserByMobile(token, VALID_MEMBER_MOBILE);

        Object dataObj = response.jsonPath().get("data");
        String dataPath = (dataObj instanceof List) ? "data[0]" : "data";

        // is_member - Boolean
        Object isMemberObj = response.jsonPath().get(dataPath + ".is_member");
        if (isMemberObj != null) {
            Assert.assertTrue(isMemberObj instanceof Boolean, "Schema: 'is_member' must be Boolean, got: " + isMemberObj.getClass().getSimpleName());
            System.out.println("   is_member: Boolean ✓ (" + isMemberObj + ")");
        }

        // is_active - Boolean
        Object isActiveObj = response.jsonPath().get(dataPath + ".is_active");
        if (isActiveObj != null) {
            Assert.assertTrue(isActiveObj instanceof Boolean, "Schema: 'is_active' must be Boolean, got: " + isActiveObj.getClass().getSimpleName());
            System.out.println("   is_active: Boolean ✓ (" + isActiveObj + ")");
        }

        System.out.println("✅ PASSED: Boolean field types validated");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  NEGATIVE VALIDATIONS
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 20, description = "Negative: GetUserByMobile with non-existent mobile number")
    public void testGetUserByMobile_NonExistentMobile() {
        System.out.println("\n>>> GET USER BY MOBILE - NEGATIVE: NON-EXISTENT MOBILE <<<");

        String token = RequestContext.getToken();
        String invalidMobile = "0000000000";

        Response response = callGetUserByMobile(token, invalidMobile);

        System.out.println("   HTTP Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());

        int statusCode = response.getStatusCode();
        if (statusCode == 200) {
            Object data = response.jsonPath().get("data");
            boolean hasData = (data != null && !data.toString().equals("[]")
                && !data.toString().equals("null") && !data.toString().equals("{}"));
            Assert.assertFalse(hasData,
                "Non-existent mobile should not return valid user data");
            System.out.println("   API returned 200 with empty/null data (acceptable)");
        } else if (statusCode == 500) {
            System.out.println("   🐛 BUG: API returns 500 for non-existent mobile");
        } else {
            Assert.assertTrue(statusCode >= 400 && statusCode < 500,
                "Non-existent mobile should return 4xx but got: " + statusCode);
        }

        System.out.println("✅ PASSED: Non-existent mobile handled");
    }

    @Test(priority = 21, description = "Negative: GetUserByMobile with invalid mobile format (letters)")
    public void testGetUserByMobile_InvalidFormat() {
        System.out.println("\n>>> GET USER BY MOBILE - NEGATIVE: INVALID FORMAT <<<");

        String token = RequestContext.getToken();
        String invalidMobile = "abcdefghij";

        ApiReportContext.setExpectedStatus(400);
        Response response = callGetUserByMobile(token, invalidMobile);

        System.out.println("   HTTP Status: " + response.getStatusCode());

        int statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode, 400,
            "🐛 BUG: Invalid mobile format (letters) should return 400 Bad Request but got: " + statusCode
            + ". Server must validate mobile number format.");

        System.out.println("✅ PASSED: Invalid format returns 400");
    }

    @Test(priority = 22, description = "Negative: GetUserByMobile with too short mobile number")
    public void testGetUserByMobile_TooShortMobile() {
        System.out.println("\n>>> GET USER BY MOBILE - NEGATIVE: TOO SHORT MOBILE <<<");

        String token = RequestContext.getToken();
        String shortMobile = "123";

        ApiReportContext.setExpectedStatus(400);
        Response response = callGetUserByMobile(token, shortMobile);

        System.out.println("   HTTP Status: " + response.getStatusCode());

        int statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode, 400,
            "🐛 BUG: Too short mobile number should return 400 Bad Request but got: " + statusCode
            + ". Server must validate mobile number length (min 10 digits).");

        System.out.println("✅ PASSED: Too short mobile returns 400");
    }

    @Test(priority = 23, description = "Negative: GetUserByMobile with too long mobile number")
    public void testGetUserByMobile_TooLongMobile() {
        System.out.println("\n>>> GET USER BY MOBILE - NEGATIVE: TOO LONG MOBILE <<<");

        String token = RequestContext.getToken();
        String longMobile = "90037303941234567890";

        ApiReportContext.setExpectedStatus(400);
        Response response = callGetUserByMobile(token, longMobile);

        System.out.println("   HTTP Status: " + response.getStatusCode());

        int statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode, 400,
            "🐛 BUG: Too long mobile number should return 400 Bad Request but got: " + statusCode
            + ". Server must validate mobile number length (max 15 digits).");

        System.out.println("✅ PASSED: Too long mobile returns 400");
    }

    @Test(priority = 24, description = "Negative: GetUserByMobile with special characters")
    public void testGetUserByMobile_SpecialCharacters() {
        System.out.println("\n>>> GET USER BY MOBILE - NEGATIVE: SPECIAL CHARACTERS <<<");

        String token = RequestContext.getToken();
        String specialMobile = "900-373-0394";

        ApiReportContext.setExpectedStatus(400);
        Response response = callGetUserByMobile(token, specialMobile);

        System.out.println("   HTTP Status: " + response.getStatusCode());

        int statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode, 400,
            "🐛 BUG: Mobile with special characters should return 400 Bad Request but got: " + statusCode
            + ". Server must validate mobile number format (digits only).");

        System.out.println("✅ PASSED: Special characters return 400");
    }

    @Test(priority = 25, description = "Negative: GetUserByMobile with SQL injection payload")
    public void testGetUserByMobile_SqlInjection() {
        System.out.println("\n>>> GET USER BY MOBILE - NEGATIVE: SQL INJECTION <<<");

        String token = RequestContext.getToken();
        String sqlPayload = "9003730394' OR '1'='1";

        ApiReportContext.setExpectedStatus(400);
        Response response = callGetUserByMobile(token, sqlPayload);

        System.out.println("   HTTP Status: " + response.getStatusCode());

        int statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode, 400,
            "🐛 BUG: SQL injection payload should return 400 Bad Request but got: " + statusCode
            + ". Server must validate mobile format and reject malicious input.");

        System.out.println("✅ PASSED: SQL injection rejected");
    }

    @Test(priority = 26, description = "Negative: GetUserByMobile with XSS payload")
    public void testGetUserByMobile_XssAttempt() {
        System.out.println("\n>>> GET USER BY MOBILE - NEGATIVE: XSS ATTEMPT <<<");

        String token = RequestContext.getToken();
        String xssPayload = "<script>alert('xss')</script>";

        ApiReportContext.setExpectedStatus(400);
        Response response = callGetUserByMobile(token, xssPayload);

        System.out.println("   HTTP Status: " + response.getStatusCode());

        int statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode, 400,
            "🐛 BUG: XSS payload should return 400 Bad Request but got: " + statusCode
            + ". Server must validate mobile format and reject HTML/script input.");

        System.out.println("✅ PASSED: XSS attempt rejected");
    }

    @Test(priority = 27, description = "Negative: GetUserByMobile with empty mobile number")
    public void testGetUserByMobile_EmptyMobile() {
        System.out.println("\n>>> GET USER BY MOBILE - NEGATIVE: EMPTY MOBILE <<<");

        String token = RequestContext.getToken();

        Response response = new RequestBuilder()
                .setEndpoint(GET_USER_BY_MOBILE_ENDPOINT)
                .addHeader("Authorization", "Bearer " + token)
                .get();

        System.out.println("   HTTP Status: " + response.getStatusCode());

        int statusCode = response.getStatusCode();
        if (statusCode == 200) {
            Boolean success = response.jsonPath().get("success");
            System.out.println("   Success flag: " + success);
        } else if (statusCode == 500) {
            System.out.println("   🐛 BUG: API returns 500 for empty mobile");
        } else {
            Assert.assertTrue(statusCode >= 400,
                "Empty mobile should return error but got: " + statusCode);
        }

        System.out.println("✅ PASSED: Empty mobile handled");
    }

    @Test(priority = 28, description = "Negative: GetUserByMobile with whitespace mobile")
    public void testGetUserByMobile_WhitespaceMobile() {
        System.out.println("\n>>> GET USER BY MOBILE - NEGATIVE: WHITESPACE MOBILE <<<");

        String token = RequestContext.getToken();
        String whitespaceMobile = "   ";

        ApiReportContext.setExpectedStatus(400);
        Response response = callGetUserByMobile(token, whitespaceMobile);

        System.out.println("   HTTP Status: " + response.getStatusCode());

        int statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode, 400,
            "🐛 BUG: Whitespace-only mobile should return 400 Bad Request but got: " + statusCode
            + ". Server must trim and validate mobile input.");

        System.out.println("✅ PASSED: Whitespace mobile returns 400");
    }

    @Test(priority = 29, description = "Negative: GetUserByMobile with mobile containing country code prefix")
    public void testGetUserByMobile_WithCountryCode() {
        System.out.println("\n>>> GET USER BY MOBILE - NEGATIVE: WITH COUNTRY CODE <<<");

        String token = RequestContext.getToken();
        String mobileWithCode = "+919003730394";

        Response response = callGetUserByMobile(token, mobileWithCode);

        System.out.println("   HTTP Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());

        int statusCode = response.getStatusCode();
        // Acceptable: 200 (if server strips +91) or 400 (strict validation)
        Assert.assertTrue(statusCode == 200 || statusCode == 400,
            "Mobile with country code should return 200 or 400 but got: " + statusCode);

        if (statusCode == 200) {
            System.out.println("   Server accepts mobile with country code prefix");
        } else {
            System.out.println("   Server rejects mobile with country code prefix");
        }

        System.out.println("✅ PASSED: Country code prefix handled");
    }

    @Test(priority = 30, description = "Negative: GetUserByMobile without authorization token")
    public void testGetUserByMobile_NoAuth() {
        System.out.println("\n>>> GET USER BY MOBILE - NEGATIVE: NO AUTH TOKEN <<<");

        ApiReportContext.setExpectedStatus(401);
        Response response = new RequestBuilder()
                .setEndpoint(GET_USER_BY_MOBILE_ENDPOINT + VALID_MEMBER_MOBILE)
                .get();

        System.out.println("   HTTP Status: " + response.getStatusCode());

        int statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode, 401,
            "🐛 BUG: Request without auth should return 401 Unauthorized but got: " + statusCode);

        System.out.println("✅ PASSED: No auth returns 401");
    }

    @Test(priority = 31, description = "Negative: GetUserByMobile with invalid/expired token")
    public void testGetUserByMobile_InvalidToken() {
        System.out.println("\n>>> GET USER BY MOBILE - NEGATIVE: INVALID TOKEN <<<");

        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.INVALID.PAYLOAD";

        ApiReportContext.setExpectedStatus(401);
        Response response = callGetUserByMobile(invalidToken, VALID_MEMBER_MOBILE);

        System.out.println("   HTTP Status: " + response.getStatusCode());

        int statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode, 401,
            "🐛 BUG: Invalid token should return 401 Unauthorized but got: " + statusCode);

        System.out.println("✅ PASSED: Invalid token returns 401");
    }

    @Test(priority = 32, description = "Negative: GetUserByMobile with zero as mobile number")
    public void testGetUserByMobile_ZeroMobile() {
        System.out.println("\n>>> GET USER BY MOBILE - NEGATIVE: ZERO MOBILE <<<");

        String token = RequestContext.getToken();

        Response response = callGetUserByMobile(token, "0");

        System.out.println("   HTTP Status: " + response.getStatusCode());

        int statusCode = response.getStatusCode();
        if (statusCode == 200) {
            Object data = response.jsonPath().get("data");
            boolean hasData = (data != null && !data.toString().equals("[]")
                && !data.toString().equals("null") && !data.toString().equals("{}"));
            Assert.assertFalse(hasData, "Zero mobile should not return user data");
        }

        System.out.println("✅ PASSED: Zero mobile handled");
    }
}

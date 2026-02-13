package com.mryoda.diagnostics.api.tests.family;

import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.AssertionUtil;
import com.mryoda.diagnostics.api.utils.RandomDataUtil;
import com.mryoda.diagnostics.api.utils.TokenManager;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import io.restassured.response.Response;
import io.restassured.RestAssured;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * FAMILY MEMBER NEGATIVE TEST SCENARIOS
 * Tests error handling and validation for various invalid inputs
 */
public class FamilyMemberNegativeTest extends BaseTest {

    private String validToken;
    private String validUserId;
    private String validFamilyMemberId;
    private String testUserMobile;

    @BeforeClass
    public void setup() {
        System.out.println("\n>>> SETUP: Creating test user and family member for negative tests <<<");

        try {
            // Generate random mobile and create user via OTP
            testUserMobile = RandomDataUtil.getRandomMobile();
            System.out.println("   Creating test user with mobile: " + testUserMobile);

            // Use TokenManager to generate token via OTP flow
            validToken = TokenManager.generateToken(testUserMobile, TokenManager.NEW_USER);
            validUserId = com.mryoda.diagnostics.api.utils.RequestContext.getNewUserUserId();

            System.out.println("   ✅ Test User Created Successfully");
            System.out.println("   User ID: " + validUserId);
            System.out.println("   Token generated: " + validToken.substring(0, 20) + "...");

            // Create one valid family member for update/delete negative tests
            Map<String, Object> payload = buildValidPayload();

            String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER;

            Response response = new RequestBuilder()
                    .setEndpoint(endpoint)
                    .addHeader("Authorization", "Bearer " + validToken)
                    .setRequestBody(payload)
                    .post();

            System.out.println("   Setup Family Member Response Status: " + response.getStatusCode());

            if (response.getStatusCode() == 201) {
                validFamilyMemberId = response.jsonPath().getString("data.guid");
                System.out.println("   ✅ Setup Family Member Created - ID: " + validFamilyMemberId);
            } else {
                System.out.println("   ⚠️ Warning: Could not create setup family member");
                System.out.println("   Response: " + response.getBody().asString());
                System.out.println("   Some negative tests requiring existing family member may be skipped");
            }

            System.out.println("   ✅ Setup completed successfully\n");

        } catch (Exception e) {
            System.out.println("   ❌ ERROR in setup: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Setup failed - cannot proceed with negative tests", e);
        }
    }

    // ==================== ADD FAMILY MEMBER - NEGATIVE TESTS ====================

    @Test(priority = 1)
    public void test_AddFamilyMember_MissingRequiredFields() {
        System.out.println("\n>>> NEGATIVE TEST: Add Family Member - Missing Required Fields <<<");

        Map<String, Object> payload = new HashMap<>();
        // Missing all required fields

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + validToken)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        AssertionUtil.verifyErrorStatus(response, "Missing required fields");
        System.out.println("   ✅ Correctly rejected missing required fields");
    }

    @Test(priority = 2)
    public void test_AddFamilyMember_MissingUserId() {
        System.out.println("\n>>> NEGATIVE TEST: Add Family Member - Missing user_id <<<");

        Map<String, Object> payload = buildValidPayload();
        payload.remove("user_id");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + validToken)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        AssertionUtil.verifyStatusCodeIn(response, 400, 422);
        System.out.println("   ✅ Correctly rejected missing user_id");
    }

    @Test(priority = 3)
    public void test_AddFamilyMember_MissingFirstName() {
        System.out.println("\n>>> NEGATIVE TEST: Add Family Member - Missing first_name <<<");

        Map<String, Object> payload = buildValidPayload();
        payload.remove("first_name");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + validToken)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        AssertionUtil.verifyStatusCodeIn(response, 400, 422);
        System.out.println("   ✅ Correctly rejected missing first_name");
    }

    @Test(priority = 4)
    public void test_AddFamilyMember_InvalidMobileFormat() {
        System.out.println("\n>>> NEGATIVE TEST: Add Family Member - Invalid mobile format <<<");

        Map<String, Object> payload = buildValidPayload();
        payload.put("mobile", "123"); // Too short

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + validToken)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        AssertionUtil.verifyStatusCodeIn(response, 400, 422);
        System.out.println("   ✅ Correctly rejected invalid mobile format");
    }

    @Test(priority = 5)
    public void test_AddFamilyMember_InvalidGender() {
        System.out.println("\n>>> NEGATIVE TEST: Add Family Member - Invalid gender value <<<");

        Map<String, Object> payload = buildValidPayload();
        payload.put("gender", "invalid_gender");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + validToken)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        AssertionUtil.verifyStatusCodeIn(response, 400, 422);
        System.out.println("   ✅ Correctly rejected invalid gender");
    }

    @Test(priority = 6)
    public void test_AddFamilyMember_InvalidDateFormat() {
        System.out.println("\n>>> NEGATIVE TEST: Add Family Member - Invalid DOB format <<<");

        Map<String, Object> payload = buildValidPayload();
        payload.put("dob", "01/01/1990"); // Wrong format (should be YYYY-MM-DD)

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + validToken)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        AssertionUtil.verifyStatusCodeIn(response, 400, 422);
        System.out.println("   ✅ Correctly rejected invalid date format");
    }

    @Test(priority = 7)
    public void test_AddFamilyMember_NoAuthToken() {
        System.out.println("\n>>> NEGATIVE TEST: Add Family Member - No Authorization Token <<<");

        Map<String, Object> payload = buildValidPayload();

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                // No Authorization header
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        AssertionUtil.verifyEquals(response.getStatusCode(), 401, "Should return 401 for missing auth token");
        System.out.println("   ✅ Correctly rejected request without auth token");
    }

    @Test(priority = 8)
    public void test_AddFamilyMember_InvalidAuthToken() {
        System.out.println("\n>>> NEGATIVE TEST: Add Family Member - Invalid Authorization Token <<<");

        Map<String, Object> payload = buildValidPayload();

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer invalid_token_12345")
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        boolean isAuthError = (response.getStatusCode() == 401 || response.getStatusCode() == 403);
        AssertionUtil.verifyEquals(isAuthError, true, "Should return 401/403 for invalid token");
        System.out.println("   ✅ Correctly rejected invalid auth token");
    }

    // ==================== GET FAMILY MEMBER - NEGATIVE TESTS ====================

    @Test(priority = 10)
    public void test_GetFamilyMember_InvalidGuid() {
        System.out.println("\n>>> NEGATIVE TEST: Get Family Member - Invalid GUID <<<");

        String invalidGuid = "00000000-0000-0000-0000-000000000000";
        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                APIEndpoints.GET_FAMILY_MEMBER_BY_ID.replace("{guid}", invalidGuid);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + validToken)
                .get();

        System.out.println("   Response Status: " + response.getStatusCode());

        AssertionUtil.verifyStatusCodeIn(response, 404, 400);
        System.out.println("   ✅ Correctly handled non-existent family member");
    }

    @Test(priority = 11)
    public void test_GetFamilyMember_NoAuthToken() {
        System.out.println("\n>>> NEGATIVE TEST: Get Family Member - No Authorization <<<");

        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                APIEndpoints.GET_FAMILY_MEMBER_BY_ID.replace("{guid}", validFamilyMemberId);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .get();

        System.out.println("   Response Status: " + response.getStatusCode());

        AssertionUtil.verifyEquals(response.getStatusCode(), 401, "Should return 401 without auth");
        System.out.println("   ✅ Correctly rejected unauthorized access");
    }

    // ==================== UPDATE FAMILY MEMBER - NEGATIVE TESTS
    // ====================

    @Test(priority = 15)
    public void test_UpdateFamilyMember_MissingGuid() {
        System.out.println("\n>>> NEGATIVE TEST: Update Family Member - Missing GUID <<<");

        Map<String, Object> payload = buildUpdatePayload();
        payload.remove("guid");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.UPDATE_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + validToken)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        AssertionUtil.verifyStatusCodeIn(response, 400, 422);
        System.out.println("   ✅ Correctly rejected update without GUID");
    }

    @Test(priority = 16)
    public void test_UpdateFamilyMember_NonExistentGuid() {
        System.out.println("\n>>> NEGATIVE TEST: Update Family Member - Non-existent GUID <<<");

        Map<String, Object> payload = buildUpdatePayload();
        payload.put("guid", "99999999-9999-9999-9999-999999999999");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.UPDATE_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + validToken)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        AssertionUtil.verifyStatusCodeIn(response, 404, 400);
        System.out.println("   ✅ Correctly handled update of non-existent member");
    }

    // ==================== DELETE FAMILY MEMBER - NEGATIVE TESTS
    // ====================

    @Test(priority = 20)
    public void test_DeleteFamilyMember_InvalidGuid() {
        System.out.println("\n>>> NEGATIVE TEST: Delete Family Member - Invalid GUID <<<");

        String invalidGuid = "invalid-guid-format";
        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                APIEndpoints.DELETE_FAMILY_MEMBER_BY_ID.replace("{guid}", invalidGuid);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + validToken)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        AssertionUtil.verifyStatusCodeIn(response, 400, 404);
        System.out.println("   ✅ Correctly rejected invalid GUID format");
    }

    @Test(priority = 21)
    public void test_DeleteFamilyMember_NoAuthToken() {
        System.out.println("\n>>> NEGATIVE TEST: Delete Family Member - No Authorization <<<");

        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                APIEndpoints.DELETE_FAMILY_MEMBER_BY_ID.replace("{guid}", validFamilyMemberId);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        AssertionUtil.verifyEquals(response.getStatusCode(), 401, "Should return 401 without auth");
        System.out.println("   ✅ Correctly rejected unauthorized delete");
    }

    // ==================== HELPER METHODS ====================

    private Map<String, Object> buildValidPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", validUserId);
        payload.put("first_name", "Test");
        payload.put("last_name", "User");
        payload.put("middle_name", "");
        payload.put("mobile", RandomDataUtil.getRandomMobile());
        payload.put("gender", "male");
        payload.put("country_code", "+91");
        payload.put("dob", "1990-01-01");
        payload.put("profile_pic", "");
        payload.put("profile_color", "#fdefca");
        payload.put("relation", "Brother");
        payload.put("title", "Mr.");
        return payload;
    }

    private Map<String, Object> buildUpdatePayload() {
        Map<String, Object> payload = buildValidPayload();
        payload.put("guid", validFamilyMemberId);
        return payload;
    }
}

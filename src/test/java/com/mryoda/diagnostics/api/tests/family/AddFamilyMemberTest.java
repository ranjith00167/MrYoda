package com.mryoda.diagnostics.api.tests.family;

import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.AssertionUtil;
import com.mryoda.diagnostics.api.utils.RandomDataUtil;
import com.mryoda.diagnostics.api.utils.TokenManager;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * COMPREHENSIVE TEST CLASS FOR ADD FAMILY MEMBER API
 * Covers both positive and negative scenarios for
 * /familymembers/addFamilyMember
 */
public class AddFamilyMemberTest extends BaseTest {

    private String validToken;
    private String validUserId;
    private String testUserMobile;

    @BeforeClass
    public void setup() {
        System.out.println("\n========================================");
        System.out.println("  ADD FAMILY MEMBER API - TEST SETUP");
        System.out.println("========================================");

        // Create test user via OTP
        testUserMobile = RandomDataUtil.getRandomMobile();
        validToken = TokenManager.generateToken(testUserMobile, TokenManager.NEW_USER);
        validUserId = com.mryoda.diagnostics.api.utils.RequestContext.getNewUserUserId();

        System.out.println("✅ Test User Created - ID: " + validUserId);
        System.out.println("========================================\n");
    }

    // ==================== POSITIVE TESTS ====================

    @Test(priority = 1, description = "QA Automation: Verify Add Family Member Valid Data All Fields Validated")
    public void test_AddFamilyMember_ValidData_AllFieldsValidated() {
        System.out.println("\n>>> POSITIVE TEST: Add Family Member - Valid Data with All Field Validations <<<");

        // Prepare test data
        String firstName = "John";
        String lastName = "Doe";
        String middleName = "";
        String mobile = RandomDataUtil.getRandomMobile();
        String gender = "male";
        String countryCode = "+91";
        String dob = "1990-01-01";
        String profilePic = "";
        String profileColor = "#fdefca";
        String relation = "Brother";
        String title = "Mr.";

        Map<String, Object> payload = com.mryoda.diagnostics.api.payloads.OrderPayloadBuilder
                .buildAddFamilyMemberPayload(
                        validUserId, firstName, lastName, middleName, mobile,
                        gender, countryCode, dob, profilePic, profileColor, relation, title);

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + validToken)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        // VALIDATION 1: Status Code
        AssertionUtil.verifyEquals(response.getStatusCode(), 201, "Status should be 201 Created");

        // VALIDATION 2: Family Member ID exists
        String familyMemberId = response.jsonPath().getString("data.guid");
        Assert.assertNotNull(familyMemberId, "Family member ID should not be null");

        // VALIDATION 3: first_name matches
        String responseFN = response.jsonPath().getString("data.first_name");
        AssertionUtil.verifyEquals(responseFN, firstName, "First name should match");

        // VALIDATION 4: last_name matches
        String responseLN = response.jsonPath().getString("data.last_name");
        AssertionUtil.verifyEquals(responseLN, lastName, "Last name should match");

        // VALIDATION 5: mobile matches
        String responseMobile = response.jsonPath().getString("data.mobile");
        AssertionUtil.verifyEquals(responseMobile, mobile, "Mobile should match");

        // VALIDATION 6: gender matches
        String responseGender = response.jsonPath().getString("data.gender");
        AssertionUtil.verifyEquals(responseGender, gender, "Gender should match");

        // VALIDATION 7: dob matches (with timestamp handling)
        String responseDob = response.jsonPath().getString("data.dob");
        AssertionUtil.verifyEquals(responseDob.startsWith(dob), true, "DOB should match");

        // VALIDATION 8: relation matches
        String responseRelation = response.jsonPath().getString("data.relation");
        AssertionUtil.verifyEquals(responseRelation, relation, "Relation should match");

        System.out.println("   ✅ All 8 validations passed!");
        System.out.println("   ✅ Family Member ID: " + familyMemberId);
    }

    // ==================== NEGATIVE TESTS - MANDATORY FIELDS ====================

    @Test(priority = 10, description = "QA Automation: Verify Add Family Member Missing All Required Fields")
    public void test_AddFamilyMember_MissingAllRequiredFields() {
        System.out.println("\n>>> NEGATIVE TEST: Add Family Member - Missing All Required Fields <<<");

        Map<String, Object> payload = new HashMap<>();
        // Empty payload - missing all required fields

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + validToken)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());
        System.out.println("   Response Body: " + response.getBody().asString());

        // VALIDATION: Should return 400 or 422 for missing required fields
        AssertionUtil.verifyStatusCodeIn(response, 400, 422);

        System.out.println("   ✅ Correctly rejected empty payload");
    }

    @Test(priority = 11, description = "QA Automation: Verify Add Family Member Missing User Id")
    public void test_AddFamilyMember_MissingUserId() {
        System.out.println("\n>>> NEGATIVE TEST: Add Family Member - Missing user_id <<<");

        Map<String, Object> payload = buildValidPayload();
        payload.remove("user_id"); // Remove mandatory field

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + validToken)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        AssertionUtil.verifyStatusCodeIn(response, 400, 422);

        System.out.println("   ✅ Correctly rejected missing user_id");
    }

    @Test(priority = 12, description = "QA Automation: Verify Add Family Member Missing First Name")
    public void test_AddFamilyMember_MissingFirstName() {
        System.out.println("\n>>> NEGATIVE TEST: Add Family Member - Missing first_name <<<");

        Map<String, Object> payload = buildValidPayload();
        payload.remove("first_name"); // Remove mandatory field

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + validToken)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        AssertionUtil.verifyStatusCodeIn(response, 400, 422);

        System.out.println("   ✅ Correctly rejected missing first_name");
    }

    @Test(priority = 13, description = "QA Automation: Verify Add Family Member Missing Last Name")
    public void test_AddFamilyMember_MissingLastName() {
        System.out.println("\n>>> NEGATIVE TEST: Add Family Member - Missing last_name <<<");

        Map<String, Object> payload = buildValidPayload();
        payload.remove("last_name");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + validToken)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        AssertionUtil.verifyStatusCodeIn(response, 400, 422);

        System.out.println("   ✅ Correctly rejected missing last_name");
    }

    // ==================== NEGATIVE TESTS - INVALID FORMATS ====================

    @Test(priority = 20, description = "QA Automation: Verify Add Family Member Invalid Mobile Format Too Short")
    public void test_AddFamilyMember_InvalidMobileFormat_TooShort() {
        System.out.println("\n>>> NEGATIVE TEST: Add Family Member - Invalid Mobile (Too Short) <<<");

        Map<String, Object> payload = buildValidPayload();
        payload.put("mobile", "123"); // Invalid: too short

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + validToken)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        AssertionUtil.verifyStatusCodeIn(response, 400, 422);

        System.out.println("   ✅ Correctly rejected invalid mobile format");
    }

    @Test(priority = 21, description = "QA Automation: Verify Add Family Member Invalid Gender")
    public void test_AddFamilyMember_InvalidGender() {
        System.out.println("\n>>> NEGATIVE TEST: Add Family Member - Invalid Gender Value <<<");

        Map<String, Object> payload = buildValidPayload();
        payload.put("gender", "invalid_gender"); // Invalid enum value

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + validToken)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        AssertionUtil.verifyStatusCodeIn(response, 400, 422);

        System.out.println("   ✅ Correctly rejected invalid gender value");
    }

    @Test(priority = 22, description = "QA Automation: Verify Add Family Member Invalid Date Format")
    public void test_AddFamilyMember_InvalidDateFormat() {
        System.out.println("\n>>> NEGATIVE TEST: Add Family Member - Invalid Date Format <<<");

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

    // ==================== NEGATIVE TESTS - AUTHENTICATION ====================

    @Test(priority = 30, description = "QA Automation: Verify Add Family Member No Auth Token")
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

    @Test(priority = 31, description = "QA Automation: Verify Add Family Member Invalid Auth Token")
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
}

package com.mryoda.diagnostics.api.tests.family;

import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.AssertionUtil;
import com.mryoda.diagnostics.api.utils.RandomDataUtil;
import com.mryoda.diagnostics.api.utils.TokenManager;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * COMPREHENSIVE TEST CLASS FOR GET FAMILY MEMBER API
 * Covers both positive and negative scenarios for
 * /familymembers/GetFamilyMemberById/{guid}
 */
public class GetFamilyMemberTest {

    private String validToken;
    private String validUserId;
    private String testFamilyMemberId;

    // Store original values for validation
    private String originalFirstName;
    private String originalLastName;
    private String originalMiddleName;
    private String originalMobile;
    private String originalGender;
    private String originalDob;
    private String originalCountryCode;
    private String originalRelation;
    private String originalTitle;

    @BeforeClass
    public void setup() {
        System.out.println("\n========================================");
        System.out.println("  GET FAMILY MEMBER API - TEST SETUP");
        System.out.println("========================================");

        // Create test user
        String mobile = RandomDataUtil.getRandomMobile();
        validToken = TokenManager.generateToken(mobile, TokenManager.NEW_USER);
        validUserId = com.mryoda.diagnostics.api.utils.RequestContext.getNewUserUserId();

        // Create a family member for testing GET operations
        originalFirstName = "TestGet";
        originalLastName = "User";
        originalMiddleName = "";
        originalMobile = RandomDataUtil.getRandomMobile();
        originalGender = "male";
        originalCountryCode = "+91";
        originalDob = "1990-01-01";
        originalRelation = "Brother";
        originalTitle = "Mr.";

        Map<String, Object> payload = com.mryoda.diagnostics.api.payloads.OrderPayloadBuilder
                .buildAddFamilyMemberPayload(
                        validUserId, originalFirstName, originalLastName, originalMiddleName,
                        originalMobile, originalGender, originalCountryCode, originalDob,
                        "", "#fdefca", originalRelation, originalTitle);

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + validToken)
                .setRequestBody(payload)
                .post();

        if (response.getStatusCode() == 201) {
            testFamilyMemberId = response.jsonPath().getString("data.guid");
            System.out.println("✅ Test Family Member Created - ID: " + testFamilyMemberId);
        }
        System.out.println("========================================\n");
    }

    // ==================== POSITIVE TESTS ====================

    @Test(priority = 1)
    public void test_GetFamilyMember_ValidId_AllFieldsValidated() {
        System.out.println("\n>>> POSITIVE TEST: Get Family Member - Valid ID with All 11 Field Validations <<<");

        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                APIEndpoints.GET_FAMILY_MEMBER_BY_ID.replace("{guid}", testFamilyMemberId);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + validToken)
                .get();

        System.out.println("   Response Status: " + response.getStatusCode());

        // VALIDATION 1: Status Code
        AssertionUtil.verifyEquals(response.getStatusCode(), 200, "Status should be 200 OK");

        // VALIDATION 2: first_name
        String fn = response.jsonPath().getString("data.first_name");
        AssertionUtil.verifyEquals(fn, originalFirstName, "First name should match");

        // VALIDATION 3: last_name
        String ln = response.jsonPath().getString("data.last_name");
        AssertionUtil.verifyEquals(ln, originalLastName, "Last name should match");

        // VALIDATION 4: middle_name
        String mn = response.jsonPath().getString("data.middle_name");
        Assert.assertEquals(mn == null ? "" : mn, originalMiddleName, "Middle name should match");

        // VALIDATION 5: mobile
        String mob = response.jsonPath().getString("data.mobile");
        AssertionUtil.verifyEquals(mob, originalMobile, "Mobile should match");

        // VALIDATION 6: gender
        String gen = response.jsonPath().getString("data.gender");
        AssertionUtil.verifyEquals(gen, originalGender, "Gender should match");

        // VALIDATION 7: dob (with timestamp handling)
        String dob = response.jsonPath().getString("data.dob");
        AssertionUtil.verifyEquals(dob.startsWith(originalDob), true, "DOB should match");

        // VALIDATION 8: country_code
        String cc = response.jsonPath().getString("data.country_code");
        AssertionUtil.verifyEquals(cc, originalCountryCode, "Country code should match");

        // VALIDATION 9: relation
        String rel = response.jsonPath().getString("data.relation");
        AssertionUtil.verifyEquals(rel, originalRelation, "Relation should match");

        // VALIDATION 10: title
        String title = response.jsonPath().getString("data.title");
        AssertionUtil.verifyEquals(title, originalTitle, "Title should match");

        // VALIDATION 11: guid
        String guid = response.jsonPath().getString("data.guid");
        AssertionUtil.verifyEquals(guid, testFamilyMemberId, "GUID should match");

        System.out.println("   ✅ All 11 field validations passed!");
        System.out.println("   ✅ Family Member: " + fn + " " + ln);
    }

    // ==================== NEGATIVE TESTS ====================

    @Test(priority = 10)
    public void test_GetFamilyMember_NonExistentGuid() {
        System.out.println("\n>>> NEGATIVE TEST: Get Family Member - Non-existent GUID <<<");

        String nonExistentGuid = "00000000-0000-0000-0000-000000000000";
        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                APIEndpoints.GET_FAMILY_MEMBER_BY_ID.replace("{guid}", nonExistentGuid);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + validToken)
                .get();

        System.out.println("   Response Status: " + response.getStatusCode());
        System.out.println("   Response Body: " + response.getBody().asString());

        // VALIDATION: Should return 404 or 400 for non-existent resource
        boolean isError = (response.getStatusCode() == 404 || response.getStatusCode() == 400);
        AssertionUtil.verifyEquals(isError, true, "Should return 404/400 for non-existent GUID");

        System.out.println("   ✅ Correctly handled non-existent family member");
    }

    @Test(priority = 11)
    public void test_GetFamilyMember_InvalidGuidFormat() {
        System.out.println("\n>>> NEGATIVE TEST: Get Family Member - Invalid GUID Format <<<");

        String invalidGuid = "invalid-guid-123";
        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                APIEndpoints.GET_FAMILY_MEMBER_BY_ID.replace("{guid}", invalidGuid);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + validToken)
                .get();

        System.out.println("   Response Status: " + response.getStatusCode());

        boolean isError = (response.getStatusCode() == 400 || response.getStatusCode() == 404);
        AssertionUtil.verifyEquals(isError, true, "Should return 400/404 for invalid GUID format");

        System.out.println("   ✅ Correctly rejected invalid GUID format");
    }

    @Test(priority = 12)
    public void test_GetFamilyMember_NoAuthToken() {
        System.out.println("\n>>> NEGATIVE TEST: Get Family Member - No Authorization Token <<<");

        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                APIEndpoints.GET_FAMILY_MEMBER_BY_ID.replace("{guid}", testFamilyMemberId);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                // No Authorization header
                .get();

        System.out.println("   Response Status: " + response.getStatusCode());

        AssertionUtil.verifyEquals(response.getStatusCode(), 401, "Should return 401 for missing auth token");

        System.out.println("   ✅ Correctly rejected unauthorized access");
    }

    @Test(priority = 13)
    public void test_GetFamilyMember_InvalidAuthToken() {
        System.out.println("\n>>> NEGATIVE TEST: Get Family Member - Invalid Authorization Token <<<");

        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                APIEndpoints.GET_FAMILY_MEMBER_BY_ID.replace("{guid}", testFamilyMemberId);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer invalid_token_xyz")
                .get();

        System.out.println("   Response Status: " + response.getStatusCode());

        boolean isAuthError = (response.getStatusCode() == 401 || response.getStatusCode() == 403);
        AssertionUtil.verifyEquals(isAuthError, true, "Should return 401/403 for invalid token");

        System.out.println("   ✅ Correctly rejected invalid auth token");
    }

    @Test(priority = 14)
    public void test_GetFamilyMember_EmptyGuid() {
        System.out.println("\n>>> NEGATIVE TEST: Get Family Member - Empty GUID <<<");

        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                APIEndpoints.GET_FAMILY_MEMBER_BY_ID.replace("{guid}", "");

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + validToken)
                .get();

        System.out.println("   Response Status: " + response.getStatusCode());

        boolean isError = (response.getStatusCode() == 400 || response.getStatusCode() == 404);
        AssertionUtil.verifyEquals(isError, true, "Should return 400/404 for empty GUID");

        System.out.println("   ✅ Correctly rejected empty GUID");
    }
}

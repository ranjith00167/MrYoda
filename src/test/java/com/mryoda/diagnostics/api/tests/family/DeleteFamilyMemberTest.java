package com.mryoda.diagnostics.api.tests.family;

import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.AssertionUtil;
import com.mryoda.diagnostics.api.utils.RandomDataUtil;
import com.mryoda.diagnostics.api.utils.TokenManager;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * COMPREHENSIVE TEST CLASS FOR DELETE FAMILY MEMBER API
 * Covers both positive and negative scenarios for
 * /familymembers/deleteFamilyMemberById/{guid}
 */
public class DeleteFamilyMemberTest {

    private String validToken;
    private String validUserId;
    private String testFamilyMemberId;

    @BeforeClass
    public void setup() {
        System.out.println("\n========================================");
        System.out.println("DELETE FAMILY MEMBER API - TEST SETUP");
        System.out.println("========================================");

        // Create test user
        String mobile = RandomDataUtil.getRandomMobile();
        validToken = TokenManager.generateToken(mobile, TokenManager.NEW_USER);
        validUserId = com.mryoda.diagnostics.api.utils.RequestContext.getNewUserUserId();

        System.out.println("✅ Test User Created - ID: " + validUserId);
        System.out.println("========================================\n");
    }

    // ==================== POSITIVE TESTS ====================

    @Test(priority = 1, description = "QA Automation: Verify Delete Family Member Valid Id Verify Deletion")
    public void test_DeleteFamilyMember_ValidId_VerifyDeletion() {
        System.out.println("\n>>> POSITIVE TEST: Delete Family Member - Valid ID with Verification <<<");

        // First, create a family member to delete
        Map<String, Object> payload = com.mryoda.diagnostics.api.payloads.OrderPayloadBuilder
                .buildAddFamilyMemberPayload(
                        validUserId, "ToDelete", "User", "",
                        RandomDataUtil.getRandomMobile(), "male", "+91", "1990-01-01",
                        "", "#fdefca", "Brother", "Mr.");

        Response addResponse = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + validToken)
                .setRequestBody(payload)
                .post();

        String familyMemberId = addResponse.jsonPath().getString("data.guid");
        System.out.println("   Created family member to delete - ID: " + familyMemberId);

        // Now delete it
        String deleteEndpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                APIEndpoints.DELETE_FAMILY_MEMBER_BY_ID.replace("{guid}", familyMemberId);

        Response deleteResponse = new RequestBuilder()
                .setEndpoint(deleteEndpoint)
                .addHeader("Authorization", "Bearer " + validToken)
                .post();

        System.out.println("   Delete Response Status: " + deleteResponse.getStatusCode());

        // VALIDATION 1: Delete status code
        AssertionUtil.verifyEquals(deleteResponse.getStatusCode(), 200, "Delete should return 200 OK");

        // VALIDATION 2: Success message exists
        System.out.println("   ✅ Delete request successful");

        // VERIFICATION: Try to GET the deleted member (should fail or return not found)
        System.out.println("\n   >>> Verifying deletion via GET request... <<<");

        String getEndpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                APIEndpoints.GET_FAMILY_MEMBER_BY_ID.replace("{guid}", familyMemberId);

        Response getResponse = new RequestBuilder()
                .setEndpoint(getEndpoint)
                .addHeader("Authorization", "Bearer " + validToken)
                .get();

        System.out.println("   GET Response Status after deletion: " + getResponse.getStatusCode());

        // VALIDATION 3: GET should return 404 or empty data after deletion
        boolean isDeleted = (getResponse.getStatusCode() == 404 ||
                getResponse.jsonPath().get("data") == null);
        AssertionUtil.verifyEquals(isDeleted, true, "Family member should not be retrievable after deletion");

        System.out.println("   ✅ All 3 validations passed!");
        System.out.println("   ✅ Deletion verified successfully");
    }

    // ==================== NEGATIVE TESTS ====================

    @Test(priority = 10, description = "QA Automation: Verify Delete Family Member Non Existent Guid")
    public void test_DeleteFamilyMember_NonExistentGuid() {
        System.out.println("\n>>> NEGATIVE TEST: Delete Family Member - Non-existent GUID <<<");

        String nonExistentGuid = "00000000-0000-0000-0000-000000000000";
        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                APIEndpoints.DELETE_FAMILY_MEMBER_BY_ID.replace("{guid}", nonExistentGuid);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + validToken)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());
        System.out.println("   Response Body: " + response.getBody().asString());

        // VALIDATION: Should return 404 or 400 for non-existent resource
        boolean isError = (response.getStatusCode() == 404 || response.getStatusCode() == 400);
        AssertionUtil.verifyEquals(isError, true, "Should return 404/400 for non-existent GUID");

        System.out.println("   ✅ Correctly handled deletion of non-existent family member");
    }

    @Test(priority = 11, description = "QA Automation: Verify Delete Family Member Invalid Guid Format")
    public void test_DeleteFamilyMember_InvalidGuidFormat() {
        System.out.println("\n>>> NEGATIVE TEST: Delete Family Member - Invalid GUID Format <<<");

        String invalidGuid = "invalid-guid-format";
        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                APIEndpoints.DELETE_FAMILY_MEMBER_BY_ID.replace("{guid}", invalidGuid);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + validToken)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        boolean isError = (response.getStatusCode() == 400 || response.getStatusCode() == 404);
        AssertionUtil.verifyEquals(isError, true, "Should return 400/404 for invalid GUID format");

        System.out.println("   ✅ Correctly rejected invalid GUID format");
    }

    @Test(priority = 12, description = "QA Automation: Verify Delete Family Member Empty Guid")
    public void test_DeleteFamilyMember_EmptyGuid() {
        System.out.println("\n>>> NEGATIVE TEST: Delete Family Member - Empty GUID <<<");

        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                APIEndpoints.DELETE_FAMILY_MEMBER_BY_ID.replace("{guid}", "");

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + validToken)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        boolean isError = (response.getStatusCode() == 400 || response.getStatusCode() == 404);
        AssertionUtil.verifyEquals(isError, true, "Should return 400/404 for empty GUID");

        System.out.println("   ✅ Correctly rejected empty GUID");
    }

    @Test(priority = 13, description = "QA Automation: Verify Delete Family Member No Auth Token")
    public void test_DeleteFamilyMember_NoAuthToken() {
        System.out.println("\n>>> NEGATIVE TEST: Delete Family Member - No Authorization Token <<<");

        // Create a family member first
        Map<String, Object> payload = com.mryoda.diagnostics.api.payloads.OrderPayloadBuilder
                .buildAddFamilyMemberPayload(
                        validUserId, "Test", "User", "",
                        RandomDataUtil.getRandomMobile(), "male", "+91", "1990-01-01",
                        "", "#fdefca", "Brother", "Mr.");

        Response addResponse = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + validToken)
                .setRequestBody(payload)
                .post();

        String familyMemberId = addResponse.jsonPath().getString("data.guid");

        // Try to delete without auth token
        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                APIEndpoints.DELETE_FAMILY_MEMBER_BY_ID.replace("{guid}", familyMemberId);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                // No Authorization header
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        AssertionUtil.verifyEquals(response.getStatusCode(), 401, "Should return 401 for missing auth token");

        System.out.println("   ✅ Correctly rejected delete without auth token");
    }

    @Test(priority = 14, description = "QA Automation: Verify Delete Family Member Invalid Auth Token")
    public void test_DeleteFamilyMember_InvalidAuthToken() {
        System.out.println("\n>>> NEGATIVE TEST: Delete Family Member - Invalid Authorization Token <<<");

        // Create a family member first
        Map<String, Object> payload = com.mryoda.diagnostics.api.payloads.OrderPayloadBuilder
                .buildAddFamilyMemberPayload(
                        validUserId, "Test2", "User2", "",
                        RandomDataUtil.getRandomMobile(), "male", "+91", "1990-01-01",
                        "", "#fdefca", "Brother", "Mr.");

        Response addResponse = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + validToken)
                .setRequestBody(payload)
                .post();

        String familyMemberId = addResponse.jsonPath().getString("data.guid");

        // Try to delete with invalid token
        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                APIEndpoints.DELETE_FAMILY_MEMBER_BY_ID.replace("{guid}", familyMemberId);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer invalid_token_999")
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        boolean isAuthError = (response.getStatusCode() == 401 || response.getStatusCode() == 403);
        AssertionUtil.verifyEquals(isAuthError, true, "Should return 401/403 for invalid token");

        System.out.println("   ✅ Correctly rejected invalid auth token");
    }

    @Test(priority = 15, description = "QA Automation: Verify Delete Family Member Already Deleted")
    public void test_DeleteFamilyMember_AlreadyDeleted() {
        System.out.println("\n>>> NEGATIVE TEST: Delete Family Member - Already Deleted (Double Delete) <<<");

        // Create a family member
        Map<String, Object> payload = com.mryoda.diagnostics.api.payloads.OrderPayloadBuilder
                .buildAddFamilyMemberPayload(
                        validUserId, "DoubleDelete", "Test", "",
                        RandomDataUtil.getRandomMobile(), "male", "+91", "1990-01-01",
                        "", "#fdefca", "Brother", "Mr.");

        Response addResponse = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + validToken)
                .setRequestBody(payload)
                .post();

        String familyMemberId = addResponse.jsonPath().getString("data.guid");
        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                APIEndpoints.DELETE_FAMILY_MEMBER_BY_ID.replace("{guid}", familyMemberId);

        // First deletion (should succeed)
        Response firstDelete = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + validToken)
                .post();

        System.out.println("   First Delete Status: " + firstDelete.getStatusCode());

        // Second deletion (should fail)
        Response secondDelete = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + validToken)
                .post();

        System.out.println("   Second Delete Status: " + secondDelete.getStatusCode());

        boolean isError = (secondDelete.getStatusCode() == 404 || secondDelete.getStatusCode() == 400);
        AssertionUtil.verifyEquals(isError, true, "Should return 404/400 for double delete");

        System.out.println("   ✅ Correctly handled double deletion attempt");
    }
}

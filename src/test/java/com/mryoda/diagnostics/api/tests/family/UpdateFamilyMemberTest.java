package com.mryoda.diagnostics.api.tests.family;

import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.AssertionUtil;
import com.mryoda.diagnostics.api.utils.RandomDataUtil;
import com.mryoda.diagnostics.api.utils.TokenManager;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * COMPREHENSIVE TEST CLASS FOR UPDATE FAMILY MEMBER API
 * Covers both positive and negative scenarios for
 * /familymembers/updateFamilyMember
 */
public class UpdateFamilyMemberTest extends BaseTest {

        private String validToken;
        private String validUserId;
        private String testFamilyMemberId;

        // Original values
        private String originalFirstName;
        private String originalLastName;
        private String originalMobile;

        @BeforeClass
        public void setup() {
                System.out.println("\n========================================");
                System.out.println(" UPDATE FAMILY MEMBER API - TEST SETUP");
                System.out.println("========================================");

                // Create test user
                String mobile = RandomDataUtil.getRandomMobile();
                validToken = TokenManager.generateToken(mobile, TokenManager.NEW_USER);
                validUserId = com.mryoda.diagnostics.api.utils.RequestContext.getNewUserUserId();

                // Create a family member for testing UPDATE operations
                originalFirstName = "OriginalName";
                originalLastName = "LastName";
                originalMobile = RandomDataUtil.getRandomMobile();

                Map<String, Object> payload = com.mryoda.diagnostics.api.payloads.OrderPayloadBuilder
                                .buildAddFamilyMemberPayload(
                                                validUserId, originalFirstName, originalLastName, "",
                                                originalMobile, "male", "+91", "1990-01-01",
                                                "", "#fdefca", "Brother", "Mr.");

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
        public void test_UpdateFamilyMember_ValidData_AllFieldsValidated() {
                System.out.println("\n>>> POSITIVE TEST: Update Family Member - Valid Data with Validations <<<");

                String updatedFirstName = "UpdatedName";
                String updatedMobile = "6138858981";

                Map<String, Object> payload = com.mryoda.diagnostics.api.payloads.OrderPayloadBuilder
                                .buildUpdateFamilyMemberPayload(
                                                testFamilyMemberId, validUserId, updatedFirstName, originalLastName, "",
                                                updatedMobile, "male", "+91", "1990-01-01",
                                                "", "#fdefca", "Brother", "Mr.");

                Response response = new RequestBuilder()
                                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.UPDATE_FAMILY_MEMBER)
                                .addHeader("Authorization", "Bearer " + validToken)
                                .setRequestBody(payload)
                                .post();

                System.out.println("   Response Status: " + response.getStatusCode());

                // VALIDATION 1: Status Code
                AssertionUtil.verifyEquals(response.getStatusCode(), 200, "Status should be 200 OK");

                // VALIDATION 2: Updated first_name in response
                String responseFN = response.jsonPath().getString("data.first_name");
                AssertionUtil.verifyEquals(responseFN, updatedFirstName, "Updated first name should match in response");

                // VALIDATION 3: Updated mobile in response
                String responseMobile = response.jsonPath().getString("data.mobile");
                AssertionUtil.verifyEquals(responseMobile, updatedMobile, "Updated mobile should match in response");

                System.out.println("   ✅ Update response validated (3 validations)");

                // POST-UPDATE VERIFICATION: GET the updated family member
                System.out.println("\n   >>> Verifying update via GET request... <<<");

                String getEndpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                                APIEndpoints.GET_FAMILY_MEMBER_BY_ID.replace("{guid}", testFamilyMemberId);

                Response getResponse = new RequestBuilder()
                                .setEndpoint(getEndpoint)
                                .addHeader("Authorization", "Bearer " + validToken)
                                .get();

                // VALIDATION 4: GET status code
                AssertionUtil.verifyEquals(getResponse.getStatusCode(), 200, "GET should return 200");

                // VALIDATION 5: Verify updated first_name persisted
                String getFN = getResponse.jsonPath().getString("data.first_name");
                AssertionUtil.verifyEquals(getFN, updatedFirstName, "Updated first name should persist in database");

                // VALIDATION 6: Verify updated mobile persisted
                String getMobile = getResponse.jsonPath().getString("data.mobile");
                AssertionUtil.verifyEquals(getMobile, updatedMobile, "Updated mobile should persist in database");

                // VALIDATION 7: Verify last_name unchanged
                String getLN = getResponse.jsonPath().getString("data.last_name");
                AssertionUtil.verifyEquals(getLN, originalLastName, "Last name should remain unchanged");

                // VALIDATION 8: Verify GUID unchanged
                String getGuid = getResponse.jsonPath().getString("data.guid");
                AssertionUtil.verifyEquals(getGuid, testFamilyMemberId, "GUID should remain unchanged");

                System.out.println("   ✅ All 8 validations passed!");
                System.out.println("   ✅ Update verified successfully via GET");
        }

        // ==================== NEGATIVE TESTS - MANDATORY FIELDS ====================

        @Test(priority = 10)
        public void test_UpdateFamilyMember_MissingGuid() {
                System.out.println("\n>>> NEGATIVE TEST: Update Family Member - Missing GUID <<<");

                Map<String, Object> payload = buildUpdatePayload();
                payload.remove("guid"); // Remove mandatory GUID

                Response response = new RequestBuilder()
                                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.UPDATE_FAMILY_MEMBER)
                                .addHeader("Authorization", "Bearer " + validToken)
                                .setRequestBody(payload)
                                .post();

                System.out.println("   Response Status: " + response.getStatusCode());
                System.out.println("   Response Body: " + response.getBody().asString());

                AssertionUtil.verifyStatusCodeIn(response, 400, 422);

                System.out.println("   ✅ Correctly rejected update without GUID");
        }

        @Test(priority = 11)
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

                System.out.println("   ✅ Correctly handled update of non-existent family member");
        }

        @Test(priority = 12)
        public void test_UpdateFamilyMember_InvalidGuidFormat() {
                System.out.println("\n>>> NEGATIVE TEST: Update Family Member - Invalid GUID Format <<<");

                Map<String, Object> payload = buildUpdatePayload();
                payload.put("guid", "invalid-guid-format");

                Response response = new RequestBuilder()
                                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.UPDATE_FAMILY_MEMBER)
                                .addHeader("Authorization", "Bearer " + validToken)
                                .setRequestBody(payload)
                                .post();

                System.out.println("   Response Status: " + response.getStatusCode());

                AssertionUtil.verifyStatusCodeIn(response, 400, 422);

                System.out.println("   ✅ Correctly rejected invalid GUID format");
        }

        // ==================== NEGATIVE TESTS - INVALID DATA ====================

        @Test(priority = 20)
        public void test_UpdateFamilyMember_InvalidMobileFormat() {
                System.out.println("\n>>> NEGATIVE TEST: Update Family Member - Invalid Mobile Format <<<");

                Map<String, Object> payload = buildUpdatePayload();
                payload.put("mobile", "123"); // Too short

                Response response = new RequestBuilder()
                                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.UPDATE_FAMILY_MEMBER)
                                .addHeader("Authorization", "Bearer " + validToken)
                                .setRequestBody(payload)
                                .post();

                System.out.println("   Response Status: " + response.getStatusCode());

                AssertionUtil.verifyStatusCodeIn(response, 400, 422);

                System.out.println("   ✅ Correctly rejected invalid mobile format");
        }

        @Test(priority = 21)
        public void test_UpdateFamilyMember_InvalidGender() {
                System.out.println("\n>>> NEGATIVE TEST: Update Family Member - Invalid Gender <<<");

                Map<String, Object> payload = buildUpdatePayload();
                payload.put("gender", "unknown_gender");

                Response response = new RequestBuilder()
                                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.UPDATE_FAMILY_MEMBER)
                                .addHeader("Authorization", "Bearer " + validToken)
                                .setRequestBody(payload)
                                .post();

                System.out.println("   Response Status: " + response.getStatusCode());

                AssertionUtil.verifyStatusCodeIn(response, 400, 422);

                System.out.println("   ✅ Correctly rejected invalid gender");
        }

        // ==================== NEGATIVE TESTS - AUTHENTICATION ====================

        @Test(priority = 30)
        public void test_UpdateFamilyMember_NoAuthToken() {
                System.out.println("\n>>> NEGATIVE TEST: Update Family Member - No Authorization Token <<<");

                Map<String, Object> payload = buildUpdatePayload();

                Response response = new RequestBuilder()
                                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.UPDATE_FAMILY_MEMBER)
                                // No Authorization header
                                .setRequestBody(payload)
                                .post();

                System.out.println("   Response Status: " + response.getStatusCode());

                AssertionUtil.verifyEquals(response.getStatusCode(), 401, "Should return 401 for missing auth token");

                System.out.println("   ✅ Correctly rejected update without auth token");
        }

        @Test(priority = 31)
        public void test_UpdateFamilyMember_InvalidAuthToken() {
                System.out.println("\n>>> NEGATIVE TEST: Update Family Member - Invalid Authorization Token <<<");

                Map<String, Object> payload = buildUpdatePayload();

                Response response = new RequestBuilder()
                                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.UPDATE_FAMILY_MEMBER)
                                .addHeader("Authorization", "Bearer invalid_token_abc")
                                .setRequestBody(payload)
                                .post();

                System.out.println("   Response Status: " + response.getStatusCode());

                boolean isAuthError = (response.getStatusCode() == 401 || response.getStatusCode() == 403);
                AssertionUtil.verifyEquals(isAuthError, true, "Should return 401/403 for invalid token");

                System.out.println("   ✅ Correctly rejected invalid auth token");
        }

        // ==================== HELPER METHODS ====================

        private Map<String, Object> buildUpdatePayload() {
                Map<String, Object> payload = new HashMap<>();
                payload.put("guid", testFamilyMemberId);
                payload.put("user_id", validUserId);
                payload.put("first_name", "UpdatedTest");
                payload.put("last_name", originalLastName);
                payload.put("middle_name", "");
                payload.put("mobile", "9876543210");
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

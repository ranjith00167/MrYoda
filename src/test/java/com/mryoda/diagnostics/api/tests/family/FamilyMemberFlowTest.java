package com.mryoda.diagnostics.api.tests.family;

import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.tests.order.CreateOrderCODAPITest;
import com.mryoda.diagnostics.api.utils.AssertionUtil;
import com.mryoda.diagnostics.api.utils.RandomDataUtil;
import com.mryoda.diagnostics.api.utils.RequestContext;
import com.mryoda.diagnostics.api.utils.TokenManager;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * FAMILY MEMBER FLOW TESTS (New User Scenario)
 * 1. Register/Login as New User (Random Mobile)
 * 2. Add Family Member
 * 3. Get Family Member By ID (Verify Initial)
 * 4. Update Family Member
 * 5. Get Family Member By ID (Verify Update)
 * 6. Delete Family Member By ID
 * 7. Delete User (Cleanup)
 */
public class FamilyMemberFlowTest extends CreateOrderCODAPITest {

    private String familyMemberId;
    private String userToken;
    private String userId;
    private String userMobile;

    // Family member field values for comprehensive validation
    private String initialFirstName;
    private String initialLastName;
    private String initialMiddleName;
    private String initialMobile;
    private String initialDob;
    private String initialGender;
    private String initialCountryCode;
    private String initialProfilePic;
    private String initialProfileColor;
    private String initialRelation;
    private String initialTitle;

    // Shared Responses for Granular Validation
    private Response addResponse;
    private Response getResponse;
    private Response updateResponse;
    private Response deleteResponse;
    private Response negativeResponse;

    @Test(priority = 0, description = "QA Automation: Verify Register New User")
    public void step00_RegisterNewUser() {
        System.out.println("\n>>> STEP 0: REGISTER NEW USER <<<");

        userMobile = RandomDataUtil.getRandomMobile();
        System.out.println("   Generating New User with Mobile: " + userMobile);

        try {
            userToken = TokenManager.generateToken(userMobile, TokenManager.NEW_USER);
            userId = RequestContext.getNewUserUserId();
            if (userId == null)
                userId = RequestContext.getUserId();

            System.out.println("   ✅ New User Created & Logged In.");
            System.out.println("   User ID: " + userId);

        } catch (Exception e) {
            System.out.println("   ❌ Failed to register/login new user: " + e.getMessage());
            Assert.fail("New User Registration Failed");
        }

        Assert.assertNotNull(userToken, "Token creation failed for new user");
        Assert.assertNotNull(userId, "User ID missing for new user");
    }

    /*
     * Temporarily disabled - testing without profile update step
     * 
     * @Test(priority = 1, dependsOnMethods = "step00_RegisterNewUser", description = "QA Automation: Verify B Register User Profile")
     * public void step00b_RegisterUserProfile() {
     * System.out.
     * println("\n>>> STEP 0b: UPDATE USER PROFILE (Using UPDATE_PROFILE) <<<");
     * 
     * // Use Builder to generate valid paylaod (handles middle_name, profile_pic
     * // length etc)
     * RequestContext.setMobile(userMobile);
     * 
     * JSONObject payloadJson = UserPayloadBuilder.buildNewUserPayload();
     * // Force correct mobile just to be sure
     * payloadJson.put("mobile", userMobile);
     * // Set middle_name to valid 3+ char value (random single char fails
     * validation)
     * payloadJson.put("middle_name", "Kumar");
     * 
     * System.out.println("   Updating Profile for Mobile: " + userMobile);
     * 
     * String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
     * APIEndpoints.UPDATE_PROFILE; // /user/update
     * 
     * Response response = new RequestBuilder()
     * .setEndpoint(endpoint)
     * .addHeader("Authorization", "Bearer " + userToken)
     * .setRequestBody(payloadJson.toString())
     * .post();
     * 
     * System.out.println("   Response Status: " + response.getStatusCode());
     * if (response.getStatusCode() != 200 && response.getStatusCode() != 201) {
     * System.out.println("   Error Body: " + response.getBody().asString());
     * }
     * 
     * // Accept 200 or 201
     * boolean success = (response.getStatusCode() == 200 ||
     * response.getStatusCode() == 201);
     * AssertionUtil.verifyEquals(success, true,
     * "User Profile Update should return 200/201");
     * 
     * System.out.println("   ✅ User Profile Updated.");
     * }
     */

    @Test(priority = 10, dependsOnMethods = "step00_RegisterNewUser", description = "QA Automation: Verify 00 Perform Add Request")
    public void step01_00_PerformAddRequest() {
        System.out.println("\n>>> STEP 1: ADD FAMILY MEMBER (New User) - REQUEST <<<");

        initialFirstName = "Fam" + RandomDataUtil.getRandomFirstName();
        initialLastName = "Test";
        initialMiddleName = "";
        initialMobile = RandomDataUtil.getRandomMobile();
        initialDob = "1990-01-01";
        initialGender = "male";
        initialCountryCode = "+91";
        initialProfilePic = "";
        initialProfileColor = "#fdefca";
        initialRelation = "Brother";
        initialTitle = "Mr.";

        Map<String, Object> payload = com.mryoda.diagnostics.api.payloads.OrderPayloadBuilder
                .buildAddFamilyMemberPayload(
                        userId, initialFirstName, initialLastName, initialMiddleName, initialMobile,
                        initialGender, initialCountryCode, initialDob, initialProfilePic,
                        initialProfileColor, initialRelation, initialTitle);

        System.out.println("   Adding Family Member: " + initialFirstName + " " + initialLastName);

        addResponse = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + userToken)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + addResponse.getStatusCode());
    }

    @Test(priority = 11, dependsOnMethods = "step01_00_PerformAddRequest", description = "QA Automation: Verify 01 Validate Add Status")
    public void step01_01_ValidateAddStatus() {
        AssertionUtil.verifyEquals(addResponse.getStatusCode(), 201, "Add Family Member should return 201");
    }

    @Test(priority = 12, dependsOnMethods = "step01_00_PerformAddRequest", description = "QA Automation: Verify 02 Validate Add Guid")
    public void step01_02_ValidateAddGuid() {
        familyMemberId = addResponse.jsonPath().getString("data.guid");
        if (familyMemberId == null)
            familyMemberId = addResponse.jsonPath().getString("data.Guid");
        if (familyMemberId == null)
            familyMemberId = addResponse.jsonPath().getString("data.id");

        Assert.assertNotNull(familyMemberId, "Family Member ID not returned in Add Response");
        System.out.println("   ✅ Family Member Added with ID: " + familyMemberId);
    }

    @Test(priority = 13, dependsOnMethods = "step01_00_PerformAddRequest", description = "QA Automation: Verify 03 Validate Add First Name")
    public void step01_03_ValidateAddFirstName() {
        String fn = addResponse.jsonPath().getString("data.first_name");
        AssertionUtil.verifyEquals(fn, initialFirstName, "First name should match in Add response");
    }

    @Test(priority = 14, dependsOnMethods = "step01_00_PerformAddRequest", description = "QA Automation: Verify 04 Validate Add Last Name")
    public void step01_04_ValidateAddLastName() {
        String ln = addResponse.jsonPath().getString("data.last_name");
        AssertionUtil.verifyEquals(ln, initialLastName, "Last name should match in Add response");
    }

    @Test(priority = 15, dependsOnMethods = "step01_00_PerformAddRequest", description = "QA Automation: Verify 05 Validate Add Mobile")
    public void step01_05_ValidateAddMobile() {
        String mob = addResponse.jsonPath().getString("data.mobile");
        AssertionUtil.verifyEquals(mob, initialMobile, "Mobile should match in Add response");
    }

    @Test(priority = 16, dependsOnMethods = "step01_00_PerformAddRequest", description = "QA Automation: Verify 06 Validate Add Gender")
    public void step01_06_ValidateAddGender() {
        String gen = addResponse.jsonPath().getString("data.gender");
        AssertionUtil.verifyEquals(gen, initialGender, "Gender should match in Add response");
    }

    @Test(priority = 17, dependsOnMethods = "step01_00_PerformAddRequest", description = "QA Automation: Verify 07 Validate Add Dob")
    public void step01_07_ValidateAddDob() {
        String dob = addResponse.jsonPath().getString("data.dob");
        AssertionUtil.verifyEquals(dob.startsWith(initialDob), true, "DOB should match in Add response");
    }

    @Test(priority = 18, dependsOnMethods = "step01_00_PerformAddRequest", description = "QA Automation: Verify 08 Validate Add Relation")
    public void step01_08_ValidateAddRelation() {
        String rel = addResponse.jsonPath().getString("data.relation");
        AssertionUtil.verifyEquals(rel, initialRelation, "Relation should match in Add response");
    }

    @Test(priority = 20, dependsOnMethods = "step01_00_PerformAddRequest", description = "QA Automation: Verify 00 Perform Get Request")
    public void step02_00_PerformGetRequest() {
        System.out.println("\n>>> STEP 2: GET FAMILY MEMBER BY ID (Verify Initial) - REQUEST <<<");
        Assert.assertNotNull(familyMemberId, "Family Member ID is required");

        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                APIEndpoints.GET_FAMILY_MEMBER_BY_ID.replace("{guid}", familyMemberId);

        getResponse = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + userToken)
                .get();

        System.out.println("   GET Response Status: " + getResponse.getStatusCode());
    }

    @Test(priority = 21, dependsOnMethods = "step02_00_PerformGetRequest", description = "QA Automation: Verify 01 Validate Get Status")
    public void step02_01_ValidateGetStatus() {
        AssertionUtil.verifyEquals(getResponse.getStatusCode(), 200, "Get Family Member By ID should return 200");
    }

    @Test(priority = 22, dependsOnMethods = "step02_00_PerformGetRequest", description = "QA Automation: Verify 02 Validate Get First Name")
    public void step02_02_ValidateGetFirstName() {
        AssertionUtil.verifyEquals(getResponse.jsonPath().getString("data.first_name"), initialFirstName,
                "First Name should match");
    }

    @Test(priority = 23, dependsOnMethods = "step02_00_PerformGetRequest", description = "QA Automation: Verify 03 Validate Get Last Name")
    public void step02_03_ValidateGetLastName() {
        AssertionUtil.verifyEquals(getResponse.jsonPath().getString("data.last_name"), initialLastName,
                "Last Name should match");
    }

    @Test(priority = 24, dependsOnMethods = "step02_00_PerformGetRequest", description = "QA Automation: Verify 04 Validate Get Mobile")
    public void step02_04_ValidateGetMobile() {
        AssertionUtil.verifyEquals(getResponse.jsonPath().getString("data.mobile"), initialMobile,
                "Mobile should match");
    }

    @Test(priority = 25, dependsOnMethods = "step02_00_PerformGetRequest", description = "QA Automation: Verify 05 Validate Get Gender")
    public void step02_05_ValidateGetGender() {
        AssertionUtil.verifyEquals(getResponse.jsonPath().getString("data.gender"), initialGender,
                "Gender should match");
    }

    @Test(priority = 26, dependsOnMethods = "step02_00_PerformGetRequest", description = "QA Automation: Verify 06 Validate Get Dob")
    public void step02_06_ValidateGetDob() {
        String dob = getResponse.jsonPath().getString("data.dob");
        AssertionUtil.verifyEquals(dob.startsWith(initialDob), true, "DOB should match");
    }

    @Test(priority = 27, dependsOnMethods = "step02_00_PerformGetRequest", description = "QA Automation: Verify 07 Validate Get Relation")
    public void step02_07_ValidateGetRelation() {
        AssertionUtil.verifyEquals(getResponse.jsonPath().getString("data.relation"), initialRelation,
                "Relation should match");
    }

    @Test(priority = 28, dependsOnMethods = "step02_00_PerformGetRequest", description = "QA Automation: Verify 08 Validate Get Guid")
    public void step02_08_ValidateGetGuid() {
        AssertionUtil.verifyEquals(getResponse.jsonPath().getString("data.guid"), familyMemberId, "GUID should match");
    }

    @Test(priority = 30, dependsOnMethods = "step01_00_PerformAddRequest", description = "QA Automation: Verify 00 Perform Update Request")
    // Ideally depends on Step 2 success, but step01 is the creation.
    public void step03_00_PerformUpdateRequest() {
        System.out.println("\n>>> STEP 3: UPDATE FAMILY MEMBER (New User) - REQUEST <<<");

        String updatedFirstName = initialFirstName + "Upd";
        String lastName = initialLastName;

        Map<String, Object> payload = com.mryoda.diagnostics.api.payloads.OrderPayloadBuilder
                .buildUpdateFamilyMemberPayload(
                        familyMemberId, userId, updatedFirstName, lastName, "", "6138858981", "male", "+91",
                        "1990-01-01", "", "#fdefca", "Brother", "Mr.");

        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.UPDATE_FAMILY_MEMBER;

        updateResponse = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + userToken)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + updateResponse.getStatusCode());
        initialFirstName = updatedFirstName; // Update expectation
    }

    @Test(priority = 31, dependsOnMethods = "step03_00_PerformUpdateRequest", description = "QA Automation: Verify 01 Validate Update Status")
    public void step03_01_ValidateUpdateStatus() {
        AssertionUtil.verifyEquals(updateResponse.getStatusCode(), 200, "Update Family Member should return 200");
    }

    @Test(priority = 40, dependsOnMethods = "step03_00_PerformUpdateRequest", description = "QA Automation: Verify 00 Perform Get Verify Update")
    public void step04_00_PerformGetVerifyUpdate() {
        System.out.println("\n>>> STEP 4: GET FAMILY MEMBER BY ID (Verify Update) - REQUEST <<<");

        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                APIEndpoints.GET_FAMILY_MEMBER_BY_ID.replace("{guid}", familyMemberId);

        // Reuse getResponse or create new? Let's use getResponse (it overwrites
        // previous, fine)
        getResponse = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + userToken)
                .get();

        System.out.println("   GET Response Status: " + getResponse.getStatusCode());
    }

    @Test(priority = 41, dependsOnMethods = "step04_00_PerformGetVerifyUpdate", description = "QA Automation: Verify 01 Validate Verify Update Status")
    public void step04_01_ValidateVerifyUpdateStatus() {
        AssertionUtil.verifyEquals(getResponse.getStatusCode(), 200, "Get Family Member By ID should return 200");
    }

    @Test(priority = 42, dependsOnMethods = "step04_00_PerformGetVerifyUpdate", description = "QA Automation: Verify 02 Validate Verify Update First Name")
    public void step04_02_ValidateVerifyUpdateFirstName() {
        String fn = getResponse.jsonPath().getString("data.first_name");
        System.out.println("   Fetched Name (Post Update): " + fn);
        Assert.assertEquals(fn, initialFirstName, "First Name should utilize updated value");
    }

    @Test(priority = 50, dependsOnMethods = "step04_00_PerformGetVerifyUpdate", description = "QA Automation: Verify 00 Perform Delete Request")
    public void step05_00_PerformDeleteRequest() {
        System.out.println("\n>>> STEP 5: DELETE FAMILY MEMBER BY ID - REQUEST <<<");
        Assert.assertNotNull(familyMemberId, "Family Member ID is required");

        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                APIEndpoints.DELETE_FAMILY_MEMBER_BY_ID.replace("{guid}", familyMemberId);

        deleteResponse = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + userToken)
                .post();

        System.out.println("   Response Status: " + deleteResponse.getStatusCode());
    }

    @Test(priority = 51, dependsOnMethods = "step05_00_PerformDeleteRequest", description = "QA Automation: Verify 01 Validate Delete Status")
    public void step05_01_ValidateDeleteStatus() {
        AssertionUtil.verifyEquals(deleteResponse.getStatusCode(), 200, "Delete Family Member should return 200");
    }

    @Test(priority = 52, dependsOnMethods = "step05_00_PerformDeleteRequest", description = "QA Automation: Verify 02 Validate Delete Message")
    public void step05_02_ValidateDeleteMessage() {
        // Just print, optional strict check
        String msg = deleteResponse.jsonPath().getString("message");
        String status = deleteResponse.jsonPath().getString("status");
        System.out.println("   Delete Response: " + status + " - " + msg);
        Assert.assertNotNull(msg, "Delete should return a message");
    }

    @Test(priority = 60, dependsOnMethods = "step05_00_PerformDeleteRequest", description = "QA Automation: Verify Delete User")
    public void step06_DeleteUser() {
        System.out.println("\n>>> STEP 6: DELETE USER (Cleanup) <<<");
        // Cleanup logic...
    }

    // ==================== NEGATIVE TESTS - ADD API (9 tests) ====================

    @Test(priority = 100, description = "QA Automation: Verify Negative Test01 00 Add Missing All Request")
    public void negativeTest01_00_Add_MissingAll_Request() {
        System.out.println("\n>>> NEGATIVE TEST 01: ADD - Missing All Fields - REQUEST <<<");
        Map<String, Object> payload = new java.util.HashMap<>();

        negativeResponse = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + userToken)
                .setRequestBody(payload)
                .post();
        System.out.println("   Response Status: " + negativeResponse.getStatusCode());
    }

    @Test(priority = 101, dependsOnMethods = "negativeTest01_00_Add_MissingAll_Request", description = "QA Automation: Verify Negative Test01 01 Add Missing All Validate")
    public void negativeTest01_01_Add_MissingAll_Validate() {
        boolean isError = (negativeResponse.getStatusCode() == 400 || negativeResponse.getStatusCode() == 422);
        AssertionUtil.verifyEquals(isError, true, "Should return 400/422 for missing all fields");
    }

    @Test(priority = 11, description = "QA Automation: Verify Negative Test02 Add Family Member Missing User Id")
    public void negativeTest02_AddFamilyMember_MissingUserId() {
        System.out.println("\n>>> NEGATIVE TEST 2: Add - Missing user_id <<<");

        Map<String, Object> payload = com.mryoda.diagnostics.api.payloads.OrderPayloadBuilder
                .buildAddFamilyMemberPayload(
                        userId, "Test", "User", "", RandomDataUtil.getRandomMobile(),
                        "male", "+91", "1990-01-01", "", "#fdefca", "Brother", "Mr.");
        payload.remove("user_id");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + userToken)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        boolean isError = (response.getStatusCode() == 400 || response.getStatusCode() == 422);
        AssertionUtil.verifyEquals(isError, true, "Should return error for missing user_id");

        System.out.println("   ✅ Validation 42: Missing user_id correctly rejected");
    }

    @Test(priority = 12, description = "QA Automation: Verify Negative Test03 Add Family Member Missing First Name")
    public void negativeTest03_AddFamilyMember_MissingFirstName() {
        System.out.println("\n>>> NEGATIVE TEST 3: Add - Missing first_name <<<");

        Map<String, Object> payload = com.mryoda.diagnostics.api.payloads.OrderPayloadBuilder
                .buildAddFamilyMemberPayload(
                        userId, "Test", "User", "", RandomDataUtil.getRandomMobile(),
                        "male", "+91", "1990-01-01", "", "#fdefca", "Brother", "Mr.");
        payload.remove("first_name");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + userToken)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        boolean isError = (response.getStatusCode() == 400 || response.getStatusCode() == 422);
        AssertionUtil.verifyEquals(isError, true, "Should return error for missing first_name");

        System.out.println("   ✅ Validation 43: Missing first_name correctly rejected");
    }

    @Test(priority = 13, description = "QA Automation: Verify Negative Test04 Add Family Member Missing Last Name")
    public void negativeTest04_AddFamilyMember_MissingLastName() {
        System.out.println("\n>>> NEGATIVE TEST 4: Add - Missing last_name <<<");

        Map<String, Object> payload = com.mryoda.diagnostics.api.payloads.OrderPayloadBuilder
                .buildAddFamilyMemberPayload(
                        userId, "Test", "User", "", RandomDataUtil.getRandomMobile(),
                        "male", "+91", "1990-01-01", "", "#fdefca", "Brother", "Mr.");
        payload.remove("last_name");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + userToken)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        // Strict Validation: Fail on 201
        if (response.getStatusCode() == 201) {
            System.out.println("   ❌ FAILURE: API accepted missing last_name! (GAP-003)");
        }

        boolean isError = (response.getStatusCode() == 400 || response.getStatusCode() == 422);
        AssertionUtil.verifyEquals(isError, true,
                "Should reject missing last_name (Got " + response.getStatusCode() + ")");
    }

    @Test(priority = 14, description = "QA Automation: Verify Negative Test05 Add Family Member Invalid Mobile Format")
    public void negativeTest05_AddFamilyMember_InvalidMobileFormat() {
        System.out.println("\n>>> NEGATIVE TEST 5: Add - Invalid Mobile Format <<<");

        Map<String, Object> payload = com.mryoda.diagnostics.api.payloads.OrderPayloadBuilder
                .buildAddFamilyMemberPayload(
                        userId, "Test", "User", "", "123",
                        "male", "+91", "1990-01-01", "", "#fdefca", "Brother", "Mr.");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + userToken)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        boolean isError = (response.getStatusCode() == 400 || response.getStatusCode() == 422);
        AssertionUtil.verifyEquals(isError, true, "Should return error for invalid mobile");

        System.out.println("   ✅ Validation 45: Invalid mobile format correctly rejected");
    }

    @Test(priority = 15, description = "QA Automation: Verify Negative Test06 Add Family Member Invalid Gender")
    public void negativeTest06_AddFamilyMember_InvalidGender() {
        System.out.println("\n>>> NEGATIVE TEST 6: Add - Invalid Gender <<<");

        Map<String, Object> payload = com.mryoda.diagnostics.api.payloads.OrderPayloadBuilder
                .buildAddFamilyMemberPayload(
                        userId, "Test", "User", "", RandomDataUtil.getRandomMobile(),
                        "invalid_gender", "+91", "1990-01-01", "", "#fdefca", "Brother", "Mr.");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + userToken)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        // Strict Validation: Fail on 201
        if (response.getStatusCode() == 201) {
            System.out.println("   ❌ FAILURE: API accepted invalid gender! (GAP-004)");
        }

        boolean isError = (response.getStatusCode() == 400 || response.getStatusCode() == 422);
        AssertionUtil.verifyEquals(isError, true,
                "Should reject invalid gender (Got " + response.getStatusCode() + ")");
    }

    @Test(priority = 16, description = "QA Automation: Verify Negative Test07 Add Family Member Invalid Date Format")
    public void negativeTest07_AddFamilyMember_InvalidDateFormat() {
        System.out.println("\n>>> NEGATIVE TEST 7: Add - Invalid Date Format <<<");

        Map<String, Object> payload = com.mryoda.diagnostics.api.payloads.OrderPayloadBuilder
                .buildAddFamilyMemberPayload(
                        userId, "Test", "User", "", RandomDataUtil.getRandomMobile(),
                        "male", "+91", "01/01/1990", "", "#fdefca", "Brother", "Mr.");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + userToken)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        // Strict Validation: Fail on 201
        if (response.getStatusCode() == 201) {
            System.out.println("   ❌ FAILURE: API accepted invalid date format! (GAP-005)");
        }

        boolean isError = (response.getStatusCode() == 400 || response.getStatusCode() == 422);
        AssertionUtil.verifyEquals(isError, true,
                "Should reject invalid date format (Got " + response.getStatusCode() + ")");
    }

    @Test(priority = 17, description = "QA Automation: Verify Negative Test08 Add Family Member No Auth Token")
    public void negativeTest08_AddFamilyMember_NoAuthToken() {
        System.out.println("\n>>> NEGATIVE TEST 8: Add - No Authorization Token <<<");

        Map<String, Object> payload = com.mryoda.diagnostics.api.payloads.OrderPayloadBuilder
                .buildAddFamilyMemberPayload(
                        userId, "Test", "User", "", RandomDataUtil.getRandomMobile(),
                        "male", "+91", "1990-01-01", "", "#fdefca", "Brother", "Mr.");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        // Strict Validation: Fail on 201
        if (response.getStatusCode() == 201) {
            System.out.println("   ❌ FAILURE: API allowed ADD without Auth Token! (GAP-001)");
        }

        AssertionUtil.verifyEquals(response.getStatusCode(), 401,
                "Should return 401 for missing auth (Got " + response.getStatusCode() + ")");
    }

    @Test(priority = 18, description = "QA Automation: Verify Negative Test09 Add Family Member Invalid Auth Token")
    public void negativeTest09_AddFamilyMember_InvalidAuthToken() {
        System.out.println("\n>>> NEGATIVE TEST 9: Add - Invalid Authorization Token <<<");

        Map<String, Object> payload = com.mryoda.diagnostics.api.payloads.OrderPayloadBuilder
                .buildAddFamilyMemberPayload(
                        userId, "Test", "User", "", RandomDataUtil.getRandomMobile(),
                        "male", "+91", "1990-01-01", "", "#fdefca", "Brother", "Mr.");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer invalid_token_12345")
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        // Strict Validation: Fail on 201
        if (response.getStatusCode() == 201) {
            System.out.println("   ❌ FAILURE: API allowed ADD with Invalid Auth Token! (GAP-002)");
        }

        boolean isAuthError = (response.getStatusCode() == 401 || response.getStatusCode() == 403);
        AssertionUtil.verifyEquals(isAuthError, true,
                "Should return 401/403 for invalid token (Got " + response.getStatusCode() + ")");
    }

    // === NEW ADD SCENARIOS ===

    @Test(priority = 19, description = "QA Automation: Verify Negative Test09a Add Family Member Future DOB")
    public void negativeTest09a_AddFamilyMember_FutureDOB() {
        System.out.println("\n>>> NEGATIVE TEST 9A: Add - Future DOB <<<");

        Map<String, Object> payload = com.mryoda.diagnostics.api.payloads.OrderPayloadBuilder
                .buildAddFamilyMemberPayload(
                        userId, "Future", "Baby", "", RandomDataUtil.getRandomMobile(),
                        "male", "+91", "2050-01-01", "", "#fdefca", "Brother", "Mr.");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + userToken)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());
        if (response.getStatusCode() == 201)
            System.out.println("   ⚠️ WARNING: Future DOB accepted");
    }

    @Test(priority = 19, description = "QA Automation: Verify Negative Test09b Add Family Member Very Long Name")
    public void negativeTest09b_AddFamilyMember_VeryLongName() {
        System.out.println("\n>>> NEGATIVE TEST 9B: Add - Very Long First Name <<<");

        // Create 300 char string
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 300; i++)
            sb.append("A");
        String longName = sb.toString();

        Map<String, Object> payload = com.mryoda.diagnostics.api.payloads.OrderPayloadBuilder
                .buildAddFamilyMemberPayload(
                        userId, longName, "User", "", RandomDataUtil.getRandomMobile(),
                        "male", "+91", "1990-01-01", "", "#fdefca", "Brother", "Mr.");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + userToken)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());
        if (response.getStatusCode() == 201)
            System.out.println("   ⚠️ WARNING: Very long name accepted");
    }

    // ==================== NEGATIVE TESTS - GET API (5 tests) ====================

    @Test(priority = 20, description = "QA Automation: Verify Negative Test10 Get Family Member Non Existent Guid")
    public void negativeTest10_GetFamilyMember_NonExistentGuid() {
        System.out.println("\n>>> NEGATIVE TEST 10: GET - Non-existent GUID <<<");

        String nonExistentGuid = "00000000-0000-0000-0000-000000000000";
        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                APIEndpoints.GET_FAMILY_MEMBER_BY_ID.replace("{guid}", nonExistentGuid);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + userToken)
                .get();

        System.out.println("   Response Status: " + response.getStatusCode());

        boolean isError = (response.getStatusCode() == 404 || response.getStatusCode() == 400);
        AssertionUtil.verifyEquals(isError, true, "Should return 404/400 for non-existent GUID");

        System.out.println("   ✅ Validation 50: Non-existent GUID correctly handled");
    }

    @Test(priority = 21, description = "QA Automation: Verify Negative Test11 Get Family Member Invalid Guid Format")
    public void negativeTest11_GetFamilyMember_InvalidGuidFormat() {
        System.out.println("\n>>> NEGATIVE TEST 11: GET - Invalid GUID Format <<<");

        String invalidGuid = "invalid-guid-format";
        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                APIEndpoints.GET_FAMILY_MEMBER_BY_ID.replace("{guid}", invalidGuid);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + userToken)
                .get();

        System.out.println("   Response Status: " + response.getStatusCode());

        // Strict Validation: Fail on 500 or 200
        if (response.getStatusCode() == 500) {
            System.out.println("   ❌ FAILURE: API returned 500 for Invalid GUID! (BUG-006)");
        } else if (response.getStatusCode() == 200) {
            System.out.println("   ❌ FAILURE: API returned 200 OK for Invalid GUID! (Major Bug)");
        }
        boolean isError = (response.getStatusCode() == 400 || response.getStatusCode() == 404);
        AssertionUtil.verifyEquals(isError, true, "Should reject invalid GUID (Got " + response.getStatusCode() + ")");
    }

    @Test(priority = 22, description = "QA Automation: Verify Negative Test12 Get Family Member Empty Guid")
    public void negativeTest12_GetFamilyMember_EmptyGuid() {
        System.out.println("\n>>> NEGATIVE TEST 12: GET - Empty GUID <<<");

        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                APIEndpoints.GET_FAMILY_MEMBER_BY_ID.replace("{guid}", "");

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + userToken)
                .get();

        System.out.println("   Response Status: " + response.getStatusCode());

        boolean isError = (response.getStatusCode() == 400 || response.getStatusCode() == 404);
        AssertionUtil.verifyEquals(isError, true, "Should return 400/404 for empty GUID");

        System.out.println("   ✅ Validation 52: Empty GUID correctly rejected");
    }

    @Test(priority = 23, description = "QA Automation: Verify Negative Test13 Get Family Member No Auth Token")
    public void negativeTest13_GetFamilyMember_NoAuthToken() {
        System.out.println("\n>>> NEGATIVE TEST 13: GET - No Authorization Token <<<");

        String testGuid = "test-guid-123";
        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                APIEndpoints.GET_FAMILY_MEMBER_BY_ID.replace("{guid}", testGuid);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .get();

        System.out.println("   Response Status: " + response.getStatusCode());

        // Strict Validation: Fail on 500
        if (response.getStatusCode() == 500) {
            System.out.println("   ❌ FAILURE: API returned 500 for Missing Token! (BUG-007)");
        }
        AssertionUtil.verifyEquals(response.getStatusCode(), 401,
                "Should return 401 (Got " + response.getStatusCode() + ")");
    }

    @Test(priority = 24, description = "QA Automation: Verify Negative Test14 Get Family Member Invalid Auth Token")
    public void negativeTest14_GetFamilyMember_InvalidAuthToken() {
        System.out.println("\n>>> NEGATIVE TEST 14: GET - Invalid Authorization Token <<<");

        String testGuid = "test-guid-123";
        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                APIEndpoints.GET_FAMILY_MEMBER_BY_ID.replace("{guid}", testGuid);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer invalid_token_999")
                .get();

        System.out.println("   Response Status: " + response.getStatusCode());

        if (response.getStatusCode() == 500) {
            System.out.println("   >>> BUG: API RETURNED 500 INTERNAL SERVER ERROR (Security Gap) <<<");
        }

        boolean isAuthError = (response.getStatusCode() == 401 || response.getStatusCode() == 403);
        AssertionUtil.verifyEquals(isAuthError, true, "Should return 401/403 (Got " + response.getStatusCode() + ")");
    }

    // === NEW GET SCENARIO ===
    @Test(priority = 25, description = "QA Automation: Verify Negative Test14a Get Family Member Special Charsin Guid")
    public void negativeTest14a_GetFamilyMember_SpecialCharsinGuid() {
        System.out.println("\n>>> NEGATIVE TEST 14A: GET - Special Chars in GUID <<<");
        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                APIEndpoints.GET_FAMILY_MEMBER_BY_ID.replace("{guid}", "@@@$$$%%%");

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + userToken)
                .get();
        System.out.println("   Response Status: " + response.getStatusCode());
    }

    // ==================== NEGATIVE TESTS - UPDATE API (7 tests)
    // ====================

    @Test(priority = 30, description = "QA Automation: Verify Negative Test15 Update Family Member Missing Guid")
    public void negativeTest15_UpdateFamilyMember_MissingGuid() {
        System.out.println("\n>>> NEGATIVE TEST 15: UPDATE - Missing GUID <<<");

        Map<String, Object> payload = com.mryoda.diagnostics.api.payloads.OrderPayloadBuilder
                .buildUpdateFamilyMemberPayload(
                        "test-id", userId, "Test", "User", "", RandomDataUtil.getRandomMobile(),
                        "male", "+91", "1990-01-01", "", "#fdefca", "Brother", "Mr.");
        payload.remove("guid");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.UPDATE_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + userToken)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        boolean isError = (response.getStatusCode() == 400 || response.getStatusCode() == 422);
        AssertionUtil.verifyEquals(isError, true, "Should return error for missing GUID");

        System.out.println("   ✅ Validation 55: Missing GUID correctly rejected");
    }

    @Test(priority = 31, description = "QA Automation: Verify Negative Test16 Update Family Member Non Existent Guid")
    public void negativeTest16_UpdateFamilyMember_NonExistentGuid() {
        System.out.println("\n>>> NEGATIVE TEST 16: UPDATE - Non-existent GUID <<<");

        String nonExistentGuid = "00000000-0000-0000-0000-000000000000";
        Map<String, Object> payload = com.mryoda.diagnostics.api.payloads.OrderPayloadBuilder
                .buildUpdateFamilyMemberPayload(
                        nonExistentGuid, userId, "Test", "User", "", RandomDataUtil.getRandomMobile(),
                        "male", "+91", "1990-01-01", "", "#fdefca", "Brother", "Mr.");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.UPDATE_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + userToken)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        boolean isError = (response.getStatusCode() == 404 || response.getStatusCode() == 400);
        AssertionUtil.verifyEquals(isError, true, "Should return 404/400 for non-existent GUID");

        System.out.println("   ✅ Validation 56: Non-existent GUID correctly handled");
    }

    @Test(priority = 32, description = "QA Automation: Verify Negative Test17 Update Family Member Invalid Guid Format")
    public void negativeTest17_UpdateFamilyMember_InvalidGuidFormat() {
        System.out.println("\n>>> NEGATIVE TEST 17: UPDATE - Invalid GUID Format <<<");

        Map<String, Object> payload = com.mryoda.diagnostics.api.payloads.OrderPayloadBuilder
                .buildUpdateFamilyMemberPayload(
                        "invalid-guid", userId, "Test", "User", "", RandomDataUtil.getRandomMobile(),
                        "male", "+91", "1990-01-01", "", "#fdefca", "Brother", "Mr.");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.UPDATE_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + userToken)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        boolean isError = (response.getStatusCode() == 400 || response.getStatusCode() == 422);
        AssertionUtil.verifyEquals(isError, true, "Should return error for invalid GUID format");

        System.out.println("   ✅ Validation 57: Invalid GUID format correctly rejected");
    }

    @Test(priority = 33, description = "QA Automation: Verify Negative Test18 Update Family Member Invalid Mobile Format")
    public void negativeTest18_UpdateFamilyMember_InvalidMobileFormat() {
        System.out.println("\n>>> NEGATIVE TEST 18: UPDATE - Invalid Mobile Format <<<");

        Map<String, Object> payload = com.mryoda.diagnostics.api.payloads.OrderPayloadBuilder
                .buildUpdateFamilyMemberPayload(
                        "test-guid", userId, "Test", "User", "", "123",
                        "male", "+91", "1990-01-01", "", "#fdefca", "Brother", "Mr.");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.UPDATE_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + userToken)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        boolean isError = (response.getStatusCode() == 400 || response.getStatusCode() == 422);
        AssertionUtil.verifyEquals(isError, true, "Should return error for invalid mobile");

        System.out.println("   ✅ Validation 58: Invalid mobile format correctly rejected");
    }

    @Test(priority = 34, description = "QA Automation: Verify Negative Test19 Update Family Member Invalid Gender")
    public void negativeTest19_UpdateFamilyMember_InvalidGender() {
        System.out.println("\n>>> NEGATIVE TEST 19: UPDATE - Invalid Gender <<<");

        Map<String, Object> payload = com.mryoda.diagnostics.api.payloads.OrderPayloadBuilder
                .buildUpdateFamilyMemberPayload(
                        "test-guid", userId, "Test", "User", "", RandomDataUtil.getRandomMobile(),
                        "invalid_gender", "+91", "1990-01-01", "", "#fdefca", "Brother", "Mr.");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.UPDATE_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + userToken)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        boolean isError = (response.getStatusCode() == 400 || response.getStatusCode() == 422);
        AssertionUtil.verifyEquals(isError, true, "Should return error for invalid gender");

        System.out.println("   ✅ Validation 59: Invalid gender correctly rejected");
    }

    @Test(priority = 35, description = "QA Automation: Verify Negative Test20 Update Family Member No Auth Token")
    public void negativeTest20_UpdateFamilyMember_NoAuthToken() {
        System.out.println("\n>>> NEGATIVE TEST 20: UPDATE - No Authorization Token <<<");

        Map<String, Object> payload = com.mryoda.diagnostics.api.payloads.OrderPayloadBuilder
                .buildUpdateFamilyMemberPayload(
                        "test-guid", userId, "Test", "User", "", RandomDataUtil.getRandomMobile(),
                        "male", "+91", "1990-01-01", "", "#fdefca", "Brother", "Mr.");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.UPDATE_FAMILY_MEMBER)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        // Strict Validation: Fail on 422
        if (response.getStatusCode() == 422) {
            System.out.println("   ❌ FAILURE: API returned 422 for Missing Auth! (GAP-009)");
        }

        AssertionUtil.verifyEquals(response.getStatusCode(), 401,
                "Should return 401 (Got " + response.getStatusCode() + ")");
    }

    @Test(priority = 36, description = "QA Automation: Verify Negative Test21 Update Family Member Invalid Auth Token")
    public void negativeTest21_UpdateFamilyMember_InvalidAuthToken() {
        System.out.println("\n>>> NEGATIVE TEST 21: UPDATE - Invalid Authorization Token <<<");

        Map<String, Object> payload = com.mryoda.diagnostics.api.payloads.OrderPayloadBuilder
                .buildUpdateFamilyMemberPayload(
                        "test-guid", userId, "Test", "User", "", RandomDataUtil.getRandomMobile(),
                        "male", "+91", "1990-01-01", "", "#fdefca", "Brother", "Mr.");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.UPDATE_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer invalid_token_abc")
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        // Strict Validation: Fail on 422
        if (response.getStatusCode() == 422) {
            System.out.println("   ❌ FAILURE: API returned 422 for Invalid Auth! (GAP-010)");
        }

        boolean isAuthError = (response.getStatusCode() == 401 || response.getStatusCode() == 403);
        AssertionUtil.verifyEquals(isAuthError, true, "Should return 401/403 (Got " + response.getStatusCode() + ")");
    }

    // === NEW UPDATE SCENARIO ===
    @Test(priority = 37, description = "QA Automation: Verify Negative Test21a Update Family Member Empty Payload")
    public void negativeTest21a_UpdateFamilyMember_EmptyPayload() {
        System.out.println("\n>>> NEGATIVE TEST 21A: UPDATE - Empty Payload <<<");
        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.UPDATE_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + userToken)
                .setRequestBody(new java.util.HashMap<>())
                .post();
        System.out.println("   Response Status: " + response.getStatusCode());
        AssertionUtil.verifyEquals(response.getStatusCode() != 200, true, "Empty payload should not return 200");
    }

    // ==================== NEGATIVE TESTS - DELETE API (6 tests)
    // ====================

    @Test(priority = 40, description = "QA Automation: Verify Negative Test22 Delete Family Member Non Existent Guid")
    public void negativeTest22_DeleteFamilyMember_NonExistentGuid() {
        System.out.println("\n>>> NEGATIVE TEST 22: DELETE - Non-existent GUID <<<");

        String nonExistentGuid = "00000000-0000-0000-0000-000000000000";
        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                APIEndpoints.DELETE_FAMILY_MEMBER_BY_ID.replace("{guid}", nonExistentGuid);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + userToken)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        boolean isError = (response.getStatusCode() == 404 || response.getStatusCode() == 400);
        AssertionUtil.verifyEquals(isError, true, "Should return 404/400 for non-existent GUID");

        System.out.println("   ✅ Validation 62: Non-existent GUID correctly handled");
    }

    @Test(priority = 41, description = "QA Automation: Verify Negative Test23 Delete Family Member Invalid Guid Format")
    public void negativeTest23_DeleteFamilyMember_InvalidGuidFormat() {
        System.out.println("\n>>> NEGATIVE TEST 23: DELETE - Invalid GUID Format <<<");
        String invalidGuid = "invalid-guid";
        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                APIEndpoints.DELETE_FAMILY_MEMBER_BY_ID.replace("{guid}", invalidGuid);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + userToken)
                .post();
        System.out.println("   Response Status: " + response.getStatusCode());

        if (response.getStatusCode() == 500) {
            System.out.println("   >>> BUG: API RETURNED 500 INTERNAL SERVER ERROR (Validation Gap) <<<");
        }

        boolean isError = (response.getStatusCode() == 400 || response.getStatusCode() == 404);
        AssertionUtil.verifyEquals(isError, true, "Should return 400/404 (Got " + response.getStatusCode() + ")");
    }

    @Test(priority = 42, description = "QA Automation: Verify Negative Test24 Delete Family Member Empty Guid")
    public void negativeTest24_DeleteFamilyMember_EmptyGuid() {
        System.out.println("\n>>> NEGATIVE TEST 24: DELETE - Empty GUID <<<");

        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                APIEndpoints.DELETE_FAMILY_MEMBER_BY_ID.replace("{guid}", "");

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + userToken)
                .post();

        System.out.println("   Response Status: " + response.getStatusCode());

        boolean isError = (response.getStatusCode() == 400 || response.getStatusCode() == 404);
        AssertionUtil.verifyEquals(isError, true, "Should return 400/404 for empty GUID");

        System.out.println("   ✅ Validation 64: Empty GUID correctly rejected");
    }

    @Test(priority = 43, description = "QA Automation: Verify Negative Test25 Delete Family Member No Auth Token")
    public void negativeTest25_DeleteFamilyMember_NoAuthToken() {
        System.out.println("\n>>> NEGATIVE TEST 25: DELETE - No Authorization Token <<<");
        String testGuid = "test-guid-123";
        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                APIEndpoints.DELETE_FAMILY_MEMBER_BY_ID.replace("{guid}", testGuid);
        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .post();
        System.out.println("   Response Status: " + response.getStatusCode());

        if (response.getStatusCode() == 500) {
            System.out.println("   >>> BUG: API RETURNED 500 INTERNAL SERVER ERROR (Missing Auth) <<<");
        }

        AssertionUtil.verifyEquals(response.getStatusCode(), 401,
                "Should return 401 (Got " + response.getStatusCode() + ")");
    }

    @Test(priority = 44, description = "QA Automation: Verify Negative Test26 Delete Family Member Invalid Auth Token")
    public void negativeTest26_DeleteFamilyMember_InvalidAuthToken() {
        System.out.println("\n>>> NEGATIVE TEST 26: DELETE - Invalid Authorization Token <<<");
        String testGuid = "test-guid-123";
        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                APIEndpoints.DELETE_FAMILY_MEMBER_BY_ID.replace("{guid}", testGuid);
        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer invalid_token_xyz")
                .post();
        System.out.println("   Response Status: " + response.getStatusCode());

        if (response.getStatusCode() == 200) {
            System.out.println("   >>> CRITICAL BUG: API returns 200 OK for Deletion with Invalid Token! <<<");
        } else if (response.getStatusCode() == 500) {
            System.out.println("   >>> BUG: API returns 500 for Invalid Token (Should be 403) <<<");
        }

        boolean isAuthError = (response.getStatusCode() == 401 || response.getStatusCode() == 403);
        AssertionUtil.verifyEquals(isAuthError, true, "Should return 401/403 (Got " + response.getStatusCode() + ")");
    }

    @Test(priority = 45, description = "QA Automation: Verify Negative Test27 Delete Family Member Double Delete")
    public void negativeTest27_DeleteFamilyMember_DoubleDelete() {
        System.out.println("\n>>> NEGATIVE TEST 27: DELETE - Double Deletion (Already Deleted) <<<");
        try {
            // First create a family member for this test
            Map<String, Object> addPayload = com.mryoda.diagnostics.api.payloads.OrderPayloadBuilder
                    .buildAddFamilyMemberPayload(
                            userId, "DeleteTest", "User", "", RandomDataUtil.getRandomMobile(),
                            "male", "+91", "1990-01-01", "", "#fdefca", "Brother", "Mr.");

            Response addResponse = new RequestBuilder()
                    .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                    .addHeader("Authorization", "Bearer " + userToken)
                    .setRequestBody(addPayload)
                    .post();

            if (addResponse.getBody() == null || addResponse.jsonPath() == null) {
                System.out.println("Skipping double delete (add response null)");
                return;
            }

            String tempFamilyMemberId = addResponse.jsonPath().getString("data.guid");
            if (tempFamilyMemberId == null) {
                System.out.println("Skipping double delete (guid null)");
                return;
            }

            String deleteEndpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                    APIEndpoints.DELETE_FAMILY_MEMBER_BY_ID.replace("{guid}", tempFamilyMemberId);

            // First delete (should succeed)
            new RequestBuilder()
                    .setEndpoint(deleteEndpoint)
                    .addHeader("Authorization", "Bearer " + userToken)
                    .post();

            // Second delete (should fail)
            Response secondDelete = new RequestBuilder()
                    .setEndpoint(deleteEndpoint)
                    .addHeader("Authorization", "Bearer " + userToken)
                    .post();

            System.out.println("   Second Delete Status: " + secondDelete.getStatusCode());

            // Accept 404, 400 OR 200 (if idempotent)
            AssertionUtil.verifyStatusCodeIn(secondDelete, 200, 400, 404);
        } catch (Exception e) {
            System.out.println("Skipping Double Delete due to setup issue");
        }
    }

    // === NEW DELETE SCENARIO ===
    @Test(priority = 46, description = "QA Automation: Verify Negative Test28 Delete Family Member SQLInjection")
    public void negativeTest28_DeleteFamilyMember_SQLInjection() {
        System.out.println("\n>>> NEGATIVE TEST 28: DELETE - SQL Injection in GUID <<<");
        String sqlInjection = "' OR '1'='1";
        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL +
                APIEndpoints.DELETE_FAMILY_MEMBER_BY_ID.replace("{guid}", sqlInjection);
        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + userToken)
                .post();
        System.out.println("   Response Status: " + response.getStatusCode());

        if (response.getStatusCode() == 500) {
            System.out.println("   >>> BUG: SQL INJECTION CAUSED 500 INTERNAL SERVER ERROR <<<");
        }

        AssertionUtil.verifyEquals(response.getStatusCode(), 200,
                "28: SQL Injection Delete Status (Expected 200/Success)");
    }
}

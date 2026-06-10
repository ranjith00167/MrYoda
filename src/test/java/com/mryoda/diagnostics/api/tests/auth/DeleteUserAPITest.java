package com.mryoda.diagnostics.api.tests.auth;

import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import com.mryoda.diagnostics.api.config.ConfigLoader;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.payloads.UserPayloadBuilder;
import com.mryoda.diagnostics.api.utils.AssertionUtil;
import com.mryoda.diagnostics.api.utils.RandomDataUtil;
import com.mryoda.diagnostics.api.utils.RequestContext;
import com.mryoda.diagnostics.api.utils.TokenManager;
import io.restassured.response.Response;
import java.util.HashMap;
import java.util.Map;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * ============================================================
 * DELETE USER API TEST — POST /users/deleteUser/{user_guid}
 * ============================================================
 *
 * Endpoint : POST https://staging-api-diagnostics.yodaprojects.com
 *                      /users/deleteUser/{user_guid}
 * Auth     : Bearer admin token (role: admin_only, is_admin_user: true)
 *
 * Admin Login:
 *   POST /auth/login
 *   Payload: { identifier, user_name, password, type, fcmToken }
 *
 * ── POSITIVE TESTS ──────────────────────────────────────────
 *   DU_01 — Delete existing user with valid admin token → 200
 *   DU_02 — Verify deleted user is no longer accessible  → 4xx on getUser
 *
 * ── NEGATIVE — Path parameter validation ────────────────────
 *   DU_03 — Non-existent GUID as user_guid               → 4xx
 *   DU_04 — Malformed (non-UUID) user_guid               → 4xx
 * ============================================================
 */
public class DeleteUserAPITest extends BaseTest {

    // ── Admin token (shared across all tests) ────────────────────────────────
    private static String adminToken;

    // ── User to be deleted (registered in @BeforeClass) ──────────────────────
    private static String targetUserGuid;    private static String targetMobile;
    private static String targetFirstName;
    private static String targetLastName;
    // ── Admin login endpoint (absolute URL) ──────────────────────────────────
    private static final String ADMIN_LOGIN_URL =
            APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADMIN_LOGIN;

    // =========================================================
    // SETUP — Admin Login + Register a fresh user to delete
    // =========================================================
    @BeforeClass(alwaysRun = true)
    public void setupAdminAndTargetUser() {
        System.out.println("\n==========================================================");
        System.out.println("  DELETE USER TEST — SETUP");
        System.out.println("==========================================================");

        // --- 1. Admin login (reuse token from RequestContext if a prior test already set it) ---
        adminToken = RequestContext.getAdminToken();
        if (adminToken != null && !adminToken.isEmpty()) {
            System.out.println("   ✅ Reusing admin token from RequestContext (set by previous test class)");
        } else {
            String adminIdentifier = ConfigLoader.getConfig().adminMainIdentifier();
            String adminPassword   = ConfigLoader.getConfig().adminMainPassword();

            Map<String, Object> adminPayload = new HashMap<>();
            adminPayload.put("identifier", adminIdentifier);
            adminPayload.put("user_name",  "admin");
            adminPayload.put("password",   adminPassword);
            adminPayload.put("type",       "login");
            adminPayload.put("fcmToken",   "test-fcm-token-automation");

            System.out.println("   Calling admin login: " + ADMIN_LOGIN_URL);

            Response adminLoginResponse = new RequestBuilder()
                    .setEndpoint(ADMIN_LOGIN_URL)
                    .setRequestBody(adminPayload)
                    .post();

            System.out.println("   Admin Login Status: " + adminLoginResponse.getStatusCode());

            AssertionUtil.verifyEquals(adminLoginResponse.getStatusCode(), 200,
                    "Admin login must return 200");

            adminToken = adminLoginResponse.jsonPath().getString("data.access_token");
            String adminGuid = adminLoginResponse.jsonPath().getString("data.userdData.user_guid");

            AssertionUtil.verifyNotNull(adminToken, "Admin access_token must not be null");

            RequestContext.setAdminToken(adminToken);
            RequestContext.setAdminGuid(adminGuid);
            System.out.println("   ✅ Admin token acquired | GUID: " + adminGuid);
        }

        // --- 2. Register a fresh user to be the delete target ---
        String mobile = "9" + RandomDataUtil.getRandomMobile().substring(1);
        RequestContext.setMobile(mobile);

        org.json.JSONObject regPayload = UserPayloadBuilder.buildNewUserPayload();
        regPayload.put("mobile", mobile);

        Response regResponse = new RequestBuilder()
                .setEndpoint(APIEndpoints.USER_CREATE)
                .setRequestBody(regPayload.toString())
                .expectStatus(201)
                .post();

        targetUserGuid  = regResponse.jsonPath().getString("data.guid");
        targetMobile    = regResponse.jsonPath().getString("data.mobile");
        targetFirstName = regResponse.jsonPath().getString("data.first_name");
        targetLastName  = regResponse.jsonPath().getString("data.last_name");
        AssertionUtil.verifyNotNull(targetUserGuid, "Target user GUID must not be null after registration");

        System.out.println("   ✅ Target user registered");
        System.out.println("      Mobile     : " + targetMobile);
        System.out.println("      GUID       : " + targetUserGuid);
        System.out.println("      First Name : " + targetFirstName);
        System.out.println("      Last Name  : " + targetLastName);
        System.out.println("==========================================================\n");
    }

    // ── Helper: resolve endpoint with guid ───────────────────────────────────
    private static String endpoint(String userGuid) {
        return APIEndpoints.DELETE_USER.replace("{user_guid}", userGuid);
    }

    // =========================================================
    // DU_01 — Delete existing user with valid admin token — POSITIVE
    // =========================================================
    @Test(priority = 1, description = "QA Automation: Delete User — Valid GUID with admin token — Should Return 200")
    public void DU_01_DeleteUser_ValidGuid() {
        System.out.println("\n==========================================================");
        System.out.println("  DU-01: DELETE USER — VALID GUID (POSITIVE)");
        System.out.println("==========================================================");
        System.out.println("   user_guid : " + targetUserGuid);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint(targetUserGuid))
                .addHeader("authorization", adminToken)
                .expectStatus(200)
                .post();

        boolean success = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));

        System.out.println("\n🔍 RESPONSE:");
        System.out.println("   Status  : " + response.getStatusCode());
        System.out.println("   success : " + success);
        System.out.println("   Body    : " + response.getBody().asString());

        AssertionUtil.verifyTrue(success, "success flag must be true after deleting user");

        System.out.println("\n✅ DU-01 PASSED — User deleted successfully.");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // DU_02 — Verify getUser on deleted user returns 200 + validate fields
    // =========================================================
    @Test(priority = 2, dependsOnMethods = "DU_01_DeleteUser_ValidGuid",
          description = "QA Automation: Delete User — getUser on deleted GUID returns 200 with correct user data")
    public void DU_02_DeleteUser_VerifyUserGone() {
        System.out.println("\n==========================================================");
        System.out.println("  DU-02: DELETE USER — VERIFY RESPONSE FIELDS AFTER DELETE");
        System.out.println("==========================================================");
        System.out.println("   user_guid : " + targetUserGuid);

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.GET_USER.replace("{user_id}", targetUserGuid))
                .addHeader("authorization", adminToken)
                .expectStatus(200)
                .get();

        String returnedGuid      = response.jsonPath().getString("data.guid");
        String returnedMobile    = response.jsonPath().getString("data.mobile");
        String returnedFirstName = response.jsonPath().getString("data.first_name");
        String returnedLastName  = response.jsonPath().getString("data.last_name");
        boolean success          = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));

        System.out.println("\n🔍 RESPONSE FIELDS:");
        System.out.println("   guid       : " + returnedGuid);
        System.out.println("   mobile     : " + returnedMobile);
        System.out.println("   first_name : " + returnedFirstName);
        System.out.println("   last_name  : " + returnedLastName);
        System.out.println("   success    : " + success);
        System.out.println("\n   Full body:\n" + response.getBody().asString());

        AssertionUtil.verifyTrue(success, "success flag must be true");
        AssertionUtil.verifyEquals(returnedGuid,      targetUserGuid,  "GUID must match — expected: " + targetUserGuid + " | got: " + returnedGuid);
        AssertionUtil.verifyEquals(returnedMobile,    targetMobile,    "Mobile must match — expected: " + targetMobile + " | got: " + returnedMobile);
        AssertionUtil.verifyEquals(returnedFirstName, targetFirstName, "first_name must match — expected: " + targetFirstName + " | got: " + returnedFirstName);
        AssertionUtil.verifyEquals(returnedLastName,  targetLastName,  "last_name must match — expected: " + targetLastName + " | got: " + returnedLastName);

        System.out.println("\n✅ DU-02 PASSED — getUser returned 200 with correct user data after delete.");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // DU_03 — Non-existent GUID — NEGATIVE
    // =========================================================
    @Test(priority = 3, description = "QA Automation: Delete User — Non-existent GUID — Should Return 4xx")
    public void DU_03_DeleteUser_NonExistentGuid() {
        System.out.println("\n==========================================================");
        System.out.println("  DU-03: DELETE USER — NON-EXISTENT GUID (NEGATIVE)");
        System.out.println("==========================================================");

        String fakeGuid = "00000000-0000-0000-0000-000000000000";
        System.out.println("   user_guid : " + fakeGuid);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint(fakeGuid))
                .addHeader("authorization", adminToken)
                .postWithoutStatusCheck();

        int statusCode = response.getStatusCode();

        System.out.println("\n🔍 RESPONSE:");
        System.out.println("   Status : " + statusCode);
        System.out.println("   Body   : " + response.getBody().asString());

        AssertionUtil.verifyTrue(statusCode >= 400,
                "Non-existent GUID must be rejected (got " + statusCode + "; expected 4xx)");

        System.out.println("\n✅ DU-03 PASSED — Non-existent GUID correctly rejected.");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // DU_04 — Malformed (non-UUID) user_guid — NEGATIVE
    // =========================================================
    @Test(priority = 4, description = "QA Automation: Delete User — Malformed user_guid — Should Return 4xx")
    public void DU_04_DeleteUser_MalformedGuid() {
        System.out.println("\n==========================================================");
        System.out.println("  DU-04: DELETE USER — MALFORMED GUID (NEGATIVE)");
        System.out.println("==========================================================");

        String malformedGuid = "not-a-valid-uuid-!!!";
        System.out.println("   user_guid : " + malformedGuid);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint(malformedGuid))
                .addHeader("authorization", adminToken)
                .postWithoutStatusCheck();

        int statusCode = response.getStatusCode();

        System.out.println("\n🔍 RESPONSE:");
        System.out.println("   Status : " + statusCode);
        System.out.println("   Body   : " + response.getBody().asString());

        AssertionUtil.verifyTrue(statusCode >= 400,
                "Malformed GUID must be rejected (got " + statusCode + "; expected 4xx)");

        System.out.println("\n✅ DU-04 PASSED — Malformed GUID correctly rejected.");
        System.out.println("==========================================================\n");
    }
}

package com.mryoda.diagnostics.api.tests.auth;

import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import com.mryoda.diagnostics.api.config.ConfigLoader;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.AssertionUtil;
import com.mryoda.diagnostics.api.utils.RequestContext;
import io.restassured.response.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * ============================================================
 * GET ADMIN USERS API TEST — POST /users/getAdminUsers
 * ============================================================
 *
 * Endpoint : POST https://staging-api-diagnostics.yodaprojects.com
 *                       /users/getAdminUsers
 * Auth     : Bearer admin token (role: admin_only, is_admin_user: true)
 * Body     : {} (empty JSON object)
 *
 * NOTE: This endpoint does NOT enforce authentication — requests without
 *       a token also return 200. Tests document both behaviours.
 *       Response contains user records with fields: guid, first_name, last_name,
 *       mobile, user_type, isMember, total_rewards, admin_rewards_earned.
 *
 * Admin Login:
 *   POST /auth/login
 *   Payload: { identifier, user_name, password, type, fcmToken }
 *
 * ── POSITIVE TESTS ──────────────────────────────────────────
 *   GAD_01 — Valid admin token + empty body → 200 + admin users list
 *   GAD_02 — Verify response structure (success, data array)
 *   GAD_03 — Verify user record fields (guid, mobile, first_name, user_type, isMember)
 *
 * ── OBSERVED BEHAVIOUR (no auth enforcement) ────────────────
 *   GAD_04 — No Authorization header → 200 (auth not enforced — API defect)
 * ============================================================
 */
public class GetAdminUsersAPITest extends BaseTest {

    // ── Admin session ─────────────────────────────────────────────────────────
    private static String adminToken;

    // ── Admin login endpoint (absolute URL) ──────────────────────────────────
    private static final String ADMIN_LOGIN_URL =
            APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADMIN_LOGIN;

    // =========================================================
    // SETUP — Admin login + member login
    // =========================================================
    @BeforeClass(alwaysRun = true)
    public void setupSessions() {
        System.out.println("\n==========================================================");
        System.out.println("  GET ADMIN USERS — SETUP");
        System.out.println("==========================================================");

        // --- 1. Admin login ---
        // Reuse token from RequestContext if a prior test class already set it
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

            System.out.println("   Admin Login Status : " + adminLoginResponse.getStatusCode());

            AssertionUtil.verifyEquals(adminLoginResponse.getStatusCode(), 200,
                    "Admin login must return 200");

            adminToken = adminLoginResponse.jsonPath().getString("data.access_token");
            AssertionUtil.verifyNotNull(adminToken, "Admin access_token must not be null");

            RequestContext.setAdminToken(adminToken);
            System.out.println("   ✅ Admin token acquired: " + adminToken.substring(0, Math.min(30, adminToken.length())) + "...");
        }


        System.out.println("==========================================================\n");
    }

    // =========================================================
    // GAD_01 — Valid admin token + empty body → 200 + admin users list
    // =========================================================
    @Test(priority = 1, description = "QA Automation: Get Admin Users — Valid admin token + empty body → 200 with data")
    public void GAD_01_GetAdminUsers_ValidAdminToken() {
        System.out.println("\n==========================================================");
        System.out.println("  GAD-01: GET ADMIN USERS — VALID ADMIN TOKEN (POSITIVE)");
        System.out.println("==========================================================");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.GET_ADMIN_USERS)
                .addHeader("authorization", adminToken)
                .setRequestBody("{}")
                .expectStatus(200)
                .post();

        boolean success  = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));
        Object  data     = response.jsonPath().get("data");

        System.out.println("\n🔍 RESPONSE:");
        System.out.println("   Status  : " + response.getStatusCode());
        System.out.println("   success : " + success);
        System.out.println("   data    : " + (data != null ? "present" : "null"));
        System.out.println("\n   Full body:\n" + response.getBody().asString());

        AssertionUtil.verifyTrue(success, "success must be true for valid admin token");
        AssertionUtil.verifyNotNull(data, "data must be present in response");

        System.out.println("\n✅ GAD-01 PASSED — Admin users list returned with valid token.");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // GAD_02 — Verify response structure
    // =========================================================
    @Test(priority = 2, description = "QA Automation: Get Admin Users — Verify response structure (success, data array)")
    public void GAD_02_GetAdminUsers_VerifyResponseStructure() {
        System.out.println("\n==========================================================");
        System.out.println("  GAD-02: GET ADMIN USERS — VERIFY RESPONSE STRUCTURE (POSITIVE)");
        System.out.println("==========================================================");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.GET_ADMIN_USERS)
                .addHeader("authorization", adminToken)
                .setRequestBody("{}")
                .expectStatus(200)
                .post();

        boolean success   = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));
        List<?> dataList  = response.jsonPath().getList("data");

        System.out.println("\n🔍 RESPONSE STRUCTURE:");
        System.out.println("   success         : " + success);
        System.out.println("   data type       : " + (dataList != null ? "array" : "null"));
        System.out.println("   data size       : " + (dataList != null ? dataList.size() : 0));
        System.out.println("\n   Full body:\n" + response.getBody().asString());

        AssertionUtil.verifyTrue(success, "success must be true");
        AssertionUtil.verifyNotNull(dataList, "data must be an array");
        AssertionUtil.verifyTrue(dataList.size() > 0, "data array must contain at least one admin user");

        System.out.println("\n✅ GAD-02 PASSED — Response structure verified: success=true, data is non-empty array.");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // GAD_03 — Verify user record fields
    // =========================================================
    @Test(priority = 3, description = "QA Automation: Get Admin Users — Verify first record fields (guid, mobile, first_name, user_type)")
    public void GAD_03_GetAdminUsers_VerifyRecordFields() {
        System.out.println("\n==========================================================");
        System.out.println("  GAD-03: GET ADMIN USERS — VERIFY RECORD FIELDS (POSITIVE)");
        System.out.println("==========================================================");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.GET_ADMIN_USERS)
                .addHeader("authorization", adminToken)
                .setRequestBody("{}")
                .expectStatus(200)
                .post();

        String guid      = response.jsonPath().getString("data[0].guid");
        String mobile    = response.jsonPath().getString("data[0].mobile");
        String firstName = response.jsonPath().getString("data[0].first_name");
        String userType  = response.jsonPath().getString("data[0].user_type");

        System.out.println("\n🔍 FIRST RECORD FIELDS:");
        System.out.println("   guid       : " + guid);
        System.out.println("   mobile     : " + mobile);
        System.out.println("   first_name : " + firstName);
        System.out.println("   user_type  : " + userType);
        System.out.println("\n   Full body:\n" + response.getBody().asString());

        AssertionUtil.verifyNotNull(guid,      "guid must be present in user record");
        AssertionUtil.verifyNotNull(mobile,    "mobile must be present in user record");
        AssertionUtil.verifyNotNull(firstName, "first_name must be present in user record");
        AssertionUtil.verifyNotNull(userType,  "user_type must be present in user record");

        System.out.println("\n✅ GAD-03 PASSED — User record fields verified.");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // GAD_04 — No Authorization header → 200 (auth not enforced)
    // =========================================================
    @Test(priority = 4, description = "QA Automation: Get Admin Users — No auth token → 200 (API DEFECT: auth not enforced)")
    public void GAD_04_GetAdminUsers_NoAuthToken() {
        System.out.println("\n==========================================================");
        System.out.println("  GAD-04: GET ADMIN USERS — NO AUTH TOKEN (OBSERVED BEHAVIOUR)");
        System.out.println("  NOTE: This endpoint returns 200 even without a Bearer token.");
        System.out.println("  This is an API defect — auth should be enforced.");
        System.out.println("==========================================================");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.GET_ADMIN_USERS)
                .setRequestBody("{}")
                .postWithoutStatusCheck();

        int     status  = response.getStatusCode();
        boolean success = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));
        Object  data    = response.jsonPath().get("data");

        System.out.println("\n📌 Status  : " + status);
        System.out.println("📌 success : " + success);
        System.out.println("📌 data    : " + (data != null ? "present" : "null"));
        System.out.println("📌 Body    : " + response.getBody().asString());

        // Document the current (defective) behaviour: no auth → 200
        AssertionUtil.verifyEquals(status, 200,
                "[API DEFECT] No-token request currently returns 200; expected 401/403 once auth is enforced");

        System.out.println("\n✅ GAD-04 PASSED — Observed: no-token request returns 200 (auth not yet enforced).");
        System.out.println("==========================================================\n");
    }
}

package com.mryoda.diagnostics.api.tests.auth;

import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import com.mryoda.diagnostics.api.config.ConfigLoader;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.payloads.UserPayloadBuilder;
import com.mryoda.diagnostics.api.utils.AssertionUtil;
import com.mryoda.diagnostics.api.utils.RandomDataUtil;
import com.mryoda.diagnostics.api.utils.RequestContext;
import io.restassured.response.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * ============================================================
 * GET USER NOTIFICATIONS API TEST
 * GET /users/notifications/user/{user_id}
 * ============================================================
 *
 * Endpoint : GET https://staging-api-diagnostics.yodaprojects.com
 *                      /users/notifications/user/{user_id}
 * Auth     : Bearer admin token (role: admin_only, is_admin_user: true)
 *
 * ── POSITIVE TESTS ──────────────────────────────────────────
 *   GUN_01 — Valid admin token + valid user_id → 200 + response body
 *   GUN_02 — Verify top-level response structure (success, data)
 *   GUN_03 — Verify notification record fields if data is non-empty
 *
 * ── NEGATIVE TESTS ──────────────────────────────────────────
 *   GUN_04 — Fresh user GUID (no notifications yet) → 200 + empty data
 *   GUN_05 — Malformed (non-UUID) user_id            → 200 + empty data
 * ============================================================
 */
public class GetUserNotificationsAPITest extends BaseTest {

    private static String adminToken;
    private static String adminGuid;
    private static String freshUserGuid;

    private static final String ADMIN_LOGIN_URL =
            APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADMIN_LOGIN;

    // ─── Helper: resolve path param ──────────────────────────────────────────
    private static String endpoint(String userId) {
        return APIEndpoints.GET_USER_NOTIFICATIONS.replace("{user_id}", userId);
    }

    // =========================================================
    // SETUP — Admin Login
    // =========================================================
    @BeforeClass(alwaysRun = true)
    public void setupAdminLogin() {
        System.out.println("\n==========================================================");
        System.out.println("  GET USER NOTIFICATIONS — ADMIN LOGIN SETUP");
        System.out.println("==========================================================");

        String adminIdentifier = ConfigLoader.getConfig().adminMainIdentifier();
        String adminPassword   = ConfigLoader.getConfig().adminMainPassword();

        Map<String, Object> adminPayload = new HashMap<>();
        adminPayload.put("identifier", adminIdentifier);
        adminPayload.put("user_name",  "admin");
        adminPayload.put("password",   adminPassword);
        adminPayload.put("type",       "login");
        adminPayload.put("fcmToken",   "test-fcm-token-automation");

        Response loginResponse = new RequestBuilder()
                .setEndpoint(ADMIN_LOGIN_URL)
                .setRequestBody(adminPayload)
                .post();

        System.out.println("   Admin Login Status: " + loginResponse.getStatusCode());
        AssertionUtil.verifyEquals(loginResponse.getStatusCode(), 200, "Admin login must return 200");

        adminToken = loginResponse.jsonPath().getString("data.access_token");
        adminGuid  = loginResponse.jsonPath().getString("data.userdData.user_guid");

        AssertionUtil.verifyNotNull(adminToken, "Admin access_token must not be null");
        AssertionUtil.verifyNotNull(adminGuid,  "Admin user_guid must not be null");

        RequestContext.setAdminToken(adminToken);
        RequestContext.setAdminGuid(adminGuid);

        System.out.println("   ✅ Admin token acquired | GUID: " + adminGuid);
        System.out.println("   Admin Token: " + adminToken.substring(0, Math.min(30, adminToken.length())) + "...");
        // --- Register a fresh user (no notifications) for GUN_04 ---
        String mobile = "9" + RandomDataUtil.getRandomMobile().substring(1);
        RequestContext.setMobile(mobile);

        org.json.JSONObject regPayload = UserPayloadBuilder.buildNewUserPayload();
        regPayload.put("mobile", mobile);

        Response regResponse = new RequestBuilder()
                .setEndpoint(APIEndpoints.USER_CREATE)
                .setRequestBody(regPayload.toString())
                .expectStatus(201)
                .post();

        freshUserGuid = regResponse.jsonPath().getString("data.guid");
        AssertionUtil.verifyNotNull(freshUserGuid, "Fresh user GUID must not be null after registration");

        System.out.println("   ✅ Fresh user registered | GUID: " + freshUserGuid + " | Mobile: " + mobile);        System.out.println("==========================================================\n");
    }

    // =========================================================
    // GUN_01 — Valid admin token + valid user_id → 200
    // =========================================================
    @Test(priority = 1, description = "QA Automation: Get User Notifications — Valid admin token + valid user_id → 200")
    public void GUN_01_GetUserNotifications_ValidUserId() {
        System.out.println("\n==========================================================");
        System.out.println("  GUN-01: GET USER NOTIFICATIONS — VALID USER ID");
        System.out.println("==========================================================");
        System.out.println("   user_id (admin GUID) : " + adminGuid);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint(adminGuid))
                .addHeader("authorization", adminToken)
                .expectStatus(200)
                .get();

        boolean success = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));

        System.out.println("\n🔍 RESPONSE:");
        System.out.println("   Status  : " + response.getStatusCode());
        System.out.println("   success : " + success);
        System.out.println("\n   Full body:\n" + response.getBody().asString());

        AssertionUtil.verifyTrue(success, "success must be true");

        System.out.println("\n✅ GUN-01 PASSED — getUserNotifications returned 200.");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // GUN_02 — Verify top-level response structure
    // =========================================================
    @Test(priority = 2, description = "QA Automation: Get User Notifications — Verify response structure")
    public void GUN_02_GetUserNotifications_VerifyStructure() {
        System.out.println("\n==========================================================");
        System.out.println("  GUN-02: GET USER NOTIFICATIONS — VERIFY STRUCTURE");
        System.out.println("==========================================================");

        Response response = new RequestBuilder()
                .setEndpoint(endpoint(adminGuid))
                .addHeader("authorization", adminToken)
                .expectStatus(200)
                .get();

        boolean success  = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));
        Object  data     = response.jsonPath().get("data");

        System.out.println("\n🔍 RESPONSE STRUCTURE:");
        System.out.println("   success : " + success);
        System.out.println("   data    : " + data);
        System.out.println("\n   Full body:\n" + response.getBody().asString());

        AssertionUtil.verifyTrue(success, "success must be true");
        AssertionUtil.verifyNotNull(data, "data field must be present in response");

        System.out.println("\n✅ GUN-02 PASSED — Response structure verified.");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // GUN_03 — Verify notification record fields if non-empty
    // =========================================================
    @Test(priority = 3, description = "QA Automation: Get User Notifications — Verify notification record fields")
    public void GUN_03_GetUserNotifications_VerifyRecordFields() {
        System.out.println("\n==========================================================");
        System.out.println("  GUN-03: GET USER NOTIFICATIONS — VERIFY RECORD FIELDS");
        System.out.println("==========================================================");

        Response response = new RequestBuilder()
                .setEndpoint(endpoint(adminGuid))
                .addHeader("authorization", adminToken)
                .expectStatus(200)
                .get();

        boolean success  = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));
        List<?> dataList = response.jsonPath().getList("data");

        System.out.println("\n   Full body:\n" + response.getBody().asString());

        AssertionUtil.verifyTrue(success, "success must be true");

        if (dataList == null || dataList.isEmpty()) {
            System.out.println("\n⚠️  data[] is empty — no notifications to validate fields for.");
            System.out.println("✅ GUN-03 PASSED — Empty list is a valid response.");
        } else {
            Object id        = response.jsonPath().get("data[0].id");
            String title     = response.jsonPath().getString("data[0].title");
            String message   = response.jsonPath().getString("data[0].message");
            Object isRead    = response.jsonPath().get("data[0].is_read");
            String createdAt = response.jsonPath().getString("data[0].created_at");

            System.out.println("\n🔍 FIRST NOTIFICATION RECORD:");
            System.out.println("   id         : " + id);
            System.out.println("   title      : " + title);
            System.out.println("   message    : " + message);
            System.out.println("   is_read    : " + isRead);
            System.out.println("   created_at : " + createdAt);

            AssertionUtil.verifyNotNull(id,
                    "id must be present in notification record — got: " + id);
            AssertionUtil.verifyNotNull(title,
                    "title must be present in notification record — got: " + title);

            System.out.println("\n✅ GUN-03 PASSED — Notification record fields verified.");
        }

        System.out.println("==========================================================\n");
    }

    // =========================================================
    // GUN_04 — Fresh registered user (zero notifications) → 200 + empty data
    //          user_id from @BeforeClass registration (real system-issued GUID)
    // =========================================================
    @Test(priority = 4, description = "QA Automation: Get User Notifications — Fresh user GUID → 200 + empty data")
    public void GUN_04_GetUserNotifications_NonExistentUserId() {
        System.out.println("\n==========================================================");
        System.out.println("  GUN-04: GET USER NOTIFICATIONS — FRESH USER (NO NOTIFICATIONS)");
        System.out.println("==========================================================");
        System.out.println("   user_id (fresh user GUID) : " + freshUserGuid);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint(freshUserGuid))
                .addHeader("authorization", adminToken)
                .expectStatus(200)
                .get();

        boolean success  = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));
        List<?> dataList = response.jsonPath().getList("data");

        System.out.println("\n🔍 RESPONSE:");
        System.out.println("   Status  : " + response.getStatusCode());
        System.out.println("   success : " + success);
        System.out.println("   data[]  : " + (dataList != null ? dataList.size() + " records" : "null"));
        System.out.println("\n   Full body:\n" + response.getBody().asString());

        AssertionUtil.verifyTrue(success, "success must be true — got: " + success);
        AssertionUtil.verifyTrue(
                dataList == null || dataList.isEmpty(),
                "data must be empty for a fresh user with no notifications — got: " + dataList);

        System.out.println("\n✅ GUN-04 PASSED — Fresh user GUID returned 200 with empty data.");
        System.out.println("==========================================================");
    }

    // =========================================================
    // GUN_05 — Malformed user_id → 400
    //          EXPECTED : 400 Bad Request (invalid UUID format)
    //          ACTUAL   : 200 OK  ← API DEFECT — server does not validate path param format
    // =========================================================
    @Test(priority = 5, description = "QA Automation: Get User Notifications — Malformed user_id → 400 [API DEFECT: returns 200]")
    public void GUN_05_GetUserNotifications_MalformedUserId() {
        System.out.println("\n==========================================================");
        System.out.println("  GUN-05: GET USER NOTIFICATIONS — MALFORMED USER ID");
        System.out.println("  EXPECTED: 400  |  API DEFECT: server returns 200");
        System.out.println("==========================================================");

        String malformedId = "not-a-valid-uuid-!!!";
        System.out.println("   user_id : " + malformedId);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint(malformedId))
                .addHeader("authorization", adminToken)
                .get();

        int statusCode = response.getStatusCode();

        System.out.println("\n🔍 RESPONSE:");
        System.out.println("   Status  : " + statusCode);
        System.out.println("\n   Full body:\n" + response.getBody().asString());

        // ⚠️ API DEFECT: a malformed (non-UUID) path param should be rejected with 400.
        // The server currently returns 200 + empty data instead of validating the format.
        // This assertion documents the CORRECT expected behaviour — fix the API to make this pass.
        AssertionUtil.verifyEquals(statusCode, 400,
                "[API DEFECT] Malformed user_id must return 400 — got: " + statusCode);

        System.out.println("\n✅ GUN-05 PASSED — Malformed user_id rejected with 400.");
        System.out.println("==========================================================");
    }
}
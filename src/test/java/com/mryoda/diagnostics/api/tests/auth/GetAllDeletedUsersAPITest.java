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
 * GET ALL DELETED USERS API TEST
 * GET /users/getAllDeletedUsers
 * ============================================================
 *
 * Endpoint : GET https://staging-api-diagnostics.yodaprojects.com
 *                      /users/getAllDeletedUsers
 * Auth     : Bearer admin token (role: admin_only, is_admin_user: true)
 *
 * ── POSITIVE TESTS ──────────────────────────────────────────
 *   GADU_01 — Valid admin token → 200 + array of deleted users
 *   GADU_02 — Verify response structure (success, data array)
 *   GADU_03 — Verify data record fields (guid, first_name, last_name, mobile, etc.)
 * ============================================================
 */
public class GetAllDeletedUsersAPITest extends BaseTest {

    private static String adminToken;

    private static final String ADMIN_LOGIN_URL =
            APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADMIN_LOGIN;

    // =========================================================
    // SETUP — Admin Login
    // =========================================================
    @BeforeClass(alwaysRun = true)
    public void setupAdminLogin() {
        System.out.println("\n==========================================================");
        System.out.println("  GET ALL DELETED USERS — ADMIN LOGIN SETUP");
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
        String adminGuid = loginResponse.jsonPath().getString("data.userdData.user_guid");

        AssertionUtil.verifyNotNull(adminToken, "Admin access_token must not be null");

        RequestContext.setAdminToken(adminToken);
        RequestContext.setAdminGuid(adminGuid);

        System.out.println("   ✅ Admin token acquired | GUID: " + adminGuid);
        System.out.println("   Admin Token: " + adminToken.substring(0, Math.min(30, adminToken.length())) + "...");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // GADU_01 — Valid admin token → 200 + data array
    // =========================================================
    @Test(priority = 1, description = "QA Automation: Get All Deleted Users — Valid admin token → 200")
    public void GADU_01_GetAllDeletedUsers_ValidAdminToken() {
        System.out.println("\n==========================================================");
        System.out.println("  GADU-01: GET ALL DELETED USERS — VALID ADMIN TOKEN");
        System.out.println("==========================================================");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.GET_ALL_DELETED_USERS)
                .addHeader("authorization", adminToken)
                .addHeader("Content-Type", "text/plain")
                .setRequestBody("{}")
                .expectStatus(200)
                .post();

        boolean success  = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));
        List<?> dataList = response.jsonPath().getList("data");
        Object  total    = response.jsonPath().get("total");

        System.out.println("\n🔍 RESPONSE:");
        System.out.println("   Status  : " + response.getStatusCode());
        System.out.println("   success : " + success);
        System.out.println("   total   : " + total);
        System.out.println("   data[]  : " + (dataList != null ? dataList.size() + " records in page" : "null"));
        System.out.println("\n   Full body:\n" + response.getBody().asString());

        AssertionUtil.verifyTrue(success, "success must be true");
        AssertionUtil.verifyNotNull(dataList, "data array must be present in response");
        AssertionUtil.verifyNotNull(total, "total must be present in response");

        System.out.println("\n✅ GADU-01 PASSED — getAllDeletedUsers returned 200 with data array.");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // GADU_02 — Verify response structure
    // =========================================================
    @Test(priority = 2, description = "QA Automation: Get All Deleted Users — Verify response structure")
    public void GADU_02_GetAllDeletedUsers_VerifyStructure() {
        System.out.println("\n==========================================================");
        System.out.println("  GADU-02: GET ALL DELETED USERS — VERIFY STRUCTURE");
        System.out.println("==========================================================");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.GET_ALL_DELETED_USERS)
                .addHeader("authorization", adminToken)
                .addHeader("Content-Type", "text/plain")
                .setRequestBody("{}")
                .expectStatus(200)
                .post();

        boolean success     = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));
        Object  total       = response.jsonPath().get("total");
        Object  page        = response.jsonPath().get("page");
        Object  limit       = response.jsonPath().get("limit");
        Object  totalPages  = response.jsonPath().get("total_pages");
        List<?> dataList    = response.jsonPath().getList("data");

        System.out.println("\n🔍 RESPONSE STRUCTURE:");
        System.out.println("   success     : " + success);
        System.out.println("   total       : " + total);
        System.out.println("   page        : " + page);
        System.out.println("   limit       : " + limit);
        System.out.println("   total_pages : " + totalPages);
        System.out.println("   data[] size : " + (dataList != null ? dataList.size() : "null"));
        System.out.println("\n   Full body:\n" + response.getBody().asString());

        AssertionUtil.verifyTrue(success, "success must be true");
        AssertionUtil.verifyNotNull(dataList,   "data array must be present");
        AssertionUtil.verifyNotNull(total,       "total must be present in response — got: " + total);
        AssertionUtil.verifyNotNull(page,        "page must be present in response — got: " + page);
        AssertionUtil.verifyNotNull(limit,       "limit must be present in response — got: " + limit);
        AssertionUtil.verifyNotNull(totalPages,  "total_pages must be present in response — got: " + totalPages);

        System.out.println("\n✅ GADU-02 PASSED — All pagination fields verified.");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // GADU_03 — Verify data record fields when list is non-empty
    // =========================================================
    @Test(priority = 3, description = "QA Automation: Get All Deleted Users — Verify data record fields")
    public void GADU_03_GetAllDeletedUsers_VerifyRecordFields() {
        System.out.println("\n==========================================================");
        System.out.println("  GADU-03: GET ALL DELETED USERS — VERIFY RECORD FIELDS");
        System.out.println("==========================================================");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.GET_ALL_DELETED_USERS)
                .addHeader("authorization", adminToken)
                .addHeader("Content-Type", "text/plain")
                .setRequestBody("{}")
                .expectStatus(200)
                .post();

        boolean success  = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));
        List<?> dataList = response.jsonPath().getList("data");

        System.out.println("\n   Full body:\n" + response.getBody().asString());

        AssertionUtil.verifyTrue(success, "success must be true");

        if (dataList == null || dataList.isEmpty()) {
            System.out.println("\n⚠️  data[] is empty — no deleted users to validate fields for.");
            System.out.println("✅ GADU-03 PASSED — Empty list is a valid response.");
        } else {
            Object id        = response.jsonPath().get("data[0].id");
            String firstName = response.jsonPath().getString("data[0].first_name");
            String lastName  = response.jsonPath().getString("data[0].last_name");
            String mobile    = response.jsonPath().getString("data[0].mobile");
            Object status    = response.jsonPath().get("data[0].status");

            System.out.println("\n🔍 FIRST RECORD FIELDS:");
            System.out.println("   id         : " + id);
            System.out.println("   first_name : " + firstName);
            System.out.println("   last_name  : " + lastName);
            System.out.println("   mobile     : " + mobile);
            System.out.println("   status     : " + status);

            AssertionUtil.verifyNotNull(id,
                    "id must be present in deleted user record — got: " + id);
            AssertionUtil.verifyNotNull(firstName,
                    "first_name must be present in deleted user record — got: " + firstName);
            AssertionUtil.verifyNotNull(lastName,
                    "last_name must be present in deleted user record — got: " + lastName);
            AssertionUtil.verifyNotNull(mobile,
                    "mobile must be present in deleted user record — got: " + mobile);

            System.out.println("\n✅ GADU-03 PASSED — Deleted user record fields verified.");
        }

        System.out.println("==========================================================\n");
    }
}

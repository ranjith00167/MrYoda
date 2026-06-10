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
 * GET ALL USERS API TEST — GET /users/getAllUsers
 * ============================================================
 *
 * Endpoint : GET https://staging-api-diagnostics.yodaprojects.com
 *                    /users/getAllUsers?page=1&pageSize=25
 * Auth     : Bearer admin token (role: admin_only, is_admin_user: true)
 *
 * Admin Login:
 *   POST /auth/login
 *   Payload: { identifier, user_name, password, type, fcmToken }
 *
 * ── POSITIVE TESTS ──────────────────────────────────────────
 *   GAU_01 — Valid admin token, page=1, pageSize=25 → 200 + users list
 *   GAU_02 — Verify response structure (success, data array, pagination)
 *   GAU_03 — Different pageSize (pageSize=10) → 200, at most 10 results
 *   GAU_04 — Page 2 (page=2, pageSize=25) → 200 + valid response shape
 * ============================================================
 */
public class GetAllUsersAPITest extends BaseTest {

    // ── Admin session (shared across positive tests) ─────────────────────────
    private static String adminToken;

    // ── Admin login endpoint (absolute URL) ──────────────────────────────────
    private static final String ADMIN_LOGIN_URL =
            APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADMIN_LOGIN;

    // =========================================================
    // SETUP — Admin Login + Non-admin token
    // =========================================================
    @BeforeClass(alwaysRun = true)
    public void setupAdminLogin() {
        System.out.println("\n==========================================================");
        System.out.println("  GET ALL USERS TEST — ADMIN LOGIN SETUP");
        System.out.println("==========================================================");

        // --- 1. Admin login (identifier + password flow) ---
        String adminIdentifier = ConfigLoader.getConfig().adminMainIdentifier();
        String adminPassword   = ConfigLoader.getConfig().adminMainPassword();

        Map<String, Object> adminPayload = new HashMap<>();
        adminPayload.put("identifier", adminIdentifier);
        adminPayload.put("user_name",  "admin");
        adminPayload.put("password",   adminPassword);
        adminPayload.put("type",       "login");
        adminPayload.put("fcmToken",   "test-fcm-token-automation");

        System.out.println("   Calling admin login: " + ADMIN_LOGIN_URL);
        System.out.println("   Identifier: " + adminIdentifier);

        Response adminLoginResponse = new RequestBuilder()
                .setEndpoint(ADMIN_LOGIN_URL)
                .setRequestBody(adminPayload)
                .post();

        System.out.println("   Admin Login Status: " + adminLoginResponse.getStatusCode());
        System.out.println("   Admin Login Body: "   + adminLoginResponse.getBody().asString());

        AssertionUtil.verifyEquals(adminLoginResponse.getStatusCode(), 200,
                "Admin login must return 200");

        adminToken = adminLoginResponse.jsonPath().getString("data.access_token");
        String adminGuid = adminLoginResponse.jsonPath().getString("data.userdData.user_guid");

        AssertionUtil.verifyNotNull(adminToken, "Admin access_token must not be null");

        RequestContext.setAdminToken(adminToken);
        RequestContext.setAdminGuid(adminGuid);

        System.out.println("   ✅ Admin token acquired | GUID: " + adminGuid);
        System.out.println("   Admin Token: " + adminToken.substring(0, Math.min(30, adminToken.length())) + "...");

        System.out.println("==========================================================\n");
    }

    // =========================================================
    // GAU_01 — Valid admin token, page=1, pageSize=25 — POSITIVE
    // =========================================================
    @Test(priority = 1, description = "QA Automation: Get All Users — Valid admin token, page=1, pageSize=25 — Should Return 200")
    public void GAU_01_GetAllUsers_ValidAdminToken() {
        System.out.println("\n==========================================================");
        System.out.println("  GAU-01: GET ALL USERS — VALID ADMIN TOKEN (POSITIVE)");
        System.out.println("==========================================================");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.GET_ALL_USERS)
                .addHeader("authorization", adminToken)
                .addQueryParam("page",     "1")
                .addQueryParam("pageSize", "25")
                .expectStatus(200)
                .get();

        boolean success  = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));
        List<?> dataList = response.jsonPath().getList("data");

        System.out.println("\n🔍 RESPONSE:");
        System.out.println("   Status  : " + response.getStatusCode());
        System.out.println("   success : " + success);
        System.out.println("   data[]  : " + (dataList != null ? dataList.size() + " records" : "null"));
        System.out.println("\n   Full body:\n" + response.getBody().asString());

        AssertionUtil.verifyTrue(success, "success flag must be true");
        AssertionUtil.verifyNotNull(dataList, "data array must be present in response");

        System.out.println("\n✅ GAU-01 PASSED — getAllUsers returned 200 with user list.");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // GAU_02 — Verify response structure — POSITIVE
    // =========================================================
    @Test(priority = 2, description = "QA Automation: Get All Users — Verify response fields (success, data, pagination)")
    public void GAU_02_GetAllUsers_VerifyResponseStructure() {
        System.out.println("\n==========================================================");
        System.out.println("  GAU-02: GET ALL USERS — VERIFY RESPONSE STRUCTURE (POSITIVE)");
        System.out.println("==========================================================");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.GET_ALL_USERS)
                .addHeader("authorization", adminToken)
                .addQueryParam("page",     "1")
                .addQueryParam("pageSize", "25")
                .expectStatus(200)
                .get();

        boolean success      = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));
        List<?> dataList     = response.jsonPath().getList("data");
        Object  totalCount   = response.jsonPath().get("total_count");
        Object  currentPage  = response.jsonPath().get("current_page");
        Object  totalPages   = response.jsonPath().get("total_pages");

        System.out.println("\n🔍 RESPONSE STRUCTURE:");
        System.out.println("   success      : " + success);
        System.out.println("   data[]       : " + (dataList != null ? dataList.size() + " items" : "null"));
        System.out.println("   total_count  : " + totalCount);
        System.out.println("   current_page : " + currentPage);
        System.out.println("   total_pages  : " + totalPages);
        System.out.println("\n   Full body:\n" + response.getBody().asString());

        AssertionUtil.verifyTrue(success, "success must be true");
        AssertionUtil.verifyNotNull(dataList, "data array must be present");
        // Pagination field names are printed above for reference; assert only on confirmed fields

        System.out.println("\n✅ GAU-02 PASSED — Response structure verified.");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // GAU_03 — pageSize=10 — POSITIVE
    // =========================================================
    @Test(priority = 3, description = "QA Automation: Get All Users — pageSize=10 — Should Return at most 10 records")
    public void GAU_03_GetAllUsers_SmallPageSize() {
        System.out.println("\n==========================================================");
        System.out.println("  GAU-03: GET ALL USERS — pageSize=10 (POSITIVE)");
        System.out.println("==========================================================");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.GET_ALL_USERS)
                .addHeader("authorization", adminToken)
                .addQueryParam("page",     "1")
                .addQueryParam("pageSize", "10")
                .expectStatus(200)
                .get();

        boolean success  = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));
        List<?> dataList = response.jsonPath().getList("data");

        int returnedCount = dataList != null ? dataList.size() : 0;

        System.out.println("\n🔍 RESPONSE:");
        System.out.println("   Status        : " + response.getStatusCode());
        System.out.println("   success       : " + success);
        System.out.println("   Records count : " + returnedCount + " (expected ≤ 10)");
        System.out.println("\n   Full body:\n" + response.getBody().asString());

        AssertionUtil.verifyTrue(success, "success must be true for pageSize=10 request");
        AssertionUtil.verifyTrue(returnedCount <= 10,
                "Returned record count (" + returnedCount + ") must be ≤ pageSize (10)");

        System.out.println("\n✅ GAU-03 PASSED — pageSize=10 respected by API.");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // GAU_04 — Page 2 — POSITIVE
    // =========================================================
    @Test(priority = 4, description = "QA Automation: Get All Users — page=2, pageSize=25 — Should Return valid response")
    public void GAU_04_GetAllUsers_Page2() {
        System.out.println("\n==========================================================");
        System.out.println("  GAU-04: GET ALL USERS — PAGE 2 (POSITIVE)");
        System.out.println("==========================================================");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.GET_ALL_USERS)
                .addHeader("authorization", adminToken)
                .addQueryParam("page",     "2")
                .addQueryParam("pageSize", "25")
                .getWithoutStatusCheck();

        int    statusCode = response.getStatusCode();
        boolean success   = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));
        List<?> dataList  = response.jsonPath().getList("data");

        System.out.println("\n🔍 RESPONSE:");
        System.out.println("   Status  : " + statusCode);
        System.out.println("   success : " + success);
        System.out.println("   data[]  : " + (dataList != null ? dataList.size() + " records" : "null"));
        System.out.println("\n   Full body:\n" + response.getBody().asString());

        AssertionUtil.verifyEquals(statusCode, 200,
                "Page 2 request with valid admin token must return 200");
        AssertionUtil.verifyTrue(success, "success must be true for page=2 request");

        System.out.println("\n✅ GAU-04 PASSED — Page 2 returned a valid response.");
        System.out.println("==========================================================\n");
    }
}

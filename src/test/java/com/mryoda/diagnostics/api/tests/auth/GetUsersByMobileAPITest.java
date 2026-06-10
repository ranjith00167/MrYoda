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
import java.util.List;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * ============================================================
 * GET USERS BY MOBILE API TEST
 * GET /users/getUsersByMobile/{mobile}
 * ============================================================
 *
 * Endpoint : GET https://staging-api-diagnostics.yodaprojects.com
 *                      /users/getUsersByMobile/{mobile}
 * Auth     : Bearer token (member credentials)
 *
 * Flow:
 *   Step 1 — Login with member credentials  → obtain member token
 *   Step 2 — Generate random mobile + call POST /users/addUser
 *             to create a fresh new user
 *   Step 3 — Call GET /users/getUsersByMobile/{random_mobile}
 *             with the member token and assert results
 *
 * ── POSITIVE TESTS ──────────────────────────────────────────
 *   GUM_01 — Member token + newly created user's mobile → 200 + user data
 *   GUM_02 — Verify all response fields (guid, mobile, first_name, last_name)
 *
 * ── NEGATIVE — Path parameter validation ────────────────────
 *   GUM_03 — Non-existent mobile (no account)    → 200 + empty data
 *   GUM_04 — Invalid mobile format               → 200 + empty data (no format validation)
 * ============================================================
 */
public class GetUsersByMobileAPITest extends BaseTest {

    // ── Member session (obtained in @BeforeClass) ─────────────────────────────
    private static String memberToken;
    private static String memberMobile;

    // ── Newly created random user (created in @BeforeClass) ──────────────────
    private static String newUserMobile;
    private static String newUserGuid;

    // ── Helper: resolve endpoint ──────────────────────────────────────────────
    private static String endpoint(String mobile) {
        return APIEndpoints.GET_USERS_BY_MOBILE.replace("{mobile}", mobile);
    }

    // =========================================================
    // SETUP — Member login + create fresh random user
    // =========================================================
    @BeforeClass(alwaysRun = true)
    public void setupSessions() {
        System.out.println("\n==========================================================");
        System.out.println("  GET USERS BY MOBILE — SETUP");
        System.out.println("==========================================================");

        // --- Step 1: Login with member credentials ---
        memberMobile = ConfigLoader.getConfig().memberMobile();
        RequestContext.setMobile(memberMobile);
        memberToken = TokenManager.generateToken(memberMobile, TokenManager.MEMBER);
        System.out.println("   ✅ Member logged in — Mobile: " + memberMobile);

        // --- Step 2: Generate random mobile + create new user ---
        newUserMobile = "9" + RandomDataUtil.getRandomMobile().substring(1);
        RequestContext.setMobile(newUserMobile);

        org.json.JSONObject regPayload = UserPayloadBuilder.buildNewUserPayload();
        regPayload.put("mobile", newUserMobile);

        Response regResponse = new RequestBuilder()
                .setEndpoint(APIEndpoints.USER_CREATE)
                .setRequestBody(regPayload.toString())
                .expectStatus(201)
                .post();

        newUserGuid = regResponse.jsonPath().getString("data.guid");
        System.out.println("   ✅ New user created — Mobile: " + newUserMobile + " | GUID: " + newUserGuid);

        System.out.println("==========================================================\n");
    }

    // =========================================================
    // GUM_01 — Member token + new user's random mobile — POSITIVE
    // =========================================================
    @Test(priority = 1, description = "QA Automation: Get Users By Mobile — Member token + new user mobile → 200 with user data")
    public void GUM_01_GetUsersByMobile_ValidMobile() {
        System.out.println("\n==========================================================");
        System.out.println("  GUM-01: GET USERS BY MOBILE — VALID MOBILE (POSITIVE)");
        System.out.println("==========================================================");
        System.out.println("   Mobile : " + newUserMobile);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint(newUserMobile))
                .addHeader("authorization", memberToken)
                .expectStatus(200)
                .get();

        boolean success       = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));
        String  returnedMobile = response.jsonPath().getString("data[0].mobile");

        System.out.println("\n🔍 RESPONSE:");
        System.out.println("   Status  : " + response.getStatusCode());
        System.out.println("   success : " + success);
        System.out.println("   mobile  : " + returnedMobile);
        System.out.println("\n   Full body:\n" + response.getBody().asString());

        AssertionUtil.verifyTrue(success, "success must be true for a valid mobile lookup");
        AssertionUtil.verifyEquals(returnedMobile, newUserMobile,
                "Returned mobile must match the queried mobile");

        System.out.println("\n✅ GUM-01 PASSED — User data returned for the newly created mobile.");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // GUM_02 — Verify all response fields — POSITIVE
    // =========================================================
    @Test(priority = 2, description = "QA Automation: Get Users By Mobile — Verify all response fields are present and correct")
    public void GUM_02_GetUsersByMobile_VerifyResponseFields() {
        System.out.println("\n==========================================================");
        System.out.println("  GUM-02: GET USERS BY MOBILE — VERIFY RESPONSE FIELDS (POSITIVE)");
        System.out.println("==========================================================");
        System.out.println("   Mobile : " + newUserMobile);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint(newUserMobile))
                .addHeader("authorization", memberToken)
                .expectStatus(200)
                .get();

        boolean success    = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));
        String  guid       = response.jsonPath().getString("data[0].guid");
        String  mobile     = response.jsonPath().getString("data[0].mobile");
        String  firstName  = response.jsonPath().getString("data[0].first_name");
        String  lastName   = response.jsonPath().getString("data[0].last_name");

        System.out.println("\n🔍 RESPONSE FIELDS:");
        System.out.println("   success    : " + success);
        System.out.println("   guid       : " + guid);
        System.out.println("   mobile     : " + mobile);
        System.out.println("   first_name : " + firstName);
        System.out.println("   last_name  : " + lastName);
        System.out.println("\n   Full body:\n" + response.getBody().asString());

        AssertionUtil.verifyTrue(success, "success must be true");
        AssertionUtil.verifyEquals(guid, newUserGuid,
                "Response GUID must match the GUID from user creation");
        AssertionUtil.verifyEquals(mobile, newUserMobile,
                "Response mobile must match the queried mobile");
        AssertionUtil.verifyNotNull(firstName, "first_name must be present in response");
        AssertionUtil.verifyNotNull(lastName,  "last_name must be present in response");

        System.out.println("\n✅ GUM-02 PASSED — All key response fields verified.");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // GUM_03 — No Authorization Header — NEGATIVE
    // =========================================================
    // GUM_03 — Non-Existent Mobile — NEGATIVE
    // =========================================================
    @Test(priority = 3, description = "QA Automation: Get Users By Mobile — Non-existent mobile → 200 + empty data")
    public void GUM_03_GetUsersByMobile_NonExistentMobile() {
        System.out.println("\n==========================================================");
        System.out.println("  GUM-03: GET USERS BY MOBILE — NON-EXISTENT MOBILE (NEGATIVE)");
        System.out.println("==========================================================");

        // Generate a random mobile that has not been registered
        String ghostMobile = "6" + RandomDataUtil.getRandomMobile().substring(1);
        System.out.println("   Mobile (ghost) : " + ghostMobile);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint(ghostMobile))
                .addHeader("authorization", memberToken)
                .getWithoutStatusCheck();

        int    status   = response.getStatusCode();
        List<?> dataList = response.jsonPath().getList("data");
        boolean success  = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));

        System.out.println("\n📌 Status  : " + status);
        System.out.println("📌 success : " + success);
        System.out.println("📌 data    : " + dataList);
        System.out.println("📌 Body    : " + response.getBody().asString());

        // API returns 200 with an empty array when mobile is not found
        AssertionUtil.verifyEquals(status, 200,
                "Non-existent mobile should return 200 with empty data; got: " + status);
        AssertionUtil.verifyTrue(dataList == null || dataList.isEmpty(),
                "data must be null or empty for an unregistered mobile; got: " + dataList);

        System.out.println("\n✅ GUM-03 PASSED — Non-existent mobile returns 200 with empty data.");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // GUM_04 — Invalid Mobile Format — NEGATIVE
    // =========================================================
    @Test(priority = 4, description = "QA Automation: Get Users By Mobile — Invalid mobile format → 200 + empty data (no format validation)")
    public void GUM_04_GetUsersByMobile_InvalidMobileFormat() {
        System.out.println("\n==========================================================");
        System.out.println("  GUM-04: GET USERS BY MOBILE — INVALID FORMAT (NEGATIVE)");
        System.out.println("==========================================================");

        String invalidMobile = "ABCD-INVALID";
        System.out.println("   Mobile : " + invalidMobile);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint(invalidMobile))
                .addHeader("authorization", memberToken)
                .getWithoutStatusCheck();

        int status = response.getStatusCode();
        System.out.println("\n📌 Status : " + status);
        System.out.println("📌 Body   : " + response.getBody().asString());

        // Any non-2xx response confirms the server rejected the invalid format
        AssertionUtil.verifyTrue(status >= 400,
                "Invalid mobile format must return a non-2xx status; got: " + status);

        System.out.println("\n✅ GUM-04 PASSED — Invalid mobile format correctly rejected.");
        System.out.println("==========================================================\n");
    }
}

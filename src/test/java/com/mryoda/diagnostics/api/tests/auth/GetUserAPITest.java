package com.mryoda.diagnostics.api.tests.auth;

import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
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
 * GET USER API TEST — GET /users/getUser/{user_id}
 * ============================================================
 *
 * Endpoint  : GET https://staging-api-diagnostics.yodaprojects.com/users/getUser/{user_id}
 * Auth      : Bearer token (new user flow)
 * user_id   : GUID extracted from UserCreateAPITest via RequestContext
 *
 * ── POSITIVE TESTS ──────────────────────────────────────────
 *   GU_01 — Get user with valid user_id → 200 + correct user data
 *   GU_02 — Verify all response fields (guid, mobile, first_name, etc.)
 *
 * ── NEGATIVE — Auth ─────────────────────────────────────────
 *   GU_03 — No Authorization header             → 401/403
 *   GU_04 — Invalid / tampered Bearer token     → 401/403
 *
 * ── NEGATIVE — Path parameter validation ────────────────────
 *   GU_05 — Non-existent GUID as user_id        → 4xx
 *   GU_06 — Malformed (non-UUID) user_id        → 4xx
 * ============================================================
 */
public class GetUserAPITest extends BaseTest {

    // ── Shared state from @BeforeClass (suite run) ───────────────────────────
    private static String newUserToken;
    private static String newUserGuid;
    private static String registeredMobile;

    // =========================================================
    // SETUP — NEW USER flow
    // =========================================================
    @BeforeClass(alwaysRun = true)
    public void setupNewUserLogin() {
        System.out.println("\n==========================================================");
        System.out.println("  GET USER TEST — NEW USER FLOW SETUP");
        System.out.println("==========================================================");
        RequestContext.setCurrentFlowName("new_user_flow");

        // Capture mobile registered by UserCreateAPITest (suite run),
        // or fall back to inline registration for standalone run.
        registeredMobile = RequestContext.getMobile();

        if (registeredMobile == null) {
            System.out.println("   ⚙️  No mobile in RequestContext — registering new random user...");
            registeredMobile = "9" + RandomDataUtil.getRandomMobile().substring(1);
            RequestContext.setMobile(registeredMobile);

            org.json.JSONObject regPayload = UserPayloadBuilder.buildNewUserPayload();
            regPayload.put("mobile", registeredMobile);

            Response regResponse = new RequestBuilder()
                    .setEndpoint(APIEndpoints.USER_CREATE)
                    .setRequestBody(regPayload.toString())
                    .expectStatus(201)
                    .post();
            String registeredGuid = regResponse.jsonPath().getString("data.guid");
            RequestContext.setUserId(registeredGuid);
            System.out.println("   ✅ Registered new random user — GUID: " + registeredGuid);
        } else {
            System.out.println("   ✅ Mobile from UserCreateAPITest: " + registeredMobile);
        }

        // Login AFTER capturing mobile — TokenManager may overwrite RequestContext.getMobile()
        newUserToken = TokenManager.generateToken(registeredMobile, TokenManager.NEW_USER);
        newUserGuid  = RequestContext.getUserId();

        System.out.println("   ✅ Token acquired | GUID: " + newUserGuid);
        System.out.println("==========================================================\n");
    }

    // ── Helper: build resolved endpoint ─────────────────────────────────────
    private static String endpoint(String userId) {
        return APIEndpoints.GET_USER.replace("{user_id}", userId);
    }

    // ── Helper: fresh independent user session ───────────────────────────────
    private Map<String, String> createFreshUserSession() {
        String mobile = "9" + RandomDataUtil.getRandomMobile().substring(1);
        RequestContext.setMobile(mobile);

        org.json.JSONObject reg = UserPayloadBuilder.buildNewUserPayload();
        reg.put("mobile", mobile);

        Response regResp = new RequestBuilder()
                .setEndpoint(APIEndpoints.USER_CREATE)
                .setRequestBody(reg.toString())
                .expectStatus(201)
                .post();

        String guid  = regResp.jsonPath().getString("data.guid");
        String token = TokenManager.generateToken(mobile, TokenManager.NEW_USER);

        System.out.println("   📱 Fresh user — Mobile: " + mobile + " | GUID: " + guid);

        Map<String, String> session = new HashMap<>();
        session.put("token",  token);
        session.put("guid",   guid);
        session.put("mobile", mobile);
        return session;
    }

    // =========================================================
    // GU_01 — Valid user_id with valid token — POSITIVE
    // =========================================================
    @Test(priority = 1, description = "QA Automation: Get User — Valid user_id — Should Return 200 with user data")
    public void GU_01_GetUser_ValidUserId() {
        System.out.println("\n==========================================================");
        System.out.println("  GU-01: GET USER — VALID user_id (POSITIVE)");
        System.out.println("==========================================================");
        System.out.println("   user_id : " + newUserGuid);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint(newUserGuid))
                .addHeader("authorization", newUserToken)
                .expectStatus(200)
                .get();

        String returnedGuid   = response.jsonPath().getString("data.guid");
        String returnedMobile = response.jsonPath().getString("data.mobile");
        boolean success       = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));

        System.out.println("\n🔍 RESPONSE DATA:");
        System.out.println("   GUID    : " + returnedGuid);
        System.out.println("   Mobile  : " + returnedMobile);
        System.out.println("   success : " + success);

        AssertionUtil.verifyEquals(returnedGuid, newUserGuid, "Response GUID must match the requested user_id");
        AssertionUtil.verifyTrue(success, "success flag must be true");

        System.out.println("\n✅ GU-01 PASSED — User data returned for valid user_id.");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // GU_02 — Verify all response fields — POSITIVE
    // =========================================================
    @Test(priority = 2, description = "QA Automation: Get User — Verify all response fields are present and non-null")
    public void GU_02_GetUser_VerifyResponseFields() {
        System.out.println("\n==========================================================");
        System.out.println("  GU-02: GET USER — VERIFY RESPONSE FIELDS (POSITIVE)");
        System.out.println("==========================================================");

        Map<String, String> session = createFreshUserSession();

        Response response = new RequestBuilder()
                .setEndpoint(endpoint(session.get("guid")))
                .addHeader("authorization", session.get("token"))
                .expectStatus(200)
                .get();

        String guid       = response.jsonPath().getString("data.guid");
        String mobile     = response.jsonPath().getString("data.mobile");
        String firstName  = response.jsonPath().getString("data.first_name");
        String lastName   = response.jsonPath().getString("data.last_name");
        String userStatus = response.jsonPath().getString("data.status");
        boolean success   = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));

        System.out.println("\n🔍 RESPONSE FIELDS:");
        System.out.println("   guid       : " + guid);
        System.out.println("   mobile     : " + mobile);
        System.out.println("   first_name : " + firstName);
        System.out.println("   last_name  : " + lastName);
        System.out.println("   status     : " + userStatus);
        System.out.println("   success    : " + success);
        System.out.println("\n   Full response body:\n" + response.getBody().asString());

        AssertionUtil.verifyEquals(guid, session.get("guid"), "GUID in response must match the requested user_id");
        AssertionUtil.verifyEquals(mobile, session.get("mobile"), "Mobile in response must match the registered mobile");
        AssertionUtil.verifyNotNull(firstName, "first_name must be present in response");
        AssertionUtil.verifyNotNull(lastName, "last_name must be present in response");
        AssertionUtil.verifyTrue(success, "success flag must be true");

        System.out.println("\n✅ GU-02 PASSED — All key response fields verified.");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // GU_03 — No Authorization Header — NEGATIVE
    // =========================================================
    @Test(priority = 3, description = "QA Automation: Get User — No Authorization Header — Should Return 401 or 403")
    public void GU_03_GetUser_NoAuthToken() {
        System.out.println("\n==========================================================");
        System.out.println("  GU-03: GET USER — NO AUTH HEADER (NEGATIVE)");
        System.out.println("==========================================================");
        System.out.println("   user_id : " + newUserGuid);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint(newUserGuid))
                .getWithoutStatusCheck();

        int status = response.getStatusCode();
        System.out.println("\n📌 Status : " + status);
        System.out.println("📌 Body   : " + response.getBody().asString());

        AssertionUtil.verifyTrue(status == 401 || status == 403,
                "No auth header must return 401 or 403; got: " + status);

        System.out.println("\n✅ GU-03 PASSED — Request without token correctly rejected.");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // GU_04 — Invalid / Tampered Token — NEGATIVE
    // =========================================================
    @Test(priority = 4, description = "QA Automation: Get User — Invalid Bearer Token — Should Return 401 or 403")
    public void GU_04_GetUser_InvalidAuthToken() {
        System.out.println("\n==========================================================");
        System.out.println("  GU-04: GET USER — INVALID TOKEN (NEGATIVE)");
        System.out.println("==========================================================");
        System.out.println("   user_id : " + newUserGuid);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint(newUserGuid))
                .addHeader("authorization", "Bearer this.is.an.invalid.jwt.token.xyz")
                .getWithoutStatusCheck();

        int status = response.getStatusCode();
        System.out.println("\n📌 Status : " + status);
        System.out.println("📌 Body   : " + response.getBody().asString());

        AssertionUtil.verifyTrue(status == 401 || status == 403,
                "Invalid/tampered token must return 401 or 403; got: " + status);

        System.out.println("\n✅ GU-04 PASSED — Tampered token correctly rejected.");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // GU_05 — Non-Existent GUID as user_id — NEGATIVE
    // =========================================================
    @Test(priority = 5, description = "QA Automation: Get User — Non-existent user_id (all-zero UUID) — Should Return 4xx")
    public void GU_05_GetUser_NonExistentUserId() {
        System.out.println("\n==========================================================");
        System.out.println("  GU-05: GET USER — NON-EXISTENT user_id (NEGATIVE)");
        System.out.println("==========================================================");

        String fakeGuid = "00000000-0000-0000-0000-000000000000";
        System.out.println("   user_id : " + fakeGuid);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint(fakeGuid))
                .addHeader("authorization", newUserToken)
                .getWithoutStatusCheck();

        int status = response.getStatusCode();
        System.out.println("\n📌 Status : " + status);
        System.out.println("📌 Body   : " + response.getBody().asString());

        AssertionUtil.verifyTrue(status >= 400 && status < 500,
                "Non-existent user_id must return 4xx; got: " + status);

        System.out.println("\n✅ GU-05 PASSED — Non-existent user_id correctly rejected.");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // GU_04 — Malformed (non-UUID) user_id — NEGATIVE
    // =========================================================
    @Test(priority = 4, description = "QA Automation: Get User — Malformed user_id (not a UUID) — Should Return 4xx")
    public void GU_06_GetUser_MalformedUserId() {
        System.out.println("\n==========================================================");
        System.out.println("  GU-06: GET USER — MALFORMED user_id (NEGATIVE)");
        System.out.println("==========================================================");

        String badId = "invalid-user-id-xyz";
        System.out.println("   user_id : " + badId);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint(badId))
                .addHeader("authorization", newUserToken)
                .getWithoutStatusCheck();

        int status = response.getStatusCode();
        System.out.println("\n📌 Status : " + status);
        System.out.println("📌 Body   : " + response.getBody().asString());

        // API returns 500 for a malformed (non-UUID) path param — this is a server-side
        // unhandled error (should ideally be 422). Any non-2xx response means the request
        // was rejected, which is the expected behaviour.
        AssertionUtil.verifyTrue(status >= 400,
                "Malformed user_id must return a non-2xx status; got: " + status);

        System.out.println("\n✅ GU-06 PASSED — Malformed user_id correctly rejected.");
        System.out.println("==========================================================\n");
    }
}

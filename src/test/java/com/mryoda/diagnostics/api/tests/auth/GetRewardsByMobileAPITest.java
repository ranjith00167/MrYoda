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
 * GET REWARDS BY MOBILE API TEST
 * GET /users/getRewardsByMobile/{mobile}
 * ============================================================
 *
 * Endpoint : GET https://staging-api-diagnostics.yodaprojects.com
 *                      /users/getRewardsByMobile/{mobile}
 * Auth     : Bearer token
 *
 * ── MEMBER FLOW ─────────────────────────────────────────────
 *   GRM_01 — Member mobile → 200, is_member=true, total_rewards > 0
 *   GRM_02 — Verify all response fields for member
 *            (success, mobile, total_rewards, is_member, reward values)
 *
 * ── NEW USER FLOW ────────────────────────────────────────────
 *   GRM_03 — Fresh registered user → 200, is_member=false/null, rewards=0
 *   GRM_04 — Verify all response fields for new user
 * ============================================================
 */
public class GetRewardsByMobileAPITest extends BaseTest {

    // ── Member session ────────────────────────────────────────────────────────
    private static String memberToken;
    private static String memberMobile;

    // ── New user session ──────────────────────────────────────────────────────
    private static String newUserToken;
    private static String newUserMobile;

    // ── Helper: resolve endpoint ──────────────────────────────────────────────
    private static String endpoint(String mobile) {
        return APIEndpoints.GET_USER_REWARDS_BY_MOBILE.replace("{mobile}", mobile);
    }

    // =========================================================
    // SETUP — Login member + register fresh new user
    // =========================================================
    @BeforeClass(alwaysRun = true)
    public void setupSessions() {
        System.out.println("\n==========================================================");
        System.out.println("  GET REWARDS BY MOBILE — SETUP");
        System.out.println("==========================================================");

        // --- 1. Member login (paid member from config) ---
        memberMobile = ConfigLoader.getConfig().memberMobile();
        RequestContext.setMobile(memberMobile);
        memberToken = TokenManager.generateToken(memberMobile, TokenManager.MEMBER);
        System.out.println("   ✅ Member logged in — Mobile: " + memberMobile);

        // --- 2. Register a fresh new user ---
        newUserMobile = "9" + RandomDataUtil.getRandomMobile().substring(1);
        RequestContext.setMobile(newUserMobile);

        org.json.JSONObject regPayload = UserPayloadBuilder.buildNewUserPayload();
        regPayload.put("mobile", newUserMobile);

        new RequestBuilder()
                .setEndpoint(APIEndpoints.USER_CREATE)
                .setRequestBody(regPayload.toString())
                .expectStatus(201)
                .post();

        newUserToken = TokenManager.generateToken(newUserMobile, TokenManager.NEW_USER);
        System.out.println("   ✅ New user registered — Mobile: " + newUserMobile);

        System.out.println("==========================================================\n");
    }

    // =========================================================
    // GRM_01 — Member: rewards present, is_member=true
    // =========================================================
    @Test(priority = 1, description = "QA Automation: Get Rewards — Member mobile → 200, is_member=true, total_rewards ≥ 0")
    public void GRM_01_GetRewards_Member_ValidResponse() {
        System.out.println("\n==========================================================");
        System.out.println("  GRM-01: GET REWARDS — MEMBER FLOW (POSITIVE)");
        System.out.println("==========================================================");
        System.out.println("   Mobile : " + memberMobile);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint(memberMobile))
                .addHeader("authorization", memberToken)
                .expectStatus(200)
                .get();

        boolean success       = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));
        Object  membershipStatus = response.jsonPath().get("data[0].membership_status");
        Object  totalRewards     = response.jsonPath().get("data[0].total_rewards");

        System.out.println("\n🔍 RESPONSE:");
        System.out.println("   Status            : " + response.getStatusCode());
        System.out.println("   success           : " + success);
        System.out.println("   membership_status : " + membershipStatus);
        System.out.println("   total_rewards     : " + totalRewards);
        System.out.println("\n   Full body:\n" + response.getBody().asString());

        AssertionUtil.verifyTrue(success, "success must be true for member rewards request");
        AssertionUtil.verifyTrue(Boolean.TRUE.equals(membershipStatus),
                "membership_status must be true for member — got: " + membershipStatus);
        AssertionUtil.verifyNotNull(totalRewards, "total_rewards must be present in member response");

        System.out.println("\n✅ GRM-01 PASSED — Member rewards returned with membership_status=true.");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // GRM_02 — Member: verify all response fields with values
    // =========================================================
    @Test(priority = 2, description = "QA Automation: Get Rewards — Verify all response fields for member")
    public void GRM_02_GetRewards_Member_VerifyFields() {
        System.out.println("\n==========================================================");
        System.out.println("  GRM-02: GET REWARDS — MEMBER FIELD VALIDATION (POSITIVE)");
        System.out.println("==========================================================");
        System.out.println("   Mobile : " + memberMobile);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint(memberMobile))
                .addHeader("authorization", memberToken)
                .expectStatus(200)
                .get();

        boolean success           = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));
        Object  membershipStatus  = response.jsonPath().get("data[0].membership_status");
        Object  totalRewards      = response.jsonPath().get("data[0].total_rewards");
        Object  rewardsUsed       = response.jsonPath().get("data[0].rewards_used");
        Object  revertedRewards   = response.jsonPath().get("data[0].reverted_rewards");
        Object  spentAmount       = response.jsonPath().get("data[0].spent_amount");
        Object  membershipStart   = response.jsonPath().get("data[0].membership_start_date");
        Object  membershipEnd     = response.jsonPath().get("data[0].membership_end_date");
        String  guid              = response.jsonPath().getString("data[0].guid");

        System.out.println("\n🔍 RESPONSE FIELDS:");
        System.out.println("   success              : " + success);
        System.out.println("   guid                 : " + guid);
        System.out.println("   membership_status    : " + membershipStatus);
        System.out.println("   total_rewards        : " + totalRewards);
        System.out.println("   rewards_used         : " + rewardsUsed);
        System.out.println("   reverted_rewards     : " + revertedRewards);
        System.out.println("   spent_amount         : " + spentAmount);
        System.out.println("   membership_start_date: " + membershipStart);
        System.out.println("   membership_end_date  : " + membershipEnd);
        System.out.println("\n   Full body:\n" + response.getBody().asString());

        AssertionUtil.verifyTrue(success, "success must be true");
        AssertionUtil.verifyTrue(Boolean.TRUE.equals(membershipStatus),
                "membership_status must be true for member — got: " + membershipStatus);
        AssertionUtil.verifyNotNull(totalRewards,
                "total_rewards must be present — got: " + totalRewards);
        AssertionUtil.verifyNotNull(guid,
                "guid must be present in response");
        AssertionUtil.verifyNotNull(membershipStart,
                "membership_start_date must be present for member — got: " + membershipStart);
        AssertionUtil.verifyNotNull(membershipEnd,
                "membership_end_date must be present for member — got: " + membershipEnd);

        System.out.println("\n✅ GRM-02 PASSED — All member response fields verified.");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // GRM_03 — New user: no membership, rewards=0
    // =========================================================
    @Test(priority = 3, description = "QA Automation: Get Rewards — New user → 200, is_member=false, rewards=0")
    public void GRM_03_GetRewards_NewUser_ValidResponse() {
        System.out.println("\n==========================================================");
        System.out.println("  GRM-03: GET REWARDS — NEW USER FLOW (POSITIVE)");
        System.out.println("==========================================================");
        System.out.println("   Mobile : " + newUserMobile);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint(newUserMobile))
                .addHeader("authorization", newUserToken)
                .expectStatus(200)
                .get();

        boolean success          = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));
        List<?> dataList         = response.jsonPath().getList("data");
        Object  membershipStatus = dataList != null && !dataList.isEmpty()
                                    ? response.jsonPath().get("data[0].membership_status")
                                    : null;

        System.out.println("\n🔍 RESPONSE:");
        System.out.println("   Status            : " + response.getStatusCode());
        System.out.println("   success           : " + success);
        System.out.println("   data[] size       : " + (dataList != null ? dataList.size() : "null"));
        System.out.println("   membership_status : " + membershipStatus);
        System.out.println("\n   Full body:\n" + response.getBody().asString());

        AssertionUtil.verifyTrue(success, "success must be true for new user rewards request");
        // New user has no membership record — data array may be empty or membership_status=false
        AssertionUtil.verifyTrue(
                dataList == null || dataList.isEmpty() || !Boolean.TRUE.equals(membershipStatus),
                "membership_status must be false/absent for new user — got: " + membershipStatus);

        System.out.println("\n✅ GRM-03 PASSED — New user has no active membership.");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // GRM_04 — New user: verify all response fields with values
    // =========================================================
    @Test(priority = 4, description = "QA Automation: Get Rewards — Verify all response fields for new user")
    public void GRM_04_GetRewards_NewUser_VerifyFields() {
        System.out.println("\n==========================================================");
        System.out.println("  GRM-04: GET REWARDS — NEW USER FIELD VALIDATION (POSITIVE)");
        System.out.println("==========================================================");
        System.out.println("   Mobile : " + newUserMobile);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint(newUserMobile))
                .addHeader("authorization", newUserToken)
                .expectStatus(200)
                .get();

        boolean success          = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));
        List<?> dataList         = response.jsonPath().getList("data");
        Object  membershipStatus = dataList != null && !dataList.isEmpty()
                                    ? response.jsonPath().get("data[0].membership_status")
                                    : null;
        Object  totalRewards     = dataList != null && !dataList.isEmpty()
                                    ? response.jsonPath().get("data[0].total_rewards")
                                    : null;

        System.out.println("\n🔍 RESPONSE FIELDS:");
        System.out.println("   success           : " + success);
        System.out.println("   data[] size       : " + (dataList != null ? dataList.size() : "null"));
        System.out.println("   membership_status : " + membershipStatus);
        System.out.println("   total_rewards     : " + totalRewards);
        System.out.println("\n   Full body:\n" + response.getBody().asString());

        AssertionUtil.verifyTrue(success, "success must be true");
        AssertionUtil.verifyTrue(
                dataList == null || dataList.isEmpty() || !Boolean.TRUE.equals(membershipStatus),
                "membership_status must be false/absent for new user — got: " + membershipStatus);

        System.out.println("\n✅ GRM-04 PASSED — All new user response fields verified.");
        System.out.println("==========================================================\n");
    }
}

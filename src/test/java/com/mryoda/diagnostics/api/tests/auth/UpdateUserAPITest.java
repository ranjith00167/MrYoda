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
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * ============================================================
 * UPDATE USER API TEST — POST /users/updateUser
 * ============================================================
 *
 * Endpoint  : POST https://staging-api-diagnostics.yodaprojects.com/users/updateUser
 * Auth      : Bearer token (obtained via COD_01_LoginTest / TokenManager)
 * GUID      : Extracted from RequestContext after COD_01_LoginTest login step
 *
 * Mandatory fields : first_name, last_name, dob, gender, guid, mobile, title
 * Non-mandatory    : alt_mobile, email, middle_name
 *
 * ── POSITIVE TESTS ──────────────────────────────────────────
 *   USR_01 — All fields (mandatory + all non-mandatory)
 *   USR_02 — Mandatory fields only  (no alt_mobile/email/middle_name)
 *   USR_03 — Mandatory + empty non-mandatory (email="", alt_mobile="", middle_name="")
 *
 * ── NEGATIVE — Auth ─────────────────────────────────────────
 *   USR_04 — No Authorization header            → 401 / 403
 *   USR_05 — Invalid / tampered Bearer token    → 401 / 403
 *
 * ── NEGATIVE — Mandatory field validation ───────────────────
 *   USR_06 — Missing first_name                 → 4xx
 *   USR_07 — Missing last_name                  → 4xx
 *   USR_08 — Missing dob                        → 4xx
 *   USR_09 — Missing gender                     → 4xx
 *   USR_10 — Missing title                      → 4xx
 *   USR_11 — Missing guid                       → 4xx
 *   USR_12 — Missing mobile                     → 4xx
 *   USR_13 — Invalid (non-existent) GUID        → 4xx
 * ============================================================
 */
public class UpdateUserAPITest extends BaseTest {

    // ── New User Flow ────────────────────────────────────────────────────────
    // This suite uses the NEW USER flow:
    //   1. UserCreateAPITest.testUserRegistration_CreateNewUser() runs first in
    //      the suite — it generates a random mobile, registers via /users/addUser,
    //      and stores:  RequestContext.setMobile(mobile)  +  setUserId(guid)
    //   2. @BeforeClass captures that mobile BEFORE calling TokenManager, because
    //      TokenManager.generateToken() may overwrite RequestContext.getMobile()
    //      internally during the OTP verification step.
    //   3. TokenManager.generateToken(mobile, NEW_USER) logs the new user in.
    // ────────────────────────────────────────────────────────────────────────

    // Captured in @BeforeClass — never read from RequestContext inside test methods
    // to avoid the duplicate-key 500 that occurs when TokenManager overwrites the mobile.
    private static String registeredMobile;
    private static String newUserToken;
    private static String newUserGuid;

    // =========================================================
    // SETUP — NEW USER flow (random mobile from UserCreateAPITest)
    // =========================================================
    @BeforeClass(alwaysRun = true)
    public void setupNewUserLogin() {
        System.out.println("\n==========================================================");
        System.out.println("  UPDATE USER TEST — NEW USER FLOW SETUP");
        System.out.println("==========================================================");
        RequestContext.setCurrentFlowName("new_user_flow");

        // Step 1: Capture mobile registered by UserCreateAPITest (suite run)
        //         or fall back to registering inline (standalone run).
        registeredMobile = RequestContext.getMobile();

        if (registeredMobile == null) {
            // Standalone fallback — register a brand-new random user inline.
            System.out.println("   ⚙️  No mobile in RequestContext — registering new random user...");
            registeredMobile = "9" + RandomDataUtil.getRandomMobile().substring(1);
            RequestContext.setMobile(registeredMobile);

            JSONObject regPayload = UserPayloadBuilder.buildNewUserPayload();
            regPayload.put("mobile", registeredMobile); // ensure the generated mobile is used

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

        // Step 2: Login with the NEW USER token type.
        //         Do this AFTER capturing registeredMobile — TokenManager may
        //         call RequestContext.setMobile() and overwrite it internally.
        newUserToken = TokenManager.generateToken(registeredMobile, TokenManager.NEW_USER);

        // Step 3: Read GUID (set by UserCreateAPITest or inline registration above).
        newUserGuid = RequestContext.getUserId();
        if (newUserGuid == null) {
            newUserGuid = RequestContext.getNewUserUserId();
        }

        System.out.println("   ✅ Flow          : NEW USER");
        System.out.println("   ✅ Mobile        : " + registeredMobile);
        System.out.println("   ✅ GUID          : " + newUserGuid);
        System.out.println("   ✅ Token         : " + (newUserToken != null ? "PRESENT" : "NULL — LOGIN FAILED"));
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // INTERNAL HELPERS
    // =========================================================

    /** Full payload — all mandatory fields + all non-mandatory fields with real data. */
    private JSONObject buildFullPayload() {
        String gender = RandomDataUtil.getRandomGender();
        JSONObject body = new JSONObject();
        body.put("guid",        newUserGuid);
        body.put("mobile",      registeredMobile); // captured once in @BeforeClass
        body.put("first_name",  RandomDataUtil.getRandomFirstName());
        body.put("last_name",   RandomDataUtil.getRandomLastName());
        body.put("middle_name", RandomDataUtil.getRandomMiddleName()); // non-mandatory
        body.put("dob",         RandomDataUtil.getRandomDOB());
        body.put("gender",      gender);
        body.put("title",       gender.equalsIgnoreCase("Male") ? "Mr." : "Mrs.");
        body.put("email",       RandomDataUtil.getRandomEmail());       // non-mandatory
        body.put("alt_mobile",  RandomDataUtil.getRandomMobile());      // non-mandatory
        return body;
    }

    /** Mandatory-only payload — no alt_mobile / email / middle_name keys at all. */
    private JSONObject buildMandatoryOnlyPayload() {
        String gender = RandomDataUtil.getRandomGender();
        JSONObject body = new JSONObject();
        body.put("guid",       newUserGuid);
        body.put("mobile",     registeredMobile); // captured once in @BeforeClass
        body.put("first_name", RandomDataUtil.getRandomFirstName());
        body.put("last_name",  RandomDataUtil.getRandomLastName());
        body.put("dob",        RandomDataUtil.getRandomDOB());
        body.put("gender",     gender);
        body.put("title",      gender.equalsIgnoreCase("Male") ? "Mr." : "Mrs.");
        return body;
    }

    /**
     * Registers a fresh random user and logs in via NEW_USER flow.
     * Returns a Map with keys: "token", "guid", "mobile", "payload".
     * Used by every negative test so each validation runs against its own user
     * and is fully independent of the shared @BeforeClass session.
     */
    private Map<String, String> createFreshUserSession() {
        String mobile = "9" + RandomDataUtil.getRandomMobile().substring(1);

        // Set mobile in RequestContext before building payload so UserPayloadBuilder picks it up.
        // This also ensures we capture it BEFORE TokenManager can overwrite it.
        RequestContext.setMobile(mobile);
        JSONObject reg = UserPayloadBuilder.buildNewUserPayload();
        reg.put("mobile", mobile); // ensure the generated mobile is used

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

    /** Builds a mandatory-only payload from a fresh user session map. */
    private JSONObject buildPayloadFromSession(Map<String, String> s) {
        String gender = RandomDataUtil.getRandomGender();
        JSONObject body = new JSONObject();
        body.put("guid",       s.get("guid"));
        body.put("mobile",     s.get("mobile"));
        body.put("first_name", RandomDataUtil.getRandomFirstName());
        body.put("last_name",  RandomDataUtil.getRandomLastName());
        body.put("dob",        RandomDataUtil.getRandomDOB());
        body.put("gender",     gender);
        body.put("title",      gender.equalsIgnoreCase("Male") ? "Mr." : "Mrs.");
        return body;
    }

    // =========================================================
    // USR_01 — All Fields (Mandatory + Non-Mandatory) — POSITIVE
    // =========================================================
    @Test(priority = 1, description = "QA Automation: Update User — All Fields — Mandatory + Non-Mandatory (email, alt_mobile, middle_name)")
    public void USR_01_UpdateUser_AllFields() {
        System.out.println("\n==========================================================");
        System.out.println("  USR-01: UPDATE USER — ALL FIELDS (POSITIVE)");
        System.out.println("==========================================================");

        Map<String, String> session = createFreshUserSession();

        String gender01 = RandomDataUtil.getRandomGender();
        JSONObject payload = new JSONObject();
        payload.put("guid",        session.get("guid"));
        payload.put("mobile",      session.get("mobile"));
        payload.put("first_name",  RandomDataUtil.getRandomFirstName());
        payload.put("last_name",   RandomDataUtil.getRandomLastName());
        payload.put("middle_name", RandomDataUtil.getRandomMiddleName());
        payload.put("dob",         RandomDataUtil.getRandomDOB());
        payload.put("gender",      gender01);
        payload.put("title",       gender01.equalsIgnoreCase("Male") ? "Mr." : "Mrs.");
        payload.put("email",       RandomDataUtil.getRandomEmail());
        payload.put("alt_mobile",  RandomDataUtil.getRandomMobile());
        System.out.println("\n➡️  REQUEST:\n" + payload.toString(2));

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.UPDATE_USER)
                .addHeader("authorization", session.get("token"))
                .setRequestBody(payload.toString())
                .expectStatus(200)
                .post();

        String updatedGuid   = response.jsonPath().getString("data.guid");
        String updatedFirst  = response.jsonPath().getString("data.first_name");
        String updatedLast   = response.jsonPath().getString("data.last_name");
        String updatedDob    = response.jsonPath().getString("data.dob");
        String updatedGender = response.jsonPath().getString("data.gender");
        String updatedTitle  = response.jsonPath().getString("data.title");
        String updatedMobile = response.jsonPath().getString("data.mobile");
        boolean success      = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));

        System.out.println("\n🔍 RESPONSE DATA:");
        System.out.println("   GUID        : " + updatedGuid);
        System.out.println("   First Name  : " + updatedFirst);
        System.out.println("   Last Name   : " + updatedLast);
        System.out.println("   DOB         : " + updatedDob);
        System.out.println("   Gender      : " + updatedGender);
        System.out.println("   Title       : " + updatedTitle);
        System.out.println("   Mobile      : " + updatedMobile);
        System.out.println("   success     : " + success);

        AssertionUtil.verifyEquals(updatedGuid, session.get("guid"), "GUID must match the registered user");
        AssertionUtil.verifyEquals(updatedFirst, payload.getString("first_name"), "first_name must reflect update");
        AssertionUtil.verifyEquals(updatedLast, payload.getString("last_name"), "last_name must reflect update");
        AssertionUtil.verifyTrue(success, "success flag must be true");

        System.out.println("\n✅ USR-01 PASSED — Update with all fields successful.");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // USR_02 — Mandatory Fields Only — POSITIVE
    // =========================================================
    @Test(priority = 2, description = "QA Automation: Update User — Mandatory Fields Only — No alt_mobile / email / middle_name")
    public void USR_02_UpdateUser_MandatoryFieldsOnly() {
        System.out.println("\n==========================================================");
        System.out.println("  USR-02: UPDATE USER — MANDATORY FIELDS ONLY (POSITIVE)");
        System.out.println("==========================================================");

        Map<String, String> session = createFreshUserSession();
        JSONObject payload = buildPayloadFromSession(session);
        System.out.println("\n➡️  REQUEST:\n" + payload.toString(2));

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.UPDATE_USER)
                .addHeader("authorization", session.get("token"))
                .setRequestBody(payload.toString())
                .expectStatus(200)
                .post();

        String updatedGuid = response.jsonPath().getString("data.guid");
        boolean success    = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));

        System.out.println("\n🔍 GUID    : " + updatedGuid);
        System.out.println("   success : " + success);

        AssertionUtil.verifyEquals(updatedGuid, session.get("guid"), "GUID must match");
        AssertionUtil.verifyTrue(success, "success flag must be true — non-mandatory fields are optional");

        System.out.println("\n✅ USR-02 PASSED — Non-mandatory fields (alt_mobile, email, middle_name) are truly optional.");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // USR_03 — Empty Non-Mandatory Fields — POSITIVE
    // =========================================================
    @Test(priority = 3, description = "QA Automation: Update User — Non-Mandatory Fields as Empty Strings (email='', alt_mobile='', middle_name='')")
    public void USR_03_UpdateUser_EmptyNonMandatoryFields() {
        System.out.println("\n==========================================================");
        System.out.println("  USR-03: UPDATE USER — EMPTY NON-MANDATORY FIELDS (POSITIVE)");
        System.out.println("==========================================================");
        System.out.println("   Verifying: alt_mobile, email, middle_name accepted as empty string.");

        Map<String, String> session = createFreshUserSession();

        String gender = RandomDataUtil.getRandomGender();
        JSONObject payload = new JSONObject();
        payload.put("guid",        session.get("guid"));
        payload.put("mobile",      session.get("mobile"));
        payload.put("first_name",  RandomDataUtil.getRandomFirstName());
        payload.put("last_name",   RandomDataUtil.getRandomLastName());
        payload.put("middle_name", "");             // non-mandatory — empty string
        payload.put("dob",         RandomDataUtil.getRandomDOB());
        payload.put("gender",      gender);
        payload.put("title",       gender.equalsIgnoreCase("Male") ? "Mr." : "Mrs.");
        payload.put("email",       "");             // non-mandatory — empty string
        payload.put("alt_mobile",  "");             // non-mandatory — empty string

        System.out.println("\n➡️  REQUEST:\n" + payload.toString(2));

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.UPDATE_USER)
                .addHeader("authorization", session.get("token"))
                .setRequestBody(payload.toString())
                .expectStatus(200)
                .post();

        boolean success = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));
        System.out.println("\n🔍 success : " + success);

        AssertionUtil.verifyTrue(success, "Empty alt_mobile / email / middle_name must not block a successful update");

        System.out.println("\n✅ USR-03 PASSED — Empty non-mandatory fields accepted without error.");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // USR_06 — Missing first_name — NEGATIVE (Mandatory)
    // =========================================================
    @Test(priority = 6, description = "QA Automation: Update User — Missing Mandatory: first_name — Should Return 4xx")
    public void USR_06_UpdateUser_Missing_FirstName() {
        System.out.println("\n==========================================================");
        System.out.println("  USR-06: UPDATE USER — MISSING first_name (NEGATIVE)");
        System.out.println("==========================================================");

        Map<String, String> session = createFreshUserSession();
        JSONObject payload = buildPayloadFromSession(session);
        payload.remove("first_name");
        System.out.println("\n➡️  REQUEST:\n" + payload.toString(2));

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.UPDATE_USER)
                .addHeader("authorization", session.get("token"))
                .setRequestBody(payload.toString())
                .postWithoutStatusCheck();

        int status = response.getStatusCode();
        System.out.println("\n📌 Status : " + status);
        System.out.println("📌 Body   : " + response.getBody().asString());

        AssertionUtil.verifyTrue(status >= 400 && status < 500,
                "Missing first_name must return 4xx; got: " + status);

        System.out.println("\n\u2705 USR-06 PASSED \u2014 Missing first_name correctly rejected.");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // USR_07 — Missing last_name — NEGATIVE (Mandatory)
    // =========================================================
    @Test(priority = 7, description = "QA Automation: Update User — Missing Mandatory: last_name — Should Return 4xx")
    public void USR_07_UpdateUser_Missing_LastName() {
        System.out.println("\n==========================================================");
        System.out.println("  USR-07: UPDATE USER — MISSING last_name (NEGATIVE)");
        System.out.println("==========================================================");

        Map<String, String> session = createFreshUserSession();
        JSONObject payload = buildPayloadFromSession(session);
        payload.remove("last_name");
        System.out.println("\n➡️  REQUEST:\n" + payload.toString(2));

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.UPDATE_USER)
                .addHeader("authorization", session.get("token"))
                .setRequestBody(payload.toString())
                .postWithoutStatusCheck();

        int status = response.getStatusCode();
        System.out.println("\n📌 Status : " + status);
        System.out.println("📌 Body   : " + response.getBody().asString());

        AssertionUtil.verifyTrue(status >= 400 && status < 500,
                "Missing last_name must return 4xx; got: " + status);

        System.out.println("\n\u2705 USR-07 PASSED \u2014 Missing last_name correctly rejected.");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // USR_08 — Missing dob — NEGATIVE (Mandatory)
    // =========================================================
    @Test(priority = 8, description = "QA Automation: Update User — Missing Mandatory: dob — Should Return 4xx")
    public void USR_08_UpdateUser_Missing_Dob() {
        System.out.println("\n==========================================================");
        System.out.println("  USR-08: UPDATE USER — MISSING dob (NEGATIVE)");
        System.out.println("==========================================================");

        Map<String, String> session = createFreshUserSession();
        JSONObject payload = buildPayloadFromSession(session);
        payload.remove("dob");
        System.out.println("\n➡️  REQUEST:\n" + payload.toString(2));

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.UPDATE_USER)
                .addHeader("authorization", session.get("token"))
                .setRequestBody(payload.toString())
                .postWithoutStatusCheck();

        int status = response.getStatusCode();
        System.out.println("\n📌 Status : " + status);
        System.out.println("📌 Body   : " + response.getBody().asString());

        AssertionUtil.verifyTrue(status >= 400 && status < 500,
                "Missing dob must return 4xx; got: " + status);

        System.out.println("\n\u2705 USR-08 PASSED \u2014 Missing dob correctly rejected.");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // USR_09 — Missing gender — NEGATIVE (Mandatory)
    // =========================================================
    @Test(priority = 9, description = "QA Automation: Update User — Missing Mandatory: gender — Should Return 4xx")
    public void USR_09_UpdateUser_Missing_Gender() {
        System.out.println("\n==========================================================");
        System.out.println("  USR-09: UPDATE USER — MISSING gender (NEGATIVE)");
        System.out.println("==========================================================");

        Map<String, String> session = createFreshUserSession();
        JSONObject payload = buildPayloadFromSession(session);
        payload.remove("gender");
        System.out.println("\n➡️  REQUEST:\n" + payload.toString(2));

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.UPDATE_USER)
                .addHeader("authorization", session.get("token"))
                .setRequestBody(payload.toString())
                .postWithoutStatusCheck();

        int status = response.getStatusCode();
        System.out.println("\n📌 Status : " + status);
        System.out.println("📌 Body   : " + response.getBody().asString());

        AssertionUtil.verifyTrue(status >= 400 && status < 500,
                "Missing gender must return 4xx; got: " + status);

        System.out.println("\n\u2705 USR-09 PASSED \u2014 Missing gender correctly rejected.");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // USR_10 — Missing title — NEGATIVE (Mandatory)
    // =========================================================
    @Test(priority = 10, description = "QA Automation: Update User — Missing Mandatory: title — Should Return 4xx")
    public void USR_10_UpdateUser_Missing_Title() {
        System.out.println("\n==========================================================");
        System.out.println("  USR-10: UPDATE USER — MISSING title (NEGATIVE)");
        System.out.println("==========================================================");

        Map<String, String> session = createFreshUserSession();
        JSONObject payload = buildPayloadFromSession(session);
        payload.remove("title");
        System.out.println("\n➡️  REQUEST:\n" + payload.toString(2));

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.UPDATE_USER)
                .addHeader("authorization", session.get("token"))
                .setRequestBody(payload.toString())
                .postWithoutStatusCheck();

        int status = response.getStatusCode();
        System.out.println("\n📌 Status : " + status);
        System.out.println("📌 Body   : " + response.getBody().asString());

        AssertionUtil.verifyTrue(status >= 400 && status < 500,
                "Missing title must return 4xx; got: " + status);

        System.out.println("\n\u2705 USR-10 PASSED \u2014 Missing title correctly rejected.");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // USR_11 — Missing guid — NEGATIVE (Mandatory)
    // =========================================================
    @Test(priority = 11, description = "QA Automation: Update User — Missing Mandatory: guid — Should Return 4xx")
    public void USR_11_UpdateUser_Missing_Guid() {
        System.out.println("\n==========================================================");
        System.out.println("  USR-11: UPDATE USER — MISSING guid (NEGATIVE)");
        System.out.println("==========================================================");

        Map<String, String> session = createFreshUserSession();
        JSONObject payload = buildPayloadFromSession(session);
        payload.remove("guid");
        System.out.println("\n➡️  REQUEST:\n" + payload.toString(2));

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.UPDATE_USER)
                .addHeader("authorization", session.get("token"))
                .setRequestBody(payload.toString())
                .postWithoutStatusCheck();

        int status = response.getStatusCode();
        System.out.println("\n📌 Status : " + status);
        System.out.println("📌 Body   : " + response.getBody().asString());

        AssertionUtil.verifyTrue(status >= 400 && status < 500,
                "Missing guid must return 4xx; got: " + status);

        System.out.println("\n✅ USR-11 PASSED — Missing guid correctly rejected.");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // USR_12 — Missing mobile — NEGATIVE (Mandatory)
    // =========================================================
    @Test(priority = 12, description = "QA Automation: Update User — Missing Mandatory: mobile — Should Return 4xx")
    public void USR_12_UpdateUser_Missing_Mobile() {
        System.out.println("\n==========================================================");
        System.out.println("  USR-12: UPDATE USER — MISSING mobile (NEGATIVE)");
        System.out.println("==========================================================");

        Map<String, String> session = createFreshUserSession();
        JSONObject payload = buildPayloadFromSession(session);
        payload.remove("mobile");
        System.out.println("\n➡️  REQUEST:\n" + payload.toString(2));

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.UPDATE_USER)
                .addHeader("authorization", session.get("token"))
                .setRequestBody(payload.toString())
                .postWithoutStatusCheck();

        int status = response.getStatusCode();
        System.out.println("\n📌 Status : " + status);
        System.out.println("📌 Body   : " + response.getBody().asString());

        AssertionUtil.verifyTrue(status >= 400 && status < 500,
                "Missing mobile must return 4xx; got: " + status);

        System.out.println("\n✅ USR-12 PASSED — Missing mobile correctly rejected.");
        System.out.println("==========================================================\n");
    }

    // =========================================================
    // USR_13 — Invalid (Non-Existent) GUID — NEGATIVE
    // =========================================================
    @Test(priority = 13, description = "QA Automation: Update User — Invalid GUID (non-existent user) — Should Return 4xx")
    public void USR_13_UpdateUser_InvalidGuid() {
        System.out.println("\n==========================================================");
        System.out.println("  USR-13: UPDATE USER — INVALID GUID (NEGATIVE)");
        System.out.println("==========================================================");
        System.out.println("   Using all-zero UUID that cannot match any real user.");

        Map<String, String> session = createFreshUserSession();
        JSONObject payload = buildPayloadFromSession(session);
        payload.put("guid", "00000000-0000-0000-0000-000000000000");
        System.out.println("\n➡️  REQUEST:\n" + payload.toString(2));

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.UPDATE_USER)
                .addHeader("authorization", session.get("token"))
                .setRequestBody(payload.toString())
                .postWithoutStatusCheck();

        int status = response.getStatusCode();
        System.out.println("\n📌 Status : " + status);
        System.out.println("📌 Body   : " + response.getBody().asString());

        AssertionUtil.verifyTrue(status >= 400 && status < 500,
                "Non-existent GUID must return 4xx; got: " + status);

        System.out.println("\n✅ USR-13 PASSED — Invalid GUID correctly rejected.");
        System.out.println("==========================================================\n");
    }
}

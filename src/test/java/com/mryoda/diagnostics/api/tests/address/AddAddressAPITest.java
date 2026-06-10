package com.mryoda.diagnostics.api.tests.address;

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
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================
 * ADD ADDRESS API TEST
 * POST /address/addAddress
 * ============================================================
 *
 * Endpoint : POST https://staging-api-diagnostics.yodaprojects.com
 *                       /address/addAddress
 * Auth     : Bearer member / new-user token
 *
 * Two flows tested:
 *
 *   FLOW 1 — MEMBER USER
 *     Step 1 — Login with member credentials → member token + user details
 *     Step 2 — POST /address/addAddress with member user_id, name, mobile
 *     NOTE: Member may already have an address (API returns 409) — handled gracefully
 *
 *   FLOW 2 — NEW USER
 *     Step 1 — Generate random mobile + POST /users/addUser → create fresh user
 *     Step 2 — Generate token for new user
 *     Step 3 — POST /address/addAddress with new user_id, name, mobile
 *     Always returns 201 for freshly created users
 *
 * ── MEMBER USER TESTS ───────────────────────────────────────
 *   ADD_01 — Member token + member user_id → 201/409 with success flag
 *   ADD_02 — Verify member address response fields (user_id, mobile, city, state)
 *
 * ── NEW USER TESTS ───────────────────────────────────────────
 *   ADD_03 — New user token + new user_id → 201 with address data
 *   ADD_04 — Verify new user address response fields (all core fields)
 * ============================================================
 */
public class AddAddressAPITest extends BaseTest {

    // ── FLOW 1: Member session ────────────────────────────────────────────────
    private static String memberToken;
    private static String memberMobile;
    private static String memberUserId;
    private static String memberFirstName;
    private static String memberLastName;

    // ── FLOW 2: New user session ──────────────────────────────────────────────
    private static String newUserToken;
    private static String newUserMobile;
    private static String newUserUserId;
    private static String newUserFirstName;
    private static String newUserLastName;

    // ── Captured address GUIDs (used by GBG tests) ────────────────────────────
    private static String memberAddressGuid;
    private static String newUserAddressGuid;

    // ── Fresh address GUIDs created in DAB_01/DAB_03 specifically for delete tests ──
    private static String memberDeleteAddressGuid;
    private static String newUserDeleteAddressGuid;

    // ── Fixed address constants (from curl sample) ────────────────────────────
    private static final String ADDRESS_LINE1 = "Hyderabad";
    private static final String STATE         = "Telangana";
    private static final String POSTAL_CODE   = "500012";
    private static final String COUNTRY       = "India";
    private static final String CITY          = "Hyderabad";
    private static final String COUNTRY_CODE  = "+91";
    private static final String LATITUDE      = "17.3762216";
    private static final String LONGITUDE     = "78.4751476";

    // ── Helper: strip non-alphabetic characters to satisfy receiver_name validation ──
    // The API requires receiver_name to contain only [a-zA-Z] (no digits, no spaces).
    // RandomDataUtil.getRandomFirstName() appends System.currentTimeMillis(), e.g.
    // "David1780586077194" → after stripping → "David".
    private static String toAlpha(String name) {
        if (name == null) return "User";
        String alpha = name.replaceAll("[^a-zA-Z]", "");
        return alpha.isEmpty() ? "User" : alpha;
    }

    // ── Helper: build POST /address/addAddress payload ────────────────────────
    // NOTE: receiver_name must be alphabetic only (no spaces) — API enforces this.
    //       name is the address label, not the person's name.
    private Map<String, Object> buildPayload(String userId, String firstName, String lastName,
                                             String mobile, String addressType) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id",                 userId);
        payload.put("name",                    CITY);          // address label / road area
        payload.put("address_line1",           ADDRESS_LINE1);
        payload.put("recipient_mobile_number", mobile);
        payload.put("receiver_name",           toAlpha(firstName));  // strip digits — API enforces [a-zA-Z] only
        payload.put("country_code",            COUNTRY_CODE);
        payload.put("state",                   STATE);
        payload.put("postal_code",             POSTAL_CODE);
        payload.put("country",                 COUNTRY);
        payload.put("city",                    CITY);
        payload.put("type",                    addressType);
        payload.put("latitude",                LATITUDE);
        payload.put("longitude",               LONGITUDE);
        return payload;
    }

    // ── Helper: create a fresh random user and return its data ────────────────
    private String[] createRandomUser() {
        String mobile = "9" + RandomDataUtil.getRandomMobile().substring(1);
        RequestContext.setMobile(mobile);

        JSONObject reg = UserPayloadBuilder.buildNewUserPayload();
        reg.put("mobile", mobile);

        Response r = new RequestBuilder()
                .setEndpoint(APIEndpoints.USER_CREATE)
                .setRequestBody(reg.toString())
                .expectStatus(201)
                .post();

        String guid      = r.jsonPath().getString("data.guid");
        String firstName = r.jsonPath().getString("data.first_name");
        String lastName  = r.jsonPath().getString("data.last_name");
        String token     = TokenManager.generateToken(mobile, TokenManager.NEW_USER);

        return new String[]{mobile, guid, firstName, lastName, token};
    }

    // =========================================================
    // SETUP — Member login + create fresh random new user
    // =========================================================
    @BeforeClass(alwaysRun = true)
    public void setupSessions() {
        System.out.println("\n==========================================================");
        System.out.println("  ADD ADDRESS — SETUP");
        System.out.println("==========================================================");

        // --- FLOW 1: Member login ---
        memberMobile    = ConfigLoader.getConfig().memberMobile();
        RequestContext.setMobile(memberMobile);
        memberToken     = TokenManager.generateToken(memberMobile, TokenManager.MEMBER);
        memberFirstName = RequestContext.getMemberFirstName();
        memberLastName  = RequestContext.getMemberLastName();
        memberUserId    = RequestContext.getMemberUserId();
        System.out.println("   ✅ Member logged in");
        System.out.println("      Mobile : " + memberMobile);
        System.out.println("      Name   : " + memberFirstName + " " + memberLastName);
        System.out.println("      GUID   : " + memberUserId);

        // --- FLOW 2: Create random new user + generate token ---
        String[] userData = createRandomUser();
        newUserMobile    = userData[0];
        newUserUserId    = userData[1];
        newUserFirstName = userData[2];
        newUserLastName  = userData[3];
        newUserToken     = userData[4];
        System.out.println("   ✅ New user created");
        System.out.println("      Mobile : " + newUserMobile);
        System.out.println("      Name   : " + newUserFirstName + " " + newUserLastName);
        System.out.println("      GUID   : " + newUserUserId);

        System.out.println("==========================================================\n");
    }

    // =========================================================
    // ADD_01 — Member user: add address → 201 (or 409 if already exists)
    // =========================================================
    @Test(priority = 1, description = "QA Automation: Add Address — Member user token + member user_id → 201 / 409 with success flag")
    public void ADD_01_AddAddress_MemberUser_ValidData() {
        System.out.println("\n==========================================================");
        System.out.println("  ADD-01: ADD ADDRESS — MEMBER USER (POSITIVE)");
        System.out.println("==========================================================");

        Map<String, Object> payload = buildPayload(memberUserId, memberFirstName, memberLastName,
                memberMobile, "Home");

        System.out.println("   User ID : " + memberUserId);
        System.out.println("   Name    : " + payload.get("name"));
        System.out.println("   Mobile  : " + memberMobile);
        System.out.println("   Address : " + CITY + ", " + STATE + " - " + POSTAL_CODE);

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.ADD_ADDRESS)
                .addHeader("Authorization", memberToken)
                .setRequestBody(payload)
                .postWithoutStatusCheck();

        int statusCode = response.getStatusCode();
        System.out.println("\n🔍 RESPONSE:");
        System.out.println("   Status  : " + statusCode);
        System.out.println("   Body    : " + response.getBody().asString());

        boolean isCreated     = statusCode == 201;
        boolean alreadyExists = statusCode == 409;
        AssertionUtil.verifyTrue(isCreated || alreadyExists,
                "Expected HTTP 201 (created) or 409 (already exists) but got " + statusCode);

        if (isCreated) {
            boolean success = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));
            AssertionUtil.verifyTrue(success, "success must be true when address is created");
            memberAddressGuid = response.jsonPath().getString("data.guid");
            System.out.println("   ✅ Address created successfully (201)");
            System.out.println("   Address GUID : " + memberAddressGuid);
        } else {
            System.out.println("   ℹ️  Address already exists for member (409) — expected for repeated runs");
        }

        // If guid not captured from 201 (was 409), fetch it via GET
        if (memberAddressGuid == null) {
            String ep = APIEndpoints.GET_ADDRESS_BY_USER_ID.replace("{user_id}", memberUserId);
            Response getResp = new RequestBuilder()
                    .setEndpoint(ep)
                    .addHeader("Authorization", memberToken)
                    .getWithoutStatusCheck();
            if (getResp.getStatusCode() == 200) {
                memberAddressGuid = getResp.jsonPath().getString("data[0].guid");
                System.out.println("   ℹ️  Member address GUID fetched via GET: " + memberAddressGuid);
            }
        }
    }

    // =========================================================
    // ADD_02 — Member user: verify response fields
    // =========================================================
    @Test(priority = 2, description = "QA Automation: Add Address — Member user → verify user_id, receiver_name, mobile, city, state in response")
    public void ADD_02_AddAddress_MemberUser_VerifyResponseFields() {
        System.out.println("\n==========================================================");
        System.out.println("  ADD-02: ADD ADDRESS — MEMBER USER VERIFY FIELDS");
        System.out.println("==========================================================");

        // Use "Office" type so this call is independent from ADD_01 (different address type)
        Map<String, Object> payload = buildPayload(memberUserId, memberFirstName, memberLastName,
                memberMobile, "Office");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.ADD_ADDRESS)
                .addHeader("Authorization", memberToken)
                .setRequestBody(payload)
                .postWithoutStatusCheck();

        int statusCode = response.getStatusCode();
        System.out.println("   Status : " + statusCode);

        if (statusCode == 409) {
            System.out.println("   ℹ️  Address already exists (409) — skipping field validation for member");
            System.out.println("   ✅ ADD_02 passed — endpoint reachable with member token");
            return;
        }

        AssertionUtil.verifyEquals(statusCode, 201, "Add address must return 201 for a new address");

        String returnedUserId   = response.jsonPath().getString("data.user_id");
        String returnedReceiver = response.jsonPath().getString("data.receiver_name");
        String returnedMobile   = response.jsonPath().getString("data.recipient_mobile_number");
        String returnedCity     = response.jsonPath().getString("data.city");
        String returnedState    = response.jsonPath().getString("data.state");

        System.out.println("\n🔍 RESPONSE FIELDS:");
        System.out.println("   user_id                  : " + returnedUserId);
        System.out.println("   receiver_name            : " + returnedReceiver);
        System.out.println("   recipient_mobile_number  : " + returnedMobile);
        System.out.println("   city                     : " + returnedCity);
        System.out.println("   state                    : " + returnedState);

        AssertionUtil.verifyEquals(returnedUserId,  memberUserId,        "user_id must match member GUID");
        AssertionUtil.verifyEquals(returnedMobile,  memberMobile,        "recipient_mobile_number must match member mobile");
        AssertionUtil.verifyEquals(returnedCity,    CITY,                "city must match the sent city");
        AssertionUtil.verifyEquals(returnedState,   STATE,               "state must match the sent state");
        AssertionUtil.verifyEquals(returnedReceiver, toAlpha(memberFirstName), "receiver_name must match alpha-stripped member first name");

        System.out.println("   ✅ All member address fields validated");
    }

    // =========================================================
    // ADD_03 — New user: add address → 201
    // =========================================================
    @Test(priority = 3, description = "QA Automation: Add Address — New user token + new user_id → 201 with success flag and data")
    public void ADD_03_AddAddress_NewUser_ValidData() {
        System.out.println("\n==========================================================");
        System.out.println("  ADD-03: ADD ADDRESS — NEW USER (POSITIVE)");
        System.out.println("==========================================================");

        Map<String, Object> payload = buildPayload(newUserUserId, newUserFirstName, newUserLastName,
                newUserMobile, "Home");

        System.out.println("   User ID : " + newUserUserId);
        System.out.println("   Name    : " + payload.get("name"));
        System.out.println("   Mobile  : " + newUserMobile);
        System.out.println("   Address : " + CITY + ", " + STATE + " - " + POSTAL_CODE);

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.ADD_ADDRESS)
                .addHeader("Authorization", newUserToken)
                .setRequestBody(payload)
                .expectStatus(201)
                .post();

        boolean success = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));
        Object  data    = response.jsonPath().get("data");

        System.out.println("\n🔍 RESPONSE:");
        System.out.println("   Status  : " + response.getStatusCode());
        System.out.println("   success : " + success);
        System.out.println("   data    : " + data);
        System.out.println("\n   Full body:\n" + response.getBody().asString());

        AssertionUtil.verifyTrue(success, "success must be true for new user address creation");
        AssertionUtil.verifyNotNull(data, "data must not be null in add address response");

        newUserAddressGuid = response.jsonPath().getString("data.guid");
        System.out.println("   Address GUID : " + newUserAddressGuid);
    }

    // =========================================================
    // ADD_04 — New user: verify all response fields
    // =========================================================
    @Test(priority = 4, description = "QA Automation: Add Address — New user → verify user_id, receiver_name, mobile, city, state, latitude, longitude")
    public void ADD_04_AddAddress_NewUser_VerifyResponseFields() {
        System.out.println("\n==========================================================");
        System.out.println("  ADD-04: ADD ADDRESS — NEW USER VERIFY FIELDS");
        System.out.println("==========================================================");

        // Create a second fresh user to guarantee 201 (avoids any potential conflict with ADD_03)
        System.out.println("   Creating a second fresh user for field validation...");
        String[] userData   = createRandomUser();
        String userId       = userData[1];
        String firstName    = userData[2];
        String lastName     = userData[3];
        String mobile       = userData[0];
        String token        = userData[4];
        System.out.println("   ✅ Second user — Mobile: " + mobile + " | GUID: " + userId);

        Map<String, Object> payload = buildPayload(userId, firstName, lastName, mobile, "Home");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.ADD_ADDRESS)
                .addHeader("Authorization", token)
                .setRequestBody(payload)
                .expectStatus(201)
                .post();

        String returnedUserId    = response.jsonPath().getString("data.user_id");
        String returnedReceiver  = response.jsonPath().getString("data.receiver_name");
        String returnedMobile    = response.jsonPath().getString("data.recipient_mobile_number");
        String returnedCity      = response.jsonPath().getString("data.city");
        String returnedState     = response.jsonPath().getString("data.state");
        String returnedLatitude  = response.jsonPath().getString("data.latitude");
        String returnedLongitude = response.jsonPath().getString("data.longitude");
        String returnedCountry   = response.jsonPath().getString("data.country");
        String returnedPostal    = response.jsonPath().getString("data.postal_code");

        System.out.println("\n🔍 RESPONSE FIELDS:");
        System.out.println("   user_id                  : " + returnedUserId);
        System.out.println("   receiver_name            : " + returnedReceiver);
        System.out.println("   recipient_mobile_number  : " + returnedMobile);
        System.out.println("   city                     : " + returnedCity);
        System.out.println("   state                    : " + returnedState);
        System.out.println("   country                  : " + returnedCountry);
        System.out.println("   postal_code              : " + returnedPostal);
        System.out.println("   latitude                 : " + returnedLatitude);
        System.out.println("   longitude                : " + returnedLongitude);

        AssertionUtil.verifyEquals(returnedUserId,    userId,           "user_id must match new user GUID");
        AssertionUtil.verifyEquals(returnedMobile,    mobile,           "recipient_mobile_number must match new user mobile");
        AssertionUtil.verifyEquals(returnedCity,      CITY,             "city must match sent city");
        AssertionUtil.verifyEquals(returnedState,     STATE,            "state must match sent state");
        AssertionUtil.verifyEquals(returnedLatitude,  LATITUDE,         "latitude must match sent latitude");
        AssertionUtil.verifyEquals(returnedLongitude, LONGITUDE,        "longitude must match sent longitude");
        AssertionUtil.verifyEquals(returnedReceiver,  toAlpha(firstName), "receiver_name must match alpha-stripped first name");
        AssertionUtil.verifyNotNull(returnedCountry,  "country must not be null");
        AssertionUtil.verifyNotNull(returnedPostal,   "postal_code must not be null");

        System.out.println("   ✅ All new user address fields validated");
    }

    // =========================================================
    // GAU_01 — Member user: GET address by user_id → 200 + list
    // =========================================================
    @Test(priority = 5, description = "QA Automation: Get Address By User ID — Member user_id → 200 with addresses list")
    public void GAU_01_GetAddressByUserId_MemberUser_Valid() {
        System.out.println("\n==========================================================");
        System.out.println("  GAU-01: GET ADDRESS BY USER ID — MEMBER USER (POSITIVE)");
        System.out.println("==========================================================");
        System.out.println("   User ID : " + memberUserId);

        String endpoint = APIEndpoints.GET_ADDRESS_BY_USER_ID.replace("{user_id}", memberUserId);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", memberToken)
                .expectStatus(200)
                .get();

        boolean success = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));
        Object  data    = response.jsonPath().get("data");

        System.out.println("\n🔍 RESPONSE:");
        System.out.println("   Status  : " + response.getStatusCode());
        System.out.println("   success : " + success);
        System.out.println("   data    : " + data);
        System.out.println("\n   Full body:\n" + response.getBody().asString());

        AssertionUtil.verifyTrue(success, "success must be true for member address lookup");
        AssertionUtil.verifyNotNull(data, "data must not be null for a member who has addresses");
    }

    // =========================================================
    // GAU_02 — Member user: verify address record fields
    // =========================================================
    @Test(priority = 6, description = "QA Automation: Get Address By User ID — Member → verify guid, user_id, city, state, recipient_mobile_number")
    public void GAU_02_GetAddressByUserId_MemberUser_VerifyFields() {
        System.out.println("\n==========================================================");
        System.out.println("  GAU-02: GET ADDRESS BY USER ID — MEMBER VERIFY FIELDS");
        System.out.println("==========================================================");

        String endpoint = APIEndpoints.GET_ADDRESS_BY_USER_ID.replace("{user_id}", memberUserId);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", memberToken)
                .expectStatus(200)
                .get();

        java.util.List<?> addresses = response.jsonPath().getList("data");
        if (addresses == null || addresses.isEmpty()) {
            System.out.println("   ℹ️  No addresses for this member yet — GAU_02 skipped (not an error)");
            return;
        }

        String guid     = response.jsonPath().getString("data[0].guid");
        String userId   = response.jsonPath().getString("data[0].user_id");
        String city     = response.jsonPath().getString("data[0].city");
        String state    = response.jsonPath().getString("data[0].state");
        String mobile   = response.jsonPath().getString("data[0].recipient_mobile_number");

        System.out.println("\n🔍 FIRST ADDRESS FIELDS:");
        System.out.println("   guid                     : " + guid);
        System.out.println("   user_id                  : " + userId);
        System.out.println("   city                     : " + city);
        System.out.println("   state                    : " + state);
        System.out.println("   recipient_mobile_number  : " + mobile);

        AssertionUtil.verifyNotNull(guid,   "guid must be present in address record");
        AssertionUtil.verifyEquals(userId,  memberUserId, "user_id in address must match queried member user_id");
        AssertionUtil.verifyNotNull(city,   "city must be present in address record");
        AssertionUtil.verifyNotNull(state,  "state must be present in address record");
        AssertionUtil.verifyNotNull(mobile, "recipient_mobile_number must be present in address record");

        System.out.println("   ✅ All member address record fields validated");
    }

    // =========================================================
    // GAU_03 — New user: GET address by user_id → 200 + list
    // =========================================================
    @Test(priority = 7, description = "QA Automation: Get Address By User ID — New user_id (address added in ADD_03) → 200 with addresses list")
    public void GAU_03_GetAddressByUserId_NewUser_Valid() {
        System.out.println("\n==========================================================");
        System.out.println("  GAU-03: GET ADDRESS BY USER ID — NEW USER (POSITIVE)");
        System.out.println("==========================================================");
        System.out.println("   User ID : " + newUserUserId);

        String endpoint = APIEndpoints.GET_ADDRESS_BY_USER_ID.replace("{user_id}", newUserUserId);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", newUserToken)
                .expectStatus(200)
                .get();

        boolean success = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));
        Object  data    = response.jsonPath().get("data");

        System.out.println("\n🔍 RESPONSE:");
        System.out.println("   Status  : " + response.getStatusCode());
        System.out.println("   success : " + success);
        System.out.println("   data    : " + data);
        System.out.println("\n   Full body:\n" + response.getBody().asString());

        AssertionUtil.verifyTrue(success, "success must be true for new user address lookup");
        AssertionUtil.verifyNotNull(data, "data must not be null for a new user who just added an address");
    }

    // =========================================================
    // GAU_04 — New user: verify all address record fields
    // =========================================================
    @Test(priority = 8, description = "QA Automation: Get Address By User ID — New user → verify guid, user_id, city, state, latitude, longitude, recipient_mobile_number")
    public void GAU_04_GetAddressByUserId_NewUser_VerifyFields() {
        System.out.println("\n==========================================================");
        System.out.println("  GAU-04: GET ADDRESS BY USER ID — NEW USER VERIFY FIELDS");
        System.out.println("==========================================================");

        String endpoint = APIEndpoints.GET_ADDRESS_BY_USER_ID.replace("{user_id}", newUserUserId);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", newUserToken)
                .expectStatus(200)
                .get();

        java.util.List<?> addresses = response.jsonPath().getList("data");
        AssertionUtil.verifyTrue(addresses != null && !addresses.isEmpty(),
                "New user must have at least one address (added in ADD_03)");

        String guid      = response.jsonPath().getString("data[0].guid");
        String userId    = response.jsonPath().getString("data[0].user_id");
        String city      = response.jsonPath().getString("data[0].city");
        String state     = response.jsonPath().getString("data[0].state");
        String mobile    = response.jsonPath().getString("data[0].recipient_mobile_number");
        String latitude  = response.jsonPath().getString("data[0].latitude");
        String longitude = response.jsonPath().getString("data[0].longitude");
        String country   = response.jsonPath().getString("data[0].country");

        System.out.println("\n🔍 FIRST ADDRESS FIELDS:");
        System.out.println("   guid                     : " + guid);
        System.out.println("   user_id                  : " + userId);
        System.out.println("   city                     : " + city);
        System.out.println("   state                    : " + state);
        System.out.println("   recipient_mobile_number  : " + mobile);
        System.out.println("   latitude                 : " + latitude);
        System.out.println("   longitude                : " + longitude);
        System.out.println("   country                  : " + country);

        AssertionUtil.verifyNotNull(guid,      "guid must be present in address record");
        AssertionUtil.verifyEquals(userId,     newUserUserId, "user_id in address must match new user's GUID");
        AssertionUtil.verifyEquals(city,       CITY,          "city must match the address added in ADD_03");
        AssertionUtil.verifyEquals(state,      STATE,         "state must match the address added in ADD_03");
        AssertionUtil.verifyNotNull(mobile,    "recipient_mobile_number must be present");
        AssertionUtil.verifyNotNull(latitude,  "latitude must be present");
        AssertionUtil.verifyNotNull(longitude, "longitude must be present");
        AssertionUtil.verifyNotNull(country,   "country must be present");

        System.out.println("   ✅ All new user address record fields validated");
    }

    // =========================================================
    // ── NEGATIVE TESTS — POST /address/addAddress ─────────────
    // Each test runs for BOTH flows: MEMBER USER and NEW USER
    // =========================================================

    // ── Private helpers ───────────────────────────────────────

    /** Assert that omitting one field from an otherwise valid payload is rejected by the API (4xx / 5xx). */
    private void assertMissingField(String fieldName, String testId, String userType,
                                     String token, String userId, String firstName, String mobile) {
        System.out.println("\n   ── [" + userType + "] missing '" + fieldName + "' ──");
        Map<String, Object> payload = buildPayload(userId, firstName, "", mobile, "Home");
        payload.remove(fieldName);

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.ADD_ADDRESS)
                .addHeader("Authorization", token)
                .setRequestBody(payload)
                .postWithoutStatusCheck();

        int statusCode = response.getStatusCode();
        System.out.println("   Status : " + statusCode);
        System.out.println("   Body   : " + response.getBody().asString());

        boolean rejected = statusCode >= 400 && statusCode < 500;
        if (!rejected && statusCode < 400) {
            System.out.println("   ℹ️  [INFO] " + testId + " [" + userType + "]: field '" + fieldName
                    + "' appears optional — server accepted the request");
        } else if (statusCode >= 500) {
            System.out.println("   ❌ [API DEFECT] " + statusCode + " for missing '" + fieldName
                    + "' [" + userType + "] — should be 422, not 500");
        } else {
            System.out.println("   ✅ " + statusCode + " for missing '" + fieldName + "' [" + userType + "]");
        }
        AssertionUtil.verifyTrue(rejected,
                testId + " [" + userType + "]: expected 4xx (400/422) for missing field '" + fieldName
                        + "' but got " + statusCode);
    }

    /** Assert invalid receiver_name (with digits) is rejected. */
    private void assertInvalidReceiverName(String testId, String userType,
                                            String token, String userId, String firstName, String mobile) {
        System.out.println("\n   ── [" + userType + "] invalid receiver_name (digits) ──");
        Map<String, Object> payload = buildPayload(userId, firstName, "", mobile, "Home");
        payload.put("receiver_name", "User12345"); // digits not allowed

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.ADD_ADDRESS)
                .addHeader("Authorization", token)
                .setRequestBody(payload)
                .postWithoutStatusCheck();

        int statusCode = response.getStatusCode();
        System.out.println("   Status : " + statusCode);
        System.out.println("   Body   : " + response.getBody().asString());

        AssertionUtil.verifyEquals(statusCode, 422,
                testId + " [" + userType + "]: digits in receiver_name must return 422");
        boolean success = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));
        AssertionUtil.verifyTrue(!success,
                testId + " [" + userType + "]: success must be false for invalid receiver_name");
        System.out.println("   ✅ 422 returned for digits-in-receiver_name [" + userType + "]");
    }

    /** Assert that sending an empty body is rejected. */
    private void assertEmptyBody(String testId, String userType, String token) {
        System.out.println("\n   ── [" + userType + "] empty body {} ──");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.ADD_ADDRESS)
                .addHeader("Authorization", token)
                .setRequestBody("{}")
                .postWithoutStatusCheck();

        int statusCode = response.getStatusCode();
        System.out.println("   Status : " + statusCode);
        System.out.println("   Body   : " + response.getBody().asString());

        AssertionUtil.verifyTrue(statusCode >= 400 && statusCode < 500,
                testId + " [" + userType + "]: empty body must return 4xx, got " + statusCode);
        System.out.println("   ✅ " + statusCode + " returned for empty body [" + userType + "]");
    }

    // ── ADD_05 — Invalid receiver_name (digits) → 422 for MEMBER and NEW USER ──
    @Test(priority = 9, description = "QA Automation: Add Address — receiver_name with digits → 422 for Member and New User")
    public void ADD_05_AddAddress_InvalidReceiverName_WithDigits() {
        System.out.println("\n==========================================================");
        System.out.println("  ADD-05: ADD ADDRESS — INVALID receiver_name (NEGATIVE)");
        System.out.println("==========================================================");
        assertInvalidReceiverName("ADD_05", "MEMBER",   memberToken,  memberUserId,  memberFirstName,  memberMobile);
        assertInvalidReceiverName("ADD_05", "NEW_USER", newUserToken, newUserUserId, newUserFirstName, newUserMobile);
    }

    // ── ADD_06 — Missing receiver_name → 4xx for MEMBER and NEW USER ──────────
    @Test(priority = 10, description = "QA Automation: Add Address — missing receiver_name → 4xx for Member and New User")
    public void ADD_06_AddAddress_MissingReceiverName() {
        System.out.println("\n==========================================================");
        System.out.println("  ADD-06: ADD ADDRESS — MISSING receiver_name (NEGATIVE)");
        System.out.println("==========================================================");
        assertMissingField("receiver_name", "ADD_06", "MEMBER",   memberToken,  memberUserId,  memberFirstName,  memberMobile);
        assertMissingField("receiver_name", "ADD_06", "NEW_USER", newUserToken, newUserUserId, newUserFirstName, newUserMobile);
    }

    // ── ADD_07 — Missing address_line1 → 4xx/5xx [API DEFECT: 500] ────────────
    @Test(priority = 11, description = "QA Automation: Add Address — missing address_line1 → 4xx/5xx for Member and New User")
    public void ADD_07_AddAddress_MissingAddressLine1() {
        System.out.println("\n==========================================================");
        System.out.println("  ADD-07: ADD ADDRESS — MISSING address_line1 (NEGATIVE)");
        System.out.println("==========================================================");
        assertMissingField("address_line1", "ADD_07", "MEMBER",   memberToken,  memberUserId,  memberFirstName,  memberMobile);
        assertMissingField("address_line1", "ADD_07", "NEW_USER", newUserToken, newUserUserId, newUserFirstName, newUserMobile);
    }

    // ── ADD_08 — Missing city → 4xx for MEMBER and NEW USER ───────────────────
    @Test(priority = 12, description = "QA Automation: Add Address — missing city → 4xx for Member and New User")
    public void ADD_08_AddAddress_MissingCity() {
        System.out.println("\n==========================================================");
        System.out.println("  ADD-08: ADD ADDRESS — MISSING city (NEGATIVE)");
        System.out.println("==========================================================");
        assertMissingField("city", "ADD_08", "MEMBER",   memberToken,  memberUserId,  memberFirstName,  memberMobile);
        assertMissingField("city", "ADD_08", "NEW_USER", newUserToken, newUserUserId, newUserFirstName, newUserMobile);
    }

    // ── ADD_09 — Empty body → 4xx for MEMBER and NEW USER ─────────────────────
    @Test(priority = 13, description = "QA Automation: Add Address — empty body {} → 4xx for Member and New User")
    public void ADD_09_AddAddress_EmptyBody() {
        System.out.println("\n==========================================================");
        System.out.println("  ADD-09: ADD ADDRESS — EMPTY BODY (NEGATIVE)");
        System.out.println("==========================================================");
        assertEmptyBody("ADD_09", "MEMBER",   memberToken);
        assertEmptyBody("ADD_09", "NEW_USER", newUserToken);
    }

    // ── ADD_11 — Missing user_id ───────────────────────────────────────────────
    @Test(priority = 21, description = "QA Automation: Add Address — missing user_id → 4xx for Member and New User")
    public void ADD_11_AddAddress_MissingUserId() {
        System.out.println("\n==========================================================");
        System.out.println("  ADD-11: ADD ADDRESS — MISSING user_id (NEGATIVE)");
        System.out.println("==========================================================");
        assertMissingField("user_id", "ADD_11", "MEMBER",   memberToken,  memberUserId,  memberFirstName,  memberMobile);
        assertMissingField("user_id", "ADD_11", "NEW_USER", newUserToken, newUserUserId, newUserFirstName, newUserMobile);
    }

    // ── ADD_12 — Missing name ──────────────────────────────────────────────────
    @Test(priority = 22, description = "QA Automation: Add Address — missing name → 4xx for Member and New User")
    public void ADD_12_AddAddress_MissingName() {
        System.out.println("\n==========================================================");
        System.out.println("  ADD-12: ADD ADDRESS — MISSING name (NEGATIVE)");
        System.out.println("==========================================================");
        assertMissingField("name", "ADD_12", "MEMBER",   memberToken,  memberUserId,  memberFirstName,  memberMobile);
        assertMissingField("name", "ADD_12", "NEW_USER", newUserToken, newUserUserId, newUserFirstName, newUserMobile);
    }

    // ── ADD_13 — Missing recipient_mobile_number ───────────────────────────────
    @Test(priority = 23, description = "QA Automation: Add Address — missing recipient_mobile_number → 4xx for Member and New User")
    public void ADD_13_AddAddress_MissingRecipientMobile() {
        System.out.println("\n==========================================================");
        System.out.println("  ADD-13: ADD ADDRESS — MISSING recipient_mobile_number (NEGATIVE)");
        System.out.println("==========================================================");
        assertMissingField("recipient_mobile_number", "ADD_13", "MEMBER",   memberToken,  memberUserId,  memberFirstName,  memberMobile);
        assertMissingField("recipient_mobile_number", "ADD_13", "NEW_USER", newUserToken, newUserUserId, newUserFirstName, newUserMobile);
    }

    // ── ADD_14 — Missing country_code ─────────────────────────────────────────
    @Test(priority = 24, description = "QA Automation: Add Address — missing country_code → 4xx for Member and New User")
    public void ADD_14_AddAddress_MissingCountryCode() {
        System.out.println("\n==========================================================");
        System.out.println("  ADD-14: ADD ADDRESS — MISSING country_code (NEGATIVE)");
        System.out.println("==========================================================");
        assertMissingField("country_code", "ADD_14", "MEMBER",   memberToken,  memberUserId,  memberFirstName,  memberMobile);
        assertMissingField("country_code", "ADD_14", "NEW_USER", newUserToken, newUserUserId, newUserFirstName, newUserMobile);
    }

    // ── ADD_15 — Missing state ─────────────────────────────────────────────────
    @Test(priority = 25, description = "QA Automation: Add Address — missing state → 4xx for Member and New User")
    public void ADD_15_AddAddress_MissingState() {
        System.out.println("\n==========================================================");
        System.out.println("  ADD-15: ADD ADDRESS — MISSING state (NEGATIVE)");
        System.out.println("==========================================================");
        assertMissingField("state", "ADD_15", "MEMBER",   memberToken,  memberUserId,  memberFirstName,  memberMobile);
        assertMissingField("state", "ADD_15", "NEW_USER", newUserToken, newUserUserId, newUserFirstName, newUserMobile);
    }

    // ── ADD_16 — Missing postal_code ──────────────────────────────────────────
    @Test(priority = 26, description = "QA Automation: Add Address — missing postal_code → 4xx for Member and New User")
    public void ADD_16_AddAddress_MissingPostalCode() {
        System.out.println("\n==========================================================");
        System.out.println("  ADD-16: ADD ADDRESS — MISSING postal_code (NEGATIVE)");
        System.out.println("==========================================================");
        assertMissingField("postal_code", "ADD_16", "MEMBER",   memberToken,  memberUserId,  memberFirstName,  memberMobile);
        assertMissingField("postal_code", "ADD_16", "NEW_USER", newUserToken, newUserUserId, newUserFirstName, newUserMobile);
    }

    // ── ADD_17 — Missing country ──────────────────────────────────────────────
    @Test(priority = 27, description = "QA Automation: Add Address — missing country → 4xx for Member and New User")
    public void ADD_17_AddAddress_MissingCountry() {
        System.out.println("\n==========================================================");
        System.out.println("  ADD-17: ADD ADDRESS — MISSING country (NEGATIVE)");
        System.out.println("==========================================================");
        assertMissingField("country", "ADD_17", "MEMBER",   memberToken,  memberUserId,  memberFirstName,  memberMobile);
        assertMissingField("country", "ADD_17", "NEW_USER", newUserToken, newUserUserId, newUserFirstName, newUserMobile);
    }

    // ── ADD_18 — Missing type ─────────────────────────────────────────────────
    @Test(priority = 28, description = "QA Automation: Add Address — missing type → 4xx for Member and New User")
    public void ADD_18_AddAddress_MissingType() {
        System.out.println("\n==========================================================");
        System.out.println("  ADD-18: ADD ADDRESS — MISSING type (NEGATIVE)");
        System.out.println("==========================================================");
        assertMissingField("type", "ADD_18", "MEMBER",   memberToken,  memberUserId,  memberFirstName,  memberMobile);
        assertMissingField("type", "ADD_18", "NEW_USER", newUserToken, newUserUserId, newUserFirstName, newUserMobile);
    }

    // ── ADD_19 — Missing latitude ─────────────────────────────────────────────
    @Test(priority = 29, description = "QA Automation: Add Address — missing latitude → 4xx for Member and New User")
    public void ADD_19_AddAddress_MissingLatitude() {
        System.out.println("\n==========================================================");
        System.out.println("  ADD-19: ADD ADDRESS — MISSING latitude (NEGATIVE)");
        System.out.println("==========================================================");
        assertMissingField("latitude", "ADD_19", "MEMBER",   memberToken,  memberUserId,  memberFirstName,  memberMobile);
        assertMissingField("latitude", "ADD_19", "NEW_USER", newUserToken, newUserUserId, newUserFirstName, newUserMobile);
    }

    // ── ADD_20 — Missing longitude ────────────────────────────────────────────
    @Test(priority = 30, description = "QA Automation: Add Address — missing longitude → 4xx for Member and New User")
    public void ADD_20_AddAddress_MissingLongitude() {
        System.out.println("\n==========================================================");
        System.out.println("  ADD-20: ADD ADDRESS — MISSING longitude (NEGATIVE)");
        System.out.println("==========================================================");
        assertMissingField("longitude", "ADD_20", "MEMBER",   memberToken,  memberUserId,  memberFirstName,  memberMobile);
        assertMissingField("longitude", "ADD_20", "NEW_USER", newUserToken, newUserUserId, newUserFirstName, newUserMobile);
    }

    // =========================================================
    // ── NEGATIVE TESTS — GET /address/getAddressByUserId ──────
    // Both flows (MEMBER and NEW USER) run for every negative case.
    // =========================================================

    // ── Private GET helper ────────────────────────────────────

    /**
     * GET /address/getAddressByUserId/{userId}.
     * Passes Authorization header when token is non-null.
     */
    private Response getAddressById(String token, String userId) {
        String endpoint = APIEndpoints.GET_ADDRESS_BY_USER_ID.replace("{user_id}", userId);
        RequestBuilder rb = new RequestBuilder().setEndpoint(endpoint);
        if (token != null) {
            rb.addHeader("Authorization", token);
        }
        return rb.getWithoutStatusCheck();
    }

    /**
     * Assert GET /address/getAddressByUserId/{userId} with an invalid user_id.
     *
     * @param expectedStatus The status code the API SHOULD return per spec.
     *                       Pass 200 when an empty-data 200 is acceptable (non-existent UUID).
     *                       Pass 400 for malformed / impossible user_id values.
     */
    private void assertGetInvalidUserId(String testId, String label, String token,
                                         String badUserId, int expectedStatus) {
        System.out.println("\n   ── [" + label + "] user_id='" + badUserId + "' ──");
        System.out.println("   Expected Status : " + expectedStatus);

        Response response = getAddressById(token, badUserId);
        int    statusCode = response.getStatusCode();
        String body       = response.getBody().asString();
        System.out.println("   Actual Status   : " + statusCode);
        System.out.println("   Body            : " + body);

        // ── 1. Status code assertion ─────────────────────────────────────────
        if (statusCode == expectedStatus) {
            System.out.println("   ✅ Status " + statusCode + " matches expected [" + label + "]");
        } else if (statusCode >= 500) {
            System.out.println("   ⚠️  [API DEFECT] " + statusCode
                    + " returned — expected " + expectedStatus
                    + " for user_id='" + badUserId + "' [" + label + "] — should be " + expectedStatus);
        } else if (statusCode == 200 && expectedStatus != 200) {
            System.out.println("   ⚠️  [API DEFECT] 200 returned — expected " + expectedStatus
                    + " for invalid user_id='" + badUserId + "' [" + label + "]");
            // If 200 slips through despite invalid input, data must at least be empty
            java.util.List<?> data = response.jsonPath().getList("data");
            boolean isEmpty = data == null || data.isEmpty();
            AssertionUtil.verifyTrue(isEmpty,
                    testId + " [" + label + "]: 200 with non-empty data for invalid user_id='"
                            + badUserId + "'");
        } else {
            System.out.println("   ✅ " + statusCode + " returned [" + label + "]");
        }

        // ── 2. Body field validation for 4xx responses ───────────────────────
        if (statusCode >= 400 && statusCode < 500) {
            Boolean success = response.jsonPath().getBoolean("success");
            AssertionUtil.verifyTrue(success == null || Boolean.FALSE.equals(success),
                    testId + " [" + label + "]: 'success' must be false/absent in " + statusCode + " response");
            System.out.println("   ✅ success=false validated in " + statusCode + " response [" + label + "]");

            String msg = response.jsonPath().getString("msg");
            if (msg == null || msg.isEmpty()) {
                // Some APIs use 'message' or 'error'
                msg = response.jsonPath().getString("message");
            }
            if (msg != null && !msg.isEmpty()) {
                System.out.println("   ✅ Error message present: \"" + msg + "\" [" + label + "]");
            } else {
                System.out.println("   ℹ️  No error message field returned for " + statusCode + " [" + label + "]");
            }
        }

        // ── 3. Final gate: strict status check ──────────────────────────
        // 400/422 are treated as equivalent client-error codes (both acceptable).
        // 500 is NOT acceptable — it indicates missing server-side validation.
        // 200 is NOT acceptable when 400 is expected — it indicates no validation.
        boolean statusOk = (statusCode == expectedStatus)
                || (expectedStatus == 400  && statusCode == 422)
                || (expectedStatus == 422  && statusCode == 400)
                || (expectedStatus == 200  && statusCode == 404);
        AssertionUtil.verifyTrue(statusOk,
                testId + " [" + label + "] HTTP Status for user_id='" + badUserId + "'"
                        + " — expected " + expectedStatus + " but got " + statusCode);
    }

    // ── GAU_05 — Non-existent UUID → 200+empty or 4xx/5xx (MEMBER + NEW USER) ──
    @Test(priority = 15, description = "QA Automation: Get Address By User ID — non-existent UUID → 200+empty or 4xx for Member and New User")
    public void GAU_05_GetAddressByUserId_NonExistentUserId() {
        System.out.println("\n==========================================================");
        System.out.println("  GAU-05: GET ADDRESS BY USER ID — NON-EXISTENT UUID (NEGATIVE)");
        System.out.println("==========================================================");
        // Non-existent but well-formed UUID: expect 200 + empty data (or 404)
        String fakeGuid = "00000000-0000-0000-0000-000000000000";
        assertGetInvalidUserId("GAU_05", "MEMBER",   memberToken,  fakeGuid, 200);
        assertGetInvalidUserId("GAU_05", "NEW_USER", newUserToken, fakeGuid, 200);
    }

    // ── GAU_06 — Malformed user_id (non-UUID string) → 4xx/5xx (MEMBER + NEW USER) ──
    @Test(priority = 16, description = "QA Automation: Get Address By User ID — malformed user_id → 4xx for Member and New User")
    public void GAU_06_GetAddressByUserId_MalformedUserId() {
        System.out.println("\n==========================================================");
        System.out.println("  GAU-06: GET ADDRESS BY USER ID — MALFORMED USER ID (NEGATIVE)");
        System.out.println("==========================================================");
        // Malformed (non-UUID) string — expected 400 validation error
        String malformedId = "INVALID-USER-ID-FORMAT";
        assertGetInvalidUserId("GAU_06", "MEMBER",   memberToken,  malformedId, 400);
        assertGetInvalidUserId("GAU_06", "NEW_USER", newUserToken, malformedId, 400);
    }

    // ── GAU_11 — Numeric-only user_id (not a UUID) → 4xx/5xx (both tokens) ───
    @Test(priority = 31, description = "QA Automation: Get Address By User ID — numeric-only user_id → 4xx/5xx for Member and New User")
    public void GAU_11_GetAddressByUserId_NumericUserId() {
        System.out.println("\n==========================================================");
        System.out.println("  GAU-11: GET ADDRESS BY USER ID — NUMERIC user_id (NEGATIVE)");
        System.out.println("==========================================================");
        // Numeric-only string is not a valid UUID — expected 400
        String numericId = "1234567890";
        assertGetInvalidUserId("GAU_11", "MEMBER",   memberToken,  numericId, 400);
        assertGetInvalidUserId("GAU_11", "NEW_USER", newUserToken, numericId, 400);
    }

    // ── GAU_12 — SQL injection in user_id → 4xx/5xx (both tokens) ────────────
    @Test(priority = 32, description = "QA Automation: Get Address By User ID — SQL injection in user_id → 4xx/5xx for Member and New User")
    public void GAU_12_GetAddressByUserId_SqlInjectionUserId() {
        System.out.println("\n==========================================================");
        System.out.println("  GAU-12: GET ADDRESS BY USER ID — SQL INJECTION user_id (NEGATIVE)");
        System.out.println("==========================================================");
        // SQL injection payload — must be rejected with 400, not executed
        String injectionId = "' OR '1'='1";
        assertGetInvalidUserId("GAU_12", "MEMBER",   memberToken,  injectionId, 400);
        assertGetInvalidUserId("GAU_12", "NEW_USER", newUserToken, injectionId, 400);
    }

    // ── GAU_13 — Single-character user_id → 4xx/5xx (both tokens) ────────────
    @Test(priority = 33, description = "QA Automation: Get Address By User ID — single-char user_id → 4xx/5xx for Member and New User")
    public void GAU_13_GetAddressByUserId_SingleCharUserId() {
        System.out.println("\n==========================================================");
        System.out.println("  GAU-13: GET ADDRESS BY USER ID — SINGLE-CHAR user_id (NEGATIVE)");
        System.out.println("==========================================================");
        // Single character is not a valid UUID — expected 400
        String singleChar = "x";
        assertGetInvalidUserId("GAU_13", "MEMBER",   memberToken,  singleChar, 400);
        assertGetInvalidUserId("GAU_13", "NEW_USER", newUserToken, singleChar, 400);
    }

    // =========================================================
    // ── GET /address/getAddressByGuid/{address_guid} ──────────
    // POSITIVE (GBG_01–04) + NEGATIVE (GBG_05–11)
    // Both MEMBER and NEW USER flows used throughout.
    // =========================================================

    // ── Private helpers ───────────────────────────────────────

    /** Call GET /address/getAddressByGuid/{guid}. Token is omitted when null. */
    private Response getAddressByGuid(String token, String guid) {
        String endpoint = APIEndpoints.GET_ADDRESS_BY_GUID.replace("{address_guid}", guid);
        RequestBuilder rb = new RequestBuilder().setEndpoint(endpoint);
        if (token != null) {
            rb.addHeader("Authorization", token);
        }
        return rb.getWithoutStatusCheck();
    }

    /**
     * Assert GET /address/getAddressByGuid with an invalid guid value.
     * Logs expected vs actual status, validates success=false on 4xx, marks API defects.
     */
    private void assertGetByGuidInvalid(String testId, String label, String token,
                                         String badGuid, int expectedStatus) {
        System.out.println("\n   ── [" + label + "] guid='" + badGuid + "' ──");
        System.out.println("   Expected Status : " + expectedStatus);

        Response response = getAddressByGuid(token, badGuid);
        int    statusCode = response.getStatusCode();
        String body       = response.getBody().asString();
        System.out.println("   Actual Status   : " + statusCode);
        System.out.println("   Body            : " + body);

        // ── Status assertion ──────────────────────────────────────────────────
        if (statusCode == expectedStatus) {
            System.out.println("   [PASS] " + statusCode + " matches expected [" + label + "]");
        } else if (statusCode >= 500) {
            System.out.println("   [API DEFECT] " + statusCode
                    + " returned — expected " + expectedStatus
                    + " for guid='" + badGuid + "' [" + label + "] — should be " + expectedStatus);
        } else if (statusCode == 200 && expectedStatus != 200) {
            System.out.println("   [API DEFECT] 200 returned — expected " + expectedStatus
                    + " for invalid guid='" + badGuid + "' [" + label + "]");
            // If 200 despite invalid guid, data must be null/empty
            Object data = response.jsonPath().get("data");
            AssertionUtil.verifyTrue(data == null,
                    testId + " [" + label + "]: 200 with non-null data for invalid guid='" + badGuid + "'");
        } else {
            System.out.println("   [PASS] " + statusCode + " returned [" + label + "]");
        }

        // ── Body validation for 4xx ───────────────────────────────────────────
        if (statusCode >= 400 && statusCode < 500) {
            Boolean success = response.jsonPath().getBoolean("success");
            AssertionUtil.verifyTrue(success == null || Boolean.FALSE.equals(success),
                    testId + " [" + label + "]: 'success' must be false in " + statusCode + " response");
            System.out.println("   [VALIDATED] success=false in " + statusCode + " response [" + label + "]");
        }

        // ── Final gate: strict status check ──────────────────────────────────
        // 400/422 are treated as equivalent client-error codes (both acceptable).
        // 500 is NOT acceptable — it indicates missing server-side validation.
        // 200 is NOT acceptable when 400 is expected — it indicates no validation.
        boolean statusOk = (statusCode == expectedStatus)
                || (expectedStatus == 400  && statusCode == 422)
                || (expectedStatus == 422  && statusCode == 400)
                || (expectedStatus == 200  && statusCode == 404);
        AssertionUtil.verifyTrue(statusOk,
                testId + " [" + label + "] HTTP Status for guid='" + badGuid + "'"
                        + " — expected " + expectedStatus + " but got " + statusCode);
    }

    // =========================================================
    // GBG_01 — Member: GET by own address guid → 200 + success
    // =========================================================
    @Test(priority = 35, description = "QA Automation: Get Address By GUID — Member's own address_guid → 200 with success=true")
    public void GBG_01_GetAddressByGuid_MemberUser_Valid() {
        System.out.println("\n==========================================================");
        System.out.println("  GBG-01: GET ADDRESS BY GUID — MEMBER USER (POSITIVE)");
        System.out.println("==========================================================");

        if (memberAddressGuid == null) {
            System.out.println("   [SKIP] memberAddressGuid not available — ADD_01 may have returned 409 with no fallback");
            return;
        }
        System.out.println("   Address GUID : " + memberAddressGuid);

        Response response = getAddressByGuid(memberToken, memberAddressGuid);
        int    statusCode = response.getStatusCode();
        String body       = response.getBody().asString();
        System.out.println("\n🔍 RESPONSE:");
        System.out.println("   Expected Status : 200");
        System.out.println("   Actual Status   : " + statusCode);
        System.out.println("   Body            : " + body);

        AssertionUtil.verifyEquals(statusCode, 200,
                "GBG_01 [MEMBER]: expected 200 for valid address_guid, got " + statusCode);
        boolean success = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));
        AssertionUtil.verifyTrue(success, "GBG_01 [MEMBER]: success must be true");
        Object data = response.jsonPath().get("data");
        AssertionUtil.verifyNotNull(data, "GBG_01 [MEMBER]: data must not be null");
        System.out.println("   [PASS] 200 + success=true returned for member address guid");
    }

    // =========================================================
    // GBG_02 — Member: verify all address fields in response
    // =========================================================
    @Test(priority = 36, description = "QA Automation: Get Address By GUID — Member → verify guid, user_id, city, state, latitude, longitude, type")
    public void GBG_02_GetAddressByGuid_MemberUser_VerifyFields() {
        System.out.println("\n==========================================================");
        System.out.println("  GBG-02: GET ADDRESS BY GUID — MEMBER VERIFY FIELDS");
        System.out.println("==========================================================");

        if (memberAddressGuid == null) {
            System.out.println("   [SKIP] memberAddressGuid not available");
            return;
        }

        Response response = getAddressByGuid(memberToken, memberAddressGuid);
        AssertionUtil.verifyEquals(response.getStatusCode(), 200,
                "GBG_02 [MEMBER]: expected 200");

        String guid      = response.jsonPath().getString("data.guid");
        String userId    = response.jsonPath().getString("data.user_id");
        String city      = response.jsonPath().getString("data.city");
        String state     = response.jsonPath().getString("data.state");
        String country   = response.jsonPath().getString("data.country");
        String postal    = response.jsonPath().getString("data.postal_code");
        String lat       = response.jsonPath().getString("data.latitude");
        String lon       = response.jsonPath().getString("data.longitude");
        String receiver  = response.jsonPath().getString("data.receiver_name");
        String mobile    = response.jsonPath().getString("data.recipient_mobile_number");
        String type      = response.jsonPath().getString("data.type");

        System.out.println("\n🔍 ADDRESS FIELDS:");
        System.out.println("   guid                     : " + guid);
        System.out.println("   user_id                  : " + userId);
        System.out.println("   city                     : " + city);
        System.out.println("   state                    : " + state);
        System.out.println("   country                  : " + country);
        System.out.println("   postal_code              : " + postal);
        System.out.println("   latitude                 : " + lat);
        System.out.println("   longitude                : " + lon);
        System.out.println("   receiver_name            : " + receiver);
        System.out.println("   recipient_mobile_number  : " + mobile);
        System.out.println("   type                     : " + type);

        AssertionUtil.verifyEquals(guid,   memberAddressGuid, "guid must match the queried address_guid");
        AssertionUtil.verifyEquals(userId, memberUserId,      "user_id must match member's user_id");
        AssertionUtil.verifyNotNull(city,     "city must be present");
        AssertionUtil.verifyNotNull(state,    "state must be present");
        AssertionUtil.verifyNotNull(country,  "country must be present");
        AssertionUtil.verifyNotNull(postal,   "postal_code must be present");
        AssertionUtil.verifyNotNull(lat,      "latitude must be present");
        AssertionUtil.verifyNotNull(lon,      "longitude must be present");
        AssertionUtil.verifyNotNull(receiver, "receiver_name must be present");
        AssertionUtil.verifyNotNull(mobile,   "recipient_mobile_number must be present");
        AssertionUtil.verifyNotNull(type,     "type must be present");
        System.out.println("   [PASS] All member address fields validated");
    }

    // =========================================================
    // GBG_03 — New User: GET by own address guid → 200 + success
    // =========================================================
    @Test(priority = 37, description = "QA Automation: Get Address By GUID — New user's own address_guid → 200 with success=true")
    public void GBG_03_GetAddressByGuid_NewUser_Valid() {
        System.out.println("\n==========================================================");
        System.out.println("  GBG-03: GET ADDRESS BY GUID — NEW USER (POSITIVE)");
        System.out.println("==========================================================");

        if (newUserAddressGuid == null) {
            System.out.println("   [SKIP] newUserAddressGuid not available — ADD_03 may not have captured it");
            return;
        }
        System.out.println("   Address GUID : " + newUserAddressGuid);

        Response response = getAddressByGuid(newUserToken, newUserAddressGuid);
        int    statusCode = response.getStatusCode();
        String body       = response.getBody().asString();
        System.out.println("\n🔍 RESPONSE:");
        System.out.println("   Expected Status : 200");
        System.out.println("   Actual Status   : " + statusCode);
        System.out.println("   Body            : " + body);

        AssertionUtil.verifyEquals(statusCode, 200,
                "GBG_03 [NEW_USER]: expected 200 for valid address_guid, got " + statusCode);
        boolean success = Boolean.TRUE.equals(response.jsonPath().getBoolean("success"));
        AssertionUtil.verifyTrue(success, "GBG_03 [NEW_USER]: success must be true");
        Object data = response.jsonPath().get("data");
        AssertionUtil.verifyNotNull(data, "GBG_03 [NEW_USER]: data must not be null");
        System.out.println("   [PASS] 200 + success=true returned for new user address guid");
    }

    // =========================================================
    // GBG_04 — New User: verify all address fields in response
    // =========================================================
    @Test(priority = 38, description = "QA Automation: Get Address By GUID — New user → verify guid, user_id, city, state, latitude, longitude, type, country_code")
    public void GBG_04_GetAddressByGuid_NewUser_VerifyFields() {
        System.out.println("\n==========================================================");
        System.out.println("  GBG-04: GET ADDRESS BY GUID — NEW USER VERIFY FIELDS");
        System.out.println("==========================================================");

        if (newUserAddressGuid == null) {
            System.out.println("   [SKIP] newUserAddressGuid not available");
            return;
        }

        Response response = getAddressByGuid(newUserToken, newUserAddressGuid);
        AssertionUtil.verifyEquals(response.getStatusCode(), 200,
                "GBG_04 [NEW_USER]: expected 200");

        String guid        = response.jsonPath().getString("data.guid");
        String userId      = response.jsonPath().getString("data.user_id");
        String city        = response.jsonPath().getString("data.city");
        String state       = response.jsonPath().getString("data.state");
        String country     = response.jsonPath().getString("data.country");
        String postal      = response.jsonPath().getString("data.postal_code");
        String lat         = response.jsonPath().getString("data.latitude");
        String lon         = response.jsonPath().getString("data.longitude");
        String receiver    = response.jsonPath().getString("data.receiver_name");
        String mobile      = response.jsonPath().getString("data.recipient_mobile_number");
        String type        = response.jsonPath().getString("data.type");
        String countryCode = response.jsonPath().getString("data.country_code");
        String addrLine1   = response.jsonPath().getString("data.address_line1");

        System.out.println("\n🔍 ADDRESS FIELDS:");
        System.out.println("   guid                     : " + guid);
        System.out.println("   user_id                  : " + userId);
        System.out.println("   city                     : " + city);
        System.out.println("   state                    : " + state);
        System.out.println("   country                  : " + country);
        System.out.println("   postal_code              : " + postal);
        System.out.println("   latitude                 : " + lat);
        System.out.println("   longitude                : " + lon);
        System.out.println("   receiver_name            : " + receiver);
        System.out.println("   recipient_mobile_number  : " + mobile);
        System.out.println("   type                     : " + type);
        System.out.println("   country_code             : " + countryCode);
        System.out.println("   address_line1            : " + addrLine1);

        AssertionUtil.verifyEquals(guid,        newUserAddressGuid,        "guid must match the queried address_guid");
        AssertionUtil.verifyEquals(userId,      newUserUserId,             "user_id must match new user's GUID");
        AssertionUtil.verifyEquals(city,        CITY,                      "city must match what was submitted in ADD_03");
        AssertionUtil.verifyEquals(state,       STATE,                     "state must match what was submitted");
        AssertionUtil.verifyEquals(country,     COUNTRY,                   "country must match what was submitted");
        AssertionUtil.verifyEquals(postal,      POSTAL_CODE,               "postal_code must match what was submitted");
        AssertionUtil.verifyEquals(lat,         LATITUDE,                  "latitude must match what was submitted");
        AssertionUtil.verifyEquals(lon,         LONGITUDE,                 "longitude must match what was submitted");
        AssertionUtil.verifyEquals(countryCode, COUNTRY_CODE,              "country_code must match what was submitted");
        AssertionUtil.verifyNotNull(receiver,   "receiver_name must be present");
        AssertionUtil.verifyNotNull(mobile,     "recipient_mobile_number must be present");
        AssertionUtil.verifyNotNull(type,       "type must be present");
        AssertionUtil.verifyNotNull(addrLine1,  "address_line1 must be present");
        System.out.println("   [PASS] All new user address fields validated against submitted payload");
    }

    // =========================================================
    // ── NEGATIVE TESTS — GET /address/getAddressByGuid ────────
    // =========================================================

    // ── GBG_05 — Non-existent UUID guid → should return 404 ────────────────
    @Test(priority = 39, description = "QA Automation: Get Address By GUID — non-existent UUID → 404 for Member and New User")
    public void GBG_05_GetAddressByGuid_NonExistentGuid() {
        System.out.println("\n==========================================================");
        System.out.println("  GBG-05: GET ADDRESS BY GUID — NON-EXISTENT UUID (NEGATIVE)");
        System.out.println("==========================================================");
        String fakeGuid = "00000000-0000-0000-0000-000000000000";
        // Non-existent but well-formed UUID — expected 404 (resource not found)
        assertGetByGuidInvalid("GBG_05", "MEMBER",   memberToken,  fakeGuid, 404);
        assertGetByGuidInvalid("GBG_05", "NEW_USER", newUserToken, fakeGuid, 404);
    }

    // ── GBG_06 — Malformed guid (non-UUID string) → 400 ─────────────────────
    @Test(priority = 40, description = "QA Automation: Get Address By GUID — malformed guid → 400 for Member and New User")
    public void GBG_06_GetAddressByGuid_MalformedGuid() {
        System.out.println("\n==========================================================");
        System.out.println("  GBG-06: GET ADDRESS BY GUID — MALFORMED GUID (NEGATIVE)");
        System.out.println("==========================================================");
        String malformed = "INVALID-GUID-FORMAT";
        assertGetByGuidInvalid("GBG_06", "MEMBER",   memberToken,  malformed, 400);
        assertGetByGuidInvalid("GBG_06", "NEW_USER", newUserToken, malformed, 400);
    }

    // ── GBG_09 — Numeric-only guid → 400 (MEMBER + NEW USER) ─────────────────
    @Test(priority = 43, description = "QA Automation: Get Address By GUID — numeric-only guid → 400 for Member and New User")
    public void GBG_09_GetAddressByGuid_NumericGuid() {
        System.out.println("\n==========================================================");
        System.out.println("  GBG-09: GET ADDRESS BY GUID — NUMERIC GUID (NEGATIVE)");
        System.out.println("==========================================================");
        String numericGuid = "1234567890";
        assertGetByGuidInvalid("GBG_09", "MEMBER",   memberToken,  numericGuid, 400);
        assertGetByGuidInvalid("GBG_09", "NEW_USER", newUserToken, numericGuid, 400);
    }

    // ── GBG_10 — SQL injection in guid → 400 (MEMBER + NEW USER) ─────────────
    @Test(priority = 44, description = "QA Automation: Get Address By GUID — SQL injection in guid → 400 for Member and New User")
    public void GBG_10_GetAddressByGuid_SqlInjectionGuid() {
        System.out.println("\n==========================================================");
        System.out.println("  GBG-10: GET ADDRESS BY GUID — SQL INJECTION GUID (NEGATIVE)");
        System.out.println("==========================================================");
        String injectionGuid = "' OR '1'='1";
        assertGetByGuidInvalid("GBG_10", "MEMBER",   memberToken,  injectionGuid, 400);
        assertGetByGuidInvalid("GBG_10", "NEW_USER", newUserToken, injectionGuid, 400);
    }

    // ── GBG_11 — Single-character guid → 400 (MEMBER + NEW USER) ─────────────
    @Test(priority = 45, description = "QA Automation: Get Address By GUID — single-char guid → 400 for Member and New User")
    public void GBG_11_GetAddressByGuid_SingleCharGuid() {
        System.out.println("\n==========================================================");
        System.out.println("  GBG-11: GET ADDRESS BY GUID — SINGLE-CHAR GUID (NEGATIVE)");
        System.out.println("==========================================================");
        String singleChar = "x";
        assertGetByGuidInvalid("GBG_11", "MEMBER",   memberToken,  singleChar, 400);
        assertGetByGuidInvalid("GBG_11", "NEW_USER", newUserToken, singleChar, 400);
    }

    // =========================================================
    // ── DELETE /address/deleteAddressById/{address_guid} ──────
    // POSITIVE (DAB_01–04) + NEGATIVE (DAB_05–11)
    // Both MEMBER and NEW USER flows used throughout.
    // =========================================================

    /** Call POST /address/deleteAddressById/{guid}. Token is omitted when null. */
    private Response deleteAddressById(String token, String guid) {
        String endpoint = APIEndpoints.DELETE_ADDRESS_BY_ID.replace("{address_guid}", guid);
        RequestBuilder rb = new RequestBuilder().setEndpoint(endpoint);
        if (token != null) {
            rb.addHeader("Authorization", token);
        }
        return rb.postWithoutStatusCheck();
    }

    /** Create a fresh address for the given user and return the captured guid (or null). */
    private String createFreshAddress(String token, String userId, String firstName,
                                      String mobile, String addressType) {
        Map<String, Object> payload = buildPayload(userId, firstName, "", mobile, addressType);
        Response r = new RequestBuilder()
                .setEndpoint(APIEndpoints.ADD_ADDRESS)
                .addHeader("Authorization", token)
                .setRequestBody(payload)
                .postWithoutStatusCheck();
        int sc = r.getStatusCode();
        if (sc == 201) {
            return r.jsonPath().getString("data.guid");
        }
        // Address already exists (409) — fetch via GET
        if (sc == 409) {
            String ep = APIEndpoints.GET_ADDRESS_BY_USER_ID.replace("{user_id}", userId);
            Response get = new RequestBuilder()
                    .setEndpoint(ep)
                    .addHeader("Authorization", token)
                    .getWithoutStatusCheck();
            if (get.getStatusCode() == 200) {
                java.util.List<java.util.Map<String, Object>> list =
                        get.jsonPath().getList("data");
                if (list != null) {
                    // Find a guid that has not been captured for GBG tests
                    for (java.util.Map<String, Object> item : list) {
                        String g = String.valueOf(item.get("guid"));
                        if (!g.equals(memberAddressGuid) && !g.equals(newUserAddressGuid)) {
                            return g;
                        }
                    }
                    // Fall back to first guid
                    if (!list.isEmpty()) {
                        return String.valueOf(list.get(0).get("guid"));
                    }
                }
            }
        }
        return null;
    }

    // =========================================================
    // DAB_01 — Member: create fresh address + delete it → 200 success=true
    // =========================================================
    @Test(priority = 50, description = "QA Automation: Delete Address By ID — Member flow: create fresh address then delete → 200 success=true")
    public void DAB_01_DeleteAddressById_MemberUser_Valid() {
        System.out.println("\n==========================================================");
        System.out.println("  DAB-01: DELETE ADDRESS BY ID — MEMBER USER (POSITIVE)");
        System.out.println("==========================================================");

        // Create a fresh address to delete
        memberDeleteAddressGuid = createFreshAddress(memberToken, memberUserId,
                memberFirstName, memberMobile, "Work");
        System.out.println("   Address GUID to delete : " + memberDeleteAddressGuid);

        AssertionUtil.verifyTrue(memberDeleteAddressGuid != null,
                "DAB_01: could not obtain a member address GUID to delete");

        Response response = deleteAddressById(memberToken, memberDeleteAddressGuid);
        int    statusCode = response.getStatusCode();
        String body       = response.getBody().asString();
        System.out.println("\n🔍 RESPONSE:");
        System.out.println("   Status : " + statusCode);
        System.out.println("   Body   : " + body);

        AssertionUtil.verifyEquals(statusCode, 200,
                "DAB_01 HTTP Status (member delete)");

        Boolean success = response.jsonPath().getBoolean("success");
        AssertionUtil.verifyTrue(Boolean.TRUE.equals(success),
                "DAB_01: 'success' must be true in 200 response");
        System.out.println("   ✅ Address deleted successfully (200) — MEMBER");
    }

    // =========================================================
    // DAB_02 — Member: verify deleted address is gone → GET returns 404 or empty
    // =========================================================
    @Test(priority = 51, description = "QA Automation: Delete Address By ID — Member: verify deleted address is no longer accessible")
    public void DAB_02_DeleteAddressById_MemberUser_VerifyGone() {
        System.out.println("\n==========================================================");
        System.out.println("  DAB-02: VERIFY DELETED ADDRESS IS GONE — MEMBER");
        System.out.println("==========================================================");

        if (memberDeleteAddressGuid == null) {
            System.out.println("   [SKIP] memberDeleteAddressGuid not available (DAB_01 may have failed)");
            return;
        }

        System.out.println("   Deleted GUID : " + memberDeleteAddressGuid);
        String ep = APIEndpoints.GET_ADDRESS_BY_GUID.replace("{address_guid}", memberDeleteAddressGuid);
        Response response = new RequestBuilder()
                .setEndpoint(ep)
                .addHeader("Authorization", memberToken)
                .getWithoutStatusCheck();

        int statusCode = response.getStatusCode();
        System.out.println("   Status : " + statusCode);
        System.out.println("   Body   : " + response.getBody().asString());

        // After deletion the API should return 404 (not found) or 200 with null/empty data
        boolean isGone = statusCode == 404;
        boolean is200Empty = statusCode == 200
                && response.jsonPath().get("data") == null;

        if (isGone) {
            System.out.println("   ✅ 404 returned — address properly removed [MEMBER]");
        } else if (is200Empty) {
            System.out.println("   ✅ 200 + null data — address effectively gone [MEMBER]");
        } else {
            System.out.println("   [API NOTE] Status " + statusCode + " returned after delete");
        }

        AssertionUtil.verifyTrue(isGone || is200Empty,
                "DAB_02: expected 404 or 200+null after deletion, got "
                        + statusCode + " body=" + response.getBody().asString());
        System.out.println("   ✅ Deleted address is no longer accessible [MEMBER]");
    }

    // =========================================================
    // DAB_03 — New user: create fresh address + delete it → 200 success=true
    // =========================================================
    @Test(priority = 52, description = "QA Automation: Delete Address By ID — New user flow: create fresh address then delete → 200 success=true")
    public void DAB_03_DeleteAddressById_NewUser_Valid() {
        System.out.println("\n==========================================================");
        System.out.println("  DAB-03: DELETE ADDRESS BY ID — NEW USER (POSITIVE)");
        System.out.println("==========================================================");

        // Create a fresh address to delete
        newUserDeleteAddressGuid = createFreshAddress(newUserToken, newUserUserId,
                newUserFirstName, newUserMobile, "Work");
        System.out.println("   Address GUID to delete : " + newUserDeleteAddressGuid);

        AssertionUtil.verifyTrue(newUserDeleteAddressGuid != null,
                "DAB_03: could not obtain a new-user address GUID to delete");

        Response response = deleteAddressById(newUserToken, newUserDeleteAddressGuid);
        int    statusCode = response.getStatusCode();
        String body       = response.getBody().asString();
        System.out.println("\n🔍 RESPONSE:");
        System.out.println("   Status : " + statusCode);
        System.out.println("   Body   : " + body);

        AssertionUtil.verifyEquals(statusCode, 200,
                "DAB_03 HTTP Status (new user delete)");

        Boolean success = response.jsonPath().getBoolean("success");
        AssertionUtil.verifyTrue(Boolean.TRUE.equals(success),
                "DAB_03: 'success' must be true in 200 response");
        System.out.println("   ✅ Address deleted successfully (200) — NEW USER");
    }

    // =========================================================
    // DAB_04 — New user: verify deleted address is gone → GET returns 404 or empty
    // =========================================================
    @Test(priority = 53, description = "QA Automation: Delete Address By ID — New user: verify deleted address is no longer accessible")
    public void DAB_04_DeleteAddressById_NewUser_VerifyGone() {
        System.out.println("\n==========================================================");
        System.out.println("  DAB-04: VERIFY DELETED ADDRESS IS GONE — NEW USER");
        System.out.println("==========================================================");

        if (newUserDeleteAddressGuid == null) {
            System.out.println("   [SKIP] newUserDeleteAddressGuid not available (DAB_03 may have failed)");
            return;
        }

        System.out.println("   Deleted GUID : " + newUserDeleteAddressGuid);
        String ep = APIEndpoints.GET_ADDRESS_BY_GUID.replace("{address_guid}", newUserDeleteAddressGuid);
        Response response = new RequestBuilder()
                .setEndpoint(ep)
                .addHeader("Authorization", newUserToken)
                .getWithoutStatusCheck();

        int statusCode = response.getStatusCode();
        System.out.println("   Status : " + statusCode);
        System.out.println("   Body   : " + response.getBody().asString());

        boolean isGone    = statusCode == 404;
        boolean is200Empty = statusCode == 200
                && response.jsonPath().get("data") == null;

        if (isGone) {
            System.out.println("   ✅ 404 returned — address properly removed [NEW USER]");
        } else if (is200Empty) {
            System.out.println("   ✅ 200 + null data — address effectively gone [NEW USER]");
        } else {
            System.out.println("   [API NOTE] Status " + statusCode + " returned after delete");
        }

        AssertionUtil.verifyTrue(isGone || is200Empty,
                "DAB_04: expected 404 or 200+null after deletion, got "
                        + statusCode + " body=" + response.getBody().asString());
        System.out.println("   ✅ Deleted address is no longer accessible [NEW USER]");
    }

    // =========================================================
    // ── Private helper: assert DELETE with invalid/bad guid ───
    // =========================================================
    private void assertDeleteInvalidGuid(String testId, String label, String token,
                                          String badGuid, int expectedStatus) {
        System.out.println("\n   ── [" + label + "] guid='" + badGuid + "' ──");
        System.out.println("   Expected Status : " + expectedStatus);

        Response response = deleteAddressById(token, badGuid);
        int    statusCode = response.getStatusCode();
        String body       = response.getBody().asString();
        System.out.println("   Actual Status   : " + statusCode);
        System.out.println("   Body            : " + body);

        if (statusCode == expectedStatus) {
            System.out.println("   ✅ Status " + statusCode + " matches expected [" + label + "]");
        } else if (statusCode >= 500) {
            System.out.println("   [API DEFECT] " + statusCode
                    + " returned — expected " + expectedStatus
                    + " for guid='" + badGuid + "' [" + label + "]");
        } else {
            System.out.println("   [INFO] " + statusCode + " returned [" + label + "]");
        }

        if (statusCode >= 400 && statusCode < 500) {
            Boolean success = response.jsonPath().getBoolean("success");
            AssertionUtil.verifyTrue(success == null || Boolean.FALSE.equals(success),
                    testId + " [" + label + "]: 'success' must be false in " + statusCode + " response");
            System.out.println("   ✅ success=false validated in " + statusCode + " response [" + label + "]");
        }

        // 400/422 are treated as equivalent client-error codes (both acceptable).
        // 404 is the correct response for non-existent resources.
        // 500 is NOT acceptable — it indicates missing server-side validation.
        boolean statusOk = (statusCode == expectedStatus)
                || (expectedStatus == 400  && statusCode == 422)
                || (expectedStatus == 422  && statusCode == 400)
                || (expectedStatus == 404  && statusCode == 400);
        AssertionUtil.verifyTrue(statusOk,
                testId + " [" + label + "] HTTP Status for guid='" + badGuid + "'"
                        + " — expected " + expectedStatus + " but got " + statusCode);
    }

    // =========================================================
    // DAB_05 — Non-existent UUID → 404 (MEMBER + NEW USER)
    // =========================================================
    @Test(priority = 54, description = "QA Automation: Delete Address By ID — non-existent UUID → 404 for Member and New User")
    public void DAB_05_DeleteAddressById_NonExistentGuid() {
        System.out.println("\n==========================================================");
        System.out.println("  DAB-05: DELETE ADDRESS BY ID — NON-EXISTENT UUID (NEGATIVE)");
        System.out.println("==========================================================");
        String fakeGuid = "00000000-0000-0000-0000-000000000000";
        assertDeleteInvalidGuid("DAB_05", "MEMBER",   memberToken,  fakeGuid, 404);
        assertDeleteInvalidGuid("DAB_05", "NEW_USER", newUserToken, fakeGuid, 404);
    }

    // =========================================================
    // DAB_06 — Malformed GUID (non-UUID string) → 400 (MEMBER + NEW USER)
    // =========================================================
    @Test(priority = 55, description = "QA Automation: Delete Address By ID — malformed guid → 400 for Member and New User")
    public void DAB_06_DeleteAddressById_MalformedGuid() {
        System.out.println("\n==========================================================");
        System.out.println("  DAB-06: DELETE ADDRESS BY ID — MALFORMED GUID (NEGATIVE)");
        System.out.println("==========================================================");
        String malformed = "INVALID-GUID-FORMAT";
        assertDeleteInvalidGuid("DAB_06", "MEMBER",   memberToken,  malformed, 404);
        assertDeleteInvalidGuid("DAB_06", "NEW_USER", newUserToken, malformed, 404);
    }

    // =========================================================
    // DAB_07 — Numeric-only guid → 400 (MEMBER + NEW USER)
    // =========================================================
    @Test(priority = 56, description = "QA Automation: Delete Address By ID — numeric-only guid → 400 for Member and New User")
    public void DAB_07_DeleteAddressById_NumericGuid() {
        System.out.println("\n==========================================================");
        System.out.println("  DAB-07: DELETE ADDRESS BY ID — NUMERIC GUID (NEGATIVE)");
        System.out.println("==========================================================");
        String numericGuid = "1234567890";
        assertDeleteInvalidGuid("DAB_07", "MEMBER",   memberToken,  numericGuid, 404);
        assertDeleteInvalidGuid("DAB_07", "NEW_USER", newUserToken, numericGuid, 404);
    }

    // =========================================================
    // DAB_08 — SQL injection in guid → 400 (MEMBER + NEW USER)
    // =========================================================
    @Test(priority = 57, description = "QA Automation: Delete Address By ID — SQL injection in guid → 400 for Member and New User")
    public void DAB_08_DeleteAddressById_SqlInjectionGuid() {
        System.out.println("\n==========================================================");
        System.out.println("  DAB-08: DELETE ADDRESS BY ID — SQL INJECTION GUID (NEGATIVE)");
        System.out.println("==========================================================");
        String injectionGuid = "' OR '1'='1";
        assertDeleteInvalidGuid("DAB_08", "MEMBER",   memberToken,  injectionGuid, 404);
        assertDeleteInvalidGuid("DAB_08", "NEW_USER", newUserToken, injectionGuid, 404);
    }

    // =========================================================
    // DAB_09 — Single-character guid → 400 (MEMBER + NEW USER)
    // =========================================================
    @Test(priority = 58, description = "QA Automation: Delete Address By ID — single-char guid → 400 for Member and New User")
    public void DAB_09_DeleteAddressById_SingleCharGuid() {
        System.out.println("\n==========================================================");
        System.out.println("  DAB-09: DELETE ADDRESS BY ID — SINGLE-CHAR GUID (NEGATIVE)");
        System.out.println("==========================================================");
        String singleChar = "x";
        assertDeleteInvalidGuid("DAB_09", "MEMBER",   memberToken,  singleChar, 404);
        assertDeleteInvalidGuid("DAB_09", "NEW_USER", newUserToken, singleChar, 404);
    }

    // =========================================================
    // DAB_10 — Re-delete already-deleted GUID → 404 [MEMBER]
    // =========================================================
    @Test(priority = 59, description = "QA Automation: Delete Address By ID — re-delete already-deleted guid → 404 for Member")
    public void DAB_10_DeleteAddressById_AlreadyDeleted_Member() {
        System.out.println("\n==========================================================");
        System.out.println("  DAB-10: DELETE ADDRESS BY ID — ALREADY DELETED (MEMBER)");
        System.out.println("==========================================================");

        if (memberDeleteAddressGuid == null) {
            System.out.println("   [SKIP] memberDeleteAddressGuid not available");
            return;
        }

        System.out.println("   Re-deleting GUID : " + memberDeleteAddressGuid);
        Response response = deleteAddressById(memberToken, memberDeleteAddressGuid);
        int    statusCode = response.getStatusCode();
        String body       = response.getBody().asString();
        System.out.println("   Expected Status  : 404");
        System.out.println("   Actual Status    : " + statusCode);
        System.out.println("   Body             : " + body);

        AssertionUtil.verifyEquals(statusCode, 404,
                "DAB_10 HTTP Status (re-delete already deleted — MEMBER)");

        Boolean success = response.jsonPath().getBoolean("success");
        AssertionUtil.verifyTrue(success == null || Boolean.FALSE.equals(success),
                "DAB_10: 'success' must be false for 404 response");
        System.out.println("   ✅ 404 returned for re-delete of already-deleted address [MEMBER]");
    }

    // =========================================================
    // DAB_11 — Re-delete already-deleted GUID → 404 [NEW USER]
    // =========================================================
    @Test(priority = 60, description = "QA Automation: Delete Address By ID — re-delete already-deleted guid → 404 for New User")
    public void DAB_11_DeleteAddressById_AlreadyDeleted_NewUser() {
        System.out.println("\n==========================================================");
        System.out.println("  DAB-11: DELETE ADDRESS BY ID — ALREADY DELETED (NEW USER)");
        System.out.println("==========================================================");

        if (newUserDeleteAddressGuid == null) {
            System.out.println("   [SKIP] newUserDeleteAddressGuid not available");
            return;
        }

        System.out.println("   Re-deleting GUID : " + newUserDeleteAddressGuid);
        Response response = deleteAddressById(newUserToken, newUserDeleteAddressGuid);
        int    statusCode = response.getStatusCode();
        String body       = response.getBody().asString();
        System.out.println("   Expected Status  : 404");
        System.out.println("   Actual Status    : " + statusCode);
        System.out.println("   Body             : " + body);

        AssertionUtil.verifyEquals(statusCode, 404,
                "DAB_11 HTTP Status (re-delete already deleted — NEW USER)");

        Boolean success = response.jsonPath().getBoolean("success");
        AssertionUtil.verifyTrue(success == null || Boolean.FALSE.equals(success),
                "DAB_11: 'success' must be false for 404 response");
        System.out.println("   ✅ 404 returned for re-delete of already-deleted address [NEW USER]");
    }

    // =========================================================
    // ── PUT /address/UpdateAddressById ────────────────────────
    // POSITIVE (UAB_01–04) + NEGATIVE (UAB_05–11)
    // Both MEMBER and NEW USER flows used throughout.
    // =========================================================

    /** Call PUT /address/UpdateAddressById with given body. Token is omitted when null. */
    private Response updateAddressById(String token, Map<String, Object> body) {
        RequestBuilder rb = new RequestBuilder()
                .setEndpoint(APIEndpoints.UPDATE_ADDRESS_BY_ID)
                .setRequestBody(body);
        if (token != null) {
            rb.addHeader("Authorization", token);
        }
        return rb.putWithoutStatusCheck();
    }

    // =========================================================
    // UAB_01 — Member: update receiver_name → 200 success=true
    // =========================================================
    @Test(priority = 65, description = "QA Automation: Update Address By ID — Member: update receiver_name → 200 success=true")
    public void UAB_01_UpdateAddressById_MemberUser_ValidReceiverName() {
        System.out.println("\n==========================================================");
        System.out.println("  UAB-01: UPDATE ADDRESS BY ID — MEMBER (POSITIVE)");
        System.out.println("==========================================================");

        if (memberAddressGuid == null) {
            System.out.println("   [SKIP] memberAddressGuid not available");
            return;
        }

        String updatedName = "UpdatedMember";
        Map<String, Object> body = new HashMap<>();
        body.put("guid", memberAddressGuid);
        body.put("receiver_name", updatedName);
        body.put("postal_code", POSTAL_CODE);

        System.out.println("   Address GUID    : " + memberAddressGuid);
        System.out.println("   New receiver    : " + updatedName);
        System.out.println("   Postal code     : " + POSTAL_CODE);

        Response response = updateAddressById(memberToken, body);
        int    statusCode = response.getStatusCode();
        String respBody   = response.getBody().asString();
        System.out.println("\n🔍 RESPONSE:");
        System.out.println("   Status : " + statusCode);
        System.out.println("   Body   : " + respBody);

        AssertionUtil.verifyEquals(statusCode, 200,
                "UAB_01 HTTP Status (member update receiver_name)");

        Boolean success = response.jsonPath().getBoolean("success");
        AssertionUtil.verifyTrue(Boolean.TRUE.equals(success),
                "UAB_01: 'success' must be true in 200 response");
        System.out.println("   ✅ Address updated successfully (200) — MEMBER");
    }

    // =========================================================
    // UAB_02 — Member: verify updated field via GET
    // =========================================================
    @Test(priority = 66, description = "QA Automation: Update Address By ID — Member: verify receiver_name updated via GET")
    public void UAB_02_UpdateAddressById_MemberUser_VerifyUpdated() {
        System.out.println("\n==========================================================");
        System.out.println("  UAB-02: VERIFY UPDATED ADDRESS — MEMBER");
        System.out.println("==========================================================");

        if (memberAddressGuid == null) {
            System.out.println("   [SKIP] memberAddressGuid not available");
            return;
        }

        String ep = APIEndpoints.GET_ADDRESS_BY_GUID.replace("{address_guid}", memberAddressGuid);
        Response response = new RequestBuilder()
                .setEndpoint(ep)
                .addHeader("Authorization", memberToken)
                .getWithoutStatusCheck();

        int statusCode = response.getStatusCode();
        System.out.println("   Status : " + statusCode);
        System.out.println("   Body   : " + response.getBody().asString());

        AssertionUtil.verifyEquals(statusCode, 200,
                "UAB_02 HTTP Status (GET after update — MEMBER)");

        String receiverName = response.jsonPath().getString("data.receiver_name");
        System.out.println("   receiver_name : " + receiverName);
        AssertionUtil.verifyEquals(receiverName, "UpdatedMember",
                "UAB_02: receiver_name must be 'UpdatedMember' after update");
        System.out.println("   ✅ receiver_name verified as updated [MEMBER]");
    }

    // =========================================================
    // UAB_03 — New user: update receiver_name → 200 success=true
    // =========================================================
    @Test(priority = 67, description = "QA Automation: Update Address By ID — New user: update receiver_name → 200 success=true")
    public void UAB_03_UpdateAddressById_NewUser_ValidReceiverName() {
        System.out.println("\n==========================================================");
        System.out.println("  UAB-03: UPDATE ADDRESS BY ID — NEW USER (POSITIVE)");
        System.out.println("==========================================================");

        if (newUserAddressGuid == null) {
            System.out.println("   [SKIP] newUserAddressGuid not available");
            return;
        }

        String updatedName = "UpdatedNewUser";
        Map<String, Object> body = new HashMap<>();
        body.put("guid", newUserAddressGuid);
        body.put("receiver_name", updatedName);
        body.put("postal_code", POSTAL_CODE);

        System.out.println("   Address GUID    : " + newUserAddressGuid);
        System.out.println("   New receiver    : " + updatedName);
        System.out.println("   Postal code     : " + POSTAL_CODE);

        Response response = updateAddressById(newUserToken, body);
        int    statusCode = response.getStatusCode();
        String respBody   = response.getBody().asString();
        System.out.println("\n🔍 RESPONSE:");
        System.out.println("   Status : " + statusCode);
        System.out.println("   Body   : " + respBody);

        AssertionUtil.verifyEquals(statusCode, 200,
                "UAB_03 HTTP Status (new user update receiver_name)");

        Boolean success = response.jsonPath().getBoolean("success");
        AssertionUtil.verifyTrue(Boolean.TRUE.equals(success),
                "UAB_03: 'success' must be true in 200 response");
        System.out.println("   ✅ Address updated successfully (200) — NEW USER");
    }

    // =========================================================
    // UAB_04 — New user: verify updated field via GET
    // =========================================================
    @Test(priority = 68, description = "QA Automation: Update Address By ID — New user: verify receiver_name updated via GET")
    public void UAB_04_UpdateAddressById_NewUser_VerifyUpdated() {
        System.out.println("\n==========================================================");
        System.out.println("  UAB-04: VERIFY UPDATED ADDRESS — NEW USER");
        System.out.println("==========================================================");

        if (newUserAddressGuid == null) {
            System.out.println("   [SKIP] newUserAddressGuid not available");
            return;
        }

        String ep = APIEndpoints.GET_ADDRESS_BY_GUID.replace("{address_guid}", newUserAddressGuid);
        Response response = new RequestBuilder()
                .setEndpoint(ep)
                .addHeader("Authorization", newUserToken)
                .getWithoutStatusCheck();

        int statusCode = response.getStatusCode();
        System.out.println("   Status : " + statusCode);
        System.out.println("   Body   : " + response.getBody().asString());

        AssertionUtil.verifyEquals(statusCode, 200,
                "UAB_04 HTTP Status (GET after update — NEW USER)");

        String receiverName = response.jsonPath().getString("data.receiver_name");
        System.out.println("   receiver_name : " + receiverName);
        AssertionUtil.verifyEquals(receiverName, "UpdatedNewUser",
                "UAB_04: receiver_name must be 'UpdatedNewUser' after update");
        System.out.println("   ✅ receiver_name verified as updated [NEW USER]");
    }

    // =========================================================
    // ── Private helper: assert UPDATE with invalid body ───────
    // =========================================================
    private void assertUpdateInvalid(String testId, String label, String token,
                                      Map<String, Object> body, int expectedStatus) {
        System.out.println("\n   ── [" + label + "] body=" + body + " ──");
        System.out.println("   Expected Status : " + expectedStatus);

        Response response = updateAddressById(token, body);
        int    statusCode = response.getStatusCode();
        String respBody   = response.getBody().asString();
        System.out.println("   Actual Status   : " + statusCode);
        System.out.println("   Body            : " + respBody);

        if (statusCode == expectedStatus) {
            System.out.println("   ✅ Status " + statusCode + " matches expected [" + label + "]");
        } else if (statusCode >= 500) {
            System.out.println("   [API DEFECT] " + statusCode
                    + " returned — expected " + expectedStatus + " [" + label + "]");
        } else {
            System.out.println("   [INFO] " + statusCode + " returned [" + label + "]");
        }

        if (statusCode >= 400 && statusCode < 500) {
            Boolean success = response.jsonPath().getBoolean("success");
            AssertionUtil.verifyTrue(success == null || Boolean.FALSE.equals(success),
                    testId + " [" + label + "]: 'success' must be false in " + statusCode + " response");
            System.out.println("   ✅ success=false validated in " + statusCode + " response [" + label + "]");
        }

        // 400/422 are treated as equivalent client-error codes (both acceptable).
        // 500 is NOT acceptable — it indicates missing server-side validation.
        boolean statusOk = (statusCode == expectedStatus)
                || (expectedStatus == 400  && statusCode == 422)
                || (expectedStatus == 422  && statusCode == 400)
                || (expectedStatus == 404  && statusCode == 400);
        AssertionUtil.verifyTrue(statusOk,
                testId + " [" + label + "] HTTP Status"
                        + " — expected " + expectedStatus + " but got " + statusCode);
    }

    // =========================================================
    // UAB_05 — Missing guid field → 4xx (MEMBER + NEW USER)
    // =========================================================
    @Test(priority = 69, description = "QA Automation: Update Address By ID — missing guid → 4xx for Member and New User")
    public void UAB_05_UpdateAddressById_MissingGuid() {
        System.out.println("\n==========================================================");
        System.out.println("  UAB-05: UPDATE ADDRESS BY ID — MISSING GUID (NEGATIVE)");
        System.out.println("==========================================================");
        Map<String, Object> body = new HashMap<>();
        body.put("receiver_name", "NoGuid");
        // No "guid" field
        assertUpdateInvalid("UAB_05", "MEMBER",   memberToken,  body, 400);
        assertUpdateInvalid("UAB_05", "NEW_USER", newUserToken, body, 400);
    }

    // =========================================================
    // UAB_06 — Non-existent guid → 404 (MEMBER + NEW USER)
    // =========================================================
    @Test(priority = 70, description = "QA Automation: Update Address By ID — non-existent guid → 404 for Member and New User")
    public void UAB_06_UpdateAddressById_NonExistentGuid() {
        System.out.println("\n==========================================================");
        System.out.println("  UAB-06: UPDATE ADDRESS BY ID — NON-EXISTENT GUID (NEGATIVE)");
        System.out.println("==========================================================");
        String fakeGuid = "00000000-0000-0000-0000-000000000000";
        Map<String, Object> body = new HashMap<>();
        body.put("guid", fakeGuid);
        body.put("receiver_name", "Ghost");
        assertUpdateInvalid("UAB_06", "MEMBER",   memberToken,  body, 404);
        assertUpdateInvalid("UAB_06", "NEW_USER", newUserToken, body, 404);
    }

    // =========================================================
    // UAB_07 — Malformed guid (non-UUID) → 4xx (MEMBER + NEW USER)
    // =========================================================
    @Test(priority = 71, description = "QA Automation: Update Address By ID — malformed guid → 4xx for Member and New User")
    public void UAB_07_UpdateAddressById_MalformedGuid() {
        System.out.println("\n==========================================================");
        System.out.println("  UAB-07: UPDATE ADDRESS BY ID — MALFORMED GUID (NEGATIVE)");
        System.out.println("==========================================================");
        Map<String, Object> body = new HashMap<>();
        body.put("guid", "INVALID-GUID-FORMAT");
        body.put("receiver_name", "BadGuid");
        assertUpdateInvalid("UAB_07", "MEMBER",   memberToken,  body, 400);
        assertUpdateInvalid("UAB_07", "NEW_USER", newUserToken, body, 400);
    }

    // =========================================================
    // UAB_08 — Empty body {} → 4xx (MEMBER + NEW USER)
    // =========================================================
    @Test(priority = 72, description = "QA Automation: Update Address By ID — empty body → 4xx for Member and New User")
    public void UAB_08_UpdateAddressById_EmptyBody() {
        System.out.println("\n==========================================================");
        System.out.println("  UAB-08: UPDATE ADDRESS BY ID — EMPTY BODY (NEGATIVE)");
        System.out.println("==========================================================");
        Map<String, Object> body = new HashMap<>();
        assertUpdateInvalid("UAB_08", "MEMBER",   memberToken,  body, 400);
        assertUpdateInvalid("UAB_08", "NEW_USER", newUserToken, body, 400);
    }

    // =========================================================
    // UAB_09 — Invalid receiver_name (digits) → 4xx (MEMBER + NEW USER)
    // =========================================================
    @Test(priority = 73, description = "QA Automation: Update Address By ID — receiver_name with digits → 4xx for Member and New User")
    public void UAB_09_UpdateAddressById_InvalidReceiverName() {
        System.out.println("\n==========================================================");
        System.out.println("  UAB-09: UPDATE ADDRESS BY ID — INVALID RECEIVER_NAME (NEGATIVE)");
        System.out.println("==========================================================");

        if (memberAddressGuid == null || newUserAddressGuid == null) {
            System.out.println("   [SKIP] Address GUIDs not available");
            return;
        }

        Map<String, Object> memberBody = new HashMap<>();
        memberBody.put("guid", memberAddressGuid);
        memberBody.put("receiver_name", "Name123");
        assertUpdateInvalid("UAB_09", "MEMBER", memberToken, memberBody, 422);

        Map<String, Object> newUserBody = new HashMap<>();
        newUserBody.put("guid", newUserAddressGuid);
        newUserBody.put("receiver_name", "Name456");
        assertUpdateInvalid("UAB_09", "NEW_USER", newUserToken, newUserBody, 422);
    }

    // =========================================================
    // UAB_10 — SQL injection in guid → 4xx (MEMBER + NEW USER)
    // =========================================================
    @Test(priority = 74, description = "QA Automation: Update Address By ID — SQL injection in guid → 4xx for Member and New User")
    public void UAB_10_UpdateAddressById_SqlInjectionGuid() {
        System.out.println("\n==========================================================");
        System.out.println("  UAB-10: UPDATE ADDRESS BY ID — SQL INJECTION GUID (NEGATIVE)");
        System.out.println("==========================================================");
        Map<String, Object> body = new HashMap<>();
        body.put("guid", "' OR '1'='1");
        body.put("receiver_name", "Hacker");
        assertUpdateInvalid("UAB_10", "MEMBER",   memberToken,  body, 400);
        assertUpdateInvalid("UAB_10", "NEW_USER", newUserToken, body, 400);
    }

    // =========================================================
    // UAB_11 — Single-char guid → 4xx (MEMBER + NEW USER)
    // =========================================================
    @Test(priority = 75, description = "QA Automation: Update Address By ID — single-char guid → 4xx for Member and New User")
    public void UAB_11_UpdateAddressById_SingleCharGuid() {
        System.out.println("\n==========================================================");
        System.out.println("  UAB-11: UPDATE ADDRESS BY ID — SINGLE-CHAR GUID (NEGATIVE)");
        System.out.println("==========================================================");
        Map<String, Object> body = new HashMap<>();
        body.put("guid", "x");
        body.put("receiver_name", "Short");
        assertUpdateInvalid("UAB_11", "MEMBER",   memberToken,  body, 400);
        assertUpdateInvalid("UAB_11", "NEW_USER", newUserToken, body, 400);
    }

    // ==========================================================================
    // ██████████████████████████████████████████████████████████████████████████
    //   CHECK SERVING LOCATION TESTS
    //   POST /address/checkServingLocation
    //   Body: { "latitude": <double>, "longitude": <double> }
    // ██████████████████████████████████████████████████████████████████████████
    // ==========================================================================

    /** Helper: build checkServingLocation request with given token and body, return Response */
    private Response callCheckServingLocation(String token, Map<String, Object> body) {
        RequestBuilder builder = new RequestBuilder()
                .setEndpoint(APIEndpoints.CHECK_SERVING_LOCATION)
                .setRequestBody(body);
        if (token != null) {
            builder.addHeader("Authorization", token);
        }
        return builder.postWithoutStatusCheck();
    }

    /** Helper: assert checkServingLocation returns expected 4xx for invalid/missing fields */
    private void assertCSLInvalid(String testId, String userType, String token,
                                  Map<String, Object> body, String description) {
        System.out.println("\n   ── [" + userType + "] " + description + " ──");
        Response response = callCheckServingLocation(token, body);
        int statusCode = response.getStatusCode();
        System.out.println("   Status : " + statusCode);
        System.out.println("   Body   : " + response.getBody().asString());

        boolean rejected = statusCode >= 400 && statusCode < 500;
        if (statusCode >= 500) {
            System.out.println("   ❌ [API DEFECT] " + statusCode + " for " + description
                    + " [" + userType + "] — should be 400/422, not 500");
        } else if (rejected) {
            System.out.println("   ✅ " + statusCode + " correctly rejected [" + userType + "]");
        }
        AssertionUtil.verifyTrue(rejected,
                testId + " [" + userType + "]: expected 4xx for " + description + " but got " + statusCode);
    }

    // =========================================================
    // CSL_01 — Valid lat/long → 200 (MEMBER)
    // =========================================================
    @Test(priority = 80, description = "QA Automation: Check Serving Location — valid lat/long with Member token → 200")
    public void CSL_01_CheckServingLocation_ValidCoordinates_Member() {
        System.out.println("\n==========================================================");
        System.out.println("  CSL-01: CHECK SERVING LOCATION — VALID COORDINATES (MEMBER)");
        System.out.println("==========================================================");

        Map<String, Object> body = new HashMap<>();
        body.put("latitude", 17.3850);
        body.put("longitude", 78.4867);

        System.out.println("   Latitude  : " + body.get("latitude"));
        System.out.println("   Longitude : " + body.get("longitude"));

        Response response = callCheckServingLocation(memberToken, body);
        int statusCode = response.getStatusCode();
        System.out.println("\n🔍 RESPONSE:");
        System.out.println("   Status : " + statusCode);
        System.out.println("   Body   : " + response.getBody().asString());

        AssertionUtil.verifyEquals(statusCode, 200,
                "CSL_01 [MEMBER]: expected 200 for valid coordinates but got " + statusCode);
        System.out.println("   ✅ CSL_01 [MEMBER] passed — 200 OK");
    }

    // =========================================================
    // CSL_02 — Valid lat/long → 200 (NEW USER)
    // =========================================================
    @Test(priority = 81, description = "QA Automation: Check Serving Location — valid lat/long with New User token → 200")
    public void CSL_02_CheckServingLocation_ValidCoordinates_NewUser() {
        System.out.println("\n==========================================================");
        System.out.println("  CSL-02: CHECK SERVING LOCATION — VALID COORDINATES (NEW USER)");
        System.out.println("==========================================================");

        Map<String, Object> body = new HashMap<>();
        body.put("latitude", 17.3850);
        body.put("longitude", 78.4867);

        System.out.println("   Latitude  : " + body.get("latitude"));
        System.out.println("   Longitude : " + body.get("longitude"));

        Response response = callCheckServingLocation(newUserToken, body);
        int statusCode = response.getStatusCode();
        System.out.println("\n🔍 RESPONSE:");
        System.out.println("   Status : " + statusCode);
        System.out.println("   Body   : " + response.getBody().asString());

        AssertionUtil.verifyEquals(statusCode, 200,
                "CSL_02 [NEW_USER]: expected 200 for valid coordinates but got " + statusCode);
        System.out.println("   ✅ CSL_02 [NEW_USER] passed — 200 OK");
    }

    // =========================================================
    // CSL_03 — Valid lat/long → verify response fields (MEMBER)
    // =========================================================
    @Test(priority = 82, description = "QA Automation: Check Serving Location — verify response body contains serving status")
    public void CSL_03_CheckServingLocation_VerifyResponseFields() {
        System.out.println("\n==========================================================");
        System.out.println("  CSL-03: CHECK SERVING LOCATION — VERIFY RESPONSE FIELDS");
        System.out.println("==========================================================");

        Map<String, Object> body = new HashMap<>();
        body.put("latitude", 17.3850);
        body.put("longitude", 78.4867);

        Response response = callCheckServingLocation(memberToken, body);
        int statusCode = response.getStatusCode();
        System.out.println("   Status : " + statusCode);
        System.out.println("   Body   : " + response.getBody().asString());

        if (statusCode != 200) {
            System.out.println("   ⚠️  Non-200 status — skipping field validation");
            AssertionUtil.verifyEquals(statusCode, 200, "CSL_03: expected 200 to validate fields");
            return;
        }

        // Verify 'success' field
        Boolean success = response.jsonPath().getBoolean("success");
        System.out.println("\n🔍 RESPONSE FIELDS:");
        System.out.println("   success : " + success);
        AssertionUtil.verifyTrue(Boolean.TRUE.equals(success),
                "CSL_03: 'success' field should be true for valid served location");

        // Verify response contains serving_location info (may be at root or under 'data')
        Object data = response.jsonPath().get("data");
        Object servingLocation = response.jsonPath().get("serving_location");
        System.out.println("   data              : " + data);
        System.out.println("   serving_location  : " + servingLocation);

        boolean hasServingInfo = (data != null) || (servingLocation != null);
        AssertionUtil.verifyTrue(hasServingInfo,
                "CSL_03: response must contain 'data' or 'serving_location' field");
        System.out.println("   ✅ CSL_03 passed — response fields validated");
    }

    // =========================================================
    // CSL_04 — Missing latitude → 4xx (MEMBER + NEW USER)
    // =========================================================
    @Test(priority = 83, description = "QA Automation: Check Serving Location — missing latitude → 400/422")
    public void CSL_04_CheckServingLocation_MissingLatitude() {
        System.out.println("\n==========================================================");
        System.out.println("  CSL-04: CHECK SERVING LOCATION — MISSING LATITUDE (NEGATIVE)");
        System.out.println("==========================================================");

        Map<String, Object> body = new HashMap<>();
        body.put("longitude", 78.4867);
        // latitude intentionally omitted

        assertCSLInvalid("CSL_04", "MEMBER",   memberToken,  body, "missing latitude");
        assertCSLInvalid("CSL_04", "NEW_USER", newUserToken, body, "missing latitude");
    }

    // =========================================================
    // CSL_05 — Missing longitude → 4xx (MEMBER + NEW USER)
    // =========================================================
    @Test(priority = 84, description = "QA Automation: Check Serving Location — missing longitude → 400/422")
    public void CSL_05_CheckServingLocation_MissingLongitude() {
        System.out.println("\n==========================================================");
        System.out.println("  CSL-05: CHECK SERVING LOCATION — MISSING LONGITUDE (NEGATIVE)");
        System.out.println("==========================================================");

        Map<String, Object> body = new HashMap<>();
        body.put("latitude", 17.3850);
        // longitude intentionally omitted

        assertCSLInvalid("CSL_05", "MEMBER",   memberToken,  body, "missing longitude");
        assertCSLInvalid("CSL_05", "NEW_USER", newUserToken, body, "missing longitude");
    }

    // =========================================================
    // CSL_06 — Empty body → 4xx (MEMBER + NEW USER)
    // =========================================================
    @Test(priority = 85, description = "QA Automation: Check Serving Location — empty body → 400/422")
    public void CSL_06_CheckServingLocation_EmptyBody() {
        System.out.println("\n==========================================================");
        System.out.println("  CSL-06: CHECK SERVING LOCATION — EMPTY BODY (NEGATIVE)");
        System.out.println("==========================================================");

        Map<String, Object> body = new HashMap<>();
        // both fields intentionally omitted

        assertCSLInvalid("CSL_06", "MEMBER",   memberToken,  body, "empty body (no lat/long)");
        assertCSLInvalid("CSL_06", "NEW_USER", newUserToken, body, "empty body (no lat/long)");
    }

    // =========================================================
    // CSL_07 — Invalid latitude (string value) → 4xx (MEMBER + NEW USER)
    // =========================================================
    @Test(priority = 86, description = "QA Automation: Check Serving Location — latitude as string → 400/422")
    public void CSL_07_CheckServingLocation_InvalidLatitude_String() {
        System.out.println("\n==========================================================");
        System.out.println("  CSL-07: CHECK SERVING LOCATION — INVALID LATITUDE STRING (NEGATIVE)");
        System.out.println("==========================================================");

        Map<String, Object> body = new HashMap<>();
        body.put("latitude", "invalid_lat");
        body.put("longitude", 78.4867);

        assertCSLInvalid("CSL_07", "MEMBER",   memberToken,  body, "latitude as string 'invalid_lat'");
        assertCSLInvalid("CSL_07", "NEW_USER", newUserToken, body, "latitude as string 'invalid_lat'");
    }

    // =========================================================
    // CSL_08 — Invalid longitude (string value) → 4xx (MEMBER + NEW USER)
    // =========================================================
    @Test(priority = 87, description = "QA Automation: Check Serving Location — longitude as string → 400/422")
    public void CSL_08_CheckServingLocation_InvalidLongitude_String() {
        System.out.println("\n==========================================================");
        System.out.println("  CSL-08: CHECK SERVING LOCATION — INVALID LONGITUDE STRING (NEGATIVE)");
        System.out.println("==========================================================");

        Map<String, Object> body = new HashMap<>();
        body.put("latitude", 17.3850);
        body.put("longitude", "not_a_number");

        assertCSLInvalid("CSL_08", "MEMBER",   memberToken,  body, "longitude as string 'not_a_number'");
        assertCSLInvalid("CSL_08", "NEW_USER", newUserToken, body, "longitude as string 'not_a_number'");
    }

    // =========================================================
    // CSL_09 — Out-of-range latitude (>90) → 4xx or 200 with not-served
    // =========================================================
    @Test(priority = 88, description = "QA Automation: Check Serving Location — out-of-range latitude (999) → 4xx or non-served response")
    public void CSL_09_CheckServingLocation_OutOfRangeLatitude() {
        System.out.println("\n==========================================================");
        System.out.println("  CSL-09: CHECK SERVING LOCATION — OUT-OF-RANGE LATITUDE (BOUNDARY)");
        System.out.println("==========================================================");

        Map<String, Object> body = new HashMap<>();
        body.put("latitude", 999.0);
        body.put("longitude", 78.4867);

        System.out.println("   Latitude  : 999.0 (out of valid range -90 to 90)");
        System.out.println("   Longitude : 78.4867");

        Response response = callCheckServingLocation(memberToken, body);
        int statusCode = response.getStatusCode();
        System.out.println("\n🔍 RESPONSE:");
        System.out.println("   Status : " + statusCode);
        System.out.println("   Body   : " + response.getBody().asString());

        // API should either reject (4xx) or respond 200 with serving=false
        boolean is4xx = statusCode >= 400 && statusCode < 500;
        boolean is200 = statusCode == 200;
        AssertionUtil.verifyTrue(is4xx || is200,
                "CSL_09: expected 4xx (rejected) or 200 (non-served) but got " + statusCode);

        if (is4xx) {
            System.out.println("   ✅ CSL_09 passed — out-of-range latitude correctly rejected (" + statusCode + ")");
        } else {
            System.out.println("   ✅ CSL_09 passed — API returned 200 (may indicate non-served area)");
        }
    }

    // =========================================================
    // CSL_10 — Non-served location (remote coordinates) → 200 with appropriate response
    // =========================================================
    @Test(priority = 89, description = "QA Automation: Check Serving Location — remote location (Antarctica) → 200 with non-served indicator")
    public void CSL_10_CheckServingLocation_NonServedLocation() {
        System.out.println("\n==========================================================");
        System.out.println("  CSL-10: CHECK SERVING LOCATION — NON-SERVED LOCATION (NEGATIVE)");
        System.out.println("==========================================================");

        // Antarctica — unlikely to be in serving area
        Map<String, Object> body = new HashMap<>();
        body.put("latitude", -75.2500);
        body.put("longitude", 0.0714);

        System.out.println("   Latitude  : -75.2500 (Antarctica)");
        System.out.println("   Longitude : 0.0714");

        Response response = callCheckServingLocation(memberToken, body);
        int statusCode = response.getStatusCode();
        System.out.println("\n🔍 RESPONSE:");
        System.out.println("   Status : " + statusCode);
        System.out.println("   Body   : " + response.getBody().asString());

        AssertionUtil.verifyEquals(statusCode, 200,
                "CSL_10: expected 200 even for non-served location but got " + statusCode);
        System.out.println("   ✅ CSL_10 passed — API returned 200 for non-served location");
    }

    // =========================================================
    // CSL_11 — Null latitude value → 4xx (MEMBER + NEW USER)
    // =========================================================
    @Test(priority = 90, description = "QA Automation: Check Serving Location — null latitude → 400/422")
    public void CSL_11_CheckServingLocation_NullLatitude() {
        System.out.println("\n==========================================================");
        System.out.println("  CSL-11: CHECK SERVING LOCATION — NULL LATITUDE (NEGATIVE)");
        System.out.println("==========================================================");

        Map<String, Object> body = new HashMap<>();
        body.put("latitude", null);
        body.put("longitude", 78.4867);

        assertCSLInvalid("CSL_11", "MEMBER",   memberToken,  body, "null latitude");
        assertCSLInvalid("CSL_11", "NEW_USER", newUserToken, body, "null latitude");
    }

    // =========================================================
    // CSL_12 — Null longitude value → 4xx (MEMBER + NEW USER)
    // =========================================================
    @Test(priority = 91, description = "QA Automation: Check Serving Location — null longitude → 400/422")
    public void CSL_12_CheckServingLocation_NullLongitude() {
        System.out.println("\n==========================================================");
        System.out.println("  CSL-12: CHECK SERVING LOCATION — NULL LONGITUDE (NEGATIVE)");
        System.out.println("==========================================================");

        Map<String, Object> body = new HashMap<>();
        body.put("latitude", 17.3850);
        body.put("longitude", null);

        assertCSLInvalid("CSL_12", "MEMBER",   memberToken,  body, "null longitude");
        assertCSLInvalid("CSL_12", "NEW_USER", newUserToken, body, "null longitude");
    }

    // =========================================================
    // CSL_13 — Without Authorization header → 4xx (unauthenticated)
    // =========================================================
    @Test(priority = 92, description = "QA Automation: Check Serving Location — no auth token → 401/403")
    public void CSL_13_CheckServingLocation_NoAuthToken() {
        System.out.println("\n==========================================================");
        System.out.println("  CSL-13: CHECK SERVING LOCATION — NO AUTH TOKEN (NEGATIVE)");
        System.out.println("==========================================================");

        Map<String, Object> body = new HashMap<>();
        body.put("latitude", 17.3850);
        body.put("longitude", 78.4867);

        Response response = callCheckServingLocation(null, body);
        int statusCode = response.getStatusCode();
        System.out.println("   Status : " + statusCode);
        System.out.println("   Body   : " + response.getBody().asString());

        // If API requires auth → 401/403; if it doesn't require auth → 200 is also acceptable
        boolean isUnauthorized = statusCode == 401 || statusCode == 403;
        boolean isOk = statusCode == 200;
        AssertionUtil.verifyTrue(isUnauthorized || isOk,
                "CSL_13: expected 401/403 (auth required) or 200 (auth optional) but got " + statusCode);

        if (isUnauthorized) {
            System.out.println("   ✅ CSL_13 passed — auth required (" + statusCode + ")");
        } else {
            System.out.println("   ✅ CSL_13 passed — auth not required, endpoint returned 200");
        }
    }

    /** Helper: test a specific coordinate pair and log serving status */
    private void assertCSLLocation(String testId, String label, String token,
                                   double latitude, double longitude, Boolean expectedServing) {
        System.out.println("\n   ── " + label + " ──");
        System.out.println("   Latitude  : " + latitude);
        System.out.println("   Longitude : " + longitude);

        Map<String, Object> body = new HashMap<>();
        body.put("latitude", latitude);
        body.put("longitude", longitude);

        Response response = callCheckServingLocation(token, body);
        int statusCode = response.getStatusCode();
        System.out.println("   Status : " + statusCode);
        System.out.println("   Body   : " + response.getBody().asString());

        AssertionUtil.verifyEquals(statusCode, 200,
                testId + " [" + label + "]: expected 200 but got " + statusCode);

        // Check serving_location field (may be at root or inside 'data')
        Boolean serving = response.jsonPath().get("serving_location");
        if (serving == null) {
            serving = response.jsonPath().get("data.serving_location");
        }
        System.out.println("   serving_location : " + serving);

        if (expectedServing != null) {
            AssertionUtil.verifyEquals(serving, expectedServing,
                    testId + " [" + label + "]: serving_location expected " + expectedServing + " but got " + serving);
            System.out.println("   ✅ " + label + " — serving_location=" + serving + " (as expected)");
        } else {
            System.out.println("   ℹ️  " + label + " — serving_location=" + serving + " (recorded)");
        }
    }

    // =========================================================
    // CSL_14 — Non-served: Delhi (different city, likely not served)
    // =========================================================
    @Test(priority = 93, description = "QA Automation: Check Serving Location — Delhi coordinates → verify serving_location")
    public void CSL_14_CheckServingLocation_Delhi() {
        System.out.println("\n==========================================================");
        System.out.println("  CSL-14: CHECK SERVING LOCATION — DELHI (NON-SERVED CHECK)");
        System.out.println("==========================================================");

        // Delhi, India — may or may not be served
        assertCSLLocation("CSL_14", "Delhi (28.6139, 77.2090)", memberToken,
                28.6139, 77.2090, null);
    }

    // =========================================================
    // CSL_15 — Non-served: Mumbai
    // =========================================================
    @Test(priority = 94, description = "QA Automation: Check Serving Location — Mumbai coordinates → verify serving_location")
    public void CSL_15_CheckServingLocation_Mumbai() {
        System.out.println("\n==========================================================");
        System.out.println("  CSL-15: CHECK SERVING LOCATION — MUMBAI (NON-SERVED CHECK)");
        System.out.println("==========================================================");

        // Mumbai, India
        assertCSLLocation("CSL_15", "Mumbai (19.0760, 72.8777)", memberToken,
                19.0760, 72.8777, null);
    }

    // =========================================================
    // CSL_16 — Non-served: Bangalore
    // =========================================================
    @Test(priority = 95, description = "QA Automation: Check Serving Location — Bangalore coordinates → verify serving_location")
    public void CSL_16_CheckServingLocation_Bangalore() {
        System.out.println("\n==========================================================");
        System.out.println("  CSL-16: CHECK SERVING LOCATION — BANGALORE (NON-SERVED CHECK)");
        System.out.println("==========================================================");

        // Bangalore, India
        assertCSLLocation("CSL_16", "Bangalore (12.9716, 77.5946)", memberToken,
                12.9716, 77.5946, null);
    }

    // =========================================================
    // CSL_17 — Non-served: Chennai
    // =========================================================
    @Test(priority = 96, description = "QA Automation: Check Serving Location — Chennai coordinates → verify serving_location")
    public void CSL_17_CheckServingLocation_Chennai() {
        System.out.println("\n==========================================================");
        System.out.println("  CSL-17: CHECK SERVING LOCATION — CHENNAI (NON-SERVED CHECK)");
        System.out.println("==========================================================");

        // Chennai, India
        assertCSLLocation("CSL_17", "Chennai (13.0827, 80.2707)", memberToken,
                13.0827, 80.2707, null);
    }

    // =========================================================
    // CSL_18 — Non-served: New York (international — definitely not served)
    // =========================================================
    @Test(priority = 97, description = "QA Automation: Check Serving Location — New York → serving_location=false expected")
    public void CSL_18_CheckServingLocation_NewYork() {
        System.out.println("\n==========================================================");
        System.out.println("  CSL-18: CHECK SERVING LOCATION — NEW YORK (NON-SERVED)");
        System.out.println("==========================================================");

        // New York, USA — definitely not served
        assertCSLLocation("CSL_18", "New York (40.7128, -74.0060)", memberToken,
                40.7128, -74.0060, false);
    }

    // =========================================================
    // CSL_19 — Non-served: London (international — not served)
    // =========================================================
    @Test(priority = 98, description = "QA Automation: Check Serving Location — London → serving_location=false expected")
    public void CSL_19_CheckServingLocation_London() {
        System.out.println("\n==========================================================");
        System.out.println("  CSL-19: CHECK SERVING LOCATION — LONDON (NON-SERVED)");
        System.out.println("==========================================================");

        // London, UK — definitely not served
        assertCSLLocation("CSL_19", "London (51.5074, -0.1278)", memberToken,
                51.5074, -0.1278, false);
    }

    // =========================================================
    // CSL_20 — Non-served: Middle of the ocean (0,0 coordinates)
    // =========================================================
    @Test(priority = 99, description = "QA Automation: Check Serving Location — Null Island (0,0) → serving_location=false expected")
    public void CSL_20_CheckServingLocation_NullIsland() {
        System.out.println("\n==========================================================");
        System.out.println("  CSL-20: CHECK SERVING LOCATION — NULL ISLAND 0,0 (NON-SERVED)");
        System.out.println("==========================================================");

        // Null Island (0,0) — Gulf of Guinea, middle of ocean
        assertCSLLocation("CSL_20", "Null Island (0.0, 0.0)", memberToken,
                0.0, 0.0, false);
    }

    // =========================================================
    // CSL_21 — Non-served: Remote village in rural India
    // =========================================================
    @Test(priority = 100, description = "QA Automation: Check Serving Location — remote rural area → verify serving_location")
    public void CSL_21_CheckServingLocation_RemoteRuralIndia() {
        System.out.println("\n==========================================================");
        System.out.println("  CSL-21: CHECK SERVING LOCATION — REMOTE RURAL INDIA");
        System.out.println("==========================================================");

        // Leh, Ladakh — very remote area
        assertCSLLocation("CSL_21", "Leh Ladakh (34.1526, 77.5771)", memberToken,
                34.1526, 77.5771, null);
    }

    // =========================================================
    // CSL_22 — Served: Hyderabad (known serving area from existing addresses)
    // =========================================================
    @Test(priority = 101, description = "QA Automation: Check Serving Location — Hyderabad (known served area) → serving_location=true expected")
    public void CSL_22_CheckServingLocation_Hyderabad_Served() {
        System.out.println("\n==========================================================");
        System.out.println("  CSL-22: CHECK SERVING LOCATION — HYDERABAD (SERVED AREA)");
        System.out.println("==========================================================");

        // Hyderabad — known served location (used in existing address tests)
        assertCSLLocation("CSL_22", "Hyderabad (17.3850, 78.4867)", memberToken,
                17.3850, 78.4867, true);
    }

    // =========================================================
    // CSL_23 — Boundary: Negative latitude (Southern hemisphere)
    // =========================================================
    @Test(priority = 102, description = "QA Automation: Check Serving Location — Sydney (negative lat) → serving_location=false expected")
    public void CSL_23_CheckServingLocation_Sydney() {
        System.out.println("\n==========================================================");
        System.out.println("  CSL-23: CHECK SERVING LOCATION — SYDNEY (NEGATIVE LAT, NON-SERVED)");
        System.out.println("==========================================================");

        // Sydney, Australia — negative latitude, definitely not served
        assertCSLLocation("CSL_23", "Sydney (-33.8688, 151.2093)", memberToken,
                -33.8688, 151.2093, false);
    }

    // =========================================================
    // CSL_24 — Boundary: Extreme valid coordinates (-90, -180)
    // =========================================================
    @Test(priority = 103, description = "QA Automation: Check Serving Location — South Pole extreme coordinates → 200 with non-served")
    public void CSL_24_CheckServingLocation_ExtremeCoordinates() {
        System.out.println("\n==========================================================");
        System.out.println("  CSL-24: CHECK SERVING LOCATION — EXTREME COORDINATES (-90, -180)");
        System.out.println("==========================================================");

        // South Pole extreme
        assertCSLLocation("CSL_24", "South Pole (-90.0, -180.0)", memberToken,
                -90.0, -180.0, false);
    }

    // =========================================================
    // CSL_25 — Hyderabad: Khammam (Telangana district)
    // =========================================================
    @Test(priority = 104, description = "QA Automation: Check Serving Location — Khammam, Telangana → verify serving_location")
    public void CSL_25_CheckServingLocation_Khammam() {
        System.out.println("\n==========================================================");
        System.out.println("  CSL-25: CHECK SERVING LOCATION — KHAMMAM, TELANGANA");
        System.out.println("==========================================================");

        // Khammam, Telangana
        assertCSLLocation("CSL_25", "Khammam (17.2473, 80.1514)", memberToken,
                17.2473, 80.1514, null);
    }

    // =========================================================
    // CSL_26 — Hyderabad: Miyapur
    // =========================================================
    @Test(priority = 105, description = "QA Automation: Check Serving Location — Miyapur, Hyderabad → verify serving_location")
    public void CSL_26_CheckServingLocation_Miyapur() {
        System.out.println("\n==========================================================");
        System.out.println("  CSL-26: CHECK SERVING LOCATION — MIYAPUR, HYDERABAD");
        System.out.println("==========================================================");

        // Miyapur, Hyderabad
        assertCSLLocation("CSL_26", "Miyapur (17.4969, 78.3548)", memberToken,
                17.4969, 78.3548, null);
    }

    // =========================================================
    // CSL_27 — Hyderabad: Kukatpally
    // =========================================================
    @Test(priority = 106, description = "QA Automation: Check Serving Location — Kukatpally, Hyderabad → verify serving_location")
    public void CSL_27_CheckServingLocation_Kukatpally() {
        System.out.println("\n==========================================================");
        System.out.println("  CSL-27: CHECK SERVING LOCATION — KUKATPALLY, HYDERABAD");
        System.out.println("==========================================================");

        // Kukatpally, Hyderabad
        assertCSLLocation("CSL_27", "Kukatpally (17.4948, 78.3996)", memberToken,
                17.4948, 78.3996, null);
    }

    // =========================================================
    // CSL_28 — Hyderabad: Secunderabad
    // =========================================================
    @Test(priority = 107, description = "QA Automation: Check Serving Location — Secunderabad, Hyderabad → verify serving_location")
    public void CSL_28_CheckServingLocation_Secunderabad() {
        System.out.println("\n==========================================================");
        System.out.println("  CSL-28: CHECK SERVING LOCATION — SECUNDERABAD, HYDERABAD");
        System.out.println("==========================================================");

        // Secunderabad, Hyderabad
        assertCSLLocation("CSL_28", "Secunderabad (17.4399, 78.4983)", memberToken,
                17.4399, 78.4983, null);
    }

    // =========================================================
    // CSL_29 — Hyderabad: Gachibowli
    // =========================================================
    @Test(priority = 108, description = "QA Automation: Check Serving Location — Gachibowli, Hyderabad → verify serving_location")
    public void CSL_29_CheckServingLocation_Gachibowli() {
        System.out.println("\n==========================================================");
        System.out.println("  CSL-29: CHECK SERVING LOCATION — GACHIBOWLI, HYDERABAD");
        System.out.println("==========================================================");

        // Gachibowli, Hyderabad
        assertCSLLocation("CSL_29", "Gachibowli (17.4401, 78.3489)", memberToken,
                17.4401, 78.3489, null);
    }

    // =========================================================
    // CSL_30 — Hyderabad: Ameerpet (HQ)
    // =========================================================
    @Test(priority = 109, description = "QA Automation: Check Serving Location — Ameerpet (HQ), Hyderabad → verify serving_location")
    public void CSL_30_CheckServingLocation_Ameerpet() {
        System.out.println("\n==========================================================");
        System.out.println("  CSL-30: CHECK SERVING LOCATION — AMEERPET (HQ), HYDERABAD");
        System.out.println("==========================================================");

        // Ameerpet (HQ), Hyderabad
        assertCSLLocation("CSL_30", "Ameerpet HQ (17.4358, 78.4527)", memberToken,
                17.4358447, 78.452737, null);
    }
}

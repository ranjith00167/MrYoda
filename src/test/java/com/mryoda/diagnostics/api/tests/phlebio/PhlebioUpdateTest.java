package com.mryoda.diagnostics.api.tests.phlebio;

import api.phlebio.PhlebioClient;
import api.phlebio.PhlebioPayloads;
import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.config.ConfigLoader;
import com.mryoda.diagnostics.api.utils.LoggerUtil;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.UUID;

/**
 * ============================================================
 * PHLEBIO — UPDATE PHLEBO  (PUT /api/v1/phlebo)
 * Base URL : https://staging-api-phlebo-notification.yodadiagnostics.com
 *
 * Flow:
 *   @BeforeClass  → savePhlebo (POST /api/v1/phlebo/savePhlebo)
 *                   extracts guid, mobile, password of the newly created phlebo.
 *
 * Request body: { "guid": "<uuid>", "name": "<name>", "email": "<email>" }
 * Token type  : phlebo Bearer JWT (role=phlebo)
 *               Falls back to admin token if phlebo login is unavailable.
 *
 * ── POSITIVE ──────────────────────────────────────────────
 *   UPDATE_01 : Valid guid + new name + new email            → 200 + GET cross-verify
 *   UPDATE_02 : Valid guid + second new name + same email    → 200 + GET cross-verify
 *   UPDATE_03 : Valid guid + same name + second new email    → 200 + GET cross-verify
 *
 * ── NEGATIVE — GUID VALIDATION ────────────────────────────
 *   UPDATE_NEG_01 : Empty guid ("")              → 400
 *   UPDATE_NEG_02 : Missing guid field           → 400
 *   UPDATE_NEG_03 : Non-existent valid-format UUID → 404
 *   UPDATE_NEG_04 : Malformed guid ("NOT-A-GUID") → 400
 *
 * ── NEGATIVE — NAME VALIDATION ────────────────────────────
 *   UPDATE_NEG_05 : Missing name field           → 400
 *   UPDATE_NEG_06 : Empty name ("")              → 400
 *   UPDATE_NEG_07 : Name exceeding max length    → 400
 *
 * ── NEGATIVE — EMAIL VALIDATION ───────────────────────────
 *   UPDATE_NEG_08 : Missing email field (optional) → 200  (email is not mandatory)
 *   UPDATE_NEG_09 : Empty email ("")             → 400
 *   UPDATE_NEG_10 : No @ symbol ("notanemail")   → 400
 *   UPDATE_NEG_11 : No domain ("user@")          → 400
 *   UPDATE_NEG_12 : No TLD ("user@domain")       → 400
 * ============================================================
 */
public class PhlebioUpdateTest extends BaseTest {

    private static String adminToken;
    private static String phleboToken;   // own JWT of the created phlebo
    private static String createdGuid;   // guid of the phlebo created in @BeforeClass
    private static String updateGuid;    // guid to use in positive update tests (own guid from token)

    private final PhlebioClient phlebioClient = new PhlebioClient();

    // ─────────────────────────────────────────────────────────────────────────
    // Setup — admin login → create phlebo → phlebo login
    // ─────────────────────────────────────────────────────────────────────────
    @BeforeClass(alwaysRun = true)
    @Override
    public void setUp() {
        LoggerUtil.info("====== Phlebio Update Test Setup Started ======");
        RestAssured.baseURI = ConfigLoader.getConfig().phlebioBaseUrl();

        // ── Step 1: Admin token (diagnostics service) ──────────────────────
        try {
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("identifier", ConfigLoader.getConfig().adminMainIdentifier());
            payload.put("password",   ConfigLoader.getConfig().adminMainPassword());
            payload.put("type",       "login");
            payload.put("fcmToken",   "ec0gPKSrIUs443ILfDLHaM:APA91bG_test_admin_fcm");
            Response r = new com.mryoda.diagnostics.api.builders.RequestBuilder()
                    .setEndpoint(
                        com.mryoda.diagnostics.api.endpoints.APIEndpoints.DIAGNOSTICS_BASE_URL
                        + "/auth/login")
                    .setRequestBody(payload)
                    .post();
            if (r.getStatusCode() == 200 || r.getStatusCode() == 201) {
                adminToken = r.jsonPath().getString("data.access_token");
            }
        } catch (Exception e) {
            LoggerUtil.info("Admin login exception: " + e.getMessage());
        }
        Assert.assertNotNull(adminToken, "Setup failed: could not obtain admin token");
        LoggerUtil.info("   Admin token obtained.");

        // ── Step 2: Create a fresh phlebo → capture guid for negative GUID tests ─
        java.util.Map<String, String> formParams = PhlebioPayloads.buildSavePhleboFormParams();
        String createdMobile   = formParams.get("mobile_number");
        String createdPassword = formParams.get("password");
        LoggerUtil.info("   Creating test phlebo | mobile: " + createdMobile);

        Response createResp = phlebioClient.savePhleboAsFormData(formParams, adminToken);
        Assert.assertTrue(
                createResp.getStatusCode() == 200 || createResp.getStatusCode() == 201,
                "Setup failed: savePhlebo returned " + createResp.getStatusCode()
                        + " | Body: " + createResp.asString());

        createdGuid = createResp.jsonPath().getString("data.guid");
        Assert.assertNotNull(createdGuid,
                "Setup failed: 'data.guid' not present in savePhlebo response.\nBody: " + createResp.asString());
        LoggerUtil.info("   Phlebo created. GUID: " + createdGuid);

        // ── Step 3: Resolve the token to use for update calls ──────────────────
        // Try phlebo login first (per the cURL spec), then fall back to admin.
        // Positive tests: use the phlebo's own guid with whichever token works.
        phleboToken = tryPhleboLogin(PhlebioPayloads.buildLoginPayload(), "existing staging phlebo");

        if (phleboToken == null) {
            System.out.println("   [SETUP] Phlebo login failed — trying newly created phlebo...");
            JSONObject createdPhleboLogin = new JSONObject();
            createdPhleboLogin.put("mobile",   createdMobile);
            createdPhleboLogin.put("password", createdPassword);
            createdPhleboLogin.put("token",    PhlebioPayloads.FCM_TOKEN);
            phleboToken = tryPhleboLogin(createdPhleboLogin, "newly created phlebo");
        }

        if (phleboToken == null) {
            System.out.println("   [SETUP] All phlebo logins failed — falling back to admin token.");
            phleboToken = adminToken;
        } else {
            System.out.println("   [SETUP] Phlebo token obtained successfully.");
        }

        // ── Step 4: Resolve updateGuid ─────────────────────────────────────────
        // Positive tests use adminToken + createdGuid (matching savePhlebo auth pattern).
        // If phlebo token was obtained, extract its phlebo_id for reference only.
        updateGuid = createdGuid;   // positive tests always update the created phlebo
        System.out.println("   [SETUP] updateGuid (created phlebo): " + updateGuid);

        LoggerUtil.info("====== Phlebio Update Test Setup Completed ======");
    }

    /**
     * Decodes the JWT payload (middle segment) and returns the value of the
     * "phlebo_id" claim, which the PUT endpoint uses to authorise self-update.
     * Returns null if the token cannot be decoded or the claim is absent.
     */
    private String extractPhleboIdFromToken(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length != 3) return null;
            byte[] decoded = java.util.Base64.getUrlDecoder().decode(parts[1] + "==");
            String payload = new String(decoded);
            // Pull out "phlebo_id":"<value>" via simple substring (no extra deps needed)
            int idx = payload.indexOf("\"phlebo_id\"");
            if (idx < 0) return null;
            int colon = payload.indexOf(":", idx);
            int open  = payload.indexOf("\"", colon);
            int close = payload.indexOf("\"", open + 1);
            if (open < 0 || close < 0) return null;
            return payload.substring(open + 1, close);
        } catch (Exception e) {
            System.out.println("   [WARN] JWT decode failed: " + e.getMessage());
            return null;
        }
    }

    /** Attempts phlebo login and returns the access token, or null on failure. */
    private String tryPhleboLogin(JSONObject loginPayload, String label) {
        JSONObject[] variants = {
            loginPayload,
            buildAltMobilePayload(loginPayload)
        };
        for (JSONObject payload : variants) {
            try {
                Response r = phlebioClient.login(payload);
                int status = r.getStatusCode();
                System.out.println("   [SETUP] Phlebo login [" + label + "] status: " + status);
                if (status == 200 || status == 201) {
                    // Try all common token paths in phlebo login response
                    String[] tokenPaths = {
                        "data.access_token",
                        "data.token",
                        "token",
                        "data.tokens.access_token",
                        "data.phlebo.access_token",
                        "data.data.access_token"
                    };
                    for (String path : tokenPaths) {
                        String token = r.jsonPath().getString(path);
                        if (token != null && !token.isEmpty()) {
                            System.out.println("   [SETUP] Token found at path: " + path);
                            return token;
                        }
                    }
                    // No token found at known paths — print partial body for diagnosis
                    String body = r.asString();
                    System.out.println("   [SETUP] Login 200 but token not found at known paths. "
                            + "Body (first 600 chars): " + body.substring(0, Math.min(600, body.length())));
                } else {
                    String body = r.asString();
                    System.out.println("   [SETUP] Login failed " + status + ": "
                            + body.substring(0, Math.min(300, body.length())));
                }
            } catch (Exception e) {
                System.out.println("   [SETUP] Phlebo login [" + label + "] exception: " + e.getMessage());
            }
        }
        return null;
    }

    /** Returns a copy of the payload with 'mobile' renamed to 'mobile_number'. */
    private JSONObject buildAltMobilePayload(JSONObject original) {
        JSONObject alt = new JSONObject(original.toString());
        if (alt.has("mobile") && !alt.has("mobile_number")) {
            alt.put("mobile_number", alt.getString("mobile"));
            alt.remove("mobile");
        }
        return alt;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Shared helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Validates the PUT response: HTTP 200/201 and success==true.
     * Optionally checks that expectedName / expectedEmail appear in the response body
     * (pass null to skip a field check).
     */
    private void assertUpdateSuccess(String testId, Response response,
                                     String expectedName, String expectedEmail) {
        int status = response.getStatusCode();
        System.out.println("   [" + testId + "] Status : " + status);
        System.out.println("   [" + testId + "] Body   : " + response.asString());

        Assert.assertTrue(status == 200 || status == 201,
                testId + ": Expected HTTP 200/201 from PUT /api/v1/phlebo but got " + status);

        Boolean success = response.jsonPath().getBoolean("success");
        Assert.assertTrue(Boolean.TRUE.equals(success),
                testId + ": 'success' must be true in response. Got: " + response.asString());

        // Validate updated name in response body (if API echoes it back)
        if (expectedName != null) {
            String nameInResp = response.jsonPath().getString("data.name");
            if (nameInResp != null) {
                Assert.assertEquals(nameInResp, expectedName,
                        testId + ": Response 'data.name' must reflect the updated name");
                System.out.println("   [" + testId + "] data.name verified: " + nameInResp);
            } else {
                System.out.println("   [" + testId + "] data.name not in PUT response body — will verify via GET");
            }
        }

        // Validate updated email in response body (if API echoes it back)
        if (expectedEmail != null) {
            String emailInResp = response.jsonPath().getString("data.email");
            if (emailInResp != null) {
                Assert.assertEquals(emailInResp, expectedEmail,
                        testId + ": Response 'data.email' must reflect the updated email");
                System.out.println("   [" + testId + "] data.email verified: " + emailInResp);
            } else {
                System.out.println("   [" + testId + "] data.email not in PUT response body — will verify via GET");
            }
        }

        System.out.println("   [PASS] " + testId + "\n");
    }

    /**
     * Cross-verifies that the update persisted by calling GET /api/v1/phlebo/{guid}
     * and asserting that the returned name and email match what was sent in the PUT.
     */
    private void assertFieldPersisted(String testId, String guid,
                                      String expectedName, String expectedEmail) {
        System.out.println("   [" + testId + "] Cross-verifying via GET /api/v1/phlebo/" + guid);
        Response getResp = phlebioClient.getPhleboByGuid(guid, adminToken);

        System.out.println("   [" + testId + "] GET Status : " + getResp.getStatusCode());
        System.out.println("   [" + testId + "] GET Body   : " + getResp.asString());

        Assert.assertEquals(getResp.getStatusCode(), 200,
                testId + ": GET after update must return 200. Actual: " + getResp.getStatusCode());

        // Verify name persisted
        if (expectedName != null) {
            String nameInGet = getResp.jsonPath().getString("data.name");
            Assert.assertNotNull(nameInGet, testId + ": GET response must contain data.name");
            Assert.assertEquals(nameInGet, expectedName,
                    testId + ": Persisted name must match updated name. Expected: '"
                            + expectedName + "' Actual: '" + nameInGet + "'");
            System.out.println("   [" + testId + "] Persisted name  : " + nameInGet + " ✓");
        }

        // Verify email persisted (field name may differ: email / emailId)
        if (expectedEmail != null) {
            String emailInGet = getResp.jsonPath().getString("data.email");
            if (emailInGet == null) emailInGet = getResp.jsonPath().getString("data.emailId");
            if (emailInGet != null) {
                Assert.assertEquals(emailInGet, expectedEmail,
                        testId + ": Persisted email must match updated email. Expected: '"
                                + expectedEmail + "' Actual: '" + emailInGet + "'");
                System.out.println("   [" + testId + "] Persisted email : " + emailInGet + " ✓");
            } else {
                System.out.println("   [" + testId + "] data.email not in GET response — field may not be returned by this API version");
            }
        }

        System.out.println("   [" + testId + "] GET cross-verify PASSED\n");
    }

    private void assertValidationError(String testId, Response response, String context) {
        int status = response.getStatusCode();
        System.out.println("   [" + testId + "] Status : " + status);
        System.out.println("   [" + testId + "] Body   : " + response.asString());
        Assert.assertTrue(status >= 400 && status < 500,
                testId + " Expected HTTP 4xx for [" + context + "]. Actual: " + status
                        + ". DEFECT: API accepted invalid input — add server-side validation.");
        System.out.println("   [PASS] " + testId + " correctly rejected: " + context + "\n");
    }

    // =========================================================
    // POSITIVE SCENARIOS
    // =========================================================

    @Test(priority = 1,
          description = "UPDATE_01: PUT /api/v1/phlebo with valid guid, new name, new email → 200 OK. "
                      + "GUID is extracted from savePhlebo response (@BeforeClass). "
                      + "Name and email are dynamically generated. "
                      + "Cross-verifies persisted changes via GET /api/v1/phlebo/{guid}.")
    public void UPDATE_01_ValidUpdate() {
        System.out.println("\n>>> UPDATE_01: VALID UPDATE — guid + new name + new email <<<");

        String newName  = "Updated " + PhlebioPayloads.randomName();
        String newEmail = "updated_" + System.currentTimeMillis() + "@testmail.com";
        System.out.println("   guid  : " + updateGuid);
        System.out.println("   name  : " + newName);
        System.out.println("   email : " + newEmail);

        // PUT — update the phlebo
        Response response = phlebioClient.updatePhlebo(updateGuid, newName, newEmail, adminToken);
        assertUpdateSuccess("UPDATE_01", response, newName, newEmail);

        // Cross-verify — GET must reflect the updated values
        assertFieldPersisted("UPDATE_01", updateGuid, newName, newEmail);
    }

    @Test(priority = 2,
          description = "UPDATE_02: PUT /api/v1/phlebo — second sequential update with a different dynamic name → 200 OK. "
                      + "Verifies last-write-wins behaviour. "
                      + "Cross-verifies persisted changes via GET /api/v1/phlebo/{guid}.")
    public void UPDATE_02_SecondNameUpdate() {
        System.out.println("\n>>> UPDATE_02: SECOND UPDATE — different name <<<");

        String newName   = "Revised " + PhlebioPayloads.randomName();
        String sameEmail = "stable_" + System.currentTimeMillis() + "@testmail.com";
        System.out.println("   guid  : " + updateGuid);
        System.out.println("   name  : " + newName);
        System.out.println("   email : " + sameEmail);

        // PUT — second sequential update
        Response response = phlebioClient.updatePhlebo(updateGuid, newName, sameEmail, adminToken);
        assertUpdateSuccess("UPDATE_02", response, newName, sameEmail);

        // Cross-verify — previous name must be overwritten, new name must be stored
        assertFieldPersisted("UPDATE_02", updateGuid, newName, sameEmail);
    }

    @Test(priority = 3,
          description = "UPDATE_03: PUT /api/v1/phlebo — update with a new dynamic email address → 200 OK. "
                      + "Cross-verifies that the new email is persisted via GET /api/v1/phlebo/{guid}.")
    public void UPDATE_03_EmailUpdate() {
        System.out.println("\n>>> UPDATE_03: EMAIL UPDATE — new email address <<<");

        String sameName = "Stable " + PhlebioPayloads.randomName();
        String newEmail = "newemail_" + System.currentTimeMillis() + "@testmail.com";
        System.out.println("   guid  : " + updateGuid);
        System.out.println("   name  : " + sameName);
        System.out.println("   email : " + newEmail);

        // PUT — update with new email
        Response response = phlebioClient.updatePhlebo(updateGuid, sameName, newEmail, adminToken);
        assertUpdateSuccess("UPDATE_03", response, sameName, newEmail);

        // Cross-verify — updated email must be persisted
        assertFieldPersisted("UPDATE_03", updateGuid, sameName, newEmail);
    }

    // =========================================================
    // NEGATIVE SCENARIOS — GUID VALIDATION
    // =========================================================

    @Test(priority = 4,
          description = "UPDATE_NEG_01: guid=\"\" (empty string) → 400 Bad Request. "
                      + "API must reject blank guid rather than looking up an empty string.")
    public void UPDATE_NEG_01_EmptyGuid() {
        System.out.println("\n>>> UPDATE_NEG_01: EMPTY GUID <<<");

        JSONObject body = new JSONObject();
        body.put("guid",  "");
        body.put("name",  "Valid Name");
        body.put("email", "valid@testmail.com");

        Response response = phlebioClient.updatePhleboRaw(body, adminToken);
        assertValidationError("UPDATE_NEG_01", response, "guid=\"\"");
    }

    @Test(priority = 5,
          description = "UPDATE_NEG_02: guid field missing from body → 400 Bad Request. "
                      + "API must enforce guid as a mandatory field.")
    public void UPDATE_NEG_02_MissingGuid() {
        System.out.println("\n>>> UPDATE_NEG_02: MISSING GUID FIELD <<<");

        JSONObject body = new JSONObject();
        // guid field intentionally omitted
        body.put("name",  "Valid Name");
        body.put("email", "valid@testmail.com");

        Response response = phlebioClient.updatePhleboRaw(body, adminToken);
        assertValidationError("UPDATE_NEG_02", response, "guid field missing");
    }

    @Test(priority = 6,
          description = "UPDATE_NEG_03: Non-existent but valid-format UUID → 404 Not Found. "
                      + "API must return 404 when no phlebo matches the provided guid.")
    public void UPDATE_NEG_03_NonExistentGuid() {
        System.out.println("\n>>> UPDATE_NEG_03: NON-EXISTENT GUID <<<");

        String phantomGuid = UUID.randomUUID().toString();
        System.out.println("   phantom guid: " + phantomGuid);

        JSONObject body = new JSONObject();
        body.put("guid",  phantomGuid);
        body.put("name",  "Some Name");
        body.put("email", "some@testmail.com");

        Response response = phlebioClient.updatePhleboRaw(body, adminToken);
        int status = response.getStatusCode();
        System.out.println("   [UPDATE_NEG_03] Status : " + status);
        System.out.println("   [UPDATE_NEG_03] Body   : " + response.asString());

        Assert.assertEquals(status, 404,
                "UPDATE_NEG_03 FAILED: Non-existent guid must return 404 but got " + status
                        + ". DEFECT: API should return 404 when the phlebo record is not found.");
        System.out.println("   [PASS] UPDATE_NEG_03 — correctly returned 404 for non-existent guid\n");
    }

    @Test(priority = 7,
          description = "UPDATE_NEG_04: Malformed guid (\"NOT-A-GUID\") → 400 Bad Request. "
                      + "API must validate UUID format before querying the database.")
    public void UPDATE_NEG_04_InvalidGuidFormat() {
        System.out.println("\n>>> UPDATE_NEG_04: MALFORMED GUID FORMAT <<<");

        JSONObject body = new JSONObject();
        body.put("guid",  "NOT-A-VALID-GUID-FORMAT");
        body.put("name",  "Valid Name");
        body.put("email", "valid@testmail.com");

        Response response = phlebioClient.updatePhleboRaw(body, adminToken);
        assertValidationError("UPDATE_NEG_04", response, "guid='NOT-A-VALID-GUID-FORMAT'");
    }

    // =========================================================
    // NEGATIVE SCENARIOS — NAME VALIDATION
    // =========================================================

    @Test(priority = 8,
          description = "UPDATE_NEG_05: name field missing from body → 400 Bad Request. "
                      + "API must enforce name as a mandatory field.")
    public void UPDATE_NEG_05_MissingName() {
        System.out.println("\n>>> UPDATE_NEG_05: MISSING NAME FIELD <<<");

        JSONObject body = new JSONObject();
        body.put("guid",  createdGuid);
        // name field intentionally omitted
        body.put("email", "valid@testmail.com");

        Response response = phlebioClient.updatePhleboRaw(body, adminToken);
        assertValidationError("UPDATE_NEG_05", response, "name field missing");
    }

    @Test(priority = 9,
          description = "UPDATE_NEG_06: name=\"\" (empty string) → 400 Bad Request. "
                      + "API must reject blank phlebo names.")
    public void UPDATE_NEG_06_EmptyName() {
        System.out.println("\n>>> UPDATE_NEG_06: EMPTY NAME <<<");

        JSONObject body = new JSONObject();
        body.put("guid",  createdGuid);
        body.put("name",  "");
        body.put("email", "valid@testmail.com");

        Response response = phlebioClient.updatePhleboRaw(body, adminToken);
        assertValidationError("UPDATE_NEG_06", response, "name=\"\"");
    }

    @Test(priority = 10,
          description = "UPDATE_NEG_07: name with 300 characters → 400 Bad Request. "
                      + "API must enforce a maximum name length to prevent DB overflow.")
    public void UPDATE_NEG_07_NameTooLong() {
        System.out.println("\n>>> UPDATE_NEG_07: NAME TOO LONG (300 chars) <<<");

        String longName = "A".repeat(300);
        System.out.println("   name length: " + longName.length());

        JSONObject body = new JSONObject();
        body.put("guid",  createdGuid);
        body.put("name",  longName);
        body.put("email", "valid@testmail.com");

        Response response = phlebioClient.updatePhleboRaw(body, adminToken);
        assertValidationError("UPDATE_NEG_07", response, "name length=300");
    }

    // =========================================================
    // NEGATIVE SCENARIOS — EMAIL VALIDATION
    // =========================================================

    @Test(priority = 11,
          description = "UPDATE_NEG_08: email field omitted → 200 OK. "
                      + "Email is not a mandatory field; update should succeed without it.")
    public void UPDATE_NEG_08_MissingEmail() {
        System.out.println("\n>>> UPDATE_NEG_08: EMAIL FIELD OMITTED (OPTIONAL) <<<");

        JSONObject body = new JSONObject();
        body.put("guid", createdGuid);
        body.put("name", "Valid Name No Email");
        // email field intentionally omitted — email is optional

        Response response = phlebioClient.updatePhleboRaw(body, adminToken);
        int status = response.getStatusCode();
        String responseBody = response.getBody().asString();
        System.out.println("[UPDATE_NEG_08] Status : " + status);
        System.out.println("[UPDATE_NEG_08] Body   : " + responseBody);
        Assert.assertEquals(status, 200,
            "UPDATE_NEG_08 FAILED: Expected 200 when email is omitted (email is optional), got " + status
            + ". Response: " + responseBody);
        System.out.println("[UPDATE_NEG_08] PASS — email is optional, update succeeded without it.");
    }

    @Test(priority = 12,
          description = "UPDATE_NEG_09: email=\"\" (empty string) → 400 Bad Request. "
                      + "API must reject a blank email address.")
    public void UPDATE_NEG_09_EmptyEmail() {
        System.out.println("\n>>> UPDATE_NEG_09: EMPTY EMAIL <<<");

        JSONObject body = new JSONObject();
        body.put("guid",  createdGuid);
        body.put("name",  "Valid Name");
        body.put("email", "");

        Response response = phlebioClient.updatePhleboRaw(body, adminToken);
        assertValidationError("UPDATE_NEG_09", response, "email=\"\"");
    }

    @Test(priority = 13,
          description = "UPDATE_NEG_10: email=\"notanemail\" (no @ symbol) → 400 Bad Request. "
                      + "API must validate RFC-compliant email format.")
    public void UPDATE_NEG_10_InvalidEmailNoAt() {
        System.out.println("\n>>> UPDATE_NEG_10: INVALID EMAIL — no @ symbol <<<");

        JSONObject body = new JSONObject();
        body.put("guid",  createdGuid);
        body.put("name",  "Valid Name");
        body.put("email", "notanemail");

        Response response = phlebioClient.updatePhleboRaw(body, adminToken);
        assertValidationError("UPDATE_NEG_10", response, "email='notanemail' (no @)");
    }

    @Test(priority = 14,
          description = "UPDATE_NEG_11: email=\"user@\" (no domain after @) → 400 Bad Request. "
                      + "API must reject emails with missing domain.")
    public void UPDATE_NEG_11_InvalidEmailNoDomain() {
        System.out.println("\n>>> UPDATE_NEG_11: INVALID EMAIL — no domain after @ <<<");

        JSONObject body = new JSONObject();
        body.put("guid",  createdGuid);
        body.put("name",  "Valid Name");
        body.put("email", "user@");

        Response response = phlebioClient.updatePhleboRaw(body, adminToken);
        assertValidationError("UPDATE_NEG_11", response, "email='user@' (missing domain)");
    }

    @Test(priority = 15,
          description = "UPDATE_NEG_12: email=\"user@domain\" (no TLD — no dot in domain) → 400 Bad Request. "
                      + "API must require a valid domain with TLD extension.")
    public void UPDATE_NEG_12_InvalidEmailNoTld() {
        System.out.println("\n>>> UPDATE_NEG_12: INVALID EMAIL — no TLD <<<");

        JSONObject body = new JSONObject();
        body.put("guid",  createdGuid);
        body.put("name",  "Valid Name");
        body.put("email", "user@domain");

        Response response = phlebioClient.updatePhleboRaw(body, adminToken);
        assertValidationError("UPDATE_NEG_12", response, "email='user@domain' (no TLD)");
    }
}

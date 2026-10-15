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

/**
 * ============================================================
 * PHLEBIO -- REGISTRATION + UPDATE  (Create → Update Phlebo)
 *
 * PHLEBO_REG_01 : Admin Login          — obtain admin Bearer token
 * PHLEBO_REG_02 : Create Phlebo        — POST /api/v1/phlebo/savePhlebo (form-data)
 *                                         extracts created phlebo guid for REG-03
 * PHLEBO_REG_03 : Update Phlebo        — PUT  /api/v1/phlebo
 *                                         { guid (from REG-02), name (dynamic), email (dynamic) }
 *                                         Cross-verifies persisted changes via
 *                                         GET /api/v1/phlebo/{guid}
 *
 * Base URL : https://staging-api-phlebo-notification.yodadiagnostics.com
 *
 * Dynamic fields (randomised per run to prevent duplicate-key errors):
 *   mobileNumber  — random 10-digit Indian mobile (7xx / 8xx / 9xx prefix)
 *   password      — random password satisfying complexity rules
 *   email         — phlebo_<timestamp>@testmail.com
 * ============================================================
 */
public class PhlebioRegistrationTest extends BaseTest {

    // ── Shared state ──────────────────────────────────────────────────────────
    /** Admin token from diagnostics service — used to auth savePhlebo and updatePhlebo. */
    private static String phleboToken;
    /** GUID of the phlebo created in PHLEBO_REG_02 — used by PHLEBO_REG_03 (update). */
    private static String createdPhleboGuid;

    private final PhlebioClient phlebioClient = new PhlebioClient();

    @BeforeClass(alwaysRun = true)
    @Override
    public void setUp() {
        LoggerUtil.info("====== Phlebio Registration Test Setup Started ======");
        RestAssured.baseURI = ConfigLoader.getConfig().phlebioBaseUrl();
        LoggerUtil.info("Phlebio Base URL: " + RestAssured.baseURI);
        LoggerUtil.info("====== Phlebio Registration Test Setup Completed ======");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PHLEBO_REG_01 : Admin Login (Phlebo Notification Service)
    // savePhlebo requires an admin-role token from the phlebo notification
    // service. The admin panel authenticates via /api/v1/admin/login.
    // Falls back through multiple admin endpoints automatically.
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 1,
          description = "PHLEBO REG-01: Admin Login (Diagnostics Service) — Obtain admin Bearer "
                      + "token required by the savePhlebo endpoint. Uses diagnostics /auth/login "
                      + "which issues a token with role=admin_only accepted by the phlebo service.")
    public void PHLEBO_REG_01_LoginPhlebo() {
        System.out.println("\n>>> PHLEBO_REG_01: ADMIN LOGIN (DIAGNOSTICS SERVICE) <<<");

        // savePhlebo requires a token with role=admin_only / is_admin_user=true.
        // This token is issued by the diagnostics admin login, not the phlebo service itself.
        String diagnosticsBaseUrl = com.mryoda.diagnostics.api.endpoints.APIEndpoints.DIAGNOSTICS_BASE_URL;
        String adminLoginUrl      = diagnosticsBaseUrl + "/auth/login";
        String adminId            = ConfigLoader.getConfig().adminMainIdentifier();
        String adminPwd           = ConfigLoader.getConfig().adminMainPassword();

        System.out.println("   Admin Login URL  : " + adminLoginUrl);
        System.out.println("   Admin Identifier : " + adminId);

        phleboToken = tryAdminLogin(adminLoginUrl, adminId, adminPwd,
                "Diagnostics /auth/login (admin.main.identifier)");

        Assert.assertNotNull(phleboToken,
                "PHLEBO_REG_01: Could not obtain an admin token from " + adminLoginUrl + ".\n"
                        + "  Verify admin.main.identifier and admin.main.password in\n"
                        + "  src/test/resources/config.properties are correct.\n"
                        + "  The identifier must be the admin mobile number (see JWT mobile field).");
        Assert.assertFalse(phleboToken.isEmpty(), "PHLEBO_REG_01: Token must not be empty");
        System.out.println("   Token (first 40 chars): "
                + phleboToken.substring(0, Math.min(40, phleboToken.length())) + "...");
        System.out.println("   [PASS] PHLEBO_REG_01 PASSED\n");
    }

    /** Sends an admin login POST and returns the token, or null if not 2xx. */
    private String tryAdminLogin(String endpoint, String identifier, String password, String label) {
        System.out.println("   " + label);
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("identifier", identifier);
        payload.put("password",   password);
        payload.put("type",       "login");
        payload.put("fcmToken",   "ec0gPKSrIUs443ILfDLHaM:APA91bG_test_admin_fcm");
        try {
            Response r = new com.mryoda.diagnostics.api.builders.RequestBuilder()
                    .setEndpoint(endpoint)
                    .setRequestBody(payload)
                    .post();
            System.out.println("   Status  : " + r.getStatusCode() + " | " + r.asString());
            if (r.getStatusCode() == 200 || r.getStatusCode() == 201) {
                return extractToken(r);
            }
        } catch (Exception e) {
            System.out.println("   Exception: " + e.getMessage());
        }
        return null;
    }

    /** Extracts the JWT token from common response paths. */
    private static String extractToken(Response r) {
        String t = r.jsonPath().getString("data.access_token");
        if (t == null || t.isEmpty()) t = r.jsonPath().getString("data.token");
        if (t == null || t.isEmpty()) t = r.jsonPath().getString("access_token");
        if (t == null || t.isEmpty()) t = r.jsonPath().getString("token");
        return (t == null || t.isEmpty()) ? null : t;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PHLEBO_REG_02 : Create Phlebo (savePhlebo)
    // Sends a fully populated payload with randomised mobile, password, email.
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 2,
          dependsOnMethods = {"PHLEBO_REG_01_LoginPhlebo"},
          description = "PHLEBO REG-02: Create Phlebo — POST /api/v1/phlebo/savePhlebo as form-data "
                      + "with dynamic mobile_number and password. Expects HTTP 200/201.")
    public void PHLEBO_REG_02_CreatePhlebo() {
        System.out.println("\n>>> PHLEBO_REG_02: CREATE PHLEBO (savePhlebo — form-data) <<<");

        // Build form-data params with correct field names (mobile_number, zone_id, type[])
        java.util.Map<String, String> formParams = PhlebioPayloads.buildSavePhleboFormParams();

        System.out.println("   Form-Data Fields :");
        formParams.forEach((k, v) -> System.out.println("     " + k + " : " + v));

        Response response = phlebioClient.savePhleboAsFormData(formParams, phleboToken);

        System.out.println("   Response Status : " + response.getStatusCode());
        System.out.println("   Response Body   : " + response.prettyPrint());

        Assert.assertTrue(response.getStatusCode() == 200 || response.getStatusCode() == 201,
                "PHLEBO_REG_02: savePhlebo must return HTTP 200 or 201. Actual: "
                        + response.getStatusCode()
                        + " | Body: " + response.asString());

        Object successFlag = response.jsonPath().get("success");
        if (successFlag != null) {
            Assert.assertTrue(Boolean.TRUE.equals(successFlag),
                    "PHLEBO_REG_02: Response 'success' must be true");
        }

        // Capture the GUID — try all known field paths
        createdPhleboGuid = response.jsonPath().getString("data.guid");
        if (createdPhleboGuid == null) createdPhleboGuid = response.jsonPath().getString("data._id");
        if (createdPhleboGuid == null) createdPhleboGuid = response.jsonPath().getString("data.id");

        if (createdPhleboGuid != null && !createdPhleboGuid.isEmpty()) {
            System.out.println("   Created Phlebo GUID : " + createdPhleboGuid);
        } else {
            System.out.println("   (No phlebo GUID in response — creation confirmed by status code)");
        }

        System.out.println("   [PASS] PHLEBO_REG_02 PASSED\n");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PHLEBO_REG_03 : Update Phlebo (PUT /api/v1/phlebo)
    // Uses the GUID captured from PHLEBO_REG_02 (savePhlebo).
    // name and email are dynamically generated on every run.
    // Cross-verifies the update persisted via GET /api/v1/phlebo/{guid}.
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 3,
          dependsOnMethods = {"PHLEBO_REG_02_CreatePhlebo"},
          description = "PHLEBO REG-03: Update Phlebo — PUT /api/v1/phlebo with the guid from "
                      + "PHLEBO_REG_02 (savePhlebo), a dynamically generated name, and a "
                      + "dynamically generated email. Expects HTTP 200/201 + success:true. "
                      + "Cross-verifies the updated values are persisted via "
                      + "GET /api/v1/phlebo/{guid}.")
    public void PHLEBO_REG_03_UpdatePhlebo() {
        System.out.println("\n>>> PHLEBO_REG_03: UPDATE PHLEBO (PUT /api/v1/phlebo) <<<");

        if (createdPhleboGuid == null || createdPhleboGuid.isEmpty()) {
            throw new org.testng.SkipException(
                    "PHLEBO_REG_03: Skipped — no phlebo GUID was captured from PHLEBO_REG_02. "
                    + "The savePhlebo response may not have returned a 'data.guid' field.");
        }

        // Dynamic name and email — unique per run
        String updatedName  = "Updated " + PhlebioPayloads.randomName();
        String updatedEmail = "reg_update_" + System.currentTimeMillis() + "@testmail.com";

        System.out.println("   guid  : " + createdPhleboGuid);
        System.out.println("   name  : " + updatedName);
        System.out.println("   email : " + updatedEmail);

        // ── PUT — update the freshly created phlebo ────────────────────────────
        Response updateResponse = phlebioClient.updatePhlebo(
                createdPhleboGuid, updatedName, updatedEmail, phleboToken);

        System.out.println("   [PHLEBO_REG_03] PUT Status : " + updateResponse.getStatusCode());
        System.out.println("   [PHLEBO_REG_03] PUT Body   : " + updateResponse.asString());

        Assert.assertTrue(
                updateResponse.getStatusCode() == 200 || updateResponse.getStatusCode() == 201,
                "PHLEBO_REG_03: PUT /api/v1/phlebo must return HTTP 200 or 201. Actual: "
                        + updateResponse.getStatusCode() + " | Body: " + updateResponse.asString());

        Assert.assertTrue(Boolean.TRUE.equals(updateResponse.jsonPath().getBoolean("success")),
                "PHLEBO_REG_03: Response 'success' must be true");

        // Check if API echoes back the updated name / email in the PUT response
        String nameInPut = updateResponse.jsonPath().getString("data.name");
        if (nameInPut != null) {
            Assert.assertEquals(nameInPut, updatedName,
                    "PHLEBO_REG_03: PUT response 'data.name' must equal the updated name");
            System.out.println("   data.name in PUT response verified: " + nameInPut);
        } else {
            System.out.println("   data.name not echoed in PUT response — will verify via GET");
        }

        String emailInPut = updateResponse.jsonPath().getString("data.email");
        if (emailInPut != null) {
            Assert.assertEquals(emailInPut, updatedEmail,
                    "PHLEBO_REG_03: PUT response 'data.email' must equal the updated email");
            System.out.println("   data.email in PUT response verified: " + emailInPut);
        } else {
            System.out.println("   data.email not echoed in PUT response — will verify via GET");
        }

        // ── GET cross-verify — confirm changes persisted in the backend ─────────
        System.out.println("   Cross-verifying via GET /api/v1/phlebo/" + createdPhleboGuid);
        Response getResponse = phlebioClient.getPhleboByGuid(createdPhleboGuid, phleboToken);

        System.out.println("   [PHLEBO_REG_03] GET Status : " + getResponse.getStatusCode());
        System.out.println("   [PHLEBO_REG_03] GET Body   : " + getResponse.asString());

        Assert.assertEquals(getResponse.getStatusCode(), 200,
                "PHLEBO_REG_03: GET /api/v1/phlebo/{guid} after update must return 200. Actual: "
                        + getResponse.getStatusCode());

        // Verify name persisted
        String persistedName = getResponse.jsonPath().getString("data.name");
        Assert.assertNotNull(persistedName, "PHLEBO_REG_03: GET response must contain 'data.name'");
        Assert.assertEquals(persistedName, updatedName,
                "PHLEBO_REG_03: Persisted name must match updated name. "
                        + "Expected: '" + updatedName + "' Actual: '" + persistedName + "'");
        System.out.println("   Persisted name  : " + persistedName + " ✓");

        // Verify email persisted (field may be 'email' or 'emailId' depending on API version)
        String persistedEmail = getResponse.jsonPath().getString("data.email");
        if (persistedEmail == null) persistedEmail = getResponse.jsonPath().getString("data.emailId");
        if (persistedEmail != null) {
            Assert.assertEquals(persistedEmail, updatedEmail,
                    "PHLEBO_REG_03: Persisted email must match updated email. "
                            + "Expected: '" + updatedEmail + "' Actual: '" + persistedEmail + "'");
            System.out.println("   Persisted email : " + persistedEmail + " ✓");
        } else {
            System.out.println("   data.email not returned by GET — field may not be exposed in this API version");
        }

        System.out.println("   [PASS] PHLEBO_REG_03 PASSED — phlebo created and updated successfully\n");
    }
}

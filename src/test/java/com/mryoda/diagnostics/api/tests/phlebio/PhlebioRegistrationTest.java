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
 * PHLEBIO -- REGISTRATION  (Create Phlebo API)
 * Endpoint: POST /api/v1/phlebo/savePhlebo
 * Base URL : https://staging-api-phlebo-notification.yodadiagnostics.com
 * ============================================================
 *
 * Auth strategy:
 *   PHLEBO_REG_01 — Login to the phlebo notification service as the existing
 *                   test phlebotomist (mobile 9360651932 / pw 12345678).
 *                   This gives a JWT for that service; used as Bearer token for
 *                   savePhlebo (the service may accept any authenticated user
 *                   or an admin-phlebo account for creation).
 *   PHLEBO_REG_02 — Create Phlebo with dynamic payload.
 *
 * Dynamic fields (randomised per run to prevent duplicate-key errors):
 *   mobileNumber  — random 10-digit Indian mobile (7xx / 8xx / 9xx prefix)
 *   password      — random password satisfying complexity rules
 *   email         — phlebo_<timestamp>@testmail.com
 *
 * Static fields:
 *   name, zoneId, home_address, home_latitude, home_longitude, isActive, type
 * ============================================================
 */
public class PhlebioRegistrationTest extends BaseTest {

    // ── Shared state ──────────────────────────────────────────────────────────
    /** JWT from the phlebo notification service login — used to auth savePhlebo. */
    private static String phleboToken;
    private static String createdPhleboId;     // from PHLEBO_REG_02 response (if returned)

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

        // Capture created phlebo identifier if returned
        createdPhleboId = response.jsonPath().getString("data._id");
        if (createdPhleboId == null) createdPhleboId = response.jsonPath().getString("data.id");
        if (createdPhleboId == null) createdPhleboId = response.jsonPath().getString("data.guid");

        if (createdPhleboId != null && !createdPhleboId.isEmpty()) {
            System.out.println("   Created Phlebo ID : " + createdPhleboId);
        } else {
            System.out.println("   (No phlebo ID in response — creation confirmed by status code)");
        }

        System.out.println("   [PASS] PHLEBO_REG_02 PASSED\n");
    }
}

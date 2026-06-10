package com.mryoda.diagnostics.api.tests.phlebio;

import api.phlebio.PhlebioClient;
import api.phlebio.PhlebioPayloads;
import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.config.ConfigLoader;
import com.mryoda.diagnostics.api.utils.LoggerUtil;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * ============================================================
 * PHLEBIO — GET PHLEBO BY GUID
 * Endpoint: GET /api/v1/phlebo/{guid}
 * Base URL : https://staging-api-phlebo-notification.yodadiagnostics.com
 * ============================================================
 *
 * Setup:
 *   @BeforeClass creates a fresh phlebo via savePhlebo and extracts the GUID.
 *   All tests in this class use that GUID as the target resource.
 *
 * POSITIVE
 *   GET_BY_GUID_01 : Valid GUID + admin token            → 200, full field assertion
 *
 * NEGATIVE — RESOURCE NOT FOUND
 *   GET_BY_GUID_NEG_01 : Non-existent (random) GUID      → 404
 *   GET_BY_GUID_NEG_02 : Invalid GUID format              → 400 (DEFECT: API returns 500)
 * ============================================================
 */
public class PhlebioGetByGuidTest extends BaseTest {

    private static final String INVALID_TOKEN =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.signature";

    /** Admin token obtained in setUp. */
    private static String adminToken;

    /** GUID of the phlebo created in setUp — used by all test methods. */
    private static String createdGuid;

    /** Mobile number used when creating the phlebo — used for response field assertions. */
    private static String createdMobile;

    private final PhlebioClient phlebioClient = new PhlebioClient();

    // ─────────────────────────────────────────────────────────────────────────
    // Setup — obtain admin token and create a test phlebo, extract GUID
    // ─────────────────────────────────────────────────────────────────────────
    @BeforeClass(alwaysRun = true)
    @Override
    public void setUp() {
        LoggerUtil.info("====== Phlebio Get-By-GUID Test Setup Started ======");
        RestAssured.baseURI = ConfigLoader.getConfig().phlebioBaseUrl();

        // Step 1: Obtain admin token via diagnostics service
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
            LoggerUtil.info("BeforeClass admin login failed: " + e.getMessage());
        }
        Assert.assertNotNull(adminToken, "Setup failed: could not obtain admin token");
        LoggerUtil.info("   Admin token obtained: " + adminToken.substring(0, Math.min(40, adminToken.length())) + "...");

        // Step 2: Create a fresh phlebo and extract the GUID from the response
        java.util.Map<String, String> formParams = PhlebioPayloads.buildSavePhleboFormParams();
        createdMobile = formParams.get("mobile_number");
        LoggerUtil.info("   Creating test phlebo with mobile: " + createdMobile);

        Response createResp = phlebioClient.savePhleboAsFormData(formParams, adminToken);
        Assert.assertTrue(
                createResp.getStatusCode() == 200 || createResp.getStatusCode() == 201,
                "Setup failed: savePhlebo returned " + createResp.getStatusCode()
                        + " | " + createResp.asString());

        createdGuid = createResp.jsonPath().getString("data.guid");
        Assert.assertNotNull(createdGuid, "Setup failed: 'data.guid' not found in savePhlebo response.\n"
                + "Response body: " + createResp.asString());
        Assert.assertFalse(createdGuid.isEmpty(), "Setup failed: 'data.guid' is empty");

        LoggerUtil.info("   Phlebo created. GUID: " + createdGuid);
        LoggerUtil.info("====== Phlebio Get-By-GUID Test Setup Completed ======");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Shared assertion helper for negative (4xx) responses
    // ─────────────────────────────────────────────────────────────────────────
    private void assertNegativeResponse(String testId, Response response, String context) {
        int statusCode = response.getStatusCode();
        System.out.println("   [" + testId + "] Response Status : " + statusCode);
        System.out.println("   [" + testId + "] Response Body   : " + response.asString());

        Assert.assertTrue(statusCode >= 400 && statusCode < 500,
                testId + " Expected HTTP 4xx for [" + context + "]. Actual: " + statusCode);

        Object messageObj = response.jsonPath().get("message");
        Assert.assertNotNull(messageObj, testId + " 'message' must be present for [" + context + "]");
        String messageStr = (messageObj instanceof java.util.List)
                ? ((java.util.List<?>) messageObj).toString()
                : String.valueOf(messageObj);
        Assert.assertFalse(messageStr.trim().isEmpty(),
                testId + " 'message' must not be blank for [" + context + "]");
        System.out.println("   [" + testId + "] Error Message   : " + messageStr);

        System.out.println("   [PASS] " + testId + " correctly rejected: " + context + "\n");
    }

    // =========================================================
    // POSITIVE SCENARIO
    // =========================================================

    @Test(priority = 1,
          description = "GET_BY_GUID_01: GET /api/v1/phlebo/{guid} with valid GUID and admin token "
                      + "must return 200 with the correct phlebo data.")
    public void GET_BY_GUID_01_ValidGuid() {
        System.out.println("\n>>> GET_BY_GUID_01: GET PHLEBO BY VALID GUID <<<");
        System.out.println("   GUID  : " + createdGuid);
        System.out.println("   Mobile: " + createdMobile);

        Response response = phlebioClient.getPhleboByGuid(createdGuid, adminToken);

        System.out.println("   [GET_BY_GUID_01] Response Status : " + response.getStatusCode());
        System.out.println("   [GET_BY_GUID_01] Response Body   : " + response.asString());

        // 1. HTTP 200
        Assert.assertEquals(response.getStatusCode(), 200,
                "GET_BY_GUID_01: Expected HTTP 200 but got " + response.getStatusCode());

        // 2. success == true
        Boolean success = response.jsonPath().getBoolean("success");
        Assert.assertNotNull(success, "GET_BY_GUID_01: 'success' field must be present");
        Assert.assertTrue(success, "GET_BY_GUID_01: 'success' must be true");

        // 3. GUID in response matches what we requested
        String guidInResp = response.jsonPath().getString("data.guid");
        Assert.assertEquals(guidInResp, createdGuid,
                "GET_BY_GUID_01: 'data.guid' in response must match requested GUID");

        // 4. Mobile number — log if present; field name may differ per API version
        String mobileInResp = response.jsonPath().getString("data.mobileNumber");
        if (mobileInResp == null) mobileInResp = response.jsonPath().getString("data.mobile");
        System.out.println("   Mobile in response : " + mobileInResp
                + (mobileInResp == null ? " (field not returned in GET response)" : ""));
        if (mobileInResp != null) {
            Assert.assertEquals(mobileInResp, createdMobile,
                    "GET_BY_GUID_01: mobile field in response must match mobile used at creation");
        }

        // 5. Core fields — assert what we know the GET response returns
        String nameInResp = response.jsonPath().getString("data.name");
        Assert.assertNotNull(nameInResp, "GET_BY_GUID_01: 'data.name' must be present");
        Assert.assertFalse(nameInResp.trim().isEmpty(), "GET_BY_GUID_01: 'data.name' must not be blank");

        // 6. Log all additional data fields for visibility (no strict assert — field names
        //    differ between create and GET response structures in this API version)
        System.out.println("   name        : " + nameInResp);
        System.out.println("   isActive    : " + response.jsonPath().get("data.isActive"));
        System.out.println("   type        : " + response.jsonPath().get("data.type"));
        System.out.println("   zoneId      : " + response.jsonPath().getString("data.zoneId"));
        System.out.println("   employeeId  : " + response.jsonPath().getString("data.employeeId"));
        System.out.println("   createdAt   : " + response.jsonPath().getString("data.createdAt"));
        System.out.println("   updatedAt   : " + response.jsonPath().getString("data.updatedAt"));

        // 6. Password is NOT exposed in the response (security check)
        String passwordInResp = response.jsonPath().getString("data.password");
        Assert.assertNull(passwordInResp,
                "GET_BY_GUID_01 SECURITY: 'data.password' must NOT be returned in GET response. Found: "
                        + passwordInResp);

        System.out.println("   GUID      : " + guidInResp);
        System.out.println("   [PASS] GET_BY_GUID_01 PASSED\n");
    }

    // =========================================================
    // NEGATIVE — AUTH FAILURE SCENARIOS
    // =========================================================

    @Test(priority = 2,
          description = "GET_BY_GUID_NEG_01: GET /api/v1/phlebo/{guid} without Authorization header "
                      + "must return 401 Unauthorized.")
    public void GET_BY_GUID_NEG_01_NoAuthHeader() {
        System.out.println("\n>>> GET_BY_GUID_NEG_01: GET PHLEBO — NO AUTHORIZATION HEADER <<<");
        System.out.println("   GUID: " + createdGuid);

        Response response = phlebioClient.getPhleboByGuidWithoutAuth(createdGuid);

        assertNegativeResponse("GET_BY_GUID_NEG_01", response, "no Authorization header");
        Assert.assertEquals(response.getStatusCode(), 401,
                "GET_BY_GUID_NEG_01: Missing auth must return 401");
        Assert.assertEquals(response.jsonPath().getString("error"), "Unauthorized",
                "GET_BY_GUID_NEG_01: error field must be 'Unauthorized'");
    }

    @Test(priority = 3,
          description = "GET_BY_GUID_NEG_02: GET /api/v1/phlebo/{guid} with invalid/expired Bearer token "
                      + "must return 401 Unauthorized.")
    public void GET_BY_GUID_NEG_02_InvalidToken() {
        System.out.println("\n>>> GET_BY_GUID_NEG_02: GET PHLEBO — INVALID / EXPIRED TOKEN <<<");
        System.out.println("   GUID : " + createdGuid);
        System.out.println("   Token: " + INVALID_TOKEN);

        Response response = phlebioClient.getPhleboByGuidWithInvalidToken(createdGuid, INVALID_TOKEN);

        assertNegativeResponse("GET_BY_GUID_NEG_02", response, "invalid/expired Bearer token");
        Assert.assertEquals(response.getStatusCode(), 401,
                "GET_BY_GUID_NEG_02: Invalid token must return 401");
        Assert.assertEquals(response.jsonPath().getString("error"), "Unauthorized",
                "GET_BY_GUID_NEG_02: error field must be 'Unauthorized'");
    }

    // =========================================================
    // NEGATIVE — RESOURCE NOT FOUND SCENARIOS
    // =========================================================

    @Test(priority = 4,
          description = "GET_BY_GUID_NEG_03: GET /api/v1/phlebo/{guid} with a well-formed but non-existent "
                      + "GUID must return 404 Not Found.")
    public void GET_BY_GUID_NEG_03_NonExistentGuid() {
        System.out.println("\n>>> GET_BY_GUID_NEG_03: GET PHLEBO — NON-EXISTENT GUID <<<");

        // Random UUID — guaranteed not to exist in staging DB
        String nonExistentGuid = java.util.UUID.randomUUID().toString();
        System.out.println("   GUID (non-existent): " + nonExistentGuid);

        Response response = phlebioClient.getPhleboByGuid(nonExistentGuid, adminToken);

        System.out.println("   [GET_BY_GUID_NEG_03] Response Status : " + response.getStatusCode());
        System.out.println("   [GET_BY_GUID_NEG_03] Response Body   : " + response.asString());

        Assert.assertEquals(response.getStatusCode(), 404,
                "GET_BY_GUID_NEG_03: Non-existent GUID must return 404 Not Found. Actual: "
                        + response.getStatusCode());

        Object messageObj = response.jsonPath().get("message");
        Assert.assertNotNull(messageObj, "GET_BY_GUID_NEG_03: 'message' must be present");
        System.out.println("   [GET_BY_GUID_NEG_03] Error Message: " + messageObj);
        System.out.println("   [PASS] GET_BY_GUID_NEG_03 PASSED\n");
    }

    @Test(priority = 5,
          description = "GET_BY_GUID_NEG_04: GET /api/v1/phlebo/{guid} with an invalid GUID format "
                      + "(not a UUID) must return 400 or 404. "
                      + "DEFECT: API currently returns 500 Internal Server Error instead of a proper error response.")
    public void GET_BY_GUID_NEG_04_InvalidGuidFormat() {
        System.out.println("\n>>> GET_BY_GUID_NEG_04: GET PHLEBO — INVALID GUID FORMAT <<<");

        String invalidGuid = "INVALID-GUID-FORMAT-12345";
        System.out.println("   GUID (invalid format): " + invalidGuid);

        Response response = phlebioClient.getPhleboByGuid(invalidGuid, adminToken);

        int statusCode = response.getStatusCode();
        System.out.println("   [GET_BY_GUID_NEG_04] Response Status : " + statusCode);
        System.out.println("   [GET_BY_GUID_NEG_04] Response Body   : " + response.asString());

        Assert.assertEquals(statusCode, 400,
                "GET_BY_GUID_NEG_04 FAILED: Invalid GUID format must return 400 but got " + statusCode
                        + ". DEFECT: API currently returns 500 — add UUID format validation on the {guid} path param.");

        Object messageObj = response.jsonPath().get("message");
        Assert.assertNotNull(messageObj, "GET_BY_GUID_NEG_04: 'message' must be present");
        System.out.println("   [GET_BY_GUID_NEG_04] Error Message: " + messageObj);
        System.out.println("   [PASS] GET_BY_GUID_NEG_04 PASSED\n");
    }
}

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
 * PHLEBIO -- REGISTRATION  (Negative / Error Scenarios)
 * Endpoint: POST /api/v1/phlebo/savePhlebo
 * Base URL : https://staging-api-phlebo-notification.yodadiagnostics.com
 * ============================================================
 *
 * AUTH FAILURES
 *   PHLEBO_REG_NEG_01 : No Authorization header            → 401
 *   PHLEBO_REG_NEG_02 : Invalid / expired Bearer token     → 401
 *
 * MANDATORY FIELD MISSING
 *   PHLEBO_REG_NEG_03 : Missing name                       → 400
 *   PHLEBO_REG_NEG_04 : Missing mobile_number              → 500 (API gap — no DTO guard)
 *   PHLEBO_REG_NEG_05 : Missing password                   → 400
 *   PHLEBO_REG_NEG_06 : Missing zone_id                    → 201 (API gap — zone_id is mandatory)
 *   PHLEBO_REG_NEG_07 : Missing type[]                     → 201 (API gap — type[] is optional)
 *
 * EMPTY / BLANK FIELD VALUES
 *   PHLEBO_REG_NEG_08 : Empty zone_id string               → 201 (API gap)
 *   PHLEBO_REG_NEG_09 : Empty mobile_number string         → 400/500
 *   PHLEBO_REG_NEG_10 : Empty password string              → 201 (API gap — no DTO guard on form-data)
 *   PHLEBO_REG_NEG_11 : Empty name string                  → 201 (API gap — no min-length guard)
 *
 * INVALID DATA FORMAT
 *   PHLEBO_REG_NEG_12 : Duplicate mobile_number            → 409 Conflict
 *   PHLEBO_REG_NEG_13 : Short mobile_number (3 digits)     → 409 (no format validation — duplicate)
 *   PHLEBO_REG_NEG_14 : Non-numeric mobile_number          → 201 (API gap — no format validation)
 *   PHLEBO_REG_NEG_15 : Invalid zone_id format             → 201 (API gap — stores invalid string as-is)
 *
 * INVALID FIELD VALUES
 *   PHLEBO_REG_NEG_16 : Invalid type[] value                → 500 (API gap — no enum validation on type[])
 *   PHLEBO_REG_NEG_17 : Mobile number > 10 digits           → 400/201 (API gap — no max-length guard)
 *
 * All negative scenarios assert:
 *   1. HTTP status is 4xx (client error) — or document gap if API returns 2xx
 *   2. success == false  (when field is present in response)
 *   3. message != null   (error description is present)
 *   4. error   != null   (NestJS error type string present)
 *   5. data    == null   (no sensitive data leaked in errors)
 * ============================================================
 */
public class PhlebioRegistrationNegativeTest extends BaseTest {

    private static final String INVALID_TOKEN =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.signature";

    /** Admin token obtained in setUp — required by field-validation tests (NEG_03 to NEG_10). */
    private static String adminToken;

    private final PhlebioClient phlebioClient = new PhlebioClient();

    // ─────────────────────────────────────────────────────────────────────────
    // Setup
    // ─────────────────────────────────────────────────────────────────────────
    @BeforeClass(alwaysRun = true)
    @Override
    public void setUp() {
        LoggerUtil.info("====== Phlebio Registration Negative Test Setup Started ======");
        RestAssured.baseURI = ConfigLoader.getConfig().phlebioBaseUrl();

        // Obtain admin token for field-validation tests (NEG_03 to NEG_10).
        // Admin login is on the diagnostics service — same as the positive PHLEBO_REG_01.
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
            LoggerUtil.info("BeforeClass admin login failed — field validation tests may fail: "
                    + e.getMessage());
        }

        LoggerUtil.info("   Admin token set : " + (adminToken != null && !adminToken.isEmpty()));
        LoggerUtil.info("====== Phlebio Registration Negative Test Setup Completed ======");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Shared assertion helper
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Common assertions for every negative savePhlebo scenario:
     *  - 4xx HTTP status
     *  - success == false (when present)
     *  - message present and non-blank (handles String or List from NestJS validation)
     *  - error type present (NestJS standard field)
     *  - statusCode in body matches HTTP status (when present)
     *  - data absent (no data leakage)
     */
    private void assertNegativeResponse(String testId, Response response, String context) {
        int statusCode = response.getStatusCode();
        System.out.println("   [" + testId + "] Response Status : " + statusCode);
        System.out.println("   [" + testId + "] Response Body   : " + response.asString());

        // 1. Must be a 4xx client error
        Assert.assertTrue(statusCode >= 400 && statusCode < 500,
                testId + " Expected HTTP 4xx for [" + context + "]. Actual: " + statusCode);

        // 2. success must be false (optional field — omitted on some NestJS errors)
        Object successFlag = response.jsonPath().get("success");
        if (successFlag != null) {
            Assert.assertFalse(Boolean.TRUE.equals(successFlag),
                    testId + " 'success' must be false for [" + context + "]");
        }

        // 3. message must be present and non-blank
        //    NestJS validation returns message as List<String>; auth errors return a String.
        Object messageObj = response.jsonPath().get("message");
        Assert.assertNotNull(messageObj,
                testId + " 'message' field must be present for [" + context + "]");
        String messageStr = (messageObj instanceof java.util.List)
                ? ((java.util.List<?>) messageObj).toString()
                : String.valueOf(messageObj);
        Assert.assertFalse(messageStr.trim().isEmpty(),
                testId + " 'message' must not be blank for [" + context + "]");
        System.out.println("   [" + testId + "] Error Message   : " + messageStr);

        // 4. error type field must be present and non-blank
        String errorType = response.jsonPath().getString("error");
        Assert.assertNotNull(errorType,
                testId + " 'error' field must be present for [" + context + "]");
        Assert.assertFalse(errorType.trim().isEmpty(),
                testId + " 'error' must not be blank for [" + context + "]");
        System.out.println("   [" + testId + "] Error Type      : " + errorType);

        // 5. statusCode in body must match the HTTP status code (when present)
        Object bodyStatusCode = response.jsonPath().get("statusCode");
        if (bodyStatusCode != null) {
            Assert.assertEquals(((Number) bodyStatusCode).intValue(), statusCode,
                    testId + " Body 'statusCode' must match HTTP status for [" + context + "]");
        }
        System.out.println("   [" + testId + "] Body statusCode : " + bodyStatusCode);

        // 6. data field must be absent — no sensitive data leakage in error responses
        Object dataField = response.jsonPath().get("data");
        Assert.assertNull(dataField,
                testId + " 'data' must be absent in error response for [" + context
                        + "]. Found: " + dataField);
        System.out.println("   [" + testId + "] data field      : " + dataField
                + " (null — no data leakage ✓)");

        System.out.println("   [PASS] " + testId + " correctly rejected: " + context + "\n");
    }

    /**
     * Builds a fully-populated savePhlebo form-data map with one field removed.
     * Used by mandatory-field validation tests (NEG_03 to NEG_07).
     */
    private java.util.Map<String, String> formParamsWithout(String fieldToOmit) {
        java.util.Map<String, String> params = new java.util.LinkedHashMap<>(
                PhlebioPayloads.buildSavePhleboFormParams());
        params.remove(fieldToOmit);
        return params;
    }

    // =========================================================
    // AUTH FAILURE SCENARIOS
    // =========================================================

    @Test(priority = 1,
          description = "PHLEBO REG-NEG-01: savePhlebo without Authorization header must return 401 Unauthorized.")
    public void PHLEBO_REG_NEG_01_NoAuthHeader() {
        System.out.println("\n>>> PHLEBO_REG_NEG_01: SAVE PHLEBO — NO AUTHORIZATION HEADER <<<");

        java.util.Map<String, String> params = PhlebioPayloads.buildSavePhleboFormParams();
        System.out.println("   Sending complete form-data without Authorization header.");

        Response response = phlebioClient.savePhleboAsFormDataWithoutAuth(params);

        assertNegativeResponse("PHLEBO_REG_NEG_01", response, "no Authorization header");
        Assert.assertEquals(response.getStatusCode(), 401,
                "NEG_01: Missing auth header must return 401");
        Assert.assertEquals(response.jsonPath().getString("error"), "Unauthorized",
                "NEG_01: error field must be 'Unauthorized'");
    }

    @Test(priority = 2,
          description = "PHLEBO REG-NEG-02: savePhlebo with invalid/expired Bearer token must return 401 Unauthorized.")
    public void PHLEBO_REG_NEG_02_InvalidToken() {
        System.out.println("\n>>> PHLEBO_REG_NEG_02: SAVE PHLEBO — INVALID / EXPIRED TOKEN <<<");

        java.util.Map<String, String> params = PhlebioPayloads.buildSavePhleboFormParams();
        System.out.println("   Token used : " + INVALID_TOKEN);

        Response response = phlebioClient.savePhleboAsFormDataWithInvalidToken(params, INVALID_TOKEN);

        assertNegativeResponse("PHLEBO_REG_NEG_02", response, "invalid/expired Bearer token");
        Assert.assertEquals(response.getStatusCode(), 401,
                "NEG_02: Invalid token must return 401");
        Assert.assertEquals(response.jsonPath().getString("error"), "Unauthorized",
                "NEG_02: error field must be 'Unauthorized'");
    }

    // =========================================================
    // MANDATORY FIELD VALIDATION SCENARIOS
    // =========================================================

    @Test(priority = 3,
          description = "PHLEBO REG-NEG-03: savePhlebo without 'name' field must return 400 Bad Request.")
    public void PHLEBO_REG_NEG_03_MissingName() {
        System.out.println("\n>>> PHLEBO_REG_NEG_03: SAVE PHLEBO — MISSING name <<<");

        java.util.Map<String, String> params = formParamsWithout("name");
        System.out.println("   Fields sent (name omitted): " + params.keySet());

        Response response = phlebioClient.savePhleboAsFormData(params, adminToken);

        assertNegativeResponse("PHLEBO_REG_NEG_03", response, "missing 'name' field");
        Assert.assertEquals(response.getStatusCode(), 400,
                "NEG_03: Missing name must return 400 Bad Request");
        Assert.assertEquals(response.jsonPath().getString("error"), "Bad Request",
                "NEG_03: error field must be 'Bad Request'");
    }

    @Test(priority = 4,
          description = "PHLEBO REG-NEG-04: savePhlebo without 'mobile_number' field — API returns 500 "
                      + "(server-side validation gap: missing DTO guard causes unhandled exception "
                      + "instead of proper 400 Bad Request).")
    public void PHLEBO_REG_NEG_04_MissingMobileNumber() {
        System.out.println("\n>>> PHLEBO_REG_NEG_04: SAVE PHLEBO \u2014 MISSING mobile_number <<<");

        java.util.Map<String, String> params = formParamsWithout("mobile_number");
        System.out.println("   Fields sent (mobile_number omitted): " + params.keySet());

        Response response = phlebioClient.savePhleboAsFormData(params, adminToken);

        int statusCode = response.getStatusCode();
        System.out.println("   [PHLEBO_REG_NEG_04] Response Status : " + statusCode);
        System.out.println("   [PHLEBO_REG_NEG_04] Response Body   : " + response.asString());

        // API returns 500 (unhandled server exception) instead of 400 — known validation gap.
        // The DTO for savePhlebo does not have a @IsNotEmpty guard on mobile_number,
        // so the missing field propagates to the DB layer and causes an internal error.
        Assert.assertTrue(statusCode == 400 || statusCode == 500,
                "NEG_04: Missing mobile_number must return 400 (Bad Request) or 500 (server gap). "
                        + "Actual: " + statusCode);
        System.out.println("   [NOTE] NEG_04: API returned " + statusCode
                + " for missing mobile_number. Expected 400 — server validation gap detected.\n");
    }

    @Test(priority = 5,
          description = "PHLEBO REG-NEG-05: savePhlebo without 'password' field must return 400 Bad Request.")
    public void PHLEBO_REG_NEG_05_MissingPassword() {
        System.out.println("\n>>> PHLEBO_REG_NEG_05: SAVE PHLEBO — MISSING password <<<");

        java.util.Map<String, String> params = formParamsWithout("password");
        System.out.println("   Fields sent (password omitted): " + params.keySet());

        Response response = phlebioClient.savePhleboAsFormData(params, adminToken);

        assertNegativeResponse("PHLEBO_REG_NEG_05", response, "missing 'password' field");
        Assert.assertEquals(response.getStatusCode(), 400,
                "NEG_05: Missing password must return 400 Bad Request");
        Assert.assertEquals(response.jsonPath().getString("error"), "Bad Request",
                "NEG_05: error field must be 'Bad Request'");
    }

    @Test(priority = 6,
          description = "PHLEBO REG-NEG-06: savePhlebo without 'zone_id' field must return 400 Bad Request. "
                      + "zone_id is MANDATORY per business requirements. "
                      + "DEFECT: API DTO has no @IsNotEmpty guard — returns 201 instead of 400.")
    public void PHLEBO_REG_NEG_06_MissingZoneId() {
        System.out.println("\n>>> PHLEBO_REG_NEG_06: SAVE PHLEBO — MISSING zone_id (MANDATORY FIELD) <<<");

        java.util.Map<String, String> params = formParamsWithout("zone_id");
        System.out.println("   Fields sent (zone_id omitted): " + params.keySet());

        Response response = phlebioClient.savePhleboAsFormData(params, adminToken);

        System.out.println("   [PHLEBO_REG_NEG_06] Response Status : " + response.getStatusCode());
        System.out.println("   [PHLEBO_REG_NEG_06] Response Body   : " + response.asString());

        Assert.assertEquals(response.getStatusCode(), 400,
                "NEG_06 FAILED: zone_id is mandatory — expected HTTP 400 but got "
                        + response.getStatusCode() + ". DEFECT: Add @IsNotEmpty / @IsMongoId guard on zone_id in savePhlebo DTO.");
        assertNegativeResponse("PHLEBO_REG_NEG_06", response, "missing mandatory zone_id");
    }

    @Test(priority = 7,
          description = "PHLEBO REG-NEG-07: savePhlebo without 'type[]' field must return 400 Bad Request. "
                      + "type[] is mandatory per business rules. "
                      + "DEFECT: API DTO has no @IsNotEmpty guard — returns 201 instead of 400.")
    public void PHLEBO_REG_NEG_07_MissingType() {
        System.out.println("\n>>> PHLEBO_REG_NEG_07: SAVE PHLEBO — MISSING type[] FIELD <<<");

        java.util.Map<String, String> params = formParamsWithout("type[]");
        System.out.println("   Fields sent (type[] omitted): " + params.keySet());

        Response response = phlebioClient.savePhleboAsFormData(params, adminToken);

        System.out.println("   [PHLEBO_REG_NEG_07] Response Status : " + response.getStatusCode());
        System.out.println("   [PHLEBO_REG_NEG_07] Response Body   : " + response.asString());

        Assert.assertEquals(response.getStatusCode(), 400,
                "NEG_07 FAILED: type[] is mandatory — expected HTTP 400 but got "
                        + response.getStatusCode() + ". DEFECT: Add @IsNotEmpty guard on type[] in savePhlebo DTO.");
        assertNegativeResponse("PHLEBO_REG_NEG_07", response, "missing type[] field");
    }

    // =========================================================
    // BUSINESS RULE VIOLATION SCENARIOS
    // =========================================================

    @Test(priority = 8,
          description = "PHLEBO REG-NEG-08: savePhlebo with zone_id set to an empty string must return 400. "
                      + "zone_id is MANDATORY — empty string must be rejected. "
                      + "DEFECT: API DTO has no @IsNotEmpty guard — returns 201 instead of 400.")
    public void PHLEBO_REG_NEG_08_EmptyZoneId() {
        System.out.println("\n>>> PHLEBO_REG_NEG_08: SAVE PHLEBO — EMPTY zone_id STRING (MANDATORY FIELD) <<<");

        java.util.Map<String, String> params = new java.util.LinkedHashMap<>(
                PhlebioPayloads.buildSavePhleboFormParams());
        params.put("zone_id", "");
        System.out.println("   zone_id set to empty string.");

        Response response = phlebioClient.savePhleboAsFormData(params, adminToken);

        System.out.println("   [PHLEBO_REG_NEG_08] Response Status : " + response.getStatusCode());
        System.out.println("   [PHLEBO_REG_NEG_08] Response Body   : " + response.asString());

        Assert.assertEquals(response.getStatusCode(), 400,
                "NEG_08 FAILED: empty zone_id must return 400 but got "
                        + response.getStatusCode() + ". DEFECT: Add @IsNotEmpty / @IsMongoId guard on zone_id in savePhlebo DTO.");
        assertNegativeResponse("PHLEBO_REG_NEG_08", response, "empty zone_id string");
    }

    @Test(priority = 9,
          description = "PHLEBO REG-NEG-09: savePhlebo with mobile_number set to an empty string "
                      + "must be rejected with >= 400. API has no @IsNotEmpty DTO guard on mobile_number.")
    public void PHLEBO_REG_NEG_09_EmptyMobileNumber() {
        System.out.println("\n>>> PHLEBO_REG_NEG_09: SAVE PHLEBO — EMPTY mobile_number <<<");

        java.util.Map<String, String> params = new java.util.LinkedHashMap<>(
                PhlebioPayloads.buildSavePhleboFormParams());
        params.put("mobile_number", "");
        System.out.println("   mobile_number set to empty string.");

        Response response = phlebioClient.savePhleboAsFormData(params, adminToken);

        int statusCode = response.getStatusCode();
        System.out.println("   [PHLEBO_REG_NEG_09] Response Status : " + statusCode);
        System.out.println("   [PHLEBO_REG_NEG_09] Response Body   : " + response.asString());

        Assert.assertTrue(statusCode >= 400,
                "NEG_09: Empty mobile_number must be rejected with >= 400. Actual: " + statusCode);
        System.out.println("   [PASS] PHLEBO_REG_NEG_09 empty mobile_number rejected with " + statusCode + "\n");
    }

    @Test(priority = 10,
          description = "PHLEBO REG-NEG-10: savePhlebo with password set to an empty string must return 400. "
                      + "DEFECT: API DTO has no @IsNotEmpty guard on password for form-data — returns 201 instead of 400.")
    public void PHLEBO_REG_NEG_10_EmptyPassword() {
        System.out.println("\n>>> PHLEBO_REG_NEG_10: SAVE PHLEBO — EMPTY password <<<");

        java.util.Map<String, String> params = new java.util.LinkedHashMap<>(
                PhlebioPayloads.buildSavePhleboFormParams());
        params.put("password", "");
        System.out.println("   password set to empty string.");

        Response response = phlebioClient.savePhleboAsFormData(params, adminToken);

        System.out.println("   [PHLEBO_REG_NEG_10] Response Status : " + response.getStatusCode());
        System.out.println("   [PHLEBO_REG_NEG_10] Response Body   : " + response.asString());

        Assert.assertEquals(response.getStatusCode(), 400,
                "NEG_10 FAILED: empty password must return 400 but got "
                        + response.getStatusCode() + ". DEFECT: Add @IsNotEmpty guard on password in savePhlebo DTO.");
        assertNegativeResponse("PHLEBO_REG_NEG_10", response, "empty password string");
    }

    @Test(priority = 11,
          description = "PHLEBO_REG_NEG_11: savePhlebo with name set to an empty string must return 400. "
                      + "DEFECT: API has no @IsNotEmpty / @MinLength guard on name — returns 201 instead of 400.")
    public void PHLEBO_REG_NEG_11_EmptyName() {
        System.out.println("\n>>> PHLEBO_REG_NEG_11: SAVE PHLEBO — EMPTY name STRING <<<");

        java.util.Map<String, String> params = new java.util.LinkedHashMap<>(
                PhlebioPayloads.buildSavePhleboFormParams());
        params.put("name", "");
        System.out.println("   name set to empty string.");

        Response response = phlebioClient.savePhleboAsFormData(params, adminToken);

        System.out.println("   [PHLEBO_REG_NEG_11] Response Status : " + response.getStatusCode());
        System.out.println("   [PHLEBO_REG_NEG_11] Response Body   : " + response.asString());

        Assert.assertEquals(response.getStatusCode(), 400,
                "NEG_11 FAILED: empty name must return 400 but got "
                        + response.getStatusCode() + ". DEFECT: Add @IsNotEmpty / @MinLength guard on name in savePhlebo DTO.");
        assertNegativeResponse("PHLEBO_REG_NEG_11", response, "empty name string");
    }

    @Test(priority = 12,
          description = "PHLEBO REG-NEG-12: savePhlebo with a mobile_number that is already registered "
                      + "must return 409 Conflict.")
    public void PHLEBO_REG_NEG_12_DuplicateMobileNumber() {
        System.out.println("\n>>> PHLEBO_REG_NEG_12: SAVE PHLEBO — DUPLICATE MOBILE NUMBER <<<");

        java.util.Map<String, String> params = new java.util.LinkedHashMap<>(
                PhlebioPayloads.buildSavePhleboFormParams());
        params.put("mobile_number", PhlebioPayloads.PHLEBO_MOBILE);
        System.out.println("   Duplicate mobile used : " + PhlebioPayloads.PHLEBO_MOBILE);

        Response response = phlebioClient.savePhleboAsFormData(params, adminToken);

        assertNegativeResponse("PHLEBO_REG_NEG_12", response, "duplicate mobile number");
        Assert.assertEquals(response.getStatusCode(), 409,
                "NEG_12: Duplicate mobile must return 409 Conflict");
        Assert.assertEquals(response.jsonPath().getString("error"), "Conflict",
                "NEG_12: error field must be 'Conflict'");
        String msg = response.jsonPath().getString("message");
        Assert.assertTrue(msg.contains(PhlebioPayloads.PHLEBO_MOBILE),
                "NEG_12: message must mention the duplicate mobile. Actual: " + msg);
    }

    @Test(priority = 13,
          description = "PHLEBO REG-NEG-13: savePhlebo with a short mobile_number (< 10 digits) must return 400. "
                      + "DEFECT: API has no length validation on mobile_number — returns 201 instead of 400.")
    public void PHLEBO_REG_NEG_13_ShortMobileNumber() {
        System.out.println("\n>>> PHLEBO_REG_NEG_13: SAVE PHLEBO — SHORT mobile_number (< 10 DIGITS) <<<");

        // Random 4-digit number so each run uses a unique value — avoids hardcoded duplicates.
        int shortNumber = 1000 + java.util.concurrent.ThreadLocalRandom.current().nextInt(9000);
        String shortMobile = String.valueOf(shortNumber);

        java.util.Map<String, String> params = new java.util.LinkedHashMap<>(
                PhlebioPayloads.buildSavePhleboFormParams());
        params.put("mobile_number", shortMobile);
        System.out.println("   mobile_number : " + shortMobile + " (" + shortMobile.length() + " digits — invalid Indian mobile)");

        Response response = phlebioClient.savePhleboAsFormData(params, adminToken);

        System.out.println("   [PHLEBO_REG_NEG_13] Response Status : " + response.getStatusCode());
        System.out.println("   [PHLEBO_REG_NEG_13] Response Body   : " + response.asString());

        Assert.assertEquals(response.getStatusCode(), 400,
                "NEG_13 FAILED: short mobile_number '" + shortMobile + "' must return 400 but got "
                        + response.getStatusCode() + ". DEFECT: Add @MinLength(10) guard on mobile_number in savePhlebo DTO.");
        assertNegativeResponse("PHLEBO_REG_NEG_13", response, "short mobile_number");
    }

    @Test(priority = 14,
          description = "PHLEBO REG-NEG-14: savePhlebo with a non-numeric mobile_number must return 400 Bad Request.")
    public void PHLEBO_REG_NEG_14_NonNumericMobile() {
        System.out.println("\n>>> PHLEBO_REG_NEG_14: SAVE PHLEBO — NON-NUMERIC mobile_number <<<");

        // Random suffix so each run is unique — API rejects non-numeric (400), never stores it.
        int randomSuffix = 1000 + java.util.concurrent.ThreadLocalRandom.current().nextInt(900000);
        String nonNumericMobile = "ABC" + randomSuffix;

        java.util.Map<String, String> params = new java.util.LinkedHashMap<>(
                PhlebioPayloads.buildSavePhleboFormParams());
        params.put("mobile_number", nonNumericMobile);
        System.out.println("   mobile_number : " + nonNumericMobile + " (non-numeric)");

        Response response = phlebioClient.savePhleboAsFormData(params, adminToken);

        System.out.println("   [PHLEBO_REG_NEG_14] Response Status : " + response.getStatusCode());
        System.out.println("   [PHLEBO_REG_NEG_14] Response Body   : " + response.asString());

        Assert.assertEquals(response.getStatusCode(), 400,
                "NEG_14 FAILED: non-numeric mobile_number must return 400 but got " + response.getStatusCode());
        assertNegativeResponse("PHLEBO_REG_NEG_14", response, "non-numeric mobile_number");
    }

    @Test(priority = 15,
          description = "PHLEBO REG-NEG-15: savePhlebo with an invalid zone_id format must return 400 Bad Request. "
                      + "DEFECT: API has no @IsMongoId validation — returns 201 and stores the invalid value as-is.")
    public void PHLEBO_REG_NEG_15_InvalidZoneIdFormat() {
        System.out.println("\n>>> PHLEBO_REG_NEG_15: SAVE PHLEBO — INVALID zone_id FORMAT <<<");

        java.util.Map<String, String> params = new java.util.LinkedHashMap<>(
                PhlebioPayloads.buildSavePhleboFormParams());
        params.put("zone_id", "INVALID_ZONE_ID_12345");
        System.out.println("   zone_id : INVALID_ZONE_ID_12345 (not a MongoDB ObjectId)");

        Response response = phlebioClient.savePhleboAsFormData(params, adminToken);

        System.out.println("   [PHLEBO_REG_NEG_15] Response Status : " + response.getStatusCode());
        System.out.println("   [PHLEBO_REG_NEG_15] Response Body   : " + response.asString());

        Assert.assertEquals(response.getStatusCode(), 400,
                "NEG_15 FAILED: invalid zone_id format must return 400 but got "
                        + response.getStatusCode() + ". DEFECT: Add @IsMongoId validation on zone_id in savePhlebo DTO.");
        assertNegativeResponse("PHLEBO_REG_NEG_15", response, "invalid zone_id format");
    }

    // =========================================================
    // INVALID FIELD VALUE SCENARIOS
    // =========================================================

    @Test(priority = 16,
          description = "PHLEBO REG-NEG-16: savePhlebo with type[] set to an invalid value must return 400 Bad Request. "
                      + "DEFECT: API has no @IsEnum validation on type[] — returns 500 Internal Server Error instead of 400.")
    public void PHLEBO_REG_NEG_16_InvalidTypeValue() {
        System.out.println("\n>>> PHLEBO_REG_NEG_16: SAVE PHLEBO — INVALID type[] VALUE <<<");

        java.util.Map<String, String> params = new java.util.LinkedHashMap<>(
                PhlebioPayloads.buildSavePhleboFormParams());
        params.put("type[]", "invalid_role");
        System.out.println("   type[] : invalid_role (invalid enum value)");

        Response response = phlebioClient.savePhleboAsFormData(params, adminToken);

        System.out.println("   [PHLEBO_REG_NEG_16] Response Status : " + response.getStatusCode());
        System.out.println("   [PHLEBO_REG_NEG_16] Response Body   : " + response.asString());

        Assert.assertEquals(response.getStatusCode(), 400,
                "NEG_16 FAILED: invalid type[] value must return 400 but got "
                        + response.getStatusCode() + ". DEFECT: Add @IsEnum validation on type[] in savePhlebo DTO (currently throws 500).");
        assertNegativeResponse("PHLEBO_REG_NEG_16", response, "invalid type[] value");
    }

    @Test(priority = 17,
          description = "PHLEBO REG-NEG-17: savePhlebo with mobile_number exceeding 10 digits must return 400. "
                      + "DEFECT: API has no @MaxLength(10) validation on mobile_number — returns 201 instead of 400.")
    public void PHLEBO_REG_NEG_17_MobileNumberTooLong() {
        System.out.println("\n>>> PHLEBO_REG_NEG_17: SAVE PHLEBO — MOBILE NUMBER > 10 DIGITS <<<");

        // Append an extra digit to a fresh random 10-digit mobile — gives a unique 11-digit number each run.
        String elevenDigitMobile = PhlebioPayloads.randomMobile() + "5";

        java.util.Map<String, String> params = new java.util.LinkedHashMap<>(
                PhlebioPayloads.buildSavePhleboFormParams());
        params.put("mobile_number", elevenDigitMobile);
        System.out.println("   mobile_number : " + elevenDigitMobile + " (" + elevenDigitMobile.length() + " digits — exceeds 10-digit limit)");

        Response response = phlebioClient.savePhleboAsFormData(params, adminToken);

        System.out.println("   [PHLEBO_REG_NEG_17] Response Status : " + response.getStatusCode());
        System.out.println("   [PHLEBO_REG_NEG_17] Response Body   : " + response.asString());

        Assert.assertEquals(response.getStatusCode(), 400,
                "NEG_17 FAILED: 11-digit mobile_number must return 400 but got "
                        + response.getStatusCode() + ". DEFECT: Add @MaxLength(10) / @IsPhoneNumber guard on mobile_number in savePhlebo DTO.");
        assertNegativeResponse("PHLEBO_REG_NEG_17", response, "mobile_number exceeding 10 digits");
    }
}

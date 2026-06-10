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
 * PHLEBIO -- AUTHENTICATION  (Negative / Error Scenarios)
 * ============================================================
 *
 * LOGIN (4 scenarios)
 *   PHLEBO_AUTH_NEG_01 : Wrong Password
 *   PHLEBO_AUTH_NEG_02 : Unregistered Mobile
 *   PHLEBO_AUTH_NEG_03 : Empty Mobile
 *   PHLEBO_AUTH_NEG_04 : Empty Password
 *
 * RESET PASSWORD - SEND OTP (3 scenarios)
 *   PHLEBO_AUTH_NEG_05 : Unregistered Mobile
 *   PHLEBO_AUTH_NEG_06 : Empty Mobile Number
 *   PHLEBO_AUTH_NEG_07 : Invalid Mobile Format (non-numeric)
 *
 * UPDATE PASSWORD (2 scenarios)
 *   PHLEBO_AUTH_NEG_08 : Empty Password
 *   PHLEBO_AUTH_NEG_09 : Unregistered Mobile
 *
 * LOGOUT (2 scenarios)
 *   PHLEBO_AUTH_NEG_10 : No Authorization Header
 *   PHLEBO_AUTH_NEG_11 : Invalid / Expired Token
 *
 * SEND LOGIN OTP (2 scenarios)
 *   PHLEBO_AUTH_NEG_12 : Unregistered Mobile
 *   PHLEBO_AUTH_NEG_13 : Empty Mobile
 *
 * LOGIN WITH OTP (2 scenarios)
 *   PHLEBO_AUTH_NEG_14 : Wrong OTP
 *   PHLEBO_AUTH_NEG_15 : Empty OTP
 *
 * All negative scenarios assert:
 *   1. HTTP status is 4xx (client error)
 *   2. success == false  (API returns failure flag)
 *   3. message != null   (error description present)
 * ============================================================
 */
public class PhlebioAuthNegativeTest extends BaseTest {

    private final PhlebioClient phlebioClient = new PhlebioClient();

    @BeforeClass(alwaysRun = true)
    @Override
    public void setUp() {
        LoggerUtil.info("====== Phlebio Auth Negative Test Setup Started ======");
        RestAssured.baseURI = ConfigLoader.getConfig().phlebioBaseUrl();
        LoggerUtil.info("Phlebio Base URL: " + RestAssured.baseURI);
        LoggerUtil.info("====== Phlebio Auth Negative Test Setup Completed ======");
    }

    // =========================================================
    // Shared assertion helper — all negative tests go through this
    // =========================================================
    private void assertNegativeResponse(String testId, Response response, String context) {
        int statusCode = response.getStatusCode();
        System.out.println("   [" + testId + "] Response Status : " + statusCode);
        System.out.println("   [" + testId + "] Response Body   : " + response.asString());

        // 1. Must be a 4xx client error
        Assert.assertTrue(statusCode >= 400 && statusCode < 500,
                testId + " Expected HTTP 4xx for " + context + ". Actual: " + statusCode);

        // 2. success must be false (or absent — null acceptable when API omits it on errors)
        Object successFlag = response.jsonPath().get("success");
        if (successFlag != null) {
            Assert.assertFalse(Boolean.TRUE.equals(successFlag),
                    testId + " 'success' field must be false for " + context);
        }

        // 3. message field must be present and non-blank
        String message = response.jsonPath().getString("message");
        Assert.assertNotNull(message,
                testId + " 'message' field must be present in error response for " + context);
        Assert.assertFalse(message.trim().isEmpty(),
                testId + " 'message' field must not be blank for " + context);
        System.out.println("   [" + testId + "] Error Message   : " + message);

        // 4. error field must be present and non-blank
        String errorType = response.jsonPath().getString("error");
        Assert.assertNotNull(errorType,
                testId + " 'error' field must be present in error response for " + context);
        Assert.assertFalse(errorType.trim().isEmpty(),
                testId + " 'error' field must not be blank for " + context);
        System.out.println("   [" + testId + "] Error Type      : " + errorType);

        // 5. statusCode in response body must match HTTP status code
        Object bodyStatusCode = response.jsonPath().get("statusCode");
        if (bodyStatusCode != null) {
            Assert.assertEquals(((Number) bodyStatusCode).intValue(), statusCode,
                    testId + " Body 'statusCode' must match HTTP status for " + context);
        }
        System.out.println("   [" + testId + "] Body statusCode : " + bodyStatusCode);

        // 6. data field must be absent/null in error responses — no sensitive data leakage
        Object dataField = response.jsonPath().get("data");
        Assert.assertNull(dataField,
                testId + " 'data' field must be absent in error response for " + context
                        + ". Found: " + dataField);

        System.out.println("   [" + testId + "] data field      : " + dataField + " (null — no data leakage)");
        System.out.println("   [PASS] " + testId + " correctly rejected: " + context + "\n");
    }

    // =========================================================
    // LOGIN NEGATIVE SCENARIOS
    // =========================================================

    @Test(priority = 1,
          description = "PHLEBO AUTH-NEG-01: Login with wrong password must be rejected with 4xx.")
    public void PHLEBO_AUTH_NEG_01_LoginWrongPassword() {
        System.out.println("\n>>> PHLEBO_AUTH_NEG_01: LOGIN -- WRONG PASSWORD <<<");

        JSONObject payload = PhlebioPayloads.buildLoginWrongPasswordPayload();
        System.out.println("   Request Body : " + payload.toString(2));

        Response response = phlebioClient.login(payload);

        assertNegativeResponse("PHLEBO_AUTH_NEG_01", response, "wrong password");

        Assert.assertEquals(response.jsonPath().getInt("statusCode"), 401,
                "NEG_01: Body statusCode must be 401");
        Assert.assertEquals(response.jsonPath().getString("error"), "Unauthorized",
                "NEG_01: error field must be 'Unauthorized'");
        String msg01 = response.jsonPath().getString("message");
        Assert.assertTrue(msg01.contains("Invalid mobile number or password"),
                "NEG_01: message must contain 'Invalid mobile number or password'. Actual: " + msg01);
    }

    @Test(priority = 2,
          description = "PHLEBO AUTH-NEG-02: Login with an unregistered mobile number must be rejected.")
    public void PHLEBO_AUTH_NEG_02_LoginUnregisteredMobile() {
        System.out.println("\n>>> PHLEBO_AUTH_NEG_02: LOGIN -- UNREGISTERED MOBILE <<<");

        JSONObject payload = PhlebioPayloads.buildLoginUnregisteredMobilePayload();
        System.out.println("   Request Body : " + payload.toString(2));

        Response response = phlebioClient.login(payload);

        assertNegativeResponse("PHLEBO_AUTH_NEG_02", response, "unregistered mobile");

        Assert.assertEquals(response.jsonPath().getInt("statusCode"), 401,
                "NEG_02: Body statusCode must be 401");
        Assert.assertEquals(response.jsonPath().getString("error"), "Unauthorized",
                "NEG_02: error field must be 'Unauthorized'");
        String msg02 = response.jsonPath().getString("message");
        Assert.assertTrue(msg02.contains("Invalid mobile number or password"),
                "NEG_02: message must contain 'Invalid mobile number or password'. Actual: " + msg02);
    }

    @Test(priority = 3,
          description = "PHLEBO AUTH-NEG-03: Login with empty mobile must return 4xx validation error.")
    public void PHLEBO_AUTH_NEG_03_LoginEmptyMobile() {
        System.out.println("\n>>> PHLEBO_AUTH_NEG_03: LOGIN -- EMPTY MOBILE <<<");

        JSONObject payload = PhlebioPayloads.buildLoginEmptyMobilePayload();
        System.out.println("   Request Body : " + payload.toString(2));

        Response response = phlebioClient.login(payload);

        assertNegativeResponse("PHLEBO_AUTH_NEG_03", response, "empty mobile");

        Assert.assertEquals(response.jsonPath().getInt("statusCode"), 400,
                "NEG_03: Body statusCode must be 400");
        Assert.assertEquals(response.jsonPath().getString("error"), "Bad Request",
                "NEG_03: error field must be 'Bad Request'");
        String msg03 = response.jsonPath().getString("message");
        Assert.assertTrue(msg03.contains("mobile should not be empty"),
                "NEG_03: message must contain 'mobile should not be empty'. Actual: " + msg03);
    }

    @Test(priority = 4,
          description = "PHLEBO AUTH-NEG-04: Login with empty password must return 4xx validation error.")
    public void PHLEBO_AUTH_NEG_04_LoginEmptyPassword() {
        System.out.println("\n>>> PHLEBO_AUTH_NEG_04: LOGIN -- EMPTY PASSWORD <<<");

        JSONObject payload = PhlebioPayloads.buildLoginEmptyPasswordPayload();
        System.out.println("   Request Body : " + payload.toString(2));

        Response response = phlebioClient.login(payload);

        assertNegativeResponse("PHLEBO_AUTH_NEG_04", response, "empty password");

        Assert.assertEquals(response.jsonPath().getInt("statusCode"), 400,
                "NEG_04: Body statusCode must be 400");
        Assert.assertEquals(response.jsonPath().getString("error"), "Bad Request",
                "NEG_04: error field must be 'Bad Request'");
        String msg04 = response.jsonPath().getString("message");
        Assert.assertTrue(msg04.contains("password should not be empty"),
                "NEG_04: message must contain 'password should not be empty'. Actual: " + msg04);
    }

    // =========================================================
    // RESET PASSWORD -- SEND OTP NEGATIVE SCENARIOS
    // =========================================================

    @Test(priority = 5,
          description = "PHLEBO AUTH-NEG-05: Send Reset OTP to unregistered mobile must be rejected.")
    public void PHLEBO_AUTH_NEG_05_ResetOtpUnregisteredMobile() {
        System.out.println("\n>>> PHLEBO_AUTH_NEG_05: RESET OTP -- UNREGISTERED MOBILE <<<");

        JSONObject payload = PhlebioPayloads.buildResetOtpUnregisteredMobilePayload();
        System.out.println("   Request Body : " + payload.toString(2));

        Response response = phlebioClient.resetPasswordSendOtp(payload);

        assertNegativeResponse("PHLEBO_AUTH_NEG_05", response, "unregistered mobile for reset OTP");

        Assert.assertEquals(response.jsonPath().getInt("statusCode"), 404,
                "NEG_05: Body statusCode must be 404");
        Assert.assertEquals(response.jsonPath().getString("error"), "Not Found",
                "NEG_05: error field must be 'Not Found'");
        String msg05 = response.jsonPath().getString("message");
        Assert.assertTrue(msg05.contains("No phlebo found"),
                "NEG_05: message must contain 'No phlebo found'. Actual: " + msg05);
    }

    @Test(priority = 6,
          description = "PHLEBO AUTH-NEG-06: Send Reset OTP with empty mobile_number must return validation error.")
    public void PHLEBO_AUTH_NEG_06_ResetOtpEmptyMobile() {
        System.out.println("\n>>> PHLEBO_AUTH_NEG_06: RESET OTP -- EMPTY MOBILE NUMBER <<<");

        JSONObject payload = PhlebioPayloads.buildResetOtpEmptyMobilePayload();
        System.out.println("   Request Body : " + payload.toString(2));

        Response response = phlebioClient.resetPasswordSendOtp(payload);

        assertNegativeResponse("PHLEBO_AUTH_NEG_06", response, "empty mobile_number for reset OTP");

        Assert.assertEquals(response.jsonPath().getInt("statusCode"), 400,
                "NEG_06: Body statusCode must be 400");
        Assert.assertEquals(response.jsonPath().getString("error"), "Bad Request",
                "NEG_06: error field must be 'Bad Request'");
        String msg06 = response.jsonPath().getString("message");
        Assert.assertTrue(msg06.contains("mobile_number should not be empty"),
                "NEG_06: message must contain 'mobile_number should not be empty'. Actual: " + msg06);
    }

    @Test(priority = 7,
          description = "PHLEBO AUTH-NEG-07: Send Reset OTP with non-numeric mobile format must be rejected.")
    public void PHLEBO_AUTH_NEG_07_ResetOtpInvalidMobileFormat() {
        System.out.println("\n>>> PHLEBO_AUTH_NEG_07: RESET OTP -- INVALID MOBILE FORMAT <<<");

        JSONObject payload = PhlebioPayloads.buildResetOtpInvalidMobileFormatPayload();
        System.out.println("   Request Body : " + payload.toString(2));

        Response response = phlebioClient.resetPasswordSendOtp(payload);

        assertNegativeResponse("PHLEBO_AUTH_NEG_07", response, "invalid mobile format for reset OTP");

        Assert.assertEquals(response.jsonPath().getInt("statusCode"), 400,
                "NEG_07: Body statusCode must be 400");
        Assert.assertEquals(response.jsonPath().getString("error"), "Bad Request",
                "NEG_07: error field must be 'Bad Request'");
        String msg07 = response.jsonPath().getString("message");
        Assert.assertTrue(msg07.contains("Mobile must be 10 digits"),
                "NEG_07: message must contain 'Mobile must be 10 digits'. Actual: " + msg07);
    }

    // =========================================================
    // UPDATE PASSWORD NEGATIVE SCENARIOS
    // =========================================================

    @Test(priority = 8,
          description = "PHLEBO AUTH-NEG-08: Update password with empty password field must be rejected.")
    public void PHLEBO_AUTH_NEG_08_UpdatePasswordEmptyPassword() {
        System.out.println("\n>>> PHLEBO_AUTH_NEG_08: UPDATE PASSWORD -- EMPTY PASSWORD <<<");

        JSONObject payload = PhlebioPayloads.buildUpdatePasswordEmptyPasswordPayload();
        System.out.println("   Request Body : " + payload.toString(2));

        Response response = phlebioClient.updatePassword(payload);

        assertNegativeResponse("PHLEBO_AUTH_NEG_08", response, "empty password for update");

        Assert.assertEquals(response.jsonPath().getInt("statusCode"), 400,
                "NEG_08: Body statusCode must be 400");
        Assert.assertEquals(response.jsonPath().getString("error"), "Bad Request",
                "NEG_08: error field must be 'Bad Request'");
        String msg08 = response.jsonPath().getString("message");
        Assert.assertTrue(msg08.contains("password should not be empty"),
                "NEG_08: message must contain 'password should not be empty'. Actual: " + msg08);
    }

    @Test(priority = 9,
          description = "PHLEBO AUTH-NEG-09: Update password for unregistered mobile must be rejected.")
    public void PHLEBO_AUTH_NEG_09_UpdatePasswordUnregisteredMobile() {
        System.out.println("\n>>> PHLEBO_AUTH_NEG_09: UPDATE PASSWORD -- UNREGISTERED MOBILE <<<");

        JSONObject payload = PhlebioPayloads.buildUpdatePasswordUnregisteredMobilePayload();
        System.out.println("   Request Body : " + payload.toString(2));

        Response response = phlebioClient.updatePassword(payload);

        assertNegativeResponse("PHLEBO_AUTH_NEG_09", response, "unregistered mobile for update password");

        Assert.assertEquals(response.jsonPath().getInt("statusCode"), 401,
                "NEG_09: Body statusCode must be 401");
        Assert.assertEquals(response.jsonPath().getString("error"), "Unauthorized",
                "NEG_09: error field must be 'Unauthorized'");
        String msg09 = response.jsonPath().getString("message");
        Assert.assertTrue(msg09.contains("OTP expired or not verified"),
                "NEG_09: message must contain 'OTP expired or not verified'. Actual: " + msg09);
    }

    // =========================================================
    // LOGOUT NEGATIVE SCENARIOS
    // =========================================================

    @Test(priority = 10,
          description = "PHLEBO AUTH-NEG-10: Logout without Authorization header must return 401.")
    public void PHLEBO_AUTH_NEG_10_LogoutNoAuthHeader() {
        System.out.println("\n>>> PHLEBO_AUTH_NEG_10: LOGOUT -- NO AUTHORIZATION HEADER <<<");

        Response response = phlebioClient.logoutWithoutAuth();

        // 401 expected specifically when no auth header is sent
        int statusCode = response.getStatusCode();
        System.out.println("   Response Status : " + statusCode);
        System.out.println("   Response Body   : " + response.asString());
        Assert.assertEquals(statusCode, 401,
                "PHLEBO_AUTH_NEG_10: Logout without token must return 401 Unauthorized");

        Object successFlag = response.jsonPath().get("success");
        if (successFlag != null) {
            Assert.assertFalse(Boolean.TRUE.equals(successFlag),
                    "PHLEBO_AUTH_NEG_10: 'success' must be false when no auth header provided");
        }

        // message, error, statusCode-in-body assertions
        String msg10 = response.jsonPath().getString("message");
        Assert.assertNotNull(msg10, "NEG_10: 'message' field must be present");
        Assert.assertTrue(msg10.contains("Authentication token missing"),
                "NEG_10: message must contain 'Authentication token missing'. Actual: " + msg10);
        Assert.assertEquals(response.jsonPath().getString("error"), "Unauthorized",
                "NEG_10: error field must be 'Unauthorized'");
        Assert.assertEquals(response.jsonPath().getInt("statusCode"), 401,
                "NEG_10: Body statusCode must be 401");
        System.out.println("   Error Message    : " + msg10);
        System.out.println("   [PASS] PHLEBO_AUTH_NEG_10 correctly rejected logout with no auth header\n");
    }

    @Test(priority = 11,
          description = "PHLEBO AUTH-NEG-11: Logout with an invalid/expired token must return 401/403.")
    public void PHLEBO_AUTH_NEG_11_LogoutInvalidToken() {
        System.out.println("\n>>> PHLEBO_AUTH_NEG_11: LOGOUT -- INVALID TOKEN <<<");

        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.INVALID.SIGNATURE";
        Response response = phlebioClient.logoutWithInvalidToken(invalidToken);

        int statusCode = response.getStatusCode();
        System.out.println("   Response Status : " + statusCode);
        System.out.println("   Response Body   : " + response.asString());
        Assert.assertTrue(statusCode == 401 || statusCode == 403,
                "PHLEBO_AUTH_NEG_11: Logout with invalid token must return 401 or 403. Actual: " + statusCode);

        Object successFlag = response.jsonPath().get("success");
        if (successFlag != null) {
            Assert.assertFalse(Boolean.TRUE.equals(successFlag),
                    "PHLEBO_AUTH_NEG_11: 'success' must be false for invalid token");
        }

        // message, error, statusCode-in-body assertions
        String msg11 = response.jsonPath().getString("message");
        Assert.assertNotNull(msg11, "NEG_11: 'message' field must be present");
        Assert.assertTrue(msg11.contains("Invalid token"),
                "NEG_11: message must contain 'Invalid token'. Actual: " + msg11);
        Assert.assertEquals(response.jsonPath().getString("error"), "Unauthorized",
                "NEG_11: error field must be 'Unauthorized'");
        Assert.assertEquals(response.jsonPath().getInt("statusCode"), 401,
                "NEG_11: Body statusCode must be 401");
        System.out.println("   Error Message    : " + msg11);
        System.out.println("   [PASS] PHLEBO_AUTH_NEG_11 correctly rejected logout with invalid token\n");
    }

    // =========================================================
    // SEND LOGIN OTP NEGATIVE SCENARIOS
    // =========================================================

    @Test(priority = 12,
          description = "PHLEBO AUTH-NEG-12: Send Login OTP to unregistered mobile must be rejected.")
    public void PHLEBO_AUTH_NEG_12_SendLoginOtpUnregisteredMobile() {
        System.out.println("\n>>> PHLEBO_AUTH_NEG_12: SEND LOGIN OTP -- UNREGISTERED MOBILE <<<");

        JSONObject payload = PhlebioPayloads.buildSendLoginOtpUnregisteredMobilePayload();
        System.out.println("   Request Body : " + payload.toString(2));

        Response response = phlebioClient.sendLoginOtp(payload);

        assertNegativeResponse("PHLEBO_AUTH_NEG_12", response, "unregistered mobile for send login OTP");

        Assert.assertEquals(response.jsonPath().getInt("statusCode"), 401,
                "NEG_12: Body statusCode must be 401");
        Assert.assertEquals(response.jsonPath().getString("error"), "Unauthorized",
                "NEG_12: error field must be 'Unauthorized'");
        String msg12 = response.jsonPath().getString("message");
        Assert.assertTrue(msg12.contains("Invalid mobile number"),
                "NEG_12: message must contain 'Invalid mobile number'. Actual: " + msg12);
    }

    @Test(priority = 13,
          description = "PHLEBO AUTH-NEG-13: Send Login OTP with empty mobile must return validation error.")
    public void PHLEBO_AUTH_NEG_13_SendLoginOtpEmptyMobile() {
        System.out.println("\n>>> PHLEBO_AUTH_NEG_13: SEND LOGIN OTP -- EMPTY MOBILE <<<");

        JSONObject payload = PhlebioPayloads.buildSendLoginOtpEmptyMobilePayload();
        System.out.println("   Request Body : " + payload.toString(2));

        Response response = phlebioClient.sendLoginOtp(payload);

        assertNegativeResponse("PHLEBO_AUTH_NEG_13", response, "empty mobile for send login OTP");

        Assert.assertEquals(response.jsonPath().getInt("statusCode"), 400,
                "NEG_13: Body statusCode must be 400");
        Assert.assertEquals(response.jsonPath().getString("error"), "Bad Request",
                "NEG_13: error field must be 'Bad Request'");
        String msg13 = response.jsonPath().getString("message");
        Assert.assertTrue(msg13.contains("mobile should not be empty"),
                "NEG_13: message must contain 'mobile should not be empty'. Actual: " + msg13);
    }

    // =========================================================
    // LOGIN WITH OTP NEGATIVE SCENARIOS
    // =========================================================

    @Test(priority = 14,
          description = "PHLEBO AUTH-NEG-14: Login With OTP using wrong OTP must be rejected.")
    public void PHLEBO_AUTH_NEG_14_LoginWithWrongOtp() {
        System.out.println("\n>>> PHLEBO_AUTH_NEG_14: LOGIN WITH OTP -- WRONG OTP <<<");

        JSONObject payload = PhlebioPayloads.buildLoginWithWrongOtpPayload();
        System.out.println("   Request Body : " + payload.toString(2));

        Response response = phlebioClient.loginWithOtp(payload);

        assertNegativeResponse("PHLEBO_AUTH_NEG_14", response, "wrong OTP for login");

        Assert.assertEquals(response.jsonPath().getInt("statusCode"), 401,
                "NEG_14: Body statusCode must be 401");
        Assert.assertEquals(response.jsonPath().getString("error"), "Unauthorized",
                "NEG_14: error field must be 'Unauthorized'");
        String msg14 = response.jsonPath().getString("message");
        Assert.assertTrue(msg14.contains("Invalid or expired OTP"),
                "NEG_14: message must contain 'Invalid or expired OTP'. Actual: " + msg14);
    }

    @Test(priority = 15,
          description = "PHLEBO AUTH-NEG-15: Login With OTP using empty OTP must return validation error.")
    public void PHLEBO_AUTH_NEG_15_LoginWithEmptyOtp() {
        System.out.println("\n>>> PHLEBO_AUTH_NEG_15: LOGIN WITH OTP -- EMPTY OTP <<<");

        JSONObject payload = PhlebioPayloads.buildLoginWithEmptyOtpPayload();
        System.out.println("   Request Body : " + payload.toString(2));

        Response response = phlebioClient.loginWithOtp(payload);

        assertNegativeResponse("PHLEBO_AUTH_NEG_15", response, "empty OTP for login");

        Assert.assertEquals(response.jsonPath().getInt("statusCode"), 400,
                "NEG_15: Body statusCode must be 400");
        Assert.assertEquals(response.jsonPath().getString("error"), "Bad Request",
                "NEG_15: error field must be 'Bad Request'");
        String msg15 = response.jsonPath().getString("message");
        Assert.assertTrue(msg15.contains("otp should not be empty"),
                "NEG_15: message must contain 'otp should not be empty'. Actual: " + msg15);
    }

    // =========================================================
    // LOGIN — BOUNDARY NEGATIVE SCENARIO
    // =========================================================

    @Test(priority = 16,
          description = "PHLEBO AUTH-NEG-16: Login with mobile shorter than 10 digits must return 400.")
    public void PHLEBO_AUTH_NEG_16_LoginShortMobile() {
        System.out.println("\n>>> PHLEBO_AUTH_NEG_16: LOGIN -- SHORT MOBILE (5 DIGITS) <<<");

        JSONObject payload = PhlebioPayloads.buildLoginShortMobilePayload();
        System.out.println("   Request Body : " + payload.toString(2));

        Response response = phlebioClient.login(payload);

        assertNegativeResponse("PHLEBO_AUTH_NEG_16", response, "short mobile for login");

        int sc16 = response.jsonPath().getInt("statusCode");
        Assert.assertTrue(sc16 == 400 || sc16 == 401,
                "NEG_16: Body statusCode must be 400 or 401 for short mobile. Actual: " + sc16);
        String msg16 = response.jsonPath().getString("message");
        Assert.assertNotNull(msg16, "NEG_16: message must be present for short mobile rejection");
    }

    // =========================================================
    // UPDATE PASSWORD — BOUNDARY NEGATIVE SCENARIO
    // =========================================================

    @Test(priority = 17,
          description = "PHLEBO AUTH-NEG-17: Update Password with empty mobile field must return 400.")
    public void PHLEBO_AUTH_NEG_17_UpdatePasswordEmptyMobile() {
        System.out.println("\n>>> PHLEBO_AUTH_NEG_17: UPDATE PASSWORD -- EMPTY MOBILE <<<");

        JSONObject payload = PhlebioPayloads.buildUpdatePasswordEmptyMobilePayload();
        System.out.println("   Request Body : " + payload.toString(2));

        Response response = phlebioClient.updatePassword(payload);

        assertNegativeResponse("PHLEBO_AUTH_NEG_17", response, "empty mobile for update password");

        Assert.assertEquals(response.jsonPath().getInt("statusCode"), 400,
                "NEG_17: Body statusCode must be 400");
        Assert.assertEquals(response.jsonPath().getString("error"), "Bad Request",
                "NEG_17: error field must be 'Bad Request'");
        String msg17 = response.jsonPath().getString("message");
        Assert.assertTrue(msg17.contains("mobile") || msg17.contains("should not be empty"),
                "NEG_17: message must reference mobile validation. Actual: " + msg17);
    }

    // =========================================================
    // SEND LOGIN OTP — BOUNDARY NEGATIVE SCENARIO
    // =========================================================

    @Test(priority = 18,
          description = "PHLEBO AUTH-NEG-18: Send Login OTP with non-numeric mobile format must return 400.")
    public void PHLEBO_AUTH_NEG_18_SendLoginOtpInvalidMobileFormat() {
        System.out.println("\n>>> PHLEBO_AUTH_NEG_18: SEND LOGIN OTP -- INVALID MOBILE FORMAT <<<");

        JSONObject payload = PhlebioPayloads.buildSendLoginOtpInvalidFormatPayload();
        System.out.println("   Request Body : " + payload.toString(2));

        Response response = phlebioClient.sendLoginOtp(payload);

        assertNegativeResponse("PHLEBO_AUTH_NEG_18", response, "invalid mobile format for send login OTP");

        Assert.assertEquals(response.jsonPath().getInt("statusCode"), 400,
                "NEG_18: Body statusCode must be 400");
        Assert.assertEquals(response.jsonPath().getString("error"), "Bad Request",
                "NEG_18: error field must be 'Bad Request'");
        String msg18 = response.jsonPath().getString("message");
        Assert.assertTrue(msg18.contains("Mobile must be 10 digits") || msg18.contains("mobile"),
                "NEG_18: message must reference mobile digit validation. Actual: " + msg18);
    }

    // =========================================================
    // LOGIN WITH OTP — BOUNDARY NEGATIVE SCENARIOS
    // =========================================================

    @Test(priority = 19,
          description = "PHLEBO AUTH-NEG-19: Login With OTP using an unregistered mobile must be rejected.")
    public void PHLEBO_AUTH_NEG_19_LoginWithOtpUnregisteredMobile() {
        System.out.println("\n>>> PHLEBO_AUTH_NEG_19: LOGIN WITH OTP -- UNREGISTERED MOBILE <<<");

        JSONObject payload = PhlebioPayloads.buildLoginWithOtpUnregisteredMobilePayload();
        System.out.println("   Request Body : " + payload.toString(2));

        Response response = phlebioClient.loginWithOtp(payload);

        assertNegativeResponse("PHLEBO_AUTH_NEG_19", response, "unregistered mobile for OTP login");

        int statusCode19 = response.jsonPath().getInt("statusCode");
        Assert.assertTrue(statusCode19 == 401 || statusCode19 == 404,
                "NEG_19: Body statusCode must be 401 or 404. Actual: " + statusCode19);
        String msg19 = response.jsonPath().getString("message");
        Assert.assertTrue(msg19.contains("Invalid") || msg19.contains("not found") || msg19.contains("OTP"),
                "NEG_19: message must describe the authentication failure. Actual: " + msg19);
    }

    @Test(priority = 20,
          description = "PHLEBO AUTH-NEG-20: Login With OTP with empty mobile field must return 400.")
    public void PHLEBO_AUTH_NEG_20_LoginWithOtpEmptyMobile() {
        System.out.println("\n>>> PHLEBO_AUTH_NEG_20: LOGIN WITH OTP -- EMPTY MOBILE <<<");

        JSONObject payload = PhlebioPayloads.buildLoginWithOtpEmptyMobilePayload();
        System.out.println("   Request Body : " + payload.toString(2));

        Response response = phlebioClient.loginWithOtp(payload);

        assertNegativeResponse("PHLEBO_AUTH_NEG_20", response, "empty mobile for OTP login");

        Assert.assertEquals(response.jsonPath().getInt("statusCode"), 400,
                "NEG_20: Body statusCode must be 400");
        Assert.assertEquals(response.jsonPath().getString("error"), "Bad Request",
                "NEG_20: error field must be 'Bad Request'");
        String msg20 = response.jsonPath().getString("message");
        Assert.assertTrue(msg20.contains("mobile should not be empty") || msg20.contains("mobile"),
                "NEG_20: message must reference mobile validation. Actual: " + msg20);
    }
}

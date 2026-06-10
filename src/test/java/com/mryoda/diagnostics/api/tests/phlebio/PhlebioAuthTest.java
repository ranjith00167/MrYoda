package com.mryoda.diagnostics.api.tests.phlebio;

import api.phlebio.PhlebioClient;
import api.phlebio.PhlebioPayloads;
import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.config.ConfigLoader;
import com.mryoda.diagnostics.api.utils.AssertionUtil;
import com.mryoda.diagnostics.api.utils.LoggerUtil;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * ============================================================
 * PHLEBIO -- AUTHENTICATION  (Positive Flow)
 * ============================================================
 *
 * PHLEBO_AUTH_01 : Login Phlebo                            [Positive]
 * PHLEBO_AUTH_02 : Reset Password -- Send OTP              [Positive]
 * PHLEBO_AUTH_03 : Update Password                         [Positive]
 * PHLEBO_AUTH_04 : Logout Phlebo                           [Positive]
 * PHLEBO_AUTH_05 : Send Login OTP                          [Positive]
 * PHLEBO_AUTH_06 : Login With OTP                          [Positive]
 *
 * Execution order is strictly enforced via TestNG priority.
 * access_token from Login is shared across test methods via
 * the static field phleboAccessToken.
 * ============================================================
 */
public class PhlebioAuthTest extends BaseTest {

    // Shared state
    private static String phleboAccessToken;
    private static String phleboRefreshToken;
    private static String phleboGuid;
    private static String phleboName;
    private static String phleboMobile;

    private final PhlebioClient phlebioClient = new PhlebioClient();

    // Override base URL to point at Phlebio staging
    @BeforeClass(alwaysRun = true)
    @Override
    public void setUp() {
        LoggerUtil.info("====== Phlebio Auth Test Setup Started ======");
        RestAssured.baseURI = ConfigLoader.getConfig().phlebioBaseUrl();
        LoggerUtil.info("Phlebio Base URL: " + RestAssured.baseURI);
        LoggerUtil.info("====== Phlebio Auth Test Setup Completed ======");
    }

    // =========================================================
    // PHLEBO_AUTH_01 : Login Phlebo -- POSITIVE
    // =========================================================
    @Test(priority = 1,
          description = "PHLEBO AUTH-01: Login -- Verify phlebotomist can login with valid mobile and password. "
                      + "Extracts access_token, refresh_token and phlebo profile from response data.")
    public void PHLEBO_AUTH_01_Login() {
        System.out.println("\n>>> PHLEBO_AUTH_01: LOGIN PHLEBO <<<");

        JSONObject payload = PhlebioPayloads.buildLoginPayload();
        System.out.println("   Request Body : " + payload.toString(2));

        Response response = phlebioClient.login(payload);

        System.out.println("   Response Status : " + response.getStatusCode());
        System.out.println("   Response Body   : " + response.prettyPrint());

        // Status assertion
        AssertionUtil.verifyEquals(response.getStatusCode(), 200, "Login must return HTTP 200");
        Assert.assertTrue(Boolean.TRUE.equals(response.jsonPath().get("success")),
                "Response 'success' field must be true for Login");

        // Validate message field
        // Actual response shape:
        //   { "success": true, "message": "Login successful",
        //     "data": { "phlebo": { "guid":"...", "name":"...", "mobileNumber":"..." },
        //               "access_token": "...", "refresh_token": "..." } }
        String message = response.jsonPath().getString("message");
        Assert.assertNotNull(message, "Response 'message' field must not be null");
        System.out.println("   message : " + message);

        // Extract tokens from data.*
        phleboAccessToken  = response.jsonPath().getString("data.access_token");
        phleboRefreshToken = response.jsonPath().getString("data.refresh_token");
        Assert.assertNotNull(phleboAccessToken,  "data.access_token must be present in login response");
        Assert.assertNotNull(phleboRefreshToken, "data.refresh_token must be present in login response");

        // Extract phlebo profile from data.phlebo{}
        phleboGuid   = response.jsonPath().getString("data.phlebo.guid");
        phleboName   = response.jsonPath().getString("data.phlebo.name");
        phleboMobile = response.jsonPath().getString("data.phlebo.mobileNumber");
        Assert.assertNotNull(phleboGuid,   "data.phlebo.guid must be present in login response");
        Assert.assertNotNull(phleboName,   "data.phlebo.name must be present in login response");
        Assert.assertNotNull(phleboMobile, "data.phlebo.mobileNumber must be present in login response");
        Assert.assertEquals(phleboMobile, PhlebioPayloads.PHLEBO_MOBILE,
                "Returned mobileNumber must match the mobile used for login");

        printResponseDetails("PHLEBO_AUTH_01", response);
        System.out.println("   [PASS] PHLEBO_AUTH_01 PASSED -- Login successful.\n");
    }

    // =========================================================
    // PHLEBO_AUTH_02 : Reset Password -- Send OTP -- POSITIVE
    // =========================================================
    @Test(priority = 2,
          description = "PHLEBO AUTH-02: Reset Password (Send OTP) -- Verify OTP is triggered for registered mobile.")
    public void PHLEBO_AUTH_02_ResetPasswordSendOtp() {
        System.out.println("\n>>> PHLEBO_AUTH_02: RESET PASSWORD -- SEND OTP <<<");

        JSONObject payload = PhlebioPayloads.buildResetPasswordSendOtpPayload();
        System.out.println("   Request Body : " + payload.toString(2));

        Response response = phlebioClient.resetPasswordSendOtp(payload);

        System.out.println("   Response Status : " + response.getStatusCode());
        System.out.println("   Response Body   : " + response.prettyPrint());

        int statusCode = response.getStatusCode();
        Assert.assertTrue(statusCode == 200 || statusCode == 201,
                "Reset Password (Send OTP) must return HTTP 200 or 201. Actual: " + statusCode);

        String message = response.jsonPath().getString("message");
        Assert.assertNotNull(message, "Response 'message' field must not be null");
        Assert.assertTrue(Boolean.TRUE.equals(response.jsonPath().get("success")),
                "Response 'success' field must be true for Send OTP");
        Assert.assertTrue(message.toLowerCase().contains("otp"),
                "Message should confirm OTP was sent. Actual: " + message);
        printResponseDetails("PHLEBO_AUTH_02", response);
        System.out.println("   [PASS] PHLEBO_AUTH_02 PASSED -- Reset Password OTP request accepted.\n");
    }

    // =========================================================
    // PHLEBO_AUTH_03 : Update Password -- POSITIVE
    // =========================================================
    @Test(priority = 3,
          description = "PHLEBO AUTH-03: Update Password -- Verify phlebotomist password can be updated. "
                      + "Password is kept as 12345678 to allow subsequent Login tests to succeed.")
    public void PHLEBO_AUTH_03_UpdatePassword() {
        System.out.println("\n>>> PHLEBO_AUTH_03: UPDATE PASSWORD <<<");

        JSONObject payload = PhlebioPayloads.buildUpdatePasswordPayload();
        System.out.println("   Request Body : " + payload.toString(2));

        Response response = phlebioClient.updatePassword(payload);

        System.out.println("   Response Status : " + response.getStatusCode());
        System.out.println("   Response Body   : " + response.prettyPrint());

        int statusCode = response.getStatusCode();
        Assert.assertTrue(statusCode == 200 || statusCode == 201,
                "Update Password must return HTTP 200 or 201. Actual: " + statusCode);

        String message = response.jsonPath().getString("message");
        Assert.assertNotNull(message, "Response 'message' field must not be null");
        Assert.assertTrue(Boolean.TRUE.equals(response.jsonPath().get("success")),
                "Response 'success' field must be true for Update Password");
        Assert.assertTrue(message.toLowerCase().contains("password"),
                "Message should confirm password was updated. Actual: " + message);
        printResponseDetails("PHLEBO_AUTH_03", response);
        System.out.println("   [PASS] PHLEBO_AUTH_03 PASSED -- Password updated successfully.\n");
    }

    // =========================================================
    // PHLEBO_AUTH_04 : Logout Phlebo -- POSITIVE
    // =========================================================
    @Test(priority = 4,
          dependsOnMethods = "PHLEBO_AUTH_01_Login",
          description = "PHLEBO AUTH-04: Logout -- Verify phlebotomist can logout using the access_token "
                      + "obtained from PHLEBO_AUTH_01_Login. FCM token is cleared server-side.")
    public void PHLEBO_AUTH_04_Logout() {
        System.out.println("\n>>> PHLEBO_AUTH_04: LOGOUT PHLEBO <<<");
        System.out.println("   Using access_token from PHLEBO_AUTH_01_Login");

        Assert.assertNotNull(phleboAccessToken,
                "access_token must be available from PHLEBO_AUTH_01_Login before running Logout");

        Response response = phlebioClient.logout(phleboAccessToken);

        System.out.println("   Response Status : " + response.getStatusCode());
        System.out.println("   Response Body   : " + response.prettyPrint());

        int statusCode = response.getStatusCode();
        Assert.assertTrue(statusCode == 200 || statusCode == 201,
                "Logout must return HTTP 200 or 201. Actual: " + statusCode);

        String message = response.jsonPath().getString("message");
        Assert.assertNotNull(message, "Response 'message' field must not be null");
        Assert.assertTrue(Boolean.TRUE.equals(response.jsonPath().get("success")),
                "Response 'success' field must be true for Logout");
        Assert.assertTrue(message.toLowerCase().contains("logout"),
                "Message should confirm logout. Actual: " + message);
        printResponseDetails("PHLEBO_AUTH_04", response);
        System.out.println("   [PASS] PHLEBO_AUTH_04 PASSED -- Logout successful. Access token invalidated.\n");
    }

    // =========================================================
    // PHLEBO_AUTH_05 : Send Login OTP -- POSITIVE
    // =========================================================
    @Test(priority = 5,
          description = "PHLEBO AUTH-05: Send Login OTP -- Verify OTP is dispatched to the "
                      + "phlebotomist mobile for OTP-based login. Expects 200/201 with success message.")
    public void PHLEBO_AUTH_05_SendLoginOtp() {
        System.out.println("\n>>> PHLEBO_AUTH_05: SEND LOGIN OTP <<<");

        JSONObject payload = PhlebioPayloads.buildSendLoginOtpPayload();
        System.out.println("   Request Body : " + payload.toString(2));

        Response response = phlebioClient.sendLoginOtp(payload);

        System.out.println("   Response Status : " + response.getStatusCode());
        System.out.println("   Response Body   : " + response.prettyPrint());

        int statusCode = response.getStatusCode();
        Assert.assertTrue(statusCode == 200 || statusCode == 201,
                "Send Login OTP must return HTTP 200 or 201. Actual: " + statusCode);

        String message = response.jsonPath().getString("message");
        Assert.assertNotNull(message, "Response 'message' field must not be null");
        Assert.assertTrue(Boolean.TRUE.equals(response.jsonPath().get("success")),
                "Response 'success' field must be true for Send Login OTP");
        Assert.assertTrue(message.toLowerCase().contains("otp"),
                "Message should confirm OTP was dispatched. Actual: " + message);
        printResponseDetails("PHLEBO_AUTH_05", response);
        System.out.println("   [PASS] PHLEBO_AUTH_05 PASSED -- Login OTP dispatched successfully.\n");
    }

    // =========================================================
    // PHLEBO_AUTH_06 : Login With OTP -- POSITIVE
    // =========================================================
    @Test(priority = 6,
          dependsOnMethods = "PHLEBO_AUTH_05_SendLoginOtp",
          description = "PHLEBO AUTH-06: Login With OTP -- Verify phlebotomist can authenticate using "
                      + "mobile + OTP (static 123456) + FCM token. Extracts access_token and refresh_token.")
    public void PHLEBO_AUTH_06_LoginWithOtp() {
        System.out.println("\n>>> PHLEBO_AUTH_06: LOGIN WITH OTP <<<");

        JSONObject payload = PhlebioPayloads.buildLoginWithOtpPayload(PhlebioPayloads.STATIC_OTP);
        System.out.println("   Request Body : " + payload.toString(2));

        Response response = phlebioClient.loginWithOtp(payload);

        System.out.println("   Response Status : " + response.getStatusCode());
        System.out.println("   Response Body   : " + response.prettyPrint());

        int statusCode = response.getStatusCode();
        Assert.assertTrue(statusCode == 200 || statusCode == 201,
                "Login With OTP must return HTTP 200 or 201. Actual: " + statusCode);

        String message = response.jsonPath().getString("message");
        Assert.assertNotNull(message, "Response 'message' field must not be null");

        // Extract tokens -- same shape as password login response
        String otpAccessToken  = response.jsonPath().getString("data.access_token");
        String otpRefreshToken = response.jsonPath().getString("data.refresh_token");
        Assert.assertNotNull(otpAccessToken,  "data.access_token must be present in OTP login response");
        Assert.assertNotNull(otpRefreshToken, "data.refresh_token must be present in OTP login response");
        Assert.assertTrue(Boolean.TRUE.equals(response.jsonPath().get("success")),
                "Response 'success' field must be true for Login With OTP");

        // Phlebo profile assertions
        String otpPhleboGuid   = response.jsonPath().getString("data.phlebo.guid");
        String otpPhleboName   = response.jsonPath().getString("data.phlebo.name");
        String otpPhleboMobile = response.jsonPath().getString("data.phlebo.mobileNumber");
        Assert.assertNotNull(otpPhleboGuid,   "data.phlebo.guid must be present in OTP login response");
        Assert.assertNotNull(otpPhleboName,   "data.phlebo.name must be present in OTP login response");
        Assert.assertNotNull(otpPhleboMobile, "data.phlebo.mobileNumber must be present in OTP login response");
        Assert.assertEquals(otpPhleboMobile, PhlebioPayloads.PHLEBO_MOBILE,
                "Returned mobileNumber must match the mobile used for OTP login");

        printResponseDetails("PHLEBO_AUTH_06", response);
        System.out.println("   [PASS] PHLEBO_AUTH_06 PASSED -- OTP login successful.\n");
    }

    // =========================================================
    // HELPER : Extract and print all available response fields
    // =========================================================
    private void printResponseDetails(String testId, Response response) {
        System.out.println("   ---- " + testId + " Response Details ----");

        // Top-level fields
        Object success    = response.jsonPath().get("success");
        String message    = response.jsonPath().getString("message");
        Object statusCode = response.jsonPath().get("statusCode");

        System.out.println("   success    : " + success);
        System.out.println("   message    : " + message);
        if (statusCode != null) System.out.println("   statusCode : " + statusCode);

        // Tokens (present in login / login-with-otp responses)
        String accessToken  = response.jsonPath().getString("data.access_token");
        String refreshToken = response.jsonPath().getString("data.refresh_token");
        Object expiresIn    = response.jsonPath().get("data.expires_in");
        if (accessToken  != null) System.out.println("   access_token    : " + accessToken);
        if (refreshToken != null) System.out.println("   refresh_token   : " + refreshToken);
        if (expiresIn    != null) System.out.println("   expires_in      : " + expiresIn);

        // Hub details (present in login responses)
        Object hubId   = response.jsonPath().get("data.hub_id");
        Object hubName = response.jsonPath().get("data.hub_name");
        if (hubId   != null) System.out.println("   hub_id          : " + hubId);
        if (hubName != null) System.out.println("   hub_name        : " + hubName);

        // Phlebo profile (present in login, update-password, logout responses)
        String guid         = response.jsonPath().getString("data.phlebo.guid");
        String name         = response.jsonPath().getString("data.phlebo.name");
        String mobileNumber = response.jsonPath().getString("data.phlebo.mobileNumber");
        Object id           = response.jsonPath().get("data.phlebo.id");
        Object employeeId   = response.jsonPath().get("data.phlebo.employeeId");
        Object zoneId       = response.jsonPath().get("data.phlebo.zoneId");
        Object email        = response.jsonPath().get("data.phlebo.email");
        Object profilePic   = response.jsonPath().get("data.phlebo.profilePic");
        Object isActive     = response.jsonPath().get("data.phlebo.isActive");
        Object type         = response.jsonPath().get("data.phlebo.type");
        Object createdAt    = response.jsonPath().get("data.phlebo.createdAt");
        Object updatedAt    = response.jsonPath().get("data.phlebo.updatedAt");

        if (guid         != null) System.out.println("   phlebo.guid        : " + guid);
        if (name         != null) System.out.println("   phlebo.name        : " + name);
        if (mobileNumber != null) System.out.println("   phlebo.mobileNumber: " + mobileNumber);
        if (id           != null) System.out.println("   phlebo.id          : " + id);
        if (employeeId   != null) System.out.println("   phlebo.employeeId  : " + employeeId);
        if (zoneId       != null) System.out.println("   phlebo.zoneId      : " + zoneId);
        if (email        != null) System.out.println("   phlebo.email       : " + email);
        if (profilePic   != null) System.out.println("   phlebo.profilePic  : " + profilePic);
        if (isActive     != null) System.out.println("   phlebo.isActive    : " + isActive);
        if (type         != null) System.out.println("   phlebo.type        : " + type);
        if (createdAt    != null) System.out.println("   phlebo.createdAt   : " + createdAt);
        if (updatedAt    != null) System.out.println("   phlebo.updatedAt   : " + updatedAt);

        System.out.println("   ----------------------------------------");
    }
}

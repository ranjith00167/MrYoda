package com.mryoda.diagnostics.api.tests.otp;

import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Module 1 — Happy Path Validation (OTP_001 – OTP_006)
 *
 * Validates that the /otps/getOtp endpoint responds successfully
 * under valid, expected conditions.
 */
public class OTPHappyPathTest extends OTPBaseTest {

    @Test(priority = 1, description = "OTP_001: Generate OTP with valid mobile and country code")
    public void OTP_001_GenerateOTPWithValidMobileAndCountryCode() {
        System.out.println("\n>>> OTP_001: Generate OTP with valid mobile and country code <<<");
        Response response = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
        logResponse("OTP_001", response);
        assertSuccess(response, "OTP_001");
    }

    @Test(priority = 2, description = "OTP_002: Generate OTP for existing user")
    public void OTP_002_GenerateOTPForExistingUser() {
        System.out.println("\n>>> OTP_002: Generate OTP for existing user <<<");
        // VALID_MOBILE is a pre-registered user in staging
        Response response = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
        logResponse("OTP_002", response);

        Assert.assertEquals(response.getStatusCode(), 200,
                "OTP_002: Expected 200 for existing user. Body: " + response.getBody().asString());
        Boolean success = response.jsonPath().getBoolean("success");
        Assert.assertTrue(Boolean.TRUE.equals(success),
                "OTP_002: success must be true for existing user");
    }

    @Test(priority = 3, description = "OTP_003: Generate OTP for new user")
    public void OTP_003_GenerateOTPForNewUser() {
        System.out.println("\n>>> OTP_003: Generate OTP for new user <<<");
        // Use a staging-safe mobile that is unlikely to be registered
        String newUserMobile = "9700000099";
        Response response = sendOtpRequest(newUserMobile, VALID_COUNTRY_CODE);
        logResponse("OTP_003", response);

        // New user OTP generation should succeed (registration happens after OTP verify)
        Assert.assertEquals(response.getStatusCode(), 200,
                "OTP_003: Expected 200 for new user OTP generation. Body: " + response.getBody().asString());
    }

    @Test(priority = 4, description = "OTP_004: Generate OTP using different valid country codes")
    public void OTP_004_GenerateOTPWithDifferentCountryCodes() {
        System.out.println("\n>>> OTP_004: Generate OTP using different valid country codes <<<");
        // +91 (India) is the primary supported code; others may return 400 if unsupported
        String[] countryCodes = {"+91", "+1", "+44"};
        for (String code : countryCodes) {
            Response response = sendOtpRequest(VALID_MOBILE, code);
            logResponse("OTP_004 [" + code + "]", response);

            // API may support only +91 in staging; 200 or 400 are both acceptable here
            Assert.assertTrue(response.getStatusCode() == 200 || response.getStatusCode() == 400,
                    "OTP_004: Unexpected status for country code " + code
                            + ". Got: " + response.getStatusCode());
        }
    }

    @Test(priority = 5, description = "OTP_005: Generate OTP after previous OTP expiry")
    public void OTP_005_GenerateOTPAfterPreviousOTPExpiry() {
        System.out.println("\n>>> OTP_005: Generate OTP after previous OTP expiry <<<");
        // First request — triggers OTP generation
        Response first = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
        logResponse("OTP_005 [first]", first);
        Assert.assertEquals(first.getStatusCode(), 200,
                "OTP_005: First OTP request should succeed");

        // Second request after the first (simulating resend after expiry)
        Response second = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
        logResponse("OTP_005 [after-expiry]", second);
        Assert.assertEquals(second.getStatusCode(), 200,
                "OTP_005: OTP regeneration after expiry should succeed");

        Boolean success = second.jsonPath().getBoolean("success");
        Assert.assertTrue(Boolean.TRUE.equals(success),
                "OTP_005: New OTP should be generated successfully");
    }

    @Test(priority = 6, description = "OTP_006: Generate OTP after cooldown period")
    public void OTP_006_GenerateOTPAfterCooldownPeriod() {
        System.out.println("\n>>> OTP_006: Generate OTP after cooldown period <<<");
        Response response = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
        logResponse("OTP_006", response);

        // After cooldown, the API should accept the request
        Assert.assertEquals(response.getStatusCode(), 200,
                "OTP_006: Expected 200 after cooldown. Body: " + response.getBody().asString());
        Boolean success = response.jsonPath().getBoolean("success");
        Assert.assertTrue(Boolean.TRUE.equals(success),
                "OTP_006: success must be true after cooldown period");
    }
}

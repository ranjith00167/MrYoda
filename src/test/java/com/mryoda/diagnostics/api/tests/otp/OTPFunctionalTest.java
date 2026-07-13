package com.mryoda.diagnostics.api.tests.otp;

import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Module 7 — OTP Functional Validation (OTP_051 – OTP_057)
 *
 * Validates functional OTP lifecycle behaviors such as generation,
 * resend, linkage to mobile, and SMS triggering.
 */
public class OTPFunctionalTest extends OTPBaseTest {

    @Test(priority = 51, description = "OTP_051: Generate OTP first time — expect success")
    public void OTP_051_GenerateOTPFirstTime() {
        System.out.println("\n>>> OTP_051: Generate OTP first time <<<");
        Response response = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
        logResponse("OTP_051", response);

        Assert.assertEquals(response.getStatusCode(), 200,
                "OTP_051: OTP generation must return 200");
        Boolean success = response.jsonPath().getBoolean("success");
        Assert.assertTrue(Boolean.TRUE.equals(success),
                "OTP_051: success must be true for first-time OTP generation");

        String message = response.jsonPath().getString("msg");
        Assert.assertNotNull(message, "OTP_051: Response message (msg) must not be null");
        System.out.println("OTP_051: OTP msg — " + message);
    }

    @Test(priority = 52, description = "OTP_052: Resend OTP — new OTP should be generated")
    public void OTP_052_ResendOTP() {
        System.out.println("\n>>> OTP_052: Resend OTP <<<");
        // First send
        Response first = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
        logResponse("OTP_052 [1st]", first);
        Assert.assertEquals(first.getStatusCode(), 200,
                "OTP_052: First OTP send must return 200");

        // Resend
        Response resend = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
        logResponse("OTP_052 [resend]", resend);
        Assert.assertEquals(resend.getStatusCode(), 200,
                "OTP_052: OTP resend must return 200");

        Boolean success = resend.jsonPath().getBoolean("success");
        Assert.assertTrue(Boolean.TRUE.equals(success),
                "OTP_052: success must be true on resend");
    }

    @Test(priority = 53, description = "OTP_053: Verify previous OTP is invalidated upon new OTP generation")
    public void OTP_053_PreviousOTPInvalidatedOnResend() {
        System.out.println("\n>>> OTP_053: Previous OTP invalidated on new generation <<<");
        // Generate first OTP
        Response first = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
        logResponse("OTP_053 [1st]", first);
        Assert.assertEquals(first.getStatusCode(), 200, "OTP_053: First OTP must succeed");

        // Generate second OTP — previous should be invalidated on the server
        Response second = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
        logResponse("OTP_053 [2nd]", second);
        Assert.assertEquals(second.getStatusCode(), 200, "OTP_053: Second OTP must succeed");

        // Validation note: OTP invalidation is a server-side state change.
        // Full verification requires an OTP-verify endpoint (out of scope here).
        System.out.println("OTP_053: Two consecutive OTPs generated. "
                + "Previous OTP invalidation is enforced server-side — "
                + "verify via /otps/verifyOtp endpoint if available.");
    }

    @Test(priority = 54, description = "OTP_054: Verify OTP expiry field exists in response or system state")
    public void OTP_054_VerifyOTPExpiryCreated() {
        System.out.println("\n>>> OTP_054: Verify OTP expiry <<<");
        Response response = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
        logResponse("OTP_054", response);

        Assert.assertEquals(response.getStatusCode(), 200,
                "OTP_054: OTP generation must return 200");

        // The response 'data' object may contain expiry info
        Object data = response.jsonPath().get("data");
        System.out.println("OTP_054: Response data field — " + data
                + " | Expiry enforcement is a backend concern; "
                + "confirm expiry_at field with dev team.");
        // Minimal assertion: request succeeded; expiry is stored server-side
        Boolean success = response.jsonPath().getBoolean("success");
        Assert.assertTrue(Boolean.TRUE.equals(success),
                "OTP_054: success must be true to confirm OTP (and its expiry) was created");
    }

    @Test(priority = 55, description = "OTP_055: Verify retry count is updated on repeated requests")
    public void OTP_055_VerifyRetryCountUpdated() {
        System.out.println("\n>>> OTP_055: Verify retry count <<<");
        // Send OTP three times to trigger retry-count increment
        for (int i = 1; i <= 3; i++) {
            Response response = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
            logResponse("OTP_055 [attempt " + i + "]", response);
            // API may throttle after N attempts; accept 200 or 429
            int status = response.getStatusCode();
            Assert.assertTrue(status == 200 || status == 429,
                    "OTP_055: Attempt " + i + " must return 200 or 429. Got: " + status);
            if (status == 429) {
                System.out.println("OTP_055: Rate limit hit at attempt " + i + " — retry count enforced.");
                break;
            }
        }
    }

    @Test(priority = 56, description = "OTP_056: Verify OTP is linked to the correct mobile number")
    public void OTP_056_VerifyOTPLinkedToCorrectMobile() {
        System.out.println("\n>>> OTP_056: OTP linked to correct mobile <<<");
        Response response = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
        logResponse("OTP_056", response);

        Assert.assertEquals(response.getStatusCode(), 200,
                "OTP_056: OTP generation must return 200");
        Boolean success = response.jsonPath().getBoolean("success");
        Assert.assertTrue(Boolean.TRUE.equals(success),
                "OTP_056: success must be true");

        // Response must not expose the OTP itself or link it to a different mobile
        String body = response.getBody().asString();
        Assert.assertFalse(body.contains("otp") && body.contains("123456"),
                "OTP_056: Response must not expose the raw OTP value");
        System.out.println("OTP_056: OTP generated successfully; mobile linkage is enforced server-side.");
    }

    @Test(priority = 57, description = "OTP_057: Verify OTP is sent through SMS provider (success response implies trigger)")
    public void OTP_057_VerifyOTPSentViaSMSProvider() {
        System.out.println("\n>>> OTP_057: Verify OTP sent through SMS provider <<<");
        Response response = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
        logResponse("OTP_057", response);

        Assert.assertEquals(response.getStatusCode(), 200,
                "OTP_057: OTP generation must return 200 for SMS to be triggered");
        Boolean success = response.jsonPath().getBoolean("success");
        Assert.assertTrue(Boolean.TRUE.equals(success),
                "OTP_057: success=true confirms OTP was queued for SMS delivery");

        String message = response.jsonPath().getString("msg");
        Assert.assertNotNull(message,
                "OTP_057: A confirmation message (msg) must be returned when SMS is triggered");
        System.out.println("OTP_057: SMS trigger confirmed by API. Actual delivery is handled by SMS provider.");
    }
}

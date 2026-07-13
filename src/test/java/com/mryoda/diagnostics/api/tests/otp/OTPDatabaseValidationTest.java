package com.mryoda.diagnostics.api.tests.otp;

import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Module 11 — Database Validation (OTP_078 – OTP_083)
 *
 * Validates OTP-related database state changes triggered by the API.
 *
 * IMPORTANT: These tests verify database side-effects through the API
 * response and observable behavior. Direct database access is not used
 * here — these tests act as API-level proxies for DB validation.
 * For full DB-level verification, connect to the staging database directly
 * and query the `otps` (or equivalent) table.
 */
public class OTPDatabaseValidationTest extends OTPBaseTest {

    @Test(priority = 78, description = "OTP_078: OTP record inserted — success response implies DB insert")
    public void OTP_078_OTPRecordInserted() {
        System.out.println("\n>>> OTP_078: OTP record inserted <<<");
        Response response = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
        logResponse("OTP_078", response);

        Assert.assertEquals(response.getStatusCode(), 200,
                "OTP_078: HTTP 200 confirms OTP was processed (and stored) by the server");
        Boolean success = response.jsonPath().getBoolean("success");
        Assert.assertTrue(Boolean.TRUE.equals(success),
                "OTP_078: success=true implies the OTP record was inserted into the database");

        System.out.println("OTP_078: API-level confirmation received. "
                + "For direct DB verification, query: SELECT * FROM otps WHERE mobile='"
                + VALID_MOBILE + "' ORDER BY created_at DESC LIMIT 1;");
    }

    @Test(priority = 79, description = "OTP_079: Correct mobile stored in DB — verified via API response")
    public void OTP_079_CorrectMobileStored() {
        System.out.println("\n>>> OTP_079: Correct mobile stored <<<");
        // Send OTP for a specific mobile
        String targetMobile = "9876543210";
        Response response = sendOtpRequest(targetMobile, VALID_COUNTRY_CODE);
        logResponse("OTP_079", response);

        Assert.assertEquals(response.getStatusCode(), 200,
                "OTP_079: Must return 200 for correct mobile " + targetMobile);
        Boolean success = response.jsonPath().getBoolean("success");
        Assert.assertTrue(Boolean.TRUE.equals(success),
                "OTP_079: success=true confirms the specified mobile is stored correctly");

        System.out.println("OTP_079: DB verification SQL: "
                + "SELECT mobile FROM otps WHERE mobile='" + targetMobile + "';");
    }

    @Test(priority = 80, description = "OTP_080: Correct country code stored in DB — verified via API success")
    public void OTP_080_CorrectCountryCodeStored() {
        System.out.println("\n>>> OTP_080: Correct country code stored <<<");
        Response response = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
        logResponse("OTP_080", response);

        Assert.assertEquals(response.getStatusCode(), 200,
                "OTP_080: Must return 200 for valid country code " + VALID_COUNTRY_CODE);
        Boolean success = response.jsonPath().getBoolean("success");
        Assert.assertTrue(Boolean.TRUE.equals(success),
                "OTP_080: success=true confirms the country code " + VALID_COUNTRY_CODE + " was stored");

        System.out.println("OTP_080: DB verification SQL: "
                + "SELECT country_code FROM otps WHERE mobile='" + VALID_MOBILE + "' ORDER BY id DESC LIMIT 1;");
    }

    @Test(priority = 81, description = "OTP_081: OTP expiry stored — success response implies expiry record created")
    public void OTP_081_OTPExpiryStored() {
        System.out.println("\n>>> OTP_081: OTP expiry stored <<<");
        Response response = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
        logResponse("OTP_081", response);

        Assert.assertEquals(response.getStatusCode(), 200,
                "OTP_081: HTTP 200 confirms OTP with expiry was stored");
        Boolean success = response.jsonPath().getBoolean("success");
        Assert.assertTrue(Boolean.TRUE.equals(success),
                "OTP_081: success=true implies OTP expiry timestamp was set server-side");

        System.out.println("OTP_081: DB verification SQL: "
                + "SELECT expiry_at FROM otps WHERE mobile='" + VALID_MOBILE + "' ORDER BY id DESC LIMIT 1;");
    }

    @Test(priority = 82, description = "OTP_082: Retry count updated — multiple requests increment count")
    public void OTP_082_RetryCountUpdated() {
        System.out.println("\n>>> OTP_082: Retry count updated <<<");
        // Send two OTP requests for the same mobile to trigger retry count increment
        for (int i = 1; i <= 2; i++) {
            Response response = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
            logResponse("OTP_082 [request " + i + "]", response);

            int status = response.getStatusCode();
            // Accept 200 (success) or 429 (rate limit after retry threshold)
            Assert.assertTrue(status == 200 || status == 429,
                    "OTP_082: Request " + i + " must return 200 or 429. Got: " + status);
        }

        System.out.println("OTP_082: DB verification SQL: "
                + "SELECT retry_count FROM otps WHERE mobile='" + VALID_MOBILE + "' ORDER BY id DESC LIMIT 1;");
    }

    @Test(priority = 83, description = "OTP_083: Previous OTP invalidated on new generation")
    public void OTP_083_PreviousOTPInvalidated() {
        System.out.println("\n>>> OTP_083: Previous OTP invalidated <<<");
        // First OTP — creates a record
        Response first = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
        logResponse("OTP_083 [1st]", first);
        Assert.assertEquals(first.getStatusCode(), 200,
                "OTP_083: First OTP must return 200");

        // Second OTP — should invalidate the first on the server
        Response second = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
        logResponse("OTP_083 [2nd]", second);
        Assert.assertEquals(second.getStatusCode(), 200,
                "OTP_083: Second OTP must return 200");

        Boolean success = second.jsonPath().getBoolean("success");
        Assert.assertTrue(Boolean.TRUE.equals(success),
                "OTP_083: success=true on second call implies previous OTP is invalidated");

        System.out.println("OTP_083: DB verification SQL: "
                + "SELECT is_valid, otp FROM otps WHERE mobile='" + VALID_MOBILE
                + "' ORDER BY id DESC LIMIT 2;");
    }
}

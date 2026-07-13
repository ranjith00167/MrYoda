package com.mryoda.diagnostics.api.tests.otp;

import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Module 2 — Mobile Number Validation (OTP_007 – OTP_020)
 *
 * Verifies the API correctly rejects or handles invalid mobile values.
 */
public class OTPMobileValidationTest extends OTPBaseTest {

    @Test(priority = 7, description = "OTP_007: Empty mobile number")
    public void OTP_007_EmptyMobile() {
        System.out.println("\n>>> OTP_007: Empty mobile <<<");
        Response response = sendOtpRequest("", VALID_COUNTRY_CODE);
        logResponse("OTP_007", response);
        assertErrorResponse(response, "OTP_007");
    }

    @Test(priority = 8, description = "OTP_008: Null mobile number")
    public void OTP_008_NullMobile() {
        System.out.println("\n>>> OTP_008: Null mobile <<<");

        Map<String, Object> payload = new HashMap<>();
        payload.put("mobile", null);
        payload.put("country_code", VALID_COUNTRY_CODE);

        Response response = baseSpec()
                .contentType(ContentType.JSON)
                .body(payload)
                .post(APIEndpoints.OTP_REQUEST);
        logResponse("OTP_008", response);
        assertErrorResponse(response, "OTP_008");
    }

    @Test(priority = 9, description = "OTP_009: Missing mobile field entirely")
    public void OTP_009_MissingMobileField() {
        System.out.println("\n>>> OTP_009: Missing mobile field <<<");

        // Only country_code present; mobile field absent
        Map<String, Object> payload = new HashMap<>();
        payload.put("country_code", VALID_COUNTRY_CODE);

        Response response = baseSpec()
                .contentType(ContentType.JSON)
                .body(payload)
                .post(APIEndpoints.OTP_REQUEST);
        logResponse("OTP_009", response);
        assertErrorResponse(response, "OTP_009");
    }

    @Test(priority = 10, description = "OTP_010: Mobile contains only spaces")
    public void OTP_010_MobileOnlySpaces() {
        System.out.println("\n>>> OTP_010: Mobile with only spaces <<<");
        Response response = sendOtpRequest("          ", VALID_COUNTRY_CODE);
        logResponse("OTP_010", response);
        assertErrorResponse(response, "OTP_010");
    }

    @Test(priority = 11, description = "OTP_011: Mobile less than minimum length (3 digits)")
    public void OTP_011_MobileTooShort() {
        System.out.println("\n>>> OTP_011: Mobile too short <<<");
        Response response = sendOtpRequest("123", VALID_COUNTRY_CODE);
        logResponse("OTP_011", response);
        assertErrorResponse(response, "OTP_011");
    }

    @Test(priority = 12, description = "OTP_012: Mobile greater than maximum length (20 digits)")
    public void OTP_012_MobileTooLong() {
        System.out.println("\n>>> OTP_012: Mobile too long <<<");
        Response response = sendOtpRequest("12345678901234567890", VALID_COUNTRY_CODE);
        logResponse("OTP_012", response);
        assertErrorResponse(response, "OTP_012");
    }

    @Test(priority = 13, description = "OTP_013: Alphabetic mobile")
    public void OTP_013_AlphabeticMobile() {
        System.out.println("\n>>> OTP_013: Alphabetic mobile <<<");
        Response response = sendOtpRequest("abcdefghij", VALID_COUNTRY_CODE);
        logResponse("OTP_013", response);
        assertErrorResponse(response, "OTP_013");
    }

    @Test(priority = 14, description = "OTP_014: Alphanumeric mobile")
    public void OTP_014_AlphanumericMobile() {
        System.out.println("\n>>> OTP_014: Alphanumeric mobile <<<");
        Response response = sendOtpRequest("98abc09090", VALID_COUNTRY_CODE);
        logResponse("OTP_014", response);
        assertErrorResponse(response, "OTP_014");
    }

    @Test(priority = 15, description = "OTP_015: Mobile with special characters")
    public void OTP_015_MobileWithSpecialChars() {
        System.out.println("\n>>> OTP_015: Mobile with special characters <<<");
        Response response = sendOtpRequest("98909@9898", VALID_COUNTRY_CODE);
        logResponse("OTP_015", response);
        assertErrorResponse(response, "OTP_015");
    }

    @Test(priority = 16, description = "OTP_016: Mobile with emoji")
    public void OTP_016_MobileWithEmoji() {
        System.out.println("\n>>> OTP_016: Mobile with emoji <<<");
        Response response = sendOtpRequest("9890\uD83D\uDE0A9898", VALID_COUNTRY_CODE);
        logResponse("OTP_016", response);
        assertErrorResponse(response, "OTP_016");
    }

    @Test(priority = 17, description = "OTP_017: Mobile with unicode digits (Arabic-Indic)")
    public void OTP_017_MobileWithUnicodeDigits() {
        System.out.println("\n>>> OTP_017: Mobile with unicode digits <<<");
        // Arabic-Indic digits ٩٨٩٠٩٠٩٨٩٨ — should be rejected or normalized
        Response response = sendOtpRequest("\u0669\u0668\u0669\u0660\u0669\u0660\u0669\u0668\u0669\u0668",
                VALID_COUNTRY_CODE);
        logResponse("OTP_017", response);
        // Acceptable: 400 reject OR 200 with success=false
        assertErrorResponse(response, "OTP_017");
    }

    @Test(priority = 18, description = "OTP_018: Mobile with leading spaces")
    public void OTP_018_MobileWithLeadingSpaces() {
        System.out.println("\n>>> OTP_018: Mobile with leading spaces <<<");
        Response response = sendOtpRequest("  9890909898", VALID_COUNTRY_CODE);
        logResponse("OTP_018", response);
        // Should either trim and succeed, or reject with error
        int status = response.getStatusCode();
        System.out.println("OTP_018: Status " + status + " — verifying trim-or-reject behavior");
        // Both behaviors are acceptable; just verify no server error
        Assert.assertNotEquals(status, 500, "OTP_018: Server must not return 500 for leading spaces");
    }

    @Test(priority = 19, description = "OTP_019: Mobile with trailing spaces")
    public void OTP_019_MobileWithTrailingSpaces() {
        System.out.println("\n>>> OTP_019: Mobile with trailing spaces <<<");
        Response response = sendOtpRequest("9890909898  ", VALID_COUNTRY_CODE);
        logResponse("OTP_019", response);
        int status = response.getStatusCode();
        System.out.println("OTP_019: Status " + status + " — verifying trim-or-reject behavior");
        Assert.assertNotEquals(status, 500, "OTP_019: Server must not return 500 for trailing spaces");
    }

    @Test(priority = 20, description = "OTP_020: Mobile with leading zeros")
    public void OTP_020_MobileWithLeadingZeros() {
        System.out.println("\n>>> OTP_020: Mobile with leading zeros <<<");
        // Leading zero could be stripped and form a different number \u2014 verify business rule
        Response response = sendOtpRequest("09890909898", VALID_COUNTRY_CODE);
        logResponse("OTP_020", response);
        int status = response.getStatusCode();
        System.out.println("OTP_020: Status " + status + " — verifying business rule for leading zeros");
        // No 500 is the minimum expectation
        Assert.assertNotEquals(status, 500, "OTP_020: Server must not return 500 for leading-zero mobile");
    }
}

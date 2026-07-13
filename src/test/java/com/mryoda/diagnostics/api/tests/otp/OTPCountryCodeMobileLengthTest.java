package com.mryoda.diagnostics.api.tests.otp;

import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Module 16 — Country Code vs Mobile Length Validation (OTP_146 – OTP_155)
 *
 * Validates how the API handles different country codes paired with mobile
 * numbers of varying lengths across India (+91), USA (+1), UK (+44), and
 * an unsupported CC (+999).
 *
 * IMPORTANT CONTEXT — API behaviour observed across Modules 1–15:
 *   Mobile validation rule : "Mobile number must be between 8 and 13 digits"
 *   Per-country length rule: NOT implemented (digit-count only, globally)
 *   Country code validation: NOT enforced at OTP dispatch stage (OTP_024/029)
 *
 *   Tests where per-country enforcement is expected but not implemented use
 *   soft assertions (assertFalse >= 500) and log a FINDING + SPEC GAP message.
 *   Hard assertMobileValidationError() is used only where the digit count is
 *   unambiguously outside the 8–13 global range.
 *
 * NOTE ON TC ID ALLOCATION:
 *   OTP_126–OTP_135 are already allocated to Module 15 (separators, digit
 *   edge cases, embedded CC). This module uses OTP_146–OTP_155, the next
 *   available range after Module 15 (OTP_121–OTP_145).
 *
 * Section A: India (+91) length scenarios  (OTP_146 – OTP_148)
 * Section B: USA (+1) scenarios            (OTP_149 – OTP_150)
 * Section C: UK (+44) scenarios            (OTP_151 – OTP_152)
 * Section D: Unsupported CC / mismatches   (OTP_153 – OTP_155)
 */
public class OTPCountryCodeMobileLengthTest extends OTPBaseTest {

    // =====================================================================
    // Section A — India (+91) Length Scenarios (OTP_146 – OTP_148)
    // =====================================================================

    @Test(priority = 146, description = "OTP_146: India (+91) with standard 10-digit mobile — expect success")
    public void OTP_146_IndiaValidTenDigits() {
        System.out.println("\n>>> OTP_146: India (+91) with valid 10-digit mobile <<<");
        // Standard Indian mobile: exactly 10 digits, within 8–13 range
        Response response = sendOtpRequest("9876543210", "+91");
        logResponse("OTP_146", response);
        assertSuccess(response, "OTP_146");
    }

    @Test(priority = 147, description = "OTP_147: India (+91) with 9-digit mobile — per-country expects error; API may accept (8–13 global rule)")
    public void OTP_147_IndiaWithNineDigits() {
        System.out.println("\n>>> OTP_147: India (+91) with 9-digit mobile <<<");
        // SPEC EXPECTATION  : Validation Error — Indian numbers must be exactly 10 digits.
        // API KNOWN BEHAVIOUR: Validates digit count 8–13 globally only.
        //                      9 digits is within 8–13 → API likely returns 200.
        // ASSERTION STRATEGY : Soft check (no 5xx) + documented FINDING for per-country gap.
        Response response = sendOtpRequest("987654321", "+91");
        logResponse("OTP_147", response);
        // BUG: API validates digit count globally (8-13) only. 9 digits passes but Indian numbers
        // must be exactly 10 digits. Per-country length enforcement is NOT implemented.
        // THIS TEST IS EXPECTED TO FAIL until the backend adds per-country length constraints.
        Assert.assertEquals(response.getStatusCode(), 400,
                "OTP_147 [BUG]: +91 with 9-digit mobile must return 400 (Indian numbers require exactly 10 digits). "
                        + "API accepted with 200 — per-country length validation is NOT enforced. "
                        + "Raise as defect: add per-country mobile length constraints.");
    }

    @Test(priority = 148, description = "OTP_148: India (+91) with 11-digit mobile — per-country expects error; API may accept (8–13 global rule)")
    public void OTP_148_IndiaWithElevenDigits() {
        System.out.println("\n>>> OTP_148: India (+91) with 11-digit mobile <<<");
        // SPEC EXPECTATION  : Validation Error — 11 digits is invalid for Indian numbers (must be 10).
        // API KNOWN BEHAVIOUR: 11 digits is within 8–13 range → API likely returns 200.
        // ASSERTION STRATEGY : Soft check (no 5xx) + documented FINDING.
        Response response = sendOtpRequest("98765432101", "+91");
        logResponse("OTP_148", response);
        // BUG: API validates digit count globally (8-13) only. 11 digits passes but Indian numbers
        // must be exactly 10 digits. Per-country length enforcement is NOT implemented.
        // THIS TEST IS EXPECTED TO FAIL until the backend adds per-country length constraints.
        Assert.assertEquals(response.getStatusCode(), 400,
                "OTP_148 [BUG]: +91 with 11-digit mobile must return 400 (Indian numbers require exactly 10 digits). "
                        + "API accepted with 200 — per-country length validation is NOT enforced. "
                        + "Raise as defect: add per-country mobile length constraints.");
    }

    // =====================================================================
    // Section B — USA (+1) Scenarios (OTP_149 – OTP_150)
    // =====================================================================

    @Test(priority = 149, description = "OTP_149: USA (+1) with valid 10-digit mobile — expect success if +1 CC is supported")
    public void OTP_149_UsaValidTenDigits() {
        System.out.println("\n>>> OTP_149: USA (+1) with valid 10-digit mobile <<<");
        // US NANP: 10-digit format, e.g. 2125551234.
        // Tests whether +1 country code is accepted by the API.
        // From OTP_024/029: CC validation is not enforced → likely returns 200.
        Response response = sendOtpRequest("2125551234", "+1");
        logResponse("OTP_149", response);
        // Spec: Success. 10-digit US number with +1 CC should be accepted.
        assertSuccess(response, "OTP_149");
    }

    @Test(priority = 150, description = "OTP_150: USA (+1) with 9-digit mobile — per-country expects error; API may accept")
    public void OTP_150_UsaWithNineDigits() {
        System.out.println("\n>>> OTP_150: USA (+1) with 9-digit mobile <<<");
        // SPEC EXPECTATION  : Validation Error — US NANP numbers must be exactly 10 digits.
        // API KNOWN BEHAVIOUR: 9 digits is within 8–13 range → likely accepted.
        // ASSERTION STRATEGY : Soft check (no 5xx) + documented FINDING.
        Response response = sendOtpRequest("212555123", "+1");
        logResponse("OTP_150", response);
        // BUG: API validates digit count globally (8-13) only. 9 digits passes but US NANP numbers
        // must be exactly 10 digits. Per-country length enforcement is NOT implemented.
        // THIS TEST IS EXPECTED TO FAIL until the backend adds per-country length constraints.
        Assert.assertEquals(response.getStatusCode(), 400,
                "OTP_150 [BUG]: +1 with 9-digit mobile must return 400 (US NANP requires exactly 10 digits). "
                        + "API accepted with 200 — per-country length validation is NOT enforced. "
                        + "Raise as defect: add per-country mobile length constraints.");
    }

    // =====================================================================
    // Section C — UK (+44) Scenarios (OTP_151 – OTP_152)
    // =====================================================================

    @Test(priority = 151, description = "OTP_151: UK (+44) with valid 10-digit mobile — expect success if +44 CC is supported")
    public void OTP_151_UkValidLength() {
        System.out.println("\n>>> OTP_151: UK (+44) with valid 10-digit mobile <<<");
        // UK mobile in E.164 format (without leading 0): 7911123456 (10 digits).
        // Tests whether +44 country code is accepted.
        Response response = sendOtpRequest("7911123456", "+44");
        logResponse("OTP_151", response);
        // Spec: Success. Valid 10-digit UK mobile with +44 CC should be accepted.
        assertSuccess(response, "OTP_151");
    }

    @Test(priority = 152, description = "OTP_152: UK (+44) with 5-digit short mobile — expect 400 (below 8-digit global minimum)")
    public void OTP_152_UkShortNumber() {
        System.out.println("\n>>> OTP_152: UK (+44) with 5-digit short mobile <<<");
        // 5 digits — unambiguously below the 8-digit global minimum regardless of CC.
        // This is a definitive validation failure: hard assertion on full error body.
        Response response = sendOtpRequest("12345", "+44");
        logResponse("OTP_152", response);
        assertMobileValidationError(response, "OTP_152");
    }

    // =====================================================================
    // Section D — Unsupported CC / Mismatches (OTP_153 – OTP_155)
    // =====================================================================

    @Test(priority = 153, description = "OTP_153: Unsupported country code (+999) with valid 10-digit mobile — expect error; known API gap")
    public void OTP_153_UnsupportedCountryCode() {
        System.out.println("\n>>> OTP_153: Unsupported country code (+999) with valid mobile <<<");
        // +999 is not a valid ITU-T country code.
        // SPEC EXPECTATION  : Validation Error.
        // KNOWN API BEHAVIOUR: API does not validate country codes at OTP stage
        //                      (documented findings from OTP_024 and OTP_029).
        //                      +999 likely returns 200.
        Response response = sendOtpRequest("9876543210", "+999");
        logResponse("OTP_153", response);
        // BUG: +999 is not a valid ITU-T country code. API must reject it with 400.
        // Same defect observed in OTP_024 (missing CC) and OTP_029 (unsupported CC).
        // CC validation is not enforced at OTP dispatch stage — KNOWN GAP across 3 modules.
        // THIS TEST IS EXPECTED TO FAIL until the backend adds country code whitelist validation.
        Assert.assertEquals(response.getStatusCode(), 400,
                "OTP_153 [BUG]: Unsupported country code +999 must return 400. "
                        + "API accepted with 200 — CC whitelist/format validation is NOT enforced. "
                        + "Same defect as OTP_024 and OTP_029. "
                        + "Raise as defect: add country code validation at OTP dispatch stage.");
    }

    @Test(priority = 154, description = "OTP_154: Valid country code (+91) with 5-digit mobile — expect 400 (below 8-digit minimum)")
    public void OTP_154_ValidCcInvalidMobileLength() {
        System.out.println("\n>>> OTP_154: Valid +91 with 5-digit mobile (below minimum) <<<");
        // Valid CC + mobile below 8-digit global minimum: unambiguous validation failure.
        // Verifies: CC does not override digit-count validation.
        Response response = sendOtpRequest("12345", "+91");
        logResponse("OTP_154", response);
        assertMobileValidationError(response, "OTP_154");
    }

    @Test(priority = 155, description = "OTP_155: CC/format mismatch — +91 (India) with US-format 10-digit number")
    public void OTP_155_CountryCodeFormatMismatch() {
        System.out.println("\n>>> OTP_155: CC format mismatch (+91 with US-format number) <<<");
        // +91 (India CC) paired with a US NANP number 2125551234 (10 digits).
        // Indian and US mobile numbers are both 10 digits, so global digit-count passes.
        // SPEC EXPECTATION  : Validation Error — number format does not match country code.
        // API KNOWN BEHAVIOUR: No CC-to-format coupling observed → likely returns 200.
        // PURPOSE: Confirms whether API enforces any coupling between CC and number format.
        Response response = sendOtpRequest("2125551234", "+91");
        logResponse("OTP_155", response);
        // BUG: +91 (India) paired with a US NANP number 2125551234. The number format does not
        // match the country code. API has no CC-to-number-format coupling — accepts any 10-digit
        // number for any CC regardless of regional format.
        // THIS TEST IS EXPECTED TO FAIL until the backend adds CC-to-format coupling validation.
        Assert.assertEquals(response.getStatusCode(), 400,
                "OTP_155 [BUG]: +91 with US-format number 2125551234 must return 400. "
                        + "API accepted with 200 — no coupling between country code and number format. "
                        + "Raise as defect if regional number format validation is a requirement.");
    }
}

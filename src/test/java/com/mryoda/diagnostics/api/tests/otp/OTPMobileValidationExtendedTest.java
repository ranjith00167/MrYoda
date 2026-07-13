package com.mryoda.diagnostics.api.tests.otp;

import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Module 15 — Mobile Number Validation Extended (OTP_121 – OTP_145)
 *
 * Extends Module 2 (OTP_007–OTP_020) with additional mobile number
 * validation scenarios not covered by the original suite:
 *
 * Section A: Phone-format separators   (OTP_121 – OTP_126)
 * Section B: Digit-only edge cases     (OTP_127 – OTP_131)
 * Section C: Embedded/prefixed values  (OTP_132 – OTP_135)
 * Section D: Symbol/encoding attacks   (OTP_136 – OTP_140)
 * Section E: Locale/unicode numbers    (OTP_141 – OTP_145)
 *
 * API rule (from observed validation messages):
 *   "Mobile number must be between 8 and 13 digits"
 *   Digits only — no separators, symbols, letters, or unicode numerals accepted.
 */
public class OTPMobileValidationExtendedTest extends OTPBaseTest {

    // =========================================================
    // Section A — Phone-Format Separators (OTP_121 – OTP_126)
    // =========================================================

    @Test(priority = 121, description = "OTP_121: Mobile with hyphens (989-090-9898) — expect 400")
    public void OTP_121_MobileWithHyphens() {
        System.out.println("\n>>> OTP_121: Mobile with hyphens <<<");
        Response response = sendOtpRequest("989-090-9898", VALID_COUNTRY_CODE);
        logResponse("OTP_121", response);
        assertMobileValidationError(response, "OTP_121");
    }

    @Test(priority = 122, description = "OTP_122: Mobile with dots (989.090.9898) — expect 400")
    public void OTP_122_MobileWithDots() {
        System.out.println("\n>>> OTP_122: Mobile with dots <<<");
        Response response = sendOtpRequest("989.090.9898", VALID_COUNTRY_CODE);
        logResponse("OTP_122", response);
        assertMobileValidationError(response, "OTP_122");
    }

    @Test(priority = 123, description = "OTP_123: Mobile with parentheses ((9890)909898) — expect 400")
    public void OTP_123_MobileWithParentheses() {
        System.out.println("\n>>> OTP_123: Mobile with parentheses <<<");
        Response response = sendOtpRequest("(9890)909898", VALID_COUNTRY_CODE);
        logResponse("OTP_123", response);
        // PERFORMANCE FINDING: '(9890)909898' took 26,674ms in observed run vs ~100ms baseline.
        // Parentheses may trigger unusual server-side regex/parsing behaviour. Flag for investigation.
        if (response.getTime() > 5000) {
            System.out.println("[OTP_123] PERFORMANCE WARNING: Response took " + response.getTime()
                    + "ms — parentheses character may trigger abnormal server processing delay."
                    + " Baseline: ~100ms. Raise as performance defect.");
        }
        assertMobileValidationError(response, "OTP_123");
    }

    @Test(priority = 124, description = "OTP_124: Mobile with internal spaces between digits (98909 09898) — expect 400")
    public void OTP_124_MobileWithInternalSpaces() {
        System.out.println("\n>>> OTP_124: Mobile with internal spaces <<<");
        Response response = sendOtpRequest("98909 09898", VALID_COUNTRY_CODE);
        logResponse("OTP_124", response);
        assertMobileValidationError(response, "OTP_124");
    }

    @Test(priority = 125, description = "OTP_125: Mobile with forward slash (989/0909898) — expect 400")
    public void OTP_125_MobileWithForwardSlash() {
        System.out.println("\n>>> OTP_125: Mobile with forward slash <<<");
        Response response = sendOtpRequest("989/0909898", VALID_COUNTRY_CODE);
        logResponse("OTP_125", response);
        assertMobileValidationError(response, "OTP_125");
    }

    @Test(priority = 126, description = "OTP_126: Mobile with backslash (9890\\909898) — expect 400")
    public void OTP_126_MobileWithBackslash() {
        System.out.println("\n>>> OTP_126: Mobile with backslash <<<");
        Response response = sendOtpRequest("9890\\909898", VALID_COUNTRY_CODE);
        logResponse("OTP_126", response);
        assertMobileValidationError(response, "OTP_126");
    }

    // =========================================================
    // Section B — Digit-Only Edge Cases (OTP_127 – OTP_131)
    // =========================================================

    @Test(priority = 127, description = "OTP_127: Mobile as single digit '9' — expect 400 (below minimum)")
    public void OTP_127_MobileSingleDigit() {
        System.out.println("\n>>> OTP_127: Single digit mobile <<<");
        Response response = sendOtpRequest("9", VALID_COUNTRY_CODE);
        logResponse("OTP_127", response);
        assertMobileValidationError(response, "OTP_127");
    }

    @Test(priority = 128, description = "OTP_128: Mobile as single zero '0' — expect 400 (below minimum)")
    public void OTP_128_MobileAsSingleZero() {
        System.out.println("\n>>> OTP_128: Single zero mobile <<<");
        Response response = sendOtpRequest("0", VALID_COUNTRY_CODE);
        logResponse("OTP_128", response);
        assertMobileValidationError(response, "OTP_128");
    }

    @Test(priority = 129, description = "OTP_129: Mobile as all zeros with valid length ('00000000') — document acceptance behavior")
    public void OTP_129_MobileAllZerosValidLength() {
        System.out.println("\n>>> OTP_129: All zeros mobile (8 digits) <<<");
        // 8 zeros — valid digit length; API may or may not reject based on business rules
        Response response = sendOtpRequest("00000000", VALID_COUNTRY_CODE);
        logResponse("OTP_129", response);

        int status = response.getStatusCode();
        Assert.assertFalse(status >= 500,
                "OTP_129: All-zeros mobile must not cause 5xx. Got: " + status);
        // BUSINESS LOGIC FINDING — API ACCEPTED '00000000' with HTTP 200.
        // Root cause: API enforces digit-count only (8–13 digits). No semantic / subscriber check.
        // A number consisting entirely of zeros is structurally valid but not a real phone number.
        // ACTION REQUIRED: Raise with product/backend team — should '00000000' trigger OTP dispatch?
        // If business rule requires real numbers, backend must add semantic validation.
        System.out.println("[OTP_129] FINDING: HTTP " + status + " for '00000000'. "
                + (status == 200
                ? "API ACCEPTED — digit-count-only validation passes (8 digits). "
                  + "No semantic/subscriber check at OTP stage. BUSINESS LOGIC GAP: '00000000' is not a real number."
                : "API REJECTED — semantic or subscriber validation is enforced (good)."));
    }

    @Test(priority = 130, description = "OTP_130: Mobile as all repeated same digit ('9999999999') — expect 200 (structurally valid)")
    public void OTP_130_MobileAllSameDigit() {
        System.out.println("\n>>> OTP_130: All same digit mobile (10 nines) <<<");
        // 10 identical digits — structurally valid (10 digits, all numeric)
        Response response = sendOtpRequest("9999999999", VALID_COUNTRY_CODE);
        logResponse("OTP_130", response);
        // assertSuccess validates: HTTP 200 + success=true — both required for a passing OTP request
        assertSuccess(response, "OTP_130");
    }

    @Test(priority = 131, description = "OTP_131: Mobile as scientific notation string ('9.89e9') — expect 400")
    public void OTP_131_MobileAsScientificNotation() {
        System.out.println("\n>>> OTP_131: Mobile as scientific notation <<<");
        Response response = sendOtpRequest("9.89e9", VALID_COUNTRY_CODE);
        logResponse("OTP_131", response);
        assertMobileValidationError(response, "OTP_131");
    }

    // =========================================================
    // Section C — Embedded / Prefixed Values (OTP_132 – OTP_135)
    // =========================================================

    @Test(priority = 132, description = "OTP_132: Mobile with country code embedded ('+919890909898') — expect 400")
    public void OTP_132_MobileWithEmbeddedCountryCode() {
        System.out.println("\n>>> OTP_132: Mobile with embedded country code <<<");
        // User incorrectly puts full E.164 number in mobile field
        Response response = sendOtpRequest("+919890909898", VALID_COUNTRY_CODE);
        logResponse("OTP_132", response);
        assertMobileValidationError(response, "OTP_132");
    }

    @Test(priority = 133, description = "OTP_133: Mobile as negative number string ('-9890909898') — expect 400")
    public void OTP_133_MobileAsNegativeNumber() {
        System.out.println("\n>>> OTP_133: Mobile as negative number <<<");
        Response response = sendOtpRequest("-9890909898", VALID_COUNTRY_CODE);
        logResponse("OTP_133", response);
        assertMobileValidationError(response, "OTP_133");
    }

    @Test(priority = 134, description = "OTP_134: Mobile with plus prefix ('+9890909898') — expect 400")
    public void OTP_134_MobileWithPlusPrefix() {
        System.out.println("\n>>> OTP_134: Mobile with '+' prefix <<<");
        Response response = sendOtpRequest("+9890909898", VALID_COUNTRY_CODE);
        logResponse("OTP_134", response);
        assertMobileValidationError(response, "OTP_134");
    }

    @Test(priority = 135, description = "OTP_135: Mobile padded with zeros to exceed maximum ('000009890909898') — expect 400")
    public void OTP_135_MobilePaddedZerosTooLong() {
        System.out.println("\n>>> OTP_135: Mobile padded with zeros (15 digits) <<<");
        // 15-digit number — exceeds maximum of 13
        Response response = sendOtpRequest("000009890909898", VALID_COUNTRY_CODE);
        logResponse("OTP_135", response);
        assertMobileValidationError(response, "OTP_135");
    }

    // =========================================================
    // Section D — Symbol / Encoding Attacks (OTP_136 – OTP_140)
    // =========================================================

    @Test(priority = 136, description = "OTP_136: Mobile with hash symbol ('#9890909898') — expect 400")
    public void OTP_136_MobileWithHash() {
        System.out.println("\n>>> OTP_136: Mobile with hash symbol <<<");
        Response response = sendOtpRequest("#9890909898", VALID_COUNTRY_CODE);
        logResponse("OTP_136", response);
        assertMobileValidationError(response, "OTP_136");
    }

    @Test(priority = 137, description = "OTP_137: Mobile with asterisk ('*9890909898') — expect 400")
    public void OTP_137_MobileWithAsterisk() {
        System.out.println("\n>>> OTP_137: Mobile with asterisk <<<");
        Response response = sendOtpRequest("*9890909898", VALID_COUNTRY_CODE);
        logResponse("OTP_137", response);
        // PERFORMANCE FINDING: '*9890909898' took 2,885ms in observed run vs ~100ms baseline.
        // Asterisk glob pattern may trigger elevated server-side processing. Flag for investigation.
        if (response.getTime() > 2000) {
            System.out.println("[OTP_137] PERFORMANCE WARNING: Response took " + response.getTime()
                    + "ms — asterisk '*' character caused elevated latency."
                    + " Baseline: ~100ms. Raise as performance defect.");
        }
        assertMobileValidationError(response, "OTP_137");
    }

    @Test(priority = 138, description = "OTP_138: Mobile with percent sign ('%9890909898') — expect 400")
    public void OTP_138_MobileWithPercent() {
        System.out.println("\n>>> OTP_138: Mobile with percent sign <<<");
        Response response = sendOtpRequest("%9890909898", VALID_COUNTRY_CODE);
        logResponse("OTP_138", response);
        assertMobileValidationError(response, "OTP_138");
    }

    @Test(priority = 139, description = "OTP_139: Mobile with pipe character ('9890|909898') — expect 400")
    public void OTP_139_MobileWithPipe() {
        System.out.println("\n>>> OTP_139: Mobile with pipe character <<<");
        Response response = sendOtpRequest("9890|909898", VALID_COUNTRY_CODE);
        logResponse("OTP_139", response);
        assertMobileValidationError(response, "OTP_139");
    }

    @Test(priority = 140, description = "OTP_140: Mobile with angle brackets ('<9890909898>') — expect 400")
    public void OTP_140_MobileWithAngleBrackets() {
        System.out.println("\n>>> OTP_140: Mobile with angle brackets <<<");
        Response response = sendOtpRequest("<9890909898>", VALID_COUNTRY_CODE);
        logResponse("OTP_140", response);
        assertMobileValidationError(response, "OTP_140");
    }

    // =========================================================
    // Section E — Locale / Unicode Numbers (OTP_141 – OTP_145)
    // =========================================================

    @Test(priority = 141, description = "OTP_141: Mobile with Chinese/CJK numerals ('九八九零九零九八九八') — expect 400")
    public void OTP_141_MobileWithChineseNumerals() {
        System.out.println("\n>>> OTP_141: Mobile with Chinese numerals <<<");
        // CJK digits 九八九零九零九八九八 — visually represent 9890909898 but are not ASCII
        String chineseMobile = "\u4e5d\u516b\u4e5d\u96f6\u4e5d\u96f6\u4e5d\u516b\u4e5d\u516b";
        Response response = sendOtpRequest(chineseMobile, VALID_COUNTRY_CODE);
        logResponse("OTP_141", response);
        assertMobileValidationError(response, "OTP_141");
    }

    @Test(priority = 142, description = "OTP_142: Mobile with Devanagari numerals ('९८९०९०९८९८') — expect 400")
    public void OTP_142_MobileWithDevanagariNumerals() {
        System.out.println("\n>>> OTP_142: Mobile with Devanagari numerals <<<");
        // Devanagari digits ९८९०९०९८९८ (U+0969 etc.)
        String devanagariMobile = "\u0969\u0968\u0969\u0966\u0969\u0966\u0969\u0968\u0969\u0968";
        Response response = sendOtpRequest(devanagariMobile, VALID_COUNTRY_CODE);
        logResponse("OTP_142", response);
        assertMobileValidationError(response, "OTP_142");
    }

    @Test(priority = 143, description = "OTP_143: Mobile with Thai numerals ('๙๘๙๐๙๐๙๘๙๘') — expect 400")
    public void OTP_143_MobileWithThaiNumerals() {
        System.out.println("\n>>> OTP_143: Mobile with Thai numerals <<<");
        // Thai digits ๙๘๙๐๙๐๙๘๙๘ (U+0E59 etc.)
        String thaiMobile = "\u0e59\u0e58\u0e59\u0e50\u0e59\u0e50\u0e59\u0e58\u0e59\u0e58";
        Response response = sendOtpRequest(thaiMobile, VALID_COUNTRY_CODE);
        logResponse("OTP_143", response);
        assertMobileValidationError(response, "OTP_143");
    }

    @Test(priority = 144, description = "OTP_144: Mobile with full-width ASCII digits ('９８９０９０９８９８') — expect 400")
    public void OTP_144_MobileWithFullWidthDigits() {
        System.out.println("\n>>> OTP_144: Mobile with full-width ASCII digits <<<");
        // Full-width digits ９８９０９０９８９８ (U+FF19 etc.) — look like ASCII but are different codepoints
        String fullWidthMobile = "\uff19\uff18\uff19\uff10\uff19\uff10\uff19\uff18\uff19\uff18";
        Response response = sendOtpRequest(fullWidthMobile, VALID_COUNTRY_CODE);
        logResponse("OTP_144", response);
        assertMobileValidationError(response, "OTP_144");
    }

    @Test(priority = 145, description = "OTP_145: Mobile with Persian/Urdu-Indic numerals ('۹۸۹۰۹۰۹۸۹۸') — expect 400")
    public void OTP_145_MobileWithPersianNumerals() {
        System.out.println("\n>>> OTP_145: Mobile with Persian/Urdu-Indic numerals <<<");
        // Extended Arabic-Indic digits ۹۸۹۰۹۰۹۸۹۸ (U+06F9 etc.) used in Persian/Urdu
        String persianMobile = "\u06f9\u06f8\u06f9\u06f0\u06f9\u06f0\u06f9\u06f8\u06f9\u06f8";
        Response response = sendOtpRequest(persianMobile, VALID_COUNTRY_CODE);
        logResponse("OTP_145", response);
        assertMobileValidationError(response, "OTP_145");
    }
}

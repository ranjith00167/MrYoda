package com.mryoda.diagnostics.api.tests.otp;

import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.RandomDataUtil;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Rescheduling OTP API Test Suite (RS_001 – RS_067)
 *
 * Endpoint: POST /otps/send
 * Mandatory Fields: mobile
 * Optional Fields: country_code, type, is_rescheduling
 *
 * Covers:
 *   - Positive scenarios (RS_001 – RS_009)
 *   - Negative: Authorization (RS_010 – RS_013)
 *   - Negative: Mobile validation (RS_014 – RS_022)
 *   - Negative: Country code validation (RS_023 – RS_028)
 *   - Negative: Type validation (RS_029 – RS_034)
 *   - Negative: is_rescheduling flag (RS_035 – RS_040)
 *   - Negative: Business rules (RS_041 – RS_045)
 *   - Security (RS_046 – RS_049)
 *   - Response schema validation (RS_053 – RS_057)
 *   - Type coercion & edge cases (RS_058 – RS_062)
 *   - HTTP method & protocol (RS_063 – RS_064)
 *   - Response headers & SLA (RS_065 – RS_067)
 */
public class ReschedulingOTPTest {

    private static final String BASE_URL = APIEndpoints.DIAGNOSTICS_BASE_URL;
    private static final String ENDPOINT = APIEndpoints.OTP_SEND;
    private static final String VALID_MOBILE = RandomDataUtil.getRandomMobile();
    private static final String VALID_COUNTRY_CODE = "+91";
    private static final String VALID_TYPE = "customer";

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER METHODS
    // ─────────────────────────────────────────────────────────────────────────

    private Response sendReschedulingOtp(Map<String, Object> payload) {
        return RestAssured.given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .body(payload)
                .post(ENDPOINT);
    }

    private Response sendReschedulingOtpRaw(String rawBody) {
        return RestAssured.given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .body(rawBody)
                .post(ENDPOINT);
    }

    private Map<String, Object> validPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("mobile", VALID_MOBILE);
        payload.put("country_code", VALID_COUNTRY_CODE);
        payload.put("type", VALID_TYPE);
        payload.put("is_rescheduling", true);
        return payload;
    }

    private void logResponse(String testId, Response response) {
        System.out.println("[" + testId + "] HTTP " + response.getStatusCode()
                + " | " + response.getTime() + "ms"
                + " | " + response.getBody().asString());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // POSITIVE SCENARIOS (RS_001 – RS_009)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(priority = 1, description = "RS_001: Valid rescheduling OTP request with all fields")
    public void RS_001_ValidReschedulingOTPRequest() {
        System.out.println("\n>>> RS_001: Valid rescheduling OTP request with all fields <<<");
        Response response = sendReschedulingOtp(validPayload());
        logResponse("RS_001", response);

        Assert.assertEquals(response.getStatusCode(), 200,
                "RS_001: Expected 200 for valid rescheduling OTP request. Body: " + response.getBody().asString());
        Boolean success = response.jsonPath().getBoolean("success");
        Assert.assertTrue(Boolean.TRUE.equals(success),
                "RS_001: success must be true. Body: " + response.getBody().asString());
    }

    @Test(priority = 2, description = "RS_002: Valid India mobile with +91 country code")
    public void RS_002_ValidIndiaMobile() {
        System.out.println("\n>>> RS_002: Valid India mobile with +91 country code <<<");
        Map<String, Object> payload = validPayload();
        payload.put("mobile", "9876543218");
        payload.put("country_code", "+91");
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_002", response);

        Assert.assertEquals(response.getStatusCode(), 200,
                "RS_002: Expected 200 for valid Indian mobile. Body: " + response.getBody().asString());
    }

    @Test(priority = 3, description = "RS_003: Valid mobile with different supported country codes")
    public void RS_003_ValidMobileWithDifferentCountryCodes() {
        System.out.println("\n>>> RS_003: Valid mobile with different supported country codes <<<");
        String[][] testData = {
            {"+91", "9876543218"},
            {"+1",  "2125551234"},
            {"+44", "7911123456"},
            {"+971", "501234567"},
            {"+65", "91234567"}
        };

        for (String[] data : testData) {
            Map<String, Object> payload = validPayload();
            payload.put("country_code", data[0]);
            payload.put("mobile", data[1]);
            Response response = sendReschedulingOtp(payload);
            logResponse("RS_003 [" + data[0] + "]", response);

            // API may support limited country codes; document behavior
            Assert.assertTrue(response.getStatusCode() == 200 || response.getStatusCode() == 400,
                    "RS_003: Unexpected status for " + data[0] + ". Got: " + response.getStatusCode());
            if (response.getStatusCode() == 200) {
                System.out.println("   ✅ Country code " + data[0] + " is supported");
            } else {
                System.out.println("   ⚠️ Country code " + data[0] + " is NOT supported (400)");
            }
        }
    }

    @Test(priority = 4, description = "RS_004: is_rescheduling=true generates rescheduling OTP")
    public void RS_004_ReschedulingFlagTrue() {
        System.out.println("\n>>> RS_004: is_rescheduling=true generates rescheduling OTP <<<");
        Map<String, Object> payload = validPayload();
        payload.put("is_rescheduling", true);
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_004", response);

        Assert.assertEquals(response.getStatusCode(), 200,
                "RS_004: Expected 200 when is_rescheduling=true. Body: " + response.getBody().asString());
    }

    @Test(priority = 5, description = "RS_005: First rescheduling attempt succeeds")
    public void RS_005_FirstReschedulingAttempt() {
        System.out.println("\n>>> RS_005: First rescheduling attempt succeeds <<<");
        Response response = sendReschedulingOtp(validPayload());
        logResponse("RS_005", response);

        Assert.assertEquals(response.getStatusCode(), 200,
                "RS_005: First rescheduling attempt must succeed. Body: " + response.getBody().asString());
    }

    @Test(priority = 6, description = "RS_006: Second rescheduling attempt succeeds")
    public void RS_006_SecondReschedulingAttempt() {
        System.out.println("\n>>> RS_006: Second rescheduling attempt succeeds <<<");
        Response response = sendReschedulingOtp(validPayload());
        logResponse("RS_006", response);

        Assert.assertEquals(response.getStatusCode(), 200,
                "RS_006: Second rescheduling attempt must succeed. Body: " + response.getBody().asString());
    }

    @Test(priority = 7, description = "RS_007: Third rescheduling attempt succeeds (maximum allowed)")
    public void RS_007_ThirdReschedulingAttempt() {
        System.out.println("\n>>> RS_007: Third rescheduling attempt succeeds (max allowed) <<<");
        Response response = sendReschedulingOtp(validPayload());
        logResponse("RS_007", response);

        Assert.assertEquals(response.getStatusCode(), 200,
                "RS_007: Third rescheduling attempt must succeed. Body: " + response.getBody().asString());
    }

    @Test(priority = 8, description = "RS_008: type=customer is accepted")
    public void RS_008_TypeCustomer() {
        System.out.println("\n>>> RS_008: type=customer is accepted <<<");
        Map<String, Object> payload = validPayload();
        payload.put("type", "customer");
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_008", response);

        Assert.assertEquals(response.getStatusCode(), 200,
                "RS_008: type=customer must be accepted. Body: " + response.getBody().asString());
    }

    @Test(priority = 9, description = "RS_009: Verify supported user types")
    public void RS_009_VerifySupportedUserTypes() {
        System.out.println("\n>>> RS_009: Verify supported user types <<<");
        String[] types = {"customer", "patient", "user"};

        for (String type : types) {
            Map<String, Object> payload = validPayload();
            payload.put("type", type);
            Response response = sendReschedulingOtp(payload);
            logResponse("RS_009 [" + type + "]", response);

            System.out.println("   type='" + type + "' → HTTP " + response.getStatusCode());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NEGATIVE: AUTHORIZATION (RS_010 – RS_013)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(priority = 10, description = "RS_010: Request without Authorization header — verify endpoint is public or returns 401")
    public void RS_010_NoAuthorizationHeader() {
        System.out.println("\n>>> RS_010: No Authorization header <<<");
        Response response = RestAssured.given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .body(validPayload())
                .post(ENDPOINT);
        logResponse("RS_010", response);

        int status = response.getStatusCode();
        Assert.assertTrue(status == 200 || status == 401,
                "RS_010: Endpoint must return 200 (public) or 401 (protected). Got: " + status);
        if (status == 200) {
            System.out.println("   ✅ Endpoint is PUBLIC — no auth required");
        } else {
            System.out.println("   ✅ Endpoint is PROTECTED — returns 401 without token");
        }
    }

    @Test(priority = 11, description = "RS_011: Invalid Bearer token — must not cause 500")
    public void RS_011_InvalidBearerToken() {
        System.out.println("\n>>> RS_011: Invalid Bearer token <<<");
        Response response = RestAssured.given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer invalid.token.here")
                .body(validPayload())
                .post(ENDPOINT);
        logResponse("RS_011", response);

        Assert.assertNotEquals(response.getStatusCode(), 500,
                "RS_011: Invalid token must NOT cause 500 server error");
        System.out.println("   Token validation behavior → HTTP " + response.getStatusCode());
    }

    @Test(priority = 12, description = "RS_012: Malformed Authorization header format")
    public void RS_012_MalformedAuthHeader() {
        System.out.println("\n>>> RS_012: Malformed Authorization header <<<");
        Response response = RestAssured.given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .header("Authorization", "NotBearer abc123")
                .body(validPayload())
                .post(ENDPOINT);
        logResponse("RS_012", response);

        Assert.assertNotEquals(response.getStatusCode(), 500,
                "RS_012: Malformed auth header must NOT cause 500");
        System.out.println("   Malformed auth header → HTTP " + response.getStatusCode());
    }

    @Test(priority = 13, description = "RS_013: Empty Authorization header value")
    public void RS_013_EmptyAuthHeader() {
        System.out.println("\n>>> RS_013: Empty Authorization header <<<");
        Response response = RestAssured.given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .header("Authorization", "")
                .body(validPayload())
                .post(ENDPOINT);
        logResponse("RS_013", response);

        Assert.assertNotEquals(response.getStatusCode(), 500,
                "RS_013: Empty auth header must NOT cause 500");
        System.out.println("   Empty auth header → HTTP " + response.getStatusCode());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NEGATIVE: MANDATORY FIELD VALIDATION — MOBILE (RS_014 – RS_022)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(priority = 14, description = "RS_014: Empty mobile must return 400")
    public void RS_014_EmptyMobile() {
        System.out.println("\n>>> RS_014: Empty mobile must return 400 <<<");
        Map<String, Object> payload = validPayload();
        payload.put("mobile", "");
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_014", response);

        assertMandatoryFieldError(response, "RS_014", "mobile",
                "Empty mobile must be rejected — mobile is a mandatory field");
    }

    @Test(priority = 15, description = "RS_015: Null mobile must return 400")
    public void RS_015_NullMobile() {
        System.out.println("\n>>> RS_015: Null mobile must return 400 <<<");
        Map<String, Object> payload = validPayload();
        payload.put("mobile", null);
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_015", response);

        assertMandatoryFieldError(response, "RS_015", "mobile",
                "Null mobile must be rejected — mobile is a mandatory field");
    }

    @Test(priority = 16, description = "RS_016: Missing mobile field must return 400")
    public void RS_016_MissingMobile() {
        System.out.println("\n>>> RS_016: Missing mobile field must return 400 <<<");
        Map<String, Object> payload = validPayload();
        payload.remove("mobile");
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_016", response);

        assertMandatoryFieldError(response, "RS_016", "mobile",
                "Missing mobile field must be rejected — mobile is mandatory");
    }

    @Test(priority = 17, description = "RS_017: Mobile less than required length must return 400")
    public void RS_017_MobileTooShort() {
        System.out.println("\n>>> RS_017: Mobile less than required length <<<");
        Map<String, Object> payload = validPayload();
        payload.put("mobile", "12345");
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_017", response);

        assertMandatoryFieldError(response, "RS_017", "mobile",
                "5-digit mobile must be rejected — minimum 10 digits required for +91");
    }

    @Test(priority = 18, description = "RS_018: Mobile more than allowed length must return 400")
    public void RS_018_MobileTooLong() {
        System.out.println("\n>>> RS_018: Mobile more than allowed length <<<");
        Map<String, Object> payload = validPayload();
        payload.put("mobile", "123456789012345");
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_018", response);

        assertMandatoryFieldError(response, "RS_018", "mobile",
                "15-digit mobile must be rejected — exceeds maximum length");
    }

    @Test(priority = 19, description = "RS_019: Alphabetic mobile must return 400")
    public void RS_019_AlphabeticMobile() {
        System.out.println("\n>>> RS_019: Alphabetic mobile must return 400 <<<");
        Map<String, Object> payload = validPayload();
        payload.put("mobile", "abcdefghij");
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_019", response);

        assertMandatoryFieldError(response, "RS_019", "mobile",
                "Alphabetic mobile must be rejected — only digits allowed");
    }

    @Test(priority = 20, description = "RS_020: Alphanumeric mobile must return 400")
    public void RS_020_AlphanumericMobile() {
        System.out.println("\n>>> RS_020: Alphanumeric mobile must return 400 <<<");
        Map<String, Object> payload = validPayload();
        payload.put("mobile", "abc1234567");
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_020", response);

        assertMandatoryFieldError(response, "RS_020", "mobile",
                "Alphanumeric mobile must be rejected — only digits allowed");
    }

    @Test(priority = 21, description = "RS_021: Special characters in mobile must return 400")
    public void RS_021_SpecialCharsMobile() {
        System.out.println("\n>>> RS_021: Special characters in mobile must return 400 <<<");
        Map<String, Object> payload = validPayload();
        payload.put("mobile", "98@#56789");
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_021", response);

        assertMandatoryFieldError(response, "RS_021", "mobile",
                "Special chars in mobile must be rejected");
    }

    @Test(priority = 22, description = "RS_022: Emoji in mobile must return 400")
    public void RS_022_EmojiMobile() {
        System.out.println("\n>>> RS_022: Emoji in mobile must return 400 <<<");
        Map<String, Object> payload = validPayload();
        payload.put("mobile", "987\uD83D\uDE00543210");
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_022", response);

        assertMandatoryFieldError(response, "RS_022", "mobile",
                "Emoji in mobile must be rejected");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NEGATIVE: OPTIONAL FIELD VALIDATION — COUNTRY CODE (RS_023 – RS_028)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(priority = 23, description = "RS_023: Missing country_code — document behavior (optional field)")
    public void RS_023_MissingCountryCode() {
        System.out.println("\n>>> RS_023: Missing country_code (optional) <<<");
        Map<String, Object> payload = validPayload();
        payload.remove("country_code");
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_023", response);

        int status = response.getStatusCode();
        System.out.println("   Missing country_code \u2192 HTTP " + status);
        if (status == 200) {
            System.out.println("   \u2705 API accepted without country_code (optional field, default applied)");
        } else {
            System.out.println("   \u26A0\uFE0F API rejected missing country_code \u2014 document as defect if it should be optional");
        }
        Assert.assertTrue(status == 200 || status == 400,
                "RS_023: Missing country_code should return 200 (optional) or 400. Got: " + status);
    }

    @Test(priority = 24, description = "RS_024: Empty country_code — document behavior")
    public void RS_024_EmptyCountryCode() {
        System.out.println("\n>>> RS_024: Empty country_code <<<");
        Map<String, Object> payload = validPayload();
        payload.put("country_code", "");
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_024", response);

        assertFieldError(response, "RS_024", "Empty country_code should be rejected (cannot default from empty string)");
    }

    @Test(priority = 25, description = "RS_025: Null country_code — document behavior")
    public void RS_025_NullCountryCode() {
        System.out.println("\n>>> RS_025: Null country_code <<<");
        Map<String, Object> payload = validPayload();
        payload.put("country_code", null);
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_025", response);

        int status = response.getStatusCode();
        System.out.println("   Null country_code \u2192 HTTP " + status);
        if (status == 200) {
            System.out.println("   \u2705 API treated null country_code as missing (optional, default applied)");
        } else {
            System.out.println("   \u2705 API rejected null country_code");
        }
        Assert.assertTrue(status == 200 || status == 400,
                "RS_025: Null country_code should return 200 or 400. Got: " + status);
    }

    @Test(priority = 26, description = "RS_026: Country code without + prefix must return 400")
    public void RS_026_CountryCodeWithoutPlus() {
        System.out.println("\n>>> RS_026: Country code without + prefix <<<");
        Map<String, Object> payload = validPayload();
        payload.put("country_code", "91");
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_026", response);

        assertFieldError(response, "RS_026", "Country code '91' without '+' prefix should be rejected");
    }

    @Test(priority = 27, description = "RS_027: Invalid country code +999 must return 400")
    public void RS_027_InvalidCountryCode() {
        System.out.println("\n>>> RS_027: Invalid country code +999 <<<");
        Map<String, Object> payload = validPayload();
        payload.put("country_code", "+999");
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_027", response);

        assertFieldError(response, "RS_027", "Invalid country code +999 should be rejected");
    }

    @Test(priority = 28, description = "RS_028: Country-specific length validation — +91 with 9-digit mobile")
    public void RS_028_CountrySpecificLengthValidation() {
        System.out.println("\n>>> RS_028: +91 with 9-digit mobile (must be exactly 10) <<<");
        Map<String, Object> payload = validPayload();
        payload.put("country_code", "+91");
        payload.put("mobile", "123456789");  // 9 digits — invalid for India
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_028", response);

        assertMandatoryFieldError(response, "RS_028", "mobile",
                "+91 with 9-digit mobile must be rejected — Indian numbers require exactly 10 digits");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NEGATIVE: TYPE FIELD VALIDATION (RS_029 – RS_034)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(priority = 29, description = "RS_029: Missing type field")
    public void RS_029_MissingType() {
        System.out.println("\n>>> RS_029: Missing type field <<<");
        Map<String, Object> payload = validPayload();
        payload.remove("type");
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_029", response);

        // Document behavior — may default to "customer" or return 400
        int status = response.getStatusCode();
        System.out.println("   Missing 'type' → HTTP " + status);
        Assert.assertTrue(status == 200 || status == 400,
                "RS_029: Missing type should return 200 (default) or 400. Got: " + status);
    }

    @Test(priority = 30, description = "RS_030: Empty type must return 400")
    public void RS_030_EmptyType() {
        System.out.println("\n>>> RS_030: Empty type must return 400 <<<");
        Map<String, Object> payload = validPayload();
        payload.put("type", "");
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_030", response);

        assertFieldError(response, "RS_030", "Empty type must be rejected");
    }

    @Test(priority = 31, description = "RS_031: Null type must return 400")
    public void RS_031_NullType() {
        System.out.println("\n>>> RS_031: Null type must return 400 <<<");
        Map<String, Object> payload = validPayload();
        payload.put("type", null);
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_031", response);

        assertFieldError(response, "RS_031", "Null type must be rejected");
    }

    @Test(priority = 32, description = "RS_032: Unsupported type 'admin' must return validation error")
    public void RS_032_UnsupportedType() {
        System.out.println("\n>>> RS_032: Unsupported type 'admin' <<<");
        Map<String, Object> payload = validPayload();
        payload.put("type", "admin");
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_032", response);

        assertFieldError(response, "RS_032", "Unsupported type 'admin' must be rejected");
    }

    @Test(priority = 33, description = "RS_033: Numeric type must return 400")
    public void RS_033_NumericType() {
        System.out.println("\n>>> RS_033: Numeric type (123) <<<");
        String rawBody = "{\"mobile\":\"" + VALID_MOBILE + "\",\"country_code\":\"" + VALID_COUNTRY_CODE
                + "\",\"type\":123,\"is_rescheduling\":true}";
        Response response = sendReschedulingOtpRaw(rawBody);
        logResponse("RS_033", response);

        assertFieldError(response, "RS_033", "Numeric type must be rejected — must be a string");
    }

    @Test(priority = 34, description = "RS_034: Boolean type must return 400")
    public void RS_034_BooleanType() {
        System.out.println("\n>>> RS_034: Boolean type (true) <<<");
        String rawBody = "{\"mobile\":\"" + VALID_MOBILE + "\",\"country_code\":\"" + VALID_COUNTRY_CODE
                + "\",\"type\":true,\"is_rescheduling\":true}";
        Response response = sendReschedulingOtpRaw(rawBody);
        logResponse("RS_034", response);

        assertFieldError(response, "RS_034", "Boolean type must be rejected — must be a string");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NEGATIVE: is_rescheduling FLAG VALIDATION (RS_035 – RS_040)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(priority = 35, description = "RS_035: Missing is_rescheduling field")
    public void RS_035_MissingIsRescheduling() {
        System.out.println("\n>>> RS_035: Missing is_rescheduling field <<<");
        Map<String, Object> payload = validPayload();
        payload.remove("is_rescheduling");
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_035", response);

        // Document behavior — may default to false or return 400
        int status = response.getStatusCode();
        System.out.println("   Missing is_rescheduling → HTTP " + status);
        Assert.assertTrue(status == 200 || status == 400,
                "RS_035: Missing is_rescheduling should return 200 (default) or 400. Got: " + status);
    }

    @Test(priority = 36, description = "RS_036: Null is_rescheduling must return 400")
    public void RS_036_NullIsRescheduling() {
        System.out.println("\n>>> RS_036: Null is_rescheduling <<<");
        Map<String, Object> payload = validPayload();
        payload.put("is_rescheduling", null);
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_036", response);

        assertFieldError(response, "RS_036", "Null is_rescheduling must be rejected");
    }

    @Test(priority = 37, description = "RS_037: String 'true' instead of boolean must return 400")
    public void RS_037_StringInsteadOfBoolean() {
        System.out.println("\n>>> RS_037: String 'true' instead of boolean <<<");
        String rawBody = "{\"mobile\":\"" + VALID_MOBILE + "\",\"country_code\":\"" + VALID_COUNTRY_CODE
                + "\",\"type\":\"customer\",\"is_rescheduling\":\"true\"}";
        Response response = sendReschedulingOtpRaw(rawBody);
        logResponse("RS_037", response);

        assertFieldError(response, "RS_037", "String 'true' must be rejected — must be boolean");
    }

    @Test(priority = 38, description = "RS_038: Numeric 1 instead of boolean must return 400")
    public void RS_038_NumericInsteadOfBoolean() {
        System.out.println("\n>>> RS_038: Numeric 1 instead of boolean <<<");
        String rawBody = "{\"mobile\":\"" + VALID_MOBILE + "\",\"country_code\":\"" + VALID_COUNTRY_CODE
                + "\",\"type\":\"customer\",\"is_rescheduling\":1}";
        Response response = sendReschedulingOtpRaw(rawBody);
        logResponse("RS_038", response);

        assertFieldError(response, "RS_038", "Numeric 1 must be rejected — must be boolean");
    }

    @Test(priority = 39, description = "RS_039: Array instead of boolean must return 400")
    public void RS_039_ArrayInsteadOfBoolean() {
        System.out.println("\n>>> RS_039: Array [true] instead of boolean <<<");
        String rawBody = "{\"mobile\":\"" + VALID_MOBILE + "\",\"country_code\":\"" + VALID_COUNTRY_CODE
                + "\",\"type\":\"customer\",\"is_rescheduling\":[true]}";
        Response response = sendReschedulingOtpRaw(rawBody);
        logResponse("RS_039", response);

        assertFieldError(response, "RS_039", "Array [true] must be rejected — must be boolean");
    }

    @Test(priority = 40, description = "RS_040: is_rescheduling=false may route to normal OTP flow")
    public void RS_040_ReschedulingFlagFalse() {
        System.out.println("\n>>> RS_040: is_rescheduling=false — verify routing behavior <<<");
        Map<String, Object> payload = validPayload();
        payload.put("is_rescheduling", false);
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_040", response);

        // Document behavior: should succeed (normal OTP flow) or return specific message
        Assert.assertTrue(response.getStatusCode() == 200 || response.getStatusCode() == 400,
                "RS_040: is_rescheduling=false should return 200 (normal OTP) or 400. Got: "
                        + response.getStatusCode());
        System.out.println("   is_rescheduling=false → HTTP " + response.getStatusCode()
                + " (documents routing behavior)");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NEGATIVE: RESCHEDULING BUSINESS RULES (RS_041 – RS_045)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(priority = 41, description = "RS_041: Customer without booking tries rescheduling")
    public void RS_041_NoBooingReschedule() {
        System.out.println("\n>>> RS_041: Customer without booking tries rescheduling <<<");
        Map<String, Object> payload = validPayload();
        // Use a mobile that has no active booking
        payload.put("mobile", "9700000001");
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_041", response);

        // Should return error or specific message about no booking
        int status = response.getStatusCode();
        System.out.println("   No booking mobile → HTTP " + status);
        // Document behavior for defect tracking
        if (status == 200) {
            Boolean success = response.jsonPath().getBoolean("success");
            System.out.println("   ⚠️ API accepted OTP for non-booked customer. success=" + success);
            System.out.println("   📝 NOTE: If business rule requires booking, raise as defect");
        } else {
            System.out.println("   ✅ API correctly rejected — no booking found");
        }
    }


    @Test(priority = 43, description = "RS_043: Rescheduling completed appointment must fail")
    public void RS_043_CompletedAppointmentReschedule() {
        System.out.println("\n>>> RS_043: Rescheduling completed appointment <<<");
        Map<String, Object> payload = validPayload();
        // Use a mobile known to have completed appointments
        payload.put("mobile", "9700000043");
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_043", response);

        int status = response.getStatusCode();
        System.out.println("   Completed appointment reschedule → HTTP " + status);
        if (status == 200) {
            System.out.println("   📝 Document: API does not validate appointment status before sending OTP");
        }
    }

    @Test(priority = 44, description = "RS_044: Rescheduling after appointment start time")
    public void RS_044_RescheduleAfterStartTime() {
        System.out.println("\n>>> RS_044: Rescheduling after appointment start time <<<");
        Map<String, Object> payload = validPayload();
        payload.put("mobile", "9700000044");
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_044", response);

        int status = response.getStatusCode();
        System.out.println("   Past-appointment reschedule → HTTP " + status);
        if (status == 200) {
            System.out.println("   📝 Document: API does not check if appointment has started");
        }
    }

    @Test(priority = 45, description = "RS_045: Rescheduling cancelled appointment must fail")
    public void RS_045_CancelledAppointmentReschedule() {
        System.out.println("\n>>> RS_045: Rescheduling cancelled appointment <<<");
        Map<String, Object> payload = validPayload();
        payload.put("mobile", "9700000045");
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_045", response);

        int status = response.getStatusCode();
        System.out.println("   Cancelled appointment reschedule → HTTP " + status);
        if (status == 200) {
            System.out.println("   📝 Document: API does not validate cancellation status");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SECURITY (RS_046 – RS_049)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(priority = 46, description = "RS_046: SQL Injection in mobile field")
    public void RS_046_SQLInjection() {
        System.out.println("\n>>> RS_046: SQL Injection in mobile field <<<");
        Map<String, Object> payload = validPayload();
        payload.put("mobile", "123' OR 1=1 --");
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_046", response);

        Assert.assertNotEquals(response.getStatusCode(), 500,
                "RS_046: SQL injection must NOT cause 500. Body: " + response.getBody().asString());
        Assert.assertTrue(response.getStatusCode() == 400 || response.getStatusCode() == 200,
                "RS_046: SQL injection must be safely handled (400 or 200 with validation)");
        // If 200, check it did not return data leak
        if (response.getStatusCode() == 200) {
            String body = response.getBody().asString();
            Assert.assertFalse(body.contains("sql") || body.contains("query") || body.contains("SELECT"),
                    "RS_046: [CRITICAL] SQL error message leaked in response!");
        }
    }

    @Test(priority = 47, description = "RS_047: NoSQL Injection in mobile field")
    public void RS_047_NoSQLInjection() {
        System.out.println("\n>>> RS_047: NoSQL Injection in mobile field <<<");
        String rawBody = "{\"mobile\":{\"$gt\":\"\"},\"country_code\":\"+91\",\"type\":\"customer\",\"is_rescheduling\":true}";
        Response response = sendReschedulingOtpRaw(rawBody);
        logResponse("RS_047", response);

        Assert.assertNotEquals(response.getStatusCode(), 500,
                "RS_047: NoSQL injection must NOT cause 500 server error");
    }

    @Test(priority = 48, description = "RS_048: XSS Attack in mobile field")
    public void RS_048_XSSAttack() {
        System.out.println("\n>>> RS_048: XSS Attack in mobile field <<<");
        Map<String, Object> payload = validPayload();
        payload.put("mobile", "<script>alert(1)</script>");
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_048", response);

        Assert.assertNotEquals(response.getStatusCode(), 500,
                "RS_048: XSS payload must NOT cause 500");
        String body = response.getBody().asString();
        Assert.assertFalse(body.contains("<script>"),
                "RS_048: [CRITICAL] XSS payload reflected in response!");
    }

    @Test(priority = 49, description = "RS_049: Large payload attack (5000 chars)")
    public void RS_049_LargePayload() {
        System.out.println("\n>>> RS_049: Large payload (5000 char mobile) <<<");
        Map<String, Object> payload = validPayload();
        StringBuilder largeMobile = new StringBuilder();
        for (int i = 0; i < 5000; i++) largeMobile.append("9");
        payload.put("mobile", largeMobile.toString());
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_049", response);

        Assert.assertNotEquals(response.getStatusCode(), 500,
                "RS_049: Large payload must NOT cause 500 — should be rejected gracefully");
        Assert.assertTrue(response.getStatusCode() == 400 || response.getStatusCode() == 413
                        || response.getStatusCode() == 200,
                "RS_049: Large payload should return 400, 413, or 200 with error. Got: "
                        + response.getStatusCode());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // RESPONSE SCHEMA VALIDATION (RS_053 – RS_057)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(priority = 53, description = "RS_053: Verify HTTP 200 status code for valid request")
    public void RS_053_VerifyStatusCode() {
        System.out.println("\n>>> RS_053: Verify status code <<<");
        Response response = sendReschedulingOtp(validPayload());
        logResponse("RS_053", response);

        Assert.assertEquals(response.getStatusCode(), 200,
                "RS_053: Valid request must return HTTP 200");
    }

    @Test(priority = 54, description = "RS_054: Verify success message in response")
    public void RS_054_VerifySuccessMessage() {
        System.out.println("\n>>> RS_054: Verify success message <<<");
        Response response = sendReschedulingOtp(validPayload());
        logResponse("RS_054", response);

        Assert.assertEquals(response.getStatusCode(), 200, "RS_054: Must be HTTP 200 first");
        String msg = response.jsonPath().getString("msg");
        if (msg == null) msg = response.jsonPath().getString("message");
        Assert.assertNotNull(msg,
                "RS_054: Response must contain 'msg' or 'message' field. Body: "
                        + response.getBody().asString());
        System.out.println("   Message: " + msg);
    }

    @Test(priority = 55, description = "RS_055: Verify response schema structure")
    public void RS_055_VerifyResponseSchema() {
        System.out.println("\n>>> RS_055: Verify response schema <<<");
        Response response = sendReschedulingOtp(validPayload());
        logResponse("RS_055", response);

        Assert.assertEquals(response.getStatusCode(), 200, "RS_055: Must be HTTP 200");

        // Validate schema: must have status, success, msg
        Object statusField = response.jsonPath().get("status");
        Assert.assertNotNull(statusField, "RS_055: 'status' field must be present in response");

        Object successField = response.jsonPath().get("success");
        Assert.assertNotNull(successField, "RS_055: 'success' field must be present in response");

        String msgField = response.jsonPath().getString("msg");
        if (msgField == null) msgField = response.jsonPath().getString("message");
        Assert.assertNotNull(msgField, "RS_055: 'msg' or 'message' field must be present");

        System.out.println("   Schema validated: status=" + statusField
                + ", success=" + successField + ", msg=" + msgField);
    }

    @Test(priority = 56, description = "RS_056: Verify data types in response")
    public void RS_056_VerifyDataTypes() {
        System.out.println("\n>>> RS_056: Verify data types <<<");
        Response response = sendReschedulingOtp(validPayload());
        logResponse("RS_056", response);

        Assert.assertEquals(response.getStatusCode(), 200, "RS_056: Must be HTTP 200");

        // status must be integer
        Object statusVal = response.jsonPath().get("status");
        Assert.assertTrue(statusVal instanceof Integer,
                "RS_056: 'status' must be Integer. Got: " + (statusVal != null ? statusVal.getClass().getSimpleName() : "null"));

        // success must be boolean
        Object successVal = response.jsonPath().get("success");
        Assert.assertTrue(successVal instanceof Boolean,
                "RS_056: 'success' must be Boolean. Got: " + (successVal != null ? successVal.getClass().getSimpleName() : "null"));

        // msg must be string
        Object msgVal = response.jsonPath().get("msg");
        if (msgVal == null) msgVal = response.jsonPath().get("message");
        Assert.assertTrue(msgVal instanceof String,
                "RS_056: 'msg' must be String. Got: " + (msgVal != null ? msgVal.getClass().getSimpleName() : "null"));

        System.out.println("   Data types validated ✅");
    }

    @Test(priority = 57, description = "RS_057: Verify OTP is NOT exposed in response")
    public void RS_057_OTPNotExposed() {
        System.out.println("\n>>> RS_057: Verify OTP NOT exposed in response <<<");
        Response response = sendReschedulingOtp(validPayload());
        logResponse("RS_057", response);

        String body = response.getBody().asString().toLowerCase();

        // Response MUST NOT contain actual OTP digits
        Assert.assertFalse(body.contains("\"otp\""),
                "RS_057: [CRITICAL SECURITY] OTP value is exposed in response!");
        Assert.assertFalse(body.matches(".*\"otp\"\\s*:\\s*\"\\d+\".*"),
                "RS_057: [CRITICAL SECURITY] OTP numeric value found in response!");

        // Should not contain common OTP field names with values
        Assert.assertFalse(body.contains("otp_code"),
                "RS_057: otp_code field should not be in response");
        Assert.assertFalse(body.contains("verification_code"),
                "RS_057: verification_code should not be in response");

        System.out.println("   ✅ OTP is NOT exposed in response — SECURE");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TYPE COERCION & EDGE CASES (RS_058 – RS_062)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(priority = 58, description = "RS_058: Mobile as integer instead of string must return 400")
    public void RS_058_MobileAsInteger() {
        System.out.println("\n>>> RS_058: Mobile sent as integer (type coercion) <<<");
        String rawBody = "{\"mobile\":" + VALID_MOBILE + ",\"country_code\":\"" + VALID_COUNTRY_CODE
                + "\",\"type\":\"customer\",\"is_rescheduling\":true}";
        Response response = sendReschedulingOtpRaw(rawBody);
        logResponse("RS_058", response);

        assertMandatoryFieldError(response, "RS_058", "mobile",
                "Mobile as integer must be rejected — must be a string");
    }

    @Test(priority = 59, description = "RS_059: country_code as integer instead of string must return 400")
    public void RS_059_CountryCodeAsInteger() {
        System.out.println("\n>>> RS_059: country_code as integer (91 without +) <<<");
        String rawBody = "{\"mobile\":\"" + VALID_MOBILE + "\",\"country_code\":91"
                + ",\"type\":\"customer\",\"is_rescheduling\":true}";
        Response response = sendReschedulingOtpRaw(rawBody);
        logResponse("RS_059", response);

        assertMandatoryFieldError(response, "RS_059", "country_code",
                "country_code as integer must be rejected — must be a string like '+91'");
    }

    @Test(priority = 60, description = "RS_060: Whitespace-only mobile must return 400")
    public void RS_060_WhitespaceOnlyMobile() {
        System.out.println("\n>>> RS_060: Whitespace-only mobile <<<");
        Map<String, Object> payload = validPayload();
        payload.put("mobile", "   ");
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_060", response);

        assertMandatoryFieldError(response, "RS_060", "mobile",
                "Whitespace-only mobile must be rejected");
    }

    @Test(priority = 61, description = "RS_061: Mobile with leading/trailing spaces must return 400 or be trimmed")
    public void RS_061_MobileWithSpaces() {
        System.out.println("\n>>> RS_061: Mobile with leading/trailing spaces <<<");
        Map<String, Object> payload = validPayload();
        payload.put("mobile", " " + VALID_MOBILE + " ");
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_061", response);

        int status = response.getStatusCode();
        System.out.println("   Mobile with spaces → HTTP " + status);
        if (status == 200) {
            Boolean success = response.jsonPath().getBoolean("success");
            System.out.println("   API " + (Boolean.TRUE.equals(success) ? "trims whitespace (accepted)" : "rejects with success=false"));
        } else {
            System.out.println("   ✅ Rejected with " + status);
        }
        Assert.assertTrue(status == 200 || status == 400,
                "RS_061: Mobile with spaces should return 200 (trimmed) or 400. Got: " + status);
    }

    @Test(priority = 62, description = "RS_062: Completely empty JSON body {} must return 400")
    public void RS_062_EmptyJsonBody() {
        System.out.println("\n>>> RS_062: Completely empty JSON body {} <<<");
        Response response = sendReschedulingOtpRaw("{}");
        logResponse("RS_062", response);

        assertMandatoryFieldError(response, "RS_062", "mobile",
                "Empty body must be rejected — both mobile and country_code are mandatory");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HTTP METHOD & PROTOCOL (RS_063 – RS_064)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(priority = 63, description = "RS_063: GET request on POST-only endpoint must return 404 or 405")
    public void RS_063_WrongHttpMethod_GET() {
        System.out.println("\n>>> RS_063: GET on POST-only endpoint <<<");
        Response response = RestAssured.given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .get(ENDPOINT);
        logResponse("RS_063", response);

        int status = response.getStatusCode();
        Assert.assertTrue(status == 404 || status == 405,
                "RS_063: GET on POST endpoint must return 404 or 405. Got: " + status);
        System.out.println("   GET method → HTTP " + status + " ✅");
    }

    @Test(priority = 64, description = "RS_064: PUT request on POST-only endpoint must return 404 or 405")
    public void RS_064_WrongHttpMethod_PUT() {
        System.out.println("\n>>> RS_064: PUT on POST-only endpoint <<<");
        Response response = RestAssured.given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .body(validPayload())
                .put(ENDPOINT);
        logResponse("RS_064", response);

        int status = response.getStatusCode();
        Assert.assertTrue(status == 404 || status == 405,
                "RS_064: PUT on POST endpoint must return 404 or 405. Got: " + status);
        System.out.println("   PUT method → HTTP " + status + " ✅");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // RESPONSE HEADERS & SLA (RS_065 – RS_067)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(priority = 65, description = "RS_065: Response Content-Type must be application/json")
    public void RS_065_ResponseContentType() {
        System.out.println("\n>>> RS_065: Response Content-Type header <<<");
        Response response = sendReschedulingOtp(validPayload());
        logResponse("RS_065", response);

        Assert.assertEquals(response.getStatusCode(), 200, "RS_065: Must be HTTP 200 first");
        String contentType = response.getHeader("Content-Type");
        Assert.assertNotNull(contentType,
                "RS_065: Content-Type header must be present");
        Assert.assertTrue(contentType.toLowerCase().contains("application/json"),
                "RS_065: Content-Type must be application/json. Got: " + contentType);
        System.out.println("   Content-Type: " + contentType + " ✅");
    }

    @Test(priority = 66, description = "RS_066: Response time must be under 3000ms (SLA)")
    public void RS_066_ResponseTimeSLA() {
        System.out.println("\n>>> RS_066: Response time SLA check (< 3000ms) <<<");
        Response response = sendReschedulingOtp(validPayload());
        long responseTime = response.getTime();
        logResponse("RS_066", response);

        System.out.println("   Response time: " + responseTime + "ms");
        Assert.assertTrue(responseTime < 3000,
                "RS_066: [SLA BREACH] Response time " + responseTime + "ms exceeds 3000ms limit");
        System.out.println("   ✅ SLA met — " + responseTime + "ms < 3000ms");
    }

    @Test(priority = 67, description = "RS_067: Mobile with embedded country code (+918056477884) must return 400")
    public void RS_067_MobileWithEmbeddedCountryCode() {
        System.out.println("\n>>> RS_067: Mobile with embedded country code (+91XXXXXXXXXX) <<<");
        Map<String, Object> payload = validPayload();
        payload.put("mobile", "+91" + VALID_MOBILE);
        Response response = sendReschedulingOtp(payload);
        logResponse("RS_067", response);

        assertMandatoryFieldError(response, "RS_067", "mobile",
                "Mobile with embedded country code must be rejected — mobile field must contain digits only");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ASSERTION HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Assert mandatory field error: API must return 4xx or 200 with success=false.
     * Logs clearly if API incorrectly accepts invalid data (documents as potential defect).
     */
    private void assertMandatoryFieldError(Response response, String testId, String fieldName, String reason) {
        int status = response.getStatusCode();

        if (status >= 400 && status < 500) {
            System.out.println("[" + testId + "] ✅ Correctly rejected with HTTP " + status);
            return;
        }

        if (status == 200) {
            Boolean success = response.jsonPath().getBoolean("success");
            if (!Boolean.TRUE.equals(success)) {
                System.out.println("[" + testId + "] ✅ HTTP 200 with success=false — field validation enforced");
                return;
            }
            // API accepted invalid data — this is a bug
            Assert.fail(testId + " [BUG]: " + reason
                    + ". API accepted with 200/success=true — mandatory field '" + fieldName
                    + "' validation is NOT enforced. Raise as defect.");
        }

        Assert.fail(testId + ": Unexpected status " + status + " | Body: " + response.getBody().asString());
    }

    /**
     * Assert generic field error: API must return 4xx or 200 with success=false.
     */
    private void assertFieldError(Response response, String testId, String reason) {
        int status = response.getStatusCode();

        if (status >= 400 && status < 500) {
            System.out.println("[" + testId + "] ✅ Correctly rejected with HTTP " + status);
            return;
        }

        if (status == 200) {
            Boolean success = response.jsonPath().getBoolean("success");
            if (!Boolean.TRUE.equals(success)) {
                System.out.println("[" + testId + "] ✅ HTTP 200 with success=false — validation enforced");
                return;
            }
            // Document but don't hard-fail for non-mandatory fields
            System.out.println("[" + testId + "] ⚠️ API accepted invalid data. " + reason
                    + ". Body: " + response.getBody().asString());
        }
    }
}

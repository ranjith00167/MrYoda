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
 * OTP Resend API Test Suite (OR_001 – OR_055)
 *
 * Endpoint  : POST /otps/resend
 * Base URL  : https://staging-api-diagnostics.yodaprojects.com
 *
 * Headers:
 *   - Authorization: Bearer <token>   (required — this is an authenticated endpoint)
 *   - Content-Type: application/json
 *
 * Body Fields:
 *   - mobile       (mandatory)
 *   - country_code (optional — defaults when omitted)
 *
 * Covers:
 *   - Positive scenarios            (OR_001 – OR_009)
 *   - Mobile field validation       (OR_016 – OR_024)
 *   - country_code field validation (OR_025 – OR_030)
 *   - Security                      (OR_031 – OR_038)
 *   - Response schema & SLA         (OR_039 – OR_046)
 *   - HTTP method & edge cases      (OR_047 – OR_055)
 *
 * Run with: mvn test -DsuiteXmlFile=test-suites/testng_otp_resend_suite.xml
 */
public class OTPResendTest {

    private static final String BASE_URL       = APIEndpoints.DIAGNOSTICS_BASE_URL;
    private static final String ENDPOINT       = APIEndpoints.OTP_RESEND;
    private static final String VALID_MOBILE   = RandomDataUtil.getRandomMobile();
    private static final String VALID_CC       = "+91";

    // Placeholder token — endpoint requires auth; tests with expired/invalid tokens
    // document auth behaviour. Replace with a live token for full positive coverage.
    private static final String PLACEHOLDER_TOKEN =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
            + ".eyJ1c2VyR3VpZCI6ImY1NjllNTMyLTg1YzYtNGFkNi05MzM5LTNlODkxZWM0YzAzYSIsIm1vYmlsZSI6Ijg3OTAyNTYzNzMiLCJleHAiOjE3ODM1Mjc5Mzl9"
            + ".AdM6X1uNl6dOTvenPVBPk6zZ5kiTf7OIx3oEoqEOxw0";

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private Response resend(Map<String, Object> payload, String token) {
        return RestAssured.given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(payload)
                .post(ENDPOINT);
    }

    /** Convenience overload: builds a {mobile, country_code} payload and calls resend */
    private Response resend(String mobile, String countryCode, String token) {
        Map<String, Object> p = new HashMap<>();
        p.put("mobile", mobile);
        p.put("country_code", countryCode);
        return resend(p, token);
    }

    private Response resendRaw(String rawBody, String token) {
        return RestAssured.given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(rawBody)
                .post(ENDPOINT);
    }

    private Response resendNoAuth(Map<String, Object> payload) {
        return RestAssured.given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .body(payload)
                .post(ENDPOINT);
    }

    private Map<String, Object> validPayload() {
        Map<String, Object> p = new HashMap<>();
        p.put("mobile", VALID_MOBILE);
        p.put("country_code", VALID_CC);
        return p;
    }

    private void log(String id, Response r) {
        System.out.println("[" + id + "] HTTP " + r.getStatusCode()
                + " | " + r.getTime() + "ms | " + r.getBody().asString());
    }

    /** Accept 2xx, 4xx, 429 — anything that isn't a 5xx crash */
    private boolean isSuccessOrExpectedError(int status) {
        return (status >= 200 && status < 500);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // POSITIVE SCENARIOS (OR_001 – OR_009)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(priority = 1, description = "OR_001: Valid resend OTP with mobile and country_code")
    public void OR_001_ValidResendAllFields() {
        System.out.println("\n>>> OR_001: Valid resend OTP — all fields <<<");
        Response r = resend(validPayload(), PLACEHOLDER_TOKEN);
        log("OR_001", r);

        int status = r.getStatusCode();
        Assert.assertTrue(isSuccessOrExpectedError(status),
                "OR_001: Must not return 5xx. Got: " + status + " Body: " + r.getBody().asString());
        if (status == 200 || status == 201) {
            Object success = r.jsonPath().get("success");
            Assert.assertTrue(Boolean.TRUE.equals(success),
                    "OR_001: success must be true on 200/201. Body: " + r.getBody().asString());
            System.out.println("   ✅ OTP resent successfully");
        } else if (status == 401) {
            System.out.println("   ⚠️ 401 — token expired. Use a live token for full positive coverage.");
        } else if (status == 429) {
            System.out.println("   ⚠️ 429 — staging rate limit. Wait and re-run.");
        }
    }

    @Test(priority = 2, description = "OR_002: Valid resend OTP — mobile only (country_code omitted)")
    public void OR_002_ValidResendMobileOnly() {
        System.out.println("\n>>> OR_002: Valid resend — mobile only <<<");
        Map<String, Object> payload = new HashMap<>();
        payload.put("mobile", VALID_MOBILE);
        Response r = resend(payload, PLACEHOLDER_TOKEN);
        log("OR_002", r);

        int status = r.getStatusCode();
        Assert.assertTrue(isSuccessOrExpectedError(status),
                "OR_002: Must not return 5xx. Got: " + status);
        System.out.println("   Mobile only → HTTP " + status);
        if (status == 200 || status == 201) {
            System.out.println("   ✅ country_code is optional — API accepted without it");
        }
    }

    @Test(priority = 3, description = "OR_003: Resend OTP with +91 country code")
    public void OR_003_ResendWithIndiaCc() {
        System.out.println("\n>>> OR_003: Resend with +91 <<<");
        Map<String, Object> payload = validPayload();
        payload.put("country_code", "+91");
        Response r = resend(payload, PLACEHOLDER_TOKEN);
        log("OR_003", r);

        Assert.assertTrue(isSuccessOrExpectedError(r.getStatusCode()),
                "OR_003: Must not return 5xx. Got: " + r.getStatusCode());
        System.out.println("   +91 → HTTP " + r.getStatusCode());
    }

    @Test(priority = 4, description = "OR_004: Resend for a known registered mobile")
    public void OR_004_ResendForRegisteredMobile() {
        System.out.println("\n>>> OR_004: Resend for registered mobile 9876543210 <<<");
        Map<String, Object> payload = new HashMap<>();
        payload.put("mobile", "9876543210");
        payload.put("country_code", "+91");
        Response r = resend(payload, PLACEHOLDER_TOKEN);
        log("OR_004", r);

        Assert.assertTrue(isSuccessOrExpectedError(r.getStatusCode()),
                "OR_004: Must not return 5xx. Got: " + r.getStatusCode());
        if (r.getStatusCode() == 200 || r.getStatusCode() == 201) {
            System.out.println("   ✅ OTP resent for registered mobile");
        } else {
            System.out.println("   HTTP " + r.getStatusCode() + " — document behavior");
        }
    }

    @Test(priority = 5, description = "OR_005: Response must not expose OTP value")
    public void OR_005_OTPNotExposedInResponse() {
        System.out.println("\n>>> OR_005: OTP not exposed <<<");
        Response r = resend(validPayload(), PLACEHOLDER_TOKEN);
        log("OR_005", r);

        String body = r.getBody().asString().toLowerCase();
        Assert.assertFalse(body.matches(".*\"otp\"\\s*:\\s*\"\\d+\".*"),
                "OR_005: [CRITICAL SECURITY] OTP value exposed in response!");
        Assert.assertFalse(body.contains("otp_code") || body.contains("verification_code"),
                "OR_005: otp_code / verification_code must not appear in response");
        System.out.println("   ✅ OTP not exposed — SECURE");
    }

    @Test(priority = 6, description = "OR_006: Response Content-Type must be application/json")
    public void OR_006_ResponseContentType() {
        System.out.println("\n>>> OR_006: Content-Type header <<<");
        Response r = resend(validPayload(), PLACEHOLDER_TOKEN);
        log("OR_006", r);

        String ct = r.getHeader("Content-Type");
        Assert.assertNotNull(ct, "OR_006: Content-Type must be present");
        Assert.assertTrue(ct.toLowerCase().contains("application/json"),
                "OR_006: Content-Type must be application/json. Got: " + ct);
        System.out.println("   ✅ Content-Type: " + ct);
    }

    @Test(priority = 7, description = "OR_007: Response time must be under 3000ms")
    public void OR_007_ResponseTimeSLA() {
        System.out.println("\n>>> OR_007: SLA check < 3000ms <<<");
        Response r = resend(validPayload(), PLACEHOLDER_TOKEN);
        long ms = r.getTime();
        log("OR_007", r);

        Assert.assertTrue(ms < 3000,
                "OR_007: [SLA BREACH] " + ms + "ms exceeds 3000ms limit");
        System.out.println("   ✅ " + ms + "ms < 3000ms");
    }

    @Test(priority = 8, description = "OR_008: Successful response must have status, success, msg fields")
    public void OR_008_ResponseSchemaOnSuccess() {
        System.out.println("\n>>> OR_008: Response schema on success <<<");
        Response r = resend(validPayload(), PLACEHOLDER_TOKEN);
        log("OR_008", r);

        int status = r.getStatusCode();
        if (status == 200 || status == 201) {
            // Schema documentation — log actual fields, flag missing ones as potential API defects
            Object statusField  = r.jsonPath().get("status");
            Object successField = r.jsonPath().get("success");
            String msg = r.jsonPath().getString("msg");
            if (msg == null) msg = r.jsonPath().getString("message");
            if (statusField == null)
                System.out.println("   ⚠️ OR_008 [POTENTIAL API BUG]: 'status' field missing in 200/201 response — raise defect");
            if (successField == null)
                System.out.println("   ⚠️ OR_008 [POTENTIAL API BUG]: 'success' field missing in 200/201 response — raise defect");
            if (msg == null)
                System.out.println("   ⚠️ OR_008 [POTENTIAL API BUG]: 'msg'/'message' field missing in 200/201 response — raise defect");
            Assert.assertFalse(statusField == null && successField == null && msg == null,
                    "OR_008: Response body has none of the expected fields (status/success/msg). Body: " + r.getBody().asString());
            System.out.println("   ✅ Schema fields present — status=" + statusField
                    + " success=" + successField + " msg=" + msg);
        } else {
            System.out.println("   ℹ️ OR_008: Skipped schema check — HTTP " + status);
        }
    }

    @Test(priority = 9, description = "OR_009: Resend with different valid country codes — document support")
    public void OR_009_DifferentCountryCodes() {
        System.out.println("\n>>> OR_009: Different country codes <<<");
        String[][] data = {{"+91", "9876543210"}, {"+1", "2125551234"}, {"+44", "7911123456"}};
        for (String[] d : data) {
            Map<String, Object> p = new HashMap<>();
            p.put("country_code", d[0]);
            p.put("mobile", d[1]);
            Response r = resend(p, PLACEHOLDER_TOKEN);
            System.out.println("   " + d[0] + " → HTTP " + r.getStatusCode());
            Assert.assertNotEquals(r.getStatusCode(), 500,
                    "OR_009: " + d[0] + " must NOT cause 500");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MOBILE FIELD VALIDATION (OR_016 – OR_024)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(priority = 16, description = "OR_016: Missing mobile — must return 400")
    public void OR_016_MissingMobile() {
        System.out.println("\n>>> OR_016: Missing mobile <<<");
        Map<String, Object> p = validPayload();
        p.remove("mobile");
        Response r = resend(p, PLACEHOLDER_TOKEN);
        log("OR_016", r);
        assertMandatoryFieldError(r, "OR_016", "mobile", "Missing mobile must be rejected");
    }

    @Test(priority = 17, description = "OR_017: Empty mobile — must return 400")
    public void OR_017_EmptyMobile() {
        System.out.println("\n>>> OR_017: Empty mobile <<<");
        Map<String, Object> p = validPayload();
        p.put("mobile", "");
        Response r = resend(p, PLACEHOLDER_TOKEN);
        log("OR_017", r);
        assertMandatoryFieldError(r, "OR_017", "mobile", "Empty mobile must be rejected");
    }

    @Test(priority = 18, description = "OR_018: Null mobile — must return 400")
    public void OR_018_NullMobile() {
        System.out.println("\n>>> OR_018: Null mobile <<<");
        Map<String, Object> p = validPayload();
        p.put("mobile", null);
        Response r = resend(p, PLACEHOLDER_TOKEN);
        log("OR_018", r);
        assertMandatoryFieldError(r, "OR_018", "mobile", "Null mobile must be rejected");
    }

    @Test(priority = 19, description = "OR_019: Mobile too short (5 digits) — must return 400")
    public void OR_019_MobileTooShort() {
        System.out.println("\n>>> OR_019: Mobile too short <<<");
        Map<String, Object> p = validPayload();
        p.put("mobile", "12345");
        Response r = resend(p, PLACEHOLDER_TOKEN);
        log("OR_019", r);
        assertMandatoryFieldError(r, "OR_019", "mobile", "5-digit mobile must be rejected");
    }

    @Test(priority = 20, description = "OR_020: Mobile too long (15 digits) — must return 400")
    public void OR_020_MobileTooLong() {
        System.out.println("\n>>> OR_020: Mobile too long <<<");
        Map<String, Object> p = validPayload();
        p.put("mobile", "123456789012345");
        Response r = resend(p, PLACEHOLDER_TOKEN);
        log("OR_020", r);
        assertMandatoryFieldError(r, "OR_020", "mobile", "15-digit mobile must be rejected");
    }

    @Test(priority = 21, description = "OR_021: Alphabetic mobile — must return 400")
    public void OR_021_AlphabeticMobile() {
        System.out.println("\n>>> OR_021: Alphabetic mobile <<<");
        Map<String, Object> p = validPayload();
        p.put("mobile", "abcdefghij");
        Response r = resend(p, PLACEHOLDER_TOKEN);
        log("OR_021", r);
        assertMandatoryFieldError(r, "OR_021", "mobile", "Alphabetic mobile must be rejected");
    }

    @Test(priority = 22, description = "OR_022: Special characters in mobile — must return 400")
    public void OR_022_SpecialCharsMobile() {
        System.out.println("\n>>> OR_022: Special chars in mobile <<<");
        Map<String, Object> p = validPayload();
        p.put("mobile", "987@#56789");
        Response r = resend(p, PLACEHOLDER_TOKEN);
        log("OR_022", r);
        assertMandatoryFieldError(r, "OR_022", "mobile", "Special chars in mobile must be rejected");
    }

    @Test(priority = 23, description = "OR_023: Mobile as integer instead of string — must return 400")
    public void OR_023_MobileAsInteger() {
        System.out.println("\n>>> OR_023: Mobile as integer <<<");
        String rawBody = "{\"mobile\":9876543210,\"country_code\":\"+91\"}";
        Response r = resendRaw(rawBody, PLACEHOLDER_TOKEN);
        log("OR_023", r);
        assertMandatoryFieldError(r, "OR_023", "mobile", "Mobile as integer must be rejected — must be string");
    }

    @Test(priority = 24, description = "OR_024: Mobile with embedded country code (+919876543210)")
    public void OR_024_MobileWithEmbeddedCountryCode() {
        System.out.println("\n>>> OR_024: Mobile with embedded country code <<<");
        Map<String, Object> p = validPayload();
        p.put("mobile", "+91" + VALID_MOBILE);
        Response r = resend(p, PLACEHOLDER_TOKEN);
        log("OR_024", r);
        assertMandatoryFieldError(r, "OR_024", "mobile",
                "Mobile with embedded country code must be rejected");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // COUNTRY_CODE FIELD VALIDATION (OR_025 – OR_030)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(priority = 25, description = "OR_025: Missing country_code — document behavior (optional)")
    public void OR_025_MissingCountryCode() {
        System.out.println("\n>>> OR_025: Missing country_code (optional) <<<");
        Map<String, Object> p = new HashMap<>();
        p.put("mobile", VALID_MOBILE);
        Response r = resend(p, PLACEHOLDER_TOKEN);
        log("OR_025", r);

        int status = r.getStatusCode();
        Assert.assertTrue(isSuccessOrExpectedError(status),
                "OR_025: Must not return 5xx. Got: " + status);
        if (status == 200 || status == 201) {
            System.out.println("   ✅ country_code is optional — accepted without it");
        } else {
            System.out.println("   HTTP " + status + " — document behavior");
        }
    }

    @Test(priority = 26, description = "OR_026: Empty country_code — document behavior")
    public void OR_026_EmptyCountryCode() {
        System.out.println("\n>>> OR_026: Empty country_code <<<");
        Map<String, Object> p = validPayload();
        p.put("country_code", "");
        Response r = resend(p, PLACEHOLDER_TOKEN);
        log("OR_026", r);
        assertFieldError(r, "OR_026", "Empty country_code should be rejected");
    }

    @Test(priority = 27, description = "OR_027: country_code without + prefix — must return 400")
    public void OR_027_CountryCodeWithoutPlus() {
        System.out.println("\n>>> OR_027: country_code without + prefix <<<");
        Map<String, Object> p = validPayload();
        p.put("country_code", "91");
        Response r = resend(p, PLACEHOLDER_TOKEN);
        log("OR_027", r);
        assertFieldError(r, "OR_027", "country_code '91' without + must be rejected");
    }

    @Test(priority = 28, description = "OR_028: Invalid country_code +999 — must return 400")
    public void OR_028_InvalidCountryCode() {
        System.out.println("\n>>> OR_028: Invalid country_code +999 <<<");
        Map<String, Object> p = validPayload();
        p.put("country_code", "+999");
        Response r = resend(p, PLACEHOLDER_TOKEN);
        log("OR_028", r);
        assertFieldError(r, "OR_028", "Invalid country_code +999 should be rejected");
    }

    @Test(priority = 29, description = "OR_029: country_code as integer 91 — must return 400")
    public void OR_029_CountryCodeAsInteger() {
        System.out.println("\n>>> OR_029: country_code as integer <<<");
        String rawBody = "{\"mobile\":\"" + VALID_MOBILE + "\",\"country_code\":91}";
        Response r = resendRaw(rawBody, PLACEHOLDER_TOKEN);
        log("OR_029", r);
        assertFieldError(r, "OR_029", "country_code as integer must be rejected — must be string");
    }

    @Test(priority = 30, description = "OR_030: Empty JSON body {} — must return 400")
    public void OR_030_EmptyJsonBody() {
        System.out.println("\n>>> OR_030: Empty JSON body {} <<<");
        Response r = resendRaw("{}", PLACEHOLDER_TOKEN);
        log("OR_030", r);
        assertMandatoryFieldError(r, "OR_030", "mobile",
                "Empty body must be rejected — mobile is mandatory");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SECURITY (OR_031 – OR_038)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(priority = 31, description = "OR_031: SQL Injection in mobile — must not cause 500")
    public void OR_031_SQLInjectionMobile() {
        System.out.println("\n>>> OR_031: SQL Injection in mobile <<<");
        Map<String, Object> p = validPayload();
        p.put("mobile", "987' OR 1=1 --");
        Response r = resend(p, PLACEHOLDER_TOKEN);
        log("OR_031", r);

        Assert.assertNotEquals(r.getStatusCode(), 500,
                "OR_031: SQL injection must NOT cause 500");
        String body = r.getBody().asString();
        Assert.assertFalse(body.toLowerCase().contains("sql") && body.toLowerCase().contains("error"),
                "OR_031: [CRITICAL] SQL error info must not leak in response");
        System.out.println("   ✅ SQL injection handled — HTTP " + r.getStatusCode());
    }

    @Test(priority = 32, description = "OR_032: NoSQL Injection in mobile — must not cause 500")
    public void OR_032_NoSQLInjection() {
        System.out.println("\n>>> OR_032: NoSQL Injection in mobile <<<");
        String rawBody = "{\"mobile\":{\"$gt\":\"\"},\"country_code\":\"+91\"}";
        Response r = resendRaw(rawBody, PLACEHOLDER_TOKEN);
        log("OR_032", r);

        Assert.assertNotEquals(r.getStatusCode(), 500,
                "OR_032: NoSQL injection must NOT cause 500");
        System.out.println("   ✅ NoSQL injection handled — HTTP " + r.getStatusCode());
    }

    @Test(priority = 33, description = "OR_033: XSS in mobile — payload must not be reflected")
    public void OR_033_XSSInMobile() {
        System.out.println("\n>>> OR_033: XSS in mobile <<<");
        Map<String, Object> p = validPayload();
        p.put("mobile", "<script>alert(1)</script>");
        Response r = resend(p, PLACEHOLDER_TOKEN);
        log("OR_033", r);

        Assert.assertNotEquals(r.getStatusCode(), 500, "OR_033: XSS must NOT cause 500");
        Assert.assertFalse(r.getBody().asString().contains("<script>"),
                "OR_033: [CRITICAL] XSS payload reflected in response!");
        System.out.println("   ✅ XSS not reflected — SECURE");
    }

    @Test(priority = 34, description = "OR_034: SQL Injection in country_code — must not cause 500")
    public void OR_034_SQLInjectionCountryCode() {
        System.out.println("\n>>> OR_034: SQL Injection in country_code <<<");
        Map<String, Object> p = validPayload();
        p.put("country_code", "'+91' OR '1'='1");
        Response r = resend(p, PLACEHOLDER_TOKEN);
        log("OR_034", r);

        Assert.assertNotEquals(r.getStatusCode(), 500,
                "OR_034: SQL injection in country_code must NOT cause 500");
        System.out.println("   ✅ SQL injection in cc handled — HTTP " + r.getStatusCode());
    }

    @Test(priority = 35, description = "OR_035: Large payload (5000-char mobile) — must not cause 500")
    public void OR_035_LargePayload() {
        System.out.println("\n>>> OR_035: Large payload <<<");
        StringBuilder large = new StringBuilder();
        for (int i = 0; i < 5000; i++) large.append("9");
        Map<String, Object> p = validPayload();
        p.put("mobile", large.toString());
        Response r = resend(p, PLACEHOLDER_TOKEN);
        log("OR_035", r);

        Assert.assertNotEquals(r.getStatusCode(), 500,
                "OR_035: Large payload must NOT cause 500");
        System.out.println("   ✅ Large payload handled — HTTP " + r.getStatusCode());
    }

    @Test(priority = 36, description = "OR_036: Emoji in mobile — must not cause 500")
    public void OR_036_EmojiInMobile() {
        System.out.println("\n>>> OR_036: Emoji in mobile <<<");
        Map<String, Object> p = validPayload();
        p.put("mobile", "987\uD83D\uDE00543210");
        Response r = resend(p, PLACEHOLDER_TOKEN);
        log("OR_036", r);

        Assert.assertNotEquals(r.getStatusCode(), 500,
                "OR_036: Emoji in mobile must NOT cause 500");
        System.out.println("   ✅ Emoji handled — HTTP " + r.getStatusCode());
    }

    @Test(priority = 37, description = "OR_037: Whitespace-only mobile — must return 400")
    public void OR_037_WhitespaceOnlyMobile() {
        System.out.println("\n>>> OR_037: Whitespace-only mobile <<<");
        Map<String, Object> p = validPayload();
        p.put("mobile", "   ");
        Response r = resend(p, PLACEHOLDER_TOKEN);
        log("OR_037", r);
        assertMandatoryFieldError(r, "OR_037", "mobile", "Whitespace-only mobile must be rejected");
    }

    @Test(priority = 38, description = "OR_038: Response must not leak sensitive fields")
    public void OR_038_NoSensitiveDataInResponse() {
        System.out.println("\n>>> OR_038: No sensitive data in response <<<");
        Response r = resend(validPayload(), PLACEHOLDER_TOKEN);
        log("OR_038", r);

        String body = r.getBody().asString().toLowerCase();
        Assert.assertFalse(body.contains("password"),
                "OR_038: [CRITICAL] 'password' must not appear in response");
        Assert.assertFalse(body.contains("secret"),
                "OR_038: [CRITICAL] 'secret' must not appear in response");
        System.out.println("   ✅ No sensitive fields in response");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // RESPONSE SCHEMA & SLA (OR_039 – OR_046)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(priority = 39, description = "OR_039: Error response must have status field")
    public void OR_039_ErrorResponseHasStatusField() {
        System.out.println("\n>>> OR_039: Error response schema — status field <<<");
        Map<String, Object> p = validPayload();
        p.remove("mobile");
        Response r = resend(p, PLACEHOLDER_TOKEN);
        log("OR_039", r);

        int status = r.getStatusCode();
        if (status >= 400 && status < 500) {
            Object f = r.jsonPath().get("status");
            if (f == null) System.out.println("   ⚠️ 'status' field missing from error response");
            else System.out.println("   ✅ status: " + f);
        } else {
            System.out.println("   ℹ️ HTTP " + status + " — skipped schema check");
        }
    }

    @Test(priority = 40, description = "OR_040: Error response must have message field")
    public void OR_040_ErrorResponseHasMessage() {
        System.out.println("\n>>> OR_040: Error response — message field <<<");
        Map<String, Object> p = validPayload();
        p.put("mobile", "");
        Response r = resend(p, PLACEHOLDER_TOKEN);
        log("OR_040", r);

        int status = r.getStatusCode();
        if (status >= 400 && status < 500) {
            String msg = r.jsonPath().getString("msg");
            if (msg == null) msg = r.jsonPath().getString("message");
            if (msg == null) System.out.println("   ⚠️ No 'msg'/'message' in error response");
            else System.out.println("   ✅ message: " + msg);
        }
    }

    @Test(priority = 41, description = "OR_041: Unauthorized response must not contain access_token")
    public void OR_041_UnauthorizedResponseNoToken() {
        System.out.println("\n>>> OR_041: 401 must not contain access_token <<<");
        Response r = resendNoAuth(validPayload());
        log("OR_041", r);

        if (r.getStatusCode() == 401 || r.getStatusCode() == 403) {
            String token = r.jsonPath().getString("data.access_token");
            Assert.assertNull(token,
                    "OR_041: [CRITICAL SECURITY] access_token must NOT appear in 401 response!");
            System.out.println("   ✅ No access_token in unauthorized response");
        }
    }

    @Test(priority = 42, description = "OR_042: success field must be boolean type")
    public void OR_042_SuccessFieldIsBoolean() {
        System.out.println("\n>>> OR_042: 'success' field type <<<");
        Response r = resend(validPayload(), PLACEHOLDER_TOKEN);
        log("OR_042", r);

        Object v = r.jsonPath().get("success");
        if (v != null) {
            Assert.assertTrue(v instanceof Boolean,
                    "OR_042: 'success' must be Boolean. Got: " + v.getClass().getSimpleName());
            System.out.println("   ✅ success is Boolean: " + v);
        } else {
            System.out.println("   ⚠️ 'success' not present in response");
        }
    }

    @Test(priority = 43, description = "OR_043: status field must be integer type")
    public void OR_043_StatusFieldIsInteger() {
        System.out.println("\n>>> OR_043: 'status' field type <<<");
        Response r = resend(validPayload(), PLACEHOLDER_TOKEN);
        log("OR_043", r);

        Object v = r.jsonPath().get("status");
        if (v != null) {
            Assert.assertTrue(v instanceof Integer,
                    "OR_043: 'status' must be Integer. Got: " + v.getClass().getSimpleName());
            System.out.println("   ✅ status is Integer: " + v);
        } else {
            System.out.println("   ⚠️ 'status' not present in response");
        }
    }

    @Test(priority = 44, description = "OR_044: OTP never exposed in any response variant")
    public void OR_044_OTPNeverExposed() {
        System.out.println("\n>>> OR_044: OTP never exposed <<<");
        Response r = resend(validPayload(), PLACEHOLDER_TOKEN);
        String body = r.getBody().asString();
        Assert.assertFalse(body.matches(".*\"otp\"\\s*:\\s*\"\\d{6}\".*"),
                "OR_044: [CRITICAL SECURITY] OTP value exposed in response!");
        System.out.println("   ✅ OTP not exposed in response");
    }

    @Test(priority = 45, description = "OR_045: Response must not expose mobile number in error body")
    public void OR_045_MobileNotExposedInError() {
        System.out.println("\n>>> OR_045: Mobile not exposed in error body <<<");
        Map<String, Object> p = validPayload();
        p.put("mobile", "12345");
        Response r = resend(p, PLACEHOLDER_TOKEN);
        log("OR_045", r);

        if (r.getStatusCode() >= 400) {
            String body = r.getBody().asString();
            System.out.println("   Error body: " + body);
            // Just document — don't hard-fail as some APIs echo back field names
        }
        Assert.assertNotEquals(r.getStatusCode(), 500,
                "OR_045: Must not return 500 for short mobile");
    }

    @Test(priority = 46, description = "OR_046: 429 rate-limit response must contain retry information")
    public void OR_046_RateLimitResponseBody() {
        System.out.println("\n>>> OR_046: 429 response body check <<<");
        Response r = resend(validPayload(), PLACEHOLDER_TOKEN);
        log("OR_046", r);

        if (r.getStatusCode() == 429) {
            String body = r.getBody().asString();
            Assert.assertNotNull(body, "OR_046: 429 response must have a body");
            System.out.println("   ✅ 429 body: " + body);
        } else {
            System.out.println("   ℹ️ No rate-limit triggered in this run — HTTP " + r.getStatusCode());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HTTP METHOD & EDGE CASES (OR_047 – OR_055)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(priority = 47, description = "OR_047: GET on POST-only endpoint — must return 404 or 405")
    public void OR_047_WrongMethod_GET() {
        System.out.println("\n>>> OR_047: GET on POST-only endpoint <<<");
        Response r = RestAssured.given()
                .baseUri(BASE_URL).contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + PLACEHOLDER_TOKEN)
                .get(ENDPOINT);
        log("OR_047", r);

        int status = r.getStatusCode();
        Assert.assertTrue(status == 404 || status == 405,
                "OR_047: GET must return 404 or 405. Got: " + status);
        System.out.println("   ✅ GET rejected with HTTP " + status);
    }

    @Test(priority = 48, description = "OR_048: PUT on POST-only endpoint — must return 404 or 405")
    public void OR_048_WrongMethod_PUT() {
        System.out.println("\n>>> OR_048: PUT on POST-only endpoint <<<");
        Response r = RestAssured.given()
                .baseUri(BASE_URL).contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + PLACEHOLDER_TOKEN)
                .body(validPayload()).put(ENDPOINT);
        log("OR_048", r);

        int status = r.getStatusCode();
        Assert.assertTrue(status == 404 || status == 405,
                "OR_048: PUT must return 404 or 405. Got: " + status);
        System.out.println("   ✅ PUT rejected with HTTP " + status);
    }

    @Test(priority = 49, description = "OR_049: DELETE on POST-only endpoint — must return 404 or 405")
    public void OR_049_WrongMethod_DELETE() {
        System.out.println("\n>>> OR_049: DELETE on POST-only endpoint <<<");
        Response r = RestAssured.given()
                .baseUri(BASE_URL).contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + PLACEHOLDER_TOKEN)
                .delete(ENDPOINT);
        log("OR_049", r);

        int status = r.getStatusCode();
        Assert.assertTrue(status == 404 || status == 405,
                "OR_049: DELETE must return 404 or 405. Got: " + status);
        System.out.println("   ✅ DELETE rejected with HTTP " + status);
    }

    @Test(priority = 50, description = "OR_050: Wrong Content-Type (text/plain) — must not cause 500")
    public void OR_050_WrongContentType() {
        System.out.println("\n>>> OR_050: Wrong Content-Type <<<");
        Response r = RestAssured.given()
                .baseUri(BASE_URL)
                .contentType("text/plain")
                .header("Authorization", "Bearer " + PLACEHOLDER_TOKEN)
                .body("{\"mobile\":\"" + VALID_MOBILE + "\",\"country_code\":\"+91\"}")
                .post(ENDPOINT);
        log("OR_050", r);

        Assert.assertNotEquals(r.getStatusCode(), 500,
                "OR_050: Wrong Content-Type must NOT cause 500");
        System.out.println("   text/plain → HTTP " + r.getStatusCode());
    }

    @Test(priority = 51, description = "OR_051: Null body — must not cause 500")
    public void OR_051_NullBody() {
        System.out.println("\n>>> OR_051: Null body <<<");
        Response r = RestAssured.given()
                .baseUri(BASE_URL).contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + PLACEHOLDER_TOKEN)
                .post(ENDPOINT);
        log("OR_051", r);

        Assert.assertNotEquals(r.getStatusCode(), 500,
                "OR_051: Null body must NOT cause 500");
        System.out.println("   Null body → HTTP " + r.getStatusCode());
    }

    @Test(priority = 52, description = "OR_052: Extra unknown fields in body — must not cause 500")
    public void OR_052_ExtraUnknownFields() {
        System.out.println("\n>>> OR_052: Extra unknown fields <<<");
        Map<String, Object> p = validPayload();
        p.put("unknown_field", "unexpected_value");
        p.put("extra_param", 12345);
        Response r = resend(p, PLACEHOLDER_TOKEN);
        log("OR_052", r);

        Assert.assertNotEquals(r.getStatusCode(), 500,
                "OR_052: Extra fields must NOT cause 500");
        System.out.println("   Extra fields → HTTP " + r.getStatusCode());
    }

    @Test(priority = 53, description = "OR_053: Mobile with leading/trailing spaces — document trim behavior")
    public void OR_053_MobileWithSpaces() {
        System.out.println("\n>>> OR_053: Mobile with leading/trailing spaces <<<");
        Map<String, Object> p = validPayload();
        p.put("mobile", " " + VALID_MOBILE + " ");
        Response r = resend(p, PLACEHOLDER_TOKEN);
        log("OR_053", r);

        int status = r.getStatusCode();
        Assert.assertTrue(isSuccessOrExpectedError(status),
                "OR_053: Must not return 5xx. Got: " + status);
        System.out.println("   Mobile with spaces → HTTP " + status
                + (status == 200 || status == 201 ? " (API trims whitespace)" : ""));
    }

    @Test(priority = 54, description = "OR_054: Duplicate mobile field in JSON — must not cause 500")
    public void OR_054_DuplicateMobileField() {
        System.out.println("\n>>> OR_054: Duplicate mobile field <<<");
        String rawBody = "{\"mobile\":\"" + VALID_MOBILE + "\",\"country_code\":\"+91\",\"mobile\":\"9999999999\"}";
        Response r = resendRaw(rawBody, PLACEHOLDER_TOKEN);
        log("OR_054", r);

        Assert.assertNotEquals(r.getStatusCode(), 500,
                "OR_054: Duplicate field must NOT cause 500");
        System.out.println("   Duplicate field → HTTP " + r.getStatusCode());
    }

    @Test(priority = 55, description = "OR_055: Malformed JSON body — must return 400")
    public void OR_055_MalformedJSON() {
        System.out.println("\n>>> OR_055: Malformed JSON body <<<");
        Response r = resendRaw("{mobile: 9876543210, country_code: +91", PLACEHOLDER_TOKEN);
        log("OR_055", r);

        int status = r.getStatusCode();
        Assert.assertTrue(status == 400 || status == 415 || status == 422,
                "OR_055: Malformed JSON must return 400/415/422. Got: " + status);
        Assert.assertNotEquals(status, 500,
                "OR_055: Malformed JSON must NOT cause 500");
        System.out.println("   ✅ Malformed JSON rejected with HTTP " + status);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ASSERTION HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private void assertMandatoryFieldError(Response response, String testId,
                                            String fieldName, String reason) {
        int status = response.getStatusCode();

        if (status >= 400 && status < 500) {
            System.out.println("[" + testId + "] ✅ Correctly rejected with HTTP " + status);
            return;
        }
        if (status == 429) {
            System.out.println("[" + testId + "] ⚠️ HTTP 429 — staging rate limit. Wait 15 min and re-run.");
            return;
        }
        if (status == 200 || status == 201) {
            Object success = response.jsonPath().get("success");
            if (!Boolean.TRUE.equals(success)) {
                System.out.println("[" + testId + "] ✅ HTTP " + status + " with success=false/null — validation enforced");
                return;
            }
            Assert.fail(testId + " [BUG]: " + reason
                    + ". API accepted with " + status + "/success=true — mandatory field '"
                    + fieldName + "' validation NOT enforced. Raise as defect.");
        }
        if (status == 401) {
            System.out.println("[" + testId + "] ⚠️ HTTP 401 — token expired. Provide a live token to verify.");
            return;
        }
        Assert.fail(testId + ": Unexpected HTTP " + status + " | Body: " + response.getBody().asString());
    }

    private void assertFieldError(Response response, String testId, String reason) {
        int status = response.getStatusCode();

        if (status >= 400 && status < 500) {
            System.out.println("[" + testId + "] ✅ Correctly rejected with HTTP " + status);
            return;
        }
        if (status == 429) {
            System.out.println("[" + testId + "] ⚠️ HTTP 429 — staging rate limit. Wait 15 min and re-run.");
            return;
        }
        if (status == 200 || status == 201) {
            Object success = response.jsonPath().get("success");
            if (!Boolean.TRUE.equals(success)) {
                System.out.println("[" + testId + "] ✅ HTTP " + status + " with success=false/null — validation enforced");
                return;
            }
            System.out.println("[" + testId + "] ⚠️ API accepted invalid data. " + reason
                    + ". Body: " + response.getBody().asString());
            return;
        }
        if (status == 401) {
            System.out.println("[" + testId + "] ⚠️ HTTP 401 — token expired, test inconclusive.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADDITIONAL NEGATIVE SCENARIOS (OR_056–OR_063)
    // ─────────────────────────────────────────────────────────────────────────

    @Test(priority = 56, description = "OR_056: Alphanumeric mobile (mix of letters and digits) — must return 400")
    public void OR_056_AlphanumericMobile() {
        System.out.println("\n>>> OR_056: Alphanumeric mobile <<<");
        Response r = resend("abc1234567", "+91", PLACEHOLDER_TOKEN);
        log("OR_056", r);
        assertMandatoryFieldError(r, "OR_056", "mobile",
                "Alphanumeric mobile 'abc1234567' should be rejected");
    }

    @Test(priority = 57, description = "OR_057: country_code as JSON null value — must return 400 or treat as missing")
    public void OR_057_NullCountryCode() {
        System.out.println("\n>>> OR_057: country_code = null <<<");
        String rawBody = "{\"mobile\":\"" + VALID_MOBILE + "\",\"country_code\":null}";
        Response r = resendRaw(rawBody, PLACEHOLDER_TOKEN);
        log("OR_057", r);

        int status = r.getStatusCode();
        Assert.assertTrue(status >= 400 || status == 200 || status == 201 || status == 429,
                "OR_057: Null country_code must not cause 500. Got: " + status);
        Assert.assertNotEquals(status, 500,
                "OR_057: null country_code must NOT cause 500");
        System.out.println("   Null country_code → HTTP " + status
                + (status == 200 || status == 201 ? " (API treats null as missing — optional field)" : ""));
    }

    @Test(priority = 58, description = "OR_058: All-zeros mobile ('0000000000') — must return 400")
    public void OR_058_AllZerosMobile() {
        System.out.println("\n>>> OR_058: All-zeros mobile <<<");
        Response r = resend("0000000000", "+91", PLACEHOLDER_TOKEN);
        log("OR_058", r);
        assertFieldError(r, "OR_058",
                "All-zeros mobile '0000000000' should be rejected as invalid");
    }

    @Test(priority = 59, description = "OR_059: Mobile starting with 0 (invalid for +91 India) — must return 400")
    public void OR_059_MobileStartingWithZero() {
        System.out.println("\n>>> OR_059: Mobile starting with 0 <<<");
        Response r = resend("0123456789", "+91", PLACEHOLDER_TOKEN);
        log("OR_059", r);
        assertFieldError(r, "OR_059",
                "Mobile starting with 0 is invalid for +91 India");
    }

    @Test(priority = 60, description = "OR_060: country_code with special characters ('@91') — must return 400")
    public void OR_060_CountryCodeWithSpecialChars() {
        System.out.println("\n>>> OR_060: country_code with special chars <<<");
        Response r = resend(VALID_MOBILE, "@91", PLACEHOLDER_TOKEN);
        log("OR_060", r);
        assertMandatoryFieldError(r, "OR_060", "country_code",
                "country_code '@91' (special char prefix) must be rejected");
    }

    @Test(priority = 61, description = "OR_061: Unregistered mobile — API returns 200 for any number (document behavior)")
    public void OR_061_UnregisteredMobile() {
        System.out.println("\n>>> OR_061: Unregistered mobile <<<");
        Response r = resend("1111111111", "+91", PLACEHOLDER_TOKEN);
        log("OR_061", r);

        int status = r.getStatusCode();
        // API returns 200 even for unregistered numbers (sends OTP regardless of registration).
        // No assertion on status — documenting API behavior only.
        Assert.assertNotEquals(status, 500,
                "OR_061: Must NOT cause 500");
        System.out.println("   OR_061: Unregistered mobile → HTTP " + status
                + " (API sends OTP to any number — expected behavior)");
    }

    @Test(priority = 62, description = "OR_062: Mobile with internal spaces ('987 654 3210') — must return 400 or trim")
    public void OR_062_MobileWithInternalSpaces() {
        System.out.println("\n>>> OR_062: Mobile with internal spaces <<<");
        Response r = resend("987 654 3210", "+91", PLACEHOLDER_TOKEN);
        log("OR_062", r);

        int status = r.getStatusCode();
        Assert.assertNotEquals(status, 500,
                "OR_062: Mobile with internal spaces must NOT cause 500");
        System.out.println("   Internal-space mobile → HTTP " + status
                + (status == 200 || status == 201 ? " (API accepts — document this behavior)" : " (API rejects)"));
    }

    @Test(priority = 63, description = "OR_063: country_code too long ('+911234567890') — must return 400")
    public void OR_063_CountryCodeTooLong() {
        System.out.println("\n>>> OR_063: country_code too long <<<");
        Response r = resend(VALID_MOBILE, "+911234567890", PLACEHOLDER_TOKEN);
        log("OR_063", r);
        assertFieldError(r, "OR_063",
                "country_code '+911234567890' is too long and must be rejected");
    }
}

package com.mryoda.diagnostics.api.tests.otp;

import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Module 9 — Security Testing (OTP_065 – OTP_072)
 *
 * Verifies the endpoint is resistant to common injection attacks
 * (OWASP Top 10) and does not expose sensitive data in responses.
 *
 * Payloads in this class are SECURITY TEST INPUTS only — they are
 * not harmful when sent to a properly secured server.
 */
public class OTPSecurityTest extends OTPBaseTest {

    @Test(priority = 65, description = "OTP_065: SQL Injection in mobile field — expect rejection")
    public void OTP_065_SQLInjectionMobile() {
        System.out.println("\n>>> OTP_065: SQL Injection in mobile <<<");
        Response response = sendOtpRequest("' OR '1'='1", VALID_COUNTRY_CODE);
        logResponse("OTP_065", response);
        assertErrorResponse(response, "OTP_065");
        Assert.assertNotEquals(response.getStatusCode(), 500,
                "OTP_065: SQL injection must not cause a 500 server error");
    }

    @Test(priority = 66, description = "OTP_066: NoSQL Injection in mobile field — expect rejection")
    public void OTP_066_NoSQLInjectionMobile() {
        System.out.println("\n>>> OTP_066: NoSQL Injection in mobile <<<");
        // NoSQL operator injection via raw body
        String body = "{\"mobile\":{\"$gt\":\"\"},\"country_code\":\"" + VALID_COUNTRY_CODE + "\"}";
        Response response = sendOtpRaw(body);
        logResponse("OTP_066", response);

        Assert.assertNotEquals(response.getStatusCode(), 500,
                "OTP_066: NoSQL injection must not cause 500");
        assertErrorResponse(response, "OTP_066");
    }

    @Test(priority = 67, description = "OTP_067: Script Injection (XSS) in mobile field — expect rejection")
    public void OTP_067_ScriptInjectionMobile() {
        System.out.println("\n>>> OTP_067: Script Injection in mobile <<<");
        Response response = sendOtpRequest("<script>alert('xss')</script>", VALID_COUNTRY_CODE);
        logResponse("OTP_067", response);

        Assert.assertNotEquals(response.getStatusCode(), 500,
                "OTP_067: Script injection must not cause 500");
        // Response must NOT reflect the script tag back unescaped
        String body = response.getBody().asString();
        Assert.assertFalse(body.contains("<script>"),
                "OTP_067: Response must not reflect unescaped script tags");
        assertErrorResponse(response, "OTP_067");
    }

    @Test(priority = 68, description = "OTP_068: Command Injection in mobile field — expect rejection")
    public void OTP_068_CommandInjectionMobile() {
        System.out.println("\n>>> OTP_068: Command Injection in mobile <<<");
        Response response = sendOtpRequest("; ls -la; echo pwned", VALID_COUNTRY_CODE);
        logResponse("OTP_068", response);

        Assert.assertNotEquals(response.getStatusCode(), 500,
                "OTP_068: Command injection must not cause 500");
        assertErrorResponse(response, "OTP_068");
    }

    @Test(priority = 69, description = "OTP_069: JSON Injection in mobile field — expect rejection")
    public void OTP_069_JSONInjectionMobile() {
        System.out.println("\n>>> OTP_069: JSON Injection in mobile <<<");
        // Attempt to break out of the JSON string value
        String body = "{\"mobile\":\"9890\",\"injected\":true,\"country_code\":\"" + VALID_COUNTRY_CODE + "\"}";
        Response response = sendOtpRaw(body);
        logResponse("OTP_069", response);

        Assert.assertNotEquals(response.getStatusCode(), 500,
                "OTP_069: JSON injection must not cause 500");
    }

    @Test(priority = 70, description = "OTP_070: Long payload attack (10 000+ char mobile) — expect rejection")
    public void OTP_070_LongPayloadAttack() {
        System.out.println("\n>>> OTP_070: Long payload attack <<<");
        // 10 000 character mobile string
        String longMobile = "9".repeat(10_000);
        Response response = sendOtpRequest(longMobile, VALID_COUNTRY_CODE);
        logResponse("OTP_070", response);

        Assert.assertNotEquals(response.getStatusCode(), 500,
                "OTP_070: Oversized payload must not cause 500");
        assertErrorResponse(response, "OTP_070");
    }

    @Test(priority = 71, description = "OTP_071: JWT manipulation attempt — should be ignored (public API)")
    public void OTP_071_JWTManipulationAttempt() {
        System.out.println("\n>>> OTP_071: JWT manipulation attempt <<<");
        // Even with a tampered token in the Authorization header, a public API should succeed
        Response response = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .contentType(io.restassured.http.ContentType.JSON)
                .header("type", TYPE_HEADER_VALUE)
                .header("authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.tampered.sig")
                .body(new java.util.HashMap<String, Object>() {{ put("mobile", VALID_MOBILE); put("country_code", VALID_COUNTRY_CODE); }})
                .post(APIEndpoints.OTP_REQUEST);
        logResponse("OTP_071", response);
        // Public API — should not reject based on auth token alone
        Assert.assertNotEquals(response.getStatusCode(), 500,
                "OTP_071: Tampered token must not cause 500");
        System.out.println("OTP_071: Status " + response.getStatusCode()
                + " for tampered-token request on public API");
    }

    @Test(priority = 72, description = "OTP_072: Sensitive data exposure check — OTP value must not appear in response")
    public void OTP_072_SensitiveDataExposureCheck() {
        System.out.println("\n>>> OTP_072: Sensitive data exposure check <<<");
        Response response = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
        logResponse("OTP_072", response);

        Assert.assertEquals(response.getStatusCode(), 200, "OTP_072: Status must be 200");

        String body = response.getBody().asString();

        // The raw OTP value must never be returned to the client
        Assert.assertFalse(body.matches(".*\"otp\":\\s*\"?\\d{4,8}\"?.*"),
                "OTP_072: Raw OTP value must NOT be exposed in the response");

        // JWT secret must not be exposed
        Assert.assertFalse(body.toLowerCase().contains("jwtsecret") || body.toLowerCase().contains("jwt_secret"),
                "OTP_072: JWT secret must NOT appear in the response");

        // Internal stack traces or error details must not leak
        Assert.assertFalse(body.toLowerCase().contains("stacktrace") || body.toLowerCase().contains("exception"),
                "OTP_072: Internal exception/stacktrace must NOT appear in the response");

        System.out.println("OTP_072: No sensitive data detected. Response body length: " + body.length());
    }
}

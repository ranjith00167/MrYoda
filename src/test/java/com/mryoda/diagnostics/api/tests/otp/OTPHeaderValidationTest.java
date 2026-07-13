package com.mryoda.diagnostics.api.tests.otp;

import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Module 5 — Header Validation (OTP_038 – OTP_044)
 *
 * Verifies the endpoint handles various header combinations correctly —
 * content-type, accept, type, origin, and referer headers.
 */
public class OTPHeaderValidationTest extends OTPBaseTest {

    private Map<String, Object> validPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("mobile", VALID_MOBILE);
        payload.put("country_code", VALID_COUNTRY_CODE);
        return payload;
    }

    @Test(priority = 38, description = "OTP_038: Valid headers — expect success")
    public void OTP_038_ValidHeaders() {
        System.out.println("\n>>> OTP_038: All valid headers <<<");
        Response response = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .header("type", TYPE_HEADER_VALUE)
                .header("accept", "*/*")
                .header("content-type", "application/json")
                .body(validPayload())
                .post(APIEndpoints.OTP_REQUEST);
        logResponse("OTP_038", response);
        assertSuccess(response, "OTP_038");
    }

    @Test(priority = 39, description = "OTP_039: Missing Content-Type header")
    public void OTP_039_MissingContentType() {
        System.out.println("\n>>> OTP_039: Missing Content-Type header <<<");
        Response response = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .header("type", TYPE_HEADER_VALUE)
                .body("{\"mobile\":\"" + VALID_MOBILE + "\",\"country_code\":\"" + VALID_COUNTRY_CODE + "\"}")
                .post(APIEndpoints.OTP_REQUEST);
        logResponse("OTP_039", response);

        // Without Content-Type the server may reject or accept the request
        int status = response.getStatusCode();
        Assert.assertNotEquals(status, 500,
                "OTP_039: Server must not return 500 for missing Content-Type");
        System.out.println("OTP_039: Status " + status + " for missing Content-Type");
    }

    @Test(priority = 40, description = "OTP_040: Invalid Content-Type (text/plain)")
    public void OTP_040_InvalidContentType() {
        System.out.println("\n>>> OTP_040: Invalid Content-Type <<<");
        Response response = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .header("type", TYPE_HEADER_VALUE)
                .header("content-type", "text/plain")
                .body("{\"mobile\":\"" + VALID_MOBILE + "\",\"country_code\":\"" + VALID_COUNTRY_CODE + "\"}")
                .post(APIEndpoints.OTP_REQUEST);
        logResponse("OTP_040", response);

        int status = response.getStatusCode();
        Assert.assertTrue(status == 400 || status == 415,
                "OTP_040: Expected 400 or 415 Unsupported Media Type. Got: " + status);
    }

    @Test(priority = 41, description = "OTP_041: Missing Accept header — should be handled gracefully")
    public void OTP_041_MissingAcceptHeader() {
        System.out.println("\n>>> OTP_041: Missing Accept header <<<");
        Response response = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .header("type", TYPE_HEADER_VALUE)
                .header("content-type", "application/json")
                .body(validPayload())
                .post(APIEndpoints.OTP_REQUEST);
        logResponse("OTP_041", response);

        // Accept header is not required; server should handle gracefully
        int status = response.getStatusCode();
        Assert.assertNotEquals(status, 500,
                "OTP_041: Missing Accept must not cause 500");
    }

    @Test(priority = 42, description = "OTP_042: Missing 'type' header — validate business rule")
    public void OTP_042_MissingTypeHeader() {
        System.out.println("\n>>> OTP_042: Missing 'type' header <<<");
        Response response = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .header("content-type", "application/json")
                // 'type' header intentionally omitted
                .body(validPayload())
                .post(APIEndpoints.OTP_REQUEST);
        logResponse("OTP_042", response);

        // If 'type' header is mandatory, expect an error; otherwise record actual behavior
        int status = response.getStatusCode();
        System.out.println("OTP_042: Without 'type' header — status: " + status
                + " (verify business rule with dev team)");
        Assert.assertNotEquals(status, 500,
                "OTP_042: Missing 'type' header must not cause 500");
    }

    @Test(priority = 43, description = "OTP_043: Invalid Origin header — access denied if enforced")
    public void OTP_043_InvalidOrigin() {
        System.out.println("\n>>> OTP_043: Invalid Origin header <<<");
        Response response = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .header("type", TYPE_HEADER_VALUE)
                .header("content-type", "application/json")
                .header("origin", "https://malicious-origin.example.com")
                .body(validPayload())
                .post(APIEndpoints.OTP_REQUEST);
        logResponse("OTP_043", response);

        // CORS enforcement varies; record actual status for baseline
        int status = response.getStatusCode();
        System.out.println("OTP_043: Invalid Origin response status: " + status
                + " (verify CORS policy with dev team)");
        Assert.assertNotEquals(status, 500,
                "OTP_043: Invalid Origin must not cause 500");
    }

    @Test(priority = 44, description = "OTP_044: Invalid Referer header — access denied if enforced")
    public void OTP_044_InvalidReferer() {
        System.out.println("\n>>> OTP_044: Invalid Referer header <<<");
        Response response = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .header("type", TYPE_HEADER_VALUE)
                .header("content-type", "application/json")
                .header("referer", "https://attacker.example.com/")
                .body(validPayload())
                .post(APIEndpoints.OTP_REQUEST);
        logResponse("OTP_044", response);

        int status = response.getStatusCode();
        System.out.println("OTP_044: Invalid Referer response status: " + status
                + " (verify referer enforcement policy)");
        Assert.assertNotEquals(status, 500,
                "OTP_044: Invalid Referer must not cause 500");
    }
}

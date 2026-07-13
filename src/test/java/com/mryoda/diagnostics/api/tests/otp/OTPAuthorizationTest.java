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
 * Module 4 — Public Access Validation (OTP_030 – OTP_037)
 *
 * Verifies that /otps/getOtp is a public API:
 *   - Requests succeed WITHOUT any Authorization header
 *   - Various header-presence combinations all work (or fail gracefully
 *     for business-rule reasons, not auth reasons)
 */
public class OTPAuthorizationTest extends OTPBaseTest {

    private Map<String, Object> validPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("mobile", VALID_MOBILE);
        payload.put("country_code", VALID_COUNTRY_CODE);
        return payload;
    }

    @Test(priority = 30, description = "OTP_030: Request with no Authorization header — must succeed (public API)")
    public void OTP_030_NoAuthorizationHeaderSucceeds() {
        System.out.println("\n>>> OTP_030: No Authorization header — public API must succeed <<<");
        Response response = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .contentType(ContentType.JSON)
                .header("type", TYPE_HEADER_VALUE)
                .body(validPayload())
                .post(APIEndpoints.OTP_REQUEST);
        logResponse("OTP_030", response);
        assertSuccess(response, "OTP_030");
    }

    @Test(priority = 31, description = "OTP_031: Request with valid mobile and country code — no auth needed")
    public void OTP_031_ValidRequestWithoutAuth() {
        System.out.println("\n>>> OTP_031: Valid request without auth <<<");
        Response response = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
        logResponse("OTP_031", response);
        assertSuccess(response, "OTP_031");
    }

    @Test(priority = 32, description = "OTP_032: Request with empty Authorization header — public API should not block")
    public void OTP_032_EmptyAuthorizationHeaderAllowed() {
        System.out.println("\n>>> OTP_032: Empty Authorization header <<<");
        Response response = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .contentType(ContentType.JSON)
                .header("authorization", "")
                .header("type", TYPE_HEADER_VALUE)
                .body(validPayload())
                .post(APIEndpoints.OTP_REQUEST);
        logResponse("OTP_032", response);
        // Public API — empty auth header should either be ignored (200) or result in non-5xx
        Assert.assertNotEquals(response.getStatusCode(), 500,
                "OTP_032: Empty Authorization header must not cause 500");
        Assert.assertEquals(response.getStatusCode(), 200,
                "OTP_032: Public API should succeed even with empty Authorization header. Body: "
                        + response.getBody().asString());
    }

    @Test(priority = 33, description = "OTP_033: Request without type header — verify behavior")
    public void OTP_033_RequestWithoutTypeHeader() {
        System.out.println("\n>>> OTP_033: Request without type header <<<");
        Response response = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .contentType(ContentType.JSON)
                .body(validPayload())
                .post(APIEndpoints.OTP_REQUEST);
        logResponse("OTP_033", response);
        int status = response.getStatusCode();
        Assert.assertNotEquals(status, 500,
                "OTP_033: Missing type header must not cause 500");
        System.out.println("OTP_033: Status " + status + " without type header (document business rule)");
    }

    @Test(priority = 34, description = "OTP_034: Request with content-type only — minimal headers")
    public void OTP_034_MinimalHeadersRequest() {
        System.out.println("\n>>> OTP_034: Minimal headers request <<<");
        Response response = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .contentType(ContentType.JSON)
                .body(validPayload())
                .post(APIEndpoints.OTP_REQUEST);
        logResponse("OTP_034", response);
        Assert.assertNotEquals(response.getStatusCode(), 500,
                "OTP_034: Minimal-header request must not cause 500");
    }

    @Test(priority = 35, description = "OTP_035: Request with all standard headers — must succeed")
    public void OTP_035_AllStandardHeadersRequest() {
        System.out.println("\n>>> OTP_035: All standard headers <<<");
        Response response = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .contentType(ContentType.JSON)
                .header("type", TYPE_HEADER_VALUE)
                .header("accept", "*/*")
                .header("origin", "https://staging-mryoda.yodaprojects.com")
                .header("referer", "https://staging-mryoda.yodaprojects.com/")
                .body(validPayload())
                .post(APIEndpoints.OTP_REQUEST);
        logResponse("OTP_035", response);
        assertSuccess(response, "OTP_035");
    }

    @Test(priority = 36, description = "OTP_036: Multiple sequential requests without auth — consistent success")
    public void OTP_036_MultipleRequestsWithoutAuth() {
        System.out.println("\n>>> OTP_036: Multiple requests without auth <<<");
        for (int i = 1; i <= 3; i++) {
            Response response = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
            logResponse("OTP_036 [" + i + "]", response);
            Assert.assertFalse(response.getStatusCode() >= 500,
                    "OTP_036: Request " + i + " must not return 5xx. Got: " + response.getStatusCode());
        }
        System.out.println("OTP_036: All 3 requests completed without server errors.");
    }

    @Test(priority = 37, description = "OTP_037: Response is consistent regardless of extra irrelevant headers")
    public void OTP_037_ExtraIrrelevantHeadersIgnored() {
        System.out.println("\n>>> OTP_037: Extra irrelevant headers ignored <<<");
        Response response = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .contentType(ContentType.JSON)
                .header("type", TYPE_HEADER_VALUE)
                .header("x-custom-header", "test-value")
                .header("x-request-id", "abc123")
                .body(validPayload())
                .post(APIEndpoints.OTP_REQUEST);
        logResponse("OTP_037", response);
        assertSuccess(response, "OTP_037");
    }
}

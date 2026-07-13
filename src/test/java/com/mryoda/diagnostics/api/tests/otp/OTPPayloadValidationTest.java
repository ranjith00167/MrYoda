package com.mryoda.diagnostics.api.tests.otp;

import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Module 6 — Request Payload Validation (OTP_045 – OTP_050)
 *
 * Verifies the endpoint correctly handles malformed, empty, or
 * structurally incorrect JSON request bodies.
 */
public class OTPPayloadValidationTest extends OTPBaseTest {

    @Test(priority = 45, description = "OTP_045: Empty request body")
    public void OTP_045_EmptyRequestBody() {
        System.out.println("\n>>> OTP_045: Empty request body <<<");
        Response response = baseSpec()
                .contentType(ContentType.JSON)
                .body("{}")
                .post(APIEndpoints.OTP_REQUEST);
        logResponse("OTP_045", response);
        assertErrorResponse(response, "OTP_045");
    }

    @Test(priority = 46, description = "OTP_046: Null / blank request body")
    public void OTP_046_NullRequestBody() {
        System.out.println("\n>>> OTP_046: Null request body <<<");
        Response response = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .header("type", TYPE_HEADER_VALUE)
                .contentType(ContentType.JSON)
                // No body provided
                .post(APIEndpoints.OTP_REQUEST);
        logResponse("OTP_046", response);

        int status = response.getStatusCode();
        Assert.assertTrue(status == 400 || status == 422,
                "OTP_046: Null body should return 400 or 422. Got: " + status);
    }

    @Test(priority = 47, description = "OTP_047: Invalid JSON structure — expect 400 Bad Request")
    public void OTP_047_InvalidJsonStructure() {
        System.out.println("\n>>> OTP_047: Invalid JSON structure <<<");
        // Deliberately broken JSON
        String malformedJson = "{mobile: 9890909898, country_code: +91}";
        Response response = baseSpec()
                .contentType(ContentType.JSON)
                .body(malformedJson)
                .post(APIEndpoints.OTP_REQUEST);
        logResponse("OTP_047", response);

        Assert.assertEquals(response.getStatusCode(), 400,
                "OTP_047: Malformed JSON must return 400 Bad Request");
    }

    @Test(priority = 48, description = "OTP_048: Additional unexpected field in body — should ignore or error")
    public void OTP_048_AdditionalUnexpectedField() {
        System.out.println("\n>>> OTP_048: Extra unexpected field <<<");
        String bodyWithExtra = "{"
                + "\"mobile\":\"" + VALID_MOBILE + "\","
                + "\"country_code\":\"" + VALID_COUNTRY_CODE + "\","
                + "\"unexpected_field\":\"injected_value\""
                + "}";
        Response response = sendOtpRaw(bodyWithExtra);
        logResponse("OTP_048", response);

        // Acceptable: server ignores extra field (200) OR rejects it (4xx)
        int status = response.getStatusCode();
        Assert.assertNotEquals(status, 500,
                "OTP_048: Extra field must not cause 500");
        System.out.println("OTP_048: Status " + status + " for extra field (verify ignore vs. reject policy)");
    }

    @Test(priority = 49, description = "OTP_049: Nested JSON object instead of string for mobile")
    public void OTP_049_NestedJsonInsteadOfString() {
        System.out.println("\n>>> OTP_049: Nested JSON instead of string <<<");
        // mobile should be a string; passing an object instead
        String nestedBody = "{"
                + "\"mobile\":{\"number\":\"" + VALID_MOBILE + "\"},"
                + "\"country_code\":\"" + VALID_COUNTRY_CODE + "\""
                + "}";
        Response response = sendOtpRaw(nestedBody);
        logResponse("OTP_049", response);
        assertErrorResponse(response, "OTP_049");
    }

    @Test(priority = 50, description = "OTP_050: Array passed instead of string for mobile")
    public void OTP_050_ArrayInsteadOfString() {
        System.out.println("\n>>> OTP_050: Array instead of string <<<");
        // mobile should be a string; passing an array instead
        String arrayBody = "{"
                + "\"mobile\":[\"" + VALID_MOBILE + "\"],"
                + "\"country_code\":\"" + VALID_COUNTRY_CODE + "\""
                + "}";
        Response response = sendOtpRaw(arrayBody);
        logResponse("OTP_050", response);
        assertErrorResponse(response, "OTP_050");
    }
}

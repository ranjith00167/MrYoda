package com.mryoda.diagnostics.api.tests.otp;

import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.RequestContext;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.testng.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * OTPBaseTest — shared constants and helper methods for all OTP test modules (OTP_001 – OTP_093).
 *
 * /otps/getOtp is a public API — no authorization header is required.
 */
public class OTPBaseTest extends BaseTest {

    // ====== Test data constants ======
    protected static final String VALID_MOBILE       = "9890909898";
    protected static final String VALID_COUNTRY_CODE = "+91";
    protected static final String TYPE_HEADER_VALUE  = "Mr.Yoda Web";

    /**
     * Sends an OTP request with the given mobile and country code.
     */
    protected Response sendOtpRequest(String mobile, String countryCode) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("mobile", mobile);
        payload.put("country_code", countryCode);

        return baseSpec()
                .contentType(ContentType.JSON)
                .body(payload)
                .post(APIEndpoints.OTP_REQUEST);
    }

    /**
     * Sends an OTP request with a raw string body (for injection / malformed JSON tests).
     */
    protected Response sendOtpRaw(String rawBody) {
        return baseSpec()
                .contentType(ContentType.JSON)
                .body(rawBody)
                .post(APIEndpoints.OTP_REQUEST);
    }

    /**
     * Builds a base RestAssured spec with the type header only (public API — no auth required).
     */
    protected RequestSpecification baseSpec() {
        return RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .header("type", TYPE_HEADER_VALUE);
    }

    /**
     * Logs the response details and stores API performance metric.
     */
    protected void logResponse(String testId, Response response) {
        RequestContext.storeApiPerformance(APIEndpoints.OTP_REQUEST, response.getTime());
        System.out.println("[" + testId + "] HTTP " + response.getStatusCode()
                + " | " + response.getTime() + "ms"
                + " | " + response.getBody().asString());
    }

    /**
     * Asserts the response represents a validation/business error.
     * Accepts: any 4xx status, or HTTP 200 with success=false.
     */
    protected void assertErrorResponse(Response response, String testId) {
        int status = response.getStatusCode();
        if (status >= 400 && status < 500) {
            System.out.println("[" + testId + "] Correct error HTTP " + status);
            return;
        }
        if (status == 200) {
            Boolean success = response.jsonPath().getBoolean("success");
            Assert.assertFalse(Boolean.TRUE.equals(success),
                    testId + ": Expected success=false in 200 response. Body: "
                            + response.getBody().asString());
            System.out.println("[" + testId + "] HTTP 200 with success=false (acceptable for validation error)");
            return;
        }
        Assert.fail(testId + ": Unexpected status " + status
                + " | Body: " + response.getBody().asString());
    }

    /**
     * Asserts HTTP 200 with success=true in response body.
     */
    protected void assertSuccess(Response response, String testId) {
        System.out.println("[" + testId + "] HTTP " + response.getStatusCode()
                + " | Body: " + response.getBody().asString());
        Assert.assertEquals(response.getStatusCode(), 200,
                testId + ": Expected HTTP 200. Body: " + response.getBody().asString());
        Boolean success = response.jsonPath().getBoolean("success");
        Assert.assertTrue(Boolean.TRUE.equals(success),
                testId + ": Expected success=true. Body: " + response.getBody().asString());
    }

    /**
     * Asserts HTTP 400 with complete mobile validation error body structure.
     *
     * Validates ALL of:
     *   - HTTP status == 400
     *   - response.error == "Validation failed"
     *   - response.details[] array is present
     *   - response.details[0].field == "mobile"
     *   - response.details[0].message is not null
     *
     * Use this for every test that expects the API to reject an invalid mobile value.
     * Checking only the status code risks false positives when the server returns 400
     * for a completely different reason (e.g., malformed auth, missing header, wrong endpoint).
     */
    protected void assertMobileValidationError(Response response, String testId) {
        Assert.assertEquals(response.getStatusCode(), 400,
                testId + ": Expected HTTP 400 for invalid mobile. Got: "
                        + response.getStatusCode() + " | " + response.getBody().asString());
        Assert.assertEquals(response.jsonPath().getString("error"), "Validation failed",
                testId + ": 'error' field must be 'Validation failed'. Body: "
                        + response.getBody().asString());
        Assert.assertNotNull(response.jsonPath().getList("details"),
                testId + ": 'details' array must be present in error response. Body: "
                        + response.getBody().asString());
        Assert.assertEquals(response.jsonPath().getString("details[0].field"), "mobile",
                testId + ": details[0].field must be 'mobile'. Body: "
                        + response.getBody().asString());
        Assert.assertNotNull(response.jsonPath().getString("details[0].message"),
                testId + ": details[0].message must not be null");
        System.out.println("[" + testId + "] Body validation passed — error/details/field/message all correct");
    }
}

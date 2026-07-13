package com.mryoda.diagnostics.api.tests.otp;

import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Module 8 — Response Validation (OTP_058 – OTP_064)
 *
 * For every successful OTP response, validates:
 *   - Status code
 *   - Success message
 *   - Schema (mandatory fields + data types)
 *   - Response headers
 *   - Content of the response body
 *
 * Actual API response schema:
 * {
 *   "status":  200,                   // number
 *   "success": true,                  // boolean
 *   "msg":     "OTP sent successfully" // string
 * }
 */
public class OTPResponseValidationTest extends OTPBaseTest {

    private Response getSuccessResponse() {
        return sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
    }

    @Test(priority = 58, description = "OTP_058: Status code validation — expect 200")
    public void OTP_058_StatusCodeValidation() {
        System.out.println("\n>>> OTP_058: Status code validation <<<");
        Response response = getSuccessResponse();
        logResponse("OTP_058", response);

        Assert.assertEquals(response.getStatusCode(), 200,
                "OTP_058: Expected HTTP 200 OK");
    }

    @Test(priority = 59, description = "OTP_059: Success message validation")
    public void OTP_059_SuccessMessageValidation() {
        System.out.println("\n>>> OTP_059: Success message validation <<<");
        Response response = getSuccessResponse();
        logResponse("OTP_059", response);

        Assert.assertEquals(response.getStatusCode(), 200, "OTP_059: Status must be 200");
        String message = response.jsonPath().getString("msg");
        Assert.assertNotNull(message, "OTP_059: 'msg' field must not be null");
        Assert.assertFalse(message.trim().isEmpty(), "OTP_059: 'msg' must not be blank");
        System.out.println("OTP_059: msg — \"" + message + "\"");
    }

    @Test(priority = 60, description = "OTP_060: Response schema validation")
    public void OTP_060_ResponseSchemaValidation() {
        System.out.println("\n>>> OTP_060: Response schema validation <<<");
        Response response = getSuccessResponse();
        logResponse("OTP_060", response);

        Assert.assertEquals(response.getStatusCode(), 200, "OTP_060: Status must be 200");

        // Validate all three top-level keys are present
        Object status  = response.jsonPath().get("status");
        Object success = response.jsonPath().get("success");
        Object msg     = response.jsonPath().get("msg");

        Assert.assertNotNull(status,  "OTP_060: 'status' field must be present");
        Assert.assertNotNull(success, "OTP_060: 'success' field must be present");
        Assert.assertNotNull(msg,     "OTP_060: 'msg' field must be present");

        System.out.println("OTP_060: Schema — status=" + status + ", success=" + success + ", msg=" + msg);
    }

    @Test(priority = 61, description = "OTP_061: Mandatory fields validation")
    public void OTP_061_MandatoryFieldValidation() {
        System.out.println("\n>>> OTP_061: Mandatory fields <<<");
        Response response = getSuccessResponse();
        logResponse("OTP_061", response);

        Assert.assertEquals(response.getStatusCode(), 200, "OTP_061: Status must be 200");

        String body = response.getBody().asString();
        Assert.assertTrue(body.contains("\"status\""),  "OTP_061: 'status' is a mandatory field");
        Assert.assertTrue(body.contains("\"success\""), "OTP_061: 'success' is a mandatory field");
        Assert.assertTrue(body.contains("\"msg\""),     "OTP_061: 'msg' is a mandatory field");
    }

    @Test(priority = 62, description = "OTP_062: Data type validation")
    public void OTP_062_DataTypeValidation() {
        System.out.println("\n>>> OTP_062: Data type validation <<<");
        Response response = getSuccessResponse();
        logResponse("OTP_062", response);

        Assert.assertEquals(response.getStatusCode(), 200, "OTP_062: Status must be 200");

        // success → Boolean
        Object successRaw = response.jsonPath().get("success");
        Assert.assertTrue(successRaw instanceof Boolean,
                "OTP_062: 'success' must be Boolean. Got: " + successRaw.getClass().getSimpleName());

        // msg → String
        Object msgRaw = response.jsonPath().get("msg");
        Assert.assertNotNull(msgRaw, "OTP_062: 'msg' field must not be null");
        Assert.assertTrue(msgRaw instanceof String,
                "OTP_062: 'msg' must be String. Got: " + msgRaw.getClass().getSimpleName());

        // status → Integer
        Object statusRaw = response.jsonPath().get("status");
        Assert.assertNotNull(statusRaw, "OTP_062: 'status' field must not be null");
        Assert.assertTrue(statusRaw instanceof Integer,
                "OTP_062: 'status' must be Integer. Got: " + statusRaw.getClass().getSimpleName());

        System.out.println("OTP_062: Data types validated — status(Integer), success(Boolean), msg(String)");
    }

    @Test(priority = 63, description = "OTP_063: Response headers validation")
    public void OTP_063_ResponseHeadersValidation() {
        System.out.println("\n>>> OTP_063: Response headers validation <<<");
        Response response = getSuccessResponse();
        logResponse("OTP_063", response);

        Assert.assertEquals(response.getStatusCode(), 200, "OTP_063: Status must be 200");

        String contentType = response.getHeader("Content-Type");
        Assert.assertNotNull(contentType, "OTP_063: Content-Type header must be present");
        Assert.assertTrue(contentType.toLowerCase().contains("application/json"),
                "OTP_063: Content-Type must contain application/json. Got: " + contentType);

        System.out.println("OTP_063: Content-Type = " + contentType);
    }

    @Test(priority = 64, description = "OTP_064: Response content validation — no sensitive data exposed")
    public void OTP_064_ResponseContentValidation() {
        System.out.println("\n>>> OTP_064: Response content validation <<<");
        Response response = getSuccessResponse();
        logResponse("OTP_064", response);

        Assert.assertEquals(response.getStatusCode(), 200, "OTP_064: Status must be 200");

        String body = response.getBody().asString();

        // OTP value must NOT be returned in the response body
        Assert.assertFalse(body.matches(".*\"otp\":\\s*\"?\\d{4,8}\"?.*"),
                "OTP_064: Response must NOT expose the OTP value");

        // JWT secret must NOT be in response
        Assert.assertFalse(body.toLowerCase().contains("jwtsecret") || body.toLowerCase().contains("jwt_secret"),
                "OTP_064: Response must NOT expose JWT secret");

        // Passwords or secrets must not leak
        Assert.assertFalse(body.toLowerCase().contains("password") || body.toLowerCase().contains("secret"),
                "OTP_064: Response must NOT expose sensitive fields");

        System.out.println("OTP_064: No sensitive data detected in response body.");
    }
}

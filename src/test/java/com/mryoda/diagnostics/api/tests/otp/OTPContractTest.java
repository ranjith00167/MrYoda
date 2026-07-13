package com.mryoda.diagnostics.api.tests.otp;

import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Module 13 — Contract Testing (OTP_090 – OTP_093)
 *
 * Verifies the OTP endpoint adheres to its published API contract:
 *   - All required fields are always present
 *   - Field data types are consistent across responses
 *   - No field is unexpectedly removed between calls
 *   - The contract is backward-compatible
 *
 * Actual API response contract:
 * {
 *   "status":  200,                   // integer
 *   "success": true,                  // boolean
 *   "msg":     "OTP sent successfully" // string
 * }
 */
public class OTPContractTest extends OTPBaseTest {

    /**
     * Validates the contract of a single OTP success response.
     */
    private void validateContract(Response response, String testId) {
        Assert.assertEquals(response.getStatusCode(), 200,
                testId + ": HTTP 200 required for contract validation");

        String body = response.getBody().asString();
        System.out.println(testId + ": Response body = " + body);

        // ---- Required fields must be present ----
        Assert.assertTrue(body.contains("\"status\""),
                testId + ": Contract violation — 'status' field missing");
        Assert.assertTrue(body.contains("\"success\""),
                testId + ": Contract violation — 'success' field missing");
        Assert.assertTrue(body.contains("\"msg\""),
                testId + ": Contract violation — 'msg' field missing");

        // ---- Data types ----
        Object successVal = response.jsonPath().get("success");
        Assert.assertTrue(successVal instanceof Boolean,
                testId + ": 'success' must be Boolean. Got: " + successVal.getClass().getSimpleName());

        Object msgVal = response.jsonPath().get("msg");
        Assert.assertNotNull(msgVal, testId + ": 'msg' must not be null");
        Assert.assertTrue(msgVal instanceof String,
                testId + ": 'msg' must be String. Got: " + msgVal.getClass().getSimpleName());

        Object statusVal = response.jsonPath().get("status");
        Assert.assertNotNull(statusVal, testId + ": 'status' must not be null");
        Assert.assertTrue(statusVal instanceof Integer,
                testId + ": 'status' must be Integer. Got: " + statusVal.getClass().getSimpleName());
    }

    @Test(priority = 90, description = "OTP_090: Required fields always present in success response")
    public void OTP_090_RequiredFieldsAlwaysPresent() {
        System.out.println("\n>>> OTP_090: Required fields always present <<<");
        // Run three calls to confirm consistency
        for (int i = 1; i <= 3; i++) {
            Response response = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
            logResponse("OTP_090 [call " + i + "]", response);
            validateContract(response, "OTP_090 [call " + i + "]");
        }
        System.out.println("OTP_090: All required fields present across 3 consecutive calls.");
    }

    @Test(priority = 91, description = "OTP_091: Field datatype consistency across multiple responses")
    public void OTP_091_FieldDatatypeConsistency() {
        System.out.println("\n>>> OTP_091: Field datatype consistency <<<");
        Class<?> firstSuccessType = null;
        Class<?> firstMessageType = null;

        for (int i = 1; i <= 3; i++) {
            Response response = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
            logResponse("OTP_091 [call " + i + "]", response);
            Assert.assertEquals(response.getStatusCode(), 200,
                    "OTP_091: Call " + i + " must return 200");

            Object successVal = response.jsonPath().get("success");
            Object msgVal = response.jsonPath().get("msg");

            if (i == 1) {
                firstSuccessType = successVal.getClass();
                firstMessageType = msgVal.getClass();
                System.out.println("OTP_091: Baseline — success: " + firstSuccessType.getSimpleName()
                        + ", msg: " + firstMessageType.getSimpleName());
            } else {
                Assert.assertEquals(successVal.getClass(), firstSuccessType,
                        "OTP_091: 'success' type changed at call " + i);
                Assert.assertEquals(msgVal.getClass(), firstMessageType,
                        "OTP_091: 'msg' type changed at call " + i);
            }
        }
        System.out.println("OTP_091: Data types consistent across all 3 calls.");
    }

    @Test(priority = 92, description = "OTP_092: No field removal — response schema stable between calls")
    public void OTP_092_NoFieldRemoval() {
        System.out.println("\n>>> OTP_092: No field removal between calls <<<");
        String[] requiredFields = {"status", "success", "msg"};

        for (int call = 1; call <= 2; call++) {
            Response response = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
            logResponse("OTP_092 [call " + call + "]", response);
            Assert.assertEquals(response.getStatusCode(), 200,
                    "OTP_092: Call " + call + " must return 200");

            String body = response.getBody().asString();
            for (String field : requiredFields) {
                Assert.assertTrue(body.contains("\"" + field + "\""),
                        "OTP_092 [call " + call + "]: Required field '" + field + "' was removed from response");
            }
        }
        System.out.println("OTP_092: Schema stable — no fields removed across consecutive calls.");
    }

    @Test(priority = 93, description = "OTP_093: Backward compatibility — contract must match documented schema")
    public void OTP_093_BackwardCompatibility() {
        System.out.println("\n>>> OTP_093: Backward compatibility <<<");
        Response response = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
        logResponse("OTP_093", response);

        Assert.assertEquals(response.getStatusCode(), 200,
                "OTP_093: HTTP 200 required for backward-compatibility check");

        // Verify documented schema is intact
        validateContract(response, "OTP_093");

        // The 'success' field must be true (not changed to a different key like 'ok', 'status', etc.)
        Boolean success = response.jsonPath().getBoolean("success");
        Assert.assertNotNull(success,
                "OTP_093: 'success' key must exist at the root level (not renamed)");

        // 'msg' must be a human-readable string (not a code or integer)
        String message = response.jsonPath().getString("msg");
        Assert.assertNotNull(message, "OTP_093: 'msg' must not be null");
        Assert.assertFalse(message.trim().isEmpty(), "OTP_093: 'msg' must not be empty");
        Assert.assertFalse(message.matches("^\\d+$"),
                "OTP_093: 'msg' must be a human-readable string, not a numeric code");

        System.out.println("OTP_093: Backward-compatible schema confirmed. "
                + "success=" + success + " | msg=\"" + message + "\"");
    }
}

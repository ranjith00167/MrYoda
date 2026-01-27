package com.mryoda.diagnostics.api.utils;

import io.restassured.response.Response;
import static org.testng.Assert.*;
import org.assertj.core.api.SoftAssertions;
import com.mryoda.diagnostics.api.utils.LoggerUtil;

/**
 * Assertion Utility for API Response Validations
 */
public class AssertionUtil {

    private static SoftAssertions softAssert;

    private AssertionUtil() {
        // Private constructor
    }

    /**
     * Initialize soft assertions
     */
    public static void initSoftAssertions() {
        softAssert = new SoftAssertions();
    }

    /**
     * Assert all soft assertions
     */
    public static void assertAll() {
        if (softAssert != null) {
            softAssert.assertAll();
        }
    }

    /**
     * Verify status code
     */
    public static void verifyStatusCode(Response response, int expectedStatusCode) {
        int actualStatusCode = response.getStatusCode();
        assertEquals(actualStatusCode, expectedStatusCode,
                "Status code mismatch. Expected: " + expectedStatusCode + ", Actual: " + actualStatusCode);
        LoggerUtil.info("Status Code Verified: " + actualStatusCode);
    }

    /**
     * Verify status code is one of the expected values
     */
    public static void verifyStatusCodeIn(Response response, int... expectedStatusCodes) {
        int actualStatusCode = response.getStatusCode();
        boolean found = false;
        for (int expected : expectedStatusCodes) {
            if (actualStatusCode == expected) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Status code " + actualStatusCode + " not found in expected list");
        LoggerUtil.info("Status Code Verified: " + actualStatusCode);
    }

    /**
     * Verify content type
     */
    public static void verifyContentType(Response response, String expectedContentType) {
        String actualContentType = response.getContentType();
        assertTrue(actualContentType.contains(expectedContentType),
                "Content type mismatch. Expected: " + expectedContentType + ", Actual: " + actualContentType);
        LoggerUtil.info("Content Type Verified: " + actualContentType);
    }

    public static void verifyNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new AssertionError("❌ " + fieldName + " should NOT be NULL");
        }
        LoggerUtil.info("✔ " + fieldName + " is valid: " + value);
    }

    public static void verifyEquals(Object actual, Object expected, String fieldName) {
        if (actual == null || expected == null) {
            throw new AssertionError("❌ " + fieldName + " comparison failed — NULL value found");
        }

        if (!actual.equals(expected)) {
            throw new AssertionError("❌ " + fieldName + " mismatch — expected: "
                    + expected + " but found: " + actual);
        }

        LoggerUtil.info("✔ " + fieldName + " is correct: " + actual);
    }

    public static void verifyNotNull(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new AssertionError("❌ " + fieldName + " should NOT be NULL or EMPTY");
        }
        LoggerUtil.info("✔ " + fieldName + " is valid: " + value);
    }

    /**
     * Verify response time
     */
    public static void verifyResponseTime(Response response, long maxResponseTime) {
        long actualResponseTime = response.getTime();
        assertTrue(actualResponseTime < maxResponseTime,
                "Response time exceeded. Expected < " + maxResponseTime + "ms, Actual: " + actualResponseTime + "ms");
        LoggerUtil.info("Response Time Verified: " + actualResponseTime + "ms");
    }

    /**
     * Verify JSON field exists
     */
    public static void verifyJsonFieldExists(Response response, String fieldPath) {
        Object value = response.jsonPath().get(fieldPath);
        assertNotNull(value, "Field '" + fieldPath + "' does not exist in response");
        LoggerUtil.info("Field Exists: " + fieldPath);
    }

    /**
     * Verify JSON field value
     */
    public static void verifyJsonFieldValue(Response response, String fieldPath, Object expectedValue) {
        Object actualValue = response.jsonPath().get(fieldPath);
        assertEquals(actualValue, expectedValue,
                "Field value mismatch for '" + fieldPath + "'. Expected: " + expectedValue + ", Actual: "
                        + actualValue);
        LoggerUtil.info("Field Value Verified: " + fieldPath + " = " + actualValue);
    }

    /**
     * Verify JSON field is not null
     */
    public static void verifyJsonFieldNotNull(Response response, String fieldPath) {
        Object value = response.jsonPath().get(fieldPath);
        assertNotNull(value, "Field '" + fieldPath + "' is null");
        LoggerUtil.info("Field Not Null: " + fieldPath);
    }

    /**
     * Verify JSON array size
     */
    public static void verifyJsonArraySize(Response response, String arrayPath, int expectedSize) {
        int actualSize = response.jsonPath().getList(arrayPath).size();
        assertEquals(actualSize, expectedSize,
                "Array size mismatch for '" + arrayPath + "'. Expected: " + expectedSize + ", Actual: " + actualSize);
        LoggerUtil.info("Array Size Verified: " + arrayPath + " = " + actualSize);
    }

    /**
     * Verify response body contains text
     */
    public static void verifyResponseBodyContains(Response response, String expectedText) {
        String responseBody = response.getBody().asString();
        assertTrue(responseBody.contains(expectedText),
                "Response body does not contain: " + expectedText);
        LoggerUtil.info("Response Body Contains: " + expectedText);
    }

    /**
     * Verify header exists
     */
    public static void verifyHeaderExists(Response response, String headerName) {
        String headerValue = response.getHeader(headerName);
        assertNotNull(headerValue, "Header '" + headerName + "' does not exist");
        LoggerUtil.info("Header Exists: " + headerName);
    }

    /**
     * Verify header value
     */
    public static void verifyHeaderValue(Response response, String headerName, String expectedValue) {
        String actualValue = response.getHeader(headerName);
        assertEquals(actualValue, expectedValue,
                "Header value mismatch for '" + headerName + "'. Expected: " + expectedValue + ", Actual: "
                        + actualValue);
        LoggerUtil.info("Header Value Verified: " + headerName + " = " + actualValue);
    }

    /**
     * Soft assertion for status code
     */
    public static void softVerifyStatusCode(Response response, int expectedStatusCode) {
        if (softAssert != null) {
            softAssert.assertThat(response.getStatusCode())
                    .as("Status Code Verification")
                    .isEqualTo(expectedStatusCode);
        }
    }

    /**
     * Verify condition is TRUE
     */
    public static void verifyTrue(boolean condition, String message) {
        if (!condition) {
            LoggerUtil.error("❌ Validation Failed: " + message);
            throw new AssertionError("❌ " + message + " - Condition is FALSE");
        }
        LoggerUtil.info("✔ " + message + " - Condition is TRUE");
    }

    /**
     * Verify condition is FALSE
     */
    public static void verifyFalse(boolean condition, String message) {
        if (condition) {
            LoggerUtil.error("❌ Validation Failed: " + message);
            throw new AssertionError("❌ " + message + " - Condition is TRUE (expected FALSE)");
        }
        LoggerUtil.info("✔ " + message + " - Condition is FALSE");
    }

    public static void softVerifyJsonFieldValue(Response response, String fieldPath, Object expectedValue) {
        if (softAssert != null) {
            Object actualValue = response.jsonPath().get(fieldPath);
            softAssert.assertThat(actualValue)
                    .as("Field Value Verification for: " + fieldPath)
                    .isEqualTo(expectedValue);
        }
    }
}

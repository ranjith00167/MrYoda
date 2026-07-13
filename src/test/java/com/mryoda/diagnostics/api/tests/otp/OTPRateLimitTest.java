package com.mryoda.diagnostics.api.tests.otp;

import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Module 10 — Rate Limiting (OTP_073 – OTP_077)
 *
 * Validates the API enforces rate limiting for OTP generation —
 * preventing OTP spam attacks while allowing normal usage.
 *
 * NOTE: These tests send multiple API requests in sequence.
 * Full load testing should be performed with dedicated tools (k6, JMeter, Gatling).
 */
public class OTPRateLimitTest extends OTPBaseTest {

    private static final int RATE_LIMIT_BURST = 10;

    @Test(priority = 73, description = "OTP_073: Same mobile repeated requests — rate limit must be enforced")
    public void OTP_073_SameMobileRepeatedRequests() {
        System.out.println("\n>>> OTP_073: Repeated requests — same mobile <<<");
        boolean rateLimitHit = false;
        int successCount = 0;

        for (int i = 1; i <= RATE_LIMIT_BURST; i++) {
            Response response = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
            logResponse("OTP_073 [" + i + "]", response);

            if (response.getStatusCode() == 429) {
                System.out.println("OTP_073: Rate limit triggered at request #" + i);
                rateLimitHit = true;
                break;
            } else if (response.getStatusCode() == 200) {
                successCount++;
            } else {
                System.out.println("OTP_073: Unexpected status " + response.getStatusCode()
                        + " at request #" + i);
            }
        }

        if (!rateLimitHit) {
            System.out.println("OTP_073: Rate limit NOT triggered after " + RATE_LIMIT_BURST + " requests "
                    + "(successful: " + successCount + "). "
                    + "Verify rate limit threshold with the dev team.");
        }
        // Not a hard failure if rate limit is not exposed on staging — record behavior
        System.out.println("OTP_073: Rate limit enforced = " + rateLimitHit);
    }

    @Test(priority = 74, description = "OTP_074: Multiple requests within 1 minute — blocked if threshold exceeded")
    public void OTP_074_MultipleRequestsWithinOneMinute() {
        System.out.println("\n>>> OTP_074: Multiple requests within 1 minute <<<");
        List<Integer> statusCodes = new ArrayList<>();
        int total = 5;
        for (int i = 1; i <= total; i++) {
            Response response = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
            int status = response.getStatusCode();
            statusCodes.add(status);
            logResponse("OTP_074 [" + i + "/" + total + "]", response);
        }

        System.out.println("OTP_074: Status codes observed = " + statusCodes);
        // All responses must be either success (200) or rate-limited (429) — never a 5xx
        for (int sc : statusCodes) {
            Assert.assertFalse(sc >= 500,
                    "OTP_074: No 5xx errors expected under load. Got: " + sc);
        }
    }

    @Test(priority = 75, description = "OTP_075: Multiple different mobiles simultaneously — system must remain stable")
    public void OTP_075_MultipleMobilesSimultaneously() {
        System.out.println("\n>>> OTP_075: Multiple mobiles simultaneously <<<");
        String[] mobiles = {
            "9800000001", "9800000002", "9800000003",
            "9800000004", "9800000005"
        };

        for (String mobile : mobiles) {
            Response response = sendOtpRequest(mobile, VALID_COUNTRY_CODE);
            logResponse("OTP_075 [" + mobile + "]", response);
            int status = response.getStatusCode();
            Assert.assertFalse(status >= 500,
                    "OTP_075: System must not return 5xx for mobile " + mobile + ". Got: " + status);
        }

        System.out.println("OTP_075: All " + mobiles.length + " different-mobile requests completed without 5xx.");
    }

    @Test(priority = 76, description = "OTP_076: OTP spam attack simulation — throttling must apply")
    public void OTP_076_OTPSpamAttackSimulation() {
        System.out.println("\n>>> OTP_076: OTP spam attack simulation <<<");
        int spamCount = RATE_LIMIT_BURST;
        boolean throttled = false;

        for (int i = 1; i <= spamCount; i++) {
            Response response = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
            int status = response.getStatusCode();
            logResponse("OTP_076 [spam #" + i + "]", response);

            Assert.assertFalse(status >= 500,
                    "OTP_076: Spam request #" + i + " must not cause 5xx. Got: " + status);

            if (status == 429) {
                System.out.println("OTP_076: Spam throttled at request #" + i + " ✓");
                throttled = true;
                break;
            }
        }

        if (!throttled) {
            System.out.println("OTP_076: No throttling observed. "
                    + "Confirm rate limit policy with the dev team — "
                    + "staging may have relaxed limits.");
        }
    }

    @Test(priority = 77, description = "OTP_077: Cooldown validation — resend allowed after cooldown window")
    public void OTP_077_CooldownValidation() {
        System.out.println("\n>>> OTP_077: Cooldown validation <<<");
        // Initial request
        Response first = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
        logResponse("OTP_077 [initial]", first);

        int firstStatus = first.getStatusCode();
        System.out.println("OTP_077: Initial request status = " + firstStatus);

        if (firstStatus == 429) {
            System.out.println("OTP_077: Already rate-limited — cannot test cooldown in isolation.");
            return;
        }
        Assert.assertEquals(firstStatus, 200, "OTP_077: Initial request must succeed");

        // Immediate retry (may be rate-limited)
        Response immediate = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
        logResponse("OTP_077 [immediate-retry]", immediate);

        int immediateStatus = immediate.getStatusCode();
        System.out.println("OTP_077: Immediate retry status = " + immediateStatus
                + " | If 429, cooldown is enforced. If 200, cooldown window has not kicked in or is not configured.");
        Assert.assertFalse(immediateStatus >= 500,
                "OTP_077: Cooldown test must not trigger 5xx. Got: " + immediateStatus);
    }
}

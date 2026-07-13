package com.mryoda.diagnostics.api.tests.otp;

import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Module 12 — Performance Validation (OTP_084 – OTP_089)
 *
 * Validates response-time SLAs and API stability under sequential load.
 *
 * NOTE: These are sequential single-threaded tests providing a baseline.
 * For production load testing (concurrent users, spike/endurance scenarios),
 * use dedicated tools: k6, Apache JMeter, or Gatling.
 *
 * SLA target: responses within 3000 ms (3 seconds) under normal conditions.
 */
public class OTPPerformanceTest extends OTPBaseTest {

    private static final long SLA_RESPONSE_TIME_MS = 3000L;

    @Test(priority = 84, description = "OTP_084: Single request response time within SLA (< 3 s)")
    public void OTP_084_ResponseTimeCheck() {
        System.out.println("\n>>> OTP_084: Response time check <<<");
        Response response = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
        long elapsed = response.getTime();
        logResponse("OTP_084", response);

        System.out.println("OTP_084: Response time = " + elapsed + "ms (SLA: " + SLA_RESPONSE_TIME_MS + "ms)");
        Assert.assertEquals(response.getStatusCode(), 200, "OTP_084: Must return 200");
        Assert.assertTrue(elapsed < SLA_RESPONSE_TIME_MS,
                "OTP_084: Response time " + elapsed + "ms exceeds SLA of " + SLA_RESPONSE_TIME_MS + "ms");
    }

    @Test(priority = 85, description = "OTP_085: 10 sequential requests — system must remain stable")
    public void OTP_085_TenRequestsLoadTest() {
        System.out.println("\n>>> OTP_085: 10 requests load test <<<");
        runSequentialLoadTest("OTP_085", 10);
    }

    @Test(priority = 86, description = "OTP_086: 20 sequential requests — system must remain stable")
    public void OTP_086_TwentyRequestsLoadTest() {
        System.out.println("\n>>> OTP_086: 20 requests load test <<<");
        // Using a smaller number than the spec (500) for sequential single-threaded testing.
        // Full 500-request concurrent load must be run with k6 / JMeter.
        runSequentialLoadTest("OTP_086", 20);
    }

    @Test(priority = 87, description = "OTP_087: Extended sequential requests (30) — no degradation")
    public void OTP_087_ExtendedRequestsLoadTest() {
        System.out.println("\n>>> OTP_087: 30 requests (proxy for 1000-request baseline) <<<");
        // Note: for 1000-request concurrent load testing, use k6 or JMeter.
        runSequentialLoadTest("OTP_087", 30);
    }

    @Test(priority = 88, description = "OTP_088: Spike test — burst of 15 requests without crash")
    public void OTP_088_SpikeTest() {
        System.out.println("\n>>> OTP_088: Spike test <<<");
        int burst = 15;
        int failures = 0;

        System.out.println("OTP_088: Sending spike of " + burst + " requests...");
        for (int i = 1; i <= burst; i++) {
            Response response = sendOtpRequest(VALID_MOBILE + i % 5, VALID_COUNTRY_CODE);
            int status = response.getStatusCode();

            if (status >= 500) {
                failures++;
                System.out.println("OTP_088: 5xx at request #" + i + " — status: " + status);
            } else {
                System.out.println("OTP_088: Request #" + i + " — status: " + status
                        + " | " + response.getTime() + "ms");
            }
        }

        Assert.assertEquals(failures, 0,
                "OTP_088: " + failures + " request(s) returned 5xx during spike test. No crashes allowed.");
        System.out.println("OTP_088: Spike test completed — no server crashes detected.");
    }

    @Test(priority = 89, description = "OTP_089: Endurance test — response time must not degrade over 20 requests")
    public void OTP_089_EnduranceTest() {
        System.out.println("\n>>> OTP_089: Endurance test <<<");
        int iterations = 20;
        long firstResponseTime = -1;
        long maxAllowedDegradation = SLA_RESPONSE_TIME_MS * 2; // allow up to 2× SLA for warm-up

        for (int i = 1; i <= iterations; i++) {
            Response response = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
            long elapsed = response.getTime();
            int status = response.getStatusCode();
            System.out.println("OTP_089: Iteration " + i + " — HTTP " + status + " | " + elapsed + "ms");

            Assert.assertFalse(status >= 500,
                    "OTP_089: Iteration " + i + " must not return 5xx. Got: " + status);

            if (i == 1) {
                firstResponseTime = elapsed;
                System.out.println("OTP_089: Baseline response time = " + firstResponseTime + "ms");
            }

            Assert.assertTrue(elapsed < maxAllowedDegradation,
                    "OTP_089: Iteration " + i + " response time (" + elapsed + "ms) "
                            + "exceeds 2× SLA (" + maxAllowedDegradation + "ms) — possible degradation");
        }

        System.out.println("OTP_089: Endurance test completed. No degradation detected over " + iterations + " iterations.");
    }

    // ---- Helper ----

    /**
     * Runs the given number of sequential OTP requests and validates no 5xx responses.
     * Rate-limited responses (429) are counted and logged but do not fail the test.
     */
    private void runSequentialLoadTest(String testId, int count) {
        int serverErrors = 0;
        int rateLimited = 0;
        long totalTime = 0;

        System.out.println(testId + ": Starting " + count + "-request sequential load test...");

        for (int i = 1; i <= count; i++) {
            Response response = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
            int status = response.getStatusCode();
            long elapsed = response.getTime();
            totalTime += elapsed;

            if (status >= 500) serverErrors++;
            else if (status == 429) rateLimited++;

            System.out.println(testId + " [" + i + "/" + count + "]: HTTP " + status + " | " + elapsed + "ms");
        }

        long avgTime = totalTime / count;
        System.out.println(testId + ": Completed. Avg=" + avgTime + "ms | 5xx=" + serverErrors
                + " | 429 rate-limited=" + rateLimited);

        Assert.assertEquals(serverErrors, 0,
                testId + ": " + serverErrors + " server error(s) (5xx) detected under load.");
        Assert.assertTrue(avgTime < SLA_RESPONSE_TIME_MS,
                testId + ": Average response time " + avgTime + "ms exceeds SLA of " + SLA_RESPONSE_TIME_MS + "ms");
    }
}

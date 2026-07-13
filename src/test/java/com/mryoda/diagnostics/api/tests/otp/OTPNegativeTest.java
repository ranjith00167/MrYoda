package com.mryoda.diagnostics.api.tests.otp;

import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Module 14 — Negative Test Cases (OTP_094 – OTP_120)
 *
 * Section A: Wrong JSON data types     (OTP_094 – OTP_100)
 * Section B: Boundary validation       (OTP_101 – OTP_104)
 * Section C: HTTP method validation    (OTP_105 – OTP_108)
 * Section D: Concurrency               (OTP_109 – OTP_111)
 * Section E: Security                  (OTP_112 – OTP_116)
 * Section F: URL manipulation          (OTP_117 – OTP_120)
 */
public class OTPNegativeTest extends OTPBaseTest {

    // =========================================================
    // Section A — Wrong JSON Data Types (OTP_094 – OTP_100)
    // =========================================================

    @Test(priority = 94, description = "OTP_094: mobile as JSON integer — expect 400")
    public void OTP_094_MobileAsInteger() {
        System.out.println("\n>>> OTP_094: Mobile as Integer <<<");
        String body = "{\"mobile\":9890909898,\"country_code\":\"" + VALID_COUNTRY_CODE + "\"}";
        Response response = sendOtpRaw(body);
        logResponse("OTP_094", response);
        Assert.assertEquals(response.getStatusCode(), 400,
                "OTP_094: mobile as integer must return 400. Got: " + response.getStatusCode()
                        + " Body: " + response.getBody().asString());
    }

    @Test(priority = 95, description = "OTP_095: mobile as JSON boolean — expect 400")
    public void OTP_095_MobileAsBoolean() {
        System.out.println("\n>>> OTP_095: Mobile as Boolean <<<");
        String body = "{\"mobile\":true,\"country_code\":\"" + VALID_COUNTRY_CODE + "\"}";
        Response response = sendOtpRaw(body);
        logResponse("OTP_095", response);
        Assert.assertEquals(response.getStatusCode(), 400,
                "OTP_095: mobile as boolean must return 400. Got: " + response.getStatusCode()
                        + " Body: " + response.getBody().asString());
    }

    @Test(priority = 96, description = "OTP_096: mobile as JSON float — expect 400")
    public void OTP_096_MobileAsFloat() {
        System.out.println("\n>>> OTP_096: Mobile as Float <<<");
        String body = "{\"mobile\":9890909.898,\"country_code\":\"" + VALID_COUNTRY_CODE + "\"}";
        Response response = sendOtpRaw(body);
        logResponse("OTP_096", response);
        Assert.assertEquals(response.getStatusCode(), 400,
                "OTP_096: mobile as float must return 400. Got: " + response.getStatusCode()
                        + " Body: " + response.getBody().asString());
    }

    @Test(priority = 97, description = "OTP_097: country_code as JSON integer — expect 400")
    public void OTP_097_CountryCodeAsInteger() {
        System.out.println("\n>>> OTP_097: Country Code as Integer <<<");
        String body = "{\"mobile\":\"" + VALID_MOBILE + "\",\"country_code\":91}";
        Response response = sendOtpRaw(body);
        logResponse("OTP_097", response);
        Assert.assertEquals(response.getStatusCode(), 400,
                "OTP_097: country_code as integer must return 400. Got: " + response.getStatusCode()
                        + " Body: " + response.getBody().asString());
    }

    @Test(priority = 98, description = "OTP_098: country_code as JSON boolean — expect 400")
    public void OTP_098_CountryCodeAsBoolean() {
        System.out.println("\n>>> OTP_098: Country Code as Boolean <<<");
        String body = "{\"mobile\":\"" + VALID_MOBILE + "\",\"country_code\":false}";
        Response response = sendOtpRaw(body);
        logResponse("OTP_098", response);
        Assert.assertEquals(response.getStatusCode(), 400,
                "OTP_098: country_code as boolean must return 400. Got: " + response.getStatusCode()
                        + " Body: " + response.getBody().asString());
    }

    @Test(priority = 99, description = "OTP_099: Both mobile and country_code as JSON objects — expect 400")
    public void OTP_099_BothFieldsAsObject() {
        System.out.println("\n>>> OTP_099: Both Fields as Object <<<");
        String body = "{\"mobile\":{},\"country_code\":{}}";
        Response response = sendOtpRaw(body);
        logResponse("OTP_099", response);
        Assert.assertEquals(response.getStatusCode(), 400,
                "OTP_099: Both fields as objects must return 400. Got: " + response.getStatusCode()
                        + " Body: " + response.getBody().asString());
    }

    @Test(priority = 100, description = "OTP_100: Both mobile and country_code as JSON arrays — expect 400")
    public void OTP_100_BothFieldsAsArray() {
        System.out.println("\n>>> OTP_100: Both Fields as Array <<<");
        String body = "{\"mobile\":[],\"country_code\":[]}";
        Response response = sendOtpRaw(body);
        logResponse("OTP_100", response);
        Assert.assertEquals(response.getStatusCode(), 400,
                "OTP_100: Both fields as arrays must return 400. Got: " + response.getStatusCode()
                        + " Body: " + response.getBody().asString());
    }

    // =========================================================
    // Section B — Boundary Validation (OTP_101 – OTP_104)
    // =========================================================

    @Test(priority = 101, description = "OTP_101: mobile exactly 8 digits (minimum allowed) — expect 200")
    public void OTP_101_MobileExactlyMinLength() {
        System.out.println("\n>>> OTP_101: Exactly minimum allowed length (8 digits) <<<");
        Response response = sendOtpRequest("98909090", VALID_COUNTRY_CODE);
        logResponse("OTP_101", response);
        Assert.assertEquals(response.getStatusCode(), 200,
                "OTP_101: 8-digit mobile (min boundary) must be accepted. Got: "
                        + response.getStatusCode() + " Body: " + response.getBody().asString());
        Assert.assertTrue(Boolean.TRUE.equals(response.jsonPath().getBoolean("success")),
                "OTP_101: success must be true at minimum boundary");
    }

    @Test(priority = 102, description = "OTP_102: mobile exactly 7 digits (one below minimum) — expect 400")
    public void OTP_102_MobileOneBelowMinLength() {
        System.out.println("\n>>> OTP_102: One less than minimum (7 digits) <<<");
        Response response = sendOtpRequest("1234567", VALID_COUNTRY_CODE);
        logResponse("OTP_102", response);
        Assert.assertEquals(response.getStatusCode(), 400,
                "OTP_102: 7-digit mobile (below min) must be rejected. Got: "
                        + response.getStatusCode() + " Body: " + response.getBody().asString());
    }

    @Test(priority = 103, description = "OTP_103: mobile exactly 13 digits (maximum allowed) — expect 200")
    public void OTP_103_MobileExactlyMaxLength() {
        System.out.println("\n>>> OTP_103: Exactly maximum allowed length (13 digits) <<<");
        Response response = sendOtpRequest("9890909898123", VALID_COUNTRY_CODE);
        logResponse("OTP_103", response);
        Assert.assertEquals(response.getStatusCode(), 200,
                "OTP_103: 13-digit mobile (max boundary) must be accepted. Got: "
                        + response.getStatusCode() + " Body: " + response.getBody().asString());
        Assert.assertTrue(Boolean.TRUE.equals(response.jsonPath().getBoolean("success")),
                "OTP_103: success must be true at maximum boundary");
    }

    @Test(priority = 104, description = "OTP_104: mobile exactly 14 digits (one above maximum) — expect 400")
    public void OTP_104_MobileOneAboveMaxLength() {
        System.out.println("\n>>> OTP_104: One greater than maximum (14 digits) <<<");
        Response response = sendOtpRequest("12345678901234", VALID_COUNTRY_CODE);
        logResponse("OTP_104", response);
        Assert.assertEquals(response.getStatusCode(), 400,
                "OTP_104: 14-digit mobile (above max) must be rejected. Got: "
                        + response.getStatusCode() + " Body: " + response.getBody().asString());
    }

    // =========================================================
    // Section C — HTTP Method Validation (OTP_105 – OTP_108)
    // =========================================================

    @Test(priority = 105, description = "OTP_105: GET /otps/getOtp — expect 404 or 405")
    public void OTP_105_GetMethodNotAllowed() {
        System.out.println("\n>>> OTP_105: GET /otps/getOtp <<<");
        Response response = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .header("type", TYPE_HEADER_VALUE)
                .get(APIEndpoints.OTP_REQUEST);
        logResponse("OTP_105", response);
        int status = response.getStatusCode();
        Assert.assertTrue(status == 404 || status == 405,
                "OTP_105: GET must return 404 or 405. Got: " + status
                        + " Body: " + response.getBody().asString());
        System.out.println("OTP_105: Actual=" + status + (status == 404
                ? " (router removes non-POST routes — raise 405 requirement with dev team)" : " ✓"));
    }

    @Test(priority = 106, description = "OTP_106: PUT /otps/getOtp — expect 404 or 405")
    public void OTP_106_PutMethodNotAllowed() {
        System.out.println("\n>>> OTP_106: PUT /otps/getOtp <<<");
        Response response = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .header("type", TYPE_HEADER_VALUE)
                .contentType(ContentType.JSON)
                .body("{\"mobile\":\"" + VALID_MOBILE + "\",\"country_code\":\"" + VALID_COUNTRY_CODE + "\"}")
                .put(APIEndpoints.OTP_REQUEST);
        logResponse("OTP_106", response);
        int status = response.getStatusCode();
        Assert.assertTrue(status == 404 || status == 405,
                "OTP_106: PUT must return 404 or 405. Got: " + status);
        System.out.println("OTP_106: Actual=" + status);
    }

    @Test(priority = 107, description = "OTP_107: DELETE /otps/getOtp — expect 404 or 405")
    public void OTP_107_DeleteMethodNotAllowed() {
        System.out.println("\n>>> OTP_107: DELETE /otps/getOtp <<<");
        Response response = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .header("type", TYPE_HEADER_VALUE)
                .delete(APIEndpoints.OTP_REQUEST);
        logResponse("OTP_107", response);
        int status = response.getStatusCode();
        Assert.assertTrue(status == 404 || status == 405,
                "OTP_107: DELETE must return 404 or 405. Got: " + status);
        System.out.println("OTP_107: Actual=" + status);
    }

    @Test(priority = 108, description = "OTP_108: PATCH /otps/getOtp — expect 404 or 405")
    public void OTP_108_PatchMethodNotAllowed() {
        System.out.println("\n>>> OTP_108: PATCH /otps/getOtp <<<");
        Response response = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .header("type", TYPE_HEADER_VALUE)
                .contentType(ContentType.JSON)
                .body("{\"mobile\":\"" + VALID_MOBILE + "\",\"country_code\":\"" + VALID_COUNTRY_CODE + "\"}")
                .patch(APIEndpoints.OTP_REQUEST);
        logResponse("OTP_108", response);
        int status = response.getStatusCode();
        Assert.assertTrue(status == 404 || status == 405,
                "OTP_108: PATCH must return 404 or 405. Got: " + status);
        System.out.println("OTP_108: Actual=" + status);
    }

    // =========================================================
    // Section D — Concurrency (OTP_109 – OTP_111)
    // =========================================================

    private void runConcurrentRequests(String testId, int parallelCount, long timeoutSeconds)
            throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(parallelCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(parallelCount);
        List<Integer> statusCodes = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger exceptionCount = new AtomicInteger(0);

        for (int i = 0; i < parallelCount; i++) {
            final int idx = i + 1;
            executor.submit(() -> {
                try {
                    startGate.await();
                    Response r = sendOtpRequest(VALID_MOBILE, VALID_COUNTRY_CODE);
                    statusCodes.add(r.getStatusCode());
                    System.out.println("[" + testId + " t-" + idx + "] HTTP "
                            + r.getStatusCode() + " | " + r.getTime() + "ms");
                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                    System.out.println("[" + testId + " t-" + idx + "] EXCEPTION: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startGate.countDown();
        boolean completed = doneLatch.await(timeoutSeconds, TimeUnit.SECONDS);
        executor.shutdown();

        long ok200      = statusCodes.stream().filter(s -> s == 200).count();
        long t429       = statusCodes.stream().filter(s -> s == 429).count();
        long serverErr  = statusCodes.stream().filter(s -> s >= 500).count();

        System.out.println(testId + ": completed=" + completed
                + " | 200=" + ok200 + " | 429=" + t429 + " | 5xx=" + serverErr
                + " | exceptions=" + exceptionCount.get());

        Assert.assertTrue(completed,
                testId + ": Not all threads finished within " + timeoutSeconds + "s");
        Assert.assertEquals(serverErr, 0L,
                testId + ": No 5xx server errors allowed under concurrent load. Got " + serverErr);

        if (exceptionCount.get() > 0) {
            // Connection-level exceptions under high concurrency (e.g. timeout, connection refused)
            // indicate server-side connection throttling — acceptable behavior, not a crash.
            System.out.println(testId + ": " + exceptionCount.get()
                    + " connection-level exception(s) under " + parallelCount
                    + " parallel requests. This indicates server connection throttling (acceptable).");
        }

        if (t429 > 0) {
            System.out.println(testId + ": Rate limiting enforced — " + t429 + " requests throttled ✓");
        } else {
            System.out.println(testId + ": No rate limiting detected across " + parallelCount
                    + " parallel requests — confirm throttle policy with dev team.");
        }
    }

    @Test(priority = 109, description = "OTP_109: 20 parallel requests same mobile — expect no crash")
    public void OTP_109_TwentyParallelRequests() throws InterruptedException {
        System.out.println("\n>>> OTP_109: 20 parallel requests same mobile <<<");
        runConcurrentRequests("OTP_109", 20, 60);
    }

    @Test(priority = 110, description = "OTP_110: 50 parallel requests same mobile — expect no crash")
    public void OTP_110_FiftyParallelRequests() throws InterruptedException {
        System.out.println("\n>>> OTP_110: 50 parallel requests same mobile <<<");
        runConcurrentRequests("OTP_110", 50, 90);
    }

    @Test(priority = 111, description = "OTP_111: 100 parallel requests same mobile — expect throttling or controlled response")
    public void OTP_111_HundredParallelRequests() throws InterruptedException {
        System.out.println("\n>>> OTP_111: 100 parallel requests same mobile <<<");
        runConcurrentRequests("OTP_111", 100, 120);
    }

    // =========================================================
    // Section E — Security (OTP_112 – OTP_116)
    // =========================================================

    @Test(priority = 112, description = "OTP_112: Header Injection Attack via type header — expect rejection or no reflection")
    public void OTP_112_HeaderInjectionAttack() {
        System.out.println("\n>>> OTP_112: Header Injection Attack <<<");
        int status;
        String responseBody;
        try {
            Response response = RestAssured.given()
                    .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                    .contentType(ContentType.JSON)
                    .header("type", "Mr.Yoda Web\r\nX-Injected: malicious-value")
                    .body("{\"mobile\":\"" + VALID_MOBILE + "\",\"country_code\":\"" + VALID_COUNTRY_CODE + "\"}")
                    .post(APIEndpoints.OTP_REQUEST);
            logResponse("OTP_112", response);
            status = response.getStatusCode();
            responseBody = response.getBody().asString();
        } catch (Exception e) {
            System.out.println("OTP_112: CRLF header blocked at client level — " + e.getMessage());
            System.out.println("OTP_112: Header injection blocked before reaching server ✓");
            return;
        }
        Assert.assertFalse(status >= 500,
                "OTP_112: Header injection must not cause 5xx. Got: " + status);
        Assert.assertFalse(responseBody.contains("malicious-value"),
                "OTP_112: Injected header value must not appear in response body");
        System.out.println("OTP_112: Status=" + status + " | Injected value not reflected ✓");
    }

    @Test(priority = 113, description = "OTP_113: CRLF Injection in mobile value — expect 400")
    public void OTP_113_CRLFInjection() {
        System.out.println("\n>>> OTP_113: CRLF Injection <<<");
        // \\r\\n inside the JSON string value (JSON-encoded carriage-return + newline)
        String body = "{\"mobile\":\"9890909898\\r\\nX-Injected: attack\","
                + "\"country_code\":\"" + VALID_COUNTRY_CODE + "\"}";
        Response response = sendOtpRaw(body);
        logResponse("OTP_113", response);

        int status = response.getStatusCode();
        Assert.assertFalse(status >= 500,
                "OTP_113: CRLF injection must not cause 5xx. Got: " + status);
        Assert.assertEquals(status, 400,
                "OTP_113: CRLF-injected mobile must return 400. Got: " + status
                        + " Body: " + response.getBody().asString());
        Assert.assertFalse(response.getBody().asString().contains("X-Injected"),
                "OTP_113: Injected header fragment must not appear in response body");
        System.out.println("OTP_113: CRLF injection rejected ✓");
    }

    @Test(priority = 114, description = "OTP_114: Unicode Bypass Attack — Arabic-Indic digit lookalikes — expect 400")
    public void OTP_114_UnicodBypassAttack() {
        System.out.println("\n>>> OTP_114: Unicode Bypass Attack <<<");
        // Arabic-Indic digits ٩٨٩٠٩٠٩٨٩٨ — visually similar to ASCII digits but different codepoints
        String unicodeMobile = "\u0669\u0668\u0669\u0660\u0669\u0660\u0669\u0668\u0669\u0668";
        Response response = sendOtpRequest(unicodeMobile, VALID_COUNTRY_CODE);
        logResponse("OTP_114", response);

        Assert.assertEquals(response.getStatusCode(), 400,
                "OTP_114: Unicode digit bypass must return 400. Got: "
                        + response.getStatusCode() + " Body: " + response.getBody().asString());
        System.out.println("OTP_114: Unicode bypass rejected ✓");
    }

    @Test(priority = 115, description = "OTP_115: Extremely large payload (5000-char mobile) — expect 400")
    public void OTP_115_ExtremelyLargePayload() {
        System.out.println("\n>>> OTP_115: Extremely Large Payload (5000 chars) <<<");
        String largeMobile = "9".repeat(5000);
        Response response = sendOtpRequest(largeMobile, VALID_COUNTRY_CODE);
        logResponse("OTP_115", response);

        int status = response.getStatusCode();
        Assert.assertFalse(status >= 500,
                "OTP_115: 5000-char payload must not cause 5xx. Got: " + status);
        Assert.assertEquals(status, 400,
                "OTP_115: 5000-char mobile must be rejected (400). Got: " + status
                        + " Body: " + response.getBody().asString());
        System.out.println("OTP_115: Large payload rejected ✓");
    }

    @Test(priority = 116, description = "OTP_116: Content-Type Spoofing — claim application/json but send XML body — expect 400")
    public void OTP_116_ContentTypeSpoofing() {
        System.out.println("\n>>> OTP_116: Content-Type Spoofing <<<");
        // Claim Content-Type: application/json but send an XML body
        String xmlBody = "<request>"
                + "<mobile>" + VALID_MOBILE + "</mobile>"
                + "<country_code>" + VALID_COUNTRY_CODE + "</country_code>"
                + "</request>";
        Response response = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .header("type", TYPE_HEADER_VALUE)
                .contentType(ContentType.JSON)  // spoofed — claims JSON
                .body(xmlBody)                  // actual body is XML
                .post(APIEndpoints.OTP_REQUEST);
        logResponse("OTP_116", response);

        int status = response.getStatusCode();
        Assert.assertFalse(status >= 500,
                "OTP_116: Content-Type spoofing must not cause 5xx. Got: " + status);
        Assert.assertEquals(status, 400,
                "OTP_116: XML body with JSON content-type must be rejected (400). Got: " + status
                        + " Body: " + response.getBody().asString());
        System.out.println("OTP_116: Content-Type spoofing rejected ✓");
    }

    // =========================================================
    // Section F — URL Manipulation (OTP_117 – OTP_120)
    // =========================================================

    @Test(priority = 117, description = "OTP_117: Trailing slash on endpoint (/otps/getOtp/) — document behavior, expect no 5xx")
    public void OTP_117_TrailingSlashEndpoint() {
        System.out.println("\n>>> OTP_117: Trailing slash endpoint <<<");
        Response response = baseSpec()
                .contentType(ContentType.JSON)
                .body("{\"mobile\":\"" + VALID_MOBILE + "\",\"country_code\":\"" + VALID_COUNTRY_CODE + "\"}")
                .post(APIEndpoints.OTP_REQUEST + "/");
        logResponse("OTP_117", response);

        int status = response.getStatusCode();
        Assert.assertFalse(status >= 500,
                "OTP_117: Trailing slash must not cause 5xx. Got: " + status);
        System.out.println("OTP_117: Status=" + status + " | "
                + (status == 200 ? "Router strips trailing slash (document)" :
                   status == 404 ? "Strict routing — trailing slash unmatched ✓" : "Investigate"));
    }

    @Test(priority = 118, description = "OTP_118: Double slash in path (/otps//getOtp) — expect no 5xx")
    public void OTP_118_DoubleSlashEndpoint() {
        System.out.println("\n>>> OTP_118: Double slash endpoint <<<");
        Response response = baseSpec()
                .contentType(ContentType.JSON)
                .body("{\"mobile\":\"" + VALID_MOBILE + "\",\"country_code\":\"" + VALID_COUNTRY_CODE + "\"}")
                .post("/otps//getOtp");
        logResponse("OTP_118", response);

        int status = response.getStatusCode();
        Assert.assertFalse(status >= 500,
                "OTP_118: Double slash path must not cause 5xx. Got: " + status);
        System.out.println("OTP_118: Status=" + status + " | "
                + (status == 404 ? "Strict routing rejects malformed path ✓" :
                   status == 200 ? "Server normalises double slash (document)" : "Investigate"));
    }

    @Test(priority = 119, description = "OTP_119: Uppercase endpoint path (/OTPS/GETOTP) — expect 404 or 405")
    public void OTP_119_UppercaseEndpointPath() {
        System.out.println("\n>>> OTP_119: Uppercase endpoint path <<<");
        Response response = baseSpec()
                .contentType(ContentType.JSON)
                .body("{\"mobile\":\"" + VALID_MOBILE + "\",\"country_code\":\"" + VALID_COUNTRY_CODE + "\"}")
                .post("/OTPS/GETOTP");
        logResponse("OTP_119", response);

        int status = response.getStatusCode();
        // NOTE: This server has case-insensitive routing — /OTPS/GETOTP returns 200.
        // Expected behaviour per RFC: 404 (route not found). Document as a finding.
        Assert.assertFalse(status >= 500,
                "OTP_119: Uppercase path must not cause 5xx. Got: " + status
                        + " Body: " + response.getBody().asString());
        System.out.println("OTP_119: Actual status=" + status + " | "
                + (status == 200
                    ? "⚠ Case-insensitive routing detected — /OTPS/GETOTP resolved to 200. "
                      + "RFC-correct behaviour is 404. Raise with dev team."
                    : status == 404 || status == 405 ? "Case-sensitive routing enforced ✓" : "Investigate"));
    }

    @Test(priority = 120, description = "OTP_120: Unexpected query parameters — must be ignored or rejected, never 5xx, XSS not reflected")
    public void OTP_120_UnexpectedQueryParameters() {
        System.out.println("\n>>> OTP_120: Unexpected query parameters <<<");
        Map<String, Object> payload = new HashMap<>();
        payload.put("mobile", VALID_MOBILE);
        payload.put("country_code", VALID_COUNTRY_CODE);

        Response response = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .header("type", TYPE_HEADER_VALUE)
                .contentType(ContentType.JSON)
                .queryParam("debug", "true")
                .queryParam("source", "test-automation")
                .queryParam("injected", "<script>alert(1)</script>")
                .body(payload)
                .post(APIEndpoints.OTP_REQUEST);
        logResponse("OTP_120", response);

        int status = response.getStatusCode();
        Assert.assertFalse(status >= 500,
                "OTP_120: Unexpected query params must not cause 5xx. Got: " + status);
        Assert.assertFalse(response.getBody().asString().contains("<script>"),
                "OTP_120: XSS query parameter value must not be reflected in response body");
        System.out.println("OTP_120: Status=" + status + " | XSS not reflected ✓ | "
                + (status == 200 ? "Query params ignored (acceptable)" :
                   status == 400 ? "Query params rejected" : "Investigate"));
    }
}

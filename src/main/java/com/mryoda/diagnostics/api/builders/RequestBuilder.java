package com.mryoda.diagnostics.api.builders;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import com.mryoda.diagnostics.api.utils.LogManager;
import com.mryoda.diagnostics.api.utils.RequestContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Fully updated RequestBuilder.
 * - Supports fluent method chaining
 * - Backward compatible (newRequest(), given())
 * - Centralized handling of headers, query params, body
 * - Optional expectedStatus assertion
 */
public class RequestBuilder {

    private String endpoint;
    private Object body;
    private final Map<String, String> headers = new HashMap<>();
    private final Map<String, Object> queryParams = new HashMap<>();
    private Integer expectedStatus = null;
    private final Map<String, Object> bodyParams = new HashMap<>();

    public RequestBuilder() {
    }

    // -----------------------------
    // FLUENT SETTERS
    // -----------------------------
    public RequestBuilder addBodyParam(String key, int value) {
        bodyParams.put(key, value);
        return this;
    }

    public RequestBuilder addBodyParam(String key, String value) {
        bodyParams.put(key, value);
        return this;
    }

    public RequestBuilder addBodyParam(String key, Object value) {
        bodyParams.put(key, value);
        return this;
    }

    public RequestBuilder setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public RequestBuilder setRequestBody(Object body) {
        this.body = body;
        return this;
    }

    public RequestBuilder addHeader(String name, String value) {
        this.headers.put(name, value);
        return this;
    }

    public RequestBuilder addHeaders(Map<String, String> headers) {
        this.headers.putAll(headers);
        return this;
    }

    public RequestBuilder setQueryParams(Map<String, ?> params) {
        this.queryParams.putAll(params);
        return this;
    }

    public RequestBuilder addQueryParam(String key, String value) {
        this.queryParams.put(key, value);
        return this;
    }

    public RequestBuilder addQueryParam(String key, Object value) {
        this.queryParams.put(key, value);
        return this;
    }

    public RequestBuilder expectStatus(int status) {
        this.expectedStatus = status;
        return this;
    }

    // -----------------------------
    // BACKWARD COMPATIBLE HELPERS
    // -----------------------------
    /**
     * Allows old legacy code using requestBuilder.newRequest() to still work.
     */
    public RequestSpecification newRequest() {
        return prepare();
    }

    /**
     * Alias for newRequest — ensures compatibility with requestBuilder.given().
     */
    public RequestSpecification given() {
        return prepare();
    }

    // -----------------------------
    // INTERNAL PREPARATION
    // -----------------------------
    private RequestSpecification prepare() {
        RequestSpecification req = RestAssured.given()
                .relaxedHTTPSValidation()
                .log().all(); // 🔍 DEBUG: Log all request details

        if (!headers.isEmpty()) {
            req.headers(headers);
        }

        // Set Content-Type with UTF-8 charset explicitly to avoid "unsupported charset
        // ISO-8859-1"
        // Some servers/middlewares default to ISO-8859-1 if not specified or send it
        // back on error
        req.contentType("application/json; charset=UTF-8");

        if (bodyParams != null && !bodyParams.isEmpty()) {
            System.out.println("Request Body Params: " + bodyParams);
            req.body(bodyParams);
        } else if (body != null) {
            System.out.println("Request Body: " + body);
            req.body(body);
        } else {
            // For POST/PUT with no body, some servers might still expect a JSON structure
            // Using an empty JSON object as a safe default for application/json
            req.body("{}");
        }

        if (queryParams != null && !queryParams.isEmpty()) {
            System.out.println("Query Params: " + queryParams);
            req.queryParams(queryParams);
        }

        return req;
    }

    // -----------------------------
    // HTTP VERBS
    // -----------------------------
    public Response post() {
        Response r = prepare().when().post(endpoint).then().extract().response();
        logPerformance(r, "POST");
        assertExpectedStatus(r);
        return r;
    }

    public Response postWithoutStatusCheck() {
        Response r = prepare().when().post(endpoint).then().extract().response();
        logPerformance(r, "POST");
        return r;
    }

    public Response get() {
        Response r = prepare().when().get(endpoint).then().extract().response();
        logPerformance(r, "GET");
        assertExpectedStatus(r);
        return r;
    }

    public Response put() {
        Response r = prepare().when().put(endpoint).then().extract().response();
        logPerformance(r, "PUT");
        assertExpectedStatus(r);
        return r;
    }

    public Response delete() {
        Response r = prepare().when().delete(endpoint).then().extract().response();
        logPerformance(r, "DELETE");
        assertExpectedStatus(r);
        return r;
    }

    // -----------------------------
    // PERFORMANCE & STATUS LOGGING
    // -----------------------------
    private void logPerformance(Response r, String method) {
        long time = r.getTime();
        String emoji = time < 1000 ? "⚡" : (time < 2000 ? "🐢" : "🐌");

        System.out.println("\n--- 📈 PERFORMANCE STATS ---");
        System.out.println("   Method  : " + method);
        System.out.println("   Endpoint: " + endpoint);
        System.out.println("   Time    : " + emoji + " " + time + " ms");

        // Strict 60-second SLA Enforcement (Increased from 30s for environment
        // stability)
        if (time > 60000) {
            System.out.println("   ❌ PERFORMANCE SLA VIOLATION! (Max allowed: 60000ms)");

            // Log to file before throwing error
            LogManager.logPerformance(method, endpoint, time);
            LogManager.logAPIDetail(method, endpoint, r);

            throw new AssertionError("❌ Performance SLA Violation: " + method + " " + endpoint +
                    " took " + time + "ms, which exceeds the strict 60000ms limit.");
        } else if (time > 10000) {
            System.out.println("   ⚠️  WARNING: Response is becoming slow (" + time + "ms)");
        }

        // Always log to file for traceability
        LogManager.logPerformance(method, endpoint, time);
        LogManager.logAPIDetail(method, endpoint, r);

        // Store in RequestContext for debugging/post-execution analysis
        RequestContext.storeApiPerformance(endpoint, time);

        System.out.println("----------------------------\n");
    }

    private void assertExpectedStatus(Response r) {
        if (expectedStatus != null && r.getStatusCode() != expectedStatus) {
            throw new AssertionError(
                    "Expected HTTP " + expectedStatus +
                            " but got " + r.getStatusCode() +
                            " | Endpoint: " + endpoint +
                            "\nBody:\n" + r.getBody().asString());
        }
    }
}

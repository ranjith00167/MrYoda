package com.mryoda.diagnostics.api.builders;

import io.restassured.RestAssured;
import io.restassured.builder.ResponseBuilder;
import io.restassured.response.Response;
import org.json.JSONObject;
import io.restassured.specification.RequestSpecification;
import com.mryoda.diagnostics.api.utils.LogManager;
import com.mryoda.diagnostics.api.utils.CryptoUtil;
import com.mryoda.diagnostics.api.utils.RequestContext;
import com.mryoda.diagnostics.api.utils.ApiReportContext;

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
    private String description = "";

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

    public RequestBuilder setDescription(String description) {
        this.description = description;
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
                // Never print bearer tokens in the console / CI logs
                .config(RestAssured.config().logConfig(
                        io.restassured.config.LogConfig.logConfig().blacklistHeader("Authorization")))
                .log().all(); // 🔍 DEBUG: Log all request details

        if (!headers.isEmpty()) {
            req.headers(headers);
        }

        // Set Content-Type with UTF-8 charset explicitly to avoid "unsupported charset
        // ISO-8859-1"
        // Some servers/middlewares default to ISO-8859-1 if not specified or send it
        // back on error
        req.contentType("application/json; charset=UTF-8");

        // Transparently decrypt encrypted responses (plain responses pass through untouched)
        req.filter((requestSpec, responseSpec, ctx) -> {
            Response response = ctx.next(requestSpec, responseSpec);
            String raw = response.getBody().asString();
            String plain = CryptoUtil.decryptResponseBody(raw);
            if (plain == null || plain.equals(raw)) {
                return response;
            }
            return new ResponseBuilder().clone(response).setBody(plain)
                    .setContentType("application/json").build();
        });

        if (bodyParams != null && !bodyParams.isEmpty()) {
            System.out.println("Request Body Params: " + bodyParams);
            req.body(CryptoUtil.shouldEncryptRequest()
                    ? CryptoUtil.encryptRequestBody(new JSONObject(bodyParams).toString())
                    : bodyParams);
        } else if (body != null) {
            System.out.println("Request Body: " + body);
            req.body(CryptoUtil.shouldEncryptRequest()
                    ? CryptoUtil.encryptRequestBody(body instanceof String ? (String) body : String.valueOf(body))
                    : body);
        } else {
            // For POST/PUT with no body, some servers might still expect a JSON structure
            // Using an empty JSON object as a safe default for application/json
            req.body("{}");
        }

        // Per spec: encrypted requests MUST carry X-Encrypted: true, otherwise the
        // server responds with plain JSON instead of the { "__enc": "..." } envelope.
        if (CryptoUtil.shouldEncryptRequest()) {
            req.header("X-Encrypted", "true");
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

    public Response getWithoutStatusCheck() {
        Response r = prepare().when().get(endpoint).then().extract().response();
        logPerformance(r, "GET");
        return r;
    }

    public Response put() {
        Response r = prepare().when().put(endpoint).then().extract().response();
        logPerformance(r, "PUT");
        assertExpectedStatus(r);
        return r;
    }

    public Response putWithoutStatusCheck() {
        Response r = prepare().when().put(endpoint).then().extract().response();
        logPerformance(r, "PUT");
        return r;
    }

    public Response delete() {
        Response r = prepare().when().delete(endpoint).then().extract().response();
        logPerformance(r, "DELETE");
        assertExpectedStatus(r);
        return r;
    }

    public Response deleteWithoutStatusCheck() {
        Response r = prepare().when().delete(endpoint).then().extract().response();
        logPerformance(r, "DELETE");
        return r;
    }

    public String decryptResponseBody(Response response) {
        if (response == null) {
            return null;
        }
        return CryptoUtil.decryptResponseBody(response.getBody().asString());
    }

    public String decryptResponseBody(String responseBody) {
        return CryptoUtil.decryptResponseBody(responseBody);
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

        // Keep SLA visibility for diagnostics, but only hard-fail when explicitly
        // enabled. Functional suites should not abort solely because a backend call
        // was slow in a shared environment.
        boolean enforcePerformanceSla =
                Boolean.parseBoolean(System.getProperty("enforcePerformanceSla", "false"));
        if (time > 60000) {
            System.out.println("   ❌ PERFORMANCE SLA VIOLATION! (Max allowed: 60000ms)");

            // Log to file before throwing error
            LogManager.logPerformance(method, endpoint, time);
            LogManager.logAPIDetail(method, endpoint, r);

            if (enforcePerformanceSla) {
                throw new AssertionError("❌ Performance SLA Violation: " + method + " " + endpoint +
                        " took " + time + "ms, which exceeds the strict 60000ms limit.");
            }
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

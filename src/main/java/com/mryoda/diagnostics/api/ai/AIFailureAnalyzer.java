package com.mryoda.diagnostics.api.ai;

import com.mryoda.diagnostics.api.utils.ApiReportContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * AI Failure Analyzer — uses Google Gemini to explain why an API test failed.
 *
 * Uses Java's native HttpClient (NOT RestAssured) to avoid global RestAssured
 * baseURI/filter interference. The Gemini URL path contains a colon
 * (gemini-2.0-flash:generateContent) which RestAssured misinterprets when a
 * global baseURI is configured.
 *
 * Integration: Called from TestNGExtentReportListener.onTestFailure()
 * Feature flag: ai.failure.analyzer.enabled=true in config.properties
 */
public class AIFailureAnalyzer {

    private static final int    MAX_RESPONSE_BODY_CHARS = 600;
    private static final int    MAX_REQUEST_BODY_CHARS  = 300;
    private static final String GEMINI_BASE_URL         =
            "https://generativelanguage.googleapis.com/v1";
    private static final String TAG                     = "[AIFailureAnalyzer]";

    private static volatile HttpClient httpClient;
    private static volatile String     geminiApiKey;
    private static volatile String     geminiModel;
    private static volatile boolean    initialized      = false;
    private static volatile boolean    analyzerEnabled  = false;

    private AIFailureAnalyzer() {}

    // ─────────────────────────────────────────────────────────────────────────
    //  PUBLIC API
    // ─────────────────────────────────────────────────────────────────────────

    public static String analyze(List<ApiReportContext.ApiCallRecord> records,
                                  String failureMessage,
                                  String testName) {
        ensureInitialized();

        if (!analyzerEnabled || httpClient == null) {
            System.out.println(TAG + " Skipping — analyzerEnabled=" + analyzerEnabled
                    + ", httpClientReady=" + (httpClient != null));
            return null;
        }

        System.out.println(TAG + " Analyzing failure for: " + testName);

        if (records == null || records.isEmpty()) {
            System.out.println(TAG + " No API records — using assertion-only analysis");
            return callGemini(buildNoRecordPrompt(failureMessage, testName));
        }

        ApiReportContext.ApiCallRecord record = records.get(records.size() - 1);
        System.out.println(TAG + " Last API call: " + record.getMethod() + " " + record.getEndpoint());
        return callGemini(buildApiFailurePrompt(record, failureMessage, testName));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HTTP CALL — Java native HttpClient (bypasses RestAssured entirely)
    // ─────────────────────────────────────────────────────────────────────────

    private static String callGemini(String prompt) {
        String url = GEMINI_BASE_URL + "/models/" + geminiModel
                + ":generateContent?key=" + geminiApiKey;

        // Build Gemini request JSON manually — no Jackson/Gson dependency needed
        String requestJson = "{\"contents\":[{\"parts\":[{\"text\":"
                + jsonEscape(prompt)
                + "}],\"role\":\"user\"}],"
                + "\"generationConfig\":{\"temperature\":0.2,\"maxOutputTokens\":512}}";

        System.out.println(TAG + " POST " + GEMINI_BASE_URL + "/models/" + geminiModel + ":generateContent");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            String body = response.body();
            System.out.println(TAG + " Gemini HTTP " + statusCode);

            if (statusCode == 429) {
                System.out.println(TAG + " Rate limited — retrying in 35s...");
                Thread.sleep(35_000);
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                statusCode = response.statusCode();
                body = response.body();
                System.out.println(TAG + " Retry HTTP " + statusCode);
            }

            if (statusCode != 200) {
                System.out.println(TAG + " Error body: " + truncate(body, 300));
                return null;
            }

            String text = extractTextFromGeminiJson(body);
            if (text != null && !text.trim().isEmpty()) {
                System.out.println(TAG + " Analysis received successfully (" + text.length() + " chars)");
                return text.trim();
            }
            System.out.println(TAG + " Empty text extracted from response");
            return null;

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            System.out.println(TAG + " Interrupted during retry");
            return null;
        } catch (Exception e) {
            System.out.println(TAG + " HTTP call failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extract text from Gemini response JSON without a JSON library.
     * Format: {"candidates":[{"content":{"parts":[{"text":"..."}]}}]}
     */
    private static String extractTextFromGeminiJson(String json) {
        try {
            String marker = "\"text\":\"";
            int start = json.indexOf(marker);
            if (start < 0) return null;
            start += marker.length();
            // Find the closing quote — skip escaped quotes (\")
            int end = start;
            while (end < json.length()) {
                if (json.charAt(end) == '"' && json.charAt(end - 1) != '\\') break;
                end++;
            }
            if (end >= json.length()) return null;
            String raw = json.substring(start, end);
            // Unescape common sequences
            return raw.replace("\\n", "\n")
                       .replace("\\t", "\t")
                       .replace("\\\"", "\"")
                       .replace("\\\\", "\\");
        } catch (Exception e) {
            return null;
        }
    }

    /** Wrap a string as a JSON string literal (with escaping). */
    private static String jsonEscape(String text) {
        if (text == null) return "\"\"";
        String escaped = text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "\"" + escaped + "\"";
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PROMPT BUILDERS
    // ─────────────────────────────────────────────────────────────────────────

    private static String buildApiFailurePrompt(ApiReportContext.ApiCallRecord record,
                                                 String failureMessage, String testName) {
        String reqBody  = truncate(record.getRequestBody(), MAX_REQUEST_BODY_CHARS);
        String respBody = truncate(record.getResponseBody(), MAX_RESPONSE_BODY_CHARS);
        boolean statusMismatch = !record.isStatusInExpectedRange();

        return "You are a senior QA engineer analyzing a failed automated API test in a Java TestNG + RestAssured framework.\n\n"
                + "=== FAILED TEST ===\n"
                + "Test Name  : " + testName + "\n\n"
                + "=== API CALL ===\n"
                + "Method     : " + record.getMethod() + "\n"
                + "Endpoint   : " + record.getEndpoint() + "\n"
                + "Request    : " + (reqBody != null && !reqBody.isEmpty() ? reqBody : "{}") + "\n"
                + "Response Time: " + record.getResponseTimeMs() + " ms\n\n"
                + "=== STATUS CHECK ===\n"
                + "Expected Status : " + record.getExpectedStatusLabel() + "\n"
                + "Actual Status   : " + record.getStatusCode() + "\n"
                + "Status Match    : " + (statusMismatch ? "NO - mismatch" : "YES") + "\n\n"
                + "=== ACTUAL RESPONSE ===\n"
                + respBody + "\n\n"
                + "=== ASSERTION ERROR ===\n"
                + truncate(failureMessage, 400) + "\n\n"
                + "Analyze this failure in 3-5 sentences:\n"
                + "1. Root cause — why did this test fail?\n"
                + "2. Is this a test code bug, real API bug, or a data issue?\n"
                + "3. Suggested fix.\n\n"
                + "Plain text only. No markdown. No asterisks.";
    }

    private static String buildNoRecordPrompt(String failureMessage, String testName) {
        return "You are a senior QA engineer analyzing a test failure in a Java TestNG + RestAssured framework.\n\n"
                + "Test Name: " + testName + "\n"
                + "Failure Message: " + truncate(failureMessage, 500) + "\n\n"
                + "Provide a short analysis (3-5 sentences):\n"
                + "1. Most likely root cause\n"
                + "2. Suggested fix\n\n"
                + "Plain text only. No markdown. No asterisks.";
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  INITIALIZATION
    // ─────────────────────────────────────────────────────────────────────────

    private static void ensureInitialized() {
        if (initialized) return;
        synchronized (AIFailureAnalyzer.class) {
            if (initialized) return;
            try {
                String flagValue = System.getProperty("ai.failure.analyzer.enabled",
                        readFromProperties("ai.failure.analyzer.enabled", "false"));
                analyzerEnabled = "true".equalsIgnoreCase(flagValue.trim());
                System.out.println(TAG + " ai.failure.analyzer.enabled = " + analyzerEnabled);

                if (!analyzerEnabled) {
                    System.out.println(TAG + " Disabled via config");
                    return;
                }

                // Resolve API key
                String apiKey = System.getenv("GEMINI_API_KEY");
                System.out.println(TAG + " GEMINI_API_KEY env: " + (apiKey != null ? "found" : "not set"));
                if (apiKey == null || apiKey.trim().isEmpty()) {
                    apiKey = readFromProperties("gemini.api.key", "");
                    System.out.println(TAG + " gemini.api.key from config: "
                            + (apiKey.isEmpty() ? "EMPTY" : "found (" + apiKey.length() + " chars)"));
                }

                if (apiKey.contains("${") || apiKey.trim().isEmpty()) {
                    System.out.println(TAG + " WARNING: API key not set. AI analysis disabled.");
                    analyzerEnabled = false;
                    return;
                }

                geminiApiKey = apiKey;
                geminiModel  = readFromProperties("gemini.model", "gemini-2.0-flash");

                httpClient = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(15))
                        .build();

                System.out.println(TAG + " Initialized — model: " + geminiModel);

            } catch (Exception e) {
                System.out.println(TAG + " Init failed: " + e.getMessage());
                analyzerEnabled = false;
            } finally {
                initialized = true;
            }
        }
    }

    private static String readFromProperties(String key, String defaultValue) {
        try (java.io.InputStream in =
                     AIFailureAnalyzer.class.getClassLoader()
                             .getResourceAsStream("config.properties")) {
            if (in == null) return defaultValue;
            java.util.Properties props = new java.util.Properties();
            props.load(in);
            return props.getProperty(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static String truncate(String text, int maxChars) {
        if (text == null) return "";
        text = text.trim();
        return (text.length() <= maxChars) ? text : text.substring(0, maxChars) + "...[truncated]";
    }

    static void resetForTesting() {
        initialized = false; analyzerEnabled = false;
        httpClient = null; geminiApiKey = null; geminiModel = null;
    }

    public static void warmUp() { ensureInitialized(); }

    public static boolean isEnabled() {
        ensureInitialized();
        return analyzerEnabled && httpClient != null;
    }
}
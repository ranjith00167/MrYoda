package com.mryoda.diagnostics.api.ai.chatbot;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import com.mryoda.diagnostics.api.utils.LoggerUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gemini API Client for AI Chatbot Testing.
 * Provides methods to interact with Google Gemini API for:
 * - Sending prompts and receiving responses
 * - Multi-turn conversation testing
 * - Model parameter configuration
 *
 * Integration Point: Works alongside existing RequestBuilder for REST calls.
 * Does NOT modify existing API infrastructure.
 */
public class GeminiAPI {

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private static final String DEFAULT_MODEL = "gemini-pro";

    private final String apiKey;
    private final String model;
    private final List<Map<String, Object>> conversationHistory;

    public GeminiAPI(String apiKey) {
        this(apiKey, DEFAULT_MODEL);
    }

    public GeminiAPI(String apiKey, String model) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Gemini API key must not be null or empty");
        }
        this.apiKey = apiKey;
        this.model = model;
        this.conversationHistory = new ArrayList<>();
    }

    /**
     * Send a single prompt to Gemini and get a response.
     *
     * @param prompt The user prompt to send
     * @return GeminiResponse containing the model's reply and metadata
     */
    public GeminiResponse sendPrompt(String prompt) {
        return sendPrompt(prompt, 0.7, 1024);
    }

    /**
     * Send a prompt with custom generation parameters.
     *
     * @param prompt      The user prompt
     * @param temperature Creativity level (0.0 - 1.0)
     * @param maxTokens   Maximum response tokens
     * @return GeminiResponse containing the model's reply and metadata
     */
    public GeminiResponse sendPrompt(String prompt, double temperature, int maxTokens) {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt must not be null or empty");
        }

        Map<String, Object> requestBody = buildRequestBody(prompt, temperature, maxTokens);

        String endpoint = String.format("%s/models/%s:generateContent?key=%s",
                GEMINI_BASE_URL, model, apiKey);

        LoggerUtil.info("[GeminiAPI] Sending prompt to model: " + model);
        LoggerUtil.info("[GeminiAPI] Prompt length: " + prompt.length() + " chars");

        Response response = RestAssured.given()
                .contentType("application/json")
                .body(requestBody)
                .post(endpoint);

        int statusCode = response.getStatusCode();
        String responseBody = response.getBody().asString();

        LoggerUtil.info("[GeminiAPI] Response status: " + statusCode);

        if (statusCode != 200) {
            LoggerUtil.error("[GeminiAPI] Error response: " + responseBody);
            return new GeminiResponse(null, statusCode, responseBody, prompt);
        }

        String generatedText = extractText(response);
        addToHistory("user", prompt);
        addToHistory("model", generatedText);

        return new GeminiResponse(generatedText, statusCode, responseBody, prompt);
    }

    /**
     * Send a message in multi-turn conversation context.
     *
     * @param userMessage The next user message in the conversation
     * @return GeminiResponse with conversation-aware reply
     */
    public GeminiResponse sendInConversation(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("User message must not be null or empty");
        }

        Map<String, Object> requestBody = buildConversationBody(userMessage);

        String endpoint = String.format("%s/models/%s:generateContent?key=%s",
                GEMINI_BASE_URL, model, apiKey);

        LoggerUtil.info("[GeminiAPI] Sending conversation message (turn " + (conversationHistory.size() + 1) + ")");

        Response response = RestAssured.given()
                .contentType("application/json")
                .body(requestBody)
                .post(endpoint);

        int statusCode = response.getStatusCode();
        String responseBody = response.getBody().asString();

        if (statusCode == 200) {
            String generatedText = extractText(response);
            addToHistory("user", userMessage);
            addToHistory("model", generatedText);
            return new GeminiResponse(generatedText, statusCode, responseBody, userMessage);
        }

        return new GeminiResponse(null, statusCode, responseBody, userMessage);
    }

    /**
     * Send a system-prompted chatbot query (simulates chatbot with persona).
     *
     * @param systemPrompt The system/persona instruction
     * @param userQuery    The user's question to the chatbot
     * @return GeminiResponse with the chatbot's reply
     */
    public GeminiResponse queryChatbot(String systemPrompt, String userQuery) {
        String combinedPrompt = String.format(
                "System Instructions: %s\n\nUser Query: %s", systemPrompt, userQuery);
        return sendPrompt(combinedPrompt);
    }

    /**
     * Reset conversation history for a fresh session.
     */
    public void resetConversation() {
        conversationHistory.clear();
        LoggerUtil.info("[GeminiAPI] Conversation history cleared");
    }

    /**
     * Get current conversation turn count.
     */
    public int getConversationTurnCount() {
        return conversationHistory.size() / 2;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private Map<String, Object> buildRequestBody(String prompt, double temperature, int maxTokens) {
        Map<String, Object> body = new HashMap<>();

        // Contents
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> content = new HashMap<>();
        List<Map<String, String>> parts = new ArrayList<>();
        Map<String, String> part = new HashMap<>();
        part.put("text", prompt);
        parts.add(part);
        content.put("parts", parts);
        content.put("role", "user");
        contents.add(content);
        body.put("contents", contents);

        // Generation config
        Map<String, Object> genConfig = new HashMap<>();
        genConfig.put("temperature", temperature);
        genConfig.put("maxOutputTokens", maxTokens);
        genConfig.put("topP", 0.95);
        genConfig.put("topK", 40);
        body.put("generationConfig", genConfig);

        return body;
    }

    private Map<String, Object> buildConversationBody(String userMessage) {
        Map<String, Object> body = new HashMap<>();

        List<Map<String, Object>> contents = new ArrayList<>(conversationHistory);

        // Add new user message
        Map<String, Object> userContent = new HashMap<>();
        List<Map<String, String>> parts = new ArrayList<>();
        Map<String, String> part = new HashMap<>();
        part.put("text", userMessage);
        parts.add(part);
        userContent.put("parts", parts);
        userContent.put("role", "user");
        contents.add(userContent);

        body.put("contents", contents);

        Map<String, Object> genConfig = new HashMap<>();
        genConfig.put("temperature", 0.7);
        genConfig.put("maxOutputTokens", 1024);
        body.put("generationConfig", genConfig);

        return body;
    }

    private void addToHistory(String role, String text) {
        Map<String, Object> entry = new HashMap<>();
        List<Map<String, String>> parts = new ArrayList<>();
        Map<String, String> part = new HashMap<>();
        part.put("text", text);
        parts.add(part);
        entry.put("parts", parts);
        entry.put("role", role);
        conversationHistory.add(entry);
    }

    private String extractText(Response response) {
        try {
            return response.jsonPath().getString(
                    "candidates[0].content.parts[0].text");
        } catch (Exception e) {
            LoggerUtil.error("[GeminiAPI] Failed to extract text: " + e.getMessage());
            return "";
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RESPONSE DTO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Immutable response object from Gemini API calls.
     */
    public static class GeminiResponse {
        private final String text;
        private final int statusCode;
        private final String rawBody;
        private final String originalPrompt;

        public GeminiResponse(String text, int statusCode, String rawBody, String originalPrompt) {
            this.text = text;
            this.statusCode = statusCode;
            this.rawBody = rawBody;
            this.originalPrompt = originalPrompt;
        }

        public String getText() { return text; }
        public int getStatusCode() { return statusCode; }
        public String getRawBody() { return rawBody; }
        public String getOriginalPrompt() { return originalPrompt; }
        public boolean isSuccess() { return statusCode == 200 && text != null; }
    }
}

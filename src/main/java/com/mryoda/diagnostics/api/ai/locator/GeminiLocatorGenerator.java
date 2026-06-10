package com.mryoda.diagnostics.api.ai.locator;

import com.mryoda.diagnostics.api.ai.chatbot.GeminiAPI;
import com.mryoda.diagnostics.api.utils.LoggerUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Gemini Locator Generator - Uses Google Gemini AI to generate alternative locators
 * when primary locators fail.
 *
 * Capabilities:
 * - Generate XPath alternatives from page HTML context
 * - Generate CSS selector alternatives
 * - Suggest robust locator strategies based on element context
 * - Rank generated locators by reliability
 *
 * Integration Point:
 * - Works with existing Locators.java (pageObjects package)
 * - Invoked by LocatorHealingManager when standard locators break
 * - Results stored via LocatorRepository
 * - Complements Healenium (Healenium uses tree-based diffing; this uses LLM reasoning)
 */
public class GeminiLocatorGenerator {

    private final GeminiAPI geminiClient;

    private static final String LOCATOR_GEN_PROMPT =
            "You are an expert Selenium WebDriver locator engineer. "
            + "Given the HTML context of a web element, generate reliable locators. "
            + "Return ONLY a JSON array of locator objects, each with: "
            + "{\"strategy\": \"xpath|css|id|name\", \"value\": \"<locator>\", "
            + "\"confidence\": <0.0-1.0>, \"reasoning\": \"<why this is reliable>\"}. "
            + "Generate 3-5 alternatives, ordered by reliability. "
            + "Prefer: id > data-testid > aria-label > unique class > structural xpath. "
            + "Avoid fragile positional xpaths.";

    public GeminiLocatorGenerator(GeminiAPI geminiClient) {
        if (geminiClient == null) {
            throw new IllegalArgumentException("GeminiAPI client must not be null");
        }
        this.geminiClient = geminiClient;
    }

    /**
     * Generate alternative locators for an element given its surrounding HTML.
     *
     * @param elementHtml      The outerHTML of the target element
     * @param parentHtml       The parent container HTML for context
     * @param originalLocator  The original (broken) locator
     * @param elementDescription Human-readable description of what the element is
     * @return List of LocatorSuggestion ordered by confidence
     */
    public List<LocatorSuggestion> generateLocators(String elementHtml, String parentHtml,
                                                     String originalLocator, String elementDescription) {
        LoggerUtil.info("[GeminiLocator] Generating alternatives for: " + elementDescription);
        LoggerUtil.info("[GeminiLocator] Original broken locator: " + originalLocator);

        String prompt = String.format(
                "%s\n\nElement Description: %s\n\n"
                + "Original (broken) locator: %s\n\n"
                + "Element HTML:\n%s\n\n"
                + "Parent Container HTML:\n%s",
                LOCATOR_GEN_PROMPT, elementDescription, originalLocator,
                truncateHtml(elementHtml, 2000), truncateHtml(parentHtml, 3000));

        GeminiAPI.GeminiResponse response = geminiClient.sendPrompt(prompt, 0.2, 1024);

        if (!response.isSuccess()) {
            LoggerUtil.error("[GeminiLocator] Generation failed: " + response.getStatusCode());
            return List.of();
        }

        return parseLocatorSuggestions(response.getText());
    }

    /**
     * Generate a single best locator with high confidence.
     *
     * @param elementHtml      The target element HTML
     * @param elementDescription Description of the element
     * @return The highest confidence LocatorSuggestion, or null
     */
    public LocatorSuggestion generateBestLocator(String elementHtml, String elementDescription) {
        List<LocatorSuggestion> suggestions = generateLocators(
                elementHtml, "", "unknown", elementDescription);

        return suggestions.isEmpty() ? null : suggestions.get(0);
    }

    /**
     * Validate whether a generated locator looks syntactically correct.
     *
     * @param strategy The locator strategy (xpath, css, id, name)
     * @param value    The locator value
     * @return true if syntactically valid
     */
    public boolean validateLocatorSyntax(String strategy, String value) {
        if (value == null || value.trim().isEmpty()) return false;

        switch (strategy.toLowerCase()) {
            case "xpath":
                return value.startsWith("/") || value.startsWith("(") || value.startsWith(".");
            case "css":
                return !value.contains("//") && (value.contains(".") || value.contains("#")
                        || value.contains("[") || value.matches("^[a-zA-Z].*"));
            case "id":
                return !value.contains(" ") && !value.contains("/");
            case "name":
                return !value.contains(" ") && !value.contains("/");
            default:
                return false;
        }
    }

    /**
     * Generate locators specifically for dynamic elements (lists, tables, repeated items).
     *
     * @param containerHtml    HTML of the container with repeating elements
     * @param targetItemText   The text/content of the specific item to locate
     * @param elementType      Type: "button", "link", "input", "text", etc.
     * @return List of dynamic-aware LocatorSuggestions
     */
    public List<LocatorSuggestion> generateDynamicLocators(String containerHtml,
                                                            String targetItemText,
                                                            String elementType) {
        String prompt = String.format(
                "%s\n\nIMPORTANT: This is a DYNAMIC/REPEATED element. "
                + "Generate locators that target the SPECIFIC item with text '%s' "
                + "among similar siblings. Use text-based matching or data attributes.\n\n"
                + "Target element type: %s\n"
                + "Target text/content: %s\n\n"
                + "Container HTML:\n%s",
                LOCATOR_GEN_PROMPT, targetItemText, elementType, targetItemText,
                truncateHtml(containerHtml, 4000));

        GeminiAPI.GeminiResponse response = geminiClient.sendPrompt(prompt, 0.2, 1024);

        if (!response.isSuccess()) {
            return List.of();
        }

        return parseLocatorSuggestions(response.getText());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private List<LocatorSuggestion> parseLocatorSuggestions(String responseText) {
        List<LocatorSuggestion> suggestions = new ArrayList<>();

        try {
            // Simple JSON array parsing for locator objects
            String text = responseText.trim();
            if (text.startsWith("```")) {
                text = text.replaceAll("```json?\\n?", "").replaceAll("```", "").trim();
            }

            // Extract individual objects from the array
            int depth = 0;
            int objStart = -1;

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '{') {
                    if (depth == 0) objStart = i;
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0 && objStart >= 0) {
                        String obj = text.substring(objStart, i + 1);
                        LocatorSuggestion suggestion = parseOneLocator(obj);
                        if (suggestion != null) {
                            suggestions.add(suggestion);
                        }
                        objStart = -1;
                    }
                }
            }
        } catch (Exception e) {
            LoggerUtil.error("[GeminiLocator] Parse error: " + e.getMessage());
        }

        // Sort by confidence descending
        suggestions.sort((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()));

        LoggerUtil.info("[GeminiLocator] Generated " + suggestions.size() + " locator suggestions");
        return suggestions;
    }

    private LocatorSuggestion parseOneLocator(String json) {
        try {
            String strategy = extractJsonString(json, "strategy");
            String value = extractJsonString(json, "value");
            double confidence = extractJsonDouble(json, "confidence");
            String reasoning = extractJsonString(json, "reasoning");

            if (strategy != null && value != null && validateLocatorSyntax(strategy, value)) {
                return new LocatorSuggestion(strategy, value, confidence, reasoning);
            }
        } catch (Exception e) {
            // Skip malformed entries
        }
        return null;
    }

    private String extractJsonString(String json, String key) {
        int keyIdx = json.indexOf("\"" + key + "\"");
        if (keyIdx < 0) return null;
        int colonIdx = json.indexOf(':', keyIdx);
        if (colonIdx < 0) return null;
        int firstQuote = json.indexOf('"', colonIdx);
        if (firstQuote < 0) return null;
        int secondQuote = json.indexOf('"', firstQuote + 1);
        // Handle escaped quotes
        while (secondQuote > 0 && json.charAt(secondQuote - 1) == '\\') {
            secondQuote = json.indexOf('"', secondQuote + 1);
        }
        if (secondQuote < 0) return null;
        return json.substring(firstQuote + 1, secondQuote);
    }

    private double extractJsonDouble(String json, String key) {
        int keyIdx = json.indexOf("\"" + key + "\"");
        if (keyIdx < 0) return 0.5;
        int colonIdx = json.indexOf(':', keyIdx);
        if (colonIdx < 0) return 0.5;
        StringBuilder num = new StringBuilder();
        for (int i = colonIdx + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (Character.isDigit(c) || c == '.') {
                num.append(c);
            } else if (num.length() > 0) {
                break;
            }
        }
        return num.length() > 0 ? Double.parseDouble(num.toString()) : 0.5;
    }

    private String truncateHtml(String html, int maxLen) {
        if (html == null) return "";
        return html.length() > maxLen ? html.substring(0, maxLen) + "..." : html;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DTO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Immutable locator suggestion from AI generation.
     */
    public static class LocatorSuggestion {
        private final String strategy;
        private final String value;
        private final double confidence;
        private final String reasoning;

        public LocatorSuggestion(String strategy, String value, double confidence, String reasoning) {
            this.strategy = strategy;
            this.value = value;
            this.confidence = confidence;
            this.reasoning = reasoning;
        }

        public String getStrategy() { return strategy; }
        public String getValue() { return value; }
        public double getConfidence() { return confidence; }
        public String getReasoning() { return reasoning; }

        @Override
        public String toString() {
            return String.format("Locator[%s='%s', confidence=%.2f]", strategy, value, confidence);
        }
    }
}

package com.mryoda.diagnostics.api.ai.locator;

import com.mryoda.diagnostics.api.utils.LoggerUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Locator Repository - Persistent storage for healed locator mappings.
 *
 * Stores:
 * - Original broken locator → healed locator mapping
 * - Healing confidence score
 * - Success count (how many times the healed locator worked)
 * - Timestamp of last heal
 *
 * Persistence: JSON file-based storage in project's `logs/ai-healed-locators/` directory.
 *
 * Integration Points:
 * - Used by LocatorHealingManager to cache and retrieve healed locators
 * - Complements Healenium's PostgreSQL storage with AI-specific locator history
 * - Does NOT interfere with existing `logs/` directory structure
 */
public class LocatorRepository {

    private static final String STORAGE_DIR = "logs/ai-healed-locators";
    private static final String MAPPING_FILE = "healed-locators.json";
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Map<String, HealedLocator> healedLocators;
    private final Path storagePath;

    public LocatorRepository() {
        this(STORAGE_DIR);
    }

    public LocatorRepository(String storageDirectory) {
        this.storagePath = Paths.get(storageDirectory);
        this.healedLocators = new ConcurrentHashMap<>();
        ensureStorageDirectory();
        loadFromDisk();
    }

    /**
     * Store a newly healed locator mapping.
     *
     * @param originalLocator   The original (broken) locator string
     * @param healedStrategy    The strategy of the healed locator (xpath, css, etc.)
     * @param healedValue       The value of the healed locator
     * @param confidence        AI confidence score (0.0-1.0)
     * @param elementDescription Human description of the element
     */
    public void storeHealedLocator(String originalLocator, String healedStrategy,
                                    String healedValue, double confidence,
                                    String elementDescription) {
        HealedLocator healed = new HealedLocator(
                originalLocator, healedStrategy, healedValue,
                confidence, elementDescription,
                LocalDateTime.now().format(TIMESTAMP_FORMAT), 1);

        healedLocators.put(originalLocator, healed);
        saveToDisk();

        LoggerUtil.info("[LocatorRepo] Stored healed mapping: " + originalLocator
                + " → " + healedStrategy + "=" + healedValue);
    }

    /**
     * Retrieve a previously healed locator.
     *
     * @param originalLocator The original locator string to look up
     * @return HealedLocator if found, null otherwise
     */
    public HealedLocator getHealedLocator(String originalLocator) {
        return healedLocators.get(originalLocator);
    }

    /**
     * Increment the success count for a healed locator (used when cached heal works).
     */
    public void incrementSuccessCount(String originalLocator) {
        HealedLocator existing = healedLocators.get(originalLocator);
        if (existing != null) {
            HealedLocator updated = new HealedLocator(
                    existing.getOriginalLocator(),
                    existing.getStrategy(),
                    existing.getValue(),
                    existing.getConfidence(),
                    existing.getElementDescription(),
                    LocalDateTime.now().format(TIMESTAMP_FORMAT),
                    existing.getSuccessCount() + 1);
            healedLocators.put(originalLocator, updated);
            saveToDisk();
        }
    }

    /**
     * Remove a healed locator (when cached heal stops working).
     */
    public void removeHealedLocator(String originalLocator) {
        healedLocators.remove(originalLocator);
        saveToDisk();
        LoggerUtil.info("[LocatorRepo] Removed stale mapping: " + originalLocator);
    }

    /**
     * Get total number of healed locators stored.
     */
    public int getHealedCount() {
        return healedLocators.size();
    }

    /**
     * Get all healed locator mappings (read-only snapshot).
     */
    public Map<String, HealedLocator> getAllHealedLocators() {
        return Map.copyOf(healedLocators);
    }

    /**
     * Get summary statistics.
     */
    public String getStatsSummary() {
        int total = healedLocators.size();
        int highConfidence = (int) healedLocators.values().stream()
                .filter(h -> h.getConfidence() >= 0.8).count();
        int totalSuccesses = healedLocators.values().stream()
                .mapToInt(HealedLocator::getSuccessCount).sum();

        return String.format("AI Healed Locators: %d total | %d high-confidence | %d total successes",
                total, highConfidence, totalSuccesses);
    }

    /**
     * Clear all stored healed locators.
     */
    public void clearAll() {
        healedLocators.clear();
        saveToDisk();
        LoggerUtil.info("[LocatorRepo] All healed locators cleared");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PERSISTENCE
    // ─────────────────────────────────────────────────────────────────────────

    private void ensureStorageDirectory() {
        try {
            Files.createDirectories(storagePath);
        } catch (IOException e) {
            LoggerUtil.error("[LocatorRepo] Failed to create storage dir: " + e.getMessage());
        }
    }

    private void saveToDisk() {
        Path file = storagePath.resolve(MAPPING_FILE);
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            writer.write("[\n");
            int i = 0;
            for (HealedLocator locator : healedLocators.values()) {
                if (i > 0) writer.write(",\n");
                writer.write(locator.toJson());
                i++;
            }
            writer.write("\n]");
        } catch (IOException e) {
            LoggerUtil.error("[LocatorRepo] Failed to save to disk: " + e.getMessage());
        }
    }

    private void loadFromDisk() {
        Path file = storagePath.resolve(MAPPING_FILE);
        if (!Files.exists(file)) return;

        try {
            String content = new String(Files.readAllBytes(file));
            parseAndLoad(content);
            LoggerUtil.info("[LocatorRepo] Loaded " + healedLocators.size() + " healed locators from disk");
        } catch (IOException e) {
            LoggerUtil.error("[LocatorRepo] Failed to load from disk: " + e.getMessage());
        }
    }

    private void parseAndLoad(String json) {
        // Simple JSON array parsing
        int depth = 0;
        int objStart = -1;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                if (depth == 0) objStart = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && objStart >= 0) {
                    String obj = json.substring(objStart, i + 1);
                    HealedLocator locator = HealedLocator.fromJson(obj);
                    if (locator != null) {
                        healedLocators.put(locator.getOriginalLocator(), locator);
                    }
                    objStart = -1;
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DTO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Immutable healed locator record.
     */
    public static class HealedLocator {
        private final String originalLocator;
        private final String strategy;
        private final String value;
        private final double confidence;
        private final String elementDescription;
        private final String lastHealed;
        private final int successCount;

        public HealedLocator(String originalLocator, String strategy, String value,
                              double confidence, String elementDescription,
                              String lastHealed, int successCount) {
            this.originalLocator = originalLocator;
            this.strategy = strategy;
            this.value = value;
            this.confidence = confidence;
            this.elementDescription = elementDescription;
            this.lastHealed = lastHealed;
            this.successCount = successCount;
        }

        public String getOriginalLocator() { return originalLocator; }
        public String getStrategy() { return strategy; }
        public String getValue() { return value; }
        public double getConfidence() { return confidence; }
        public String getElementDescription() { return elementDescription; }
        public String getLastHealed() { return lastHealed; }
        public int getSuccessCount() { return successCount; }

        public String toJson() {
            return String.format(
                    "  {\"originalLocator\": \"%s\", \"strategy\": \"%s\", \"value\": \"%s\", "
                    + "\"confidence\": %.2f, \"elementDescription\": \"%s\", "
                    + "\"lastHealed\": \"%s\", \"successCount\": %d}",
                    escapeJson(originalLocator), escapeJson(strategy), escapeJson(value),
                    confidence, escapeJson(elementDescription), lastHealed, successCount);
        }

        public static HealedLocator fromJson(String json) {
            try {
                String original = extractField(json, "originalLocator");
                String strategy = extractField(json, "strategy");
                String value = extractField(json, "value");
                double confidence = extractDouble(json, "confidence");
                String desc = extractField(json, "elementDescription");
                String lastHealed = extractField(json, "lastHealed");
                int successCount = extractInt(json, "successCount");

                if (original == null || strategy == null || value == null) return null;

                return new HealedLocator(original, strategy, value, confidence,
                        desc, lastHealed, successCount);
            } catch (Exception e) {
                return null;
            }
        }

        private static String extractField(String json, String key) {
            int keyIdx = json.indexOf("\"" + key + "\"");
            if (keyIdx < 0) return null;
            int colonIdx = json.indexOf(':', keyIdx);
            if (colonIdx < 0) return null;
            int firstQuote = json.indexOf('"', colonIdx);
            if (firstQuote < 0) return null;
            int secondQuote = json.indexOf('"', firstQuote + 1);
            while (secondQuote > 0 && json.charAt(secondQuote - 1) == '\\') {
                secondQuote = json.indexOf('"', secondQuote + 1);
            }
            if (secondQuote < 0) return null;
            return json.substring(firstQuote + 1, secondQuote);
        }

        private static double extractDouble(String json, String key) {
            int keyIdx = json.indexOf("\"" + key + "\"");
            if (keyIdx < 0) return 0;
            int colonIdx = json.indexOf(':', keyIdx);
            if (colonIdx < 0) return 0;
            StringBuilder num = new StringBuilder();
            for (int i = colonIdx + 1; i < json.length(); i++) {
                char c = json.charAt(i);
                if (Character.isDigit(c) || c == '.') num.append(c);
                else if (num.length() > 0) break;
            }
            return num.length() > 0 ? Double.parseDouble(num.toString()) : 0;
        }

        private static int extractInt(String json, String key) {
            return (int) extractDouble(json, key);
        }

        private static String escapeJson(String text) {
            if (text == null) return "";
            return text.replace("\\", "\\\\").replace("\"", "\\\"");
        }

        @Override
        public String toString() {
            return String.format("Healed[%s → %s='%s' (%.0f%% confidence, %d successes)]",
                    originalLocator, strategy, value, confidence * 100, successCount);
        }
    }
}

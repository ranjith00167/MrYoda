package com.mryoda.diagnostics.api.ai.config;

import com.mryoda.diagnostics.api.utils.LoggerUtil;

import java.io.InputStream;
import java.util.Properties;

/**
 * Feature Flag Manager for AI Locator Healing.
 *
 * <p>Controls whether AI-powered locator healing is active during test execution.
 * When disabled, tests fail immediately on broken locators (normal Selenium behavior).
 * When enabled, the system attempts AI-powered alternatives before failing.</p>
 *
 * <h2>Configuration</h2>
 * <p>Reads from {@code config.properties} on the classpath:</p>
 * <pre>
 *   ai.healing.enabled=true
 *   ai.healing.confidence.threshold=0.75
 *   ai.healing.max.suggestions=5
 *   ai.healing.cache.enabled=true
 * </pre>
 *
 * <p>System properties override file values (useful for CI):</p>
 * <pre>
 *   -Dai.healing.enabled=false
 * </pre>
 *
 * <h2>Integration Points</h2>
 * <ul>
 *   <li>{@link com.mryoda.diagnostics.api.ai.locator.LocatorHealingManager} — checks {@code isAiHealingEnabled()}</li>
 *   <li>{@code LocatorHealingAdapter} (src/test) — reads threshold for confidence gating</li>
 *   <li>{@code Hooks.java} — initializes during {@code @Before} lifecycle</li>
 * </ul>
 */
public class FeatureFlagManager {

    private final boolean aiHealingEnabled;
    private final boolean cacheEnabled;
    private final double confidenceThreshold;
    private final int maxSuggestions;

    /**
     * Load flags from config.properties with system property overrides.
     */
    public FeatureFlagManager() {
        Properties props = loadProperties();

        this.aiHealingEnabled = resolveFlag(props, "ai.healing.enabled", true);
        this.cacheEnabled = resolveFlag(props, "ai.healing.cache.enabled", true);
        this.confidenceThreshold = resolveDouble(props, "ai.healing.confidence.threshold", 0.75);
        this.maxSuggestions = resolveInt(props, "ai.healing.max.suggestions", 5);

        LoggerUtil.info("[FeatureFlags] AI Healing: " + aiHealingEnabled);
        LoggerUtil.info("[FeatureFlags] Cache: " + cacheEnabled);
        LoggerUtil.info("[FeatureFlags] Confidence Threshold: " + confidenceThreshold);
        LoggerUtil.info("[FeatureFlags] Max Suggestions: " + maxSuggestions);
    }

    /**
     * Explicit constructor for testing or programmatic override.
     *
     * @param aiHealingEnabled     Master toggle for AI healing
     * @param cacheEnabled         Whether to use LocatorRepository cache
     * @param confidenceThreshold  Minimum confidence for auto-heal (0.0-1.0)
     * @param maxSuggestions       Maximum locator suggestions to generate
     */
    public FeatureFlagManager(boolean aiHealingEnabled, boolean cacheEnabled,
                               double confidenceThreshold, int maxSuggestions) {
        this.aiHealingEnabled = aiHealingEnabled;
        this.cacheEnabled = cacheEnabled;
        this.confidenceThreshold = confidenceThreshold;
        this.maxSuggestions = maxSuggestions;
    }

    // ─────────────────────────────────────────────────────────────────────────

    /** Whether AI locator healing is active. */
    public boolean isAiHealingEnabled() { return aiHealingEnabled; }

    /** Whether healed locators are cached and reused. */
    public boolean isCacheEnabled() { return cacheEnabled; }

    /** Minimum confidence score to auto-heal without manual review. */
    public double getConfidenceThreshold() { return confidenceThreshold; }

    /** Maximum number of alternative locator suggestions to generate. */
    public int getMaxSuggestions() { return maxSuggestions; }

    /** Summary for logging. */
    public String getSummary() {
        return String.format(
                "FeatureFlags[healing=%s, cache=%s, threshold=%.2f, maxSuggestions=%d]",
                aiHealingEnabled, cacheEnabled, confidenceThreshold, maxSuggestions);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (Exception e) {
            LoggerUtil.info("[FeatureFlags] config.properties not found — using defaults");
        }
        return props;
    }

    private boolean resolveFlag(Properties props, String key, boolean defaultValue) {
        String sysProp = System.getProperty(key);
        if (sysProp != null) {
            return Boolean.parseBoolean(sysProp);
        }
        String propValue = props.getProperty(key);
        if (propValue != null) {
            return Boolean.parseBoolean(propValue.trim());
        }
        return defaultValue;
    }

    private double resolveDouble(Properties props, String key, double defaultValue) {
        String sysProp = System.getProperty(key);
        if (sysProp != null) {
            try { return Double.parseDouble(sysProp); } catch (NumberFormatException ignored) {}
        }
        String propValue = props.getProperty(key);
        if (propValue != null) {
            try { return Double.parseDouble(propValue.trim()); } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    private int resolveInt(Properties props, String key, int defaultValue) {
        String sysProp = System.getProperty(key);
        if (sysProp != null) {
            try { return Integer.parseInt(sysProp); } catch (NumberFormatException ignored) {}
        }
        String propValue = props.getProperty(key);
        if (propValue != null) {
            try { return Integer.parseInt(propValue.trim()); } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }
}

package com.mryoda.diagnostics.api.ai.locator;

import com.mryoda.diagnostics.api.utils.LoggerUtil;
import com.mryoda.diagnostics.api.ai.locator.GeminiLocatorGenerator.LocatorSuggestion;

import java.util.List;

/**
 * Locator Healing Manager - Orchestrates AI-powered locator healing when elements fail.
 *
 * This class is Selenium-independent (lives in src/main).
 * The Selenium integration adapter (LocatorHealingAdapter) lives in src/test.
 *
 * Workflow:
 * 1. Primary locator fails → caller provides page HTML context
 * 2. Manager checks repository for cached heal
 * 3. If cache miss, asks GeminiLocatorGenerator for alternatives
 * 4. Returns ordered list of suggestions for caller to try
 * 5. Caller reports which suggestion worked → stored in LocatorRepository
 *
 * Integration Points:
 * - Works with existing DriverFactory via LocatorHealingAdapter (src/test)
 * - Complements Healenium: Healenium uses tree-diff; this uses LLM reasoning
 * - Stores healed mappings in LocatorRepository for persistence
 */
public class LocatorHealingManager {

    private final GeminiLocatorGenerator locatorGenerator;
    private final LocatorRepository repository;
    private boolean enabled;

    public LocatorHealingManager(GeminiLocatorGenerator locatorGenerator, LocatorRepository repository) {
        if (locatorGenerator == null) {
            throw new IllegalArgumentException("GeminiLocatorGenerator must not be null");
        }
        if (repository == null) {
            throw new IllegalArgumentException("LocatorRepository must not be null");
        }
        this.locatorGenerator = locatorGenerator;
        this.repository = repository;
        this.enabled = true;
    }

    /**
     * Get healing suggestions for a failed locator.
     *
     * @param originalLocator    String representation of the broken locator
     * @param pageHtml           Current page HTML context
     * @param elementDescription Human-readable description for AI context
     * @return HealingResult with cached or newly generated suggestions
     */
    public HealingResult getHealingSuggestions(String originalLocator, String pageHtml,
                                               String elementDescription) {
        if (!enabled) {
            LoggerUtil.info("[LocatorHealing] AI healing is disabled");
            return new HealingResult(List.of(), null, false);
        }

        LoggerUtil.info("[LocatorHealing] Healing request for: " + elementDescription);
        LoggerUtil.info("[LocatorHealing] Original locator: " + originalLocator);

        // Step 1: Check repository for cached heal
        LocatorRepository.HealedLocator cached = repository.getHealedLocator(originalLocator);
        if (cached != null) {
            LoggerUtil.info("[LocatorHealing] Found cached heal: " + cached);
            return new HealingResult(List.of(), cached, true);
        }

        // Step 2: Generate new alternatives via Gemini
        List<LocatorSuggestion> suggestions = locatorGenerator.generateLocators(
                pageHtml, "", originalLocator, elementDescription);

        if (suggestions.isEmpty()) {
            LoggerUtil.error("[LocatorHealing] No suggestions generated");
        } else {
            LoggerUtil.info("[LocatorHealing] Generated " + suggestions.size() + " suggestions");
        }

        return new HealingResult(suggestions, null, false);
    }

    /**
     * Get healing suggestions specifically for dynamic/repeated elements.
     *
     * @param containerHtml HTML of the container with repeated elements
     * @param targetText    Text of the specific item to find
     * @param elementType   Type: button, link, input, text, etc.
     * @return List of LocatorSuggestions
     */
    public List<LocatorSuggestion> getDynamicHealingSuggestions(String containerHtml,
                                                                String targetText,
                                                                String elementType) {
        if (!enabled) return List.of();

        LoggerUtil.info("[LocatorHealing] Dynamic healing for: '" + targetText + "' (" + elementType + ")");
        return locatorGenerator.generateDynamicLocators(containerHtml, targetText, elementType);
    }

    /**
     * Report that a suggestion successfully found the element.
     * Stores the healed mapping in the repository.
     *
     * @param originalLocator    The original broken locator string
     * @param suggestion         The suggestion that worked
     * @param elementDescription Description of the element
     */
    public void reportHealingSuccess(String originalLocator, LocatorSuggestion suggestion,
                                      String elementDescription) {
        repository.storeHealedLocator(originalLocator, suggestion.getStrategy(),
                suggestion.getValue(), suggestion.getConfidence(), elementDescription);
        LoggerUtil.info("[LocatorHealing] ✅ HEALED: " + originalLocator + " → " + suggestion);
    }

    /**
     * Report that a cached heal was used successfully (increment counter).
     */
    public void reportCachedHealSuccess(String originalLocator) {
        repository.incrementSuccessCount(originalLocator);
    }

    /**
     * Report that a cached heal failed (remove from cache to trigger re-generation).
     */
    public void reportCachedHealFailure(String originalLocator) {
        repository.removeHealedLocator(originalLocator);
        LoggerUtil.info("[LocatorHealing] Cached heal invalidated: " + originalLocator);
    }

    /**
     * Enable or disable AI healing.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        LoggerUtil.info("[LocatorHealing] AI healing " + (enabled ? "ENABLED" : "DISABLED"));
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get healing statistics.
     */
    public String getHealingStats() {
        return repository.getStatsSummary();
    }

    /**
     * Enhanced healing flow with confidence gating.
     *
     * Flow:
     * 1. Healenium (handled externally by SelfHealingDriver)
     * 2. Locator Repository Cache
     * 3. Gemini Locator Generation
     * 4. Confidence Validation (>= 0.75 to auto-heal)
     * 5. Store Result
     *
     * @param originalLocator    The broken locator
     * @param pageHtml           Page context
     * @param elementDescription Element description for AI
     * @return LocatorHealingResult with confidence gating
     */
    public LocatorHealingResult healWithConfidence(String originalLocator, String pageHtml,
                                                    String elementDescription) {
        if (!enabled) {
            return LocatorHealingResult.failed("AI healing is disabled");
        }

        LoggerUtil.info("[LocatorHealing] ═══ Enhanced Healing Flow ═══");
        LoggerUtil.info("[LocatorHealing] Step 1: Healenium → handled by SelfHealingDriver");

        // Step 2: Check repository cache
        LocatorRepository.HealedLocator cached = repository.getHealedLocator(originalLocator);
        if (cached != null) {
            LoggerUtil.info("[LocatorHealing] Step 2: Cache HIT → " + cached.getValue());
            return LocatorHealingResult.fromCache(cached);
        }
        LoggerUtil.info("[LocatorHealing] Step 2: Cache MISS");

        // Step 3: Gemini generation
        List<LocatorSuggestion> suggestions = locatorGenerator.generateLocators(
                pageHtml, "", originalLocator, elementDescription);

        if (suggestions.isEmpty()) {
            LoggerUtil.error("[LocatorHealing] Step 3: Gemini → No suggestions");
            return LocatorHealingResult.failed("No AI suggestions generated");
        }

        LocatorSuggestion best = suggestions.get(0);
        LocatorSuggestion backup = suggestions.size() > 1 ? suggestions.get(1) : null;

        // Step 4: Confidence validation
        LocatorHealingResult result = LocatorHealingResult.fromGeminiSuggestions(best, backup);

        if (!result.isAutoHealAllowed()) {
            LoggerUtil.info(String.format(
                    "[LocatorHealing] Step 4: ⚠️ LOW CONFIDENCE (%.2f < 0.75) — NOT auto-healing",
                    result.getConfidence()));
        } else {
            LoggerUtil.info(String.format(
                    "[LocatorHealing] Step 4: ✅ CONFIDENCE OK (%.2f >= 0.75) — auto-heal allowed",
                    result.getConfidence()));

            // Step 5: Store in repository
            repository.storeHealedLocator(originalLocator, best.getStrategy(),
                    best.getValue(), best.getConfidence(), elementDescription);
            LoggerUtil.info("[LocatorHealing] Step 5: Stored in repository");
        }

        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RESULT DTO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Result of a healing request — contains either cached heal or new suggestions.
     */
    public static class HealingResult {
        private final List<LocatorSuggestion> suggestions;
        private final LocatorRepository.HealedLocator cachedHeal;
        private final boolean fromCache;

        public HealingResult(List<LocatorSuggestion> suggestions,
                              LocatorRepository.HealedLocator cachedHeal, boolean fromCache) {
            this.suggestions = suggestions != null ? List.copyOf(suggestions) : List.of();
            this.cachedHeal = cachedHeal;
            this.fromCache = fromCache;
        }

        public List<LocatorSuggestion> getSuggestions() { return suggestions; }
        public LocatorRepository.HealedLocator getCachedHeal() { return cachedHeal; }
        public boolean isFromCache() { return fromCache; }
        public boolean hasSuggestions() { return !suggestions.isEmpty() || cachedHeal != null; }
    }
}

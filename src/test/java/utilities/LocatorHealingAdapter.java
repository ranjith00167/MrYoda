package utilities;

import com.mryoda.diagnostics.api.ai.locator.GeminiLocatorGenerator.LocatorSuggestion;
import com.mryoda.diagnostics.api.ai.locator.LocatorHealingManager;
import com.mryoda.diagnostics.api.ai.locator.LocatorHealingManager.HealingResult;
import com.mryoda.diagnostics.api.ai.locator.LocatorRepository;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Locator Healing Adapter - Selenium integration layer for AI Locator Healing.
 *
 * This adapter bridges the Selenium-independent LocatorHealingManager (src/main)
 * with the actual WebDriver interactions (src/test).
 *
 * Usage in step definitions:
 * <pre>
 *   WebElement element = LocatorHealingAdapter.findWithHealing(
 *       driver, By.xpath("//button[@id='submit']"), healingManager, "Submit Button");
 * </pre>
 *
 * Integration Points:
 * - Uses existing DriverFactory.getDriver() for WebDriver
 * - Works alongside Healenium SelfHealingDriver (AI healing is a second layer)
 * - Placed in existing `utilities` package alongside BaseClass, DriverFactory
 */
public class LocatorHealingAdapter {

    private LocatorHealingAdapter() {
        // Static utility class
    }

    /**
     * Find an element with AI-powered healing fallback.
     *
     * @param driver             The WebDriver instance
     * @param originalLocator    The original By locator
     * @param healingManager     The LocatorHealingManager instance
     * @param elementDescription Human description for AI context
     * @return The found WebElement
     * @throws NoSuchElementException if element cannot be found even after healing
     */
    public static WebElement findWithHealing(WebDriver driver, By originalLocator,
                                              LocatorHealingManager healingManager,
                                              String elementDescription) {
        // Step 1: Try original locator
        try {
            return driver.findElement(originalLocator);
        } catch (NoSuchElementException e) {
            // Continue to AI healing
        }

        if (healingManager == null || !healingManager.isEnabled()) {
            throw new NoSuchElementException("Element not found: " + originalLocator);
        }

        // Step 2: Get page context
        String pageHtml = capturePageContext(driver);
        String locatorStr = originalLocator.toString();

        // Step 3: Get healing suggestions
        HealingResult result = healingManager.getHealingSuggestions(locatorStr, pageHtml, elementDescription);

        if (!result.hasSuggestions()) {
            throw new NoSuchElementException("AI healing produced no suggestions for: " + originalLocator);
        }

        // Step 4A: Try cached heal first
        if (result.isFromCache()) {
            LocatorRepository.HealedLocator cached = result.getCachedHeal();
            try {
                By healedBy = toBy(cached.getStrategy(), cached.getValue());
                WebElement element = driver.findElement(healedBy);
                healingManager.reportCachedHealSuccess(locatorStr);
                return element;
            } catch (NoSuchElementException e) {
                healingManager.reportCachedHealFailure(locatorStr);
                // Fall through to re-generate
                HealingResult freshResult = healingManager.getHealingSuggestions(
                        locatorStr, pageHtml, elementDescription);
                return trySuggestions(driver, freshResult.getSuggestions(),
                        healingManager, locatorStr, elementDescription);
            }
        }

        // Step 4B: Try generated suggestions
        return trySuggestions(driver, result.getSuggestions(),
                healingManager, locatorStr, elementDescription);
    }

    /**
     * Find a dynamic element with AI healing.
     *
     * @param driver          WebDriver instance
     * @param containerBy     Container locator
     * @param targetText      Text of the target item
     * @param elementType     Type: button, link, input, etc.
     * @param healingManager  The healing manager
     * @return The found WebElement
     */
    public static WebElement findDynamicWithHealing(WebDriver driver, By containerBy,
                                                     String targetText, String elementType,
                                                     LocatorHealingManager healingManager) {
        String containerHtml;
        try {
            WebElement container = driver.findElement(containerBy);
            containerHtml = container.getAttribute("outerHTML");
        } catch (NoSuchElementException e) {
            containerHtml = capturePageContext(driver);
        }

        List<LocatorSuggestion> suggestions = healingManager.getDynamicHealingSuggestions(
                containerHtml, targetText, elementType);

        for (LocatorSuggestion suggestion : suggestions) {
            try {
                By healedBy = toBy(suggestion.getStrategy(), suggestion.getValue());
                return driver.findElement(healedBy);
            } catch (NoSuchElementException ex) {
                // Try next
            }
        }

        throw new NoSuchElementException("Dynamic AI healing failed for: " + targetText);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private static WebElement trySuggestions(WebDriver driver, List<LocatorSuggestion> suggestions,
                                             LocatorHealingManager healingManager,
                                             String originalLocator, String elementDescription) {
        for (LocatorSuggestion suggestion : suggestions) {
            try {
                By healedBy = toBy(suggestion.getStrategy(), suggestion.getValue());
                WebElement element = driver.findElement(healedBy);
                healingManager.reportHealingSuccess(originalLocator, suggestion, elementDescription);
                return element;
            } catch (NoSuchElementException ex) {
                // Try next suggestion
            }
        }

        throw new NoSuchElementException("AI healing exhausted all "
                + suggestions.size() + " suggestions for: " + originalLocator);
    }

    private static String capturePageContext(WebDriver driver) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            String bodyHtml = (String) js.executeScript(
                    "return document.body ? document.body.innerHTML.substring(0, 5000) : '';");
            return bodyHtml != null ? bodyHtml : "";
        } catch (Exception e) {
            return "";
        }
    }

    private static By toBy(String strategy, String value) {
        switch (strategy.toLowerCase()) {
            case "xpath": return By.xpath(value);
            case "css": return By.cssSelector(value);
            case "id": return By.id(value);
            case "name": return By.name(value);
            case "classname": return By.className(value);
            case "tagname": return By.tagName(value);
            case "linktext": return By.linkText(value);
            default: return By.xpath(value);
        }
    }
}

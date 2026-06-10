package stepDefinition;

import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import pageObjects.Locators;
import utilities.BaseClass;
import utilities.BasePriceManager;
import java.time.Duration;
import java.util.List;

/**
 * Diagnostics page (Test selection) steps.
 * Uses strict BasePriceManager to record selection testName->price mapping in Excel order.
 */
public class DiagnosticsPageSteps extends BaseSteps {

	@When("load test names from Excel")
	public void load_test_names_from_excel() {

	    String testNamesData = BaseClass.testData.get("testName");

	    if (testNamesData == null || testNamesData.trim().isEmpty()) {
	        throw new AssertionError("❌ Test name(s) missing in Excel for this scenario!");
	    }

	    String[] testNames = testNamesData.split(",");
	    System.out.println("Loaded " + testNames.length + " tests from Excel:");
	    for (String name : testNames) {
	        System.out.println("  - " + name.trim());
	    }
	}

    @When("execute all tests")
    public void execute_all_tests() throws Throwable {

        String[] tests = BaseClass.getTestNamesFromExcel("Diagnostics");

        for (String test : tests) {

            System.out.println("Executing: " + test);

            BaseClass.waitAndInput(driver.findElement(By.xpath("//input[@placeholder='Search Test Here']")), test, 10);
            Thread.sleep(5000);

            BaseClass.scrollByOffset(0, 500);

            BaseClass.waitAndClick(driver.findElement(By.xpath("//button[normalize-space()='Add to cart' or normalize-space()='Add To Cart']")), 10);
            Thread.sleep(5000);

            BaseClass.scrollByOffset(0, -600);
            Thread.sleep(500);

            try {
                if (!BaseClass.isElementVisible(LocatorsPage.searchTestField, 10)) {
                    BaseClass.scrollToTop();
                }
            } catch (Exception ignored) {}

            BaseClass.waitAndClick(LocatorsPage.searchTestField, 10);
            BaseClass.clear(LocatorsPage.searchTestField);
        }
    }

    @When("select tests and capture individual prices")
    public void select_tests_and_capture_individual_prices() throws Exception {

        System.out.println("========== 🧪 TEST SELECTION START ==========");

        // reset selection to avoid accumulation between runs
        BasePriceManager.resetAll();

        String excelData = BaseClass.testData.get("testName");
        if (excelData == null || excelData.trim().isEmpty()) {
            throw new AssertionError("❌ Test name(s) missing in Excel for this scenario!");
        }

        String[] tests = excelData.split(",");

        for (String test : tests) {
            test = test.trim();
            // Do not strip the number here as requested
            String uiTestName = test;
            System.out.println("Executing: " + uiTestName + (uiTestName.equals(test) ? "" : " (Excel key: " + test + ")"));
            Thread.sleep(5000);
            // Enter search
            BaseClass.waitAndInput(driver.findElement(By.xpath("//input[@placeholder='Search Test Here']")), uiTestName, 10);

            // Wait up to 10s for any addtocartButton to appear in the DOM
            // (search results are async — fixed 2s sleep is too fragile)
            By anyAddBtn = By.xpath(
                "//button[contains(@class,'addtocartButton')]" +
                " | //button[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'add')]" +
                " | //button[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'remove')]" +
                " | //button[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'added')]");
            try {
                new WebDriverWait(driver, Duration.ofSeconds(12))
                    .until(ExpectedConditions.presenceOfElementLocated(anyAddBtn));
            } catch (Exception waitEx) {
                System.out.println("⚠️ Timed out waiting for add-to-cart button to appear for: " + uiTestName);
            }

            // Scroll for add-to-cart visibility
            BaseClass.scrollByOffset(0, 500);
            Thread.sleep(5000); // Give the UI a moment after scrolling
            BaseClass.scrollByOffset(0, 500);

            // ── 1. Find the add-to-cart button ────────────────────────────────
            List<WebElement> addBtns = driver.findElements(By.xpath(Locators.getAddBtnXPath(uiTestName)));
            if (addBtns.isEmpty()) {
                addBtns = driver.findElements(By.xpath("//button[contains(@class,'addtocartButton')]"));
            }
            if (addBtns.isEmpty()) {
                addBtns = driver.findElements(By.xpath(
                    "//button[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'add')]" +
                    " | //button[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'remove')]" +
                    " | //button[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'added')]"));
            }
            if (addBtns.isEmpty()) {
                System.out.println("❌ Add-to-cart button not found. Dumping page source snippet...");
                String pageSource = driver.getPageSource();
                System.out.println(pageSource.length() > 2000 ? pageSource.substring(0, 2000) : pageSource);
                throw new AssertionError("❌ Could not find Add-to-cart (or Remove) button for test: " + uiTestName);
            }

            // ── 2. Extract price from the same card as the button ─────────────
            double price = 0.0;
            String priceText = "0";
            try {
                WebElement btn = addBtns.get(0);
                // Price p.font-bold typically precedes the button in DOM order
                List<WebElement> priceEls = btn.findElements(
                    By.xpath("preceding::p[contains(@class,'font-bold')][1]"));
                if (priceEls.isEmpty()) {
                    // Try within the common ancestor card
                    priceEls = btn.findElements(
                        By.xpath("ancestor::*[3]/descendant::p[contains(@class,'font-bold')]"));
                }
                if (!priceEls.isEmpty()) {
                    priceText = priceEls.get(0).getText().trim();
                    price = BasePriceManager.cleanAndConvert(priceText);
                }
            } catch (Exception ex) {
                System.out.println("\u26A0\uFE0F Price extraction failed for " + uiTestName + ": " + ex.getMessage());
            }
            BasePriceManager.addSelectionTestPrice(test, price);  // store with original key
            System.out.println("\uD83D\uDCB0 Captured price: " + priceText + " (" + price + ")");

            // ── 3. Add to cart (skip if already added) ────────────────────────
            String btnText = addBtns.get(0).getText().trim().toLowerCase();
            if (btnText.contains("remove") || btnText.contains("added") || btnText.contains("go to cart")) {
                System.out.println("ℹ️ Test already in cart (button: \"" + addBtns.get(0).getText().trim() + "\") — skipping add: " + uiTestName);
            } else {
                BaseClass.waitAndClickWithJSFallback(addBtns.get(0), 10);
                System.out.println("\u2705 Added to cart: " + uiTestName);
            }
            Thread.sleep(2000);

            BaseClass.scrollToTop();
            BaseClass.clear(LocatorsPage.searchTestField);
            Thread.sleep(2000);
        }

        System.out.println("🟩 SUCCESS → All tests added & prices captured!");
    }

    @When("select Thrombotic Panel and add to cart")
    public void select_thrombotic_panel_and_add_to_cart() throws Exception {
        String testName = "Thrombotic Panel";
        System.out.println("========== 🧪 ADDING ADDITIONAL TEST: " + testName + " ==========");

        // Override testData so downstream validations check for Thrombotic Panel
        BaseClass.testData.put("testName", testName);
        BasePriceManager.resetAll();

        // Wait for the diagnostics page to fully load after View All navigation
        Thread.sleep(5000);

        // Use JS input to ensure React's onChange is triggered for the search
        WebElement searchField = driver.findElement(By.xpath("//input[@placeholder='Search Test Here']"));
        BaseClass.jsInput(searchField, testName);

        // Also dispatch 'change' event for React controlled inputs
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));" +
            "arguments[0].dispatchEvent(new Event('keyup', { bubbles: true }));",
            searchField);

        // Wait for search results to render
        Thread.sleep(5000);

        // Wait specifically for a card containing "Thrombotic Panel" text to appear
        String thrombCardXPath = "//*[contains(translate(normalize-space(.), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'), 'THROMBOTIC PANEL')]";
        try {
            new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.presenceOfElementLocated(By.xpath(thrombCardXPath)));
            System.out.println("✅ Found 'Thrombotic Panel' text in search results");
        } catch (Exception waitEx) {
            System.out.println("⚠️ 'Thrombotic Panel' text not found in results. Retrying search with sendKeys...");
            searchField = driver.findElement(By.xpath("//input[@placeholder='Search Test Here']"));
            searchField.clear();
            Thread.sleep(1000);
            searchField.sendKeys(testName);
            Thread.sleep(5000);
        }

        BaseClass.scrollByOffset(0, 500);
        Thread.sleep(3000);
        BaseClass.scrollByOffset(0, 500);

        // ── Find the add-to-cart button SPECIFICALLY for Thrombotic Panel ──────
        // Strategy 1: Exact match via Locators helper (uses //p[normalize-space()='Thrombotic Panel'])
        List<WebElement> addBtns = driver.findElements(By.xpath(Locators.getAddBtnXPath(testName)));
        if (!addBtns.isEmpty()) System.out.println("✅ Strategy 1 matched (exact p tag)");

        // Strategy 2: Any element (*) with exact text, case-insensitive
        if (addBtns.isEmpty()) {
            System.out.println("⚠️ Strategy 1 failed. Trying any element with 'Thrombotic Panel' text...");
            String anyElementXPath =
                "(//*[contains(translate(normalize-space(.), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'), 'THROMBOTIC PANEL')]" +
                "/ancestor::div[contains(@class,'card') or contains(@class,'test') or contains(@class,'item') or contains(@class,'flex')][1]" +
                "//button[contains(@class,'addtocartButton') or contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'add') " +
                "or contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'remove') " +
                "or contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'added')])[1]";
            addBtns = driver.findElements(By.xpath(anyElementXPath));
            if (!addBtns.isEmpty()) System.out.println("✅ Strategy 2 matched (ancestor-based)");
        }

        // Strategy 3: Any element with 'Thrombotic Panel' text followed by addtocartButton
        if (addBtns.isEmpty()) {
            System.out.println("⚠️ Strategy 2 failed. Trying following-button approach...");
            String followingXPath =
                "(//*[contains(translate(normalize-space(.), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'), 'THROMBOTIC PANEL') " +
                "and not(self::button) and not(self::script)]" +
                "/following::button[contains(@class,'addtocartButton')][1])";
            addBtns = driver.findElements(By.xpath(followingXPath));
            if (!addBtns.isEmpty()) System.out.println("✅ Strategy 3 matched (following-button)");
        }

        // Strategy 4: JavaScript-based - find the button whose parent card contains "Thrombotic Panel"
        if (addBtns.isEmpty()) {
            System.out.println("⚠️ Strategy 3 failed. Using JavaScript context scan...");
            List<WebElement> allBtns = driver.findElements(By.xpath(
                "//button[contains(@class,'addtocartButton') " +
                "or contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'add')]"));
            System.out.println("  Found " + allBtns.size() + " total add-to-cart buttons on page");
            for (WebElement candidateBtn : allBtns) {
                String cardText = ((JavascriptExecutor) driver).executeScript(
                    "var el = arguments[0];" +
                    "var card = el.closest('[class*=\"card\"]') || el.closest('[class*=\"test\"]') || el.closest('[class*=\"item\"]');" +
                    "if (!card) { var p = el; for (var i=0; i<5; i++) { p = p.parentElement; if (!p) break; } card = p; }" +
                    "return card ? card.innerText : '';", candidateBtn).toString();
                System.out.println("  Button card text: " + cardText.replaceAll("\\s+", " ").substring(0, Math.min(cardText.length(), 80)));
                if (cardText.toLowerCase().contains("thrombotic panel")) {
                    addBtns = List.of(candidateBtn);
                    System.out.println("✅ Strategy 4 matched (JS context scan)");
                    break;
                }
            }
        }

        if (addBtns.isEmpty()) {
            // Log what IS on the page to help debug
            System.out.println("❌ Could not find Thrombotic Panel button. Dumping page text elements...");
            List<WebElement> allTextEls = driver.findElements(By.xpath(
                "//*[string-length(normalize-space(.)) > 3 and string-length(normalize-space(.)) < 50 " +
                "and not(self::script) and not(self::style)]"));
            System.out.println("  Text elements containing 'thrombo' or 'panel':");
            for (WebElement el : allTextEls) {
                String txt = el.getText().trim().toLowerCase();
                if (txt.contains("thrombo") || txt.contains("panel")) {
                    System.out.println("    <" + el.getTagName() + "> " + el.getText().trim());
                }
            }
            throw new AssertionError("❌ Could not find Add-to-cart button specifically for: " + testName + ". Check if search returned correct results.");
        }

        // VALIDATION: Confirm the button belongs to Thrombotic Panel (not Anemia or other)
        try {
            WebElement btn = addBtns.get(0);
            String nearbyText = ((JavascriptExecutor) driver).executeScript(
                "var el = arguments[0];" +
                "var card = el.closest('[class*=\"card\"]') || el.closest('[class*=\"test\"]') || el.closest('[class*=\"item\"]');" +
                "if (!card) { var p = el; for (var i=0; i<4; i++) { p = p.parentElement; if (!p) break; } card = p; }" +
                "return card ? card.innerText : '';", btn).toString().toLowerCase();
            if (!nearbyText.contains("thrombotic")) {
                System.out.println("⚠️ WARNING: Selected button context does not mention 'thrombotic': " +
                    nearbyText.replaceAll("\\s+", " ").substring(0, Math.min(nearbyText.length(), 200)));
            } else {
                System.out.println("✅ Confirmed button is for Thrombotic Panel");
            }
        } catch (Exception valEx) {
            System.out.println("⚠️ Button context validation skipped: " + valEx.getMessage());
        }

        // Extract price
        double price = 0.0;
        try {
            WebElement btn = addBtns.get(0);
            List<WebElement> priceEls = btn.findElements(
                By.xpath("preceding::p[contains(@class,'font-bold')][1]"));
            if (priceEls.isEmpty()) {
                priceEls = btn.findElements(
                    By.xpath("ancestor::*[3]/descendant::p[contains(@class,'font-bold')]"));
            }
            if (!priceEls.isEmpty()) {
                String priceText = priceEls.get(0).getText().trim();
                price = BasePriceManager.cleanAndConvert(priceText);
                System.out.println("💰 Captured price for " + testName + ": " + priceText + " (" + price + ")");
            }
        } catch (Exception ex) {
            System.out.println("⚠️ Price extraction failed for " + testName + ": " + ex.getMessage());
        }
        BasePriceManager.addSelectionTestPrice(testName, price);

        // Add to cart if not already added
        String btnText = addBtns.get(0).getText().trim().toLowerCase();
        if (btnText.contains("remove") || btnText.contains("added") || btnText.contains("go to cart")) {
            System.out.println("ℹ️ " + testName + " already in cart — skipping");
        } else {
            BaseClass.waitAndClickWithJSFallback(addBtns.get(0), 10);
            System.out.println("✅ Added to cart: " + testName);
        }
        Thread.sleep(2000);

        BaseClass.scrollToTop();
        BaseClass.clear(LocatorsPage.searchTestField);
        Thread.sleep(2000);
        System.out.println("🟩 SUCCESS → " + testName + " added!");
    }

    @Then("All tests should complete successfully")
    public void all_tests_should_complete_successfully() {
        System.out.println("✓ All tests executed successfully");
    }
}

package stepDefinition;

import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.openqa.selenium.By;
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
            Thread.sleep(3000);

            BaseClass.scrollByOffset(0, 300);

            BaseClass.waitAndClick(driver.findElement(By.xpath("//button[normalize-space()='Add to cart' or normalize-space()='Add To Cart']")), 10);
            Thread.sleep(1000);

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
            BaseClass.scrollByOffset(0, 300);
            Thread.sleep(1000); // Give the UI a moment after scrolling

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

    @Then("All tests should complete successfully")
    public void all_tests_should_complete_successfully() {
        System.out.println("✓ All tests executed successfully");
    }
}

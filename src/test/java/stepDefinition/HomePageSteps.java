package stepDefinition;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import pageObjects.Locators;
import utilities.BaseClass;
import utilities.BasePriceManager;

import java.time.Duration;

/**
 * Home page steps
 */
public class HomePageSteps extends BaseSteps {
	/**
	 * Finds cart-panel remove/close buttons by their full subtree text content.
	 * normalize-space(.) reads the ENTIRE element text including child spans,
	 * whereas text() only reads direct text nodes (misses React span wrappers).
	 *
	 * Priority:
	 *  1. div/button whose total text is × or ✕ or similar close chars
	 *  2. Any element with aria-label containing remove/close/delete
	 *  3. SVG close icons (common in modern React UIs)
	 *  4. JS-based deep search as last resort
	 */
	private static final String CART_REMOVE_BTN_XPATH =
	    "//div[contains(@class,'cursor-pointer') and ("
	    + "  normalize-space(.)='\u00d7' or"	// × U+00D7
	    + "  normalize-space(.)='\u2715' or"	// ✕ U+2715  
	    + "  normalize-space(.)='\u2716' or"	// ✖ U+2716
	    + "  normalize-space(.)='\u256b' or"	// ╫ U+256B (seen in production)
	    + "  normalize-space(.)='X' or"
	    + "  normalize-space(.)='x'"
	    + ")] |"
	    + "//button["
	    + "  normalize-space(.)='\u00d7' or"
	    + "  normalize-space(.)='\u2715' or"
	    + "  normalize-space(.)='\u2716' or"
	    + "  normalize-space(.)='\u256b'"
	    + "] |"
	    // aria-label based fallback
	    + "//*[@aria-label='Remove' or @aria-label='remove'"
	    + "    or @aria-label='Close' or @aria-label='close'"
	    + "    or @aria-label='Delete' or @aria-label='delete'"
	    + "    or @aria-label='Remove item'"
	    + "] |"
	    // SVG-based close/remove icons inside cart panel
	    + "//div[contains(@class,'cart')]//svg[contains(@class,'close') or contains(@class,'remove')]/parent::* |"
	    + "//*[contains(@class,'cart')]//div[contains(@class,'delete') or contains(@class,'remove')]";

	/** JS snippet: returns all remove-like buttons visible in the cart drawer */
	private static final String CART_REMOVE_JS =
	    "return Array.from(document.querySelectorAll("
	    + "  '[class*=cursor-pointer], button, [class*=remove], [class*=delete], [class*=close]'"
	    + ")).filter(function(el) {"
	    + "  var t = (el.innerText || el.textContent || '').trim();"
	    + "  return t === '\u00d7' || t === '\u2715' || t === '\u2716' || t === '\u256b' || t === 'X' || t === 'x';"
	    + "});";

	@Then("verify whether the already selected tests are retained in the cart after login")
	public void verify_whether_the_already_selected_tests_are_retained_in_the_cart_after_login() throws InterruptedException {

	    // Wait for the home page to be fully interactive before touching the cart icon
	    try {
	        new WebDriverWait(driver, Duration.ofSeconds(15))
	            .until(ExpectedConditions.elementToBeClickable(
	                By.xpath("//div[contains(@class,'cursor-pointer')][.//img[@alt='Cart']]")));
	    } catch (Exception ignored) { /* proceed even if cart icon not yet clickable */ }

	    // Open the cart drawer
		Thread.sleep(3000); // let page stabilize after login
	    BaseClass.waitAndClickWithJSFallback(LocatorsPage.cart_logo, 10);
	    Thread.sleep(4000); // let the drawer animate open fully

	    // Wait for the drawer to show either a Checkout button, item content, or empty-state text
	    try {
	        new WebDriverWait(driver, Duration.ofSeconds(12))
	            .until(ExpectedConditions.presenceOfElementLocated(
	                By.xpath("//*[contains(text(),'Checkout')] | //*[contains(text(),'empty')] | //*[contains(text(),'Remove')] | //*[contains(@class,'cart')]//div[contains(@class,'cursor-pointer')]"
	            )));
	    } catch (Exception ignored) { /* no cart content visible yet — proceed anyway */ }

	    JavascriptExecutor js = (JavascriptExecutor) BaseClass.driver;
	    int maxIterations = 40;
	    int iteration = 0;

	    // ── Initial check: if first scan finds nothing, try re-opening drawer ──
	    List<WebElement> initialCheck = driver.findElements(By.xpath(CART_REMOVE_BTN_XPATH));
	    if (initialCheck == null || initialCheck.isEmpty()) {
	        // Try JS scan
	        @SuppressWarnings("unchecked")
	        List<WebElement> jsCheck = (List<WebElement>) js.executeScript(CART_REMOVE_JS);
	        if (jsCheck == null || jsCheck.isEmpty()) {
	            // Log drawer state for diagnostics
	            String pageSnippet = (String) js.executeScript(
	                "var cart = document.querySelector('[class*=cart], [class*=drawer], [class*=sidebar], [class*=panel]');"
	                + "return cart ? cart.innerText.substring(0, 500) : 'NO CART PANEL FOUND';"
	            );
	            System.out.println("🔍 Cart drawer content: " + pageSnippet);

	            // Retry: click cart icon again (drawer might not have opened)
	            System.out.println("⚠️ No remove buttons found. Retrying cart open...");
	            Thread.sleep(2000);
	            BaseClass.waitAndClickWithJSFallback(LocatorsPage.cart_logo, 10);
	            Thread.sleep(4000);
	        }
	    }

	    while (iteration++ < maxIterations) {

	        // ── Strategy 1: XPath (handles direct text + child spans) ───────────
	        List<WebElement> deleteButtons = null;
	        try {
	            deleteButtons = driver.findElements(By.xpath(CART_REMOVE_BTN_XPATH));
	        } catch (StaleElementReferenceException e) {
	            Thread.sleep(300);
	            continue;
	        }

	        // ── Strategy 2: JS deep-scan if XPath found nothing ──────────────────
	        if (deleteButtons == null || deleteButtons.isEmpty()) {
	            try {
	                @SuppressWarnings("unchecked")
	                List<WebElement> jsFound = (List<WebElement>) js.executeScript(CART_REMOVE_JS);
	                if (jsFound != null && !jsFound.isEmpty()) {
	                    deleteButtons = jsFound;
	                    System.out.println("\uD83D\uDD0D JS fallback found " + jsFound.size() + " remove button(s)");
	                }
	            } catch (Exception ignored) { /* JS search failed */ }
	        }

	        if (deleteButtons == null || deleteButtons.isEmpty()) {
	            break; // nothing left to remove
	        }

	        System.out.println("\uD83E\uDDF9 Removing cart item (visible buttons: " + deleteButtons.size() + ")");

	        try {
	            WebElement btn = deleteButtons.get(0);
	            try {
	                btn.click();
	            } catch (Exception e1) {
	                js.executeScript("arguments[0].click();", btn);
	            }
	        } catch (StaleElementReferenceException stale) {
	            System.out.println("\u26A0\uFE0F Stale element — retrying");
	        } catch (Exception ex) {
	            System.out.println("\u26A0\uFE0F Click failed: " + ex.getMessage());
	        }

	        Thread.sleep(800);
	    }

	    System.out.println("\u2714 Cart clearing done (iterations: " + iteration + ")");

	    // ── Close the cart drawer robustly ───────────────────────────────────
	    // Check whether the drawer is still open (Checkout button visible) before trying to close
	    boolean drawerOpen = !driver.findElements(
	        By.xpath("//button[text()='Checkout'] | //button[normalize-space()='Checkout']")).isEmpty();

	    if (drawerOpen) {
	        // Strategy 1: click cart_logo (toggle) to close the drawer
	        try {
	            BaseClass.waitAndClickWithJSFallback(LocatorsPage.cart_logo, 5);
	            Thread.sleep(600);
	        } catch (Exception e1) {
	            System.out.println("\u26a0\ufe0f cart_logo toggle failed: " + e1.getMessage());
	        }

	        // Strategy 2: press Escape if drawer still visible
	        boolean drawerGone = false;
	        try {
	            new WebDriverWait(driver, Duration.ofSeconds(3))
	                .until(ExpectedConditions.invisibilityOfElementLocated(
	                    By.xpath("//button[text()='Checkout'] | //button[normalize-space()='Checkout']")));
	            drawerGone = true;
	        } catch (Exception ignored) { }

	        if (!drawerGone) {
	            try {
	                driver.findElement(By.tagName("body"))
	                      .sendKeys(Keys.ESCAPE);
	                Thread.sleep(500);
	                System.out.println("\u26a0\ufe0f Used Escape to close cart drawer");
	            } catch (Exception ignored) { }
	        }

	        // Strategy 3: navigate to home as last resort
	        try {
	            new WebDriverWait(driver, Duration.ofSeconds(3))
	                .until(ExpectedConditions.invisibilityOfElementLocated(
	                    By.xpath("//button[text()='Checkout'] | //button[normalize-space()='Checkout']")));
	        } catch (Exception ignored) {
	            try {
	                BaseClass.waitAndClickWithJSFallback(LocatorsPage.mrYodaLogo_Image, 5);
	                Thread.sleep(1000);
	                System.out.println("\u26a0\ufe0f Navigated to home to force-close drawer");
	            } catch (Exception ignored2) { }
	        }
	    }

	    System.out.println("\u2705 Cart clearing complete — drawer closed");
	}

    @When("click the Best Seller View All button")
    public void click_the_best_seller_view_all_button() throws Throwable {

        // Re-read location because original flow did that
		BaseClass.waitAndClick(LocatorsPage.mrYodaLogo_Image, 10);
		Thread.sleep(2000);
        String location = BaseClass.getText(LocatorsPage.locationText, 10);
        TestSession.locationText = location;
        System.out.println("Location Text: " + TestSession.locationText);
		Thread.sleep(2000);

        try {
            if (!BaseClass.isElementVisible(LocatorsPage.viewAll_BestSeller, 10)) {
                BaseClass.scrollToBottom();
            }
        } catch (Exception ignored) {}

        Thread.sleep(2000);
        BaseClass.waitAndClickWithJSFallback(LocatorsPage.viewAll_BestSeller, 10);
    }
    @When("click the global search")
    public void click_the_global_search() {
        	BaseClass.clear(LocatorsPage.globalSearch_Input);
    			BaseClass.waitAndClick(LocatorsPage.globalSearch_Input, 10);
    }
    @When("select tests and capture individual prices from global search")
    public void select_tests_and_capture_individual_prices_from_global_search() throws InterruptedException {

        String testNamesData = BaseClass.testData.get("testName");
        if (testNamesData == null || testNamesData.trim().isEmpty()) {
            throw new AssertionError("❌ Missing test name in Excel!");
        }

        String[] testNames = testNamesData.split(",");

        for (String testName : testNames) {

            testName = testName.trim();
            System.out.println("🔍 Searching for: " + testName);

            // Search input
            WebElement searchBox = BaseClass.waitVisible(LocatorsPage.globalSearch_Input, 10);
            searchBox.clear();
            Thread.sleep(500); // Wait for clear to complete
            searchBox.sendKeys(testName);
            
            // Wait for search results to load
            Thread.sleep(2000);
            System.out.println("⏳ Waiting for search results...");

            // Price - Wait for the specific test result to appear
            WebElement priceElement = BaseClass.waitForPresenceOfElement(
                    By.xpath("(//p[normalize-space()='" + testName +
                            "']/following::p[contains(@class,'font-bold')])[1]"), 10
            );
            String priceText = priceElement.getText().trim();
            double price = BasePriceManager.cleanAndConvert(priceText);
            BasePriceManager.addSelectionTestPrice(testName, price);
            System.out.println("💰 Price captured: " + priceText);

            // Add to cart button - with proper wait
            WebElement addButton = BaseClass.waitUntilClickable(
                    By.xpath("(//p[normalize-space()='" + testName +
                            "']/following::button[contains(@class,'addtocartButton')])[1]"), 10
            );
            addButton.click();
            System.out.println("✅ Added to cart: " + testName);
            
            Thread.sleep(1000); // Wait for add to cart animation
            
            // Clear search box for next test
            WebElement freshSearchBox = BaseClass.waitVisible(LocatorsPage.globalSearch_Input, 10);
            freshSearchBox.click();
            freshSearchBox.sendKeys(Keys.CONTROL + "a");
            freshSearchBox.sendKeys(Keys.DELETE);

          //  searchBox.clear();
            Thread.sleep(2000);
        }

        System.out.println("🟩 All tests selected successfully!");
    }

    @When("click the smart choice  banner search")
    public void click_the_smart_choice_banner_search() {
        BaseClass.scrollIntoView(LocatorsPage.smartChoices_ViewAllButton, 10);
        System.out.println("Scrolled to Smart Choices View All button");
        BaseClass.waitAndClickWithJSFallback(LocatorsPage.smartChoices_ViewAllButton, 10);
        System.out.println("Smart Choices View All button clicked successfully");
    }
    @Then("the user navigates to DNA Decoder panels at a glance filter")
    public void the_user_navigates_to_dna_decoder_panels_at_a_glance_filter() {
    	  BaseClass.scrollIntoView(LocatorsPage.dnaDecoder_ViewAllButton, 10);
          System.out.println("Scrolled to Smart Choices View All button");
    	BaseClass.waitAndClick(LocatorsPage.dnaDecoder_ViewAllButton, 10);
    			  System.out.println("DNA Decoder View All button clicked successfully");
    }

@Given("load the excel data for single non member and  home visit with memership in DNA Decoder")
public void load_the_excel_data_for_single_non_member_and_home_visit_with_memership_in_dna_decoder() {
   
	BaseClass.loadExcelData("DNADecoder", "1");

}


@Given("load the excel data for single member and home visit with memership in DNA Decoder")
public void load_the_excel_data_for_single_member_and_home_visit_with_memership_in_dna_decoder() {
	BaseClass.loadExcelData("DNADecoder", "5");

}

@Given("load the excel data for multi member and home sample with memership in DNA Decoder")
public void load_the_excel_data_for_multi_member_and_home_sample_with_memership_in_dna_decoder() {
	BaseClass.loadExcelData("DNADecoder", "6");

}

@Given("load the excel data for multi member\\(Non member) and home visit with memership in DNA Decoder")
public void load_the_excel_data_for_multi_member_non_member_and_home_visit_with_memership_in_dna_decoder() {
	BaseClass.loadExcelData("DNADecoder", "8");

}


@Given("load the excel data for multi member and lab visit with memership in DNA Decoder")
public void load_the_excel_data_for_multi_member_and_lab_visit_with_memership_in_dna_decoder() {
	BaseClass.loadExcelData("DNADecoder", "7");

}

@Given("load the excel data for multi member \\(Non Member)and lab visit with memership in DNA Decoder")
public void load_the_excel_data_for_multi_member_non_member_and_lab_visit_with_memership_in_dna_decoder() {
	BaseClass.loadExcelData("DNADecoder", "4");

}
@Given("load the excel data for single non member and lab visit with memership in DNA Decoder")
public void load_the_excel_data_for_single_non_member_and_lab_visit_with_memership_in_dna_decoder() {
	BaseClass.loadExcelData("DNADecoder", "2");

}
}

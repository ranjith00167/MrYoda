package stepDefinition;

import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import utilities.BaseClass;
import utilities.BasePriceManager;
import stepDefinition.TestSession;

public class CheckoutPageSteps extends BaseSteps {

    @When("click on the cart icon")
    public void click_on_the_cart_icon() {
        BaseClass.waitAndClick(LocatorsPage.cart_logo, 10);
    }

    @Then("validate checkout summary header values")
    public void validate_checkout_summary_header_values() {

        System.out.println("\n================= 🧾 CHECKOUT VALIDATION START =================");

        BasePriceManager.resetCheckoutOnly();

        String testNamesData = BaseClass.testData.get("testName");
        if (testNamesData == null || testNamesData.isBlank()) {
            throw new AssertionError("❌ Test names missing in Excel!");
        }

        String[] excelTests = testNamesData.split(",");
        List<String> excelList = Arrays.stream(excelTests)
                .map(String::trim)
                .collect(Collectors.toList());

        System.out.println("\n📌 Tests Loaded From Excel:");
        excelList.forEach(t -> System.out.println("   • " + t));

        double totalCalculated = 0.0;
        int captured = 0;

        System.out.println("\n📌 Validating Each Test Against Checkout Page:\n");

        // Wait for at least one product row to be visible before validating.
        // Do NOT constrain to 'divide-y' parent — that class is absent on the new-user checkout page.
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(12))
                .until(org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//div[contains(@class,'text-textHeading')]"
                )));
        } catch (Exception e) {
            System.out.println("⚠️ Checkout product list did not load within 12s — proceeding with validation anyway");
        }

        // In multi-member scenarios, only some tests are in the cart at this step.
        // Read the actual cart count from the header ("X Tests/Packages in Cart") and
        // validate only that many tests — the rest will be added when members are assigned.
        int cartCount = excelList.size(); // default: validate all
        try {
            String headerText = LocatorsPage.checkoutSummaryHeader.getText();
            java.util.regex.Matcher hm = java.util.regex.Pattern
                    .compile("(\\d+)\\s*Tests?/Packages?", java.util.regex.Pattern.CASE_INSENSITIVE)
                    .matcher(headerText);
            if (hm.find()) {
                cartCount = Integer.parseInt(hm.group(1));
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not read cart count from header — validating all Excel tests");
        }
        int testsToValidate = Math.min(excelList.size(), cartCount);
        if (testsToValidate < excelList.size()) {
            System.out.println("ℹ️ Cart has " + cartCount + " item(s) but Excel has " + excelList.size()
                    + " — validating first " + testsToValidate + " (remaining are for other members added later)");
        }

        for (int i = 0; i < testsToValidate; i++) {

            String testName = excelList.get(i);

            // Primary: look for the row inside a divide-y container (member flow)
            // Fallback: find the row anywhere on the page (new-user / single-member flow)
            String rowXpath = String.format(
                "//*[" +
                    ".//div[contains(@class,'text-textHeading') and contains(normalize-space(.), '%s')]" +
                    " and .//*[contains(@class,'cursor-pointer')]" +
                "]",
                testName
            );
            // Use the most-specific (deepest) matching ancestor — filter out wrappers
            // that contain the whole cart by requiring a cursor-pointer price block as a sibling.
            String narrowXpath = String.format(
                "//div[" +
                    ".//div[contains(@class,'text-textHeading') and contains(normalize-space(.), '%s')]" +
                    " and .//div[contains(@class,'cursor-pointer')]" +
                "][not(.//div[contains(@class,'divide-y')])]" +
                " | " +
                "//div[contains(@class,'divide-y')]//div[" +
                    ".//div[contains(@class,'text-textHeading') and contains(normalize-space(.), '%s')]" +
                "]",
                testName, testName
            );

            List<WebElement> rows = driver.findElements(By.xpath(narrowXpath));
            // Pick the shallowest element that still contains a price block
            if (rows.isEmpty()) {
                rows = driver.findElements(By.xpath(rowXpath));
            }
            if (rows.isEmpty()) {
                // Debug: print all text-textHeading elements found on page to diagnose name mismatch
                List<WebElement> allHeaders = driver.findElements(
                    By.xpath("//div[contains(@class,'text-textHeading')]"));
                System.out.println("🔍 DEBUG: Found " + allHeaders.size() + " text-textHeading elements on page:");
                allHeaders.forEach(h -> System.out.println("   • [" + h.getText().trim() + "]"));
                throw new AssertionError("❌ NOT FOUND in checkout → " + testName);
            }

            WebElement row = rows.get(0);

            // Extract price — always last value
            WebElement priceBlock = row.findElement(
                    By.xpath(".//div[contains(@class,'cursor-pointer')]/span")
            );

            String combined = priceBlock.getText()
                    .replaceAll("\\s+", " ")
                    .trim();

            Matcher matcher = Pattern.compile("₹\\s*[0-9,]+").matcher(combined);
            List<String> prices = new ArrayList<>();
            while (matcher.find()) prices.add(matcher.group().trim());

            String rawPrice = prices.get(prices.size() - 1);
            double checkoutPrice = BasePriceManager.cleanAndConvert(rawPrice);

            double selectionPrice = BasePriceManager.getSelectionPrices().get(i);

            if (Math.abs(checkoutPrice - selectionPrice) > 0.1) {
                throw new AssertionError("❌ PRICE MISMATCH for: " + testName +
                        " | Selection: ₹" + selectionPrice +
                        " | Checkout: ₹" + checkoutPrice);
            }

            System.out.println("   ✔ Price Matched → ₹" + checkoutPrice);

            BasePriceManager.addCheckoutTestPrice(testName, checkoutPrice);
            totalCalculated += checkoutPrice;
            captured++;
        }

        System.out.println("\n📌 Matching Completed. Captured: " + captured);

        // Header Validation
        Matcher countMatcher = Pattern.compile("(\\d+)\\s*Tests?/Packages?",
                Pattern.CASE_INSENSITIVE)
                .matcher(LocatorsPage.checkoutSummaryHeader.getText());

        int headerCount = countMatcher.find() ? Integer.parseInt(countMatcher.group(1)) : 0;
        if (headerCount != captured) {
            throw new AssertionError("Count mismatch → Header: " + headerCount + " | Captured: " + captured);
        }

        Matcher priceMatcher = Pattern.compile("₹\\s*([\\d,]+)")
                .matcher(LocatorsPage.checkoutSummaryHeader.getText());

        double headerTotal = priceMatcher.find()
                ? Double.parseDouble(priceMatcher.group(1).replace(",", ""))
                : 0;

        if (Math.abs(headerTotal - totalCalculated) > 0.1) {
            throw new AssertionError("Total mismatch → Header: ₹" + headerTotal +
                    " | Calculated: ₹" + totalCalculated);
        }

        TestSession.totalCheckoutAmount = (int) Math.round(headerTotal);

        System.out.println("\n================= ✅ CHECKOUT VALIDATION PASSED =================\n");

        BaseClass.waitAndClick(LocatorsPage.checkoutButton, 10);
    }


    @When("validate individual checkout prices")
    public void validate_individual_checkout_prices() {

        System.out.println("========== 🔍 VALIDATING INDIVIDUAL CHECKOUT PRICES ==========");

        String[] tests = BaseClass.getTestNamesFromExcel("Diagnostics");

        for (String testName : tests) {

            String rowXpath = String.format(
                "//div[contains(@class,'divide-y')]//div[" +
                ".//div[contains(@class,'text-textHeading') and contains(normalize-space(.), \"%s\")]" +
                "]",
                testName
            );

            List<WebElement> rows = driver.findElements(By.xpath(rowXpath));

            if (rows.isEmpty()) {
                System.out.println("⚠️ Skipping → test NOT FOUND in checkout: " + testName);
                continue;
            }

            WebElement row = rows.get(0);

            WebElement priceEl = row.findElement(
                By.xpath(".//div[contains(@class,'cursor-pointer')]//span[" +
                         "translate(text(),'0123456789','9999999999') != text()" +
                         "][last()]")
            );

            String rawPrice = priceEl.getText().trim();
            double price = BasePriceManager.cleanAndConvert(rawPrice);

            System.out.println("TEST: " + testName + " → Checkout Price: ₹" + price);

            BasePriceManager.addCheckoutTestPrice(testName, price);
        }

        System.out.println("========== ✅ CHECKOUT PRICE EXTRACTION DONE ==========");
    }

    @Then("extract the total amount during checkout for the multi-member scenario")
    public void extract_the_total_amount_during_checkout_for_the_multi_member_scenario() {

        WebElement totalValueElement = LocatorsPage.checkout_Total_price;
        if (totalValueElement == null) {
            throw new AssertionError("Checkout total element not found on page.");
        }

        String totalText = totalValueElement.getText();
        String CheckoutTotalValue = totalText.replaceAll("[^0-9]", "");
        if (CheckoutTotalValue.isEmpty()) {
            throw new AssertionError("Unable to parse checkout total. Raw text: " + totalText);
        }

        TestSession.totalCheckoutAmount = Integer.parseInt(CheckoutTotalValue);
        System.out.println("Total Amount from Checkout: ₹" + TestSession.totalCheckoutAmount);

        BaseClass.waitAndClick(LocatorsPage.proceed_cart, 10);
    }
    @Then("validate checkout summary header values of DNA Decoder tests")
    public void validate_checkout_summary_header_values_of_dna_decoder_tests() {

        System.out.println("\n================= 🧾 CHECKOUT VALIDATION START =================");

        BasePriceManager.resetCheckoutOnly();

        String excelData = BaseClass.testData.get("testName");
        String[] excelTests = excelData.split(",");
        List<String> excelList = List.of(excelTests);

        System.out.println("\n📌 Tests Loaded From Excel:");
        excelList.forEach(t -> System.out.println("   • " + t));

        int captured = 0;
        double totalCalculated = 0.0;

        System.out.println("\n📌 Validating Each Test From Excel Against Checkout Page:\n");

        for (int i = 0; i < excelList.size(); i++) {

            String testName = excelList.get(i);

            String rowXpath = String.format(
                    "//div[contains(@class,'divide-y')]//div[" +
                            "translate(normalize-space(.//div[contains(@class,'text-textHeading')])," +
                            "'ABCDEFGHIJKLMNOPQRSTUVWXYZ'," +
                            "'abcdefghijklmnopqrstuvwxyz')" +
                            " = '%s'" +
                            "]",
                    testName.toLowerCase()
            );

            List<WebElement> rows = driver.findElements(By.xpath(rowXpath));

            if (rows.isEmpty()) {
                System.out.println("❌ NOT FOUND in checkout → " + testName);
                throw new AssertionError("Expected test NOT found in checkout: " + testName);
            }

            WebElement row = rows.get(0);

            // ==========================================================
            // 🔥 CORRECT FINAL PRICE EXTRACTION (WORKS FOR ALL DOM TYPES)
            // ==========================================================

            // extract <span> block containing both strike + discount OR single price
            WebElement priceBlock = row.findElement(
                    By.xpath(".//div[contains(@class,'cursor-pointer')]/span")
            );

            String combined = priceBlock.getText()
                    .replaceAll("\\s+", " ")
                    .trim(); // e.g. "₹30,000 ₹25,000" or "₹1,00,000"

            // extract ALL prices from the string
            List<String> prices = new ArrayList<>();
            Matcher matcher = Pattern.compile("₹\\s*[0-9,]+").matcher(combined);

            while (matcher.find()) {
                prices.add(matcher.group().trim());
            }

            if (prices.isEmpty()) {
                throw new RuntimeException("❌ No price values found for: " + testName);
            }

            // ALWAYS take the LAST price (discounted or only price)
            String rawPrice = prices.get(prices.size() - 1);

            double checkoutPrice = BasePriceManager.cleanAndConvert(rawPrice);

            // Store & accumulate
            BasePriceManager.addCheckoutTestPrice(testName, checkoutPrice);
            totalCalculated += checkoutPrice;
            captured++;

            // ==========================================================
            // 💠 VALIDATE SELECTION PRICE VS CHECKOUT PRICE
            // ==========================================================
            if (BasePriceManager.getSelectionPrices().size() <= i) {
                throw new AssertionError("Selection price missing for index " + i);
            }

            double selectionPrice = BasePriceManager.getSelectionPrices().get(i);

            if (Math.abs(checkoutPrice - selectionPrice) > 0.1) {
                throw new AssertionError("❌ PRICE MISMATCH for " + testName +
                        " | Selection: ₹" + selectionPrice +
                        " | Checkout: ₹" + checkoutPrice);
            }

            System.out.println("   ✔ Price Matched → ₹" + checkoutPrice);
            System.out.println();
        }

        System.out.println("\n📌 Matching Completed. Captured: " + captured);

        // ==========================================================
        // 🔥 HEADER SUMMARY VALIDATION
        // ==========================================================
        WebElement header = LocatorsPage.checkoutSummaryHeader;
        String headerText = header.getText().trim();

        Matcher countMatcher = Pattern.compile("(\\d+)\\s*Tests?/Packages?",
                Pattern.CASE_INSENSITIVE).matcher(headerText);

        Matcher priceMatcher = Pattern.compile("₹\\s*([\\d,]+)").matcher(headerText);

        int headerCount = countMatcher.find() ? Integer.parseInt(countMatcher.group(1)) : 0;
        double headerTotal = priceMatcher.find()
                ? Double.parseDouble(priceMatcher.group(1).replace(",", ""))
                : 0;

        if (headerCount != captured) {
            throw new AssertionError("Count mismatch → Header: " + headerCount + " | Captured: " + captured);
        }

        if (Math.abs(headerTotal - totalCalculated) > 0.1) {
            throw new AssertionError("Total mismatch → Header: ₹" + headerTotal +
                    " | Calculated: ₹" + totalCalculated);
        }

        TestSession.totalCheckoutAmount = (int) Math.round(headerTotal);

        System.out.println("\n================= ✅ CHECKOUT VALIDATION PASSED =================\n");

        BaseClass.waitAndClick(LocatorsPage.checkoutButton, 10);
    }

}

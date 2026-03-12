package stepDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import utilities.BaseClass;
import utilities.BasePriceManager;

/**
 * Multi-member selection and validation.
 * After selecting members, it also ensures overall checkout totals reflect the selections.
 */
public class AddMemberPageSteps extends BaseSteps {
	    public static List<String> uiMemberNames = new ArrayList<>();
	    public static List<String> excelMemberNames = new ArrayList<>();

    @When("select the member")
    public void select_the_member()  throws Throwable{
        BaseClass.waitAndClick(LocatorsPage.members_tab, 10);
        Thread.sleep(4000);
        BaseClass.waitAndClick(LocatorsPage.proceed_cart, 10);
    }

    @When("select the member for multi member scenario")
    public void select_the_member_for_multi_member_scenario() throws Throwable {
 
        BaseClass.waitAndClick(LocatorsPage.members_tab, 10);
        Thread.sleep(1500);
 
        String[] excelMembers =BaseClass. testData.get("memberNames").split(",");
        List<String> targetMembers = new ArrayList<>();
        for (String m : excelMembers) {
            String trimmed = m.trim();
            if (!trimmed.isEmpty()) {
                targetMembers.add(trimmed);
            }
        }
        com.mryoda.diagnostics.api.utils.RequestContext.setMemberIds(targetMembers);
 
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🎯 Processing " + targetMembers.size() + " members from Excel");
        System.out.println("=".repeat(80));
 
        for (String memberName : targetMembers) {
 
            System.out.println("\n➡ Processing Member: " + memberName);
 
            String rawName = memberName == null ? "" : memberName.trim();
            String[] parts = rawName.split("\\s+");
            String firstName = parts.length > 0 && !parts[0].isEmpty() ? parts[0] : rawName;
            System.out.println("   🔍 First Name extracted: [" + firstName + "]");
 
            String memberCardXpath = String.format(
                "//div[contains(@class,'rounded') and contains(@class,'bg-') and .//text()[contains(.,'%s')] and .//text()[contains(.,'tests selected')]]",
                firstName
            );
 
            WebElement memberCard = null;
            boolean foundSpecificCard = false;
 
            for (int attempt = 1; attempt <= 10; attempt++) {
                try {
                    List<WebElement> cards = driver.findElements(By.xpath(memberCardXpath));
                    System.out.println("   🔎 Attempt " + attempt + ": Found " + cards.size() + " potential cards");
 
                    for (WebElement card : cards) {
                        String cardText = card.getText();
                        boolean hasThisMember = cardText.contains(firstName);
                        boolean hasTestsSelected = cardText.contains("tests selected");
 
                        boolean isSpecificCard = true;
                        int memberCount = 0;
                        for (String otherMember : targetMembers) {
                            String otherFirst = otherMember.split(" ")[0];
                            if (cardText.contains(otherFirst)) {
                                memberCount++;
                            }
                        }
                        if (memberCount > 1) {
                            isSpecificCard = false;
                            System.out.println("   ⚠️  Skipping parent container (contains " + memberCount + " members)");
                        }
 
                        if (hasThisMember && hasTestsSelected && isSpecificCard) {
                            memberCard = card;
                            foundSpecificCard = true;
                            System.out.println("   ✅ Found SPECIFIC card for: " + memberName);
                            System.out.println("   📄 Card text preview: " + cardText.substring(0, Math.min(80, cardText.length())).replaceAll("\n", " | "));
                            break;
                        }
                    }
 
                    if (foundSpecificCard) break;
 
                } catch (Exception e) {
                    System.out.println("   🔄 Attempt " + attempt + " - " + e.getMessage());
                }
 
                if (attempt < 10 && !foundSpecificCard) {
                    BaseClass.scrollModal(250);
                    Thread.sleep(400);
                }
            }
 
            if (!foundSpecificCard || memberCard == null) {
                System.out.println("   ❌ Member not found → " + memberName);
                continue;
            }
 
            BaseClass.scrollIntoView(memberCard, 5);
            Thread.sleep(500);
 
            String cardText = memberCard.getText();
            Pattern pattern = Pattern.compile("(\\d+)\\s*tests selected");
            Matcher matcher = pattern.matcher(cardText);
 
            int currentTests = 0;
            if (matcher.find()) {
                currentTests = Integer.parseInt(matcher.group(1));
            }
 
            System.out.println("   📊 Current tests: " + currentTests);
 
            if (currentTests == 0) {
 
                List<WebElement> selectAllButtons = memberCard.findElements(
                    By.xpath(".//*[text()='Select All']")
                );
 
                if (selectAllButtons.isEmpty()) {
                    System.out.println("   🔽 Expanding member section...");
                    BaseClass.waitAndClickWithJSFallback(memberCard, 3);
                    Thread.sleep(1000);
 
                    List<WebElement> refreshedCards = driver.findElements(By.xpath(memberCardXpath));
                    for (WebElement card : refreshedCards) {
                        String text = card.getText();
                        int count = 0;
                        for (String m : targetMembers) {
                            if (text.contains(m.split(" ")[0])) count++;
                        }
                        if (count == 1 && text.contains(firstName)) {
                            memberCard = card;
                            break;
                        }
                    }
 
                    selectAllButtons = memberCard.findElements(By.xpath(".//*[text()='Select All']"));
                } else {
                    System.out.println("   ✓ Already expanded");
                }
 
                if (!selectAllButtons.isEmpty()) {
 
                    WebElement selectAllBtn = selectAllButtons.get(0);
                    BaseClass.scrollIntoView(selectAllBtn, 2);
                    Thread.sleep(300);
                    selectAllBtn.click();
                    System.out.println("   ✔ Clicked 'Select All' for: " + memberName);
                    Thread.sleep(1500);
 
                    List<WebElement> refreshedCards = driver.findElements(By.xpath(memberCardXpath));
                    for (WebElement card : refreshedCards) {
                        String text = card.getText();
                        int count = 0;
                        for (String m : targetMembers) {
                            if (text.contains(m.split(" ")[0])) count++;
                        }
                        if (count == 1 && text.contains(firstName)) {
                            String updatedText = card.getText();
                            Matcher m = pattern.matcher(updatedText);
                            if (m.find()) {
                                int newTests = Integer.parseInt(m.group(1));
                                System.out.println("   ✅ VERIFIED: " + newTests + " tests now selected");
                            }
 
                            try {
                                List<WebElement> collapseBtn = card.findElements(By.xpath(".//*[contains(text(),'tests selected')]"));
                                if (!collapseBtn.isEmpty()) {
                                    collapseBtn.get(0).click();
                                    System.out.println("   🔽 Collapsed section");
                                    Thread.sleep(600);
                                }
                            } catch (Exception e) {
                                System.out.println("   ⚠️  Could not collapse");
                            }
 
                            break;
                        }
                    }
 
                } else {
                    System.out.println("   ⚠️  'Select All' button not found");
                }
 
            } else {
                System.out.println("   ⏭️  SKIPPED: Already has " + currentTests + " tests");
            }
 
            System.out.println("   ✓ Completed: " + memberName);
        }
 
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🎯 Clicking Proceed...");
        System.out.println("=".repeat(80) + "\n");
    }
 
 
 
    // -------- UI MEMBER NAME EXTRACTION --------
    @When("the user extract the member names from the page")
    public void the_user_extract_the_member_names_from_the_page() {

        uiMemberNames.clear();
        System.out.println("\n🔍 Extracting UI Member Names...");

        for (WebElement element : LocatorsPage.memberElements) {
            String name = element.getText().replace("(Self)", "").trim();
            if (!name.isEmpty()) {
                uiMemberNames.add(name);
            }
        }

        if (uiMemberNames.isEmpty()) {
            throw new AssertionError("❌ No UI member names captured!");
        }

        System.out.println("🟩 UI Member Names: " + uiMemberNames);
    }

    @Then("validate selected tests and prices inside member popup")
    public void validate_selected_tests_and_prices_inside_member_popup() throws Throwable {

        System.out.println("\n========== POPUP VALIDATION ==========");

        for (String member : BasePriceManager.getAllMemberNames()) {

            String firstName = member.split(" ")[0];

            String cardXpath = "//div[contains(@class,'rounded') and contains(text(),'" + firstName + "')]";
            WebElement memberCard = driver.findElement(By.xpath(cardXpath));
            BaseClass.scrollIntoView(memberCard, 5);
            memberCard.click();
            Thread.sleep(700);

            List<String> expectedTests = BasePriceManager.getMemberTests(firstName);
            System.out.println("\n➡ Validating: " + firstName + " → " + expectedTests);

            for (String test : expectedTests) {

                String normalized = test.replace("-1","").replace("-2","").trim();

                String testXpath = ".//p[contains(normalize-space(),'" + normalized + "')]";
                List<WebElement> testRows = memberCard.findElements(By.xpath(testXpath));

                if(testRows.isEmpty())
                    throw new AssertionError("❌ Missing test under popup: " + test);

                WebElement priceElement = memberCard.findElement(
                        By.xpath(testXpath + "/../following-sibling::div//p[contains(@class,'font-bold')]")
                );

                String priceTxt = priceElement.getText().trim();
                double price = BasePriceManager.cleanAndConvert(priceTxt);
                BasePriceManager.addUnitPrice(normalized, price);

                System.out.println("   ✔ " + normalized + " → " + priceTxt);
            }

            memberCard.click(); // collapse
            Thread.sleep(400);
        }

        System.out.println("\n========== POPUP VALIDATION PASSED ==========");

        // 🚀 👇 Proceed to next step automatically
        BaseClass.waitAndClick(LocatorsPage.proceed_cart, 10);
        System.out.println("➡ Clicked Proceed button");
        Thread.sleep(1500);

        System.out.println("==============================================\n");
    }

    @Then("validate all cart items and totals from excel")
    public void validate_all_cart_items_and_totals_from_excel() {

        System.out.println("\n========== CART VALIDATION ==========");

        double calculatedGrandTotal = 0.0;

        for (String test : BasePriceManager.getAllTestsUnique()) {

            int qty = BasePriceManager.getTotalTestCount(test);
            double unitPrice = BasePriceManager.getUnitPrice(test);
            double expectedTotal = unitPrice * qty;

            System.out.println("\n🔍 " + test + " | Qty: " + qty);

            String cartTestXpath = "//div[contains(text(),'" + test + "')]/..";
            WebElement row = driver.findElement(By.xpath(cartTestXpath));

            String qtyTxt = row.findElement(By.xpath(".//span[contains(text(),'x')]")).getText().replace("x","").trim();
            int uiQty = Integer.parseInt(qtyTxt);

            Assert.assertEquals(uiQty, qty, "❌ Quantity mismatch → " + test);

            String totalTxt = row.findElement(By.xpath(".//span[contains(text(),'₹')]")).getText();
            double uiTotal = BasePriceManager.cleanAndConvert(totalTxt);

            Assert.assertEquals(uiTotal, expectedTotal, 0.1, "❌ Total Mismatch: " + test);
            System.out.println("   ✔ Row total validated: ₹" + expectedTotal);

            calculatedGrandTotal += expectedTotal;
        }

        // Validate Global Cart Total
        String cartTotalTxt = LocatorsPage.amountToPay.getText().trim();
        double cartTotal = BasePriceManager.cleanAndConvert(cartTotalTxt);

        Assert.assertEquals(cartTotal, calculatedGrandTotal, 0.1,
                "❌ Final Cart Total mismatch");

        System.out.println("\n🎉 ALL CART VALIDATIONS PASSED SUCCESSFULLY!");
        System.out.println("==========================================\n");
    }

    // -------- EXCEL NAME EXTRACTION --------
    @When("the user extracts the member names from the excel sheet")
    public void the_user_extracts_the_member_names_from_the_excel_sheet() {

        excelMemberNames.clear();

        // Read from already loaded Excel row (Row 6 in this scenario)
        String memberNamesCell = BaseClass.testData.get("memberNames");
        System.out.println("\n📌 Raw Excel Value → memberNames: " + memberNamesCell);

        if (memberNamesCell == null || memberNamesCell.trim().isEmpty()) {
            throw new AssertionError("❌ Excel member names missing. Fix Excel data!");
        }

        List<String> formattedExcelNames = BaseClass.getUiFormattedMemberNames(memberNamesCell);

        if (formattedExcelNames == null || formattedExcelNames.isEmpty()) {
            throw new AssertionError("❌ Formatting failed. No valid Excel names returned!");
        }

        excelMemberNames.addAll(formattedExcelNames);
        System.out.println("🧹 Formatted Excel Member Names: " + excelMemberNames);
        System.out.println("✔ Excel Member Names successfully extracted & formatted!");
    }


    // -------- VALIDATION --------
    @Then("the member names on the page should match the excel")
    public void the_member_names_on_the_page_should_match_the_excel() {

    	    System.out.println("\n⚖ Validating UI vs Excel Member Names...");

    	    // 1️⃣ Check list sizes first
    	    if (uiMemberNames.size() != excelMemberNames.size()) {
    	        throw new AssertionError(
    	            "❌ Member count mismatch!" +
    	            "\nUI Count: " + uiMemberNames.size() +
    	            "\nExcel Count: " + excelMemberNames.size() +
    	            "\nUI Names: " + uiMemberNames +
    	            "\nExcel Names: " + excelMemberNames
    	        );
    	    }

    	    // 2️⃣ Validate each name index-wise
    	    for (int i = 0; i < uiMemberNames.size(); i++) {
    	        String uiName = uiMemberNames.get(i);
    	        String excelName = excelMemberNames.get(i);

    	        if (!uiName.equals(excelName)) {
    	            throw new AssertionError(
    	                "❌ Member name mismatch at index " + i +
    	                "\nUI: " + uiName +
    	                "\nExcel: " + excelName +
    	                "\n⚠ Names must match exactly (after formatting)"
    	            );
    	        }

    	        System.out.println("✔ Match (" + i + "): " + uiName);
    	    }

    	    System.out.println("🎯 Member names validation PASSED!");
    	}




}
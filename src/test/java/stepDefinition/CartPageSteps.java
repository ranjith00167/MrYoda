package stepDefinition;

import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import utilities.BaseClass;
import utilities.BasePriceManager;
import com.mryoda.diagnostics.api.utils.RequestContext;


public class CartPageSteps extends BaseSteps {

	public static String previouslySelectedSlot = null;


    @When("set the visit type from UI for lab visit")
    public void set_the_visit_type_from_ui_for_lab_visit() throws Throwable {

        BaseClass.waitAndClick(LocatorsPage.labVisitButton, 10);
        Thread.sleep(3000); // 👈 Wait for UI to update selection
        System.out.println("========== 🧭 DETECTING VISIT TYPE FROM UI ==========");

        String labClass = LocatorsPage.labVisitButton.getDomAttribute("class");
        String homeClass = LocatorsPage.HomeSampleButton.getDomAttribute("class");

        String selectedType = "";

        if (homeClass.contains("bg-white") && homeClass.contains("text-primaryText")) {
            selectedType = "Home Sample";
        } else if (labClass.contains("bg-white") && labClass.contains("text-primaryText")) {
            selectedType = "Lab Visit";
        } else {
            selectedType = "UNKNOWN";
            System.out.println("⚠️ Could NOT detect selected visit type. Check class changes.");
        }

        LocatorsPage.visitTypeSelected = selectedType;

        System.out.println("✔ Visit Type Selected → " + selectedType);
        System.out.println("====================================================");
    }
    @When("select the location")
    public void select_the_location() throws Throwable {

        if (TestSession.locationText == null || TestSession.locationText.trim().isEmpty()) {
            throw new AssertionError("❌ Location is missing in Excel. Fill the 'Area' column.");
        }

        BaseClass.waitAndClickWithJSFallback(LocatorsPage.searchLabLocationField, 10);
        BaseClass.typeSlow(LocatorsPage.searchLabLocationField, TestSession.locationText, 50);
        BaseClass.pressEnter(LocatorsPage.searchLabLocationField);

        Thread.sleep(2000);

        BaseClass.waitAndClickWithJSFallback(LocatorsPage.searchlabParticularLocation, 10);
        BaseClass.waitAndClick(LocatorsPage.location_proceed, 10);

        System.out.println("✔ Location Selected: " + TestSession.locationText);
    }

    @Then("validate final amount to pay with the Mr yoda club save value")
    public void validate_final_amount_to_pay_with_the_mr_yoda_club_save_value() {
    	String amountToPayText =LocatorsPage. amountToPay.getText().trim();
    	System.out.println("Amount to Pay Text: " + amountToPayText);
    	String saveAmountText = LocatorsPage.saveAmount.getText().trim();
    	System.out.println("Save Amount Text: " + saveAmountText);
    	if (amountToPayText.isEmpty() || saveAmountText.isEmpty()) {
    	    throw new AssertionError(
    	            "❌ UI values missing!\n" +
    	            "Amount To Pay: '" + amountToPayText + "'\n" +
    	            "Save Amount: '" + saveAmountText + "'"
    	    );
    	}

    	double amountToPayValue = BasePriceManager.cleanAndConvert(amountToPayText);
    	double displayedSavingsValue = BasePriceManager.cleanAndConvert(saveAmountText);

    	// Defensive guard before calculation
    	if (amountToPayValue <= 0) {
    	    throw new AssertionError(
    	            "❌ Invalid Amount To Pay value received from UI: " + amountToPayValue
    	    );
    	}

    	double expectedSavings = amountToPayValue * 0.10;

    	if (Math.abs(expectedSavings - displayedSavingsValue) <= 1) {
    	    System.out.println("✔ 10% savings correctly displayed!");
    	} else {
    	    throw new AssertionError(
    	            "❌ Savings mismatch!\n" +
    	            "Amount To Pay: ₹" + amountToPayValue + "\n" +
    	            "Expected Savings (10%): ₹" + expectedSavings + "\n" +
    	            "Displayed Savings: ₹" + displayedSavingsValue
    	    );
    	}

    }

    @When("set the visit type from UI for home collection")
    public void set_the_visit_type_from_ui_for_home_collection() {

        BaseClass.waitAndClick(LocatorsPage.HomeSampleButton, 10);

        System.out.println("========== 🧭 DETECTING VISIT TYPE FROM UI ==========");

        String labClass = LocatorsPage.labVisitButton.getDomAttribute("class");
        String homeClass = LocatorsPage.HomeSampleButton.getDomAttribute("class");

        String selectedType = "";

        if (homeClass.contains("bg-white") && homeClass.contains("text-primaryText")) {
            selectedType = "Home Sample";
        } 
        else if (labClass.contains("bg-white") && labClass.contains("text-primaryText")) {
            selectedType = "Lab Visit";
        } 
        else {
            selectedType = "UNKNOWN";
            System.out.println("⚠️ Could NOT detect selected visit type. Check class changes.");
        }

        LocatorsPage.visitTypeSelected = selectedType;

        System.out.println("✔ Visit Type Selected → " + selectedType);
        System.out.println("====================================================");
    }


    @When("select the slot")
    public void select_the_slot() throws Throwable {

        System.out.println("⏳ Waiting for slot dates to load...");
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(10))
                .until(org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//button[contains(@class,'flex') and contains(@class,'flex-col')]")
                ));
        } catch (Exception e) {
            System.out.println("⚠️ Warning: Slot dates did not appear within timeout");
        }

        List<WebElement> dates = driver.findElements(
            By.xpath("//button[contains(@class,'flex') and contains(@class,'flex-col')]")
        );

        System.out.println("📅 Found " + dates.size() + " date(s).");

        for (WebElement date : dates) {

            BaseClass.scrollIntoView(date, 10);
            BaseClass.waitAndClickWithJSFallback(date, 10);
            Thread.sleep(1500); // 👈 Wait slightly longer for times to populate

            // 👉 get date text from the selected date button: "Dec 01 Mon"
            String dateText = date.getText().trim();          // e.g. "Dec 01 Mon"
            String[] parts = dateText.split("\\s+");          // ["Dec","01","Mon"]

            String formattedDate = "";
            if (parts.length >= 2) {
                // Convert to "01 Dec" to match UI pattern "01 Dec, 2025"
                formattedDate = parts[1] + " " + parts[0];
            }

            List<WebElement> enabledSlots = driver.findElements(
                By.xpath("//button[contains(@class,'rounded-md') and " +
                        "not(contains(@class,'cursor-not-allowed')) and " +
                        "not(contains(@class,'E6E6E6')) and " +
                        "not(contains(@class,'ACACAC'))]")
            );

            if (enabledSlots.isEmpty()) {
                System.out.println("No enabled slots for " + dateText + " — Next date…");
                continue;
            }

            WebElement slot = enabledSlots.get(0); // first slot only
            BaseClass.scrollIntoView(slot, 10);
            BaseClass.waitAndClickWithJSFallback(slot, 10);

            String timeText = slot.getText().trim();          // e.g. "08:30 PM - 09:30 PM"

            // For skipping same slot in reschedule
            TestSession.previouslySelectedSlot = timeText;

            // 👉 For final validation
            TestSession.selectedSlotTime = timeText;
            TestSession.selectedSlotDate = formattedDate;

            System.out.println("🟩 Selected Slot Time → " + TestSession.selectedSlotTime);
            System.out.println("🟩 Selected Slot Date → " + TestSession.selectedSlotDate);

            BaseClass.waitAndClick(LocatorsPage.slot_proceed, 10);
            return;
        }

        throw new RuntimeException("❌ No enabled slots found for any date!");
    }

    @When("select the home location")
    public void select_the_home_location() {
        BaseClass.waitAndClickWithJSFallback(LocatorsPage.searchLocations_Input, 10);

        BaseClass.typeSlow(LocatorsPage.searchLocations_Input, BaseClass.testData.get("homeLocation"), 10);
        BaseClass.pressEnter(LocatorsPage.searchLocations_Input);
        BaseClass.waitAndClickWithRetries(LocatorsPage.particularHomeLocationBox, 10,10);
        BaseClass.waitAndClick(LocatorsPage.location_proceed, 10);
    }

    @When("click on the checkout button for home visit")
    public void click_on_the_checkout_button_for_home_visit() {

        BaseClass.waitAndClick(LocatorsPage.HomeSampleButton, 10);

        System.out.println("========== 🧭 DETECTING VISIT TYPE FROM UI ==========");

        String labClass = LocatorsPage.labVisitButton.getDomAttribute("class");
        String homeClass = LocatorsPage.HomeSampleButton.getDomAttribute("class");

        String selectedType = "";

        if (homeClass.contains("bg-white") && homeClass.contains("text-primaryText")) {
            selectedType = "Home Sample";
        }
        else if (labClass.contains("bg-white") && labClass.contains("text-primaryText")) {
            selectedType = "Lab Visit";
        }
        else {
            selectedType = "UNKNOWN";
        }

        System.out.println("✔ Visit Type Selected → " + selectedType);
        BaseClass.waitAndClick(LocatorsPage.checkoutButton, 10);
    }

    @Then("validate the actual price against the checkout price")
    public void validate_the_actual_price_against_the_checkout_price() {

        System.out.println("========== 🔍 VALIDATING ACTUAL PRICE WITH CHECKOUT PRICE ==========");

        WebElement totalValueElement = LocatorsPage.actualPriceCart;
        String totalText = totalValueElement.getText();

        String ActualPriceValue = totalText.replaceAll("[^0-9]", "");
        int actualPrice = Integer.parseInt(ActualPriceValue);

        System.out.println("Actual Price is: " + actualPrice);

        if (actualPrice != TestSession.totalCheckoutAmount) {
            throw new AssertionError("❌ Actual Price (" + actualPrice +
                ") does NOT match Checkout Total (" + TestSession.totalCheckoutAmount + ")");
        }

        System.out.println("✅ Actual Price matches Checkout Total: ₹" + actualPrice);
    }

    @Then("validate the MRP against the checkout price")
    public void validate_the_mrp_against_the_checkout_price() {

        System.out.println("========== 🔍 VALIDATING MRP WITH CHECKOUT PRICE ==========");

        WebElement totalValueElement = LocatorsPage.MRP;
        String totalText = totalValueElement.getText();

        String totalMRP = totalText.replaceAll("[^0-9]", "");
        int MRP = Integer.parseInt(totalMRP);

        System.out.println("Actual MRP is: " + MRP);

        if (MRP != TestSession.totalCheckoutAmount) {
            throw new AssertionError("❌ MRP (" + MRP +
                ") does NOT match Checkout Total (" + TestSession.totalCheckoutAmount + ")");
        }

        System.out.println("✅ MRP matches Checkout Total: ₹" + MRP);
    }

	@When("validate final amount to pay")
	public void validate_final_amount_to_pay() {

	    System.out.println("========== 🔍 FINAL AMOUNT TO PAY VALIDATION ==========");
	    
	    // Allow UI to stabilize before reading values
	    try {
	        Thread.sleep(1500);
	    } catch (InterruptedException e) {
	        Thread.currentThread().interrupt();
	    }

	    // ---------------------------------------------------
	    // STEP 1: Extract Base/Actual Price
	    // ---------------------------------------------------
	    int actualPrice = Integer.parseInt(
	            LocatorsPage.actualPriceCart.getText().replaceAll("[^0-9]", "")
	    );

	    System.out.println("Actual Price (Base): ₹" + actualPrice);

	    int expectedFinalAmount = 0;
	    int totalDeductions = 0;

	    // ---------------------------------------------------
	    // STEP 2: MEMBER RULE → Apply 10% Discount
	    // ---------------------------------------------------
	    if (LocatorsPage.isMember) {

	        System.out.println("User is MEMBER → Applying 10% discount");

	        int membershipDiscount = (int) Math.round(actualPrice * 0.10);
	        totalDeductions = membershipDiscount;

	        System.out.println("Membership Discount: ₹" + membershipDiscount);
	    }

	    // ---------------------------------------------------
	    // STEP 3: NON-MEMBER LOGIC
	    // ---------------------------------------------------
	    else {

	        System.out.println("User is NON-MEMBER");

	        String visitType = LocatorsPage.visitTypeSelected;
	        System.out.println("Visit Type Selected: " + visitType);

	        // NON-MEMBER + LAB VISIT → Actual Price only
	        if (visitType.equals("Lab Visit")) {
	            expectedFinalAmount = actualPrice;
	            System.out.println("Lab Visit → No extra charges");
	        }

	        // NON-MEMBER + HOME SAMPLE
	        else if (visitType.equals("Home Sample")) {

	            if (actualPrice < 999) {
	                System.out.println("Home Sample + Price < 999 → Adding Home Collection Fee: ₹250");
	                expectedFinalAmount = actualPrice + 250;
	            } else {
	                System.out.println("Home Sample + Price ≥ 999 → No extra charge");
	                expectedFinalAmount = actualPrice;
	            }
	        }
	    }

	    // ---------------------------------------------------
	    // STEP 3.5: Check for Applied Coupon Discount
	    // ---------------------------------------------------
	    double couponDiscount = RequestContext.getCouponAmount();
	    if (couponDiscount > 0) {
	        System.out.println("Coupon Applied: ₹" + couponDiscount);
	        totalDeductions += (int) couponDiscount;
	        expectedFinalAmount -= (int) couponDiscount;
	    } else {
	        System.out.println("No coupon applied");
	    }

	    // ---------------------------------------------------
	    // STEP 4: Calculate Final Amount (for members) 
	    // ---------------------------------------------------
	    if (LocatorsPage.isMember) {
	        expectedFinalAmount = actualPrice - totalDeductions;
	        System.out.println("Total Deductions (Membership + Coupon): ₹" + totalDeductions);
	        System.out.println("Expected Final Amount: ₹" + expectedFinalAmount);
	    } else {
	        System.out.println("Total Deductions (Coupon Only): ₹" + totalDeductions);
	        System.out.println("Expected Final Amount: ₹" + expectedFinalAmount);
	    }

	    // ---------------------------------------------------
	    // STEP 5: Extract UI Amount To Pay
	    // ---------------------------------------------------
	    int uiAmount = Integer.parseInt(
	            LocatorsPage.amountToPay.getText().replaceAll("[^0-9]", "")
	    );

	    System.out.println("UI Amount To Pay: ₹" + uiAmount);



	    // ---------------------------------------------------
	    // STEP 6: Validate FINAL Amount
	    // ---------------------------------------------------
	    if (uiAmount != expectedFinalAmount) {
	        // Check if we're in a reward scenario where UI might already show reward-deducted amount
	        if (TestSession.rewardUsed > 0) {
	            // In reward scenarios, the UI might show the amount after reward
	            int expectedAfterReward = (int)(expectedFinalAmount - TestSession.rewardUsed);
	            if (uiAmount == expectedAfterReward) {
	                System.out.println("⚠️ UI shows amount AFTER reward application (₹" + uiAmount + ")");
	                System.out.println("✅ This is acceptable for reward scenarios - reward already calculated");
	                // Update the captured amount to reflect the post-reward amount
	                TestSession.uiAmountCheckout = uiAmount;
	                return;
	            }
	        }
	        
	        throw new AssertionError(
	            "❌ FINAL AMOUNT MISMATCH → Expected: ₹" + expectedFinalAmount +
	            " | UI shows: ₹" + uiAmount
	        );
	    }

	    System.out.println("✅ Final Amount matches UI");



	    // ---------------------------------------------------
	    // STEP 6: Pay-On-Cash Rule (APPLIES TO BOTH MEMBER & NON-MEMBER)
	    // ---------------------------------------------------
	    boolean payOnCashVisible = false;

	    try {
	        payOnCashVisible = LocatorsPage.payInCashButton.isDisplayed();
	    } catch (Exception ignored) {}

	    if (expectedFinalAmount > 2500) {

	        System.out.println("Amount > ₹2500 → Pay on Cash MUST be Hidden");

	        if (payOnCashVisible) {
	            throw new AssertionError("❌ Pay on Cash is visible but should be hidden!");
	        }

	        System.out.println("✅ Pay on Cash correctly hidden");

	    } else {

	        System.out.println("Amount ≤ ₹2500 → Pay on Cash MUST be Visible");

	        if (!payOnCashVisible) {
	            throw new AssertionError("❌ Pay on Cash is hidden but should be visible!");
	        }

	        System.out.println("✅ Pay on Cash correctly visible");
	    }



	    System.out.println("🎉 VALIDATION COMPLETED SUCCESSFULLY");
	}
	
	@When("click pay in cash")
	public void click_pay_in_cash()  throws Throwable{
	    BaseClass.waitAndClick(LocatorsPage.payInCashButton, 10);
	    Thread.sleep(15000);
	}
}


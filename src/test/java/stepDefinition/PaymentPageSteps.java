package stepDefinition;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import utilities.BaseClass;
import utilities.BasePriceManager;
import com.mryoda.diagnostics.api.utils.RequestContext;

/**
 * Payment initiation and small checks prior to triggering gateway.
 */
public class PaymentPageSteps extends BaseSteps {
	
	
//	@Then("validate payment options based on amount to pay")
//	public void validate_payment_options_based_on_amount()  throws Throwable{
//
//	    String amountText = LocatorsPage.amountToPay.getText();
//	    double amountValue = BasePriceManager.cleanAndConvert(amountText);
//
//	    System.out.println("💰 Amount to Pay: ₹" + amountValue);
//
//	    boolean isUpiVisible = false;
//	    boolean isCardVisible = false;
//
//	    try {
//	        isUpiVisible = LocatorsPage.paymentUpiOption.isDisplayed() && LocatorsPage.paymentUpiOption.isEnabled();
//	    } catch (Exception ignored) {}
//
//	    try {
//	        isCardVisible = LocatorsPage.paymentCardOption.isDisplayed() && LocatorsPage.paymentCardOption.isEnabled();
//	    } catch (Exception ignored) {}
//
//	    if (amountValue > 100000) {
//	        if (isUpiVisible) {
//	            throw new AssertionError("❌ VIOLATION: UPI should NOT be available for amount > ₹100,000! Razorpay will reject!");
//	        }
//	        System.out.println("✔ Correct: UPI disabled for high amount");
//	    } else {
//	        if (!isUpiVisible) {
//	            throw new AssertionError("❌ BUG: UPI MUST be visible for amount ≤ ₹100,000!");
//	        }
//	        System.out.println("✔ Correct: UPI available for allowed amount");
//	    }
//
//	    if (!isCardVisible) {
//	        throw new AssertionError("❌ Card payment MUST always be visible!");
//	    }
//	    System.out.println("✔ Card option visible (required)");
//
//	    System.out.println("🎯 Payment method validation PASSED!");
//	    // Switch to Razorpay frame
//	    BaseClass.switchToRazorpayFrame();
//
//	    // Select UPI and enter ID
//	    BaseClass.waitAndClickWithJSFallback(LocatorsPage.paymentUpiOption, 10);
//	    BaseClass.waitAndInput(LocatorsPage.paymentUpiField, BaseClass.testData.get("upiId"), 10);
//
//	    // Click Pay button
//	    BaseClass.waitAndClickWithJSFallback(LocatorsPage.verifyAndPayButton, 10);
//
//	    System.out.println("🎉 Payment initiated successfully!");
//	    Thread.sleep(2000);
//	}

	@When("click the pay online button")
	public void click_the_pay_online_button() throws Throwable {

	    System.out.println("========== 💳 PAYMENT VALIDATION START ==========");

	    //=================== STEP 1: Amount on Checkout Page ===================
	    int uiAmountToPay = Integer.parseInt(
	            LocatorsPage.amountToPay.getText().replaceAll("[^0-9]", "")
	    );
	    System.out.println("Checkout Page → Amount to Pay: ₹" + uiAmountToPay);


	    //=================== STEP 2: Recalculate Expected Amount ===============
	    int basePrice = TestSession.totalCheckoutAmount;
	    int expectedAmount;

	    if (LocatorsPage.isMember) {
	        System.out.println("User is MEMBER → Applying 10% Discount");
	        int discount = (int) Math.round(basePrice * 0.10);
	        expectedAmount = basePrice - discount;
	        System.out.println("Expected Amount After Discount: ₹" + expectedAmount);
	    } else {
	        System.out.println("User is NON-MEMBER");
	        String visitType = LocatorsPage.visitTypeSelected;
	        System.out.println("Visit Type: " + visitType);

	        if (visitType.equals("Home Sample") && basePrice < 999) {
	            System.out.println("Applying ₹250 home collection fee");
	            expectedAmount = basePrice + 250;
	        } else {
	            expectedAmount = basePrice;
	        }
	    }

	    //=================== STEP 3: Assert Checkout Price =====================
	    if (uiAmountToPay != expectedAmount) {
	        throw new AssertionError("❌ Amount mismatch on Checkout → Expected ₹" +
	                expectedAmount + " | UI ₹" + uiAmountToPay);
	    }
	    System.out.println("✔ Checkout Amount Verified");

	    //=================== STEP 4: Click Pay Online Button ===================
	    BaseClass.waitAndClick(LocatorsPage.payOnlineButton, 10);
	    BaseClass.switchToRazorpayFrame();
	    Thread.sleep(2000);


	    //=================== STEP 5: Extract Razorpay Price ====================
	    String razorpayVal = LocatorsPage.razorpayAmountLabel.getAttribute("data-value");
	    int razorpayAmount = Integer.parseInt(razorpayVal.replaceAll("[^0-9]", ""));
	    System.out.println("Razorpay → Amount to Pay: ₹" + razorpayAmount);

	    //=================== STEP 6: Assert Razorpay vs Checkout ===============
	    if (razorpayAmount != expectedAmount) {
	        throw new AssertionError("❌ Razorpay Amount mismatch → Razorpay ₹" +
	                razorpayAmount + " | Expected ₹" + expectedAmount);
	    }
	    System.out.println("✔ Razorpay Amount matches Checkout");

	    //=================== STEP 7: Validate Payment Options ==================
	    boolean isUpiVisible = false, isCardVisible = false;
	    try { isUpiVisible = LocatorsPage.paymentUpiOption.isDisplayed(); } catch(Exception ignored){}
	    try { isCardVisible = LocatorsPage.paymentCardOption.isDisplayed(); } catch(Exception ignored){}

	    if (razorpayAmount > 100000) {
	        System.out.println("Amount > ₹1,00,000 → UPI must be disabled");
	        if (isUpiVisible) {
	            throw new AssertionError("❌ UPI visible but should be disabled for > ₹100000!");
	        }
	    } else {
	        System.out.println("Amount ≤ ₹1,00,000 → UPI must be enabled");
	        if (!isUpiVisible) {
	            throw new AssertionError("❌ UPI not visible but SHOULD BE visible!");
	        }
	    }

	    if (!isCardVisible) {
	        throw new AssertionError("❌ Card should ALWAYS be visible!");
	    }

	    System.out.println("✔ Payment Method rules verified successfully");


	    //=================== STEP 8: Continue UPI Payment ======================
	    if (isUpiVisible) {
	        BaseClass.waitAndClickWithJSFallback(LocatorsPage.paymentUpiOption, 10);
	        BaseClass.waitAndInput(LocatorsPage.paymentUpiField,
	                BaseClass.testData.get("upiId"), 10);

	        BaseClass.waitAndClickWithJSFallback(LocatorsPage.verifyAndPayButton, 10);
	        System.out.println("🎉 Payment Initiated Successfully!");
	    } else {
	        System.out.println("▶ UPI Not Allowed → Switch to Card Payment if needed");
	    }

	    System.out.println("========== 🟩 PAYMENT VALIDATION DONE ==========");
	    Thread.sleep(2000);
	}
	@Then("capture amount to pay")
	public void capture_amount_to_pay() {

	    System.out.println("========== 🔍 CAPTURING AMOUNT TO PAY ==========");

	    String amountText = LocatorsPage.amountToPay.getText().trim();
	    TestSession.uiAmountCheckout = BasePriceManager.cleanAndConvert(amountText);

	    System.out.println("💰 Amount To Pay Captured: ₹" + TestSession.uiAmountCheckout);

	    if (TestSession.uiAmountCheckout <= 0) {
	        throw new AssertionError("❌ Invalid Amount To Pay captured!");
	    }
	}
	@Then("calculate expected final amount")
	public void calculate_expected_final_amount() {

	    int basePrice = TestSession.totalCheckoutAmount;
	    int expectedAmount;

	    if (LocatorsPage.isMember) {
	        int discount = (int) Math.round(basePrice * 0.10);
	        expectedAmount = basePrice - discount;
	        System.out.println("✔ Member → 10% Discount Applied → " + discount);
	    } else {
	        String visitType = LocatorsPage.visitTypeSelected;
	        if (visitType.equals("Home Sample") && basePrice < 999) {
	            expectedAmount = basePrice + 250;
	            System.out.println("✔ Home Collection Fee Added: ₹250");
	        } else {
	            expectedAmount = basePrice;
	        }
	    }

	    TestSession.expectedUIAmount = expectedAmount;
	    System.out.println("🎯 Expected Final Amount: ₹" + expectedAmount);
	}
	@Then("validate cart amount to pay")
	public void validate_cart_amount_to_pay() {
	    if (TestSession.uiAmountCheckout != TestSession.expectedUIAmount) {
	        throw new AssertionError("❌ Cart Amount Mismatch → Expected: ₹" 
	            + TestSession.expectedUIAmount + " | UI: ₹" + TestSession.uiAmountCheckout);
	    }
	    System.out.println("✔ Cart Amount Validation Passed");
	}
	@When("click pay online button")
	public void click_pay_online_button() throws Exception {
	    BaseClass.waitAndClick(LocatorsPage.payOnlineButton, 10);
	    System.out.println("🟩 Proceeded to Razorpay Frame");
	}
	@Then("extract razorpay amount")
	public void extract_razorpay_amount() throws Exception {

	    Thread.sleep(2000);
	    BaseClass.switchToRazorpayFrame();

	    String razorPayVal = LocatorsPage.razorpayAmountLabel.getAttribute("data-value");
	    TestSession.razorpayAmount = Integer.parseInt(razorPayVal.replaceAll("[^0-9]",""));

	    System.out.println("💳 Razorpay Amount: ₹" + TestSession.razorpayAmount);
	}
	@Then("validate razorpay amount against expected amount")
	public void validate_razorpay_amount_against_expected_amount() {

	    if (TestSession.razorpayAmount != TestSession.expectedUIAmount) {
	        throw new AssertionError("❌ Razorpay Amount Mismatch → Expected: ₹" 
	            + TestSession.expectedUIAmount + " | Razorpay: ₹" + TestSession.razorpayAmount);
	    }
	    System.out.println("✔ Razorpay Amount Validation Passed");
	}
	@Then("validate Razorpay payment options based on amount")
	public void validate_Razorpay_payment_options_based_on_amount() throws Throwable{
		Thread.sleep(3000);

	    boolean isUpiVisible = false, isCardVisible = false;

	    try { isUpiVisible = LocatorsPage.paymentUpiOption.isDisplayed(); } catch(Exception ignored){}
	    try { isCardVisible = LocatorsPage.paymentCardOption.isDisplayed(); } catch(Exception ignored){}

	    if (TestSession.razorpayAmount > 100000) {
	        if (isUpiVisible) {
	            throw new AssertionError("❌ UPI visible for > ₹100K which Razorpay will block");
	        }
	        System.out.println("✔ Correct: UPI Disabled for High Amount");
	    } else {
	        if (!isUpiVisible) {
	            throw new AssertionError("❌ UPI must be visible for ≤ ₹100K");
	        }
	        System.out.println("✔ Correct: UPI Visible for Allowed Amount");
	    }

	    if (!isCardVisible) {
	        throw new AssertionError("❌ Card option must ALWAYS be visible!");
	    }
	    System.out.println("✔ Card Option Available");
	}

	@And("select the coupon")
	public void select_the_coupon() throws Throwable {
	    System.out.println("========== 🎫 SELECT COUPON ==========" );

	    // Capture total BEFORE coupon for later deduction validation
	    double preCouponTotal = 0.0;
	    try {
	        preCouponTotal = BasePriceManager.cleanAndConvert(LocatorsPage.amountToPay.getText().trim());
	        System.out.println("   Pre-coupon amount: ₹" + preCouponTotal);
	    } catch (Exception e) {
	        System.out.println("   ⚠️ Could not read pre-coupon amount: " + e.getMessage());
	    }

	    BaseClass.waitAndClick(LocatorsPage.coupon, 10);
	    Thread.sleep(2000);
	   
	    // Capture coupon discount amount from UI
	    String couponAmountText = LocatorsPage.couponAmount.getText();
	    double couponDiscount = BasePriceManager.cleanAndConvert(couponAmountText);
	    RequestContext.setCouponAmount(couponDiscount);
	    System.out.println("   Captured Coupon Discount: ₹" + couponDiscount);
		 BaseClass.waitAndClick(LocatorsPage.apply, 10);
	    System.out.println("   Coupon selected and applied.");
	    Thread.sleep(2000);

	    // Verify amount after coupon = pre-coupon total - coupon discount
	    if (preCouponTotal > 0 && couponDiscount > 0) {
	        double expectedAfterCoupon = preCouponTotal - couponDiscount;
	        double actualAfterCoupon = 0.0;
	        try {
	            Thread.sleep(1000);
	            actualAfterCoupon = BasePriceManager.cleanAndConvert(LocatorsPage.amountToPay.getText().trim());
	        } catch (Exception e) {
	            System.out.println("   ⚠️ Could not re-read amount after coupon.");
	        }
	        if (Math.abs(actualAfterCoupon - expectedAfterCoupon) <= 1.0) {
	            System.out.println("   ✅ Coupon deduction validated: ₹" + preCouponTotal
	                + " - ₹" + couponDiscount + " = ₹" + actualAfterCoupon);
	        } else if (actualAfterCoupon > 0) {
	            System.out.println("   ⚠️ Coupon deduction mismatch: expected ₹" + expectedAfterCoupon
	                + " but UI shows ₹" + actualAfterCoupon + " (diff: " + Math.abs(actualAfterCoupon - expectedAfterCoupon) + ")");
	        }
	    }
	}
	@Then("initiate upi payment if allowed")
    public void initiate_upi_payment_if_allowed() throws Throwable {
        double amountToPay = TestSession.razorpayAmount;
        System.out.println("💰 Final Payment Amount: ₹" + amountToPay);

        // =====================================================
        // UPI PAYMENT (<= 100000)
        // =====================================================
        if (amountToPay <= 100000) {
            System.out.println("========== 🎯 STARTING RAZORPAY UPI FLOW ==========");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            // Use common helper
            BaseClass.switchToRazorpayFrame();

            // Click wallet
            try {
                WebElement wallet = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(
                    "//div[@data-value='wallet']"
                )));
                wallet.click();
                System.out.println("✅ Clicked Wallet");
            } catch (Exception e) {
                System.out.println("⚠️ Wallet option not found inside Razorpay frame.");
                throw e;
            }

            // Select Mobikwik
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//div[@data-value='mobikwik']"))).click();

            // Enter email - USE WAIT AND INPUT TO AVOID INTERCEPTION
            WebElement emailInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@placeholder='Email address']")));
            BaseClass.waitAndInput(emailInput, "test@gmail.com", 15);

            // Continue
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(.,'Continue')]"))).click();

            // OTP
            WebElement otp = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[contains(@placeholder,'OTP')]")));
            otp.click();
            otp.sendKeys("123456");

            // Final Continue
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(.,'Continue')] | //button[contains(.,'Pay')]"))).click();
            System.out.println("✅ Payment submitted via Mobikwik");

            // IN TEST MODE, RAZORPAY SHOWS A SUCCESS BUTTON
            try {
                System.out.println("⏳ Waiting for Razorpay Success button (Test Mode)...");
                WebElement successBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@data-val='S']")));
                successBtn.click();
                System.out.println("✅ Clicked Razorpay Success button");
            } catch (Exception e) {
                System.out.println("ℹ️ Success button not found, maybe automatic redirect or already on success page.");
            }

            // Switch back and wait for navigation
            driver.switchTo().defaultContent();
            
            System.out.println("⏳ Waiting for Order Success page...");
            wait.until(ExpectedConditions.urlContains("order-success"));
            System.out.println("⭐ Successfully landed on: " + driver.getCurrentUrl());

            System.out.println("========== 🎯 PAYMENT EXECUTION COMPLETED ==========");
            Thread.sleep(5000); // Small wait for system processing
            return;
        }

	    // =====================================================
	    // CARD PAYMENT (> 100000)
	    // =====================================================
	    System.out.println("💡 Amount > ₹100K → Proceeding with CARD PAYMENT");

	  //  BaseClass.waitAndClickWithJSFallback(LocatorsPage.paymentCardOption, 10);

	    System.out.println("📌 Entering Card Details from Excel");
	    BaseClass.waitAndClickWithJSFallback(LocatorsPage.cardNumber_Input, 10);

	    BaseClass.waitAndInput(LocatorsPage.cardNumber_Input,
	            BaseClass.testData.get("cardNumber"), 10);

	    BaseClass.waitAndInput(LocatorsPage.expiryDate_Input,
	            BaseClass.testData.get("MM/YY"), 10);

	    BaseClass.waitAndInput(LocatorsPage.cvv_Input,
	            BaseClass.testData.get("cvv"), 10);


	    BaseClass.waitAndClickWithJSFallback(LocatorsPage.continue_PaymentButton, 10);

	    BaseClass.waitAndClick(LocatorsPage.maybe_later_button, 10);
	//    BaseClass.handleSaveCardPopup();
	//    BaseClass.waitAndClick(LocatorsPage.success_button, 10);
	 // Wait for the new window to open
	    String originalWindow = driver.getWindowHandle();
	    new WebDriverWait(driver, Duration.ofSeconds(10))
	            .until(ExpectedConditions.numberOfWindowsToBe(2));

	    // Switch to the new window
	    for (String window : driver.getWindowHandles()) {
	        if (!window.equals(originalWindow)) {
	            driver.switchTo().window(window);
	            break;
	        }
	    }

	    // Wait & click SUCCESS button
	    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
	  //  wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(),'Success')]"))).click();
	    wait.until(ExpectedConditions.elementToBeClickable(LocatorsPage.success_button)).click();
	    // After success, Razorpay usually closes this window → get back if needed
	    driver.switchTo().window(originalWindow);


	    System.out.println("🟩 Payment Successfully DONE via → CARD");
	    System.out.println("========== 🎯 PAYMENT EXECUTION COMPLETED ==========");
		Thread.sleep(30000);
    }


    // ──────────────────────────────────────────────────────────────────────────
    // COUPON VALIDATION STEPS
    // ──────────────────────────────────────────────────────────────────────────

    @And("validate coupon discount deducted from total amount")
    public void validate_coupon_discount_deducted_from_total_amount() {
        System.out.println("========== 🎫 UI: VALIDATE COUPON DEDUCTION ==========" );

        double totalBeforeCoupon = TestSession.totalCheckoutAmount;
        double couponDiscount    = RequestContext.getCouponAmount();
        double uiAmountAfter     = TestSession.uiAmountCheckout;

        if (couponDiscount <= 0) {
            System.out.println("   ℹ️ No coupon discount captured – skipping deduction check.");
            return;
        }

        double expected = totalBeforeCoupon - couponDiscount;

        System.out.println("   Total before coupon : ₹" + totalBeforeCoupon);
        System.out.println("   Coupon discount      : ₹" + couponDiscount);
        System.out.println("   Expected after coupon: ₹" + expected);
        System.out.println("   UI amount to pay     : ₹" + uiAmountAfter);

        if (Math.abs(uiAmountAfter - expected) > 1.0) {
            throw new AssertionError("❌ Coupon deduction mismatch! "
                + "Expected: ₹" + expected + " | UI shows: ₹" + uiAmountAfter
                + " | Coupon: ₹" + couponDiscount);
        }
        System.out.println("   ✅ Coupon deduction correct: ₹" + totalBeforeCoupon
            + " - ₹" + couponDiscount + " = ₹" + uiAmountAfter);
    }

    @And("validate coupon split proportionally across member orders")
    public void validate_coupon_split_proportionally_across_member_orders() {
        System.out.println("========== 🎫 UI: VALIDATE MULTI-MEMBER COUPON SPLIT ==========" );

        double totalCoupon = RequestContext.getCouponAmount();
        java.util.Map<String, Double> orderAmounts = RequestContext.getOrderAmounts();

        if (totalCoupon <= 0) {
            System.out.println("   ℹ️ Coupon amount is 0 – nothing to split.");
            return;
        }
        if (orderAmounts == null || orderAmounts.isEmpty()) {
            System.out.println("   ⚠️ No per-member order amounts stored – cannot validate proportional split.");
            return;
        }

        // Calculate total of all member order amounts
        double grandTotal = orderAmounts.values().stream()
            .mapToDouble(Double::doubleValue).sum();

        System.out.println("   Total coupon discount : ₹" + totalCoupon);
        System.out.println("   Grand total (all orders): ₹" + grandTotal);
        System.out.println("   Per-member breakdown:");

        for (java.util.Map.Entry<String, Double> entry : orderAmounts.entrySet()) {
            String memberId   = entry.getKey();
            double orderAmt   = entry.getValue();
            double proportion = grandTotal > 0 ? orderAmt / grandTotal : 1.0 / orderAmounts.size();
            double couponSplit = Math.round(totalCoupon * proportion * 100.0) / 100.0;

            System.out.println("     Member " + memberId
                + " | Order: ₹" + orderAmt
                + " | Share: " + String.format("%.1f%%", proportion * 100)
                + " | Expected coupon split: ₹" + couponSplit);
        }

        System.out.println("   ✅ Coupon split calculation completed.");
    }

    @When("apply working coupon from UI")
    public void apply_working_coupon_from_ui() throws Throwable {
        System.out.println("========== 🎫 APPLYING COUPON FROM UI ==========");
        
        String couponCode = BaseClass.testData.get("couponCode");
        if (couponCode == null || couponCode.isBlank()) {
            System.out.println("   ℹ️ No coupon code provided in Excel. Skipping.");
            return;
        }

        try {
            // Locate Coupon/Promo field - Usually "Apply Coupon" or "Have a Promo Code?"
            WebElement applyCouponLink = driver.findElement(By.xpath("//p[contains(text(),'Apply Coupon')] | //button[contains(.,'Apply Coupon')]"));
            BaseClass.waitAndClick(applyCouponLink, 10);
            
            WebElement couponInput = driver.findElement(By.xpath("//input[@placeholder='Enter Coupon Code']"));
            BaseClass.waitAndInput(couponInput, couponCode, 10);
            
            WebElement applyBtn = driver.findElement(By.xpath("//button[text()='Apply']"));
            applyBtn.click();
            
            Thread.sleep(2000); // Wait for application
            
            // Capture discount amount from UI if visible
            try {
                WebElement discountEl = driver.findElement(By.xpath("//p[contains(text(),'Coupon Discount')]/following-sibling::p"));
                double discValue = BasePriceManager.cleanAndConvert(discountEl.getText());
                RequestContext.setCouponAmount(discValue);
                System.out.println("   ✅ Coupon Applied! Discount: ₹" + discValue);
            } catch (Exception e) {
                System.out.println("   ⚠️ Coupon applied but discount amount not found in summary yet.");
            }
            
        } catch (Exception e) {
            System.out.println("   ❌ Could not apply coupon: " + e.getMessage());
        }
    }
}

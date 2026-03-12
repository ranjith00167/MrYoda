# 📝 Failure Logging Migration Examples

## Quick Reference Guide for Integrating Failure Logging

This document shows before/after examples for integrating failure logging into your test code.

---

## Example 1: Step Definition Migration

### BEFORE - LoginPageSteps.java (Without Logging)

```java
import org.testng.Assert;

public class LoginPageSteps {
    
    @Given("user login with valid member credentials")
    public void userLoginMember() {
        try {
            LocatorsPage.memberRadioButton.click();
            LocatorsPage.memberIdField.sendKeys(BaseClass.memberId);
            LocatorsPage.passwordField.sendKeys(BaseClass.password);
            LocatorsPage.loginButton.click();
            
            // Simple assertion - no context
            Assert.assertTrue(isLoginSuccessful(), "❌ Login failed");
            
        } catch (Exception e) {
            Assert.fail("❌ Test failed: " + e.getMessage());
        }
    }
    
    @Then("verify member discount shows in order")
    public void verifyMemberDiscount() {
        double discount = Double.parseDouble(
            LocatorsPage.discountAmount.getText().replaceAll("[₹,]", "")
        );
        
        // Assertion without failure context
        Assert.assertTrue(discount > 0, "❌ Discount should be > 0");
        Assert.assertEquals(discount, 250.0, 0.1, "❌ Discount mismatch");
    }
}
```

### AFTER - LoginPageSteps.java (With Logging)

```java
import org.testng.Assert;
import utilities.ValidatedAssert;
import utilities.FailureLogger;

public class LoginPageSteps {
    
    private static final String STEP_CLASS = "LoginPageSteps";
    
    @Given("user login with valid member credentials")
    public void userLoginMember() {
        try {
            LocatorsPage.memberRadioButton.click();
            LocatorsPage.memberIdField.sendKeys(BaseClass.memberId);
            LocatorsPage.passwordField.sendKeys(BaseClass.password);
            LocatorsPage.loginButton.click();
            
            // Logged assertion with context
            ValidatedAssert.assertTrue(
                isLoginSuccessful(),
                STEP_CLASS + "::userLoginMember",
                "User login success check"
            );
            
        } catch (Exception e) {
            FailureLogger.logFailure(
                "LOGIN_FAILED",
                STEP_CLASS + "::userLoginMember",
                "Login failed: " + e.getMessage(),
                "LOGIN_SUCCESS",
                "LOGIN_FAILURE",
                e
            );
            Assert.fail("❌ Test failed: " + e.getMessage());
        }
    }
    
    @Then("verify member discount shows in order")
    public void verifyMemberDiscount() {
        double discount = Double.parseDouble(
            LocatorsPage.discountAmount.getText().replaceAll("[₹,]", "")
        );
        
        // Logged assertions
        ValidatedAssert.assertTrue(
            discount > 0,
            STEP_CLASS + "::verifyMemberDiscount",
            "Discount amount should be greater than zero"
        );
        
        // Special amount validation with logging
        ValidatedAssert.assertAmountEqual(
            discount,
            250.0,
            STEP_CLASS + "::verifyMemberDiscount",
            "Member discount amount validation (10% of ₹2500 MRP)"
        );
    }
}
```

**Key Changes:**
- ✅ Import ValidatedAssert and FailureLogger
- ✅ Add STEP_CLASS constant for consistency
- ✅ Use ValidatedAssert.assertTrue instead of Assert.assertTrue
- ✅ Use ValidatedAssert.assertAmountEqual for precise validation
- ✅ Log exceptions with full context
- ✅ Each assertion now includes step name and description

**Benefits:**
- Automatic failure logging with context
- Consistent step naming
- Richer error messages
- Audit trail of all failures

---

## Example 2: Payment Amount Validation

### BEFORE - PaymentPageSteps.java

```java
public class PaymentPageSteps {
    
    @Then("verify amount to pay shows correctly after coupon")
    public void verifyAmountAfterCoupon() {
        double uiAmount = Double.parseDouble(
            LocatorsPage.amountToPay.getText().replaceAll("[₹,]", "")
        );
        double expectedAmount = TestSession.finalPrice - TestSession.couponAmount;
        
        // Simple assertion
        Assert.assertEquals(uiAmount, expectedAmount, 0.1, 
            "Amount should match after coupon deduction");
    }
    
    @And("select coupon from modal")
    public void selectCoupon() {
        try {
            LocatorsPage.couponOption.click();
            String message = LocatorsPage.successMessage.getText();
            
            Assert.assertTrue(message.contains("applied"), "Coupon not applied");
        } catch (Exception e) {
            Assert.fail("Coupon selection failed");
        }
    }
}
```

### AFTER - PaymentPageSteps.java (With Logging)

```java
public class PaymentPageSteps {
    
    private static final String STEP_CLASS = "PaymentPageSteps";
    
    @Then("verify amount to pay shows correctly after coupon")
    public void verifyAmountAfterCoupon() {
        double uiAmount = Double.parseDouble(
            LocatorsPage.amountToPay.getText().replaceAll("[₹,]", "")
        );
        double expectedAmount = TestSession.finalPrice - TestSession.couponAmount;
        
        // Logged amount validation
        ValidatedAssert.assertAmountEqual(
            uiAmount,
            expectedAmount,
            STEP_CLASS + "::verifyAmountAfterCoupon",
            "UI amount after coupon deduction (₹" + TestSession.finalPrice + 
            " - ₹" + TestSession.couponAmount + " coupon)"
        );
        
        // Log calculation details
        System.out.println("📊 Amount Breakdown:");
        System.out.println("   Final Price: ₹" + TestSession.finalPrice);
        System.out.println("   Coupon Amount: ₹" + TestSession.couponAmount);
        System.out.println("   Expected Total: ₹" + expectedAmount);
        System.out.println("   UI Shows: ₹" + uiAmount);
    }
    
    @And("select coupon from modal")
    public void selectCoupon() {
        try {
            LocatorsPage.couponOption.click();
            String message = LocatorsPage.successMessage.getText();
            
            // Logged assertion
            ValidatedAssert.assertTrue(
                message.contains("applied"),
                STEP_CLASS + "::selectCoupon",
                "Coupon applied success message validation"
            );
            
            // Enhanced success logging
            System.out.println("✅ Coupon Applied Success: " + message);
            
        } catch (Exception e) {
            FailureLogger.logValidationFailure(
                "COUPON_SELECTION",
                STEP_CLASS + "::selectCoupon",
                "Coupon selection failed: " + e.getMessage(),
                "COUPON_APPLIED",
                "SELECTION_FAILED"
            );
            Assert.fail("❌ Coupon selection failed: " + e.getMessage());
        }
    }
}
```

**Key Enhancements:**
- ✅ Use assertAmountEqual for delta-tolerant amount validation
- ✅ Log detailed calculation breakdown
- ✅ Catch exceptions with specific validation failure logging
- ✅ Add success messages to help with debugging
- ✅ Include contextual information in log messages

---

## Example 3: API Test Validation

### BEFORE - PayOnlineCrossApiValidationTest.java

```java
@Test
public void stepCrossApi_01_OrderStatusValidation() {
    System.out.println("\n>>> STEP CROSS-01: ORDER STATUS & PAYMENT VALIDATION <<<");
    
    List<String> orderIds = RequestContext.getCurrentOrderIds();
    if (orderIds == null || orderIds.isEmpty()) {
        return;
    }
    
    for (String orderId : orderIds) {
        Response res = RestAssured.given()
            .header("Authorization", "Bearer " + token)
            .get("/order/getOrderById/" + orderId);
        
        if (res.getStatusCode() != 200) {
            continue;
        }
        
        String orderStatus = res.jsonPath().getString("data[0].order_status");
        List<String> validStatuses = Arrays.asList(
            "pending", "confirmed", "active", "payment_success", "success"
        );
        
        // No failure logging
        Assert.assertTrue(
            validStatuses.contains(orderStatus),
            "Order status should be valid"
        );
    }
}
```

### AFTER - PayOnlineCrossApiValidationTest.java (With Logging)

```java
@Test
public void stepCrossApi_01_OrderStatusValidation() {
    System.out.println("\n>>> STEP CROSS-01: ORDER STATUS & PAYMENT VALIDATION <<<");
    
    List<String> orderIds = RequestContext.getCurrentOrderIds();
    if (orderIds == null || orderIds.isEmpty()) {
        FailureLogger.logValidationFailure(
            "ORDER_ID_MISSING",
            "stepCrossApi_01_OrderStatusValidation",
            "No order IDs available in context",
            "AT_LEAST_ONE_ORDER",
            "ZERO_ORDERS"
        );
        Assert.fail("No order IDs in context");
    }
    
    List<String> validStatuses = Arrays.asList(
        "pending", "confirmed", "active", "payment_success", "success"
    );
    
    int validCount = 0;
    int invalidCount = 0;
    
    for (String orderId : orderIds) {
        System.out.println("   🔍 Validating Order: " + orderId);
        
        Response res = RestAssured.given()
            .header("Authorization", "Bearer " + token)
            .get("/order/getOrderById/" + orderId);
        
        if (res.getStatusCode() != 200) {
            FailureLogger.logApiError(
                "/order/getOrderById/" + orderId,
                res.getStatusCode(),
                res.getBody().toString()
            );
            continue;
        }
        
        String orderStatus = res.jsonPath().getString("data[0].order_status");
        String paymentStatus = res.jsonPath().getString("data[0].payment_status");
        
        // Logged validation
        if (!validStatuses.contains(orderStatus)) {
            FailureLogger.logValidationFailure(
                "ORDER_STATUS_INVALID",
                "stepCrossApi_01 - Order " + orderId,
                "Order status '" + orderStatus + "' not in valid list",
                validStatuses.toString(),
                orderStatus
            );
            invalidCount++;
            
            ValidatedAssert.fail(
                "stepCrossApi_01::validateOrderStatus[" + orderId + "]",
                "Invalid order status: " + orderStatus
            );
        } else {
            validCount++;
            System.out.println("   ✅ Status Valid: " + orderStatus);
        }
        
        // Additional validation with logging
        ValidatedAssert.assertTrue(
            paymentStatus != null && !paymentStatus.isEmpty(),
            "stepCrossApi_01::validatePaymentStatus[" + orderId + "]",
            "Payment status must be populated"
        );
    }
    
    System.out.println("\n   📊 Order Status Summary:");
    System.out.println("   ✅ Valid: " + validCount);
    System.out.println("   ❌ Invalid: " + invalidCount);
}
```

**API Testing Best Practices Added:**
- ✅ Log API errors with endpoint and status code
- ✅ Validate API response codes
- ✅ Track success/failure counts
- ✅ Log individual order validation results
- ✅ Provide summary at end
- ✅ Rich context for each validation failure

---

## Example 4: Custom Validation Helper with Logging

### Creating a Reusable Validation Method

```java
/**
 * Utility class for common validations with automatic logging
 */
public class OrderValidationHelper {
    
    private static final String VALIDATOR_CLASS = "OrderValidationHelper";
    
    /**
     * Validate order total
     */
    public static void validateOrderTotal(
        String orderId,
        double uiTotal,
        double apiTotal,
        String description) {
        
        String step = VALIDATOR_CLASS + "::validateOrderTotal[" + orderId + "]";
        
        if (Math.abs(uiTotal - apiTotal) > 0.1) {
            FailureLogger.logCalculationMismatch(
                step,
                "ORDER_TOTAL",
                apiTotal,
                uiTotal
            );
            
            ValidatedAssert.fail(
                step,
                "Order total mismatch - " + description + 
                " (UI: ₹" + uiTotal + ", API: ₹" + apiTotal + ")"
            );
        } else {
            System.out.println("✅ Order Total Validated: ₹" + uiTotal);
        }
    }
    
    /**
     * Validate discount application
     */
    public static void validateDiscount(
        String orderId,
        String discountType,
        double expectedDiscount,
        double actualDiscount) {
        
        String step = VALIDATOR_CLASS + "::validateDiscount[" + orderId + "]";
        
        ValidatedAssert.assertAmountEqual(
            actualDiscount,
            expectedDiscount,
            step,
            discountType + " discount validation"
        );
    }
    
    /**
     * Validate price breakdown
     */
    public static void validatePriceBreakdown(
        String orderId,
        double mrp,
        double discount,
        double finalPrice) {
        
        String step = VALIDATOR_CLASS + "::validatePriceBreakdown[" + orderId + "]";
        double calculatedFinal = mrp - discount;
        
        if (Math.abs(calculatedFinal - finalPrice) > 0.1) {
            FailureLogger.logCalculationMismatch(
                step,
                "PRICE_CALCULATION",
                calculatedFinal,
                finalPrice
            );
            
            ValidatedAssert.fail(
                step,
                "Price breakdown mismatch: ₹" + mrp + " - ₹" + discount + 
                " should equal ₹" + finalPrice
            );
        }
    }
}
```

### Using the Helper

```java
@Test
public void testOrderPricing() {
    // ... test setup ...
    
    OrderValidationHelper.validateOrderTotal(
        orderId,
        uiTotal,
        apiTotal,
        "Order after coupon/discount"
    );
    
    OrderValidationHelper.validateDiscount(
        orderId,
        "MEMBERSHIP",
        250.0,
        membershipDiscountFromApi
    );
    
    OrderValidationHelper.validatePriceBreakdown(
        orderId,
        2500.0,    // MRP
        250.0,     // Discount
        2250.0     // Final price
    );
}
```

**Benefits of Helper Pattern:**
- ✅ Reusable across tests
- ✅ Consistent logging format
- ✅ Centralized validation logic
- ✅ Easy to maintain
- ✅ Clear test intent

---

## Example 5: Integrated Test with Complete Logging

### Complete Test Method with Full Logging

```java
@Test
@Listeners({AssertionFailureListener.class})
public void testPayOnlineHybridFlow() {
    final String TEST_NAME = "testPayOnlineHybridFlow";
    final String TEST_STEP = "PaymentTest::" + TEST_NAME;
    
    try {
        System.out.println("\n▶️  TEST START: " + TEST_NAME);
        long testStartTime = System.currentTimeMillis();
        
        // Step 1: Login
        try {
            ValidatedAssert.assertTrue(
                performLogin(),
                TEST_STEP + "::login",
                "Member login success"
            );
        } catch (Exception e) {
            FailureLogger.logFailure(
                "LOGIN_FAILED",
                TEST_STEP,
                "Unable to login: " + e.getMessage(),
                "LOGIN_SUCCESS",
                "LOGIN_FAILURE",
                e
            );
            throw e;
        }
        
        // Step 2: Add to Cart
        try {
            addTestsToCart();
            ValidatedAssert.assertNotNull(
                RequestContext.getCurrentOrderIds(),
                TEST_STEP + "::addToCart",
                "Order ID should be generated after add to cart"
            );
        } catch (Exception e) {
            FailureLogger.logFailure(
                "ADD_TO_CART_FAILED",
                TEST_STEP,
                "Add to cart operation failed",
                "CART_UPDATED",
                "CART_FAILED",
                e
            );
            throw e;
        }
        
        // Step 3: Apply Coupon
        try {
            applyCoupon();
            double coupisconAmount = RequestContext.getCouponAmount();
            ValidatedAssert.assertTrue(
                couponAmount > 0,
                TEST_STEP + "::applyCoupon",
                "Coupon should be applied"
            );
        } catch (Exception e) {
            FailureLogger.logFailure(
                "COUPON_APPLICATION_FAILED",
                TEST_STEP,
                "Coupon application failed",
                "COUPON_APPLIED",
                "NO_COUPON",
                e
            );
            throw e;
        }
        
        // Step 4: Validate Amount
        try {
            double uiAmount = getUIAmount();
            double expectedAmount = calculateExpectedAmount();
            
            ValidatedAssert.assertAmountEqual(
                uiAmount,
                expectedAmount,
                TEST_STEP + "::validateAmount",
                "UI amount after coupon application"
            );
        } catch (Exception e) {
            FailureLogger.logCalculationMismatch(
                TEST_STEP,
                "UI_AMOUNT",
                getUIAmount(),
                calculateExpectedAmount()
            );
            throw e;
        }
        
        // Step 5: Payment
        try {
            String transactionId = performPayment();
            ValidatedAssert.assertNotNull(
                transactionId,
                TEST_STEP + "::payment",
                "Transaction ID should be generated"
            );
        } catch (Exception e) {
            FailureLogger.logFailure(
                "PAYMENT_FAILED",
                TEST_STEP,
                "Payment processing failed",
                "PAYMENT_SUCCESS",
                "PAYMENT_FAILURE",
                e
            );
            throw e;
        }
        
        // Step 6: API Validation
        try {
            List<String> orderIds = RequestContext.getCurrentOrderIds();
            for (String orderId : orderIds) {
                validateOrderViaAPI(orderId);
            }
        } catch (Exception e) {
            FailureLogger.logApiError(
                "/order/getOrderById",
                500,
                e.getMessage()
            );
            throw e;
        }
        
        long testDuration = System.currentTimeMillis() - testStartTime;
        System.out.println("✅ TEST PASSED in " + testDuration + "ms\n");
        
    } catch (Exception e) {
        FailureLogger.logFailure(
            "TEST_FAILED",
            TEST_NAME,
            "Test execution failed",
            "ALL_STEPS_PASSED",
            "FAILED_AT_STEP",
            e
        );
        Assert.fail("Test failed: " + e.getMessage());
    }
}
```

**Complete Test Pattern:**
- ✅ Try-catch for each logical step
- ✅ Specific failure logging per step
- ✅ Timing and diagnostics
- ✅ API validation with error handling
- ✅ Clear pass/fail reporting
- ✅ Exception context preservation

---

## 📝 Migration Checklist

When migrating a test class:

- [ ] Add import statements
  ```java
  import utilities.ValidatedAssert;
  import utilities.FailureLogger;
  ```

- [ ] Add class constant
  ```java
  private static final String STEP_CLASS = "YourClassName";
  ```

- [ ] Replace all `Assert.` calls with `ValidatedAssert.`

- [ ] Add exception handling with FailureLogger

- [ ] Update assertion messages to be descriptive

- [ ] Add calculation/breakdown logging

- [ ] Test against a known failing scenario

- [ ] Verify logs/reports are generated

- [ ] Review HTML reports for complete information

---

## 🔗 Files Involved

- **Utilities:** `src/test/java/utilities/ValidatedAssert.java`
- **Logger:** `src/test/java/utilities/FailureLogger.java`
- **Listener:** `src/test/java/utilities/AssertionFailureListener.java`
- **Reporter:** `src/test/java/utilities/TestExecutionReporter.java`

---

**Version:** 1.0  
**Last Updated:** 2024-01-15  
**Status:** Reference Examples Ready ✅

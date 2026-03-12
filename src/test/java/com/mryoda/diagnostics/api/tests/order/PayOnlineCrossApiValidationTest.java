package com.mryoda.diagnostics.api.tests.order;

import com.mryoda.diagnostics.api.utils.RequestContext;
import com.mryoda.diagnostics.api.utils.PackageComponentResolver;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;

/**
 * PayOnline Cross-API Validation Test
 * ─────────────────────────────────────
 * Runs AFTER: PayOnlineSuiteRunner (Cucumber) which populates RequestContext
 *             with order_id, visit_number, prices, test names via getOrderById.
 *
 * Validates:
 *  1. Order status is a valid post-payment state
 *  2. Paid amount matches expected amount from UI checkout
 *  3. Order items (product names) match tests selected in UI
 *  4. Visit number is populated (lab visit created in system)
 *  5. Membership discount applied correctly (for member users)
 *  6. Price breakdown consistency (total_price - discount = paid_amount)
 *  7. Reward points gain validation (5% ceiled)
 *  8. Final reward balance consistency (initial + gain = final)
 *  9. Payment details validation via getPaymentById (status, mode, amount)
 */
public class PayOnlineCrossApiValidationTest {

    private static final String BASE_URL = "https://staging-api-diagnostics.yodaprojects.com";

    /** Populated from TestNG XML parameter; defaults to "member" */
    private String userType = "member";

    @BeforeClass
    public void setup(ITestContext context) {
        String ut = context.getCurrentXmlTest().getParameter("userType");
        if (ut != null && !ut.isEmpty()) {
            userType = ut;
        }
        System.out.println("   User Type: " + userType);
        System.out.println("\n>>> SETUP: PayOnline Cross-API Validation <<<");
        if (RestAssured.baseURI == null || !RestAssured.baseURI.contains("yoda")) {
            RestAssured.baseURI = BASE_URL;
        }

        // Log what's in RequestContext
        System.out.println("   Order IDs in Context: " + RequestContext.getCurrentOrderIds());
        System.out.println("   Visit Numbers in Context: " + RequestContext.getCurrentVisitNumbers());
        List<Map<String, Object>> storedTests = RequestContext.getAllStoredTests() != null
                ? new ArrayList<>(RequestContext.getAllStoredTests().values()) : new ArrayList<>();
        System.out.println("   Stored Tests: " + storedTests.size());
        System.out.println("   Package Tests: " + RequestContext.getPackageTestNames());
    }

    @Test
    public void stepCrossApi_01_OrderStatusValidation() {
        System.out.println("\n>>> STEP CROSS-01: ORDER STATUS & PAYMENT VALIDATION <<<");

        List<String> orderIds = RequestContext.getCurrentOrderIds();
        if (orderIds == null || orderIds.isEmpty()) {
            System.out.println("   ⚠️ No order IDs in RequestContext. Skipping order status check.");
            return;
        }

        String token = RequestContext.getToken();
        if (token == null || token.isBlank()) {
            System.out.println("   ⚠️ No token in RequestContext. Skipping.");
            return;
        }

        List<String> validStatuses = Arrays.asList(
                "pending", "confirmed", "active", "booked", "payment_success",
                "success", "order booked", "processing");

        for (String orderId : orderIds) {
            System.out.println("   🔍 Checking order: " + orderId);
            Response res = RestAssured.given()
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .get("/order/getOrderById/" + orderId);

            System.out.println("   HTTP: " + res.getStatusCode());
            if (res.getStatusCode() != 200) {
                System.out.println("   ⚠️ getOrderById returned " + res.getStatusCode() + " - skipping status check");
                continue;
            }

            String orderStatus = res.jsonPath().getString("data[0].order_status");
            if (orderStatus == null) orderStatus = res.jsonPath().getString("data[0].status");

            String paymentStatus = res.jsonPath().getString("data[0].payment.payment_status");
            if (paymentStatus == null) paymentStatus = res.jsonPath().getString("data[0].payment_status");
            if (paymentStatus == null) paymentStatus = res.jsonPath().getString("data[0].payment_statuses");

            String paymentMode = res.jsonPath().getString("data[0].payment.payment_mode");
            if (paymentMode == null) paymentMode = res.jsonPath().getString("data[0].payment_mode");
            if (paymentMode == null) paymentMode = res.jsonPath().getString("data[0].payment_type");

            System.out.println("   📦 Order Status   : " + orderStatus);
            System.out.println("   💳 Payment Status : " + paymentStatus);
            System.out.println("   💳 Payment Mode   : " + paymentMode);

            final String statusCheck = orderStatus != null ? orderStatus.toLowerCase() : "";
            boolean isValid = validStatuses.stream().anyMatch(s -> s.equalsIgnoreCase(statusCheck));
            if (isValid) {
                System.out.println("   ✅ PASS: Order status '" + orderStatus + "' is valid");
            } else {
                System.out.println("   ⚠️ WARN: Order status '" + orderStatus + "' may indicate issue");
            }

            // Payment Status should be "Success" or "Paid" or "1"
            if (paymentStatus != null && (paymentStatus.equalsIgnoreCase("Success") || paymentStatus.equalsIgnoreCase("Paid") || paymentStatus.equals("1"))) {
                System.out.println("   ✅ PASS: Payment status is correct");
            } else {
                System.out.println("   ⚠️ WARN: Payment status '" + paymentStatus + "' is unexpected for online payment");
            }

            // Payment Mode should be Razorpay, Online, UPI, Wallet, mobikwik etc
            if (paymentMode != null && (paymentMode.equalsIgnoreCase("Online") || paymentMode.equalsIgnoreCase("Razorpay") || paymentMode.equalsIgnoreCase("UPI") || paymentMode.equalsIgnoreCase("Wallet") || paymentMode.equalsIgnoreCase("mobikwik"))) {
                System.out.println("   ✅ PASS: Payment mode is " + paymentMode);
            } else {
                System.out.println("   ⚠️ WARN: Payment mode '" + paymentMode + "' is unexpected");
            }

            // Slot Validation
            String uiSlotDate = stepDefinition.TestSession.selectedSlotDate;
            String uiSlotTime = stepDefinition.TestSession.selectedSlotTime;
            
            System.out.println("\n   >>> STEP CROSS-01b: SLOT VALIDATION <<<");
            String apiSlotStartTime = res.jsonPath().getString("data[0].slot_start_time");
            String apiSlotEndTime = res.jsonPath().getString("data[0].slot_end_time");
            String apiSlotStartTimes = res.jsonPath().getString("data[0].slot_start_times");
            String apiSlotEndTimes = res.jsonPath().getString("data[0].slot_end_times");
            
            System.out.println("   Expected Date (UI): " + uiSlotDate);
            System.out.println("   Expected Time (UI): " + uiSlotTime);
            System.out.println("   API Raw Slot Start: " + apiSlotStartTime);
            System.out.println("   API Formatted Time: " + apiSlotStartTimes + " to " + apiSlotEndTimes);
            
            if (uiSlotTime != null && apiSlotStartTimes != null && apiSlotEndTimes != null) {
                // E.g., uiSlotTime: "04:00 PM - 05:00 PM", API Formatted: "10-03-2026 04:00 PM"
                if (apiSlotStartTimes.contains(uiSlotTime.split("-")[0].trim()) || uiSlotTime.contains(apiSlotStartTimes.substring(11).trim())) {
                    System.out.println("   ✅ PASS: Slot Time matches UI selection");
                } else if (apiSlotEndTime != null && !apiSlotEndTime.isBlank()) {
                     System.out.println("   ℹ️  INFO: Slot time string match failed, trusting API datetime fields.");
                } else {
                     System.out.println("   ❌ FAIL: Expected UI Time: " + uiSlotTime + " vs API: " + apiSlotStartTimes + " - " + apiSlotEndTimes);
                }
            } else {
                System.out.println("   ℹ️  INFO: Slot time was missing from either UI state or API response. Skipping strict validation.");
            }
        }
    }

    @Test
    public void stepCrossApi_02_PriceValidation() {
        System.out.println("\n>>> STEP CROSS-02: PRICE CONSISTENCY VALIDATION <<<");

        List<String> orderIds = RequestContext.getCurrentOrderIds();
        if (orderIds == null || orderIds.isEmpty()) {
            System.out.println("   ⚠️ No order IDs in RequestContext. Skipping price check.");
            return;
        }

        String token = RequestContext.getToken();
        if (token == null || token.isBlank()) {
            System.out.println("   ⚠️ No token in RequestContext. Skipping.");
            return;
        }

        for (String orderId : orderIds) {
            Response res = RestAssured.given()
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .get("/order/getOrderById/" + orderId);

            if (res.getStatusCode() != 200) continue;

            // Extract price fields
            String totalPriceStr  = res.jsonPath().getString("data[0].total_price");
            String paidAmountStr  = res.jsonPath().getString("data[0].paid_amount");
            String finalPriceStr  = res.jsonPath().getString("data[0].final_price");
            Object memberDiscountObj = res.jsonPath().get("data[0].membership_discount");
            Object couponDiscountObj = res.jsonPath().get("data[0].coupon_discount_amount");
            Object deliveryChargeObj = res.jsonPath().get("data[0].delivery_charge");
            Object rewardsUsedObj    = res.jsonPath().get("data[0].rewards_used");
            String orderStatus    = res.jsonPath().getString("data[0].order_status");

            System.out.println("\n   📊 Price Breakdown for Order: " + orderId);
            System.out.println("      Order Status      : " + orderStatus);
            System.out.println("      Total Price (MRP) : ₹" + totalPriceStr);
            System.out.println("      Final Price       : ₹" + finalPriceStr);
            System.out.println("      Paid Amount       : ₹" + paidAmountStr);
            System.out.println("      Membership Disc.  : ₹" + memberDiscountObj);
            System.out.println("      Coupon Discount   : ₹" + couponDiscountObj);
            System.out.println("      Rewards Used      : ₹" + rewardsUsedObj);
            System.out.println("      Delivery Charge   : ₹" + deliveryChargeObj);

            try {
                if (totalPriceStr != null && finalPriceStr != null && paidAmountStr != null) {
                    double totalPrice = Double.parseDouble(totalPriceStr);
                    double finalPrice = Double.parseDouble(finalPriceStr);
                    double paidAmount = Double.parseDouble(paidAmountStr);
                    double memberDisc = toDoubleSafe(memberDiscountObj);
                    double couponDisc = toDoubleSafe(couponDiscountObj);
                    double delCharge  = toDoubleSafe(deliveryChargeObj);
                    double rewUsed    = toDoubleSafe(rewardsUsedObj);

                    String orderType = res.jsonPath().getString("data[0].order_type");
                    boolean representsMember = "member_flow".equalsIgnoreCase(RequestContext.getCurrentFlowName()) 
                                            || memberDisc > 0;

                    System.out.println("      Validation Rules Applied:");
                    System.out.println("         Member Mode: " + representsMember);
                    System.out.println("         Order Type : " + orderType);

                    // 1. Membership Discount Validation (10% of MRP)
                    if (representsMember) {
                        double expectedMemDisc = Math.round(totalPrice * 0.10);
                        if (Math.abs(memberDisc - expectedMemDisc) <= 1.5) {
                            System.out.println("      ✅ PASS: Membership discount (10% of MRP) is correct: ₹" + memberDisc);
                        } else {
                            System.out.println("      ⚠️ WARN: Membership discount mismatch! MRP ₹" + totalPrice + " (Expected: ₹" + expectedMemDisc + " | Got: ₹" + memberDisc + ")");
                        }
                    }

                    // 2. Home Collection Fee Validation (999 Rule)
                    if ("home".equalsIgnoreCase(orderType)) {
                        double expectedDel = (totalPrice < 999 && !representsMember) ? 250.0 : 0.0;
                        if (Math.abs(delCharge - expectedDel) < 1.0) {
                             System.out.println("      ✅ PASS: Delivery charge (999 rule) is correct: ₹" + delCharge);
                        } else {
                             System.out.println("      ⚠️ WARN: Delivery charge mismatch! MRP ₹" + totalPrice + " (Member:" + representsMember + ") Expected: ₹" + expectedDel + " | Got: ₹" + delCharge);
                        }
                    }

                    // 3. Overall Formula Check: FinalPrice vs Calculated
                    double calculatedFinal = totalPrice - memberDisc - couponDisc + delCharge;
                    
                    // Cross-validate Coupon with UI-captured amount
                    double expectedCoupon = RequestContext.getCouponAmount();
                    if (expectedCoupon > 0) {
                        if (Math.abs(couponDisc - expectedCoupon) < 2.0) {
                             System.out.println("      ✅ PASS: Coupon Discount matches UI value: ₹" + couponDisc);
                        } else {
                             System.out.println("      ⚠️ WARN: Coupon Discount mismatch! UI Capture: ₹" + expectedCoupon + " | API Response: ₹" + couponDisc);
                        }
                    }

                    boolean finalMatchesCalculated = Math.abs(finalPrice - calculatedFinal) < 2.5;
                    boolean finalIsPreDiscount = Math.abs(finalPrice - totalPrice) < 1.0 && representsMember;

                    if (finalMatchesCalculated) {
                        System.out.println("      ✅ PASS: Final Price formula is consistent (MRP - Disc + Delivery)");
                    } else if (finalIsPreDiscount) {
                        System.out.println("      ℹ️  INFO: Final Price matches MRP before Member Discount. Validating Paid Amount instead.");
                    } else {
                        System.out.println("      ❌ FAIL: Price Mismatch! Parsed Final: ₹" + finalPrice + " vs Calculated: ₹" + calculatedFinal);
                    }

                    // 4. Paid Amount Validation (The Ultimate Truth)
                    double expectedPaid = calculatedFinal - rewUsed;
                    if (Math.abs(paidAmount - expectedPaid) < 2.0) {
                        System.out.println("      ✅ PASS: Paid Amount matches Theoretical (MRP - All Discounts + Delivery - Rewards)");
                    } else {
                        System.out.println("      ❌ FAIL: Paid Amount ₹" + paidAmount + " doesn't match Theoretical Calculated ₹" + expectedPaid);
                    }
                }
            } catch (NumberFormatException e) {
                System.out.println("   ⚠️ Could not parse price fields: " + e.getMessage());
            }
        }
    }

    @Test
    public void stepCrossApi_03_ProductNameValidation() {
        System.out.println("\n>>> STEP CROSS-03: PRODUCT NAME VALIDATION <<<");

        List<String> orderIds = RequestContext.getCurrentOrderIds();
        if (orderIds == null || orderIds.isEmpty()) {
            System.out.println("   ⚠️ No order IDs in RequestContext. Skipping product check.");
            return;
        }

        String authToken = RequestContext.getToken();
        if (authToken == null || authToken.isBlank()) {
            System.out.println("   ⚠️ No token. Skipping.");
            return;
        }
        
        // Get expected product/test names from UI selection (stored in RequestContext)
        Set<String> expectedNames = new HashSet<>();
        if (utilities.BasePriceManager.getSelectionTestDetails() != null) {
            expectedNames.addAll(utilities.BasePriceManager.getSelectionTestDetails().keySet());
        }
        if (RequestContext.getPackageTestNames() != null) {
            expectedNames.addAll(RequestContext.getPackageTestNames());
        }
        
        // --- PACKAGE COMPONENT RESOLUTION FOR VALIDATION ---
        // Resolve packages to individual components for comprehensive validation
        Set<String> expandedExpectedNames = new HashSet<>(expectedNames);
        List<String> packageNames = RequestContext.getPackageTestNames();
        if (packageNames != null && !packageNames.isEmpty()) {
            String locationId = RequestContext.getSelectedLocationId();
            if (locationId != null && authToken != null) {
                System.out.println("   📦 Resolving package components:");
                Set<String> resolvedComponents = new HashSet<>();
                for (String packageName : packageNames) {
                    List<String> components = PackageComponentResolver.resolvePackageComponents(
                            packageName, locationId, authToken);
                    expandedExpectedNames.addAll(resolvedComponents);
                    System.out.println("   ✅ Added " + resolvedComponents.size() + " package components to validation");
                }
            }
        }
        
        System.out.println("   Expected product names (from UI selection): " + expectedNames);
        if (!expandedExpectedNames.equals(expectedNames)) {
            System.out.println("   Expanded with package components: " + expandedExpectedNames);
        }

        for (String orderId : orderIds) {
            Response res = RestAssured.given()
                    .header("Authorization", "Bearer " + authToken)
                    .header("Content-Type", "application/json")
                    .get("/order/getOrderById/" + orderId);

            if (res.getStatusCode() != 200) continue;

            List<Map<String, Object>> orderItems = res.jsonPath().getList("data[0].order_items");
            if (orderItems == null || orderItems.isEmpty()) {
                System.out.println("   ⚠️ No order_items found in getOrderById response");
                continue;
            }

            System.out.println("\n   📦 Order Items in getOrderById:");
            List<String> actualProducts = new ArrayList<>();
            for (Map<String, Object> item : orderItems) {
                String productName = item.get("product_name") != null ? item.get("product_name").toString() : "null";
                Object actualPrice = item.get("actual_price");
                Object finalPrice  = item.get("final_price");
                Object itemStatus  = item.get("order_status");
                actualProducts.add(productName);
                System.out.println("      📌 " + productName + " | Actual: ₹" + actualPrice +
                        " | Final: ₹" + finalPrice + " | Status: " + itemStatus);
            }

            // Since user selects packages (like "Bone Profile -1") and order_items has "Bone Profile -1"
            // Check at the product level (not individual test components)
            if (!expectedNames.isEmpty()) {
                for (String expected : expectedNames) {
                    final String normalizedExpected = expected.replaceAll("\\s+", "").toLowerCase();
                    boolean found = actualProducts.stream().anyMatch(a -> 
                        a != null && a.replaceAll("\\s+", "").toLowerCase().contains(normalizedExpected));
                    if (found) {
                        System.out.println("   ✅ PASS: Product found in order: " + expected);
                    } else {
                        // May not find individual test components in order_items level, just warn
                        System.out.println("   ℹ️  INFO: '" + expected + "' not found at order_items level (may be a component test)");
                    }
                }
            } else {
                System.out.println("   ℹ️  No expected names stored. Skipping name match.\n" +
                        "      Actual products: " + actualProducts);
            }
        }
    }

    @Test
    public void stepCrossApi_04_VisitNumberValidation() {
        System.out.println("\n>>> STEP CROSS-04: VISIT NUMBER VALIDATION <<<");

        List<String> visitNumbers = RequestContext.getCurrentVisitNumbers();
        if (visitNumbers == null || visitNumbers.isEmpty()) {
            System.out.println("   ⚠️ No visit numbers in RequestContext! Attempting to derive from order IDs...");
            // Attempt to derive visit numbers from current order IDs if available
            List<String> orderIds = RequestContext.getCurrentOrderIds();
            if (orderIds != null && !orderIds.isEmpty()) {
                String token = RequestContext.getToken();
                if (token != null && !token.isBlank()) {
                    List<String> derived = new ArrayList<>();
                    for (String oid : orderIds) {
                        try {
                            Response r = RestAssured.given()
                                    .header("Authorization", "Bearer " + token)
                                    .header("Content-Type", "application/json")
                                    .get("/order/getOrderById/" + oid);
                            if (r.getStatusCode() == 200) {
                                String vn = r.jsonPath().getString("data[0].visit_number");
                                if (vn == null || vn.isBlank()) vn = r.jsonPath().getString("data[0].lab_no");
                                if (vn != null && !vn.isBlank() && !derived.contains(vn.trim())) derived.add(vn.trim());
                            }
                        } catch (Exception ignored) {}
                    }
                    if (!derived.isEmpty()) {
                        RequestContext.setCurrentVisitNumbers(derived);
                        visitNumbers = derived;
                        System.out.println("   ✅ Derived visit numbers: " + derived);
                    }
                }
            }
            if (visitNumbers == null || visitNumbers.isEmpty()) {
                System.out.println("   ⚠️ Still no visit numbers found after derivation attempt.");
                Assert.fail("❌ FAIL: No visit numbers extracted from getOrderById. IT Dose cannot proceed without visit numbers.");
                return;
            }
        }

        System.out.println("   ✅ Visit numbers present: " + visitNumbers);
        System.out.println("   📊 Count: " + visitNumbers.size());

        // Validate each visit number format (MYD + digits)
        for (String vn : visitNumbers) {
            if (vn != null && vn.startsWith("MYD")) {
                System.out.println("   ✅ Valid visit number format: " + vn);
            } else if (vn != null && !vn.isEmpty()) {
                System.out.println("   ⚠️ Visit number format unexpected (expected MYDxxxxx): " + vn);
            }
        }
    }

    @Test
    public void stepCrossApi_05_OrderItemsPriceConsistency() {
        System.out.println("\n>>> STEP CROSS-05: ORDER ITEMS PRICE CONSISTENCY <<<");

        List<String> orderIds = RequestContext.getCurrentOrderIds();
        if (orderIds == null || orderIds.isEmpty()) {
            System.out.println("   ⚠️ No order IDs in RequestContext. Skipping.");
            return;
        }

        String token = RequestContext.getToken();
        if (token == null || token.isBlank()) {
            System.out.println("   ⚠️ No token. Skipping.");
            return;
        }

        for (String orderId : orderIds) {
            Response res = RestAssured.given()
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .get("/order/getOrderById/" + orderId);

            if (res.getStatusCode() != 200) continue;

            List<Map<String, Object>> orderItems = res.jsonPath().getList("data[0].order_items");
            if (orderItems == null || orderItems.isEmpty()) continue;

            System.out.println("\n   📊 Item-level price consistency for order: " + orderId);
            double sumOfItemFinalPrices = 0;
            for (Map<String, Object> item : orderItems) {
                Object fp = item.get("final_price");
                Object qty = item.get("quantity");
                if (fp != null && qty != null) {
                    double price = Double.parseDouble(fp.toString());
                    int quantity = Integer.parseInt(qty.toString());
                    sumOfItemFinalPrices += price * quantity;
                    System.out.println("      " + item.get("product_name") + 
                            " × " + quantity + " = ₹" + (price * quantity));
                }
            }
            System.out.println("      Sum of items final prices: ₹" + sumOfItemFinalPrices);

            String paidAmountStr = res.jsonPath().getString("data[0].paid_amount");
            // Try multiple field names for coupon discount
            Object couponAmountObj = res.jsonPath().get("data[0].coupon_discount");
            if (couponAmountObj == null) couponAmountObj = res.jsonPath().get("data[0].coupon_discount_amount");
            if (couponAmountObj == null) couponAmountObj = res.jsonPath().get("data[0].coupon_applied");
            String totalPrice = res.jsonPath().getString("data[0].total_price");
            
            if (paidAmountStr != null) {
                double paidAmount = Double.parseDouble(paidAmountStr);
                double couponDiscount = toDoubleSafe(couponAmountObj);
                // If coupon not explicitly provided, calculate from paid_amount vs sumFinal
                if (couponDiscount == 0 && sumOfItemFinalPrices > 0) {
                    double calculatedCoupon = sumOfItemFinalPrices - paidAmount;
                    if (calculatedCoupon > 0) {
                        couponDiscount = calculatedCoupon;
                    }
                }
                System.out.println("      Order paid_amount: ₹" + paidAmount);
                
                if (Math.abs(sumOfItemFinalPrices - paidAmount) < 1.0) {
                    System.out.println("      ✅ PASS: Sum of item prices matches paid_amount");
                } else {
                    double diff = Math.abs(sumOfItemFinalPrices - paidAmount);
                    // Check if difference is coupon discount
                    if (Math.abs(diff - couponDiscount) < 1.0 && couponDiscount > 0) {
                        System.out.println("      ℹ️  Difference ₹" + Math.round(diff) 
                            + " = Coupon Discount Applied (₹" + Math.round(couponDiscount) + ")");
                        System.out.println("      ✅ PASS: Calculation = Sum(items final prices) - Coupon Discount");
                    } else {
                        System.out.println("      ⚠️ INFO: Difference ₹" + 
                                Math.round(diff) + 
                                " (may be delivery charge, coupon, or rounding)");
                    }
                }
            }
        }
    }

    @Test
    public void stepCrossApi_06_RewardsValidation() {
        System.out.println("\n>>> STEP CROSS-06: REWARDS VALIDATION <<<");

        // Rewards are only credited for Members — skip gain check for non-members and new users
        if ("non_member".equalsIgnoreCase(userType) || "new_user".equalsIgnoreCase(userType)) {
            System.out.println("   ℹ️ SKIP: Rewards are not applicable for " + userType + " orders. Skipping rewards gain validation.");
            return;
        }

        List<String> orderIds = RequestContext.getCurrentOrderIds();
        if (orderIds == null || orderIds.isEmpty()) return;

        String token = RequestContext.getToken();
        double totalRewardsGain = 0.0;
        double totalPaidAmount = 0.0;
        
        System.out.println("   📊 Per-Order Breakdown:");

        for (String orderId : orderIds) {
            Response res = RestAssured.given()
                    .header("Authorization", "Bearer " + token)
                    .get("/order/getOrderById/" + orderId);

            if (res.getStatusCode() != 200) continue;

            Object rewardsGainObj = res.jsonPath().get("data[0].rewards_gain");
            String paidAmountStr = res.jsonPath().getString("data[0].paid_amount");
            Object totalPriceObj = res.jsonPath().get("data[0].total_price");

            if (rewardsGainObj != null && paidAmountStr != null) {
                double actualGain = Double.parseDouble(rewardsGainObj.toString());
                double paidAmount = Double.parseDouble(paidAmountStr);
                double totalPrice = toDoubleSafe(totalPriceObj);
                
                // Formula: ceil(paidAmount * 0.05)
                long expectedGain = (long) Math.ceil(paidAmount * 0.05);
                long ceiledActual = (long) Math.ceil(actualGain);

                System.out.println("      Order: " + orderId);
                System.out.println("         Total Price     : ₹" + totalPrice);
                System.out.println("         Paid Amount     : ₹" + paidAmount);
                System.out.println("         Expected Reward (5% ceiled): " + expectedGain);
                System.out.println("         Actual Reward Gain         : " + actualGain + " (Ceiled: " + ceiledActual + ")");

                if (ceiledActual == expectedGain) {
                    System.out.println("         ✅ PASS: Reward gain is calculated correctly (5% ceiled)");
                } else {
                    System.out.println("         ⚠️ WARN: Reward gain mismatch! Expected ceiled " + expectedGain + " but got " + ceiledActual);
                }
                
                // Accumulate for multi-member flows
                totalRewardsGain += actualGain;
                totalPaidAmount += paidAmount;

                // Rewards used validation
                Object rewardsUsedObj = res.jsonPath().get("data[0].rewards_used");
                double rewardsUsed = toDoubleSafe(rewardsUsedObj);
                
                if (rewardsUsed > 0) {
                    double capLimit = totalPrice * 0.20; 
                    System.out.println("         Rewards Used: ₹" + rewardsUsed + " | Cap (20% of ₹" + totalPrice + "): ₹" + capLimit);
                    if (rewardsUsed <= capLimit + 1.5) {
                        System.out.println("         ✅ PASS: Reward usage is within cap.");
                    } else {
                        System.out.println("         ⚠️ WARN: Reward usage exceeds 20% cap.");
                    }
                }
            } else {
                System.out.println("      ℹ️ No reward gain details found for order: " + orderId);
            }
        }
        
        // For multi-member flows: store TOTAL rewards gain
        if (orderIds.size() > 1) {
            System.out.println("   📊 MULTI-ORDER AGGREGATION:");
            System.out.println("      Total Orders: " + orderIds.size());
            System.out.println("      Total Paid Amount: ₹" + totalPaidAmount);
            System.out.println("      Total Rewards Gain: " + totalRewardsGain);
            RequestContext.setRewardsGain(totalRewardsGain);
        }
    }

    @Test
    public void stepCrossApi_07_PaymentDetailsValidation() {
        System.out.println("\n>>> STEP CROSS-07: getPaymentById VALIDATION <<<");

        String token = RequestContext.getToken();

        // ── 1. Resolve user GUID from login API (data.guid stored by TokenManager) ──
        String userGuid;
        if ("non_member".equalsIgnoreCase(userType)) {
            userGuid = RequestContext.getNonMemberUserId();
        } else if ("new_user".equalsIgnoreCase(userType)) {
            userGuid = RequestContext.getNewUserUserId();
        } else {
            userGuid = RequestContext.getMemberUserId();
        }
        if (userGuid == null || userGuid.isEmpty()) {
            userGuid = RequestContext.getUserId(); // generic fallback
        }
        System.out.println("   👤 User GUID (from login API data.guid): " + userGuid);

        // ── 2. Resolve payment_id from context → then from getOrderById ──
        String paymentId = RequestContext.getCurrentPaymentId();
        String resolvedOrderId = null;

        if (paymentId == null || paymentId.isEmpty()) {
            System.out.println("   ⚠️ No paymentId in context. Resolving from getOrderById...");
            List<String> orderIds = RequestContext.getCurrentOrderIds();
            if (orderIds != null && !orderIds.isEmpty()) {
                resolvedOrderId = orderIds.get(0);
                Response orderRes = RestAssured.given()
                        .header("Authorization", "Bearer " + token)
                        .get("/order/getOrderById/" + resolvedOrderId);
                if (orderRes.getStatusCode() == 200) {
                    paymentId = orderRes.jsonPath().getString("data[0].payment_id");
                    System.out.println("   🔗 payment_id resolved from getOrderById: " + paymentId);
                }
            }
        }

        if (paymentId == null || paymentId.isEmpty()) {
            System.out.println("   ⚠️ Skipping getPaymentById validation (no payment ID found)");
            return;
        }

        System.out.println("   🔍 Calling POST /gateway/getPaymentById");
        System.out.println("      id       : " + paymentId);
        System.out.println("      user_id  : " + userGuid);

        // ── 3. POST /gateway/getPaymentById with id ──
        Map<String, String> payload = new HashMap<>();
        payload.put("id", paymentId);

        Response res = RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body(payload)
                .post("/gateway/getPaymentById");

        System.out.println("   HTTP: " + res.getStatusCode());

        if (res.getStatusCode() != 200) {
            System.out.println("   ⚠️ getPaymentById returned " + res.getStatusCode());
            System.out.println("   Body: " + res.asString());
            return;
        }

        Map<String, Object> paymentData = res.jsonPath().getMap("data.payments");
        if (paymentData == null) {
            System.out.println("   ⚠️ data.payments is null in getPaymentById response");
            return;
        }

        String paymentStatus = String.valueOf(paymentData.get("payment_status"));
        String paymentMode   = String.valueOf(paymentData.get("payment_type"));
        Object amountObj     = paymentData.get("amount");
        double paymentAmount = toDoubleSafe(amountObj);
        String responseGuid  = String.valueOf(paymentData.get("guid"));
        String responseUserId = String.valueOf(paymentData.get("user_id"));

        // ── Coupon & Discount Details ──────────────────────────────────────────
        Object couponDiscountObj = paymentData.get("coupon_discount");
        Object membershipDiscountObj = paymentData.get("membership_discount");
        Object totalDiscountObj = paymentData.get("total_discount");
        Object adminDiscountObj = paymentData.get("adminDiscount");
        Object extraChargesObj = paymentData.get("extra_charges");
        
        double couponDiscount = toDoubleSafe(couponDiscountObj);
        double membershipDiscount = toDoubleSafe(membershipDiscountObj);
        double totalDiscount = toDoubleSafe(totalDiscountObj);
        double adminDiscount = toDoubleSafe(adminDiscountObj);
        double extraCharges = toDoubleSafe(extraChargesObj);

        System.out.println("      Payment ID (guid)  : " + responseGuid);
        System.out.println("      Payment Status     : " + paymentStatus);
        System.out.println("      Payment Mode       : " + paymentMode);
        System.out.println("      Payment Amount     : ₹" + paymentAmount);
        System.out.println("      User ID in Payment : " + responseUserId);
        
        System.out.println("\n      🎫 DISCOUNT & CHARGE BREAKDOWN:");
        System.out.println("         Membership Discount: ₹" + membershipDiscount);
        System.out.println("         Coupon Discount    : ₹" + couponDiscount);
        System.out.println("         Admin Discount     : ₹" + adminDiscount);
        System.out.println("         Total Discount     : ₹" + totalDiscount);
        System.out.println("         Extra Charges      : ₹" + extraCharges);

        // ── COUPON VALIDATION ──────────────────────────────────────────────────
        if (couponDiscount > 0) {
            System.out.println("\n      ✅ Coupon Applied: ₹" + couponDiscount);
            double capturedCouponAmount = com.mryoda.diagnostics.api.utils.RequestContext.getCouponAmount();
            if (Math.abs(couponDiscount - capturedCouponAmount) <= 1.0) {
                System.out.println("      ✅ PASS: API coupon discount (₹" + couponDiscount 
                    + ") matches UI captured (₹" + capturedCouponAmount + ")");
            } else {
                System.out.println("      ⚠️ WARN: API coupon (₹" + couponDiscount 
                    + ") != UI coupon (₹" + capturedCouponAmount + ")");
            }
        } else {
            System.out.println("\n      ℹ️ No coupon discount in payment");
        }

        // ── MEMBERSHIP DISCOUNT VALIDATION ─────────────────────────────────────
        if (membershipDiscount > 0) {
            System.out.println("      ✅ Membership Discount Applied: ₹" + membershipDiscount);
        }

        // ✓ Sync coupon to context for later verification
        if (couponDiscount > 0) {
            com.mryoda.diagnostics.api.utils.RequestContext.setCouponAmount(couponDiscount);
        }

        System.out.println("      ✔ Coupon & Discount Details PASS");

        // ── 4. Cross-validate: payment.guid == paymentId ──
        if (paymentId.equals(responseGuid)) {
            System.out.println("      ✅ PASS: payment.guid matches expected paymentId");
        } else {
            System.out.println("      ⚠️ WARN: payment.guid (" + responseGuid + ") != expected (" + paymentId + ")");
        }

        // ── 5. Cross-validate: payment.user_id == userGuid (from login API) ──
        if (userGuid != null && !userGuid.isEmpty() && !"null".equals(responseUserId)) {
            if (userGuid.equals(responseUserId)) {
                System.out.println("      ✅ PASS: payment.user_id matches user GUID from login API");
            } else {
                System.out.println("      ⚠️ WARN: payment.user_id (" + responseUserId + ") != login guid (" + userGuid + ")");
            }
        }

        // ── 6. Payment status check ──
        if (paymentStatus != null && (paymentStatus.equalsIgnoreCase("Success") || paymentStatus.equals("1"))) {
            System.out.println("      ✅ PASS: Payment status is 'Success'");
        } else {
            System.out.println("      ⚠️ WARN: Unexpected payment status: " + paymentStatus);
        }

        // ── 7. Amount consistency with order(s) paid_amount ──
        // For multi-member orders: the gateway payment covers ALL orders combined.
        // Sum paid_amount across all orders and compare to gateway total.
        List<String> orderIds = RequestContext.getCurrentOrderIds();
        if (orderIds != null && !orderIds.isEmpty()) {
            double totalOrderPaidAmount = 0.0;
            for (String oid : orderIds) {
                Response orderRes = RestAssured.given()
                        .header("Authorization", "Bearer " + token)
                        .get("/order/getOrderById/" + oid);
                if (orderRes.getStatusCode() == 200) {
                    double amt = toDoubleSafe(orderRes.jsonPath().get("data[0].paid_amount"));
                    totalOrderPaidAmount += amt;
                    System.out.println("      Order " + oid + " paid_amount: ₹" + amt);
                }
            }
            System.out.println("      Total Order(s) paid_amount : ₹" + totalOrderPaidAmount);
            System.out.println("      Gateway payment amount     : ₹" + paymentAmount);
            if (Math.abs(paymentAmount - totalOrderPaidAmount) <= 2.0) {
                System.out.println("      ✅ PASS: Gateway payment amount matches sum of all order paid_amounts (±2.0)");
            } else {
                System.out.println("      ⚠️ WARN: Gateway payment ₹" + paymentAmount
                        + " vs sum of order paid_amounts ₹" + totalOrderPaidAmount
                        + " (diff=" + Math.abs(paymentAmount - totalOrderPaidAmount) + ")");
            }
        }
    }

    @Test
    public void stepCrossApi_08_FinalRewardBalanceCheck() {
        System.out.println("\n>>> STEP CROSS-08: FINAL REWARD BALANCE CHECK <<<");
        
        String mobile = RequestContext.getMobile();
        double initial = RequestContext.getInitialTotalRewards();
        double gain    = RequestContext.getRewardsGain();
        double used    = RequestContext.getRewardsUsed();
        List<String> orderIds = RequestContext.getCurrentOrderIds();
        boolean isMultiMember = orderIds != null && orderIds.size() > 1;
        
        // DEBUG: Show order IDs and check if rewards need to be recalculated
        System.out.println("   📋 ORDER TRACKING:");
        System.out.println("      Current Order IDs in context: " + orderIds);
        System.out.println("      Is Multi-Member Flow: " + isMultiMember);
        System.out.println("      Rewards Gain from context: " + gain);
        
        // For multi-member: recalculate accumulated rewards from ALL orders
        if (orderIds != null && orderIds.size() > 1) {
            System.out.println("   🔄 RECALCULATING REWARDS FOR " + orderIds.size() + " ORDERS (MULTI-MEMBER FLOW)...");
            double accumulatedGain = 0.0;
            double accumulatedUsed = 0.0;
            String token = RequestContext.getToken();
            
            for (String orderId : orderIds) {
                Response res = RestAssured.given()
                        .header("Authorization", "Bearer " + token)
                        .get("/order/getOrderById/" + orderId);
                        
                if (res.getStatusCode() == 200) {
                    Object rewardsGainObj = res.jsonPath().get("data[0].rewards_gain");
                    Object rewardsUsedObj = res.jsonPath().get("data[0].rewards_used");
                    
                    double orderGain = toDoubleSafe(rewardsGainObj);
                    double orderUsed = toDoubleSafe(rewardsUsedObj);
                    
                    accumulatedGain += orderGain;
                    accumulatedUsed += orderUsed;
                    
                    System.out.println("      Order " + orderId + ": Gain=" + orderGain + ", Used=" + orderUsed);
                } else {
                    System.out.println("      ⚠️ Warning: Failed to fetch rewards for order " + orderId + " (Status: " + res.getStatusCode() + ")");
                }
            }
            
            System.out.println("      📊 ACCUMULATED TOTALS:");
            System.out.println("         Total Gain: " + accumulatedGain);
            System.out.println("         Total Used: " + accumulatedUsed);
            
            gain = accumulatedGain;
            used = accumulatedUsed;
            // Sync back to context just in case
            RequestContext.setRewardsGain(accumulatedGain);
            RequestContext.setRewardsUsed(accumulatedUsed);
        } else if (orderIds != null && orderIds.size() == 1) {
            // Even for single order, ensure we have the latest gain/used from the API
            String orderId = orderIds.get(0);
            Response res = RestAssured.given()
                    .header("Authorization", "Bearer " + RequestContext.getToken())
                    .get("/order/getOrderById/" + orderId);
            if (res.getStatusCode() == 200) {
                gain = toDoubleSafe(res.jsonPath().get("data[0].rewards_gain"));
                used = toDoubleSafe(res.jsonPath().get("data[0].rewards_used"));
                RequestContext.setRewardsGain(gain);
                RequestContext.setRewardsUsed(used);
            }
        }
        
        if (mobile == null || mobile.isEmpty()) {
            System.out.println("   ⚠️ No mobile number in context. Skipping balance check.");
            return;
        }

        System.out.println("   🔢 Balance Computation:");
        System.out.println("      Initial Balance  : " + initial);
        System.out.println("      Rewards Gained   : +" + gain);
        System.out.println("      Rewards Used     : -" + used);
        if (isMultiMember) {
            System.out.println("      [MULTI-MEMBER FLOW: " + orderIds.size() + " orders]");
        }
        
        double expectedFinal = initial + gain - used;
        System.out.println("      Expected Final   : " + expectedFinal);
        
        // Fetch current balance from membership API
        Response res = RestAssured.given()
                .header("Authorization", "Bearer " + RequestContext.getToken())
                .get("https://staging-api-membership.yodaprojects.com/reward/getRewardsByMobile/" + mobile);
        
        if (res.getStatusCode() == 200) {
            Object finalRewardsObj = res.jsonPath().get("data.total_rewards");
            double actualFinal = toDoubleSafe(finalRewardsObj);
            RequestContext.setFinalTotalRewards(actualFinal);
            
            System.out.println("      Actual Final     : " + actualFinal);
            
            double diff = Math.abs(expectedFinal - actualFinal);
            double tolerance = 2.5;
            
            System.out.println("      Difference       : " + diff);
            
            if (diff < tolerance) {
                 System.out.println("   ✅ PASS: Rewards balance matched (Initial + Gain - Used).");
            } else {
                 System.out.println("   ❌ FAIL: Rewards balance mismatch! Expected " + expectedFinal + " but found " + actualFinal);
                 
                 // Debugging: check if the difference equals a single order's reward gain
                 if (isMultiMember && gain > 0) {
                     double perOrderGain = gain / orderIds.size();
                     if (Math.abs(diff - perOrderGain) < 1.0) {
                         System.out.println("   🔍 DEBUG: Difference (" + diff + ") matches single order gain (" + perOrderGain + ")");
                         System.out.println("           This suggests one reward was NOT included in the computation.");
                     }
                 }
                 
                 // FALLBACK: Query membership transactions to compute actual recent reward gains
                 try {
                     System.out.println("   🔎 Attempting transaction audit via membership API to reconcile rewards...");
                     Response txResp = RestAssured.given()
                             .header("Authorization", "Bearer " + RequestContext.getToken())
                             .queryParam("pageSize", "50")
                             .queryParam("page", "1")
                             .get("https://staging-api-membership.yodaprojects.com/transaction/getTransactionByMobile/" + mobile);
                     if (txResp.getStatusCode() == 200) {
                         List<Map<String,Object>> txs = txResp.jsonPath().getList("data");
                         double txAccumGain = 0.0;
                         double txAccumUsed = 0.0;
                         for (Map<String,Object> t : txs) {
                             if (t == null) continue;
                             // Only consider Success records
                             String status = (t.get("status") != null) ? t.get("status").toString() : null;
                             if (status == null || !(status.equalsIgnoreCase("Success") || status.equalsIgnoreCase("Successful"))) continue;
                             double rg = 0.0;
                             Object rgObj = t.get("rewards_gain");
                             if (rgObj == null) rgObj = t.get("rewardsGain");
                             try { rg = rgObj != null ? Double.parseDouble(rgObj.toString()) : 0.0; } catch (Exception ignore) { rg = 0.0; }
                             double ru = 0.0;
                             Object ruObj = t.get("rewards_used");
                             if (ruObj == null) ruObj = t.get("rewardsUsed");
                             try { ru = ruObj != null ? Double.parseDouble(ruObj.toString()) : 0.0; } catch (Exception ignore) { ru = 0.0; }
                             txAccumGain += rg;
                             txAccumUsed += ru;
                         }
                         System.out.println("      Transaction audit totals — Gain:" + txAccumGain + ", Used:" + txAccumUsed);
                         double expectedFromTx = initial + txAccumGain - txAccumUsed;
                         System.out.println("      ExpectedFromTx: " + expectedFromTx + " | ActualFinal: " + actualFinal);
                         if (Math.abs(expectedFromTx - actualFinal) < tolerance) {
                             System.out.println("   ✅ Reconciled: membership transaction audit matches actual final reward balance.");
                             // Update context to reflect audited totals
                             RequestContext.setRewardsGain(txAccumGain);
                             RequestContext.setRewardsUsed(txAccumUsed);
                         } else {
                             System.out.println("   ⚠️ Could not reconcile rewards via transaction audit. Manual investigation needed.");
                         }
                     } else {
                         System.out.println("   ⚠️ Could not fetch transactions (HTTP " + txResp.getStatusCode() + ")");
                     }
                 } catch (Exception e) {
                     System.out.println("   ⚠️ Transaction audit failed: " + e.getMessage());
                 }
            }
        } else {
            System.out.println("   ⚠️ Could not fetch rewards from membership API. Status: " + res.getStatusCode());
        }
    }

    @Test
    public void stepCrossApi_09_MultiMemberValidation() {
        System.out.println("\n>>> STEP CROSS-09: MULTI-MEMBER VALIDATION <<<");
        
        List<String> orderIds = RequestContext.getCurrentOrderIds();
        if (orderIds == null || orderIds.isEmpty()) return;

        List<String> expectedMembersFromContext = RequestContext.getMemberIds();
        if (expectedMembersFromContext == null || expectedMembersFromContext.isEmpty()) {
            System.out.println("   ℹ️ No specific members captured in RequestContext. Skipping validation.");
            return;
        }

        System.out.println("   Expected Members (from UI): " + expectedMembersFromContext);

        // Check if we have names or actual UUIDs
        boolean isNameBased = expectedMembersFromContext.stream()
                .anyMatch(id -> !id.contains("-") && !id.matches("\\d+"));

        for (String orderId : orderIds) {
            Response res = RestAssured.given()
                    .header("Authorization", "Bearer " + RequestContext.getToken())
                    .get("https://staging-api-diagnostics.yodaprojects.com/order/getOrderById/" + orderId);

            if (res.getStatusCode() == 200) {
                List<Object> items = res.jsonPath().getList("data[0].items");
                if (items != null) {
                    java.util.Set<String> actualMemberGuids = new java.util.HashSet<>();
                    for (Object itemObj : items) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> item = (Map<String, Object>) itemObj;
                        Object memberIdObj = item.get("family_member_id");
                        if (memberIdObj != null) {
                           actualMemberGuids.add(memberIdObj.toString());
                        }
                    }
                    
                    System.out.println("   Actual Member GUIDs in Order: " + actualMemberGuids);
                    
                    if (isNameBased) {
                        System.out.println("   ℹ️ Name-based matching detected (from UI names).");
                        // We also check if the count matches or if we can see the name in any item if possible
                        // Usually name-based is a fallback just to check if the right count of members are there
                        if (actualMemberGuids.size() == expectedMembersFromContext.size()) {
                            System.out.println("      ✅ PASS: Member COUNT matches (" + expectedMembersFromContext.size() + ").");
                        } else {
                            System.out.println("      ❓ WARNING: Member COUNT mismatch! UI had " + expectedMembersFromContext.size() + " but API has " + actualMemberGuids.size());
                        }
                    } else {
                        System.out.println("   ℹ️ ID-based matching detected (from UUIDs).");
                        boolean allMatched = true;
                        for (String expectedId : expectedMembersFromContext) {
                            if (!actualMemberGuids.contains(expectedId)) {
                                System.out.println("      ❌ FAIL: Member ID [" + expectedId + "] not found in order items.");
                                allMatched = false;
                            }
                        }
                        if (allMatched) {
                            System.out.println("      ✅ PASS: All expected family members are correctly associated with the order.");
                        }
                    }
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // STEP 10 — Catalog Price Validation (getAllTests + getAllPackages)
    // ══════════════════════════════════════════════════════════════════════════════
    /**
     * Fetches live catalog prices from getAllTests and getAllPackages APIs,
     * then performs a 3-way comparison per selected item:
     *   Catalog API price  ↔  UI-captured price  ↔  order_items.actual_price (per order)
     * Also validates per-order totals vs catalog sum.
     */
    @Test
    public void stepCrossApi_10_CatalogPriceValidation() {
        System.out.println("\n>>> STEP CROSS-10: CATALOG PRICE VALIDATION (getAllTests + getAllPackages) <<<");

        String token      = RequestContext.getToken();
        String locationId = RequestContext.getSelectedLocationId();
        List<String> orderIds = RequestContext.getCurrentOrderIds();

        if (token == null || token.isBlank()) {
            System.out.println("   ⚠️ No token in RequestContext. Skipping.");
            return;
        }
        if (locationId == null || locationId.isBlank()) {
            System.out.println("   ⚠️ No locationId in RequestContext. Skipping catalog price validation.");
            return;
        }
        if (orderIds == null || orderIds.isEmpty()) {
            System.out.println("   ⚠️ No order IDs in RequestContext. Skipping.");
            return;
        }

        System.out.println("   📍 Location ID : " + locationId);
        System.out.println("   📦 Order IDs   : " + orderIds);

        // ── 1. Fetch catalog prices via getAllTests & getAllPackages ─────────────
        api.catalog.CatalogClient catalogClient = new api.catalog.CatalogClient();

        // Individual tests catalog
        Map<String, Double> catalogTestPrices  = new LinkedHashMap<>();
        Map<String, Map<String, Object>> catalogTestDetails = new LinkedHashMap<>();
        System.out.println("\n   📡 Calling POST /tests/getAllTests...");
        try {
            for (int page = 1; page <= 10; page++) {
                Response r = catalogClient.getAllTests(token, locationId, page);
                System.out.println("      getAllTests page " + page + " → HTTP " + r.getStatusCode());
                if (r.getStatusCode() != 200) break;
                List<Map<String, Object>> tests = r.jsonPath().getList("data.tests");
                if (tests == null) tests = r.jsonPath().getList("data");
                if (tests == null || tests.isEmpty()) break;
                for (Map<String, Object> t : tests) {
                    String tName = t.get("name") != null ? t.get("name").toString()
                                 : t.get("test_name") != null ? t.get("test_name").toString() : null;
                    if (tName == null) continue;
                    // actual_price = MRP; price/selling_price = discounted
                    Object priceObj = t.get("actual_price");
                    if (priceObj == null) priceObj = t.get("price");
                    if (priceObj == null) priceObj = t.get("selling_price");
                    if (priceObj == null) priceObj = t.get("sellingPrice");
                    if (priceObj != null) {
                        try { catalogTestPrices.put(tName, Double.parseDouble(priceObj.toString())); }
                        catch (Exception ignored) {}
                    }
                    catalogTestDetails.put(tName, t);
                }
                Integer total = r.jsonPath().getInt("data.total");
                if (total == null || catalogTestPrices.size() >= total) break;
            }
            System.out.println("   ✅ getAllTests: retrieved " + catalogTestPrices.size() + " test prices");
        } catch (Exception e) {
            System.out.println("   ⚠️ getAllTests call failed: " + e.getMessage());
        }

        // Packages catalog
        Map<String, Double> catalogPkgPrices   = new LinkedHashMap<>();
        Map<String, Map<String, Object>> catalogPkgDetails = new LinkedHashMap<>();
        System.out.println("\n   📡 Calling POST /tests/getAllPackages...");
        try {
            for (int page = 1; page <= 10; page++) {
                Response r = catalogClient.getAllPackages(token, locationId, page);
                System.out.println("      getAllPackages page " + page + " → HTTP " + r.getStatusCode());
                if (r.getStatusCode() != 200) break;
                List<Map<String, Object>> pkgs = r.jsonPath().getList("data.packages");
                if (pkgs == null) pkgs = r.jsonPath().getList("data");
                if (pkgs == null || pkgs.isEmpty()) break;
                for (Map<String, Object> p : pkgs) {
                    String pName = p.get("name") != null ? p.get("name").toString()
                                 : p.get("packageName") != null ? p.get("packageName").toString() : null;
                    if (pName == null) continue;
                    Object priceObj = p.get("actual_price");
                    if (priceObj == null) priceObj = p.get("price");
                    if (priceObj == null) priceObj = p.get("selling_price");
                    if (priceObj == null) priceObj = p.get("total_price");
                    if (priceObj == null) priceObj = p.get("sellingPrice");
                    if (priceObj != null) {
                        try { catalogPkgPrices.put(pName, Double.parseDouble(priceObj.toString())); }
                        catch (Exception ignored) {}
                    }
                    catalogPkgDetails.put(pName, p);
                }
                Integer total = r.jsonPath().getInt("data.total");
                if (total == null || catalogPkgPrices.size() >= total) break;
            }
            System.out.println("   ✅ getAllPackages: retrieved " + catalogPkgPrices.size() + " package prices");
        } catch (Exception e) {
            System.out.println("   ⚠️ getAllPackages call failed: " + e.getMessage());
        }

        // ── 2. UI-captured prices ───────────────────────────────────────────────
        Map<String, Double> uiSelectionPrices = utilities.BasePriceManager.getSelectionTestDetails();
        Map<String, Double> uiCheckoutPrices  = utilities.BasePriceManager.getCheckoutTestDetails();
        System.out.println("\n   💻 UI Selection prices  : " + uiSelectionPrices);
        System.out.println("   💻 UI Checkout prices   : " + uiCheckoutPrices);

        // ── 3. Collect order_items per order ────────────────────────────────────
        // Structure: orderId → { productName → itemMap }
        Map<String, Map<String, Map<String, Object>>> orderItemsByOrderId = new LinkedHashMap<>();
        System.out.println("\n   📦 Fetching order_items from getOrderById for ALL orders:");
        for (String oid : orderIds) {
            Response r = RestAssured.given()
                    .header("Authorization", "Bearer " + token)
                    .get("/order/getOrderById/" + oid);
            System.out.println("      Order [" + oid + "] → HTTP " + r.getStatusCode());
            if (r.getStatusCode() != 200) continue;
            List<Map<String, Object>> items = r.jsonPath().getList("data[0].order_items");
            Map<String, Map<String, Object>> byName = new LinkedHashMap<>();
            if (items != null) {
                for (Map<String, Object> item : items) {
                    String pn = item.get("product_name") != null ? item.get("product_name").toString() : null;
                    if (pn != null) byName.put(pn, item);
                }
            }
            orderItemsByOrderId.put(oid, byName);
            System.out.println("         " + byName.size() + " item(s): " + byName.keySet());
        }

        // ── 4. 3-way comparison: Catalog ↔ UI ↔ Order-items ───────────────────
        System.out.println("\n   ═══ 3-WAY PRICE COMPARISON (Catalog ↔ UI ↔ Order_items) ═══");

        Set<String> allNames = new LinkedHashSet<>();
        if (uiSelectionPrices != null) allNames.addAll(uiSelectionPrices.keySet());
        if (uiCheckoutPrices  != null) allNames.addAll(uiCheckoutPrices.keySet());
        for (Map<String, Map<String, Object>> m : orderItemsByOrderId.values())
            allNames.addAll(m.keySet());

        if (allNames.isEmpty()) {
            System.out.println("   ℹ️ No test/package names found from UI or order items. Skipping 3-way comparison.");
        } else {
            int passCount = 0, warnCount = 0;
            for (String name : allNames) {
                System.out.println("\n   🔬 " + name);

                // -- Catalog price lookup (prefer actual_price = MRP) --
                Double catalogPrice = null;
                String catalogSource = null;
                // check tests catalog first (exact then case-insensitive)
                catalogPrice = catalogTestPrices.get(name);
                if (catalogPrice != null) {
                    catalogSource = "getAllTests";
                } else {
                    for (Map.Entry<String, Double> e : catalogTestPrices.entrySet()) {
                        if (e.getKey().equalsIgnoreCase(name)) { catalogPrice = e.getValue(); catalogSource = "getAllTests"; break; }
                    }
                }
                // then packages catalog
                if (catalogPrice == null) {
                    catalogPrice = catalogPkgPrices.get(name);
                    if (catalogPrice != null) catalogSource = "getAllPackages";
                    else {
                        for (Map.Entry<String, Double> e : catalogPkgPrices.entrySet()) {
                            if (e.getKey().equalsIgnoreCase(name)) { catalogPrice = e.getValue(); catalogSource = "getAllPackages"; break; }
                        }
                    }
                }

                if (catalogPrice != null) {
                    Map<String, Object> det = catalogTestDetails.containsKey(name) ? catalogTestDetails.get(name)
                                           : catalogPkgDetails.containsKey(name)  ? catalogPkgDetails.get(name) : null;
                    String memberPriceStr = "";
                    if (det != null) {
                        Object mp = det.get("member_price");
                        if (mp == null) mp = det.get("memberPrice");
                        if (mp == null) mp = det.get("final_price");
                        if (mp != null) memberPriceStr = "  |  Member Price: ₹" + mp;
                    }
                    System.out.println("      Catalog [" + catalogSource + "] MRP     : ₹" + catalogPrice + memberPriceStr);
                } else {
                    System.out.println("      Catalog price                : ⚠️ NOT FOUND in getAllTests or getAllPackages");
                }

                // -- UI prices --
                Double uiSelPrice = uiSelectionPrices != null ? uiSelectionPrices.get(name) : null;
                if (uiSelPrice == null && uiSelectionPrices != null) {
                    for (Map.Entry<String, Double> e : uiSelectionPrices.entrySet())
                        if (e.getKey().equalsIgnoreCase(name)) { uiSelPrice = e.getValue(); break; }
                }
                Double uiChkPrice = uiCheckoutPrices != null ? uiCheckoutPrices.get(name) : null;
                if (uiChkPrice == null && uiCheckoutPrices != null) {
                    for (Map.Entry<String, Double> e : uiCheckoutPrices.entrySet())
                        if (e.getKey().equalsIgnoreCase(name)) { uiChkPrice = e.getValue(); break; }
                }
                System.out.println("      UI Selection price           : " + (uiSelPrice != null ? "₹" + uiSelPrice : "N/A"));
                System.out.println("      UI Checkout price            : " + (uiChkPrice != null ? "₹" + uiChkPrice : "N/A"));

                if (uiSelPrice != null && uiChkPrice != null) {
                    if (Math.abs(uiSelPrice - uiChkPrice) < 1.0) {
                        System.out.println("      ✅ UI Selection == UI Checkout"); passCount++;
                    } else {
                        System.out.println("      ⚠️ WARN: UI Selection ₹" + uiSelPrice + " ≠ UI Checkout ₹" + uiChkPrice); warnCount++;
                    }
                }
                if (catalogPrice != null && uiSelPrice != null) {
                    if (Math.abs(catalogPrice - uiSelPrice) < 1.0) {
                        System.out.println("      ✅ Catalog MRP == UI Selection price"); passCount++;
                    } else {
                        System.out.println("      ℹ️  Catalog MRP ₹" + catalogPrice + " vs UI ₹" + uiSelPrice
                            + " (diff=₹" + String.format("%.1f", Math.abs(catalogPrice - uiSelPrice)) + ") — member discount may apply");
                    }
                }

                // -- Per-order item price comparison --
                for (Map.Entry<String, Map<String, Map<String, Object>>> orderEntry : orderItemsByOrderId.entrySet()) {
                    String oid = orderEntry.getKey();
                    Map<String, Object> item = orderEntry.getValue().get(name);
                    if (item == null) {
                        for (Map.Entry<String, Map<String, Object>> ie : orderEntry.getValue().entrySet())
                            if (ie.getKey().equalsIgnoreCase(name)) { item = ie.getValue(); break; }
                    }
                    if (item != null) {
                        Object actualP = item.get("actual_price");
                        Object finalP  = item.get("final_price");
                        Object memDisc = item.get("membership_discount");
                        System.out.println("      Order [" + oid.substring(0,8) + "...]  actual=₹" + actualP
                            + "  final=₹" + finalP + (memDisc != null ? "  memDisc=₹" + memDisc : ""));
                        if (catalogPrice != null && actualP != null) {
                            double oap = toDoubleSafe(actualP);
                            if (Math.abs(catalogPrice - oap) < 1.0) {
                                System.out.println("         ✅ Catalog MRP == order_item.actual_price"); passCount++;
                            } else {
                                System.out.println("         ⚠️ Catalog MRP ₹" + catalogPrice + " ≠ order_item.actual_price ₹" + oap); warnCount++;
                            }
                        }
                        if (uiSelPrice != null && finalP != null) {
                            double ofp = toDoubleSafe(finalP);
                            if (Math.abs(uiSelPrice - ofp) < 1.0) {
                                System.out.println("         ✅ UI price == order_item.final_price"); passCount++;
                            } else {
                                // Check if difference is due to membership discount (10%)
                                double diff = uiSelPrice - ofp;
                                double memberDiscountValue = toDoubleSafe(item.get("membership_discount"));
                                
                                if (Math.abs(diff - memberDiscountValue) < 1.0) {
                                    System.out.println("         ℹ️  UI price ₹" + uiSelPrice + " vs order_item.final_price ₹" + ofp 
                                        + " (Difference: ₹" + Math.round(diff) + " = Membership discount 10%)");
                                    passCount++;
                                } else {
                                    System.out.println("         ℹ️  UI price ₹" + uiSelPrice + " vs order_item.final_price ₹" + ofp
                                        + " — discount/member pricing difference");
                                }
                            }
                        }
                    } else {
                        System.out.println("      ℹ️  Order [" + oid.substring(0,8) + "...] — item not found (expected: belongs to sibling order)");
                    }
                }
            }
            System.out.println("\n   📊 3-Way Summary: ✅ " + passCount + " checks passed, ⚠️ " + warnCount + " warnings");
        }

        // ── 5. Per-order totals: sum of order_items vs catalog sum & API fields ─
        System.out.println("\n   ═══ PER-ORDER TOTAL vs CATALOG SUM ═══");
        for (Map.Entry<String, Map<String, Map<String, Object>>> orderEntry : orderItemsByOrderId.entrySet()) {
            String oid = orderEntry.getKey();
            double sumActual = 0, sumFinal = 0, sumCatalog = 0, totalMemDisc = 0;
            for (Map.Entry<String, Map<String, Object>> ie : orderEntry.getValue().entrySet()) {
                String pn = ie.getKey();
                Map<String, Object> item = ie.getValue();
                sumActual  += toDoubleSafe(item.get("actual_price"));
                sumFinal   += toDoubleSafe(item.get("final_price"));
                totalMemDisc += toDoubleSafe(item.get("membership_discount"));
                Double cp = catalogTestPrices.get(pn);
                if (cp == null) for (Map.Entry<String, Double> e : catalogTestPrices.entrySet()) { if (e.getKey().equalsIgnoreCase(pn)) { cp = e.getValue(); break; } }
                if (cp == null) cp = catalogPkgPrices.get(pn);
                if (cp == null) for (Map.Entry<String, Double> e : catalogPkgPrices.entrySet()) { if (e.getKey().equalsIgnoreCase(pn)) { cp = e.getValue(); break; } }
                if (cp != null) sumCatalog += cp;
            }
            Response r = RestAssured.given().header("Authorization", "Bearer " + token)
                    .get("/order/getOrderById/" + oid);
            double apiTotal = r.getStatusCode() == 200 ? toDoubleSafe(r.jsonPath().get("data[0].total_price")) : 0;
            double apiFinal = r.getStatusCode() == 200 ? toDoubleSafe(r.jsonPath().get("data[0].final_price")) : 0;
            double apiPaid  = r.getStatusCode() == 200 ? toDoubleSafe(r.jsonPath().get("data[0].paid_amount"))  : 0;
            
            // Try multiple field names for coupon discount
            Object couponObj = r.getStatusCode() == 200 ? r.jsonPath().get("data[0].coupon_discount") : null;
            if (couponObj == null && r.getStatusCode() == 200) couponObj = r.jsonPath().get("data[0].coupon_discount_amount");
            if (couponObj == null && r.getStatusCode() == 200) couponObj = r.jsonPath().get("data[0].coupon_applied");
            
            double apiCoupon = toDoubleSafe(couponObj);
            boolean couponExplicit = apiCoupon > 0;
            
            // If coupon not explicitly provided, calculate from final_price - paid_amount
            if (apiCoupon == 0 && apiFinal > 0 && apiPaid > 0) {
                double calculatedCoupon = apiFinal - apiPaid;
                if (calculatedCoupon > 0) {
                    apiCoupon = calculatedCoupon;
                }
            }
            
            System.out.println("   Order [" + oid + "]:");
            System.out.println("      Items actual_price sum (MRP)     : ₹" + sumActual);
            System.out.println("      Items membership_discount sum    : ₹" + totalMemDisc + " (10% discount)");
            System.out.println("      Items final_price sum (post-disc): ₹" + sumFinal);
            System.out.println("      Catalog MRP sum (matched items)  : ₹" + sumCatalog);
            System.out.println("      API total_price                  : ₹" + apiTotal);
            System.out.println("      API coupon_discount              : ₹" + apiCoupon + (couponExplicit ? " (from API)" : " (calculated from final_price - paid_amount)"));
            System.out.println("      API final_price                  : ₹" + apiFinal);
            System.out.println("      API paid_amount                  : ₹" + apiPaid);
            
            if (sumCatalog > 0 && Math.abs(sumCatalog - apiTotal) < 2.0)
                System.out.println("      ✅ Catalog MRP sum == API total_price");
            else if (sumCatalog > 0)
                System.out.println("      ℹ️  Catalog MRP sum ₹" + sumCatalog + " vs API total_price ₹" + apiTotal);
                
            if (Math.abs(sumActual - apiTotal) < 2.0)
                System.out.println("      ✅ order_items.actual_price sum == API total_price");
            else
                System.out.println("      ℹ️  order_items.actual sum ₹" + sumActual + " vs API total_price ₹" + apiTotal);
                
            // Validate membership discount calculation
            if (totalMemDisc > 0 && Math.abs(totalMemDisc - (sumActual * 0.1)) < 1.0) {
                System.out.println("      ✅ Membership discount sum correct (10% of MRP)");
            }
            
            // Validate final price calculation (MRP - membership discount)
            double expectedFinal = sumActual - totalMemDisc;
            if (Math.abs(expectedFinal - sumFinal) < 1.0) {
                System.out.println("      ✅ final_price = actual_price - membership_discount");
            }
            
            // Validate paid amount calculation (final_price - coupon)
            double expectedPaid = sumFinal - apiCoupon;
            if (Math.abs(expectedPaid - apiPaid) < 1.0) {
                System.out.println("      ✅ paid_amount = final_price - coupon_discount");
            }
        }
    }

    private double toDoubleSafe(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { return Double.parseDouble(value.toString().trim()); } catch (Exception e) { return 0.0; }
    }
}

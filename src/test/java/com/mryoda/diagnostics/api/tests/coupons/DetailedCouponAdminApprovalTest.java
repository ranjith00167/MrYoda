package com.mryoda.diagnostics.api.tests.coupons;

import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.tests.order.CreateOrderCODAPITest;
import com.mryoda.diagnostics.api.utils.AssertionUtil;
import com.mryoda.diagnostics.api.utils.RequestContext;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DETAILED COUPON ADMIN APPROVAL FLOW (TC_CPN_017)
 * Follows the complete step-by-step flow:
 * Login -> Location -> Search -> AddToCart (w/ Coupon) -> Address -> Slot ->
 * VerifyPayment -> Admin Login -> Approval
 */
public class DetailedCouponAdminApprovalTest extends CreateOrderCODAPITest {

    private String validCouponGuid;
    private double minOrderAmount = 0;
    private String cartGuid;
    private String paymentId;
    private String orderId;
    private int remainingAmount;

    @Test(priority = 1, description = "Step 1: Discover a working coupon")
    public void step01_DiscoverCoupon() {
        System.out.println("\n>>> Step 1: Discovering Working Coupon...");
        String userId = RequestContext.getMemberUserId();
        String token = RequestContext.getMemberToken();
        if (userId == null || token == null)
            Assert.fail("User context missing. Ensure Login and Search ran first.");

        String[] types = { "prime", "nonPrime" };
        for (String type : types) {
            Map<String, String> payload = new HashMap<>();
            payload.put("coupon_user_type", type);
            Response response = RestAssured.given()
                    .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                    .contentType(ContentType.JSON)
                    .body(payload)
                    .post(APIEndpoints.GET_ALL_COUPONS);

            if (response.getStatusCode() == 200) {
                List<Map<String, Object>> coupons = response.jsonPath().getList("data");
                if (coupons != null && !coupons.isEmpty()) {
                    validCouponGuid = (String) coupons.get(0).get("guid");
                    Object mo = coupons.get(0).get("min_order_amount");
                    minOrderAmount = mo instanceof Number ? ((Number) mo).doubleValue() : 0;
                    System.out.println(
                            "   ✅ Selected Coupon: " + validCouponGuid + " (Min Order: " + minOrderAmount + ")");
                    break;
                }
            }
        }
        Assert.assertNotNull(validCouponGuid, "Coupon discovery failed");
    }

    @Test(priority = 2, dependsOnMethods = "step01_DiscoverCoupon", description = "Step 2: Add to Cart with Coupon")
    public void step02_AddToCartWithCoupon() {
        System.out.println("\n>>> Step 2: Adding to Cart with Coupon...");
        String userId = RequestContext.getMemberUserId();
        String token = RequestContext.getMemberToken();

        // Build Payload
        Map<String, Object> payload = buildCouponCartPayload(userId, validCouponGuid, minOrderAmount);

        Response response = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(payload)
                .post(APIEndpoints.ADD_TO_CART);

        AssertionUtil.verifyStatusCode(response, 200);
        System.out.println("   ✅ Successfully added items with coupon " + validCouponGuid);

        // Capture Cart Details
        Response getCart = callGetCartAPI(token, userId, "home");
        Object dataObj = getCart.jsonPath().get("data");
        String dataPath = (dataObj instanceof List) ? "data[0]" : "data";

        cartGuid = getCart.jsonPath().getString(dataPath + ".guid");
        remainingAmount = getCart.jsonPath().get(dataPath + ".totalPrice") != null
                ? getCart.jsonPath().getInt(dataPath + ".totalPrice")
                : 0;

        System.out.println("   Cart GUID: " + cartGuid);
        System.out.println("   Total Remaining to Pay (after coupon): ₹" + remainingAmount);
        Assert.assertNotNull(cartGuid, "Cart GUID should not be null");
    }

    @Test(priority = 3, dependsOnMethods = "step02_AddToCartWithCoupon", description = "Step 3: Setup Address and Slot")
    public void step03_AddressAndSlot() {
        System.out.println("\n>>> Step 3: Setting up Address and Slot...");
        String userId = RequestContext.getMemberUserId();
        String token = RequestContext.getMemberToken();

        // Address
        Map<String, String> addressDetails = callAddAddressAPI(token, userId);
        String addressId = addressDetails.get("id");
        String addressGuid = addressDetails.get("guid");

        // Slot
        Map<String, String> slotDetails = findAvailableSlot(token, addressGuid);
        String slotGuid = slotDetails.get("guid");

        // Update Cart
        updateCartWithSlot(token, userId, slotGuid, addressGuid);

        // Push values to context for Step 4
        RequestContext.setCurrentAddressId(addressId);
        RequestContext.setCurrentSlotGuid(slotGuid);
        RequestContext.setExpectedSlotTiming(slotDetails.get("date"), slotDetails.get("time"));

        System.out.println("   ✅ Step 3 Complete.");
    }

    @Test(priority = 4, dependsOnMethods = "step03_AddressAndSlot", description = "Step 4: Verify Payment (Create Order)")
    public void step04_VerifyPaymentAndCreateOrder() {
        System.out.println("\n>>> Step 4: Verifying Payment & Creating Order...");
        String userId = RequestContext.getMemberUserId();
        String token = RequestContext.getMemberToken();
        String addressId = RequestContext.getCurrentAddressId();
        String slotGuid = RequestContext.getCurrentSlotGuid();
        String date = RequestContext.getExpectedSlotDate();
        String time = RequestContext.getExpectedSlotTimeString();
        String labLocationId = RequestContext.getSelectedLocationId();
        if (labLocationId == null || labLocationId.trim().isEmpty()) {
            labLocationId = RequestContext.getLocationId(DEFAULT_LOCATION);
        }
        if (labLocationId == null || labLocationId.trim().isEmpty()) {
            labLocationId = "676a5fa720093d2807af03a5";
        }

        Map<String, String> verifyResult = callVerifyPaymentAPI(token, userId, cartGuid, addressId, slotGuid,
                labLocationId, "home", remainingAmount, date, time, "mobile");

        paymentId = verifyResult.get("paymentId");
        orderId = verifyResult.get("orderId");

        Assert.assertNotNull(paymentId, "Payment ID missing");
        Assert.assertNotNull(orderId, "Order ID missing");
        System.out.println("   ✅ Order Created: " + orderId + " | Payment ID: " + paymentId);
    }

    @Test(priority = 5, dependsOnMethods = "step04_VerifyPaymentAndCreateOrder", description = "Step 5: Admin Login and Status Check")
    public void step05_AdminCheck() {
        System.out.println("\n>>> Step 5: Admin Login & Status Verification...");

        // Admin Login
        callMainAdminLoginAPI();
        String adminToken = RequestContext.getAdminToken();
        Assert.assertNotNull(adminToken, "Admin Token missing");

        // Check Pending Status
        Response paymentDetails = callGetPaymentByIdAPI(adminToken, paymentId);
        String status = paymentDetails.jsonPath().getString("data.payments.payment_status");
        System.out.println("   Payment Status (Admin View): [" + status + "]");

        Assert.assertTrue(status != null && status.toLowerCase().contains("pending"),
                "Expected 'Pending' for COD + Coupon. Found: " + status);
        System.out.println("   ✅ Status Verified as Pending Approval.");
    }

    @Test(priority = 6, dependsOnMethods = "step05_AdminCheck", description = "Step 6: Admin Approval of Remaining Amount")
    public void step06_AdminApproval() {
        System.out.println("\n>>> Step 6: Performing Admin Approval...");
        String adminToken = RequestContext.getAdminToken();
        String adminGuid = RequestContext.getAdminGuid();
        if (adminGuid == null)
            adminGuid = "d9b1879a-b364-42f9-990c-44a9da47b293";

        // Manually call approval to handle status vs success field
        Map<String, Object> detail = new HashMap<>();
        detail.put("type", "Cash");
        detail.put("amount", remainingAmount);
        detail.put("transactionId", "");
        detail.put("remarks", "Coupon Flow Approval");

        List<Map<String, Object>> paymentDetailsList = new java.util.ArrayList<>();
        paymentDetailsList.add(detail);

        Map<String, Object> payload = new HashMap<>();
        payload.put("payment_id", paymentId);
        payload.put("approved_user_name", adminGuid);
        payload.put("payment_details", paymentDetailsList);

        Response response = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(payload)
                .post(APIEndpoints.APPROVE_PAYMENT);

        System.out.println("   Approval Response Status: " + response.getStatusCode());
        System.out.println("   Approval Response Body: " + response.getBody().asString());

        // Verify status 200/201 OR success field
        int sc = response.getStatusCode();
        boolean isSuccess = (sc == 200 || sc == 201);
        if (response.jsonPath().get("success") != null) {
            isSuccess = isSuccess && response.jsonPath().getBoolean("success");
        }

        Assert.assertTrue(isSuccess, "Approve Payment failed. Status: " + sc);

        // Final Status Check
        Response finalPayment = callGetPaymentByIdAPI(adminToken, paymentId);
        String finalStatus = finalPayment.jsonPath().getString("data.payments.payment_status");
        System.out.println("   ✅ Final Status after Admin Approval: [" + finalStatus + "]");

        Assert.assertTrue(
                finalStatus.equalsIgnoreCase("Paid") || finalStatus.equalsIgnoreCase("Approved")
                        || finalStatus.equalsIgnoreCase("Success"),
                "Payment should be Paid/Approved/Success. Found: " + finalStatus);
    }

    // --- Payload Helper ---
    private Map<String, Object> buildCouponCartPayload(String userId, String couponGuid, double minOrder) {
        String locationId = RequestContext.getSelectedLocationId();
        if (locationId == null || locationId.trim().isEmpty()) {
            locationId = RequestContext.getLocationId(DEFAULT_LOCATION);
        }
        if (locationId == null || locationId.trim().isEmpty()) {
            locationId = "676a5fa720093d2807af03a5";
        }
        String brandId = RequestContext.getBrandId("Diagnostics");
        if (brandId == null) {
            Map<String, String> brands = RequestContext.getAllBrands();
            if (!brands.isEmpty())
                brandId = brands.values().iterator().next();
        }

        Map<String, Map<String, Object>> allTests = RequestContext.getAllTests();
        Map<String, Object> testToUse = null;
        for (Map<String, Object> t : allTests.values()) {
            double price = 0;
            if (t.get("price") instanceof Number)
                price = ((Number) t.get("price")).doubleValue();
            if (price >= minOrder) {
                testToUse = t;
                break;
            }
        }
        if (testToUse == null && !allTests.isEmpty()) {
            testToUse = allTests.values().iterator().next();
        }

        Assert.assertNotNull(testToUse, "No suitable tests found in Search results. Ensure GlobalSearchAPITest ran.");

        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", userId);
        payload.put("order_type", "home");
        payload.put("lab_location_id", locationId);
        payload.put("coupon_guid", couponGuid);

        List<Map<String, Object>> products = new ArrayList<>();
        Map<String, Object> p = new HashMap<>();
        p.put("product_id", testToUse.get("_id").toString());
        p.put("quantity", 1);
        p.put("type", "home");
        p.put("brand_id", brandId);
        p.put("location_id", locationId);
        p.put("family_member_id", java.util.Collections.singletonList(userId));
        products.add(p);

        payload.put("product_details", products);
        return payload;
    }
}

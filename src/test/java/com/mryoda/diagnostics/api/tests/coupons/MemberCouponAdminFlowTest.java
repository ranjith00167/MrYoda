package com.mryoda.diagnostics.api.tests.coupons;

import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.tests.order.CreateOrderCODAPITest;
import com.mryoda.diagnostics.api.utils.AssertionUtil;
import com.mryoda.diagnostics.api.config.ConfigLoader;
import com.mryoda.diagnostics.api.utils.RequestContext;
import com.mryoda.diagnostics.api.utils.TokenManager;
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
 * COMPREHENSIVE MEMBER COUPON FLOW (TC_CPN_017)
 * This test follows the entire "normal member flow" to ensure no 500 errors
 * occur
 * and all state is correctly established before Admin Approval.
 */
public class MemberCouponAdminFlowTest extends CreateOrderCODAPITest {

    private String validCouponGuid;
    private double minOrderAmount = 0;
    private String cartGuid;
    private String paymentId;
    private String orderId;
    private int totalPrice;

    @Test(priority = 1, description = "Step 1: Member Login")
    public void step01_MemberLogin() {
        System.out.println("\n>>> Step 1: Member Login <<<");
        String mobile = ConfigLoader.getConfig().memberMobile();
        String token = TokenManager.generateToken(mobile, TokenManager.MEMBER);
        String userId = RequestContext.getMemberUserId();

        RequestContext.setToken(token);
        RequestContext.setUserId(userId);

        Assert.assertNotNull(token, "Login failed, token is null");
        System.out.println("   ✅ Member Logged In: " + userId);
    }

    @Test(priority = 2, dependsOnMethods = "step01_MemberLogin", description = "Step 2: Location & Brand Selection")
    public void step02_SetupLocationAndBrand() {
        System.out.println("\n>>> Step 2: Location & Brand Selection <<<");
        String token = RequestContext.getToken();

        // Fetch and store locations
        Response locRes = new com.mryoda.diagnostics.api.builders.RequestBuilder()
                .setEndpoint(APIEndpoints.GET_LOCATION)
                .addHeader("Authorization", token)
                .post();

        List<Map<String, Object>> locations = locRes.jsonPath().getList("data");
        if (locations != null && !locations.isEmpty()) {
            String selectedLocName = null;
            String selectedLocId = null;
            for (Map<String, Object> loc : locations) {
                String locName = (String) loc.get("title");
                String locId = (String) loc.get("_id");
                if (locName != null && locId != null) {
                    RequestContext.storeLocation(locName, locId);
                    if ("Madhapur".equalsIgnoreCase(locName)) {
                        selectedLocName = locName;
                        selectedLocId = locId;
                    }
                    if (selectedLocName == null) {
                        selectedLocName = locName;
                        selectedLocId = locId;
                    }
                }
            }
            RequestContext.setSelectedLocation(selectedLocName);
            System.out.println("   ✅ Selected Location: " + selectedLocName + " (" + selectedLocId + ")");
        }

        Response brandRes = new com.mryoda.diagnostics.api.builders.RequestBuilder()
                .setEndpoint(APIEndpoints.GET_ALL_BRANDS)
                .addHeader("Authorization", token)
                .addBodyParam("page", 1)
                .post();

        List<Map<String, Object>> brands = brandRes.jsonPath().getList("data");
        if (brands != null && !brands.isEmpty()) {
            String selectedBrandName = null;
            String selectedBrandId = null;
            for (Map<String, Object> brand : brands) {
                String brandName = (String) brand.get("title");
                String brandId = (String) brand.get("Guid");
                if (brandName != null && brandId != null) {
                    RequestContext.storeBrand(brandName, brandId);
                    if ("Diagnostics".equalsIgnoreCase(brandName)) {
                        selectedBrandName = brandName;
                        selectedBrandId = brandId;
                    }
                    if (selectedBrandName == null) {
                        selectedBrandName = brandName;
                        selectedBrandId = brandId;
                    }
                }
            }
            RequestContext.setSelectedBrand(selectedBrandName);
            System.out.println("   ✅ Selected Brand: " + selectedBrandName + " (" + selectedBrandId + ")");
        }
    }

    @Test(priority = 3, dependsOnMethods = "step02_SetupLocationAndBrand", description = "Step 3: Global Search")
    public void step03_GlobalSearch() {
        System.out.println("\n>>> Step 3: Global Search <<<");
        String token = RequestContext.getToken();
        String locationId = RequestContext.getSelectedLocationId();

        Map<String, Object> body = new HashMap<>();
        body.put("page", 1);
        body.put("limit", 50);
        body.put("search_string", "Glucose");
        body.put("sort_by", "Type");
        body.put("location", locationId);

        Response response = new com.mryoda.diagnostics.api.builders.RequestBuilder()
                .setEndpoint(APIEndpoints.GLOBAL_SEARCH)
                .addHeader("Authorization", "Bearer " + token)
                .setRequestBody(body)
                .post();

        List<Map<String, Object>> tests = response.jsonPath().getList("data.docs");
        if (tests == null)
            tests = response.jsonPath().getList("data"); // fallback
        Assert.assertNotNull(tests, "Search results should not be null");

        for (Map<String, Object> t : tests) {
            if (t == null)
                continue;
            String name = (String) t.get("test_name");
            if (name == null)
                name = (String) t.get("name");
            String id = (String) t.get("_id");
            RequestContext.storeTestDetails(name, id, (String) t.get("test_id"),
                    ((Number) t.get("price")).intValue(), 0, 0, "test");
        }
        System.out.println("   ✅ Global Search completed. Tests stored in context.");
    }

    @Test(priority = 4, dependsOnMethods = "step03_GlobalSearch", description = "Step 4: Discover Coupon")
    public void step04_DiscoverCoupon() {
        System.out.println("\n>>> Step 4: Discovering Valid Coupon <<<");
        Map<String, String> payload = new HashMap<>();
        payload.put("coupon_user_type", "prime");

        Response response = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .contentType(ContentType.JSON)
                .body(payload)
                .post(APIEndpoints.GET_ALL_COUPONS);

        List<Map<String, Object>> coupons = response.jsonPath().getList("data");
        if (coupons != null && !coupons.isEmpty()) {
            validCouponGuid = (String) coupons.get(0).get("guid");
            Object mo = coupons.get(0).get("min_order_amount");
            minOrderAmount = mo instanceof Number ? ((Number) mo).doubleValue() : 0;
            System.out.println("   ✅ Coupon Found: " + validCouponGuid + " (Min Order: " + minOrderAmount + ")");
        } else {
            Assert.fail("No prime coupons found for member");
        }
    }

    @Test(priority = 5, dependsOnMethods = "step04_DiscoverCoupon", description = "Step 5: Add to Cart with Coupon")
    public void step05_AddToCartWithCoupon() {
        System.out.println("\n>>> Step 5: Add To Cart with Coupon <<<");
        String token = RequestContext.getToken();
        String userId = RequestContext.getUserId();

        Map<String, Object> payload = buildCouponPayload(userId, validCouponGuid, minOrderAmount);

        Response response = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(payload)
                .post(APIEndpoints.ADD_TO_CART);

        AssertionUtil.verifyStatusCode(response, 200);
        cartGuid = response.jsonPath().getString("data.guid");
        System.out.println("   ✅ Added to Cart. GUID: " + cartGuid);
    }

    @Test(priority = 6, dependsOnMethods = "step05_AddToCartWithCoupon", description = "Step 6: Verify Cart")
    public void step06_VerifyCart() {
        System.out.println("\n>>> Step 6: Verify Cart & Extract Total <<<");
        String token = RequestContext.getToken();
        String userId = RequestContext.getUserId();

        Response response = callGetCartAPI(token, userId, "home");
        Object dataObj = response.jsonPath().get("data");
        String dataPath = (dataObj instanceof List) ? "data[0]" : "data";

        totalPrice = response.jsonPath().getInt(dataPath + ".totalPrice");
        cartGuid = response.jsonPath().getString(dataPath + ".guid");
        RequestContext.setCurrentCartId(cartGuid);
        RequestContext.setCurrentTotalPrice(totalPrice);

        System.out.println("   ✅ Cart Verified. Total Price after Discount: ₹" + totalPrice);
    }

    @Test(priority = 7, dependsOnMethods = "step06_VerifyCart", description = "Step 7: Add Address & Slot")
    public void step07_AddAddressAndSlot() {
        System.out.println("\n>>> Step 7: Address and Slot Selection <<<");
        String token = RequestContext.getToken();
        String userId = RequestContext.getUserId();

        Map<String, String> address = callAddAddressAPI(token, userId);
        String addressGuid = address.get("guid");
        String addressId = address.get("id");
        RequestContext.setCurrentAddressGuid(addressGuid);
        RequestContext.setCurrentAddressId(addressId);

        Map<String, String> slot = findAvailableSlot(token, addressGuid);
        String slotGuid = slot.get("guid");
        RequestContext.setCurrentSlotGuid(slotGuid);
        RequestContext.setExpectedSlotTiming(slot.get("date"), slot.get("time"));

        updateCartWithSlot(token, userId, slotGuid, addressGuid);
        System.out.println("   ✅ Address and Slot updated in Cart.");
    }

    @Test(priority = 8, dependsOnMethods = "step07_AddAddressAndSlot", description = "Step 8: Verify Payment/Confirm Order")
    public void step08_ConfirmOrder() {
        System.out.println("\n>>> Step 8: Confirming Order (Cash) <<<");
        String token = RequestContext.getToken();
        String userId = RequestContext.getUserId();
        String addressId = RequestContext.getCurrentAddressId();
        String slotGuid = RequestContext.getCurrentSlotGuid();
        String date = RequestContext.getExpectedSlotDate();
        String time = RequestContext.getExpectedSlotTimeString();
        String locId = RequestContext.getSelectedLocationId();

        Map<String, String> verifyRes = callVerifyPaymentAPI(token, userId, cartGuid, addressId, slotGuid,
                locId, "home", totalPrice, date, time, "mobile");

        paymentId = verifyRes.get("paymentId");
        orderId = verifyRes.get("orderId");
        Assert.assertNotNull(paymentId, "Payment ID generation failed");
        System.out.println("   ✅ Order Confirmed. Order ID: " + orderId + ", Payment ID: " + paymentId);
    }

    @Test(priority = 9, dependsOnMethods = "step08_ConfirmOrder", description = "Step 9: Admin Login & Status Check")
    public void step09_AdminLoginAndCheck() {
        System.out.println("\n>>> Step 9: Admin Verification <<<");
        callMainAdminLoginAPI();
        String adminToken = RequestContext.getAdminToken();

        Response response = callGetPaymentByIdAPI(adminToken, paymentId);
        String status = response.jsonPath().getString("data.payments.payment_status");
        System.out.println("   Current Payment Status: " + status);
        Assert.assertTrue(status.toLowerCase().contains("pending"), "Status should be pending approval");
    }

    @Test(priority = 10, dependsOnMethods = "step09_AdminLoginAndCheck", description = "Step 10: Admin Approval")
    public void step10_AdminApprovePayment() {
        System.out.println("\n>>> Step 10: Admin Approving Payment <<<");
        String adminToken = RequestContext.getAdminToken();
        String adminGuid = RequestContext.getAdminGuid();
        if (adminGuid == null)
            adminGuid = "d9b1879a-b364-42f9-990c-44a9da47b293";

        // Approving the exact remaining amount
        Map<String, Object> detail = new HashMap<>();
        detail.put("type", "Cash");
        detail.put("amount", totalPrice);
        detail.put("transactionId", "");
        detail.put("remarks", "Approved via Coupon Flow");

        List<Map<String, Object>> paymentDetailsList = new ArrayList<>();
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

        System.out.println("   Approval Response: " + response.getStatusCode());
        Assert.assertTrue(response.getStatusCode() == 200 || response.getStatusCode() == 201, "Approval failed");

        Response finalCheck = callGetPaymentByIdAPI(adminToken, paymentId);
        String finalStatus = finalCheck.jsonPath().getString("data.payments.payment_status");
        System.out.println("   ✅ Final Payment Status: " + finalStatus);
        Assert.assertTrue(finalStatus.equalsIgnoreCase("Paid") || finalStatus.equalsIgnoreCase("Approved")
                || finalStatus.equalsIgnoreCase("Success"));
    }

    // --- INTERNAL HELPERS ---
    private Map<String, Object> buildCouponPayload(String userId, String couponGuid, double minOrder) {
        String locationId = RequestContext.getSelectedLocationId();
        String brandId = RequestContext.getSelectedBrandId();

        Map<String, Map<String, Object>> allTests = RequestContext.getAllTests();
        Map<String, Object> testToUse = null;
        for (Map<String, Object> t : allTests.values()) {
            if (((Number) t.get("price")).doubleValue() >= minOrder) {
                testToUse = t;
                break;
            }
        }
        if (testToUse == null)
            testToUse = allTests.values().iterator().next();

        int qty = 1;
        if (testToUse != null) {
            double price = 0;
            if (testToUse.get("price") instanceof Number) {
                price = ((Number) testToUse.get("price")).doubleValue();
            }
            if (price > 0 && price < minOrder) {
                qty = (int) Math.ceil(minOrder / price);
                if (qty > 10)
                    qty = 10;
            }
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", userId);
        payload.put("order_type", "home");
        payload.put("lab_location_id", locationId);
        payload.put("coupon_guid", couponGuid);

        List<Map<String, Object>> products = new ArrayList<>();
        Map<String, Object> p = new HashMap<>();
        p.put("product_id", testToUse.get("_id").toString());
        p.put("quantity", qty);
        p.put("type", "home");
        p.put("brand_id", brandId);
        p.put("location_id", locationId);
        p.put("family_member_id", java.util.Collections.singletonList(userId));
        products.add(p);

        payload.put("product_details", products);
        return payload;
    }
}

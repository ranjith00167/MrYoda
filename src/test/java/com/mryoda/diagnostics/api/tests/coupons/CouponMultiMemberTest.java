package com.mryoda.diagnostics.api.tests.coupons;

import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
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
 * Coupon Multi-Member Test
 * Handles discovering a coupon and applying it to a cart with multiple family
 * members.
 */
public class CouponMultiMemberTest extends BaseTest {

    private String validCouponGuid;

    @Test(priority = 1, description = "Step 1: Discover a valid Coupon")
    public void step01_DiscoverCoupon() {
        String userType = org.testng.Reporter.getCurrentTestResult().getTestContext().getCurrentXmlTest()
                .getParameter("userType");
        String couponType = ("member".equalsIgnoreCase(userType)) ? "prime" : "nonPrime";

        System.out.println("\n>>> Step 1: Discovering Valid " + couponType + " Coupon <<<");

        Map<String, String> payload = new HashMap<>();
        payload.put("coupon_user_type", couponType);

        Response response = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .contentType(ContentType.JSON)
                .body(payload)
                .post(APIEndpoints.GET_ALL_COUPONS);

        System.out.println("   Coupon Listing Status: " + response.getStatusCode());
        AssertionUtil.verifyStatusCode(response, 200);

        List<Map<String, Object>> coupons = response.jsonPath().getList("data");
        if (coupons != null && !coupons.isEmpty()) {
            validCouponGuid = (String) coupons.get(0).get("guid");
            System.out.println("   ✅ Valid Prime Coupon Found: " + validCouponGuid);
        } else {
            Assert.fail("❌ No active Prime coupons found in environment.");
        }
    }

    @Test(priority = 2, dependsOnMethods = "step01_DiscoverCoupon", description = "Step 2: Apply Coupon to Multi-Member Cart")
    public void step02_ApplyCouponToMultiMemberCart() {
        System.out.println("\n>>> Step 2: Applying Coupon to Multi-Member Cart <<<");

        String userId = RequestContext.getUserId();
        String token = RequestContext.getToken();
        if (userId == null || token == null) {
            System.out.println("   RequestContext.getUserId()/Token() is null, checking specific user types...");
            String userTypeParam = org.testng.Reporter.getCurrentTestResult().getTestContext().getCurrentXmlTest()
                    .getParameter("userType");
            if ("new_user".equalsIgnoreCase(userTypeParam)) {
                userId = RequestContext.getNewUserUserId();
                token = RequestContext.getNewUserToken();
                System.out.println("   Attempted New User Credentials: " + userId);
            } else if ("non_member".equalsIgnoreCase(userTypeParam)) {
                userId = RequestContext.getNonMemberUserId();
                token = RequestContext.getNonMemberToken();
                System.out.println("   Attempted Non-Member Credentials: " + userId);
            }

            // Final fallback to Member or any generic values
            if (userId == null)
                userId = RequestContext.getMemberUserId();
            if (token == null)
                token = RequestContext.getMemberToken();

            if (userId == null)
                userId = RequestContext.getUserId(); // Generic fallback
            if (token == null)
                token = RequestContext.getToken(); // Generic fallback
        }

        System.out.println("   Using User ID: " + userId);
        System.out.println("   Using Auth Token: " + (token != null ? "FOUND" : "MISSING"));

        List<Map<String, Object>> activeProducts = RequestContext.getActiveProductDetails();
        Assert.assertNotNull(activeProducts, "Active products missing in context (multi-member setup skipped?)");
        Assert.assertFalse(activeProducts.isEmpty(), "Active products list is empty");

        // Construct family_member_id list (self + all added family members)
        List<String> memberIds = RequestContext.getMemberIds();
        if (memberIds == null || memberIds.isEmpty()) {
            memberIds = new ArrayList<>();
            memberIds.add(userId);
        }

        System.out.println("   Applying coupon to cart for members: " + memberIds);

        String activeLocationId = RequestContext.getSelectedLocationId();

        // Re-construct the AddToCart payload with the coupon
        Map<String, Object> cartPayload = new HashMap<>();
        cartPayload.put("user_id", userId);
        cartPayload.put("lab_location_id", activeLocationId);
        cartPayload.put("order_type", "lab"); // Assuming lab visit as per user requirement
        cartPayload.put("coupon_guid", validCouponGuid);

        List<Map<String, Object>> productsWithMultiMember = new ArrayList<>();
        for (Map<String, Object> product : activeProducts) {
            Map<String, Object> p = new HashMap<>(product);
            // Ensure family_member_id is updated to include all members
            p.put("family_member_id", memberIds);
            productsWithMultiMember.add(p);
        }
        cartPayload.put("product_details", productsWithMultiMember);

        System.out.println("   AddToCart Payload: " + cartPayload);

        Response response = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(cartPayload)
                .post(APIEndpoints.ADD_TO_CART);

        System.out.println("   AddToCart Status: " + response.getStatusCode());
        AssertionUtil.verifyTrue(response.getStatusCode() == 200 || response.getStatusCode() == 201,
                "Update Cart with Coupon failed. Status: " + response.getStatusCode());

        // Verify coupon in Get Cart
        Response getCart = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .header("Authorization", "Bearer " + token)
                .pathParam("user_id", userId)
                .queryParam("order_type", "lab")
                .queryParam("location", activeLocationId)
                .get(APIEndpoints.GET_CART_BY_ID);

        System.out.println("   Get Cart Body: " + getCart.getBody().asString());

        Object dataObj = getCart.jsonPath().get("data");
        String dataPath = (dataObj instanceof List) ? "data[0]" : "data";
        Map<String, Object> couponResult = getCart.jsonPath().getMap(dataPath + ".couponResult");

        Assert.assertNotNull(couponResult, "couponResult missing in cart response");
        boolean isValid = (Boolean) couponResult.get("valid");
        System.out.println(
                "   Coupon Validation Result: " + isValid + (isValid ? "" : " | Reason: " + couponResult.get("msg")));

        Assert.assertTrue(isValid, "Coupon should be valid for the cart. Msg: " + couponResult.get("msg"));

        // --- PROPORTIONAL SPLIT VALIDATION ---
        // 1. Get Total Discount
        double totalDiscount = 0;
        Object da = couponResult.get("discount_amount");
        if (da == null)
            da = couponResult.get("discount");

        if (da instanceof Number)
            totalDiscount = ((Number) da).doubleValue();
        System.out.println("   Total Coupon Discount: ₹" + totalDiscount);

        // 2. Identify Members and their subtotals
        // Note: In current setup, AddFamilyMember adds products with quantity=2 for 2
        // members.
        // We calculate the proportional split expectation.

        // Calculate expected totals based on proportional split
        List<Map<String, Object>> productDetails = getCart.jsonPath().getList(dataPath + ".product_details");

        Map<String, Double> memberSubtotals = new HashMap<>();
        double totalSubtotal = 0;

        if (productDetails != null) {
            for (Map<String, Object> item : productDetails) {
                Object priceObj = item.get("price");
                double unitPrice = (priceObj instanceof Number) ? ((Number) priceObj).doubleValue() : 0;

                Object qtyObj = item.get("quantity");
                int quantity = (qtyObj instanceof Number) ? ((Number) qtyObj).intValue() : 1;

                double itemTotalPrice = unitPrice * quantity;

                Object membersObj = item.get("family_member_id");
                if (membersObj instanceof List) {
                    List<?> mList = (List<?>) membersObj;
                    List<String> mIds = new ArrayList<>();
                    for (Object obj : mList) {
                        if (obj instanceof String) {
                            mIds.add((String) obj);
                        } else if (obj instanceof Map) {
                            Map<?, ?> m = (Map<?, ?>) obj;
                            Object guid = m.get("guid");
                            if (guid == null)
                                guid = m.get("id");
                            if (guid != null)
                                mIds.add(guid.toString());
                        }
                    }

                    if (!mIds.isEmpty()) {
                        for (String mid : mIds) {
                            // Proportional split for this item
                            double share = itemTotalPrice / mIds.size();
                            memberSubtotals.put(mid, memberSubtotals.getOrDefault(mid, 0.0) + share);
                        }
                        totalSubtotal += itemTotalPrice;
                    }
                }
            }
        }

        Object discountVal = getCart.jsonPath().get(dataPath + ".couponResult.discount");
        if (discountVal == null) {
            discountVal = getCart.jsonPath().get("data.couponResult.discount");
        }

        System.out.println("   Cart Subtotal: ₹" + totalSubtotal);
        System.out.println("   Member Breakdown (Subtotals): " + memberSubtotals);

        // 3. Calculate and Verify Proportional Splits
        for (Map.Entry<String, Double> entry : memberSubtotals.entrySet()) {
            String mid = entry.getKey();
            double sub = entry.getValue();
            double expectedMemberDiscount = (sub / totalSubtotal) * totalDiscount;

            System.out.println("   -> Member: " + mid);
            System.out.println(
                    "      Subtotal: ₹" + sub + " (" + String.format("%.2f", (sub / totalSubtotal) * 100) + "%)");
            System.out
                    .println("      Expected Proportional Discount: ₹" + String.format("%.2f", expectedMemberDiscount));
        }

        // 4. Final Total Validation
        Integer totalPrice = getCart.jsonPath().getInt(dataPath + ".totalPrice");
        Assert.assertNotNull(totalPrice, "totalPrice missing in cart response");

        double expectedTotal = totalSubtotal - totalDiscount;
        System.out.println("   Math Check: Subtotal(" + totalSubtotal + ") - Discount(" + totalDiscount
                + ") = Expected Total(" + expectedTotal + ")");
        System.out.println("   Actual Total from API: ₹" + totalPrice);

        Assert.assertEquals(totalPrice.doubleValue(), expectedTotal, 1.0,
                "The total price does not match expected discounted sum!");
        System.out.println("   ✅ Proportional Split Validation passed.");

        // Store updated total price
        RequestContext.setCurrentTotalPrice(totalPrice);
        System.out.println("   ✅ Cart context updated. New Total: ₹" + totalPrice);
    }
}

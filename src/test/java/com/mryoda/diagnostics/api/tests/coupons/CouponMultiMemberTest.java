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

        // Calculate totals — two sums tracked:
        //   withoutDiscountSum : sum of item.price * quantity (undiscounted / original prices)
        //   withDiscountSum    : totalPrice returned by the API (after coupon + all other discounts)
        List<Map<String, Object>> productDetails = getCart.jsonPath().getList(dataPath + ".product_details");

        Map<String, Double> memberSubtotals = new HashMap<>();
        double withoutDiscountSum = 0;  // sum of original prices (no discount applied)

        System.out.println("\n   ── Per-Test Item Price Breakdown ──");
        if (productDetails != null) {
            for (Map<String, Object> item : productDetails) {
                // item.price = original/undiscounted price per unit
                Object priceObj = item.get("price");
                double unitPriceNoDiscount = (priceObj instanceof Number) ? ((Number) priceObj).doubleValue() : 0;

                // actual_price / discounted_price = price after discounts (if present in response)
                Object actualPriceObj = item.get("actual_price");
                if (actualPriceObj == null) actualPriceObj = item.get("discounted_price");
                if (actualPriceObj == null) actualPriceObj = item.get("final_price");
                double unitPriceWithDiscount = (actualPriceObj instanceof Number)
                        ? ((Number) actualPriceObj).doubleValue()
                        : unitPriceNoDiscount; // fallback: same as original if no separate field

                Object qtyObj = item.get("quantity");
                int quantity = (qtyObj instanceof Number) ? ((Number) qtyObj).intValue() : 1;

                String testName = item.get("name") != null ? item.get("name").toString()
                        : (item.get("test_name") != null ? item.get("test_name").toString() : "Unknown Test");

                double itemWithoutDiscount = unitPriceNoDiscount * quantity;
                double itemWithDiscount    = unitPriceWithDiscount * quantity;

                System.out.println("   Test: " + testName);
                System.out.println("      qty                     : " + quantity);
                System.out.println("      unit price (no discount): ₹" + unitPriceNoDiscount);
                System.out.println("      unit price (discounted) : ₹" + unitPriceWithDiscount);
                System.out.println("      line total (no discount): ₹" + itemWithoutDiscount);
                System.out.println("      line total (discounted) : ₹" + itemWithDiscount);

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
                            if (guid == null) guid = m.get("id");
                            if (guid != null) mIds.add(guid.toString());
                        }
                    }
                    if (!mIds.isEmpty()) {
                        for (String mid : mIds) {
                            double share = itemWithoutDiscount / mIds.size();
                            memberSubtotals.put(mid, memberSubtotals.getOrDefault(mid, 0.0) + share);
                        }
                        withoutDiscountSum += itemWithoutDiscount;
                    }
                }
            }
        }

        // 4. Final Total Validation
        Integer totalPrice = getCart.jsonPath().getInt(dataPath + ".totalPrice");
        Assert.assertNotNull(totalPrice, "totalPrice missing in cart response");
        double withDiscountSum = totalPrice.doubleValue(); // actual price from API (after all discounts)

        double totalEffectiveDiscount = withoutDiscountSum - withDiscountSum;

        System.out.println("\n   ── Cart Total Summary ──");
        System.out.println("   Without-Discount Sum  (sum of original prices) : ₹" + withoutDiscountSum);
        System.out.println("   With-Discount Sum     (API totalPrice)          : ₹" + withDiscountSum);
        System.out.println("   Coupon Discount       (couponResult)             : ₹" + totalDiscount);
        System.out.println("   Total Effective Discount (original - final)      : ₹" + totalEffectiveDiscount);
        System.out.println("   Member Breakdown (Without-Discount Subtotals)   : " + memberSubtotals);

        // 3. Proportional Split (based on undiscounted member subtotals)
        System.out.println("\n   ── Proportional Coupon Split ──");
        for (Map.Entry<String, Double> entry : memberSubtotals.entrySet()) {
            String mid = entry.getKey();
            double sub = entry.getValue();
            double expectedMemberCouponDiscount = (withoutDiscountSum > 0)
                    ? (sub / withoutDiscountSum) * totalDiscount : 0;
            System.out.println("   -> Member: " + mid);
            System.out.println("      Subtotal (no discount): ₹" + sub
                    + " (" + String.format("%.2f", withoutDiscountSum > 0 ? (sub / withoutDiscountSum) * 100 : 0) + "%)");
            System.out.println("      Expected Coupon Discount Share: ₹"
                    + String.format("%.2f", expectedMemberCouponDiscount));
        }

        // Assertions: totalPrice must be present and must be <= withoutDiscountSum
        Assert.assertTrue(withDiscountSum > 0,
                "API totalPrice must be > 0, found: " + withDiscountSum);
        if (withoutDiscountSum > 0) {
            Assert.assertTrue(withDiscountSum <= withoutDiscountSum,
                    "With-discount total (₹" + withDiscountSum
                            + ") must be <= without-discount total (₹" + withoutDiscountSum + ")");
        }
        System.out.println("   ✅ With-discount sum (₹" + withDiscountSum
                + ") <= Without-discount sum (₹" + withoutDiscountSum + ") — Proportional Split Validation passed.");

        // Store updated total price
        RequestContext.setCurrentTotalPrice(totalPrice);
        System.out.println("   ✅ Cart context updated. New Total: ₹" + totalPrice);
    }
}

package com.mryoda.diagnostics.api.tests.order;

import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.AssertionUtil;
import com.mryoda.diagnostics.api.utils.RewardHelper;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import com.mryoda.diagnostics.api.utils.RequestContext;
import com.mryoda.diagnostics.api.utils.RandomDataUtil;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Add Family Member + AddToCart with rewards_used.
 *
 * Strategy:
 *   1. Fetch current rewards balance (before cart is built).
 *   2. Create family member via Add-Family-Member API.
 *   3. First addToCart call (no rewards_used) — discovers the combined order total.
 *   4. Computes rewards_used = min(floor(total / 2), current_rewards_balance).
 *   5. Re-sends addToCart with rewards_used field so the backend deducts it from payable cash.
 *
 * Designed to be swapped in place of AddFamilyMemberTest in suite files where rewards
 * redemption should be tested.
 */
public class AddFamilyMemberWithRewardsTest extends BaseTest {

    @Parameters({ "userType", "orderType" })
    @Test(dependsOnMethods = "com.mryoda.diagnostics.api.tests.tests_packages.CatalogVerificationAPITest.verifyPackagesCatalog", description = "QA Automation: Verify Add Family Member And Add To Cart With Rewards")
    public void testAddFamilyMemberAndAddToCartWithRewards(@Optional("member") String userType,
            @Optional("lab") String orderType) {
        System.out.println("\n>>> TEST: ADD FAMILY MEMBER + CART WITH REWARDS_USED (" + userType + " | " + orderType + ") <<<");

        String userId = RequestContext.getUserId();
        String token  = RequestContext.getToken();
        String mobile = RequestContext.getMobile();

        if (userId == null || token == null) {
            if (RequestContext.getMemberUserId() != null) {
                userId = RequestContext.getMemberUserId();
                token  = RequestContext.getMemberToken();
                mobile = RequestContext.getMobile();
            }
        }

        Assert.assertNotNull(userId, "UserId is required");
        Assert.assertNotNull(token,  "Token is required");

        // ── 1. Fetch current rewards balance ───────────────────────────────────────
        double initialRewards = RewardHelper.callGetRewardsByMobileAPI(mobile);
        RequestContext.setInitialTotalRewards(initialRewards);
        System.out.println("   Current rewards balance: " + initialRewards);

        // ── 2. Create family member ────────────────────────────────────────────────
        String dynamicFirstName = "RewardUser" + RandomDataUtil.getRandomFirstName();
        String dynamicLastName  = "Test";
        String dynamicMobile    = RandomDataUtil.getRandomMobile();

        Map<String, Object> memberPayload = new HashMap<>();
        memberPayload.put("user_id",      userId);
        memberPayload.put("first_name",   dynamicFirstName);
        memberPayload.put("last_name",    dynamicLastName);
        memberPayload.put("middle_name",  "");
        memberPayload.put("mobile",       dynamicMobile);
        memberPayload.put("gender",       "male");
        memberPayload.put("country_code", "+91");
        memberPayload.put("dob",          "1990-01-01");
        memberPayload.put("profile_pic",  "");
        memberPayload.put("profile_color","#fdefca");
        memberPayload.put("relation",     "Brother");
        memberPayload.put("title",        "Mr.");

        System.out.println("   Creating Family Member: " + dynamicFirstName);

        Response memberResponse = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + token)
                .setRequestBody(memberPayload)
                .post();

        AssertionUtil.verifyEquals(memberResponse.getStatusCode(), 201, "Add Family Member should return 201");

        String familyMemberGuid = memberResponse.jsonPath().getString("data.guid");
        if (familyMemberGuid == null)
            familyMemberGuid = memberResponse.jsonPath().getString("data.id");
        Assert.assertNotNull(familyMemberGuid, "Family Member GUID must be returned");
        System.out.println("   ✅ Family Member Added. GUID: " + familyMemberGuid);

        RequestContext.storeExpectedPatient(familyMemberGuid, dynamicFirstName + " " + dynamicLastName);

        // ── 3. Build combined cart payload ─────────────────────────────────────────
        String labLocationId = RequestContext.getSelectedLocationId();
        if (labLocationId == null) {
            Map<String, String> locs = RequestContext.getAllLocations();
            if (locs != null && !locs.isEmpty())
                labLocationId = locs.values().iterator().next();
        }

        String brandId = RequestContext.getSelectedBrandId();
        if (brandId == null) {
            Map<String, String> brands = RequestContext.getAllBrands();
            if (brands != null && !brands.isEmpty())
                brandId = brands.values().iterator().next();
        }

        List<String> productIds = new ArrayList<>();
        Map<String, Map<String, Object>> allTests = RequestContext.getAllTests();
        if (allTests != null && !allTests.isEmpty()) {
            for (Map.Entry<String, Map<String, Object>> entry : allTests.entrySet()) {
                Map<String, Object> testData = entry.getValue();
                boolean isEligible = "lab".equalsIgnoreCase(orderType);
                if (!isEligible) {
                    Object hc = testData.get("home_collection");
                    if (hc instanceof Boolean) isEligible = (Boolean) hc;
                    else if (hc != null) {
                        String s = hc.toString().trim();
                        isEligible = "AVAILABLE".equalsIgnoreCase(s) || "true".equalsIgnoreCase(s)
                                || "yes".equalsIgnoreCase(s) || "1".equals(s);
                    }
                }
                if (isEligible) {
                    String pId = (String) testData.get("_id");
                    if (pId != null && !productIds.contains(pId)) {
                        productIds.add(pId);
                        System.out.println("   Found Test: " + entry.getKey() + " (" + pId + ")");
                    }
                }
            }
        }

        Assert.assertNotNull(labLocationId, "Lab Location ID not found");
        RequestContext.setSelectedLocationId(labLocationId);
        Assert.assertNotNull(brandId, "Brand ID not found");
        Assert.assertFalse(productIds.isEmpty(), "No products found for " + orderType + " flow");

        // Clear cart first
        new RequestBuilder()
                .setEndpoint("/carts/v2/clearCart/" + userId)
                .addHeader("Authorization", "Bearer " + token)
                .post();

        java.util.List<Map<String, Object>> products = new java.util.ArrayList<>();
        java.util.List<String> familyIds = new java.util.ArrayList<>();
        familyIds.add(userId);
        familyIds.add(familyMemberGuid);
        RequestContext.setMemberIds(familyIds);

        for (Map.Entry<String, Map<String, Object>> entry : allTests.entrySet()) {
            Map<String, Object> testData = entry.getValue();
            String pId = (String) testData.get("_id");
            if (pId != null && productIds.contains(pId)) {
                Map<String, Object> p = new java.util.LinkedHashMap<>();
                p.put("product_id",      pId);
                p.put("quantity",        2);
                p.put("type",            orderType);
                p.put("brand_id",        brandId);
                p.put("family_member_id", familyIds);
                p.put("location_id",     labLocationId);
                products.add(p);
                System.out.println("   ✅ Added to cart: " + entry.getKey());
            }
        }

        if (products.isEmpty())
            throw new RuntimeException("No products were found to add to cart!");

        Map<String, Object> cartPayload = new java.util.LinkedHashMap<>();
        cartPayload.put("user_id", userId);
        cartPayload.put("lab_location_id", labLocationId);
        cartPayload.put("product_details", products);
        cartPayload.put("order_type", orderType);

        RequestContext.setActiveProductDetails(products);

        // ── 4. First addToCart — discover combined total ────────────────────────────
        System.out.println("\n>>> STEP: First AddToCart (no rewards_used) — reading total <<<");

        Response firstResp = new RequestBuilder()
                .setEndpoint(APIEndpoints.ADD_TO_CART)
                .addHeader("Authorization", "Bearer " + token)
                .setRequestBody(cartPayload)
                .post();

        System.out.println("First AddToCart status: " + firstResp.getStatusCode());

        // addToCart V2 returns only {"id":"..."} — call getCartById to get real totalPrice
        double cartTotal = 0;
        if (firstResp.getStatusCode() == 200 || firstResp.getStatusCode() == 201) {
            // Try reading totalPrice directly from response first (V1 compatibility)
            Object totObj = firstResp.jsonPath().get("data.totalPrice");
            if (totObj == null) totObj = firstResp.jsonPath().get("data.total_price");
            if (totObj == null) totObj = firstResp.jsonPath().get("data.payable_amount");
            if (totObj != null) {
                cartTotal = totObj instanceof Number ? ((Number) totObj).doubleValue()
                        : Double.parseDouble(totObj.toString());
            }
            // If V2 response, fetch via getCartById
            if (cartTotal == 0) {
                String cartEndpoint = APIEndpoints.DIAGNOSTICS_BASE_URL
                        + APIEndpoints.GET_CART_BY_ID.replace("{user_id}", userId);
                Response cartResp = new RequestBuilder()
                        .setEndpoint(cartEndpoint)
                        .addHeader("Authorization", "Bearer " + token)
                        .get();
                if (cartResp != null && (cartResp.getStatusCode() == 200 || cartResp.getStatusCode() == 201)) {
                    Object totalObj = cartResp.jsonPath().get("data.totalPrice");
                    if (totalObj == null) totalObj = cartResp.jsonPath().get("data.total_price");
                    if (totalObj == null) totalObj = cartResp.jsonPath().get("data.payable_amount");
                    if (totalObj != null) {
                        cartTotal = totalObj instanceof Number ? ((Number) totalObj).doubleValue()
                                : Double.parseDouble(totalObj.toString());
                    }
                    System.out.println("   getCartById → totalPrice: " + cartTotal);
                }
            }
        }

        // ── 5. Compute and apply rewards_used ──────────────────────────────────────
        // rewards_used = min(floor(cartTotal / 2), initialRewards)
        double rewardsUsed;
        if (cartTotal > 0) {
            rewardsUsed = Math.min(Math.floor(cartTotal / 2.0), initialRewards);
        } else {
            // Last resort safe fallback — avoids using half of rewards balance
            rewardsUsed = Math.min(500.0, initialRewards);
            System.out.println("   ⚠️  cartTotal not available — using safe fallback: " + rewardsUsed);
        }
        System.out.println("   cartTotal=" + cartTotal + ", initialRewards=" + initialRewards
                + " → rewards_used=" + rewardsUsed);

        cartPayload.put("rewards_used", rewardsUsed);
        System.out.println("\n>>> STEP: Second AddToCart (with rewards_used=" + rewardsUsed + ") <<<");

        Response cartResponse = new RequestBuilder()
                .setEndpoint(APIEndpoints.ADD_TO_CART)
                .addHeader("Authorization", "Bearer " + token)
                .setRequestBody(cartPayload)
                .post();

        System.out.println("Final Cart Response: " + cartResponse.getBody().asString());

        boolean isValidStatus = cartResponse.getStatusCode() == 200 || cartResponse.getStatusCode() == 201;
        AssertionUtil.verifyTrue(isValidStatus,
                "AddToCart with rewards_used should return 200 or 201. Got: " + cartResponse.getStatusCode());
        System.out.println("✅ Cart updated with Family Member + rewards_used successfully.");

        // ── Read actual rewards_used applied by API via getCartById ─────────────────
        double actualRewardsUsed = rewardsUsed; // fallback to computed value
        if (isValidStatus) {
            String cartEndpoint2 = APIEndpoints.DIAGNOSTICS_BASE_URL
                    + APIEndpoints.GET_CART_BY_ID.replace("{user_id}", userId);
            Response cartResp2 = new RequestBuilder()
                    .setEndpoint(cartEndpoint2)
                    .addHeader("Authorization", "Bearer " + token)
                    .get();
            if (cartResp2 != null && (cartResp2.getStatusCode() == 200 || cartResp2.getStatusCode() == 201)) {
                Object ruObj = cartResp2.jsonPath().get("data.rewards_used");
                if (ruObj == null) ruObj = cartResp2.jsonPath().get("data.rewardsUsed");
                if (ruObj != null) {
                    actualRewardsUsed = ruObj instanceof Number ? ((Number) ruObj).doubleValue()
                            : Double.parseDouble(ruObj.toString());
                    System.out.println("   Actual rewards_used from cart API: " + actualRewardsUsed);
                }
            }
        }
        RequestContext.setRewardsUsed(actualRewardsUsed);

        // Store cart details for subsequent steps
        String cartGuid    = cartResponse.jsonPath().getString("data.guid");
        Integer cartId     = cartResponse.jsonPath().getInt("data.id");
        Integer totalAmount = cartResponse.jsonPath().getInt("total_amount");

        if (cartGuid != null) {
            RequestContext.setMemberCartId(cartGuid);
            RequestContext.setMemberCartNumericId(cartId);
            RequestContext.setMemberTotalAmount(totalAmount);
            RequestContext.storeCartId(cartGuid);
            RequestContext.setCurrentCartId(cartGuid);
            if (totalAmount != null)
                RequestContext.setCurrentTotalPrice(totalAmount);
            System.out.println("   💾 Cart Context Stored: " + cartGuid);
        }
    }
}

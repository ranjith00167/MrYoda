package com.mryoda.diagnostics.api.tests.order;

import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.AssertionUtil;
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
 * Add Family Member Test
 * Isolates the creation of a family member.
 * Designed to be run as a modular step in a TestNG suite.
 */
public class AddFamilyMemberTest extends BaseTest {

    @Parameters({ "userType", "orderType" })
    @Test(dependsOnMethods = "com.mryoda.diagnostics.api.tests.tests_packages.CatalogVerificationAPITest.verifyPackagesCatalog")
    public void testAddFamilyMemberAndAddToCart(@Optional("member") String userType,
            @Optional("home") String orderType) {
        System.out.println("\n>>> TEST: ADD FAMILY MEMBER (" + userType + " | " + orderType + ") <<<");

        String userId = RequestContext.getUserId();
        String token = RequestContext.getToken();

        if (userId == null || token == null) {
            System.out.println(
                    "⚠️ Context missing (UserId/Token). Attempting to fetch from Member Context or defaulting.");
            // Fallback for when run independently or if previous steps didn't set generic
            // context
            if (RequestContext.getMemberUserId() != null) {
                userId = RequestContext.getMemberUserId();
                token = RequestContext.getMemberToken();
            }
        }

        Assert.assertNotNull(userId, "UserId is required for Add Family Member");
        Assert.assertNotNull(token, "Token is required for Add Family Member");

        // Dynamic Data Generation
        String dynamicFirstName = "HomeUser" + RandomDataUtil.getRandomFirstName();
        String dynamicLastName = "Test";
        String dynamicMobile = RandomDataUtil.getRandomMobile();

        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", userId);
        payload.put("first_name", dynamicFirstName);
        payload.put("last_name", dynamicLastName);
        payload.put("middle_name", "");
        payload.put("mobile", dynamicMobile);
        payload.put("gender", "male");
        payload.put("country_code", "+91");
        payload.put("dob", "1990-01-01");
        payload.put("profile_pic", "");
        payload.put("profile_color", "#fdefca");
        payload.put("relation", "Brother");
        payload.put("title", "Mr.");

        System.out.println("   Creating Family Member: " + dynamicFirstName);

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + token)
                .setRequestBody(payload)
                .post();

        // Validation
        AssertionUtil.verifyEquals(response.getStatusCode(), 201, "Add Family Member should return 201");

        // Extract GUID
        String familyMemberGuid = response.jsonPath().getString("data.guid");
        if (familyMemberGuid == null)
            familyMemberGuid = response.jsonPath().getString("data.id");

        Assert.assertNotNull(familyMemberGuid, "Family Member GUID must be returned");
        System.out.println("   ✅ Family Member Added. GUID: " + familyMemberGuid);

        // Store in Context if needed by subsequent steps
        // RequestContext.setLastCreatedFamilyMemberId(familyMemberGuid);

        System.out.println("\n>>> TEST: UPDATE CART WITH FAMILY MEMBER <<<");

        // Dynamic Data Retrieval
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

        // Collect tests based on orderType
        List<String> productIds = new ArrayList<>();
        Map<String, Map<String, Object>> allTests = RequestContext.getAllTests();
        if (allTests != null && !allTests.isEmpty()) {
            for (Map.Entry<String, Map<String, Object>> entry : allTests.entrySet()) {
                Map<String, Object> testData = entry.getValue();
                boolean isEligible = false;

                if ("lab".equalsIgnoreCase(orderType)) {
                    isEligible = true; // All tests are eligible for lab visit
                } else {
                    Object homeCollectionObj = testData.get("home_collection");
                    if (homeCollectionObj instanceof Boolean) {
                        isEligible = (Boolean) homeCollectionObj;
                    } else if (homeCollectionObj != null) {
                        String strVal = homeCollectionObj.toString().trim();
                        isEligible = "AVAILABLE".equalsIgnoreCase(strVal) || "true".equalsIgnoreCase(strVal)
                                || "yes".equalsIgnoreCase(strVal) || "1".equals(strVal);
                    }
                }

                if (isEligible) {
                    String pId = (String) testData.get("_id");
                    if (pId != null && !productIds.contains(pId)) {
                        productIds.add(pId);
                        System.out.println("   Found Test for " + orderType + ": " + entry.getKey() + " (" + pId + ")");
                    }
                }
            }
        }

        Assert.assertNotNull(labLocationId, "Lab Location ID not found in Context");
        Assert.assertNotNull(brandId, "Brand ID not found in Context");
        Assert.assertTrue(!productIds.isEmpty(), "No suitable Products found for " + orderType + " flow!");

        // --- CLEAR CART FIRST (Ensures clean state for quantity tests) ---
        System.out.println("   Clearing cart for user: " + userId);
        new RequestBuilder()
                .setEndpoint("/carts/v2/clearCart/" + userId)
                .addHeader("Authorization", "Bearer " + token)
                .post();

        // --- PREPARE PRODUCT DETAILS ---
        java.util.List<Map<String, Object>> products = new java.util.ArrayList<>();
        java.util.List<String> familyIds = new java.util.ArrayList<>();
        familyIds.add(userId); // User ID first as per sample
        familyIds.add(familyMemberGuid); // Family Member ID second

        System.out.println("   Adding selected tests to cart for " + orderType + " flow...");

        for (Map.Entry<String, Map<String, Object>> entry : allTests.entrySet()) {
            String testName = entry.getKey();
            Map<String, Object> testData = entry.getValue();
            String pId = (String) testData.get("_id");

            if (pId != null && productIds.contains(pId)) {
                Map<String, Object> p = new java.util.LinkedHashMap<>();
                p.put("product_id", pId);
                p.put("quantity", 2); // 2 people
                p.put("type", orderType);
                p.put("brand_id", brandId);
                p.put("family_member_id", familyIds);
                p.put("location_id", labLocationId);
                products.add(p);
                System.out.println("   ✅ Added to cart: " + testName + " (" + pId + ")");
            }
        }

        if (products.isEmpty()) {
            throw new RuntimeException("No products were found to add to cart!");
        }

        Map<String, Object> cartPayload = new java.util.LinkedHashMap<>();
        cartPayload.put("user_id", userId);
        cartPayload.put("lab_location_id", labLocationId);
        cartPayload.put("product_details", products);
        cartPayload.put("order_type", orderType);

        System.out.println("Final AddToCart Payload (STRICT): " + cartPayload);

        // Save products for subsequent steps (like adding slot)
        RequestContext.setActiveProductDetails(products);

        Response cartResponse = new RequestBuilder()
                .setEndpoint(APIEndpoints.ADD_TO_CART)
                .addHeader("Authorization", "Bearer " + token)
                .setRequestBody(cartPayload)
                .post();

        System.out.println("Cart Response: " + cartResponse.getBody().asString());

        AssertionUtil.verifyEquals(cartResponse.getStatusCode(), 200, "Update Cart should return 200");
        System.out.println("   ✅ Cart Updated Successfully. Response: " + cartResponse.getBody().asString());

        // Store Cart Details in Context for subsequent Order/Payment steps
        String cartGuid = cartResponse.jsonPath().getString("data.guid");
        Integer cartId = cartResponse.jsonPath().getInt("data.id");
        Integer totalAmount = cartResponse.jsonPath().getInt("total_amount");

        if (cartGuid != null) {
            RequestContext.setMemberCartId(cartGuid);
            RequestContext.setMemberCartNumericId(cartId);
            RequestContext.setMemberTotalAmount(totalAmount);
            // Also set generic/legacy context if needed
            RequestContext.storeCartId(cartGuid);
            RequestContext.setCurrentCartId(cartGuid);
            if (totalAmount != null)
                RequestContext.setCurrentTotalPrice(totalAmount);

            System.out.println("   💾 Cart Context Stored: " + cartGuid);
        }
    }
}

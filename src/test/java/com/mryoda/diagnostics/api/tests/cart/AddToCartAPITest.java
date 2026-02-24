package com.mryoda.diagnostics.api.tests.cart;

import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.AssertionUtil;
import com.mryoda.diagnostics.api.utils.RequestContext;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
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
 * Add to Cart API Test Class
 * Handles /carts/v2/addCart endpoint
 * - Adds test items to cart with all required data
 * - Uses stored user_id, test_id, brand_id, location_id
 */
public class AddToCartAPITest extends BaseTest {

    private String resolveLabLocationId(Map<String, Object> payload) {
        Object payloadLabLocation = payload.get("lab_location_id");
        if (payloadLabLocation != null && !payloadLabLocation.toString().trim().isEmpty()) {
            return payloadLabLocation.toString().trim();
        }

        Object productDetailsObj = payload.get("product_details");
        if (productDetailsObj instanceof List) {
            List<?> productDetails = (List<?>) productDetailsObj;
            for (Object item : productDetails) {
                if (item instanceof Map) {
                    Object locationObj = ((Map<?, ?>) item).get("location_id");
                    if (locationObj != null && !locationObj.toString().trim().isEmpty()) {
                        return locationObj.toString().trim();
                    }
                }
            }
        }

        String selectedLocationId = RequestContext.getSelectedLocationId();
        if (selectedLocationId != null && !selectedLocationId.trim().isEmpty()) {
            return selectedLocationId.trim();
        }

        String defaultLocationId = RequestContext.getLocationId(DEFAULT_LOCATION);
        if (defaultLocationId != null && !defaultLocationId.trim().isEmpty()) {
            return defaultLocationId.trim();
        }

        return null;
    }

    private void attachCouponAndLocation(Map<String, Object> payload, String applyCoupon, String couponUserType) {
        String resolvedLabLocation = resolveLabLocationId(payload);
        if (resolvedLabLocation != null) {
            payload.put("lab_location_id", resolvedLabLocation);
        }

        if ("true".equalsIgnoreCase(applyCoupon)) {
            String couponGuid = fetchCouponGuid(couponUserType);
            if (couponGuid != null && !couponGuid.trim().isEmpty()) {
                payload.put("coupon_guid", couponGuid.trim());
                System.out.println("   ✅ coupon_guid attached: " + couponGuid);
            } else {
                System.out.println("   ⚠️ applyCoupon=true but no coupon_guid found for type: " + couponUserType);
            }
        }
    }

    private Map<String, Object> buildCartPayloadWithAllTests(String userId, String brandName, String locationName) {

        System.out.println("\n🔍 CUSTOM PAYLOAD BUILDER for " + userId);

        // Get brand and location IDs
        String brandId = RequestContext.getBrandId(brandName);
        String locationId = RequestContext.getLocationId(locationName);

        if (brandId == null || brandId.isEmpty()) {
            Map<String, String> allBrands = RequestContext.getAllBrands();
            if (!allBrands.isEmpty()) {
                brandId = allBrands.values().iterator().next();
                System.out.println(
                        "   ⚠️ Brand ID not found strictly for '" + brandName + "', using first available: " + brandId);
            } else {
                System.out.println("   ❌ Brand ID not found for '" + brandName + "' and no brands available.");
                Assert.fail("Brand ID required for AddToCart");
                return null;
            }
        }

        if (locationId == null || locationId.isEmpty()) {
            System.out.println(
                    "⚠️ Direct match for location '" + locationName + "' failed. searching for partial match...");
            Map<String, String> allLocs = RequestContext.getAllLocations();
            for (Map.Entry<String, String> entry : allLocs.entrySet()) {
                if (entry.getKey().contains(locationName)) {
                    locationId = entry.getValue();
                    System.out.println("   ✅ Found partial match: " + entry.getKey() + " -> " + locationId);
                    break;
                }
            }
            // Fallback to first available if still null
            if (locationId == null && !allLocs.isEmpty()) {
                locationId = allLocs.values().iterator().next();
                System.out.println("   ⚠️ Fallback to first available location: " + locationId);
            }

            if (locationId == null) {
                System.out.println("   ❌ Location ID not found and no locations available.");
                Assert.fail("Location ID required for AddToCart");
                return null;
            }
        }

        Map<String, Map<String, Object>> allTests = RequestContext.getAllTests();
        if (allTests == null || allTests.isEmpty()) {
            System.out.println("   ⚠️  No tests found in RequestContext (must run GlobalSearch first)");
            return null;
        }

        String flowType = System.getProperty("orderType", "home"); // Use System property set by TestNG or CLI
        System.out.println("   Final Flow Type determined: " + flowType);

        List<Map<String, Object>> productDetailsList = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> entry : allTests.entrySet()) {

            Map<String, Object> testData = entry.getValue();
            String testId = (String) testData.get("_id");

            // Check home collection
            Object homeCollectionObj = testData.get("home_collection");
            boolean isHome = false;

            if (homeCollectionObj != null) {
                if (homeCollectionObj instanceof Boolean) {
                    isHome = (Boolean) homeCollectionObj;
                } else {
                    String s = homeCollectionObj.toString().trim();
                    isHome = s.equalsIgnoreCase("AVAILABLE") || s.equalsIgnoreCase("YES") || s.equalsIgnoreCase("TRUE")
                            || s.equals("1");
                }
            }

            // For Lab Visit flow, we add tests even if they DON'T support home collection
            if (flowType.equals("lab") || isHome) {
                Map<String, Object> productDetail = new HashMap<>();
                productDetail.put("product_id", testId);
                productDetail.put("quantity", 1);
                productDetail.put("type", isHome && flowType.equals("home") ? "home" : "lab");
                productDetail.put("brand_id", brandId);
                productDetail.put("location_id", locationId);

                List<String> familyMemberIds = new ArrayList<>();
                familyMemberIds.add(userId);
                productDetail.put("family_member_id", familyMemberIds);

                productDetailsList.add(productDetail);
                System.out.println("      ✅ Added test to payload: " + entry.getKey() + " (Type: "
                        + productDetail.get("type") + ")");
            } else {
                System.out.println("      ⏭️  Skipped test (No Home Collection): " + entry.getKey());
            }
        }

        if (productDetailsList.isEmpty()) {
            System.out.println("   ⚠️  No suitable tests were added to payload.");
            return null;
        }

        // Store selected items for Report Visibility using generic storage which
        // Listener can read
        // The listener reads RequestContext.getMemberCartItems() which returns
        // List<Map<String, Object>>
        // We will store it there. Ideally we should strictly match user type, but for
        // reporting this is sufficient given sequential execution.
        RequestContext.setMemberCartItems(productDetailsList);

        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", userId);
        payload.put("product_details", productDetailsList);
        // Explicitly adding order_type as requested for home sample collection flow
        payload.put("order_type", flowType);
        payload.put("lab_location_id", locationId);

        return payload;
    }

    private Response callAddToCartAPI(String token, Map<String, Object> payload) {

        // Add order_type='home' if home collection tests are present
        List<Map<String, Object>> products = (List<Map<String, Object>>) payload.get("product_details");
        boolean hasHome = false;
        String labLoc = null;

        for (Map<String, Object> p : products) {
            if ("home".equals(p.get("type")))
                hasHome = true;
            if (labLoc == null && p.get("location_id") != null)
                labLoc = p.get("location_id").toString();
        }

        if (hasHome && labLoc != null) {
            payload.put("order_type", "home");
            payload.put("lab_location_id", labLoc);
        } else if (!hasHome && labLoc != null) {
            payload.put("order_type", "lab");
            payload.put("lab_location_id", labLoc);
        } else {
            String fallbackLabLocation = resolveLabLocationId(payload);
            if (fallbackLabLocation != null) {
                payload.put("lab_location_id", fallbackLabLocation);
            }
        }

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.ADD_TO_CART)
                .addHeader("Authorization", token)
                .setRequestBody(payload)
                .post();

        int statusCode = response.getStatusCode();
        if (statusCode != 200 && statusCode != 201) {
            System.out.println("❌ AddToCart Failed with " + statusCode + ": " + response.getBody().asString());
        }

        return response;
    }

    private void validateAddToCartResponse(Response response, String userType, Map<String, Object> payload) {
        System.out.println("\n🔍 VALIDATING ADD TO CART RESPONSE for " + userType);

        if (response.getStatusCode() != 200 && response.getStatusCode() != 201) {
            System.out.println("   ❌ FAILED RESPONSE Body: " + response.getBody().asString());
            Assert.fail("AddToCart failed with status " + response.getStatusCode());
        }

        // Print response for visibility
        System.out.println("   📄 Response Body: " + response.getBody().asString());

        Boolean success = response.jsonPath().getBoolean("success");
        AssertionUtil.verifyTrue(success, "Success flag");

        // Validate basic fields
        String cartGuid = response.jsonPath().getString("data.guid");
        Integer cartId = response.jsonPath().getInt("data.id");
        Integer totalAmount = response.jsonPath().getInt("total_amount");

        AssertionUtil.verifyNotNull(cartGuid, "Cart GUID");
        AssertionUtil.verifyNotNull(cartId, "Cart Numeric ID");

        // --- STRICT CROSS-VALIDATION WITH PAYLOAD ---
        System.out.println("   🔍 Cross-validating response with requested payload...");

        // 1. User ID Validation
        String reqUserId = (String) payload.get("user_id");
        String respUserId = response.jsonPath().getString("data.user_id");
        AssertionUtil.verifyEquals(respUserId, reqUserId, "User ID Persistence");

        // 2. Lab Location ID Validation
        if (payload.containsKey("lab_location_id")) {
            String reqLabLoc = (String) payload.get("lab_location_id");
            String respLabLoc = response.jsonPath().getString("data.lab_location_id");
            AssertionUtil.verifyEquals(respLabLoc, reqLabLoc, "Lab Location ID Persistence");
        }

        // 3. Coupon GUID Validation
        if (payload.containsKey("coupon_guid")) {
            String reqCoupon = (String) payload.get("coupon_guid");
            String respCoupon = response.jsonPath().getString("data.coupon_guid");

            // Try nested if not found at root of data
            if (respCoupon == null) {
                respCoupon = response.jsonPath().getString("data.coupon.guid");
            }

            if (respCoupon != null) {
                AssertionUtil.verifyEquals(respCoupon, reqCoupon, "Coupon GUID Persistence");
            } else {
                System.out.println("   ⚠️  WARNING: coupon_guid was sent (" + reqCoupon
                        + ") but NOT echoed in response data object.");
            }
        }

        switch (userType) {
            case "MEMBER":
                RequestContext.setMemberCartId(cartGuid);
                RequestContext.setMemberCartNumericId(cartId);
                if (totalAmount != null)
                    RequestContext.setMemberTotalAmount(totalAmount);
                break;
            case "NON_MEMBER":
                RequestContext.setNonMemberCartId(cartGuid);
                RequestContext.setNonMemberCartNumericId(cartId);
                if (totalAmount != null)
                    RequestContext.setNonMemberTotalAmount(totalAmount);
                break;
            case "NEW_USER":
                RequestContext.setNewUserCartId(cartGuid);
                RequestContext.setNewUserCartNumericId(cartId);
                if (totalAmount != null)
                    RequestContext.setNewUserTotalAmount(totalAmount);
                break;
        }
        System.out.println("   ✅ Cart Data Validated and Stored for " + userType + " (GUID: " + cartGuid + ")");
    }

    private String fetchCouponGuid(String couponUserType) {
        System.out.println("\n--- DEBUG: Fetching Coupon for " + couponUserType + " ---");
        Map<String, String> payload = new HashMap<>();
        payload.put("coupon_user_type", couponUserType);

        io.restassured.response.Response response = io.restassured.RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .contentType(io.restassured.http.ContentType.JSON)
                .body(payload)
                .post(APIEndpoints.GET_ALL_COUPONS);

        if (response.getStatusCode() == 200) {
            String body = response.asString();
            System.out.println("   DEBUG: Full Response Body: " + body);
            List<Map<String, Object>> coupons = response.jsonPath().get("data");
            if (coupons != null && !coupons.isEmpty()) {
                Map<String, Object> coupon = coupons.get(0);
                System.out.println("   DEBUG: First Coupon Data: " + coupon);

                Object guidObj = coupon.get("guid");
                if (guidObj != null) {
                    String guid = guidObj.toString().trim();
                    System.out.println("   DEBUG: Extracted GUID: [" + guid + "]");
                    return guid;
                }
            }
        }
        System.out.println(
                "   ERROR: Could not find coupon for " + couponUserType + ". Status: " + response.getStatusCode());
        return null;
    }

    @Parameters({ "orderType", "applyCoupon" })
    @Test(priority = 8, dependsOnMethods = "com.mryoda.diagnostics.api.tests.tests_packages.GlobalSearchAPITest.testGlobalSearch_ForMember")
    public void testAddToCart_ForMember(@Optional("home") String orderType, @Optional("false") String applyCoupon) {
        System.out.println(
                "\n--- AddToCart For Member (Parameter: " + orderType + ", applyCoupon: " + applyCoupon + ") ---");
        System.setProperty("orderType", orderType);
        String token = RequestContext.getMemberToken();
        String userId = RequestContext.getMemberUserId();

        Map<String, Object> payload = buildCartPayloadWithAllTests(userId, "Diagnostics", DEFAULT_LOCATION);
        if (payload != null) {
            attachCouponAndLocation(payload, applyCoupon, "prime");
            Response response = callAddToCartAPI(token, payload);
            validateAddToCartResponse(response, "MEMBER", payload);
        }
    }

    @Parameters({ "orderType", "applyCoupon" })
    @Test(priority = 8, dependsOnMethods = "com.mryoda.diagnostics.api.tests.tests_packages.GlobalSearchAPITest.testGlobalSearch_ForNonMember")
    public void testAddToCart_ForNonMember(@Optional("home") String orderType, @Optional("false") String applyCoupon) {
        System.out.println(
                "\n--- AddToCart For Non-Member (Parameter: " + orderType + ", applyCoupon: " + applyCoupon + ") ---");
        System.setProperty("orderType", orderType);
        String token = RequestContext.getNonMemberToken();
        String userId = RequestContext.getNonMemberUserId();

        Map<String, Object> payload = buildCartPayloadWithAllTests(userId, "Diagnostics", DEFAULT_LOCATION);
        if (payload != null) {
            attachCouponAndLocation(payload, applyCoupon, "nonPrime");
            Response response = callAddToCartAPI(token, payload);
            validateAddToCartResponse(response, "NON_MEMBER", payload);
        }
    }

    @Parameters({ "orderType", "applyCoupon" })
    @Test(priority = 9, dependsOnMethods = "com.mryoda.diagnostics.api.tests.tests_packages.GlobalSearchAPITest.testGlobalSearch_ForNewUser")
    public void testAddToCart_ForNewUser(@Optional("home") String orderType, @Optional("false") String applyCoupon) {
        System.out.println(
                "\n--- AddToCart For New User (Parameter: " + orderType + ", applyCoupon: " + applyCoupon + ") ---");
        System.setProperty("orderType", orderType);
        String token = RequestContext.getNewUserToken();
        String userId = RequestContext.getNewUserUserId();

        Map<String, Object> payload = buildCartPayloadWithAllTests(userId, "Diagnostics", DEFAULT_LOCATION);
        if (payload != null) {
            attachCouponAndLocation(payload, applyCoupon, "nonPrime");
            Response response = callAddToCartAPI(token, payload);
            validateAddToCartResponse(response, "NEW_USER", payload);
        }
    }
}

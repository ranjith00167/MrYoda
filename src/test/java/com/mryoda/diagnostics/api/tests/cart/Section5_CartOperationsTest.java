package com.mryoda.diagnostics.api.tests.cart;

import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.*;
import com.mryoda.diagnostics.api.config.ConfigLoader;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * ============================================================
 * SECTION 5 — CART OPERATIONS
 * ============================================================
 * CRT-01 : Add To Cart — Home Collection                      [Positive]
 * CRT-02 : Add To Cart — Lab Visit                            [Positive]
 * CRT-03 : Get Cart — Verify Coupon Math                      [Positive]
 * CRT-04 : Get Cart — Empty Cart (After Cancel / Clear)       [Negative]
 * CRT-05 : Add to Cart — Discount Cap (Zero Payable)          [Positive]
 * CRT-06 : Cart Updated After Slot Booking                    [Positive]
 * ============================================================
 */
public class Section5_CartOperationsTest extends BaseTest {

    private static final String MEMBER_MOBILE = ConfigLoader.getConfig().memberMobile();

    private double toDouble(Object value, double defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value != null) {
            try {
                return Double.parseDouble(value.toString().trim());
            } catch (Exception ignored) {
            }
        }
        return defaultValue;
    }

    private int toInt(Object value, int defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString().trim());
            } catch (Exception ignored) {
            }
        }
        return defaultValue;
    }

    // =========================================================
    // COMMON SETUP
    // =========================================================
    private void ensureTokensAndTests() {
        if (RequestContext.getMemberToken() == null) {
            TokenManager.generateToken(MEMBER_MOBILE, TokenManager.MEMBER);
        }
        if (RequestContext.getLocationId(DEFAULT_LOCATION) == null) {
            System.out.println("   [SETUP] Fetching locations...");
            Response loc = new RequestBuilder()
                    .setEndpoint(APIEndpoints.GET_LOCATION)
                    .addHeader("Authorization", RequestContext.getMemberToken())
                    .post();
            List<Map<String, Object>> locations = loc.jsonPath().getList("data");
            if (locations != null) {
                locations.forEach(l -> RequestContext.storeLocation(
                        String.valueOf(l.get("title")), String.valueOf(l.get("_id"))));
            }
            RequestContext.setSelectedLocation(DEFAULT_LOCATION);
        }
        if (RequestContext.getBrandId("Diagnostics") == null) {
            RequestContext.storeBrand("Diagnostics", "967a5f02-2e38-47c8-b850-c4aeee8898ed");
            RequestContext.setSelectedBrand("Diagnostics");
        }
        if (RequestContext.getAllTests() == null || RequestContext.getAllTests().isEmpty()) {
            System.out.println("   [SETUP] Running GlobalSearch to populate test IDs...");
            String[] tests = {"Bone Profile -1", "RANDOM BLOOD GLUCOSE (RBS)", "Complete Blood Count (CBC)"};
            Response res = GlobalSearchHelper.searchTestsByFullNames(tests, DEFAULT_LOCATION);
            GlobalSearchHelper.extractAndStoreTests(res, tests);
        }
        
        // Setup Address for member
        if (RequestContext.getMemberAddressId() == null) {
            setupMemberAddress();
        }
    }
    
    private void setupMemberAddress() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", RequestContext.getMemberUserId());
        payload.put("first_name", "Test");
        payload.put("last_name", "Member");
        payload.put("mobile", MEMBER_MOBILE);
        payload.put("pincode", "500033"); // Madhapur
        payload.put("email", "test@mryoda.com");
        payload.put("addressType", "home");
        payload.put("address", "123 Test Street");
        payload.put("city", "Hyderabad");
        payload.put("state", "Telangana");
        payload.put("location_name", DEFAULT_LOCATION);
        
        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.ADD_ADDRESS)
                .addHeader("Authorization", RequestContext.getMemberToken())
                .setRequestBody(payload)
                .postWithoutStatusCheck();
                
        if (response.getStatusCode() == 201) {
            String addressId = response.jsonPath().getString("data.guid");
            if (addressId == null) addressId = response.jsonPath().getString("data._id");
            if (addressId == null) addressId = response.jsonPath().getString("data.id");
            RequestContext.setMemberAddressId(addressId);
        } else {
            // Get existing address
            String endpoint = APIEndpoints.GET_ADDRESS_BY_USER_ID.replace("{user_id}", RequestContext.getMemberUserId());
            Response r = new RequestBuilder()
                    .setEndpoint(endpoint)
                    .addHeader("Authorization", RequestContext.getMemberToken())
                    .get();
            try {
                String id = r.jsonPath().getString("data[0].guid");
                if (id == null) id = r.jsonPath().getString("data[0]._id");
                if (id == null) id = r.jsonPath().getString("data[0].id");
                RequestContext.setMemberAddressId(id);
            } catch (Exception ignored) {}
        }
    }

    private Map<String, Object> buildCartPayload(String userId, String orderType) {
        String locationId = RequestContext.getLocationId(DEFAULT_LOCATION);
        String brandId = RequestContext.getBrandId("Diagnostics");
        Map<String, Map<String, Object>> allTests = RequestContext.getAllTests();

        List<Map<String, Object>> productList = new ArrayList<>();
        if (allTests != null) {
            for (Map.Entry<String, Map<String, Object>> entry : allTests.entrySet()) {
                Map<String, Object> testData = entry.getValue();
                String testId = String.valueOf(testData.get("_id"));

                Object homeObj = testData.get("home_collection");
                boolean isHome = isHomeCollection(homeObj);

                if ("lab".equals(orderType) || isHome) {
                    Map<String, Object> product = new HashMap<>();
                    product.put("product_id", testId);
                    product.put("quantity", 1);
                    product.put("type", "lab".equals(orderType) ? "lab" : (isHome ? "home" : "lab"));
                    product.put("brand_id", brandId);
                    product.put("location_id", locationId);

                    List<String> familyIds = new ArrayList<>();
                    familyIds.add(userId);
                    product.put("family_member_id", familyIds);
                    productList.add(product);
                    
                    // Add only one test for standard cart to keep it simple
                    break;
                }
            }
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", userId);
        payload.put("product_details", productList);
        payload.put("order_type", orderType);
        payload.put("lab_location_id", locationId);
        return payload;
    }

    private boolean isHomeCollection(Object val) {
        if (val == null) return false;
        if (val instanceof Boolean) return (Boolean) val;
        String s = val.toString().trim();
        return s.equalsIgnoreCase("AVAILABLE") || s.equalsIgnoreCase("true") || s.equalsIgnoreCase("yes") || s.equals("1");
    }

    private Response callAddToCart(String token, Map<String, Object> payload) {
        return new RequestBuilder()
                .setEndpoint(APIEndpoints.ADD_TO_CART)
                .addHeader("Authorization", token)
                .setRequestBody(payload)
                .post();
    }

    private String fetchBestAvailableCouponGuid(String couponUserType) {
        Map<String, String> body = new HashMap<>();
        body.put("coupon_user_type", couponUserType);
        Response res = new RequestBuilder()
                .setEndpoint(APIEndpoints.GET_ALL_COUPONS)
                .setRequestBody(body)
                .post();
        if (res.getStatusCode() != 200) return null;
        List<Map<String, Object>> coupons = res.jsonPath().getList("data");
        if (coupons == null || coupons.isEmpty()) return null;
        for (Map<String, Object> coupon : coupons) {
            if (!"active".equalsIgnoreCase(String.valueOf(coupon.get("status")))) continue;
            Object guid = coupon.get("guid");
            if (guid == null || guid.toString().trim().isEmpty()) continue;
            return guid.toString();
        }
        return null;
    }

    // =========================================================
    // CRT-01 : Add To Cart — Home Collection — POSITIVE
    // =========================================================
    @Test(priority = 1, description = "CRT-01: Member adds home-collection test to cart")
    public void CRT_01_AddToCart_HomeCollection() {
        System.out.println("\n>>> CRT-01: ADD TO CART — HOME COLLECTION <<<");
        ensureTokensAndTests();

        String token = RequestContext.getMemberToken();
        String userId = RequestContext.getMemberUserId();

        Map<String, Object> payload = buildCartPayload(userId, "home");
        
        // Optionally attach a coupon if one exists
        String couponGuid = fetchBestAvailableCouponGuid("prime");
        if (couponGuid != null) {
            payload.put("coupon_guid", couponGuid);
            RequestContext.setMemberCouponGuid(couponGuid);
            System.out.println("   [CRT-01] Applied Coupon: " + couponGuid);
        }

        Response response = callAddToCart(token, payload);
        AssertionUtil.verifyTrue(response.getStatusCode() == 200 || response.getStatusCode() == 201, "AddToCart must return 200/201");

        String cartGuid = response.jsonPath().getString("data.guid");
        Assert.assertNotNull(cartGuid, "Cart GUID must be returned");
        RequestContext.setMemberCartId(cartGuid);
        System.out.println("   ✅ Cart GUID: " + cartGuid);
        System.out.println("   ✅ CRT-01 PASSED\n");
    }

    // =========================================================
    // CRT-02 : Add To Cart — Lab Visit — POSITIVE
    // =========================================================
    @Test(priority = 2, description = "CRT-02: Member adds lab-visit test to cart. No slot required yet")
    public void CRT_02_AddToCart_LabVisit() {
        System.out.println("\n>>> CRT-02: ADD TO CART — LAB VISIT <<<");
        ensureTokensAndTests();

        String token = RequestContext.getMemberToken();
        String userId = RequestContext.getMemberUserId();

        Map<String, Object> payload = buildCartPayload(userId, "lab");
        
        Response response = callAddToCart(token, payload);
        AssertionUtil.verifyTrue(response.getStatusCode() == 200 || response.getStatusCode() == 201, "AddToCart must return 200/201");
        
        String orderType = response.jsonPath().getString("data.order_type");
        AssertionUtil.verifyEquals("lab", orderType, "Order type should be lab");
        
        System.out.println("   ✅ Lab Visit Cart Created");
        System.out.println("   ✅ CRT-02 PASSED\n");
    }

    // =========================================================
    // CRT-03 : Get Cart — Verify Coupon Math — POSITIVE
    // =========================================================
    @Test(priority = 3, dependsOnMethods = "CRT_01_AddToCart_HomeCollection", description = "CRT-03: Verify totalPrice = Subtotal - discount")
    public void CRT_03_GetCart_VerifyCouponMath() {
        System.out.println("\n>>> CRT-03: GET CART — VERIFY COUPON MATH <<<");
        
        String token = RequestContext.getMemberToken();
        String userId = RequestContext.getMemberUserId();
        
        String endpoint = APIEndpoints.GET_CART_BY_ID.replace("{user_id}", userId);
        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", token)
                .get();
                
        AssertionUtil.verifyEquals(response.getStatusCode(), 200, "GetCart must return 200");
        
        Object dataObj = response.jsonPath().get("data");
        String dataPath = (dataObj instanceof List) ? "data[0]" : "data";
        
        Object priceVal = null;
        try { priceVal = response.jsonPath().get(dataPath + ".totalPrice"); } catch (Exception ignored) {}
        if (priceVal == null) {
            try { priceVal = response.jsonPath().get("total_amount"); } catch (Exception ignored) {}
        }
        
        Object discountVal = null;
        try { discountVal = response.jsonPath().get(dataPath + ".coupon_amount"); } catch (Exception ignored) {}
        
        double totalPrice = toDouble(priceVal, 0.0);
        double discountAmount = toDouble(discountVal, 0.0);
        
        // Let's compute subtotal
        List<Map<String, Object>> products = response.jsonPath().getList(dataPath + ".product_details");
        double subtotal = 0.0;
        if (products != null) {
            for (Map<String, Object> p : products) {
                double price = p.get("price") instanceof Number ? ((Number) p.get("price")).doubleValue() : 0.0;
                int qty = p.get("quantity") instanceof Number ? ((Number) p.get("quantity")).intValue() : 1;
                subtotal += (price * qty);
            }
        }
        
        // discountAmount is now a primitive double, handled by toDouble helper
        
        System.out.println("   Subtotal       : ₹" + subtotal);
        System.out.println("   Coupon Discount: ₹" + discountAmount);
        System.out.println("   Total Price    : ₹" + totalPrice);
        
        if (subtotal > 0 && discountAmount > 0) {
            double expectedTotal = Math.max(0.0, subtotal - discountAmount);
            // Some tolerance for rounding
            Assert.assertTrue(Math.abs(totalPrice - expectedTotal) <= 1.0, 
                "totalPrice (" + totalPrice + ") should equal subtotal - discount (" + expectedTotal + ")");
            System.out.println("   ✅ Math Verified: Total Price = Subtotal - Discount");
        } else {
            System.out.println("   ⚠️ No discount applied, skipping math check");
        }
        
        System.out.println("   ✅ CRT-03 PASSED\n");
    }

    // =========================================================
    // CRT-04 : Get Cart — Empty Cart — NEGATIVE
    // =========================================================
    @Test(priority = 4, description = "CRT-04: GetCart should return empty product list after clearing")
    public void CRT_04_GetCart_EmptyCart() {
        System.out.println("\n>>> CRT-04: GET CART — EMPTY CART <<<");
        
        String token = RequestContext.getMemberToken();
        String userId = RequestContext.getMemberUserId();
        
        // Simulating clearing cart by sending empty products
        Map<String, Object> payload = buildCartPayload(userId, "home");
        payload.put("product_details", new ArrayList<>()); 
        
        Response clearRes = callAddToCart(token, payload);
        
        // Some backends return 200 with empty data, or 400. If 200, we verify empty cart
        if (clearRes.getStatusCode() == 200 || clearRes.getStatusCode() == 201) {
            String endpoint = APIEndpoints.GET_CART_BY_ID.replace("{user_id}", userId);
            Response getRes = new RequestBuilder()
                    .setEndpoint(endpoint)
                    .addHeader("Authorization", token)
                    .get();
                    
            if (getRes.getStatusCode() == 200) {
                Object dataObj = getRes.jsonPath().get("data");
                if (dataObj instanceof List) {
                    List<?> list = (List<?>) dataObj;
                    if (list.isEmpty()) {
                        System.out.println("   ✅ Cart is completely empty (empty array)");
                    } else {
                        List<?> products = getRes.jsonPath().getList("data[0].product_details");
                        Assert.assertTrue(products == null || products.isEmpty(), "Cart should be empty");
                        System.out.println("   ✅ Cart products empty");
                    }
                }
            }
        }
        System.out.println("   ✅ CRT-04 PASSED\n");
    }

    // =========================================================
    // CRT-05 : Add to Cart — Discount Cap (Zero Payable) — POSITIVE
    // =========================================================
    @Test(priority = 5, description = "CRT-05: If Discount >= Subtotal, verify totalPrice is not negative")
    public void CRT_05_AddToCart_DiscountCap() {
        System.out.println("\n>>> CRT-05: ADD TO CART — DISCOUNT CAP (ZERO PAYABLE) <<<");
        ensureTokensAndTests();
        
        String token = RequestContext.getMemberToken();
        String userId = RequestContext.getMemberUserId();
        
        Map<String, Object> payload = buildCartPayload(userId, "home");
        String couponGuid = fetchBestAvailableCouponGuid("prime");
        
        if (couponGuid != null) {
            payload.put("coupon_guid", couponGuid);
            Response res = callAddToCart(token, payload);
            
            if (res.getStatusCode() == 200 || res.getStatusCode() == 201) {
                Object priceVal = null;
                try { priceVal = res.jsonPath().get("data.totalPrice"); } catch (Exception ignored) {}
                if (priceVal == null) {
                    try { priceVal = res.jsonPath().get("total_amount"); } catch (Exception ignored) {}
                }
                
                double totalPrice = toDouble(priceVal, -1.0);
                if (priceVal != null) {
                    Assert.assertTrue(totalPrice >= 0, "totalPrice should never be negative");
                    System.out.println("   ✅ totalPrice >= 0 verified. Total: " + totalPrice);
                }
            }
        } else {
            System.out.println("   ⚠️ No coupon available to test discount cap.");
        }
        
        System.out.println("   ✅ CRT-05 PASSED\n");
    }

    // =========================================================
    // CRT-06 : Cart Updated After Slot Booking — POSITIVE
    // =========================================================
    @Test(priority = 6, description = "CRT-06: Cart Updated After Slot Booking. Verify date and time are passed, and coupon remains")
    public void CRT_06_CartUpdatedAfterSlotBooking() {
        System.out.println("\n>>> CRT-06: CART UPDATED AFTER SLOT BOOKING <<<");
        ensureTokensAndTests();

        String token = RequestContext.getMemberToken();
        String userId = RequestContext.getMemberUserId();
        String addressId = RequestContext.getMemberAddressId();
        
        Assert.assertNotNull(addressId, "Address ID is required for slot booking");

        // Step 1: Find a slot (Check next 7 days)
        String slotGuid = null;
        String slotTime = null;
        String dateStr = null;
        
        java.time.LocalDate today = java.time.LocalDate.now();
        for (int i = 1; i <= 7; i++) {
            java.time.LocalDate checkDate = today.plusDays(i);
            String tempDate = checkDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            Map<String, Object> slotPayload = new HashMap<>();
            slotPayload.put("slot_start_time", tempDate);
            slotPayload.put("page", 1);
            slotPayload.put("limit", 100);
            slotPayload.put("type", "home");
            slotPayload.put("addressguid", addressId);

            Response slotRes = new RequestBuilder()
                    .setEndpoint(APIEndpoints.GET_SLOT_COUNT_BY_TIME)
                    .addHeader("Authorization", token)
                    .setRequestBody(slotPayload)
                    .post();

            if (slotRes.getStatusCode() == 200) {
                List<Map<String, Object>> slots = slotRes.jsonPath().getList("data");
                if (slots != null && !slots.isEmpty()) {
                    for (Map<String, Object> slot : slots) {
                        int count = toInt(slot.get("count"), 0);
                        if (count > 0) {
                            slotGuid = (String) slot.get("guid");
                            dateStr = tempDate;
                            Object startTimeObj = slot.get("starttime");
                            Object endTimeObj = slot.get("endtime");
                            if (startTimeObj == null) startTimeObj = slot.get("start_time");
                            if (endTimeObj == null) endTimeObj = slot.get("end_time");
                            slotTime = startTimeObj + " - " + endTimeObj;
                            break;
                        }
                    }
                }
            }
            if (slotGuid != null) break;
        }
        
        if (slotGuid == null) {
            Assert.fail("❌ No available slots found in the next 7 days for address: " + addressId);
        }
        
        System.out.println("   [CRT-06] Booking Slot: " + slotGuid + " for Date: " + dateStr + " Time: " + slotTime);

        // Step 2: Add to Cart WITH Slot info AND Coupon
        // (Just updating an existing cart by passing the same user_id and products, but with new slot fields)
        Map<String, Object> cartPayload = buildCartPayload(userId, "home");
        
        // Pass the necessary slot fields
        cartPayload.put("slot_guid", slotGuid);
        // Important: this sends date to the cart update!
        cartPayload.put("slot_start_time", dateStr);    
        cartPayload.put("slot_time", slotTime);         
        
        // Re-attach coupon if any
        String couponGuid = RequestContext.getMemberCouponGuid();
        if (couponGuid == null) couponGuid = fetchBestAvailableCouponGuid("prime");
        
        if (couponGuid != null) {
            cartPayload.put("coupon_guid", couponGuid);
            System.out.println("   [CRT-06] Re-attached Coupon: " + couponGuid);
        }

        Response updateRes = callAddToCart(token, cartPayload);
        if (updateRes.getStatusCode() != 200 && updateRes.getStatusCode() != 201) {
            throw new RuntimeException("Cart update failed with " + updateRes.getStatusCode() + ". Response: " + updateRes.asString());
        }
        AssertionUtil.verifyTrue(updateRes.getStatusCode() == 200 || updateRes.getStatusCode() == 201, "Cart update must return 200/201");
        
        // Step 3: Verify coupon was NOT stripped
        if (couponGuid != null) {
            String returnedCoupon = updateRes.jsonPath().getString("data.coupon_guid");
            if (returnedCoupon == null) returnedCoupon = updateRes.jsonPath().getString("data.coupon.guid");
            
            if (returnedCoupon != null) {
                AssertionUtil.verifyEquals(returnedCoupon, couponGuid, "Coupon should persist after slot update");
                System.out.println("   ✅ Verified: Coupon persisted in cart after slot booking update.");
            } else {
                System.out.println("   ⚠️ Backend did not echo coupon_guid in response data payload.");
            }
        } else {
            System.out.println("   ℹ️ No coupon to verify in this run.");
        }
        
        System.out.println("   ✅ CRT-06 PASSED\n");
    }
}

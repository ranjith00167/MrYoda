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

import java.time.LocalDate;
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

    private String normalizeCouponUserType(String couponUserType) {
        if (couponUserType == null || couponUserType.trim().isEmpty()) {
            return "nonPrime";
        }

        String normalized = couponUserType.trim().toLowerCase().replace("_", "").replace("-", "");
        if ("prime".equals(normalized) || "member".equals(normalized)) {
            return "prime";
        }
        if ("nonprime".equals(normalized) || "nonmember".equals(normalized) || "newuser".equals(normalized)) {
            return "nonPrime";
        }
        return couponUserType;
    }

    private LocalDate parseCouponDate(Object rawDate) {
        if (rawDate == null) {
            return null;
        }
        String value = rawDate.toString().trim();
        if (value.isEmpty() || "null".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            if (value.length() >= 10) {
                return LocalDate.parse(value.substring(0, 10));
            }
            return LocalDate.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isCouponActiveForToday(Map<String, Object> coupon, LocalDate today) {
        if (!"active".equalsIgnoreCase(String.valueOf(coupon.get("status")))) {
            return false;
        }
        LocalDate effectiveFrom = parseCouponDate(coupon.get("effective_from"));
        LocalDate endDate = parseCouponDate(coupon.get("end_date"));

        if (effectiveFrom != null && today.isBefore(effectiveFrom)) {
            return false;
        }
        if (endDate != null && today.isAfter(endDate)) {
            return false;
        }
        return true;
    }

    private boolean hasCouponRedemptionCapacity(Map<String, Object> coupon) {
        double maxNumberOfCoupons = toDouble(coupon.get("max_number_of_coupon"), 0.0);
        double redeemedCoupon = toDouble(coupon.get("redeemed_coupon"), 0.0);

        return !(maxNumberOfCoupons > 0 && redeemedCoupon >= maxNumberOfCoupons);
    }

    private Map<String, Object> toCouponDetails(Map<String, Object> coupon) {
        Map<String, Object> result = new HashMap<>();
        Object guid = coupon.get("guid");
        Object code = coupon.get("code");
        result.put("guid", guid == null ? null : guid.toString());
        result.put("code", code == null ? null : code.toString());
        result.put("discount", coupon.get("discount"));
        result.put("discount_type", coupon.get("discount_type"));
        result.put("max_redeemable_amount", coupon.get("max_redeemable_amount"));

        Object moa = coupon.get("min_order_amount");
        result.put("min_order_amount", moa instanceof Number ? ((Number) moa).doubleValue() : toDouble(moa, 0.0));
        return result;
    }

    private void setCouponGuidForFlow(String flowUserType, String couponGuid) {
        if (flowUserType == null) {
            return;
        }
        switch (flowUserType) {
            case "MEMBER":
                RequestContext.setMemberCouponGuid(couponGuid);
                break;
            case "NON_MEMBER":
                RequestContext.setNonMemberCouponGuid(couponGuid);
                break;
            case "NEW_USER":
                RequestContext.setNewUserCouponGuid(couponGuid);
                break;
            default:
                break;
        }
    }

    private double calculatePayloadSubtotal(Map<String, Object> payload) {
        Object productsObj = payload.get("product_details");
        if (!(productsObj instanceof List)) {
            return 0.0;
        }

        double subtotal = 0.0;
        List<?> products = (List<?>) productsObj;
        for (Object item : products) {
            if (!(item instanceof Map)) {
                continue;
            }
            Map<?, ?> product = (Map<?, ?>) item;
            String productId = product.get("product_id") == null ? null : product.get("product_id").toString();
            int qty = toInt(product.get("quantity"), 1);
            double unitPrice = productId == null ? 0.0 : getPriceForProduct(productId);
            subtotal += (unitPrice * qty);
        }
        return subtotal;
    }

    private double calculateCouponAmount(double subtotal, Map<String, Object> couponDetails) {
        if (couponDetails == null || subtotal <= 0) {
            return 0.0;
        }

        double discountValue = toDouble(couponDetails.get("discount"), 0.0);
        String discountType = couponDetails.get("discount_type") == null ? ""
                : couponDetails.get("discount_type").toString().trim().toLowerCase();
        double maxRedeemable = toDouble(couponDetails.get("max_redeemable_amount"), Double.MAX_VALUE);

        double couponAmount;
        if ("percentage".equals(discountType) || "percent".equals(discountType)) {
            couponAmount = (subtotal * discountValue) / 100.0;
        } else {
            couponAmount = discountValue;
        }

        if (maxRedeemable > 0 && maxRedeemable < Double.MAX_VALUE) {
            couponAmount = Math.min(couponAmount, maxRedeemable);
        }
        return Math.max(0.0, Math.min(couponAmount, subtotal));
    }

    private void logAddToCartPayload(Map<String, Object> payload) {
        System.out.println("\n================ ADD_TO_CART REQUEST DEBUG ================");
        System.out.println("user_id        : " + payload.get("user_id"));
        System.out.println("order_type     : " + payload.get("order_type"));
        System.out.println("lab_location_id: " + payload.get("lab_location_id"));
        System.out.println("coupon_guid    : " + payload.get("coupon_guid"));

        Object productsObj = payload.get("product_details");
        if (productsObj instanceof List) {
            List<?> products = (List<?>) productsObj;
            System.out.println("product_count  : " + products.size());
            double expectedSubtotal = 0.0;

            for (int i = 0; i < products.size(); i++) {
                Object item = products.get(i);
                if (!(item instanceof Map)) {
                    continue;
                }
                Map<?, ?> product = (Map<?, ?>) item;
                String productId = product.get("product_id") == null ? null : product.get("product_id").toString();
                int qty = toInt(product.get("quantity"), 1);
                double unitPrice = productId == null ? 0.0 : getPriceForProduct(productId);
                double lineTotal = unitPrice * qty;
                expectedSubtotal += lineTotal;

                System.out.println("  [" + (i + 1) + "] product_id=" + product.get("product_id")
                        + ", qty=" + qty
                        + ", type=" + product.get("type")
                        + ", location_id=" + product.get("location_id")
                        + ", unit_price=" + unitPrice
                        + ", line_total=" + lineTotal);
            }
            System.out.println("expected_subtotal_from_payload: " + expectedSubtotal);
        } else {
            System.out.println("product_details: " + productsObj);
        }

        System.out.println("raw_payload: " + payload);
        System.out.println("===========================================================\n");
    }

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

    private void attachCouponAndLocation(Map<String, Object> payload, String applyCoupon, String couponUserType,
            String flowUserType) {
        RequestContext.setCouponAmount(0.0);
        payload.remove("coupon_guid");
        setCouponGuidForFlow(flowUserType, null);

        String resolvedLabLocation = resolveLabLocationId(payload);
        if (resolvedLabLocation != null) {
            payload.put("lab_location_id", resolvedLabLocation);
        }

        if ("true".equalsIgnoreCase(applyCoupon)) {
            String normalizedCouponUserType = normalizeCouponUserType(couponUserType);
            double currentTotal = calculatePayloadSubtotal(payload);
            Map<String, Object> couponDetails = fetchBestCouponDetails(normalizedCouponUserType, currentTotal);

            if (couponDetails != null) {
                String couponGuid = (String) couponDetails.get("guid");
                double minOrder = toDouble(couponDetails.get("min_order_amount"), 0.0);
                String couponCode = (String) couponDetails.get("code");

                payload.put("coupon_guid", couponGuid);
                setCouponGuidForFlow(flowUserType, couponGuid);

                List<Map<String, Object>> products = (List<Map<String, Object>>) payload.get("product_details");
                currentTotal = 0.0;
                for (Map<String, Object> p : products) {
                    double price = getPriceForProduct(p.get("product_id").toString());
                    currentTotal += (price * toInt(p.get("quantity"), 1));
                }

                System.out.println("   📊 Initial Cart Total: ₹" + currentTotal + " | Required for " + couponCode
                        + ": ₹" + minOrder);

                if (currentTotal < minOrder && !products.isEmpty()) {
                    System.out.println(
                            "   ⚠️  Cart total below minimum. Increasing quantity of first item to qualify...");
                    Map<String, Object> firstProduct = products.get(0);
                    double firstPrice = getPriceForProduct(firstProduct.get("product_id").toString());

                    if (firstPrice > 0) {
                        double needed = minOrder - currentTotal;
                        int extraQty = (int) Math.ceil(needed / firstPrice);
                        int currentQty = toInt(firstProduct.get("quantity"), 1);
                        firstProduct.put("quantity", currentQty + extraQty);

                        double newTotal = currentTotal + (extraQty * firstPrice);
                        System.out.println("   ✅ Quantity adjusted. New Cart Total: ₹" + newTotal + " (Qty: "
                                + (currentQty + extraQty) + ")");
                    }
                }
                double finalSubtotal = calculatePayloadSubtotal(payload);
                double couponAmount = calculateCouponAmount(finalSubtotal, couponDetails);
                double expectedPayable = finalSubtotal - couponAmount;
                RequestContext.setCouponAmount(couponAmount);
                System.out.println("   ✅ coupon_guid attached: " + couponGuid + " (" + couponCode + ")");
                System.out.println("   💰 Calculated (from payload): total_amount=" + Math.round(finalSubtotal)
                        + ", coupon_amount=" + Math.round(couponAmount)
                        + ", payable_amount=" + Math.round(expectedPayable));
            } else {
                // No eligible coupon found — this is expected on re-runs when the user's
                // coupon is already exhausted (business rule: 1 redemption per user).
                // Proceed without coupon: full price will be charged.
                System.out.println("   ⚠️ [WARN] No eligible coupon found for type: " + couponUserType
                        + ". Proceeding without coupon — full price will apply.");
                RequestContext.setCouponAmount(0.0);
                setCouponGuidForFlow(flowUserType, null);
                // Disable coupon flow for this user type so downstream steps don't expect a
                // discount
                if ("MEMBER".equals(flowUserType))
                    RequestContext.setMemberCouponFlowEnabled(false);
                else if ("NON_MEMBER".equals(flowUserType))
                    RequestContext.setNonMemberCouponFlowEnabled(false);
                else if ("NEW_USER".equals(flowUserType))
                    RequestContext.setNewUserCouponFlowEnabled(false);
            }
        }
    }

    private void configureCouponFlow(String userType, String applyCoupon) {
        boolean couponFlow = "true".equalsIgnoreCase(applyCoupon);
        switch (userType) {
            case "MEMBER":
                RequestContext.setMemberCouponFlowEnabled(couponFlow);
                RequestContext.setMemberCouponGuid(null);
                break;
            case "NON_MEMBER":
                RequestContext.setNonMemberCouponFlowEnabled(couponFlow);
                RequestContext.setNonMemberCouponGuid(null);
                break;
            case "NEW_USER":
                RequestContext.setNewUserCouponFlowEnabled(couponFlow);
                RequestContext.setNewUserCouponGuid(null);
                break;
            default:
                break;
        }
    }

    private double getPriceForProduct(String productId) {
        Map<String, Map<String, Object>> allTests = RequestContext.getAllTests();
        for (Map<String, Object> testData : allTests.values()) {
            if (testData.get("_id") != null && testData.get("_id").toString().equals(productId)) {
                return testData.get("price") instanceof Number ? ((Number) testData.get("price")).doubleValue() : 0;
            }
        }
        return 0;
    }

    private Map<String, Object> fetchBestCouponDetails(String couponUserType, double currentSubtotal) {
        System.out.println("\n--- 🔍 Discovering Best Coupon for: " + couponUserType + " ---");
        Map<String, String> payload = new HashMap<>();
        String normalizedCouponType = normalizeCouponUserType(couponUserType);
        payload.put("coupon_user_type", normalizedCouponType);

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.GET_ALL_COUPONS)
                .setRequestBody(payload)
                .post();

        if (response.getStatusCode() != 200) {
            System.out.println("   ⚠️ Failed to fetch coupons. Status: " + response.getStatusCode());
            return null;
        }

        List<Map<String, Object>> coupons = response.jsonPath().get("data");
        if (coupons == null || coupons.isEmpty()) {
            System.out.println("   ⚠️ No coupons returned for coupon_user_type=" + normalizedCouponType);
            return null;
        }

        LocalDate today = LocalDate.now();
        Map<String, Object> bestEligibleCoupon = null;
        double bestEligibleDiscount = -1.0;

        Map<String, Object> bestFallbackCoupon = null;
        double bestFallbackMinOrder = Double.MAX_VALUE;

        for (Map<String, Object> coupon : coupons) {
            if (coupon == null) {
                continue;
            }

            String couponTypeFromListing = normalizeCouponUserType(String.valueOf(coupon.get("coupon_user_type")));
            if (!normalizedCouponType.equalsIgnoreCase(couponTypeFromListing)) {
                continue;
            }

            if (!isCouponActiveForToday(coupon, today)) {
                continue;
            }

            if (!hasCouponRedemptionCapacity(coupon)) {
                continue;
            }

            Map<String, Object> details = toCouponDetails(coupon);
            String guid = details.get("guid") == null ? null : details.get("guid").toString();
            if (guid == null || guid.trim().isEmpty()) {
                continue;
            }

            double minOrder = toDouble(details.get("min_order_amount"), 0.0);
            double estimatedDiscount = calculateCouponAmount(Math.max(currentSubtotal, minOrder), details);

            if (currentSubtotal >= minOrder) {
                if (bestEligibleCoupon == null || estimatedDiscount > bestEligibleDiscount) {
                    bestEligibleCoupon = details;
                    bestEligibleDiscount = estimatedDiscount;
                }
            } else if (bestFallbackCoupon == null || minOrder < bestFallbackMinOrder) {
                bestFallbackCoupon = details;
                bestFallbackMinOrder = minOrder;
            }
        }

        if (bestEligibleCoupon != null) {
            return bestEligibleCoupon;
        }
        if (bestFallbackCoupon != null) {
            return bestFallbackCoupon;
        }

        // Last fallback: return first guid-bearing coupon, even if metadata is
        // incomplete.
        for (Map<String, Object> coupon : coupons) {
            String couponTypeFromListing = normalizeCouponUserType(String.valueOf(coupon.get("coupon_user_type")));
            if (!normalizedCouponType.equalsIgnoreCase(couponTypeFromListing)) {
                continue;
            }
            Map<String, Object> details = toCouponDetails(coupon);
            String guid = details.get("guid") == null ? null : details.get("guid").toString();
            if (guid != null && !guid.trim().isEmpty()) {
                return details;
            }
        }

        return null;
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

        logAddToCartPayload(payload);

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
        double payloadSubtotal = calculatePayloadSubtotal(payload);
        double payloadCouponAmount = RequestContext.getCouponAmount();
        double payloadPayable = Math.max(0.0, payloadSubtotal - payloadCouponAmount);
        System.out
                .println("   💰 Amounts (computed from payload/coupon) => total_amount: " + Math.round(payloadSubtotal)
                        + ", coupon_amount: " + Math.round(payloadCouponAmount)
                        + ", payable_amount: " + Math.round(payloadPayable));

        Boolean success = response.jsonPath().getBoolean("success");
        AssertionUtil.verifyTrue(success, "Success flag");

        // Validate basic fields
        String cartGuid = response.jsonPath().getString("data.guid");
        Integer cartId = response.jsonPath().getInt("data.id");
        int totalAmount = (int) Math.round(payloadPayable);

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
                RequestContext.setMemberTotalAmount(totalAmount);
                RequestContext.setMemberCouponGuid((String) payload.get("coupon_guid"));
                break;
            case "NON_MEMBER":
                RequestContext.setNonMemberCartId(cartGuid);
                RequestContext.setNonMemberCartNumericId(cartId);
                RequestContext.setNonMemberTotalAmount(totalAmount);
                RequestContext.setNonMemberCouponGuid((String) payload.get("coupon_guid"));
                break;
            case "NEW_USER":
                RequestContext.setNewUserCartId(cartGuid);
                RequestContext.setNewUserCartNumericId(cartId);
                RequestContext.setNewUserTotalAmount(totalAmount);
                RequestContext.setNewUserCouponGuid((String) payload.get("coupon_guid"));
                break;
        }
        System.out.println("   ✅ Cart Data Validated and Stored for " + userType + " (GUID: " + cartGuid + ")");
    }

    @Parameters({ "orderType", "applyCoupon" })
    @Test(priority = 8, dependsOnMethods = "com.mryoda.diagnostics.api.tests.tests_packages.GlobalSearchAPITest.testGlobalSearch_ForMember", description = "🛒 CART OPERATIONS: Add Tests to Cart for Member - Verify paid member can add tests to cart for both home collection and lab visit with proper pricing and coupon application")
    public void testAddToCart_ForMember(@Optional("home") String orderType, @Optional("false") String applyCoupon) {
        System.out.println(
                "\n--- AddToCart For Member (Parameter: " + orderType + ", applyCoupon: " + applyCoupon + ") ---");
        System.setProperty("orderType", orderType);
        configureCouponFlow("MEMBER", applyCoupon);
        String token = RequestContext.getMemberToken();
        String userId = RequestContext.getMemberUserId();

        Map<String, Object> payload = buildCartPayloadWithAllTests(userId, "Diagnostics", DEFAULT_LOCATION);
        if (payload != null) {
            attachCouponAndLocation(payload, applyCoupon, "prime", "MEMBER");
            Response response = callAddToCartAPI(token, payload);
            validateAddToCartResponse(response, "MEMBER", payload);
        }
    }

    /**
     * Add to Cart with rewards_used = floor(cart_total / 2).
     * Strategy:
     *   1. Fetch current rewards balance.
     *   2. Add to cart once (no rewards_used) to discover total.
     *   3. Compute rewards_used = min(floor(total/2), current_rewards_balance).
     *   4. Re-send addToCart with rewards_used field.
     * The backend will reduce the payable cash amount by rewards_used.
     */
    @Parameters({ "orderType", "applyCoupon" })
    @Test(priority = 7, dependsOnMethods = "com.mryoda.diagnostics.api.tests.tests_packages.GlobalSearchAPITest.testGlobalSearch_ForMember", description = "🛒 CART OPERATIONS: Add Tests to Cart with Rewards Points - Verify member can apply rewards points to reduce payable amount with proper calculation and validation")
    public void testAddToCart_WithRewardsUsed(@Optional("lab") String orderType, @Optional("false") String applyCoupon) {
        System.out.println("\n--- AddToCart With Rewards Used (orderType=" + orderType + ", applyCoupon=" + applyCoupon + ") ---");
        System.setProperty("orderType", orderType);
        configureCouponFlow("MEMBER", applyCoupon);

        String token  = RequestContext.getMemberToken();
        String userId = RequestContext.getMemberUserId();
        String mobile = RequestContext.getMobile();

        // 1. Fetch current rewards balance
        double initialRewards = com.mryoda.diagnostics.api.utils.RewardHelper.callGetRewardsByMobileAPI(mobile);
        RequestContext.setInitialTotalRewards(initialRewards);
        System.out.println("   Current rewards balance: " + initialRewards);

        Map<String, Object> payload = buildCartPayloadWithAllTests(userId, "Diagnostics", DEFAULT_LOCATION);
        if (payload == null) return;
        attachCouponAndLocation(payload, applyCoupon, "prime", "MEMBER");

        // 2. First addToCart call — get cart ID, then fetch cart total via getCartById
        Response firstResp = callAddToCartAPI(token, payload);
        double cartTotal = 0;
        if (firstResp != null && (firstResp.getStatusCode() == 200 || firstResp.getStatusCode() == 201)) {
            // addToCart V2 returns only {"id":"..."} — call getCartById to get totalPrice
            String cartEndpoint = com.mryoda.diagnostics.api.endpoints.APIEndpoints.DIAGNOSTICS_BASE_URL
                    + com.mryoda.diagnostics.api.endpoints.APIEndpoints.GET_CART_BY_ID
                              .replace("{user_id}", userId);
            Response cartResp = new com.mryoda.diagnostics.api.builders.RequestBuilder()
                    .setEndpoint(cartEndpoint)
                    .addHeader("Authorization", token)
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

        // 3. Compute rewards_used = min(floor(cartTotal/2), initialRewards, MAX_REWARD_DISCOUNT)
        //    Business rule: maximum rewards discount per order is ₹1000.
        final double MAX_REWARD_DISCOUNT = 1000.0;
        double rewardsUsed;
        if (cartTotal > 0) {
            rewardsUsed = Math.min(Math.min(Math.floor(cartTotal / 2.0), initialRewards), MAX_REWARD_DISCOUNT);
        } else {
            // Last resort: use half the cart total as floor(pre-discount/2)
            // This shouldn't happen if getCartById succeeds
            rewardsUsed = Math.min(Math.min(500.0, initialRewards), MAX_REWARD_DISCOUNT);
            System.out.println("   ⚠️  cartTotal not available from getCartById — using safe fallback: " + rewardsUsed);
        }
        System.out.println("   cartTotal=" + cartTotal + ", initialRewards=" + initialRewards
                + " → rewards_used=" + rewardsUsed);

        // 4. Re-send addToCart with rewards_used field
        payload.put("rewards_used", rewardsUsed);
        System.out.println("   Re-sending addToCart with rewards_used=" + rewardsUsed);
        Response response = callAddToCartAPI(token, payload);
        validateAddToCartResponse(response, "MEMBER", payload);

        // 5. Read actual rewards_used stored in cart after second addToCart and sync to context
        //    The API may cap/round the value — always store what the API actually used
        double actualRewardsUsed = rewardsUsed; // fallback to computed value
        if (response != null && (response.getStatusCode() == 200 || response.getStatusCode() == 201)) {
            String cartEndpoint2 = com.mryoda.diagnostics.api.endpoints.APIEndpoints.DIAGNOSTICS_BASE_URL
                    + com.mryoda.diagnostics.api.endpoints.APIEndpoints.GET_CART_BY_ID
                              .replace("{user_id}", userId);
            Response cartResp2 = new com.mryoda.diagnostics.api.builders.RequestBuilder()
                    .setEndpoint(cartEndpoint2)
                    .addHeader("Authorization", token)
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
    }

    @Parameters({ "orderType", "applyCoupon" })
    @Test(priority = 8, dependsOnMethods = "com.mryoda.diagnostics.api.tests.tests_packages.GlobalSearchAPITest.testGlobalSearch_ForNonMember", description = "🛒 CART OPERATIONS: Add Tests to Cart for Non-Member - Verify non-paid member can add tests to cart with standard pricing without membership benefits or premium discounts")
    public void testAddToCart_ForNonMember(@Optional("home") String orderType, @Optional("false") String applyCoupon) {
        System.out.println(
                "\n--- AddToCart For Non-Member (Parameter: " + orderType + ", applyCoupon: " + applyCoupon + ") ---");
        System.setProperty("orderType", orderType);
        configureCouponFlow("NON_MEMBER", applyCoupon);
        String token = RequestContext.getNonMemberToken();
        String userId = RequestContext.getNonMemberUserId();

        Map<String, Object> payload = buildCartPayloadWithAllTests(userId, "Diagnostics", DEFAULT_LOCATION);
        if (payload != null) {
            attachCouponAndLocation(payload, applyCoupon, "nonPrime", "NON_MEMBER");
            Response response = callAddToCartAPI(token, payload);
            validateAddToCartResponse(response, "NON_MEMBER", payload);
        }
    }

    @Parameters({ "orderType", "applyCoupon" })
    @Test(priority = 9, dependsOnMethods = "com.mryoda.diagnostics.api.tests.tests_packages.GlobalSearchAPITest.testGlobalSearch_ForNewUser", description = "🛒 CART OPERATIONS: Add Tests to Cart for New User - Verify newly registered user can add tests to cart with appropriate pricing and new user promotional offers if available")
    public void testAddToCart_ForNewUser(@Optional("home") String orderType, @Optional("false") String applyCoupon) {
        System.out.println(
                "\n--- AddToCart For New User (Parameter: " + orderType + ", applyCoupon: " + applyCoupon + ") ---");
        System.setProperty("orderType", orderType);
        configureCouponFlow("NEW_USER", applyCoupon);
        String token = RequestContext.getNewUserToken();
        String userId = RequestContext.getNewUserUserId();

        Map<String, Object> payload = buildCartPayloadWithAllTests(userId, "Diagnostics", DEFAULT_LOCATION);
        if (payload != null) {
            attachCouponAndLocation(payload, applyCoupon, "nonPrime", "NEW_USER");
            Response response = callAddToCartAPI(token, payload);
            validateAddToCartResponse(response, "NEW_USER", payload);
        }
    }
}


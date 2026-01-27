package com.mryoda.diagnostics.api.tests;

import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.AssertionUtil;
import com.mryoda.diagnostics.api.utils.RequestContext;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlotAndCartUpdateAPITest extends BaseTest {

    // -------------------------------
    // HELPER: Find Available Slot
    // -------------------------------
    private void findAndStoreSlot(String userType, String token, String addressGuid) {
        System.out.println("\n📅 SEARCHING FOR AVAILABLE SLOTS FOR " + userType);

        java.time.LocalDate today = java.time.LocalDate.now();
        int maxDaysToCheck = 7;
        String selectedSlotGuid = null;
        String selectedSlotTime = null;

        for (int i = 0; i < maxDaysToCheck; i++) {
            String checkDate = today.plusDays(i).toString();

            Map<String, Object> payload = new HashMap<>();
            payload.put("slot_start_time", checkDate);
            payload.put("page", 1);
            payload.put("limit", 100);
            payload.put("type", "home");
            payload.put("addressguid", addressGuid);

            Response response = new RequestBuilder()
                    .setEndpoint(APIEndpoints.GET_SLOT_COUNT_BY_TIME)
                    .addHeader("Authorization", token)
                    .setRequestBody(payload)
                    .post();

            if (response.getStatusCode() == 200) {
                List<Map<String, Object>> slots = response.jsonPath().getList("data");
                if (slots != null) {
                    for (Map<String, Object> slot : slots) {
                        Object countObj = slot.get("count");
                        int count = 0;
                        if (countObj instanceof Number)
                            count = ((Number) countObj).intValue();
                        else if (countObj instanceof String)
                            count = Integer.parseInt((String) countObj);

                        if (count > 0) {
                            selectedSlotGuid = (String) slot.get("guid");
                            String startTime = (String) slot.get("starttime");
                            String endTime = (String) slot.get("endtime");
                            selectedSlotTime = startTime + " - " + endTime;

                            System.out.println("   ✅ Found Slot: " + checkDate + " " + selectedSlotTime);
                            break;
                        }
                    }
                }
            }
            if (selectedSlotGuid != null)
                break;
        }

        if (selectedSlotGuid == null) {
            Assert.fail("❌ No available slots found for " + userType);
        }

        switch (userType) {
            case "MEMBER":
                RequestContext.setMemberSlotId(selectedSlotGuid);
                RequestContext.setMemberSlotTime(selectedSlotTime);
                break;
            case "NON_MEMBER":
                RequestContext.setNonMemberSlotId(selectedSlotGuid);
                RequestContext.setNonMemberSlotTime(selectedSlotTime);
                break;
            case "NEW_USER":
                RequestContext.setNewUserSlotId(selectedSlotGuid);
                RequestContext.setNewUserSlotTime(selectedSlotTime);
                break;
        }
    }

    // -------------------------------
    // HELPER: Update Cart With Slot
    // -------------------------------
    private void updateCartWithSlot(String userType, String token, String userId, String slotGuid) {
        System.out.println("\n📦 UPDATING CART WITH SLOT FOR " + userType);

        // 1. Get tests from RequestContext
        Map<String, Map<String, Object>> allTests = RequestContext.getAllTests();
        List<Map<String, Object>> productDetails = new ArrayList<>();

        String brandId = RequestContext.getSelectedBrandId();
        if (brandId == null)
            brandId = RequestContext.getBrandId("Diagnostics");

        // Get Lab Location ID (Madhapur)
        String labLocationId = RequestContext.getLocationId(DEFAULT_LOCATION);

        for (Map.Entry<String, Map<String, Object>> entry : allTests.entrySet()) {
            Map<String, Object> testData = entry.getValue();

            // Check home collection
            Object homeCollectionObj = testData.get("home_collection");
            boolean isHome = false;
            if (homeCollectionObj != null) {
                String s = homeCollectionObj.toString();
                if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("AVAILABLE") || s.equals("1"))
                    isHome = true;
            }

            if (isHome) {
                Map<String, Object> product = new HashMap<>();
                product.put("product_id", testData.get("_id"));
                product.put("quantity", 1);
                product.put("type", "home");
                product.put("brand_id", brandId);
                product.put("location_id", labLocationId);

                List<String> family = new ArrayList<>();
                family.add(userId);
                product.put("family_member_id", family);

                productDetails.add(product);
            }
        }

        if (productDetails.isEmpty()) {
            System.out.println("⚠️  No home collection tests to add to cart!");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", userId);
        payload.put("product_details", productDetails);
        payload.put("slot_guid", slotGuid);
        payload.put("lab_location_id", labLocationId);
        payload.put("order_type", "home");
        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.ADD_TO_CART)
                .addHeader("Authorization", token)
                .setRequestBody(payload)
                .post();

        AssertionUtil.verifyEquals(response.getStatusCode(), 200, "HTTP Status 200");
        AssertionUtil.verifyTrue(response.jsonPath().getBoolean("success"), "Success flag");

        String cartGuid = response.jsonPath().getString("data.guid");
        System.out.println("   ✅ Cart Updated with Slot. Cart GUID: " + cartGuid);

        // Update Cart ID in RequestContext just in case it changed (it shouldn't but
        // good practice)
        switch (userType) {
            case "MEMBER":
                RequestContext.setMemberCartId(cartGuid);
                break;
            case "NON_MEMBER":
                RequestContext.setNonMemberCartId(cartGuid);
                break;
            case "NEW_USER":
                RequestContext.setNewUserCartId(cartGuid);
                break;
        }
    }

    @Test(priority = 18, dependsOnMethods = "com.mryoda.diagnostics.api.tests.AddressAPITest.testAddAddress_ForNonMember")
    public void testSlotAndCartUpdate_ForNonMember() {
        findAndStoreSlot("NON_MEMBER", RequestContext.getNonMemberToken(), RequestContext.getNonMemberAddressId());
        updateCartWithSlot("NON_MEMBER", RequestContext.getNonMemberToken(), RequestContext.getNonMemberUserId(),
                RequestContext.getNonMemberSlotId());
    }

    @Test(priority = 18, dependsOnMethods = "com.mryoda.diagnostics.api.tests.AddressAPITest.testAddAddress_ForMember")
    public void testSlotAndCartUpdate_ForMember() {
        findAndStoreSlot("MEMBER", RequestContext.getMemberToken(), RequestContext.getMemberAddressId());
        updateCartWithSlot("MEMBER", RequestContext.getMemberToken(), RequestContext.getMemberUserId(),
                RequestContext.getMemberSlotId());
    }

    @Test(priority = 18, dependsOnMethods = "com.mryoda.diagnostics.api.tests.AddressAPITest.testAddAddress_ForNewUser")
    public void testSlotAndCartUpdate_ForNewUser() {
        findAndStoreSlot("NEW_USER", RequestContext.getNewUserToken(), RequestContext.getNewUserAddressId());
        updateCartWithSlot("NEW_USER", RequestContext.getNewUserToken(), RequestContext.getNewUserUserId(),
                RequestContext.getNewUserSlotId());
    }
}

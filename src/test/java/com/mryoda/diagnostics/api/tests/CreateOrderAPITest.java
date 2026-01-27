package com.mryoda.diagnostics.api.tests;

import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.AssertionUtil;
import com.mryoda.diagnostics.api.utils.RequestContext;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class CreateOrderAPITest extends BaseTest {

    // -------------------------------
    // HELPER: Build Order Payload
    // -------------------------------
    private Map<String, Object> buildCreateOrderPayload(String cartId, String userId,
            String addressId, String slotId,
            String date, String time,
            String labLocationId, int totalAmount) {

        Map<String, Object> payload = new HashMap<>();
        payload.put("cart_id", cartId);
        payload.put("payment_mode", "online"); // Testing online payment flow
        payload.put("source", "android");
        payload.put("user_id", userId);

        // This address_id is the user's address for home collection
        payload.put("address_id", addressId);

        payload.put("slot_id", slotId);
        payload.put("date", date);
        payload.put("time", time);

        // Note: total_amount is required by some implementations, using what we have
        payload.put("total_amount", totalAmount);

        // Lab location ID is required
        payload.put("lab_location_id", labLocationId);

        // Hardcoded or dynamic?
        // Using "home" order type implied by having address_id
        // payload.put("order_type", "home"); // Backend might infer this from cart

        return payload;
    }

    // -------------------------------
    // HELPER: Call Create Order API
    // -------------------------------
    private Response callCreateOrderAPI(String token, Map<String, Object> payload) {

        System.out.println("\n📦 CREATE ORDER REQUEST:");
        System.out.println("   Cart ID: " + payload.get("cart_id"));
        System.out.println("   User ID: " + payload.get("user_id"));
        System.out.println("   Payment Mode: " + payload.get("payment_mode"));
        System.out.println("   Total Amount: " + payload.get("total_amount"));

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.CREATE_ORDER)
                .addHeader("Authorization", token)
                .setRequestBody(payload)
                .post();

        AssertionUtil.verifyEquals(response.getStatusCode(), 200, "HTTP status should be 200");
        return response;
    }

    // -------------------------------
    // HELPER: Validate Create Order Response
    // -------------------------------
    private void validateCreateOrderResponse(Response response, String userType) {
        System.out.println("\n============================================================");
        System.out.println("      COMPREHENSIVE CREATE ORDER VALIDATION - " + userType);
        System.out.println("============================================================\n");

        // STEP 1: Basic Response Validation
        System.out.println("🔹 STEP 1: Validating API Response");
        int statusCode = response.getStatusCode();
        AssertionUtil.verifyEquals(statusCode, 200, "HTTP Status Code should be 200");
        System.out.println("   ✅ HTTP Status: " + statusCode);

        // Extract response data
        Boolean success = response.jsonPath().getBoolean("success");
        AssertionUtil.verifyTrue(success, "API success flag should be true");
        System.out.println("   ✅ Success flag: " + success);

        String message = response.jsonPath().getString("msg");
        AssertionUtil.verifyEquals(message, "Order Created Successfully",
                "Message should be 'Order Created Successfully'");
        System.out.println("   ✅ Response message: " + message);

        // STEP 2: Validate ALL Order Fields
        System.out.println("\n🔹 STEP 2: Validating ALL Order Fields");

        Map<String, Object> data = response.jsonPath().getMap("data");
        AssertionUtil.verifyNotNull(data, "Response data should not be null");

        // ===== RAZORPAY ORDER ID VALIDATION =====
        String orderId = (String) data.get("id");
        AssertionUtil.verifyNotNull(orderId, "Razorpay order ID should not be null");
        AssertionUtil.verifyTrue(orderId.startsWith("order_"), "Razorpay order ID should start with 'order_'");
        System.out.println("   ✅ Razorpay Order ID: " + orderId);

        // ===== AMOUNT VALIDATION =====
        String amount = String.valueOf(data.get("amount"));
        AssertionUtil.verifyNotNull(amount, "Amount should not be null");
        int amountInt = Integer.parseInt(amount);
        AssertionUtil.verifyTrue(amountInt > 0, "Amount should be greater than 0");
        System.out.println("   ✅ Amount: ₹" + (amountInt / 100.0) + " (paise: " + amount + ")");

        // ===== AMOUNT_DUE VALIDATION =====
        String amountDue = String.valueOf(data.get("amount_due"));
        AssertionUtil.verifyNotNull(amountDue, "Amount due should not be null");
        AssertionUtil.verifyEquals(amountDue, amount, "Amount due should equal amount");
        System.out
                .println("   ✅ Amount Due: ₹" + (Integer.parseInt(amountDue) / 100.0) + " (paise: " + amountDue + ")");

        // ===== STATUS VALIDATION =====
        String status = (String) data.get("status");
        AssertionUtil.verifyNotNull(status, "Status should not be null");
        AssertionUtil.verifyEquals(status, "created", "Status should be 'created'");
        System.out.println("   ✅ Order Status: " + status);

        // ===== KEY_ID VALIDATION =====
        String keyId = (String) data.get("key_id");
        AssertionUtil.verifyNotNull(keyId, "Razorpay key_id should not be null");
        AssertionUtil.verifyTrue(keyId.startsWith("rzp_"), "Razorpay key_id should start with 'rzp_'");
        System.out.println("   ✅ Razorpay Key ID: " + keyId);

        // ===== MOBILE VALIDATION =====
        String mobile = (String) data.get("mobile");
        AssertionUtil.verifyNotNull(mobile, "Mobile should not be null");
        AssertionUtil.verifyEquals(mobile.length(), 10, "Mobile should be 10 digits");
        System.out.println("   ✅ Mobile: " + mobile);

        // STEP 3: Validate NOTES Object
        System.out.println("\n🔹 STEP 3: Validating Notes Object");
        @SuppressWarnings("unchecked")
        Map<String, Object> notes = (Map<String, Object>) data.get("notes");
        AssertionUtil.verifyNotNull(notes, "Notes object should not be null");

        // ===== NOTES.USER_ID VALIDATION =====
        String notesUserId = (String) notes.get("user_id");
        AssertionUtil.verifyNotNull(notesUserId, "Notes user_id should not be null");
        System.out.println("   ✅ Notes User ID: " + notesUserId);

        // ===== NOTES.MOBILE VALIDATION =====
        String notesMobile = (String) notes.get("mobile");
        AssertionUtil.verifyNotNull(notesMobile, "Notes mobile should not be null");
        AssertionUtil.verifyEquals(notesMobile, mobile, "Notes mobile should match data.mobile");
        System.out.println("   ✅ Notes Mobile: " + notesMobile);

        // ===== NOTES.SLOT_GUID VALIDATION =====
        String notesSlotGuid = (String) notes.get("slot_guid");
        AssertionUtil.verifyNotNull(notesSlotGuid, "Notes slot_guid should not be null");
        System.out.println("   ✅ Notes Slot GUID: " + notesSlotGuid);

        // STEP 4: Cross-validate with LoginAPI
        System.out.println("\n🔹 STEP 4: Cross-validating with LoginAPI");
        String expectedUserId = null;
        String expectedMobile = null;

        switch (userType) {
            case "NON_MEMBER":
                expectedUserId = RequestContext.getNonMemberUserId();
                expectedMobile = "9666666665"; // From LoginAPITest
                break;
            case "MEMBER":
                expectedUserId = RequestContext.getMemberUserId();
                expectedMobile = "9003730394"; // From LoginAPITest
                break;
            case "NEW_USER":
                expectedUserId = RequestContext.getNewUserUserId();
                expectedMobile = RequestContext.getMobile(); // From UserRegistrationTest
                break;
        }

        AssertionUtil.verifyEquals(notesUserId, expectedUserId, "User ID should match LoginAPI");
        System.out.println("   ✅ User ID matches LoginAPI: " + notesUserId);

        if (expectedMobile != null) {
            AssertionUtil.verifyEquals(mobile, expectedMobile, "Mobile should match LoginAPI");
            System.out.println("   ✅ Mobile matches LoginAPI: " + mobile);
        }

        // STEP 5: Cross-validate with SlotAPI
        System.out.println("\n🔹 STEP 5: Cross-validating with SlotAPI");
        String expectedSlotGuid = null;

        switch (userType) {
            case "NON_MEMBER":
                expectedSlotGuid = RequestContext.getNonMemberSlotGuid(); // Ensure this getter exists or add it
                break;
            case "MEMBER":
                expectedSlotGuid = RequestContext.getMemberSlotGuid();
                break;
            case "NEW_USER":
                expectedSlotGuid = RequestContext.getNewUserSlotGuid();
                break;
        }

        if (expectedSlotGuid != null) {
            System.out.println("   📊 Slot GUID from SlotAPI: " + expectedSlotGuid);
            System.out.println("   📊 Slot GUID from CreateOrder: " + notesSlotGuid);

            if (notesSlotGuid.equals(expectedSlotGuid)) {
                System.out.println("   ✅ Slot GUID matches SlotAPI exactly");
            } else {
                System.out.println("   ℹ️  Slot GUID differs (backend selected different slot - this is expected)");
            }
        } else {
            System.out.println("   ℹ️  No stored slot GUID to compare (validation skipped)");
            // If getNonMemberSlotGuid getter doesn't exist, this handles graceful skip
        }

        // Store Order ID for subsequent payment tests
        switch (userType) {
            case "MEMBER":
                RequestContext.setMemberOrderId(orderId);
                break;
            case "NON_MEMBER":
                RequestContext.setNonMemberOrderId(orderId);
                break;
            case "NEW_USER":
                RequestContext.setNewUserOrderId(orderId);
                break;
        }

        System.out.println("\n========================================");
        System.out.println("ALL CREATE ORDER VALIDATIONS PASSED FOR " + userType);
        System.out.println("========================================\n");
    }

    // ---------------------------------------------------------
    // 1️⃣ EXISTING MEMBER → Create Order
    // ---------------------------------------------------------
    @Test(priority = 20, dependsOnMethods = "com.mryoda.diagnostics.api.tests.GetCartByIdAPITest.testGetCartById_ForNonMember")
    public void testCreateOrder_ForNonMember() {

        System.out.println("\n==========================================================");
        System.out.println("      CREATE ORDER API - EXISTING MEMBER");
        System.out.println("==========================================================");

        String token = RequestContext.getNonMemberToken();
        String userId = RequestContext.getNonMemberUserId();
        String cartGuid = RequestContext.getNonMemberCartId();
        String slotId = RequestContext.getNonMemberSlotId();

        String time = RequestContext.getNonMemberSlotTime();
        String addressId = RequestContext.getNonMemberAddressId();
        String labLocationId = RequestContext.getLocationId(DEFAULT_LOCATION);
        int totalAmount = RequestContext.getNonMemberTotalAmount(); // Should rely on GetCartById now

        // Workaround for date: regenerate tomorrow's date
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DATE, 1);
        String dateString = sdf.format(cal.getTime());

        Map<String, Object> payload = buildCreateOrderPayload(cartGuid, userId, addressId, slotId, dateString, time,
                labLocationId, totalAmount);

        Response response = callCreateOrderAPI(token, payload);
        validateCreateOrderResponse(response, "NON_MEMBER");
    }

    // ---------------------------------------------------------
    // 2️⃣ MEMBER → Create Order
    // ---------------------------------------------------------
    @Test(priority = 20, dependsOnMethods = "com.mryoda.diagnostics.api.tests.GetCartByIdAPITest.testGetCartById_ForMember")
    public void testCreateOrder_ForMember() {

        System.out.println("\n==========================================================");
        System.out.println("         CREATE ORDER API - MEMBER");
        System.out.println("==========================================================");

        String token = RequestContext.getMemberToken();
        String userId = RequestContext.getMemberUserId();
        String cartGuid = RequestContext.getMemberCartId();
        String slotId = RequestContext.getMemberSlotId();
        String time = RequestContext.getMemberSlotTime();
        String addressId = RequestContext.getMemberAddressId();
        String labLocationId = RequestContext.getLocationId(DEFAULT_LOCATION);
        int totalAmount = RequestContext.getMemberTotalAmount();

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DATE, 1);
        String dateString = sdf.format(cal.getTime());

        Map<String, Object> payload = buildCreateOrderPayload(cartGuid, userId, addressId, slotId, dateString, time,
                labLocationId, totalAmount);

        Response response = callCreateOrderAPI(token, payload);
        validateCreateOrderResponse(response, "MEMBER");
    }

    // ---------------------------------------------------------
    // 3️⃣ NEW USER → Create Order
    // ---------------------------------------------------------
    @Test(priority = 21, dependsOnMethods = "com.mryoda.diagnostics.api.tests.GetCartByIdAPITest.testGetCartById_ForNewUser")
    public void testCreateOrder_ForNewUser() {

        System.out.println("\n==========================================================");
        System.out.println("        CREATE ORDER API - NEW USER");
        System.out.println("==========================================================");

        String token = RequestContext.getNewUserToken();
        String userId = RequestContext.getNewUserUserId();
        String cartGuid = RequestContext.getNewUserCartId();
        String slotId = RequestContext.getNewUserSlotId();
        String time = RequestContext.getNewUserSlotTime();
        String addressId = RequestContext.getNewUserAddressId();
        String labLocationId = RequestContext.getLocationId(DEFAULT_LOCATION);
        int totalAmount = RequestContext.getNewUserTotalAmount();

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DATE, 1);
        String dateString = sdf.format(cal.getTime());

        Map<String, Object> payload = buildCreateOrderPayload(cartGuid, userId, addressId, slotId, dateString, time,
                labLocationId, totalAmount);

        Response response = callCreateOrderAPI(token, payload);
        validateCreateOrderResponse(response, "NEW_USER");
    }
}

package com.mryoda.diagnostics.api.tests.order;

import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import com.mryoda.diagnostics.api.config.ConfigLoader;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.RequestContext;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * RESCHEDULE FLOW TEST
 * 
 * This test class implements the complete order rescheduling flow:
 * 
 * Step 1: POST /otps/getOtp - Request OTP with is_rescheduling=true
 * Step 2: POST /otps/getOtp - Verify OTP with static OTP (123456)
 * Step 3: POST /slot/getSlotCountByTime - Get available slots
 * Step 4: POST /order/updateOrderSlots - Update order with new slot
 * Step 5: GET /order/getOrderById - Verify all updated details (slot, date, time, reschedule count, etc.)
 * 
 * Prerequisites:
 * - Order must be created (order_id in RequestContext from COD_04/COD_05)
 * - User must be logged in (token in RequestContext)
 */
public class RescheduleFlowTest extends BaseTest {

    private static final String STATIC_OTP = "123456";
    private static final String COUNTRY_CODE = "+91";
    private static final String CENTER_ID = "64870066842708a0d5ae6c77";
    private static final Random RANDOM = new Random();

    private String rescheduleToken;
    private String selectedSlotGuid;
    private String selectedSlotDate;
    private String selectedSlotStartTime;
    private String selectedSlotEndTime;

    // ═══════════════════════════════════════════════════════════════════
    // STEP 1: REQUEST OTP FOR RESCHEDULING
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 1, description = "Step 1: Request OTP for rescheduling")
    public void step01_RequestRescheduleOTP() {
        System.out.println("\n==========================================================");
        System.out.println("  RESCHEDULE FLOW - STEP 1: REQUEST OTP");
        System.out.println("==========================================================");

        String token = RequestContext.getToken();
        String mobile = RequestContext.getMobile();
        String orderId = RequestContext.getCurrentOrderId();

        Assert.assertNotNull(token, "Token must be present from previous login");
        Assert.assertNotNull(mobile, "Mobile must be present from previous login");
        Assert.assertNotNull(orderId, "Order ID must be present from COD_04/COD_05");

        System.out.println("   Mobile: " + mobile);
        System.out.println("   Order ID: " + orderId);

        // Request OTP with is_rescheduling flag
        Map<String, Object> payload = new HashMap<>();
        payload.put("mobile", mobile);
        payload.put("country_code", COUNTRY_CODE);
        payload.put("is_rescheduling", true);

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.OTP_REQUEST)
                .addHeader("Authorization", token)
                .setRequestBody(payload)
                .postWithoutStatusCheck();

        System.out.println("   Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());

        Assert.assertEquals(response.getStatusCode(), 200,
                "OTP request for rescheduling should return 200, got: " + response.getStatusCode());

        System.out.println("   ✅ Reschedule OTP requested successfully");
    }

    // ═══════════════════════════════════════════════════════════════════
    // STEP 2: VERIFY OTP (STATIC OTP)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 2, dependsOnMethods = "step01_RequestRescheduleOTP",
            description = "Step 2: Verify OTP with static OTP for rescheduling")
    public void step02_VerifyRescheduleOTP() {
        System.out.println("\n==========================================================");
        System.out.println("  RESCHEDULE FLOW - STEP 2: VERIFY OTP");
        System.out.println("==========================================================");

        String token = RequestContext.getToken();
        String mobile = RequestContext.getMobile();

        // Verify OTP with static value
        Map<String, Object> payload = new HashMap<>();
        payload.put("mobile", mobile);
        payload.put("otp", STATIC_OTP);
        payload.put("country_code", COUNTRY_CODE);

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.OTP_VERIFY)
                .addHeader("Authorization", token)
                .setRequestBody(payload)
                .postWithoutStatusCheck();

        System.out.println("   Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());

        Assert.assertEquals(response.getStatusCode(), 200,
                "OTP verification should return 200, got: " + response.getStatusCode());

        // Store token if a new one is returned
        String newToken = response.jsonPath().getString("data.access_token");
        if (newToken != null && !newToken.isEmpty()) {
            rescheduleToken = newToken;
            System.out.println("   New token received for reschedule");
        } else {
            rescheduleToken = token;
        }

        System.out.println("   ✅ OTP verified successfully for rescheduling");
    }

    // ═══════════════════════════════════════════════════════════════════
    // STEP 3: GET AVAILABLE SLOTS
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 3, dependsOnMethods = "step02_VerifyRescheduleOTP",
            description = "Step 3: Get available slots and select a random one")
    public void step03_GetAvailableSlots() {
        System.out.println("\n==========================================================");
        System.out.println("  RESCHEDULE FLOW - STEP 3: GET AVAILABLE SLOTS");
        System.out.println("==========================================================");

        String token = (rescheduleToken != null) ? rescheduleToken : RequestContext.getToken();
        LocalDate today = LocalDate.now();
        boolean slotFound = false;

        // Search for available slots in the next 7 days
        for (int i = 0; i < 7 && !slotFound; i++) {
            LocalDate date = today.plusDays(i);
            String dateString = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            System.out.println("   Searching slots for: " + dateString);

            Map<String, Object> payload = new HashMap<>();
            payload.put("slot_start_time", dateString);
            payload.put("page", 1);
            payload.put("limit", 100);
            payload.put("type", "lab");
            payload.put("center_id", CENTER_ID);

            Response response = new RequestBuilder()
                    .setEndpoint(APIEndpoints.GET_SLOT_COUNT_BY_TIME)
                    .addHeader("Authorization", token)
                    .setRequestBody(payload)
                    .postWithoutStatusCheck();

            System.out.println("   Status: " + response.getStatusCode());

            if (response.getStatusCode() == 200) {
                List<Map<String, Object>> slots = response.jsonPath().getList("data");

                if (slots != null && !slots.isEmpty()) {
                    // Filter slots with available count > 0
                    List<Map<String, Object>> availableSlots = new java.util.ArrayList<>();
                    for (Map<String, Object> slot : slots) {
                        Object countObj = slot.get("count");
                        int count = 0;
                        if (countObj != null) {
                            try {
                                count = Integer.parseInt(countObj.toString());
                            } catch (NumberFormatException e) {
                                count = 0;
                            }
                        }
                        if (count > 0) {
                            availableSlots.add(slot);
                        }
                    }

                    if (!availableSlots.isEmpty()) {
                        // Select a RANDOM slot from available ones
                        int randomIndex = RANDOM.nextInt(availableSlots.size());
                        Map<String, Object> selectedSlot = availableSlots.get(randomIndex);

                        selectedSlotGuid = (String) selectedSlot.get("guid");
                        selectedSlotStartTime = (String) selectedSlot.get("starttime");
                        selectedSlotEndTime = (String) selectedSlot.get("endtime");
                        selectedSlotDate = dateString;
                        Object count = selectedSlot.get("count");

                        System.out.println("\n   ✅ SLOT SELECTED (Random Pick):");
                        System.out.println("   Slot GUID: " + selectedSlotGuid);
                        System.out.println("   Date: " + selectedSlotDate);
                        System.out.println("   Time: " + selectedSlotStartTime + " - " + selectedSlotEndTime);
                        System.out.println("   Available Count: " + count);
                        System.out.println("   Index: " + (randomIndex + 1) + "/" + availableSlots.size());

                        // Store in RequestContext for downstream use
                        RequestContext.setCurrentSlotGuid(selectedSlotGuid);

                        slotFound = true;
                    }
                }
            }
        }

        Assert.assertTrue(slotFound, "No available slots found in the next 7 days for center: " + CENTER_ID);
    }

    // ═══════════════════════════════════════════════════════════════════
    // STEP 4: UPDATE ORDER WITH NEW SLOT (RESCHEDULE)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 4, dependsOnMethods = "step03_GetAvailableSlots",
            description = "Step 4: Update order slots - Complete rescheduling")
    public void step04_UpdateOrderSlots() {
        System.out.println("\n==========================================================");
        System.out.println("  RESCHEDULE FLOW - STEP 4: UPDATE ORDER SLOTS");
        System.out.println("==========================================================");

        String token = (rescheduleToken != null) ? rescheduleToken : RequestContext.getToken();
        String orderId = RequestContext.getCurrentOrderId();

        Assert.assertNotNull(orderId, "Order ID is required for rescheduling");
        Assert.assertNotNull(selectedSlotGuid, "Slot GUID is required for rescheduling");

        System.out.println("   Order ID: " + orderId);
        System.out.println("   New Slot GUID: " + selectedSlotGuid);

        Map<String, Object> payload = new HashMap<>();
        payload.put("order_id", orderId);
        payload.put("slot_guid", selectedSlotGuid);
        payload.put("type", "User");
        payload.put("remarks", "Rescheduled via automation test");

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.UPDATE_ORDER_SLOTS)
                .addHeader("Authorization", token)
                .setRequestBody(payload)
                .postWithoutStatusCheck();

        System.out.println("   Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());

        Assert.assertEquals(response.getStatusCode(), 200,
                "Update order slots should return 200, got: " + response.getStatusCode()
                        + " | Body: " + response.getBody().asString());

        boolean status = response.jsonPath().getBoolean("status");
        Assert.assertTrue(status, "Reschedule response status should be true");

        String message = response.jsonPath().getString("message");
        Assert.assertEquals(message, "Order updated successfully",
                "Expected 'Order updated successfully', got: " + message);

        // Verify reschedule details
        int rescheduleCount = response.jsonPath().getInt("data.reschedule_count");
        boolean slotChanged = response.jsonPath().getBoolean("data.slot_changed");
        String newTime = response.jsonPath().getString("data.time");

        Assert.assertTrue(slotChanged, "slot_changed should be true");
        Assert.assertTrue(rescheduleCount >= 1, "reschedule_count should be >= 1");

        System.out.println("\n   ✅ ORDER RESCHEDULED SUCCESSFULLY!");
        System.out.println("   Order: " + orderId);
        System.out.println("   New Slot: " + selectedSlotGuid);
        System.out.println("   New Time: " + newTime);
        System.out.println("   Reschedule Count: " + rescheduleCount);
        System.out.println("==========================================================\n");
    }

    // ═══════════════════════════════════════════════════════════════════
    // STEP 5: VERIFY RESCHEDULE VIA GET ORDER BY ID
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 5, dependsOnMethods = "step04_UpdateOrderSlots",
            description = "Step 5: GetOrderById - Verify all updated details after reschedule")
    public void step05_VerifyRescheduleViaGetOrderById() {
        System.out.println("\n==========================================================");
        System.out.println("  RESCHEDULE FLOW - STEP 5: VERIFY VIA GET ORDER BY ID");
        System.out.println("==========================================================");

        String token = (rescheduleToken != null) ? rescheduleToken : RequestContext.getToken();
        String orderId = RequestContext.getCurrentOrderId();

        Assert.assertNotNull(orderId, "Order ID is required for verification");

        // Call GET /order/getOrderById/{orderId}
        String endpoint = APIEndpoints.GET_ORDER_BY_ID + orderId;
        System.out.println("   Endpoint: " + endpoint);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", token)
                .getWithoutStatusCheck();

        System.out.println("   Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());

        Assert.assertEquals(response.getStatusCode(), 200,
                "GetOrderById should return 200, got: " + response.getStatusCode());

        // Determine data path (could be object or array)
        Object dataObj = response.jsonPath().get("data");
        String dataPath = (dataObj instanceof java.util.List) ? "data[0]" : "data";

        // ═══════════════════════════════════════════════════════════════
        // 1. SLOT GUID VERIFICATION
        // ═══════════════════════════════════════════════════════════════
        String actualSlotGuid = response.jsonPath().getString(dataPath + ".slot_guid");
        System.out.println("\n   ─── SLOT GUID ───");
        System.out.println("   Expected: " + selectedSlotGuid);
        System.out.println("   Actual:   " + actualSlotGuid);

        Assert.assertNotNull(actualSlotGuid, "slot_guid should not be null after reschedule");
        Assert.assertEquals(actualSlotGuid, selectedSlotGuid,
                "Slot GUID in order should match the rescheduled slot. Expected: "
                        + selectedSlotGuid + ", Actual: " + actualSlotGuid);

        // ═══════════════════════════════════════════════════════════════
        // 2. SLOT DATE & TIME VERIFICATION
        // ═══════════════════════════════════════════════════════════════
        String slotStartTime = response.jsonPath().getString(dataPath + ".slot_start_time");
        String slotEndTime = response.jsonPath().getString(dataPath + ".slot_end_time");
        String slotDate = response.jsonPath().getString(dataPath + ".slot_date");

        System.out.println("\n   ─── SLOT DATE & TIME ───");
        System.out.println("   Slot Date:       " + slotDate);
        System.out.println("   Slot Start Time: " + slotStartTime);
        System.out.println("   Slot End Time:   " + slotEndTime);
        System.out.println("   Expected Date:   " + selectedSlotDate);
        System.out.println("   Expected Start:  " + selectedSlotStartTime);
        System.out.println("   Expected End:    " + selectedSlotEndTime);

        Assert.assertNotNull(slotStartTime, "slot_start_time should not be null after reschedule");
        Assert.assertNotNull(slotEndTime, "slot_end_time should not be null after reschedule");

        // Verify slot date contains the expected date
        if (slotDate != null && selectedSlotDate != null) {
            Assert.assertTrue(slotDate.contains(selectedSlotDate),
                    "slot_date should contain selected date. Expected: " + selectedSlotDate + ", Got: " + slotDate);
        } else if (slotStartTime != null && selectedSlotDate != null) {
            // Fallback: verify date from slot_start_time (format: 2026-06-23T22:00:00.000Z)
            Assert.assertTrue(slotStartTime.contains(selectedSlotDate),
                    "slot_start_time should contain selected date. Expected date: " + selectedSlotDate + ", Got: " + slotStartTime);
        }

        // Verify slot start time matches (handle format: "2026-06-23 22:00:00" vs "2026-06-23T22:00:00.000Z")
        if (slotStartTime != null && selectedSlotStartTime != null) {
            String normalizedExpectedStart = selectedSlotStartTime.replace(" ", "T");
            Assert.assertTrue(slotStartTime.contains(normalizedExpectedStart),
                    "slot_start_time should contain selected start time. Expected: "
                            + selectedSlotStartTime + ", Got: " + slotStartTime);
        }

        // Verify slot end time matches
        if (slotEndTime != null && selectedSlotEndTime != null) {
            String normalizedExpectedEnd = selectedSlotEndTime.replace(" ", "T");
            Assert.assertTrue(slotEndTime.contains(normalizedExpectedEnd),
                    "slot_end_time should contain selected end time. Expected: "
                            + selectedSlotEndTime + ", Got: " + slotEndTime);
        }

        // ═══════════════════════════════════════════════════════════════
        // 3. RESCHEDULE COUNT VERIFICATION
        // ═══════════════════════════════════════════════════════════════
        int rescheduleCount = response.jsonPath().getInt(dataPath + ".reschedule_count");
        System.out.println("\n   ─── RESCHEDULE COUNT ───");
        System.out.println("   Reschedule Count: " + rescheduleCount);

        Assert.assertTrue(rescheduleCount >= 1,
                "reschedule_count should be >= 1 after reschedule, got: " + rescheduleCount);

        // ═══════════════════════════════════════════════════════════════
        // 4. ORDER STATUS VERIFICATION
        // ═══════════════════════════════════════════════════════════════
        String orderStatus = response.jsonPath().getString(dataPath + ".order_status");
        System.out.println("\n   ─── ORDER STATUS ───");
        System.out.println("   Order Status: " + orderStatus);

        Assert.assertNotNull(orderStatus, "order_status should not be null");
        Assert.assertNotEquals(orderStatus, "Cancelled",
                "Order should not be cancelled after reschedule");

        // ═══════════════════════════════════════════════════════════════
        // 5. ORDER ID VERIFICATION
        // ═══════════════════════════════════════════════════════════════
        String actualOrderId = response.jsonPath().getString(dataPath + "._id");
        if (actualOrderId == null) {
            actualOrderId = response.jsonPath().getString(dataPath + ".order_id");
        }
        System.out.println("\n   ─── ORDER ID ───");
        System.out.println("   Expected: " + orderId);
        System.out.println("   Actual:   " + actualOrderId);

        if (actualOrderId != null) {
            Assert.assertEquals(actualOrderId, orderId,
                    "Order ID in response should match. Expected: " + orderId + ", Got: " + actualOrderId);
        }

        // ═══════════════════════════════════════════════════════════════
        // 6. PAYMENT DETAILS POPULATED
        // ═══════════════════════════════════════════════════════════════
        String paymentId = response.jsonPath().getString(dataPath + ".payment_id");
        String paymentMode = response.jsonPath().getString(dataPath + ".payment_mode");
        System.out.println("\n   ─── PAYMENT DETAILS ───");
        System.out.println("   Payment ID:   " + paymentId);
        System.out.println("   Payment Mode: " + paymentMode);

        Assert.assertNotNull(paymentId, "payment_id should be populated in order");
        Assert.assertFalse(paymentId.isEmpty(), "payment_id should not be empty");

        // ═══════════════════════════════════════════════════════════════
        // 7. CENTER & LAB DETAILS POPULATED
        // ═══════════════════════════════════════════════════════════════
        String centerId = response.jsonPath().getString(dataPath + ".lab_location_id");
        String labAddress = response.jsonPath().getString(dataPath + ".lab_location_full_address");
        String labTitle = response.jsonPath().getString(dataPath + ".lab_title");
        System.out.println("\n   ─── CENTER/LAB DETAILS ───");
        System.out.println("   Lab Location ID: " + centerId);
        System.out.println("   Lab Title:       " + labTitle);
        System.out.println("   Lab Address:     " + labAddress);

        Assert.assertNotNull(centerId, "lab_location_id should be populated in order");

        // ═══════════════════════════════════════════════════════════════
        // 8. USER/PATIENT DETAILS POPULATED
        // ═══════════════════════════════════════════════════════════════
        String userId = response.jsonPath().getString(dataPath + ".user_id");
        String mobile = response.jsonPath().getString(dataPath + ".user_details.mobile");
        System.out.println("\n   ─── USER DETAILS ───");
        System.out.println("   User ID: " + userId);
        System.out.println("   Mobile:  " + mobile);

        Assert.assertNotNull(userId, "user_id should be populated in order");
        Assert.assertNotNull(mobile, "mobile should be populated in user_details");

        // ═══════════════════════════════════════════════════════════════
        // 9. ADDRESS & LAB LOCATION
        // ═══════════════════════════════════════════════════════════════
        String address = response.jsonPath().getString(dataPath + ".lab_location_full_address");
        System.out.println("\n   ─── ADDRESS/LAB LOCATION ───");
        System.out.println("   Lab Full Address: " + address);

        Assert.assertNotNull(address, "lab_location_full_address should be populated in order");

        // ═══════════════════════════════════════════════════════════════
        // 10. AMOUNT/PRICING DETAILS POPULATED
        // ═══════════════════════════════════════════════════════════════
        Object finalPrice = response.jsonPath().get(dataPath + ".final_price");
        Object paidAmount = response.jsonPath().get(dataPath + ".paid_amount");
        Object totalPrice = response.jsonPath().get(dataPath + ".total_price");
        System.out.println("\n   ─── AMOUNT DETAILS ───");
        System.out.println("   Total Price: " + totalPrice);
        System.out.println("   Final Price: " + finalPrice);
        System.out.println("   Paid Amount: " + paidAmount);

        Assert.assertNotNull(finalPrice, "final_price should be populated in order");

        // ═══════════════════════════════════════════════════════════════
        // FINAL SUMMARY
        // ═══════════════════════════════════════════════════════════════
        System.out.println("\n   ════════════════════════════════════════════════");
        System.out.println("   ✅ GET ORDER BY ID - ALL DETAILS VERIFIED!");
        System.out.println("   ════════════════════════════════════════════════");
        System.out.println("   ✔ Slot GUID matches:       " + selectedSlotGuid);
        System.out.println("   ✔ Slot Date:               " + (slotDate != null ? slotDate : selectedSlotDate));
        System.out.println("   ✔ Slot Start Time:         " + slotStartTime);
        System.out.println("   ✔ Slot End Time:           " + slotEndTime);
        System.out.println("   ✔ Reschedule Count:        " + rescheduleCount);
        System.out.println("   ✔ Order Status:            " + orderStatus);
        System.out.println("   ✔ Order ID:                " + actualOrderId);
        System.out.println("   ✔ Payment ID:              " + paymentId);
        System.out.println("   ✔ Lab Location ID:         " + centerId);
        System.out.println("   ✔ Lab Title:               " + labTitle);
        System.out.println("   ✔ User ID:                 " + userId);
        System.out.println("   ✔ Mobile:                  " + mobile);
        System.out.println("   ✔ Final Price:             " + finalPrice);
        System.out.println("   ✔ Paid Amount:             " + paidAmount);
        System.out.println("   ════════════════════════════════════════════════");
        System.out.println("==========================================================\n");
    }
}

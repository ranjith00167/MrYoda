package com.mryoda.diagnostics.api.payloads;

import java.util.HashMap;
import java.util.Map;

public class OrderPayloadBuilder {

    /**
     * Builds the payload for adding a new address.
     */
    public static Map<String, Object> buildAddressPayload(String userId, String receiverName, String mobile,
            String addressLine1, String locationName, String type, String country, String state, String city,
            String postalCode, String countryCode, String latitude, String longitude) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", userId);
        payload.put("receiver_name", receiverName);
        payload.put("recipient_mobile_number", mobile);
        payload.put("address_line1", addressLine1);
        payload.put("name", locationName);
        payload.put("type", type);
        payload.put("country", country);
        payload.put("state", state);
        payload.put("city", city);
        payload.put("postal_code", postalCode);
        payload.put("country_code", countryCode);
        payload.put("latitude", latitude);
        payload.put("longitude", longitude);
        return payload;
    }

    /**
     * Builds the payload for searching available slots.
     */
    public static Map<String, Object> buildSlotSearchPayload(String date, int page, int limit, String type,
            String addressGuid) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("slot_start_time", date);
        payload.put("page", page);
        payload.put("limit", limit);
        payload.put("type", type);
        payload.put("addressguid", addressGuid);
        return payload;
    }

    /**
     * Builds the payload for VerifyPayment (COD Order Creation).
     */
    public static Map<String, Object> buildVerifyPaymentPayload(String cartId, String paymentMode, String source,
            String userId, String addressId, String slotId, String date, String time, int totalAmount,
            String orderType, String labLocationId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("cart_id", cartId);
        payload.put("payment_mode", paymentMode);
        payload.put("source", source);
        payload.put("user_id", userId);
        payload.put("address_id", addressId); // Ensure this is passed as String if needed
        payload.put("slot_id", slotId);
        payload.put("date", date);
        payload.put("time", time);
        payload.put("total_amount", totalAmount);
        payload.put("order_type", orderType);
        payload.put("lab_location_id", labLocationId);
        return payload;
    }
    
    /**
     * Builds the payload for CreateOrder (if used separately).
     */
    public static Map<String, Object> buildCreateOrderPayload(String cartId, String userId, String addressId, 
            String slotId, String date, String time, String labLocationId, int totalAmount, String orderType) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("cart_id", cartId);
        payload.put("payment_mode", "cash");
        payload.put("source", "android");
        payload.put("user_id", userId);
        payload.put("address_id", addressId);
        payload.put("slot_id", slotId);
        payload.put("date", date);
        payload.put("time", time);
        payload.put("total_amount", totalAmount);
        payload.put("lab_location_id", labLocationId);
        payload.put("order_type", orderType);
        return payload;
    }
}

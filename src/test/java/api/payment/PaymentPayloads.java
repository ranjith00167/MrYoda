package api.payment;

import java.util.HashMap;
import java.util.Map;

public class PaymentPayloads {

    public static Map<String, Object> buildVerifyPaymentPayload(String cartId, String paymentMode, String source,
            String userId, String addressId, String slotId, String date, String time, int totalAmount,
            String orderType, String labLocationId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("cart_id", cartId);
        payload.put("payment_mode", paymentMode);
        payload.put("source", source);
        payload.put("user_id", userId);
        payload.put("address_id", addressId); 
        payload.put("slot_id", slotId);
        payload.put("date", date);
        payload.put("time", time);
        payload.put("total_amount", totalAmount);
        payload.put("order_type", orderType);
        payload.put("lab_location_id", labLocationId);
        return payload;
    }
}

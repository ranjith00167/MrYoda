package api.order;

import java.util.HashMap;
import java.util.Map;

public class OrderPayloads {

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

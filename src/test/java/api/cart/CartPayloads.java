package api.cart;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class CartPayloads {
    
    public static Map<String, Object> buildAddToCartPayload(String userId, String locationId, List<Map<String, Object>> items) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", userId);
        payload.put("location_id", locationId);
        payload.put("items", items);
        return payload;
    }
}

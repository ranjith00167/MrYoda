package api.address;

import java.util.HashMap;
import java.util.Map;

public class AddressPayloads {
    
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
}

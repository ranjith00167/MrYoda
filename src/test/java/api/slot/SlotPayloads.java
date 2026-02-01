package api.slot;

import java.util.HashMap;
import java.util.Map;

public class SlotPayloads {

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
}

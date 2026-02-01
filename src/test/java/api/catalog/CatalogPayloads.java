package api.catalog;

import java.util.HashMap;
import java.util.Map;

public class CatalogPayloads {

    public static Map<String, Object> buildGlobalSearchPayload(String searchString, String locationId, int page, int limit) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("page", page);
        payload.put("limit", limit);
        payload.put("search_string", searchString);
        payload.put("sort_by", "Type");
        payload.put("location", locationId);
        return payload;
    }
}

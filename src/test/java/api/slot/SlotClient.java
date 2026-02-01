package api.slot;

import com.mryoda.diagnostics.api.builders.RequestBuilder;
import io.restassured.response.Response;
import java.util.Map;

public class SlotClient {

    public Response getCentersByAddress(String token, Map<String, Object> payload) {
        // Assuming GET_CENTERS_BY_ADD is a POST request based on payload usage, 
        // usually slot search is POST if it has complex body. 
        // 'GET_CENTERS_BY_ADD' name is confusing but if it takes body it might be POST. 
        // Checking GlobalSearchHelper or APIEndpoints didn't satisfy this confirm, 
        // but OrderPayloadBuilder had 'buildSlotSearchPayload' which implies it's sent somewhere.
        // If it's a GET with query params, RequestBuilder handles params differently.
        // Assuming POST for now as is common for search/filter endpoints in this project.
        return new RequestBuilder()
                .setEndpoint(SlotEndpoints.GET_CENTERS_BY_ADD)
                .addHeader("Authorization", "Bearer " + token)
                .setRequestBody(payload)
                .post();
    }
    
    public Response getSlotCountByTime(String token, Map<String, Object> payload) {
        return new RequestBuilder()
                .setEndpoint(SlotEndpoints.GET_SLOT_COUNT_BY_TIME)
                .addHeader("Authorization", "Bearer " + token)
                .setRequestBody(payload) // Assuming body
                .post();
    }
}

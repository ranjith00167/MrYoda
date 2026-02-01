package api.catalog;

import com.mryoda.diagnostics.api.builders.RequestBuilder;
import io.restassured.response.Response;
import java.util.Map;

public class CatalogClient {

    public Response globalSearch(String token, Map<String, Object> payload) {
        return new RequestBuilder()
                .setEndpoint(CatalogEndpoints.GLOBAL_SEARCH)
                .addHeader("Authorization", "Bearer " + token)
                .setRequestBody(payload)
                .post();
    }
    
    public Response getLocations() {
        return new RequestBuilder()
                .setEndpoint(CatalogEndpoints.GET_LOCATION)
                .get();
    }

    public Response getSampleType(String token) {
        return new RequestBuilder()
                .setEndpoint(CatalogEndpoints.GET_SAMPLE_TYPE)
                .addHeader("Authorization", "Bearer " + token)
                .get();
    }
}

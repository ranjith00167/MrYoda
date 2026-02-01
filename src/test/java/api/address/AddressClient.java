package api.address;

import com.mryoda.diagnostics.api.builders.RequestBuilder;
import io.restassured.response.Response;
import java.util.Map;

public class AddressClient {

    public Response addAddress(String token, Map<String, Object> payload) {
        return new RequestBuilder()
                .setEndpoint(AddressEndpoints.ADD_ADDRESS)
                .addHeader("Authorization", "Bearer " + token)
                .setRequestBody(payload)
                .post();
    }

    public Response getAddressByUserId(String token, String userId) {
        return new RequestBuilder()
                .setEndpoint(AddressEndpoints.GET_ADDRESS_BY_USER_ID.replace("{user_id}", userId))
                .addHeader("Authorization", "Bearer " + token)
                .get();
    }
}

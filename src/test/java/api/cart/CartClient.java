package api.cart;

import com.mryoda.diagnostics.api.builders.RequestBuilder;
import io.restassured.response.Response;
import java.util.Map;
import java.util.List;

public class CartClient {

    public Response addToCart(String token, Map<String, Object> payload) {
        return new RequestBuilder()
                .setEndpoint(CartEndpoints.ADD_TO_CART)
                .addHeader("Authorization", "Bearer " + token)
                .setRequestBody(payload)
                .post();
    }

    public Response getCartById(String token, String userId) {
        return new RequestBuilder()
                .setEndpoint(CartEndpoints.GET_CART_BY_ID.replace("{user_id}", userId))
                .addHeader("Authorization", "Bearer " + token)
                .get();
    }
}

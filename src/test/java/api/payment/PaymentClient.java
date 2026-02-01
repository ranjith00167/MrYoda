package api.payment;

import com.mryoda.diagnostics.api.builders.RequestBuilder;
import io.restassured.response.Response;
import java.util.Map;

public class PaymentClient {

    public Response verifyPayment(String token, Map<String, Object> payload) {
        return new RequestBuilder()
                .setEndpoint(PaymentEndpoints.VERIFY_PAYMENT)
                .addHeader("Authorization", "Bearer " + token)
                .setRequestBody(payload)
                .post();
    }

    public Response getPaymentById(String token, Map<String, Object> queryParams) {
        return new RequestBuilder()
                .setEndpoint(PaymentEndpoints.GET_PAYMENT_BY_ID)
                .addHeader("Authorization", "Bearer " + token)
                .setQueryParams(queryParams) 
                .get();
    }
}

package api.payment;

import com.mryoda.diagnostics.api.builders.RequestBuilder;
import io.restassured.response.Response;
import java.util.Map;

public class PaymentClient {

    /**
     * STEP 1 – Initiate Razorpay online payment.
     * Returns razorpay_order_id used in the verify step.
     */
    public Response initiatePayment(String token, Map<String, Object> payload) {
        return new RequestBuilder()
                .setEndpoint(PaymentEndpoints.INITIATE_PAYMENT)
                .addHeader("Authorization", "Bearer " + token)
                .setRequestBody(payload)
                .post();
    }

    /**
     * STEP 2 – Verify Razorpay payment after gateway callback.
     */
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

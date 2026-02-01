package api.order;

import com.mryoda.diagnostics.api.builders.RequestBuilder;
import io.restassured.response.Response;
import java.util.Map;

public class OrderClient {

    public Response createOrder(String token, Map<String, Object> payload) {
        return new RequestBuilder()
                .setEndpoint(OrderEndpoints.CREATE_ORDER)
                .addHeader("Authorization", "Bearer " + token)
                .setRequestBody(payload)
                .post();
    }

    public Response getOrderById(String token, String orderId) {
        // Appending ID if the endpoint ends with /
        String endpoint = OrderEndpoints.GET_ORDER_BY_ID;
        if (!endpoint.endsWith("/")) endpoint += "/";
        
        return new RequestBuilder()
                .setEndpoint(endpoint + orderId)
                .addHeader("Authorization", "Bearer " + token)
                .get();
    }
    
    public Response getOrderTrackingStatus(String token, String guid) {
         return new RequestBuilder()
                .setEndpoint(OrderEndpoints.GET_ORDER_TRACKING_STATUS.replace("{guid}", guid))
                .addHeader("Authorization", "Bearer " + token)
                .get();
    }
    
    public Response approvePayment(String token, Map<String, Object> payload) {
        return new RequestBuilder()
                .setEndpoint(OrderEndpoints.APPROVE_PAYMENT)
                .addHeader("Authorization", "Bearer " + token)
                .setRequestBody(payload)
                .post();
    }
    
    public Response adminVerifyOtp(String token, Map<String, Object> payload) {
        return new RequestBuilder()
                .setEndpoint(OrderEndpoints.ADMIN_VERIFY_OTP)
                .addHeader("Authorization", "Bearer " + token)
                .setRequestBody(payload)
                .post();
    }
    
    public Response getVisitStatus(String token) {
        // Typically external URL might not need Bearer token or might need specific auth. 
        // Assuming Standard Bearer for now as it's in the same project context.
        return new RequestBuilder()
                .setEndpoint(OrderEndpoints.VISIT_STATUS_UAT)
                .addHeader("Authorization", "Bearer " + token)
                .get();
    }
    
    public Response getReportDetails(String token, String visitNumber) {
        return new RequestBuilder()
                .setEndpoint(OrderEndpoints.GET_REPORT_DETAILS.replace("{visit_Number}", visitNumber))
                .addHeader("Authorization", "Bearer " + token)
                .get();
    }
}

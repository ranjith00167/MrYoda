package com.mryoda.diagnostics.api.tests.order;

import com.mryoda.diagnostics.api.builders.RequestBuilder;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.AssertionUtil;
import com.mryoda.diagnostics.api.utils.RequestContext;
import io.restassured.response.Response;
import org.testng.annotations.Test;
import java.util.HashMap;
import java.util.Map;

public class COD_19_OrderCancellationTest {

    @Test(groups = "cancel_flow")
    public void step19_A_VerifyOrderBeforeCancellation() {
        System.out.println("\n>>> STEP 19-A: VERIFY ORDER BEFORE CANCELLATION <<<");
        String orderId = RequestContext.getCurrentOrderId();
        AssertionUtil.verifyNotNull(orderId, "Order ID should not be null");

        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.GET_ORDER_BY_ID + orderId;
        System.out.println("Target URL: " + endpoint);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + RequestContext.getToken())
                .get();

        System.out.println("Response Status: " + response.getStatusCode());
        AssertionUtil.verifyEquals(response.getStatusCode(), 200, "Get Order By ID should return 200");

        Object statusObj = response.jsonPath().get("data.order_status");
        if (statusObj == null) {
            statusObj = response.jsonPath().get("data[0].order_status");
        }
        
        String orderStatus;
        if (statusObj instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) statusObj;
            orderStatus = list.isEmpty() ? null : list.get(0).toString();
        } else {
            orderStatus = statusObj != null ? statusObj.toString() : null;
        }
        
        System.out.println("✅ Current Order Status: " + orderStatus);
        // Usually it should be 'samples_collected' or 'paid' at this point in the modular flow
    }

    @Test(groups = "cancel_flow", dependsOnMethods = "step19_A_VerifyOrderBeforeCancellation")
    public void step19_B_CancelOrder() {
        System.out.println("\n>>> STEP 19-B: CANCEL ORDER <<<");
        String orderId = RequestContext.getCurrentOrderId();
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("order_guid", orderId);
        payload.put("order_status", "Cancelled");

        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.UPDATE_ORDER;
        System.out.println("Target URL: " + endpoint);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + RequestContext.getToken())
                .setRequestBody(payload)
                .post();

        System.out.println("Response Status: " + response.getStatusCode());
        AssertionUtil.verifyEquals(response.getStatusCode(), 200, "Update Order Status should return 200");

        String msg = response.jsonPath().getString("msg");
        System.out.println("Response Message: " + msg);
        AssertionUtil.verifyEquals(msg, "Order updated successfully", "Success message should match");
        
        System.out.println("✅ Order Status Updated to 'Cancelled' successfully.");
    }

    @Test(groups = "cancel_flow", dependsOnMethods = "step19_B_CancelOrder")
    public void step19_C_VerifyOrderAfterCancellation() {
        System.out.println("\n>>> STEP 19-C: VERIFY ORDER AFTER CANCELLATION <<<");
        String orderId = RequestContext.getCurrentOrderId();

        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.GET_ORDER_BY_ID + orderId;
        System.out.println("Target URL: " + endpoint);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + RequestContext.getToken())
                .get();

        System.out.println("Response Status: " + response.getStatusCode());
        AssertionUtil.verifyEquals(response.getStatusCode(), 200, "Get Order By ID should return 200");

        Object statusObj = response.jsonPath().get("data.order_status");
        if (statusObj == null) {
            statusObj = response.jsonPath().get("data[0].order_status");
        }
        
        String orderStatus;
        if (statusObj instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) statusObj;
            orderStatus = list.isEmpty() ? null : list.get(0).toString();
        } else {
            orderStatus = statusObj != null ? statusObj.toString() : null;
        }
        
        System.out.println("✅ Final Order Status: " + orderStatus);
        AssertionUtil.verifyEquals(orderStatus, "Cancelled", "Order status should be 'Cancelled'");
        
        System.out.println("✅ Cancellation Flow Verified Mathematically and via API.");
    }
}

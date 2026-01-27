package com.mryoda.diagnostics.api.tests;

import com.mryoda.diagnostics.api.utils.RequestContext;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

public class COD_14_VerifySamplesCollectedStatusTest extends CreateOrderCODAPITest {

    @Test
    public void step14_VerifySamplesCollectedStatus() {
        System.out.println("\n>>> STEP 14: VERIFY STATUS (SAMPLES COLLECTED) IN ORDER DETAILS <<<");
        String token = RequestContext.getToken();
        String orderId = RequestContext.getCurrentOrderId();

        Response response = callGetOrderByIdAPI(token, orderId);
        Assert.assertEquals(response.getStatusCode(), 200, "GetOrderById should return 200");

        Object statusObj = response.jsonPath().get("data.order_status");
        String status = null;

        if (statusObj instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) statusObj;
            if (!list.isEmpty()) {
                status = list.get(0).toString();
            }
        } else if (statusObj != null) {
            status = statusObj.toString();
        }

        System.out.println("   Current Order Status (Raw): " + statusObj);
        System.out.println("   Current Order Status (Extracted): " + status);

        Assert.assertNotNull(status, "Order Status should not be null");

        String normalizedStatus = status.toLowerCase().replace(" ", "_").replace("[", "").replace("]", "");

        if (normalizedStatus.contains("sample_collected") || normalizedStatus.contains("samples_collected")) {
            System.out.println("✅ Order Status Verified: " + status);
        } else {
            Assert.fail("Order Status mismatch! Expected 'samples_collected' or 'Sample Collected' but got '" + status
                    + "'");
        }
    }
}

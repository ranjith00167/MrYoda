package com.mryoda.diagnostics.api.tests;

import com.mryoda.diagnostics.api.utils.RequestContext;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.Map;

public class COD_04_VerifyPaymentPreCheckTest extends CreateOrderCODAPITest {

    @Test
    public void step04_VerifyPaymentPreCheck() {
        System.out.println("\n>>> STEP 4: VERIFY PAYMENT (PRE-CHECK) <<<");
        String token = RequestContext.getToken();
        String userId = RequestContext.getUserId();
        String cartId = RequestContext.getCurrentCartId();
        String addressId = RequestContext.getCurrentAddressId();
        String slotGuid = RequestContext.getCurrentSlotGuid();
        int totalPrice = RequestContext.getCurrentTotalPrice();

        // Fetch Cart again to get labLocationId
        Response cartRes = callGetCartAPI(token, userId);
        Object dataObj = cartRes.jsonPath().get("data");
        String dataPath = (dataObj instanceof java.util.List) ? "data[0]" : "data";
        String labLocationId = cartRes.jsonPath().getString(dataPath + ".lab_location_id");
        String orderType = cartRes.jsonPath().getString(dataPath + ".order_type");

        // Need Date/Time from slot search.
        String date = RequestContext.getSlotStartDate();
        String time = RequestContext.getMemberSlotTime(); // Used this field in Step 3

        Map<String, String> result = callVerifyPaymentAPI(token, userId, cartId, addressId, slotGuid,
                labLocationId, orderType, totalPrice, date, time, "mobile");

        String paymentId = result.get("paymentId");
        String orderId = result.get("orderId");

        RequestContext.setCurrentPaymentId(paymentId);
        RequestContext.setCurrentOrderId(orderId);
    }
}

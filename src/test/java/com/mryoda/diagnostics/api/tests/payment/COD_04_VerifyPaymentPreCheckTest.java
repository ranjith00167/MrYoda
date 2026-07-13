package com.mryoda.diagnostics.api.tests.payment;

import com.mryoda.diagnostics.api.tests.order.CreateOrderCODAPITest;

import com.mryoda.diagnostics.api.utils.RequestContext;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.Map;

public class COD_04_VerifyPaymentPreCheckTest extends CreateOrderCODAPITest {

    @Test(description = "QA Automation: Verify Payment Pre Check")
    public void step04_VerifyPaymentPreCheck() {
        System.out.println("\n>>> STEP 4: VERIFY PAYMENT (PRE-CHECK) <<<");
        String token = RequestContext.getToken();
        String userId = RequestContext.getUserId();
        String cartId = RequestContext.getCurrentCartId();
        String addressId = RequestContext.getCurrentAddressId();
        String slotGuid = RequestContext.getCurrentSlotGuid();
        int totalPrice = RequestContext.getCurrentTotalPrice();

        // Determine orderType
        String orderType = (addressId != null) ? "home" : "lab";

        // Fetch Cart again to get labLocationId
        Response cartRes = callGetCartAPI(token, userId, orderType);
        Object dataObj = cartRes.jsonPath().get("data");
        String dataPath = (dataObj instanceof java.util.List) ? "data[0]" : "data";
        String labLocationId = cartRes.jsonPath().getString(dataPath + ".lab_location_id");
        String cartOrderType = cartRes.jsonPath().getString(dataPath + ".order_type");
        if (cartOrderType != null)
            orderType = cartOrderType;

        // Read slot date with fallback
        String date = RequestContext.getSlotStartDate();
        if (date == null) {
            date = RequestContext.getExpectedSlotDate();
        }

        // Read slot time with fallback chain
        String time = RequestContext.getMemberSlotTime();
        if (time == null) {
            time = RequestContext.getNonMemberSlotTime();
        }
        if (time == null) {
            time = RequestContext.getExpectedSlotTimeString();
        }

        System.out.println("   Slot Date: " + date);
        System.out.println("   Slot Time: " + time);
        System.out.println("   Order Type: " + orderType);

        Map<String, String> result = callVerifyPaymentAPI(token, userId, cartId, addressId, slotGuid,
                labLocationId, orderType, totalPrice, date, time, "mobile");

        String paymentId = result.get("paymentId");
        String orderId = result.get("orderId");

        RequestContext.setCurrentPaymentId(paymentId);
        RequestContext.setCurrentOrderId(orderId);
    }
}

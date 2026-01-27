package com.mryoda.diagnostics.api.tests;

import com.mryoda.diagnostics.api.utils.RequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;

public class COD_07_AssignOrderTest extends CreateOrderCODAPITest {

    @Test
    public void step07_AssignOrder() {
        System.out.println("\n>>> STEP 7: ASSIGN ORDER <<<");
        String orderId = RequestContext.getCurrentOrderId();
        String phleboGuid = RequestContext.getCurrentPhleboGuid();
        int totalPrice = RequestContext.getCurrentTotalPrice();
        String paymentId = RequestContext.getCurrentPaymentId();
        String addressGuid = RequestContext.getCurrentAddressGuid();
        String userId = RequestContext.getUserId();
        String slotGuid = RequestContext.getCurrentSlotGuid();

        String orderTrackingId = callAssignOrderAPI(orderId, phleboGuid, totalPrice, paymentId, addressGuid, userId,
                slotGuid);
        Assert.assertNotNull(orderTrackingId, "Order Tracking ID must be returned");
        RequestContext.setCurrentOrderTrackingId(orderTrackingId);
    }
}

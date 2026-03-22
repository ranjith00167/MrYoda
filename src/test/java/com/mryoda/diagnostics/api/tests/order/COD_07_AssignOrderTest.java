package com.mryoda.diagnostics.api.tests.order;

import com.mryoda.diagnostics.api.utils.RequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;

public class COD_07_AssignOrderTest extends CreateOrderCODAPITest {

    @Test(description = "QA Automation: Verify Assign Order")
    public void step07_AssignOrder() {
        System.out.println("\n>>> STEP 7: ASSIGN ORDER (MULTI-ORDER SUPPORT) <<<");
        java.util.List<String> orderIds = RequestContext.getCurrentOrderIds();
        String phleboGuid = RequestContext.getCurrentPhleboGuid();
        int totalPrice = RequestContext.getCurrentTotalPrice();
        String paymentId = RequestContext.getCurrentPaymentId();
        String addressGuid = RequestContext.getCurrentAddressGuid();
        String userId = RequestContext.getUserId();
        String slotGuid = RequestContext.getCurrentSlotGuid();

        java.util.List<String> trackingIds = new java.util.ArrayList<>();

        for (String orderId : orderIds) {
            System.out.println("   -> Assigning Order: " + orderId);
            String orderTrackingId = callAssignOrderAPI(orderId, phleboGuid, totalPrice, paymentId, addressGuid, userId,
                    slotGuid);
            Assert.assertNotNull(orderTrackingId, "Order Tracking ID must be returned for order: " + orderId);
            trackingIds.add(orderTrackingId);
        }

        RequestContext.setCurrentOrderTrackingIds(trackingIds);
        System.out.println("✅ All " + trackingIds.size() + " orders assigned successfully.");
    }
}

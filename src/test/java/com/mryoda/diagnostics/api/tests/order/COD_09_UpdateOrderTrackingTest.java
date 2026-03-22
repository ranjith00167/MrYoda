package com.mryoda.diagnostics.api.tests.order;

import com.mryoda.diagnostics.api.utils.RequestContext;
import org.testng.annotations.Test;

public class COD_09_UpdateOrderTrackingTest extends CreateOrderCODAPITest {

    @Test(description = "QA Automation: Verify Update Order Tracking")
    public void step09_UpdateOrderTracking() {
        System.out.println("\n>>> STEP 9: UPDATE ORDER TRACKING (MULTI-ORDER) <<<");

        String lat = RequestContext.getLocationLatitude("UserAddress");
        String lng = RequestContext.getLocationLongitude("UserAddress");
        String name = RequestContext.getLocationId("UserAddressName");

        java.util.List<String> trackingIds = RequestContext.getCurrentOrderTrackingIds();
        java.util.List<String> orderIds = RequestContext.getCurrentOrderIds();

        for (int i = 0; i < trackingIds.size(); i++) {
            String tid = trackingIds.get(i);
            String oid = (i < orderIds.size()) ? orderIds.get(i) : orderIds.get(0);

            System.out.println("   -> Updating Tracking for Order: " + oid + " (TID: " + tid + ")");
            callUpdateOrderTrackingAPI(tid, oid, lat, lng, name);
        }
    }
}

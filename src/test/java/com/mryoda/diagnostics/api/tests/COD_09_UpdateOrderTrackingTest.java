package com.mryoda.diagnostics.api.tests;

import com.mryoda.diagnostics.api.utils.RequestContext;
import org.testng.annotations.Test;

public class COD_09_UpdateOrderTrackingTest extends CreateOrderCODAPITest {

    @Test
    public void step09_UpdateOrderTracking() {
        System.out.println("\n>>> STEP 9: UPDATE ORDER TRACKING <<<");

        String lat = RequestContext.getLocationLatitude("UserAddress");
        String lng = RequestContext.getLocationLongitude("UserAddress");
        String name = RequestContext.getLocationId("UserAddressName"); // using key as 'id' field in generic map

        // If not found in generic map (since I used loose keys in step 3), fallback or
        // ensure Step 3 sets them correctly.
        // In Step 3 I used:
        // RequestContext.storeLocationCoordinates("UserAddress", lat, lng);
        // RequestContext.storeLocation("UserAddressName", name);
        // NOTE: storeLocation puts it in 'locations' map. getLocationId gets it from
        // 'locations'. Correct.

        String orderTrackingId = RequestContext.getCurrentOrderTrackingId();
        String orderId = RequestContext.getCurrentOrderId();

        callUpdateOrderTrackingAPI(orderTrackingId, orderId, lat, lng, name);
    }
}

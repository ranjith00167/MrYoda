package com.mryoda.diagnostics.api.tests;

import com.mryoda.diagnostics.api.utils.RequestContext;
import org.testng.annotations.Test;

public class COD_08_VerifyStatusAssignedTest extends CreateOrderCODAPITest {

    @Test
    public void step08_VerifyStatusAssigned() {
        System.out.println("\n>>> STEP 8: VERIFY STATUS (ASSIGNED) <<<");
        String orderTrackingId = RequestContext.getCurrentOrderTrackingId();
        callGetOrderTrackingStatusAPI(orderTrackingId, "Phlebotomist assigned");
    }
}

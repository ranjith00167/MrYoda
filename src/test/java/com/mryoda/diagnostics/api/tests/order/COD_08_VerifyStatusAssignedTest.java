package com.mryoda.diagnostics.api.tests.order;

import com.mryoda.diagnostics.api.utils.RequestContext;
import org.testng.annotations.Test;

public class COD_08_VerifyStatusAssignedTest extends CreateOrderCODAPITest {

    @Test(description = "QA Automation: Verify Status Assigned")
    public void step08_VerifyStatusAssigned() {
        System.out.println("\n>>> STEP 8: VERIFY STATUS (ASSIGNED - MULTI-ORDER) <<<");
        java.util.List<String> trackingIds = RequestContext.getCurrentOrderTrackingIds();
        for (String tid : trackingIds) {
            System.out.println("   -> Verifying Status for Tracking ID: " + tid);
            callGetOrderTrackingStatusAPI(tid, "Phlebotomist assigned");
        }
    }
}

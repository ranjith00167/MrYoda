package com.mryoda.diagnostics.api.tests.order;

import com.mryoda.diagnostics.api.utils.RequestContext;
import org.testng.annotations.Test;

public class COD_13_UpdateStatusSamplesCollectedTest extends CreateOrderCODAPITest {

    @Test
    public void step13_UpdateStatusSamplesCollected() {
        System.out.println("\n>>> STEP 13: UPDATE STATUS (SAMPLES COLLECTED - MULTI-ORDER) <<<");
        String sampleType = RequestContext.getCurrentSampleType();
        java.util.List<String> trackingIds = RequestContext.getCurrentOrderTrackingIds();

        if (sampleType != null) {
            for (String tid : trackingIds) {
                System.out.println("   -> Updating Samples Collected for Tracking ID: " + tid);
                callUpdateOrderSamplesCollectedAPI(tid, sampleType);
            }
        } else {
            System.out.println("⚠️ Skipping Step 13: No Sample Type available from previous step.");
        }
    }
}

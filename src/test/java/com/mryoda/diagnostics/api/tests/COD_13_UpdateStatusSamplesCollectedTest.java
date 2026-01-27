package com.mryoda.diagnostics.api.tests;

import com.mryoda.diagnostics.api.utils.RequestContext;
import org.testng.annotations.Test;

public class COD_13_UpdateStatusSamplesCollectedTest extends CreateOrderCODAPITest {

    @Test
    public void step13_UpdateStatusSamplesCollected() {
        System.out.println("\n>>> STEP 13: UPDATE STATUS (SAMPLES COLLECTED) <<<");
        String sampleType = RequestContext.getCurrentSampleType();
        String orderTrackingId = RequestContext.getCurrentOrderTrackingId();

        if (sampleType != null) {
            callUpdateOrderSamplesCollectedAPI(orderTrackingId, sampleType);
        } else {
            System.out.println("⚠️ Skipping Step 13: No Sample Type available from previous step.");
        }
    }
}

package com.mryoda.diagnostics.api.tests;

import com.mryoda.diagnostics.api.utils.RequestContext;
import org.testng.annotations.Test;

public class COD_10_FinalVerificationTest extends CreateOrderCODAPITest {

    @Test
    public void step10_FinalVerification() {
        System.out.println("\n>>> STEP 10: FINAL VERIFICATION (Status & Phlebo) <<<");
        String orderTrackingId = RequestContext.getCurrentOrderTrackingId();
        String orderId = RequestContext.getCurrentOrderId();
        String phleboGuid = RequestContext.getCurrentPhleboGuid();
        String token = RequestContext.getToken();

        // 1. Status Check
        callGetOrderTrackingStatusAPI(orderTrackingId, "inprogress");

        // 2. Phlebo Check
        verifyPhlebotomistAssignment(token, orderId, phleboGuid);

        System.out.println("✅ Detailed COD Flow Step 10 Completed.");
    }
}

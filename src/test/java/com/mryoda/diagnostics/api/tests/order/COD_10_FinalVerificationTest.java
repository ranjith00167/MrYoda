package com.mryoda.diagnostics.api.tests.order;

import com.mryoda.diagnostics.api.utils.RequestContext;
import org.testng.annotations.Test;

public class COD_10_FinalVerificationTest extends CreateOrderCODAPITest {

    @Test
    public void step10_FinalVerification() {
        System.out.println("\n>>> STEP 10: FINAL VERIFICATION (Status & Phlebo - MULTI-ORDER) <<<");
        java.util.List<String> trackingIds = RequestContext.getCurrentOrderTrackingIds();
        java.util.List<String> orderIds = RequestContext.getCurrentOrderIds();
        String phleboGuid = RequestContext.getCurrentPhleboGuid();
        String token = RequestContext.getToken();

        for (int i = 0; i < trackingIds.size(); i++) {
            String tid = trackingIds.get(i);
            String oid = (i < orderIds.size()) ? orderIds.get(i) : orderIds.get(0);

            System.out.println("   -> Verifying Order: " + oid + " (TID: " + tid + ")");
            // 1. Status Check
            callGetOrderTrackingStatusAPI(tid, "inprogress");

            // 2. Phlebo Check
            verifyPhlebotomistAssignment(token, oid, phleboGuid);

            // 3. History Check
            verifyOrderHistory(token, oid, "inprogress");
        }

        System.out.println("✅ Detailed COD Flow Step 10 Completed for all orders.");
    }
}

package com.mryoda.diagnostics.api.tests.order;

import com.mryoda.diagnostics.api.utils.RequestContext;
import org.testng.annotations.Test;

public class COD_11_AdminVerifyOtpTest extends CreateOrderCODAPITest {

    @Test(description = "QA Automation: Verify Admin Verify Otp")
    public void step11_AdminVerifyOtp() {
        System.out.println("\n>>> STEP 11: ADMIN VERIFY OTP (MULTI-ORDER) <<<");
        java.util.List<String> trackingIds = RequestContext.getCurrentOrderTrackingIds();
        java.util.List<String> orderIds = RequestContext.getCurrentOrderIds();

        if (trackingIds.isEmpty() || orderIds.isEmpty()) {
            System.out.println(
                    "⚠️ Warning: No tracking IDs or order IDs found in context. Skipping multi-order verification.");
            // Fallback for singular if lists are empty but singular fields are not (safety)
            String singTid = RequestContext.getCurrentOrderTrackingId();
            String singOid = RequestContext.getCurrentOrderId();
            if (singTid != null && singOid != null) {
                System.out.println("   -> Verifying Singular Order: " + singOid + " (TID: " + singTid + ")");
                callAdminVerifyOtpAPI(singTid, singOid);
            }
            return;
        }

        for (int i = 0; i < trackingIds.size(); i++) {
            String tid = trackingIds.get(i);
            String oid = (i < orderIds.size()) ? orderIds.get(i) : orderIds.get(0);

            System.out.println("   -> Verifying OTP for Order: " + oid + " (TID: " + tid + ")");
            callAdminVerifyOtpAPI(tid, oid);
        }

        System.out.println("✅ Detailed COD Flow Step 11 Completed for all " + trackingIds.size() + " orders.");
    }
}

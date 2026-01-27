package com.mryoda.diagnostics.api.tests;

import com.mryoda.diagnostics.api.utils.RequestContext;
import org.testng.annotations.Test;

public class COD_11_AdminVerifyOtpTest extends CreateOrderCODAPITest {

    @Test
    public void step11_AdminVerifyOtp() {
        System.out.println("\n>>> STEP 11: ADMIN VERIFY OTP <<<");
        String orderTrackingId = RequestContext.getCurrentOrderTrackingId();
        String orderId = RequestContext.getCurrentOrderId();

        callAdminVerifyOtpAPI(orderTrackingId, orderId);
        System.out.println("✅ Detailed COD Flow Step 11 Completed.");
    }
}

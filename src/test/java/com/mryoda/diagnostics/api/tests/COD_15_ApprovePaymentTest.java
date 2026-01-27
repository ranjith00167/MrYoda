package com.mryoda.diagnostics.api.tests;

import com.mryoda.diagnostics.api.utils.RequestContext;
import org.testng.annotations.Test;

public class COD_15_ApprovePaymentTest extends CreateOrderCODAPITest {

    @Test
    public void step15_ApprovePayment() {
        System.out.println("\n>>> STEP 15: APPROVE PAYMENT <<<");
        String token = RequestContext.getToken();
        String orderId = RequestContext.getCurrentOrderId();

        callApprovePaymentAPI(token, orderId);
        System.out.println("✅ Detailed COD Flow Step 15 Completed.");
    }
}

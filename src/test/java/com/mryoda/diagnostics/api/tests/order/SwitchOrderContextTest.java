package com.mryoda.diagnostics.api.tests.order;

import com.mryoda.diagnostics.api.utils.RequestContext;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Switches the active order in RequestContext to a specific index within
 * the multi-order list (currentOrderIds).
 *
 * Use BEFORE COD_19_OrderCancellationTest in multi-member suites so that
 * the correct member's order is targeted for cancellation.
 *
 *   orderIndex=0  → primary member's order  (default after order creation)
 *   orderIndex=1  → added family member's order
 */
public class SwitchOrderContextTest {

    @Parameters("orderIndex")
    @Test(description = "Switch active order context to the specified index in currentOrderIds list")
    public void switchActiveOrder(@Optional("1") String orderIndexParam) {
        System.out.println("\n>>> SWITCH ORDER CONTEXT <<<");

        int orderIndex;
        try {
            orderIndex = Integer.parseInt(orderIndexParam.trim());
        } catch (NumberFormatException e) {
            Assert.fail("❌ Invalid orderIndex parameter: '" + orderIndexParam + "'. Must be an integer.");
            return;
        }

        List<String> orderIds = RequestContext.getCurrentOrderIds();
        Assert.assertNotNull(orderIds, "❌ currentOrderIds is null — order creation step may not have run.");
        Assert.assertFalse(orderIds.isEmpty(), "❌ currentOrderIds is empty.");

        if (orderIndex >= orderIds.size()) {
            System.out.println("⚠️  orderIndex=" + orderIndex
                    + " is out of range (total orders=" + orderIds.size()
                    + "). Defaulting to last order (index " + (orderIds.size() - 1) + ").");
            orderIndex = orderIds.size() - 1;
        }

        String targetOrderId = orderIds.get(orderIndex);
        String previousOrderId = RequestContext.getCurrentOrderId();

        RequestContext.setCurrentOrderId(targetOrderId);

        // Also switch the active visit number to match the target order (used by COD_19 cancellation)
        String targetVisit = RequestContext.getVisitForOrder(targetOrderId);
        if (targetVisit != null) {
            RequestContext.setVisitNumber(targetVisit);
            System.out.println("   Switched visit number  : " + targetVisit);
        }

        System.out.println("   Previous active order : " + previousOrderId + " (index 0 = primary member)");
        System.out.println("   Switched to order     : " + targetOrderId + " (index " + orderIndex + ")");
        System.out.println("   Available orders      : " + orderIds);
        System.out.println("✅ Order context switched. COD-19/20 will now target order: " + targetOrderId);
    }
}

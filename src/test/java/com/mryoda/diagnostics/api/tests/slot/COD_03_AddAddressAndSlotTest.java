package com.mryoda.diagnostics.api.tests.slot;

import com.mryoda.diagnostics.api.tests.order.CreateOrderCODAPITest;

import com.mryoda.diagnostics.api.utils.RequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.Map;

public class COD_03_AddAddressAndSlotTest extends CreateOrderCODAPITest {

    private static final String CENTER_ID = "64870066842708a0d5ae6c77";

    @Test(description = "QA Automation: Verify Add Address And Slot")
    public void step03_AddAddressAndSlot() {
        System.out.println("\n>>> STEP 3: ADD ADDRESS & SLOT <<<");
        String token = RequestContext.getToken();
        String userId = RequestContext.getUserId();

        Map<String, String> addressDetails = callAddAddressAPI(token, userId);
        String addressId = addressDetails.get("id");
        String addressGuid = addressDetails.get("guid");
        Assert.assertNotNull(addressGuid, "Address GUID required");

        // Store Address Details - specifically Lat/Lng/Name for later
        RequestContext.storeLocationCoordinates("UserAddress", addressDetails.get("lat"), addressDetails.get("lng"));
        RequestContext.storeLocation("UserAddressName", addressDetails.get("name"));
        RequestContext.setCurrentAddressGuid(addressGuid);
        RequestContext.setCurrentAddressId(addressId);

        // Find Slot - Use lab slot search (lab visit flow)
        Map<String, String> slotDetails = findAvailableLabSlot(token, CENTER_ID);
        String slotGuid = slotDetails.get("guid");
        Assert.assertNotNull(slotGuid, "Slot GUID required");

        RequestContext.setCurrentSlotGuid(slotGuid);
        RequestContext.setExpectedSlotTiming(slotDetails.get("date"), slotDetails.get("time"));
        RequestContext.setExpectedAddressName(addressDetails.get("address"));

        // Sync slot date/time to fields read by COD_04
        RequestContext.setSlotStartDate(slotDetails.get("date"));
        RequestContext.setNonMemberSlotTime(slotDetails.get("time"));

        updateCartWithSlot(token, userId, slotGuid, addressGuid);
        System.out.println("✅ Address & Slot Configured (Lab Visit).");
    }
}

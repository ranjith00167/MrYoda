package com.mryoda.diagnostics.api.tests.slot;

import com.mryoda.diagnostics.api.tests.order.CreateOrderCODAPITest;
import com.mryoda.diagnostics.api.utils.RequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.Map;

public class COD_03_AddLabSlotTest extends CreateOrderCODAPITest {

    @Test(description = "QA Automation: Verify Add Lab Slot")
    public void step03_AddLabSlot() {
        System.out.println("\n>>> STEP 3: ADD LAB SLOT <<<");
        String token = RequestContext.getToken();
        String userId = RequestContext.getUserId();

        // Lab visit uses locationId as center_id
        String centerId = RequestContext.getSelectedLocationId();
        if (centerId == null || centerId.isEmpty()) {
            centerId = RequestContext.getLocationId(DEFAULT_LOCATION);
        }

        System.out.println("   Using Center ID (Location ID): " + centerId);

        // Find Lab Slot
        Map<String, String> slotDetails = findAvailableLabSlot(token, centerId);
        String slotGuid = slotDetails.get("guid");
        Assert.assertNotNull(slotGuid, "Lab Slot GUID required");

        RequestContext.setCurrentSlotGuid(slotGuid);
        RequestContext.setExpectedSlotTiming(slotDetails.get("date"), slotDetails.get("time"));

        // Sync slot date/time to fields read by COD_04
        RequestContext.setSlotStartDate(slotDetails.get("date"));
        RequestContext.setMemberSlotTime(slotDetails.get("time"));
        RequestContext.setCurrentAddressId(null); // Lab flow has no home address

        // Store Location Name for Validation
        String locationName = DEFAULT_LOCATION;
        if (RequestContext.getSelectedLocationId() != null) {
            for (Map.Entry<String, String> entry : RequestContext.getAllLocations().entrySet()) {
                if (entry.getValue().equals(centerId)) {
                    locationName = entry.getKey();
                    break;
                }
            }
        }
        RequestContext.setExpectedAddressName(locationName);

        // Update cart for lab visit (order_type: lab, address_id: null)
        updateCartWithLabSlot(token, userId, slotGuid, centerId);

        System.out.println("✅ Lab Slot Configured.");
    }
}

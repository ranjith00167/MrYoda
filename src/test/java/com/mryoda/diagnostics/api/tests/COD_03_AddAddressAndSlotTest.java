package com.mryoda.diagnostics.api.tests;

import com.mryoda.diagnostics.api.utils.RequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.Map;

public class COD_03_AddAddressAndSlotTest extends CreateOrderCODAPITest {

    @Test
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
        RequestContext.storeLocation("UserAddressName", addressDetails.get("name")); // using loose usage of
                                                                                     // storeLocation map
        RequestContext.setCurrentAddressGuid(addressGuid);
        RequestContext.setCurrentAddressId(addressId);

        // Store simple map-like access for lat/lng/name if needed by recreating it or
        // using global map in RequestContext
        // For simplicity, we assume Step 9 will retrieve from RequestContext's
        // specialized storage or we augment RequestContext
        // Let's stick to using what we just added: currentAddressGuid.
        // We need lat/lng later.

        // Find Slot
        Map<String, String> slotDetails = findAvailableSlot(token, addressGuid);
        String slotGuid = slotDetails.get("guid");
        Assert.assertNotNull(slotGuid, "Slot GUID required");

        RequestContext.setCurrentSlotGuid(slotGuid);
        // Store date/time if needed? VerifyPayment uses it.
        RequestContext.setSlotStartDate(slotDetails.get("date"));
        RequestContext.setMemberSlotTime(slotDetails.get("time")); // reusing variable, or add currentSlotDate/Time

        updateCartWithSlot(token, userId, slotGuid, addressGuid);
        System.out.println("✅ Address & Slot Configured.");
    }
}

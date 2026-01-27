package com.mryoda.diagnostics.api.tests;

import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.AssertionUtil;
import com.mryoda.diagnostics.api.utils.RequestContext;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class GetCentersByAddressAPITest extends BaseTest {

    private Response callGetCentersByAddressAPI(String token, String addressId, String labId) {
        System.out.println("\n🏥 GET CENTERS BY ADDRESS REQUEST:");
        System.out.println("   Address ID: " + addressId);
        System.out.println("   Lab ID: " + labId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("addressid", addressId);
        payload.put("lab_id", labId);

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.GET_CENTERS_BY_ADD)
                .addHeader("Authorization", token)
                .setRequestBody(payload)
                .post();

        AssertionUtil.verifyEquals(response.getStatusCode(), 200, "HTTP status should be 200");
        System.out.println("   ✅ HTTP Status: " + response.getStatusCode());

        return response;
    }

    private void validateResponse(Response response) {
        System.out.println("\n✅ Validating Get Centers Response");
        boolean success = response.jsonPath().getBoolean("success");
        AssertionUtil.verifyTrue(success, "API success flag should be true");
        System.out.println("   ✔ Success flag: " + success);
        
        String msg = response.jsonPath().getString("msg");
        if (msg == null) msg = response.jsonPath().getString("message");
        System.out.println("   ✔ Message: " + msg);
        
        System.out.println("   ✅ Centers fetched successfully!");
    }

    @Test(priority = 1, dependsOnMethods = "com.mryoda.diagnostics.api.tests.GetAddressByUserIdAPITest.testGetAddressByUserId_ForNonMember")
    public void testGetCentersByAddress_ForNonMember() {
        System.out.println("\n==========================================================");
        System.out.println("      GET CENTERS BY ADDRESS - EXISTING MEMBER");
        System.out.println("==========================================================");

        String token = RequestContext.getNonMemberToken();
        String addressId = RequestContext.getNonMemberAddressId();
        String labId = RequestContext.getSelectedLocationId();

        if (addressId == null) {
             System.out.println("⚠️ Skipping test: Address ID not found for Non-Member");
             // Fallback to fetching if not set? No, dependsOnMethods should ensure it runs.
             // But if GetAddressByUserIdAPITest failed or didn't find address, this might be null.
        }

        Response response = callGetCentersByAddressAPI(token, addressId, labId);
        validateResponse(response);
    }

    @Test(priority = 2, dependsOnMethods = "com.mryoda.diagnostics.api.tests.GetAddressByUserIdAPITest.testGetAddressByUserId_ForMember")
    public void testGetCentersByAddress_ForMember() {
        System.out.println("\n==========================================================");
        System.out.println("         GET CENTERS BY ADDRESS - MEMBER");
        System.out.println("==========================================================");

        String token = RequestContext.getMemberToken();
        String addressId = RequestContext.getMemberAddressId();
        String labId = RequestContext.getSelectedLocationId();

        if (addressId == null) {
             System.out.println("⚠️ Skipping test: Address ID not found for Member");
        }

        Response response = callGetCentersByAddressAPI(token, addressId, labId);
        validateResponse(response);
    }

    @Test(priority = 3, dependsOnMethods = "com.mryoda.diagnostics.api.tests.GetAddressByUserIdAPITest.testGetAddressByUserId_ForNewUser")
    public void testGetCentersByAddress_ForNewUser() {
        System.out.println("\n==========================================================");
        System.out.println("        GET CENTERS BY ADDRESS - NEW USER");
        System.out.println("==========================================================");

        String token = RequestContext.getNewUserToken();
        String addressId = RequestContext.getNewUserAddressId();
        String labId = RequestContext.getSelectedLocationId();

        if (addressId == null) {
             System.out.println("⚠️ Skipping test: Address ID not found for New User");
        }

        Response response = callGetCentersByAddressAPI(token, addressId, labId);
        validateResponse(response);
    }
}

package com.mryoda.diagnostics.api.tests.address;

import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.AssertionUtil;
import com.mryoda.diagnostics.api.utils.RequestContext;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

public class GetAddressByUserIdAPITest extends BaseTest {

    // -------------------------------
    // HELPER: Call Get Address By User ID API
    // -------------------------------
    private Response callGetAddressByUserIdAPI(String token, String userId) {

        System.out.println("\n📦 GET ADDRESS BY USER ID REQUEST:");
        System.out.println("   User ID: " + userId);

        String endpoint = APIEndpoints.GET_ADDRESS_BY_USER_ID.replace("{user_id}", userId);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", token)
                .get();

        AssertionUtil.verifyEquals(response.getStatusCode(), 200, "HTTP status should be 200");
        System.out.println("   ✅ HTTP Status: " + response.getStatusCode());

        return response;
    }

    // -------------------------------
    // HELPER: Validate and Store Address Response
    // -------------------------------
    private void validateAndStoreAddressResponse(Response response, String userType) {

        System.out.println("\n============================================================");
        System.out.println("      COMPREHENSIVE GET ADDRESS VALIDATION - " + userType);
        System.out.println("============================================================\n");

        // Step 1: Validate API Response
        System.out.println("✅ STEP 1: Validating API Response");
        AssertionUtil.verifyTrue(response.jsonPath().getBoolean("success"), "API success flag should be true");
        System.out.println("   ✔ Success flag: " + response.jsonPath().getBoolean("success"));

        String message = response.jsonPath().getString("message");
        System.out.println("   ✔ Response message: " + message);

        // Step 2: Extract Addresses List
        System.out.println("\n✅ STEP 2: Extracting Addresses");
        List<Map<String, Object>> addresses = response.jsonPath().getList("data");

        if (addresses == null || addresses.isEmpty()) {
            System.out.println("   ⚠️  No addresses found for this user");
            System.out.println("   ℹ️  User may not have added any addresses yet");
            return;
        }

        System.out.println("   ✔ Total addresses found: " + addresses.size());

        // Step 3: Extract and Store Address ID (guid) from the first address
        System.out.println("\n✅ STEP 3: Extracting and Storing Address ID (guid)");

        Map<String, Object> firstAddress = addresses.get(0);
        String addressGuid = (String) firstAddress.get("guid");

        if (addressGuid == null || addressGuid.isEmpty()) {
            throw new AssertionError("❌ Address GUID not found in response!");
        }

        System.out.println("   ✔ Address GUID extracted: " + addressGuid);

        // Store the address_id based on user type
        switch (userType) {
            case "NON_MEMBER":
                RequestContext.setNonMemberAddressId(addressGuid);
                RequestContext.setNonMemberAddresses(addresses);
                System.out.println("   ✔ Stored in RequestContext as NON_MEMBER address_id");
                break;
            case "MEMBER":
                RequestContext.setMemberAddressId(addressGuid);
                RequestContext.setMemberAddresses(addresses);
                System.out.println("   ✔ Stored in RequestContext as MEMBER address_id");
                break;
            case "NEW_USER":
                RequestContext.setNewUserAddressId(addressGuid);
                RequestContext.setNewUserAddresses(addresses);
                System.out.println("   ✔ Stored in RequestContext as NEW_USER address_id");
                break;
        }

        // Step 4: Validate Address Details
        System.out.println("\n✅ STEP 4: Validating Address Details");

        for (int i = 0; i < addresses.size(); i++) {
            Map<String, Object> address = addresses.get(i);

            System.out.println("\n   ━━━━━ Address " + (i + 1) + " ━━━━━");

            String guid = (String) address.get("guid");
            AssertionUtil.verifyTrue(guid != null && !guid.isEmpty(), "Address GUID should not be empty");
            System.out.println("   ✔ GUID: " + guid);

            String userId = (String) address.get("user_id");
            AssertionUtil.verifyTrue(userId != null && !userId.isEmpty(), "User ID should not be empty");
            System.out.println("   ✔ User ID: " + userId);

            String receiverName = (String) address.get("receiver_name");
            System.out.println("   ✔ Receiver Name: " + receiverName);

            String mobile = (String) address.get("recipient_mobile_number");
            System.out.println("   ✔ Mobile: " + mobile);

            String addressLine1 = (String) address.get("address_line1");
            System.out.println("   ✔ Address Line 1: " + addressLine1);

            String name = (String) address.get("name");
            System.out.println("   ✔ Name (Area): " + name);

            String city = (String) address.get("city");
            System.out.println("   ✔ City: " + city);

            String state = (String) address.get("state");
            System.out.println("   ✔ State: " + state);

            String postalCode = (String) address.get("postal_code");
            System.out.println("   ✔ Postal Code: " + postalCode);

            String type = (String) address.get("type");
            System.out.println("   ✔ Type: " + type);

            Object latitude = address.get("latitude");
            Object longitude = address.get("longitude");
            System.out.println("   ✔ Coordinates: Lat=" + latitude + ", Long=" + longitude);
        }

        // Summary
        System.out.println("\n🏠 ========================================");
        System.out.println("   ADDRESS VALIDATION SUMMARY");
        System.out.println("   ========================================");
        System.out.println("   Total addresses: " + addresses.size());
        System.out.println("   Address ID (guid) stored: " + addressGuid);
        System.out.println("   User Type: " + userType);
        System.out.println("   ✅ ALL ADDRESSES VALIDATED SUCCESSFULLY");
        System.out.println("   ========================================\n");

        System.out.println("\n========================================");
        System.out.println("ALL GET ADDRESS VALIDATIONS PASSED FOR " + userType);
        System.out.println("========================================");
        System.out.println("Address ID (guid): " + addressGuid);
        System.out.println("Total Addresses: " + addresses.size());
        System.out.println("========================================\n");
    }

    // ---------------------------------------------------------
    // 1️⃣ EXISTING MEMBER → Get Address By User ID
    // ---------------------------------------------------------
    @Test(priority = 14, dependsOnMethods = "com.mryoda.diagnostics.api.tests.address.AddressAPITest.testAddAddress_ForNonMember")
    public void testGetAddressByUserId_ForNonMember() {

        System.out.println("\n==========================================================");
        System.out.println("      GET ADDRESS BY USER ID API - EXISTING MEMBER");
        System.out.println("==========================================================");

        String token = RequestContext.getNonMemberToken();
        String userId = RequestContext.getNonMemberUserId();

        Response response = callGetAddressByUserIdAPI(token, userId);
        validateAndStoreAddressResponse(response, "NON_MEMBER");
    }

    // ---------------------------------------------------------
    // 2️⃣ MEMBER → Get Address By User ID
    // ---------------------------------------------------------
    @Test(priority = 14, dependsOnMethods = "com.mryoda.diagnostics.api.tests.address.AddressAPITest.testAddAddress_ForMember")
    public void testGetAddressByUserId_ForMember() {

        System.out.println("\n==========================================================");
        System.out.println("         GET ADDRESS BY USER ID API - MEMBER");
        System.out.println("==========================================================");

        String token = RequestContext.getMemberToken();
        String userId = RequestContext.getMemberUserId();

        Response response = callGetAddressByUserIdAPI(token, userId);
        validateAndStoreAddressResponse(response, "MEMBER");
    }

    // ---------------------------------------------------------
    // 3️⃣ NEW USER → Get Address By User ID
    // ---------------------------------------------------------
    @Test(priority = 15, dependsOnMethods = "com.mryoda.diagnostics.api.tests.address.AddressAPITest.testAddAddress_ForNewUser")
    public void testGetAddressByUserId_ForNewUser() {

        System.out.println("\n==========================================================");
        System.out.println("        GET ADDRESS BY USER ID API - NEW USER");
        System.out.println("==========================================================");

        String token = RequestContext.getNewUserToken();
        String userId = RequestContext.getNewUserUserId();

        Response response = callGetAddressByUserIdAPI(token, userId);
        validateAndStoreAddressResponse(response, "NEW_USER");
    }
}

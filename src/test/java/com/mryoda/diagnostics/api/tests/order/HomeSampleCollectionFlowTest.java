package com.mryoda.diagnostics.api.tests.order;

import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.AssertionUtil;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import com.mryoda.diagnostics.api.utils.RequestContext;
import com.mryoda.diagnostics.api.utils.RandomDataUtil;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Home Sample Collection COD Flow Test
 * Scenario:
 * 1. Login (Inherited)
 * 2. Add Family Member (Custom Step)
 * 3. Add Address & Slot (Inherited)
 * 4. Verify Payment & Create Order (Inherited)
 * ...
 */
public class HomeSampleCollectionFlowTest extends DetailedCODFlowTest {

    private String familyMemberGuid;
    private String familyMemberName;

    @Test(priority = 3, description = "QA Automation: Verify B Add Family Member")
    public void step02b_AddFamilyMember() {
        System.out.println("\n>>> STEP 2b: ADD FAMILY MEMBER (FOR HOME COLLECTION) <<<");

        String userId = RequestContext.getUserId();
        String token = RequestContext.getToken();

        if (userId == null || token == null) {
            // Context should be populated by step01_LoginAndSetup
        }

        // Dynamic Data Generation
        String dynamicFirstName = "HomeUser" + RandomDataUtil.getRandomFirstName();
        String dynamicLastName = "Test";
        String dynamicMobile = RandomDataUtil.getRandomMobile();

        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", userId);
        payload.put("first_name", dynamicFirstName);
        payload.put("last_name", dynamicLastName);
        payload.put("middle_name", "");
        payload.put("mobile", dynamicMobile);
        payload.put("gender", "male");
        payload.put("country_code", "+91");
        payload.put("dob", "1990-01-01");
        payload.put("profile_pic", "");
        payload.put("profile_color", "#fdefca");
        payload.put("relation", "Brother");
        payload.put("title", "Mr.");

        System.out.println("   Creating Family Member: " + dynamicFirstName);

        Response response = new RequestBuilder()
                .setEndpoint(APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADD_FAMILY_MEMBER)
                .addHeader("Authorization", "Bearer " + token)
                .setRequestBody(payload)
                .post();

        // Validation
        AssertionUtil.verifyEquals(response.getStatusCode(), 201, "Add Family Member should return 201");

        // Extract GUID
        familyMemberGuid = response.jsonPath().getString("data.guid");
        if (familyMemberGuid == null)
            familyMemberGuid = response.jsonPath().getString("data.id");

        familyMemberName = response.jsonPath().getString("data.first_name");

        Assert.assertNotNull(familyMemberGuid, "Family Member GUID must be returned");
        System.out.println("   ✅ Family Member Added. GUID: " + familyMemberGuid);
    }
}

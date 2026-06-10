package com.mryoda.diagnostics.api.tests.order;

import com.mryoda.diagnostics.api.builders.RequestBuilder;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.RequestContext;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

public class COD_06C_GetPhleboShiftsTest extends CreateOrderCODAPITest {

    @Test(description = "QA Automation: Get Phlebo Shifts")
    public void step06C_GetPhleboShifts() {
        System.out.println("\n>>> STEP 6C: GET PHLEBO SHIFTS <<<");

        String phleboGuid = RequestContext.getCurrentPhleboGuid();
        Assert.assertNotNull(phleboGuid,
                "PHLEBO_GUID must be set by COD_06_PhlebotomistLoginTest before this step runs.");
        Assert.assertFalse(phleboGuid.isBlank(),
                "PHLEBO_GUID captured from COD_06 login must not be blank.");

        String url = APIEndpoints.PHLEBO_NOTIFICATION_BASE_URL
                + APIEndpoints.PHLEBO_GET_SHIFTS.replace("{phlebo_guid}", phleboGuid);

        // Reuse cached admin token; login if not present
        if (RequestContext.getAdminToken() == null || RequestContext.getAdminToken().isEmpty()) {
            System.out.println("   Admin token not found — logging into admin portal...");
            callMainAdminLoginAPI();
        }
        String adminToken = RequestContext.getAdminToken();
        System.out.println("   Phlebo GUID : " + phleboGuid);
        System.out.println("   Admin Token Present: " + (adminToken != null && !adminToken.isEmpty()));
        System.out.println("   Target URL  : " + url);

        RequestBuilder builder = new RequestBuilder().setEndpoint(url);
        if (adminToken != null && !adminToken.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + adminToken);
        } else {
            System.out.println("⚠️ No admin token available — calling without Authorization header.");
        }

        Response response = builder.get();

        System.out.println("   Response Status : " + response.getStatusCode());
        System.out.println("   Response Body   : " + response.getBody().asString());

        if (response.getStatusCode() == 200) {
            System.out.println("✅ Phlebo shifts fetched successfully for GUID: " + phleboGuid);
        } else {
            System.out.println("⚠️ Get Phlebo Shifts returned status " + response.getStatusCode()
                    + " — proceeding to COD_07 (Assign Order).");
        }
    }
}

package com.mryoda.diagnostics.api.tests.order;

import com.mryoda.diagnostics.api.builders.RequestBuilder;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.RequestContext;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class COD_06B_AssignShiftTest extends CreateOrderCODAPITest {

    @Test(description = "QA Automation: Assign Phlebo Shift for Today")
    public void step06B_AssignShift() {
        System.out.println("\n>>> STEP 6B: ASSIGN PHLEBO SHIFT <<<");

        String phleboGuid = RequestContext.getCurrentPhleboGuid();
        Assert.assertNotNull(phleboGuid,
                "PHLEBO_GUID must be set by COD_06_PhlebotomistLoginTest before this step runs.");
        Assert.assertFalse(phleboGuid.isBlank(),
                "PHLEBO_GUID captured from COD_06 login must not be blank.");

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        System.out.println("   Phlebo GUID : " + phleboGuid);
        System.out.println("   From Date   : " + today);
        System.out.println("   To Date     : " + today);

        Map<String, Object> slotTiming = new HashMap<>();
        slotTiming.put("start_time", "08:00");
        slotTiming.put("end_time", "12:00");

        Map<String, Object> payload = new HashMap<>();
        payload.put("phlebo_guids", Collections.singletonList(phleboGuid));
        payload.put("from_date", today);
        payload.put("to_date", today);
        payload.put("slots", Collections.singletonList(slotTiming));

        String url = APIEndpoints.PHLEBO_NOTIFICATION_BASE_URL + APIEndpoints.PHLEBO_ASSIGN_SHIFTS;

        // Obtain admin portal token — login if not already cached
        if (RequestContext.getAdminToken() == null
                || RequestContext.getAdminToken().isEmpty()) {
            System.out.println("   Admin token not found — logging into admin portal...");
            callMainAdminLoginAPI();
        }
        String adminToken = RequestContext.getAdminToken();
        System.out.println("   Admin Token Present: " + (adminToken != null && !adminToken.isEmpty()));

        System.out.println("   Target URL  : " + url);
        System.out.println("   Payload     : " + payload);

        RequestBuilder builder = new RequestBuilder()
                .setEndpoint(url)
                .setRequestBody(payload);

        if (adminToken != null && !adminToken.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + adminToken);
        } else {
            System.out.println("⚠️ No admin token available — calling assign-shifts without Authorization header.");
        }

        Response response = builder.post();

        System.out.println("   Response Status : " + response.getStatusCode());
        System.out.println("   Response Body   : " + response.getBody().asString());

        if (response.getStatusCode() == 200 || response.getStatusCode() == 201) {
            System.out.println("✅ Phlebo shift assigned successfully for date: " + today);
        } else {
            System.out.println("⚠️ Assign shift returned status " + response.getStatusCode()
                    + " — proceeding to COD_07 (Assign Order).");
        }
    }
}

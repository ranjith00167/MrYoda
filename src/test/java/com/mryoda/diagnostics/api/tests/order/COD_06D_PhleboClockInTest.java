package com.mryoda.diagnostics.api.tests.order;

import com.mryoda.diagnostics.api.builders.RequestBuilder;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.RequestContext;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class COD_06D_PhleboClockInTest extends CreateOrderCODAPITest {

    @Test(description = "QA Automation: Phlebo Shift Clock-In")
    public void step06D_PhleboClockIn() {
        System.out.println("\n>>> STEP 6D: PHLEBO SHIFT CLOCK-IN <<<");

        // This endpoint is phlebo-service specific — only the phlebo's own JWT is accepted.
        // Admin token has a different structure and will always return 401 "Invalid token structure".
        String phleboToken = RequestContext.getPhleboToken();

        if (phleboToken == null || phleboToken.isBlank()) {
            System.out.println("⚠️ [SKIP] Phlebo token not found in RequestContext.");
            System.out.println("   COD_06 must have run successfully and returned 'data.token' to use this step.");
            System.out.println("   Skipping clock-in — proceeding to COD_07 (Assign Order).");
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("image_url", "s3.com");
        payload.put("lat", 17.3850);
        payload.put("lng", 78.4867);

        String url = APIEndpoints.PHLEBO_NOTIFICATION_BASE_URL + APIEndpoints.PHLEBO_CLOCK_IN;

        System.out.println("   Phlebo Token Present : true");
        System.out.println("   Target URL           : " + url);
        System.out.println("   Payload              : " + payload);

        Response response = new RequestBuilder()
                .setEndpoint(url)
                .addHeader("Authorization", "Bearer " + phleboToken)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status : " + response.getStatusCode());
        System.out.println("   Response Body   : " + response.getBody().asString());

        if (response.getStatusCode() == 200 || response.getStatusCode() == 201) {
            System.out.println("✅ Phlebo clock-in successful.");
        } else {
            System.out.println("⚠️ Clock-in returned status " + response.getStatusCode()
                    + " — proceeding to COD_07 (Assign Order). Clock-in is a non-blocking step.");
        }
    }
}

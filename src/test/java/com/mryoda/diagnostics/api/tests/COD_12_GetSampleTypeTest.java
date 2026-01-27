package com.mryoda.diagnostics.api.tests;

import com.mryoda.diagnostics.api.utils.RequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;

public class COD_12_GetSampleTypeTest extends CreateOrderCODAPITest {

    @Test
    public void step12_GetSampleType() {
        System.out.println("\n>>> STEP 12: GET SAMPLE TYPE <<<");
        String token = RequestContext.getToken();
        String sampleType = callGetSampleTypeAPI(token);

        Assert.assertNotNull(sampleType, "Sample Type must be retrieved");
        System.out.println("✅ Sample Type Retrieved: " + sampleType);

        RequestContext.setCurrentSampleType(sampleType);
    }
}

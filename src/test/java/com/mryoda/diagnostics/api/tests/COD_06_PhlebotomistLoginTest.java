package com.mryoda.diagnostics.api.tests;

import com.mryoda.diagnostics.api.utils.RequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;

public class COD_06_PhlebotomistLoginTest extends CreateOrderCODAPITest {

    @Test
    public void step06_PhlebotomistLogin() {
        System.out.println("\n>>> STEP 6: PHLEBOTOMIST LOGIN <<<");
        String phlebotomistGuid = callPhlebotomistLoginAPI();
        Assert.assertNotNull(phlebotomistGuid, "Phlebotomist GUID required for assignment");
        RequestContext.setCurrentPhleboGuid(phlebotomistGuid);
    }
}

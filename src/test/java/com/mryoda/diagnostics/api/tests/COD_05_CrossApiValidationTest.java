package com.mryoda.diagnostics.api.tests;

import com.mryoda.diagnostics.api.utils.RequestContext;
import io.restassured.response.Response;
import org.testng.annotations.Test;
import java.util.List;
import java.util.Map;

public class COD_05_CrossApiValidationTest extends CreateOrderCODAPITest {

    @Test
    public void step05_CrossApiValidation() {
        System.out.println("\n>>> STEP 5: CROSS-API VALIDATION <<<");
        String token = RequestContext.getToken();
        String userId = RequestContext.getUserId();
        String paymentId = RequestContext.getCurrentPaymentId();
        String cartId = RequestContext.getCurrentCartId();
        int totalPrice = RequestContext.getCurrentTotalPrice();
        String addressId = RequestContext.getCurrentAddressId();
        String slotGuid = RequestContext.getCurrentSlotGuid();

        Response paymentResponse = callGetPaymentByIdAPI(token, paymentId);
        Response cartResponse = callGetCartAPI(token, userId);

        // Extract Product Names for validation
        List<String> expectedProductNames = new java.util.ArrayList<>();
        Object dataObj = cartResponse.jsonPath().get("data");
        String dataPath = (dataObj instanceof java.util.List) ? "data[0]" : "data";
        List<Map<String, Object>> cartProducts = cartResponse.jsonPath().getList(dataPath + ".product_details");
        if (cartProducts != null) {
            for (Map<String, Object> prod : cartProducts) {
                String pName = (String) prod.get("product_name");
                if (pName != null)
                    expectedProductNames.add(pName);
            }
        }

        performCrossAPIValidations(cartResponse, paymentResponse, totalPrice, cartId, paymentId, userId,
                addressId, slotGuid, expectedProductNames);
    }
}

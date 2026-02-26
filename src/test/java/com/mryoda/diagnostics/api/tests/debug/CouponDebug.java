package com.mryoda.diagnostics.api.tests.debug;

import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class CouponDebug {
    
    @Test
    public void debugNonPrimeCoupons() {
        System.out.println("\n>>> DEBUG: GetAllCoupons for nonPrime <<<");
        Map<String, String> payload = new HashMap<>();
        payload.put("coupon_user_type", "nonPrime");

        Response response = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .contentType(ContentType.JSON)
                .body(payload)
                .post(APIEndpoints.GET_ALL_COUPONS);

        System.out.println("Status Code: " + response.getStatusCode());
        System.out.println("Response Body: " + response.getBody().asString());
        
        System.out.println("\n>>> DEBUG: GetAllCoupons for prime <<<");
        payload.put("coupon_user_type", "prime");
        response = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .contentType(ContentType.JSON)
                .body(payload)
                .post(APIEndpoints.GET_ALL_COUPONS);
        System.out.println("Status Code: " + response.getStatusCode());
        System.out.println("Response Body: " + response.getBody().asString());
    }
}

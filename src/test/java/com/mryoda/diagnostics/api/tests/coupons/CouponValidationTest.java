package com.mryoda.diagnostics.api.tests.coupons;

import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import com.mryoda.diagnostics.api.utils.RequestContext;
import org.testng.Assert;

import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CouponValidationTest extends BaseTest {

    @Test(priority = 1)
    public void testGetAllCoupons_Prime() {
        System.out.println("\n>>> TESTS: Validate GetAllCoupons - Prime <<<");

        Map<String, String> payload = new HashMap<>();
        payload.put("coupon_user_type", "prime");

        Response response = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .contentType(ContentType.JSON)
                .body(payload)
                .post(APIEndpoints.GET_ALL_COUPONS);

        System.out.println("Response Status: " + response.getStatusCode());
        RequestContext.storeApiPerformance(APIEndpoints.GET_ALL_COUPONS, response.getTime());

        Assert.assertEquals(response.getStatusCode(), 200, "Expected status code 200 for Prime coupons");

        Object data = response.jsonPath().get("data");
        if (data instanceof List) {
            List<Map<String, Object>> coupons = (List<Map<String, Object>>) data;
            System.out.println("[SUCCESS] Coupons found: " + coupons.size());
            Assert.assertFalse(coupons.isEmpty(), "Coupons list should not be empty for Prime");

            // Validate fields for each coupon
            for (Map<String, Object> coupon : coupons) {
                System.out.println("Processing Coupon: " + coupon); // Debugging
                Assert.assertNotNull(coupon.get("code"), "Coupon code should not be null");
                Assert.assertNotNull(coupon.get("guid"), "Coupon ID (guid) should not be null");

                // Validate user type if present (TC_CPN_001)
                Object userType = coupon.get("coupon_user_type");
                if (userType != null) {
                    Assert.assertEquals(userType.toString().toLowerCase(), "prime",
                            "Only Prime coupons should be returned");
                }
            }
        } else {
            Assert.fail("Data field is not a list!");
        }
    }

    @Test(priority = 2)
    public void testGetAllCoupons_NonPrime() {
        System.out.println("\n>>> TESTS: Validate GetAllCoupons - NonPrime <<<");

        Map<String, String> payload = new HashMap<>();
        payload.put("coupon_user_type", "nonPrime");

        Response response = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .contentType(ContentType.JSON)
                .body(payload)
                .post(APIEndpoints.GET_ALL_COUPONS);

        System.out.println("Response Status: " + response.getStatusCode());
        RequestContext.storeApiPerformance(APIEndpoints.GET_ALL_COUPONS, response.getTime());

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected status code 200 for NonPrime coupons. Response: " + response.getBody().asString());

        Object data = response.jsonPath().get("data");
        if (data instanceof List) {
            List<Map<String, Object>> coupons = (List<Map<String, Object>>) data;
            System.out.println("[SUCCESS] Coupons found: " + coupons.size());
            Assert.assertFalse(coupons.isEmpty(), "Coupons list should not be empty for NonPrime");

            for (Map<String, Object> coupon : coupons) {
                Object userType = coupon.get("coupon_user_type");
                if (userType != null) {
                    Assert.assertEquals(userType.toString().toLowerCase(), "nonprime",
                            "Only NonPrime coupons should be returned");
                }
            }
        }
    }

    // --- NEGATIVE SCENARIOS ---

    @Test(priority = 3)
    public void testGetAllCoupons_InvalidUserType() {
        System.out.println("\n>>> TESTS: Validate GetAllCoupons - Invalid User Type <<<");
        Map<String, String> payload = new HashMap<>();
        payload.put("coupon_user_type", "invalid_user_type_xyz");

        Response response = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .contentType(ContentType.JSON)
                .body(payload)
                .post(APIEndpoints.GET_ALL_COUPONS);

        System.out.println("Status: " + response.getStatusCode());
        if (response.getStatusCode() != 200) {
            System.out.println("Response: " + response.getBody().asString());
        }
        RequestContext.storeApiPerformance(APIEndpoints.GET_ALL_COUPONS, response.getTime());

        if (response.getStatusCode() == 200) {
            Object data = response.jsonPath().get("data");
            List<?> list = (List<?>) data;
            Assert.assertTrue(list == null || list.isEmpty(), "Data should be empty for invalid user type");
        } else {
            Assert.assertTrue(response.getStatusCode() >= 400, "Expected error status for invalid input");
        }
    }

    @Test(priority = 4)
    public void testGetAllCoupons_EmptyUserType() {
        System.out.println("\n>>> TESTS: Validate GetAllCoupons - Empty User Type <<<");
        Map<String, String> payload = new HashMap<>();
        payload.put("coupon_user_type", "");

        Response response = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .contentType(ContentType.JSON)
                .body(payload)
                .post(APIEndpoints.GET_ALL_COUPONS);

        System.out.println("Status: " + response.getStatusCode());
        if (response.getStatusCode() != 200) {
            System.out.println("Response: " + response.getBody().asString());
        }
        RequestContext.storeApiPerformance(APIEndpoints.GET_ALL_COUPONS, response.getTime());
        Assert.assertNotEquals(response.getStatusCode(), 200, "Should not return success for empty user type");
    }
}

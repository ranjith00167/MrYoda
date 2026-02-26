package com.mryoda.diagnostics.api.debug;

import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.RequestContext;
import com.mryoda.diagnostics.api.utils.TokenManager;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DebugCartJsonTest extends BaseTest {

    @Test
    public void debugCart() {
        // Setup a minimal context
        String mobile = "8220220227"; // Use a known mobile
        String token = TokenManager.generateToken(mobile, TokenManager.NON_MEMBER);
        String userId = RequestContext.getNonMemberUserId();

        System.out.println("User ID: " + userId);

        // Fetch coupons
        Map<String, String> payload = new HashMap<>();
        payload.put("coupon_user_type", "nonPrime");
        Response response = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .contentType(ContentType.JSON)
                .body(payload)
                .post(APIEndpoints.GET_ALL_COUPONS);

        List<Map<String, Object>> coupons = response.jsonPath().getList("data");
        if (coupons == null || coupons.isEmpty()) {
            System.out.println("No coupons found");
            return;
        }
        String couponGuid = (String) coupons.get(0).get("guid");

        // Fetch a product
        Map<String, Object> searchBody = new HashMap<>();
        searchBody.put("page", 1);
        searchBody.put("limit", 1);
        searchBody.put("search_string", "Glucose");
        searchBody.put("location", "676a5fa720093d2807af03a5");
        Response searchRes = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(searchBody)
                .post(APIEndpoints.GLOBAL_SEARCH);

        String productId = searchRes.jsonPath().getString("data.docs[0]._id");
        String brandId = searchRes.jsonPath().getString("data.docs[0].brand_id");
        if (brandId == null)
            brandId = "676a5fa720093d2807af03a5"; // fallback

        // Add to cart for 2 people (imaginary IDs for debug)
        List<String> memberIds = new ArrayList<>();
        memberIds.add(userId);
        memberIds.add("676a5fa720093d2807af03a6"); // imaginary second member

        Map<String, Object> cartPayload = new HashMap<>();
        cartPayload.put("user_id", userId);
        cartPayload.put("lab_location_id", "676a5fa720093d2807af03a5");
        cartPayload.put("order_type", "lab");
        cartPayload.put("coupon_guid", couponGuid);

        List<Map<String, Object>> products = new ArrayList<>();
        Map<String, Object> p = new HashMap<>();
        p.put("product_id", productId);
        p.put("quantity", 2);
        p.put("type", "lab");
        p.put("brand_id", brandId);
        p.put("location_id", "676a5fa720093d2807af03a5");
        p.put("family_member_id", memberIds);
        products.add(p);
        cartPayload.put("product_details", products);

        Response addRes = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(cartPayload)
                .post(APIEndpoints.ADD_TO_CART);

        System.out.println("Add Cart Response: " + addRes.getBody().asString());

        Response getCart = RestAssured.given()
                .baseUri(APIEndpoints.DIAGNOSTICS_BASE_URL)
                .header("Authorization", "Bearer " + token)
                .pathParam("user_id", userId)
                .queryParam("order_type", "lab")
                .queryParam("location", "676a5fa720093d2807af03a5")
                .get(APIEndpoints.GET_CART_BY_ID);

        System.out.println("Get Cart JSON:\n" + getCart.getBody().asPrettyString());
    }
}

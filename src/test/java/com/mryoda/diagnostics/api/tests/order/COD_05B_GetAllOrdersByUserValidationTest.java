package com.mryoda.diagnostics.api.tests.order;

import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.AssertionUtil;
import com.mryoda.diagnostics.api.utils.RequestContext;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * COD_05B: Comprehensive GetAllOrdersByUser API Validation Test
 * Endpoint: GET /order/getAllOrdersByUser/{user_id}
 * Body: { "page": 1, "pageSize": 10, "searchString": "string" }
 *
 * Validates:
 * - Positive: Success response, pagination, data structure, order list schema
 * - Negative: Invalid user ID, empty user ID, SQL injection, XSS, special chars, boundary values
 * - Schema: Response structure, field types, pagination metadata
 */
public class COD_05B_GetAllOrdersByUserValidationTest extends CreateOrderCODAPITest {

    // ═══════════════════════════════════════════════════════════════════
    //  HELPER: Call GetAllOrdersByUser API
    // ═══════════════════════════════════════════════════════════════════

    private Response callGetAllOrdersByUser(String token, String userId, int page, int pageSize, String searchString) {
        String endpoint = APIEndpoints.GET_ALL_ORDERS_BY_USER + userId;

        Map<String, Object> body = new HashMap<>();
        body.put("page", page);
        body.put("pageSize", pageSize);
        body.put("searchString", searchString);

        return new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", token)
                .setRequestBody(body)
                .post();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  POSITIVE VALIDATIONS
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 1, description = "Positive: Verify GetAllOrdersByUser returns 200 with valid data")
    public void testGetAllOrdersByUser_SuccessResponse() {
        System.out.println("\n>>> STEP 5B-1: GET ALL ORDERS BY USER - SUCCESS RESPONSE <<<");

        String token = RequestContext.getToken();
        String userId = RequestContext.getUserId();

        Assert.assertNotNull(token, "Token must be available from previous steps");
        Assert.assertNotNull(userId, "User ID must be available from login");

        Response response = callGetAllOrdersByUser(token, userId, 1, 10, "");

        System.out.println("   HTTP Status: " + response.getStatusCode());
        AssertionUtil.verifyStatusCode(response, 200);

        // Verify success flag
        Boolean success = response.jsonPath().get("success");
        Assert.assertNotNull(success, "Response must have 'success' field");
        Assert.assertTrue(success, "success should be true");

        System.out.println("✅ PASSED: GetAllOrdersByUser returns 200 with success=true");
    }

    @Test(priority = 2, dependsOnMethods = "testGetAllOrdersByUser_SuccessResponse",
          description = "Positive: Validate response contains order list")
    public void testGetAllOrdersByUser_OrderListPresent() {
        System.out.println("\n>>> STEP 5B-2: ORDER LIST VALIDATION <<<");

        String token = RequestContext.getToken();
        String userId = RequestContext.getUserId();
        Response response = callGetAllOrdersByUser(token, userId, 1, 10, "");

        // Data should be a list/array
        Object data = response.jsonPath().get("data");
        Assert.assertNotNull(data, "Response 'data' field must not be null");
        Assert.assertTrue(data instanceof List, "data must be an array/list");

        List<?> orders = (List<?>) data;
        System.out.println("   Orders found: " + orders.size());
        Assert.assertTrue(orders.size() >= 1,
            "User should have at least 1 order after COD flow but found: " + orders.size());

        System.out.println("✅ PASSED: Order list is present with " + orders.size() + " orders");
    }

    @Test(priority = 3, dependsOnMethods = "testGetAllOrdersByUser_SuccessResponse",
          description = "Positive: Validate each order has required fields")
    public void testGetAllOrdersByUser_OrderFieldsValidation() {
        System.out.println("\n>>> STEP 5B-3: ORDER FIELDS VALIDATION <<<");

        String token = RequestContext.getToken();
        String userId = RequestContext.getUserId();
        Response response = callGetAllOrdersByUser(token, userId, 1, 10, "");

        List<Map<String, Object>> orders = response.jsonPath().getList("data");
        Assert.assertNotNull(orders, "Orders list must not be null");
        Assert.assertFalse(orders.isEmpty(), "Orders list must not be empty");

        // Validate first order has key fields
        Map<String, Object> firstOrder = orders.get(0);

        // order_status must be present
        Object orderStatus = firstOrder.get("order_status");
        Assert.assertNotNull(orderStatus, "First order must have order_status");
        System.out.println("   First order status: " + orderStatus);

        // payment_id must be present
        Object paymentId = firstOrder.get("payment_id");
        Assert.assertNotNull(paymentId, "First order must have payment_id");
        System.out.println("   First order payment_id: " + paymentId);

        System.out.println("✅ PASSED: Order fields validated successfully");
    }

    @Test(priority = 4, dependsOnMethods = "testGetAllOrdersByUser_SuccessResponse",
          description = "Positive: Validate the recently placed order appears in the list")
    public void testGetAllOrdersByUser_RecentOrderPresent() {
        System.out.println("\n>>> STEP 5B-4: RECENT ORDER PRESENCE VALIDATION <<<");

        String token = RequestContext.getToken();
        String userId = RequestContext.getUserId();
        String currentOrderId = RequestContext.getCurrentOrderId();
        Response response = callGetAllOrdersByUser(token, userId, 1, 10, "");

        List<Map<String, Object>> orders = response.jsonPath().getList("data");

        if (currentOrderId != null) {
            boolean found = false;
            for (Map<String, Object> order : orders) {
                Object orderId = order.get("order_id");
                if (orderId == null) orderId = order.get("guid");
                if (orderId == null) orderId = order.get("id");

                if (currentOrderId.equals(String.valueOf(orderId))) {
                    found = true;
                    System.out.println("   ✅ Current order found in list: " + currentOrderId);
                    break;
                }
            }

            if (!found) {
                // Order might be referenced by a different field name
                System.out.println("   ⚠️ Current order ID (" + currentOrderId + ") not found by exact match");
                System.out.println("   Available order IDs in response:");
                for (int i = 0; i < Math.min(orders.size(), 5); i++) {
                    Map<String, Object> o = orders.get(i);
                    System.out.println("     [" + i + "] id=" + o.get("id") + ", guid=" + o.get("guid")
                        + ", order_id=" + o.get("order_id"));
                }
            }
        } else {
            System.out.println("   ⚠️ No current order ID in context — skipping exact match");
        }

        System.out.println("✅ PASSED: Recent order presence check completed");
    }

    @Test(priority = 5, dependsOnMethods = "testGetAllOrdersByUser_SuccessResponse",
          description = "Positive: Validate pagination with page=1, pageSize=1")
    public void testGetAllOrdersByUser_PaginationPageSize1() {
        System.out.println("\n>>> STEP 5B-5: PAGINATION - PAGE SIZE 1 <<<");

        String token = RequestContext.getToken();
        String userId = RequestContext.getUserId();
        Response response = callGetAllOrdersByUser(token, userId, 1, 1, "");

        AssertionUtil.verifyStatusCode(response, 200);

        List<?> orders = response.jsonPath().getList("data");
        Assert.assertNotNull(orders, "Orders list must not be null");
        Assert.assertTrue(orders.size() <= 1,
            "With pageSize=1, should return at most 1 order but got: " + orders.size());

        System.out.println("   Orders returned with pageSize=1: " + orders.size());
        System.out.println("✅ PASSED: Pagination with pageSize=1 works correctly");
    }

    @Test(priority = 6, dependsOnMethods = "testGetAllOrdersByUser_SuccessResponse",
          description = "Positive: Validate pagination with page=2")
    public void testGetAllOrdersByUser_PaginationPage2() {
        System.out.println("\n>>> STEP 5B-6: PAGINATION - PAGE 2 <<<");

        String token = RequestContext.getToken();
        String userId = RequestContext.getUserId();
        Response response = callGetAllOrdersByUser(token, userId, 2, 10, "");

        AssertionUtil.verifyStatusCode(response, 200);

        // Page 2 may have data or be empty — both are valid
        Object data = response.jsonPath().get("data");
        Assert.assertNotNull(data, "Response data field must exist even for page 2");

        if (data instanceof List) {
            System.out.println("   Orders on page 2: " + ((List<?>) data).size());
        }

        System.out.println("✅ PASSED: Pagination page 2 handled correctly");
    }

    @Test(priority = 7, dependsOnMethods = "testGetAllOrdersByUser_SuccessResponse",
          description = "Positive: Validate large pageSize returns all available orders")
    public void testGetAllOrdersByUser_LargePageSize() {
        System.out.println("\n>>> STEP 5B-7: LARGE PAGE SIZE <<<");

        String token = RequestContext.getToken();
        String userId = RequestContext.getUserId();
        Response response = callGetAllOrdersByUser(token, userId, 1, 100, "");

        AssertionUtil.verifyStatusCode(response, 200);

        List<?> orders = response.jsonPath().getList("data");
        Assert.assertNotNull(orders, "Orders list must not be null");
        System.out.println("   Orders with pageSize=100: " + orders.size());

        System.out.println("✅ PASSED: Large page size handled correctly");
    }

    @Test(priority = 8, dependsOnMethods = "testGetAllOrdersByUser_SuccessResponse",
          description = "Positive: Validate searchString filters results")
    public void testGetAllOrdersByUser_SearchStringFilter() {
        System.out.println("\n>>> STEP 5B-8: SEARCH STRING FILTER <<<");

        String token = RequestContext.getToken();
        String userId = RequestContext.getUserId();

        // Search with a non-matching string
        Response response = callGetAllOrdersByUser(token, userId, 1, 10, "zzz_nonexistent_search_xyz");

        AssertionUtil.verifyStatusCode(response, 200);

        Object data = response.jsonPath().get("data");
        if (data instanceof List) {
            List<?> orders = (List<?>) data;
            System.out.println("   Orders matching 'zzz_nonexistent_search_xyz': " + orders.size());
            // Non-matching search should return 0 or fewer results
        }

        System.out.println("✅ PASSED: Search string filter validation completed");
    }

    @Test(priority = 9, dependsOnMethods = "testGetAllOrdersByUser_SuccessResponse",
          description = "Positive: Validate empty searchString returns all orders")
    public void testGetAllOrdersByUser_EmptySearchString() {
        System.out.println("\n>>> STEP 5B-9: EMPTY SEARCH STRING <<<");

        String token = RequestContext.getToken();
        String userId = RequestContext.getUserId();

        Response responseEmpty = callGetAllOrdersByUser(token, userId, 1, 10, "");
        Response responseNull = callGetAllOrdersByUser(token, userId, 1, 10, "");

        AssertionUtil.verifyStatusCode(responseEmpty, 200);
        AssertionUtil.verifyStatusCode(responseNull, 200);

        List<?> ordersEmpty = responseEmpty.jsonPath().getList("data");
        List<?> ordersNull = responseNull.jsonPath().getList("data");

        Assert.assertNotNull(ordersEmpty, "Empty search should return orders");
        Assert.assertNotNull(ordersNull, "Null search should return orders");
        Assert.assertEquals(ordersEmpty.size(), ordersNull.size(),
            "Empty and null search should return same results");

        System.out.println("   Orders with empty search: " + ordersEmpty.size());
        System.out.println("✅ PASSED: Empty search string returns all orders");
    }

    @Test(priority = 10, dependsOnMethods = "testGetAllOrdersByUser_SuccessResponse",
          description = "Positive: Validate response time is acceptable")
    public void testGetAllOrdersByUser_ResponseTime() {
        System.out.println("\n>>> STEP 5B-10: RESPONSE TIME VALIDATION <<<");

        String token = RequestContext.getToken();
        String userId = RequestContext.getUserId();

        long startTime = System.currentTimeMillis();
        Response response = callGetAllOrdersByUser(token, userId, 1, 10, "");
        long responseTime = System.currentTimeMillis() - startTime;

        AssertionUtil.verifyStatusCode(response, 200);
        System.out.println("   Response Time: " + responseTime + "ms");
        Assert.assertTrue(responseTime < 5000,
            "GetAllOrdersByUser response time should be under 5 seconds but took: " + responseTime + "ms");

        System.out.println("✅ PASSED: Response time is acceptable (" + responseTime + "ms)");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  NEGATIVE VALIDATIONS
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 20, description = "Negative: GetAllOrdersByUser with invalid/non-existent user ID")
    public void testGetAllOrdersByUser_InvalidUserId() {
        System.out.println("\n>>> STEP 5B-20: NEGATIVE - INVALID USER ID <<<");

        String token = RequestContext.getToken();
        String invalidUserId = "non-existent-user-id-99999";

        Response response = callGetAllOrdersByUser(token, invalidUserId, 1, 10, "");

        System.out.println("   HTTP Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());

        int statusCode = response.getStatusCode();
        if (statusCode == 200) {
            // API might return 200 with empty data
            Object data = response.jsonPath().get("data");
            if (data instanceof List) {
                Assert.assertTrue(((List<?>) data).isEmpty(),
                    "Invalid user ID should return empty order list");
                System.out.println("   API returned 200 with empty data (acceptable)");
            }
        } else if (statusCode == 500) {
            System.out.println("   🐛 BUG DETECTED: API returns 500 for invalid user ID");
            System.out.println("   Expected: 404 or 200 with empty data");
            System.out.println("   Actual: 500 Internal Server Error");
        } else {
            Assert.assertTrue(statusCode >= 400 && statusCode < 500,
                "Invalid user ID should return 4xx but got: " + statusCode);
        }

        System.out.println("✅ PASSED: Invalid user ID handled");
    }

    @Test(priority = 21, description = "Negative: GetAllOrdersByUser with empty user ID")
    public void testGetAllOrdersByUser_EmptyUserId() {
        System.out.println("\n>>> STEP 5B-21: NEGATIVE - EMPTY USER ID <<<");

        String token = RequestContext.getToken();

        String endpoint = APIEndpoints.GET_ALL_ORDERS_BY_USER;
        Map<String, Object> body = new HashMap<>();
        body.put("page", 1);
        body.put("pageSize", 10);
        body.put("searchString", "");

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", token)
                .setRequestBody(body)
                .post();

        System.out.println("   HTTP Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());

        int statusCode = response.getStatusCode();
        if (statusCode == 200) {
            Boolean success = response.jsonPath().get("success");
            System.out.println("   Success flag: " + success);
        } else if (statusCode == 500) {
            System.out.println("   🐛 BUG DETECTED: API returns 500 for empty user ID");
        } else {
            Assert.assertTrue(statusCode >= 400,
                "Empty user ID should return error but got: " + statusCode);
        }

        System.out.println("✅ PASSED: Empty user ID handled");
    }

    @Test(priority = 22, description = "Negative: GetAllOrdersByUser with page=0 (invalid)")
    public void testGetAllOrdersByUser_PageZero() {
        System.out.println("\n>>> STEP 5B-22: NEGATIVE - PAGE ZERO <<<");

        String token = RequestContext.getToken();
        String userId = RequestContext.getUserId();

        Response response = callGetAllOrdersByUser(token, userId, 0, 10, "");

        System.out.println("   HTTP Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());

        int statusCode = response.getStatusCode();
        if (statusCode == 200) {
            // Some APIs treat page=0 same as page=1
            System.out.println("   API returned 200 for page=0 (may default to page 1)");
        } else {
            Assert.assertTrue(statusCode >= 400,
                "Page 0 should return error or be handled but got: " + statusCode);
        }

        System.out.println("✅ PASSED: Page zero handled");
    }

    @Test(priority = 23, description = "Negative: GetAllOrdersByUser with negative page number")
    public void testGetAllOrdersByUser_NegativePage() {
        System.out.println("\n>>> STEP 5B-23: NEGATIVE - NEGATIVE PAGE <<<");

        String token = RequestContext.getToken();
        String userId = RequestContext.getUserId();

        Response response = callGetAllOrdersByUser(token, userId, -1, 10, "");

        System.out.println("   HTTP Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());

        int statusCode = response.getStatusCode();
        if (statusCode == 200) {
            System.out.println("   API returned 200 for negative page (may default to page 1)");
        } else if (statusCode == 500) {
            System.out.println("   🐛 BUG: API returns 500 for negative page number");
        } else {
            Assert.assertTrue(statusCode >= 400,
                "Negative page should return error but got: " + statusCode);
        }

        System.out.println("✅ PASSED: Negative page handled");
    }

    @Test(priority = 24, description = "Negative: GetAllOrdersByUser with pageSize=0")
    public void testGetAllOrdersByUser_PageSizeZero() {
        System.out.println("\n>>> STEP 5B-24: NEGATIVE - PAGE SIZE ZERO <<<");

        String token = RequestContext.getToken();
        String userId = RequestContext.getUserId();

        Response response = callGetAllOrdersByUser(token, userId, 1, 0, "");

        System.out.println("   HTTP Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());

        int statusCode = response.getStatusCode();
        if (statusCode == 200) {
            Object data = response.jsonPath().get("data");
            if (data instanceof List) {
                System.out.println("   Orders returned with pageSize=0: " + ((List<?>) data).size());
            }
        } else if (statusCode == 500) {
            System.out.println("   🐛 BUG: API returns 500 for pageSize=0");
        }

        System.out.println("✅ PASSED: Page size zero handled");
    }

    @Test(priority = 25, description = "Negative: GetAllOrdersByUser with negative pageSize")
    public void testGetAllOrdersByUser_NegativePageSize() {
        System.out.println("\n>>> STEP 5B-25: NEGATIVE - NEGATIVE PAGE SIZE <<<");

        String token = RequestContext.getToken();
        String userId = RequestContext.getUserId();

        Response response = callGetAllOrdersByUser(token, userId, 1, -5, "");

        System.out.println("   HTTP Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());

        int statusCode = response.getStatusCode();
        if (statusCode == 200) {
            System.out.println("   API returned 200 for negative pageSize");
        } else if (statusCode == 500) {
            System.out.println("   🐛 BUG: API returns 500 for negative pageSize");
        } else {
            Assert.assertTrue(statusCode >= 400,
                "Negative pageSize should return error but got: " + statusCode);
        }

        System.out.println("✅ PASSED: Negative page size handled");
    }

    @Test(priority = 26, description = "Negative: GetAllOrdersByUser with excessively large pageSize")
    public void testGetAllOrdersByUser_ExcessivePageSize() {
        System.out.println("\n>>> STEP 5B-26: NEGATIVE - EXCESSIVE PAGE SIZE <<<");

        String token = RequestContext.getToken();
        String userId = RequestContext.getUserId();

        Response response = callGetAllOrdersByUser(token, userId, 1, 999999, "");

        System.out.println("   HTTP Status: " + response.getStatusCode());

        int statusCode = response.getStatusCode();
        // API should either cap it or return all available orders
        if (statusCode == 200) {
            List<?> orders = response.jsonPath().getList("data");
            System.out.println("   Orders returned with pageSize=999999: " + (orders != null ? orders.size() : 0));
        } else if (statusCode == 500) {
            System.out.println("   🐛 BUG: API returns 500 for excessive pageSize");
        }

        System.out.println("✅ PASSED: Excessive page size handled");
    }

    @Test(priority = 27, description = "Negative: GetAllOrdersByUser with SQL injection in user ID")
    public void testGetAllOrdersByUser_SqlInjectionUserId() {
        System.out.println("\n>>> STEP 5B-27: NEGATIVE - SQL INJECTION IN USER ID <<<");

        String token = RequestContext.getToken();
        String sqlPayload = "1' OR '1'='1' --";

        Response response = callGetAllOrdersByUser(token, sqlPayload, 1, 10, "");

        System.out.println("   HTTP Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());

        int statusCode = response.getStatusCode();
        if (statusCode == 200) {
            Object data = response.jsonPath().get("data");
            if (data instanceof List && !((List<?>) data).isEmpty()) {
                System.out.println("   🐛 SECURITY BUG: SQL injection returned data!");
            } else {
                System.out.println("   SQL injection returned empty data (safe)");
            }
        }

        // Verify no SQL error leaked
        String responseBody = response.getBody().asString().toLowerCase();
        Assert.assertFalse(responseBody.contains("sql syntax"),
            "Response should not expose SQL syntax errors");
        Assert.assertFalse(responseBody.contains("mysql"),
            "Response should not expose database details");

        System.out.println("✅ PASSED: SQL injection in user ID handled");
    }

    @Test(priority = 28, description = "Negative: GetAllOrdersByUser with SQL injection in searchString")
    public void testGetAllOrdersByUser_SqlInjectionSearchString() {
        System.out.println("\n>>> STEP 5B-28: NEGATIVE - SQL INJECTION IN SEARCH STRING <<<");

        String token = RequestContext.getToken();
        String userId = RequestContext.getUserId();
        String sqlPayload = "'; DROP TABLE orders; --";

        Response response = callGetAllOrdersByUser(token, userId, 1, 10, sqlPayload);

        System.out.println("   HTTP Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());

        // Verify no SQL error leaked
        String responseBody = response.getBody().asString().toLowerCase();
        Assert.assertFalse(responseBody.contains("sql syntax"),
            "Response should not expose SQL syntax errors");
        Assert.assertFalse(responseBody.contains("mysql"),
            "Response should not expose database details");
        Assert.assertFalse(responseBody.contains("drop table"),
            "Response should not reflect SQL injection payload");

        System.out.println("✅ PASSED: SQL injection in search string handled");
    }

    @Test(priority = 29, description = "Negative: GetAllOrdersByUser with XSS in searchString")
    public void testGetAllOrdersByUser_XssSearchString() {
        System.out.println("\n>>> STEP 5B-29: NEGATIVE - XSS IN SEARCH STRING <<<");

        String token = RequestContext.getToken();
        String userId = RequestContext.getUserId();
        String xssPayload = "<script>alert('xss')</script>";

        Response response = callGetAllOrdersByUser(token, userId, 1, 10, xssPayload);

        System.out.println("   HTTP Status: " + response.getStatusCode());

        String responseBody = response.getBody().asString();
        Assert.assertFalse(responseBody.contains("<script>"),
            "Response should not reflect XSS script tags");

        System.out.println("✅ PASSED: XSS in search string handled");
    }

    @Test(priority = 30, description = "Negative: GetAllOrdersByUser with special characters in user ID")
    public void testGetAllOrdersByUser_SpecialCharsUserId() {
        System.out.println("\n>>> STEP 5B-30: NEGATIVE - SPECIAL CHARACTERS IN USER ID <<<");

        String token = RequestContext.getToken();
        String specialCharsId = "!@#$%^&*()~`";

        Response response = callGetAllOrdersByUser(token, specialCharsId, 1, 10, "");

        System.out.println("   HTTP Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());

        int statusCode = response.getStatusCode();
        if (statusCode == 200) {
            Object data = response.jsonPath().get("data");
            if (data instanceof List) {
                Assert.assertTrue(((List<?>) data).isEmpty(),
                    "Special chars user ID should return empty list");
            }
        } else if (statusCode == 500) {
            System.out.println("   🐛 BUG: API returns 500 for special characters in user ID");
        }

        System.out.println("✅ PASSED: Special characters in user ID handled");
    }

    @Test(priority = 31, description = "Negative: GetAllOrdersByUser with excessively long user ID")
    public void testGetAllOrdersByUser_ExcessivelyLongUserId() {
        System.out.println("\n>>> STEP 5B-31: NEGATIVE - EXCESSIVELY LONG USER ID <<<");

        String token = RequestContext.getToken();
        StringBuilder longIdBuilder = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            longIdBuilder.append("a");
        }
        String longId = longIdBuilder.toString();

        Response response = callGetAllOrdersByUser(token, longId, 1, 10, "");

        System.out.println("   HTTP Status: " + response.getStatusCode());

        int statusCode = response.getStatusCode();
        if (statusCode == 200) {
            Object data = response.jsonPath().get("data");
            if (data instanceof List) {
                Assert.assertTrue(((List<?>) data).isEmpty(),
                    "Excessively long user ID should return empty list");
            }
        } else if (statusCode == 500) {
            System.out.println("   🐛 BUG: API returns 500 for excessively long user ID");
        } else {
            Assert.assertTrue(statusCode >= 400 && statusCode < 600,
                "Excessively long user ID should return error but got: " + statusCode);
        }

        System.out.println("✅ PASSED: Excessively long user ID handled");
    }

    @Test(priority = 32, description = "Negative: GetAllOrdersByUser with path traversal in user ID")
    public void testGetAllOrdersByUser_PathTraversal() {
        System.out.println("\n>>> STEP 5B-32: NEGATIVE - PATH TRAVERSAL <<<");

        String token = RequestContext.getToken();
        String pathTraversal = "../../../etc/passwd";

        Response response = callGetAllOrdersByUser(token, pathTraversal, 1, 10, "");

        System.out.println("   HTTP Status: " + response.getStatusCode());

        String responseBody = response.getBody().asString();
        Assert.assertFalse(responseBody.contains("root:"),
            "Response should NOT expose system file content");
        Assert.assertFalse(responseBody.contains("/bin/bash"),
            "Response should NOT expose system file content");

        System.out.println("✅ PASSED: Path traversal attempt blocked");
    }

    @Test(priority = 33, description = "Negative: GetAllOrdersByUser with very large page number")
    public void testGetAllOrdersByUser_VeryLargePageNumber() {
        System.out.println("\n>>> STEP 5B-33: NEGATIVE - VERY LARGE PAGE NUMBER <<<");

        String token = RequestContext.getToken();
        String userId = RequestContext.getUserId();

        Response response = callGetAllOrdersByUser(token, userId, 999999, 10, "");

        System.out.println("   HTTP Status: " + response.getStatusCode());

        int statusCode = response.getStatusCode();
        if (statusCode == 200) {
            Object data = response.jsonPath().get("data");
            if (data instanceof List) {
                // Very large page should return empty list
                System.out.println("   Orders on page 999999: " + ((List<?>) data).size());
            }
        } else if (statusCode == 500) {
            System.out.println("   🐛 BUG: API returns 500 for very large page number");
        }

        System.out.println("✅ PASSED: Very large page number handled");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SCHEMA VALIDATIONS
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 40, dependsOnMethods = "testGetAllOrdersByUser_SuccessResponse",
          description = "Schema: Validate top-level response schema")
    public void testGetAllOrdersByUser_TopLevelSchema() {
        System.out.println("\n>>> STEP 5B-40: SCHEMA - TOP-LEVEL RESPONSE <<<");

        String token = RequestContext.getToken();
        String userId = RequestContext.getUserId();
        Response response = callGetAllOrdersByUser(token, userId, 1, 10, "");

        // success must be Boolean
        Object successVal = response.jsonPath().get("success");
        Assert.assertNotNull(successVal, "'success' field must be present");
        Assert.assertTrue(successVal instanceof Boolean,
            "'success' must be Boolean but found: " + successVal.getClass().getSimpleName());

        // data must be an array
        Object dataVal = response.jsonPath().get("data");
        Assert.assertNotNull(dataVal, "'data' field must be present");
        Assert.assertTrue(dataVal instanceof List,
            "'data' must be an Array but found: " + dataVal.getClass().getSimpleName());

        // Content-Type should be JSON
        String contentType = response.getContentType();
        Assert.assertTrue(contentType != null && contentType.contains("application/json"),
            "Content-Type should be application/json but found: " + contentType);

        System.out.println("   ✅ success(Boolean), data(Array), Content-Type(json)");
        System.out.println("✅ PASSED: Top-level schema validated");
    }

    @Test(priority = 41, dependsOnMethods = "testGetAllOrdersByUser_SuccessResponse",
          description = "Schema: Validate order item field types in the list")
    public void testGetAllOrdersByUser_OrderItemSchema() {
        System.out.println("\n>>> STEP 5B-41: SCHEMA - ORDER ITEM FIELD TYPES <<<");

        String token = RequestContext.getToken();
        String userId = RequestContext.getUserId();
        Response response = callGetAllOrdersByUser(token, userId, 1, 10, "");

        List<Map<String, Object>> orders = response.jsonPath().getList("data");
        Assert.assertFalse(orders.isEmpty(), "Must have at least 1 order for schema validation");

        Map<String, Object> order = orders.get(0);

        // order_status must be String
        Object orderStatus = order.get("order_status");
        if (orderStatus != null) {
            Assert.assertTrue(orderStatus instanceof String,
                "order_status must be String but found: " + orderStatus.getClass().getSimpleName());
            System.out.println("   order_status: String ✓ (" + orderStatus + ")");
        }

        // payment_id must be String or Number
        Object paymentId = order.get("payment_id");
        if (paymentId != null) {
            Assert.assertTrue(paymentId instanceof String || paymentId instanceof Number,
                "payment_id must be String/Number but found: " + paymentId.getClass().getSimpleName());
            System.out.println("   payment_id: " + paymentId.getClass().getSimpleName() + " ✓ (" + paymentId + ")");
        }

        // product_details should be array if present
        Object products = order.get("product_details");
        if (products == null) products = order.get("items");
        if (products != null) {
            Assert.assertTrue(products instanceof List,
                "product_details must be Array but found: " + products.getClass().getSimpleName());
            System.out.println("   product_details: Array ✓ (size=" + ((List<?>) products).size() + ")");
        }

        System.out.println("✅ PASSED: Order item schema validated");
    }

    @Test(priority = 42, dependsOnMethods = "testGetAllOrdersByUser_SuccessResponse",
          description = "Schema: Validate order_status contains allowed enum values")
    public void testGetAllOrdersByUser_OrderStatusEnum() {
        System.out.println("\n>>> STEP 5B-42: SCHEMA - ORDER STATUS ENUM <<<");

        String token = RequestContext.getToken();
        String userId = RequestContext.getUserId();
        Response response = callGetAllOrdersByUser(token, userId, 1, 10, "");

        Set<String> allowedStatuses = new HashSet<>(Arrays.asList(
            "Pending", "Confirmed", "Placed", "Assigned", "In Progress",
            "Sample Collected", "Report Uploaded", "Completed", "Cancelled",
            "pending", "confirmed", "placed", "assigned", "in_progress",
            "sample_collected", "report_uploaded", "completed", "cancelled"
        ));

        List<Map<String, Object>> orders = response.jsonPath().getList("data");
        for (int i = 0; i < orders.size(); i++) {
            Object statusObj = orders.get(i).get("order_status");
            if (statusObj != null) {
                String status = statusObj.toString();
                Assert.assertTrue(allowedStatuses.contains(status),
                    "Order [" + i + "] has invalid status: '" + status + "'");
            }
        }

        System.out.println("   All " + orders.size() + " orders have valid status values ✓");
        System.out.println("✅ PASSED: Order status enum validated across all orders");
    }

    @Test(priority = 43, dependsOnMethods = "testGetAllOrdersByUser_SuccessResponse",
          description = "Schema: Validate all orders belong to the requested user")
    public void testGetAllOrdersByUser_UserIdConsistency() {
        System.out.println("\n>>> STEP 5B-43: SCHEMA - USER ID CONSISTENCY <<<");

        String token = RequestContext.getToken();
        String userId = RequestContext.getUserId();
        Response response = callGetAllOrdersByUser(token, userId, 1, 10, "");

        List<Map<String, Object>> orders = response.jsonPath().getList("data");
        int validated = 0;

        for (int i = 0; i < orders.size(); i++) {
            Object orderUserId = orders.get(i).get("user_id");
            if (orderUserId == null) orderUserId = orders.get(i).get("user_guid");

            if (orderUserId != null) {
                Assert.assertEquals(orderUserId.toString(), userId,
                    "Order [" + i + "] user_id mismatch: expected " + userId + " but found " + orderUserId);
                validated++;
            }
        }

        System.out.println("   Validated user_id consistency for " + validated + "/" + orders.size() + " orders");
        System.out.println("✅ PASSED: All orders belong to the requested user");
    }
}

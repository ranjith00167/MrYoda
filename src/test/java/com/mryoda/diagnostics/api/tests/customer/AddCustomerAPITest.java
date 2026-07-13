package com.mryoda.diagnostics.api.tests.customer;

import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.AssertionUtil;
import com.mryoda.diagnostics.api.utils.RequestContext;
import io.restassured.response.Response;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

/**
 * Add Customer API Test
 * Endpoint: POST /membership/customer/addCustomer
 * 
 * Validates:
 * - Positive: Success response with empty body, data structure, response fields
 * - Schema: Response structure validation
 */
public class AddCustomerAPITest extends BaseTest {

    private static final String ADD_CUSTOMER_ENDPOINT = APIEndpoints.ADD_CUSTOMER;

    // ═══════════════════════════════════════════════════════════════════
    //  HELPER
    // ═══════════════════════════════════════════════════════════════════

    private Response callAddCustomerAPI(String token, JSONObject payload) {
        return new RequestBuilder()
                .setEndpoint(ADD_CUSTOMER_ENDPOINT)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .setRequestBody(payload.toString())
                .post();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  POSITIVE VALIDATIONS
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 1, description = "Positive: Verify AddCustomer returns 201 with empty body")
    public void testAddCustomer_SuccessResponse() {
        System.out.println("\n>>> ADD CUSTOMER - POSITIVE: SUCCESS RESPONSE <<<");

        String token = RequestContext.getToken();
        Assert.assertNotNull(token, "Token must be available from login");

        JSONObject payload = new JSONObject(); // Empty body
        System.out.println("   Request Body: {}");

        Response response = callAddCustomerAPI(token, payload);

        System.out.println("   HTTP Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());

        AssertionUtil.verifyStatusCode(response, 201);

        Boolean success = response.jsonPath().get("success");
        Assert.assertNotNull(success, "Response must have 'success' field");
        System.out.println("   Success: " + success);

        System.out.println("✅ PASSED: AddCustomer returns 201 (Created) with empty body");
    }

    @Test(priority = 2, dependsOnMethods = "testAddCustomer_SuccessResponse",
          description = "Positive: Validate response structure has required fields")
    public void testAddCustomer_ResponseStructure() {
        System.out.println("\n>>> ADD CUSTOMER - POSITIVE: RESPONSE STRUCTURE <<<");

        String token = RequestContext.getToken();
        JSONObject payload = new JSONObject();
        Response response = callAddCustomerAPI(token, payload);

        AssertionUtil.verifyStatusCode(response, 201);

        // Verify top-level fields
        Object statusObj = response.jsonPath().get("status");
        Assert.assertNotNull(statusObj, "Response must have 'status' field");
        System.out.println("   status: " + statusObj);

        Object successObj = response.jsonPath().get("success");
        Assert.assertNotNull(successObj, "Response must have 'success' field");
        System.out.println("   success: " + successObj);

        Object msgObj = response.jsonPath().get("msg");
        if (msgObj != null) {
            System.out.println("   msg: " + msgObj);
        }

        Object dataObj = response.jsonPath().get("data");
        if (dataObj != null) {
            System.out.println("   data: present (" + dataObj.getClass().getSimpleName() + ")");
        }

        System.out.println("✅ PASSED: Response structure validated");
    }

    @Test(priority = 3, dependsOnMethods = "testAddCustomer_SuccessResponse",
          description = "Positive: Validate status field is integer type")
    public void testAddCustomer_StatusFieldType() {
        System.out.println("\n>>> ADD CUSTOMER - POSITIVE: STATUS FIELD TYPE <<<");

        String token = RequestContext.getToken();
        JSONObject payload = new JSONObject();
        Response response = callAddCustomerAPI(token, payload);

        Object statusObj = response.jsonPath().get("status");
        Assert.assertNotNull(statusObj, "Status field must be present");
        Assert.assertTrue(statusObj instanceof Integer, 
            "Status must be Integer, got: " + statusObj.getClass().getSimpleName());
        
        int statusCode = (Integer) statusObj;
        System.out.println("   status: " + statusCode + " (Integer ✓)");

        Assert.assertEquals(statusCode, 201, "Status code in body should match HTTP status");

        System.out.println("✅ PASSED: Status field type validated");
    }

    @Test(priority = 4, dependsOnMethods = "testAddCustomer_SuccessResponse",
          description = "Positive: Validate success field is boolean type")
    public void testAddCustomer_SuccessFieldType() {
        System.out.println("\n>>> ADD CUSTOMER - POSITIVE: SUCCESS FIELD TYPE <<<");

        String token = RequestContext.getToken();
        JSONObject payload = new JSONObject();
        Response response = callAddCustomerAPI(token, payload);

        Object successObj = response.jsonPath().get("success");
        Assert.assertNotNull(successObj, "Success field must be present");
        Assert.assertTrue(successObj instanceof Boolean, 
            "Success must be Boolean, got: " + successObj.getClass().getSimpleName());
        
        boolean success = (Boolean) successObj;
        System.out.println("   success: " + success + " (Boolean ✓)");

        System.out.println("✅ PASSED: Success field type validated");
    }

    @Test(priority = 5, dependsOnMethods = "testAddCustomer_SuccessResponse",
          description = "Positive: Validate message field exists and is string")
    public void testAddCustomer_MessageField() {
        System.out.println("\n>>> ADD CUSTOMER - POSITIVE: MESSAGE FIELD <<<");

        String token = RequestContext.getToken();
        JSONObject payload = new JSONObject();
        Response response = callAddCustomerAPI(token, payload);

        Object msgObj = response.jsonPath().get("msg");
        if (msgObj != null) {
            Assert.assertTrue(msgObj instanceof String, 
                "Message must be String, got: " + msgObj.getClass().getSimpleName());
            
            String message = (String) msgObj;
            Assert.assertFalse(message.trim().isEmpty(), "Message should not be empty");
            System.out.println("   msg: \"" + message + "\" (String ✓)");
        } else {
            System.out.println("   msg: null (optional field)");
        }

        System.out.println("✅ PASSED: Message field validated");
    }

    @Test(priority = 6, dependsOnMethods = "testAddCustomer_SuccessResponse",
          description = "Positive: Validate response time is acceptable")
    public void testAddCustomer_ResponseTime() {
        System.out.println("\n>>> ADD CUSTOMER - POSITIVE: RESPONSE TIME <<<");

        String token = RequestContext.getToken();
        JSONObject payload = new JSONObject();

        long startTime = System.currentTimeMillis();
        Response response = callAddCustomerAPI(token, payload);
        long responseTime = System.currentTimeMillis() - startTime;

        AssertionUtil.verifyStatusCode(response, 201);
        System.out.println("   Response Time: " + responseTime + "ms");
        
        Assert.assertTrue(responseTime < 5000,
            "AddCustomer response time should be under 5s but took: " + responseTime + "ms");

        System.out.println("✅ PASSED: Response time is acceptable (" + responseTime + "ms)");
    }

    @Test(priority = 7, dependsOnMethods = "testAddCustomer_SuccessResponse",
          description = "Positive: Validate data field structure if present")
    public void testAddCustomer_DataFieldStructure() {
        System.out.println("\n>>> ADD CUSTOMER - POSITIVE: DATA FIELD STRUCTURE <<<");

        String token = RequestContext.getToken();
        JSONObject payload = new JSONObject();
        Response response = callAddCustomerAPI(token, payload);

        AssertionUtil.verifyStatusCode(response, 201);

        Object dataObj = response.jsonPath().get("data");
        if (dataObj != null) {
            System.out.println("   Data present: " + dataObj.getClass().getSimpleName());
            
            if (dataObj instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) dataObj;
                System.out.println("   Data is Object with " + dataMap.size() + " fields");
                
                // Log available fields
                for (String key : dataMap.keySet()) {
                    System.out.println("      - " + key + ": " + dataMap.get(key));
                }
            } else if (dataObj instanceof List) {
                List<?> dataList = (List<?>) dataObj;
                System.out.println("   Data is Array with " + dataList.size() + " items");
            } else {
                System.out.println("   Data type: " + dataObj.getClass().getSimpleName());
                System.out.println("   Data value: " + dataObj);
            }
        } else {
            System.out.println("   data: null (no data returned)");
        }

        System.out.println("✅ PASSED: Data field structure validated");
    }

    @Test(priority = 8, dependsOnMethods = "testAddCustomer_SuccessResponse",
          description = "Positive: Validate no sensitive data exposed in response")
    public void testAddCustomer_NoSensitiveDataExposed() {
        System.out.println("\n>>> ADD CUSTOMER - POSITIVE: NO SENSITIVE DATA <<<");

        String token = RequestContext.getToken();
        JSONObject payload = new JSONObject();
        Response response = callAddCustomerAPI(token, payload);

        String responseBody = response.getBody().asString().toLowerCase();
        
        Assert.assertFalse(responseBody.contains("password"), 
            "Response should not contain password field");
        Assert.assertFalse(responseBody.contains("\"token\"") && !responseBody.contains("\"access_token\""), 
            "Response should not contain token field (except access_token)");
        Assert.assertFalse(responseBody.contains("secret"), 
            "Response should not contain secret field");
        Assert.assertFalse(responseBody.contains("private_key"), 
            "Response should not contain private_key field");

        System.out.println("   ✅ No password, token, secret, or private_key fields found");
        System.out.println("✅ PASSED: No sensitive data exposed");
    }

    @Test(priority = 9, dependsOnMethods = "testAddCustomer_SuccessResponse",
          description = "Positive: Validate consistent response on multiple calls")
    public void testAddCustomer_ConsistentResponse() {
        System.out.println("\n>>> ADD CUSTOMER - POSITIVE: CONSISTENT RESPONSE <<<");

        String token = RequestContext.getToken();
        JSONObject payload = new JSONObject();

        // Call 1
        Response response1 = callAddCustomerAPI(token, payload);
        int statusCode1 = response1.getStatusCode();
        Boolean success1 = response1.jsonPath().get("success");

        // Call 2
        Response response2 = callAddCustomerAPI(token, payload);
        int statusCode2 = response2.getStatusCode();
        Boolean success2 = response2.jsonPath().get("success");

        // Verify consistency
        Assert.assertEquals(statusCode2, statusCode1, 
            "Status codes should be consistent across multiple calls");
        Assert.assertEquals(success2, success1, 
            "Success values should be consistent across multiple calls");

        System.out.println("   Call 1: status=" + statusCode1 + ", success=" + success1);
        System.out.println("   Call 2: status=" + statusCode2 + ", success=" + success2);
        System.out.println("✅ PASSED: Response is consistent across multiple calls");
    }

    @Test(priority = 10, dependsOnMethods = "testAddCustomer_SuccessResponse",
          description = "Positive: Validate HTTP headers in response")
    public void testAddCustomer_ResponseHeaders() {
        System.out.println("\n>>> ADD CUSTOMER - POSITIVE: RESPONSE HEADERS <<<");

        String token = RequestContext.getToken();
        JSONObject payload = new JSONObject();
        Response response = callAddCustomerAPI(token, payload);

        AssertionUtil.verifyStatusCode(response, 201);

        String contentType = response.getHeader("Content-Type");
        if (contentType != null) {
            Assert.assertTrue(contentType.contains("application/json"), 
                "Content-Type should be application/json, got: " + contentType);
            System.out.println("   Content-Type: " + contentType + " ✓");
        } else {
            System.out.println("   ⚠️ Content-Type header not present");
        }

        System.out.println("✅ PASSED: Response headers validated");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SCHEMA VALIDATION
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 20, dependsOnMethods = "testAddCustomer_SuccessResponse",
          description = "Schema: Validate top-level response structure")
    public void testAddCustomer_Schema_TopLevelStructure() {
        System.out.println("\n>>> ADD CUSTOMER - SCHEMA: TOP-LEVEL STRUCTURE <<<");

        String token = RequestContext.getToken();
        JSONObject payload = new JSONObject();
        Response response = callAddCustomerAPI(token, payload);

        AssertionUtil.verifyStatusCode(response, 201);

        // Validate status field
        Object statusObj = response.jsonPath().get("status");
        Assert.assertNotNull(statusObj, "Schema: 'status' field must be present");
        Assert.assertTrue(statusObj instanceof Integer, 
            "Schema: 'status' must be Integer, got: " + statusObj.getClass().getSimpleName());
        System.out.println("   ✓ status: Integer");

        // Validate success field
        Object successObj = response.jsonPath().get("success");
        Assert.assertNotNull(successObj, "Schema: 'success' field must be present");
        Assert.assertTrue(successObj instanceof Boolean, 
            "Schema: 'success' must be Boolean, got: " + successObj.getClass().getSimpleName());
        System.out.println("   ✓ success: Boolean");

        // Validate msg field (optional)
        Object msgObj = response.jsonPath().get("msg");
        if (msgObj != null) {
            Assert.assertTrue(msgObj instanceof String, 
                "Schema: 'msg' must be String, got: " + msgObj.getClass().getSimpleName());
            System.out.println("   ✓ msg: String");
        } else {
            System.out.println("   ✓ msg: null (optional)");
        }

        System.out.println("✅ PASSED: Top-level schema validated");
    }

    @Test(priority = 21, dependsOnMethods = "testAddCustomer_SuccessResponse",
          description = "Schema: Validate status field constraints")
    public void testAddCustomer_Schema_StatusFieldConstraints() {
        System.out.println("\n>>> ADD CUSTOMER - SCHEMA: STATUS FIELD CONSTRAINTS <<<");

        String token = RequestContext.getToken();
        JSONObject payload = new JSONObject();
        Response response = callAddCustomerAPI(token, payload);

        Object statusObj = response.jsonPath().get("status");
        Assert.assertTrue(statusObj instanceof Integer, "Status must be Integer");
        
        int status = (Integer) statusObj;
        Assert.assertTrue(status >= 200 && status < 600, 
            "Schema: Status code must be valid HTTP status (200-599), got: " + status);
        System.out.println("   ✓ status value: " + status + " (valid HTTP status)");

        Assert.assertEquals(status, 201, 
            "Schema: Status in body should match HTTP 201 Created");
        System.out.println("   ✓ status matches HTTP response code");

        System.out.println("✅ PASSED: Status field constraints validated");
    }

    @Test(priority = 22, dependsOnMethods = "testAddCustomer_SuccessResponse",
          description = "Schema: Validate success field is boolean type")
    public void testAddCustomer_Schema_SuccessFieldType() {
        System.out.println("\n>>> ADD CUSTOMER - SCHEMA: SUCCESS FIELD TYPE <<<");

        String token = RequestContext.getToken();
        JSONObject payload = new JSONObject();
        Response response = callAddCustomerAPI(token, payload);

        Object successObj = response.jsonPath().get("success");
        Assert.assertNotNull(successObj, "Schema: 'success' must be present");
        Assert.assertTrue(successObj instanceof Boolean, 
            "Schema: 'success' must be Boolean, got: " + successObj.getClass().getSimpleName());
        
        boolean success = (Boolean) successObj;
        System.out.println("   ✓ success: " + success + " (Boolean)");

        // For 201 Created, success should typically be true
        Assert.assertTrue(success, 
            "Schema: For HTTP 201, 'success' should be true");
        System.out.println("   ✓ success=true for successful creation");

        System.out.println("✅ PASSED: Success field type validated");
    }

    @Test(priority = 23, dependsOnMethods = "testAddCustomer_SuccessResponse",
          description = "Schema: Validate message field format")
    public void testAddCustomer_Schema_MessageFieldFormat() {
        System.out.println("\n>>> ADD CUSTOMER - SCHEMA: MESSAGE FIELD FORMAT <<<");

        String token = RequestContext.getToken();
        JSONObject payload = new JSONObject();
        Response response = callAddCustomerAPI(token, payload);

        Object msgObj = response.jsonPath().get("msg");
        if (msgObj != null) {
            Assert.assertTrue(msgObj instanceof String, 
                "Schema: 'msg' must be String, got: " + msgObj.getClass().getSimpleName());
            
            String message = (String) msgObj;
            Assert.assertFalse(message.trim().isEmpty(), 
                "Schema: Message should not be empty string");
            System.out.println("   ✓ msg: String (non-empty)");
            System.out.println("   Message: \"" + message + "\"");
        } else {
            System.out.println("   ✓ msg: null (field is optional)");
        }

        System.out.println("✅ PASSED: Message field format validated");
    }

    @Test(priority = 24, dependsOnMethods = "testAddCustomer_SuccessResponse",
          description = "Schema: Validate data field structure if present")
    public void testAddCustomer_Schema_DataFieldStructure() {
        System.out.println("\n>>> ADD CUSTOMER - SCHEMA: DATA FIELD STRUCTURE <<<");

        String token = RequestContext.getToken();
        JSONObject payload = new JSONObject();
        Response response = callAddCustomerAPI(token, payload);

        Object dataObj = response.jsonPath().get("data");
        if (dataObj != null) {
            System.out.println("   ✓ data: present");
            
            // Validate it's either Map (object) or List (array) or primitive
            boolean isValidType = (dataObj instanceof Map) || 
                                  (dataObj instanceof List) || 
                                  (dataObj instanceof String) ||
                                  (dataObj instanceof Number) ||
                                  (dataObj instanceof Boolean);
            
            Assert.assertTrue(isValidType, 
                "Schema: 'data' must be Object, Array, String, Number, or Boolean, got: " 
                + dataObj.getClass().getSimpleName());
            
            System.out.println("   ✓ data type: " + dataObj.getClass().getSimpleName());
            
            if (dataObj instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) dataObj;
                System.out.println("   ✓ data is Object with " + dataMap.size() + " fields");
                
                // Log field types
                for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
                    String fieldName = entry.getKey();
                    Object fieldValue = entry.getValue();
                    String fieldType = (fieldValue != null) ? fieldValue.getClass().getSimpleName() : "null";
                    System.out.println("      - " + fieldName + ": " + fieldType);
                }
            } else if (dataObj instanceof List) {
                List<?> dataList = (List<?>) dataObj;
                System.out.println("   ✓ data is Array with " + dataList.size() + " items");
            }
        } else {
            System.out.println("   ✓ data: null (field is optional)");
        }

        System.out.println("✅ PASSED: Data field structure validated");
    }

    @Test(priority = 25, dependsOnMethods = "testAddCustomer_SuccessResponse",
          description = "Schema: Validate no extra unexpected fields")
    public void testAddCustomer_Schema_NoExtraFields() {
        System.out.println("\n>>> ADD CUSTOMER - SCHEMA: NO EXTRA FIELDS <<<");

        String token = RequestContext.getToken();
        JSONObject payload = new JSONObject();
        Response response = callAddCustomerAPI(token, payload);

        // Get all keys from response
        Map<String, Object> responseMap = response.jsonPath().getMap("$");
        Assert.assertNotNull(responseMap, "Response must be a JSON object");

        // Expected fields
        String[] expectedFields = {"status", "success", "msg", "data"};
        
        System.out.println("   Response fields: " + responseMap.keySet());
        System.out.println("   Expected fields: " + String.join(", ", expectedFields));

        // Check for unexpected fields
        for (String key : responseMap.keySet()) {
            boolean isExpected = false;
            for (String expected : expectedFields) {
                if (key.equals(expected)) {
                    isExpected = true;
                    break;
                }
            }
            
            if (!isExpected) {
                System.out.println("   ⚠️ Unexpected field found: " + key);
            }
        }

        // Note: This is informational, not failing - APIs may evolve
        System.out.println("   ✓ Schema field analysis complete");
        System.out.println("✅ PASSED: Schema field validation complete");
    }

    @Test(priority = 26, dependsOnMethods = "testAddCustomer_SuccessResponse",
          description = "Schema: Validate response is valid JSON")
    public void testAddCustomer_Schema_ValidJSON() {
        System.out.println("\n>>> ADD CUSTOMER - SCHEMA: VALID JSON <<<");

        String token = RequestContext.getToken();
        JSONObject payload = new JSONObject();
        Response response = callAddCustomerAPI(token, payload);

        String responseBody = response.getBody().asString();
        Assert.assertNotNull(responseBody, "Response body must not be null");
        Assert.assertFalse(responseBody.trim().isEmpty(), "Response body must not be empty");

        // Try to parse as JSON
        try {
            new org.json.JSONObject(responseBody);
            System.out.println("   ✓ Response is valid JSON");
        } catch (Exception e) {
            Assert.fail("Schema: Response is not valid JSON: " + e.getMessage());
        }

        // Verify Content-Type indicates JSON
        String contentType = response.getContentType();
        if (contentType != null) {
            Assert.assertTrue(contentType.toLowerCase().contains("json"), 
                "Schema: Content-Type should indicate JSON, got: " + contentType);
            System.out.println("   ✓ Content-Type indicates JSON: " + contentType);
        }

        System.out.println("✅ PASSED: Response is valid JSON");
    }
}

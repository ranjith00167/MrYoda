package com.mryoda.diagnostics.api.tests.order;

import com.mryoda.diagnostics.api.builders.RequestBuilder;
import com.mryoda.diagnostics.api.config.ConfigLoader;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.AssertionUtil;
import com.mryoda.diagnostics.api.utils.RequestContext;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * FREE MEMBERSHIP — CANCELLATION FLOW TEST
 *
 * Flow:
 *   Step 1 : POST /order/v2NewReturningCashback  — retrieve cashback info for the order
 *   Step 2 : POST /order/v2updateOrder           — cancel the order with remarks
 *
 * Both steps fully validate all fields returned in each API response.
 */
public class FreeMembershipCancellationFlowTest {

    private String adminToken;
    private String adminGuid;

    // ─────────────────────────────────────────────────────────────────────
    // STEP 1 : v2NewReturningCashback
    // ─────────────────────────────────────────────────────────────────────
    @Test(
        priority = 1,
        description = "Free Membership Cancellation - Step 1: GET Cashback Info via v2NewReturningCashback and verify all response fields"
    )
    public void step01_GetCashbackInfoForCancellation() {
        System.out.println("\n=================================================================");
        System.out.println("  FREE MEMBERSHIP CANCELLATION — STEP 1: v2NewReturningCashback");
        System.out.println("=================================================================");

        String orderGuid = RequestContext.getCurrentOrderId();
        AssertionUtil.verifyNotNull(orderGuid, "Order GUID must not be null before calling v2NewReturningCashback");
        System.out.println("   Order GUID : " + orderGuid);

        Map<String, Object> payload = new HashMap<>();
        payload.put("order_guid", orderGuid);

        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.NEW_RETURNING_CASHBACK;
        System.out.println("   Endpoint   : " + endpoint);
        System.out.println("   Payload    : " + payload);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + RequestContext.getToken())
                .setRequestBody(payload)
                .post();

        System.out.println("   HTTP Status : " + response.getStatusCode());
        System.out.println("   Response    : " + response.getBody().asString());

        // ── Top-level envelope validations ────────────────────────────────
        AssertionUtil.verifyEquals(response.getStatusCode(), 200,
                "v2NewReturningCashback must return HTTP 200");

        Integer apiStatus  = response.jsonPath().get("status");
        Boolean apiSuccess = response.jsonPath().getBoolean("success");
        String  apiMsg     = response.jsonPath().getString("msg");

        AssertionUtil.verifyEquals(apiStatus, 200,
                "v2NewReturningCashback: response.status must be 200");
        AssertionUtil.verifyTrue(Boolean.TRUE.equals(apiSuccess),
                "v2NewReturningCashback: response.success must be true");
        AssertionUtil.verifyNotNull(apiMsg,
                "v2NewReturningCashback: response.msg must be present");

        System.out.println("   status  : " + apiStatus);
        System.out.println("   success : " + apiSuccess);
        System.out.println("   msg     : " + apiMsg);

        // ── membershipCancelAmount fields ─────────────────────────────────
        Double paidAmount            = response.jsonPath().getDouble("data.membershipCancelAmount.paid_amount");
        Double orderItemAmount       = response.jsonPath().getDouble("data.membershipCancelAmount.orderItemAmount");
        Double actualPrice           = response.jsonPath().getDouble("data.membershipCancelAmount.actual_price");
        Double adjustedRefundAmount  = response.jsonPath().getDouble("data.membershipCancelAmount.adjustedRefundAmount");
        Double remainingRewards      = response.jsonPath().getDouble("data.membershipCancelAmount.remaining_rewards");
        Double takingRewards         = response.jsonPath().getDouble("data.membershipCancelAmount.actual_taking_rewards");
        String referenceCode         = response.jsonPath().getString("data.membershipCancelAmount.reference_code");
        String adjustedMessage       = response.jsonPath().getString("data.membershipCancelAmount.adjustedMessage");

        System.out.println("   paid_amount           : " + paidAmount);
        System.out.println("   orderItemAmount       : " + orderItemAmount);
        System.out.println("   actual_price          : " + actualPrice);
        System.out.println("   adjustedRefundAmount  : " + adjustedRefundAmount);
        System.out.println("   remaining_rewards     : " + remainingRewards);
        System.out.println("   actual_taking_rewards : " + takingRewards);
        System.out.println("   reference_code        : " + referenceCode);
        System.out.println("   adjustedMessage       : " + adjustedMessage);

        AssertionUtil.verifyNotNull(paidAmount,           "membershipCancelAmount.paid_amount must be present");
        AssertionUtil.verifyNotNull(orderItemAmount,      "membershipCancelAmount.orderItemAmount must be present");
        AssertionUtil.verifyNotNull(actualPrice,          "membershipCancelAmount.actual_price must be present");
        AssertionUtil.verifyNotNull(adjustedRefundAmount, "membershipCancelAmount.adjustedRefundAmount must be present");
        AssertionUtil.verifyNotNull(referenceCode,        "membershipCancelAmount.reference_code must be present");
        AssertionUtil.verifyNotNull(adjustedMessage,      "membershipCancelAmount.adjustedMessage must be present");

        AssertionUtil.verifyTrue(paidAmount >= 0,
                "membershipCancelAmount.paid_amount must be >= 0");
        AssertionUtil.verifyTrue(actualPrice >= paidAmount,
                "membershipCancelAmount.actual_price must be >= paid_amount");
        AssertionUtil.verifyTrue(
                Math.abs(orderItemAmount - paidAmount) < 0.01,
                "membershipCancelAmount.orderItemAmount must equal paid_amount");
        AssertionUtil.verifyTrue(
                Math.abs(adjustedRefundAmount - paidAmount) < 0.01,
                "membershipCancelAmount.adjustedRefundAmount must equal paid_amount");
        AssertionUtil.verifyTrue(referenceCode.startsWith("MY"),
                "membershipCancelAmount.reference_code must start with 'MY'");

        if (remainingRewards != null) {
            AssertionUtil.verifyTrue(remainingRewards >= 0,
                    "membershipCancelAmount.remaining_rewards must be >= 0");
        }
        if (takingRewards != null) {
            AssertionUtil.verifyTrue(takingRewards >= 0,
                    "membershipCancelAmount.actual_taking_rewards must be >= 0");
        }

        // ── canceled_amount nested fields ─────────────────────────────────
        Double cancelledCash           = response.jsonPath().getDouble("data.membershipCancelAmount.canceled_amount.orderedByCash");
        Double cancelledItemAmount     = response.jsonPath().getDouble("data.membershipCancelAmount.canceled_amount.orderItemAmount");
        Double cancelledActualPrice    = response.jsonPath().getDouble("data.membershipCancelAmount.canceled_amount.actual_price");
        Double cancelledRemainingRew   = response.jsonPath().getDouble("data.membershipCancelAmount.canceled_amount.remaining_rewards");
        Double cancelledTakingRew      = response.jsonPath().getDouble("data.membershipCancelAmount.canceled_amount.actual_taking_rewards");

        System.out.println("   canceled_amount.orderedByCash         : " + cancelledCash);
        System.out.println("   canceled_amount.orderItemAmount       : " + cancelledItemAmount);
        System.out.println("   canceled_amount.actual_price          : " + cancelledActualPrice);
        System.out.println("   canceled_amount.remaining_rewards     : " + cancelledRemainingRew);
        System.out.println("   canceled_amount.actual_taking_rewards : " + cancelledTakingRew);

        if (cancelledCash        != null) AssertionUtil.verifyTrue(cancelledCash        >= 0, "canceled_amount.orderedByCash must be >= 0");
        if (cancelledItemAmount  != null) AssertionUtil.verifyTrue(cancelledItemAmount  >= 0, "canceled_amount.orderItemAmount must be >= 0");
        if (cancelledActualPrice != null) AssertionUtil.verifyTrue(cancelledActualPrice >= 0, "canceled_amount.actual_price must be >= 0");
        if (cancelledRemainingRew!= null) AssertionUtil.verifyTrue(cancelledRemainingRew>= 0, "canceled_amount.remaining_rewards must be >= 0");
        if (cancelledTakingRew   != null) AssertionUtil.verifyTrue(cancelledTakingRew   >= 0, "canceled_amount.actual_taking_rewards must be >= 0");

        System.out.println("\n✅ STEP 1 PASSED: v2NewReturningCashback all fields validated successfully.");
    }

    // ─────────────────────────────────────────────────────────────────────
    // STEP 2 : v2updateOrder — Cancel Order
    // ─────────────────────────────────────────────────────────────────────
    @Test(
        priority = 2,
        dependsOnMethods = "step01_GetCashbackInfoForCancellation",
        description = "Free Membership Cancellation - Step 2: Cancel order via v2updateOrder and verify all response fields"
    )
    public void step02_CancelOrder() {
        System.out.println("\n=================================================================");
        System.out.println("  FREE MEMBERSHIP CANCELLATION — STEP 2: v2updateOrder (Cancel)");
        System.out.println("=================================================================");

        String orderGuid = RequestContext.getCurrentOrderId();
        AssertionUtil.verifyNotNull(orderGuid, "Order GUID must not be null before calling v2updateOrder");
        System.out.println("   Order GUID : " + orderGuid);

        Map<String, Object> payload = new HashMap<>();
        payload.put("order_guid",         orderGuid);
        payload.put("order_status",       "Cancelled");
        payload.put("cancelled_remarks",  "test");

        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.UPDATE_ORDER;
        System.out.println("   Endpoint   : " + endpoint);
        System.out.println("   Payload    : " + payload);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + RequestContext.getToken())
                .setRequestBody(payload)
                .post();

        System.out.println("   HTTP Status : " + response.getStatusCode());
        System.out.println("   Response    : " + response.getBody().asString());

        // ── Status code ───────────────────────────────────────────────────
        AssertionUtil.verifyEquals(response.getStatusCode(), 200,
                "v2updateOrder (Cancel) must return HTTP 200");

        // ── Response body fields ──────────────────────────────────────────
        String msg     = response.jsonPath().getString("msg");
        Boolean success = response.jsonPath().getBoolean("success");
        Integer status  = response.jsonPath().get("status");

        System.out.println("   msg     : " + msg);
        System.out.println("   success : " + success);
        System.out.println("   status  : " + status);

        AssertionUtil.verifyNotNull(msg,
                "v2updateOrder: response must contain a 'msg' field");
        AssertionUtil.verifyEquals(msg, "Order updated successfully",
                "v2updateOrder: msg should be 'Order updated successfully'");

        if (success != null) {
            AssertionUtil.verifyTrue(Boolean.TRUE.equals(success),
                    "v2updateOrder: response.success must be true");
        }
        if (status != null) {
            AssertionUtil.verifyEquals(status, 200,
                    "v2updateOrder: response.status must be 200");
        }

        // ── Cross-verify via GET order ────────────────────────────────────
        System.out.println("\n   Cross-verifying order status via GET order...");
        String getEndpoint = APIEndpoints.DIAGNOSTICS_BASE_URL
                + APIEndpoints.GET_ORDER_BY_ID + orderGuid;

        Response getResponse = new RequestBuilder()
                .setEndpoint(getEndpoint)
                .addHeader("Authorization", "Bearer " + RequestContext.getToken())
                .get();

        System.out.println("   GET HTTP Status : " + getResponse.getStatusCode());
        AssertionUtil.verifyEquals(getResponse.getStatusCode(), 200,
                "GET order after cancellation must return 200");

        // order_status can be at data.order_status or data[0].order_status
        Object statusObj = getResponse.jsonPath().get("data.order_status");
        if (statusObj == null) {
            statusObj = getResponse.jsonPath().get("data[0].order_status");
        }

        String orderStatus;
        if (statusObj instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) statusObj;
            orderStatus = list.isEmpty() ? null : list.get(0).toString();
        } else {
            orderStatus = statusObj != null ? statusObj.toString() : null;
        }

        System.out.println("   Confirmed order_status : " + orderStatus);
        AssertionUtil.verifyEquals(orderStatus, "Cancelled",
                "Order status must be 'Cancelled' after v2updateOrder");

        System.out.println("\n✅ STEP 2 PASSED: Order successfully cancelled and status confirmed as 'Cancelled'.");
    }

    // ─────────────────────────────────────────────────────────────────────
    // STEP 3 : Admin Login
    // ─────────────────────────────────────────────────────────────────────
    @Test(
        priority = 3,
        dependsOnMethods = "step02_CancelOrder",
        description = "Free Membership Cancellation - Step 3: Admin Login to obtain token and GUID for approving the cancellation"
    )
    public void step03_AdminLogin() {
        System.out.println("\n=================================================================");
        System.out.println("  FREE MEMBERSHIP CANCELLATION — STEP 3: ADMIN LOGIN");
        System.out.println("=================================================================");

        String adminIdentifier = ConfigLoader.getConfig().adminMainIdentifier();
        String adminPassword   = ConfigLoader.getConfig().adminMainPassword();

        Map<String, Object> payload = new HashMap<>();
        payload.put("identifier", adminIdentifier);
        payload.put("user_name",  "admin");
        payload.put("password",   adminPassword);
        payload.put("type",       "login");
        payload.put("fcmToken",   "ec0gPKSrIUs443ILfDLHaM:APA91bG6Ax2ZisptMxd2dPgpfNTmdRRsaXmXYmT3TuOWleJsBgyf9TSpZ-NwcJdqa_TmjRb33gyfjAK69KNo8WiW_8V9_ov3PM6UsYHvJyBmiv-B6M5KAuQ");

        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADMIN_LOGIN;
        System.out.println("   Endpoint   : " + endpoint);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .setRequestBody(payload)
                .post();

        System.out.println("   HTTP Status : " + response.getStatusCode());
        System.out.println("   Response    : " + response.getBody().asString());

        AssertionUtil.verifyEquals(response.getStatusCode(), 200,
                "Admin login must return HTTP 200");

        adminToken = response.jsonPath().getString("data.access_token");
        adminGuid  = response.jsonPath().getString("data.userdData.user_guid");

        AssertionUtil.verifyNotNull(adminToken, "Admin access_token must not be null after login");
        AssertionUtil.verifyNotNull(adminGuid,  "Admin user_guid must not be null after login");

        System.out.println("   Admin GUID  : " + adminGuid);
        System.out.println("   Admin Token : " + adminToken.substring(0, Math.min(30, adminToken.length())) + "...");
        System.out.println("\n✅ STEP 3 PASSED: Admin login successful.");
    }

    // ─────────────────────────────────────────────────────────────────────
    // STEP 4 : Approve Cancellation via approveCancelldOrder
    // ─────────────────────────────────────────────────────────────────────
    @Test(
        priority = 4,
        dependsOnMethods = "step03_AdminLogin",
        description = "Free Membership Cancellation - Step 4: Admin approves the cancellation via approveCancelldOrder API"
    )
    public void step04_ApproveCancellation() {
        System.out.println("\n=================================================================");
        System.out.println("  FREE MEMBERSHIP CANCELLATION — STEP 4: APPROVE CANCELLATION");
        System.out.println("=================================================================");

        String orderGuid = RequestContext.getCurrentOrderId();
        AssertionUtil.verifyNotNull(orderGuid,  "Order GUID must not be null before calling approveCancelldOrder");
        AssertionUtil.verifyNotNull(adminToken, "Admin token must not be null");
        AssertionUtil.verifyNotNull(adminGuid,  "Admin GUID must not be null");

        System.out.println("   Order GUID  : " + orderGuid);
        System.out.println("   Admin GUID  : " + adminGuid);

        Map<String, Object> payload = new HashMap<>();
        payload.put("order_id",          orderGuid);
        payload.put("remarks",           "test");
        payload.put("admin_approval_by", adminGuid);
        payload.put("status",            "Approve");

        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.APPROVE_CANCELLED_ORDER;
        System.out.println("   Endpoint    : " + endpoint);
        System.out.println("   Payload     : " + payload);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + adminToken)
                .addHeader("type", "Mr.Yoda-Admin")
                .setRequestBody(payload)
                .post();

        System.out.println("   HTTP Status : " + response.getStatusCode());
        System.out.println("   Response    : " + response.getBody().asString());

        // Idempotency: if already approved, treat as success
        String responseMsg = response.jsonPath().getString("msg");
        if (responseMsg == null) responseMsg = response.jsonPath().getString("message");

        boolean alreadyApproved = response.getStatusCode() == 400
                && responseMsg != null
                && responseMsg.toLowerCase().contains("already approved");

        if (alreadyApproved) {
            System.out.println("   ℹ️  Order was already approved — treating as idempotent success.");
            System.out.println("\n✅ STEP 4 PASSED: Cancellation already approved (idempotent).");
            return;
        }

        AssertionUtil.verifyEquals(response.getStatusCode(), 200,
                "approveCancelldOrder must return HTTP 200");

        Boolean success = response.jsonPath().getBoolean("success");
        if (success != null) {
            AssertionUtil.verifyTrue(Boolean.TRUE.equals(success),
                    "approveCancelldOrder: success flag must be true");
        }

        AssertionUtil.verifyNotNull(responseMsg,
                "approveCancelldOrder: response must contain a msg/message field");

        System.out.println("   msg     : " + responseMsg);
        System.out.println("   success : " + success);
        System.out.println("\n✅ STEP 4 PASSED: Cancellation approved successfully by admin.");
    }
}

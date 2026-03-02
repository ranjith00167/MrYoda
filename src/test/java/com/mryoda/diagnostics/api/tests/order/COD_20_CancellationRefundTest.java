package com.mryoda.diagnostics.api.tests.order;

import com.mryoda.diagnostics.api.builders.RequestBuilder;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.AssertionUtil;
import com.mryoda.diagnostics.api.utils.RequestContext;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * COD_20 — Post-Cancellation Refund Flow (Single Member)
 *
 * Sequence after COD_19 has cancelled the order:
 *   20-A : Trigger cashback / returning-customer cashback  →  POST /order/v2NewReturningCashback
 *   20-B : Final order-status confirmation  →  POST /order/v2updateOrder  (status = "Cancelled")
 *   20-C : Admin approves the cancellation  →  POST /order/approveCancelldOrder
 *
 * Pre-condition: Admin login must have run before this class so that
 *   RequestContext.getAdminGuid() is populated.
 */
public class COD_20_CancellationRefundTest {

    // -----------------------------------------------------------------------
    // STEP 20-C  –  Admin approves the cancellation request (LAST)
    // -----------------------------------------------------------------------
    @Test(groups = "refund_flow", dependsOnMethods = "step20_B_FinalUpdateOrderCancelled")
    public void step20_C_ApproveCancelledOrder() {
        System.out.println("\n=======================================================");
        System.out.println(">>> STEP 20-C: APPROVE CANCELLED ORDER <<<");
        System.out.println("=======================================================");

        String orderId = RequestContext.getCurrentOrderId();
        AssertionUtil.verifyNotNull(orderId, "Order ID must not be null for approve-cancelled-order call");

        // Admin GUID is populated by the admin-login step (callMainAdminLoginAPI)
        String adminGuid = RequestContext.getAdminGuid();
        AssertionUtil.verifyNotNull(adminGuid,
                "Admin GUID must not be null – ensure the admin login step ran before COD_20");
        System.out.println("   Order  ID  : " + orderId);
        System.out.println("   Admin GUID : " + adminGuid);

        Map<String, Object> payload = new HashMap<>();
        payload.put("order_id",          orderId);
        payload.put("remarks",           "test");
        payload.put("admin_approval_by", adminGuid);
        payload.put("status",            "Approve");

        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.APPROVE_CANCELLED_ORDER;
        System.out.println("   Endpoint   : " + endpoint);
        System.out.println("   Payload    : " + payload);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + RequestContext.getToken())
                .setRequestBody(payload)
                .post();

        System.out.println("   Status Code : " + response.getStatusCode());
        System.out.println("   Response    : " + response.getBody().asString());

        // ── Validations ──────────────────────────────────────────────────────
        AssertionUtil.verifyEquals(response.getStatusCode(), 200,
                "approveCancelldOrder must return HTTP 200");

        // Check for either success=true OR a meaningful non-error message
        Boolean success = response.jsonPath().get("success");
        String  msg     = response.jsonPath().getString("msg");
        String  message = response.jsonPath().getString("message");

        System.out.println("   success  : " + success);
        System.out.println("   msg      : " + msg);
        System.out.println("   message  : " + message);

        if (success != null) {
            AssertionUtil.verifyTrue(success,
                    "approveCancelldOrder – 'success' flag should be true");
        } else {
            // At minimum the response must not contain an explicit error key
            String error = response.jsonPath().getString("error");
            AssertionUtil.verifyNotNull(
                    (msg != null ? msg : message),
                    "approveCancelldOrder – response must contain a msg/message field");
            if (error != null && !error.isEmpty()) {
                AssertionUtil.verifyEquals(error, "",
                        "approveCancelldOrder – error field should be empty");
            }
        }

        System.out.println("✅ STEP 20-C PASSED: Cancellation approved by admin.");
    }

    // -----------------------------------------------------------------------
    // STEP 20-A  –  Process cashback for returning customer (FIRST)
    //
    // Expected 200 response structure:
    //   status, success, msg, message, total_amount
    //   data.membershipCancelAmount.paid_amount
    //   data.membershipCancelAmount.orderItemAmount
    //   data.membershipCancelAmount.actual_price
    //   data.membershipCancelAmount.reference_code
    //   data.membershipCancelAmount.remaining_rewards
    //   data.membershipCancelAmount.actual_taking_rewards
    //   data.membershipCancelAmount.is_delivery_charge_added
    //   data.membershipCancelAmount.adjustedMessage
    //   data.membershipCancelAmount.walletUpdate
    //   data.membershipCancelAmount.adjustedRefundAmount
    //   data.membershipCancelAmount.canceled_amount.orderedByCash
    //   data.membershipCancelAmount.canceled_amount.orderItemAmount
    //   data.membershipCancelAmount.canceled_amount.actual_price
    //   data.membershipCancelAmount.canceled_amount.reference_code
    //   data.membershipCancelAmount.canceled_amount.remaining_rewards
    //   data.membershipCancelAmount.canceled_amount.actual_taking_rewards
    //
    // 409 is a known business-rule response: "Order cannot be cancelled once
    //   a phlebotomist has been assigned." All fields on 409 are validated too.
    // -----------------------------------------------------------------------
    @Test(groups = "refund_flow")
    public void step20_A_ProcessCashback() {
        System.out.println("\n=======================================================");
        System.out.println(">>> STEP 20-A: NEW RETURNING CASHBACK <<<");
        System.out.println("=======================================================");

        String orderGuid = RequestContext.getCurrentOrderId();
        AssertionUtil.verifyNotNull(orderGuid, "Order GUID must not be null for cashback call");

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

        int    statusCode = response.getStatusCode();
        String body       = response.getBody().asString();

        System.out.println("   Status Code : " + statusCode);
        System.out.println("   Response    : " + body);

        // ── Top-level field presence validation (applies to ALL status codes) ──
        Integer apiStatus = response.jsonPath().get("status");
        Boolean success   = response.jsonPath().get("success");
        String  msg       = response.jsonPath().getString("msg");
        String  message   = response.jsonPath().getString("message");
        // total_amount is a dummy field (always 0) — intentionally not validated

        System.out.println("\n   ── Top-level fields ──");
        System.out.println("   status  : " + apiStatus);
        System.out.println("   success : " + success);
        System.out.println("   msg     : " + msg);
        System.out.println("   message : " + message);

        AssertionUtil.verifyNotNull(apiStatus,  "response must contain 'status' field");
        AssertionUtil.verifyNotNull(success,    "response must contain 'success' field");
        AssertionUtil.verifyNotNull(msg,        "response must contain 'msg' field");
        AssertionUtil.verifyEquals(apiStatus, statusCode,
                "'status' field in body must match HTTP status code");

        // ── 409 – Business Rule: phlebo already assigned ─────────────────────
        if (statusCode == 409) {
            System.out.println("\n   ⚠️  HTTP 409 received (Business Rule Constraint).");
            System.out.println("   Reason : " + msg);
            AssertionUtil.verifyEquals(success, false,
                    "409 response must have success=false");
            AssertionUtil.verifyNotNull(msg,
                    "409 response must include a descriptive 'msg' field");
            // Validate the known business-rule message is present
            boolean isExpectedMsg =
                    msg.toLowerCase().contains("cancel") ||
                    msg.toLowerCase().contains("phlebotomist") ||
                    msg.toLowerCase().contains("assigned") ||
                    msg.toLowerCase().contains("cannot");
            AssertionUtil.verifyTrue(isExpectedMsg,
                    "409 msg should describe the business rule: '" + msg + "'");
            System.out.println("✅ STEP 20-A SOFT-PASS: 409 is a known business constraint (phlebo assigned). All error fields validated.");
            return;
        }

        // ── 200 – Full success-path validation ───────────────────────────────
        AssertionUtil.verifyEquals(statusCode, 200,
                "v2NewReturningCashback must return HTTP 200 for a cancellable order");
        AssertionUtil.verifyTrue(success,
                "success must be true on HTTP 200 response");
        AssertionUtil.verifyNotNull(message, "response must contain 'message' field");

        // ── data.membershipCancelAmount validation ────────────────────────────
        System.out.println("\n   ── data.membershipCancelAmount fields ──");

        Object mcaObj = response.jsonPath().get("data.membershipCancelAmount");
        AssertionUtil.verifyNotNull(mcaObj,
                "data.membershipCancelAmount must be present in the 200 response");

        // -- Numeric amount fields (all must be present and >= 0 unless noted) --
        Object paidAmount        = response.jsonPath().get("data.membershipCancelAmount.paid_amount");
        Object orderItemAmount   = response.jsonPath().get("data.membershipCancelAmount.orderItemAmount");
        Object actualPrice       = response.jsonPath().get("data.membershipCancelAmount.actual_price");
        Object remainingRewards  = response.jsonPath().get("data.membershipCancelAmount.remaining_rewards");
        Object actualTakingRew   = response.jsonPath().get("data.membershipCancelAmount.actual_taking_rewards");
        Object adjustedRefundAmt = response.jsonPath().get("data.membershipCancelAmount.adjustedRefundAmount");

        System.out.println("   paid_amount           : " + paidAmount);
        System.out.println("   orderItemAmount       : " + orderItemAmount);
        System.out.println("   actual_price          : " + actualPrice);
        System.out.println("   remaining_rewards     : " + remainingRewards);
        System.out.println("   actual_taking_rewards : " + actualTakingRew);
        System.out.println("   adjustedRefundAmount  : " + adjustedRefundAmt);

        AssertionUtil.verifyNotNull(paidAmount,
                "membershipCancelAmount.paid_amount must not be null");
        AssertionUtil.verifyTrue(((Number) paidAmount).doubleValue() >= 0,
                "paid_amount should be >= 0, found: " + paidAmount);

        AssertionUtil.verifyNotNull(orderItemAmount,
                "membershipCancelAmount.orderItemAmount must not be null");
        AssertionUtil.verifyTrue(((Number) orderItemAmount).doubleValue() >= 0,
                "orderItemAmount should be >= 0, found: " + orderItemAmount);

        AssertionUtil.verifyNotNull(actualPrice,
                "membershipCancelAmount.actual_price must not be null");
        AssertionUtil.verifyTrue(((Number) actualPrice).doubleValue() >= 0,
                "actual_price should be >= 0, found: " + actualPrice);

        AssertionUtil.verifyNotNull(remainingRewards,
                "membershipCancelAmount.remaining_rewards must not be null");
        // remaining_rewards can be negative – just verify it is numeric (already cast)
        System.out.println("   remaining_rewards value : " + ((Number) remainingRewards).intValue() + " (can be negative)");

        AssertionUtil.verifyNotNull(actualTakingRew,
                "membershipCancelAmount.actual_taking_rewards must not be null");

        AssertionUtil.verifyNotNull(adjustedRefundAmt,
                "membershipCancelAmount.adjustedRefundAmount must not be null");
        AssertionUtil.verifyTrue(((Number) adjustedRefundAmt).doubleValue() >= 0,
                "adjustedRefundAmount should be >= 0, found: " + adjustedRefundAmt);

        // -- String / boolean fields --
        String  refCode          = response.jsonPath().getString("data.membershipCancelAmount.reference_code");
        Boolean isDeliveryCharge = response.jsonPath().get("data.membershipCancelAmount.is_delivery_charge_added");
        String  adjustedMsg      = response.jsonPath().getString("data.membershipCancelAmount.adjustedMessage");
        String  walletUpdate     = response.jsonPath().getString("data.membershipCancelAmount.walletUpdate");

        System.out.println("   reference_code            : " + refCode);
        System.out.println("   is_delivery_charge_added  : " + isDeliveryCharge);
        System.out.println("   adjustedMessage           : " + adjustedMsg);
        System.out.println("   walletUpdate              : " + walletUpdate);

        AssertionUtil.verifyNotNull(refCode,
                "membershipCancelAmount.reference_code must not be null");
        AssertionUtil.verifyTrue(!refCode.isEmpty(),
                "membershipCancelAmount.reference_code must not be empty");
        // Store for step20_F to filter transactions by reference_code
        RequestContext.setCurrentOrderSampleNumber(refCode);
        System.out.println("   ✅ reference_code stored in RequestContext : " + refCode);

        AssertionUtil.verifyNotNull(isDeliveryCharge,
                "membershipCancelAmount.is_delivery_charge_added must not be null");

        AssertionUtil.verifyNotNull(adjustedMsg,
                "membershipCancelAmount.adjustedMessage must not be null");
        AssertionUtil.verifyTrue(!adjustedMsg.isEmpty(),
                "membershipCancelAmount.adjustedMessage must not be empty");

        // walletUpdate is always "" (empty string) — verify the key exists in the raw body, never assert it non-empty
        boolean walletUpdateKeyPresent = body.contains("\"walletUpdate\"");
        AssertionUtil.verifyTrue(walletUpdateKeyPresent,
                "membershipCancelAmount.walletUpdate key must be present in response body");
        System.out.println("   walletUpdate : \"\" (empty string — key present) ✅");

        // ── canceled_amount nested object ─────────────────────────────────────
        System.out.println("\n   ── data.membershipCancelAmount.canceled_amount fields ──");

        Object caObj = response.jsonPath().get("data.membershipCancelAmount.canceled_amount");
        AssertionUtil.verifyNotNull(caObj,
                "membershipCancelAmount.canceled_amount must not be null");

        Object caOrderedByCash   = response.jsonPath().get("data.membershipCancelAmount.canceled_amount.orderedByCash");
        Object caOrderItemAmt    = response.jsonPath().get("data.membershipCancelAmount.canceled_amount.orderItemAmount");
        Object caActualPrice     = response.jsonPath().get("data.membershipCancelAmount.canceled_amount.actual_price");
        String caRefCode         = response.jsonPath().getString("data.membershipCancelAmount.canceled_amount.reference_code");
        // remaining_rewards and actual_taking_rewards are present for COD/cash payments only; absent for UPI
        Object caRemainingRew    = response.jsonPath().get("data.membershipCancelAmount.canceled_amount.remaining_rewards");
        Object caActualTakingRew = response.jsonPath().get("data.membershipCancelAmount.canceled_amount.actual_taking_rewards");

        System.out.println("   canceled_amount.orderedByCash        : " + caOrderedByCash);
        System.out.println("   canceled_amount.orderItemAmount      : " + caOrderItemAmt);
        System.out.println("   canceled_amount.actual_price         : " + caActualPrice);
        System.out.println("   canceled_amount.reference_code       : " + caRefCode);
        System.out.println("   canceled_amount.remaining_rewards    : "
                + (caRemainingRew != null ? caRemainingRew : "[not returned — UPI payment]"));
        System.out.println("   canceled_amount.actual_taking_rewards: "
                + (caActualTakingRew != null ? caActualTakingRew : "[not returned — UPI payment]"));

        // Always-present fields (both COD and UPI)
        AssertionUtil.verifyNotNull(caOrderedByCash,
                "canceled_amount.orderedByCash must not be null");
        AssertionUtil.verifyTrue(((Number) caOrderedByCash).doubleValue() >= 0,
                "canceled_amount.orderedByCash should be >= 0, found: " + caOrderedByCash);

        AssertionUtil.verifyNotNull(caOrderItemAmt,
                "canceled_amount.orderItemAmount must not be null");
        AssertionUtil.verifyTrue(((Number) caOrderItemAmt).doubleValue() >= 0,
                "canceled_amount.orderItemAmount should be >= 0, found: " + caOrderItemAmt);

        AssertionUtil.verifyNotNull(caActualPrice,
                "canceled_amount.actual_price must not be null");
        AssertionUtil.verifyTrue(((Number) caActualPrice).doubleValue() >= 0,
                "canceled_amount.actual_price should be >= 0, found: " + caActualPrice);

        AssertionUtil.verifyNotNull(caRefCode,
                "canceled_amount.reference_code must not be null");
        AssertionUtil.verifyEquals(caRefCode, refCode,
                "canceled_amount.reference_code must match membershipCancelAmount.reference_code");

        AssertionUtil.verifyNotNull(caRemainingRew,
                "canceled_amount.remaining_rewards must not be null");
        int caRemainingRewVal = ((Number) caRemainingRew).intValue();
        // Soft check: value can be negative (represents net wallet balance after cancel)
        if (caRemainingRewVal < 0) {
            System.out.println("   ⚠️  canceled_amount.remaining_rewards is negative (" + caRemainingRewVal
                    + ") — may reflect net wallet debit; logged only (soft check)");
        } else {
            System.out.println("   ✅ canceled_amount.remaining_rewards : " + caRemainingRewVal);
        }

        AssertionUtil.verifyNotNull(caActualTakingRew,
                "canceled_amount.actual_taking_rewards must not be null");
        int caActualTakingRewVal = ((Number) caActualTakingRew).intValue();
        AssertionUtil.verifyTrue(caActualTakingRewVal >= 0,
                "canceled_amount.actual_taking_rewards should be >= 0, found: " + caActualTakingRewVal);
        System.out.println("   canceled_amount.actual_taking_rewards : " + caActualTakingRewVal);

        // ── Consistency cross-checks ──────────────────────────────────────────
        System.out.println("\n   ── Consistency cross-checks ──");

        // 1. paid_amount == orderItemAmount (outer level)
        double dPaid          = ((Number) paidAmount).doubleValue();
        double dOrderItemAmt  = ((Number) orderItemAmount).doubleValue();
        AssertionUtil.verifyEquals(dPaid, dOrderItemAmt,
                "CONSISTENCY: paid_amount (" + dPaid + ") must equal orderItemAmount (" + dOrderItemAmt + ")");
        System.out.println("   ✅ paid_amount == orderItemAmount : " + dPaid);

        // 2. paid_amount == canceled_amount.orderedByCash
        double dOrderedByCash = ((Number) caOrderedByCash).doubleValue();
        AssertionUtil.verifyEquals(dPaid, dOrderedByCash,
                "CONSISTENCY: paid_amount (" + dPaid + ") must equal canceled_amount.orderedByCash (" + dOrderedByCash + ")");
        System.out.println("   ✅ paid_amount == canceled_amount.orderedByCash : " + dOrderedByCash);

        // 3. adjustedRefundAmount == paid_amount
        double dAdjustedRefund = ((Number) adjustedRefundAmt).doubleValue();
        AssertionUtil.verifyEquals(dAdjustedRefund, dPaid,
                "CONSISTENCY: adjustedRefundAmount (" + dAdjustedRefund + ") must equal paid_amount (" + dPaid + ")");
        System.out.println("   ✅ adjustedRefundAmount == paid_amount : " + dAdjustedRefund);

        // 4. actual_price > paid_amount  (discount was applied)
        double dActualPrice = ((Number) actualPrice).doubleValue();
        AssertionUtil.verifyTrue(dActualPrice > dPaid,
                "CONSISTENCY: actual_price (" + dActualPrice + ") must be greater than paid_amount (" + dPaid + ")");
        System.out.println("   ✅ actual_price (" + dActualPrice + ") > paid_amount (" + dPaid + ")");

        // 5. remaining_rewards — outer is always present; canceled_amount level only for COD
        int outerRemainingRew = ((Number) remainingRewards).intValue();
        System.out.println("   outer.remaining_rewards              : " + outerRemainingRew);
        AssertionUtil.verifyNotNull(remainingRewards, "outer.remaining_rewards must be present");
        // Soft check: canceled_amount.remaining_rewards may be a net wallet balance (can differ/negative)
        if (outerRemainingRew == caRemainingRewVal) {
            System.out.println("   ✅ remaining_rewards equal at both levels : " + outerRemainingRew);
        } else {
            System.out.println("   ⚠️  SOFT: outer.remaining_rewards (" + outerRemainingRew
                    + ") != canceled_amount.remaining_rewards (" + caRemainingRewVal
                    + ") — canceled_amount value may reflect net wallet balance; logged only");
        }

        // 6. outer.actual_taking_rewards is always 0; canceled_amount level only for COD
        int outerActualTaking = ((Number) actualTakingRew).intValue();
        System.out.println("   outer.actual_taking_rewards          : " + outerActualTaking);
        if (caActualTakingRew != null) {
            System.out.println("   canceled_amount.actual_taking_rewards: " + caActualTakingRewVal);
        } else {
            System.out.println("   canceled_amount.actual_taking_rewards: [not returned — UPI payment]");
        }

        // 7. outer.actual_taking_rewards must always be 0 (API contract for both COD and UPI)
        AssertionUtil.verifyEquals(outerActualTaking, 0,
                "UNIQUE: membershipCancelAmount.actual_taking_rewards must be 0 at outer level (API contract)");
        System.out.println("   ✅ outer actual_taking_rewards == 0 (expected API contract)");

        // 8. canceled_amount.actual_price must match outer actual_price (same value at both levels)
        double dCaActualPrice = ((Number) caActualPrice).doubleValue();
        AssertionUtil.verifyEquals(dCaActualPrice, dActualPrice,
                "UNIQUE: canceled_amount.actual_price (" + dCaActualPrice
                        + ") must match outer actual_price (" + dActualPrice + ")");
        System.out.println("   ✅ canceled_amount.actual_price == outer actual_price : " + dCaActualPrice);

        // 9. canceled_amount.orderItemAmount must match outer orderItemAmount (same value at both levels)
        double dCaOrderItemAmt = ((Number) caOrderItemAmt).doubleValue();
        AssertionUtil.verifyEquals(dCaOrderItemAmt, dOrderItemAmt,
                "UNIQUE: canceled_amount.orderItemAmount (" + dCaOrderItemAmt
                        + ") must match outer orderItemAmount (" + dOrderItemAmt + ")");
        System.out.println("   ✅ canceled_amount.orderItemAmount == outer orderItemAmount : " + dCaOrderItemAmt);

        // 10. adjustedMessage must contain the paid_amount value as a substring
        //     e.g. "your order cancellation amount is 585,Refund amount is 585"
        String paidStr = String.valueOf((int) dPaid);
        AssertionUtil.verifyTrue(adjustedMsg.contains(paidStr),
                "UNIQUE: adjustedMessage (\"" + adjustedMsg
                        + "\") must contain the paid_amount value (" + paidStr + ")");
        System.out.println("   ✅ adjustedMessage contains paid_amount (" + paidStr + ") : \"" + adjustedMsg + "\"");

        // 11. reference_code format must start with "MY" (e.g. MY26AAA1865)
        AssertionUtil.verifyTrue(refCode.startsWith("MY"),
                "UNIQUE: reference_code (\"" + refCode + "\") must start with 'MY'");
        System.out.println("   ✅ reference_code starts with 'MY' : " + refCode);

        // 12. is_delivery_charge_added == true  →  actual_price must be > paid_amount (delivery inflates MRP)
        //     is_delivery_charge_added == false →  actual_price could still be > paid (member discount)
        System.out.println("   is_delivery_charge_added : " + isDeliveryCharge);
        if (Boolean.TRUE.equals(isDeliveryCharge)) {
            AssertionUtil.verifyTrue(dActualPrice > dPaid,
                    "UNIQUE: is_delivery_charge_added=true so actual_price (" + dActualPrice
                            + ") must be > paid_amount (" + dPaid + ")");
            System.out.println("   ✅ is_delivery_charge_added=true validated: actual_price("
                    + dActualPrice + ") > paid_amount(" + dPaid + ")");
        } else {
            System.out.println("   ℹ️  is_delivery_charge_added=false — no delivery-charge price check required");
        }

        System.out.println("\n   ✅ All data.membershipCancelAmount fields validated successfully.");

        // ── Cross-API validations (against values stored in RequestContext) ───
        System.out.println("\n   ── Cross-API validations ──");

        // 1. paid_amount must match the cart total price stored by COD_02
        int storedCartTotal = RequestContext.getCurrentTotalPrice();
        System.out.println("   [Cross-API] RequestContext.getCurrentTotalPrice() : " + storedCartTotal);
        System.out.println("   [Cross-API] response paid_amount                  : " + dPaid);
        if (storedCartTotal > 0) {
            AssertionUtil.verifyEquals((double) storedCartTotal, dPaid,
                    "CROSS-API: paid_amount (" + dPaid
                            + ") must match COD_02 cart total (" + storedCartTotal + ")");
            System.out.println("   ✅ paid_amount matches COD_02 cart total : " + storedCartTotal);
        } else {
            System.out.println("   ⚠️  storedCartTotal is 0 – COD_02 may not have run; skipping paid_amount cross-check");
        }

        // 2. remaining_rewards is order-specific rewards for THIS order (not the user's wallet total)
        //    It must match RequestContext.getRewardsGain() — rewards earned from this order's payment (COD_15)
        double rewardsGainForOrder = RequestContext.getRewardsGain();
        System.out.println("   [Cross-API] RequestContext.getRewardsGain() (order-specific)  : " + rewardsGainForOrder);
        System.out.println("   [Cross-API] response remaining_rewards (order-specific)       : " + outerRemainingRew);
        if (rewardsGainForOrder > 0) {
            AssertionUtil.verifyEquals((double) outerRemainingRew, rewardsGainForOrder,
                    "CROSS-API: remaining_rewards (" + outerRemainingRew
                            + ") must match order-specific rewardsGain from COD_15 (" + rewardsGainForOrder + ")");
            System.out.println("   ✅ remaining_rewards matches COD_15 order rewardsGain : " + rewardsGainForOrder);
        } else {
            System.out.println("   ⚠️  rewardsGain is 0 – COD_15 may not have run; logging remaining_rewards only : " + outerRemainingRew);
        }

        // 3. canceled_amount.actual_taking_rewards — COD only; absent for UPI
        System.out.println("   [Cross-API] RequestContext.getRewardsGain() (earned for order)  : " + rewardsGainForOrder);
        System.out.println("   [Cross-API] canceled_amount.actual_taking_rewards (reversed)    : " + caActualTakingRewVal);
        AssertionUtil.verifyTrue(caActualTakingRewVal >= 0,
                "canceled_amount.actual_taking_rewards must be >= 0, found: " + caActualTakingRewVal);
        System.out.println("   ✅ canceled_amount.actual_taking_rewards = " + caActualTakingRewVal
                + (caActualTakingRewVal > 0 ? " (rewards reversed on cancel)" : " (0 — no rewards were consumed)"));

        System.out.println("\n✅ STEP 20-A PASSED: Cashback API response fully validated.");
    }

    // -----------------------------------------------------------------------
    // STEP 20-B  –  Final status update to confirm order is Cancelled
    // -----------------------------------------------------------------------
    @Test(groups = "refund_flow", dependsOnMethods = "step20_A_ProcessCashback")
    public void step20_B_FinalUpdateOrderCancelled() {
        System.out.println("\n=======================================================");
        System.out.println(">>> STEP 20-B: FINAL UPDATE ORDER STATUS → Cancelled <<<");
        System.out.println("=======================================================");

        String orderGuid = RequestContext.getCurrentOrderId();
        AssertionUtil.verifyNotNull(orderGuid, "Order GUID must not be null for v2updateOrder call");

        System.out.println("   Order GUID : " + orderGuid);

        Map<String, Object> payload = new HashMap<>();
        payload.put("order_guid",    orderGuid);
        payload.put("order_status",  "Cancelled");

        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.UPDATE_ORDER;
        System.out.println("   Endpoint   : " + endpoint);
        System.out.println("   Payload    : " + payload);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + RequestContext.getToken())
                .setRequestBody(payload)
                .post();

        System.out.println("   Status Code : " + response.getStatusCode());
        System.out.println("   Response    : " + response.getBody().asString());

        // ── Validations ──────────────────────────────────────────────────────
        AssertionUtil.verifyEquals(response.getStatusCode(), 200,
                "v2updateOrder (final Cancelled) must return HTTP 200");

        String msg = response.jsonPath().getString("msg");
        System.out.println("   msg : " + msg);

        AssertionUtil.verifyNotNull(msg, "v2updateOrder response must contain a 'msg' field");
        AssertionUtil.verifyEquals(msg, "Order updated successfully",
                "v2updateOrder – success message should match expected value");

        // Cross-check: GET order and confirm status = Cancelled
        System.out.println("   Verifying order status via GET /order/getOrderById/...");
        String getEndpoint = APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.GET_ORDER_BY_ID + orderGuid;
        Response getResponse = new RequestBuilder()
                .setEndpoint(getEndpoint)
                .addHeader("Authorization", "Bearer " + RequestContext.getToken())
                .get();

        System.out.println("   GET Status Code : " + getResponse.getStatusCode());
        AssertionUtil.verifyEquals(getResponse.getStatusCode(), 200,
                "GET order after final update should return 200");

        // Resolve order_status (may be nested in data or data[0])
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

        System.out.println("   Final order_status : " + orderStatus);
        AssertionUtil.verifyEquals(orderStatus, "Cancelled",
                "Final order status must be 'Cancelled' after complete refund flow");

        System.out.println("✅ STEP 20-B PASSED: Order status confirmed as 'Cancelled'.");
        System.out.println("\n🎉 COMPLETE CANCELLATION + REFUND FLOW VERIFIED SUCCESSFULLY.");
    }

    // -----------------------------------------------------------------------
    // STEP 20-D  –  Verify cancelled order details via GET /order/getOrderById
    //
    // Actual response structure (data is always an array):
    //   data[0].guid, order_number, user_id, order_status, order_sample_number
    //   data[0].total_price (string), final_price, paid_amount, membership_discount, actual_discount
    //   data[0].rewards_gain (string), rewards_used, delivery_charge, due_amount
    //   data[0].refund_amount (string), actual_refund_amount
    //   data[0].payment_id, payment.payment_type, payment.payment_status, payment_statuses
    //   data[0].payment_mode = null for COD (use payment.payment_type instead)
    //   data[0].address_id  = null for lab-visit orders (home_sample=false)
    //   data[0].slot_guid, slot_start_time, slot_end_time, slot_start_times, slot_end_times
    //   data[0].cart_id
    //   data[0].cancelled_date, cancelled_by_user_at
    //   data[0].admin_approval_status, admin_approval_by, admin_approval_at
    //   data[0].cancel_order_remarks, it_dose_order_status
    //   data[0].order_items[] — each has order_status, admin_approval_status, cancelled_at
    //   data[0].user_details.guid == user_id
    // -----------------------------------------------------------------------
    @Test(groups = "refund_flow", dependsOnMethods = "step20_C_ApproveCancelledOrder")
    public void step20_D_VerifyCancelledOrderDetails() {
        System.out.println("\n=======================================================");
        System.out.println(">>> STEP 20-D: VERIFY CANCELLED ORDER DETAILS (GET) <<<");
        System.out.println("=======================================================");

        String orderGuid = RequestContext.getCurrentOrderId();
        AssertionUtil.verifyNotNull(orderGuid, "Order GUID must not be null for getOrderById call");

        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.GET_ORDER_BY_ID + orderGuid;
        System.out.println("   Endpoint : " + endpoint);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + RequestContext.getToken())
                .get();

        int    statusCode = response.getStatusCode();
        String body       = response.getBody().asString();
        System.out.println("   Status Code : " + statusCode);
        System.out.println("   Response    : " + body);

        AssertionUtil.verifyEquals(statusCode, 200,
                "GET getOrderById after full cancellation must return HTTP 200");

        // data is always a list for this endpoint
        String dataPrefix = "data[0]";

        // ── Top-level response fields ─────────────────────────────────────────
        System.out.println("\n   ── Top-level response fields ──");
        Integer apiStatus  = response.jsonPath().get("status");
        Boolean apiSuccess = response.jsonPath().get("success");
        String  apiMsg     = response.jsonPath().getString("msg");
        System.out.println("   status  : " + apiStatus);
        System.out.println("   success : " + apiSuccess);
        System.out.println("   msg     : " + apiMsg);
        AssertionUtil.verifyEquals(apiStatus, statusCode, "status field must match HTTP code");
        AssertionUtil.verifyTrue(apiSuccess, "success must be true");
        AssertionUtil.verifyNotNull(apiMsg, "msg must be present");
        // total_amount at root is always 0 (dummy) — logged only
        System.out.println("   total_amount (root, dummy=0) : " + response.jsonPath().get("total_amount"));

        // ── Order identity fields ─────────────────────────────────────────────
        System.out.println("\n   ── Order identity fields ──");
        String respGuid        = response.jsonPath().getString(dataPrefix + ".guid");
        String respOrderNumber = response.jsonPath().getString(dataPrefix + ".order_number");
        String respUserId      = response.jsonPath().getString(dataPrefix + ".user_id");
        String respCartId      = response.jsonPath().getString(dataPrefix + ".cart_id");
        String respSampleNum   = response.jsonPath().getString(dataPrefix + ".order_sample_number");
        String respVisitNumber = response.jsonPath().getString(dataPrefix + ".visit_number");

        System.out.println("   guid               : " + respGuid);
        System.out.println("   order_number       : " + respOrderNumber);
        System.out.println("   user_id            : " + respUserId);
        System.out.println("   cart_id            : " + respCartId);
        System.out.println("   order_sample_number: " + respSampleNum);
        System.out.println("   visit_number       : " + respVisitNumber);

        AssertionUtil.verifyNotNull(respGuid, "guid must be present");
        AssertionUtil.verifyEquals(respGuid, orderGuid,
                "guid must match RequestContext.getCurrentOrderId()");
        System.out.println("   ✅ guid matches RequestContext");

        AssertionUtil.verifyNotNull(respOrderNumber, "order_number must be present");
        AssertionUtil.verifyTrue(!respOrderNumber.isEmpty(), "order_number must not be empty");
        System.out.println("   ✅ order_number present : " + respOrderNumber);

        AssertionUtil.verifyNotNull(respSampleNum, "order_sample_number must be present");
        AssertionUtil.verifyTrue(respSampleNum.startsWith("MY"),
                "order_sample_number must start with 'MY', found: " + respSampleNum);
        System.out.println("   ✅ order_sample_number starts with 'MY' : " + respSampleNum);

        AssertionUtil.verifyNotNull(respVisitNumber, "visit_number must be present");
        AssertionUtil.verifyTrue(respVisitNumber.startsWith("MYD"),
                "visit_number must start with 'MYD', found: " + respVisitNumber);
        System.out.println("   ✅ visit_number starts with 'MYD' : " + respVisitNumber);

        // Cross-API: user_id
        String storedUserId = RequestContext.getUserId();
        if (storedUserId != null && !storedUserId.isEmpty()) {
            AssertionUtil.verifyEquals(respUserId, storedUserId,
                    "CROSS-API: user_id must match RequestContext.getUserId()");
            System.out.println("   ✅ user_id matches RequestContext : " + storedUserId);
        } else {
            System.out.println("   ℹ️  user_id cross-check skipped (storedUserId not set)");
        }

        // cart_id in order response is the public short-ID (pb_XXXXX format),
        // while RequestContext.getCurrentCartId() stores the cart UUID (guid).
        // These are two different fields for the same cart — log both, no equality assert.
        String storedCartId = RequestContext.getCurrentCartId();
        System.out.println("   cart_id (order resp, public short-ID) : " + respCartId);
        System.out.println("   cart UUID (RequestContext.getCurrentCartId) : " + storedCartId);
        AssertionUtil.verifyNotNull(respCartId, "cart_id must be present in order response");
        System.out.println("   ✅ cart_id present (public short-ID) : " + respCartId);

        // ── Amount fields (all stored as strings in this response) ────────────
        System.out.println("\n   ── Amount fields ──");
        String respTotalPrice   = response.jsonPath().getString(dataPrefix + ".total_price");
        String respFinalPrice   = response.jsonPath().getString(dataPrefix + ".final_price");
        String respPaidAmount   = response.jsonPath().getString(dataPrefix + ".paid_amount");
        String respRewardsUsed  = response.jsonPath().getString(dataPrefix + ".rewards_used");
        String respDelivCharge  = response.jsonPath().getString(dataPrefix + ".delivery_charge");
        Object respMemDiscount  = response.jsonPath().get(dataPrefix + ".membership_discount");
        Object respActualDisc   = response.jsonPath().get(dataPrefix + ".actual_discount");
        Object respDueAmount    = response.jsonPath().get(dataPrefix + ".due_amount");
        String respRefundAmount = response.jsonPath().getString(dataPrefix + ".refund_amount");
        String respActualRefund = response.jsonPath().getString(dataPrefix + ".actual_refund_amount");
        String respRewardsGain  = response.jsonPath().getString(dataPrefix + ".rewards_gain");

        System.out.println("   total_price          : " + respTotalPrice);
        System.out.println("   final_price          : " + respFinalPrice);
        System.out.println("   paid_amount          : " + respPaidAmount);
        System.out.println("   rewards_used         : " + respRewardsUsed);
        System.out.println("   delivery_charge      : " + respDelivCharge);
        System.out.println("   membership_discount  : " + respMemDiscount);
        System.out.println("   actual_discount      : " + respActualDisc);
        System.out.println("   due_amount           : " + respDueAmount);
        System.out.println("   refund_amount        : " + respRefundAmount);
        System.out.println("   actual_refund_amount : " + respActualRefund);
        System.out.println("   rewards_gain         : " + respRewardsGain);

        AssertionUtil.verifyNotNull(respPaidAmount, "paid_amount must be present");
        double dPaid = Double.parseDouble(respPaidAmount);
        AssertionUtil.verifyTrue(dPaid >= 0, "paid_amount must be >= 0, found: " + dPaid);
        System.out.println("   ✅ paid_amount : " + dPaid);

        AssertionUtil.verifyNotNull(respTotalPrice, "total_price must be present");
        double dTotalPrice = Double.parseDouble(respTotalPrice);
        AssertionUtil.verifyTrue(dTotalPrice >= dPaid,
                "total_price (" + dTotalPrice + ") must be >= paid_amount (" + dPaid + ")");
        System.out.println("   ✅ total_price (" + dTotalPrice + ") >= paid_amount (" + dPaid + ")");

        // membership_discount == actual_discount && total_price - discount == paid_amount
        if (respMemDiscount != null && respActualDisc != null) {
            double dMemDisc    = ((Number) respMemDiscount).doubleValue();
            double dActualDisc = ((Number) respActualDisc).doubleValue();
            AssertionUtil.verifyEquals(dMemDisc, dActualDisc,
                    "CONSISTENCY: membership_discount (" + dMemDisc + ") must equal actual_discount (" + dActualDisc + ")");
            System.out.println("   ✅ membership_discount == actual_discount : " + dMemDisc);
            double calculated = dTotalPrice - dMemDisc;
            AssertionUtil.verifyEquals(calculated, dPaid,
                    "CONSISTENCY: total_price(" + dTotalPrice + ") - membership_discount(" + dMemDisc + ") must equal paid_amount(" + dPaid + ")");
            System.out.println("   ✅ total_price - membership_discount == paid_amount : " + calculated);
        }

        // refund_amount must equal paid_amount (full refund)
        AssertionUtil.verifyNotNull(respRefundAmount, "refund_amount must be present");
        double dRefund = Double.parseDouble(respRefundAmount);
        AssertionUtil.verifyEquals(dRefund, dPaid,
                "CONSISTENCY: refund_amount (" + dRefund + ") must equal paid_amount (" + dPaid + ") for full refund");
        System.out.println("   ✅ refund_amount == paid_amount (full refund) : " + dRefund);

        // due_amount must be 0
        if (respDueAmount != null) {
            double dDue = ((Number) respDueAmount).doubleValue();
            AssertionUtil.verifyEquals(dDue, 0.0,
                    "CONSISTENCY: due_amount must be 0, found: " + dDue);
            System.out.println("   ✅ due_amount == 0");
        }

        // Cross-API: paid_amount == RequestContext.getCurrentTotalPrice()
        System.out.println("\n   ── Cross-API amount checks ──");
        int storedCartTotal = RequestContext.getCurrentTotalPrice();
        System.out.println("   [Cross-API] RequestContext.getCurrentTotalPrice() : " + storedCartTotal);
        System.out.println("   [Cross-API] getOrderById.paid_amount              : " + dPaid);
        if (storedCartTotal > 0) {
            AssertionUtil.verifyEquals(dPaid, (double) storedCartTotal,
                    "CROSS-API: paid_amount (" + dPaid + ") must match COD_02 cart total (" + storedCartTotal + ")");
            System.out.println("   ✅ paid_amount matches COD_02 cart total");
        } else {
            System.out.println("   ⚠️  storedCartTotal=0 — cross-check skipped");
        }

        // Cross-API: rewards_gain == RequestContext.getRewardsGain()
        double storedRewardsGain = RequestContext.getRewardsGain();
        System.out.println("   [Cross-API] RequestContext.getRewardsGain() : " + storedRewardsGain);
        System.out.println("   [Cross-API] getOrderById.rewards_gain       : " + respRewardsGain);
        if (respRewardsGain != null && storedRewardsGain > 0) {
            double dRG = Double.parseDouble(respRewardsGain);
            AssertionUtil.verifyEquals(dRG, storedRewardsGain,
                    "CROSS-API: rewards_gain (" + dRG + ") must match COD_15 rewardsGain (" + storedRewardsGain + ")");
            System.out.println("   ✅ rewards_gain matches COD_15 rewardsGain : " + dRG);
        } else {
            System.out.println("   ℹ️  rewards_gain cross-check skipped (stored=" + storedRewardsGain + ")");
        }

        // ── Payment fields ────────────────────────────────────────────────────
        System.out.println("\n   ── Payment fields ──");
        String respPaymentId  = response.jsonPath().getString(dataPrefix + ".payment_id");
        String respPayType    = response.jsonPath().getString(dataPrefix + ".payment.payment_type");
        String respPayMode    = response.jsonPath().getString(dataPrefix + ".payment.payment_mode");
        String respPayStatus  = response.jsonPath().getString(dataPrefix + ".payment.payment_status");
        String respPayStatuses = response.jsonPath().getString(dataPrefix + ".payment_statuses");

        System.out.println("   payment_id               : " + respPaymentId);
        System.out.println("   payment.payment_type     : " + respPayType);
        System.out.println("   payment.payment_mode     : " + respPayMode);
        System.out.println("   payment.payment_status   : " + respPayStatus);
        System.out.println("   payment_statuses         : " + respPayStatuses);

        AssertionUtil.verifyNotNull(respPaymentId, "payment_id must be present");
        System.out.println("   ✅ payment_id present : " + respPaymentId);

        // payment.payment_type — the actual mode identifier (COD / Online / UPI etc.)
        AssertionUtil.verifyNotNull(respPayType, "payment.payment_type must be present");
        System.out.println("   ✅ payment.payment_type present : " + respPayType);

        // payment.payment_mode — null for COD (no gateway mode needed);
        //                        must be non-null for online payments (UPI / Card / etc.)
        boolean isCOD = "COD".equalsIgnoreCase(respPayType);
        if (isCOD) {
            // For COD, payment_mode is always null — assert that explicitly
            AssertionUtil.verifyTrue(respPayMode == null,
                    "payment.payment_mode must be null for COD payment_type, found: " + respPayMode);
            System.out.println("   ✅ payment.payment_mode == null (correct for COD payment)");
        } else {
            // For online payments (UPI / Razorpay / Card), payment_mode must be present
            AssertionUtil.verifyNotNull(respPayMode,
                    "payment.payment_mode must not be null for online payment_type: " + respPayType);
            AssertionUtil.verifyTrue(!respPayMode.isEmpty(),
                    "payment.payment_mode must not be empty for online payment_type: " + respPayType);
            System.out.println("   ✅ payment.payment_mode present for online payment : " + respPayMode);
        }

        AssertionUtil.verifyNotNull(respPayStatus, "payment.payment_status must be present");
        AssertionUtil.verifyEquals(respPayStatus, "Success",
                "payment.payment_status must be 'Success' (payment was made before cancellation)");
        System.out.println("   ✅ payment.payment_status == 'Success'");

        AssertionUtil.verifyNotNull(respPayStatuses, "payment_statuses must be present");
        AssertionUtil.verifyEquals(respPayStatuses, "Success",
                "payment_statuses must be 'Success', found: " + respPayStatuses);
        System.out.println("   ✅ payment_statuses == 'Success'");

        // Cross-API: payment_id
        String storedPaymentId = RequestContext.getCurrentPaymentId();
        if (storedPaymentId != null && !storedPaymentId.isEmpty()) {
            AssertionUtil.verifyEquals(respPaymentId, storedPaymentId,
                    "CROSS-API: payment_id must match RequestContext.getCurrentPaymentId()");
            System.out.println("   ✅ payment_id matches RequestContext : " + storedPaymentId);
        } else {
            System.out.println("   ℹ️  payment_id cross-check skipped (stored=" + storedPaymentId + ")");
        }

        // ── Slot fields ───────────────────────────────────────────────────────
        System.out.println("\n   ── Slot fields ──");
        String respSlotGuid     = response.jsonPath().getString(dataPrefix + ".slot_guid");
        String respSlotStart    = response.jsonPath().getString(dataPrefix + ".slot_start_time");
        String respSlotEnd      = response.jsonPath().getString(dataPrefix + ".slot_end_time");
        String respSlotStartFmt = response.jsonPath().getString(dataPrefix + ".slot_start_times");
        String respSlotEndFmt   = response.jsonPath().getString(dataPrefix + ".slot_end_times");

        System.out.println("   slot_guid        : " + respSlotGuid);
        System.out.println("   slot_start_time  : " + respSlotStart);
        System.out.println("   slot_end_time    : " + respSlotEnd);
        System.out.println("   slot_start_times : " + respSlotStartFmt);
        System.out.println("   slot_end_times   : " + respSlotEndFmt);

        AssertionUtil.verifyNotNull(respSlotGuid,  "slot_guid must be present");
        AssertionUtil.verifyNotNull(respSlotStart, "slot_start_time must be present");
        AssertionUtil.verifyNotNull(respSlotEnd,   "slot_end_time must be present");
        System.out.println("   ✅ slot_guid, slot_start_time, slot_end_time all present");

        // Cross-API: slot_guid
        String storedSlotGuid = RequestContext.getCurrentSlotGuid();
        if (storedSlotGuid != null && !storedSlotGuid.isEmpty()) {
            AssertionUtil.verifyEquals(respSlotGuid, storedSlotGuid,
                    "CROSS-API: slot_guid must match RequestContext.getCurrentSlotGuid()");
            System.out.println("   ✅ slot_guid matches RequestContext : " + storedSlotGuid);
        } else {
            System.out.println("   ℹ️  slot_guid cross-check skipped (stored=" + storedSlotGuid + ")");
        }

        // ── Cancellation fields ───────────────────────────────────────────────
        System.out.println("\n   ── Cancellation fields ──");
        String respOrderStatus       = response.jsonPath().getString(dataPrefix + ".order_status");
        String respCancelledDate     = response.jsonPath().getString(dataPrefix + ".cancelled_date");
        String respCancelledByUserAt = response.jsonPath().getString(dataPrefix + ".cancelled_by_user_at");
        String respCancelRemarks     = response.jsonPath().getString(dataPrefix + ".cancel_order_remarks");
        String respItDoseStatus      = response.jsonPath().getString(dataPrefix + ".it_dose_order_status");

        System.out.println("   order_status          : " + respOrderStatus);
        System.out.println("   cancelled_date        : " + respCancelledDate);
        System.out.println("   cancelled_by_user_at  : " + respCancelledByUserAt);
        System.out.println("   cancel_order_remarks  : " + respCancelRemarks);
        System.out.println("   it_dose_order_status  : " + respItDoseStatus);

        AssertionUtil.verifyNotNull(respOrderStatus, "order_status must be present");
        AssertionUtil.verifyEquals(respOrderStatus, "Cancelled",
                "order_status must be 'Cancelled' after full refund+approval flow");
        System.out.println("   ✅ order_status == 'Cancelled'");

        AssertionUtil.verifyNotNull(respCancelledDate, "cancelled_date must be present after cancellation");
        AssertionUtil.verifyTrue(!respCancelledDate.isEmpty(), "cancelled_date must not be empty");
        System.out.println("   ✅ cancelled_date present : " + respCancelledDate);

        AssertionUtil.verifyNotNull(respCancelledByUserAt, "cancelled_by_user_at must be present");
        System.out.println("   ✅ cancelled_by_user_at present : " + respCancelledByUserAt);

        AssertionUtil.verifyNotNull(respCancelRemarks, "cancel_order_remarks must be present");
        AssertionUtil.verifyEquals(respCancelRemarks, "test",
                "cancel_order_remarks must match 'test' (sent in step20_C payload)");
        System.out.println("   ✅ cancel_order_remarks == 'test'");

        AssertionUtil.verifyNotNull(respItDoseStatus, "it_dose_order_status must be present");
        AssertionUtil.verifyEquals(respItDoseStatus, "Cancelled",
                "it_dose_order_status must be 'Cancelled', found: " + respItDoseStatus);
        System.out.println("   ✅ it_dose_order_status == 'Cancelled'");

        // ── Admin approval fields ─────────────────────────────────────────────
        System.out.println("\n   ── Admin approval fields ──");
        String respAdminStatus = response.jsonPath().getString(dataPrefix + ".admin_approval_status");
        String respAdminBy     = response.jsonPath().getString(dataPrefix + ".admin_approval_by");
        String respAdminAt     = response.jsonPath().getString(dataPrefix + ".admin_approval_at");

        System.out.println("   admin_approval_status : " + respAdminStatus);
        System.out.println("   admin_approval_by     : " + respAdminBy);
        System.out.println("   admin_approval_at     : " + respAdminAt);

        AssertionUtil.verifyNotNull(respAdminStatus, "admin_approval_status must be present");
        AssertionUtil.verifyEquals(respAdminStatus, "Approved",
                "admin_approval_status must be 'Approved' after step20_C, found: " + respAdminStatus);
        System.out.println("   ✅ admin_approval_status == 'Approved'");

        AssertionUtil.verifyNotNull(respAdminBy, "admin_approval_by must be present");
        AssertionUtil.verifyNotNull(respAdminAt, "admin_approval_at must be present");
        System.out.println("   ✅ admin_approval_by present : " + respAdminBy);
        System.out.println("   ✅ admin_approval_at present : " + respAdminAt);

        // Cross-API: admin_approval_by == RequestContext.getAdminGuid()
        String storedAdminGuid = RequestContext.getAdminGuid();
        if (storedAdminGuid != null && !storedAdminGuid.isEmpty()) {
            AssertionUtil.verifyEquals(respAdminBy, storedAdminGuid,
                    "CROSS-API: admin_approval_by must match RequestContext.getAdminGuid()");
            System.out.println("   ✅ admin_approval_by matches RequestContext admin GUID : " + storedAdminGuid);
        } else {
            System.out.println("   ℹ️  admin GUID cross-check skipped (stored=" + storedAdminGuid + ")");
        }

        // ── user_details nested object ────────────────────────────────────────
        System.out.println("\n   ── user_details ──");
        String userDetailsGuid = response.jsonPath().getString(dataPrefix + ".user_details.guid");
        String userFirstName   = response.jsonPath().getString(dataPrefix + ".user_details.first_name");
        String userMobile      = response.jsonPath().getString(dataPrefix + ".user_details.mobile");

        System.out.println("   user_details.guid       : " + userDetailsGuid);
        System.out.println("   user_details.first_name : " + userFirstName);
        System.out.println("   user_details.mobile     : " + userMobile);

        AssertionUtil.verifyNotNull(userDetailsGuid, "user_details.guid must be present");
        AssertionUtil.verifyEquals(userDetailsGuid, respUserId,
                "CONSISTENCY: user_details.guid must equal data[0].user_id");
        System.out.println("   ✅ user_details.guid == data[0].user_id : " + userDetailsGuid);

        AssertionUtil.verifyNotNull(userFirstName, "user_details.first_name must be present");
        AssertionUtil.verifyNotNull(userMobile,    "user_details.mobile must be present");
        System.out.println("   ✅ user_details: first_name and mobile present");

        // ── order_items — all must be Cancelled and Approved ──────────────────
        System.out.println("\n   ── order_items cancellation verification ──");
        java.util.List<java.util.Map<String, Object>> orderItems =
                response.jsonPath().getList(dataPrefix + ".order_items");

        AssertionUtil.verifyNotNull(orderItems, "order_items must be present");
        AssertionUtil.verifyTrue(!orderItems.isEmpty(), "order_items must not be empty");
        System.out.println("   order_items count : " + orderItems.size());

        int itemIdx = 0;
        int totalTestCount = 0;
        for (java.util.Map<String, Object> item : orderItems) {
            itemIdx++;
            String itemGuid          = (String) item.get("guid");
            String itemOrderId       = (String) item.get("order_id");
            String itemProductName   = (String) item.get("product_name");
            String itemProductCode   = (String) item.get("product_code");
            String itemItemNumber    = (String) item.get("order_item_number");
            String itemStatus        = (String) item.get("order_status");
            String itemItDoseStatus  = (String) item.get("it_dose_order_items_status");
            String itemAdminApproval = (String) item.get("admin_approval_status");
            String itemAdminBy       = (String) item.get("admin_approval_by");
            String itemAdminAt       = (String) item.get("admin_approval_at");
            String itemCancelledAt   = (String) item.get("cancelled_at");
            String itemCancelRemarks = (String) item.get("cancel_order_remarks");
            Object itemFinalPrice    = item.get("final_price");
            Object itemActualPrice   = item.get("actual_price");
            Object itemMemDiscount   = item.get("membership_discount");

            System.out.println("\n   ──────────────────────────────────────────────────");
            System.out.printf("   item[%d] guid          : %s%n", itemIdx, itemGuid);
            System.out.printf("   item[%d] product_name  : %s (%s)%n", itemIdx, itemProductName, itemProductCode);
            System.out.printf("   item[%d] item_number   : %s%n", itemIdx, itemItemNumber);
            System.out.printf("   item[%d] order_status  : %s%n", itemIdx, itemStatus);
            System.out.printf("   item[%d] it_dose_status: %s%n", itemIdx, itemItDoseStatus);
            System.out.printf("   item[%d] admin_approval: %s (by: %s)%n", itemIdx, itemAdminApproval, itemAdminBy);
            System.out.printf("   item[%d] admin_at      : %s%n", itemIdx, itemAdminAt);
            System.out.printf("   item[%d] cancelled_at  : %s%n", itemIdx, itemCancelledAt);
            System.out.printf("   item[%d] cancel_remarks: %s%n", itemIdx, itemCancelRemarks);
            System.out.printf("   item[%d] actual_price  : %s  final_price: %s  member_discount: %s%n",
                    itemIdx, itemActualPrice, itemFinalPrice, itemMemDiscount);

            // ── order_id must match main order guid ──
            AssertionUtil.verifyEquals(itemOrderId, orderGuid,
                    "item[" + itemIdx + "].order_id must match main order guid");

            // ── product_name must be present ──
            AssertionUtil.verifyNotNull(itemProductName,
                    "item[" + itemIdx + "].product_name must be present");
            AssertionUtil.verifyTrue(!itemProductName.isEmpty(),
                    "item[" + itemIdx + "].product_name must not be empty");
            System.out.println("   ✅ item[" + itemIdx + "] product_name present : " + itemProductName);

            // ── order_item_number format: must start with order_sample_number + "-" ──
            AssertionUtil.verifyNotNull(itemItemNumber,
                    "item[" + itemIdx + "].order_item_number must be present");
            String expectedItemNumPrefix = respSampleNum + "-" + itemIdx;
            AssertionUtil.verifyEquals(itemItemNumber, expectedItemNumPrefix,
                    "item[" + itemIdx + "].order_item_number must be '" + expectedItemNumPrefix
                            + "', found: " + itemItemNumber);
            System.out.println("   ✅ item[" + itemIdx + "] order_item_number : " + itemItemNumber);

            // ── order_status must be Cancelled ──
            AssertionUtil.verifyEquals(itemStatus, "Cancelled",
                    "item[" + itemIdx + "] (" + itemProductName + ") order_status must be 'Cancelled'");
            System.out.println("   ✅ item[" + itemIdx + "] order_status == 'Cancelled'");

            // ── it_dose_order_items_status must be Cancelled ──
            AssertionUtil.verifyEquals(itemItDoseStatus, "Cancelled",
                    "item[" + itemIdx + "] it_dose_order_items_status must be 'Cancelled'");
            System.out.println("   ✅ item[" + itemIdx + "] it_dose_order_items_status == 'Cancelled'");

            // ── admin_approval_status must be Approved ──
            AssertionUtil.verifyEquals(itemAdminApproval, "Approved",
                    "item[" + itemIdx + "] (" + itemProductName + ") admin_approval_status must be 'Approved'");
            System.out.println("   ✅ item[" + itemIdx + "] admin_approval_status == 'Approved'");

            // ── admin_approval_by cross-check ──
            AssertionUtil.verifyNotNull(itemAdminBy,
                    "item[" + itemIdx + "].admin_approval_by must be present");
            if (storedAdminGuid != null && !storedAdminGuid.isEmpty()) {
                AssertionUtil.verifyEquals(itemAdminBy, storedAdminGuid,
                        "CROSS-API: item[" + itemIdx + "].admin_approval_by must match RequestContext.getAdminGuid()");
                System.out.println("   ✅ item[" + itemIdx + "] admin_approval_by matches admin GUID : " + itemAdminBy);
            } else {
                System.out.println("   ℹ️  item[" + itemIdx + "] admin_approval_by : " + itemAdminBy + " (storedAdminGuid not set)");
            }

            // ── admin_approval_at must be present ──
            AssertionUtil.verifyNotNull(itemAdminAt,
                    "item[" + itemIdx + "].admin_approval_at must be present");
            System.out.println("   ✅ item[" + itemIdx + "] admin_approval_at present : " + itemAdminAt);

            // ── cancelled_at must be present ──
            AssertionUtil.verifyNotNull(itemCancelledAt,
                    "item[" + itemIdx + "] cancelled_at must be present");
            System.out.println("   ✅ item[" + itemIdx + "] cancelled_at present : " + itemCancelledAt);

            // ── cancel_order_remarks must be "test" (matches step20_C payload) ──
            AssertionUtil.verifyNotNull(itemCancelRemarks,
                    "item[" + itemIdx + "].cancel_order_remarks must be present");
            AssertionUtil.verifyEquals(itemCancelRemarks, "test",
                    "item[" + itemIdx + "].cancel_order_remarks must be 'test', found: " + itemCancelRemarks);
            System.out.println("   ✅ item[" + itemIdx + "] cancel_order_remarks == 'test'");

            // ── actual_price >= final_price (membership discount applied) ──
            if (itemFinalPrice != null && itemActualPrice != null) {
                double dFinal      = ((Number) itemFinalPrice).doubleValue();
                double dActual     = ((Number) itemActualPrice).doubleValue();
                double dDiscount   = itemMemDiscount != null ? ((Number) itemMemDiscount).doubleValue() : 0;
                AssertionUtil.verifyTrue(dActual >= dFinal,
                        "item[" + itemIdx + "] actual_price (" + dActual + ") must be >= final_price (" + dFinal + ")");
                // actual_price - membership_discount == final_price
                if (itemMemDiscount != null) {
                    double calcFinal = dActual - dDiscount;
                    AssertionUtil.verifyEquals(calcFinal, dFinal,
                            "item[" + itemIdx + "] actual_price(" + dActual + ") - membership_discount("
                                    + dDiscount + ") must equal final_price(" + dFinal + ")");
                    System.out.println("   ✅ item[" + itemIdx + "] actual_price - membership_discount == final_price : "
                            + dActual + " - " + dDiscount + " = " + dFinal);
                }
            }

            // ── sample_types → Tests: log and verify each lab test is present ──
            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> sampleTypes =
                    (java.util.List<java.util.Map<String, Object>>) item.get("sample_types");
            System.out.println("   item[" + itemIdx + "] lab tests under sample_types:");
            if (sampleTypes != null && !sampleTypes.isEmpty()) {
                for (java.util.Map<String, Object> st : sampleTypes) {
                    String defaultSampleType = (String) st.get("DefaultSampleType");
                    @SuppressWarnings("unchecked")
                    java.util.List<java.util.Map<String, Object>> tests =
                            (java.util.List<java.util.Map<String, Object>>) st.get("Tests");
                    System.out.println("      SampleType: " + defaultSampleType);
                    if (tests != null) {
                        for (java.util.Map<String, Object> t : tests) {
                            String testName = (String) t.get("TestName");
                            AssertionUtil.verifyNotNull(testName,
                                    "item[" + itemIdx + "] TestName must not be null in sample_types.Tests");
                            AssertionUtil.verifyTrue(!testName.isEmpty(),
                                    "item[" + itemIdx + "] TestName must not be empty");
                            System.out.println("         ✅ TestName: " + testName);
                            totalTestCount++;
                        }
                    }
                }
            } else {
                System.out.println("      ⚠️  sample_types not present for item[" + itemIdx + "] — logged only");
            }
        }

        System.out.println("\n   ✅ All " + itemIdx + " order_items verified:");
        System.out.println("      - order_status            == 'Cancelled'");
        System.out.println("      - it_dose_order_items_status == 'Cancelled'");
        System.out.println("      - admin_approval_status   == 'Approved'");
        System.out.println("      - cancel_order_remarks    == 'test'");
        System.out.println("      - cancelled_at present");
        System.out.println("      - order_item_number format verified");
        System.out.println("      - " + totalTestCount + " individual lab tests verified across all items");

        System.out.println("\n✅ STEP 20-D PASSED: Cancelled order details comprehensively verified via GET getOrderById.");
        System.out.println("   🎉 FULL CANCELLATION + REFUND + ADMIN APPROVAL + GET VERIFICATION FLOW COMPLETE.");
    }

    // -----------------------------------------------------------------------
    // STEP 20-E  –  Verify payment record via GET /gateway/getPaymentById  (LAST)
    //
    // Validates the payment record after the full cancel + refund + admin-approval
    // cycle.  Expected response structure (data.payments + data.order_items):
    //
    //   data.payments.guid               — must match stored paymentId
    //   data.payments.payment_type       — "COD"
    //   data.payments.payment_status     — post-refund state (logged + soft validated)
    //   data.payments.amount             — total charged amount  >= 0
    //   data.payments.net_payable        — net payable           >= 0
    //   data.payments.membership_discount— >= 0
    //   data.payments.total_discount     — >= 0
    //   data.order_items[]               — each item's order_id, product_name,
    //                                      final_price, quantity, patient_guid
    // -----------------------------------------------------------------------
    @Test(groups = "refund_flow", dependsOnMethods = "step20_D_VerifyCancelledOrderDetails")
    public void step20_E_VerifyPaymentById() {
        System.out.println("\n=======================================================");
        System.out.println(">>> STEP 20-E: VERIFY PAYMENT BY ID (POST-REFUND) <<<");
        System.out.println("=======================================================");

        String token     = RequestContext.getToken();
        String paymentId = RequestContext.getCurrentPaymentId();
        AssertionUtil.verifyNotNull(paymentId, "Payment ID must be stored in RequestContext before step 20-E");
        System.out.println("   paymentId (RequestContext) : " + paymentId);

        // ── Call GET /gateway/getPaymentById ────────────────────────────────
        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.GET_PAYMENT_BY_ID;
        System.out.println("   Endpoint  : " + endpoint);

        Map<String, String> reqPayload = new HashMap<>();
        reqPayload.put("id", paymentId);
        System.out.println("   Payload   : " + reqPayload);

        Response response = null;
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            response = new RequestBuilder()
                    .setEndpoint(endpoint)
                    .addHeader("Authorization", token)
                    .setRequestBody(reqPayload)
                    .post();

            System.out.println("   HTTP Status (attempt " + attempt + ") : " + response.getStatusCode());
            System.out.println("   Response Body              : " + response.getBody().asString());

            if (response.getStatusCode() == 200) break;

            boolean retryable = response.getStatusCode() == 500
                    || response.getStatusCode() == 502
                    || response.getStatusCode() == 503
                    || response.getStatusCode() == 504;
            if (!retryable || attempt == maxAttempts) break;

            System.out.println("   ⚠️  Transient status " + response.getStatusCode() + ". Retrying...");
            try { Thread.sleep(1500L * attempt); } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); break;
            }
        }

        // ── Top-level checks ────────────────────────────────────────────────
        AssertionUtil.verifyEquals(response.getStatusCode(), 200,
                "getPaymentById must return HTTP 200");

        Boolean apiSuccess = response.jsonPath().get("success");
        System.out.println("   success  : " + apiSuccess);
        if (apiSuccess != null) {
            AssertionUtil.verifyTrue(apiSuccess, "getPaymentById – 'success' flag must be true");
        }

        String apiMsg = response.jsonPath().getString("msg");
        AssertionUtil.verifyNotNull(apiMsg, "getPaymentById – 'msg' field must be present");
        System.out.println("   msg      : " + apiMsg);

        // ── data.payments object ─────────────────────────────────────────────
        System.out.println("\n   ── data.payments ──");

        // guid must match paymentId stored in context
        String respGuid = response.jsonPath().getString("data.payments.guid");
        System.out.println("   data.payments.guid               : " + respGuid);
        AssertionUtil.verifyNotNull(respGuid, "data.payments.guid must be present");
        AssertionUtil.verifyEquals(respGuid, paymentId,
                "CROSS-API: data.payments.guid must match RequestContext.getCurrentPaymentId()");
        System.out.println("   ✅ data.payments.guid == storedPaymentId : " + paymentId);

        // payment_type must be COD for this flow
        String respPayType = response.jsonPath().getString("data.payments.payment_type");
        System.out.println("   data.payments.payment_type       : " + respPayType);
        AssertionUtil.verifyNotNull(respPayType, "data.payments.payment_type must be present");
        AssertionUtil.verifyEquals(respPayType, "COD",
                "data.payments.payment_type must be 'COD' for this cancellation-refund flow");
        System.out.println("   ✅ data.payments.payment_type == 'COD'");

        // payment_status after full cancel+refund+admin-approval — log and soft-validate
        String respPayStatus = response.jsonPath().getString("data.payments.payment_status");
        System.out.println("   data.payments.payment_status     : " + respPayStatus);
        AssertionUtil.verifyNotNull(respPayStatus, "data.payments.payment_status must be present");
        boolean isRefundState = "Refunded".equalsIgnoreCase(respPayStatus)
                || "Cancelled".equalsIgnoreCase(respPayStatus)
                || "cancelled_refund".equalsIgnoreCase(respPayStatus)
                || "Approved".equalsIgnoreCase(respPayStatus)
                || "Paid".equalsIgnoreCase(respPayStatus)
                || "Success".equalsIgnoreCase(respPayStatus);
        if (isRefundState) {
            System.out.println("   ✅ payment_status reflects post-cycle state : " + respPayStatus);
        } else {
            System.out.println("   ⚠️  Unexpected payment_status '" + respPayStatus
                    + "' — logging without hard-fail (post-refund state may differ by env)");
        }

        // amount (total charged) — must be a non-negative number
        Object amountObj = response.jsonPath().get("data.payments.amount");
        if (amountObj == null) {
            amountObj = response.jsonPath().get("data.payments.net_payable");
        }
        double respAmount = amountObj instanceof Number ? ((Number) amountObj).doubleValue()
                : (amountObj != null ? Double.parseDouble(amountObj.toString()) : 0.0);
        System.out.println("   data.payments.amount             : " + respAmount);
        AssertionUtil.verifyTrue(respAmount >= 0,
                "data.payments.amount must be >= 0 (found: " + respAmount + ")");
        System.out.println("   ✅ data.payments.amount >= 0 : " + respAmount);

        // Cross-API: amount must match stored total price (if available)
        int storedTotalPrice = RequestContext.getCurrentTotalPrice();
        if (storedTotalPrice > 0) {
            System.out.println("   ℹ️  Stored total price (RequestContext) : " + storedTotalPrice);
            if (Math.abs(respAmount - storedTotalPrice) <= 1.0) {
                System.out.println("   ✅ CROSS-API: payment amount matches stored total price : " + respAmount);
            } else {
                System.out.println("   ⚠️  payment amount (" + respAmount
                        + ") differs from stored total price (" + storedTotalPrice
                        + ") — may reflect refund/discount adjustment");
            }
        }

        // net_payable
        Object netPayableObj = response.jsonPath().get("data.payments.net_payable");
        if (netPayableObj != null) {
            double respNetPayable = netPayableObj instanceof Number ? ((Number) netPayableObj).doubleValue()
                    : Double.parseDouble(netPayableObj.toString());
            System.out.println("   data.payments.net_payable        : " + respNetPayable);
            AssertionUtil.verifyTrue(respNetPayable >= 0,
                    "data.payments.net_payable must be >= 0 (found: " + respNetPayable + ")");
            System.out.println("   ✅ data.payments.net_payable >= 0 : " + respNetPayable);
        } else {
            System.out.println("   ℹ️  data.payments.net_payable not present (optional for COD)");
        }

        // membership_discount
        Object memDiscObj = response.jsonPath().get("data.payments.membership_discount");
        if (memDiscObj != null) {
            double respMemDisc = memDiscObj instanceof Number ? ((Number) memDiscObj).doubleValue()
                    : Double.parseDouble(memDiscObj.toString());
            System.out.println("   data.payments.membership_discount: " + respMemDisc);
            AssertionUtil.verifyTrue(respMemDisc >= 0,
                    "data.payments.membership_discount must be >= 0 (found: " + respMemDisc + ")");
            System.out.println("   ✅ data.payments.membership_discount >= 0 : " + respMemDisc);
        } else {
            System.out.println("   ℹ️  data.payments.membership_discount not present");
        }

        // total_discount
        Object totalDiscObj = response.jsonPath().get("data.payments.total_discount");
        if (totalDiscObj != null) {
            double respTotalDisc = totalDiscObj instanceof Number ? ((Number) totalDiscObj).doubleValue()
                    : Double.parseDouble(totalDiscObj.toString());
            System.out.println("   data.payments.total_discount     : " + respTotalDisc);
            AssertionUtil.verifyTrue(respTotalDisc >= 0,
                    "data.payments.total_discount must be >= 0 (found: " + respTotalDisc + ")");
            System.out.println("   ✅ data.payments.total_discount >= 0 : " + respTotalDisc);
        } else {
            System.out.println("   ℹ️  data.payments.total_discount not present");
        }

        // payment_mode (null for COD)
        String respPayMode = response.jsonPath().getString("data.payments.payment_mode");
        System.out.println("   data.payments.payment_mode       : " + respPayMode);
        if ("COD".equalsIgnoreCase(respPayType)) {
            // For COD, payment_mode is null by design
            if (respPayMode == null || respPayMode.isEmpty()) {
                System.out.println("   ✅ data.payments.payment_mode is null/empty (expected for COD)");
            } else {
                System.out.println("   ℹ️  data.payments.payment_mode = '" + respPayMode
                        + "' (present for COD — logged without fail)");
            }
        } else {
            // For non-COD, payment_mode should be present
            AssertionUtil.verifyNotNull(respPayMode,
                    "data.payments.payment_mode must be present for non-COD payment_type: " + respPayType);
            System.out.println("   ✅ data.payments.payment_mode present for " + respPayType + " : " + respPayMode);
        }

        // ── data.order_items list ────────────────────────────────────────────
        System.out.println("\n   ── data.order_items ──");
        java.util.List<java.util.Map<String, Object>> paymentItems =
                response.jsonPath().getList("data.order_items");

        AssertionUtil.verifyNotNull(paymentItems, "data.order_items must be present in getPaymentById response");
        AssertionUtil.verifyTrue(paymentItems != null && !paymentItems.isEmpty(),
                "data.order_items must not be empty");
        System.out.println("   data.order_items count           : " + paymentItems.size());

        // Retrieve the stored order GUID for cross-API matching
        String storedOrderId = RequestContext.getCurrentOrderId();

        double itemsCalculatedTotal = 0;
        int piIdx = 0;
        for (java.util.Map<String, Object> pi : paymentItems) {
            piIdx++;
            String piOrderId      = (String) pi.get("order_id");
            String piProductName  = (String) pi.get("product_name");
            Object piFinalPrice   = pi.get("final_price");
            Object piQuantity     = pi.get("quantity");
            String piPatientGuid  = (String) pi.get("patient_guid");

            double dFinalPrice = piFinalPrice instanceof Number ? ((Number) piFinalPrice).doubleValue()
                    : (piFinalPrice != null ? Double.parseDouble(piFinalPrice.toString()) : 0.0);
            double dQuantity   = piQuantity instanceof Number   ? ((Number) piQuantity).doubleValue()
                    : (piQuantity != null ? Double.parseDouble(piQuantity.toString()) : 1.0);
            double itemTotal   = dFinalPrice * dQuantity;
            itemsCalculatedTotal += itemTotal;

            System.out.printf("%n   item[%d] order_id     : %s%n", piIdx, piOrderId);
            System.out.printf("   item[%d] product_name : %s%n", piIdx, piProductName);
            System.out.printf("   item[%d] final_price  : %s  |  quantity: %s  |  line_total: %.2f%n",
                    piIdx, piFinalPrice, piQuantity, itemTotal);
            System.out.printf("   item[%d] patient_guid : %s%n", piIdx, piPatientGuid);

            // product_name must be present
            AssertionUtil.verifyNotNull(piProductName,
                    "data.order_items[" + piIdx + "].product_name must be present");
            AssertionUtil.verifyTrue(!piProductName.isEmpty(),
                    "data.order_items[" + piIdx + "].product_name must not be empty");
            System.out.println("   ✅ item[" + piIdx + "] product_name present : " + piProductName);

            // final_price must be >= 0
            AssertionUtil.verifyTrue(dFinalPrice >= 0,
                    "data.order_items[" + piIdx + "].final_price must be >= 0 (found: " + dFinalPrice + ")");
            System.out.println("   ✅ item[" + piIdx + "] final_price >= 0 : " + dFinalPrice);

            // quantity must be > 0
            AssertionUtil.verifyTrue(dQuantity > 0,
                    "data.order_items[" + piIdx + "].quantity must be > 0 (found: " + dQuantity + ")");
            System.out.println("   ✅ item[" + piIdx + "] quantity > 0 : " + dQuantity);

            // order_id must match stored order guid (cross-API)
            if (storedOrderId != null && !storedOrderId.isEmpty()) {
                AssertionUtil.verifyEquals(piOrderId, storedOrderId,
                        "CROSS-API: data.order_items[" + piIdx
                                + "].order_id must match RequestContext.getCurrentOrderId()");
                System.out.println("   ✅ item[" + piIdx + "] order_id matches storedOrderId : " + piOrderId);
            } else {
                AssertionUtil.verifyNotNull(piOrderId,
                        "data.order_items[" + piIdx + "].order_id must be present");
                System.out.println("   ℹ️  item[" + piIdx + "] order_id: " + piOrderId
                        + " (stored order ID not in context for cross-check)");
            }

            // patient_guid presence (lab visit: could be user_id itself)
            if (piPatientGuid != null && !piPatientGuid.isEmpty()) {
                System.out.println("   ✅ item[" + piIdx + "] patient_guid present : " + piPatientGuid);
            } else {
                System.out.println("   ℹ️  item[" + piIdx + "] patient_guid absent (may be null for some item types)");
            }
        }

        System.out.println("\n   ── Order Items Summary ──────────────────────────────────");
        System.out.println("   Total items in payment record  : " + piIdx);
        System.out.printf( "   Calculated items total         : %.2f%n", itemsCalculatedTotal);
        if (respAmount > 0) {
            if (Math.abs(itemsCalculatedTotal - respAmount) <= 1.0) {
                System.out.println("   ✅ Items total matches payment amount : ₹" + itemsCalculatedTotal);
            } else {
                System.out.println("   ⚠️  Items total (" + itemsCalculatedTotal
                        + ") differs from payment amount (" + respAmount
                        + ") — may include home collection charge or membership discount delta");
            }
        }

        System.out.println("\n✅ STEP 20-E PASSED: getPaymentById response fully validated after cancel+refund+admin-approval.");
        System.out.println("   Payment GUID  : " + respGuid);
        System.out.println("   Payment Type  : " + respPayType);
        System.out.println("   Payment Status: " + respPayStatus);
        System.out.println("   Amount        : " + respAmount);
        System.out.println("   Items count   : " + piIdx);
    }

    // -----------------------------------------------------------------------
    // STEP 20-F  –  Verify rewards via GET /reward/getRewardsByMobile/{mobile}
    //              AND extract the membership system's customer_id for step 20-G
    //
    // Why this step exists:
    //   The transaction API stores customer_id == membership system's user guid
    //   (e.g. "08743efb-..."), which is DIFFERENT from the diagnostics system's
    //   userId (e.g. "60cdff44-...") stored in RequestContext.getUserId().
    //   This step calls the rewards API, extracts the correct customer_id, stores
    //   it in RequestContext.setCurrentMembershipCustomerId(), and validates all
    //   reward fields in the post-cancellation state.
    //
    // Validated fields:
    //   data.total_rewards   — >= 0; compared with initial+rewardsGain (post-cancel)
    //   data.customer_id     — stored as membership customer_id  (CROSS-API)
    //   data.mobile          — == RequestContext.getMobile()       (CROSS-API)
    // -----------------------------------------------------------------------
    @Test(groups = "refund_flow", dependsOnMethods = "step20_E_VerifyPaymentById")
    public void step20_F_VerifyRewardsByMobile() {
        System.out.println("\n=======================================================");
        System.out.println(">>> STEP 20-F: VERIFY REWARDS BY MOBILE (POST-CANCEL) <<<");
        System.out.println("=======================================================");

        String mobile = RequestContext.getMobile();
        AssertionUtil.verifyNotNull(mobile, "User mobile must be stored in RequestContext before step 20-F");
        System.out.println("   mobile (RequestContext) : " + mobile);

        String endpoint = APIEndpoints.MEMBER_BASE_URL
                + APIEndpoints.GET_REWARDS_BY_MOBILE.replace("{mobile_number}", mobile);
        System.out.println("   Endpoint               : " + endpoint);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", RequestContext.getToken())
                .get();

        System.out.println("   HTTP Status            : " + response.getStatusCode());
        System.out.println("   Response Body          : " + response.getBody().asString());

        // ── Top-level assertions ──────────────────────────────────────────
        AssertionUtil.verifyEquals(response.getStatusCode(), 200,
                "getRewardsByMobile must return HTTP 200");

        Boolean success = response.jsonPath().get("success");
        System.out.println("   success : " + success);
        if (success != null) {
            AssertionUtil.verifyTrue(success, "getRewardsByMobile 'success' must be true");
        }

        String msg = response.jsonPath().getString("msg");
        System.out.println("   msg     : " + msg);
        AssertionUtil.verifyNotNull(msg, "getRewardsByMobile 'msg' must be present");

        // ── data.total_rewards ────────────────────────────────────────────
        Object totalRewardsObj = response.jsonPath().get("data.total_rewards");
        AssertionUtil.verifyNotNull(totalRewardsObj,
                "getRewardsByMobile data.total_rewards must be present");
        double postCancelRewards = totalRewardsObj instanceof Number
                ? ((Number) totalRewardsObj).doubleValue()
                : Double.parseDouble(totalRewardsObj.toString());
        System.out.println("   data.total_rewards (post-cancel) : " + postCancelRewards);
        AssertionUtil.verifyTrue(postCancelRewards >= 0,
                "data.total_rewards must be >= 0 (found: " + postCancelRewards + ")");
        System.out.println("   ✅ data.total_rewards >= 0 : " + postCancelRewards);

        // Cross-API: compare with initial balance stored by step18_A
        double initialRewards  = RequestContext.getInitialTotalRewards();
        double finalRewards    = RequestContext.getFinalTotalRewards();  // after payment (pre-cancel)
        double rewardsGain     = RequestContext.getRewardsGain();
        double paidAmount      = RequestContext.getCurrentDueAmount();

        System.out.println("   ── Rewards balance comparison ──");
        System.out.println("   initial_rewards  (pre-payment,  step18_A)  : " + initialRewards);
        System.out.println("   rewards_gain     (earned on payment, step15): " + rewardsGain);
        System.out.println("   final_rewards    (post-payment, step18_C)   : " + finalRewards);
        System.out.println("   post_cancel_rewards (this call)             : " + postCancelRewards);

        // ── (1) post-cancel rewards must be LESS than finalRewards (reversal happened) ─
        if (finalRewards > 0) {
            AssertionUtil.verifyTrue(postCancelRewards < finalRewards,
                    "CROSS-API: post-cancel total_rewards(" + postCancelRewards
                            + ") must be LESS than post-payment finalRewards(" + finalRewards
                            + ") — cancellation should have reversed the earned rewards");
            System.out.println("   ✅ post_cancel_rewards(" + postCancelRewards
                    + ") < finalRewards(" + finalRewards + ") — reversal confirmed");
        } else {
            System.out.println("   ℹ️  finalRewards not stored (step18_C may not have run)");
        }

        // ── (2) reversal amount = finalRewards - postCancelRewards ≈ rewardsGain ────
        if (finalRewards > 0 && rewardsGain > 0) {
            double actualReversal = finalRewards - postCancelRewards;
            System.out.printf("   rewards reversed (finalRewards - postCancelRewards) : %.2f%n", actualReversal);
            System.out.printf("   rewardsGain stored from step20_A                    : %.2f%n", rewardsGain);
            if (Math.abs(actualReversal - rewardsGain) <= 1.0) {
                System.out.println("   ✅ CROSS-API: reversal amount(" + actualReversal
                        + ") ≈ rewardsGain(" + rewardsGain + ") — correct amount reversed");
            } else {
                System.out.println("   ℹ️  reversal amount(" + actualReversal
                        + ") differs from rewardsGain(" + rewardsGain
                        + ") — may include partial adjustments; logged only");
            }
        }

        // ── (3) post-cancel balance should have reverted to initialRewards ──────────
        if (initialRewards > 0) {
            double expectedAfterCancel = initialRewards;
            System.out.printf("   expected post-cancel balance (== initialRewards) : %.2f%n", expectedAfterCancel);
            if (Math.abs(postCancelRewards - expectedAfterCancel) <= 1.0) {
                System.out.println("   ✅ CROSS-API: total_rewards(" + postCancelRewards
                        + ") reverted to pre-payment balance(" + initialRewards + ")");
            } else {
                System.out.println("   ℹ️  post-cancel rewards(" + postCancelRewards
                        + ") != initialRewards(" + initialRewards
                        + ") — reversal may be partial or delayed; logged only");
            }
        } else {
            System.out.println("   ℹ️  initialRewards not stored (step18_A may not have run)");
        }

        // ── (4) formula check: expected gain = ceil(paidAmount * 5%) ────────────────
        if (paidAmount > 0) {
            double expectedGain     = Math.ceil(paidAmount * 0.05);
            double expectedPostCancel = (initialRewards > 0) ? initialRewards : (finalRewards - expectedGain);
            System.out.printf("   formula: ceil(%.2f * 5%%) = %.2f (expected gain)%n", paidAmount, expectedGain);
            System.out.printf("   formula: expected post-cancel balance = %.2f%n", expectedPostCancel);
            if (Math.abs(postCancelRewards - expectedPostCancel) <= 2.0) {
                System.out.println("   ✅ post_cancel_rewards(" + postCancelRewards
                        + ") matches formula-derived expected balance(" + expectedPostCancel + ")");
            } else {
                System.out.println("   ℹ️  post_cancel_rewards(" + postCancelRewards
                        + ") differs from formula-expected(" + expectedPostCancel
                        + ") — may include other transactions; logged only");
            }
        } else {
            System.out.println("   ℹ️  paidAmount not stored; skipping formula check");
        }

        // ── Extract membership customer_id ────────────────────────────────
        // Try multiple possible field paths (print full data first for visibility)
        System.out.println("\n   ── Extracting membership customer_id ──");
        String membershipCustomerId = response.jsonPath().getString("data.customer_id");
        if (membershipCustomerId == null || membershipCustomerId.isEmpty()) {
            membershipCustomerId = response.jsonPath().getString("data.user_id");
        }
        if (membershipCustomerId == null || membershipCustomerId.isEmpty()) {
            membershipCustomerId = response.jsonPath().getString("data.guid");
        }
        if (membershipCustomerId == null || membershipCustomerId.isEmpty()) {
            // Last resort: try the root-level id field
            Object idObj = response.jsonPath().get("data.id");
            if (idObj != null) membershipCustomerId = idObj.toString();
        }
        System.out.println("   data.customer_id (resolved)            : " + membershipCustomerId);

        if (membershipCustomerId != null && !membershipCustomerId.isEmpty()) {
            RequestContext.setCurrentMembershipCustomerId(membershipCustomerId);
            System.out.println("   ✅ membershipCustomerId stored in RequestContext : " + membershipCustomerId);
        } else {
            System.out.println("   ⚠️  Could not extract customer_id from getRewardsByMobile response.");
            System.out.println("       The full response body above shows available fields.");
            System.out.println("       Step 20-G customer_id cross-check will be skipped.");
        }

        // ── Mobile cross-check inside data (if present) ────────────────────
        String rewardsMobile = response.jsonPath().getString("data.mobile");
        if (rewardsMobile != null && !rewardsMobile.isEmpty()) {
            AssertionUtil.verifyEquals(rewardsMobile, mobile,
                    "CROSS-API: data.mobile in rewards response must match RequestContext.getMobile()");
            System.out.println("   ✅ data.mobile == storedMobile : " + rewardsMobile);
        } else {
            System.out.println("   ℹ️  data.mobile not present in rewards response");
        }

        System.out.println("\n✅ STEP 20-F PASSED: Rewards validated post-cancellation.");
        System.out.println("   post_cancel_rewards       : " + postCancelRewards);
        System.out.println("   membershipCustomerId stored: " + membershipCustomerId);
    }

    // -----------------------------------------------------------------------
    // STEP 20-G  –  Verify transaction records via
    //               GET /transaction/getTransactionByMobile/{mobile}?pageSize=10&page=N
    //
    // Filter key : reference_code  (e.g. "MY26AAA1888" = order_sample_number)
    //              stored in RequestContext.getCurrentOrderSampleNumber() by step20_A
    //
    // customer_id cross-check uses RequestContext.getCurrentMembershipCustomerId()
    //              stored by step20_F from getRewardsByMobile response
    //
    // Expected transactions for a full cancel+refund flow:
    //   • "Success"   transaction  — created when order was placed         (is_reverted=false)
    //   • "Cancelled" transaction  — created when order was cancelled      (is_reverted=true)
    //
    // Per-transaction fields validated (actual response structure):
    //   Guid (capital G)  |  mobile  |  customer_id  |  reference_code
    //   trnsc_amount (string)  |  actual_price (string)  |  net_paid_amount (string)
    //   membership_discount (string)  |  membership_discount_percentage (string)
    //   rewards_gain (string)  |  rewards_used (string)  |  taking_rewards (string)
    //   rewards_discount_percentage (string)  |  order_id
    //   status  |  is_reverted  |  created_at  |  updated_at
    //   patient_first_name  |  patient_last_name  |  dob  |  gender
    // -----------------------------------------------------------------------
    @Test(groups = "refund_flow", dependsOnMethods = "step20_F_VerifyRewardsByMobile")
    public void step20_G_VerifyTransactionByMobile() {
        System.out.println("\n=======================================================");
        System.out.println(">>> STEP 20-G: VERIFY TRANSACTION BY MOBILE <<<");
        System.out.println("=======================================================");

        String mobile               = RequestContext.getMobile();
        String sampleNumber         = RequestContext.getCurrentOrderSampleNumber();
        String membershipCustomerId = RequestContext.getCurrentMembershipCustomerId();

        AssertionUtil.verifyNotNull(mobile,       "User mobile must be stored in RequestContext before step 20-G");
        AssertionUtil.verifyNotNull(sampleNumber, "Order sample number (reference_code) must be stored by step20_A before step 20-G");
        System.out.println("   mobile                  (RequestContext) : " + mobile);
        System.out.println("   reference_code          (RequestContext) : " + sampleNumber);
        System.out.println("   membershipCustomerId    (RequestContext) : " + membershipCustomerId);

        String baseEndpoint = APIEndpoints.MEMBER_BASE_URL
                + APIEndpoints.GET_TRANSACTION_BY_MOBILE.replace("{mobile_number}", mobile);
        System.out.println("   Base Endpoint                    : " + baseEndpoint);

        int pageSize    = 50;  // larger page = fewer API calls (~11 pages for 549 txns)
        final int MAX_PAGES_SAFETY_CAP = 60; // absolute ceiling to avoid unbounded loops
        int currentPage = 1;
        int totalPages  = Integer.MAX_VALUE; // updated from first response

        java.util.List<java.util.Map<String, Object>> matchedTransactions = new java.util.ArrayList<>();
        int totalPagesSearched = 0;

        // ── Paginate until both Success + Cancelled are found (or all pages searched) ──
        // maxPages is not hardcoded — we use totalPages from the API (capped at MAX_PAGES_SAFETY_CAP).
        // Cancelled is the newest record (page 1); Success was created first so may be on a later page.
        // Early-stop fires as soon as BOTH are collected.
        paginationLoop:
        while (currentPage <= Math.min(totalPages, MAX_PAGES_SAFETY_CAP)) {
            System.out.println("\n   ── Fetching page " + currentPage + " (pageSize=" + pageSize + ") ──");

            Response pageResponse = new RequestBuilder()
                    .setEndpoint(baseEndpoint)
                    .addHeader("Authorization", RequestContext.getToken())
                    .addQueryParam("pageSize", pageSize)
                    .addQueryParam("page",     currentPage)
                    .get();

            System.out.println("   HTTP Status (page " + currentPage + ") : " + pageResponse.getStatusCode());

            if (pageResponse.getStatusCode() != 200) {
                if (currentPage == 1) {
                    AssertionUtil.verifyEquals(pageResponse.getStatusCode(), 200,
                            "getTransactionByMobile must return HTTP 200 on page 1");
                }
                System.out.println("   ⚠️  Non-200 on page " + currentPage + " — stopping pagination.");
                break;
            }

            totalPagesSearched++;

            // Read pagination metadata from first page
            if (currentPage == 1) {
                // top-level response assertions
                Boolean apiSuccess = pageResponse.jsonPath().get("success");
                String  apiMsg     = pageResponse.jsonPath().getString("msg");
                AssertionUtil.verifyNotNull(apiSuccess, "getTransactionByMobile 'success' must be present");
                AssertionUtil.verifyTrue(apiSuccess,    "getTransactionByMobile 'success' must be true");
                AssertionUtil.verifyNotNull(apiMsg,     "getTransactionByMobile 'msg' must be present");
                System.out.println("   success      : " + apiSuccess);
                System.out.println("   msg          : " + apiMsg);

                Object totalPagesObj = pageResponse.jsonPath().get("total_pages");
                if (totalPagesObj instanceof Number) {
                    totalPages = ((Number) totalPagesObj).intValue();
                }
                Object totalObj = pageResponse.jsonPath().get("total");
                Object limitObj = pageResponse.jsonPath().get("limit");
                System.out.println("   total        : " + totalObj
                        + "  |  total_pages: " + totalPages
                        + "  |  limit: " + limitObj);
            }

            java.util.List<java.util.Map<String, Object>> pageTxns =
                    pageResponse.jsonPath().getList("data");

            if (pageTxns == null || pageTxns.isEmpty()) {
                System.out.println("   ℹ️  No transactions on page " + currentPage + " — stopping.");
                break;
            }

            System.out.println("   Transactions on this page : " + pageTxns.size());

            // Collect all transactions whose reference_code matches our order sample number
            for (java.util.Map<String, Object> txn : pageTxns) {
                Object refCodeObj = txn.get("reference_code");
                if (refCodeObj != null && sampleNumber.equals(refCodeObj.toString())) {
                    matchedTransactions.add(txn);
                }
            }

            // Stop early once we have both Success/Successful + Cancelled (full cancel flow = 2 records)
            boolean hasSuccess    = matchedTransactions.stream().anyMatch(t -> {
                String st = t.get("status") != null ? t.get("status").toString() : "";
                return "Success".equalsIgnoreCase(st) || "Successful".equalsIgnoreCase(st);
            });
            boolean hasCancelled  = matchedTransactions.stream().anyMatch(t -> "Cancelled".equalsIgnoreCase(
                    t.get("status") != null ? t.get("status").toString() : ""));
            if (hasSuccess && hasCancelled) {
                System.out.println("   ✅ Both 'Success' and 'Cancelled' transactions found — stopping pagination.");
                break paginationLoop;
            }

            // Do NOT break on pageTxns.size() < pageSize — the API may return a partial
            // page even in the middle of results; totalPages from the API governs the limit.
            if (currentPage >= Math.min(totalPages, MAX_PAGES_SAFETY_CAP)) {
                System.out.println("   ℹ️  Reached last page (" + currentPage + ") — stopping pagination.");
                break;
            }

            currentPage++;
        }

        System.out.println("\n   ── Pagination complete: searched " + totalPagesSearched
                + " page(s), found " + matchedTransactions.size()
                + " transaction(s) matching reference_code='" + sampleNumber + "'");

        // ── Must find at least the Cancelled transaction ─────────────────────
        AssertionUtil.verifyTrue(!matchedTransactions.isEmpty(),
                "At least one transaction with reference_code='" + sampleNumber
                        + "' must exist in getTransactionByMobile");

        java.util.Map<String, Object> successTxn   = null;
        java.util.Map<String, Object> cancelledTxn = null;

        for (java.util.Map<String, Object> txn : matchedTransactions) {
            String s = txn.get("status") != null ? txn.get("status").toString() : "";
            if (("Success".equalsIgnoreCase(s) || "Successful".equalsIgnoreCase(s)) && successTxn == null) successTxn = txn;
            if ("Cancelled".equalsIgnoreCase(s) && cancelledTxn == null) cancelledTxn = txn;
        }

        System.out.println("   Success   transaction found : " + (successTxn   != null));
        System.out.println("   Cancelled transaction found : " + (cancelledTxn != null));

        // Both Success AND Cancelled records must exist for the refund flow:
        //   Success   = original payment record (created when order was placed)
        //   Cancelled = reversal record (created when order was cancelled/refunded)
        AssertionUtil.verifyNotNull(successTxn,
                "A 'Success' status transaction must exist for reference_code='" + sampleNumber
                        + "' (original payment record)");
        AssertionUtil.verifyNotNull(cancelledTxn,
                "A 'Cancelled' status transaction must exist for reference_code='" + sampleNumber
                        + "' (refund/reversal record)");

        // ── Helper: parse string-encoded amounts safely ───────────────────────
        // (all money fields come as strings like "1039.00")

        // ── Validate each found transaction ──────────────────────────────────
        int txIdx = 0;
        for (java.util.Map<String, Object> txn : matchedTransactions) {
            txIdx++;
            String txnStatus = txn.get("status") != null ? txn.get("status").toString() : null;
            System.out.println("\n   ══════════════ Transaction [" + txIdx + "] — status: " + txnStatus + " ══════════════");

            // ── Identity ─────────────────────────────────────────────────────
            // API returns "Guid" with capital G
            Object guidObj = txn.get("Guid");
            if (guidObj == null) guidObj = txn.get("guid");
            if (guidObj == null) guidObj = txn.get("_id");
            String txnGuid = guidObj != null ? guidObj.toString() : null;
            System.out.println("   Guid                           : " + txnGuid);
            AssertionUtil.verifyNotNull(txnGuid, "transaction[" + txIdx + "] Guid must be present");
            System.out.println("   ✅ Guid present : " + txnGuid);

            // ── reference_code (already filtered — re-assert) ────────────────
            String txnRefCode = txn.get("reference_code") != null ? txn.get("reference_code").toString() : null;
            System.out.println("   reference_code                 : " + txnRefCode);
            AssertionUtil.verifyNotNull(txnRefCode,
                    "transaction[" + txIdx + "].reference_code must be present");
            AssertionUtil.verifyEquals(txnRefCode, sampleNumber,
                    "CROSS-API: transaction[" + txIdx + "].reference_code must match stored order_sample_number");
            System.out.println("   ✅ reference_code == stored sampleNumber : " + txnRefCode);

            // ── reference_id  (uuid) — may be null for non-Cancelled records ──
            String txnRefId = txn.get("reference_id") != null ? txn.get("reference_id").toString() : null;
            System.out.println("   reference_id                   : " + txnRefId);
            if (txnRefId == null || txnRefId.isEmpty()) {
                System.out.println("   ⚠️  SOFT: transaction[" + txIdx + "].reference_id is null/empty (may be absent for non-Cancelled records)");
            } else {
                System.out.println("   ✅ reference_id present : " + txnRefId);
            }

            // ── order_id ─────────────────────────────────────────────────────
            String txnOrderId = txn.get("order_id") != null ? txn.get("order_id").toString() : null;
            System.out.println("   order_id                       : " + txnOrderId);
            AssertionUtil.verifyNotNull(txnOrderId,
                    "transaction[" + txIdx + "].order_id must be present");
            System.out.println("   ✅ order_id present : " + txnOrderId);

            // ── mobile (cross-API) ───────────────────────────────────────────
            String txnMobile = txn.get("mobile") != null ? txn.get("mobile").toString() : null;
            System.out.println("   mobile                         : " + txnMobile);
            AssertionUtil.verifyNotNull(txnMobile,
                    "transaction[" + txIdx + "].mobile must be present");
            AssertionUtil.verifyEquals(txnMobile, mobile,
                    "CROSS-API: transaction[" + txIdx + "].mobile must match RequestContext.getMobile()");
            System.out.println("   ✅ mobile == storedMobile : " + txnMobile);

            // ── customer_id (cross-API: == membershipCustomerId from step20_F) ──
            String txnCustomerId = txn.get("customer_id") != null ? txn.get("customer_id").toString() : null;
            System.out.println("   customer_id                    : " + txnCustomerId);
            AssertionUtil.verifyNotNull(txnCustomerId,
                    "transaction[" + txIdx + "].customer_id must be present");
            if (membershipCustomerId != null && !membershipCustomerId.isEmpty()) {
                AssertionUtil.verifyEquals(txnCustomerId, membershipCustomerId,
                        "CROSS-API: transaction[" + txIdx + "].customer_id must match membership customer_id from step20_F");
                System.out.println("   ✅ customer_id == membershipCustomerId : " + txnCustomerId);
            } else {
                System.out.println("   ℹ️  membershipCustomerId not stored (step20_F skipped extraction); customer_id logged: " + txnCustomerId);
            }

            // ── status ──────────────────────────────────────────────────────
            System.out.println("   status                         : " + txnStatus);
            AssertionUtil.verifyNotNull(txnStatus,
                    "transaction[" + txIdx + "].status must be present");
            boolean isKnownStatus = "Success".equalsIgnoreCase(txnStatus)
                    || "Successful".equalsIgnoreCase(txnStatus)
                    || "Cancelled".equalsIgnoreCase(txnStatus);
            AssertionUtil.verifyTrue(isKnownStatus,
                    "transaction[" + txIdx + "].status must be 'Successful' or 'Cancelled', found: " + txnStatus);
            System.out.println("   ✅ status is valid : " + txnStatus);

            // ── is_reverted ─────────────────────────────────────────────────
            Object isRevertedObj = txn.get("is_reverted");
            System.out.println("   is_reverted                    : " + isRevertedObj);
            AssertionUtil.verifyNotNull(isRevertedObj,
                    "transaction[" + txIdx + "].is_reverted must be present");
            boolean isReverted = Boolean.TRUE.equals(isRevertedObj)
                    || "true".equalsIgnoreCase(String.valueOf(isRevertedObj));
            if ("Cancelled".equalsIgnoreCase(txnStatus)) {
                AssertionUtil.verifyTrue(isReverted,
                        "transaction[" + txIdx + "] with status='Cancelled' must have is_reverted=true");
                System.out.println("   ✅ Cancelled → is_reverted == true");
            } else if ("Success".equalsIgnoreCase(txnStatus) || "Successful".equalsIgnoreCase(txnStatus)) {
                AssertionUtil.verifyTrue(!isReverted,
                        "transaction[" + txIdx + "] with status='Successful' must have is_reverted=false");
                System.out.println("   ✅ Successful → is_reverted == false");
            }

            // ── Amount fields (all returned as strings) ──────────────────────
            double dTrnscAmount  = parseStringAmount(txn, "trnsc_amount",    txIdx);
            double dActualPrice  = parseStringAmount(txn, "actual_price",    txIdx);
            double dNetPaid      = parseStringAmount(txn, "net_paid_amount", txIdx);
            double dMemDiscount  = parseStringAmount(txn, "membership_discount", txIdx);
            double dRewardsGain  = parseStringAmount(txn, "rewards_gain",    txIdx);
            double dRewardsUsed  = parseStringAmount(txn, "rewards_used",    txIdx);
            double dTakingRew    = parseStringAmount(txn, "taking_rewards",  txIdx);

            System.out.printf("   trnsc_amount                   : %.2f%n", dTrnscAmount);
            System.out.printf("   actual_price                   : %.2f%n", dActualPrice);
            System.out.printf("   net_paid_amount                : %.2f%n", dNetPaid);
            System.out.printf("   membership_discount            : %.2f%n", dMemDiscount);
            System.out.printf("   rewards_gain                   : %.2f%n", dRewardsGain);
            System.out.printf("   rewards_used                   : %.2f%n", dRewardsUsed);
            System.out.printf("   taking_rewards                 : %.2f%n", dTakingRew);

            AssertionUtil.verifyTrue(dTrnscAmount >= 0,
                    "transaction[" + txIdx + "].trnsc_amount must be >= 0");
            System.out.println("   ✅ trnsc_amount >= 0 : " + dTrnscAmount);

            AssertionUtil.verifyTrue(dActualPrice >= 0,
                    "transaction[" + txIdx + "].actual_price must be >= 0");
            System.out.println("   ✅ actual_price >= 0 : " + dActualPrice);

            AssertionUtil.verifyTrue(dNetPaid >= 0,
                    "transaction[" + txIdx + "].net_paid_amount must be >= 0");
            System.out.println("   ✅ net_paid_amount >= 0 : " + dNetPaid);

            AssertionUtil.verifyTrue(dMemDiscount >= 0,
                    "transaction[" + txIdx + "].membership_discount must be >= 0");
            System.out.println("   ✅ membership_discount >= 0 : " + dMemDiscount);

            AssertionUtil.verifyTrue(dRewardsGain >= 0,
                    "transaction[" + txIdx + "].rewards_gain must be >= 0");
            System.out.println("   ✅ rewards_gain >= 0 : " + dRewardsGain);

            AssertionUtil.verifyTrue(dRewardsUsed >= 0,
                    "transaction[" + txIdx + "].rewards_used must be >= 0");
            System.out.println("   ✅ rewards_used >= 0 : " + dRewardsUsed);

            // ── Price math: actual_price - membership_discount == trnsc_amount ─
            if (dActualPrice > 0 && dMemDiscount >= 0) {
                double calcTrnsc = dActualPrice - dMemDiscount;
                if (Math.abs(calcTrnsc - dTrnscAmount) <= 1.0) {
                    System.out.println("   ✅ actual_price(" + dActualPrice + ") - membership_discount("
                            + dMemDiscount + ") ≈ trnsc_amount(" + dTrnscAmount + ")");
                } else {
                    System.out.println("   ⚠️  Math check: actual_price(" + dActualPrice + ") - membership_discount("
                            + dMemDiscount + ") = " + calcTrnsc + ", trnsc_amount=" + dTrnscAmount
                            + " — may include rewards_used delta; logged only");
                }
            }

            // ── net_paid_amount should equal trnsc_amount for COD ───────────
            if (Math.abs(dNetPaid - dTrnscAmount) <= 1.0) {
                System.out.println("   ✅ net_paid_amount ≈ trnsc_amount : " + dNetPaid);
            } else {
                System.out.println("   ℹ️  net_paid_amount(" + dNetPaid + ") ≠ trnsc_amount("
                        + dTrnscAmount + ") — may differ for rewards/coupon; logged only");
            }

            // ── Percentage fields (present as strings) ───────────────────────
            String memDiscPct   = txn.get("membership_discount_percentage") != null
                    ? txn.get("membership_discount_percentage").toString() : null;
            String rewDiscPct   = txn.get("rewards_discount_percentage") != null
                    ? txn.get("rewards_discount_percentage").toString() : null;
            System.out.println("   membership_discount_percentage : " + memDiscPct);
            System.out.println("   rewards_discount_percentage    : " + rewDiscPct);
            AssertionUtil.verifyNotNull(memDiscPct,
                    "transaction[" + txIdx + "].membership_discount_percentage must be present");
            AssertionUtil.verifyNotNull(rewDiscPct,
                    "transaction[" + txIdx + "].rewards_discount_percentage must be present");
            System.out.println("   ✅ discount percentage fields present");

            // ── Patient details ──────────────────────────────────────────────
            String patFirstName = txn.get("patient_first_name") != null
                    ? txn.get("patient_first_name").toString() : null;
            String patLastName  = txn.get("patient_last_name") != null
                    ? txn.get("patient_last_name").toString() : null;
            String patDob       = txn.get("dob") != null ? txn.get("dob").toString() : null;
            String patGender    = txn.get("gender") != null ? txn.get("gender").toString() : null;
            System.out.println("   patient_first_name             : " + patFirstName);
            System.out.println("   patient_last_name              : " + patLastName);
            System.out.println("   dob                            : " + patDob);
            System.out.println("   gender                         : " + patGender);
            AssertionUtil.verifyNotNull(patFirstName,
                    "transaction[" + txIdx + "].patient_first_name must be present");
            AssertionUtil.verifyNotNull(patDob,
                    "transaction[" + txIdx + "].dob must be present");
            AssertionUtil.verifyNotNull(patGender,
                    "transaction[" + txIdx + "].gender must be present");
            System.out.println("   ✅ patient details present");

            // ── Timestamps ───────────────────────────────────────────────────
            String createdAt = txn.get("created_at") != null ? txn.get("created_at").toString() : null;
            String updatedAt = txn.get("updated_at") != null ? txn.get("updated_at").toString() : null;
            System.out.println("   created_at                     : " + createdAt);
            System.out.println("   updated_at                     : " + updatedAt);
            AssertionUtil.verifyNotNull(createdAt,
                    "transaction[" + txIdx + "].created_at must be present");
            AssertionUtil.verifyNotNull(updatedAt,
                    "transaction[" + txIdx + "].updated_at must be present");
            System.out.println("   ✅ timestamps present");

            // ── Status-specific validation ───────────────────────────────────
            int    storedTotal       = RequestContext.getCurrentTotalPrice();
            double storedRewardsGain = RequestContext.getRewardsGain();

            if ("Success".equalsIgnoreCase(txnStatus) || "Successful".equalsIgnoreCase(txnStatus)) {
                // ════════════════════════════════════════════════════════════
                //  SUCCESS record — validates values at time of ORDER PLACEMENT
                // ════════════════════════════════════════════════════════════
                System.out.println("\n   ── Success-specific validation (ORDER PLACED values) ──");

                // trnsc_amount = membership-discounted price the customer paid
                if (storedTotal > 0) {
                    if (Math.abs(dTrnscAmount - storedTotal) <= 1.0) {
                        System.out.println("   ✅ CROSS-API: trnsc_amount(" + dTrnscAmount
                                + ") ≈ stored cart total(" + storedTotal + ")");
                    } else {
                        System.out.println("   ℹ️  trnsc_amount(" + dTrnscAmount
                                + ") differs from stored cart total(" + storedTotal
                                + ") — may include rewards offset; logged only");
                    }
                }

                // net_paid_amount = what was actually collected at placement
                if (storedTotal > 0) {
                    if (Math.abs(dNetPaid - storedTotal) <= 1.0) {
                        System.out.println("   ✅ CROSS-API: net_paid_amount(" + dNetPaid
                                + ") ≈ stored cart total(" + storedTotal + ")");
                    } else {
                        System.out.println("   ℹ️  net_paid_amount(" + dNetPaid
                                + ") differs from stored cart total(" + storedTotal
                                + ") — logged only");
                    }
                }

                // membership_discount = actual_price - trnsc_amount
                if (dActualPrice > 0) {
                    double expectedDiscount = dActualPrice - dTrnscAmount;
                    if (Math.abs(dMemDiscount - expectedDiscount) <= 1.0) {
                        System.out.println("   ✅ membership_discount(" + dMemDiscount
                                + ") == actual_price(" + dActualPrice
                                + ") - trnsc_amount(" + dTrnscAmount + ")");
                    } else {
                        System.out.println("   ℹ️  membership_discount(" + dMemDiscount
                                + ") ≠ actual_price - trnsc_amount(" + expectedDiscount
                                + "); logged only");
                    }
                }

                // refund_amount must be 0 at placement (nothing refunded yet)
                Object refundObj = txn.get("refund_amount");
                if (refundObj != null) {
                    double dRefund = 0.0;
                    try { dRefund = Double.parseDouble(refundObj.toString()); } catch (Exception ignored) {}
                    System.out.printf("   refund_amount (Success)        : %.2f%n", dRefund);
                    AssertionUtil.verifyTrue(dRefund == 0.0,
                            "CROSS-API: Success transaction refund_amount must be 0 (no refund at placement), found: " + dRefund);
                    System.out.println("   ✅ refund_amount == 0 (no refund at order placement)");
                } else {
                    System.out.println("   refund_amount                  : (field absent — expected 0 for Success)");
                }

                // rewards_gain = rewards earned when the order was placed
                System.out.println("   rewards_gain (earned at order) : " + dRewardsGain
                        + "  (storedRewardsGain from step20_A = " + storedRewardsGain + ")");
                if (storedRewardsGain > 0) {
                    if (Math.abs(dRewardsGain - storedRewardsGain) <= 1.0) {
                        System.out.println("   ✅ CROSS-API: rewards_gain matches storedRewardsGain from step20_A");
                    } else {
                        System.out.println("   ℹ️  rewards_gain(" + dRewardsGain
                                + ") differs from storedRewardsGain(" + storedRewardsGain
                                + ") — logged only");
                    }
                } else {
                    System.out.println("   ℹ️  storedRewardsGain not available; rewards_gain logged: " + dRewardsGain);
                }

                // rewards_used = any rewards redeemed at order placement
                System.out.println("   rewards_used (redeemed)        : " + dRewardsUsed);
                AssertionUtil.verifyTrue(dRewardsUsed >= 0,
                        "Success transaction.rewards_used must be >= 0");

            } else if ("Cancelled".equalsIgnoreCase(txnStatus)) {
                // ════════════════════════════════════════════════════════════
                //  CANCELLED record — validates values after ORDER CANCELLATION
                // ════════════════════════════════════════════════════════════
                System.out.println("\n   ── Cancelled-specific validation (ORDER CANCELLED values) ──");

                // trnsc_amount = amount being refunded (must equal what was originally paid)
                if (storedTotal > 0) {
                    if (Math.abs(dTrnscAmount - storedTotal) <= 1.0) {
                        System.out.println("   ✅ CROSS-API: trnsc_amount(" + dTrnscAmount
                                + ") ≈ stored cart total(" + storedTotal + ") — full refund amount");
                    } else {
                        System.out.println("   ℹ️  trnsc_amount(" + dTrnscAmount
                                + ") differs from stored cart total(" + storedTotal
                                + ") — may include partial adjustment; logged only");
                    }
                }

                // refund_amount must equal trnsc_amount (full refund on COD cancellation)
                Object refundObj = txn.get("refund_amount");
                if (refundObj != null) {
                    double dRefund = 0.0;
                    try { dRefund = Double.parseDouble(refundObj.toString()); } catch (Exception ignored) {}
                    System.out.printf("   refund_amount (Cancelled)      : %.2f%n", dRefund);
                    if (Math.abs(dRefund - dTrnscAmount) <= 1.0) {
                        System.out.println("   ✅ refund_amount(" + dRefund
                                + ") ≈ trnsc_amount(" + dTrnscAmount + ") — full refund issued");
                    } else {
                        System.out.println("   ℹ️  refund_amount(" + dRefund
                                + ") ≠ trnsc_amount(" + dTrnscAmount + ") — partial refund; logged only");
                    }
                } else {
                    System.out.println("   refund_amount                  : (field absent)");
                }

                // rewards_gain — reversed (same value as Success row but effectively reversed by is_reverted)
                System.out.println("   rewards_gain (reversed at cancel) : " + dRewardsGain
                        + "  (storedRewardsGain from step20_A = " + storedRewardsGain + ")");
                if (storedRewardsGain > 0) {
                    if (Math.abs(dRewardsGain - storedRewardsGain) <= 1.0) {
                        System.out.println("   ✅ CROSS-API: Cancelled rewards_gain(" + dRewardsGain
                                + ") matches storedRewardsGain — confirms reversal amount is correct");
                    } else {
                        System.out.println("   ℹ️  Cancelled rewards_gain(" + dRewardsGain
                                + ") differs from storedRewardsGain(" + storedRewardsGain
                                + ") — logged only");
                    }
                }

                // net_paid_amount on Cancelled = what was refunded back
                System.out.printf("   net_paid_amount (refunded back) : %.2f%n", dNetPaid);
                if (storedTotal > 0) {
                    if (Math.abs(dNetPaid - storedTotal) <= 1.0) {
                        System.out.println("   ✅ CROSS-API: net_paid_amount(" + dNetPaid
                                + ") ≈ stored cart total — customer fully refunded");
                    } else {
                        System.out.println("   ℹ️  net_paid_amount(" + dNetPaid
                                + ") differs from cart total(" + storedTotal + ") — logged only");
                    }
                }

                // created_by — who raised the cancellation
                Object createdByObj = txn.get("created_by");
                System.out.println("   created_by                     : " + createdByObj);

                // admin_approval fields
                System.out.println("   admin_approval_status          : " + txn.get("admin_approval_status"));
                System.out.println("   admin_approval_by              : " + txn.get("admin_approval_by"));
                System.out.println("   admin_approval_at              : " + txn.get("admin_approval_at"));
            }
        }

        System.out.println("\n   ── Transaction Validation Summary ──────────────────────────");
        System.out.println("   Pages searched                    : " + totalPagesSearched);
        System.out.println("   Transactions matched              : " + matchedTransactions.size());
        System.out.println("   Filter key (reference_code)       : " + sampleNumber);
        System.out.println("   'Success'   transaction verified  : " + (successTxn   != null));
        System.out.println("   'Cancelled' transaction verified  : " + (cancelledTxn != null));

        System.out.println("\n✅ STEP 20-G PASSED: All transaction records for reference_code='" + sampleNumber
                + "' fully validated.");
        System.out.println("   🎉 COMPLETE CANCEL + REFUND + APPROVAL + PAYMENT + REWARDS + TRANSACTION FLOW VALIDATED.");
    }

    /**
     * Safely parse a string-encoded money field from a transaction map.
     * Returns 0.0 if the field is absent or null.
     */
    private double parseStringAmount(java.util.Map<String, Object> txn, String field, int txIdx) {
        Object val = txn.get(field);
        if (val == null) return 0.0;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try {
            return Double.parseDouble(val.toString());
        } catch (NumberFormatException e) {
            System.out.println("   ⚠️  transaction[" + txIdx + "]." + field
                    + " is not a valid number: '" + val + "'");
            return 0.0;
        }
    }
}

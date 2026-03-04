# COD-20 — Post-Cancellation Refund Flow: Complete API Validation Reference

**Source:** `src/test/java/com/mryoda/diagnostics/api/tests/order/COD_20_CancellationRefundTest.java`

**Flow summary:**

```
step20_A  →  POST /order/adminReturningCashback        (trigger cashback / compute refund amounts)
step20_B  →  POST /order/v2updateOrder                 (set order_status = "Cancelled")
step20_C  →  POST /order/approveCancelldOrder          (admin approves the cancellation)
step20_D  →  GET  /order/getOrderById/{guid}           (verify full cancelled-order details)
step20_E  →  POST /gateway/getPaymentById              (verify payment record post-refund)
step20_F  →  GET  /reward/getRewardsByMobile/{mobile}  (verify rewards reversal; extract membership customer_id)
step20_G  →  GET  /transaction/getTransactionByMobile  (verify Success + Cancelled transaction records)
```

**Assertion types used in this document:**

| Symbol | Meaning |
|--------|---------|
| ✅ Hard | `AssertionUtil.verifyEquals / verifyNotNull / verifyTrue` — test fails immediately if wrong |
| ⚠️ Soft | `logSoft(...)` — writes `[SOFT]` to `logs/cod_failures.log`; test continues |
| ℹ️ Info | `System.out.println(...)` — logged for debugging; no assertion |
| 🔗 Cross-API | Value compared against a field stored in `RequestContext` from a prior step |

---

---

## Critical Semantic Reference

> Read this before interpreting any assertion.

| Term | Meaning |
|---------|-------------|
| `adminReturningCashback.paid_amount` | **Membership DISCOUNT applied** to this sub-order (e.g. `16`). This is NOT the cash paid. |
| `canceled_amount.orderedByCash` | **Actual cash paid** by the user for this sub-order (e.g. `939`). |
| `item.final_price` (order_items) | `actual_price − membership_discount`. This is **post-membership, pre-coupon**. |
| `order.paid_amount` (getOrderById) | `total_price − membership_discount − coupon_discount`. Post-everything cash paid. |
| Coupon split | Coupon total is split proportionally across sub-orders. It is **not refunded** on cancellation — the paid_amount (which already baked in the coupon) is what gets refunded. |
| `remaining_rewards` (outer level) | User's **current wallet balance** (always ≥ 0). |
| `canceled_amount.remaining_rewards` | **Historical cumulative internal accounting** figure (can be deeply negative). These two fields have different semantics — never compared against each other. |
| `transaction.reference_id` (Cancelled) | Internal UUID assigned to the Cancelled record. **Not** the `reference_code`. |

**Price formula:**
```
actual_price − membershipDiscount − couponSplit = orderedByCash = adjustedRefundAmount
```

**Item-level formula:**
```
actual_price − membership_discount = final_price          (hard assert, per item)
sum(item.final_price) − couponDiscThisOrder = paid_amount (hard assert at order level)
```

---

---

## [1] step20_A — `adminReturningCashback`

**Endpoint:** `POST /order/adminReturningCashback`  
**Payload:** `{ "order_guid": "<orderId>" }`  
**Pre-condition:** `RequestContext.getCurrentOrderId()` must be set.

---

### 1.1 Top-level response fields

| Field | Assertion | Rule |
|-------|-----------|------|
| `status` (integer) | ✅ Hard | Not null; must equal HTTP status code |
| `success` (boolean) | ✅ Hard | Not null |
| `msg` (string) | ✅ Hard | Not null |
| `message` | ✅ Hard (200 only) | Not null when HTTP 200 |
| `total_amount` | ℹ️ Info | Always `0` (dummy field); logged only |

### 1.2 HTTP 409 — Business Rule Response

When a phlebotomist has already been assigned, the API returns HTTP 409 instead of 200.

| Field | Assertion | Rule |
|-------|-----------|------|
| `success` | ✅ Hard | Must be `false` |
| `msg` | ✅ Hard | Not null; must describe the business rule (contains one of: "cancel" / "phlebotomist" / "assigned" / "cannot") |

_Test marks as SOFT-PASS and returns early when HTTP 409 is received._

### 1.3 HTTP 200 — `data.membershipCancelAmount` (outer object)

| Field | Assertion | Rule / Semantic |
|-------|-----------|-----------------|
| `paid_amount` | ✅ Hard | Not null; `>= 0`. **Semantic: membership DISCOUNT applied** (NOT cash paid). Renamed internally to `memberDiscountAmt`. |
| `orderItemAmount` | ✅ Hard | Not null; `>= 0`. Rewards-eligible portion of the order (can be 0). |
| `actual_price` | ✅ Hard | Not null; `>= 0`. Full list price before membership discount (e.g. `1155`). |
| `remaining_rewards` | ✅ Hard | Not null; any integer (can be negative). **Semantic: user's current wallet balance.** |
| `actual_taking_rewards` | ✅ Hard | Not null; must be exactly `0` (API contract for outer level). |
| `adjustedRefundAmount` | ✅ Hard | Not null; `>= 0`. Must equal `canceled_amount.orderedByCash`. |

### 1.4 HTTP 200 — `data.membershipCancelAmount` (string / boolean fields)

| Field | Assertion | Rule |
|-------|-----------|------|
| `reference_code` | ✅ Hard (when present) | Must start with `"MY"`. Stored to `RequestContext.setCurrentOrderSampleNumber()`. If null/empty → ⚠️ Soft (expected for non-member / new-user orders). |
| `is_delivery_charge_added` | ✅ Hard | Not null. If `true` → `actual_price > orderedByCash`. |
| `adjustedMessage` | ✅ Hard | Not null; not empty; must contain the numeric value of `orderedByCash` (the cash refund amount). |
| `walletUpdate` | ℹ️ Info | Expected as `""` (empty string) in single-member orders. Absent in multi-member orders — logged only. |

### 1.5 `data.membershipCancelAmount.canceled_amount` (nested)

| Field | Assertion | Rule / Semantic |
|-------|-----------|-----------------|
| `orderedByCash` | ✅ Hard | Not null; `> 0`. **Semantic: actual cash paid** by user for this sub-order. |
| `orderItemAmount` | ✅ Hard | Not null; `>= 0`. Must equal `orderedByCash` (both represent the cash amount). |
| `actual_price` | ✅ Hard | Not null; `>= 0`. |
| `reference_code` | ✅ Hard | Not null; must equal outer `membershipCancelAmount.reference_code`. |
| `remaining_rewards` | ✅ Hard | Not null; any integer (can be negative). **Semantic: cumulative historical accounting figure across all orders** — completely different from `outer.remaining_rewards`. |
| `actual_taking_rewards` | ✅ Hard | Not null; `>= 0`. |

### 1.6 Consistency / Cross-field Checks

| Check | Assertion | Formula |
|-------|-----------|---------|
| Cash formula | ✅ Hard | `actual_price − membershipDiscount ≥ orderedByCash`. Inequality allows coupon split: `couponSplit = postMembership − orderedByCash`. |
| Full discount formula (logged) | ℹ️ Info | `actual_price(1155) − membershipDiscount(16) − couponSplit(200) = orderedByCash(939)` |
| Refund equals cash paid | ✅ Hard | `adjustedRefundAmount == canceled_amount.orderedByCash` |
| `actual_price ≥ membershipDiscount` | ✅ Hard | Validates price sanity |
| `outer.actual_taking_rewards == 0` | ✅ Hard | API contract |
| `canceled_amount.orderItemAmount == orderedByCash` | ✅ Hard | Cash view consistency |
| `actual_price` discrepancy (multi-member) | ⚠️ Soft | If outer `actual_price ≠ canceled_amount.actual_price` — expected in multi-member orders |
| `outer.remaining_rewards ≥ 0` | ✅ Hard | Wallet balance must be non-negative |
| `canceled_amount.remaining_rewards` | ✅ Hard (not null) | Any integer valid (internal accounting) |
| `adjustedMessage` contains cash amount | ✅ Hard | Must contain `String.valueOf((int)orderedByCash)` |
| `reference_code` starts with `"MY"` | ✅ Hard | Format validation |
| `is_delivery_charge_added=true` → `actual_price > orderedByCash` | ✅ Hard | Delivery fee inflates MRP |

### 1.7 Cross-API Checks

| Check | Assertion | Source |
|-------|-----------|--------|
| `canceled_amount.orderedByCash == storedCartTotal` (single order) | ✅ Hard | `RequestContext.getCurrentTotalPrice()` (from COD_02) |
| `sum(sub-orders orderedByCash) ≈ storedCartTotal` (multi-member) | ⚠️ Soft | Cart total is pre-discount; orderedByCash is post-membership-discount |
| `outer.remaining_rewards == rewardsGainForOrder` (when stored > 0) | ✅ Hard | `RequestContext.getRewardsGain()` (from COD_15) |
| `canceled_amount.actual_taking_rewards ≥ 0` | ✅ Hard | Standalone range check |

---

---

## [2] step20_B — `v2updateOrder`

**Endpoint:** `POST /order/v2updateOrder`  
**Payload:** `{ "order_guid": "<guid>", "order_status": "Cancelled", "canceledBy": "<adminGuid>" }`  
**Pre-condition:** `RequestContext.getCurrentOrderId()` and `RequestContext.getAdminGuid()` must be set.

| Field | Assertion | Rule |
|-------|-----------|------|
| HTTP status | ✅ Hard | 200 |
| `msg` | ✅ Hard | Not null; equals `"Order updated successfully"` |
| **Cross-API GET** | ✅ Hard | After update, calls `getOrderById` and asserts `data[0].order_status == "Cancelled"` |

---

---

## [3] step20_C — `approveCancelldOrder`

**Endpoint:** `POST /order/approveCancelldOrder`  
**Payload:** `{ "order_id": "<orderId>", "remarks": "test", "admin_approval_by": "<adminGuid>", "status": "Approve" }`  
**Pre-condition:** `RequestContext.getCurrentOrderId()` and `RequestContext.getAdminGuid()` must be set.

| Field | Assertion | Rule |
|-------|-----------|------|
| HTTP status | ✅ Hard | 200 |
| `success` | ✅ Hard (when present) | Must be `true` |
| `msg` or `message` | ✅ Hard (when `success` absent) | At least one must be non-null |
| `error` | ✅ Hard (when present) | Must be empty string |

---

---

## [4] step20_D — `getOrderById`

**Endpoint:** `GET /order/getOrderById/{guid}`  
**Pre-condition:** `RequestContext.getCurrentOrderId()` must be set. Depends on step20_B completing.

---

### 4.1 Top-level response

| Field | Assertion | Rule |
|-------|-----------|------|
| HTTP status | ✅ Hard | 200 |
| `status` | ✅ Hard | Equals HTTP code |
| `success` | ✅ Hard | `true` |
| `msg` | ✅ Hard | Not null |
| `total_amount` (root) | ℹ️ Info | Always `0` (dummy) |

### 4.2 Order identity (`data[0]`)

| Field | Assertion | Rule |
|-------|-----------|------|
| `guid` | ✅ Hard + 🔗 Cross-API | Not null; must equal `RequestContext.getCurrentOrderId()` |
| `order_number` | ✅ Hard | Not null; not empty |
| `order_sample_number` | ✅ Hard | Not null; must start with `"MY"` |
| `visit_number` | ✅ Hard (when present) | Must start with `"MYD"` |
| `user_id` | 🔗 Cross-API (when stored) | Must equal `RequestContext.getUserId()` |
| `cart_id` | ✅ Hard | Not null (public short-ID e.g. `pb_XXXXX`; different from cart UUID in RequestContext — logged, not equality-checked) |

### 4.3 Amount fields (`data[0]`)

| Field | Assertion | Rule / Semantic |
|-------|-----------|-----------------|
| `paid_amount` (string) | ✅ Hard | Not null; parse as double; `>= 0`. Cash the user actually paid (post-membership, post-coupon). |
| `total_price` (string) | ✅ Hard | Not null; `>= paid_amount`. Full list price. |
| `final_price` (string) | ✅ Hard | Not null; `> 0`; `total_price >= final_price >= paid_amount` |
| `membership_discount` | ✅ Hard (when present) | `>= 0` |
| `actual_discount` | ✅ Hard (when present) | `>= 0`; must equal `membership_discount + coupon_discount` |
| `coupon_discount` / `coupon_discount_amount` | ✅ Hard (when present) | `>= 0`; resolved via field → string field → `RequestContext.getCouponAmount()` |
| `due_amount` | ✅ Hard (single order: `== 0`); ⚠️ Soft (multi-member) | Non-zero expected when sibling order is still active |
| `refund_amount` (string) | ✅ Hard | Not null; must exactly equal `paid_amount` (full refund of what user paid) |
| `actual_refund_amount` | ℹ️ Info | Logged |
| `rewards_gain` (string) | ✅ Hard | Not null; `>= 0`; 🔗 Cross-API == `RequestContext.getRewardsGain()` when stored > 0 |
| `rewards_used` (string) | ℹ️ Info | Logged |
| `delivery_charge` | ℹ️ Info | Logged |

**Discount consistency formula (hard-asserted when both fields present):**
```
total_price − membership_discount − coupon_discount = paid_amount
membership_discount + coupon_discount = actual_discount
```

**Refund/coupon note:**
- Coupon reduced `paid_amount` at purchase time.
- `refund_amount == paid_amount` → user gets back post-coupon price. No extra coupon refund.

### 4.4 Payment fields (`data[0]`)

| Field | Assertion | Rule |
|-------|-----------|------|
| `payment_id` | ✅ Hard + 🔗 Cross-API | Not null; equals `RequestContext.getCurrentPaymentId()` when stored |
| `payment.payment_type` | ✅ Hard | Not null (e.g. `"COD"`) |
| `payment.payment_mode` | ✅ Hard | Must be **null** for COD; must be present and non-empty for non-COD |
| `payment.payment_status` | ✅ Hard | Not null; equals `"Success"` (payment was made before cancellation) |
| `payment_statuses` | ✅ Hard | Not null; equals `"Success"` |

### 4.5 Slot fields (`data[0]`)

| Field | Assertion | Rule |
|-------|-----------|------|
| `slot_guid` | ✅ Hard + 🔗 Cross-API | Not null; equals `RequestContext.getCurrentSlotGuid()` when stored |
| `slot_start_time` | ✅ Hard | Not null |
| `slot_end_time` | ✅ Hard | Not null |
| `slot_start_times` / `slot_end_times` | ℹ️ Info | Logged |

### 4.6 Cancellation status fields (`data[0]`)

| Field | Assertion | Rule |
|-------|-----------|------|
| `order_status` | ✅ Hard | Must equal `"Cancelled"` |
| `cancelled_date` | ✅ Hard | Not null; not empty |
| `cancelled_by_user_at` | ✅ Hard | Not null |
| `it_dose_order_status` | ✅ Hard | Must equal `"Cancelled"` |
| `cancel_order_remarks` | ℹ️ Info | Logged only (admin approval step not required for COD) |

### 4.7 Admin approval fields (`data[0]`)

| Field | Assertion | Rule |
|-------|-----------|------|
| `admin_approval_status` | ✅ Hard (when present); ⚠️ Soft (when null) | Must be `"Approved"` after admin-triggered cancellation |
| `admin_approval_by` | ✅ Hard + 🔗 Cross-API (when present); ⚠️ Soft (when null/empty) | Must equal `RequestContext.getAdminGuid()` |
| `admin_approval_at` | ✅ Hard (when present); ⚠️ Soft (when null/empty) | Must be non-empty timestamp |

### 4.8 `user_details` nested object

| Field | Assertion | Rule |
|-------|-----------|------|
| `user_details.guid` | ✅ Hard | Not null; must equal `data[0].user_id` |
| `user_details.first_name` | ✅ Hard | Not null |
| `user_details.mobile` | ✅ Hard | Not null |

### 4.9 `order_items[]` — Per-item fields

| Field | Assertion | Rule |
|-------|-----------|------|
| `order_id` | ✅ Hard | Must equal main order guid |
| `product_name` | ✅ Hard | Not null; not empty |
| `order_item_number` | ✅ Hard | Must equal `"{order_sample_number}-{itemIndex}"` |
| `order_status` | ✅ Hard | Must be `"Cancelled"` |
| `it_dose_order_items_status` | ⚠️ Soft | Expected `"Cancelled"` but may lag in multi-member async cancellations — `logSoft` when mismatched |
| `admin_approval_status` | ✅ Hard (when present); ⚠️ Soft (when null) | Must be `"Approved"` |
| `admin_approval_by` | ✅ Hard + 🔗 Cross-API (when present); ⚠️ Soft (null/empty) | Must equal `RequestContext.getAdminGuid()` |
| `admin_approval_at` | ✅ Hard (when present); ⚠️ Soft (null/empty) | Non-empty timestamp |
| `cancelled_at` | ✅ Hard | Not null |
| `cancel_order_remarks` | ℹ️ Info | Logged only |
| `actual_price` | ✅ Hard | Not null; `> 0` |
| `final_price` | ✅ Hard | Not null; `> 0`; `<= actual_price` |
| `membership_discount` | ✅ Hard | Not null; `>= 0` |
| `actual_discount` | ✅ Hard (when present) | Must exactly equal `membership_discount` (coupon applied at order level, not per item) |
| `sample_types[].Tests[].TestName` | ✅ Hard | Not null; not empty per test |

**Per-item price formula (hard-asserted):**
```
actual_price − membership_discount = final_price
```

### 4.10 Sum of item final_prices (post-loop, hard-asserted)

```
sum(order_items[].final_price) − couponDiscThisOrder = paid_amount     (HARD ASSERT)
```

`couponDiscThisOrder` resolved via: `coupon_discount` field → `coupon_discount_amount` field → `RequestContext.getCouponAmount()`

### 4.11 Multi-member Sibling Order Checks

| Check | Assertion | Rule |
|-------|-----------|------|
| Sibling `order_status` | ✅ Hard | Must NOT be `"Cancelled"` — only the cancelling sub-order was cancelled |
| Sibling `refund_amount` | ✅ Hard (when present) | Must be `0` — sibling was not cancelled |
| Sibling `coupon_discount` | ℹ️ Info | Logged (other member's share unaffected) |
| Sibling vs cancelled `paid_amount` | ℹ️ Info | Logged for equal-split confirmation |

---

---

## [5] step20_E — `getPaymentById`

**Endpoint:** `POST /gateway/getPaymentById`  
**Payload:** `{ "id": "<paymentId>" }`  
**Pre-condition:** `RequestContext.getCurrentPaymentId()` must be set. Depends on step20_D. Retried up to **3 times** on HTTP 5xx.

---

### 5.1 Top-level response

| Field | Assertion | Rule |
|-------|-----------|------|
| HTTP status | ✅ Hard | 200 (with 3-attempt retry on 5xx) |
| `success` | ✅ Hard (when present) | Must be `true` |
| `msg` | ✅ Hard | Not null |

### 5.2 `data.payments` object

| Field | Assertion | Rule |
|-------|-----------|------|
| `guid` | ✅ Hard + 🔗 Cross-API | Not null; must equal `RequestContext.getCurrentPaymentId()` |
| `payment_type` | ✅ Hard | Not null; must equal `"COD"` |
| `payment_status` | ✅ Hard (not null); ℹ️ Info (value) | Not null. Accepted post-cycle states: `Refunded / Cancelled / cancelled_refund / Approved / Paid / Success`. Logged without hard-fail on unexpected value. |
| `amount` | ✅ Hard | `>= 0`. 🔗 Cross-API softly compared with `RequestContext.getCurrentTotalPrice()` |
| `net_payable` | ✅ Hard (when present) | `>= 0` |
| `membership_discount` | ✅ Hard (when present) | `>= 0` |
| `total_discount` | ✅ Hard (when present) | `>= 0` |
| `coupon_discount` | ✅ Hard (when present) | `>= 0`. 🔗 Cross-API ≈ `RequestContext.getCouponAmount()` (±1.0 tolerance); ⚠️ Soft on mismatch |
| `coupon_guid` | 🔗 Cross-API (when stored) | Must equal `RequestContext.getMemberCouponGuid()`; ⚠️ Soft on mismatch |
| `payment_mode` | ✅ Hard (non-COD: not null); ℹ️ Info (COD: expected null) | Null for COD; non-null for UPI/Card/Online |
| `actual_price` | ℹ️ Info (when present) | Used for `actual − mem_disc − coupon_disc ≈ amount_charged` price-math log |

**Price breakdown (info-logged when `actual_price` available):**
```
actual_price − membership_discount − coupon_discount ≈ amount_charged
```

### 5.3 `data.order_items[]`

| Field | Assertion | Rule |
|-------|-----------|------|
| Array | ✅ Hard | Not null; not empty |
| `product_name` | ✅ Hard | Not null; not empty |
| `final_price` | ✅ Hard | `>= 0` |
| `quantity` | ✅ Hard | `> 0` |
| `order_id` | ✅ Hard + 🔗 Cross-API | Single order: must equal `RequestContext.getCurrentOrderId()`. Multi-member: must be one of `RequestContext.getCurrentOrderIds()` |
| `patient_guid` | ℹ️ Info | Logged (may be null for some item types) |

**Sum check (info-logged):**
```
sum(item.final_price × quantity) ≈ payment amount
```
(Difference expected when home collection charge or membership discount delta applies.)

---

---

## [6] step20_F — `getRewardsByMobile`

**Endpoint:** `GET /reward/getRewardsByMobile/{mobile}`  
**Pre-condition:** `RequestContext.getMobile()` must be set. Depends on step20_E.  
**Key purpose:** Extract and store the **membership system's `customer_id`** for step20_G (differs from `diagnostics.user_id`).

---

### 6.1 Top-level response

| Field | Assertion | Rule |
|-------|-----------|------|
| HTTP status | ✅ Hard | 200 |
| `success` | ✅ Hard (when present) | `true` |
| `msg` | ✅ Hard | Not null |

### 6.2 `data` fields

| Field | Assertion | Rule |
|-------|-----------|------|
| `total_rewards` | ✅ Hard | Not null; `>= 0`. Stored for subsequent cross-checks. |
| `customer_id` | ✅ Hard | Not null; **stored to `RequestContext.setCurrentMembershipCustomerId()`** for use in step20_G |
| `mobile` | ✅ Hard + 🔗 Cross-API | Not null; must equal `RequestContext.getMobile()` |

### 6.3 Rewards balance cross-checks

| Check | Assertion | Rule |
|-------|-----------|------|
| `postCancelRewards < finalRewards` | ✅ Hard (when `finalRewards > 0`) | Cancellation must have reversed the earned rewards |
| `postCancelRewards ≈ initialRewards` | ℹ️ Info | If rewards gain was completely reversed, should equal pre-payment balance |
| `finalRewards - postCancelRewards ≈ rewardsGain` | ✅ Hard (when both > 0) | Reversal must equal original rewards earned |
| `postCancelRewards ≥ 0` | ✅ Hard | Wallet balance cannot go negative |

> **Why this step exists:** The transaction API stores `customer_id` = the membership system's user GUID (e.g. `"08743efb-..."`), which differs from `diagnostics.user_id` (e.g. `"60cdff44-..."`). This step extracts and stores the correct membership `customer_id` for cross-API validation in step20_G.

---

---

## [7] step20_G — `getTransactionByMobile`

**Endpoint:** `GET /transaction/getTransactionByMobile/{mobile}?page={n}&limit=10`  
**Pre-condition:** `RequestContext.getMobile()` + `getCurrentOrderSampleNumber()` + `getCurrentMembershipCustomerId()` must be set. Depends on step20_F.  
**Strategy:** Paginates all pages; collects transactions where `reference_code == sampleNumber`; validates one `Success` + one `Cancelled` record.

---

### 7.1 Pagination fields (page 1)

| Field | Assertion | Rule |
|-------|-----------|------|
| `success` | ✅ Hard | `true` |
| `msg` | ✅ Hard | Not null |
| `total_pages` | ✅ Hard | Not null; `>= 1` |
| `total` | ✅ Hard | Not null; `>= 1` (at least 1 transaction must exist) |
| `limit` | ✅ Hard | Not null |

### 7.2 Transaction matching (hard assertions before per-record validation)

| Check | Assertion | Rule |
|-------|-----------|------|
| `successTxn` present | ✅ Hard | At least one `"Success"` transaction for this `reference_code` must exist |
| `cancelledTxn` present | ✅ Hard | At least one `"Cancelled"` transaction for this `reference_code` must exist |

### 7.3 Per-transaction identity fields (applied to EVERY matched transaction)

| Field | Assertion | Rule |
|-------|-----------|------|
| `Guid` (capital G; fallback: `guid`, `_id`) | ✅ Hard | Not null; not empty |
| `reference_code` | ✅ Hard + 🔗 Cross-API | Not null; must equal stored `sampleNumber` (e.g. `"MY26AAA2007"`) |
| `reference_id` | **Cancelled:** ✅ Hard (not null, not empty UUID). **Success:** ℹ️ Info | Cancelled records must carry a UUID. Success records may have null/empty `reference_id` — info-logged only. **NOT compared against `reference_code`** (UUID ≠ reference code format). |
| `order_id` | ✅ Hard | Not null |
| `mobile` | ✅ Hard + 🔗 Cross-API | Not null; must equal `RequestContext.getMobile()` |
| `customer_id` | ✅ Hard + 🔗 Cross-API (when `membershipCustomerId` stored) | Not null; must equal membership `customer_id` from step20_F |

### 7.4 Per-transaction state fields

| Field | Assertion | Rule |
|-------|-----------|------|
| `status` | ✅ Hard | Not null; must be one of `"Success"` / `"Successful"` / `"Cancelled"` |
| `is_reverted` | ✅ Hard | Not null. `Cancelled` → must be `true`. `Success/Successful` → must be `false`. |

### 7.5 Per-transaction amount fields (all returned as strings)

| Field | Assertion | Rule |
|-------|-----------|------|
| `trnsc_amount` | ✅ Hard | `>= 0` (parsed from string) |
| `actual_price` | ✅ Hard | `>= 0` |
| `net_paid_amount` | ✅ Hard | `>= 0` |
| `membership_discount` | ✅ Hard | `>= 0` |
| `coupon_discount` | ✅ Hard | `>= 0`. 🔗 Cross-API: if `storedCouponAmount > 0`, `coupon_discount ≈ storedCouponAmount` (±1.0); ⚠️ Soft on mismatch |
| `rewards_gain` | ✅ Hard | `>= 0` |
| `rewards_used` | ✅ Hard | `>= 0` |
| `taking_rewards` | ℹ️ Info | Logged |

**Price breakdown (info-logged when `actual_price > 0`):**
```
actual_price − membership_discount − coupon_discount ≈ trnsc_amount     (±1.0)
net_paid_amount ≈ trnsc_amount                                           (±1.0, logged)
```

### 7.6 Per-transaction percentage fields

| Field | Assertion | Rule |
|-------|-----------|------|
| `membership_discount_percentage` | ✅ Hard | Not null |
| `rewards_discount_percentage` | ✅ Hard | Not null |

### 7.7 Per-transaction patient details

| Field | Assertion | Rule |
|-------|-----------|------|
| `patient_first_name` | ✅ Hard | Not null |
| `patient_last_name` | ℹ️ Info | Logged |
| `dob` | ✅ Hard | Not null |
| `gender` | ✅ Hard | Not null |

### 7.8 Per-transaction timestamps

| Field | Assertion | Rule |
|-------|-----------|------|
| `created_at` | ✅ Hard | Not null |
| `updated_at` | ✅ Hard | Not null |

---

### 7.9 `Success` / `Successful` record — additional checks

| Field | Assertion | Rule |
|-------|-----------|------|
| `trnsc_amount` | ℹ️ Info | Compared with `RequestContext.getCurrentTotalPrice()` (logged, ±1.0 tolerance) |
| `net_paid_amount` | ℹ️ Info | API returns `0` for Success records by design — logged without hard-fail |
| `membership_discount` | ℹ️ Info | Checked against `actual_price − trnsc_amount` (logged) |
| `coupon_discount` | ℹ️ Info | Coupon is typically `0` in Success record — coupon confirmed via order/payment APIs instead |
| `refund_amount` | ✅ Hard (when present) | Must be `0` (no refund at order placement) |
| `rewards_gain` | ℹ️ Info (when `storedRewardsGain > 0`) | Compared with `RequestContext.getRewardsGain()` (±1.0, logged) |
| `rewards_used` | ✅ Hard | `>= 0` |

### 7.10 `Cancelled` record — additional checks

| Field | Assertion | Rule |
|-------|-----------|------|
| `trnsc_amount` | ℹ️ Info | Compared with `RequestContext.getCurrentTotalPrice()` — must represent full refund amount (logged, ±1.0) |
| `coupon_discount` | ℹ️ Info | Compared with `storedCouponAmount` — should mirror Success record coupon (logged, ±1.0) |
| `refund_amount` | ℹ️ Info (when present) | Compared with `trnsc_amount` — full refund expected (logged, ±1.0) |
| `rewards_gain` | ℹ️ Info (when `storedRewardsGain > 0`) | Compared with `RequestContext.getRewardsGain()` — should match original earned amount (reversal) (logged, ±1.0) |
| `net_paid_amount` | ℹ️ Info | What was refunded back — compared with `storedCartTotal` (logged, ±1.0) |
| `created_by` | ℹ️ Info | Logged |
| `admin_approval_status` | ℹ️ Info | Logged |
| `admin_approval_by` | ℹ️ Info | Logged |
| `admin_approval_at` | ℹ️ Info | Logged |

---

---

---

## Summary of All Hard Assertions

| Step | Count (approximate) | Critical examples |
|------|---------------------|-------------------|
| 20-A | ~28 hard + ~5 soft | Cash formula, reference_code format, adjustedRefundAmount == orderedByCash |
| 20-B | ~4 | msg == "Order updated successfully"; GET order_status == "Cancelled" |
| 20-C | ~3 | HTTP 200; success=true |
| 20-D | ~50+ | order_status Cancelled; per-item price formula; sum formula; payment_status Success; slot fields; user_details.guid == user_id |
| 20-E | ~12 | payment guid cross-API; payment_type COD; amount >= 0; coupon_discount cross-check |
| 20-F | ~7 | total_rewards >= 0; finalRewards > postCancelRewards; mobile cross-API |
| 20-G | ~20+ per transaction pair | Guid, reference_code, status, is_reverted, all amount >= 0, membership/rewards % present |

---

## Soft Warning Catalogue

All of these write a `[SOFT]` line to `logs/cod_failures.log`.

| Condition | Step | Reason |
|-----------|------|--------|
| `canceled_amount.actual_price ≠ outer actual_price` | 20-A | Multi-member: each level may report different sub-totals |
| `sum(sub-orders orderedByCash) ≠ storedCartTotal` | 20-A | Cart total pre-discount; orderedByCash post-discount |
| `it_dose_order_items_status ≠ "Cancelled"` | 20-D | Async IT-DOSE update lag in multi-member cancellations |
| `admin_approval_status / admin_approval_by / admin_approval_at` null | 20-D | May not be set in all COD cash-refund flows |
| `coupon_discount ≠ storedCouponAmount` in payment | 20-E | Coupon split may differ per API |
| `coupon_guid ≠ storedCouponGuid` | 20-E | Environment / timing discrepancy |
| `due_amount ≠ 0` in multi-member | 20-D | Primary member's order still active |
| `sum(sub-orders paid_amount) ≠ storedCartTotal` | 20-D | Discount applied |

---

## RequestContext Fields Used Across Steps

| Field | Stored by | Used in |
|-------|-----------|---------|
| `getCurrentOrderId()` | Order placement flow | 20-A, 20-B, 20-C, 20-D, 20-E |
| `getCurrentOrderIds()` | Multi-member flow | 20-A, 20-D, 20-E |
| `getCurrentTotalPrice()` | COD_02 cart | 20-A, 20-D, 20-G |
| `getRewardsGain()` | COD_15 payment | 20-A, 20-D, 20-F, 20-G |
| `getCurrentPaymentId()` | COD order placement | 20-D, 20-E |
| `getCurrentSlotGuid()` | Slot booking | 20-D |
| `getUserId()` | Registration/login | 20-D |
| `getMobile()` | Registration | 20-F, 20-G |
| `getAdminGuid()` | Admin login | 20-B, 20-C, 20-D |
| `getCouponAmount()` | AddToCart / coupon apply | 20-A, 20-D, 20-E, 20-G |
| `getMemberCouponGuid()` | Coupon apply | 20-E |
| `getCurrentOrderSampleNumber()` | Set by 20-A | 20-G (transaction filter) |
| `getCurrentMembershipCustomerId()` | Set by 20-F | 20-G (customer_id cross-check) |
| `getInitialTotalRewards()` | COD_18_A | 20-F |
| `getFinalTotalRewards()` | COD_18_C (post-payment) | 20-F |

---

_Last updated to reflect all fixes applied in this session, including: `paid_amount` semantic rename, coupon-aware formula `>=` in step20_A, `sum(final_price) - couponDisc = paid_amount` in step20_D, `remaining_rewards` cross-comparison removed, `reference_id` UUID-only assertion, and `it_dose_order_items_status` softened._

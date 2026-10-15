# Phlebo Flow - Validation Log

Generated: 2026-10-13 15:11:25

| Item | Value |
|---|---|
| Suite | test-suites/testng_non_member_cod_modular.xml |
| Phlebo notification service | https://staging-api-phlebo-notification.yodadiagnostics.com |
| Diagnostics service | https://staging-api-diagnostics.yodaprojects.com |
| Total checks | 509 |
| Passed | 488 |
| Failed (test failures) | 0 |
| Backend defects | 21 |
| Distinct issues (Jira candidates) | 12 |

FAIL = a check did not hold. DEFECT = backend returned wrong / stale / null data (reported as SKIPPED in the run unless `-Dphlebo.failOnDefects=true`).

## Results by step

| Step | Endpoint under test | Passed | Failed | Defects |
|---|---|---|---|---|
| PHLEBO_MGMT_01 | `POST https://staging-api-phlebo-notification.yodadiagnostics.com/api/v1/phlebo/login` | 21 | 0 | 0 |
| PHLEBO_MGMT_02 | `POST https://staging-api-diagnostics.yodaprojects.com/auth/login` | 13 | 0 | 0 |
| PHLEBO_MGMT_03 | `POST https://staging-api-phlebo-notification.yodadiagnostics.com/api/v1/phlebo/assign-shifts` | 32 | 0 | 0 |
| PHLEBO_MGMT_04 | `GET https://staging-api-phlebo-notification.yodadiagnostics.com/api/v1/phlebo/{phlebo_guid}/shifts` | 37 | 0 | 1 |
| PHLEBO_MGMT_05 | `POST https://staging-api-phlebo-notification.yodadiagnostics.com/api/v1/phlebo/shift/clock-in` | 3 | 0 | 0 |
| PHLEBO_MGMT_06 | `POST https://staging-api-diagnostics.yodaprojects.com/order_tracking/assignOrder` | 93 | 0 | 4 |
| PHLEBO_MGMT_07 | `POST https://staging-api-diagnostics.yodaprojects.com/order_tracking/updateOrderTracking` | 63 | 0 | 3 |
| PHLEBO_MGMT_08 | `POST https://staging-api-phlebo-notification.yodadiagnostics.com/api/v1/phlebo/upload` | 7 | 0 | 0 |
| PHLEBO_MGMT_09 | `POST https://staging-api-diagnostics.yodaprojects.com/order_tracking/verifyotp` | 48 | 0 | 2 |
| PHLEBO_MGMT_10 | `GET https://staging-api-diagnostics.yodaprojects.com/order/getOrderById/{order_id}` | 80 | 0 | 2 |
| PHLEBO_MGMT_11 | `POST http://43.204.199.216/yoda_uat_8.0/API/ChangeBarcode/ChangeBarCodedata` | 7 | 0 | 0 |
| PHLEBO_MGMT_12 | `POST https://staging-api-diagnostics.yodaprojects.com/order_tracking/updateOrderTracking` | 84 | 0 | 9 |

## Issue index

| ID | Type | Suggested priority | Check | Steps affected |
|---|---|---|---|---|
| ISSUE-01 | DEFECT | Medium | type has the same representation in login and profile | PHLEBO_MGMT_04 |
| ISSUE-02 | DEFECT | Low | timeline: order.updated_at refreshed by phlebo assignment | PHLEBO_MGMT_06, 07, 09, 10, 12 |
| ISSUE-03 | DEFECT | Medium | null: top-level payment_status mirrors payment.payment_status | PHLEBO_MGMT_06, 07, 09, 10, 12 |
| ISSUE-04 | DEFECT | High | assignment: phlebo zone == order zone | PHLEBO_MGMT_06 |
| ISSUE-05 | DEFECT | High | assignment: order slot inside phlebo shift 08:00-12:00 | PHLEBO_MGMT_06 |
| ISSUE-06 | DEFECT | Medium | tracking: updated_at refreshed on each status change | PHLEBO_MGMT_07, 12 |
| ISSUE-07 | DEFECT | Medium | status: display_order_status reflects collection | PHLEBO_MGMT_12 |
| ISSUE-08 | DEFECT | Medium | null: items sample_collected_at / phlebotomist_assigned_at stamped | PHLEBO_MGMT_12 |
| ISSUE-09 | DEFECT | Medium | null: sample_collection_by recorded | PHLEBO_MGMT_12 |
| ISSUE-10 | DEFECT | Medium | null: reached_home_at recorded | PHLEBO_MGMT_12 |
| ISSUE-11 | DEFECT | Low | timeline: order.updated_at refreshed by sample collection | PHLEBO_MGMT_12 |
| ISSUE-12 | DEFECT | Low | timeline: sample_collection_date not before the samples_collected event | PHLEBO_MGMT_12 |

## Issue details

### ISSUE-01 - DEFECT: type has the same representation in login and profile

| Jira field | Value |
|---|---|
| Summary | [Phlebo flow] type has the same representation in login and profile (GET /api/v1/phlebo/{phlebo_guid}/shifts) |
| Issue type | Bug |
| Suggested priority | Medium |
| Labels | api-automation, phlebo-flow, backend-defect |
| Steps affected | PHLEBO_MGMT_04 |
| Automated test | `PhlebioFlowValidationTest.PHLEBO_VALIDATION` - `[PHLEBO_MGMT_04 profile (GET phlebo by guid) vs login] type has the same representation in login and profile` |

**Occurrences (endpoint, expected, actual)**

| Step | Endpoint | Expected | Actual |
|---|---|---|---|
| PHLEBO_MGMT_04 profile (GET phlebo by guid) vs login | `GET https://staging-api-phlebo-notification.yodadiagnostics.com/api/v1/phlebo/{phlebo_guid}/shifts`<br>cross-check: `GET https://staging-api-phlebo-notification.yodadiagnostics.com/api/v1/phlebo/{guid}` | `Condition holds: type has the same representation in login and profile` | `login={phlebo} profile=[phlebo]` |

**Description**

The check `type has the same representation in login and profile` found inconsistent backend data. Detail: login={phlebo} profile=[phlebo]

**Steps to reproduce**

1. Create a non-member COD order (login, add to cart, address and slot, place order).
2. Run the phlebo flow in order, PHLEBO_MGMT_01 up to PHLEBO_MGMT_04 (suite `testng_non_member_cod_modular.xml`).
3. Call the endpoint(s) above and compare the response field with the expected value.

**Test data of the run**

- Order id: `[2b3e1421-b7d6-4fff-a7e2-660856fdec49]`
- Order tracking id: `[2db61a62-e7fd-43f0-a9d1-c3fbc2074db2]`
- Phlebo guid: `1f289ec9-65c6-43e3-b396-fe5da33dbae3`
- Visit number: `MYD24222`

---

### ISSUE-02 - DEFECT: timeline: order.updated_at refreshed by phlebo assignment

| Jira field | Value |
|---|---|
| Summary | [Phlebo flow] timeline: order.updated_at refreshed by phlebo assignment (POST /order_tracking/assignOrder) |
| Issue type | Bug |
| Suggested priority | Low |
| Labels | api-automation, phlebo-flow, backend-defect |
| Steps affected | PHLEBO_MGMT_06, 07, 09, 10, 12 |
| Automated test | `PhlebioFlowValidationTest.PHLEBO_VALIDATION` - `[PHLEBO_MGMT_06 assignOrder] timeline: order.updated_at refreshed by phlebo assignment` |

**Occurrences (endpoint, expected, actual)**

| Step | Endpoint | Expected | Actual |
|---|---|---|---|
| PHLEBO_MGMT_06 assignOrder | `POST https://staging-api-diagnostics.yodaprojects.com/order_tracking/assignOrder`<br>cross-check: `GET https://staging-api-diagnostics.yodaprojects.com/order/getOrderById/{order_id}` | `Condition holds: timeline: order.updated_at refreshed by phlebo assignment` | `updated_at 2026-10-13T09:41:04.223Z is older than phlebo_assigned_at 2026-10-13T09:41:04.264Z` |
| PHLEBO_MGMT_07 updateOrderTracking(inprogress) | `POST https://staging-api-diagnostics.yodaprojects.com/order_tracking/updateOrderTracking`<br>cross-check: `GET https://staging-api-diagnostics.yodaprojects.com/order/getOrderById/{order_id}` | `Condition holds: timeline: order.updated_at refreshed by phlebo assignment` | `updated_at 2026-10-13T09:41:04.223Z is older than phlebo_assigned_at 2026-10-13T09:41:04.264Z` |
| PHLEBO_MGMT_09 verifyotp [2b3e1421-b7d6-4fff-a7e2-660856fdec49] | `POST https://staging-api-diagnostics.yodaprojects.com/order_tracking/verifyotp`<br>cross-check: `GET https://staging-api-diagnostics.yodaprojects.com/order/getOrderById/{order_id}` | `Condition holds: timeline: order.updated_at refreshed by phlebo assignment` | `updated_at 2026-10-13T09:41:04.223Z is older than phlebo_assigned_at 2026-10-13T09:41:04.264Z` |
| PHLEBO_MGMT_10 getOrderById (sample details) | `GET https://staging-api-diagnostics.yodaprojects.com/order/getOrderById/{order_id}` | `Condition holds: timeline: order.updated_at refreshed by phlebo assignment` | `updated_at 2026-10-13T09:41:04.223Z is older than phlebo_assigned_at 2026-10-13T09:41:04.264Z` |
| PHLEBO_MGMT_12 updateOrderTracking(samples_collected) | `POST https://staging-api-diagnostics.yodaprojects.com/order_tracking/updateOrderTracking`<br>cross-check: `GET https://staging-api-diagnostics.yodaprojects.com/order/getOrderById/{order_id}` | `Condition holds: timeline: order.updated_at refreshed by phlebo assignment` | `updated_at 2026-10-13T09:41:04.223Z is older than phlebo_assigned_at 2026-10-13T09:41:04.264Z` |

**Description**

The check `timeline: order.updated_at refreshed by phlebo assignment` found inconsistent backend data. Detail: updated_at 2026-10-13T09:41:04.223Z is older than phlebo_assigned_at 2026-10-13T09:41:04.264Z

**Steps to reproduce**

1. Create a non-member COD order (login, add to cart, address and slot, place order).
2. Run the phlebo flow in order, PHLEBO_MGMT_01 up to PHLEBO_MGMT_06 (suite `testng_non_member_cod_modular.xml`).
3. Call the endpoint(s) above and compare the response field with the expected value.

**Test data of the run**

- Order id: `[2b3e1421-b7d6-4fff-a7e2-660856fdec49]`
- Order tracking id: `[2db61a62-e7fd-43f0-a9d1-c3fbc2074db2]`
- Phlebo guid: `1f289ec9-65c6-43e3-b396-fe5da33dbae3`
- Visit number: `MYD24222`

---

### ISSUE-03 - DEFECT: null: top-level payment_status mirrors payment.payment_status

| Jira field | Value |
|---|---|
| Summary | [Phlebo flow] null: top-level payment_status mirrors payment.payment_status (POST /order_tracking/assignOrder) |
| Issue type | Bug |
| Suggested priority | Medium |
| Labels | api-automation, phlebo-flow, backend-defect |
| Steps affected | PHLEBO_MGMT_06, 07, 09, 10, 12 |
| Automated test | `PhlebioFlowValidationTest.PHLEBO_VALIDATION` - `[PHLEBO_MGMT_06 assignOrder] null: top-level payment_status mirrors payment.payment_status` |

**Occurrences (endpoint, expected, actual)**

| Step | Endpoint | Expected | Actual |
|---|---|---|---|
| PHLEBO_MGMT_06 assignOrder | `POST https://staging-api-diagnostics.yodaprojects.com/order_tracking/assignOrder`<br>cross-check: `GET https://staging-api-diagnostics.yodaprojects.com/order/getOrderById/{order_id}` | `Condition holds: null: top-level payment_status mirrors payment.payment_status` | `payment_status is null while payment.payment_status is [Pending]` |
| PHLEBO_MGMT_07 updateOrderTracking(inprogress) | `POST https://staging-api-diagnostics.yodaprojects.com/order_tracking/updateOrderTracking`<br>cross-check: `GET https://staging-api-diagnostics.yodaprojects.com/order/getOrderById/{order_id}` | `Condition holds: null: top-level payment_status mirrors payment.payment_status` | `payment_status is null while payment.payment_status is [Pending]` |
| PHLEBO_MGMT_09 verifyotp [2b3e1421-b7d6-4fff-a7e2-660856fdec49] | `POST https://staging-api-diagnostics.yodaprojects.com/order_tracking/verifyotp`<br>cross-check: `GET https://staging-api-diagnostics.yodaprojects.com/order/getOrderById/{order_id}` | `Condition holds: null: top-level payment_status mirrors payment.payment_status` | `payment_status is null while payment.payment_status is [Pending]` |
| PHLEBO_MGMT_10 getOrderById (sample details) | `GET https://staging-api-diagnostics.yodaprojects.com/order/getOrderById/{order_id}` | `Condition holds: null: top-level payment_status mirrors payment.payment_status` | `payment_status is null while payment.payment_status is [Pending]` |
| PHLEBO_MGMT_12 updateOrderTracking(samples_collected) | `POST https://staging-api-diagnostics.yodaprojects.com/order_tracking/updateOrderTracking`<br>cross-check: `GET https://staging-api-diagnostics.yodaprojects.com/order/getOrderById/{order_id}` | `Condition holds: null: top-level payment_status mirrors payment.payment_status` | `payment_status is null while payment.payment_status is [Pending]` |

**Description**

The check `null: top-level payment_status mirrors payment.payment_status` found inconsistent backend data. Detail: payment_status is null while payment.payment_status is [Pending]

**Steps to reproduce**

1. Create a non-member COD order (login, add to cart, address and slot, place order).
2. Run the phlebo flow in order, PHLEBO_MGMT_01 up to PHLEBO_MGMT_06 (suite `testng_non_member_cod_modular.xml`).
3. Call the endpoint(s) above and compare the response field with the expected value.

**Test data of the run**

- Order id: `[2b3e1421-b7d6-4fff-a7e2-660856fdec49]`
- Order tracking id: `[2db61a62-e7fd-43f0-a9d1-c3fbc2074db2]`
- Phlebo guid: `1f289ec9-65c6-43e3-b396-fe5da33dbae3`
- Visit number: `MYD24222`

---

### ISSUE-04 - DEFECT: assignment: phlebo zone == order zone

| Jira field | Value |
|---|---|
| Summary | [Phlebo flow] assignment: phlebo zone == order zone (POST /order_tracking/assignOrder) |
| Issue type | Bug |
| Suggested priority | High |
| Labels | api-automation, phlebo-flow, backend-defect |
| Steps affected | PHLEBO_MGMT_06 |
| Automated test | `PhlebioFlowValidationTest.PHLEBO_VALIDATION` - `[PHLEBO_MGMT_06 assignOrder] assignment: phlebo zone == order zone` |

**Occurrences (endpoint, expected, actual)**

| Step | Endpoint | Expected | Actual |
|---|---|---|---|
| PHLEBO_MGMT_06 assignOrder | `POST https://staging-api-diagnostics.yodaprojects.com/order_tracking/assignOrder`<br>cross-check: `GET https://staging-api-diagnostics.yodaprojects.com/order/getOrderById/{order_id}` | `Condition holds: assignment: phlebo zone == order zone` | `phlebo zoneId [6a5487f5bf5622a2bebd0ba3] differs from order zone [64870066842708a0d5ae6c77]` |

**Description**

The check `assignment: phlebo zone == order zone` found inconsistent backend data. Detail: phlebo zoneId [6a5487f5bf5622a2bebd0ba3] differs from order zone [64870066842708a0d5ae6c77]

**Steps to reproduce**

1. Create a non-member COD order (login, add to cart, address and slot, place order).
2. Run the phlebo flow in order, PHLEBO_MGMT_01 up to PHLEBO_MGMT_06 (suite `testng_non_member_cod_modular.xml`).
3. Call the endpoint(s) above and compare the response field with the expected value.

**Test data of the run**

- Order id: `[2b3e1421-b7d6-4fff-a7e2-660856fdec49]`
- Order tracking id: `[2db61a62-e7fd-43f0-a9d1-c3fbc2074db2]`
- Phlebo guid: `1f289ec9-65c6-43e3-b396-fe5da33dbae3`
- Visit number: `MYD24222`

---

### ISSUE-05 - DEFECT: assignment: order slot inside phlebo shift 08:00-12:00

| Jira field | Value |
|---|---|
| Summary | [Phlebo flow] assignment: order slot inside phlebo shift 08:00-12:00 (POST /order_tracking/assignOrder) |
| Issue type | Bug |
| Suggested priority | High |
| Labels | api-automation, phlebo-flow, backend-defect |
| Steps affected | PHLEBO_MGMT_06 |
| Automated test | `PhlebioFlowValidationTest.PHLEBO_VALIDATION` - `[PHLEBO_MGMT_06 assignOrder] assignment: order slot inside phlebo shift 08:00-12:00` |

**Occurrences (endpoint, expected, actual)**

| Step | Endpoint | Expected | Actual |
|---|---|---|---|
| PHLEBO_MGMT_06 assignOrder | `POST https://staging-api-diagnostics.yodaprojects.com/order_tracking/assignOrder`<br>cross-check: `GET https://staging-api-diagnostics.yodaprojects.com/order/getOrderById/{order_id}` | `Condition holds: assignment: order slot inside phlebo shift 08:00-12:00` | `slot 15:42-15:47 is outside the phlebo shift 08:00-12:00` |

**Description**

The check `assignment: order slot inside phlebo shift 08:00-12:00` found inconsistent backend data. Detail: slot 15:42-15:47 is outside the phlebo shift 08:00-12:00

**Steps to reproduce**

1. Create a non-member COD order (login, add to cart, address and slot, place order).
2. Run the phlebo flow in order, PHLEBO_MGMT_01 up to PHLEBO_MGMT_06 (suite `testng_non_member_cod_modular.xml`).
3. Call the endpoint(s) above and compare the response field with the expected value.

**Test data of the run**

- Order id: `[2b3e1421-b7d6-4fff-a7e2-660856fdec49]`
- Order tracking id: `[2db61a62-e7fd-43f0-a9d1-c3fbc2074db2]`
- Phlebo guid: `1f289ec9-65c6-43e3-b396-fe5da33dbae3`
- Visit number: `MYD24222`

---

### ISSUE-06 - DEFECT: tracking: updated_at refreshed on each status change

| Jira field | Value |
|---|---|
| Summary | [Phlebo flow] tracking: updated_at refreshed on each status change (POST /order_tracking/updateOrderTracking) |
| Issue type | Bug |
| Suggested priority | Medium |
| Labels | api-automation, phlebo-flow, backend-defect |
| Steps affected | PHLEBO_MGMT_07, 12 |
| Automated test | `PhlebioFlowValidationTest.PHLEBO_VALIDATION` - `[PHLEBO_MGMT_07 updateOrderTracking(inprogress)] tracking: updated_at refreshed on each status change` |

**Occurrences (endpoint, expected, actual)**

| Step | Endpoint | Expected | Actual |
|---|---|---|---|
| PHLEBO_MGMT_07 updateOrderTracking(inprogress) | `POST https://staging-api-diagnostics.yodaprojects.com/order_tracking/updateOrderTracking`<br>cross-check: `GET https://staging-api-diagnostics.yodaprojects.com/order/getOrderById/{order_id}` | `Condition holds: tracking: updated_at refreshed on each status change` | `updated_at 2026-10-13T09:41:04.279Z is older than the latest status change 2026-10-13T09:41:06.639Z` |
| PHLEBO_MGMT_12 updateOrderTracking(samples_collected) | `POST https://staging-api-diagnostics.yodaprojects.com/order_tracking/updateOrderTracking`<br>cross-check: `GET https://staging-api-diagnostics.yodaprojects.com/order/getOrderById/{order_id}` | `Condition holds: tracking: updated_at refreshed on each status change` | `updated_at 2026-10-13T09:41:04.279Z is older than the latest status change 2026-10-13T09:41:18.744Z` |

**Description**

The check `tracking: updated_at refreshed on each status change` found inconsistent backend data. Detail: updated_at 2026-10-13T09:41:04.279Z is older than the latest status change 2026-10-13T09:41:06.639Z

**Steps to reproduce**

1. Create a non-member COD order (login, add to cart, address and slot, place order).
2. Run the phlebo flow in order, PHLEBO_MGMT_01 up to PHLEBO_MGMT_07 (suite `testng_non_member_cod_modular.xml`).
3. Call the endpoint(s) above and compare the response field with the expected value.

**Test data of the run**

- Order id: `[2b3e1421-b7d6-4fff-a7e2-660856fdec49]`
- Order tracking id: `[2db61a62-e7fd-43f0-a9d1-c3fbc2074db2]`
- Phlebo guid: `1f289ec9-65c6-43e3-b396-fe5da33dbae3`
- Visit number: `MYD24222`

---

### ISSUE-07 - DEFECT: status: display_order_status reflects collection

| Jira field | Value |
|---|---|
| Summary | [Phlebo flow] status: display_order_status reflects collection (POST /order_tracking/updateOrderTracking) |
| Issue type | Bug |
| Suggested priority | Medium |
| Labels | api-automation, phlebo-flow, backend-defect |
| Steps affected | PHLEBO_MGMT_12 |
| Automated test | `PhlebioFlowValidationTest.PHLEBO_VALIDATION` - `[PHLEBO_MGMT_12 updateOrderTracking(samples_collected)] status: display_order_status reflects collection` |

**Occurrences (endpoint, expected, actual)**

| Step | Endpoint | Expected | Actual |
|---|---|---|---|
| PHLEBO_MGMT_12 updateOrderTracking(samples_collected) | `POST https://staging-api-diagnostics.yodaprojects.com/order_tracking/updateOrderTracking`<br>cross-check: `GET https://staging-api-diagnostics.yodaprojects.com/order/getOrderById/{order_id}` | `Condition holds: status: display_order_status reflects collection` | `display_order_status is still [Order booked] while order_status is [Sample Collected] and status [Open]` |

**Description**

The check `status: display_order_status reflects collection` found inconsistent backend data. Detail: display_order_status is still [Order booked] while order_status is [Sample Collected] and status [Open]

**Steps to reproduce**

1. Create a non-member COD order (login, add to cart, address and slot, place order).
2. Run the phlebo flow in order, PHLEBO_MGMT_01 up to PHLEBO_MGMT_12 (suite `testng_non_member_cod_modular.xml`).
3. Call the endpoint(s) above and compare the response field with the expected value.

**Test data of the run**

- Order id: `[2b3e1421-b7d6-4fff-a7e2-660856fdec49]`
- Order tracking id: `[2db61a62-e7fd-43f0-a9d1-c3fbc2074db2]`
- Phlebo guid: `1f289ec9-65c6-43e3-b396-fe5da33dbae3`
- Visit number: `MYD24222`

---

### ISSUE-08 - DEFECT: null: items sample_collected_at / phlebotomist_assigned_at stamped

| Jira field | Value |
|---|---|
| Summary | [Phlebo flow] null: items sample_collected_at / phlebotomist_assigned_at stamped (POST /order_tracking/updateOrderTracking) |
| Issue type | Bug |
| Suggested priority | Medium |
| Labels | api-automation, phlebo-flow, backend-defect |
| Steps affected | PHLEBO_MGMT_12 |
| Automated test | `PhlebioFlowValidationTest.PHLEBO_VALIDATION` - `[PHLEBO_MGMT_12 updateOrderTracking(samples_collected)] null: items sample_collected_at / phlebotomist_assigned_at stamped` |

**Occurrences (endpoint, expected, actual)**

| Step | Endpoint | Expected | Actual |
|---|---|---|---|
| PHLEBO_MGMT_12 updateOrderTracking(samples_collected) | `POST https://staging-api-diagnostics.yodaprojects.com/order_tracking/updateOrderTracking`<br>cross-check: `GET https://staging-api-diagnostics.yodaprojects.com/order/getOrderById/{order_id}` | `Condition holds: null: items sample_collected_at / phlebotomist_assigned_at stamped` | `null on items [MY26AAA7521-1, MY26AAA7521-2, MY26AAA7521-3, MY26AAA7521-4]` |

**Description**

The check `null: items sample_collected_at / phlebotomist_assigned_at stamped` found inconsistent backend data. Detail: null on items [MY26AAA7521-1, MY26AAA7521-2, MY26AAA7521-3, MY26AAA7521-4]

**Steps to reproduce**

1. Create a non-member COD order (login, add to cart, address and slot, place order).
2. Run the phlebo flow in order, PHLEBO_MGMT_01 up to PHLEBO_MGMT_12 (suite `testng_non_member_cod_modular.xml`).
3. Call the endpoint(s) above and compare the response field with the expected value.

**Test data of the run**

- Order id: `[2b3e1421-b7d6-4fff-a7e2-660856fdec49]`
- Order tracking id: `[2db61a62-e7fd-43f0-a9d1-c3fbc2074db2]`
- Phlebo guid: `1f289ec9-65c6-43e3-b396-fe5da33dbae3`
- Visit number: `MYD24222`

---

### ISSUE-09 - DEFECT: null: sample_collection_by recorded

| Jira field | Value |
|---|---|
| Summary | [Phlebo flow] null: sample_collection_by recorded (POST /order_tracking/updateOrderTracking) |
| Issue type | Bug |
| Suggested priority | Medium |
| Labels | api-automation, phlebo-flow, backend-defect |
| Steps affected | PHLEBO_MGMT_12 |
| Automated test | `PhlebioFlowValidationTest.PHLEBO_VALIDATION` - `[PHLEBO_MGMT_12 updateOrderTracking(samples_collected)] null: sample_collection_by recorded` |

**Occurrences (endpoint, expected, actual)**

| Step | Endpoint | Expected | Actual |
|---|---|---|---|
| PHLEBO_MGMT_12 updateOrderTracking(samples_collected) | `POST https://staging-api-diagnostics.yodaprojects.com/order_tracking/updateOrderTracking`<br>cross-check: `GET https://staging-api-diagnostics.yodaprojects.com/order/getOrderById/{order_id}` | `Condition holds: null: sample_collection_by recorded` | `sample_collection_by is null after collection` |

**Description**

The check `null: sample_collection_by recorded` found inconsistent backend data. Detail: sample_collection_by is null after collection

**Steps to reproduce**

1. Create a non-member COD order (login, add to cart, address and slot, place order).
2. Run the phlebo flow in order, PHLEBO_MGMT_01 up to PHLEBO_MGMT_12 (suite `testng_non_member_cod_modular.xml`).
3. Call the endpoint(s) above and compare the response field with the expected value.

**Test data of the run**

- Order id: `[2b3e1421-b7d6-4fff-a7e2-660856fdec49]`
- Order tracking id: `[2db61a62-e7fd-43f0-a9d1-c3fbc2074db2]`
- Phlebo guid: `1f289ec9-65c6-43e3-b396-fe5da33dbae3`
- Visit number: `MYD24222`

---

### ISSUE-10 - DEFECT: null: reached_home_at recorded

| Jira field | Value |
|---|---|
| Summary | [Phlebo flow] null: reached_home_at recorded (POST /order_tracking/updateOrderTracking) |
| Issue type | Bug |
| Suggested priority | Medium |
| Labels | api-automation, phlebo-flow, backend-defect |
| Steps affected | PHLEBO_MGMT_12 |
| Automated test | `PhlebioFlowValidationTest.PHLEBO_VALIDATION` - `[PHLEBO_MGMT_12 updateOrderTracking(samples_collected)] null: reached_home_at recorded` |

**Occurrences (endpoint, expected, actual)**

| Step | Endpoint | Expected | Actual |
|---|---|---|---|
| PHLEBO_MGMT_12 updateOrderTracking(samples_collected) | `POST https://staging-api-diagnostics.yodaprojects.com/order_tracking/updateOrderTracking`<br>cross-check: `GET https://staging-api-diagnostics.yodaprojects.com/order/getOrderById/{order_id}` | `Condition holds: null: reached_home_at recorded` | `reached_home_at is null although phlebo went inprogress and collected samples` |

**Description**

The check `null: reached_home_at recorded` found inconsistent backend data. Detail: reached_home_at is null although phlebo went inprogress and collected samples

**Steps to reproduce**

1. Create a non-member COD order (login, add to cart, address and slot, place order).
2. Run the phlebo flow in order, PHLEBO_MGMT_01 up to PHLEBO_MGMT_12 (suite `testng_non_member_cod_modular.xml`).
3. Call the endpoint(s) above and compare the response field with the expected value.

**Test data of the run**

- Order id: `[2b3e1421-b7d6-4fff-a7e2-660856fdec49]`
- Order tracking id: `[2db61a62-e7fd-43f0-a9d1-c3fbc2074db2]`
- Phlebo guid: `1f289ec9-65c6-43e3-b396-fe5da33dbae3`
- Visit number: `MYD24222`

---

### ISSUE-11 - DEFECT: timeline: order.updated_at refreshed by sample collection

| Jira field | Value |
|---|---|
| Summary | [Phlebo flow] timeline: order.updated_at refreshed by sample collection (POST /order_tracking/updateOrderTracking) |
| Issue type | Bug |
| Suggested priority | Low |
| Labels | api-automation, phlebo-flow, backend-defect |
| Steps affected | PHLEBO_MGMT_12 |
| Automated test | `PhlebioFlowValidationTest.PHLEBO_VALIDATION` - `[PHLEBO_MGMT_12 updateOrderTracking(samples_collected)] timeline: order.updated_at refreshed by sample collection` |

**Occurrences (endpoint, expected, actual)**

| Step | Endpoint | Expected | Actual |
|---|---|---|---|
| PHLEBO_MGMT_12 updateOrderTracking(samples_collected) | `POST https://staging-api-diagnostics.yodaprojects.com/order_tracking/updateOrderTracking`<br>cross-check: `GET https://staging-api-diagnostics.yodaprojects.com/order/getOrderById/{order_id}` | `Condition holds: timeline: order.updated_at refreshed by sample collection` | `updated_at 2026-10-13T09:41:04.223Z is older than the samples_collected tracking time 2026-10-13T09:41:18.744Z` |

**Description**

The check `timeline: order.updated_at refreshed by sample collection` found inconsistent backend data. Detail: updated_at 2026-10-13T09:41:04.223Z is older than the samples_collected tracking time 2026-10-13T09:41:18.744Z

**Steps to reproduce**

1. Create a non-member COD order (login, add to cart, address and slot, place order).
2. Run the phlebo flow in order, PHLEBO_MGMT_01 up to PHLEBO_MGMT_12 (suite `testng_non_member_cod_modular.xml`).
3. Call the endpoint(s) above and compare the response field with the expected value.

**Test data of the run**

- Order id: `[2b3e1421-b7d6-4fff-a7e2-660856fdec49]`
- Order tracking id: `[2db61a62-e7fd-43f0-a9d1-c3fbc2074db2]`
- Phlebo guid: `1f289ec9-65c6-43e3-b396-fe5da33dbae3`
- Visit number: `MYD24222`

---

### ISSUE-12 - DEFECT: timeline: sample_collection_date not before the samples_collected event

| Jira field | Value |
|---|---|
| Summary | [Phlebo flow] timeline: sample_collection_date not before the samples_collected event (POST /order_tracking/updateOrderTracking) |
| Issue type | Bug |
| Suggested priority | Low |
| Labels | api-automation, phlebo-flow, backend-defect |
| Steps affected | PHLEBO_MGMT_12 |
| Automated test | `PhlebioFlowValidationTest.PHLEBO_VALIDATION` - `[PHLEBO_MGMT_12 updateOrderTracking(samples_collected)] timeline: sample_collection_date not before the samples_collected event` |

**Occurrences (endpoint, expected, actual)**

| Step | Endpoint | Expected | Actual |
|---|---|---|---|
| PHLEBO_MGMT_12 updateOrderTracking(samples_collected) | `POST https://staging-api-diagnostics.yodaprojects.com/order_tracking/updateOrderTracking`<br>cross-check: `GET https://staging-api-diagnostics.yodaprojects.com/order/getOrderById/{order_id}` | `Condition holds: timeline: sample_collection_date not before the samples_collected event` | `sample_collection_date 2026-10-13T09:41:08.038Z precedes samples_collected time 2026-10-13T09:41:18.744Z` |

**Description**

The check `timeline: sample_collection_date not before the samples_collected event` found inconsistent backend data. Detail: sample_collection_date 2026-10-13T09:41:08.038Z precedes samples_collected time 2026-10-13T09:41:18.744Z

**Steps to reproduce**

1. Create a non-member COD order (login, add to cart, address and slot, place order).
2. Run the phlebo flow in order, PHLEBO_MGMT_01 up to PHLEBO_MGMT_12 (suite `testng_non_member_cod_modular.xml`).
3. Call the endpoint(s) above and compare the response field with the expected value.

**Test data of the run**

- Order id: `[2b3e1421-b7d6-4fff-a7e2-660856fdec49]`
- Order tracking id: `[2db61a62-e7fd-43f0-a9d1-c3fbc2074db2]`
- Phlebo guid: `1f289ec9-65c6-43e3-b396-fe5da33dbae3`
- Visit number: `MYD24222`

---


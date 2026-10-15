# Phlebo Flow - Issues Report

Source: suite log `run_val9.txt` (`test-suites/testng_non_member_cod_modular.xml`, `PhlebioManagementTest` + `PhlebioFlowValidationTest`).

Result: 536 tests run, 0 failures, 21 skipped (backend defects), grouped into 12 distinct issues.

`payment_type` is only required after payment is approved (Online / Cash is known only then), so it is no longer reported before approval.

Backend defects are printed as `[DEFECT]` and are skipped (not failed) unless `-Dphlebo.failOnDefects=true` is set.

The Jira-ready, auto-generated log (expected / actual per occurrence, test data, repro steps) is `docs/validation-logs/Phlebo_Validation_Log_latest.md`.

### Endpoint key

| Alias | Base URL |
|---|---|
| `{PHLEBO}` | `https://staging-api-phlebo-notification.yodadiagnostics.com` |
| `{DIAG}` | `https://staging-api-diagnostics.yodaprojects.com` |
| `{UAT-LIS}` | `http://43.204.199.216/yoda_uat_8.0` |

| Step | Primary endpoint | Cross-checked with |
|---|---|---|
| 01 | `POST {PHLEBO}/api/v1/phlebo/login` | - |
| 02 | `POST {DIAG}/auth/login` | - |
| 03 | `POST {PHLEBO}/api/v1/phlebo/assign-shifts` | `GET {PHLEBO}/api/v1/phlebo/{phlebo_guid}/shifts` |
| 04 | `GET {PHLEBO}/api/v1/phlebo/{phlebo_guid}/shifts` | `GET {PHLEBO}/api/v1/phlebo/{guid}` |
| 05 | `POST {PHLEBO}/api/v1/phlebo/shift/clock-in` | - |
| 06 | `POST {DIAG}/order_tracking/assignOrder` | `GET {DIAG}/order/getOrderById/{order_id}` |
| 07 | `POST {DIAG}/order_tracking/updateOrderTracking` (inprogress) | `GET {DIAG}/order/getOrderById/{order_id}` |
| 08 | `POST {PHLEBO}/api/v1/phlebo/upload` | - |
| 09 | `POST {DIAG}/order_tracking/verifyotp` | `GET {DIAG}/order/getOrderById/{order_id}` |
| 10 | `GET {DIAG}/order/getOrderById/{order_id}` | - |
| 11 | `POST {UAT-LIS}/API/ChangeBarcode/ChangeBarCodedata` | `GET {DIAG}/order/getOrderById/{order_id}` |
| 12 | `POST {DIAG}/order_tracking/updateOrderTracking` (samples_collected) | `GET {DIAG}/order/getOrderById/{order_id}` |

---

## 1. Test failures (hard)

| Step | Endpoint | Problem | Cause | Status |
|---|---|---|---|---|
| PHLEBO_MGMT_12 (samples_collected) | `POST {DIAG}/order_tracking/updateOrderTracking`; read-back `GET {DIAG}/order/getOrderById/{order_id}` | `LIS: sample_status present -> sample_status is missing/empty` (seen in `run_val5`) | Test-side bug. `sample_status` is returned as an array of objects (`[{LabNo, Test[]}]`), but the check read it as a single object. | Fixed in `PhlebioIntegrityValidator.checkLis`. Confirmed in `run_val8`: 0 failures. |

---

## 2. Backend defects

### 2.1 Wrong or inconsistent status

| Step | Endpoint | Issue |
|---|---|---|
| 12 | `POST {DIAG}/order_tracking/updateOrderTracking`; read-back `GET {DIAG}/order/getOrderById/{order_id}` | `display_order_status` is still "Order booked" and `status` is "Open", while `order_status` is "Sample Collected". |

### 2.2 Null or never-filled fields

| Step | Endpoint | Field | Issue |
|---|---|---|---|
| 12 | `POST {DIAG}/order_tracking/updateOrderTracking`; read-back `GET {DIAG}/order/getOrderById/{order_id}` | Items' `sample_collected_at` and `phlebotomist_assigned_at` | null on all 4 items |
| 12 | same as above | `sample_collection_by` | null after collection |
| 12 | same as above | `reached_home_at` | null, although the phlebo went in progress and collected samples |
| 06, 07, 09, 10, 12 | the step endpoints above (06 assignOrder, 07 and 12 updateOrderTracking, 09 verifyotp, 10 getOrderById) | top-level `payment_status` | null, while `payment.payment_status` is "Success" |

### 2.3 Stale or wrong timestamps

| Step | Endpoint | Issue |
|---|---|---|
| 06, 07, 09, 10, 12 | `POST {DIAG}/order_tracking/assignOrder` (06), `POST {DIAG}/order_tracking/updateOrderTracking` (07, 12), `POST {DIAG}/order_tracking/verifyotp` (09), `GET {DIAG}/order/getOrderById/{order_id}` (10) | Order `updated_at` is older than `phlebo_assigned_at` (not refreshed by the assignment). |
| 07, 12 | `POST {DIAG}/order_tracking/updateOrderTracking`; read-back `GET {DIAG}/order/getOrderById/{order_id}` | Tracking row `updated_at` is not refreshed after status changes. |
| 12 | `POST {DIAG}/order_tracking/updateOrderTracking`; read-back `GET {DIAG}/order/getOrderById/{order_id}` | Order `updated_at` is not refreshed by sample collection. |
| 12 | same as above | `sample_collection_date` (set at OTP verify) is earlier than the `samples_collected` event time. |

### 2.4 Wrong assignment accepted (step 06)

| Endpoint | Issue | Detail |
|---|---|---|
| `POST {DIAG}/order_tracking/assignOrder`; read-back `GET {DIAG}/order/getOrderById/{order_id}` and `GET {PHLEBO}/api/v1/phlebo/{guid}` | Zone mismatch | Phlebo zone differs from order zone, yet the assignment is accepted. |
| same as above; shift from `GET {PHLEBO}/api/v1/phlebo/{phlebo_guid}/shifts` | Slot outside shift | Order slot is outside the phlebo shift 08:00-12:00, yet the assignment is accepted. |

### 2.5 Format mismatch (step 04)

| Endpoint | Issue | Detail |
|---|---|---|
| Login `POST {PHLEBO}/api/v1/phlebo/login` vs profile `GET {PHLEBO}/api/v1/phlebo/{guid}` (step 04 also reads `GET {PHLEBO}/api/v1/phlebo/{phlebo_guid}/shifts`) | Phlebo `type` | Login returns `{phlebo}`, profile returns `[phlebo]`. |

### 2.6 Seen in earlier runs (not re-verified in `run_val8`)

| Endpoint | Issue | Detail |
|---|---|---|
| `POST {UAT-LIS}/API/ChangeBarcode/ChangeBarCodedata` / LIS data via `GET {DIAG}/order/getOrderById/{order_id}` | LIS "Sample Collected Date" | Earlier than the order creation time. |
| `GET {DIAG}/order/getOrderById/{order_id}` | `is_prime_member` | true for the non_member user. |

---

## 3. Failures outside the phlebo flow

Not reported as failures in `run_val8` (0 failures). Seen failing in `run_val5`.

| Test | Symptom |
|---|---|
| COD_99_TriggerUITest | UI automation failure |
| COD_17_ReportGenerationTest | "Only 0/2 tests synced after 10 retries" (report / LIS sync) |

---

## 4. Notes

- The barcode step (11) posts to the UAT `CHANGE_BARCODE` URL. The prod URL is not wired.
- Business states for steps 03 and 05 are tolerated.
- Negative tests (wrong OTP etc.) are not added.

## 5. Next steps

1. Raise the backend defects in section 2 with the backend team (one ticket per distinct issue; use the generated log for expected / actual values).
2. Re-run the suite after backend fixes; `-Dphlebo.failOnDefects=true` turns the defects into failures.
3. Delete the temp files in `C:\Users\RANJITH\MrYoda`: `run_val*.txt`, `modular_run.txt`, `order_dump.json`, `item_dump.json`, `ss_dump.json`.

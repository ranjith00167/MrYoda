# Phlebo Flow - Validation Log

Generated: 2026-10-14 10:14:25

| Item | Value |
|---|---|
| Suite | test-suites/testng_non_member_cod_modular.xml |
| Phlebo notification service | https://staging-api-phlebo-notification.yodadiagnostics.com |
| Diagnostics service | https://staging-api-diagnostics.yodaprojects.com |
| Total checks | 121 |
| Passed | 120 |
| Failed (test failures) | 0 |
| Backend defects | 1 |
| Distinct issues (Jira candidates) | 1 |

FAIL = a check did not hold. DEFECT = backend returned wrong / stale / null data (reported as SKIPPED in the run unless `-Dphlebo.failOnDefects=true`).

## Results by step

| Step | Endpoint under test | Passed | Failed | Defects |
|---|---|---|---|---|
| PHLEBO_MGMT_01 | `POST https://staging-api-phlebo-notification.yodadiagnostics.com/api/v1/phlebo/login` | 21 | 0 | 0 |
| PHLEBO_MGMT_02 | `POST https://staging-api-diagnostics.yodaprojects.com/auth/login` | 13 | 0 | 0 |
| PHLEBO_MGMT_03 | `POST https://staging-api-phlebo-notification.yodadiagnostics.com/api/v1/phlebo/assign-shifts` | 39 | 0 | 0 |
| PHLEBO_MGMT_04 | `GET https://staging-api-phlebo-notification.yodadiagnostics.com/api/v1/phlebo/{phlebo_guid}/shifts` | 44 | 0 | 1 |
| PHLEBO_MGMT_05 | `POST https://staging-api-phlebo-notification.yodadiagnostics.com/api/v1/phlebo/shift/clock-in` | 3 | 0 | 0 |

## Issue index

| ID | Type | Suggested priority | Check | Steps affected |
|---|---|---|---|---|
| ISSUE-01 | DEFECT | Medium | type has the same representation in login and profile | PHLEBO_MGMT_04 |

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

- Order id: `[]`
- Order tracking id: `[]`
- Phlebo guid: `null`
- Visit number: `null`

---


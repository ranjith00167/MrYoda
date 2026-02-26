# Coupon Scenario Coverage Document

Date: 2026-02-24
Project: MrYoda

## Suites Used
- `testng_coupon_usertype_listing.xml` (TC_CPN_001, TC_CPN_002)
- `testng_coupon_comprehensive.xml` (TC_CPN_003 to TC_CPN_034)

## Coverage Summary
- Total planned scenarios: 34
- Included in automation: 34
- Executed in latest run set: 34
- Passed: 27
- Failed: 7
- Skipped: 0

## Scenario-wise Status
| Test Case | Scenario | Included | Latest Status |
|---|---|---|---|
| TC_CPN_001 | Prime user receives only Prime coupons | Yes | PASS |
| TC_CPN_002 | NonPrime user receives only NonPrime coupons | Yes | PASS |
| TC_CPN_003 | Expired coupons are not listed | Yes | PASS |
| TC_CPN_004 | Future effective coupons are not listed | Yes | PASS |
| TC_CPN_005 | Active coupon within valid date range should be valid | Yes | PASS |
| TC_CPN_006 | Expired coupon should be rejected | Yes | PASS |
| TC_CPN_007 | Inactive coupon should be rejected | Yes | PASS |
| TC_CPN_008 | Invalid coupon GUID should be rejected | Yes | FAIL |
| TC_CPN_009 | Allow coupon when order total >= min_order_amount | Yes | PASS |
| TC_CPN_010 | Reject coupon when order total < min_order_amount | Yes | FAIL |
| TC_CPN_011 | Boundary test: order total exactly equals min_order_amount | Yes | PASS |
| TC_CPN_012 | Apply valid coupon successfully | Yes | PASS |
| TC_CPN_013 | Apply expired coupon | Yes | PASS |
| TC_CPN_014 | Prime coupon applied by NonPrime user | Yes | PASS |
| TC_CPN_015 | NonPrime coupon applied by Prime user | Yes | PASS |
| TC_CPN_016 | Prevent duplicate coupon application | Yes | FAIL |
| TC_CPN_017 | Validate remaining payable after coupon | Yes | PASS |
| TC_CPN_018 | Validate coupon + admin cash payment split | Yes | PASS |
| TC_CPN_019 | Coupon persists in cart | Yes | PASS |
| TC_CPN_020 | Remove coupon from cart | Yes | FAIL |
| TC_CPN_021 | Coupon carried forward / total subtraction validation | Yes | PASS |
| TC_CPN_022 | Coupon expires before order placement | Yes | PASS |
| TC_CPN_023 | Redeem coupon within allowed limit | Yes | PASS |
| TC_CPN_024 | Exceed max_number_of_coupon limit | Yes | PASS |
| TC_CPN_025 | Apply coupon for multiple family members | Yes | PASS |
| TC_CPN_026 | Membership cap per member respected | Yes | PASS |
| TC_CPN_027 | Coupon value equals order total | Yes | PASS |
| TC_CPN_028 | Coupon value greater than order total | Yes | PASS |
| TC_CPN_029 | Min order amount 1 rupee below threshold | Yes | FAIL |
| TC_CPN_030 | Last redemption allowed | Yes | PASS |
| TC_CPN_031 | Redemption after max limit | Yes | PASS |
| TC_CPN_032 | Unauthorized coupon attempt should be rejected | Yes | FAIL |
| TC_CPN_033 | Manipulate discount value in request payload | Yes | PASS |
| TC_CPN_034 | Missing auth token should return unauthorized | Yes | FAIL |

## Current Failed Scenarios
- TC_CPN_008
- TC_CPN_010
- TC_CPN_016
- TC_CPN_020
- TC_CPN_029
- TC_CPN_032
- TC_CPN_034

## Notes
- TC_CPN_032 and TC_CPN_034 failures currently indicate backend auth/security behavior (unauthorized/missing token requests are returning success).
- The remaining failed cases are functional behavior mismatches in coupon application rules in current environment data.

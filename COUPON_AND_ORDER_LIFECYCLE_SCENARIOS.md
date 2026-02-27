# 🛡️ Master Scenarios: Coupon & Order Lifecycle — COMPLETE (E2E)

> Document covers all scenarios identified from the MrYoda Diagnostics codebase.
> Updated: 2026-02-27

---

## SECTION 1 — Authentication & User Setup

| Scenario ID | Title | User Type | Positive/Negative | Description |
| :--- | :--- | :--- | :--- | :--- |
| **AUTH-01** | OTP Login — Member | Member (Prime) | Positive | Login with valid mobile + OTP; receive auth token. |
| **AUTH-02** | OTP Login — Non-Member | Non-Member (Non-Prime) | Positive | Login with valid mobile + OTP for a non-prime user. |
| **AUTH-03** | OTP Login — New User | New User | Positive | First-time registration: OTP → Create User → Receive token. |
| **AUTH-04** | Invalid OTP | Any | **Negative** | Submit wrong/expired OTP; must be rejected with appropriate error. |
| **AUTH-05** | Admin Login | Admin | Positive | Admin logs in via `/auth/login`; receives `admin_token` for approval flow. |
| **AUTH-06** | Phlebotomist Login | Phlebo | Positive | Phlebo logs in via `/phlebo/loginPhlebo`; GUID stored for order assignment. |

---

## SECTION 2 — Location & Catalog

| Scenario ID | Title | Positive/Negative | Description |
| :--- | :--- | :--- | :--- |
| **CAT-01** | Get Lab Locations | Positive | Fetch all locations; verify at least one is returned with a valid `_id`. |
| **CAT-02** | Diagnostics Catalog | Positive | Verify a specific test appears in the diagnostics catalog with a valid `price` and `_id`. |
| **CAT-03** | DNA Decoder Catalog | Positive | Fetch DNA Decoder tests; verify at least one test returned. |
| **CAT-04** | PGX (Pharmacogenomics) Catalog | Positive | Fetch PGX tests with `pharmacogenomics=true` filter; verify list is non-empty. |
| **CAT-05** | Fetal Medicine Catalog | Positive | Fetch fetal medicine tests; verify test names and prices. |
| **CAT-06** | Packages Catalog | Positive | Fetch available health packages; verify count and pricing details. |
| **CAT-07** | Location Not Available | **Negative** | Search tests with an invalid or unsupported location ID. |

---

## SECTION 3 — Global Search (Test Discovery)

| Scenario ID | Title | User Type | Positive/Negative | Description |
| :--- | :--- | :--- | :--- |
| **GSR-01** | Global Search — Member | Member | Positive | Search for a test by name; verify test returned with `price`, `_id`, `home_collection` flag. |
| **GSR-02** | Global Search — Non-Member | Non-Member | Positive | Same as above for a non-prime user. |
| **GSR-03** | Global Search — New User | New User | Positive | Same as above for a first-time user. |
| **GSR-04** | Search — No Results | Any | **Negative** | Search with a completely random keyword; must return an empty result gracefully. |

---

## SECTION 4 — Coupon Validation (Single Member)

| Scenario ID | Title | User Type | Positive/Negative | Description |
| :--- | :--- | :--- | :--- |
| **CPN-01** | Apply Valid Prime Coupon | Member | Positive | Prime user applies an active Prime coupon meeting `min_order_amount`. `couponResult.valid=true`. |
| **CPN-02** | Apply Valid Non-Prime Coupon | Non-Member / New User | Positive | Non-prime user applies a `nonPrime` type active coupon. |
| **CPN-03** | Exact Min Order Boundary | Member | Positive | Cart total equals exactly `min_order_amount`. Coupon must be accepted. |
| **CPN-04** | Below Min Order (₹1 short) | Member | **Negative** | Cart total is `min_order_amount - 1`. Coupon must be rejected. |
| **CPN-05** | Expired Coupon | Any | **Negative** | Apply a coupon where `end_date` is in the past. `couponResult.valid=false`. |
| **CPN-06** | Future Coupon (Not Yet Active) | Any | **Negative** | Apply a coupon where `effective_from` is in the future. Must be rejected. |
| **CPN-07** | Prime Coupon by Non-Prime User | Non-Member | **Negative** | Non-prime user attempts a `prime`-type coupon. Must be rejected. |
| **CPN-08** | Non-Prime Coupon by Prime User | Member | **Negative** | Member user attempts a `nonPrime`-type coupon. Must be rejected. |
| **CPN-09** | Exceeded Global Redemption Limit | Any | **Negative** | Coupon's `redeemed_coupon >= max_number_of_coupon`. Must be rejected. |
| **CPN-10** | Last Redemption Allowed | Any | Positive | Coupon is at `redeemed_coupon = max_number_of_coupon - 1`. Must still be accepted. |
| **CPN-11** | Invalid/Random GUID | Any | **Negative** | Pass `coupon_guid = "00000000-0000-0000-0000-000000000000"`. Must return `valid=false`. |
| **CPN-12** | Duplicate Same-Order Application | Member | **Negative** | Apply the same coupon twice to the same cart session. Second apply must be blocked. |
| **CPN-13** | Coupon Removal | Member | Positive | After applying coupon, update cart without `coupon_guid`. Verify discount is removed and `totalPrice` reverts. |
| **CPN-14** | Coupon Persistence Across Calls | Member | Positive | Fetch cart 3 times. Coupon and `discount_amount` must be consistent across all responses. |
| **CPN-15** | Payload Tampering (Discount Injection) | Member | **Negative** | Manually set `discount_amount=99999` in the request. Backend must recalculate and ignore the client value. |
| **CPN-16** | Unauthorized Request (Bad Token) | Any | **Negative** | Apply coupon with an invalid Bearer token. Must return 401/403 or `valid=false`. |
| **CPN-17** | Missing Auth Token | Any | **Negative** | Send AddToCart with no `Authorization` header. Must be rejected. |

---

## SECTION 5 — Cart Operations

| Scenario ID | Title | User Type | Positive/Negative | Description |
| :--- | :--- | :--- | :--- |
| **CRT-01** | Add To Cart — Home Collection | Member | Positive | Add a home-collection test to cart. `order_type=home`, coupon optionally attached. |
| **CRT-02** | Add To Cart — Lab Visit | Member | Positive | Add a lab-visit test. `order_type=lab`, no slot required at this stage. |
| **CRT-03** | Get Cart — Verify Coupon Math | Member | Positive | `GetCart` → verify: `totalPrice = Subtotal - discount_amount`. |
| **CRT-04** | Get Cart — Empty Cart | Any | **Negative** | After cancellation, `GetCart` should return empty product list. |
| **CRT-05** | Add to Cart — Discount Cap (Zero Payable) | Member | Positive | If `Discount >= Subtotal`, verify `totalPrice = 0` (not negative). |
| **CRT-06** | Cart Updated After Slot Booking | Member | Positive | After updating slot, cart still contains the `coupon_guid`. Coupon must not be stripped. |

---

## SECTION 6 — Family Member Scenarios

| Scenario ID | Title | Positive/Negative | Description |
| :--- | :--- | :--- | :--- |
| **FAM-01** | Add Family Member | Positive | Add a family member; verify `guid` returned and stored in context. |
| **FAM-02** | Proportional Discount Split (2 Members) | Positive | Two members in cart with a coupon. Verify each member's discount is proportional to their subtotal. |
| **FAM-03** | Proportional Split (3 Members) | Positive | Three members: `Member_Discount = (Member_Subtotal / Cart_Total) * Total_Discount`. |
| **FAM-04** | Shared Product Split | Positive | Add one shared product (e.g. Home Collection Fee) for 2 members. Each member gets exactly 50% of that cost before discount is applied. |
| **FAM-05** | Family Cart Min-Order | Positive | Cart total meets `min_order_amount` only when all members' portions are combined. Coupon must apply. |
| **FAM-06** | Add Member Mid-Session | Positive | Add a new family member to a cart that already has a coupon. Verify proportional split is auto-recalculated. |
| **FAM-07** | Rounding Accuracy | Positive | Sum of all individual member discount shares must equal the total `discount_amount` (no ₹1 rounding gap). |
| **FAM-08** | Delete Family Member | Positive | Delete a family member; verify they are removed from cart products and split is recalculated. |
| **FAM-09** | Get Family Members List | Positive | Retrieve all family members for a user; verify names and GUIDs. |
| **FAM-10** | Invalid Family Member GUID | **Negative** | Add a product with a non-existent `family_member_id`. Must return error. |

---

## SECTION 7 — Address & Slot Booking

| Scenario ID | Title | Positive/Negative | Description |
| :--- | :--- | :--- | :--- |
| **SLT-01** | Add Valid Home Address | Positive | Add a valid address; receive `address_id` and `address_guid`. |
| **SLT-02** | Get Available Slots | Positive | Fetch slots for the next 7 days; pick first available slot with `count > 0`. |
| **SLT-03** | Update Cart With Slot | Positive | Update cart with `slot_guid` + `address_guid`. Verify coupon persists in payload. |
| **SLT-04** | Slot Not Available | **Negative** | No available slots exist for the next 7 days. The test must gracefully fail with appropriate message. |
| **SLT-05** | Slot for Lab Visit | Positive | For `order_type=lab`, slot booking via `GET_CENTERS_BY_ADD`. No address needed. |

---

## SECTION 8 — Order Creation & COD Flow

| Scenario ID | Title | User Type | Positive/Negative | Description |
| :--- | :--- | :--- | :--- |
| **ORD-01** | Create COD Order — Member | Member | Positive | `VerifyPayment` with `payment_mode=COD`. Order created; status = `Pending`. |
| **ORD-02** | Create COD Order — Non-Member | Non-Member | Positive | Same flow for a non-prime user; `nonPrime` coupon if enabled. |
| **ORD-03** | Create COD Order — New User | New User | Positive | Same flow for a first-time user with a fresh account. |
| **ORD-04** | Order With Coupon — Price Persistence | Member | Positive | `totalPrice` in `VerifyPayment` must match the discounted `totalPrice` from `GetCart`. |
| **ORD-05** | COD Limit Exceeded (>₹2500) | Any | **Negative** | Cart total exceeds ₹2500 COD limit. Order creation must block with a clear error. |
| **ORD-06** | Duplicate Order Attempt | Member | **Negative** | Attempt `VerifyPayment` twice for the same cart. Second attempt must be rejected or idempotent. |

---

## SECTION 9 — Order Tracking (Phlebotomist Flow)

| Scenario ID | Title | Positive/Negative | Description |
| :--- | :--- | :--- | :--- |
| **TRK-01** | Assign Phlebo to Order | Positive | Admin assigns a phlebotomist GUID to the order. Status becomes `Phlebotomist assigned`. |
| **TRK-02** | Admin OTP Verification | Positive | Admin verifies the collection OTP for the Order Tracking ID. |
| **TRK-03** | Update Tracking (In-Progress) | Positive | Phlebo updates order tracking status to `inprogress` with lat/lng. |
| **TRK-04** | Samples Collected | Positive | Update order status to `samples_collected`. Verify in `GetOrderById`. |
| **TRK-05** | Get Sample Type | Positive | Fetch sample type for the test before updating collection status. |
| **TRK-06** | Wrong Phlebo Assigned | **Negative** | Assign a GUID of a non-existent phlebotomist. Must return an error. |

---

## SECTION 10 — Admin Portal: Payment Approval & Rejection

| Scenario ID | Title | Positive/Negative | Description | Expected Outcome |
| :--- | :--- | :--- | :--- | :--- |
| **ADM-01** | Full COD Approval | Positive | Admin approves the exact `remaining_payable` (after coupon). | Status → `Paid/Approved`. |
| **ADM-02** | Multi-Member Approval | Positive | One payment covers tests for 3 family members. Admin approves; all members updated. | All member statuses → `Paid`. |
| **ADM-03** | Partial Approval | **Negative** | Admin approves less than `remaining_payable`. | Status must stay `Pending`. |
| **ADM-04** | Payment Rejection | **Negative** | Admin rejects ("Cash not received"). | Status → `Unpaid`. User prompted to retry. |
| **ADM-05** | Approve Cancelled Order | **Negative** | Admin tries to approve payment for a cancelled order. | System must block or flag as error. |
| **ADM-06** | Coupon Math Visibility | Positive | Admin Portal must show: **Original Total**, **Coupon Discount**, **Amount to Collect**. | Values must match `GetCart` data. |

---

## SECTION 11 — Rewards (Post-Payment Validation)

> Rewards only apply to **Member (Prime)** users after a successful payment.

| Scenario ID | Title | Positive/Negative | Description | Calculation |
| :--- | :--- | :--- | :--- | :--- |
| **RWD-01** | Initial Rewards Balance | Positive | Capture reward points before payment. Stored in context. | `initial_rewards = GetRewardsByMobile()` |
| **RWD-02** | Rewards Gain After Approval | Positive | After Admin approves, `rewards_gain` is returned by the Approve API. | `expected_final = initial + rewards_gain` |
| **RWD-03** | Final Balance Verification | Positive | Re-fetch rewards balance. Must match: `initial + rewards_gain`. | Tolerance: ≤ ₹0.1 difference. |
| **RWD-04** | Rewards for Non-Member | Positive | Rewards step is **skipped** for Non-Member/New User flows (no rewards earned). | Step logs "Skipping" and passes. |
| **RWD-05** | Coupon Impact on Rewards | Positive | Rewards are calculated on `due_amount` (Payable after coupon), **not** on original total. | `rewards_gain is based on ₹(Total - Coupon)`. |

---

## SECTION 12 — Post-Order: Cancellation & Refund

| Scenario ID | Title | Positive/Negative | Description | Financial Math |
| :--- | :--- | :--- | :--- | :--- |
| **CAN-01** | Order Status Before Cancellation | Positive | Verify order is in correct pre-cancellation state (e.g., `Paid`, `samples_collected`). | N/A |
| **CAN-02** | Cancel Order | Positive | Call `v2updateOrder` with `order_status=Cancelled`. Must return success message. | N/A |
| **CAN-03** | Status After Cancellation | Positive | Verify `GetOrderById` returns `order_status=Cancelled`. | N/A |
| **REF-01** | Full Refund (Solo Member) | Positive | User paid ₹1200 (₹1500 - ₹300 coupon). Refund = **₹1200 only**. Coupon value not refunded. | `Refund = Paid Amount (not Original Total)` |
| **REF-02** | Partial Family Refund | Positive | Member B cancels from group order. Refund = Member B's proportional share of *Paid* amount. | `Refund_B = (B_Subtotal / Cart_Total) * Paid_Amount` |
| **REF-03** | Coupon Reclamation on Cancel | Positive | Order cancelled **before** samples collected. Coupon returned to user's "Available" list. | Redemption count decremented. |
| **REF-04** | Post-Sample Collection Refund | **Negative** | Refund after Phlebo visit. Must deduct collection fees. | `Refund = Paid_Amount - Collection_Fee` |
| **REF-05** | Coupon Burned Post-Refund | **Negative** | If order is fully refunded but samples were collected, coupon may not be re-issued. | Business policy dependent. |

---

## SECTION 13 — Report & Visit Validation (Post-Lab)

| Scenario ID | Title | Positive/Negative | Description |
| :--- | :--- | :--- | :--- |
| **RPT-01** | Fetch Report by Visit Number | Positive | After lab processing, call `GetReportDetailsByVisitNumber`. Verify report data is returned. |
| **RPT-02** | Verify Report Date | Positive | Report's `order_date` must match the date the order was created. Handle timezone and format variations. |
| **RPT-03** | Report Fidelity Score | Positive | All critical report fields (patient name, test name, result, date) must be populated — no nulls. |
| **RPT-04** | Report for Invalid Visit | **Negative** | Call report API with a random/old visit number. Must return `not found` gracefully. |

---

## SECTION 14 — Calculation Edge Cases (Cross-Cutting)

| ID | Edge Case | Rule |
| :--- | :--- | :--- |
| **E-01** | Zero Payable | If `Discount >= Subtotal`, `totalPrice = 0`. Never negative. |
| **E-02** | Percentage Discount Cap | 50% off up to ₹500: if Subtotal=₹2000, discount is capped at ₹500, not ₹1000. |
| **E-03** | Rounding in Family Split | Sum of all member individual discounts must equal the total `discount_amount` (no ₹1 gap). |
| **E-04** | Rewards on Net Amount | Rewards are earned on `Payable Amount` (after coupon), not on the original `Subtotal`. |
| **E-05** | Refund Never Exceeds Paid | Refund amount can never be greater than the actual amount received by the system (paid amount after coupon). |

---

## SECTION 15 — Summary: Scenarios by User Type

| User Type | Coupon Type | Rewards? | COD Limit | Key Extra Scenario |
| :--- | :--- | :--- | :--- | :--- |
| **Member (Prime)** | `prime` | ✅ Yes | ₹2500 | Rewards gain after admin approval |
| **Non-Member** | `nonPrime` | ❌ No | ₹2500 | No rewards step; coupon type mismatch tests |
| **New User** | `nonPrime` | ❌ No | ₹2500 | Full registration flow from OTP onwards |
| **Family (Multi-Member)** | Same as primary user | Per primary | Same | Proportional split, shared product, partial refund |
| **Admin** | N/A | N/A | N/A | Approve, Reject, Cross-flow check |

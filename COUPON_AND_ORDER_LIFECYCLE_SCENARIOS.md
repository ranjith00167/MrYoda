MrYoda Diagnostics
Master Test Case Document — Order, Coupon & Reschedule Lifecycle (E2E)

Version: 6.0
Updated: 2026-02-27
Scope: Functional + Business + Financial + Medical + Security + Concurrency + Technical Handover

1️⃣ Authentication & Access Control
TC-AUTH-01 — OTP Login (Prime)
Expected: Token issued, role = Prime

TC-AUTH-02 — OTP Login (Non-Member)
Expected: Token issued, role = NonPrime

TC-AUTH-03 — Invalid OTP
Expected: Rejected (400)

TC-AUTH-04 — OTP Expiry
Expected: Rejected (400)

TC-AUTH-05 — OTP Brute Force
Expected: Account locked after threshold attempts

TC-AUTH-06 — Admin Login
Expected: Admin token issued

TC-AUTH-07 — Phlebo Login
Expected: GUID returned

TC-AUTH-08 — JWT Expired
Expected: 401 Unauthorized

TC-AUTH-09 — Token Role Misuse
Scenario: Admin token used in User endpoints / Client token in Admin endpoints.
Expected: 403 Forbidden

TC-AUTH-10 — Logout Token Invalidation
Expected: Once logged out, previous token is blacklisted.

2️⃣ Catalog & Search
TC-CAT-01 — Fetch Locations
Expected: At least one valid location returned.

TC-CAT-02 — Valid Global Search
Expected: Test ID, status, and price returned. 

TC-CAT-03 — Invalid Location Fetch
Expected: Error/Empty response for non-serviced coordinates.

TC-CAT-04 — SQL Injection Attempt
Expected: Input sanitized.

TC-CAT-05 — Extremely Long Query
Expected: Graceful handling without systemic timeout.

TC-CAT-06 — Location-Test Mismatch
Scenario: Test A exists in City X but not in City Y. Search from City Y.
Expected: Test not found / Not available in location.

3️⃣ Collection Mode & Service Charge Logic
Rules:
Modes: Lab Visit / Home Collection
Non-Member: Home AND Subtotal < ₹999 → +₹250
Prime: Home → ₹0
Lab: ₹0 for all

TC-HC-01 — Non-Member Home ₹800 → +₹250
TC-HC-02 — Non-Member Home ₹999 → No charge
TC-HC-03 — Boundary ₹998.99 → Charge applied
TC-HC-04 — Prime Home Any Amount → No charge
TC-HC-05 — Lab Visit → No charge
TC-HC-06 — Threshold Drop Below ₹999 → Dynamic addition of ₹250
TC-HC-07 — Threshold Cross Above ₹999 → Dynamic removal of ₹250
TC-HC-08 — Family Combined Threshold
Scenario: 2 members with ₹500 each.
Expected: Combined subtotal = ₹1000. No ₹250 charge.

TC-HC-09 — Prime Downgrade Mid-Cart
Scenario: User is Prime, adds items, Prime expires.
Expected: Cart recalculates and adds ₹250 if below threshold.

4️⃣ Coupon Engine
TC-CPN-01 — Valid Coupon
TC-CPN-02 — Below MOV (Minimum Order Value)
TC-CPN-03 — Expired / Future Coupon
TC-CPN-04 — Stack Attempt (Multiple Coupons)
TC-CPN-05 — Invalid GUID Mapping
TC-CPN-06 — Redemption Limit Reach
TC-CPN-07 — Parallel Last Redemption
Scenario: 2 users apply the very last redemption of a coupon simultaneously.
Expected: Only one succeeds.

TC-CPN-08 — MOV Recalculation After Partial Cancel
Scenario: Price drops below MOV due to item removal.
Expected: Coupon automatically removed.

5️⃣ Prime Membership & Per-Member Cap
Rules:
Prime inheritance for all family members in order.
Max ₹1000 discount per member.
Discount ≤ member subtotal.

TC-PCAP-01 — 1 Member High Subtotal → ₹1000 Cap
TC-PCAP-02 — 5 Members → ₹5000 Max Cart Discount
TC-PCAP-03 — Mixed Subtotals
Scenario: Member A (₹2000), Member B (₹400).
Expected: A gets ₹1000 off, B gets ₹400 off.

TC-PCAP-04 — Remove Member → Dynamic Cap Reduction
TC-PCAP-05 — Zero Subtotal Member
Scenario: Adding a ₹0 free test for a member.
Expected: They contribute ₹0 to the total order cap.

6️⃣ Cart & Financial Split (Precision)
TC-CRT-01 — Proportional Discount Validation
Validation: (MemberSubtotal / CartSubtotal) * TotalDiscount.
TC-CRT-02 — No Rounding Gap
Validation: Sum(member shares) MUST exactly equal Cart Payable. (Precision check to ₹0.01).
TC-CRT-03 — Shared Charge Split
Expected: ₹250 service charge split proportionally across members.

7️⃣ Address Management (Technical)
TC-ADR-01 — Add Valid Address (201 Created)
TC-ADR-02 — Duplicate Address Handling
Expected: Backend returns 409 Conflict if same address details sent twice.

TC-ADR-03 — Coordinate Validation
Scenario: Map Pin (Lat/Long) mismatch with City Name.
Expected: Rejected / Flagged for correction.

TC-ADR-04 — Address Hijacking
Scenario: User X attempts to use User Y's address_guid in an order.
Expected: 403 Forbidden / Rejected.

8️⃣ Slot Management
TC-SLT-01 — Book Slot (Inventory Decrease)
TC-SLT-02 — Parallel Booking (Overselling check)
TC-SLT-03 — Post-Cancellation Slot Restoration
TC-SLT-04 — Sample Rejection → Inventory Re-opening

9️⃣ Order Creation & Payment
TC-ORD-01 — COD Limit Check (≤ ₹2500)
TC-ORD-02 — Double Click Payment Protection
TC-ORD-03 — Price Tampering
Scenario: Intercepting request to change total_price.
Expected: Backend recalculation match; Rejection.

TC-ORD-04 — Payment Callback Idempotency
Scenario: Gateway sends "Success" callback twice.
Expected: Single order creation / single capture in DB.

🔟 Lab Portal (CodItDose)
TC-LAB-01 — Multi-Department Visit (Single ID, Multiple Worklists)
TC-LAB-02 — Sample Rejection Flow (Status reverts to "Pending Collection")
TC-LAB-03 — Critical Value SMS/Email Alert
TC-LAB-04 — Result Modification Audit (Logged with Tech ID)
TC-LAB-05 — Final Authorization Lock (No edits after approval)

1️⃣1️⃣ Phlebotomist Flow
TC-TRK-01 — Assign Phlebo
TC-TRK-02 — Phlebo Assignment Conflict (Hijack Prevention)
TC-TRK-03 — Cancel After Assignment (Rejected in UI/API)

1️⃣2️⃣ Cancellation & Refunds (Precision)
TC-CAN-01 — Partial Family Refund
TC-CAN-02 — Refund After Partial Lab Completion
Scenario: Test A and B processed, Test C cancelled.
Expected: Refund for Test C only.

TC-CAN-03 — Double Refund Request
Expected: System prevents 2nd refund ledger entry.

1️⃣3️⃣ Rewards & Loyalty
TC-RWD-01 — Proportional Reward Reversal on Refund
TC-RWD-02 — Negative Reward Edge Case

1️⃣4️⃣ State Transition Matrix
Strict Validation Checklist (Illegal jumps):
- Pending → ResultEntered (REJECT)
- Cancelled → Approved (REJECT)
- ResultsEntered → Pending (REJECT)

1️⃣5️⃣ Reschedule Scenarios (Max 3 attempts)
TC-RSCH-01 — First, Second, Third Reschedule (Allowed)
TC-RSCH-02 — Fourth Attempt (Blocked)
TC-RSCH-03 — Reschedule After Slot Expiry
TC-RSCH-04 — Counter Persistence (Across Logout)
TC-RSCH-05 — Reschedule After Assignment (REJECT)

1️⃣6️⃣ Technical Handover (Handshake Scenarios)
TC-TECH-01 — API-to-UI Shared Context
Scenario: Visit number extracted from API Payment response matches Visit number searched in Lab Portal.
Expected: 1:1 Match; No manual sin entry needed.

TC-TECH-02 — JVM Session Persistence
Scenario: Test suite running in single thread vs parallel thread.
Expected: RequestContext data preserved without "null" leakage.

1️⃣7️⃣ Performance & Security
TC-SEC-01 — IDOR (Insecure Direct Object Reference)
Scenario: Accessing reports/orders of User B via User A's token.
TC-SEC-02 — JWT Replay Attack
TC-PERF-01 — Search Latency (<500ms for 100 users)
TC-PERF-02 — Report Generation Throughput (50 PDFs/min)
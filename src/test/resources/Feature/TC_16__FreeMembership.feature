# ══════════════════════════════════════════════════════════════════════════════
# HYBRID SUITE : Place lab orders and pay online
# ══════════════════════════════════════════════════════════════════════════════
# PHASE 1 – UI Automation : Login → Test selection → Cart → Checkout → Pay Online
#                           (Razorpay completes INSIDE the browser)
# PHASE 2 – API Verify    : Extract token from browser → getOrderById → validate
#
# NOTE: No InitiatePayment / VerifyPayment API calls needed.
#       The payment is completed end-to-end through the Razorpay UI.
#       After payment success, we use getOrderById API to verify the order.
# ══════════════════════════════════════════════════════════════════════════════
@diagnostics @payOnlineHybrid
Feature: Place lab orders and complete payment via UI, then verify via getOrderById API

  Background:
    Given the user is on the login page
  # ─────────────────────────────────────────────────────────────────────────────
  # Scenario 1 : Single Member – Lab Visit – Pay Online
  # ─────────────────────────────────────────────────────────────────────────────

  @hybridLabOrderMember
  Scenario: Single member places a lab visit order via UI and verifies via getOrderById API
    # ── UI : Login ──────────────────────────────────────────────────────────────
    Given load the excel data for single member and lab visit
    When create an account with random mobile number
    And click on the submit button
    Then the location should be auto-detected on the dashboard
    And find whether the account holder is member or non member
    And verify whether the already selected tests are retained in the cart after login
    # Login via API immediately after UI login to establish token
    And login via API and refresh auth token
    # ── API : Pricing Data Retrieval (before UI selection) ─────────────────────
    Then the location ID is available from RequestContext
    And a valid authentication token is available
    When API call is made to GET_ALL_TESTS endpoint
    Then GET_ALL_TESTS should return complete test list
    And all tests should have pricing information
    And pricing should be parsed correctly from multiple field names
    And log test pricing summary - total tests retrieved
    # ── API : GET_ALL_PACKAGES Pricing Validation ───────────────────────────────
    When API call is made to GET_ALL_PACKAGES endpoint
    Then GET_ALL_PACKAGES should return complete package list
    And all packages should have pricing information
    And package prices should be parsed correctly
    And log package pricing summary - total packages retrieved
    # ── UI : Test Selection ─────────────────────────────────────────────────────
    And click the Best Seller View All button
   # And load test names from Excel
   # And select tests and capture individual prices
    And select Thrombotic Panel and add to cart
    Then All tests should complete successfully
    # ── API : GLOBAL_SEARCH Validation with Excel Test Names ──────────────────
    When API call is made to GLOBAL_SEARCH for first test name from Excel
    Then GLOBAL_SEARCH should return test or package details
    And log the GLOBAL_SEARCH results for verification
    # ── API : Package Component Resolution with Excel Package Names ────────────
    When API call is made to resolve first package from Excel
    Then package should be found in GET_ALL_PACKAGES
    And package components should be extracted successfully
    And components should be individual test names
    And log the package to component mapping for validation
    # ── API : Test Pricing vs UI Pricing Validation ─────────────────────────────
    When fetch all test prices from GET_ALL_TESTS API
    Then API test prices should match captured UI prices
    And log comparison results showing all tests match or flag discrepancies
    # ── UI : Cart & Checkout ────────────────────────────────────────────────────
    When click on the cart icon
    And set the visit type from UI for lab visit
    Then validate checkout summary header values
    And select the member
    And select the location
    And select the slot
    # ── API : Cross-API Pricing Consistency Validation ──────────────────────────
    When fetch all package prices from GET_ALL_PACKAGES API
    And search for specific tests using GLOBAL_SEARCH API
    Then pricing from all three APIs should be consistent
    And pricing for common tests should have no discrepancies
    And log pricing validation summary for cross-API consistency
    # ── API : Package Components in Visit Status Validation ────────────────────
    And load test names from Excel including packages
    # ── UI : Checkout Validation ────────────────────────────────────────────────
    And validate the actual price against the checkout price
    When the user extract the member names from the page
    When the user extracts the member names from the excel sheet
    Then the member names on the page should match the excel
    And validate final amount to pay
    Then capture amount to pay
    Then calculate expected final amount
    # ── API : Price Calculation Validation with Mixed Items ────────────────────
    Then calculate expected total from API prices
    Then checkout total should match API calculated total
    And log the calculation breakdown
    Then validate cart amount to pay
    Then verify cart details via getCartById API for member
    # ── UI : Payment via Razorpay (browser-based) ───────────────────────────────
    When click pay online button
    Then extract razorpay amount
    Then validate razorpay amount against expected amount
    Then validate Razorpay payment options based on amount
    Then initiate upi payment if allowed
    # ── API : Verify Order via getOrderById ─────────────────────────────────────
    And capture order id from browser after payment
    Then fetch order by id via API and assert status is confirmed
    # ── API : Validate Free Membership Rewards ─────────────────────────────────
    Then validate free membership remarks in getRewardsByMobile API
    # ── UI : Confirm order on screen ────────────────────────────────────────────
    And click the go to orders button
    And click the view button in orders page
    # ── RESPONSE CAPTURE FOR VALIDATION ANALYSIS ───────────────────────────────
    Then generate API response capture report for validation analysis
  
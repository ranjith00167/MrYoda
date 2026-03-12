# Placing lab and home orders for multi members with and without membership and completing online payment in most popular packages
@diagnostics @AddMemberCompleteflow
Feature: Place lab orders and pay online

  Background:
    Given the user is on the login page

  @add_member_multi_lab_order_membership
  Scenario: Place a lab visit for a family member with the profile owner and complete online payment in most popular packages(Member)
    # ── UI : Login ──────────────────────────────────────────────────────────────
    Given load the excel data for multi member and lab visit with memership
    When the user enters otp
    And click on the submit button
    Then the location should be auto-detected on the dashboard
    And find whether the account holder is member or non member
    And verify whether the already selected tests are retained in the cart after login
    # Login via API immediately after UI login to establish token
    And login via API and refresh auth token
    And capture initial rewards balance for member
    # ── UI : Test Selection ─────────────────────────────────────────────────────
    And click the Best Seller View All button
    And load test names from Excel
    And select tests and capture individual prices
    Then All tests should complete successfully
    When click on the cart icon
    And set the visit type from UI for lab visit
    Then validate checkout summary header values
    And select the member for multi member scenario
  # Then validate selected tests and prices inside member popup
  # Then validate all cart items and totals from excel
    And extract the total amount during checkout for the multi-member scenario
    And select the location
    And select the slot
    And validate the actual price against the checkout price
    When the user extract the member names from the page
    When the user extracts the member names from the excel sheet
    Then the member names on the page should match the excel
    And validate final amount to pay
  # And click the pay online button
    Then capture amount to pay
    Then calculate expected final amount
    Then validate cart amount to pay
    Then verify cart details via getCartById API for member
    # ── UI : Payment via Razorpay (browser-based) ───────────────────────────────
    When click pay online button
    Then extract razorpay amount
    Then validate razorpay amount against expected amount
    Then validate Razorpay payment options based on amount
    Then initiate upi payment if allowed
    # ── API : Verify Order via getOrderById ─────────────────────────────────────
    And capture all order ids from browser after payment for multi member
    Then fetch order by id via API and assert status is confirmed
    # ── UI : Confirm order on screen ────────────────────────────────────────────
    And click the go to orders button
    And click the view button in orders page

  @add_member_multi_home_order_membership
  Scenario: Place a home visit for a family member with the profile owner and complete online payment in most popular packages(Member)
    # ── UI : Login ──────────────────────────────────────────────────────────────
    Given load the excel data for multi member and home collection with memership
    When the user enters otp
    And click on the submit button
    Then the location should be auto-detected on the dashboard
    And find whether the account holder is member or non member
    And verify whether the already selected tests are retained in the cart after login
    # Login via API immediately after UI login to establish token
    And login via API and refresh auth token
    And capture initial rewards balance for member
    # ── UI : Test Selection ─────────────────────────────────────────────────────
    And click the Best Seller View All button
    And load test names from Excel
    And select tests and capture individual prices
    Then All tests should complete successfully
    When click on the cart icon
    And set the visit type from UI for home collection
    Then validate checkout summary header values
    And select the member for multi member scenario
    And extract the total amount during checkout for the multi-member scenario
    And select the home location
    And select the slot
    And validate the actual price against the checkout price
    When the user extract the member names from the page
    When the user extracts the member names from the excel sheet
    Then the member names on the page should match the excel
    And validate final amount to pay
    # And click the pay online button
    Then capture amount to pay
    Then calculate expected final amount
    Then validate cart amount to pay
    Then verify cart details via getCartById API for member
    # ── UI : Payment via Razorpay (browser-based) ───────────────────────────────
    When click pay online button
    Then extract razorpay amount
    Then validate razorpay amount against expected amount
    Then validate Razorpay payment options based on amount
    Then initiate upi payment if allowed
    # ── API : Verify Order via getOrderById ─────────────────────────────────────
    And capture all order ids from browser after payment for multi member
    Then fetch order by id via API and assert status is confirmed
    # ── UI : Confirm order on screen ────────────────────────────────────────────
    And click the go to orders button
    And click the view button in orders page

  @add_member_multi_lab_order_without_membership
  Scenario: Place a lab visit for a family member with the profile owner and complete online payment in most popular packages(Without Member)
    # ── UI : Login ──────────────────────────────────────────────────────────────
    Given load the excel data for multi member and lab visit without memership
    When the user enters otp
    And click on the submit button
    Then the location should be auto-detected on the dashboard
    And find whether the account holder is member or non member
    And verify whether the already selected tests are retained in the cart after login
    # Login via API immediately after UI login to establish token
    And login via API and refresh auth token
    # ── UI : Test Selection ─────────────────────────────────────────────────────
    And click the Best Seller View All button
    And load test names from Excel
    And select tests and capture individual prices
    Then All tests should complete successfully
    When click on the cart icon
    And set the visit type from UI for lab visit
    Then validate checkout summary header values
    And select the member for multi member scenario
    And extract the total amount during checkout for the multi-member scenario
    And select the location
    And select the slot
    And validate the actual price against the checkout price
    When the user extract the member names from the page
    When the user extracts the member names from the excel sheet
    Then the member names on the page should match the excel
    And validate final amount to pay
    And validate final amount to pay with the Mr yoda club save value
    Then capture amount to pay
    Then calculate expected final amount
    Then validate cart amount to pay
    Then verify cart details via getCartById API for member
    # ── UI : Payment via Razorpay (browser-based) ───────────────────────────────
    When click pay online button
    Then extract razorpay amount
    Then validate razorpay amount against expected amount
    Then validate Razorpay payment options based on amount
    Then initiate upi payment if allowed
    # ── API : Verify Order via getOrderById ─────────────────────────────────────
    And extract auth token from browser local storage
    And capture all order ids from browser after payment for multi member
    Then fetch order by id via API and assert status is confirmed
    # ── UI : Confirm order on screen ────────────────────────────────────────────
    And click the go to orders button
    And click the view button in orders page

  @add_member_multi_lab_order_new_user
  Scenario: Place a lab visit for a family member with a new user and complete online payment in most popular packages(New User)
    # ── UI : Registration & Login ────────────────────────────────────────────────
    Given load the excel data for multi member and lab visit for new user
    When create an account with random mobile number
    And click on the submit button
    Then the location should be auto-detected on the dashboard
    And find whether the account holder is member or non member
    And verify whether the already selected tests are retained in the cart after login
    # Login via API immediately after UI login to establish token
    And login via API and refresh auth token
    # ── UI : Test Selection ─────────────────────────────────────────────────────
    And click the Best Seller View All button
    And load test names from Excel
    And select tests and capture individual prices
    Then All tests should complete successfully
    # ── UI : Cart & Checkout ────────────────────────────────────────────────────
    When click on the cart icon
    And set the visit type from UI for lab visit
    Then validate checkout summary header values
    And select the member for multi member scenario
    And extract the total amount during checkout for the multi-member scenario
    And select the location
    And select the slot
    And validate the actual price against the checkout price
    When the user extract the member names from the page
    When the user extracts the member names from the excel sheet
    Then the member names on the page should match the excel
    And validate final amount to pay
    And validate final amount to pay with the Mr yoda club save value
    Then capture amount to pay
    Then calculate expected final amount
    Then validate cart amount to pay
    Then verify cart details via getCartById API for member
    # ── UI : Payment via Razorpay (browser-based) ───────────────────────────────
    When click pay online button
    Then extract razorpay amount
    Then validate razorpay amount against expected amount
    Then validate Razorpay payment options based on amount
    Then initiate upi payment if allowed
    # ── API : Verify Order via getOrderById ─────────────────────────────────────
    And extract auth token from browser local storage
    And capture all order ids from browser after payment for multi member
    Then fetch order by id via API and assert status is confirmed
    # ── UI : Confirm order on screen ────────────────────────────────────────────
    And click the go to orders button
    And click the view button in orders page

  @add_member_multi_home_order_new_user
  Scenario: Place a home visit for a family member with a new user and complete online payment in most popular packages(New User)
    # ── UI : Registration & Login ────────────────────────────────────────────────
    Given load the excel data for multi member and home collection for new user
    When create an account with random mobile number
    And click on the submit button
    Then the location should be auto-detected on the dashboard
    And find whether the account holder is member or non member
    And verify whether the already selected tests are retained in the cart after login
    # Login via API immediately after UI login to establish token
    And login via API and refresh auth token
    # ── UI : Test Selection ─────────────────────────────────────────────────────
    And click the Best Seller View All button
    And load test names from Excel
    And select tests and capture individual prices
    Then All tests should complete successfully
    # ── UI : Cart & Checkout ────────────────────────────────────────────────────
    When click on the cart icon
    And set the visit type from UI for home collection
    Then validate checkout summary header values
    And select the member for multi member scenario
    And extract the total amount during checkout for the multi-member scenario
    And select the home location
    And select the slot
    And validate the actual price against the checkout price
    When the user extract the member names from the page
    When the user extracts the member names from the excel sheet
    Then the member names on the page should match the excel
    And validate final amount to pay
    And validate final amount to pay with the Mr yoda club save value
    Then capture amount to pay
    Then calculate expected final amount
    Then validate cart amount to pay
    Then verify cart details via getCartById API for member
    # ── UI : Payment via Razorpay (browser-based) ───────────────────────────────
    When click pay online button
    Then extract razorpay amount
    Then validate razorpay amount against expected amount
    Then validate Razorpay payment options based on amount
    Then initiate upi payment if allowed
    # ── API : Verify Order via getOrderById ─────────────────────────────────────
    And extract auth token from browser local storage
    And capture all order ids from browser after payment for multi member
    Then fetch order by id via API and assert status is confirmed
    # ── UI : Confirm order on screen ────────────────────────────────────────────
    And click the go to orders button
    And click the view button in orders page

  @add_member_multi_home_order_without_membership
  Scenario: Place a home visit for a family member with the profile owner and complete online payment in most popular packages(without Member)
    # ── UI : Login ──────────────────────────────────────────────────────────────
    Given load the excel data for multi member and home collection without memership
    When the user enters otp
    And click on the submit button
    Then the location should be auto-detected on the dashboard
    And find whether the account holder is member or non member
    And verify whether the already selected tests are retained in the cart after login
    # Login via API immediately after UI login to establish token
    And login via API and refresh auth token
    # ── UI : Test Selection ─────────────────────────────────────────────────────
    And click the Best Seller View All button
    And load test names from Excel
    And select tests and capture individual prices
    Then All tests should complete successfully
    When click on the cart icon
    And set the visit type from UI for home collection
    Then validate checkout summary header values
    And select the member for multi member scenario
    And extract the total amount during checkout for the multi-member scenario
    And select the home location
    And select the slot
    And validate the actual price against the checkout price
    When the user extract the member names from the page
    When the user extracts the member names from the excel sheet
    Then the member names on the page should match the excel
    And validate final amount to pay
    And validate final amount to pay with the Mr yoda club save value
    Then capture amount to pay
    Then calculate expected final amount
    Then validate cart amount to pay
    Then verify cart details via getCartById API for member
    # ── UI : Payment via Razorpay (browser-based) ───────────────────────────────
    When click pay online button
    Then extract razorpay amount
    Then validate razorpay amount against expected amount
    Then validate Razorpay payment options based on amount
    Then initiate upi payment if allowed
    # ── API : Verify Order via getOrderById ─────────────────────────────────────
    And extract auth token from browser local storage
    And capture all order ids from browser after payment for multi member
    Then fetch order by id via API and assert status is confirmed
    # ── UI : Confirm order on screen ────────────────────────────────────────────
    And click the go to orders button
    And click the view button in orders page

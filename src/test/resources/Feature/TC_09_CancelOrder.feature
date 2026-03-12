@diagnostics @CancelCompleteFlow
Feature: Place lab orders and pay online with cancel order flow(Member)

  Background:
    Given the user is on the login page

  @CancelLabOrder
  Scenario: Place a lab visit for a single member and complete online payment  in most popular packages
    Given load the excel data for single member and lab visit
    When the user enters otp
    And click on the submit button
    Then the location should be auto-detected on the dashboard
    And find whether the account holder is member or non member
    And verify whether the already selected tests are retained in the cart after login
    And click the Best Seller View All button
    And load test names from Excel
    And select tests and capture individual prices
    Then All tests should complete successfully
    When click on the cart icon
    And set the visit type from UI for lab visit
    Then validate checkout summary header values
    And select the member
  # And extract the total amount during checkout for the multi-member scenario
    And select the location
    And select the slot
    And validate the actual price against the checkout price
    When the user extract the member names from the page
    When the user extracts the member names from the excel sheet
    Then the member names on the page should match the excel
    And validate final amount to pay
    Then capture amount to pay
    Then calculate expected final amount
    Then validate cart amount to pay
    When click pay online button
    Then extract razorpay amount
    Then validate razorpay amount against expected amount
    Then validate Razorpay payment options based on amount
    Then initiate upi payment if allowed
    And click the go to orders button
    And click the view button in orders page
    And click on cancel order button in orders page
    And validate the tests in shows admin approval pending state

  @cancelHomeOrder
  Scenario: Place a home sample visit for a single member and complete online payment with cancel order flow
    Given load the excel data for single member and home collection
    When the user enters otp
    And click on the submit button
    Then the location should be auto-detected on the dashboard
    And find whether the account holder is member or non member
    And verify whether the already selected tests are retained in the cart after login
    And click the Best Seller View All button
    And load test names from Excel
    And select tests and capture individual prices
    Then All tests should complete successfully
    When click on the cart icon
    And set the visit type from UI for home collection
    Then validate checkout summary header values
    And select the member
    And select the home location
    And select the slot
    And validate the actual price against the checkout price
    When the user extract the member names from the page
    When the user extracts the member names from the excel sheet
    Then the member names on the page should match the excel
    And validate final amount to pay
    Then capture amount to pay
    Then calculate expected final amount
    Then validate cart amount to pay
    When click pay online button
    Then extract razorpay amount
    Then validate razorpay amount against expected amount
    Then validate Razorpay payment options based on amount
    Then initiate upi payment if allowed
    And click the go to orders button
    And click the view button in orders page
    And click on cancel order button in orders page
    And validate the tests in shows admin approval pending state

  @cancelLabOrderNonMember
  Scenario: Place a lab visit for a single non member and complete online payment with cancel order flow(Non Member)
    Given load the excel data for single non member and lab visit
    When the user enters otp
    And click on the submit button
    Then the location should be auto-detected on the dashboard
    And find whether the account holder is member or non member
    And verify whether the already selected tests are retained in the cart after login
    And click the Best Seller View All button
    And load test names from Excel
    And select tests and capture individual prices
    Then All tests should complete successfully
    When click on the cart icon
    And set the visit type from UI for lab visit
    Then validate checkout summary header values
    And select the member
  # And extract the total amount during checkout for the multi-member scenario
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
    When click pay online button
    Then extract razorpay amount
    Then validate razorpay amount against expected amount
    Then validate Razorpay payment options based on amount
    Then initiate upi payment if allowed
    And click the go to orders button
    And click the view button in orders page
    And click on cancel order button in orders page
    And validate the tests in shows admin approval pending state

  @rescheduleHomeOrderNonMember
  Scenario: Place a home sample visit for a single  non member and complete online payment  with cancel order flow(Non Member)
    Given load the excel data for single non member and home collection
    When the user enters otp
    And click on the submit button
    Then the location should be auto-detected on the dashboard
    And find whether the account holder is member or non member
    And verify whether the already selected tests are retained in the cart after login
    And click the Best Seller View All button
    And load test names from Excel
    And select tests and capture individual prices
  # And execute all tests
    Then All tests should complete successfully
    When click on the cart icon
    And set the visit type from UI for home collection
    Then validate checkout summary header values
    And select the member
    And select the home location
    And select the slot
    And validate the actual price against the checkout price
    When the user extract the member names from the page
    When the user extracts the member names from the excel sheet
    Then the member names on the page should match the excel
    And validate final amount to pay
    And validate final amount to pay with the Mr yoda club save value
  # And click the pay online button
    Then capture amount to pay
    Then calculate expected final amount
    Then validate cart amount to pay
    When click pay online button
    Then extract razorpay amount
    Then validate razorpay amount against expected amount
    Then validate Razorpay payment options based on amount
    Then initiate upi payment if allowed
    And click the go to orders button
    And click the view button in orders page
    And click on cancel order button in orders page
    And validate the tests in shows admin approval pending state

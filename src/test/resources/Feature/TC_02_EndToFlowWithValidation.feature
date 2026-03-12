@diagnostics
Feature: Place lab orders and pay online

  Background:
    Given the user is on the login page

  @endtoend_multi_lab_order_membership
  Scenario: Place a lab visit for a family member with the profile owner and complete online payment in most popular packages(Member)
    Given load the excel data for multi member and lab visit with memership
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
    When click pay online button
    Then extract razorpay amount
    Then validate razorpay amount against expected amount
    Then validate Razorpay payment options based on amount
    Then initiate upi payment if allowed
    And click the go to orders button
    And click the view button in orders page

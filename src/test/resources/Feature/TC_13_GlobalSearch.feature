# Placing lab orders and home samples and paying online for single member and non member from global search
@diagnostics @completeGlobalsearchFlow
Feature: Place lab orders and pay online from global search

  Background:
    Given the user is on the login page

  @singleLabOrderMembership @completeLoginFlow
  Scenario: Place a lab visit for a single member and complete online payment  in most popular packages
    Given load the excel data for single member and lab visit
    When the user enters otp
    And click on the submit button
    And find whether the account holder is member or non member
    And verify whether the already selected tests are retained in the cart after login
    And click the global search
    And load test names from Excel
    And select tests and capture individual prices from global search
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

  @singleHomeOrderMembershipp @completeLoginFlow
  Scenario: Place a home sample visit for a single member and complete online payment  in most popular packages
    Given load the excel data for single member and home collection
    When the user enters otp
    And click on the submit button
    And find whether the account holder is member or non member
    And verify whether the already selected tests are retained in the cart after login
    And click the global search
    And load test names from Excel
    And select tests and capture individual prices from global search
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

  @singleLabOrderWithoutMembershipp @completeLoginFlow
  Scenario: Place a lab visit for a single non member and complete online payment  in most popular packages
    Given load the excel data for single non member and lab visit
    When the user enters otp
    And click on the submit button
    And find whether the account holder is member or non member
    And verify whether the already selected tests are retained in the cart after login
    And click the global search
    And load test names from Excel
    And select tests and capture individual prices from global search
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

  @singleHomeOrderWithoutMembership @completeLoginFlow
  Scenario: Place a home sample visit for a single  non member and complete online payment  in most popular packages
    Given load the excel data for single non member and home collection
    When the user enters otp
    And click on the submit button
    And find whether the account holder is member or non member
    And verify whether the already selected tests are retained in the cart after login
    And click the global search
    And load test names from Excel
    And select tests and capture individual prices from global search
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

  @add_member_multi_lab_order_membershipp
  Scenario: Place a lab visit for a family member with the profile owner and complete online payment in most popular packages(Member)
    Given load the excel data for multi member and lab visit with memership
    When the user enters otp
    And click on the submit button
    And find whether the account holder is member or non member
    And verify whether the already selected tests are retained in the cart after login
    And click the global search
    And load test names from Excel
    And select tests and capture individual prices from global search
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
    When click pay online button
    Then extract razorpay amount
    Then validate razorpay amount against expected amount
    Then validate Razorpay payment options based on amount
    Then initiate upi payment if allowed
    And click the go to orders button
    And click the view button in orders page

  @add_member_multi_home_order_membershipp
  Scenario: Place a home visit for a family member with the profile owner and complete online payment in most popular packages(Member)
    Given load the excel data for multi member and home collection with memership
    When the user enters otp
    And click on the submit button
    And find whether the account holder is member or non member
    And verify whether the already selected tests are retained in the cart after login
    And click the global search
    And load test names from Excel
    And select tests and capture individual prices from global search
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
    When click pay online button
    Then extract razorpay amount
    Then validate razorpay amount against expected amount
    Then validate Razorpay payment options based on amount
    Then initiate upi payment if allowed
    And click the go to orders button
    And click the view button in orders page

  @add_member_multi_lab_order_without_membershipp
  Scenario: Place a lab visit for a family member with the profile owner and complete online payment in most popular packages(Without Member)
    Given load the excel data for multi member and lab visit without memership
    When the user enters otp
    And click on the submit button
    Then the location should be auto-detected on the dashboard
    And find whether the account holder is member or non member
    And verify whether the already selected tests are retained in the cart after login
    And click the global search
    And load test names from Excel
    And select tests and capture individual prices from global search
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

  @add_member_multi_home_order_without_membershipp
  Scenario: Place a home visit for a family member with the profile owner and complete online payment in most popular packages(without Member)
    Given load the excel data for multi member and home collection without memership
    When the user enters otp
    And click on the submit button
    Then the location should be auto-detected on the dashboard
    And find whether the account holder is member or non member
    And verify whether the already selected tests are retained in the cart after login
    And click the global search
    And load test names from Excel
    And select tests and capture individual prices from global search
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
    When click pay online button
    Then extract razorpay amount
    Then validate razorpay amount against expected amount
    Then validate Razorpay payment options based on amount
    Then initiate upi payment if allowed
    And click the go to orders button
    And click the view button in orders page

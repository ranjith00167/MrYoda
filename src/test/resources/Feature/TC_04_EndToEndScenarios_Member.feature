  @completescenario
Feature: Place lab orders and pay online for multiple scenarios of members and tests

  Background:
    Given the user is on the login page

  @enrollmentWithMembership
  Scenario: User logs into the application
    Given load the excel data for multi member and lab visit with memership
    When the user enters otp
    And click on the submit button

  @location_detectWithMembership
  Scenario: Location Auto-Detect on Dashboard
    Given load the excel data for multi member and lab visit with memership
    When the user enters otp
    And click on the submit button
    Then the location should be auto-detected on the dashboard

  @membershipWithMembership
  Scenario: Identify membership type
    Given load the excel data for multi member and lab visit with memership
    When the user enters otp
    And click on the submit button
    And find whether the account holder is member or non member

  @select_testsWithMembership
  Scenario: Select tests and capture individual prices
    Given load the excel data for multi member and lab visit with memership
    When the user enters otp
    And click on the submit button
    And find whether the account holder is member or non member
    And click the Best Seller View All button
    And load test names from Excel
    And select tests and capture individual prices
    Then All tests should complete successfully

  @cartWithMembership
  Scenario: Validating the visit type
    Given load the excel data for multi member and lab visit with memership
    When the user enters otp
    And click on the submit button
    And find whether the account holder is member or non member
    And click the Best Seller View All button
    And load test names from Excel
    And select tests and capture individual prices
    Then All tests should complete successfully
    When click on the cart icon
    And set the visit type from UI for lab visit

  @checkout_headerWithMembership
  Scenario: Validate checkout summary header values with individual prices
    Given load the excel data for multi member and lab visit with memership
    When the user enters otp
    And click on the submit button
    And find whether the account holder is member or non member
    And click the Best Seller View All button
    And load test names from Excel
    And select tests and capture individual prices
    Then All tests should complete successfully
    When click on the cart icon
    And set the visit type from UI for lab visit
    Then validate checkout summary header values

  @multi_memberWithMembership
  Scenario: Select the member for multi member scenario
    Given load the excel data for multi member and lab visit with memership
    When the user enters otp
    And click on the submit button
    And find whether the account holder is member or non member
    And click the Best Seller View All button
    And load test names from Excel
    And select tests and capture individual prices
    Then All tests should complete successfully
    When click on the cart icon
    And set the visit type from UI for lab visit
    Then validate checkout summary header values
    And select the member for multi member scenario

  @multi_member_totalWithMembership
  Scenario: Extract total amount for multi member scenario
    Given load the excel data for multi member and lab visit with memership
    When the user enters otp
    And click on the submit button
    And find whether the account holder is member or non member
    And click the Best Seller View All button
    And load test names from Excel
    And select tests and capture individual prices
    Then All tests should complete successfully
    When click on the cart icon
    And set the visit type from UI for lab visit
    Then validate checkout summary header values
    And select the member for multi member scenario
    And extract the total amount during checkout for the multi-member scenario

  @final_amountWithMembership
  Scenario: Validate actual amount with add member page total amount
    Given load the excel data for multi member and lab visit with memership
    When the user enters otp
    And click on the submit button
    And find whether the account holder is member or non member
    And click the Best Seller View All button
    And load test names from Excel
    And select tests and capture individual prices
    And click on the cart icon
    And set the visit type from UI for lab visit
    Then validate checkout summary header values
    And select the member for multi member scenario
  # And extracting the total amount while checkout for multi member scenario
    And extract the total amount during checkout for the multi-member scenario
    And select the location
    And select the slot
    And validate the actual price against the checkout price
      When the user extract the member names from the page
    When the user extracts the member names from the excel sheet
    Then the member names on the page should match the excel
    And validate final amount to pay

  @payment
  Scenario: Complete online paymentWithMembership
    Given load the excel data for multi member and lab visit with memership
    When the user enters otp
    And click on the submit button
    And find whether the account holder is member or non member
    And click the Best Seller View All button
    And load test names from Excel
    And select tests and capture individual prices
    And click on the cart icon
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
   # And click the pay online button
    Then capture amount to pay
    Then calculate expected final amount
    Then validate cart amount to pay
    When click pay online button
    Then extract razorpay amount
    Then validate razorpay amount against expected amount
    Then validate Razorpay payment options based on amount
    Then initiate upi payment if allowed

  @order_view
  Scenario: View the order after paymentWithMembership
    Given load the excel data for multi member and lab visit with memership
    When the user enters otp
    And click on the submit button
    And find whether the account holder is member or non member
    And click the Best Seller View All button
    And load test names from Excel
    And select tests and capture individual prices
    And click on the cart icon
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

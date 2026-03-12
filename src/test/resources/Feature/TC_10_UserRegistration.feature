@diagnostics @newUserRegistrationCompleteflow
Feature: New User registration

  Background:
    Given the user is on the login page

  @NewUserRegistrationNew
  Scenario: Place a lab visit for a single member and complete online payment  in most popular packages
    Given load the excel data for single member and lab visit for a new user
    When create an account with random mobile number
    And click on the submit button
    Then the location should be auto-detected on the dashboard
    And find whether the account holder is member or non member
    And verify whether the already selected tests are retained in the cart after login
    # Login via API immediately after UI login to establish token
    And login via API and refresh auth token
    And click on the profile icon
    Then validate whether the user is a new user by checking the presence of welcome message
    When click the profile registration icon
    When the user selects title from the dropdown
    And the user enters first name in registration page
    And the user enters middle name in registration page
    And the user enters last name  in registration page
    And the user selects gender
    And the user enters date of birth  in registration page
    And the user clicks on submit button
    Then the new member should be added successfully
    When the user clicks on the profile icon
    Then validate the profile name matches with the registered name
    Then validate the age should be correct based on DOB entered
    Then validate the gender should be correct based on selection
    Then validate the mobile number should be correct based on registration
  # Add address
    And the user navigates to view profile page
    When the user clicks on add address button in profile
    And the user enters complete address
    When the user clicks save button for address
  # Then validate success message appears for address save
    And validate address is displayed in profile address list
    # Order placing
    And click the Best Seller View All button
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
    Then verify cart details via getCartById API for member
    # ── UI : Payment via Razorpay ───────────────────────────────────────────────
    When click pay online button
    Then extract razorpay amount
    Then validate razorpay amount against expected amount
    Then validate Razorpay payment options based on amount
    Then initiate upi payment if allowed
    # ── API : Verify Order via getOrderById ─────────────────────────────────────
    And extract auth token from browser local storage
    And capture order id from browser after payment
    Then fetch order by id via API and assert status is confirmed
    # ── UI : Confirm order on screen ────────────────────────────────────────────
    And click the go to orders button
    And click the view button in orders page

  @NewUserRegistrationUpdate
  Scenario: Place a lab visit for a single member and complete online payment  in most popular packages
    Given load the excel data for single member and lab visit for a new user
    When create an account with random mobile number
    And click on the submit button
    Then the location should be auto-detected on the dashboard
    And find whether the account holder is member or non member
    And verify whether the already selected tests are retained in the cart after login
    And click on the profile icon
    Then validate whether the user is a new user by checking the presence of welcome message
    When click the profile registration icon
    When the user selects title from the dropdown
    And the user enters first name in registration page
    And the user enters middle name in registration page
    And the user enters last name  in registration page
    And the user selects gender
    And the user enters date of birth  in registration page
    And the user clicks on submit button
    Then the new member should be added successfully
    When the user clicks on the profile icon
    Then validate the profile name matches with the registered name
    Then validate the age should be correct based on DOB entered
    Then validate the gender should be correct based on selection
    Then validate the mobile number should be correct based on registration
    And the user navigates to view profile page
    When the user clicks on edit profile button
    When the user updates first name in registration page
    And the user updates middle name in registration page
    And the user updates last name  in registration page
    And the user enters date of birth  in registration page
    And the user selects gender
    And the user clicks on save button
    Then the member details should be updated successfully
    Then validate the profile name matches with the updated name

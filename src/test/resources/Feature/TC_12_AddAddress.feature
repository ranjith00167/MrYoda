@diagnostics @profile @user_address @AddAddressCompleteFlow
Feature: User Address Management in Profile Page

  Background:
    Given the user is on the login page

  # ============================================
  # NEW USER REGISTRATION & ADDRESS ADDITION
  # ============================================
  @NewUserRegistration @registration_with_address
  Scenario: Place a lab visit for a single member and complete online payment in most popular packages
    # Step 1: Create new account
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
    # Step 4: Add address for newly registered user (Required for lab visit booking)
    When the user clicks on the profile icon
    And the user navigates to view profile page
  # Then validate user is on my addresses page
    When the user clicks on add address button in profile
  # Then validate add address form is displayed
  # And validate map is displayed
    When the user enters complete address
    When the user clicks save button for address
  # Then validate success message appears for address save
    And validate address is displayed in profile address list

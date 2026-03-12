@FamilyAndFriends @Regression @login
Feature: Add Family and Friends Member
  As a registered Mr. Yoda user
  I want to add family members and friends to my account
  So that I can manage their health records and book tests for them

  Background:
    Given the user is on the login page
    When the user enters otp
    And click on the submit button
    And find whether the account holder is member or non member
    And the user navigates to Family and Friends page

  @Smoke @Positive
  Scenario: Successfully add a new family member with all mandatory fields
    Given load the excel data for single member and lab visit for a new user
    When the user selects title from the dropdown
    And the user enters first name
    And the user enters last name
    And the user selects country code
    And the user enters mobile number
    And the user selects gender
    And the user selects relation
    And the user enters date of birth
    And the user clicks on Save button
    Then the new member should be added successfully
    And the user should see the member in the family list
    And a success message should be displayed

  @Positive
  Scenario: Add a new family member with all fields including optional fields
    Given the user clicks on Add New Member button
    When the user uploads profile photo
    And the user selects title from the dropdown
    And the user enters first name
    And the user enters middle name
    And the user enters last name
    And the user selects country code
    And the user enters mobile number
    And the user selects gender
    And the user selects relation
    And the user enters date of birth
    And the user enters email
    And the user clicks on Save button
    Then the new member should be added successfully
    And the member card should display the profile photo
    And the user should see complete member details in the list

  @Positive
  Scenario: Add multiple family members with unique names
    Given the user clicks on Add New Member button
    When the user fills the form with randomly generated unique name
    And the user enters all mandatory fields with valid data
    And the user clicks on Save button
    Then the new member should be added successfully
    And the user clicks on Add New Member button again
    When the user fills the form with another randomly generated unique name
    And the user enters all mandatory fields with valid data
    And the user clicks on Save button
    Then the second member should be added successfully
    And both members should be visible in the family list

  @Negative
  Scenario: Attempt to add member without mandatory fields
    Given the user clicks on Add New Member button
    When the user leaves first name field empty
    And the user leaves last name field empty
    And the user clicks on Save button
    Then the form should not be submitted
    And validation error for first name should be displayed
    And validation error for last name should be displayed

  @Negative
  Scenario: Validate mobile number field with invalid data
    Given the user clicks on Add New Member button
    When the user fills all mandatory fields except mobile number
    And the user enters invalid mobile number
    And the user clicks on Save button
    Then the form should not be submitted
    And validation error for mobile number should be displayed

  @Negative
  Scenario: Attempt to add member with invalid email format
    Given the user clicks on Add New Member button
    When the user fills all mandatory fields
    And the user enters invalid email
    And the user clicks on Save button
    Then the form should not be submitted
    And validation error for email should be displayed

  @Positive
  Scenario: Cancel adding a new member
    Given the user clicks on Add New Member button
    When the user enters first name
    And the user enters last name
    And the user clicks on Cancel button
    Then the add member form should be closed
    And the member should not be added to the list
    And the user should remain on Family and Friends page

  @Positive
  Scenario: Verify added member appears in the member list
    Given the user has successfully added a member
    When the user views the Family and Friends page
    Then the member should be visible in the list
    And the member card should display correct name
    And the member card should display correct relation
    And the member card should display correct age and gender

  @Positive
  Scenario: Search for added member using search functionality
    Given the user has multiple members in the family list
    When the user enters member name in the search field
    Then only matching members should be displayed
    And other members should be filtered out

  @Positive
  Scenario: View all family members using See All link
    Given the user has more than three members in the family list
    When the user clicks on See All link
    Then all family members should be displayed
    And the complete list should be visible

  @Positive
  Scenario: Add family members with different relations
    Given the user clicks on Add New Member button
    When the user enters first name
    And the user enters last name
    And the user enters mobile number
    And the user selects gender
    And the user selects relation
    And the user enters date of birth
    And the user clicks on Save button
    Then the member should be added with correct relation

  @Boundary
  Scenario: Validate date of birth field with boundary values
    Given the user clicks on Add New Member button
    When the user enters minimum allowed date of birth
    Then the date should be accepted
    When the user enters maximum allowed date of birth
    Then the date should be accepted
    When the user enters invalid date
    Then validation error for date should be displayed

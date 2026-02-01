Feature: COD Flow Sample Lifecycle

  @ITDose
  Scenario: Process COD order sample through full lab workflow
    Given I am on the login page
    When I enter valid credentials
    Then I should be logged in successfully
    When I click on the Department button
    And I click on the Laboratory button
    And I click on the list view button
    And I click on the sample management button
    And I click on the sample collection button
   # And I select the laboratory name
    And I select the search option
    And I click on the visit number
    And I enter the visit number
    And I click on the search button
    And I extract the SIN NO from the UI
    And I click on the view icon
    #And I click on the select checkbox
    And I select the sample type
    And I click on the collect button
    And I click on the list view button
    And I click on the sample management button
    And I click on the sample receive area
    And I enter the SIN NO in the input box
    And I click on the search button
    And I click on the select checkbox
    And I click on the save button
    And I click on the list view button
    And I click on the sample management button
    And I click on the Department receive button
    And I select the SIN NO in the dropdown
    And I enter the SIN NO in the input box in the department receive area
    And I click on the search button
    And I click on the sample receive checkbox
    And I click on the receive button
    And I click on the list view button
    And I click on the sample processing button
    And I click on the result entry button
    And I enter the SIN NO in the input box in the department receive area
    And I click on the search button
    And I click on the visit number
    And I enter the value of the tests
    And I click on the approve button

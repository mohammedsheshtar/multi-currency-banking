Feature: Currency Management
  As a bank administrator
  I want to manage currencies
  So that I can support multiple currencies in the system

  Scenario: Create a new currency
    Given I am an authenticated user
    When I send a POST request to "/api/currencies" with the following data:
      """
      {
        "countryCode": "GB",
        "symbol": "£"
      }
      """
    Then the response status code should be 201
    And the response should contain the currency details

  Scenario: Get all currencies
    Given I am an authenticated user
    And the following currencies exist:
      | countryCode | symbol |
      | US         | $      |
      | GB         | £      |
      | EU         | €      |
    When I send a GET request to "/api/currencies"
    Then the response status code should be 200
    And the response should contain 3 currencies

  Scenario: Get currency by country code
    Given I am an authenticated user
    And a currency with country code "US" exists
    When I send a GET request to "/api/currencies/US"
    Then the response status code should be 200
    And the response should contain the currency details for "US" 
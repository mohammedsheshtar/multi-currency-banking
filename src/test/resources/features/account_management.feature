Feature: Account Management
  As a bank user
  I want to manage my accounts
  So that I can handle my finances in different currencies

  Scenario: Create a new account
    Given I am an authenticated user
    When I send a POST request to "/api/v1/users/accounts" with the following data:
      """
      {
        "initialBalance": 1000.00,
        "countryCode": "US",
        "accountType": "SAVINGS"
      }
      """
    Then the response status code should be 200
    And the response should contain the account details

  Scenario: List user accounts
    Given I am an authenticated user
    And I have the following accounts:
      | accountNumber | balance | countryCode | accountType |
      | ACC001       | 1000.00 | US          | SAVINGS     |
      | ACC002       | 2000.00 | GB          | CHECKING    |
    When I send a GET request to "/api/v1/users/accounts"
    Then the response status code should be 200
    And the response should contain 2 accounts

  Scenario: Close an account
    Given I am an authenticated user
    And I have an account with number "ACC001"
    When I send a POST request to "/api/v1/users/accounts/ACC001"
    Then the response status code should be 200
    And the account should be closed 
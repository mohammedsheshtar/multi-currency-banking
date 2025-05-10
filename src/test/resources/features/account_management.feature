Feature: Account Management
  As a bank user
  I want to manage my accounts
  So that I can perform banking operations

  Scenario: Create a new account
    Given I am an authenticated user
    When I send a POST request to "/api/accounts" with the following data:
      """
      {
        "currency": "USD",
        "initialBalance": 1000.00
      }
      """
    Then the response status code should be 201
    And the response should contain the account details

  Scenario: Get account balance
    Given I am an authenticated user
    And I have an existing account
    When I send a GET request to "/api/accounts/{accountId}/balance"
    Then the response status code should be 200
    And the response should contain the current balance 
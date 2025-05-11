Feature: Transaction Management
  As a bank user
  I want to perform transactions
  So that I can manage my money

  Scenario: Deposit money into account
    Given I am an authenticated user
    And I have an account with number "ACC001"
    When I send a POST request to "/api/v1/accounts/deposit" with the following data:
      """
      {
        "accountNumber": "ACC001",
        "countryCode": "US",
        "amount": 500.00
      }
      """
    Then the response status code should be 200
    And the response should contain the deposit details

  Scenario: Withdraw money from account
    Given I am an authenticated user
    And I have an account with number "ACC001" and balance 1000.00
    When I send a POST request to "/api/v1/accounts/withdraw" with the following data:
      """
      {
        "accountNumber": "ACC001",
        "countryCode": "US",
        "amount": 200.00
      }
      """
    Then the response status code should be 200
    And the response should contain the withdrawal details

  Scenario: Transfer money between accounts
    Given I am an authenticated user
    And I have an account with number "ACC001" and balance 1000.00
    And I have an account with number "ACC002" and balance 500.00
    When I send a POST request to "/api/v1/accounts/transfer" with the following data:
      """
      {
        "sourceAccount": "ACC001",
        "destinationAccount": "ACC002",
        "amount": 300.00,
        "countryCode": "US"
      }
      """
    Then the response status code should be 200
    And the response should contain the transfer details

  Scenario: View transaction history
    Given I am an authenticated user
    And I have an account with number "ACC001"
    And I have performed some transactions on account "ACC001"
    When I send a GET request to "/api/v1/accounts/transactions/1"
    Then the response status code should be 200
    And the response should contain the transaction history 
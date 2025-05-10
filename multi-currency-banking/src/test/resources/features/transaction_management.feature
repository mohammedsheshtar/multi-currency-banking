Feature: Transaction Management
  As a bank user
  I want to perform transactions
  So that I can manage my money across different currencies

  Background:
    Given I am an authenticated user
    And I have a source account with balance 1000.00 USD
    And I have a destination account with balance 500.00 EUR

  Scenario: Transfer money between accounts
    When I send a POST request to "/api/transactions/transfer" with the following data:
      """
      {
        "sourceAccountNumber": "USD123456",
        "destinationAccountNumber": "EUR789012",
        "amount": 100.00,
        "currency": "USD"
      }
      """
    Then the response status code should be 200
    And the transaction should be completed successfully
    And the source account balance should be 900.00 USD
    And the destination account balance should be 500.00 EUR

  Scenario: Transfer with insufficient funds
    When I send a POST request to "/api/transactions/transfer" with the following data:
      """
      {
        "sourceAccountNumber": "USD123456",
        "destinationAccountNumber": "EUR789012",
        "amount": 1500.00,
        "currency": "USD"
      }
      """
    Then the response status code should be 400
    And the response should contain insufficient funds error

  Scenario: Transfer with invalid currency
    When I send a POST request to "/api/transactions/transfer" with the following data:
      """
      {
        "sourceAccountNumber": "USD123456",
        "destinationAccountNumber": "EUR789012",
        "amount": 100.00,
        "currency": "INVALID"
      }
      """
    Then the response status code should be 400
    And the response should contain invalid currency error

  Scenario: Get transaction history
    Given the following transactions exist:
      | sourceAccount | destinationAccount | amount | currency | status    |
      | USD123456    | EUR789012         | 100.00 | USD      | COMPLETED |
      | EUR789012    | USD123456         | 50.00  | EUR      | COMPLETED |
    When I send a GET request to "/api/transactions/history/USD123456"
    Then the response status code should be 200
    And the response should contain 2 transactions 
Feature: Transaction History
  As a bank user
  I want to view my account transaction history
  So that I can track my financial activity

  Background:
    Given I am an authenticated user
    And I have an account with transactions

  Scenario: Get transaction history for my account
    When I GET "/api/v1/accounts/transactions/{accountId}"
    Then the response status should be 200
    And the response should contain a list of transactions 
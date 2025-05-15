Feature: Shop Transaction Management
  As a bank user
  I want to view my shop transactions
  So that I can track my shopping activity

  Background:
    Given I am an authenticated user
    And I have an account with shop transactions

  Scenario: Get shop transaction history for my account
    When I GET "/api/v1/accounts/shop-transactions/{accountId}"
    Then the response status should be 200
    And the response should contain a list of shop transactions
    And each shop transaction should have required fields 
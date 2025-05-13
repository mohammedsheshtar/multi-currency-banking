Feature: Shop Management
  As a bank user
  I want to buy items from the shop
  So that I can use my account balance for purchases

  Background:
    Given I am an authenticated user
    And I have an account with sufficient balance

  Scenario: Buy an item from the shop
    When I POST "/api/v1/shop/buy" with the following data:
      | itemId | quantity |
      | 1      | 2        |
    Then the response status should be 200
    And the response should contain purchase details
    And my account balance should be updated

  Scenario: Try to buy item with insufficient balance
    When I POST "/api/v1/shop/buy" with the following data:
      | itemId | quantity |
      | 1      | 999      |
    Then the response status should be 400
    And the response should contain error message about insufficient balance 
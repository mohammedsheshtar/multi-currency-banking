Feature: Account management
  Scenario: List my accounts
    Given I am an authenticated user
    When I GET "/api/v1/users/accounts"
    Then the response status should be 200

  Scenario: Get account transaction history
    Given I am an authenticated user
    And I have a valid accountId
    When I GET "/api/v1/accounts/transactions/{accountId}"
    Then the response status should be 200

  Scenario: Close an account
    Given I am an authenticated user
    And I have a valid accountNumber
    When I POST "/api/v1/users/accounts/{accountNumber}"
    Then the response status should be 200 
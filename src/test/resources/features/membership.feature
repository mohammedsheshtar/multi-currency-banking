Feature: Membership Management
  As a bank user
  I want to manage my membership
  So that I can view my membership details

  Background:
    Given I am an authenticated user

  Scenario: Get my membership details
    When I GET "/api/v1/users/membership"
    Then the response status should be 200
    And the response should contain membership details

  Scenario: Get membership benefits
    When I GET "/api/v1/users/membership/benefits"
    Then the response status should be 200
    And the response should contain membership benefits

  Scenario: Get membership status
    When I GET "/api/v1/users/membership/status"
    Then the response status should be 200
    And the response should contain membership status 
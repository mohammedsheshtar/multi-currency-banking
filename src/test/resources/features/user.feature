Feature: User KYC management
  Scenario: Get my KYC
    Given I am an authenticated user
    When I GET "/api/v1/users/kyc"
    Then the response status should be 200

  Scenario: Create or update my KYC
    Given I am an authenticated user
    When I POST "/api/v1/users/kyc" with valid KYC data
    Then the response status should be 200 
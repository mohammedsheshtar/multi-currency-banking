Feature: KYC Management
  As a bank user
  I want to manage my KYC information
  So that I can verify my identity with the bank

  Scenario: Submit KYC information
    Given I am an authenticated user
    When I send a POST request to "/api/v1/users/kyc" with the following data:
      """
      {
        "firstName": "John",
        "lastName": "Doe",
        "dateOfBirth": "1990-01-01",
        "civilId": "123456789",
        "country": "US",
        "phoneNumber": "+1234567890",
        "homeAddress": "123 Main St",
        "salary": 5000.00
      }
      """
    Then the response status code should be 200
    And the response should contain the KYC details

  Scenario: View my KYC information
    Given I am an authenticated user
    And I have submitted my KYC information
    When I send a GET request to "/api/v1/users/kyc"
    Then the response status code should be 200
    And the response should contain my KYC details 
Feature: KYC Management
  As a bank administrator
  I want to manage customer KYC information
  So that I can verify customer identity and comply with regulations

  Background:
    Given I am an authenticated user
    And I have a registered user with username "testuser"

  Scenario: Submit KYC information successfully
    When I send a POST request to "/api/kyc" with the following data:
      """
      {
        "firstName": "John",
        "lastName": "Doe",
        "country": "US",
        "dateOfBirth": "1990-01-01",
        "civilId": "123456789",
        "phoneNumber": "+1234567890",
        "homeAddress": "123 Main St, City, Country"
      }
      """
    Then the response status code should be 201
    And the KYC information should be saved successfully

  Scenario: Submit KYC with invalid date of birth
    When I send a POST request to "/api/kyc" with the following data:
      """
      {
        "firstName": "John",
        "lastName": "Doe",
        "country": "US",
        "dateOfBirth": "invalid-date",
        "civilId": "123456789",
        "phoneNumber": "+1234567890",
        "homeAddress": "123 Main St, City, Country"
      }
      """
    Then the response status code should be 400
    And the response should contain date of birth validation error

  Scenario: Submit KYC with invalid phone number
    When I send a POST request to "/api/kyc" with the following data:
      """
      {
        "firstName": "John",
        "lastName": "Doe",
        "country": "US",
        "dateOfBirth": "1990-01-01",
        "civilId": "123456789",
        "phoneNumber": "invalid-phone",
        "homeAddress": "123 Main St, City, Country"
      }
      """
    Then the response status code should be 400
    And the response should contain phone number validation error

  Scenario: Get KYC information
    Given KYC information exists for user "testuser"
    When I send a GET request to "/api/kyc/testuser"
    Then the response status code should be 200
    And the response should contain the KYC details 
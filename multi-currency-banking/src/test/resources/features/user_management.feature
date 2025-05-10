Feature: User Management
  As a new user
  I want to register and authenticate
  So that I can access the banking services

  Scenario: Register a new user successfully
    When I send a POST request to "/users/register" with the following data:
      """
      {
        "username": "newuser",
        "password": "Password123"
      }
      """
    Then the response status code should be 200
    And the user should be registered successfully

  Scenario: Register a user with invalid password
    When I send a POST request to "/users/register" with the following data:
      """
      {
        "username": "newuser",
        "password": "weak"
      }
      """
    Then the response status code should be 400
    And the response should contain password validation error

  Scenario: Register a user with existing username
    Given a user with username "existinguser" exists
    When I send a POST request to "/users/register" with the following data:
      """
      {
        "username": "existinguser",
        "password": "Password123"
      }
      """
    Then the response status code should be 400
    And the response should contain username already exists error 
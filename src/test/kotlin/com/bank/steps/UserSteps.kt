package com.bank.steps

import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import io.cucumber.java.en.Then
import io.cucumber.java.en.And
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions
import com.bank.user.UserRepository
import com.bank.user.UserEntity
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
class UserSteps {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private var response: String? = null

    @When("I send a POST request to {string} with the following data:")
    fun iSendAPostRequest(endpoint: String, requestBody: String) {
        val result = mockMvc.perform(
            MockMvcRequestBuilders.post(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        ).andReturn()
        
        CommonSteps.lastStatusCode = result.response.status
        response = result.response.contentAsString
    }

    @And("the user should be registered successfully")
    fun theUserShouldBeRegisteredSuccessfully() {
        val responseMap = objectMapper.readValue(response, Map::class.java)
        Assertions.assertTrue(responseMap.isEmpty())
        
        // Verify user exists in database
        val user = userRepository.findByUsername("newuser")
        Assertions.assertNotNull(user)
        Assertions.assertTrue(passwordEncoder.matches("Password123", user!!.password))
    }

    @And("the response should contain password validation error")
    fun theResponseShouldContainPasswordValidationError() {
        val responseMap = objectMapper.readValue(response, Map::class.java)
        Assertions.assertTrue(responseMap.containsKey("error"))
        Assertions.assertTrue((responseMap["error"] as String).contains("password"))
    }

    @Given("a user with username {string} exists")
    fun aUserWithUsernameExists(username: String) {
        userRepository.save(
            UserEntity(
                username = username,
                password = passwordEncoder.encode("Password123"),
                createdAt = LocalDateTime.now()
            )
        )
    }

    @And("the response should contain username already exists error")
    fun theResponseShouldContainUsernameAlreadyExistsError() {
        val responseMap = objectMapper.readValue(response, Map::class.java)
        Assertions.assertTrue(responseMap.containsKey("error"))
        Assertions.assertTrue((responseMap["error"] as String).contains("username"))
    }
} 
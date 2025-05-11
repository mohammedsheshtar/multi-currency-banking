package com.bank.steps

import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import io.cucumber.java.en.Then
import io.cucumber.java.en.And
import org.junit.jupiter.api.Assertions
import org.springframework.beans.factory.annotation.Autowired
import com.bank.user.UserRepository
import com.bank.user.UserEntity
import com.bank.authentication.jwt.JwtService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import com.fasterxml.jackson.databind.ObjectMapper
import com.bank.account.AccountRepository
import com.bank.account.AccountEntity
import com.bank.currency.CurrencyRepository
import java.math.BigDecimal

class CommonSteps {

    companion object {
        var lastStatusCode: Int = 0
        var authToken: String? = null
        var response: String? = null
    }

    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var jwtService: JwtService
    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder
    @Autowired
    private lateinit var mockMvc: MockMvc
    @Autowired
    private lateinit var objectMapper: ObjectMapper
    @Autowired
    private lateinit var accountRepository: AccountRepository
    @Autowired
    private lateinit var currencyRepository: CurrencyRepository

    @Given("I am an authenticated user")
    fun iAmAnAuthenticatedUser() {
        val testUser = userRepository.findByUsername("testuser") ?: run {
            userRepository.save(
                UserEntity(
                    username = "testuser",
                    password = passwordEncoder.encode("testpass")
                )
            )
        }
        authToken = jwtService.generateToken(testUser.username)
    }

    @And("I have an account with number {string}")
    fun iHaveAnAccountWithNumber(accountNumber: String) {
        val user = userRepository.findByUsername("testuser")
            ?: throw IllegalStateException("Test user not found")
        val currency = currencyRepository.findByCountryCode("US")
            ?: throw IllegalStateException("Currency not found")

        accountRepository.save(
            AccountEntity(
                user = user,
                balance = BigDecimal("1000.000"),
                currency = currency,
                isActive = true,
                accountNumber = accountNumber,
                accountType = "SAVINGS"
            )
        )
    }

    @When("I send a POST request to {string} with the following data:")
    fun iSendAPostRequest(endpoint: String, requestBody: String) {
        val result = mockMvc.perform(
            MockMvcRequestBuilders.post(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer $authToken")
                .content(requestBody)
        ).andReturn()
        
        lastStatusCode = result.response.status
        response = result.response.contentAsString
    }

    @When("I send a GET request to {string}")
    fun iSendAGetRequest(endpoint: String) {
        val result = mockMvc.perform(
            MockMvcRequestBuilders.get(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer $authToken")
        ).andReturn()
        lastStatusCode = result.response.status
        response = result.response.contentAsString
    }

    @Then("the response status code should be {int}")
    fun theResponseStatusCodeShouldBe(statusCode: Int) {
        Assertions.assertEquals(statusCode, lastStatusCode)
    }
} 
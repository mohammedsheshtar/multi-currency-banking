package com.bank.Multi_currency_Banking

import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import io.cucumber.java.en.Then
import io.cucumber.java.en.And
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class AccountStepDefinitions {
    private val restTemplate = RestTemplate()
    private var response: ResponseEntity<String>? = null
    private var jwtToken: String = ""
    private var accountId: Long = 0
    private var accountNumber: String = ""

    @Given("I am an authenticated user")
    fun iAmAuthenticatedUser() {
        val registerBody = mapOf("username" to "testuser2", "password" to "testpass2")
        restTemplate.postForEntity("http://localhost:9000/api/v1/authentication/register", registerBody, String::class.java)
        val loginBody = mapOf("username" to "testuser2", "password" to "testpass2")
        val loginResponse = restTemplate.postForEntity("http://localhost:9000/api/v1/authentication/login", loginBody, Map::class.java)
        jwtToken = loginResponse.body?.get("token") as String
    }

    @When("I GET \"/api/v1/users/accounts\"")
    fun iGetUserAccounts() {
        val headers = HttpHeaders()
        headers["Authorization"] = "Bearer $jwtToken"
        val entity = HttpEntity<String>(headers)
        response = restTemplate.exchange("http://localhost:9000/api/v1/users/accounts", HttpMethod.GET, entity, String::class.java)
    }

    @And("I have a valid accountId")
    fun iHaveAValidAccountId() {
        // Create an account to get a valid accountId
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers["Authorization"] = "Bearer $jwtToken"
        val accountBody = mapOf(
            "initialBalance" to BigDecimal(0),
            "countryCode" to "KWD",
            "accountType" to "SAVINGS"
        )
        val entity = HttpEntity(accountBody, headers)
        val createResponse = restTemplate.postForEntity("http://localhost:9000/api/v1/users/accounts", entity, Map::class.java)
        accountId = (createResponse.body?.get("id") as? Number)?.toLong() ?: 1L
        accountNumber = createResponse.body?.get("accountNumber") as? String ?: ""
    }

    @When("I GET \"/api/v1/accounts/transactions/{accountId}\"")
    fun iGetAccountTransactions() {
        val headers = HttpHeaders()
        headers["Authorization"] = "Bearer $jwtToken"
        val entity = HttpEntity<String>(headers)
        response = restTemplate.exchange("http://localhost:9000/api/v1/accounts/transactions/$accountId", HttpMethod.GET, entity, String::class.java)
    }

    @And("I have a valid accountNumber")
    fun iHaveAValidAccountNumber() {
        if (accountNumber.isBlank()) {
            iHaveAValidAccountId() // fallback to create account if not already done
        }
    }

    @When("I POST \"/api/v1/users/accounts/{accountNumber}\"")
    fun iPostCloseAccount() {
        val headers = HttpHeaders()
        headers["Authorization"] = "Bearer $jwtToken"
        val entity = HttpEntity<String>(headers)
        response = restTemplate.exchange("http://localhost:9000/api/v1/users/accounts/$accountNumber", HttpMethod.POST, entity, String::class.java)
    }

    @Then("the response status should be 200")
    fun theResponseStatusShouldBe200() {
        assertEquals(200, response?.statusCodeValue)
    }
} 
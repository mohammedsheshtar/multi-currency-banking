package com.bank.Multi_currency_Banking

import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

class TransactionStepDefinitions {
    private val testContext = TestContext()
    private var accountId: String? = null
    private var response: ResponseEntity<*>? = null

    @Given("I am an authenticated user")
    fun iAmAuthenticatedUser() {
        // Already handled by TestContext init
    }

    @And("I have an account with transactions")
    fun iHaveAnAccountWithTransactions() {
        // Create an account
        val headers = testContext.getHeaders()
        val accountRequest = mapOf(
            "currency" to "USD"
        )
        val entity = HttpEntity(accountRequest, headers)
        val accountResponse = testContext.restTemplate.postForEntity(
            "${testContext.baseUrl}/api/v1/users/accounts",
            entity,
            Map::class.java
        )
        accountId = (accountResponse.body?.get("id") ?: accountResponse.body?.get("accountId"))?.toString()

        // Add a transaction (e.g., deposit)
        val transactionRequest = mapOf(
            "amount" to 100,
            "type" to "DEPOSIT"
        )
        val transactionEntity = HttpEntity(transactionRequest, headers)
        testContext.restTemplate.postForEntity(
            "${testContext.baseUrl}/api/v1/accounts/${accountId}/deposit",
            transactionEntity,
            Any::class.java
        )
    }

    @When("I GET \"/api/v1/accounts/transactions/{accountId}\"")
    fun iGetTransactions() {
        val headers = testContext.getHeaders()
        val entity = HttpEntity<Any>(headers)
        response = testContext.restTemplate.exchange(
            "${testContext.baseUrl}/api/v1/accounts/transactions/$accountId",
            HttpMethod.GET,
            entity,
            Any::class.java
        )
    }

    @Then("the response status should be {int}")
    fun theResponseStatusShouldBe(status: Int) {
        assertEquals(HttpStatus.valueOf(status), response?.statusCode)
    }

    @Then("the response should contain a list of transactions")
    fun theResponseShouldContainListOfTransactions() {
        val responseBody = response?.body
        // Accept both list or map with a 'transactions' key
        val isList = responseBody is List<*>
        val isMapWithList = responseBody is Map<*, *> && (responseBody["transactions"] is List<*>)
        assertTrue(isList || isMapWithList, "Response should be a list or contain a 'transactions' list")
    }
} 
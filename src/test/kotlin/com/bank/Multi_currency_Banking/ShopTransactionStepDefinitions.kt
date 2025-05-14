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

class ShopTransactionStepDefinitions {
    private val testContext = TestContext()
    private var accountId: String? = null
    private var response: ResponseEntity<*>? = null

    @Given("I am an authenticated user")
    fun iAmAuthenticatedUser() {
        // Already handled by TestContext init
    }

    @And("I have an account with shop transactions")
    fun iHaveAnAccountWithShopTransactions() {
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

        // Add a shop transaction
        val shopTransactionRequest = mapOf(
            "amount" to 50.0,
            "shopName" to "Test Shop",
            "description" to "Test purchase"
        )
        val transactionEntity = HttpEntity(shopTransactionRequest, headers)
        testContext.restTemplate.postForEntity(
            "${testContext.baseUrl}/api/v1/accounts/${accountId}/shop-transactions",
            transactionEntity,
            Any::class.java
        )
    }

    @When("I GET \"/api/v1/accounts/shop-transactions/{accountId}\"")
    fun iGetShopTransactions() {
        val headers = testContext.getHeaders()
        val entity = HttpEntity<Any>(headers)
        response = testContext.restTemplate.exchange(
            "${testContext.baseUrl}/api/v1/accounts/shop-transactions/$accountId",
            HttpMethod.GET,
            entity,
            Any::class.java
        )
    }

    @Then("the response status should be {int}")
    fun theResponseStatusShouldBe(status: Int) {
        assertEquals(HttpStatus.valueOf(status), response?.statusCode)
    }

    @Then("the response should contain a list of shop transactions")
    fun theResponseShouldContainListOfShopTransactions() {
        val responseBody = response?.body
        // Accept both list or map with a 'transactions' key
        val isList = responseBody is List<*>
        val isMapWithList = responseBody is Map<*, *> && (responseBody["transactions"] is List<*>)
        assertTrue(isList || isMapWithList, "Response should be a list or contain a 'transactions' list")
    }

    @Then("each shop transaction should have required fields")
    fun eachShopTransactionShouldHaveRequiredFields() {
        val responseBody = response?.body
        val transactions = when (responseBody) {
            is List<*> -> responseBody
            is Map<*, *> -> responseBody["transactions"] as? List<*>
            else -> null
        }

        assertTrue(transactions?.isNotEmpty() == true, "Should have at least one transaction")

        transactions?.forEach { transaction ->
            val transactionMap = transaction as? Map<*, *>
            assertTrue(transactionMap?.containsKey("id") == true, "Transaction should have an id")
            assertTrue(transactionMap?.containsKey("amount") == true, "Transaction should have an amount")
            assertTrue(transactionMap?.containsKey("shopName") == true, "Transaction should have a shop name")
            assertTrue(transactionMap?.containsKey("description") == true, "Transaction should have a description")
            assertTrue(transactionMap?.containsKey("timestamp") == true, "Transaction should have a timestamp")
        }
    }
} 
package com.bank.Multi_currency_Banking

import io.cucumber.datatable.DataTable
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
import java.math.BigDecimal

class ShopStepDefinitions {
    private val testContext = TestContext()
    private var response: ResponseEntity<*>? = null
    private var accountId: String? = null
    private var initialBalance: BigDecimal? = null

    @Given("I am an authenticated user")
    fun iAmAuthenticatedUser() {
        // Already handled by TestContext init
    }

    @And("I have an account with sufficient balance")
    fun iHaveAnAccountWithSufficientBalance() {
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

        // Deposit money to have sufficient balance
        val depositRequest = mapOf(
            "amount" to 1000,
            "type" to "DEPOSIT"
        )
        val depositEntity = HttpEntity(depositRequest, headers)
        val depositResponse = testContext.restTemplate.postForEntity(
            "${testContext.baseUrl}/api/v1/accounts/${accountId}/deposit",
            depositEntity,
            Map::class.java
        )

        // Store initial balance
        initialBalance = BigDecimal(depositResponse.body?.get("balance").toString())
    }

    @When("I POST {string} with the following data:")
    fun iPostWithData(endpoint: String, dataTable: DataTable) {
        val headers = testContext.getHeaders()
        val requestData = dataTable.asMaps()[0]
        val entity = HttpEntity(requestData, headers)
        response = testContext.restTemplate.exchange(
            "${testContext.baseUrl}$endpoint",
            HttpMethod.POST,
            entity,
            Any::class.java
        )
    }

    @Then("the response status should be {int}")
    fun theResponseStatusShouldBe(status: Int) {
        assertEquals(HttpStatus.valueOf(status), response?.statusCode)
    }

    @Then("the response should contain purchase details")
    fun theResponseShouldContainPurchaseDetails() {
        val responseBody = response?.body as? Map<*, *>
        assertTrue(responseBody?.containsKey("purchaseId") == true)
        assertTrue(responseBody?.containsKey("itemId") == true)
        assertTrue(responseBody?.containsKey("quantity") == true)
        assertTrue(responseBody?.containsKey("totalAmount") == true)
    }

    @Then("my account balance should be updated")
    fun myAccountBalanceShouldBeUpdated() {
        val headers = testContext.getHeaders()
        val entity = HttpEntity<Any>(headers)
        val accountResponse = testContext.restTemplate.exchange(
            "${testContext.baseUrl}/api/v1/accounts/${accountId}",
            HttpMethod.GET,
            entity,
            Map::class.java
        )
        
        val currentBalance = BigDecimal(accountResponse.body?.get("balance").toString())
        assertTrue(currentBalance < initialBalance!!, "Account balance should be less than initial balance")
    }

    @Then("the response should contain error message about insufficient balance")
    fun theResponseShouldContainErrorMessageAboutInsufficientBalance() {
        val responseBody = response?.body as? Map<*, *>
        assertTrue(responseBody?.containsKey("message") == true)
        val message = responseBody?.get("message").toString()
        assertTrue(message.contains("insufficient", ignoreCase = true) || 
                  message.contains("balance", ignoreCase = true))
    }
} 
package com.bank.Multi_currency_Banking

import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod

class MembershipStepDefinitions {
    private var response: ResponseEntity<*>? = null
    private val testContext = TestContext()

    @When("I GET {string}")
    fun iGetEndpoint(endpoint: String) {
        val headers = testContext.getHeaders()
        val entity = HttpEntity<Any>(headers)
        response = testContext.restTemplate.exchange(
            testContext.baseUrl + endpoint,
            HttpMethod.GET,
            entity,
            Any::class.java
        )
    }

    @Then("the response status should be {int}")
    fun theResponseStatusShouldBe(status: Int) {
        assertEquals(HttpStatus.valueOf(status), response?.statusCode)
    }

    @Then("the response should contain membership details")
    fun theResponseShouldContainMembershipDetails() {
        val responseBody = response?.body as? Map<*, *>
        assertTrue(responseBody?.containsKey("membershipType") == true)
        assertTrue(responseBody?.containsKey("joinDate") == true)
        assertTrue(responseBody?.containsKey("expiryDate") == true)
    }

    @Then("the response should contain membership benefits")
    fun theResponseShouldContainMembershipBenefits() {
        val responseBody = response?.body as? Map<*, *>
        assertTrue(responseBody?.containsKey("benefits") == true)
        assertTrue(responseBody?.get("benefits") is List<*>)
    }

    @Then("the response should contain membership status")
    fun theResponseShouldContainMembershipStatus() {
        val responseBody = response?.body as? Map<*, *>
        assertTrue(responseBody?.containsKey("status") == true)
        assertTrue(responseBody?.containsKey("isActive") == true)
    }
} 
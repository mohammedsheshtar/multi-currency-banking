package com.bank.Multi_currency_Banking

import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import io.cucumber.java.en.Then
import io.cucumber.java.en.And
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.RestTemplate
import java.time.LocalDate
import java.math.BigDecimal

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class UserStepDefinitions {
    private val restTemplate = RestTemplate()
    private var response: ResponseEntity<String>? = null
    private var jwtToken: String = ""

    @Given("I am an authenticated user")
    fun iAmAuthenticatedUser() {
        // Register and login to get JWT token (mock or real, depending on test setup)
        val registerBody = mapOf("username" to "testuser", "password" to "testpass")
        restTemplate.postForEntity("http://localhost:9000/api/v1/authentication/register", registerBody, String::class.java)
        val loginBody = mapOf("username" to "testuser", "password" to "testpass")
        val loginResponse = restTemplate.postForEntity("http://localhost:9000/api/v1/authentication/login", loginBody, Map::class.java)
        jwtToken = loginResponse.body?.get("token") as String
    }

    @When("I GET \"/api/v1/users/kyc\"")
    fun iGetMyKyc() {
        val headers = HttpHeaders()
        headers["Authorization"] = "Bearer $jwtToken"
        val entity = HttpEntity<String>(headers)
        response = restTemplate.exchange("http://localhost:9000/api/v1/users/kyc", HttpMethod.GET, entity, String::class.java)
    }

    @When("I POST \"/api/v1/users/kyc\" with valid KYC data")
    fun iPostMyKyc() {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers["Authorization"] = "Bearer $jwtToken"
        val kycBody = mapOf(
            "firstName" to "John",
            "lastName" to "Doe",
            "dateOfBirth" to LocalDate.of(1990, 1, 1).toString(),
            "civilId" to "123456789012",
            "country" to "Kuwait",
            "phoneNumber" to "12345678",
            "homeAddress" to "Somewhere St",
            "salary" to BigDecimal(500.000)
        )
        val entity = HttpEntity(kycBody, headers)
        response = restTemplate.postForEntity("http://localhost:9000/api/v1/users/kyc", entity, String::class.java)
    }

    @Then("the response status should be 200")
    fun theResponseStatusShouldBe200() {
        assertEquals(200, response?.statusCodeValue)
    }
} 
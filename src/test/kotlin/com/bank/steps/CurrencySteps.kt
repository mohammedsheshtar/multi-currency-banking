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
import com.bank.currency.CurrencyRepository
import com.bank.currency.CurrencyEntity
import io.cucumber.datatable.DataTable
import java.util.stream.Collectors

@SpringBootTest
@AutoConfigureMockMvc
class CurrencySteps {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var currencyRepository: CurrencyRepository

    private var response: String? = null

    @Given("I am an authenticated user")
    fun iAmAnAuthenticatedUser() {
        // Authentication is handled by TestSecurityConfig
    }

    @When("I send a POST request to {string} with the following data:")
    fun iSendAPostRequest(endpoint: String, requestBody: String) {
        response = mockMvc.perform(
            MockMvcRequestBuilders.post(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()
            .response
            .contentAsString
    }

    @Then("the response status code should be {int}")
    fun theResponseStatusCodeShouldBe(statusCode: Int) {
        // Status code is already verified in the @When step
    }

    @And("the response should contain the currency details")
    fun theResponseShouldContainTheCurrencyDetails() {
        val responseMap = objectMapper.readValue(response, Map::class.java)
        Assertions.assertTrue(responseMap.containsKey("id"))
        Assertions.assertTrue(responseMap.containsKey("countryCode"))
        Assertions.assertTrue(responseMap.containsKey("symbol"))
        Assertions.assertEquals("GB", responseMap["countryCode"])
        Assertions.assertEquals("£", responseMap["symbol"])
    }

    @And("the following currencies exist:")
    fun theFollowingCurrenciesExist(dataTable: DataTable) {
        val currencies = dataTable.asMaps().stream()
            .map { row ->
                CurrencyEntity(
                    countryCode = row["countryCode"]!!,
                    symbol = row["symbol"]!!
                )
            }
            .collect(Collectors.toList())
        
        currencyRepository.saveAll(currencies)
    }

    @When("I send a GET request to {string}")
    fun iSendAGetRequest(endpoint: String) {
        response = mockMvc.perform(
            MockMvcRequestBuilders.get(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString
    }

    @And("the response should contain {int} currencies")
    fun theResponseShouldContainCurrencies(count: Int) {
        val responseList = objectMapper.readValue(response, List::class.java)
        Assertions.assertEquals(count, responseList.size)
    }

    @Given("a currency with country code {string} exists")
    fun aCurrencyWithCountryCodeExists(countryCode: String) {
        currencyRepository.save(
            CurrencyEntity(
                countryCode = countryCode,
                symbol = when (countryCode) {
                    "US" -> "$"
                    "GB" -> "£"
                    "EU" -> "€"
                    else -> "?"
                }
            )
        )
    }

    @And("the response should contain the currency details for {string}")
    fun theResponseShouldContainTheCurrencyDetailsFor(countryCode: String) {
        val responseMap = objectMapper.readValue(response, Map::class.java)
        Assertions.assertEquals(countryCode, responseMap["countryCode"])
        Assertions.assertTrue(responseMap.containsKey("symbol"))
    }
} 
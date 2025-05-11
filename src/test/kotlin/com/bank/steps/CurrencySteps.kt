package com.bank.steps

import com.bank.currency.CurrencyEntity
import com.bank.currency.CurrencyRepository
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import com.fasterxml.jackson.databind.ObjectMapper

class CurrencySteps {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var currencyRepository: CurrencyRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private var response: String? = null

    @Given("the following currencies exist in the system")
    fun theFollowingCurrenciesExist(currencies: List<CurrencyEntity>) {
        currencyRepository.saveAll(currencies)
    }

    @Then("I should receive a list of currencies")
    fun iShouldReceiveAListOfCurrencies() {
        assertNotNull(response)
        val currencies = objectMapper.readValue(response, Array<CurrencyEntity>::class.java)
        assertNotNull(currencies)
    }

    @Then("the response should contain {int} currencies")
    fun theResponseShouldContainCurrencies(count: Int) {
        assertNotNull(response)
        val currencies = objectMapper.readValue(response, Array<CurrencyEntity>::class.java)
        assertEquals(count, currencies.size)
    }
} 
package com.bank.currency

import jakarta.persistence.Column
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class CurrenciesController(
    private val currenciesService: CurrenciesService
) {

    @GetMapping("/api/v1/currencies")
    fun getAllCurrencies(): ResponseEntity<*> {
        return currenciesService.getAllCurrencies()
    }
}

data class CurrencyResponse(
    val countryCode: String,
    val symbol: String,
    val name: String
)
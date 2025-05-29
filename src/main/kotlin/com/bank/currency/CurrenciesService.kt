package com.bank.currency

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class CurrenciesService(
    private val currencyRepository: CurrencyRepository
) {

    fun getAllCurrencies(): ResponseEntity<*>{
        return ResponseEntity.ok().body(
           currencyRepository.findAll().map {
               CurrencyResponse(
                   countryCode = it.countryCode,
                   symbol = it.symbol,
                   name = it.name
               )
           }
        )
    }
}
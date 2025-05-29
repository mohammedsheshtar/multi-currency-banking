package com.bank.`conversion-rate`

import com.bank.currency.CurrencyRepository
import com.bank.exchange.ExchangeRateApi
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class ConversionRatesService(
    private val exchangeRateApi: ExchangeRateApi,
    private val conversionRatesRepository: ConversionRatesRepository,
    private val currencyRepository: CurrencyRepository
) {

    fun getAllRates(): ResponseEntity<*>{
        val allRates = conversionRatesRepository.findAll()
        return ResponseEntity.ok(allRates)
    }

    fun getConversionRate(request: ConversionRateRequest): ResponseEntity<*> {
        val from = request.from.trim().uppercase()
        val to = request.to.trim().uppercase()

        if (from.isBlank() || to.isBlank()) {
            return ResponseEntity.badRequest().body(mapOf("error" to "'from' and 'to' fields are required"))
        }

        if (from == to) {
            return ResponseEntity.badRequest().body(mapOf("error" to "'from' and 'to' must be different currencies"))
        }

        if (!currencyRepository.existsByCountryCode(from)) {
            return ResponseEntity.badRequest().body(mapOf("error" to "invalid 'from' currency: $from"))
        }

        if (!currencyRepository.existsByCountryCode(to)) {
            return ResponseEntity.badRequest().body(mapOf("error" to "invalid 'to' currency: $to"))
        }

        return try {
            val rate = exchangeRateApi.getRate(from, to)
            ResponseEntity.ok(mapOf(
                "from" to from,
                "to" to to,
                "rate" to rate
            ))
        } catch (e: Exception) {
            ResponseEntity.status(500).body(mapOf("error" to "failed to get rate: ${e.message}"))
        }
    }
}
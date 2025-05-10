package com.bank.exchange

import com.bank.config.ExchangeRateConfig
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.math.BigDecimal

@Service
class ExchangeRateApi(private val config: ExchangeRateConfig) {

    private val webClient = WebClient.create("${config.baseUrl}/${config.apiKey}")

    fun getRate(from: String, to: String): BigDecimal {
        val response = webClient.get()
            .uri("/pair/$from/$to")
            .retrieve()
            .bodyToMono(ExchangeRateResponse::class.java)
            .block()

        return response?.conversion_rate?.toBigDecimal()
            ?: throw RuntimeException("Failed to get exchange rate from $from to $to")
    }

    data class ExchangeRateResponse(val conversion_rate: Double)
}

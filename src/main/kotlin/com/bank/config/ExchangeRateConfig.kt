package com.bank.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "exchange-rate")
class ExchangeRateConfig {
    lateinit var baseUrl: String
    lateinit var apiKey: String
}


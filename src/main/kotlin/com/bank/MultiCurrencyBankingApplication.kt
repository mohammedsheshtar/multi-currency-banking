package com.bank

import com.bank.config.ExchangeRateConfig
import com.hazelcast.config.Config
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(ExchangeRateConfig::class)
class MultiCurrencyBankingApplication

fun main(args: Array<String>) {
    runApplication<MultiCurrencyBankingApplication>(*args)
    mcCacheConfig.getMapConfig("kyc").setTimeToLiveSeconds(60)
    mcCacheConfig.getMapConfig("account").setTimeToLiveSeconds(60)
    mcCacheConfig.getMapConfig("tiers").setTimeToLiveSeconds(60)
    mcCacheConfig.getMapConfig("transaction").setTimeToLiveSeconds(60)
    mcCacheConfig.getMapConfig("shop").setTimeToLiveSeconds(60)
    mcCacheConfig.getMapConfig("shopTransaction").setTimeToLiveSeconds(60)
    mcCacheConfig.getMapConfig("conversionRate").setTimeToLiveSeconds(3600)

}

val mcCacheConfig = Config("multi-currency-cache")
val serverMcCache: HazelcastInstance = Hazelcast.newHazelcastInstance(mcCacheConfig)

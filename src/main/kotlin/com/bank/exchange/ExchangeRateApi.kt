package com.bank.exchange

import com.bank.config.ExchangeRateConfig
import com.bank.`conversion-rate`.ConversionRatesRepository
import com.bank.serverMcCache
import com.hazelcast.logging.Logger
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDateTime
import com.bank.`conversion-rate`.ConversionRateEntity

private val loggerConversion = Logger.getLogger("conversionRate")


@Service
class ExchangeRateApi(
    private val config: ExchangeRateConfig,
    private val conversionRatesRepository: ConversionRatesRepository
    ) {

    private val webClient = WebClient.create("${config.baseUrl}/${config.apiKey}")
    private val cache = serverMcCache.getMap<String, BigDecimal>("conversionRate")
    private val freshnessThresholdHours = 6L


    fun getRate(from: String, to: String): BigDecimal {
        val key = "${from.uppercase()}-${to.uppercase()}"

        // check cache
        cache[key]?.let {
            loggerConversion.info("⚠\uFE0F conversion rate found in cache...")
            return it
        }

        // check DB and update
        val dbRate = conversionRatesRepository.findByFromCurrencyAndToCurrency(from, to)
        if (dbRate != null) {
            val hoursOld = Duration.between(dbRate.timestamp, LocalDateTime.now()).toHours()
            if (hoursOld < freshnessThresholdHours) {
                loggerConversion.info("⚠\uFE0F found conversion rate in DB...rate is still recent...updating cache...")
                cache[key] = dbRate.rate
                return dbRate.rate
            } else {
                loggerConversion.info("⚠\uFE0F found conversion rate in DB... rate is outdated: ($hoursOld hrs), fetching new one from open API...")
            }
        }

        // fetch from open API
        val response = try {
            webClient.get()
                .uri("/pair/$from/$to")
                .retrieve()
                .bodyToMono(ExchangeRateResponse::class.java)
                .block()
        } catch (e: Exception) {
            loggerConversion.info("API fetch failed: ${e.message}")
            // If DB fallback is stale but no API, you may decide to return it anyway
            if (dbRate != null) {
                loggerConversion.info("⚠\uFE0F open API not responding, returning outdated rate from DB as fallback")
                return dbRate.rate
            } else {
                throw RuntimeException("⚠\uFE0F unable to fetch currency rate and no fallback available")
            }
        }

        val newRate = response?.conversion_rate?.toBigDecimal()
            ?: throw RuntimeException("⚠\uFE0F invalid response from open API")

        // update DB and Cache
        if (dbRate != null) {
            dbRate.rate = newRate
            dbRate.timestamp = LocalDateTime.now()
            conversionRatesRepository.save(dbRate)
            loggerConversion.info("⚠\uFE0F conversion rate updated in DB")
        } else {
            conversionRatesRepository.save(
                ConversionRateEntity(
                    fromCurrency = from,
                    toCurrency = to,
                    rate = newRate
                )
            )
            loggerConversion.info("⚠\uFE0F new conversion rate added in DB")
        }

        cache[key] = newRate
        loggerConversion.info("⚠\uFE0F new conversion rate fetched from open API and saved into cache")

        return newRate
    }

    data class ExchangeRateResponse(val conversion_rate: Double)
}

package com.bank.`conversion-rate`

import com.bank.user.UserRepository
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@Tag(name="ConversionRateAPI")
@RestController
class ConversionRatesController(
    private val conversionRatesService: ConversionRatesService,
    private val userRepository: UserRepository
) {

    @GetMapping("/api/v1/conversion/rates")
    fun getAllRates(): ResponseEntity<*> {
        val username = SecurityContextHolder.getContext().authentication.name
        val user = userRepository.findByUsername(username)
            ?: throw IllegalArgumentException("user has no id...")

        return conversionRatesService.getAllRates()
    }

    @GetMapping("/api/v1/conversion/rate")
    fun getConversionRate(
        @RequestParam from: String,
        @RequestParam to: String
    ): ResponseEntity<*> {
        val username = SecurityContextHolder.getContext().authentication.name
        val user = userRepository.findByUsername(username)
            ?: throw IllegalArgumentException("user has no id...")

        val request = ConversionRateRequest(from, to)
        return conversionRatesService.getConversionRate(request)
    }
}

data class ConversionRateRequest(
    val from: String,
    val to: String
)

data class ConversionRateResponse(
    val from: String,
    val to: String,
    val rate: BigDecimal
)
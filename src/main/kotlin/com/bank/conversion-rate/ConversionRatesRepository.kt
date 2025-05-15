package com.bank.`conversion-rate`

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
interface ConversionRatesRepository : JpaRepository<ConversionRateEntity, Long> {
    fun findByFromCurrencyAndToCurrency(from: String, to: String): ConversionRateEntity?
}

@Entity
@Table(name = "conversion_rates")
data class ConversionRateEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "from_currency")
    val fromCurrency: String,

    @Column(name = "to_currency")
    val toCurrency: String,

    @Column(name = "rate", precision = 9, scale = 3)
    var rate: BigDecimal,

    @Column(name = "timestamp", nullable = false)
    var timestamp: LocalDateTime = LocalDateTime.now()

) {
    constructor() : this(null, "", "", BigDecimal.ZERO, LocalDateTime.now())
}
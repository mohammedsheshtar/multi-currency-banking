package com.bank.transaction

import com.bank.account.AccountEntity
import com.bank.currency.CurrencyEntity
import com.bank.promocode.PromoCodeEntity
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
interface TransactionRepository : JpaRepository<TransactionEntity, Long> {
//    fun findBySourceAccountNumber(accountNumber: String): List<TransactionEntity>
//    fun findByDestinationNumber(accountNumber: String): List<TransactionEntity>
//    fun findByStatus(status: TransactionStatus): List<TransactionEntity>
//    fun findByTimeStampBetween(start: LocalDateTime, end: LocalDateTime): List<TransactionEntity>
//    fun findByPromoCodeId(promoCodeId: Long): List<TransactionEntity>
fun findBySourceAccount_Id(accountId: Long): List<TransactionEntity>?

}

@Entity
@Table(name = "transactions")
data class TransactionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account")
    val sourceAccount: AccountEntity? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_account")
    val destinationAccount: AccountEntity? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id")
    val currency: CurrencyEntity,

    @Column(precision = 9, scale = 3)
    val amount: BigDecimal,

    @Enumerated(EnumType.STRING)
    val status: TransactionStatus? = null,

    @Column(name = "time_stamp")
    val timeStamp: LocalDateTime = LocalDateTime.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promo_code_id")
    val promoCode: PromoCodeEntity? = null,

    @Column(name = "conversion_rate")
    val conversionRate: BigDecimal? = null

) {
    constructor() : this(null, null, null, CurrencyEntity(), BigDecimal("0.0"), null, LocalDateTime.now(), PromoCodeEntity(), null)
}

enum class TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED,
    CANCELLED
}


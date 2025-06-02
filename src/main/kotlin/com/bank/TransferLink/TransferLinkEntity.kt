package com.bank.TransferLink

// TransferLinkEntity.kt

import com.bank.account.AccountEntity
import com.bank.currency.CurrencyEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "transfer_links")
data class TransferLinkEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(unique = true)
    val linkId: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account")
    val sourceAccount: AccountEntity,

    @Column(precision = 9, scale = 3)
    val amount: BigDecimal,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id")
    val currency: CurrencyEntity,

    @Column(name = "expires_at")
    val expiresAt: LocalDateTime,

    @Column(name = "is_used")
    var isUsed: Boolean = false,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)
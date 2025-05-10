package com.bank.account

import com.bank.currency.CurrencyEntity
import com.bank.user.UserEntity
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Repository
interface AccountRepository : JpaRepository<AccountEntity, Long> {
    fun findByUserId(userId: Long?): List<AccountEntity>?
    fun findByCurrencyId(currencyId: Long): List<AccountEntity>?
    fun existsByAccountNumber(accountNumber: String): Boolean
    fun findByAccountNumber(accountNumber: String): AccountEntity?
    override fun findById(accountId: Long): Optional<AccountEntity>
}

@Entity
@Table(name = "accounts")
data class AccountEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    val user: UserEntity,

    @Column(precision = 9, scale = 3)
    var balance: BigDecimal,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id", referencedColumnName = "id")
    val currency: CurrencyEntity,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "is_active")
    val isActive: Boolean,

    @Column(name = "account_number")
    val accountNumber: String,

    @Column(name = "account_type")
    val accountType: String
) {
    constructor() : this(null, UserEntity(), BigDecimal.ZERO, CurrencyEntity(), LocalDateTime.now(), true, "", "")
}
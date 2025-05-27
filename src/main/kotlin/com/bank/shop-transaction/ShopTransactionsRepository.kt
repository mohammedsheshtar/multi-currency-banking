package com.bank.`shop-transaction`

import com.bank.account.AccountEntity
import com.bank.membership.MembershipTierEntity
import com.bank.shop.ShopEntity
import com.bank.user.UserEntity
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ShopTransactionsRepository : JpaRepository<ShopTransactionsEntity, Long> {
    fun findByUser_Id(userId: Long): List<ShopTransactionsEntity>

}

@Entity
@Table(name = "shop_transactions")
data class ShopTransactionsEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    val user: UserEntity,

    val tierName: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", referencedColumnName = "id")
    val item: ShopEntity,

    @Column(name = "points_spent")
    val pointsSpent: Int,

    @Column(name = "time_of_transaction")
    val timeOfTransaction: LocalDateTime,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_tier", referencedColumnName = "id")
    val userTier: MembershipTierEntity


) {
    constructor() : this(null, UserEntity(), "", ShopEntity(), 0, LocalDateTime.now(), MembershipTierEntity())
}
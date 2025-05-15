package com.bank.shop

import jakarta.persistence.*
import java.math.BigDecimal
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ShopsRepository : JpaRepository<ShopEntity, Long> {
//    fun findByTierIn(tiers: List<MembershipTier>): List<StoreItem>
fun findByItemNameIgnoreCase(itemName: String): ShopEntity?
}

@Entity
@Table(name = "shops")
data class ShopEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "item_name")
    val itemName: String,

    @Column(name = "tier_name")
    val tierName: String,

    @Column(name = "point_cost")
    val pointCost: Int,

    @Column(name = "item_quantity")
    val itemQuantity: Int
) {
    constructor() : this(null, "", "", 0, 0)
}

enum class MembershipTier {
    BRONZE, SILVER, GOLD, PLATINUM, DIAMOND
}
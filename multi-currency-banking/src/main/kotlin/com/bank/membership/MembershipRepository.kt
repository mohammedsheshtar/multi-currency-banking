package com.bank.membership

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
interface MembershipRepository : JpaRepository<MembershipTierEntity, Long> {
    fun findByTierName(tierName: String): MembershipTierEntity?
}

@Entity
@Table(name = "membership_tiers")
data class MembershipTierEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "tier_name")
    val tierName: String,

    @Column(name = "member_limit")
    val memberLimit: Int,

    @Column(name = "discount_amount")
    val discountAmount: BigDecimal
) {
 constructor() : this(null, "", 0, BigDecimal.ZERO)
}
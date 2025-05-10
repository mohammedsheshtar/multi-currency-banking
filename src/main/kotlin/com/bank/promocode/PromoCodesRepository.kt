package com.bank.promocode

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PromoCodeRepository : JpaRepository<PromoCodeEntity, Long> {
fun findByCode(code: Int): PromoCodeEntity?
}

@Entity
@Table(name = "promo_codes")
data class PromoCodeEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val code: Int,

    val description: String
) {
    constructor() : this(null, 0, "")
}
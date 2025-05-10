package com.bank.kyc

import com.bank.user.UserEntity
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate

@Repository
interface KYCRepository : JpaRepository<KYCEntity, Long> {
    fun findByUserId(userId: Long): KYCEntity?
}

@Entity
@Table(name = "kycs")
data class KYCEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @OneToOne
    @JoinColumn(name = "user_id")
    val user: UserEntity,

    @Column(name = "first_name")
    val firstName: String,

    @Column(name = "last_name")
    val lastName: String,

    val country: String,

    @Column(name = "date_of_birth")
    val dateOfBirth: LocalDate,

    @Column(name = "civil_id")
    val civilId: String,

    @Column(name = "phone_number")
    val phoneNumber: String,

    @Column(name = "home_address")
    val homeAddress: String,

    @Column(precision = 9, scale = 3)
    val salary: BigDecimal

) {
    constructor() : this(null, UserEntity(), "", "", "", LocalDate.MIN, "", "", "", BigDecimal.ZERO)
}
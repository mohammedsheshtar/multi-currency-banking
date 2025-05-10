package com.bank.role

import com.bank.user.UserEntity
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RoleRepository : JpaRepository<RoleEntity, Long> {
   fun findByUserId(userId: Long): List<RoleEntity>
}

@Entity
@Table(name = "roles")
data class RoleEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "role_name")
    val roleName: RoleName,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: UserEntity
) {
    constructor() : this(null, RoleName.CUSTOMER, UserEntity())
}

enum class RoleName {
    ADMIN,
    CUSTOMER
}


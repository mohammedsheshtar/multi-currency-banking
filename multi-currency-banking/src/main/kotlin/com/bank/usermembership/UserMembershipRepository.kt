package com.bank.usermembership

import com.bank.account.AccountEntity
import com.bank.membership.MembershipTierEntity
import com.bank.user.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import jakarta.persistence.*

@Repository
interface UserMembershipRepository : JpaRepository<UserMembershipEntity, Long> {
    fun findByUserId(userId: Long): List<UserMembershipEntity>
    fun findByAccountId(accountId: Long): UserMembershipEntity?
    fun findByMembershipTierId(membershipTierId: Long): List<UserMembershipEntity>
}

@Entity
@Table(name = "user_memberships")
data class UserMembershipEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: UserEntity,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    val account: AccountEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_tiers_id", nullable = false)
    val membershipTier: MembershipTierEntity,

    @Column(nullable = false)
    val tierPoints: Int = 0
) {
    constructor() : this(null, UserEntity(), AccountEntity(), MembershipTierEntity(), 0)
}

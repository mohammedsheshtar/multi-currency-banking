package com.bank.membership

import com.bank.user.UserRepository
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@Tag(name="MembershipAPI")
@RestController
class MembershipController(
    private val membershipService: MembershipService,
    private val userRepository: UserRepository
) {
    @GetMapping("/api/v1/memberships")
    fun getAll(): List<ListMembershipResponse> {
        val username = SecurityContextHolder.getContext().authentication.name
        val user = userRepository.findByUsername(username) ?: throw IllegalArgumentException("user has no id...")
        return membershipService.getAll(user.id)
    }

    @GetMapping("/api/v1/memberships/tier/{name}")
    fun getByTierName(@PathVariable name: String): ResponseEntity<*> {
        return membershipService.getByTierName(name)
    }
}

data class ListMembershipResponse(
    val tierName: String,
    val memberLimit: Int,
    val discountAmount: BigDecimal
)
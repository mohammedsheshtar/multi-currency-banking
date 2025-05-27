package com.bank.kyc

import com.bank.user.UserRepository
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDate

@Tag(name="KYC-API")
@RestController
class KYCsController(
    private val kycsService: KYCsService,
    private val userRepository: UserRepository
) {

    @GetMapping("/api/v1/users/kyc")
    fun getMyKYC(): ResponseEntity<*>? {
        val username = SecurityContextHolder.getContext().authentication.name
        val user = userRepository.findByUsername(username)
            ?: throw IllegalArgumentException("user has no id...")

        return user.id?.let { kycsService.getKYC(it) }

    }

    @PostMapping("/api/v1/users/kyc")
    fun addOrUpdateMyKYC(@RequestBody request: KYCRequest): ResponseEntity<*>? {
        val username = SecurityContextHolder.getContext().authentication.name
        val user = userRepository.findByUsername(username)
            ?: throw IllegalArgumentException("user has no id...")

        return user.id?.let { kycsService.addOrUpdateKYC(request, it) }
    }
}


class KYCRequest(
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val civilId: String,
    val country: String,
    val phoneNumber: String,
    val homeAddress: String,
    val salary: BigDecimal

)


data class KYCResponse(
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val civilId: String,
    val country: String,
    val phoneNumber: String,
    val homeAddress: String,
    val salary: BigDecimal,
    val tier: String,
    val points: Int
)
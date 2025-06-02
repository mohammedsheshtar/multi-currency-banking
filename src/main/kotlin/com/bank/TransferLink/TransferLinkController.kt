package com.bank.TransferLink

import com.bank.user.UserRepository
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@Tag(name = "TransferLinkAPI")
@RestController
@RequestMapping("/api/v1/transfer-links")
class TransferLinkController(
    private val transferLinkService: TransferLinkService,
    private val userRepository: UserRepository
) {
    @PostMapping("/generate")
    fun generateTransferLink(
        @RequestBody request: GenerateTransferLinkRequest
    ): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication.name
        val user = userRepository.findByUsername(username)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "User not found"))

        return user.id?.let { userId ->
            transferLinkService.generateTransferLink(
                accountNumber = request.accountNumber,
                amount = request.amount,
                currencyCode = request.currencyCode,
                userId = userId
            )
        } ?: ResponseEntity.badRequest().body(mapOf("error" to "User ID not found"))
    }

    @GetMapping("/{linkId}")
    fun getTransferLinkDetails(
        @PathVariable linkId: String
    ): ResponseEntity<Any> {
        return transferLinkService.getTransferLinkDetails(linkId)
    }
}

data class GenerateTransferLinkRequest(
    val accountNumber: String,
    val amount: BigDecimal,
    val currencyCode: String
) 
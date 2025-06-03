package com.bank.TransferLink

import com.bank.account.AccountRepository
import com.bank.currency.CurrencyRepository
import com.bank.user.UserRepository
import com.hazelcast.logging.Logger
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDateTime

private val loggerTransferLink = Logger.getLogger("transferLink")

@Tag(name = "TransferLinkAPI")
@RestController
@RequestMapping("/api/v1/transfer-links")
class TransferLinkController(
    private val transferLinkRepository: TransferLinkRepository,
    private val accountRepository: AccountRepository,
    private val currencyRepository: CurrencyRepository,
    private val userRepository: UserRepository
) {
    @PostMapping("/generate")
    fun generateTransferLink(
        @RequestBody request: GenerateTransferLinkRequest
    ): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication.name
        val user = userRepository.findByUsername(username)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "User not found"))

        val userId = user.id ?: return ResponseEntity.badRequest().body(mapOf("error" to "User ID not found"))

        // Validate account exists and belongs to user
        val account = accountRepository.findByAccountNumber(request.accountNumber)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "Account not found"))

        if (account.user.id != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "Account does not belong to user"))
        }

        // Validate currency exists
        val currency = currencyRepository.findByCountryCode(request.currencyCode)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "Currency not found"))

        // Validate amount is positive
        if (request.amount <= BigDecimal.ZERO) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "Amount must be positive"))
        }

        // Create transfer link with 24-hour expiration
        val transferLink = TransferLinkEntity(
            sourceAccount = account,
            amount = request.amount,
            currency = currency,
            expiresAt = LocalDateTime.now().plusHours(24)
        )

        try {
            val savedLink = transferLinkRepository.save(transferLink)
            loggerTransferLink.info("Generated transfer link: ${savedLink.linkId} for account: ${request.accountNumber}")
            
            return ResponseEntity.ok(mapOf(
                "linkId" to savedLink.linkId,
                "amount" to savedLink.amount,
                "currency" to savedLink.currency.countryCode,
                "expiresAt" to savedLink.expiresAt
            ))
        } catch (e: Exception) {
            loggerTransferLink.severe("Error generating transfer link: ${e.message}")
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to generate transfer link"))
        }
    }

    @GetMapping("/{linkId}")
    fun getTransferLinkDetails(
        @PathVariable linkId: String
    ): ResponseEntity<Any> {
        val transferLink = transferLinkRepository.findByLinkId(linkId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "Transfer link not found"))

        if (transferLink.isUsed) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "Transfer link has already been used"))
        }

        if (transferLink.expiresAt.isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "Transfer link has expired"))
        }

        return ResponseEntity.ok(mapOf(
            "linkId" to transferLink.linkId,
            "amount" to transferLink.amount,
            "currency" to transferLink.currency.countryCode,
            "expiresAt" to transferLink.expiresAt,
            "sourceAccount" to transferLink.sourceAccount.accountNumber
        ))
    }

    @PostMapping("/generate-payment-link")
    fun generatePaymentLink(
        @RequestBody request: GeneratePaymentLinkRequest
    ): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication.name
        val user = userRepository.findByUsername(username)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "User not found"))

        val userId = user.id ?: return ResponseEntity.badRequest().body(mapOf("error" to "User ID not found"))

        // Validate account exists and belongs to user
        val account = accountRepository.findByAccountNumber(request.accountNumber)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "Account not found"))

        if (account.user.id != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "Account does not belong to user"))
        }

        // Validate currency exists
        val currency = currencyRepository.findByCountryCode(request.currencyCode)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "Currency not found"))

        // Validate amount is positive
        if (request.amount <= BigDecimal.ZERO) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "Amount must be positive"))
        }

        // Create transfer link with 24-hour expiration
        val transferLink = TransferLinkEntity(
            sourceAccount = account,
            amount = request.amount,
            currency = currency,
            expiresAt = LocalDateTime.now().plusHours(24)
        )

        try {
            val savedLink = transferLinkRepository.save(transferLink)
            loggerTransferLink.info("Generated payment link: ${savedLink.linkId} for account: ${request.accountNumber}")
            
            // Return a more user-friendly response for payment links
            return ResponseEntity.ok(mapOf(
                "paymentLink" to "http://localhost:9000/api/v1/transfer-links/pay/${savedLink.linkId}",
                "amount" to savedLink.amount,
                "currency" to savedLink.currency.countryCode,
                "expiresAt" to savedLink.expiresAt,
                "description" to request.description
            ))
        } catch (e: Exception) {
            loggerTransferLink.severe("Error generating payment link: ${e.message}")
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to generate payment link"))
        }
    }

    @GetMapping("/pay/{linkId}")
    fun getPaymentLinkDetails(
        @PathVariable linkId: String
    ): ResponseEntity<Any> {
        val transferLink = transferLinkRepository.findByLinkId(linkId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "Payment link not found"))

        if (transferLink.isUsed) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "Payment link has already been used"))
        }

        if (transferLink.expiresAt.isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "Payment link has expired"))
        }

        return ResponseEntity.ok(mapOf(
            "amount" to transferLink.amount,
            "currency" to transferLink.currency.countryCode,
            "expiresAt" to transferLink.expiresAt,
            "recipientAccount" to transferLink.sourceAccount.accountNumber
        ))
    }
}

data class GenerateTransferLinkRequest(
    val accountNumber: String,
    val amount: BigDecimal,
    val currencyCode: String
)

data class GeneratePaymentLinkRequest(
    val accountNumber: String,
    val amount: BigDecimal,
    val currencyCode: String,
    val description: String? = null  // Optional description for the payment
) 
package com.bank.TransferLink

import com.bank.account.AccountEntity
import com.bank.account.AccountRepository
import com.bank.currency.CurrencyEntity
import com.bank.currency.CurrencyRepository
import com.bank.user.UserRepository
import com.hazelcast.logging.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

private val loggerTransferLink = Logger.getLogger("transferLink")

@Service
class TransferLinkService(
    private val transferLinkRepository: TransferLinkRepository,
    private val accountRepository: AccountRepository,
    private val currencyRepository: CurrencyRepository,
    private val userRepository: UserRepository
) {
    fun generateTransferLink(
        accountNumber: String,
        amount: BigDecimal,
        currencyCode: String,
        userId: Long
    ): ResponseEntity<Any> {
        // Validate account exists and belongs to user
        val account = accountRepository.findByAccountNumber(accountNumber)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "Account not found"))

        if (account.user.id != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "Account does not belong to user"))
        }

        // Validate currency exists
        val currency = currencyRepository.findByCountryCode(currencyCode)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "Currency not found"))

        // Validate amount is positive
        if (amount <= BigDecimal.ZERO) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "Amount must be positive"))
        }

        // Create transfer link with 24-hour expiration
        val transferLink = TransferLinkEntity(
            sourceAccount = account,
            amount = amount,
            currency = currency,
            expiresAt = LocalDateTime.now().plusHours(24)
        )

        try {
            val savedLink = transferLinkRepository.save(transferLink)
            loggerTransferLink.info("Generated transfer link: ${savedLink.linkId} for account: $accountNumber")
            
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

    fun getTransferLinkDetails(linkId: String): ResponseEntity<Any> {
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
}

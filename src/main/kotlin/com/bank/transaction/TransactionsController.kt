package com.bank.transaction

import com.bank.account.AccountRepository
import com.bank.user.UserRepository
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale.IsoCountryCode

@Tag(name="TransactionAPI")
@RestController
class TransactionsController(
    private val transactionsService: TransactionsService,
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository
) {
    @PostMapping("/api/v1/accounts/deposit")
    fun depositAccount(@RequestBody request: DepositRequest): ResponseEntity<*> {
        val username = SecurityContextHolder.getContext().authentication.name
        val user = userRepository.findByUsername(username) ?: throw IllegalArgumentException("user was not found...")
        return transactionsService.depositAccount(request, user.id)
    }
    @PostMapping("/api/v1/accounts/withdraw")
    fun withdrawAccount(@RequestBody request: WithdrawRequest): ResponseEntity<*> {
        val username = SecurityContextHolder.getContext().authentication.name
        val user = userRepository.findByUsername(username) ?: throw IllegalArgumentException("user was not found...")
        return transactionsService.withdrawAccount(request, user.id)
    }

    @PostMapping("/api/v1/accounts/transfer")
    fun transferAccounts(@RequestBody request: TransferRequest): ResponseEntity<*> {
        val username = SecurityContextHolder.getContext().authentication.name
        val user = userRepository.findByUsername(username) ?: throw IllegalArgumentException("user was not found...")
        return transactionsService.transferAccounts(request, user.id)
    }

    @GetMapping("/api/v1/accounts/transactions/{accountId}")
    fun getTransactionHistory(@PathVariable accountId: Long): ResponseEntity<*> {
        val username = SecurityContextHolder.getContext().authentication.name
        val user = userRepository.findByUsername(username)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "User not found"))

        val account = accountRepository.findById(accountId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to "Account not found"))

        if (account.user.id != user.id) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to "Account does not belong to user"))
        }

        return transactionsService.getTransactionHistory(accountId)
    }

    @GetMapping("/api/v1/user/accounts/transactions")
    fun getAllTransactionHistory(): ResponseEntity<*> {
        val username = SecurityContextHolder.getContext().authentication.name
        val user = userRepository.findByUsername(username) ?: throw IllegalArgumentException("user was not found...")
        return transactionsService.getAllTransactionHistory(user.id)
    }
}

data class DepositRequest(
    val accountNumber: String,
    val countryCode: String,
    val amount: BigDecimal
)

data class DepositResponse(
    val newBalance: BigDecimal,
    val transferStatus: String,
    val isConverted: Boolean,
    val amountDeposited: BigDecimal
)

data class WithdrawRequest(
    val accountNumber: String,
    val countryCode: String,
    val amount: BigDecimal
)

data class WithdrawResponse(
    val newBalance: BigDecimal,
    val transferStatus: String,
    val isConverted: Boolean,
    val amountWithdrawn: BigDecimal
)

data class TransferRequest(
    val sourceAccount: String,
    val destinationAccount: String,
    val amount: BigDecimal,
    val countryCode: String
)

data class TransferResponse(
    val sourceNewBalance: BigDecimal,
    val transferStatus: String,
    val isSourceConverted: Boolean,
    val sourceAmountWithdrawn: BigDecimal,
    val transferFee: BigDecimal
)

data class TransactionHistoryResponse(
    val accountNumber: String,
    val accountCurrency: String,
    val requestedCurrency: String,
    val amount: BigDecimal,
    val status: String,
    val timeStamp: LocalDateTime,
    val transactionType: String,
    val conversionRate: BigDecimal?
)

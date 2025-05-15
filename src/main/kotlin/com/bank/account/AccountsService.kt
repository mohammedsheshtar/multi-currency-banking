package com.bank.account

import com.bank.currency.CurrencyRepository
import com.bank.membership.MembershipRepository
import com.bank.serverMcCache
import com.bank.user.UserRepository
import com.bank.usermembership.UserMembershipEntity
import com.bank.usermembership.UserMembershipRepository
import com.hazelcast.logging.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.security.SecureRandom
import java.time.LocalDateTime
private val  loggerAccount = Logger.getLogger("account")


@Service
class AccountsService(
    private val accountRepository: AccountRepository,
    private val userRepository: UserRepository,
    private val currencyRepository: CurrencyRepository,
    private val userMembershipRepository: UserMembershipRepository,
    private val membershipRepository: MembershipRepository
) {
    fun listUserAccounts(userId: Long?): ResponseEntity<Any> {
       val accountCache = serverMcCache.getMap<Long, List<ListAccountResponse>>("account")

        accountCache[userId]?.let {
            loggerAccount.info("Returning list of accounts for userId=$userId")
            return ResponseEntity.ok(it)
        }

        val accounts = accountRepository.findByUserId(userId)?.filter { it.isActive }
        if (accounts != null) {
            if (accounts.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(mapOf("error" to "no accounts found for this user"))
            }
        }

        val response = accounts?.map { account ->
            val membership = account.id?.let { userMembershipRepository.findByAccountId(it) }
            val tierName = membership?.membershipTier?.tierName ?: "UNKNOWN"
            val points = membership?.tierPoints ?: "could not find points..." as Int

            ListAccountResponse(
                balance = account.balance,
                accountNumber = account.accountNumber,
                accountType = account.accountType,
                createdAt = account.createdAt,
                countryCode = account.currency.countryCode,
                symbol = account.currency.symbol,
                accountTier = tierName,
                points = points
            )
        }

        loggerAccount.info("No account(s) found, caching new data...")
        accountCache[userId] = response
        return ResponseEntity.ok(response)
    }

    fun createAccount(request: CreateAccount, userId: Long?): ResponseEntity<Any> {
        val currency = currencyRepository.findByCountryCode(request.countryCode)
            ?: return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "Invalid currency code: ${request.countryCode}"))

        val user = userRepository.findById(userId)
            ?: return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "user with was not found"))

        val bronzeTier = membershipRepository.findByTierName("BRONZE")
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "could not find membership tier"))

        if (request.initialBalance < BigDecimal(0.000) || request.initialBalance > BigDecimal(1000000.000)) {
            return ResponseEntity
                .badRequest()
                .body(mapOf("error" to "Initial balance must be between ${currency.symbol}0 and ${currency.symbol}1,000,000"))
        }

        val userAccounts = accountRepository.findAll().filter { it.user.id == user.id && it.isActive }
        if (userAccounts.size >= 5) {
            return ResponseEntity
                .badRequest()
                .body(mapOf("error" to "user has reached the maximum limit of 5 active accounts"))
        }

        val account = accountRepository.save(AccountEntity(
            user = user,
            balance = request.initialBalance,
            currency = currency,
            createdAt = LocalDateTime.now(),
            isActive = true,
            accountNumber = generateUniqueAccountNumber(),
            accountType = request.accountType
        ))

        userMembershipRepository.save(UserMembershipEntity(
            user = user,
            account = account,
            membershipTier = bronzeTier,
            tierPoints = 0
        ))

        val accountCache = serverMcCache.getMap<Long, List<CreateAccountResponse>>("account")
        loggerAccount.info("account=${account.id} for userId=$userId has been created...invalidating cache")
        accountCache.remove(userId)

        return ResponseEntity.ok().body(CreateAccountResponse(
            balance = account.balance,
            accountNumber = account.accountNumber,
            accountType = account.accountType,
            createdAt = account.createdAt,
            countryCode = account.currency.countryCode,
            symbol = account.currency.symbol
        ))
    }

    fun closeAccount(accountNumber: String, userId: Long?): ResponseEntity<Any> {
        val account = accountRepository.findAll()
            .firstOrNull { it.accountNumber == accountNumber && it.user.id == userId }
            ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "account not found or does not belong to the user"))

        if (!account.isActive) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to "account is already closed"))
        }

        if (account.balance > BigDecimal.ZERO) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to "cannot close account with non-zero balance"))
        }

        val closedAccount = account.copy(isActive = false)
        accountRepository.save(closedAccount)


        val accountCache = serverMcCache.getMap<Long, List<CreateAccountResponse>>("account")
        loggerAccount.info("accountId=${account.id} for userId=$userId has been closed...invalidating cache")
        accountCache.remove(userId)

        return ResponseEntity.ok().build()
    }


    fun generateSecureAccountNumber(): String {
        val secureRandom = SecureRandom()
        val prefix = "77"
        val randomDigits = (1..12)
            .map { secureRandom.nextInt(10) }
            .joinToString("")
        return "$prefix$randomDigits"
    }
    fun generateUniqueAccountNumber(): String {
        var accountNumber: String
        do {
            accountNumber = generateSecureAccountNumber()
        } while (accountRepository.existsByAccountNumber(accountNumber))
        return accountNumber
    }
}
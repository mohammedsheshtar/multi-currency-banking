package com.bank.transaction

import com.bank.account.AccountRepository
import com.bank.account.ListAccountResponse
import com.bank.currency.CurrencyRepository
import com.bank.exchange.ExchangeRateApi
import com.bank.kyc.KYCResponse
import com.bank.membership.MembershipRepository
import com.bank.promocode.PromoCodeRepository
import com.bank.serverMcCache
import com.bank.shop.ListItemsResponse
import com.bank.usermembership.UserMembershipRepository
import com.hazelcast.logging.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

private val loggerAccount = Logger.getLogger("account")
private val loggerTransaction = Logger.getLogger("transaction")
private val loggerShop = Logger.getLogger("shop")
private val loggerKyc = Logger.getLogger("kyc")
const val TRANSACTION_TYPE_DEPOSIT = 103
const val TRANSACTION_TYPE_WITHDRAW = 104
const val TRANSACTION_TYPE_TRANSFER = 101
const val TRANSACTION_TYPE_FEE = 102

@Service
class TransactionsService(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val promoCodeRepository: PromoCodeRepository,
    private val currencyRepository: CurrencyRepository,
    private val userMembershipRepository: UserMembershipRepository,
    private val membershipRepository: MembershipRepository,
    private val exchangeRateApi: ExchangeRateApi
) {

    fun getTransactionHistory(accountId: Long): ResponseEntity<*> {
        val transactionCache = serverMcCache.getMap<Long, List<TransactionHistoryResponse?>?>("transaction")

        transactionCache[accountId]?.let {
            loggerTransaction.info("Returning list of transactions for accountId=$accountId")
            return ResponseEntity.ok(it)
        }

        val transactionHistory = transactionRepository.findBySourceAccount_Id(accountId)

        val account = accountRepository.findById(accountId).filter { it.isActive }.orElse(null)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "account id not found"))

        val response = transactionHistory?.map { transactionHistory ->
                TransactionHistoryResponse(
                    accountNumber = account.accountNumber,
                    accountCurrency = account.currency.countryCode,
                    requestedCurrency = transactionHistory.currency.countryCode,
                    amount = transactionHistory.amount,
                    status = transactionHistory.status.toString(),
                    timeStamp = transactionHistory.timeStamp,
                    transactionType = transactionHistory.promoCode?.description.toString(),
                    conversionRate = transactionHistory.conversionRate
                )
            }

        loggerTransaction.info("No transaction(s) found, caching new data...")
        transactionCache[accountId] = response
        return ResponseEntity.ok(response)
    }

    fun depositAccount(request: DepositRequest, userId: Long?): ResponseEntity<*> {
        val account = accountRepository.findByAccountNumber(request.accountNumber)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "account with number ${request.accountNumber} was not found"))

        val requestedCurrency = currencyRepository.findByCountryCode(request.countryCode)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "Currency not supported"))

        val userMembership = userId?.let { userMembershipRepository.findByUser_Id(it) }
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "Membership not found"))

        val promoCodeDeposit = promoCodeRepository.findByCode(TRANSACTION_TYPE_DEPOSIT)

        if (account.user.id != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to "unauthorized access"))
        }

        if (!account.isActive) {
            return ResponseEntity.badRequest().body(mapOf("error" to "account is not active..."))
        }

        if (request.amount < BigDecimal("1.000") || request.amount > BigDecimal("100000.000")) {
            return ResponseEntity.badRequest().body(mapOf("error" to "amount must be between ${account.currency.symbol}1 and ${account.currency.symbol}100,000"))
        }

        var conversionRate = BigDecimal.ONE
        val (finalAmount, wasConverted) = if (account.currency.countryCode != request.countryCode) {
            conversionRate = exchangeRateApi.getRate(request.countryCode, account.currency.countryCode)
            request.amount.multiply(conversionRate).setScale(3, RoundingMode.HALF_UP) to true
        } else {
            request.amount to false
        }

        account.balance += finalAmount
        accountRepository.save(account)

        transactionRepository.save(TransactionEntity(
            sourceAccount = account,
            destinationAccount = null,
            currency = requestedCurrency,
            amount = request.amount,
            timeStamp = LocalDateTime.now(),
            promoCode = promoCodeDeposit,
            status = TransactionStatus.COMPLETED,
            conversionRate = conversionRate
        ))

        val earnedPoints = request.amount.multiply(BigDecimal("0.05")).toInt()
        val updatedPoints = userMembership.tierPoints + earnedPoints

        val allTiers = membershipRepository.findAll().sortedBy { it.memberLimit }
        val currentTier = userMembership.membershipTier
        val nextTier = allTiers
            .filter { updatedPoints >= it.memberLimit }
            .maxByOrNull { it.memberLimit }
            ?: allTiers.first()

        val updatedMembership = if (nextTier.memberLimit > currentTier.memberLimit) {
            userMembership.copy(
                tierPoints = updatedPoints,
                membershipTier = nextTier
            )
        } else {
            userMembership.copy(tierPoints = updatedPoints)
        }


        userMembershipRepository.save(updatedMembership)

        val shopCache = serverMcCache.getMap<Long, List<ListItemsResponse>>("shop")
        loggerShop.info("user membership for userId=${userId} has been updated...invalidating cache")
        shopCache.remove(userId)

        val kycCache = serverMcCache.getMap<Long, KYCResponse>("kyc")
        loggerKyc.info("KYC for userId=$userId has been updated...invalidating cache")
        kycCache.remove(userId)

        val accountCache = serverMcCache.getMap<Long, List<ListAccountResponse>>("account")
        loggerAccount.info("user=$userId deposited into accountId=${account.id}...invalidating cache")
        accountCache.remove(userId)

        val transactionCache = serverMcCache.getMap<Long, List<TransactionHistoryResponse>>("transaction")
        loggerShop.info("transaction history for accountId=${account.id} has been updated...invalidating cache")
        transactionCache.remove(account.id)

        return ResponseEntity.ok().body(DepositResponse(
            newBalance = account.balance,
            transferStatus = TransactionStatus.COMPLETED.toString(),
            isConverted = wasConverted,
            amountDeposited = finalAmount
        ))
    }


    fun withdrawAccount(request: WithdrawRequest, userId: Long?): ResponseEntity<*> {
        val account = accountRepository.findByAccountNumber(request.accountNumber)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "account with number ${request.accountNumber} was not found"))

        val requestedCurrency = currencyRepository.findByCountryCode(request.countryCode)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "Currency not supported"))

        val promoCode = promoCodeRepository.findByCode(TRANSACTION_TYPE_WITHDRAW)

        if (account.user.id != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to "Unauthorized access"))
        }

        if (!account.isActive) {
            return ResponseEntity.badRequest().body(mapOf("error" to "account is not active..."))
        }

        if (request.amount < BigDecimal("1.000") || request.amount > BigDecimal("100000.000")) {
            return ResponseEntity.badRequest().body(mapOf("error" to "amount must be between ${account.currency.symbol}1 and ${account.currency.symbol}100,000"))
        }

        var conversionRate = BigDecimal.ONE // default: no conversion
        val (finalAmount, wasConverted) = if (account.currency.countryCode != request.countryCode) {
            conversionRate = exchangeRateApi.getRate(request.countryCode, account.currency.countryCode)
            request.amount.multiply(conversionRate).setScale(3, RoundingMode.HALF_UP) to true
        } else {
            request.amount to false
        }

        if (finalAmount > account.balance) {
            return ResponseEntity.badRequest().body(mapOf("error" to "insufficient balance"))
        }

        account.balance -= finalAmount
        accountRepository.save(account)

        transactionRepository.save(TransactionEntity(
            sourceAccount = account,
            destinationAccount = null,
            currency = requestedCurrency,
            amount = request.amount,
            timeStamp = LocalDateTime.now(),
            promoCode = promoCode,
            status = TransactionStatus.COMPLETED,
            conversionRate = conversionRate

        ))

        val accountCache = serverMcCache.getMap<Long, List<ListAccountResponse>>("account")
        loggerAccount.info("user=$userId withdrew from accountId=${account.id}...invalidating cache")
        accountCache.remove(userId)

        val transactionCache = serverMcCache.getMap<Long, List<TransactionHistoryResponse>>("transaction")
        loggerShop.info("transaction history for accountId=${account.id} has been updated...invalidating cache")
        transactionCache.remove(account.id)

        return ResponseEntity.ok().body(WithdrawResponse(
            newBalance = account.balance,
            transferStatus = TransactionStatus.COMPLETED.toString(),
            isConverted = wasConverted,
            amountWithdrawn = finalAmount
        ))
    }

    fun transferAccounts(request: TransferRequest, userId: Long?): ResponseEntity<*> {

        val sourceAccount = accountRepository.findByAccountNumber(request.sourceAccount)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "account with number ${request.sourceAccount} was not found"))

        val destinationAccount = accountRepository.findByAccountNumber(request.destinationAccount)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "account with number ${request.destinationAccount} was not found"))

        val requestedCurrency = currencyRepository.findByCountryCode(request.countryCode)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "unsupported currency"))

        val userSourceMembership = sourceAccount.user.id?.let { userMembershipRepository.findByUser_Id(it) }
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "membership for source account user not found"))

        val userDestinationMembership = destinationAccount.user.id?.let { userMembershipRepository.findByUser_Id(it) }
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "membership for destination account user not found"))

        val promoCode = promoCodeRepository.findByCode(TRANSACTION_TYPE_TRANSFER)

        val feePromo = promoCodeRepository.findByCode(TRANSACTION_TYPE_FEE)

        if (sourceAccount.user.id != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to "Unauthorized access"))
        }

//        if (destinationAccount.user.id != userId) {
//            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to "Unauthorized access"))
//        }

        if (!sourceAccount.isActive) {
            return ResponseEntity.badRequest().body(mapOf("error" to "source account is not active..."))
        }

        if (!destinationAccount.isActive) {
            return ResponseEntity.badRequest().body(mapOf("error" to "destination account is not active..."))
        }

        if (request.amount < BigDecimal("1.000") || request.amount > BigDecimal("100000.000")) {
            return ResponseEntity.badRequest().body(mapOf("error" to "amount must be between ${requestedCurrency.symbol}1 and ${requestedCurrency.symbol}100,000"))
        }

        if (request.sourceAccount == request.destinationAccount) {
            return ResponseEntity.badRequest().body(mapOf("error" to "you cannot transfer funds between the same account..."))
        }

        val (fromRate, wasSourceConverted) = if (request.countryCode != sourceAccount.currency.countryCode){exchangeRateApi.getRate(request.countryCode, sourceAccount.currency.countryCode) to true}
        else{BigDecimal("1.000") to false}
        val sourceAmount = request.amount.multiply(fromRate).setScale(3, RoundingMode.HALF_UP)

        val (toRate, wasDestinationConverted) = if(request.countryCode != destinationAccount.currency.countryCode) {exchangeRateApi.getRate(request.countryCode, destinationAccount.currency.countryCode) to true}
        else{BigDecimal("1.000") to false}
        val destinationAmount = request.amount.multiply(toRate).setScale(3, RoundingMode.HALF_UP)

        val feeRate = if("USD" != sourceAccount.currency.countryCode){exchangeRateApi.getRate("USD", sourceAccount.currency.countryCode)}
        else{BigDecimal("1.000")}
        val rawFee = BigDecimal("15.000")
        val discountMultiplier = BigDecimal.ONE.subtract(userSourceMembership.membershipTier.discountAmount)
        val feeInAccountCurrency = feeRate
            .multiply(rawFee)
            .multiply(discountMultiplier)
            .setScale(3, RoundingMode.HALF_UP)


        if (sourceAmount > sourceAccount.balance) {
            return ResponseEntity.badRequest().body(mapOf("error" to "insufficient balance"))
        }

        sourceAccount.balance -= sourceAmount

        if (feeInAccountCurrency > sourceAccount.balance) {
            return ResponseEntity.badRequest().body(mapOf("error" to "insufficient balance"))
        }

        sourceAccount.balance -= feeInAccountCurrency
        destinationAccount.balance += destinationAmount


        accountRepository.save(sourceAccount)
        accountRepository.save(destinationAccount)

        transactionRepository.save(TransactionEntity(
            sourceAccount = sourceAccount,
            destinationAccount = destinationAccount,
            currency = requestedCurrency,
            amount = request.amount,
            timeStamp = LocalDateTime.now(),
            promoCode = promoCode,
            status = TransactionStatus.COMPLETED,
            conversionRate = fromRate
        ))

        transactionRepository.save(TransactionEntity(
            sourceAccount = sourceAccount,
            destinationAccount = null,
            currency = requestedCurrency,
            amount = feeInAccountCurrency,
            timeStamp = LocalDateTime.now(),
            promoCode = feePromo,
            status = TransactionStatus.COMPLETED,
            conversionRate = feeRate
        ))

        val sameUser = sourceAccount.user.id == destinationAccount.user.id

        if (!sameUser) {
            val sourceEarnedPoints = request.amount.multiply(BigDecimal("0.05")).toInt()
            val sourceUpdatedPoints = userSourceMembership.tierPoints + sourceEarnedPoints

            val sourceAllTiers = membershipRepository.findAll().sortedBy { it.memberLimit }
            val sourceNextTier = sourceAllTiers
                .filter { sourceUpdatedPoints >= it.memberLimit }
                .maxByOrNull { it.memberLimit }
                ?: sourceAllTiers.first()


            val currentSourceTierLimit = userSourceMembership.membershipTier.memberLimit
            val sourceUpdatedMembership = if (sourceNextTier.memberLimit > currentSourceTierLimit) {
                userSourceMembership.copy(
                    tierPoints = sourceUpdatedPoints,
                    membershipTier = sourceNextTier
                )
            } else {
                userSourceMembership.copy(tierPoints = sourceUpdatedPoints)
            }
            userMembershipRepository.save(sourceUpdatedMembership)
        }

        val destinationEarnedPoints = request.amount.multiply(BigDecimal("0.5")).toInt()
        val destinationUpdatedPoints = userDestinationMembership.tierPoints + destinationEarnedPoints

        val destinationAllTiers = membershipRepository.findAll().sortedBy { it.memberLimit }
        val destinationNextTier = destinationAllTiers
            .filter { destinationUpdatedPoints >= it.memberLimit }
            .maxByOrNull { it.memberLimit }
            ?: destinationAllTiers.first()


        val currentDestinationTierLimit = userDestinationMembership.membershipTier.memberLimit
        val destinationUpdatedMembership = if (destinationNextTier.memberLimit > currentDestinationTierLimit) {
            userDestinationMembership.copy(
                tierPoints = destinationUpdatedPoints,
                membershipTier = destinationNextTier
            )
        } else {
            userDestinationMembership.copy(tierPoints = destinationUpdatedPoints)
        }

        userMembershipRepository.save(destinationUpdatedMembership)


        val shopCache = serverMcCache.getMap<Long, List<ListItemsResponse>>("shop")
        if (!sameUser) {
            loggerShop.info("user membership for userId=${sourceAccount.user.id} has be updated...invalidating cache")
            shopCache.remove(sourceAccount.user.id)
        }

        loggerShop.info("user membership for userId=${destinationAccount.user.id} has be updated...invalidating cache")
        shopCache.remove(destinationAccount.user.id)

        val kycCache = serverMcCache.getMap<Long, KYCResponse>("kyc")
        if(!sameUser) {
            loggerKyc.info("KYC for userId=${sourceAccount.user.id} has been updated...invalidating cache")
            kycCache.remove(sourceAccount.user.id)
        }

        loggerKyc.info("KYC for userId=${destinationAccount.user.id} has been updated...invalidating cache")
        kycCache.remove(destinationAccount.user.id)

        val accountCache = serverMcCache.getMap<Long, List<ListAccountResponse>>("account")
        loggerAccount.info("transfer occurred for source account with userId=${sourceAccount.user.id} ...invalidating cache")
        accountCache.remove(sourceAccount.user.id)

        loggerAccount.info("transfer occurred for destination account with userId=${destinationAccount.user.id} ...invalidating cache")
        accountCache.remove(sourceAccount.user.id)

        val transactionCache = serverMcCache.getMap<Long, List<TransactionHistoryResponse>>("transaction")
        loggerShop.info("transaction history for source accountId=${sourceAccount.id} has been updated...invalidating cache")
        transactionCache.remove(sourceAccount.id)

        loggerShop.info("transaction history for destination accountId=${destinationAccount.id} has been updated...invalidating cache")
        transactionCache.remove(destinationAccount.id)


        return ResponseEntity.ok().body(TransferResponse(
            sourceNewBalance = sourceAccount.balance,
            transferStatus = TransactionStatus.COMPLETED.toString(),
            isSourceConverted = wasSourceConverted,
            sourceAmountWithdrawn = sourceAmount,
            transferFee = feeInAccountCurrency
        ))
    }

    fun getAllTransactionHistory(userId: Long?): ResponseEntity<*> {

        val accounts = accountRepository.findByUserId(userId)?.filter { it.isActive }
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("error" to "account(s) not found")

        val accountIds = accounts.map { it }

        val transactions = transactionRepository.findBySourceAccountInOrDestinationAccountIn(accountIds, accountIds)

        val transactionHistory = transactions.map {
            it.sourceAccount?.let { it1 ->
                it.sourceAccount?.currency?.let { it2 ->
                    TransactionHistoryResponse(
                        accountNumber = it1.accountNumber,
                        accountCurrency = it2.countryCode,
                        requestedCurrency = it.currency.countryCode,
                        amount = it.amount,
                        status = it.status.toString(),
                        timeStamp = it.timeStamp,
                        transactionType = it.promoCode?.description.toString(),
                        conversionRate = it.conversionRate
                    )
                }
            }
        }
        return ResponseEntity.ok(transactionHistory)
    }
}
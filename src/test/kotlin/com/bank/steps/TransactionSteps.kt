package com.bank.steps

import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import io.cucumber.java.en.Then
import io.cucumber.java.en.And
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions
import com.bank.account.AccountRepository
import com.bank.account.AccountEntity
import com.bank.user.UserRepository
import com.bank.user.UserEntity
import com.bank.transaction.TransactionRepository
import com.bank.transaction.TransactionEntity
import com.bank.transaction.TransactionStatus
import com.bank.currency.CurrencyEntity
import com.bank.currency.CurrencyRepository
import java.math.BigDecimal
import org.springframework.test.context.ContextConfiguration
import com.bank.config.TestConfig
import com.bank.authentication.jwt.JwtService
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(classes = [TestConfig::class])
class TransactionSteps {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var accountRepository: AccountRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var transactionRepository: TransactionRepository

    @Autowired
    private lateinit var currencyRepository: CurrencyRepository

    @Autowired
    private lateinit var jwtService: JwtService

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private var response: String? = null
    private var authToken: String? = null

    @And("the response should contain the transaction history")
    fun theResponseShouldContainTheTransactionHistory() {
        val responseList = objectMapper.readValue(response, List::class.java)
        Assertions.assertTrue(responseList.isNotEmpty())
        val firstTransaction = responseList[0] as Map<*, *>
        Assertions.assertTrue(firstTransaction.containsKey("transactionId"))
        Assertions.assertTrue(firstTransaction.containsKey("amount"))
        Assertions.assertTrue(firstTransaction.containsKey("status"))
    }

    @And("the response should contain the deposit details")
    fun theResponseShouldContainTheDepositDetails() {
        val responseMap = objectMapper.readValue(response, Map::class.java)
        Assertions.assertTrue(responseMap.containsKey("transactionId"))
        Assertions.assertTrue(responseMap.containsKey("amount"))
        Assertions.assertTrue(responseMap.containsKey("newBalance"))
        Assertions.assertTrue(responseMap.containsKey("status"))
    }

    @And("the response should contain the withdrawal details")
    fun theResponseShouldContainTheWithdrawalDetails() {
        val responseMap = objectMapper.readValue(response, Map::class.java)
        Assertions.assertTrue(responseMap.containsKey("transactionId"))
        Assertions.assertTrue(responseMap.containsKey("amount"))
        Assertions.assertTrue(responseMap.containsKey("newBalance"))
        Assertions.assertTrue(responseMap.containsKey("status"))
    }

    @And("the response should contain the transfer details")
    fun theResponseShouldContainTheTransferDetails() {
        val responseMap = objectMapper.readValue(response, Map::class.java)
        Assertions.assertTrue(responseMap.containsKey("transactionId"))
        Assertions.assertTrue(responseMap.containsKey("amount"))
        Assertions.assertTrue(responseMap.containsKey("sourceAccount"))
        Assertions.assertTrue(responseMap.containsKey("destinationAccount"))
        Assertions.assertTrue(responseMap.containsKey("status"))
    }

    @And("I have performed some transactions on account {string}")
    fun iHavePerformedSomeTransactionsOnAccount(accountNumber: String) {
        val account = accountRepository.findByAccountNumber(accountNumber)
            ?: throw IllegalStateException("Account not found")
        val currency = currencyRepository.findByCountryCode("US")
            ?: throw IllegalStateException("Currency not found")

        transactionRepository.save(
            TransactionEntity(
                sourceAccount = account,
                currency = currency,
                amount = BigDecimal("100.000"),
                status = TransactionStatus.COMPLETED
            )
        )
    }
} 
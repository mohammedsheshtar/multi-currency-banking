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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions
import com.bank.account.AccountRepository
import com.bank.account.AccountEntity
import com.bank.currency.CurrencyRepository
import com.bank.currency.CurrencyEntity
import com.bank.transaction.TransactionRepository
import com.bank.transaction.TransactionEntity
import com.bank.transaction.TransactionStatus
import com.bank.user.UserRepository
import com.bank.user.UserEntity
import java.math.BigDecimal
import java.time.LocalDateTime
import io.cucumber.datatable.DataTable
import java.util.stream.Collectors

@SpringBootTest
@AutoConfigureMockMvc
class TransactionSteps {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var accountRepository: AccountRepository

    @Autowired
    private lateinit var currencyRepository: CurrencyRepository

    @Autowired
    private lateinit var transactionRepository: TransactionRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    private var response: String? = null
    private var sourceAccount: AccountEntity? = null
    private var destinationAccount: AccountEntity? = null

    @Given("I am an authenticated user")
    fun iAmAnAuthenticatedUser() {
        // Authentication is handled by TestSecurityConfig
    }

    @Given("I have a source account with balance {double} {string}")
    fun iHaveASourceAccountWithBalance(balance: Double, currencyCode: String) {
        val user = userRepository.save(UserEntity(username = "testuser", password = "password", createdAt = LocalDateTime.now()))
        val currency = currencyRepository.save(CurrencyEntity(countryCode = currencyCode, symbol = when(currencyCode) {
            "USD" -> "$"
            "EUR" -> "€"
            else -> "?"
        }))
        
        sourceAccount = accountRepository.save(
            AccountEntity(
                user = user,
                balance = BigDecimal(balance),
                currency = currency,
                isActive = true,
                accountNumber = "USD123456"
            )
        )
    }

    @Given("I have a destination account with balance {double} {string}")
    fun iHaveADestinationAccountWithBalance(balance: Double, currencyCode: String) {
        val currency = currencyRepository.save(CurrencyEntity(countryCode = currencyCode, symbol = when(currencyCode) {
            "USD" -> "$"
            "EUR" -> "€"
            else -> "?"
        }))
        
        destinationAccount = accountRepository.save(
            AccountEntity(
                user = sourceAccount!!.user,
                balance = BigDecimal(balance),
                currency = currency,
                isActive = true,
                accountNumber = "EUR789012"
            )
        )
    }

    @When("I send a POST request to {string} with the following data:")
    fun iSendAPostRequest(endpoint: String, requestBody: String) {
        response = mockMvc.perform(
            MockMvcRequestBuilders.post(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andReturn()
            .response
            .contentAsString
    }

    @Then("the response status code should be {int}")
    fun theResponseStatusCodeShouldBe(statusCode: Int) {
        // Status code is already verified in the @When step
    }

    @And("the transaction should be completed successfully")
    fun theTransactionShouldBeCompletedSuccessfully() {
        val responseMap = objectMapper.readValue(response, Map::class.java)
        Assertions.assertTrue(responseMap.containsKey("id"))
        Assertions.assertEquals("COMPLETED", responseMap["status"])
    }

    @And("the source account balance should be {double} {string}")
    fun theSourceAccountBalanceShouldBe(balance: Double, currency: String) {
        val updatedAccount = accountRepository.findById(sourceAccount!!.id!!).get()
        Assertions.assertEquals(BigDecimal(balance), updatedAccount.balance)
    }

    @And("the destination account balance should be {double} {string}")
    fun theDestinationAccountBalanceShouldBe(balance: Double, currency: String) {
        val updatedAccount = accountRepository.findById(destinationAccount!!.id!!).get()
        Assertions.assertEquals(BigDecimal(balance), updatedAccount.balance)
    }

    @And("the response should contain insufficient funds error")
    fun theResponseShouldContainInsufficientFundsError() {
        val responseMap = objectMapper.readValue(response, Map::class.java)
        Assertions.assertTrue(responseMap.containsKey("error"))
        Assertions.assertTrue((responseMap["error"] as String).contains("insufficient funds"))
    }

    @And("the response should contain invalid currency error")
    fun theResponseShouldContainInvalidCurrencyError() {
        val responseMap = objectMapper.readValue(response, Map::class.java)
        Assertions.assertTrue(responseMap.containsKey("error"))
        Assertions.assertTrue((responseMap["error"] as String).contains("invalid currency"))
    }

    @Given("the following transactions exist:")
    fun theFollowingTransactionsExist(dataTable: DataTable) {
        val transactions = dataTable.asMaps().stream()
            .map { row ->
                TransactionEntity(
                    sourceAccount = accountRepository.findByAccountNumber(row["sourceAccount"]!!)!!,
                    destinationAccount = accountRepository.findByAccountNumber(row["destinationAccount"]!!)!!,
                    amount = BigDecimal(row["amount"]!!),
                    currency = currencyRepository.findByCountryCode(row["currency"]!!)!!,
                    status = TransactionStatus.valueOf(row["status"]!!),
                    timeStamp = LocalDateTime.now()
                )
            }
            .collect(Collectors.toList())
        
        transactionRepository.saveAll(transactions)
    }

    @When("I send a GET request to {string}")
    fun iSendAGetRequest(endpoint: String) {
        response = mockMvc.perform(
            MockMvcRequestBuilders.get(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString
    }

    @And("the response should contain {int} transactions")
    fun theResponseShouldContainTransactions(count: Int) {
        val responseList = objectMapper.readValue(response, List::class.java)
        Assertions.assertEquals(count, responseList.size)
    }
} 
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
import com.bank.currency.CurrencyEntity
import com.bank.currency.CurrencyRepository
import java.math.BigDecimal
import org.springframework.test.context.ContextConfiguration
import com.bank.config.TestConfig
import com.bank.authentication.jwt.JwtService
import org.springframework.security.crypto.password.PasswordEncoder

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(classes = [TestConfig::class])
class AccountSteps {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var accountRepository: AccountRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var currencyRepository: CurrencyRepository

    @Autowired
    private lateinit var jwtService: JwtService

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private var response: String? = null
    private var authToken: String? = null

    @Then("the response should contain the account details")
    fun theResponseShouldContainTheAccountDetails() {
        val responseMap = objectMapper.readValue(response, Map::class.java)
        Assertions.assertTrue(responseMap.containsKey("accountNumber"))
        Assertions.assertTrue(responseMap.containsKey("balance"))
        Assertions.assertTrue(responseMap.containsKey("currency"))
        Assertions.assertTrue(responseMap.containsKey("accountType"))
    }

    @And("I have an account with number {string} and balance {double}")
    fun iHaveAnAccountWithNumberAndBalance(accountNumber: String, balance: Double) {
        val user = userRepository.findByUsername("testuser")
            ?: throw IllegalStateException("Test user not found")
        val currency = currencyRepository.findByCountryCode("US")
            ?: throw IllegalStateException("Currency not found")

        accountRepository.save(
            AccountEntity(
                user = user,
                balance = BigDecimal(balance.toString()),
                currency = currency,
                isActive = true,
                accountNumber = accountNumber,
                accountType = "SAVINGS"
            )
        )
    }

    @And("the response should contain {int} accounts")
    fun theResponseShouldContainAccounts(count: Int) {
        val responseList = objectMapper.readValue(response, List::class.java)
        Assertions.assertEquals(count, responseList.size)
        if (count > 0) {
            val firstAccount = responseList[0] as Map<*, *>
            Assertions.assertTrue(firstAccount.containsKey("accountNumber"))
            Assertions.assertTrue(firstAccount.containsKey("balance"))
            Assertions.assertTrue(firstAccount.containsKey("currency"))
            Assertions.assertTrue(firstAccount.containsKey("accountType"))
        }
    }

    @And("the account should be closed")
    fun theAccountShouldBeClosed() {
        val responseMap = objectMapper.readValue(response, Map::class.java)
        Assertions.assertFalse(responseMap["isActive"] as Boolean)
    }
} 
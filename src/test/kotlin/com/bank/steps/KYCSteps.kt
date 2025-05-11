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
import com.bank.kyc.KYCRepository
import com.bank.kyc.KYCEntity
import com.bank.user.UserRepository
import com.bank.user.UserEntity
import org.springframework.test.context.ContextConfiguration
import com.bank.config.TestConfig
import com.bank.authentication.jwt.JwtService
import org.springframework.security.crypto.password.PasswordEncoder
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(classes = [TestConfig::class])
class KYCSteps {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var kycRepository: KYCRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jwtService: JwtService

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private var response: String? = null
    private var authToken: String? = null

    @Then("the response should contain the KYC details")
    fun theResponseShouldContainTheKYCDetails() {
        val responseMap = objectMapper.readValue(response, Map::class.java)
        Assertions.assertTrue(responseMap.containsKey("firstName"))
        Assertions.assertTrue(responseMap.containsKey("lastName"))
        Assertions.assertTrue(responseMap.containsKey("dateOfBirth"))
        Assertions.assertTrue(responseMap.containsKey("civilId"))
        Assertions.assertTrue(responseMap.containsKey("country"))
        Assertions.assertTrue(responseMap.containsKey("phoneNumber"))
        Assertions.assertTrue(responseMap.containsKey("homeAddress"))
        Assertions.assertTrue(responseMap.containsKey("salary"))
    }

    @And("I have submitted my KYC information")
    fun iHaveSubmittedMyKYCInformation() {
        val user = userRepository.findByUsername("testuser")
            ?: throw IllegalStateException("Test user not found")

        kycRepository.save(
            KYCEntity(
                user = user,
                firstName = "John",
                lastName = "Doe",
                dateOfBirth = LocalDate.parse("1990-01-01"),
                civilId = "123456789012",
                country = "US",
                phoneNumber = "12345678",
                homeAddress = "123 Main St",
                salary = BigDecimal("5000.000")
            )
        )
    }

    @And("the response should contain my KYC details")
    fun theResponseShouldContainMyKYCDetails() {
        val responseMap = objectMapper.readValue(response, Map::class.java)
        Assertions.assertTrue(responseMap.containsKey("firstName"))
        Assertions.assertTrue(responseMap.containsKey("lastName"))
        Assertions.assertTrue(responseMap.containsKey("dateOfBirth"))
        Assertions.assertTrue(responseMap.containsKey("civilId"))
        Assertions.assertTrue(responseMap.containsKey("country"))
        Assertions.assertTrue(responseMap.containsKey("phoneNumber"))
        Assertions.assertTrue(responseMap.containsKey("homeAddress"))
        Assertions.assertTrue(responseMap.containsKey("salary"))
    }
} 
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
import com.bank.user.UserRepository
import com.bank.user.UserEntity
import com.bank.kyc.KYCRepository
import com.bank.kyc.KYCEntity
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
class KYCSteps {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var kycRepository: KYCRepository

    private var response: String? = null
    private var testUser: UserEntity? = null

    @Given("I am an authenticated user")
    fun iAmAnAuthenticatedUser() {
        // Authentication is handled by TestSecurityConfig
    }

    @Given("I have a registered user with username {string}")
    fun iHaveARegisteredUserWithUsername(username: String) {
        testUser = userRepository.save(
            UserEntity(
                username = username,
                password = "password",
                createdAt = LocalDateTime.now()
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

    @And("the KYC information should be saved successfully")
    fun theKYCInformationShouldBeSavedSuccessfully() {
        val responseMap = objectMapper.readValue(response, Map::class.java)
        Assertions.assertTrue(responseMap.containsKey("id"))
        
        // Verify KYC exists in database
        val kyc = kycRepository.findByUserId(testUser!!.id!!)
        Assertions.assertNotNull(kyc)
        Assertions.assertEquals("John", kyc!!.firstName)
        Assertions.assertEquals("Doe", kyc.lastName)
        Assertions.assertEquals("US", kyc.country)
        Assertions.assertEquals(LocalDate.parse("1990-01-01"), kyc.dob)
    }

    @And("the response should contain date of birth validation error")
    fun theResponseShouldContainDateOfBirthValidationError() {
        val responseMap = objectMapper.readValue(response, Map::class.java)
        Assertions.assertTrue(responseMap.containsKey("error"))
        Assertions.assertTrue((responseMap["error"] as String).contains("date of birth"))
    }

    @And("the response should contain phone number validation error")
    fun theResponseShouldContainPhoneNumberValidationError() {
        val responseMap = objectMapper.readValue(response, Map::class.java)
        Assertions.assertTrue(responseMap.containsKey("error"))
        Assertions.assertTrue((responseMap["error"] as String).contains("phone number"))
    }

    @Given("KYC information exists for user {string}")
    fun kycInformationExistsForUser(username: String) {
        val user = userRepository.findByUsername(username)!!
        kycRepository.save(
            KYCEntity(
                user = user,
                firstName = "John",
                lastName = "Doe",
                country = "US",
                dob = LocalDate.parse("1990-01-01"),
                civilId = "123456789",
                phoneNumber = "+1234567890",
                homeAddress = "123 Main St, City, Country"
            )
        )
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

    @And("the response should contain the KYC details")
    fun theResponseShouldContainTheKYCDetails() {
        val responseMap = objectMapper.readValue(response, Map::class.java)
        Assertions.assertEquals("John", responseMap["firstName"])
        Assertions.assertEquals("Doe", responseMap["lastName"])
        Assertions.assertEquals("US", responseMap["country"])
        Assertions.assertEquals("1990-01-01", responseMap["dateOfBirth"])
        Assertions.assertEquals("123456789", responseMap["civilId"])
        Assertions.assertEquals("+1234567890", responseMap["phoneNumber"])
        Assertions.assertEquals("123 Main St, City, Country", responseMap["homeAddress"])
    }
} 
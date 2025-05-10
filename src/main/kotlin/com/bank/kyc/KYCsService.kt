package com.bank.kyc

import com.bank.serverMcCache
import com.bank.user.UserRepository
import com.hazelcast.logging.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
private val loggerKyc = Logger.getLogger("kyc")


@Service
class KYCsService(
    private val kycRepository: KYCRepository,
    private val userRepository: UserRepository
) {
    fun getKYC(userId: Long): ResponseEntity<*> {
        val kycCache = serverMcCache.getMap<Long, KYCResponse>("kyc")

        kycCache[userId]?.let {
            loggerKyc.info("Returning KYC for userId=$userId")
            return ResponseEntity.ok(it)
        }

        val kyc = kycRepository.findByUserId(userId)
            ?: return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "user was not found"))

        val response = KYCResponse(
            firstName = kyc.firstName,
            lastName = kyc.lastName,
            dateOfBirth = kyc.dateOfBirth,
            civilId = kyc.civilId,
            phoneNumber = kyc.phoneNumber,
            homeAddress = kyc.homeAddress,
            salary = kyc.salary,
            country = kyc.country
        )

        loggerKyc.info("No KYC found, caching new data...")
        kycCache[userId] = response
        return ResponseEntity.ok(response)
    }


    fun addOrUpdateKYC(request: KYCRequest, userId: Long): ResponseEntity<Any> {
        val user = userRepository.findById(userId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "user was not found"))

        val existing = kycRepository.findByUserId(userId) // retrieving whatever data available in the KYC database for this user

        val age = java.time.Period.between(request.dateOfBirth, LocalDate.now()).years
        if (age < 18) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "you must be 18 or older to register"))
        }

        val nameRegex = Regex("^[\\p{L} ]{2,50}$") // Unicode letters only, allows Arabic & English names
        val digitsOnlyRegex = Regex("^\\d+$")

        if (!nameRegex.matches(request.firstName)) {
            return ResponseEntity.badRequest().body(mapOf("error" to "first name must only contain letters"))
        }

        if (!nameRegex.matches(request.lastName)) {
            return ResponseEntity.badRequest().body(mapOf("error" to "last name must only contain letters"))
        }

        if (!digitsOnlyRegex.matches(request.civilId) || request.civilId.length != 12) {
            return ResponseEntity.badRequest().body(mapOf("error" to "civil ID must be exactly 12 digits"))
        }

        if (!digitsOnlyRegex.matches(request.phoneNumber) || request.phoneNumber.length != 8) {
            return ResponseEntity.badRequest().body(mapOf("error" to "phone number must be exactly 8 digits"))
        }

        if (request.dateOfBirth.isAfter(LocalDate.now())) {
            return ResponseEntity.badRequest().body(mapOf("error" to "date of birth must be in the past"))
        }

        if (request.salary < BigDecimal(100.000) || request.salary > BigDecimal(1000000.000)) {
            return ResponseEntity.badRequest().body(mapOf("error" to "salary must be between 100 and 1,000,000 KD"))
        }

        val kyc = if (existing != null) { // updating data
            existing.copy(
                user = user,
                firstName = request.firstName,
                lastName = request.lastName,
                dateOfBirth = request.dateOfBirth,
                salary = request.salary
            )
        } else {
            KYCEntity( // making a new KYC profile for this user
                user = user,
                firstName = request.firstName,
                lastName = request.lastName,
                dateOfBirth = request.dateOfBirth,
                civilId = request.civilId,
                phoneNumber = request.phoneNumber,
                homeAddress = request.homeAddress,
                country = request.country,
                salary = request.salary
            )
        }

        kycRepository.save(kyc) // saving the new/updated data

        val kycCache = serverMcCache.getMap<Long, KYCResponse>("kyc")
        loggerKyc.info("KYC for userId=$userId has been updated...invalidating cache")
        kycCache.remove(userId)

        return ResponseEntity.ok(user.id?.let {
            KYCResponse( // returning the results of the operation to the client
                firstName = kyc.firstName,
                lastName = kyc.lastName,
                dateOfBirth = kyc.dateOfBirth,
                civilId = kyc.civilId,
                phoneNumber = kyc.phoneNumber,
                homeAddress = kyc.homeAddress,
                salary = kyc.salary,
                country = kyc.country
            )
        })
    }
}
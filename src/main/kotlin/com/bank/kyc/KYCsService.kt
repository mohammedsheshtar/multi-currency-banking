package com.bank.kyc

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
import java.time.LocalDate
private val loggerKyc = Logger.getLogger("kyc")


@Service
class KYCsService(
    private val kycRepository: KYCRepository,
    private val userRepository: UserRepository,
    private val userMembershipRepository: UserMembershipRepository,
    private val membershipRepository: MembershipRepository
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

        val userMembership = userMembershipRepository.findByUser_Id(userId)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "user is not enrolled in membership program"))

        val response = KYCResponse(
            firstName = kyc.firstName,
            lastName = kyc.lastName,
            dateOfBirth = kyc.dateOfBirth,
            civilId = kyc.civilId,
            phoneNumber = kyc.phoneNumber,
            homeAddress = kyc.homeAddress,
            salary = kyc.salary,
            country = kyc.country,
            tier = userMembership.membershipTier.tierName,
            points = userMembership.tierPoints
        )

        loggerKyc.info("No KYC found, caching new data...")
        kycCache[userId] = response
        return ResponseEntity.ok(response)
    }


    fun addOrUpdateKYC(request: KYCRequest, userId: Long): ResponseEntity<Any> {
        val user = userRepository.findById(userId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "user was not found"))

        val existing = kycRepository.findByUserId(userId)

        val age = java.time.Period.between(request.dateOfBirth, LocalDate.now()).years
        if (age < 18) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "you must be 18 or older to register"))
        }

        val bronzeTier = membershipRepository.findByTierName("BRONZE")
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "could not find membership tier"))

        val nameRegex = Regex("^[\\p{L} ]{2,50}$")
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

        if (request.salary < BigDecimal(100) || request.salary > BigDecimal(1_000_000)) {
            return ResponseEntity.badRequest().body(mapOf("error" to "salary must be between 100 and 1,000,000 KD"))
        }

        val kyc: KYCEntity
        val userMembership: UserMembershipEntity

        if (existing != null) {
            // ✅ UPDATE FLOW — MUST SAVE the updated object
            kyc = kycRepository.save(
                existing.copy(
                    firstName = request.firstName,
                    lastName = request.lastName,
                    dateOfBirth = request.dateOfBirth,
                    salary = request.salary,
                    civilId = request.civilId,
                    phoneNumber = request.phoneNumber,
                    homeAddress = request.homeAddress,
                    country = request.country
                )
            )

            userMembership = userMembershipRepository.findByUser_Id(userId)
                ?: return ResponseEntity.badRequest().body(mapOf("error" to "user is not enrolled in membership program"))

        } else {
            // ✅ CREATE FLOW
            kyc = kycRepository.save(
                KYCEntity(
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
            )

            userMembership = userMembershipRepository.save(
                UserMembershipEntity(
                    user = user,
                    kyc = kyc,
                    membershipTier = bronzeTier,
                    tierPoints = 0
                )
            )
        }

        // ✅ Invalidate cache
        val kycCache = serverMcCache.getMap<Long, KYCResponse>("kyc")
        loggerKyc.info("KYC for userId=$userId has been updated...invalidating cache")
        kycCache.remove(userId)

        // ✅ Return response
        return ResponseEntity.ok(
            user.id?.let {
                KYCResponse(
                    firstName = kyc.firstName,
                    lastName = kyc.lastName,
                    dateOfBirth = kyc.dateOfBirth,
                    civilId = kyc.civilId,
                    phoneNumber = kyc.phoneNumber,
                    homeAddress = kyc.homeAddress,
                    salary = kyc.salary,
                    country = kyc.country,
                    tier = userMembership.membershipTier.tierName,
                    points = userMembership.tierPoints
                )
            }
        )
    }
}
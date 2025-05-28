package com.bank.membership

import com.bank.serverMcCache
import com.hazelcast.logging.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.util.*
private val loggerTiers = Logger.getLogger("tiers")


@Service
class MembershipService(
    private val membershipRepository: MembershipRepository
) {
    fun getAll(userId: Long?): List<ListMembershipResponse> {
        val tiersCache = serverMcCache.getMap<Long, List<ListMembershipResponse>>("tiers")

        tiersCache[userId]?.let {
            loggerTiers.info("returning list of tiers from cache")
            return it
        }

        val memberships = membershipRepository.findAll()
        val response = memberships.map { memberships -> ListMembershipResponse(
            tierName = memberships.tierName,
            memberLimit = memberships.memberLimit,
            discountAmount = memberships.discountAmount
        ) }

        loggerTiers.info("no tiers list found...caching new data")
        tiersCache[userId] = response
        return response
    }

    fun getByTierName(tierName: String): ResponseEntity<*> {
        return try {
            val tierNameUpper = tierName.uppercase(Locale.getDefault())
            ResponseEntity.ok().body(membershipRepository.findByTierName(tierNameUpper))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "membership tier not found"))
        }
    }
}
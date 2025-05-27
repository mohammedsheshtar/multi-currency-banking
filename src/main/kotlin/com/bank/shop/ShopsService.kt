package com.bank.shop

import com.bank.account.AccountRepository
import com.bank.kyc.KYCEntity
import com.bank.serverMcCache
import com.bank.`shop-transaction`.ShopTransactionsEntity
import com.bank.`shop-transaction`.ShopTransactionsRepository
import com.bank.transaction.TransactionHistoryResponse
import com.bank.user.UserRepository
import com.bank.usermembership.UserMembershipRepository
import com.hazelcast.logging.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.time.LocalDateTime
private val loggerShop = Logger.getLogger("shop")
private val loggerShopTransaction = Logger.getLogger("shopTransaction")

@Service
class ShopsService(
    private val shopsRepository: ShopsRepository,
    private val userMembershipRepository: UserMembershipRepository,
    private val shopTransactionsRepository: ShopTransactionsRepository,
    private val userRepository: UserRepository
) {

    fun listItems(userId: Long): ResponseEntity<*> {
        val shopCache = serverMcCache.getMap<Long, List<ListItemsResponse>>("shop")

        shopCache[userId]?.let {
            loggerShop.info("Returning list of shop items for userId=$userId")
            return ResponseEntity.ok(it)
        }

        val membership = userMembershipRepository.findByUser_Id(userId)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "user membership was not found"))

        val userTier = membership.membershipTier.tierName

        val allowedTiers = listOf("BRONZE", "SILVER", "GOLD", "PLATINUM", "DIAMOND")
        val userTierIndex = allowedTiers.indexOf(userTier.uppercase())

        val allItems = shopsRepository.findAll()

        val response = allItems.map { item ->
            val itemTierIndex = allowedTiers.indexOf(item.tierName.uppercase())

            ListItemsResponse(
                itemName = item.itemName,
                tierName = item.tierName,
                pointCost = item.pointCost,
                itemQuantity = item.itemQuantity,
                isPurchasable = itemTierIndex <= userTierIndex
            )
        }


        loggerShop.info("No shop list found, caching new data...")
        shopCache[userId] = response
        return ResponseEntity.ok(response)
    }

    fun buyItem(userId: Long, itemId: Long): ResponseEntity<*> {
//        val account = accountRepository.findById(request.accountId).orElseThrow {
//            IllegalArgumentException("account not found")
//        }

        val user = userRepository.findById(userId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "user not found"))

        val membership = userMembershipRepository.findByUser_Id(userId)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "user membership not found"))

        val item = shopsRepository.findById(itemId).orElse(null)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "item with ID $itemId not found"))


        if (membership.tierPoints < item.pointCost)
            return ResponseEntity.badRequest().body(mapOf("error" to "insufficient points"))

        if (item.itemQuantity <= 0)
            return ResponseEntity.badRequest().body(mapOf("error" to "item out of stock"))

        val userTierRank = MembershipTier.entries.indexOf(
            MembershipTier.valueOf(membership.membershipTier.tierName.uppercase())
        )

        val itemTierRank = MembershipTier.entries.indexOf(
            MembershipTier.valueOf(item.tierName.uppercase())
        )

        if (userTierRank < itemTierRank) {
            return ResponseEntity.badRequest().body(mapOf("error" to "your membership tier does not allow this purchase"))
        }

        val updatedPoints = membership.tierPoints - item.pointCost

        userMembershipRepository.save(
            membership.copy(tierPoints = updatedPoints)
        )

        shopsRepository.save(item.copy(itemQuantity = item.itemQuantity - 1))

        shopTransactionsRepository.save(
            ShopTransactionsEntity(
                user = user,
                tierName = membership.membershipTier.tierName,
                item = item,
                pointsSpent = item.pointCost,
                timeOfTransaction = LocalDateTime.now(),
                userTier = membership.membershipTier
            )
        )

        val shopTransactionCache = serverMcCache.getMap<Long, List<ShopTransactionResponse?>>("shopTransaction")
        loggerShopTransaction.info("shopping history for userId=${userId} has been updated...invalidating cache")

        shopTransactionCache.remove(userId)

        return ResponseEntity.ok().body(PurchaseResponse(
            updatedPoints = updatedPoints
        )

        )
    }

    fun getShopTransaction(userId: Long): ResponseEntity<*> {
        val shopTransactionCache = serverMcCache.getMap<Long, List<ShopTransactionResponse?>>("shopTransaction")

        shopTransactionCache[userId]?.let {
            loggerShop.info("Returning list of shopping history for userId=$userId")
            return ResponseEntity.ok(it)
        }

        val shopHistory = shopTransactionsRepository.findByUser_Id(userId)

        val userMembership = userMembershipRepository.findByUser_Id(userId)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "user membership was not found"))

        val response = shopHistory.map { tx ->
            val item = tx.item.id?.let { shopsRepository.findById(it).orElse(null) }
            item?.let {
                ShopTransactionResponse(
                    itemName = it.itemName,
                    itemTier = it.tierName,
                    pointsSpent = tx.pointsSpent,
                    timeOfTransaction = tx.timeOfTransaction,
                    accountTier = userMembership.membershipTier.tierName
                )
            }
        }
        loggerShop.info("no shopping history found, caching new data...")
        shopTransactionCache[userId] = response
        return ResponseEntity.ok(response)
    }
}

package com.bank.shop

import com.bank.account.AccountRepository
import com.bank.serverMcCache
import com.bank.`shop-transaction`.ShopTransactionsEntity
import com.bank.`shop-transaction`.ShopTransactionsRepository
import com.bank.transaction.TransactionHistoryResponse
import com.bank.usermembership.UserMembershipRepository
import com.hazelcast.logging.Logger
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
    private val accountRepository: AccountRepository
) {

    fun listItems(accountId: Long): ResponseEntity<*> {
        val shopCache = serverMcCache.getMap<Long, List<ListItemsResponse>>("shop")

        shopCache[accountId]?.let {
            loggerShop.info("Returning list of accounts for accountId=$accountId")
            return ResponseEntity.ok(it)
        }

        val membership = userMembershipRepository.findByAccountId(accountId)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "user membership was not found"))

        val userTier = membership.membershipTier.tierName

        val allowedTiers = listOf("BRONZE", "SILVER", "GOLD", "PLATINUM", "DIAMOND")
        val index = allowedTiers.indexOf(userTier.uppercase())
        val visibleTiers = allowedTiers.subList(0, index + 1)
        val items = shopsRepository.findAll().filter { it.tierName in visibleTiers }

        val response = items.map{ items -> ListItemsResponse(
            itemName = items.itemName,
            tierName = items.tierName,
            pointCost = items.pointCost,
            itemQuantity = items.itemQuantity
        ) }

        loggerShop.info("No shop list found, caching new data...")
        shopCache[accountId] = response
        return ResponseEntity.ok(response)
    }

    fun buyItem(request: PurchaseRequest): ResponseEntity<*> {
        val account = accountRepository.findById(request.accountId).orElseThrow {
            IllegalArgumentException("account not found")
        }

        val userMembership = userMembershipRepository.findByAccountId(request.accountId)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "account was not found"))

        val membership = userMembershipRepository.findByAccountId(request.accountId)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "user membership not found"))

        val item = shopsRepository.findByItemNameIgnoreCase(request.itemName.lowercase().trim().lowercase())
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "item not found"))

        if (membership.tierPoints < item.pointCost)
            return ResponseEntity.badRequest().body(mapOf("error" to "insufficient points"))

        if (item.itemQuantity <= 0)
            return ResponseEntity.badRequest().body(mapOf("error" to "item out of stock"))

        val updatedPoints = membership.tierPoints - item.pointCost

        userMembershipRepository.save(
            membership.copy(tierPoints = updatedPoints)
        )

        shopsRepository.save(item.copy(itemQuantity = item.itemQuantity - 1))

        shopTransactionsRepository.save(
            ShopTransactionsEntity(
                account = account,
                tierName = membership.membershipTier.tierName,
                item = item,
                pointsSpent = item.pointCost,
                timeOfTransaction = LocalDateTime.now(),
                accountTier = userMembership.membershipTier

            )
        )

        val shopTransactionCache = serverMcCache.getMap<Long, List<ShopTransactionResponse?>>("shopTransaction")
        loggerShopTransaction.info("shopping history for account=${account.id} has been updated...invalidating cache")

        shopTransactionCache.remove(account.id)

        return ResponseEntity.ok().body(PurchaseResponse(
            updatedPoints = updatedPoints
        )

        )
    }

    fun getShopTransaction(accountId: Long): ResponseEntity<*> {
        val shopTransactionCache = serverMcCache.getMap<Long, List<ShopTransactionResponse?>>("shopTransaction")

        shopTransactionCache[accountId]?.let {
            loggerShop.info("Returning list of shopping history for accountId=$accountId")
            return ResponseEntity.ok(it)
        }

        val shopHistory = shopTransactionsRepository.findByAccountId(accountId)

        val userMembership = userMembershipRepository.findByAccountId(accountId)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "account was not found"))

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
        shopTransactionCache[accountId] = response
        return ResponseEntity.ok(response)
    }
}

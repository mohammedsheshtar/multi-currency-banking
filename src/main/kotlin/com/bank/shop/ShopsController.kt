package com.bank.shop

import com.bank.account.AccountRepository
import com.bank.kyc.KYCRepository
import com.bank.user.UserRepository
import io.swagger.v3.oas.annotations.tags.Tag
import org.apache.logging.log4j.util.StringMap
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDateTime

@Tag(name="ShopAPI")

@RestController
class ShopController(
    private val shopsService: ShopsService,
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository,
    private val kycRepository: KYCRepository
) {

    @GetMapping("/api/v1/shop/items")
    fun viewItems(): ResponseEntity<*> {
        val username = SecurityContextHolder.getContext().authentication.name

        val user = userRepository.findByUsername(username)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "user not found"))

        val userId = user.id ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(mapOf("error" to "user ID not found"))

        val kyc = user.id?.let { kycRepository.findByUserId(it) }
            ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to "account not found"))

        if (kyc.user.id != user.id) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to "user ID does not belong to user"))
        }

        return shopsService.listItems(userId)
    }

    @PostMapping("/api/v1/shop/buy/{itemId}")
    fun buyItem(@PathVariable itemId: Long): ResponseEntity<*> {
        val username = SecurityContextHolder.getContext().authentication.name

        val user = userRepository.findByUsername(username)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "user not found"))

        val userId = user.id ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(mapOf("error" to "user ID not found"))

        val kyc = user.id?.let { kycRepository.findByUserId(it) }
            ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to "account not found"))

        if (kyc.user.id != user.id) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to "user ID does not belong to user"))
        }

        return shopsService.buyItem(userId, itemId)
    }

    @GetMapping("/api/v1/shop/history")
    fun getShopTransaction(): ResponseEntity<*> {
        val username = SecurityContextHolder.getContext().authentication.name
        val user = userRepository.findByUsername(username)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "User not found"))

        val userId = user.id ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(mapOf("error" to "user ID not found"))

        val kyc = user.id?.let { kycRepository.findByUserId(it) }
            ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to "account not found"))

        if (kyc.user.id != user.id) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to "Account does not belong to user"))
        }

        return shopsService.getShopTransaction(userId)
    }

}

//data class PurchaseRequest(
//    val itemId: Long
//)

data class PurchaseResponse(
    val updatedPoints: Int,
)

data class ListItemsResponse(
    val itemName: String,
    val tierName: String,
    val pointCost: Int,
    val itemQuantity: Int,
    val isPurchasable: Boolean
) : Serializable

data class ShopTransactionResponse(
    val itemName: String,
    val itemTier: String,
    val accountTier: String,
    val pointsSpent: Int,
    val timeOfTransaction: LocalDateTime
)


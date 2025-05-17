package com.bank.shop

import com.bank.account.AccountRepository
import com.bank.user.UserRepository
import io.swagger.v3.oas.annotations.tags.Tag
import org.apache.logging.log4j.util.StringMap
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Tag(name="ShopAPI")

@RestController
class ShopController(
    private val shopsService: ShopsService,
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository
) {

    @GetMapping("/api/v1/shop/items/{accountId}")
    fun viewItems(@PathVariable accountId: Long): ResponseEntity<*> {
        val username = SecurityContextHolder.getContext().authentication.name
        val user = userRepository.findByUsername(username)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "User not found"))

        val account = accountRepository.findById(accountId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to "Account not found"))

        if (account.user.id != user.id) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to "Account does not belong to user"))
        }

        return shopsService.listItems(accountId)
    }

    @PostMapping("/api/v1/shop/buy")
    fun buyItem(@RequestBody request: PurchaseRequest): ResponseEntity<*> {
        return shopsService.buyItem(request)
    }

    @GetMapping("/api/v1/shop/history/{accountId}")
    fun getShopTransaction(@PathVariable accountId: Long): ResponseEntity<*> {
        val username = SecurityContextHolder.getContext().authentication.name
        val user = userRepository.findByUsername(username)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "User not found"))

        val account = accountRepository.findById(accountId).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to "Account not found"))

        if (account.user.id != user.id) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to "Account does not belong to user"))
        }

        return shopsService.getShopTransaction(accountId)
    }

}

data class PurchaseRequest(
    val accountId: Long,
    val itemId: Long
)

data class PurchaseResponse(
    val updatedPoints: Int,
)

data class ListItemsResponse(
    val itemName: String,
    val tierName: String,
    val pointCost: Int,
    val itemQuantity: Int,
    val isPurchasable: Boolean
)

data class ShopTransactionResponse(
    val itemName: String,
    val itemTier: String,
    val accountTier: String,
    val pointsSpent: Int,
    val timeOfTransaction: LocalDateTime
)


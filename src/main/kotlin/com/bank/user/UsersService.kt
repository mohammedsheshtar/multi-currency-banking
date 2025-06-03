package com.bank.user

import com.bank.authentication.jwt.JwtService
import com.bank.role.RoleEntity
import com.bank.role.RoleName
import com.bank.role.RoleRepository
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class UsersService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val roleRepository: RoleRepository,
    private val jwtService: JwtService
) {
    fun registerUser(request: CreateUserDTO): ResponseEntity<Any> {
        if (userRepository.existsByUsername(request.username)) {
            return ResponseEntity
                .badRequest()
                .body(mapOf("error" to "Username '${request.username}' is already taken."))
        }

        if (request.username.length >= 12) {
            return ResponseEntity
                .badRequest()
                .body(mapOf("error" to "Username '${request.username}' is too long."))
        }

        if (request.username.length <= 4) {
            return ResponseEntity
                .badRequest()
                .body(mapOf("error" to "Username '${request.username}' is too short."))
        }

        if (request.password.length < 6) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "password must be at least 6 characters"))
        }

        if (request.password.length > 20) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "password must be less than 20 characters"))
        }

        if (!request.password.any { it.isUpperCase() }) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "password must have at least one capital letter"))
        }

        if (!request.password.any { it.isDigit() }) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "password must have at least one digit"))
        }

        val hashedPassword = passwordEncoder.encode(request.password)
        val user = userRepository.save(
            UserEntity(
                username = request.username,
                password = hashedPassword,
                createdAt = LocalDateTime.now()
            )
        )

        roleRepository.save(RoleEntity(user = user, roleName = RoleName.CUSTOMER))

        val token = jwtService.generateToken(user.username)

        return ResponseEntity.ok(mapOf("token" to token))
    }
}
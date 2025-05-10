package com.bank.authentication


import com.bank.authentication.jwt.JwtService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.authentication.*
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.annotation.*

@Tag(name="AuthenticationAPI")
@RestController
@RequestMapping("/authentication")
class AuthenticationController(
    private val authenticationManager: AuthenticationManager,
    private val userDetailsService: UserDetailsService,
    private val jwtService: JwtService
) {

    @PostMapping("/api/v1/authentication/login")
    fun login(@RequestBody authRequest: AuthenticationRequest): AuthenticationResponse {
        val authToken = UsernamePasswordAuthenticationToken(authRequest.username, authRequest.password)
        val authentication = authenticationManager.authenticate(authToken)

        if (authentication.isAuthenticated) {
            val userDetails = userDetailsService.loadUserByUsername(authRequest.username)
            val token = jwtService.generateToken(userDetails.username)
            return AuthenticationResponse (token)
        } else {
            throw UsernameNotFoundException("Invalid user request!")
        }
    }
}

data class AuthenticationRequest(
    val username: String,
    val password: String
)

data class AuthenticationResponse(
    val token: String
)
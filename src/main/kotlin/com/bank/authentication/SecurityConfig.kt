package com.bank.authentication


import com.bank.authentication.jwt.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter


@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthenticationFilter,
    private val userDetailsService: UserDetailsService
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf { it.disable() }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/authentication/api/v1/authentication/login",
                    "/api/v1/authentication/register",
                    "/api-docs",
                    "/api/welcome/v1/**").permitAll()
                it.requestMatchers(
                    "/api/v1/users/kyc",
                    "/api/v1/users/accounts",
                    "/api/v1/users/accounts/{accountNumber}",
                    "/api/v1/accounts/deposit",
                    "/api/v1/accounts/withdraw",
                    "/api/v1/accounts/transfer",
                    "/api/v1/memberships/tier/{name}",
                    "/api/v1/memberships",
                    "/api/v1/shop/items",
                    "/api/v1/shop/buy/{itemId}",
                    "/api/v1/accounts/transactions/{accountId}",
                    "/api/v1/shop/history",
                    "/api/v1/currencies",
                    "/api/v1/conversion/rates",
                    "/api/v1/conversion/rate").authenticated()
                    .anyRequest().authenticated()
            }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager =
        config.authenticationManager

    @Bean
    fun authenticationProvider(): AuthenticationProvider {
        val provider = DaoAuthenticationProvider()
        provider.setUserDetailsService(userDetailsService)
        provider.setPasswordEncoder(passwordEncoder())
        return provider
    }
}
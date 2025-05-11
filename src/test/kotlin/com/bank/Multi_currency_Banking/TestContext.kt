package com.bank.Multi_currency_Banking

import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import com.fasterxml.jackson.databind.ObjectMapper

class TestContext {
    val restTemplate = TestRestTemplate()
    val baseUrl = "http://localhost:9000"
    val objectMapper = ObjectMapper()
    private var authToken: String? = null

    init {
        // Register and authenticate a test user
        val registerRequest = mapOf(
            "username" to "testuser",
            "email" to "test@example.com",
            "password" to "testpass123"
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val registerEntity = HttpEntity(registerRequest, headers)
        restTemplate.exchange(
            "$baseUrl/api/v1/auth/register",
            HttpMethod.POST,
            registerEntity,
            Any::class.java
        )

        val loginRequest = mapOf(
            "username" to "testuser",
            "password" to "testpass123"
        )

        val loginEntity = HttpEntity(loginRequest, headers)
        val loginResponse = restTemplate.exchange(
            "$baseUrl/api/v1/auth/login",
            HttpMethod.POST,
            loginEntity,
            Map::class.java
        )

        authToken = loginResponse.body?.get("token") as? String
    }

    fun getHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        authToken?.let { headers.setBearerAuth(it) }
        return headers
    }
} 
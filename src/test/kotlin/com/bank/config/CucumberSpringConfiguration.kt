package com.bank.config

import org.springframework.boot.test.context.SpringBootTest
import io.cucumber.spring.CucumberContextConfiguration
import com.bank.MultiCurrencyBankingApplication
import org.springframework.test.context.ActiveProfiles

@CucumberContextConfiguration
@SpringBootTest(classes = [MultiCurrencyBankingApplication::class])
@ActiveProfiles("test")
class CucumberSpringConfiguration 
package com.bank.welcome

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name="WelcomeAPI")
@RestController
class WelcomePage(){
    @GetMapping("/api/welcome/v1")
    fun welcomeMessage(): String{
return "Welcome to XChange! where the world is in your hands!"
    }
}
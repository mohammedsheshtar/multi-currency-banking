package com.bank.welcome

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class WelcomePage(){
    @GetMapping("/api/welcome")
    fun welcomeToXChange(): String{
return "Welcome to XChange! where the world is in your hands!"
    }
}
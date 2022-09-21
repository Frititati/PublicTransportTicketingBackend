package it.polito.wa2.registration_login.controllers

import it.polito.wa2.registration_login.dtos.LoginDTO
import it.polito.wa2.registration_login.services.LoginService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class LoginEndpoint (val loginService: LoginService) {

    @PostMapping("/user/login")
    fun login(@RequestBody credentials:LoginDTO): ResponseEntity<String> {
        val loginStatus = loginService.login(credentials)
        return ResponseEntity.status(loginStatus.first).body(loginStatus.second)
    }
}
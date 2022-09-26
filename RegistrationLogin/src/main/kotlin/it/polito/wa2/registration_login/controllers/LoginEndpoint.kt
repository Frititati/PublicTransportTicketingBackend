package it.polito.wa2.registration_login.controllers

import it.polito.wa2.registration_login.dtos.LoginDTO
import it.polito.wa2.registration_login.services.LoginService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class LoginEndpoint(val loginService: LoginService) {

    @PostMapping("/user/login")
    fun loginUser(@RequestBody credentials: LoginDTO): ResponseEntity<LoginJWT> {
        val loginStatus = loginService.loginUser(credentials)
        return ResponseEntity.status(loginStatus.first).body(LoginJWT(loginStatus.second))
    }

    @PostMapping("/device/login")
    fun loginDevice(@RequestBody credentials: LoginDTO): ResponseEntity<LoginJWT> {
        val loginStatus = loginService.loginDevice(credentials)
        return ResponseEntity.status(loginStatus.first).body(LoginJWT(loginStatus.second))
    }
}

data class LoginJWT(val jwt: String?)
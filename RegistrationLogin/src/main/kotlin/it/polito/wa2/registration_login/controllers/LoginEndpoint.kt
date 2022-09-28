package it.polito.wa2.registration_login.controllers

import it.polito.wa2.registration_login.dtos.LoginDTO
import it.polito.wa2.registration_login.dtos.LoginJWTDTO
import it.polito.wa2.registration_login.services.LoginService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
class LoginEndpoint(val loginService: LoginService) {

    /**
     * @param credentials {
     *                      username: String
     *                      password: String
     *                    }
     *
     * @return If username and password are correct, it returns the JWT that will be inserted in the header of
     *          other calls, so the user will be authorized to perform other actions
     */
    @PostMapping("/user/login")
    suspend fun loginUser(@RequestBody @Valid credentials: LoginDTO): ResponseEntity<LoginJWTDTO> {
        val loginStatus = loginService.loginUser(credentials)
        return ResponseEntity(loginStatus.second, loginStatus.first)
    }

    /**
     * @param credentials {
     *                      username: String
     *                      password: String
     *                    }
     *
     * @return If username and password are correct, it returns the JWT that will be inserted in the header of
     *          other calls, so the device will be authorized to perform other actions
     */
    @PostMapping("/device/login")
    suspend fun loginDevice(@RequestBody @Valid credentials: LoginDTO): ResponseEntity<LoginJWTDTO> {
        val loginStatus = loginService.loginDevice(credentials)
        return ResponseEntity(loginStatus.second, loginStatus.first)
    }
}
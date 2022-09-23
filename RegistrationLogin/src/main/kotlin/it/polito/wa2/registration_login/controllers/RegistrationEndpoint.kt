package it.polito.wa2.registration_login.controllers

import it.polito.wa2.registration_login.dtos.RegistrationDTO
import it.polito.wa2.registration_login.dtos.ValidateDTO
import it.polito.wa2.registration_login.services.DeviceService
import it.polito.wa2.registration_login.services.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
class RegistrationEndpoint(val userService: UserService, val deviceService: DeviceService) {

    @PostMapping("/user/register")
    fun registerUser(@RequestBody payload: RegistrationDTO): ResponseEntity<RegistrationToValidate> {
        val registrationStatus: Pair<HttpStatus, UUID?> = userService.register(payload)

        return if (registrationStatus.first === HttpStatus.ACCEPTED)
            ResponseEntity.status(registrationStatus.first)
                .body(RegistrationToValidate(registrationStatus.second, payload.email))
        else
            ResponseEntity.status(registrationStatus.first).body(null)
    }

    @PostMapping("/user/validate")
    fun validateUser(@RequestBody payload: ValidateRegistration): ResponseEntity<ValidateDTO> {
        val validationStatus: Pair<HttpStatus, ValidateDTO?> =
            userService.validate(payload.provisional_id, payload.activation_code)

        return ResponseEntity.status(validationStatus.first).body(validationStatus.second)
    }

    @PostMapping("/iot/register")
    fun registerIOT(@RequestBody payload: RegistrationDTO) : ResponseEntity<RegistrationToValidate> {
        return ResponseEntity(null, HttpStatus.OK)
    }
}

data class RegistrationToValidate(val provisional_id: UUID?, val email: String)
data class ValidateRegistration(val provisional_id: String, val activation_code: Int)
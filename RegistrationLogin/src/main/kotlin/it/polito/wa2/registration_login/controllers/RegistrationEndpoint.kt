package it.polito.wa2.registration_login.controllers

import it.polito.wa2.registration_login.dtos.DeviceRegistrationDTO
import it.polito.wa2.registration_login.dtos.RegistrationToValidateDTO
import it.polito.wa2.registration_login.dtos.UserRegistrationDTO
import it.polito.wa2.registration_login.dtos.ValidateDTO
import it.polito.wa2.registration_login.services.RegisterService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
class RegistrationEndpoint(val registerService: RegisterService) {

    @PostMapping("/user/register")
    suspend fun registerUser(@RequestBody payload: UserRegistrationDTO): ResponseEntity<RegistrationToValidateDTO?> {
        val registrationStatus = registerService.registerUser(payload)

        return ResponseEntity(registrationStatus.second, registrationStatus.first)
    }

    @PostMapping("/user/validate")
    suspend fun validateUser(@RequestBody payload: ValidateRegistration): ResponseEntity<ValidateDTO> {
        val validationStatus = registerService.validate(payload.provisional_id, payload.activation_code)

        return ResponseEntity(validationStatus.second, validationStatus.first)
    }

    @PostMapping("/device/register")
    suspend fun registerDevice(@RequestBody payload: DeviceRegistrationDTO): ResponseEntity<DeviceRegistered> {

        val registrationStatus: Pair<HttpStatus, String?> = registerService.registerDevice(payload)

        return if (registrationStatus.first === HttpStatus.ACCEPTED)
            ResponseEntity.status(registrationStatus.first)
                .body(DeviceRegistered(registrationStatus.second))
        else
            ResponseEntity.status(registrationStatus.first).body(null)
    }
}

data class ValidateRegistration(val provisional_id: String, val activation_code: Int)
data class DeviceRegistered(val name: String?)
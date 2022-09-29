package it.polito.wa2.registration_login.controllers

import it.polito.wa2.registration_login.dtos.*
import it.polito.wa2.registration_login.services.RegisterService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
class RegistrationEndpoint(val registerService: RegisterService) {

    /**
     * @param payload {
     *                   username: String
     *                   password: String (at least 8 characters, no empty spaces, one uppercase letter,
     *                                     one lowercase letter, a special character and a digit)
     *                   email: String (email format)
     *                }
     *
     *  If username and email are not already used, it creates a new user
     *
     *  @return provisional_id and email as JSON
     *  Also, it sends to the user a mail with the activation_code
     *  The account is still not active
     */
    @PostMapping("/user/register")
    suspend fun registerUser(@RequestBody @Valid payload: UserRegistrationDTO): ResponseEntity<RegistrationToValidateDTO?> {
        val registrationStatus = registerService.registerUser(payload)
        return ResponseEntity(registrationStatus.second, registrationStatus.first)
    }

    /**
     * @param payload {
     *                   provisional_id : String (UUID)
     *                   activation_code : Int (6 digits)
     *                }
     *
     *  If provisional_id and activation_code are correct, the user will be activated,
     *
     *  @return user_id, username and email
     *
     *  User have 5 attempts to insert the right activation_code, otherwise the account will be eliminated
     */
    @PostMapping("/user/validate")
    suspend fun validateUser(@RequestBody @Valid payload: ValidateRegistrationDTO): ResponseEntity<ValidateDTO> {
        val validationStatus = registerService.validate(payload.provisional_id, payload.activation_code)

        return ResponseEntity(validationStatus.second, validationStatus.first)
    }

    /**
     * @param payload {
     *                  name: String
     *                  password: String (at least 8 characters, no empty spaces, one uppercase letter,
     *                                     one lowercase letter, a special character and a digit)
     *                  zone: String
     *                }
     *
     *  If the user is logged as admin, it creates a device
     *
     *  @return name of the device created by the admin
     */
    @PostMapping("/device/register")
    suspend fun registerDevice(@RequestBody @Valid payload: DeviceRegistrationDTO): ResponseEntity<DeviceRegisteredDTO> {

        val registrationStatus = registerService.registerDevice(payload)

        return ResponseEntity(registrationStatus.second, registrationStatus.first)
    }
}
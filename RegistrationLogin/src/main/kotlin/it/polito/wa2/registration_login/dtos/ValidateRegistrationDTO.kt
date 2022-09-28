package it.polito.wa2.registration_login.dtos

import javax.validation.constraints.*

data class ValidateRegistrationDTO(
    @get:NotEmpty(message = "Provisional ID cannot be empty")
    @get:NotNull(message = "Provisional ID cannot be null")
    val provisional_id: String,
    @get:NotNull(message = "Activation code cannot be null")
    @get:Min(100000, message = "Activation code have 6 digits")
    @get:Max(999999, message = "Activation code have 6 digits")
    val activation_code: Int,
)

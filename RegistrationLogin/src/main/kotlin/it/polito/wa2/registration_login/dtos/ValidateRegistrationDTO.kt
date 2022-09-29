package it.polito.wa2.registration_login.dtos

import javax.validation.constraints.*

data class ValidateRegistrationDTO(
    @get:NotEmpty(message = "Provisional ID cannot be empty")
    @get:NotNull(message = "Provisional ID cannot be null")
    val provisional_id: String,
    @get:NotNull(message = "Activation code cannot be null")
    val activation_code: Int,
)

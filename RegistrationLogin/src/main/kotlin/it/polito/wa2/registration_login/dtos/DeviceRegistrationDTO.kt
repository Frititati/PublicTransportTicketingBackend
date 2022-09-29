package it.polito.wa2.registration_login.dtos

import javax.validation.constraints.*

data class DeviceRegistrationDTO(

    @get:NotEmpty(message = "Name cannot be empty")
    @get:NotNull(message = "Name cannot be null")
    val name: String,

    @get:NotEmpty(message = "Password cannot be empty")
    @get:NotNull(message = "Password cannot be null")
    @get:Size(min = 8, message = "Password have at least 8 characters")
    val password: String,

    @get:NotEmpty(message = "Zone cannot be empty")
    @get:NotNull(message = "Zone cannot be null")
    val zone: String
)


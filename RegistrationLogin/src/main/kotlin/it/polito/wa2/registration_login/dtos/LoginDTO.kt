package it.polito.wa2.registration_login.dtos

import javax.validation.constraints.*

data class LoginDTO(
    @get:NotEmpty(message = "Username cannot be empty")
    @get:NotNull(message = "Username cannot be null")
    var username: String,

    @get:NotEmpty(message = "Password cannot be empty")
    @get:NotNull(message = "Password cannot be null")
    var password: String
)

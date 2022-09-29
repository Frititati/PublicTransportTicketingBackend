package it.polito.wa2.registration_login.dtos

import javax.validation.constraints.*

data class UserRegistrationDTO(

    @get:NotEmpty(message = "Username cannot be empty")
    @get:NotNull(message = "Username cannot be null")
    var username: String,

    @get:NotEmpty(message = "Password cannot be empty")
    @get:NotNull(message = "Password cannot be null")
    @get:Size(min = 8, message = "Password must have at least 8 characters")
    var password: String,

    @get:NotEmpty(message = "Email cannot be empty")
    @get:NotNull(message = "Email cannot be null")
    @get:Email(message = "Email must be in email format")
    var email: String
)

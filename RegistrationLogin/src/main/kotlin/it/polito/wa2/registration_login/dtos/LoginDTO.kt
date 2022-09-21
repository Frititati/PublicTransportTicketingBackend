package it.polito.wa2.registration_login.dtos

import javax.validation.constraints.*

data class LoginDTO(
    @field:NotEmpty
    @field:NotNull
    var username: String,

    @field:NotEmpty
    @field:NotNull
    var password: String
)

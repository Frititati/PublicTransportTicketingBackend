package it.polito.wa2.registration_login.dtos

import javax.validation.constraints.*

data class DeviceRegistrationDTO(

    @field:NotEmpty
    @field:NotNull
    val name: String,

    @field:NotEmpty
    @field:NotNull
    @field:Size(min = 8)
    val password: String,

    @field:NotEmpty
    @field:NotNull
    val zone: String
)

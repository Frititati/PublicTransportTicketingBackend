package it.polito.wa2.registration_login.dtos

data class ValidateDTO(
    val userId : Long,
    val username : String,
    val email : String
)

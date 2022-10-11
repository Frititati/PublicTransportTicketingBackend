package it.polito.wa2.registration_login.entities

import com.fasterxml.jackson.annotation.JsonProperty

data class UserRegister(
    @JsonProperty("username")
    val username: String
)
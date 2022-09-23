package it.polito.wa2.registration_login.dtos

import it.polito.wa2.registration_login.security.Role

data class UserLoggedDTO(val username : String, val role : Role)

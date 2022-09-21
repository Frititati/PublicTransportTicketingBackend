package it.polito.wa2.travel.dtos

import it.polito.wa2.travel.security.Role

data class UserLoggedDTO(val username : String, val role : Role)

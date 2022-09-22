package it.polito.wa2.transit.dtos

import it.polito.wa2.transit.security.Role


data class UserLoggedDTO(val username : String, val role : Role)

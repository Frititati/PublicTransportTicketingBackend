package it.polito.wa2.payment.dtos

import it.polito.wa2.payment.security.Role

data class UserLoggedDTO(val username : String, val role : Role)

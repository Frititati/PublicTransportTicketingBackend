package it.polito.wa2.ticketcatalogue.dtos

import it.polito.wa2.ticketcatalogue.security.Role

data class UserLoggedDTO(val username : String, val role : Role)

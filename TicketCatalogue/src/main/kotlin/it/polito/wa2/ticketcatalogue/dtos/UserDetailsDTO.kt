package it.polito.wa2.ticketcatalogue.dtos

import java.time.LocalDateTime

data class UserDetailsDTO(
    var name : String?,
    var address: String?,
    var dateOfBirth : LocalDateTime?,
    var telephoneNumber : Long?,
)

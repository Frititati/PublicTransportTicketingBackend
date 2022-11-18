package it.polito.wa2.ticketcatalogue.dtos

data class UserDetailsDTO(
    var name : String?,
    var address: String?,
    var dateOfBirth : String?,
    var telephoneNumber : Long?,
)

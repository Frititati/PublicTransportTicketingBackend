package it.polito.wa2.ticketcatalogue.dtos

data class AvailableTicketCreationDTO(
    val price: Double,
    val type: String,
    val minAge : Long?,
    val maxAge : Long?,
    val zones : String
)

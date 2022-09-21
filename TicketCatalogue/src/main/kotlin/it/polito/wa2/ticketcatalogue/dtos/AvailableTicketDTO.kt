package it.polito.wa2.ticketcatalogue.dtos

import it.polito.wa2.ticketcatalogue.entities.AvailableTicket
import it.polito.wa2.ticketcatalogue.entities.TicketType

data class AvailableTicketDTO(
    var ticketId: Long?,
    var price: Double,
    var type: TicketType,
    var minAge : Long,
    var maxAge : Long
)

fun AvailableTicket.toDTO() : AvailableTicketDTO {
    return AvailableTicketDTO(ticketId, price, type, minAge, maxAge)
}

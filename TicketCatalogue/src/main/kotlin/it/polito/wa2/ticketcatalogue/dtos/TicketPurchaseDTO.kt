package it.polito.wa2.ticketcatalogue.dtos

import it.polito.wa2.ticketcatalogue.entities.TicketType
import java.time.LocalDateTime

data class TicketPurchaseDTO(
    val cmd: String,
    val quantity: Int,
    val zones: String,
    val type : TicketType,
    val exp : LocalDateTime?
)

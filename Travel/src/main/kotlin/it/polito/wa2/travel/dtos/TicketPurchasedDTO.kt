package it.polito.wa2.travel.dtos

import it.polito.wa2.travel.entities.TicketPurchased
import java.time.LocalDateTime
import java.util.*

data class TicketPurchasedDTO(val ticketID : UUID?, val issuedAt : LocalDateTime, val exp : LocalDateTime, val zid : String, val type : String, val jws : String)

fun TicketPurchased.toDTO() : TicketPurchasedDTO {
    return TicketPurchasedDTO(ticketID, issuedAt, exp, zid, type, jws)
}

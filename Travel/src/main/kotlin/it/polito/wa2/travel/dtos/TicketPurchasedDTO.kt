package it.polito.wa2.travel.dtos

import it.polito.wa2.travel.entities.TicketPurchased
import java.time.LocalDateTime
import java.util.*

data class TicketPurchasedDTO(val id : UUID?, val issuedAt : LocalDateTime, val exp : LocalDateTime, val zid : Set<String>, val type : String, val jws : String)

fun TicketPurchased.toDTO() : TicketPurchasedDTO {
    return TicketPurchasedDTO(id, issuedAt, exp, zid, type, jws)
}

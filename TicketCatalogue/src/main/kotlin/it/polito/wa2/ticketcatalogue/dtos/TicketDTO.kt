package it.polito.wa2.ticketcatalogue.dtos

import java.time.LocalDateTime
import java.util.*

data class TicketDTO(
    val id : UUID?,
    val issuedAt : LocalDateTime,
    val exp : LocalDateTime,
    val zid : Set<String>,
    val type : String,
    val jws : String
)

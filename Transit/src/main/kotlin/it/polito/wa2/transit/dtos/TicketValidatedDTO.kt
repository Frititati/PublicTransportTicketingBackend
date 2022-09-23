package it.polito.wa2.transit.dtos

import it.polito.wa2.transit.entities.TicketValidated
import java.time.LocalDateTime
import java.util.*

data class TicketValidatedDTO(var id: Long?, val ticketId : UUID, val validationDate: LocalDateTime, val zid: String)

fun TicketValidated.toDTO(): TicketValidatedDTO {
    return TicketValidatedDTO(id, ticketId, validationDate, zid)
}
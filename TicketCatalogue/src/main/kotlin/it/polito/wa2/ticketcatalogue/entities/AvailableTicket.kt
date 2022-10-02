package it.polito.wa2.ticketcatalogue.entities

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("available_tickets")
data class AvailableTicket(
    @Id
    @Column("ticket_id")
    var ticketId: Long?,
    @Column("price")
    var price: Double,
    @Column("type")
    var type: TicketType,
    @Column("min_age")
    var minAge : Long,
    @Column("max_age")
    var maxAge : Long,
    @Column("zones")
    var zones : String
)
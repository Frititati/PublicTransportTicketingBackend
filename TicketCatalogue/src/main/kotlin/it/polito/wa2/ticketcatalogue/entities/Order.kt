package it.polito.wa2.ticketcatalogue.entities

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("orders")
data class Order(
    @Id
    @Column("id")
    var id: Long?,
    @Column("nickname")
    var nickname : String,
    @Column("number_tickets")
    var numberTickets: Int,
    @Column("ticket_id")
    var ticketId: Long,
    @Column("status")
    var status: PaymentStatus,
    @Column("price")
    var price: Double
)
package it.polito.wa2.travel.entities

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.*

@Table("ticket_purchased")
class TicketPurchased  (
    @Id
    @Column("id")
    var id : Long?,
    @Column("ticket_id")
    var ticketID: UUID?, // sub
    @Column("issued_at")
    val issuedAt : LocalDateTime, // iat
    @Column("exp")
    val exp : LocalDateTime,
    @Column("zid")
    val zid : String,

    @Column("type")
    val type : String,
    @Column("jws")
    val jws : String,
    @Column("user_id")
    val userID : Long,
)
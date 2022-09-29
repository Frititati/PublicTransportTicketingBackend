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
    var id: UUID?, // sub
    @Column("issued_at")
    val issuedAt : LocalDateTime, // iat
    @Column("exp")
    val exp : LocalDateTime,

    // TODO here we make a string and later process it using ',' as seperator
    @Column("zid")
    val zid : Set<String>,

    @Column("type")
    val type : String,
    @Column("jws")
    val jws : String,
    @Column("user_id")
    val userID : Long,
)
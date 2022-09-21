package it.polito.wa2.travel.entities

import java.time.LocalDateTime
import java.util.*
import javax.persistence.*

@Entity
class TicketPurchased  (
    @Id
    var id: UUID?, // sub
    val issuedAt : LocalDateTime, // iat
    val exp : LocalDateTime,
    @ElementCollection
    val zid : Set<String>,

    val type : String,

    @Column(length = 1024)
    val jws : String,

    @ManyToOne
    var userDetails: UserDetails? = null
)
package it.polito.wa2.transit.entities
import java.time.LocalDateTime
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("ticket_validated")
class TicketValidated (
    @Id
    @Column("id")
    val id: UUID,
    @Column("validation_date")
    val validationDate: LocalDateTime,
    @Column("zid")
    val zid: String
    // TODO add user
)
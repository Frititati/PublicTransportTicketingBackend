package it.polito.wa2.travel.entities

import java.time.LocalDateTime
import javax.persistence.*

@Entity
class UserDetails (
    @Id
    @GeneratedValue(strategy= GenerationType.SEQUENCE)
    val id : Long?,
    val nickname: String,
    var name : String?,
    var address: String?,
    var dateOfBirth : LocalDateTime?,
    var telephoneNumber : Long?,

    @OneToMany(mappedBy = "userDetails")
    val ticketPurchased: MutableSet<TicketPurchased> = mutableSetOf()
) {

    fun addTicket(t: TicketPurchased) {
        t.userDetails = this
        ticketPurchased.add(t)
    }
}
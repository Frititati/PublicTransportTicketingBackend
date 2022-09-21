package it.polito.wa2.travel.repositories

import it.polito.wa2.travel.entities.TicketPurchased
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TicketPurchasedRepository : CrudRepository<TicketPurchased, Long> {
    fun findAllByUserDetails_Nickname(nickname: String) : List<TicketPurchased>

}
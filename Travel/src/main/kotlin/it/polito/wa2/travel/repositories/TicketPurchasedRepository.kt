package it.polito.wa2.travel.repositories

import it.polito.wa2.travel.entities.TicketPurchased
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Repository
interface TicketPurchasedRepository : R2dbcRepository<TicketPurchased, Long> {
    fun findAllByUserID(userID: Long) : Flux<TicketPurchased>

}
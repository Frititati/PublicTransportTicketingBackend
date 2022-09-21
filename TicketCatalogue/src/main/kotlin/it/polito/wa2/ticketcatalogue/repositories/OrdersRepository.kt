package it.polito.wa2.ticketcatalogue.repositories

import it.polito.wa2.ticketcatalogue.entities.Order
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux

interface OrdersRepository : R2dbcRepository<Order, Long> {

    fun findAllByNickname(nickname : String) : Flux<Order>

}
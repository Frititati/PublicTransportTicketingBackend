package it.polito.wa2.ticketcatalogue.repositories

import it.polito.wa2.ticketcatalogue.entities.Order
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import java.time.LocalDateTime

interface OrdersRepository : R2dbcRepository<Order, Long> {

    fun findAllByNickname(nickname: String): Flux<Order>

    fun findOrderByPurchaseDateGreaterThanEqualAndPurchaseDateLessThanEqual(initialDate : LocalDateTime, finalDate: LocalDateTime) : Flux<Order>

    fun findOrderByPurchaseDateGreaterThanEqualAndPurchaseDateLessThanEqualAndNickname(nitialDate : LocalDateTime, finalDate: LocalDateTime, nickname : String) : Flux<Order>
}
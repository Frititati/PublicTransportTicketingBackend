package it.polito.wa2.payment.repositories

import it.polito.wa2.payment.entities.Transaction
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux

interface TransactionsRepository : R2dbcRepository<Transaction, Long> {

    fun findAllByNickname(nickname : String) : Flux<Transaction>

}
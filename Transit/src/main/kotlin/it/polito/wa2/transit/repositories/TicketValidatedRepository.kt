package it.polito.wa2.transit.repositories

import it.polito.wa2.transit.entities.TicketValidated
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.*

interface TicketValidatedRepository : R2dbcRepository<TicketValidated, Long> {

    fun existsByTicketId(ticket_id: UUID) : Mono<Boolean>

    fun findTicketValidatedByZid(zid: String): Flux<TicketValidated>

    fun findTicketValidatedByValidationDateGreaterThanEqualAndValidationDateLessThanEqual(initialDate : LocalDateTime, finalDate: LocalDateTime): Flux<TicketValidated>

    fun findTicketValidatedByValidationDateGreaterThanEqualAndValidationDateLessThanEqualAndNickname(initialDate : LocalDateTime, finalDate: LocalDateTime, nickname: String): Flux<TicketValidated>
}
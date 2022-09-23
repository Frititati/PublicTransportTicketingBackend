package it.polito.wa2.transit.repositories

import it.polito.wa2.transit.entities.TicketValidated
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Mono
import java.util.*

interface TicketValidatedRepository : R2dbcRepository<TicketValidated, UUID>
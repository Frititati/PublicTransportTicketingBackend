package it.polito.wa2.ticketcatalogue.repositories

import it.polito.wa2.ticketcatalogue.entities.AvailableTicket
import org.springframework.data.r2dbc.repository.R2dbcRepository

interface AvailableTicketsRepository : R2dbcRepository<AvailableTicket, Long>
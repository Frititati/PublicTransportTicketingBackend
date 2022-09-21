package it.polito.wa2.ticketcatalogue.services

import it.polito.wa2.ticketcatalogue.dtos.AvailableTicketDTO
import it.polito.wa2.ticketcatalogue.dtos.toDTO
import it.polito.wa2.ticketcatalogue.repositories.AvailableTicketsRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class TicketService{
    @Autowired
    lateinit var availableTicketsRepository: AvailableTicketsRepository

    private val log = LoggerFactory.getLogger(javaClass)

    fun getAllTickets() : Pair<HttpStatus, Flux<AvailableTicketDTO>> {
        return try {
            Pair(HttpStatus.OK, availableTicketsRepository.findAll().map { it.toDTO() })
        } catch (e: Exception) {
            log.error("Exception: $e", e)
            Pair(HttpStatus.BAD_REQUEST, Flux.empty())
        }
    }
}
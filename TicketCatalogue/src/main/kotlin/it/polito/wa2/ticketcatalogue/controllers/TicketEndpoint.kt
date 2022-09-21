package it.polito.wa2.ticketcatalogue.controllers

import it.polito.wa2.ticketcatalogue.dtos.AvailableTicketDTO
import it.polito.wa2.ticketcatalogue.services.TicketService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
class TicketEndpoint(val ticketService: TicketService) {

    @GetMapping("/tickets")
    suspend fun getTickets() : ResponseEntity<Flux<AvailableTicketDTO>> {
        val result = ticketService.getAllTickets()
        return ResponseEntity(result.second, result.first)
    }
}
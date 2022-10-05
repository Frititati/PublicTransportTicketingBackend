package it.polito.wa2.transit.controllers

import it.polito.wa2.transit.dtos.TicketToValidateDTO
import it.polito.wa2.transit.dtos.TicketValidatedDTO
import it.polito.wa2.transit.services.TransitService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class TransitEndpoint(val transitService: TransitService) {

    /**
     * @param ticket {
     *                  jws String that contains the jws of the ticket with all the information
     *                  zid String that contains the zone that we have to check
     *               }
     *
     * @return If zone of the ticket and of the device are the same, it returns id, ticketId, validationDate
     *         and zid of the ticket
     *
     */
    @PostMapping("/ticket/validate")
    suspend fun ticketValidate(@RequestBody ticket: TicketToValidateDTO): ResponseEntity<TicketValidatedDTO> {
        val result = transitService.validateTicket(ticket)
        return ResponseEntity(result.second, result.first)
    }
}
package it.polito.wa2.transit.controllers

import it.polito.wa2.transit.dtos.TicketToValidateDTO
import it.polito.wa2.transit.dtos.TicketValidatedDTO
import it.polito.wa2.transit.services.TransitService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class TransitEndpoint(val transitService: TransitService) {
    @PostMapping("/ticket/validate")
    suspend fun ticketValidate(@RequestBody ticket: TicketToValidateDTO): ResponseEntity<TicketValidatedDTO> {
        return ResponseEntity(null, HttpStatus.BAD_REQUEST)
    }
}
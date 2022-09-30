package it.polito.wa2.transit.controllers

import it.polito.wa2.transit.dtos.TicketToValidateDTO
import it.polito.wa2.transit.dtos.TicketValidatedDTO
import it.polito.wa2.transit.dtos.TimeReportDTO
import it.polito.wa2.transit.services.TransitService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux

@RestController
class TransitEndpoint(val transitService: TransitService) {
    @PostMapping("/ticket/validate")
    suspend fun ticketValidate(@RequestBody ticket: TicketToValidateDTO): ResponseEntity<TicketValidatedDTO> {
        val result = transitService.validateTicket(ticket)
        return ResponseEntity(result.second, result.first)
    }

    @GetMapping("/admin/transit")
    suspend fun getTransit(): ResponseEntity<Flux<TicketValidatedDTO>> {
        val result = transitService.getAllTransit()
        return ResponseEntity(result.second, result.first)
    }

    /**
     * @param userId : id of the user you want to see
     * Get orders of a specific user
     */
    @GetMapping("/admin/transit/{zid}")
    suspend fun getTransitByZone(@PathVariable zid: String): ResponseEntity<Flux<TicketValidatedDTO>> {
        val result = transitService.getAllTransitByZone(zid)
        return ResponseEntity(result.second, result.first)
    }

    /**
     * Get list of users with their orders on a selectable time period
     */
    @PostMapping("/admin/transit/")
    suspend fun getTransitWithOrdersTimePeriod(@RequestBody timeReport : TimeReportDTO) : ResponseEntity<Flux<TicketValidatedDTO>> {
        val result = transitService.getAllTransitByTimePeriod(timeReport)
        return ResponseEntity(result.second, result.first)
    }

    /**
     * @param userId : id of the user you want to see
     * Get orders of a specific user on a selectable time period
     */
    @PostMapping("/admin/transit/{nickname}/{zid}/")
    suspend fun getUserOrdersTimePeriod(@PathVariable zid: String, @PathVariable nickname: String, @RequestBody timeReport: TimeReportDTO) : ResponseEntity<Flux<TicketValidatedDTO>> {
        val result = transitService.getAllTransitByNicknameAndTimePeriod(nickname, timeReport)
        return ResponseEntity(result.second, result.first)
    }
}
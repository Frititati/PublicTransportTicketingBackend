package it.polito.wa2.transit.controllers

import it.polito.wa2.transit.dtos.TicketValidatedDTO
import it.polito.wa2.transit.dtos.TimeReportDTO
import it.polito.wa2.transit.services.TransitService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
class AdminEndpoint(val transitService: TransitService) {

    /**
     * @return List of validated tickets with the validation date and their zones
     */
    @GetMapping("/admin/transit")
    suspend fun getTransit(): ResponseEntity<Flux<TicketValidatedDTO>> {
        val result = transitService.getAllTransit()
        return ResponseEntity(result.second, result.first)
    }

    /**
     * @param zid zid of the zone you want to see
     *
     * @return Validated tickets with the validation date of a specific zone
     */
    @GetMapping("/admin/transit/{zid}")
    suspend fun getTransitByZone(@PathVariable zid: String): ResponseEntity<Flux<TicketValidatedDTO>> {
        val result = transitService.getAllTransitByZone(zid)
        return ResponseEntity(result.second, result.first)
    }

    /**
     * @param timeReport initialDate and finalDate of the period you want to check in the yyyy-MM-dd format
     *
     * @return List of validated tickets with the validation date and their zones on a selectable time period
     */
    @PostMapping("/admin/transit/")
    suspend fun getTransitWithOrdersTimePeriod(@RequestBody timeReport: TimeReportDTO): ResponseEntity<Flux<TicketValidatedDTO>> {
        val result = transitService.getAllTransitByTimePeriod(timeReport)
        return ResponseEntity(result.second, result.first)
    }

    /**
     * @param username username of the user of which you want to see the validated tickets
     * @param timeReport initialDate and finalDate of the period you want to check in the yyyy-MM-dd format
     *
     * @return List of validated tickets of a specific zone and a specific user on a selectable time period
     */
    @PostMapping("/admin/transit/{username}")
    suspend fun getTransitByUserTimePeriod(
        @PathVariable username: String,
        @RequestBody timeReport: TimeReportDTO,
    ): ResponseEntity<Flux<TicketValidatedDTO>> {
        val result = transitService.getAllTransitByUsernameAndTimePeriod(username, timeReport)
        return ResponseEntity(result.second, result.first)
    }

}
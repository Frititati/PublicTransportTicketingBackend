package it.polito.wa2.travel.controllers

import it.polito.wa2.travel.dtos.TicketPurchasedDTO
import it.polito.wa2.travel.dtos.UserDetailsDTO
import it.polito.wa2.travel.dtos.UsernameDTO
import it.polito.wa2.travel.services.TravelerService
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
class AdminEndpoint(val travelerService: TravelerService) {

    /**
     * @return JSON list of usernames for which there exists any information
     */
    @GetMapping("/admin/travelers")
    suspend fun getUsernames(): ResponseEntity<Flux<UsernameDTO>?> {
        val result = travelerService.getTravelers()
        return ResponseEntity(result.second, result.first)
    }

    /**
     * @return Profile corresponding to the username
     */
    @GetMapping("/admin/traveler/{username}/profile")
    suspend fun getUserProfile(@PathVariable username: String): ResponseEntity<UserDetailsDTO?> {
        val result = travelerService.getUserProfile(username)
        return ResponseEntity(result.second.awaitFirstOrNull(), result.first)
    }

    /**
     * @return Tickets owned by the selected user
     */
    @GetMapping("/admin/traveler/{username}/tickets")
    suspend fun getUserTickets(@PathVariable username: String): ResponseEntity<Flux<TicketPurchasedDTO>?> {
        val result = travelerService.getUserTickets(username)
        return ResponseEntity(result.second, result.first)
    }
}
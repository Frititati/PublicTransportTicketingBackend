package it.polito.wa2.travel.controllers

import it.polito.wa2.travel.dtos.TicketPurchasedDTO
import it.polito.wa2.travel.dtos.UserDetailsDTO
import it.polito.wa2.travel.dtos.UserNicknameDTO
import it.polito.wa2.travel.services.TravelerService
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
class AdminEndpoint(val travelerService: TravelerService) {

    @GetMapping("/admin/travelers")
    suspend fun getUsernames(): ResponseEntity<Flux<UserNicknameDTO>?> {
        val result = travelerService.getTravelers()
        return ResponseEntity(result.second, result.first)
    }

    @GetMapping("/admin/traveler/{nickname}/profile")
    suspend fun getUserProfile(@PathVariable nickname: String): ResponseEntity<UserDetailsDTO?> {
        val result = travelerService.getUserProfile(nickname)
        return ResponseEntity(result.second.awaitFirstOrNull(), result.first)
    }

    @GetMapping("/admin/traveler/{nickname}/tickets")
    suspend fun getUserTickets(@PathVariable nickname: String): ResponseEntity<Flux<TicketPurchasedDTO>?> {
        val result = travelerService.getUserTickets(nickname)
        return ResponseEntity(result.second, result.first)
    }
}
package it.polito.wa2.travel.controllers

import it.polito.wa2.travel.dtos.TicketPurchasedDTO
import it.polito.wa2.travel.dtos.UserDetailsDTO
import it.polito.wa2.travel.services.TravelerService
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitLast
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import javax.validation.Valid

@RestController
class CustomerEndpoint(val travelerService: TravelerService) {

    @GetMapping("/my/profile")
    suspend fun getProfile(): ResponseEntity<UserDetailsDTO> {
        val username = ReactiveSecurityContextHolder.getContext()
            .map { obj: SecurityContext -> obj.authentication.principal as String }.awaitLast()
        val result = travelerService.getUserProfile(username)
        return ResponseEntity(result.second.awaitFirstOrNull(), result.first)
    }

    @PutMapping("/my/profile")
    suspend fun updateProfile(@Valid @RequestBody payload: UserDetailsDTO): ResponseEntity<Void> {
        // pass date in yyyy-MM-dd format
        val result = travelerService.userUpdate(payload)
        return ResponseEntity(null, result)
    }

    @GetMapping("/my/tickets")
    suspend fun getTickets(): ResponseEntity<Flux<TicketPurchasedDTO>> {
        val username = ReactiveSecurityContextHolder.getContext()
            .map { obj: SecurityContext -> obj.authentication.principal as String }.awaitLast()
        val result = travelerService.getUserTickets(username)
        return ResponseEntity(result.second, result.first)
    }
}


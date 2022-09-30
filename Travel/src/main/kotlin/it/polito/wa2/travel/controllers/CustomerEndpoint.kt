package it.polito.wa2.travel.controllers

import it.polito.wa2.travel.dtos.TicketPurchasedDTO
import it.polito.wa2.travel.dtos.UserDetailsDTO
import it.polito.wa2.travel.services.TravelerService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import javax.validation.Valid

@RestController
class CustomerEndpoint(val travelerService: TravelerService) {

    @GetMapping("/my/profile")
    suspend fun getProfile(): ResponseEntity<Mono<UserDetailsDTO>> {
        val result = travelerService.getUserByNickname()
        return ResponseEntity(result.second, result.first)
    }

    @PutMapping("/my/profile")
    suspend fun updateProfile(@Valid @RequestBody payload: UserDetailsDTO): ResponseEntity<Void> {
        // TODO improve input for date of birth
        val result = travelerService.userUpdate(payload)
        return ResponseEntity(null, result)
    }

    @GetMapping("/my/tickets")
    suspend fun getTickets(): ResponseEntity<Flux<TicketPurchasedDTO>> {
        val nickname = SecurityContextHolder.getContext().authentication.principal.toString()
        val result = travelerService.getUserTickets(nickname)
        return ResponseEntity(result.second, result.first)
    }


    @PostMapping("/my/tickets")
    suspend fun buyTickets(@RequestBody ticketPurchase: TicketPurchase): ResponseEntity<List<TicketPurchasedDTO>?> {
        // INFO please note that this is called by TicketCatalogue, not by a user
        return if (travelerService.validatePurchaseTicket(ticketPurchase)) {
            //val tickets = travelerService.createTickets(nickname, ticketPurchase.quantity, ticketPurchase.zones)
            val result = travelerService.addTickets(ticketPurchase.quantity, ticketPurchase.zones, ticketPurchase.type, ticketPurchase.exp)
            ResponseEntity(result.second, result.first)
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null)
        }
    }
}

data class TicketPurchase(val cmd: String, val quantity: Int, val zones: String, val type : String, val exp : LocalDateTime?)


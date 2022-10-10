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
import java.time.LocalDateTime
import javax.validation.Valid

@RestController
class CustomerEndpoint(val travelerService: TravelerService) {

    @GetMapping("/my/profile")
    suspend fun getProfile(): ResponseEntity<UserDetailsDTO> {
        val result = travelerService.getUserByNickname()
        return ResponseEntity(result.second.awaitFirstOrNull(), result.first)
    }

    @PutMapping("/my/profile")
    suspend fun updateProfile(@Valid @RequestBody payload: UserDetailsDTO): ResponseEntity<Void> {
        // TODO improve input for date of birth (Giacomo)
        val result = travelerService.userUpdate(payload)
        return ResponseEntity(null, result)
    }

    @GetMapping("/my/tickets")
    suspend fun getTickets(): ResponseEntity<Flux<TicketPurchasedDTO>> {
        val nickname = ReactiveSecurityContextHolder.getContext()
            .map { obj: SecurityContext -> obj.authentication.principal as String }.awaitLast()
        val result = travelerService.getUserTickets(nickname)
        return ResponseEntity(result.second, result.first)
    }

//    @PostMapping("/my/tickets")
//    suspend fun buyTickets(@RequestBody ticketPurchase: TicketPurchase): ResponseEntity<List<TicketPurchasedDTO>?> {
//        // INFO please note that this is called by TicketCatalogue, not by a user
//        return if (travelerService.validatePurchaseTicket(ticketPurchase)) {
//            println(ticketPurchase)
//            //val tickets = travelerService.createTickets(nickname, ticketPurchase.quantity, ticketPurchase.zones)
//            val result = travelerService.addTickets(ticketPurchase.quantity, ticketPurchase.zones, ticketPurchase.type, ticketPurchase.exp)
//            println(result.second)
//            ResponseEntity(result.second, result.first)
//        } else {
//            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null)
//        }
//    }
}

data class TicketPurchase(
    val cmd: String,
    val quantity: Int,
    val zones: String,
    val type: String,
    val exp: LocalDateTime?
)


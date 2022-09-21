package it.polito.wa2.travel.controllers

import it.polito.wa2.travel.dtos.TicketPurchasedDTO
import it.polito.wa2.travel.dtos.UserDetailsDTO
import it.polito.wa2.travel.services.TravelerService
import org.apache.coyote.Response
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import javax.validation.Valid

@RestController
class CustomerEndpoint(val travelerService: TravelerService) {

    @GetMapping("/my/profile")
    fun getProfile(): ResponseEntity<UserDetailsDTO> {
        val nickname = SecurityContextHolder.getContext().authentication.principal.toString()
        val user = travelerService.getUserByNickname(nickname)
        return ResponseEntity.status(HttpStatus.OK).body(user)
    }

    @PutMapping("/my/profile")
    fun updateProfile(@Valid @RequestBody payload: UserDetailsDTO): ResponseEntity<Response> {
        val nickname = SecurityContextHolder.getContext().authentication.principal.toString()
        travelerService.userUpdate(nickname, payload)
        return ResponseEntity.status(HttpStatus.OK).body(null)
    }

    @GetMapping("/my/tickets")
    fun getTickets(): ResponseEntity<List<TicketPurchasedDTO>> {
        val nickname = SecurityContextHolder.getContext().authentication.principal.toString()
        val tickets = travelerService.getUserTickets(nickname)
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(tickets)
    }


    @PostMapping("/my/tickets")
    fun buyTickets(@RequestBody ticketPurchase: TicketPurchase): ResponseEntity<List<TicketPurchasedDTO>> {
        return if (travelerService.validatePurchaseTicket(ticketPurchase)) {
            val nickname = SecurityContextHolder.getContext().authentication.principal.toString()

            //val tickets = travelerService.createTickets(nickname, ticketPurchase.quantity, ticketPurchase.zones)
            val tickets = travelerService.addTickets(nickname, ticketPurchase.quantity, ticketPurchase.zones, ticketPurchase.type, ticketPurchase.exp)
            ResponseEntity.status(HttpStatus.CREATED).body(tickets)
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null)
        }
    }
}

data class TicketPurchase(val cmd: String, val quantity: Int, val zones: String, val type : String, val exp : LocalDateTime?)


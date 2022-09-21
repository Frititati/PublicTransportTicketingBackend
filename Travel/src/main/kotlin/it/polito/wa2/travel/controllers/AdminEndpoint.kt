package it.polito.wa2.travel.controllers

import it.polito.wa2.travel.dtos.TicketPurchasedDTO
import it.polito.wa2.travel.dtos.UserDetailsDTO
import it.polito.wa2.travel.services.TravelerService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class AdminEndpoint(val travelerService: TravelerService) {

    @GetMapping("/admin/travelers")
    fun getUsernames(): ResponseEntity<List<String?>> {
        val users = travelerService.getTravelers()
        return ResponseEntity.status(HttpStatus.OK).body(users)
    }

    @GetMapping("/admin/traveler/{nickname}/profile")
    fun getUserProfile(@PathVariable nickname: String): ResponseEntity<UserDetailsDTO> {
        return if (travelerService.doesUserExist(nickname)){
            val user = travelerService.getUserProfile(nickname)
            ResponseEntity.status(HttpStatus.OK).body(user)
        }
        else ResponseEntity.status(HttpStatus.NO_CONTENT).body(null)
    }

    @GetMapping("/admin/traveler/{nickname}/tickets")
    fun getUserTickets(@PathVariable nickname:String): ResponseEntity<List<TicketPurchasedDTO>> {
        return if (travelerService.doesUserExist(nickname)){
            val tickets = travelerService.getProfileTickets(nickname)
            ResponseEntity.status(HttpStatus.OK).body(tickets)
        }
        else ResponseEntity.status(HttpStatus.NO_CONTENT).body(null)
    }

}
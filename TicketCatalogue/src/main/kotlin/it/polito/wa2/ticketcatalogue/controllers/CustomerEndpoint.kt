package it.polito.wa2.ticketcatalogue.controllers

import it.polito.wa2.ticketcatalogue.dtos.OrderDTO
import it.polito.wa2.ticketcatalogue.dtos.PurchaseRequestDTO
import it.polito.wa2.ticketcatalogue.services.CustomerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import javax.validation.Valid

@RestController
class CustomerEndpoint(val customerService: CustomerService) {

    /**
     * @param ticketId: id of the ticket you want to buy; you can find it on /tickets
     * @param purchaseRequest: body of the message (otherwise you receive a 400 http error)
     *                          {
     *                              "numberOfTickets": 1,            // you have to buy at least one ticket
     *                              "creditCard": 12345678912345,   // at least 14 characters, at most 16
     *                              "expirationDate": "01-2021",   // String formatted as MM-YYYY
     *                              "cvv": 123,                   // Int of 3 characters
     *                              "cardHolder": "Test"         // at least 1 character, at most 40
     *                          }
     */
    @PostMapping("/shop/{ticketId}")
    suspend fun purchaseTicket(@PathVariable ticketId: Long, @RequestBody @Valid purchaseRequest: Mono<PurchaseRequestDTO>): ResponseEntity<OrderDTO?> {

        val purchase = withContext(Dispatchers.IO) {
            purchaseRequest
                .map { it }
                .onErrorResume { Mono.empty() }
                .block()
        }

        return if(purchase != null) {
            val result = customerService.purchaseTicket(ticketId, purchase)
            ResponseEntity(result.second, result.first)
        } else ResponseEntity(null, HttpStatus.BAD_REQUEST)

    }

    @GetMapping("/orders")
    suspend fun getAllOrders() : ResponseEntity<List<OrderDTO>?>{
        val result = customerService.getOrders()
        return ResponseEntity(result.second, result.first)
    }

    /**
     * @param orderId : id of the order you want to check; if the order isn't yours, you receive a 401 http error
     */
    @GetMapping("/orders/{orderId}")
    suspend fun getOrder(@PathVariable orderId : Long) : ResponseEntity<OrderDTO?> {
        val result = customerService.getSingleOrder(orderId)
        return ResponseEntity(result.second, result.first)
    }
}


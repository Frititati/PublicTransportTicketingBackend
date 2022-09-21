package it.polito.wa2.payment.controllers

import it.polito.wa2.payment.dtos.TransactionDTO
import it.polito.wa2.payment.services.PaymentService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
class CustomerEndpoint(val paymentService: PaymentService) {

    @GetMapping("/transactions")
    suspend fun userTransactions() : ResponseEntity<Flux<TransactionDTO>> {
        val result = paymentService.userTransactions()
        return ResponseEntity(result.second, result.first)
    }
}
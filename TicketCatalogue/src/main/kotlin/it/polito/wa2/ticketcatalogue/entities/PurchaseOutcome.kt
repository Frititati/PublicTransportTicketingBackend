package it.polito.wa2.ticketcatalogue.entities

import com.fasterxml.jackson.annotation.JsonProperty

data class PurchaseOutcome (
    @JsonProperty("transactionId")
    val transactionId: Long,
    @JsonProperty("status")
    val status: PaymentStatus
)
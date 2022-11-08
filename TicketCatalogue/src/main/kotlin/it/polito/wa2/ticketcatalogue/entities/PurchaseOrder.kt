package it.polito.wa2.ticketcatalogue.entities

import com.fasterxml.jackson.annotation.JsonProperty

data class PurchaseOrder(
    @JsonProperty("username")
    val username: String,
    @JsonProperty("transactionId")
    val transactionId: Long,
    @JsonProperty("price")
    val price: Double,
    @JsonProperty("creditCard")
    val creditCard: Long,
    @JsonProperty("expirationDate")
    val expirationDate: String,
    @JsonProperty("cvv")
    val cvv: Int,
    @JsonProperty("cardHolder")
    val cardHolder: String
)

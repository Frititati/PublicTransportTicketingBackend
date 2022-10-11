package it.polito.wa2.travel.entities

import com.fasterxml.jackson.annotation.JsonProperty

data class TicketAddition(
    @JsonProperty("quantity")
    val quantity: Int,
    @JsonProperty("zones")
    val zones: String,
    @JsonProperty("type")
    val type: TicketType,
    @JsonProperty("username")
    val username: String
)

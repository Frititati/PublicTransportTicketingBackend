package it.polito.wa2.ticketcatalogue.entities

import com.fasterxml.jackson.annotation.JsonProperty

data class TicketAddition(
    @JsonProperty("quantity")
    val quantity: Int,
    @JsonProperty("zones")
    val zones: String,
    @JsonProperty("type")
    val type: TicketType,
    @JsonProperty("exp")
    val exp: String?,
    @JsonProperty("userNickName")
    val userNickName: String
)
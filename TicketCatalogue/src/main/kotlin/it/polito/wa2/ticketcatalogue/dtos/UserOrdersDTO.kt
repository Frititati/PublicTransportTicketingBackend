package it.polito.wa2.ticketcatalogue.dtos

import it.polito.wa2.ticketcatalogue.entities.Order

data class UserOrdersDTO(
    val name : String?,
    val orders : List<Order>
)

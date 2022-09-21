package it.polito.wa2.ticketcatalogue.dtos

import it.polito.wa2.ticketcatalogue.entities.Order
import it.polito.wa2.ticketcatalogue.entities.PaymentStatus

data class OrderDTO(
    var id: Long?,
    var nickname : String,
    var numberTickets: Int,
    var ticketId: Long,
    var status: PaymentStatus,
    var price: Double
)

fun Order.toDTO() : OrderDTO {
    return OrderDTO(id, nickname, numberTickets, ticketId, status, price)
}

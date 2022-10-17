package it.polito.wa2.ticketcatalogue.dtos

import it.polito.wa2.ticketcatalogue.entities.Order
import it.polito.wa2.ticketcatalogue.entities.PaymentStatus
import java.time.LocalDateTime

data class OrderDTO(
    var id: Long?,
    var username : String,
    var numberTickets: Int,
    var ticketId: Long,
    var status: PaymentStatus,
    var price: Double,
    var purchaseDate : LocalDateTime
)

fun Order.toDTO() : OrderDTO {
    return OrderDTO(id, username, numberTickets, ticketId, status, price, purchaseDate)
}

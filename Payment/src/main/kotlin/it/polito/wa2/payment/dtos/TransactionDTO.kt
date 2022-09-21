package it.polito.wa2.payment.dtos

import it.polito.wa2.payment.entities.PaymentStatus
import it.polito.wa2.payment.entities.Transaction

data class TransactionDTO(
    var id: Long?,
    var transactionId: Long,
    var price: Double,
    var nickname: String,
    var status: PaymentStatus
)

fun Transaction.toDTO() : TransactionDTO {
    return TransactionDTO(id, transactionId, price, nickname, status)
}

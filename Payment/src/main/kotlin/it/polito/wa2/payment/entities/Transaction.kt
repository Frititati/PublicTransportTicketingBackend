package it.polito.wa2.payment.entities

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("transactions")
data class Transaction(
    @Id
    @Column("id")
    var id: Long?,
    @Column("transaction_id")
    var transactionId: Long,
    @Column("price")
    var price: Double,
    @Column("username")
    var username: String,
    @Column("status")
    var status: PaymentStatus
)
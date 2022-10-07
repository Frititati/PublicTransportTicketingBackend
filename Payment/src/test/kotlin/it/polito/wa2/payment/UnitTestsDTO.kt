package it.polito.wa2.payment

import it.polito.wa2.payment.dtos.TransactionDTO
import it.polito.wa2.payment.dtos.toDTO
import it.polito.wa2.payment.entities.PaymentStatus
import it.polito.wa2.payment.entities.Transaction
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class UnitTestsDTO {
    @Test
    fun sameTransactionTest() {
        val t = TransactionDTO(1,1,10.0,"testUser", PaymentStatus.ACCEPTED)
        val t2 = Transaction(1,1,10.0, "testUser",PaymentStatus.ACCEPTED)
        assert(t == t2.toDTO())
    }

    @Test
    fun differentTransactionTest() {
        val t = TransactionDTO(1,1,10.0,"testUser", PaymentStatus.ACCEPTED)
        val t2 = Transaction(1,1,10.0, "testUser",PaymentStatus.REJECTED)
        assert(t != t2.toDTO())
    }
}
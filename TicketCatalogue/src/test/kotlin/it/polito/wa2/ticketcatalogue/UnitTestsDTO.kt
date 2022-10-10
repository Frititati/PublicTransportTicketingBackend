package it.polito.wa2.ticketcatalogue

import it.polito.wa2.ticketcatalogue.dtos.AvailableTicketDTO
import it.polito.wa2.ticketcatalogue.dtos.OrderDTO
import it.polito.wa2.ticketcatalogue.dtos.toDTO
import it.polito.wa2.ticketcatalogue.entities.AvailableTicket
import it.polito.wa2.ticketcatalogue.entities.Order
import it.polito.wa2.ticketcatalogue.entities.PaymentStatus
import it.polito.wa2.ticketcatalogue.entities.TicketType
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime

@SpringBootTest
class UnitTestsDTO {
    @Test
    fun sameAvailableTicketsTest() {
        val at = AvailableTicketDTO(1, 10.0, TicketType.DAILY, 0, 99, "A")
        val at2 = AvailableTicket(1, 10.0, TicketType.DAILY, 0, 99, "A")
        assert(at == at2.toDTO())
    }

    @Test
    fun differentAvailableTicketsTest() {
        val at = AvailableTicketDTO(1, 10.0, TicketType.DAILY, 0, 99, "A")
        val at2 = AvailableTicket(1, 10.0, TicketType.MONTHLY, 0, 99, "A")
        assert(at != at2.toDTO())
    }

    @Test
    fun sameOrderTest() {
        val o = OrderDTO(1, "Test", 1, 1, PaymentStatus.ACCEPTED, 10.0, LocalDateTime.now())
        val o2 = Order(1, "Test", 1, 1, PaymentStatus.ACCEPTED, 10.0, LocalDateTime.now())
        assert(o == o2.toDTO())
    }

    @Test
    fun differentOrderTest() {
        val o = OrderDTO(1, "Test", 1, 1, PaymentStatus.ACCEPTED, 10.0, LocalDateTime.now())
        val o2 = Order(1, "Test", 1, 1, PaymentStatus.REJECTED, 10.0, LocalDateTime.now())
        assert(o != o2.toDTO())
    }
}
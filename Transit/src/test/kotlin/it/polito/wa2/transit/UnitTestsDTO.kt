package it.polito.wa2.transit

import it.polito.wa2.transit.dtos.TicketValidatedDTO
import it.polito.wa2.transit.dtos.toDTO
import it.polito.wa2.transit.entities.TicketValidated
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime
import java.util.*

@SpringBootTest
class UnitTestsDTO {

    @Test
    fun sameTicketValidatedTest() {
        val uuid = UUID.randomUUID()
        val at = TicketValidatedDTO(1, uuid, LocalDateTime.now(), "A", "username")
        val at2 = TicketValidated(1,uuid, LocalDateTime.now(), "A", "username")
        assert(at == at2.toDTO())
    }

    @Test
    fun differentTicketValidatedTest() {
        val uuid = UUID.randomUUID()
        val at = TicketValidatedDTO(1, uuid, LocalDateTime.now(), "A", "username")
        val at2 = TicketValidated(1, uuid, LocalDateTime.now(), "B", "username")
        assert(at != at2.toDTO())
    }

}
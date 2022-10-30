package it.polito.wa2.travel

import it.polito.wa2.travel.dtos.TicketPurchasedDTO
import it.polito.wa2.travel.dtos.UserDetailsDTO
import it.polito.wa2.travel.dtos.toDTO
import it.polito.wa2.travel.entities.TicketPurchased
import it.polito.wa2.travel.entities.UserDetails
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime
import java.util.*

@SpringBootTest
class UnitTestDTO {

    @Test
    fun sameTicketPurchaseTest() {
        val date = LocalDateTime.of(2022,5,8,8,10,0)
        val tp1 = TicketPurchasedDTO(UUID.fromString("testMe"), date, date.plusMinutes(1),"Z","test","testJws")
        val tp2 = TicketPurchased(
            UUID.fromString("testMe").toString().toLong(),
            UUID.fromString("testMe"),
            date,
            date.plusMinutes(1),
            "Z",
            "test",
            "testJws"
        )
        assert(tp1 == tp2.toDTO())
    }
    @Test
    fun differentTicketPurchaseTest() {
        val date = LocalDateTime.of(2022,5,8,8,10,0)
        val tp1 = TicketPurchasedDTO(UUID.fromString("testMe"), date, date.plusMinutes(1),"Z","test","testJws")
        val tp2 = TicketPurchased(
            UUID.fromString("testMeAgain").toString().toLong(),
            UUID.fromString("testMe"),
            date,
            date.plusMinutes(1),
            "Z",
            "test",
            "testJws"
        )
        assert(tp1 != tp2.toDTO())
    }
    @Test
    fun sameUserDetailsTest(){
        val date = LocalDateTime.of(2022,5,8,8,10,0)
        val u1 = UserDetails(1,"nickname","name","address",date,1234567890)
        val u2 = UserDetailsDTO("name","address",date,1234567890)
        assert(u2==u1.toDTO())
    }
    @Test
    fun differentUserDetailsTest(){
        val date = LocalDateTime.of(2022,5,8,8,10,0)
        val u1 = UserDetails(1,"nickname","name","address",date,1234567890)
        val u2 = UserDetailsDTO("name2","address2",date,1234567890)
        assert(u2!=u1.toDTO())
    }
}

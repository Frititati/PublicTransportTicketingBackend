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
class UnitTestsDTO {

    @Test
    fun sameTicketPurchaseTest() {
        val date = LocalDateTime.of(2022,5,8,8,10,0)
        val tp1 = TicketPurchasedDTO(UUID.fromString("eda6bff4-cc1e-46be-80fe-b5a59fcc75e3"), date, date.plusMinutes(1),"Z","test","testJws")
        val tp2 = TicketPurchased(
            1,
            UUID.fromString("eda6bff4-cc1e-46be-80fe-b5a59fcc75e3"),
            date,
            date.plusMinutes(1),
            "Z",
            "test",
            "testJws",
            1
        )
        assert(tp1 == tp2.toDTO())
    }
    @Test
    fun differentTicketPurchaseTest() {
        val date = LocalDateTime.of(2022,5,8,8,0,0)
        val tp1 = TicketPurchasedDTO(UUID.fromString("eda6bff4-cc1e-46be-80fe-b5a59fcc75e3"), date, date.plusMinutes(1),"Z","test","testJws")
        val tp2 = TicketPurchased(
            1,
            UUID.fromString("eea6bff4-cc1e-46be-80fe-b5a59fcc75e3"),
            date,
            date.plusMinutes(1),
            "Z",
            "test",
            "testJws",
            1
        )
        assert(tp1 != tp2.toDTO())
    }
    @Test
    fun sameUserDetailsTest(){
        val date = LocalDateTime.of(2011,8,8,8,0,0)
        val u1 = UserDetails(1,"nickname","name","address",date,1234567890)
        val u2 = UserDetailsDTO("name","address","2011-8-8",1234567890)
        assert(u2==u1.toDTO())
    }
    @Test
    fun differentUserDetailsTest(){
        val date = LocalDateTime.of(2022,5,8,8,0,0)
        val u1 = UserDetails(1,"nickname","name","address",date,1234567890)
        val u2 = UserDetailsDTO("name2","address2","09/08/2011",1234567890)
        assert(u2!=u1.toDTO())
    }

}
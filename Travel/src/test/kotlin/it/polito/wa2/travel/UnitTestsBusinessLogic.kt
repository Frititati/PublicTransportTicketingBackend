package it.polito.wa2.travel

import it.polito.wa2.travel.dtos.UserDetailsDTO
import it.polito.wa2.travel.services.TravelerService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration

@SpringBootTest
@ContextConfiguration
class UnitTestsBusinessLogic {

    @Autowired
    lateinit var travelService: TravelerService
    annotation class WithMockCustomUser(val username: String, val role : String)

    @Test
    fun retrieveUserTransactionsWrong() = runBlocking {
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, travelService.getUserByNickname().first)
    }
    @Test
    @WithMockCustomUser(username = "TestUser", role = "CUSTOMER")
    fun retrieveUserCorrectly() = runBlocking {
        Assertions.assertEquals(HttpStatus.OK, travelService.getUserByNickname().first)
    }
    @Test
    @WithMockCustomUser(username = "TestUser", role = "CUSTOMER")
    fun userUpdateTest(){
        val u = UserDetailsDTO("name2","address",null,null)
        runBlocking {
            Assertions.assertEquals(travelService.userUpdate(u), HttpStatus.ACCEPTED)
        }
    }
    @Test
    @WithMockCustomUser(username = "TestUser", role = "CUSTOMER")
    fun testUserTickets(){
        runBlocking {
            Assertions.assertEquals(travelService.getUserTickets("TestUser").first,HttpStatus.ACCEPTED)
        }
    }
    @Test
    fun testGetTravelers(){
        runBlocking {
            Assertions.assertEquals(travelService.getTravelers().first,HttpStatus.ACCEPTED)
        }
    }
    @Test
    @WithMockCustomUser(username = "TestUser", role = "CUSTOMER")
    fun testGetUserProfile(){
        runBlocking {
            Assertions.assertEquals(travelService.getUserProfile("TestUser").first,HttpStatus.ACCEPTED)
        }
    }
}
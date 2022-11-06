package it.polito.wa2.travel

import it.polito.wa2.travel.dtos.UserDetailsDTO
import it.polito.wa2.travel.entities.UserRegister
import it.polito.wa2.travel.repositories.UserDetailsRepository
import it.polito.wa2.travel.security.Role
import it.polito.wa2.travel.services.TravelerService
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.context.support.WithSecurityContext
import org.springframework.security.test.context.support.WithSecurityContextFactory
import org.springframework.test.context.ContextConfiguration
import java.util.*

@SpringBootTest
@ContextConfiguration
class UnitTestsBusinessLogic {

    @Autowired
    lateinit var travelService: TravelerService

    @Autowired
    lateinit var userDetailsRepository: UserDetailsRepository

    @Retention(AnnotationRetention.RUNTIME)
    @WithSecurityContext(factory = WithMockCustomUserSecurityContextFactory::class)
    annotation class WithMockCustomUser(val username: String, val role: Role)

    class WithMockCustomUserSecurityContextFactory : WithSecurityContextFactory<WithMockCustomUser> {
        override fun createSecurityContext(customUser: WithMockCustomUser): SecurityContext {
            val context = SecurityContextHolder.createEmptyContext()
            val principal = customUser.username
            val auth = UsernamePasswordAuthenticationToken(
                principal, null, mutableListOf(SimpleGrantedAuthority("ROLE_${customUser.role}"))
            )
            context.authentication = auth
            return context
        }
    }

    @Test
    fun retrieveUserTransactionsWrong() = runBlocking {
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, travelService.getUserProfile("fail").first)
    }

    @Test
    @WithMockCustomUser(username = "TestUser", role = Role.CUSTOMER)
    fun retrieveUserCorrectly() {
        runBlocking {
            userDetailsRepository.deleteAllByUsername("TestUser").subscribe()
            val user = UserRegister("TestUser")
            travelService.processUserRegister(user)
            val response = travelService.getUserProfile("TestUser")
            userDetailsRepository.deleteAllByUsername("TestUser").subscribe()
            Assertions.assertEquals(HttpStatus.OK, response.first)
        }
    }

    @Test
    @WithMockCustomUser(username = "TestUser", role = Role.CUSTOMER)
    fun userUpdateTest() {
        runBlocking {
            userDetailsRepository.deleteAllByUsername("TestUser").awaitSingleOrNull()
            val u = UserDetailsDTO("name", "address", "2011-08-08", 1234567890)
            val user = UserRegister("TestUser")
            travelService.processUserRegister(user)
            Assertions.assertEquals(HttpStatus.ACCEPTED, travelService.userUpdate(u))
        }
    }

    @Test
    @WithMockCustomUser(username = "TestUser", role = Role.CUSTOMER)
    fun testUserTickets() {
        runBlocking {
            userDetailsRepository.deleteAllByUsername("TestUser").awaitSingleOrNull()
            val user = UserRegister("TestUser")
            travelService.processUserRegister(user)
            Assertions.assertEquals(HttpStatus.OK, travelService.getUserTickets("TestUser").first)
        }
    }

    @Test
    fun testGetTravelers() {
        runBlocking {
            Assertions.assertEquals(HttpStatus.OK, travelService.getTravelers().first)
        }
    }

    @Test
    @WithMockCustomUser(username = "TestUser", role = Role.CUSTOMER)
    fun testGetUserProfile() {
        runBlocking {
            userDetailsRepository.deleteAllByUsername("TestUser").awaitSingleOrNull()
            val user = UserRegister("TestUser")
            travelService.processUserRegister(user)
            Assertions.assertEquals(HttpStatus.OK,travelService.getUserProfile("TestUser").first)
        }
    }

    @Test
    @WithMockCustomUser(username = "TestUser", role = Role.CUSTOMER)
    fun testProcessUserAddition(){
        runBlocking {
            userDetailsRepository.deleteAllByUsername("TestUser").awaitSingleOrNull()
            val u = UserRegister("TestUser")
            Assertions.assertEquals(true, travelService.processUserRegister(u))
        }
    }
}
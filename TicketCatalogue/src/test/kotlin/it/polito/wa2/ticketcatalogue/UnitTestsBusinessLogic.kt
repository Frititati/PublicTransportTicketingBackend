package it.polito.wa2.ticketcatalogue

import it.polito.wa2.ticketcatalogue.dtos.*
import it.polito.wa2.ticketcatalogue.repositories.AvailableTicketsRepository
import it.polito.wa2.ticketcatalogue.repositories.OrdersRepository
import it.polito.wa2.ticketcatalogue.services.AdminService
import it.polito.wa2.ticketcatalogue.services.CustomerService
import it.polito.wa2.ticketcatalogue.services.TicketService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithSecurityContext
import org.springframework.security.test.context.support.WithSecurityContextFactory
import org.springframework.test.context.ContextConfiguration

@SpringBootTest
@ContextConfiguration
class UnitTestsBusinessLogic {

    @Autowired
    lateinit var adminService: AdminService

    @Autowired
    lateinit var customerService: CustomerService

    @Autowired
    lateinit var ticketService: TicketService

    @Autowired
    lateinit var ordersRepository: OrdersRepository

    @Autowired
    lateinit var availableTicketsRepository: AvailableTicketsRepository

    @Retention(AnnotationRetention.RUNTIME)
    @WithSecurityContext(factory = WithMockCustomUserSecurityContextFactory::class)
    annotation class WithMockCustomUser(val username: String, val role : String)

    class WithMockCustomUserSecurityContextFactory : WithSecurityContextFactory<WithMockCustomUser> {
        override fun createSecurityContext(customUser: WithMockCustomUser): SecurityContext {
            val context = SecurityContextHolder.createEmptyContext()
            val principal = PrincipalUserDTO(customUser.username, "")
            val auth: Authentication =
                UsernamePasswordAuthenticationToken(principal, null, mutableListOf(SimpleGrantedAuthority("ROLE_${customUser.role}")))
            context.authentication = auth
            return context
        }
    }

    /** TICKETS */

    @Test
    fun getAllTickets_Correctly() = runBlocking {
        Assertions.assertEquals(HttpStatus.OK, ticketService.getAllTickets().first)
    }

    /** CUSTOMER */
    @Test
    fun purchaseTicketsWithoutLogin() = runBlocking {
        val purchaseRequest = PurchaseRequestDTO(1, 12345678912345,"11-2021",123,"Test")
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, customerService.purchaseTicket(1, purchaseRequest).first)
    }

    @Test
    @WithMockCustomUser(username = "TestUser", role = "CUSTOMER")
    fun purchaseTickets_ticketAge099() = runBlocking {

        val availableTicket = AvailableTicketCreationDTO(10.0, "DAILY", 0 ,99, "A")
        val ticket = adminService.addTicket(availableTicket)

        val purchaseRequest = PurchaseRequestDTO(10, 12345678912345,"11-2021",123,"Test")

        val request = customerService.purchaseTicket(ticket.second?.ticketId!!, purchaseRequest)

        ordersRepository.deleteById(request.second?.id!!).subscribe()
        availableTicketsRepository.deleteById(ticket.second?.ticketId!!).subscribe()

        Assertions.assertEquals(HttpStatus.OK, request.first)


    }

    @Test
    @WithMockCustomUser(username = "TestUser", role = "CUSTOMER")
    fun purchaseTickets_ticketNotExist() = runBlocking {
        val purchaseRequest = PurchaseRequestDTO(0, 12345678912345,"11-2021",123,"Test")
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, customerService.purchaseTicket(1, purchaseRequest).first)
    }

    @Test
    @WithMockCustomUser(username = "TestUser", role = "CUSTOMER")
    fun purchaseTickets_ticketAgeLimited() = runBlocking {

        val availableTicket = AvailableTicketCreationDTO(10.0, "DAILY", 15 ,99, "A")
        val ticket = adminService.addTicket(availableTicket)


        val purchaseRequest = PurchaseRequestDTO(10, 12345678912345,"11-2021",123,"Test")
        availableTicketsRepository.deleteById(ticket.second?.ticketId!!).subscribe()
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, customerService.purchaseTicket(ticket.second?.ticketId!!, purchaseRequest).first)
    }

    @Test
    fun getOrdersWithoutLogin() = runBlocking {
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, customerService.getOrders().first)
    }

    @Test
    @WithMockCustomUser(username = "TestUser", role = "CUSTOMER")
    fun getOrders_OK() = runBlocking {
        Assertions.assertEquals(HttpStatus.OK, customerService.getOrders().first)
    }

    @Test
    @WithMockCustomUser(username = "TestUser", role = "CUSTOMER")
    fun getSingleOrder_NotExist() = runBlocking {
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, customerService.getSingleOrder(100).first)
    }

    /** ADMIN */

    @Test
    fun addTicket_Correctly() = runBlocking {
        val availableTicket = AvailableTicketCreationDTO(10.0, "DAILY", 0 ,99, "A")
        val ticket = adminService.addTicket(availableTicket)

        availableTicketsRepository.deleteById(ticket.second?.ticketId!!).subscribe()
        Assertions.assertEquals(HttpStatus.OK, ticket.first)
    }

    @Test
    fun addTicket_minAgeMajor99() = runBlocking {
        val availableTicket = AvailableTicketCreationDTO(10.0, "DAILY", 100,99, "A")
        val ticket = adminService.addTicket(availableTicket)

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, ticket.first)
    }

    @Test
    fun addTicket_maxAgeMajor99() = runBlocking {
        val availableTicket = AvailableTicketCreationDTO(10.0, "DAILY", 0 ,100, "A")
        val ticket = adminService.addTicket(availableTicket)

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, ticket.first)
    }

    @Test
    fun addTicket_typeWrong() = runBlocking {
        val availableTicket = AvailableTicketCreationDTO(10.0, "AA", 0 ,99, "A")
        val ticket = adminService.addTicket(availableTicket)

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, ticket.first)
    }

    @Test
    fun retrieveAllOrders_Correctly() = runBlocking {
        Assertions.assertEquals(HttpStatus.OK, adminService.retrieveAllOrders().first)
    }

    @Test
    fun retrieveUsersWithOrders_Wrong() = runBlocking {
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, adminService.usersWithOrders(null).first)
    }

    @Test
    fun retrieveUserOrders_Correctly() = runBlocking {
        Assertions.assertEquals(HttpStatus.OK, adminService.getUserOrders("Test",null).first)
    }

    @Test
    fun retrieveUserOrders_CorrectlyTimeReport() = runBlocking {

            val report = TimeReportDTO("2021-12-11", "2022-12-11")

            Assertions.assertEquals(HttpStatus.OK, adminService.getUserOrders("Test", report).first)
    }

    @Test
    fun retrieveUserOrders_CorrectlyTimeReportWrong() = runBlocking {

        val report = TimeReportDTO("2023-12-11", "2022-12-11")

        Assertions.assertEquals(HttpStatus.OK, adminService.getUserOrders("1", report).first)
    }

    @Test
    fun retrieveUserOrders_CorrectlyTimeReportFormatWrong() = runBlocking {

        val report = TimeReportDTO("2021-142-11", "2022-12-11")

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, adminService.getUserOrders("1", report).first)
    }

}
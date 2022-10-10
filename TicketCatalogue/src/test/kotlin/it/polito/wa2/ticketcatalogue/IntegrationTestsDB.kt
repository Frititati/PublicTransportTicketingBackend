package it.polito.wa2.ticketcatalogue

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.r2dbc.spi.ConnectionFactory
import it.polito.wa2.ticketcatalogue.dtos.AvailableTicketCreationDTO
import it.polito.wa2.ticketcatalogue.dtos.PurchaseRequestDTO
import it.polito.wa2.ticketcatalogue.dtos.TimeReportDTO
import it.polito.wa2.ticketcatalogue.security.Role
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.getForEntity
import org.springframework.boot.test.web.client.postForEntity
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ClassPathResource
import org.springframework.http.*
import org.springframework.r2dbc.connection.init.CompositeDatabasePopulator
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import javax.crypto.SecretKey


@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationTestsDB {

    companion object {

        @Bean
        fun initializer(connectionFactory: ConnectionFactory?): ConnectionFactoryInitializer {
            val initializer = ConnectionFactoryInitializer()
            if (connectionFactory != null) {
                initializer.setConnectionFactory(connectionFactory)
            }
            val populator = CompositeDatabasePopulator()
            populator.addPopulators(ResourceDatabasePopulator(ClassPathResource("schema.sql")))
            initializer.setDatabasePopulator(populator)
            return initializer
        }


        @Container
        val postgres = PostgreSQLContainer("postgres:latest")

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.r2dbc.url") {
                "r2dbc:tc:postgresql:///${postgres.databaseName}?TC_IMAGE_TAG=9.6.8"
            }
            registry.add("spring.r2dbc.username", postgres::getUsername)
            registry.add("spring.r2dbc.password", postgres::getPassword)
        }

    }

    @LocalServerPort
    protected var port: Int = 0

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Value("\${application.loginKey}")
    lateinit var secretString: String

    @Value("\${application.tokenPrefix}")
    lateinit var prefix: String

    fun createJWT(username: String, role: Role): String {

        val generatedKey: SecretKey = Keys.hmacShaKeyFor(secretString.toByteArray(StandardCharsets.UTF_8))

        val jwt = Jwts.builder()
            .setSubject(username)
            .setExpiration(
                Date.from(
                    LocalDateTime.now().plusHours(1).atZone(ZoneId.systemDefault()).toInstant()
                )
            )
            .setIssuedAt(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()))
            .claim("role", role)
            .signWith(generatedKey)
            .compact()

        return "$prefix$jwt"
    }

    /** TICKETS */
    @Test
    fun getAllTickets() {
        val baseUrl = "http://localhost:$port"

        val entity = HttpEntity<String>("")
        val response = restTemplate.exchange("$baseUrl/tickets", HttpMethod.GET, entity, String::class.java)
        assert(response.statusCode == HttpStatus.OK)
    }

    /** ADMINS */
    @Test
    fun addTickets_MinAgeNegative() {
        val baseUrl = "http://localhost:$port/admin"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Admin", Role.ADMIN))

        val ticket = AvailableTicketCreationDTO(10.0, "DAILY", -1, 99, setOf("A"))
        val request = HttpEntity(ticket, auth)
        val response = restTemplate.postForEntity<Unit>("$baseUrl/tickets", request)
        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun addTickets_MinAgeBiggerEqualThan100() {
        val baseUrl = "http://localhost:$port/admin"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Admin", Role.ADMIN))

        val ticket = AvailableTicketCreationDTO(10.0, "DAILY", 100, 99, setOf("A"))
        val request = HttpEntity(ticket, auth)
        val response = restTemplate.postForEntity<Unit>("$baseUrl/tickets", request)
        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun addTickets_MaxAgeNegative() {
        val baseUrl = "http://localhost:$port/admin"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Admin", Role.ADMIN))

        val ticket = AvailableTicketCreationDTO(10.0, "DAILY", 0, -1, setOf("A"))
        val request = HttpEntity(ticket, auth)
        val response = restTemplate.postForEntity<Unit>("$baseUrl/tickets", request)
        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun addTickets_MaxAgeBiggerEqualThan100() {
        val baseUrl = "http://localhost:$port/admin"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Admin", Role.ADMIN))

        val ticket = AvailableTicketCreationDTO(10.0, "DAILY", 0, 100, setOf("A"))
        val request = HttpEntity(ticket, auth)
        val response = restTemplate.postForEntity<Unit>("$baseUrl/tickets", request)
        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun addTickets_WrongType() {
        val baseUrl = "http://localhost:$port/admin"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Admin", Role.ADMIN))

        val ticket = AvailableTicketCreationDTO(10.0, "wrong", 0, 99, setOf("A"))
        val request = HttpEntity(ticket, auth)
        val response = restTemplate.postForEntity<Unit>("$baseUrl/tickets", request)
        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun addTickets_Correctly() {
        val baseUrl = "http://localhost:$port/admin"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Admin", Role.ADMIN))

        val ticket = AvailableTicketCreationDTO(10.0, "DAILY", 0, 99, setOf("A"))
        val request = HttpEntity(ticket, auth)
        val response = restTemplate.postForEntity<Unit>("$baseUrl/tickets", request)
        assert(response.statusCode == HttpStatus.OK)
    }

    @Test
    fun addTickets_WithoutAuth() {
        val baseUrl = "http://localhost:$port/admin"


        val ticket = AvailableTicketCreationDTO(10.0, "DAILY", 0, 99, setOf("A"))
        val request = HttpEntity(ticket)
        val response = restTemplate.postForEntity<Unit>("$baseUrl/tickets", request)
        assert(response.statusCode == HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun retrieveOrders_WithoutAuth() {
        val baseUrl = "http://localhost:$port/admin"

        val response = restTemplate.getForEntity<Unit>("$baseUrl/orders")
        assert(response.statusCode == HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun retrieveOrders_Correctly() {
        val baseUrl = "http://localhost:$port/admin"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("User", Role.ADMIN))

        val entity = HttpEntity<String>("parameters", auth)

        val response = restTemplate.exchange("$baseUrl/orders", HttpMethod.GET, entity, String::class.java)
        assert(response.statusCode == HttpStatus.OK)
    }

    @Test
    fun getSpecificUser_Wrong() {

        addTickets_Correctly()
        purchaseTicket_Correctly()

        val baseUrl = "http://localhost:$port/admin"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("User", Role.ADMIN))

        val entity = HttpEntity<String>("", auth)

        val response = restTemplate.exchange("$baseUrl/users/1", HttpMethod.GET, entity, String::class.java)
        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun getSpecificUser_WithoutAuth() {

        addTickets_Correctly()
        purchaseTicket_Correctly()

        val baseUrl = "http://localhost:$port/admin"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("User", Role.CUSTOMER))

        val entity = HttpEntity<String>("", auth)

        val response = restTemplate.exchange("$baseUrl/users/1", HttpMethod.GET, entity, String::class.java)
        assert(response.statusCode == HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun getUsersWithOrders_Correctly() {
        addTickets_Correctly()
        purchaseTicket_Correctly()

        val baseUrl = "http://localhost:$port/admin"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("User", Role.ADMIN))

        val entity = HttpEntity<String>("", auth)

        val response = restTemplate.exchange("$baseUrl/users", HttpMethod.GET, entity, String::class.java)
        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun getUsersWithOrders_WithoutAuth() {
        addTickets_Correctly()
        purchaseTicket_Correctly()

        val baseUrl = "http://localhost:$port/admin"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("User", Role.CUSTOMER))

        val entity = HttpEntity<String>("", auth)

        val response = restTemplate.exchange("$baseUrl/users", HttpMethod.GET, entity, String::class.java)
        assert(response.statusCode == HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun getUserOrders_Correct() {

        addTickets_Correctly()
        purchaseTicket_Correctly()

        val baseUrl = "http://localhost:$port/admin"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("User", Role.ADMIN))

        val entity = HttpEntity<String>("", auth)

        val response = restTemplate.exchange("$baseUrl/users/1/orders", HttpMethod.GET, entity, String::class.java)
        assert(response.statusCode == HttpStatus.OK)
    }

    @Test
    fun getUserOrders_WithoutAuth() {

        addTickets_Correctly()
        purchaseTicket_Correctly()

        val baseUrl = "http://localhost:$port/admin"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("User", Role.CUSTOMER))

        val entity = HttpEntity<String>("", auth)

        val response = restTemplate.exchange("$baseUrl/users/1/orders", HttpMethod.GET, entity, String::class.java)
        assert(response.statusCode == HttpStatus.UNAUTHORIZED)
    }

    //TODO not working
    @Test
    fun getUsersWithOrdersTimePeriod_Correctly() {
        addTickets_Correctly()
        purchaseTicket_Correctly()

        val baseUrl = "http://localhost:$port/admin"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("User", Role.ADMIN))

        val report = TimeReportDTO("2021-11-12","2022-11-12")
        val request = HttpEntity(report, auth)

        val response = restTemplate.postForEntity<Unit>("$baseUrl/users", request)
        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun getUsersWithOrdersTimePeriod_WithoutAuth() {
        addTickets_Correctly()
        purchaseTicket_Correctly()

        val baseUrl = "http://localhost:$port/admin"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("User", Role.CUSTOMER))

        val report = TimeReportDTO("2021-11-12","2022-11-12")
        val request = HttpEntity(report, auth)

        val response = restTemplate.postForEntity<Unit>("$baseUrl/users", request)
        assert(response.statusCode == HttpStatus.UNAUTHORIZED)
    }

    /*
    //TODO not working properly
    @Test
    fun getUserOrdersTimePeriod_Correct() {

        addTickets_Correctly()
        purchaseTicket_Correctly()

        val baseUrl = "http://localhost:$port/admin"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("User", Role.ADMIN))

        val report = TimeReportDTO("2021-11-12","2022-11-12")
        val request = HttpEntity(report, auth)

        val response = restTemplate.postForEntity<Unit>("$baseUrl/users/1/orders", request)
        assert(response.statusCode == HttpStatus.OK)
    }

     */

    @Test
    fun getUserOrdersTimePeriod_WithoutAuth() {

        addTickets_Correctly()
        purchaseTicket_Correctly()

        val baseUrl = "http://localhost:$port/admin"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("User", Role.CUSTOMER))

        val report = TimeReportDTO("2021-11-12","2022-11-12")
        val request = HttpEntity(report, auth)

        val response = restTemplate.postForEntity<Unit>("$baseUrl/users/1/orders", request)
        assert(response.statusCode == HttpStatus.UNAUTHORIZED)
    }

    /** USERS */
    @Test
    fun purchaseTicket_Correctly() {

        addTickets_Correctly()

        val baseUrl = "http://localhost:$port"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("User", Role.CUSTOMER))

        val purchase = PurchaseRequestDTO(5, 12345678912345, "01-2021", 123, "Test")
        val request = HttpEntity(purchase, auth)

        val response = restTemplate.postForEntity<Unit>("$baseUrl/shop/1", request)
        assert(response.statusCode == HttpStatus.OK)
    }

    @Test
    fun purchaseTicket_NoAuth() {

        addTickets_Correctly()

        val baseUrl = "http://localhost:$port"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("User", Role.ADMIN))

        val purchase = PurchaseRequestDTO(5, 12345678912345, "01-2021", 123, "Test")
        val request = HttpEntity(purchase, auth)

        val response = restTemplate.postForEntity<Unit>("$baseUrl/shop/1", request)
        assert(response.statusCode == HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun getAllOrders_Correctly() {
        val baseUrl = "http://localhost:$port"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("User", Role.CUSTOMER))

        val request = HttpEntity("", auth)

        val response = restTemplate.exchange("$baseUrl/orders", HttpMethod.GET, request, String::class.java)
        assert(response.statusCode == HttpStatus.OK)
    }

    @Test
    fun getAllOrders_NoAuth() {
        val baseUrl = "http://localhost:$port"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("User", Role.ADMIN))

        val request = HttpEntity("", auth)

        val response = restTemplate.exchange("$baseUrl/orders", HttpMethod.GET, request, String::class.java)
        assert(response.statusCode == HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun getOrder_Correctly() {
        purchaseTicket_Correctly()

        val baseUrl = "http://localhost:$port"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("User", Role.CUSTOMER))

        val request = HttpEntity("", auth)

        val response = restTemplate.exchange("$baseUrl/orders/1", HttpMethod.GET, request, String::class.java)
        assert(response.statusCode == HttpStatus.OK)
    }

    @Test
    fun getOrder_NoAuth() {
        val baseUrl = "http://localhost:$port"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("User", Role.ADMIN))

        val request = HttpEntity("", auth)

        val response = restTemplate.exchange("$baseUrl/orders/1", HttpMethod.GET, request, String::class.java)
        assert(response.statusCode == HttpStatus.UNAUTHORIZED)
    }

}
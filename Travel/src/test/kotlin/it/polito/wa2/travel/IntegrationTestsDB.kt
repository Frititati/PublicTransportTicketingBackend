package it.polito.wa2.travel

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.r2dbc.spi.ConnectionFactory
import it.polito.wa2.travel.dtos.UserDetailsDTO
import it.polito.wa2.travel.entities.UserDetails
import it.polito.wa2.travel.repositories.UserDetailsRepository
import it.polito.wa2.travel.security.Role
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
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

    @Autowired
    lateinit var userDetailsRepository: UserDetailsRepository

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

    fun addUserOnDatabase(username: String) = runBlocking {
        if (userDetailsRepository.existsUserDetailsByUsername(username).awaitFirstOrNull() == false)
            userDetailsRepository.save(
                UserDetails(null, username, null, null, null, null)
            ).awaitLast()
    }

    /** CUSTOMER */
    @Test
    fun getProfileNotExist() {
        val baseUrl = "http://localhost:$port/my"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("User1", Role.CUSTOMER))

        val request = HttpEntity("", auth)

        val response = restTemplate.exchange("$baseUrl/profile", HttpMethod.GET, request, String::class.java)
        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun getProfileCorrectly() {

        putProfileCorrectly()

        val baseUrl = "http://localhost:$port/my"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("User", Role.CUSTOMER))

        val request = HttpEntity("", auth)

        val response = restTemplate.exchange("$baseUrl/profile", HttpMethod.GET, request, String::class.java)

        assert(response.statusCode == HttpStatus.OK)
    }

    @Test
    fun getProfileNotAuth() {

        putProfileCorrectly()

        val baseUrl = "http://localhost:$port/my"

        val request = HttpEntity("")

        val response = restTemplate.exchange("$baseUrl/profile", HttpMethod.GET, request, String::class.java)

        assert(response.statusCode == HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun putProfileCorrectly() {

        addUserOnDatabase("User")

        val baseUrl = "http://localhost:$port/my"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("User", Role.CUSTOMER))

        val details = UserDetailsDTO("Username", "Address", "1997-12-11", 3332541254)

        val request = HttpEntity(details, auth)

        val response = restTemplate.exchange("$baseUrl/profile", HttpMethod.PUT, request, String::class.java)

        assert(response.statusCode == HttpStatus.ACCEPTED)
    }

    @Test
    fun putProfileNotExist() {

        val baseUrl = "http://localhost:$port/my"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("NotExist", Role.CUSTOMER))

        val details = UserDetailsDTO("Username", "Address", "1997-12-11", 3332541254)

        val request = HttpEntity(details, auth)

        val response = restTemplate.exchange("$baseUrl/profile", HttpMethod.PUT, request, String::class.java)

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun putProfileNotAuth() {

        val baseUrl = "http://localhost:$port/my"

        val details = UserDetailsDTO("Username", "Address", "1997-12-11", 3332541254)

        val request = HttpEntity(details)

        val response = restTemplate.exchange("$baseUrl/profile", HttpMethod.PUT, request, String::class.java)

        assert(response.statusCode == HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun putProfileNoName() {

        addUserOnDatabase("Username")

        val baseUrl = "http://localhost:$port/my"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Username", Role.CUSTOMER))

        val details = UserDetailsDTO("", "Address", "1997-12-11", 3332541254)

        val request = HttpEntity(details, auth)

        val response = restTemplate.exchange("$baseUrl/profile", HttpMethod.PUT, request, String::class.java)

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun putProfileNoData() {

        addUserOnDatabase("Username")

        val baseUrl = "http://localhost:$port/my"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Username", Role.CUSTOMER))

        val details = UserDetailsDTO("", "", "", null)

        val request = HttpEntity(details, auth)

        val response = restTemplate.exchange("$baseUrl/profile", HttpMethod.PUT, request, String::class.java)

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun putProfileNoAddress() {

        addUserOnDatabase("Username")

        val baseUrl = "http://localhost:$port/my"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Username", Role.CUSTOMER))

        val details = UserDetailsDTO("Username", "", "1997-12-11", 3332541254)

        val request = HttpEntity(details, auth)

        val response = restTemplate.exchange("$baseUrl/profile", HttpMethod.PUT, request, String::class.java)

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun putProfileNoDate() {

        addUserOnDatabase("Username")

        val baseUrl = "http://localhost:$port/my"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Username", Role.CUSTOMER))

        val details = UserDetailsDTO("Username", "Address", "", 1234567890)

        val request = HttpEntity(details, auth)

        val response = restTemplate.exchange("$baseUrl/profile", HttpMethod.PUT, request, String::class.java)

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun putProfileNullNumber() {

        addUserOnDatabase("Username")

        val baseUrl = "http://localhost:$port/my"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Username", Role.CUSTOMER))

        val details = UserDetailsDTO("Username", "Address", "1997-12-11", null)

        val request = HttpEntity(details, auth)

        val response = restTemplate.exchange("$baseUrl/profile", HttpMethod.PUT, request, String::class.java)

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun putProfileNullName() {

        addUserOnDatabase("Username")

        val baseUrl = "http://localhost:$port/my"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Username", Role.CUSTOMER))

        val details = UserDetailsDTO(null, "Address", "1997-12-11", 1234567890)

        val request = HttpEntity(details, auth)

        val response = restTemplate.exchange("$baseUrl/profile", HttpMethod.PUT, request, String::class.java)

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun putProfileTooCharName() {

        addUserOnDatabase("Username")

        val baseUrl = "http://localhost:$port/my"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Username", Role.CUSTOMER))

        val details = UserDetailsDTO(
            "mmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm",
            "Address",
            "1997-12-11",
            1234567890
        )

        val request = HttpEntity(details, auth)

        val response = restTemplate.exchange("$baseUrl/profile", HttpMethod.PUT, request, String::class.java)

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun putProfileNullAddress() {

        addUserOnDatabase("Username")

        val baseUrl = "http://localhost:$port/my"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Username", Role.CUSTOMER))

        val details = UserDetailsDTO("User", null, "1997-12-11", 1234567890)

        val request = HttpEntity(details, auth)

        val response = restTemplate.exchange("$baseUrl/profile", HttpMethod.PUT, request, String::class.java)

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun putProfileTooCharAddress() {

        addUserOnDatabase("Username")

        val baseUrl = "http://localhost:$port/my"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Username", Role.CUSTOMER))

        val details = UserDetailsDTO(
            "Username",
            "mmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm",
            "1997-12-11",
            1234567890
        )

        val request = HttpEntity(details, auth)

        val response = restTemplate.exchange("$baseUrl/profile", HttpMethod.PUT, request, String::class.java)

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun putProfileNullDate() {

        addUserOnDatabase("Username")

        val baseUrl = "http://localhost:$port/my"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Username", Role.CUSTOMER))

        val details = UserDetailsDTO("User", "Address", null, 1234567890)

        val request = HttpEntity(details, auth)

        val response = restTemplate.exchange("$baseUrl/profile", HttpMethod.PUT, request, String::class.java)

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun putProfileTooShortDate() {

        addUserOnDatabase("Username")

        val baseUrl = "http://localhost:$port/my"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Username", Role.CUSTOMER))

        val details = UserDetailsDTO("User", "Address", "1997-12-1", 1234567890)

        val request = HttpEntity(details, auth)

        val response = restTemplate.exchange("$baseUrl/profile", HttpMethod.PUT, request, String::class.java)

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun putProfileTooCharDate() {

        addUserOnDatabase("Username")

        val baseUrl = "http://localhost:$port/my"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Username", Role.CUSTOMER))

        val details = UserDetailsDTO("User", "Address", "11997-12-11", 1234567890)

        val request = HttpEntity(details, auth)

        val response = restTemplate.exchange("$baseUrl/profile", HttpMethod.PUT, request, String::class.java)

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun putProfileTooShortNumber() {

        addUserOnDatabase("Username")

        val baseUrl = "http://localhost:$port/my"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Username", Role.CUSTOMER))

        val details = UserDetailsDTO("User", "Address", "1997-12-11", 234567890)

        val request = HttpEntity(details, auth)

        val response = restTemplate.exchange("$baseUrl/profile", HttpMethod.PUT, request, String::class.java)

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun putProfileTooLongNumber() {

        addUserOnDatabase("Username")

        val baseUrl = "http://localhost:$port/my"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Username", Role.CUSTOMER))

        val details = UserDetailsDTO("User", "Address", "1997-12-11", 1234567890123456)

        val request = HttpEntity(details, auth)

        val response = restTemplate.exchange("$baseUrl/profile", HttpMethod.PUT, request, String::class.java)

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun getTicketsCorrectly() {

        addUserOnDatabase("UserTickets")

        val baseUrl = "http://localhost:$port/my"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("UserTickets", Role.CUSTOMER))

        val request = HttpEntity("", auth)

        val response = restTemplate.exchange("$baseUrl/tickets", HttpMethod.GET, request, String::class.java)

        assert(response.statusCode == HttpStatus.OK)
    }

    @Test
    fun getTicketsWrongName() {

        val baseUrl = "http://localhost:$port/my"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("UserTickets1", Role.CUSTOMER))

        val request = HttpEntity("", auth)

        val response = restTemplate.exchange("$baseUrl/tickets", HttpMethod.GET, request, String::class.java)

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun getTicketsNoAuth() {

        val baseUrl = "http://localhost:$port/my"

        val request = HttpEntity("")

        val response = restTemplate.exchange("$baseUrl/tickets", HttpMethod.GET, request, String::class.java)

        assert(response.statusCode == HttpStatus.UNAUTHORIZED)
    }

    /** ADMIN */

    @Test
    fun getTravelersCorrectly() {

        val baseUrl = "http://localhost:$port/admin"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Admin", Role.ADMIN))

        val request = HttpEntity("", auth)

        val response = restTemplate.exchange("$baseUrl/travelers", HttpMethod.GET, request, String::class.java)

        assert(response.statusCode == HttpStatus.OK)
    }

    @Test
    fun getTravelersNoAuth() {

        val baseUrl = "http://localhost:$port/admin"

        val request = HttpEntity("")

        val response = restTemplate.exchange("$baseUrl/travelers", HttpMethod.GET, request, String::class.java)

        assert(response.statusCode == HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun getUserProfileCorrectly() {

        addUserOnDatabase("Someone")

        val baseUrl = "http://localhost:$port/admin"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Admin", Role.ADMIN))

        val request = HttpEntity("", auth)

        val response =
            restTemplate.exchange("$baseUrl/traveler/Someone/profile", HttpMethod.GET, request, String::class.java)

        assert(response.statusCode == HttpStatus.OK)
    }

    @Test
    fun getUserProfileNotExist() {

        val baseUrl = "http://localhost:$port/admin"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Admin", Role.ADMIN))

        val request = HttpEntity("", auth)

        val response =
            restTemplate.exchange("$baseUrl/traveler/NoUser/profile", HttpMethod.GET, request, String::class.java)

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun getUserProfileNoAuth() {

        val baseUrl = "http://localhost:$port/admin"

        val request = HttpEntity("")

        val response =
            restTemplate.exchange("$baseUrl/traveler/NoUser/profile", HttpMethod.GET, request, String::class.java)

        assert(response.statusCode == HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun getUserTicketsCorrectly() {

        addUserOnDatabase("UserTicket")

        val baseUrl = "http://localhost:$port/admin"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Admin", Role.ADMIN))

        val request = HttpEntity("", auth)

        val response =
            restTemplate.exchange("$baseUrl/traveler/UserTicket/tickets", HttpMethod.GET, request, String::class.java)

        assert(response.statusCode == HttpStatus.OK)
    }

    @Test
    fun getUserTicketsNotExist() {

        val baseUrl = "http://localhost:$port/admin"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Admin", Role.ADMIN))

        val request = HttpEntity("", auth)

        val response =
            restTemplate.exchange("$baseUrl/traveler/UserTicket2/tickets", HttpMethod.GET, request, String::class.java)

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun getUserTicketsNoAuth() {

        val baseUrl = "http://localhost:$port/admin"

        val request = HttpEntity("")

        val response =
            restTemplate.exchange("$baseUrl/traveler/UserTicket/tickets", HttpMethod.GET, request, String::class.java)

        assert(response.statusCode == HttpStatus.UNAUTHORIZED)
    }

}
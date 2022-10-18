package it.polito.wa2.transit

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.r2dbc.spi.ConnectionFactory
import it.polito.wa2.transit.dtos.TicketToValidateDTO
import it.polito.wa2.transit.dtos.TicketValidatedDTO
import it.polito.wa2.transit.dtos.TimeReportDTO
import it.polito.wa2.transit.security.Role
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.testcontainers.junit.jupiter.Testcontainers
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import javax.crypto.SecretKey

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransitIntegrationTests {
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

    fun createJWSTicket(uuid: String, expiresAt:LocalDateTime, issuedAt:LocalDateTime, type:String, zones:String, username:String): String {

        val generatedKey: SecretKey = Keys.hmacShaKeyFor(secretString.toByteArray(StandardCharsets.UTF_8))

        val jwt = Jwts.builder().setSubject(uuid.toString())
            .setExpiration(Date.from(expiresAt.atZone(ZoneId.systemDefault()).toInstant()))
            .setIssuedAt(Date.from(issuedAt.atZone(ZoneId.systemDefault()).toInstant()))
            .claim("type", type)
            .claim("zid", zones)
            .claim("username", username).signWith(generatedKey).compact()

        return "$prefix$jwt"
    }

    @Test
    fun getTransit(){
        val baseUrl = "http://localhost:$port"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Admin", Role.ADMIN))
        val entity = HttpEntity<String>("")
        val response = restTemplate.exchange("$baseUrl/transit", HttpMethod.GET, entity, String::class.java)
        assert(response.statusCode == HttpStatus.OK)
    }

    @Test
    fun getTransitByZone(){
        val baseUrl = "http://localhost:$port"
        val zid = "A"
        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Admin", Role.ADMIN))
        val entity = HttpEntity<String>("")
        val response = restTemplate.exchange("$baseUrl/transit/$zid", HttpMethod.GET, entity, String::class.java)
        assert(response.statusCode == HttpStatus.OK)
    }

    @Test
    fun getTransitByOrderTimePeriod(){
        val baseUrl = "http://localhost:$port"
        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Admin", Role.ADMIN))
        //TODO: fix TimeReportDTO
        val time = TimeReportDTO("2022-10-17", "2022-10-20")
        val request = HttpEntity(time, auth)
        val response = restTemplate.postForEntity<Unit>("$baseUrl/admin/transit", request)
        assert(response.statusCode == HttpStatus.OK)
    }

    @Test
    fun getTransitByOrderTimePeriodAndNickname(){
        val baseUrl = "http://localhost:$port"
        val nickname = "prova1"
        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Admin", Role.ADMIN))
        //TODO: fix TimeReportDTO
        val time = TimeReportDTO("2022-10-17", "2022-10-20")
        val request = HttpEntity(time, auth)
        val response = restTemplate.postForEntity<Unit>("$baseUrl/admin/transit/$nickname", request)
        assert(response.statusCode == HttpStatus.OK)
    }

    @Test
    fun ticketValidation(){
        val baseUrl = "http://localhost:$port"
        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Device1", Role.DEVICE))
        //TODO: fix TimeReportDTO
        val jws = "ciao"
        val ticketToValidate = TicketToValidateDTO(jws)
        val request = HttpEntity(ticketToValidate, auth)
        val response = restTemplate.postForEntity<Unit>("$baseUrl/ticket/validate", request)
        assert(response.statusCode == HttpStatus.OK)
    }

    @Test
    fun ticketValidationEmptyJws(){
        val baseUrl = "http://localhost:$port"
        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Device1", Role.DEVICE))
        val jws = ""
        val ticketToValidate = TicketToValidateDTO(jws)
        val request = HttpEntity(ticketToValidate, auth)
        val response = restTemplate.postForEntity<Unit>("$baseUrl/ticket/validate", request)
        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun ticketValidationExpired(){
        val baseUrl = "http://localhost:$port"
        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Device1", Role.DEVICE))

        //TODO: fix this with reasonable values (in this case, an expired JWS)
        val jws = createJWSTicket("", LocalDateTime.now(), LocalDateTime.now(), "DAILY", "A", "prova1")

        val ticketToValidate = TicketToValidateDTO(jws)
        val request = HttpEntity(ticketToValidate, auth)
        val response = restTemplate.postForEntity<Unit>("$baseUrl/ticket/validate", request)
        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun ticketValidationAlreadyValidated(){
        val baseUrl = "http://localhost:$port"
        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Device1", Role.DEVICE))

        //TODO: fix this with reasonable values
        val jws = createJWSTicket("", LocalDateTime.now(), LocalDateTime.now(), "DAILY", "A", "prova1")

        val ticketToValidate = TicketToValidateDTO(jws)
        val request = HttpEntity(ticketToValidate, auth)

        val response = restTemplate.postForEntity<Unit>("$baseUrl/ticket/validate", request)
        assert(response.statusCode == HttpStatus.OK)

        val response2 = restTemplate.postForEntity<Unit>("$baseUrl/ticket/validate", request)
        assert(response2.statusCode == HttpStatus.BAD_REQUEST)
    }
}
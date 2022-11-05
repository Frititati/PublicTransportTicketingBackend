package it.polito.wa2.transit

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.r2dbc.spi.ConnectionFactory
import it.polito.wa2.transit.dtos.TicketToValidateDTO
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
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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

    @Value("\${application.ticketKey}")
    lateinit var secretTicketString: String

    @Value("\${application.tokenPrefix}")
    lateinit var prefix: String

    fun createJWT(username: String, role: Role): String {

        val generatedKey: SecretKey = Keys.hmacShaKeyFor(secretString.toByteArray(StandardCharsets.UTF_8))

        val jwt = Jwts.builder()
            .setSubject(username)
            .setExpiration(
                Date.from(
                    LocalDateTime.now().plusHours(24).atZone(ZoneId.systemDefault()).toInstant()
                )
            )
            .setIssuedAt(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()))
            .claim("role", role)
            .signWith(generatedKey)
            .compact()

        return "$prefix$jwt"
    }

    fun createJWTDevice(username:String, zone:String):String{
        val generatedKey: SecretKey = Keys.hmacShaKeyFor(secretString.toByteArray(StandardCharsets.UTF_8))

        val jwt = Jwts.builder()
            .setSubject(username)
            .setExpiration(
                Date.from(
                    LocalDateTime.now().plusHours(24).atZone(ZoneId.systemDefault()).toInstant()
                )
            )
            .setIssuedAt(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()))
            .claim("role", Role.DEVICE)
            .claim("zone", zone)
            .signWith(generatedKey)
            .compact()

        return "$prefix$jwt"
    }

    fun createJWSTicket(uuid: String, expiresAt:LocalDateTime, issuedAt:LocalDateTime, type:String, zones:String, username:String): String {

        val generatedKey: SecretKey = Keys.hmacShaKeyFor(secretTicketString.toByteArray(StandardCharsets.UTF_8))

        val jwt = Jwts.builder().setSubject(uuid)
            .setExpiration(Date.from(expiresAt.atZone(ZoneId.systemDefault()).toInstant()))
            .setIssuedAt(Date.from(issuedAt.atZone(ZoneId.systemDefault()).toInstant()))
            .claim("type", type)
            .claim("zid", zones)
            .claim("username", username).signWith(generatedKey).compact()

        return jwt
    }

    fun localDateToString(date: LocalDateTime): String{
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return date.format(formatter)
    }

    @Test
    fun getTransit(){
        val baseUrl = "http://localhost:$port"

        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("User", Role.ADMIN))

        val entity = HttpEntity<String>("parameters", auth)
        val response = restTemplate.exchange("$baseUrl/admin/transit", HttpMethod.GET, entity, String::class.java)

        print(response.statusCode)
        assert(response.statusCode == HttpStatus.OK)
    }

    @Test
    fun getTransitByZone(){
        val baseUrl = "http://localhost:$port"
        val zid = "A"
        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Admin", Role.ADMIN))
        val entity = HttpEntity<String>("", auth)
        val response = restTemplate.exchange("$baseUrl/admin/transit/$zid", HttpMethod.GET, entity, String::class.java)
        assert(response.statusCode == HttpStatus.OK)
    }

    //TODO: discuss if we have ticket to insert for testing and its correspondent time period
    @Test
    fun getTransitByOrderTimePeriod(){
        val baseUrl = "http://localhost:$port"
        val auth = HttpHeaders()
        auth.set("Authorization", createJWT("Admin", Role.ADMIN))
        val time = TimeReportDTO(localDateToString(LocalDateTime.now().minusDays(3)), localDateToString(LocalDateTime.now()))
        val request = HttpEntity(time, auth)
        val response = restTemplate.exchange("$baseUrl/admin/transit/", HttpMethod.POST, request, String::class.java)
        assert(response.statusCode == HttpStatus.OK)
    }

    @Test
    fun getTransitByOrderTimePeriodAndNickname(){
        val baseUrl = "http://localhost:$port"
        val username = "prova1"
        val auth = HttpHeaders()
        val jwt = createJWT("Admin", Role.ADMIN)
        auth.set("Authorization", jwt)

        val time = TimeReportDTO("2021-10-17", localDateToString(LocalDateTime.now()))

        val request = HttpEntity(time, auth)
        val response = restTemplate.exchange("$baseUrl/admin/transit/${username}", HttpMethod.POST, request, String::class.java)
        assert(response.statusCode == HttpStatus.OK)
    }

    @Test
    fun ticketValidation(){
        val baseUrl = "http://localhost:$port"
        val auth = HttpHeaders()
        auth.set("Authorization", createJWTDevice("Device1", "A"))

        val jws = createJWSTicket("55e8bda8-4f86-11ed-bdc3-0242ac120002", LocalDateTime.now().plusHours(1), LocalDateTime.now(), "DAILY", "A", "prova1")

        val ticketToValidate = TicketToValidateDTO(jws)
        val request = HttpEntity(ticketToValidate, auth)
        val response = restTemplate.postForEntity<Unit>("$baseUrl/ticket/validate", request)
        assert(response.statusCode == HttpStatus.ACCEPTED)
    }

    @Test
    fun ticketValidationEmptyJws(){
        val baseUrl = "http://localhost:$port"
        val auth = HttpHeaders()
        auth.set("Authorization", createJWTDevice("Device1", "A"))
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
        auth.set("Authorization", createJWTDevice("Device1", "A"))

        val jws = createJWSTicket("caa845b8-4f82-11ed-bdc3-0242ac120002", LocalDateTime.now().minusHours(1), LocalDateTime.now(), "DAILY", "A", "prova1")

        val ticketToValidate = TicketToValidateDTO(jws)
        val request = HttpEntity(ticketToValidate, auth)
        val response = restTemplate.postForEntity<Unit>("$baseUrl/ticket/validate", request)
        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun dailyTicketValidationAlreadyValidated(){
        val baseUrl = "http://localhost:$port"
        val auth = HttpHeaders()
        auth.set("Authorization", createJWTDevice("Device1", "A"))

        val jws = createJWSTicket("1a2d5aec-4f83-11ed-bdc3-0242ac120002", LocalDateTime.now().plusHours(1), LocalDateTime.now(), "DAILY", "A", "prova1")

        val ticketToValidate = TicketToValidateDTO(jws)
        val request = HttpEntity(ticketToValidate, auth)

        val response = restTemplate.postForEntity<Unit>("$baseUrl/ticket/validate", request)
        assert(response.statusCode == HttpStatus.ACCEPTED)

        val response2 = restTemplate.postForEntity<Unit>("$baseUrl/ticket/validate", request)
        assert(response2.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun notDailyTicketMultipleValidation(){
        val baseUrl = "http://localhost:$port"
        val auth = HttpHeaders()
        auth.set("Authorization", createJWTDevice("Device1", "A"))

        val jws = createJWSTicket("1a2d5aec-4f83-11ed-bdc3-0242ac120002", LocalDateTime.now().plusHours(1), LocalDateTime.now(), "MONTHLY", "A", "prova1")

        val ticketToValidate = TicketToValidateDTO(jws)
        val request = HttpEntity(ticketToValidate, auth)

        val response = restTemplate.postForEntity<Unit>("$baseUrl/ticket/validate", request)
        assert(response.statusCode == HttpStatus.ACCEPTED)

        val response2 = restTemplate.postForEntity<Unit>("$baseUrl/ticket/validate", request)
        assert(response2.statusCode == HttpStatus.ACCEPTED)
    }
}
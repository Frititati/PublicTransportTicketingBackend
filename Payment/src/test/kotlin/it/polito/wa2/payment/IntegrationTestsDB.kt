package it.polito.wa2.payment

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.r2dbc.spi.ConnectionFactory
import it.polito.wa2.payment.security.Role
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.getForEntity
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

    fun createAdminJWT(username: String, role: Role): String {

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

    @Test
    fun returnUserTransactionsWithoutAuth() {
        val baseUrl = "http://localhost:$port/"

        val response = restTemplate.getForEntity<Unit>("$baseUrl/transactions")
        assert(response.statusCode == HttpStatus.UNAUTHORIZED)

    }

    @Test
    fun returnUserTransactionsCorrectly() {
        val baseUrl = "http://localhost:$port/"

        val auth = HttpHeaders()
        auth.set("Authorization", createAdminJWT("User", Role.CUSTOMER))

        val entity = HttpEntity<String>("parameters", auth)
        val response = restTemplate.exchange("$baseUrl/transactions", HttpMethod.GET, entity, String::class.java)
        assert(response.statusCode == HttpStatus.OK)
    }

    @Test
    fun returnUserTransactionsIncorrectly() {
        val baseUrl = "http://localhost:$port/"

        // only customers can access /transactions
        val auth = HttpHeaders()
        auth.set("Authorization", createAdminJWT("User", Role.ADMIN))

        val entity = HttpEntity<String>("parameters", auth)
        val response = restTemplate.exchange("$baseUrl/transactions", HttpMethod.GET, entity, String::class.java)
        assert(response.statusCode == HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun returnAllTransactionsWithoutAuth() {
        val baseUrl = "http://localhost:$port/admin"

        val response = restTemplate.getForEntity<Unit>("$baseUrl/transactions")
        assert(response.statusCode == HttpStatus.UNAUTHORIZED)

    }

    @Test
    fun returnAllTransactionsCorrectly() {
        val baseUrl = "http://localhost:$port/admin"

        val auth = HttpHeaders()
        auth.set("Authorization", createAdminJWT("User", Role.ADMIN))

        val entity = HttpEntity<String>("parameters", auth)
        val response = restTemplate.exchange("$baseUrl/transactions", HttpMethod.GET, entity, String::class.java)
        assert(response.statusCode == HttpStatus.OK)
    }

    @Test
    fun returnAllTransactionsIncorrectly() {
        val baseUrl = "http://localhost:$port/admin"

        // only customers can access /transactions
        val auth = HttpHeaders()
        auth.set("Authorization", createAdminJWT("User", Role.CUSTOMER))

        val entity = HttpEntity<String>("parameters", auth)
        val response = restTemplate.exchange("$baseUrl/transactions", HttpMethod.GET, entity, String::class.java)
        assert(response.statusCode == HttpStatus.UNAUTHORIZED)
    }

}
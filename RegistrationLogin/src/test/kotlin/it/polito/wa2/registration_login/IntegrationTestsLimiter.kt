package it.polito.wa2.registration_login

import it.polito.wa2.registration_login.dtos.ValidateRegistrationDTO
import it.polito.wa2.registration_login.dtos.UserRegistrationDTO
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.postForEntity
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID
import org.springframework.http.ResponseEntity


@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class IntegrationTestsLimiter {
    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:latest")

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.r2dbc.url", postgres::getJdbcUrl)
            registry.add("spring.r2dbc.username", postgres::getUsername)
            registry.add("spring.r2dbc.password", postgres::getPassword)
            //registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }
        }
    }

    @LocalServerPort
    protected var port: Int = 0

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Test
    fun rateLimiterRegister() {
        val baseUrl = "http://localhost:$port/user"
        val user = UserRegistrationDTO("testRateLimiter", "Password123", "testRateLimiter@gmail.com")

        val request = HttpEntity(user)

        var response : ResponseEntity<Unit>? = null
        for(i in 1..15) {
            response = restTemplate.postForEntity("$baseUrl/register", request)
        }

        assert(response?.statusCode == HttpStatus.TOO_MANY_REQUESTS)
    }

    @Test
    fun rateLimiterValidate() {
        val baseUrl = "http://localhost:$port/user"

        val request = HttpEntity(ValidateRegistrationDTO(UUID.randomUUID().toString(), 1))

        var response : ResponseEntity<Unit>? = null
        for(i in 1..15) {
            response = restTemplate.postForEntity("$baseUrl/validate", request)
        }

        assert(response?.statusCode == HttpStatus.TOO_MANY_REQUESTS)
    }
}
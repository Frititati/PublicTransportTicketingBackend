package it.polito.wa2.registration_login

import io.r2dbc.spi.ConnectionFactory
import it.polito.wa2.registration_login.dtos.ValidateRegistrationDTO
import it.polito.wa2.registration_login.dtos.UserRegistrationDTO
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.postForEntity
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.r2dbc.connection.init.CompositeDatabasePopulator
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator


@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationTestsLimiter {

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
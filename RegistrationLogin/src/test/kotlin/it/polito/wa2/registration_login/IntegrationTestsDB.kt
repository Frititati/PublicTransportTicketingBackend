package it.polito.wa2.registration_login

import it.polito.wa2.registration_login.dtos.RegistrationToValidateDTO
import it.polito.wa2.registration_login.dtos.ValidateRegistrationDTO
import it.polito.wa2.registration_login.dtos.LoginDTO
import it.polito.wa2.registration_login.dtos.UserRegistrationDTO
import it.polito.wa2.registration_login.repositories.ActivationRepository
import it.polito.wa2.registration_login.repositories.UserRepository
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
import java.time.LocalDateTime


@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class IntegrationTestsDB {
    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:latest")

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.r2dbc.url", IntegrationTestsLimiter.postgres::getJdbcUrl)
            registry.add("spring.r2dbc.username", IntegrationTestsLimiter.postgres::getUsername)
            registry.add("spring.r2dbc.password", IntegrationTestsLimiter.postgres::getPassword)
            //registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }
        }
    }

    @LocalServerPort
    protected var port: Int = 0

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var activationRepository: ActivationRepository

    @Autowired
    lateinit var userRepository : UserRepository


    @Test
    fun registerUserSuccessfully() {
        val baseUrl = "http://localhost:$port/user"
        val user = UserRegistrationDTO("testNickname", "Password123)", "testEmail@gmail.com")

        val request = HttpEntity(user)
        val response = restTemplate.postForEntity<Unit>("$baseUrl/register", request)

        assert(response.statusCode == HttpStatus.ACCEPTED)


    }

    @Test
    fun registerUserEmptyEmail() {
        val baseUrl = "http://localhost:$port/user"
        val user = UserRegistrationDTO("testNickname", "Password123)", "")

        val request = HttpEntity(user)
        val response = restTemplate.postForEntity<Unit>("$baseUrl/register", request)
        assert(response.statusCode == HttpStatus.BAD_REQUEST)

    }

    @Test
    fun registerUserEmptyNickname() {
        val baseUrl = "http://localhost:$port/user"
        val user = UserRegistrationDTO("", "Password123)", "testEmail@gmail.com")

        val request = HttpEntity(user)
        val response = restTemplate.postForEntity<Unit>("$baseUrl/register", request)
        assert(response.statusCode == HttpStatus.BAD_REQUEST)

    }

    @Test
    fun registerUserEmptyPassword() {
        val baseUrl = "http://localhost:$port/user"
        val user = UserRegistrationDTO("testNickname", "", "testEmail@gmail.com")

        val request = HttpEntity(user)
        val response = restTemplate.postForEntity<Unit>("$baseUrl/register", request)
        assert(response.statusCode == HttpStatus.BAD_REQUEST)

    }

    @Test
    fun registerUserInvalidEmail_NoFinalPart() {
        val baseUrl = "http://localhost:$port/user"
        val user = UserRegistrationDTO("testNickname", "Password123)", "testEmail@gmail")

        val request = HttpEntity(user)
        val response = restTemplate.postForEntity<Unit>("$baseUrl/register", request)
        assert(response.statusCode == HttpStatus.BAD_REQUEST)

    }

    @Test
    fun registerUserInvalidEmail_NoAt() {
        val baseUrl = "http://localhost:$port/user"
        val user = UserRegistrationDTO("testNickname", "Password123)", "testEmailGmail.com")

        val request = HttpEntity(user)
        val response = restTemplate.postForEntity<Unit>("$baseUrl/register", request)
        assert(response.statusCode == HttpStatus.BAD_REQUEST)

    }

    @Test
    fun registerUserInvalidPassword_NoSpecialChars() {
        val baseUrl = "http://localhost:$port/user"
        val user = UserRegistrationDTO("testNickname", "Password123", "testEmail@gmail.com")

        val request = HttpEntity(user)
        val response = restTemplate.postForEntity<Unit>("$baseUrl/register", request)
        assert(response.statusCode == HttpStatus.BAD_REQUEST)

    }

    @Test
    fun registerUserInvalidPassword_No8Chars() {
        val baseUrl = "http://localhost:$port/user"
        val user = UserRegistrationDTO("testNickname", "pass1)", "testEmail@gmail.com")

        val request = HttpEntity(user)
        val response = restTemplate.postForEntity<Unit>("$baseUrl/register", request)
        assert(response.statusCode == HttpStatus.BAD_REQUEST)

    }

    @Test
    fun registerUserInvalidPassword_NoNumber() {
        val baseUrl = "http://localhost:$port/user"
        val user = UserRegistrationDTO("testNickname", "Password)", "testEmail@gmail.com")

        val request = HttpEntity(user)
        val response = restTemplate.postForEntity<Unit>("$baseUrl/register", request)
        assert(response.statusCode == HttpStatus.BAD_REQUEST)

    }

    @Test
    fun registerUserInvalidPassword_NoUpperCaseLetter() {
        val baseUrl = "http://localhost:$port/user"
        val user = UserRegistrationDTO("testNickname", "password123)", "testEmail@gmail.com")

        val request = HttpEntity(user)
        val response = restTemplate.postForEntity<Unit>("$baseUrl/register", request)
        assert(response.statusCode == HttpStatus.BAD_REQUEST)

    }

    @Test
    fun registerUserNotUniqueEmail() {
        val baseUrl = "http://localhost:$port/user"
        val user = UserRegistrationDTO("testNicknameUnique", "Password123)", "testEmail2@gmail.com")

        val request = HttpEntity(user)
        restTemplate.postForEntity<Unit>("$baseUrl/register", request)

        val response = restTemplate.postForEntity<Unit>("$baseUrl/register", request)
        assert(response.statusCode == HttpStatus.BAD_REQUEST)

    }

    @Test
    fun registerUserNotUniqueNickname() {
        val baseUrl = "http://localhost:$port/user"
        val user = UserRegistrationDTO("testNickname2", "Password123)", "testEmailUnique@gmail.com")

        val request = HttpEntity(user)
        restTemplate.postForEntity<Unit>("$baseUrl/register", request)

        val response = restTemplate.postForEntity<Unit>("$baseUrl/register", request)
        assert(response.statusCode == HttpStatus.BAD_REQUEST)

    }
    
    @Test
    fun validateUserSuccessfully(){
        val baseUrl = "http://localhost:$port/user"
        val user = UserRegistrationDTO("testNicknameValidate", "Password123)", "testEmailValidate@gmail.com")

        val request = HttpEntity(user)
        val response = restTemplate.postForEntity<RegistrationToValidateDTO>("$baseUrl/register", request)

        val activationCode = activationRepository.findById(response.body?.provisional_id!!).block()?.activationCode

        val requestValidate = HttpEntity(ValidateRegistrationDTO(response.body?.provisional_id!!.toString(), activationCode!!))

        val responseValidate = restTemplate.postForEntity<Unit>("$baseUrl/validate", requestValidate)
        assert(responseValidate.statusCode == HttpStatus.CREATED)

    }

    @Test
    fun validateUserWrongUUID() {
        val baseUrl = "http://localhost:$port/user"

        val requestValidate = HttpEntity(ValidateRegistrationDTO(UUID.randomUUID().toString(), 123))

        val responseValidate = restTemplate.postForEntity<Unit>("$baseUrl/validate", requestValidate)
        assert(responseValidate.statusCode == HttpStatus.NOT_FOUND)

    }

    @Test
    fun validateUserAfterExpiration() {
        val baseUrl = "http://localhost:$port/user"
        val user = UserRegistrationDTO("testNicknameValidateExp", "Password123)", "testEmailValidateExp@gmail.com")

        val request = HttpEntity(user)
        val response = restTemplate.postForEntity<RegistrationToValidateDTO>("$baseUrl/register", request)

        val activationRow = activationRepository.findById(response.body?.provisional_id!!).block()

        activationRow!!.deadline = LocalDateTime.now().minusDays(1)

        activationRepository.save(activationRow).block()

        val requestValidate = HttpEntity(ValidateRegistrationDTO(response.body?.provisional_id!!.toString(), activationRow.activationCode))

        val responseValidate = restTemplate.postForEntity<Unit>("$baseUrl/validate", requestValidate)
        assert(responseValidate.statusCode == HttpStatus.NOT_FOUND)
    }

    @Test
    fun validateUserWrongActivationCode_OnlyOne() {
        val baseUrl = "http://localhost:$port/user"
        val user = UserRegistrationDTO("testNicknameValidateC", "Password123)", "testEmailValidateC@gmail.com")

        val request = HttpEntity(user)
        val response = restTemplate.postForEntity<RegistrationToValidateDTO>("$baseUrl/register", request)

        val requestValidate = HttpEntity(ValidateRegistrationDTO(response.body?.provisional_id!!.toString(), 1))

        val responseValidate = restTemplate.postForEntity<Unit>("$baseUrl/validate", requestValidate)
        assert(responseValidate.statusCode == HttpStatus.NOT_FOUND)
    }

    @Test
    fun validateUserWrongActivationCode_CounterTo0() {
        val baseUrl = "http://localhost:$port/user"
        val user = UserRegistrationDTO("testNicknameValidateC0", "Password123)", "testEmailValidateC0@gmail.com")

        val request = HttpEntity(user)
        val response = restTemplate.postForEntity<RegistrationToValidateDTO>("$baseUrl/register", request)

        val requestValidate = HttpEntity(ValidateRegistrationDTO(response.body?.provisional_id!!.toString(), 1))

        var responseValidate : ResponseEntity<Unit>? = null
        for(i in 1..5) {
            responseValidate = restTemplate.postForEntity("$baseUrl/validate", requestValidate)

        }
        assert(responseValidate?.statusCode == HttpStatus.NOT_FOUND)
    }


    @Test
    fun loginWithWrongUsername() {
        val baseUrl = "http://localhost:$port/user"
        val user = UserRegistrationDTO("loginNickname", "Password123)", "loginEmail@gmail.com")

        val request = HttpEntity(user)
        restTemplate.postForEntity<Unit>("$baseUrl/register", request)

        val response = restTemplate.postForEntity<Unit>("$baseUrl/login", LoginDTO("", user.password))

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun loginWithWrongPassword() {
        val baseUrl = "http://localhost:$port/user"
        val user = UserRegistrationDTO("loginNickname2", "Password123)", "loginEmail2@gmail.com")

        val request = HttpEntity(user)
        restTemplate.postForEntity<Unit>("$baseUrl/register", request)

        val response = restTemplate.postForEntity<Unit>("$baseUrl/login", LoginDTO(user.username, ""))

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun loginWithCorrectData_Inactivated() {
        val baseUrl = "http://localhost:$port/user"
        val user = UserRegistrationDTO("loginNickname3", "Password123)", "loginEmail3@gmail.com")

        val request = HttpEntity(user)
        restTemplate.postForEntity<Unit>("$baseUrl/register", request)

        val response = restTemplate.postForEntity<Unit>("$baseUrl/login", LoginDTO(user.username, user.password))

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun loginWithCorrectData_Activated() {
        val baseUrl = "http://localhost:$port/user"
        val user = UserRegistrationDTO("loginNickname3", "Password123)", "loginEmail3@gmail.com")

        val request = HttpEntity(user)
        restTemplate.postForEntity<Unit>("$baseUrl/register", request)

        val dbUser = userRepository.findByUsername(user.username).block()

        dbUser?.id?.let {
            val db = userRepository.findById(it).block()
            db?.active = true
            userRepository.save(db!!)
        }

        val response = restTemplate.postForEntity<String>("$baseUrl/login", LoginDTO(user.username, user.password))

        assert(response.statusCode == HttpStatus.OK)
    }
}
package it.polito.wa2.registration_login

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.r2dbc.spi.ConnectionFactory
import it.polito.wa2.registration_login.dtos.*
import it.polito.wa2.registration_login.repositories.ActivationRepository
import it.polito.wa2.registration_login.repositories.UserRepository
import it.polito.wa2.registration_login.security.Role
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.postForEntity
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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

    @Autowired
    lateinit var activationRepository: ActivationRepository

    @Autowired
    lateinit var userRepository : UserRepository

    @Value("\${application.registration_loginKey}")
    lateinit var secretString: String

    @Value("\${application.tokenPrefix}")
    lateinit var prefix : String

    fun createAdminJWT() : String {

        val generatedKey: SecretKey = Keys.hmacShaKeyFor(secretString.toByteArray(StandardCharsets.UTF_8))

        val jwt = Jwts.builder()
            .setSubject("Admin")
            .setExpiration(
                Date.from(
                    LocalDateTime.now().plusHours(1).atZone(ZoneId.systemDefault()).toInstant()
                )
            )
            .setIssuedAt(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()))
            .claim("role", Role.ADMIN)
            .signWith(generatedKey)
            .compact()

        return "$prefix$jwt"
    }

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
        for(i in 1..6) {
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
            userRepository.save(db!!).subscribe()
        }

        val response = restTemplate.postForEntity<String>("$baseUrl/login", LoginDTO(user.username, user.password))

        assert(response.statusCode == HttpStatus.OK)
    }

    /** NEW TESTS */

    @Test
    fun registerDeviceWithoutAuth() {

        val baseUrl = "http://localhost:$port/device"
        val device = DeviceRegistrationDTO("testDevice", "Password123)", "A")

        val request = HttpEntity(device)
        val response = restTemplate.postForEntity<Unit>("$baseUrl/register", request)

        assert(response.statusCode == HttpStatus.UNAUTHORIZED)


    }

    @Test
    fun registerDeviceSuccessfully() {

        val baseUrl = "http://localhost:$port/device"
        val device = DeviceRegistrationDTO("testDevice", "Password123)", "A")
        val auth = HttpHeaders()
        auth.set("Authorization", createAdminJWT())
        val request = HttpEntity(device,auth)
        val response = restTemplate.postForEntity<Unit>("$baseUrl/register", request)

        assert(response.statusCode == HttpStatus.ACCEPTED)


    }

    @Test
    fun registerDeviceEmptyZone() {
        val baseUrl = "http://localhost:$port/device"
        val device = DeviceRegistrationDTO("testNickname", "Password123)", "")

        val auth = HttpHeaders()
        auth.set("Authorization", createAdminJWT())
        val request = HttpEntity(device,auth)
        val response = restTemplate.postForEntity<Unit>("$baseUrl/register", request)
        assert(response.statusCode == HttpStatus.BAD_REQUEST)

    }

    @Test
    fun registerDeviceEmptyName() {
        val baseUrl = "http://localhost:$port/device"
        val device = DeviceRegistrationDTO("", "Password123)", "A")

        val auth = HttpHeaders()
        auth.set("Authorization", createAdminJWT())
        val request = HttpEntity(device,auth)
        val response = restTemplate.postForEntity<Unit>("$baseUrl/register", request)
        assert(response.statusCode == HttpStatus.BAD_REQUEST)

    }

    @Test
    fun registerDeviceEmptyPassword() {
        val baseUrl = "http://localhost:$port/device"
        val device = DeviceRegistrationDTO("testNickname", "", "A")

        val auth = HttpHeaders()
        auth.set("Authorization", createAdminJWT())
        val request = HttpEntity(device,auth)
        val response = restTemplate.postForEntity<Unit>("$baseUrl/register", request)
        assert(response.statusCode == HttpStatus.BAD_REQUEST)

    }

    @Test
    fun registerDeviceInvalidPassword_No8Chars() {
        val baseUrl = "http://localhost:$port/device"
        val device = DeviceRegistrationDTO("testNickname", "pass1)", "A")

        val auth = HttpHeaders()
        auth.set("Authorization", createAdminJWT())
        val request = HttpEntity(device,auth)
        val response = restTemplate.postForEntity<Unit>("$baseUrl/register", request)
        assert(response.statusCode == HttpStatus.BAD_REQUEST)

    }

    @Test
    fun registerDeviceInvalidPassword_NoNumber() {
        val baseUrl = "http://localhost:$port/device"
        val device = DeviceRegistrationDTO("testNickname", "Password)", "A")

        val auth = HttpHeaders()
        auth.set("Authorization", createAdminJWT())
        val request = HttpEntity(device,auth)
        val response = restTemplate.postForEntity<Unit>("$baseUrl/register", request)
        assert(response.statusCode == HttpStatus.BAD_REQUEST)

    }

    @Test
    fun registerDeviceInvalidPassword_NoUpperCaseLetter() {
        val baseUrl = "http://localhost:$port/device"
        val device = DeviceRegistrationDTO("testNickname", "password123)", "A")

        val auth = HttpHeaders()
        auth.set("Authorization", createAdminJWT())
        val request = HttpEntity(device,auth)
        val response = restTemplate.postForEntity<Unit>("$baseUrl/register", request)
        assert(response.statusCode == HttpStatus.BAD_REQUEST)

    }

    @Test
    fun registerDeviceNotUniqueNickname() {
        val baseUrl = "http://localhost:$port/device"
        val device = DeviceRegistrationDTO("testNickname2", "Password123)", "A")

        val auth = HttpHeaders()
        auth.set("Authorization", createAdminJWT())
        val request = HttpEntity(device,auth)
        restTemplate.postForEntity<Unit>("$baseUrl/register", request)

        val response = restTemplate.postForEntity<Unit>("$baseUrl/register", request)
        assert(response.statusCode == HttpStatus.BAD_REQUEST)

    }

    @Test
    fun loginWithWrongDeviceName() {
        val baseUrl = "http://localhost:$port/device"
        val device = DeviceRegistrationDTO("loginNickname", "Password123)", "A")

        val auth = HttpHeaders()
        auth.set("Authorization", createAdminJWT())
        val request = HttpEntity(device,auth)
        restTemplate.postForEntity<Unit>("$baseUrl/register", request)

        val response = restTemplate.postForEntity<Unit>("$baseUrl/login", LoginDTO("", device.password))

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun loginWithWrongDevicePassword() {
        val baseUrl = "http://localhost:$port/device"
        val device = DeviceRegistrationDTO("loginNickname2", "Password123)", "A")

        val auth = HttpHeaders()
        auth.set("Authorization", createAdminJWT())
        val request = HttpEntity(device,auth)
        restTemplate.postForEntity<Unit>("$baseUrl/register", request)

        val response = restTemplate.postForEntity<Unit>("$baseUrl/login", LoginDTO(device.name, ""))

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun loginWithCorrectData_Device() {
        val baseUrl = "http://localhost:$port/device"
        val device = DeviceRegistrationDTO("loginNickname3", "Password123)", "A")

        val auth = HttpHeaders()
        auth.set("Authorization", createAdminJWT())
        val request = HttpEntity(device,auth)
        restTemplate.postForEntity<Unit>("$baseUrl/register", request)

        val response = restTemplate.postForEntity<String>("$baseUrl/login", LoginDTO(device.name, device.password))

        assert(response.statusCode == HttpStatus.OK)
    }

    @Test
    fun makeAdminWithIncorrectUsername() {
        val baseUrl = "http://localhost:$port"
        val user = UserRegistrationDTO("adminNickname", "Password123)", "loginEmailAdmin@gmail.com")

        val request = HttpEntity(user)
        restTemplate.postForEntity<Unit>("$baseUrl/user/register", request)

        val dbUser = userRepository.findByUsername(user.username).block()

        dbUser?.id?.let {
            val db = userRepository.findById(it).block()
            db?.active = true
            userRepository.save(db!!).subscribe()
        }

        val auth = HttpHeaders()
        auth.set("Authorization", createAdminJWT())
        val request2 = HttpEntity("", auth)
        val response = restTemplate.postForEntity<String>("$baseUrl/admin/wrongName/update", request2)

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun makeAdminWithNotActivatedUser() {
        val baseUrl = "http://localhost:$port"
        val user = UserRegistrationDTO("adminNickname2", "Password123)", "loginEmailAdmin2@gmail.com")

        val request = HttpEntity(user)
        restTemplate.postForEntity<Unit>("$baseUrl/user/register", request)

        val auth = HttpHeaders()
        auth.set("Authorization", createAdminJWT())
        val request2 = HttpEntity("", auth)
        val response = restTemplate.postForEntity<Unit>("$baseUrl/admin/${user.username}/update", request2)

        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun makeAdminCorrectly() {
        val baseUrl = "http://localhost:$port"
        val user = UserRegistrationDTO("adminNickname3", "Password123)", "loginEmailAdmin3@gmail.com")

        val request = HttpEntity(user)
        restTemplate.postForEntity<Unit>("$baseUrl/user/register", request)

        val dbUser = userRepository.findByUsername(user.username).block()

        dbUser?.id?.let {
            val db = userRepository.findById(it).block()
            db?.active = true
            userRepository.save(db!!).subscribe()
        }

        val auth = HttpHeaders()
        auth.set("Authorization", createAdminJWT())
        val request2 = HttpEntity("", auth)
        val response = restTemplate.postForEntity<String>("$baseUrl/admin/${user.username}/update", request2)

        assert(response.statusCode == HttpStatus.ACCEPTED)
    }

}
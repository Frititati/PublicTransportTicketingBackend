package it.polito.wa2.registration_login

import it.polito.wa2.registration_login.dtos.RegistrationDTO
import it.polito.wa2.registration_login.dtos.ValidateDTO
import it.polito.wa2.registration_login.entities.Activation
import it.polito.wa2.registration_login.entities.User
import it.polito.wa2.registration_login.repositories.ActivationRepository
import it.polito.wa2.registration_login.repositories.UserRepository
import it.polito.wa2.registration_login.security.Role
import it.polito.wa2.registration_login.security.SecurityConfiguration
import it.polito.wa2.registration_login.services.EmailService
import it.polito.wa2.registration_login.services.UserService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import java.time.LocalDateTime
import java.util.*

@SpringBootTest
class UnitTestsBusinessLogic {

    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var emailService: EmailService

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var activationRepository: ActivationRepository


    @Autowired
    lateinit var securityConfiguration: SecurityConfiguration

    @Test
    fun rejectRegisterInvalidPassword_empty() {
        val user = RegistrationDTO("testPassword", "", "testPassword@gmail.com")
        Assertions.assertEquals(Pair(HttpStatus.BAD_REQUEST, null), userService.register(user))
    }

    @Test
    fun rejectRegisterInvalidPassword_less8Chars() {
        val user = RegistrationDTO("testPassword", "Pass1)", "testPassword@gmail.com")
        Assertions.assertEquals(Pair(HttpStatus.BAD_REQUEST, null), userService.register(user))
    }

    @Test
    fun rejectRegisterInvalidPassword_emptySpace() {
        val user = RegistrationDTO("testPassword", "Password 123)", "testPassword@gmail.com")
        Assertions.assertEquals(Pair(HttpStatus.BAD_REQUEST, null), userService.register(user))
    }

    @Test
    fun rejectRegisterInvalidPassword_noLowerCase() {
        val user = RegistrationDTO("testPassword", "PASSWORD123)", "testPassword@gmail.com")
        Assertions.assertEquals(Pair(HttpStatus.BAD_REQUEST, null), userService.register(user))
    }

    @Test
    fun rejectRegisterInvalidPassword_noUpperCase() {
        val user = RegistrationDTO("testPassword", "password123)", "testPassword@gmail.com")
        Assertions.assertEquals(Pair(HttpStatus.BAD_REQUEST, null), userService.register(user))
    }

    @Test
    fun rejectRegisterInvalidPassword_noDigits() {
        val user = RegistrationDTO("testPassword", "Password)", "testPassword@gmail.com")
        Assertions.assertEquals(Pair(HttpStatus.BAD_REQUEST, null), userService.register(user))
    }

    @Test
    fun rejectRegisterInvalidPassword_noSpecialChars() {
        val user = RegistrationDTO("testPassword", "Password123", "testPassword@gmail.com")
        Assertions.assertEquals(Pair(HttpStatus.BAD_REQUEST, null), userService.register(user))
    }

    @Test
    fun rejectRegisterInvalidEmail_noAt() {
        val user = RegistrationDTO("testPassword", "Password123)", "testPasswordGmail.com")
        Assertions.assertEquals(Pair(HttpStatus.BAD_REQUEST, null), userService.register(user))
    }

    @Test
    fun rejectRegisterInvalidEmail_noDomain() {
        val user = RegistrationDTO("testPassword", "Password123)", "testPassword@gmailcom")
        Assertions.assertEquals(Pair(HttpStatus.BAD_REQUEST, null), userService.register(user))
    }

    @Test
    fun rejectRegisterNotUniqueNickname() {
        val userValid = RegistrationDTO("testUser", "Password123)", "testUser1@gmail.com")
        val userInvalid = RegistrationDTO("testUser", "Password123)", "testUser2@gmail.com")

        val response: Pair<HttpStatus, UUID?> = userService.register(userValid)
        val invalidResponse: Pair<HttpStatus, UUID?> = userService.register(userInvalid)

        val userId = activationRepository.findById(response.second!!).get().user.id

        activationRepository.deleteById(response.second!!)
        userRepository.deleteById(userId!!)

        Assertions.assertEquals(Pair(HttpStatus.BAD_REQUEST, null), invalidResponse)
    }

    @Test
    fun rejectRegisterNotUniqueEmail() {
        val userValid = RegistrationDTO("testUser1", "Password123)", "testUser@gmail.com")
        val userInvalid = RegistrationDTO("testUser2", "Password123)", "testUser@gmail.com")

        val response: Pair<HttpStatus, UUID?> = userService.register(userValid)
        val invalidResponse: Pair<HttpStatus, UUID?> = userService.register(userInvalid)

        val userId = activationRepository.findById(response.second!!).get().user.id

        activationRepository.deleteById(response.second!!)
        userRepository.deleteById(userId!!)

        Assertions.assertEquals(Pair(HttpStatus.BAD_REQUEST, null), invalidResponse)
    }

    @Test
    fun acceptRegisterValidUser() {
        val user = RegistrationDTO("testUser", "Password123)", "testUser@gmail.com")

        val response: Pair<HttpStatus, UUID?> = userService.register(user)

        val userId = activationRepository.findById(response.second!!).get().user.id

        activationRepository.deleteById(response.second!!)
        userRepository.deleteById(userId!!)

        Assertions.assertEquals(HttpStatus.ACCEPTED, response.first)
    }

    @Test
    fun rejectValidationEmpty_uuid() {
        Assertions.assertEquals(Pair(HttpStatus.NOT_FOUND, null), userService.validate("", 123456))
    }

    @Test
    fun rejectValidationEmpty_activationCode() {
        val user =
            userRepository.save(
                User(
                    null,
                    "testUser",
                    securityConfiguration.passwordEncoder().encode("Password123)"),
                    "testUser@gmail.com",
                    Role.CUSTOMER,
                    false,
                    null
                )
            )
        val activation =
            activationRepository.save(
                Activation(
                    null,
                    (Math.random() * (999999 - 100000) + 100000).toInt(),
                    LocalDateTime.now().plusDays(1),
                    5,
                    user
                )
            )

        userRepository.deleteById(user.id!!)

        Assertions.assertEquals(Pair(HttpStatus.NOT_FOUND, null), userService.validate(activation.id.toString(), 0))
    }

    @Test
    fun reduceActivationOnWrongCode() {
        val user =
            userRepository.save(
                User(
                    null,
                    "testUser",
                    securityConfiguration.passwordEncoder().encode("Password123)"),
                    "testUser@gmail.com",
                    Role.CUSTOMER,
                    false,
                    null
                )
            )
        val activation =
            activationRepository.save(
                Activation(
                    null,
                    (Math.random() * (999999 - 100000) + 100000).toInt(),
                    LocalDateTime.now().plusDays(1),
                    5,
                    user
                )
            )

        userService.validate(activation.id.toString(), 0)

        val activation2: Activation = activationRepository.findById(activation.id!!).get()

        userService.validate(activation.id.toString(), 0)
        val activation3: Activation = activationRepository.findById(activation.id!!).get()

        userRepository.deleteById(user.id!!)

        Assertions.assertEquals(4, activation2.counter)
        Assertions.assertEquals(3, activation3.counter)
    }

    @Test
    fun deleteActivationOnManyWrongCode() {
        val user =
            userRepository.save(
                User(
                    null,
                    "testUser",
                    securityConfiguration.passwordEncoder().encode("Password123)"),
                    "testUser@gmail.com",
                    Role.CUSTOMER,
                    false,
                    null
                )
            )
        val activation =
            activationRepository.save(
                Activation(
                    null,
                    (Math.random() * (999999 - 100000) + 100000).toInt(),
                    LocalDateTime.now().plusDays(1),
                    5,
                    user
                )
            )

        userService.validate(activation.id.toString(), 0)

        val activation2: Activation = activationRepository.findById(activation.id!!).get()

        userService.validate(activation.id.toString(), 0)
        val activation3: Activation = activationRepository.findById(activation.id!!).get()

        userService.validate(activation.id.toString(), 0)

        val activation4: Activation = activationRepository.findById(activation.id!!).get()

        userService.validate(activation.id.toString(), 0)
        val activation5: Activation = activationRepository.findById(activation.id!!).get()

        userService.validate(activation.id.toString(), 0)
        val activationRowEmpty = activationRepository.findById(activation.id!!)
        val userRowEmpty = userRepository.findById(user.id!!)

        Assertions.assertEquals(4, activation2.counter)
        Assertions.assertEquals(3, activation3.counter)
        Assertions.assertEquals(2, activation4.counter)
        Assertions.assertEquals(1, activation5.counter)
        Assertions.assertTrue(activationRowEmpty.isEmpty)
        Assertions.assertTrue(userRowEmpty.isEmpty)
    }

    @Test
    fun deleteActivationOnExpiredDeadline() {
        val user =
            userRepository.save(
                User(
                    null,
                    "testUser",
                    securityConfiguration.passwordEncoder().encode("Password123)"),
                    "testUser@gmail.com",
                    Role.CUSTOMER,
                    false,
                    null
                )
            )
        val activation =
            activationRepository.save(
                Activation(
                    null,
                    (Math.random() * (999999 - 100000) + 100000).toInt(),
                    LocalDateTime.now().minusDays(1),
                    5,
                    user
                )
            )

        userService.validate(activation.id.toString(), activation.activationCode)

        val activationRowEmpty = activationRepository.findById(activation.id!!)
        val userRowEmpty = userRepository.findById(user.id!!)

        Assertions.assertTrue(activationRowEmpty.isEmpty)
        Assertions.assertTrue(userRowEmpty.isEmpty)
    }

    @Test
    fun rejectActivationOnManyWrongCode() {
        val user =
            userRepository.save(
                User(
                    null,
                    "testUser",
                    securityConfiguration.passwordEncoder().encode("Password123)"),
                    "testUser@gmail.com",
                    Role.CUSTOMER,
                    false,
                    null
                )
            )
        val activation =
            activationRepository.save(
                Activation(
                    null,
                    (Math.random() * (999999 - 100000) + 100000).toInt(),
                    LocalDateTime.now().plusDays(1),
                    5,
                    user
                )
            )

        userService.validate(activation.id.toString(), 0)

        userService.validate(activation.id.toString(), 0)

        userService.validate(activation.id.toString(), 0)

        userService.validate(activation.id.toString(), 0)

        val validationResponse: Pair<HttpStatus, ValidateDTO?> =
            userService.validate(activation.id.toString(), 0)

        Assertions.assertEquals(HttpStatus.NOT_FOUND, validationResponse.first)
    }

    @Test
    fun rejectActivationOnExpiredDeadline() {
        val user =
            userRepository.save(
                User(
                    null,
                    "testUser",
                    securityConfiguration.passwordEncoder().encode("Password123)"),
                    "testUser@gmail.com",
                    Role.CUSTOMER,
                    false,
                    null
                )
            )
        val activation =
            activationRepository.save(
                Activation(
                    null,
                    (Math.random() * (999999 - 100000) + 100000).toInt(),
                    LocalDateTime.now().minusDays(1),
                    5,
                    user
                )
            )

        val validationResponse: Pair<HttpStatus, ValidateDTO?> =
            userService.validate(activation.id.toString(), activation.activationCode)

        Assertions.assertEquals(HttpStatus.NOT_FOUND, validationResponse.first)
    }

    @Test
    fun acceptValidation() {
        val user =
            userRepository.save(
                User(
                    null,
                    "testUser",
                    securityConfiguration.passwordEncoder().encode("Password123)"),
                    "testUser@gmail.com",
                    Role.CUSTOMER,
                    false,
                    null
                )
            )
        val activation =
            activationRepository.save(
                Activation(
                    null,
                    (Math.random() * (999999 - 100000) + 100000).toInt(),
                    LocalDateTime.now().plusDays(1),
                    5,
                    user
                )
            )

        val validationResponse: Pair<HttpStatus, ValidateDTO?> =
            userService.validate(activation.id.toString(), activation.activationCode)

        userRepository.deleteById(user.id!!)

        Assertions.assertEquals(HttpStatus.CREATED, validationResponse.first)
        Assertions.assertEquals(user.id, validationResponse.second!!.userId)
        Assertions.assertEquals(user.email, validationResponse.second!!.email)
        Assertions.assertEquals(user.nickname, validationResponse.second!!.nickname)
    }

    @Test
    fun acceptValidationWithOneBadRequest() {
        val user =
            userRepository.save(
                User(
                    null,
                    "testUser",
                    securityConfiguration.passwordEncoder().encode("Password123)"),
                    "testUser@gmail.com",
                    Role.CUSTOMER,
                    false,
                    null
                )
            )
        val activation =
            activationRepository.save(
                Activation(
                    null,
                    (Math.random() * (999999 - 100000) + 100000).toInt(),
                    LocalDateTime.now().plusDays(1),
                    5,
                    user
                )
            )

        userService.validate(activation.id.toString(), 0)

        val validationResponse: Pair<HttpStatus, ValidateDTO?> =
            userService.validate(activation.id.toString(), activation.activationCode)

        userRepository.deleteById(user.id!!)

        Assertions.assertEquals(HttpStatus.CREATED, validationResponse.first)
        Assertions.assertEquals(user.id, validationResponse.second!!.userId)
        Assertions.assertEquals(user.email, validationResponse.second!!.email)
        Assertions.assertEquals(user.nickname, validationResponse.second!!.nickname)
    }

    @Test
    fun acceptFullRegistrationValidation() {
        val registrationDTO = RegistrationDTO("testUser", "Password123)", "testUser@gmail.com")

        val registrationResponse: Pair<HttpStatus, UUID?> = userService.register(registrationDTO)

        val activation : Activation = activationRepository.findById(registrationResponse.second!!).get()

        val validationResponse: Pair<HttpStatus, ValidateDTO?> =
            userService.validate(activation.id.toString(), activation.activationCode)

        userRepository.deleteById(activation.user.id!!)

        Assertions.assertEquals(HttpStatus.CREATED, validationResponse.first)
        Assertions.assertEquals(activation.user.id, validationResponse.second!!.userId)
        Assertions.assertEquals(registrationDTO.email, validationResponse.second!!.email)
        Assertions.assertEquals(registrationDTO.nickname, validationResponse.second!!.nickname)
    }

    @Test
    fun acceptSendMail() {
        val emailResult = emailService.sendMessage("testUser@gmail.com", 123456)

        Assertions.assertTrue(emailResult)
    }
}
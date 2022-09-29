package it.polito.wa2.registration_login

import it.polito.wa2.registration_login.dtos.RegistrationToValidateDTO
import it.polito.wa2.registration_login.dtos.UserRegistrationDTO
import it.polito.wa2.registration_login.dtos.ValidateDTO
import it.polito.wa2.registration_login.entities.Activation
import it.polito.wa2.registration_login.entities.User
import it.polito.wa2.registration_login.repositories.ActivationRepository
import it.polito.wa2.registration_login.repositories.UserRepository
import it.polito.wa2.registration_login.security.Role
import it.polito.wa2.registration_login.security.WebSecurityConfig
import it.polito.wa2.registration_login.services.EmailService
import it.polito.wa2.registration_login.services.RegisterService
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import java.time.LocalDateTime

@SpringBootTest
class UnitTestsBusinessLogic {

    @Autowired
    lateinit var registerService: RegisterService

    @Autowired
    lateinit var emailService: EmailService

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var activationRepository: ActivationRepository


    @Autowired
    lateinit var webSecurityConfig: WebSecurityConfig

    @Test
    fun rejectRegisterInvalidPassword_empty() = runBlocking {
        val user = UserRegistrationDTO("testPassword", "", "testPassword@gmail.com")
        Assertions.assertEquals(Pair(HttpStatus.BAD_REQUEST, null), registerService.registerUser(user))
    }

    @Test
    fun rejectRegisterInvalidPassword_less8Chars() = runBlocking {
        val user = UserRegistrationDTO("testPassword", "Pass1)", "testPassword@gmail.com")
        Assertions.assertEquals(Pair(HttpStatus.BAD_REQUEST, null), registerService.registerUser(user))
    }

    @Test
    fun rejectRegisterInvalidPassword_emptySpace() = runBlocking {
        val user = UserRegistrationDTO("testPassword", "Password 123)", "testPassword@gmail.com")
        Assertions.assertEquals(Pair(HttpStatus.BAD_REQUEST, null), registerService.registerUser(user))
    }

    @Test
    fun rejectRegisterInvalidPassword_noLowerCase() = runBlocking {
        val user = UserRegistrationDTO("testPassword", "PASSWORD123)", "testPassword@gmail.com")
        Assertions.assertEquals(Pair(HttpStatus.BAD_REQUEST, null), registerService.registerUser(user))
    }

    @Test
    fun rejectRegisterInvalidPassword_noUpperCase() = runBlocking {
        val user = UserRegistrationDTO("testPassword", "password123)", "testPassword@gmail.com")
        Assertions.assertEquals(Pair(HttpStatus.BAD_REQUEST, null), registerService.registerUser(user))
    }

    @Test
    fun rejectRegisterInvalidPassword_noDigits() = runBlocking {
        val user = UserRegistrationDTO("testPassword", "Password)", "testPassword@gmail.com")
        Assertions.assertEquals(Pair(HttpStatus.BAD_REQUEST, null), registerService.registerUser(user))
    }

    @Test
    fun rejectRegisterInvalidPassword_noSpecialChars() = runBlocking {
        val user = UserRegistrationDTO("testPassword", "Password123", "testPassword@gmail.com")
        Assertions.assertEquals(Pair(HttpStatus.BAD_REQUEST, null), registerService.registerUser(user))
    }

    @Test
    fun rejectRegisterInvalidEmail_noAt() = runBlocking {
        val user = UserRegistrationDTO("testPassword", "Password123)", "testPasswordGmail.com")
        Assertions.assertEquals(Pair(HttpStatus.BAD_REQUEST, null), registerService.registerUser(user))
    }

    @Test
    fun rejectRegisterInvalidEmail_noDomain() = runBlocking {
        val user = UserRegistrationDTO("testPassword", "Password123)", "testPassword@gmailcom")
        Assertions.assertEquals(Pair(HttpStatus.BAD_REQUEST, null), registerService.registerUser(user))
    }

    @Test
    fun rejectRegisterNotUniqueNickname() = runBlocking {
        val userValid = UserRegistrationDTO("testUser", "Password123)", "testUser1@gmail.com")
        val userInvalid = UserRegistrationDTO("testUser", "Password123)", "testUser2@gmail.com")

        val response: Pair<HttpStatus, RegistrationToValidateDTO?> = registerService.registerUser(userValid)
        val invalidResponse: Pair<HttpStatus, RegistrationToValidateDTO?> = registerService.registerUser(userInvalid)

        val userId = activationRepository.findById(response.second?.provisional_id!!).awaitSingleOrNull()?.userId

        //activationRepository.deleteById(response.second?.provisional_id!!)
        userRepository.deleteById(userId!!).subscribe()

        Assertions.assertEquals(Pair(HttpStatus.BAD_REQUEST, null), invalidResponse)
    }

    @Test
    fun rejectRegisterNotUniqueEmail() = runBlocking {
        val userValid = UserRegistrationDTO("testUser1", "Password123)", "testUser@gmail.com")
        val userInvalid = UserRegistrationDTO("testUser2", "Password123)", "testUser@gmail.com")

        val response: Pair<HttpStatus, RegistrationToValidateDTO?> = registerService.registerUser(userValid)
        val invalidResponse: Pair<HttpStatus, RegistrationToValidateDTO?> = registerService.registerUser(userInvalid)

        val userId = activationRepository.findById(response.second?.provisional_id!!).awaitSingleOrNull()?.userId

        //activationRepository.deleteById(response.second?.provisional_id!!)
        userRepository.deleteById(userId!!).subscribe()

        Assertions.assertEquals(Pair(HttpStatus.BAD_REQUEST, null), invalidResponse)
    }

    @Test
    fun acceptRegisterValidUser() = runBlocking {
        val user = UserRegistrationDTO("testUser", "Password123)", "testUser@gmail.com")

        val response: Pair<HttpStatus, RegistrationToValidateDTO?> = registerService.registerUser(user)

        val userId = activationRepository.findById(response.second?.provisional_id!!).awaitSingleOrNull()?.userId

        //activationRepository.deleteById(response.second?.provisional_id!!)
        userRepository.deleteById(userId!!).subscribe()

        Assertions.assertEquals(HttpStatus.ACCEPTED, response.first)
    }

    @Test
    fun rejectValidationEmpty_uuid() = runBlocking {
        Assertions.assertEquals(Pair(HttpStatus.NOT_FOUND, null), registerService.validate("", 123456))
    }

    @Test
    fun rejectValidationEmpty_activationCode() = runBlocking {
        val user =
            userRepository.save(
                User(
                    null,
                    "testUser",
                    webSecurityConfig.passwordEncoder().encode("Password123)"),
                    "testUser@gmail.com",
                    Role.CUSTOMER.ordinal,
                    false
                )
            ).awaitLast()
        val activation =
            activationRepository.save(
                Activation(
                    null,
                    (Math.random() * (999999 - 100000) + 100000).toInt(),
                    LocalDateTime.now().plusDays(1),
                    5,
                    user.id!!
                )
            ).awaitLast()

        userRepository.deleteById(user.id!!).subscribe()

        Assertions.assertEquals(Pair(HttpStatus.NOT_FOUND, null), registerService.validate(activation.id.toString(), 0))
    }

    @Test
    fun reduceActivationOnWrongCode() = runBlocking {
        val user =
            userRepository.save(
                User(
                    null,
                    "testUser",
                    webSecurityConfig.passwordEncoder().encode("Password123)"),
                    "testUser@gmail.com",
                    Role.CUSTOMER.ordinal,
                    false
                )
            ).awaitLast()
        val activation =
            activationRepository.save(
                Activation(
                    null,
                    (Math.random() * (999999 - 100000) + 100000).toInt(),
                    LocalDateTime.now().plusDays(1),
                    5,
                    user.id!!
                )
            ).awaitLast()

        registerService.validate(activation.id.toString(), 0)

        val activation2: Activation = activationRepository.findById(activation.id!!).awaitFirst()

        registerService.validate(activation.id.toString(), 0)
        val activation3: Activation = activationRepository.findById(activation.id!!).awaitFirst()

        userRepository.deleteById(user.id!!).subscribe()

        Assertions.assertEquals(4, activation2.counter)
        Assertions.assertEquals(3, activation3.counter)
    }

    @Test
    fun deleteActivationOnManyWrongCode() = runBlocking {
        val user =
            userRepository.save(
                User(
                    null,
                    "testUser",
                    webSecurityConfig.passwordEncoder().encode("Password123)"),
                    "testUser@gmail.com",
                    Role.CUSTOMER.ordinal,
                    false
                )
            ).awaitLast()
        val activation =
            activationRepository.save(
                Activation(
                    null,
                    (Math.random() * (999999 - 100000) + 100000).toInt(),
                    LocalDateTime.now().plusDays(1),
                    5,
                    user.id!!
                )
            ).awaitLast()

        registerService.validate(activation.id.toString(), 0)

        val activation2: Activation = activationRepository.findById(activation.id!!).awaitFirst()

        registerService.validate(activation.id.toString(), 0)
        val activation3: Activation = activationRepository.findById(activation.id!!).awaitFirst()

        registerService.validate(activation.id.toString(), 0)

        val activation4: Activation = activationRepository.findById(activation.id!!).awaitFirst()

        registerService.validate(activation.id.toString(), 0)
        val activation5: Activation = activationRepository.findById(activation.id!!).awaitFirst()

        registerService.validate(activation.id.toString(), 0)
        val activationRowEmpty = activationRepository.findById(activation.id!!).awaitSingleOrNull()
        val userRowEmpty = userRepository.findById(user.id!!).awaitSingleOrNull()

        Assertions.assertEquals(4, activation2.counter)
        Assertions.assertEquals(3, activation3.counter)
        Assertions.assertEquals(2, activation4.counter)
        Assertions.assertEquals(1, activation5.counter)
        Assertions.assertEquals(activationRowEmpty, null)
        Assertions.assertEquals(userRowEmpty, null)
    }

    @Test
    fun deleteActivationOnExpiredDeadline() = runBlocking {
        val user =
            userRepository.save(
                User(
                    null,
                    "testUser",
                    webSecurityConfig.passwordEncoder().encode("Password123)"),
                    "testUser@gmail.com",
                    Role.CUSTOMER.ordinal,
                    false
                )
            ).awaitLast()
        val activation =
            activationRepository.save(
                Activation(
                    null,
                    (Math.random() * (999999 - 100000) + 100000).toInt(),
                    LocalDateTime.now().minusDays(1),
                    5,
                    user.id!!
                )
            ).awaitLast()

        registerService.validate(activation.id.toString(), activation.activationCode)

        val activationRowEmpty = activationRepository.findById(activation.id!!).awaitSingleOrNull()
        val userRowEmpty = userRepository.findById(user.id!!).awaitSingleOrNull()

        Assertions.assertEquals(activationRowEmpty, null)
        Assertions.assertEquals(userRowEmpty, null)
    }

    @Test
    fun rejectActivationOnManyWrongCode() = runBlocking {
        val user =
            userRepository.save(
                User(
                    null,
                    "testUser",
                    webSecurityConfig.passwordEncoder().encode("Password123)"),
                    "testUser@gmail.com",
                    Role.CUSTOMER.ordinal,
                    false
                )
            ).awaitLast()
        val activation =
            activationRepository.save(
                Activation(
                    null,
                    (Math.random() * (999999 - 100000) + 100000).toInt(),
                    LocalDateTime.now().plusDays(1),
                    5,
                    user.id!!
                )
            ).awaitLast()

        registerService.validate(activation.id.toString(), 0)

        registerService.validate(activation.id.toString(), 0)

        registerService.validate(activation.id.toString(), 0)

        registerService.validate(activation.id.toString(), 0)

        val validationResponse: Pair<HttpStatus, ValidateDTO?> =
            registerService.validate(activation.id.toString(), 0)

        Assertions.assertEquals(HttpStatus.NOT_FOUND, validationResponse.first)
    }

    @Test
    fun rejectActivationOnExpiredDeadline() = runBlocking {
        val user =
            userRepository.save(
                User(
                    null,
                    "testUser",
                    webSecurityConfig.passwordEncoder().encode("Password123)"),
                    "testUser@gmail.com",
                    Role.CUSTOMER.ordinal,
                    false
                )
            ).awaitLast()
        val activation =
            activationRepository.save(
                Activation(
                    null,
                    (Math.random() * (999999 - 100000) + 100000).toInt(),
                    LocalDateTime.now().minusDays(1),
                    5,
                    user.id!!
                )
            ).awaitLast()

        val validationResponse: Pair<HttpStatus, ValidateDTO?> =
            registerService.validate(activation.id.toString(), activation.activationCode)

        Assertions.assertEquals(HttpStatus.NOT_FOUND, validationResponse.first)
    }

    @Test
    fun acceptValidation() = runBlocking {
        val user =
            userRepository.save(
                User(
                    null,
                    "testUser",
                    webSecurityConfig.passwordEncoder().encode("Password123)"),
                    "testUser@gmail.com",
                    Role.CUSTOMER.ordinal,
                    false
                )
            ).awaitLast()
        val activation =
            activationRepository.save(
                Activation(
                    null,
                    (Math.random() * (999999 - 100000) + 100000).toInt(),
                    LocalDateTime.now().plusDays(1),
                    5,
                    user.id!!
                )
            ).awaitLast()

        val validationResponse: Pair<HttpStatus, ValidateDTO?> =
            registerService.validate(activation.id.toString(), activation.activationCode)

        userRepository.deleteById(user.id!!).subscribe()

        Assertions.assertEquals(HttpStatus.CREATED, validationResponse.first)
        Assertions.assertEquals(user.id, validationResponse.second!!.userId)
        Assertions.assertEquals(user.email, validationResponse.second!!.email)
        Assertions.assertEquals(user.username, validationResponse.second!!.username)
    }

    @Test
    fun acceptValidationWithOneBadRequest() = runBlocking {
        val user =
            userRepository.save(
                User(
                    null,
                    "testUser",
                    webSecurityConfig.passwordEncoder().encode("Password123)"),
                    "testUser@gmail.com",
                    Role.CUSTOMER.ordinal,
                    false
                )
            ).awaitLast()
        val activation =
            activationRepository.save(
                Activation(
                    null,
                    (Math.random() * (999999 - 100000) + 100000).toInt(),
                    LocalDateTime.now().plusDays(1),
                    5,
                    user.id!!
                )
            ).awaitLast()

        registerService.validate(activation.id.toString(), 0)

        val validationResponse: Pair<HttpStatus, ValidateDTO?> =
            registerService.validate(activation.id.toString(), activation.activationCode)

        userRepository.deleteById(user.id!!).subscribe()

        Assertions.assertEquals(HttpStatus.CREATED, validationResponse.first)
        Assertions.assertEquals(user.id, validationResponse.second!!.userId)
        Assertions.assertEquals(user.email, validationResponse.second!!.email)
        Assertions.assertEquals(user.username, validationResponse.second!!.username)
    }

    @Test
    fun acceptFullRegistrationValidation() = runBlocking {
        val userRegistrationDTO = UserRegistrationDTO("testUser", "Password123)", "testUser@gmail.com")

        val registrationResponse: Pair<HttpStatus, RegistrationToValidateDTO?> = registerService.registerUser(userRegistrationDTO)

        val activation: Activation = activationRepository.findById(registrationResponse.second?.provisional_id!!).awaitFirst()

        val validationResponse: Pair<HttpStatus, ValidateDTO?> =
            registerService.validate(activation.id.toString(), activation.activationCode)

        userRepository.deleteById(activation.userId!!).subscribe()

        Assertions.assertEquals(HttpStatus.CREATED, validationResponse.first)
        Assertions.assertEquals(activation.userId, validationResponse.second!!.userId)
        Assertions.assertEquals(userRegistrationDTO.email, validationResponse.second!!.email)
        Assertions.assertEquals(userRegistrationDTO.username, validationResponse.second!!.username)
    }

    @Test
    fun acceptSendMail() {
        val emailResult = emailService.sendMessage("testUser@gmail.com", 123456)

        Assertions.assertTrue(emailResult)
    }
}
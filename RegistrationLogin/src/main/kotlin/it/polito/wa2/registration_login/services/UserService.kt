package it.polito.wa2.registration_login.services

import it.polito.wa2.registration_login.dtos.*
import it.polito.wa2.registration_login.entities.Activation
import it.polito.wa2.registration_login.entities.User
import it.polito.wa2.registration_login.repositories.ActivationRepository
import it.polito.wa2.registration_login.repositories.UserRepository
import it.polito.wa2.registration_login.security.Role
import it.polito.wa2.registration_login.security.SecurityConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class UserService {

    @Autowired
    lateinit var emailService: EmailService

    @Autowired
    lateinit var activationRepository: ActivationRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var securityConfiguration: SecurityConfiguration

    private val specialChar = "[!\"#$%&'()*+,-./:;\\\\<=>?@\\[\\]^_`{|}~]"
    private val mailChar =
        "^(?=.{1,64}@)[A-Za-z\\d_-]+(\\.[A-Za-z\\d_-]+)*@[^-][A-Za-z\\d-]+(\\.[A-Za-z\\d-]+)*(\\.[A-Za-z]{2,})$"
    private val min = 100000
    private val max = 999999

    fun register(user: RegistrationDTO): Pair<HttpStatus, UUID?> {
        /**
         * 1. username, password, and email address cannot be empty;
         * 2. username and email address must be unique system-wide;
         * 3. password must be reasonably strong (it must not contain any whitespace, it must be at least 8 characters long,
        it must contain at least one digit, one uppercase letter, one lowercase letter, one non-alphanumeric character);
         * 4. email address must be valid.
         */
        return when {
            user.nickname.isEmpty() || user.email.isEmpty() -> Pair(HttpStatus.BAD_REQUEST, null)
            userRepository.findByNickname(user.nickname)?.nickname?.isNotEmpty() == true -> Pair(HttpStatus.BAD_REQUEST, null)
            userRepository.findByEmail(user.email)?.email?.isNotEmpty() == true -> Pair(HttpStatus.BAD_REQUEST, null)
            !validatePassword(user.password) -> Pair(HttpStatus.BAD_REQUEST, null)
            !validateEmail(user.email) -> Pair(HttpStatus.BAD_REQUEST, null)
            else -> {
                try {
                    val userToDB =
                        userRepository.save(
                            User(
                                null,
                                user.nickname,
                                securityConfiguration.passwordEncoder().encode(user.password),
                                user.email,
                                Role.CUSTOMER,
                                false,
                                null
                            )
                        )

                    val activationRow =
                        activationRepository.save(
                            Activation(
                                null,
                                (Math.random() * (max - min) + min).toInt(),
                                LocalDateTime.now().plusDays(1),
                                5,
                                userToDB
                            )
                        )

                    emailService.sendMessage(user.email, activationRow.activationCode)

                    Pair(HttpStatus.ACCEPTED, activationRow.id)
                } catch (e: Exception) {
                    Pair(HttpStatus.BAD_REQUEST, null)
                }
            }
        }
    }

    fun validatePassword(password: String): Boolean {
        return when {
            password.isEmpty() -> {
                println("Password must not be empty")
                false
            }
            password.length < 8 -> {
                println("Password must be at least 8 characters long")
                false
            }
            password.contains(" ") -> {
                println("Password must not contains empty spaces")
                false
            }
            !password.contains("[a-z]".toRegex()) -> {
                println("Password does not contain any lowercase letters")
                false
            }
            !password.contains("[A-Z]".toRegex()) -> {
                println("Password does not contain any uppercase letters")
                false
            }
            !password.contains("\\d".toRegex()) -> {
                println("Password does not contain any digits")
                false
            }
            !password.contains(specialChar.toRegex()) -> {
                println("Password does not contain any special characters")
                false
            }
            else -> true
        }
    }

    fun validateEmail(email: String): Boolean {
        return when {
            mailChar.toRegex().matches(email) -> true
            else -> {
                println("Invalid email")
                false
            }
        }
    }

    fun validate(provisional_id: String, activation_code: Int): Pair<HttpStatus, ValidateDTO?> {

        try {
            // if provisional_id exist, fetch the row from db
            val activationRow = activationRepository.findById(UUID.fromString(provisional_id))

            if (activationRow.isEmpty)
                return Pair(HttpStatus.NOT_FOUND, null)
            else {
                val activation = activationRow.get()
                val activationDTO = activation.toDTO()

                // if expiration < now() -> delete rows from db + 404
                if (activationDTO.deadline.isBefore(LocalDateTime.now())) {
                    activationRepository.deleteById(UUID.fromString(provisional_id))
                    activationDTO.user.id?.let { userRepository.deleteById(it) }
                    return Pair(HttpStatus.NOT_FOUND, null)
                } else if (activation_code != activationDTO.activationCode) {
                    // if provisional_id ok but wrong activation_code -> counter-1 + 404
                    if (activationDTO.counter == 1) {
                        // if counter == 0 -> 404 + delete rows from db
                        activationRepository.deleteById(UUID.fromString(provisional_id))
                        activationDTO.user.id?.let { userRepository.deleteById(it) }

                    } else {
                        //counter - 1
                        activation.counter--

                        activationRepository.save(activation)
                    }
                    return Pair(HttpStatus.NOT_FOUND, null)

                } else {
                    // if provisional_id + activation_code ok -> return ValidateDTO + 201
                    val userRow = userRepository.findById(activationDTO.user.id!!)
                    val user = userRow.get()
                    user.active = true
                    user.activation = null

                    userRepository.save(user)

                    activationRepository.deleteById(UUID.fromString(provisional_id))

                    val userDTO = user.toDTO()
                    return Pair(HttpStatus.CREATED, ValidateDTO(userDTO.id!!, userDTO.nickname, userDTO.email))
                }
            }

        } catch (e: Exception) {
            return Pair(HttpStatus.NOT_FOUND, null)
        }

    }

    @Scheduled(fixedDelay = 20000)
    fun registrationScheduledCleanup() {
        activationRepository.findAllExpired().map {
            it.user.id?.let { it1 -> userRepository.deleteById(it1) }
        }
    }
}



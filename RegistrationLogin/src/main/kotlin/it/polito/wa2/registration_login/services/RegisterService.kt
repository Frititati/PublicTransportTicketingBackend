package it.polito.wa2.registration_login.services

import it.polito.wa2.registration_login.dtos.*
import it.polito.wa2.registration_login.entities.Activation
import it.polito.wa2.registration_login.entities.Device
import it.polito.wa2.registration_login.entities.User
import it.polito.wa2.registration_login.entities.UserRegister
import it.polito.wa2.registration_login.repositories.ActivationRepository
import it.polito.wa2.registration_login.repositories.DeviceRepository
import it.polito.wa2.registration_login.repositories.UserRepository
import it.polito.wa2.registration_login.security.Role
import it.polito.wa2.registration_login.security.WebSecurityConfig
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
class RegisterService(
    @Autowired
    private val kafkaTemplate: KafkaTemplate<String, UserRegister>,
    @Value("\${kafka.topics.userRegister}")
    val topic: String
) {

    @Autowired
    lateinit var emailService: EmailService

    @Autowired
    lateinit var activationRepository: ActivationRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var deviceRepository: DeviceRepository

    @Autowired
    lateinit var webSecurityConfig: WebSecurityConfig

    private val log = LoggerFactory.getLogger(javaClass)

    private val specialChar = "[!\"#$%&'()*+,-./:;\\\\<=>?@\\[\\]^_`{|}~]"
    private val mailChar =
        "^(?=.{1,64}@)[A-Za-z\\d_-]+(\\.[A-Za-z\\d_-]+)*@[^-][A-Za-z\\d-]+(\\.[A-Za-z\\d-]+)*(\\.[A-Za-z]{2,})$"
    private val min = 100000
    private val max = 999999

    /**
     * @param user {
     *                 username: String,
     *                 email: String,
     *                 password: String
     *              }
     *
     * @return HttpStatus: 200 OK or 400 error
     *         RegistrationToValidateDTO: If 200 return provisional_id and email, otherwise null
     */
    suspend fun registerUser(user: UserRegistrationDTO): Pair<HttpStatus, RegistrationToValidateDTO?> {
        /**
         * 1. username, password, and email address cannot be empty;
         * 2. username and email address must be unique system-wide;
         * 3. password must be reasonably strong (it must not contain any whitespace, it must be at least 8 characters long,
        it must contain at least one digit, one uppercase letter, one lowercase letter, one non-alphanumeric character);
         * 4. email address must be valid.
         */
        return when {
            user.username.isEmpty() || user.email.isEmpty() -> Pair(HttpStatus.BAD_REQUEST, null)
            userRepository.findByUsername(user.username).awaitFirstOrNull()?.username?.isNotEmpty() == true -> Pair(
                HttpStatus.BAD_REQUEST,
                null
            )

            userRepository.findByEmail(user.email)
                .awaitFirstOrNull()?.email?.isNotEmpty() == true -> Pair(HttpStatus.BAD_REQUEST, null)

            !validatePassword(user.password) -> Pair(HttpStatus.BAD_REQUEST, null)
            !validateEmail(user.email) -> Pair(HttpStatus.BAD_REQUEST, null)

            else -> {
                try {
                    val userToDB = userRepository.save(
                        User(
                            null,
                            user.username,
                            webSecurityConfig.passwordEncoder().encode(user.password),
                            user.email,
                            Role.CUSTOMER.ordinal,
                            false,
                        )
                    ).awaitLast()

                    val activationRow =
                        activationRepository.save(
                            Activation(
                                null,
                                (Math.random() * (max - min) + min).toInt(),
                                LocalDateTime.now().plusDays(1),
                                5,
                                userToDB.id
                            )
                        ).awaitLast()



                    emailService.sendMessage(user.email, activationRow.activationCode)

                    Pair(HttpStatus.ACCEPTED, RegistrationToValidateDTO(activationRow.id, user.email))
                } catch (e: Exception) {
                    Pair(HttpStatus.BAD_REQUEST, null)
                }
            }
        }
    }

    /**
     * @param password String ti validate
     *
     * @return true if password has at least 8 characters, no empty spaces, an uppercase letter, a lowercase letter,
     *         a digit and a special character
     */
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

    /**
     * @param email String to validate
     *
     * @return True if the String is formatted as email
     *         False if it is not formatted in a good way
     */
    fun validateEmail(email: String): Boolean {
        return when {
            mailChar.toRegex().matches(email) -> true
            else -> {
                println("Invalid email")
                false
            }
        }
    }

    /**
     * @param device {
     *                  name: String
     *                  password: String
     *                  zone: String
     *               }
     *
     * @return HttpStatus 200 OK or 400 error
     *         The name of the created device if everything is OK (200) otherwise null
     */

    suspend fun registerDevice(device: DeviceRegistrationDTO): Pair<HttpStatus, DeviceRegisteredDTO?> {
        /**
         * 1. username, password, and zone cannot be empty;
         * 2. username must be unique system-wide;
         * 3. password must be reasonably strong (it must not contain any whitespace, it must be at least 8 characters long,
        it must contain at least one digit, one uppercase letter, one lowercase letter, one non-alphanumeric character);
         */
        return when {
            device.name.isEmpty() -> Pair(HttpStatus.BAD_REQUEST, null)
            deviceRepository.findByName(device.name).awaitFirstOrNull()?.name?.isNotEmpty() == true -> Pair(
                HttpStatus.BAD_REQUEST,
                null
            )

            !validatePassword(device.password) -> Pair(HttpStatus.BAD_REQUEST, null)

            device.zone.isEmpty() -> Pair(HttpStatus.BAD_REQUEST, null)

            else -> {
                try {
                    val deviceRow = deviceRepository.save(
                        Device(
                            null,
                            device.name,
                            webSecurityConfig.passwordEncoder().encode(device.password),
                            device.zone
                        )
                    ).awaitLast()

                    val deviceDTO = deviceRow.toDTO()
                    Pair(HttpStatus.ACCEPTED, DeviceRegisteredDTO(deviceDTO.id!!, deviceDTO.name, device.zone))
                } catch (e: Exception) {
                    Pair(HttpStatus.BAD_REQUEST, null)
                }
            }
        }
    }

    /**
     * @param provisional_id String with UUID provided to the user
     * @param activation_code Int with the activation_code provided by email to the user
     *
     * @return HttpStatus 200 OK or 404 error
     *         userId, nickname and email if everything is OK, otherwise null
     */
    suspend fun validate(provisional_id: String, activation_code: Int): Pair<HttpStatus, ValidateDTO?> {

        try {
            // if provisional_id exist, fetch the row from db
            activationRepository.findById(UUID.fromString(provisional_id)).awaitFirstOrNull()?.let {
                val activationDTO = it.toDTO()

                // if expiration < now() -> delete rows from db + 404
                if (activationDTO.deadline.isBefore(LocalDateTime.now())) {
                    activationDTO.userId.let { user ->
                        if (user != null) {
                            userRepository.deleteById(user).awaitSingleOrNull()
                        }
                    }
                    return Pair(HttpStatus.NOT_FOUND, null)
                } else if (activation_code != activationDTO.activationCode) {
                    // if provisional_id ok but wrong activation_code -> counter-1 + 404
                    if (activationDTO.counter == 1) {
                        // if counter == 0 -> 404 + delete rows from db
                        activationDTO.userId?.let { user -> userRepository.deleteById(user).subscribe() }

                    } else {
                        //counter - 1
                        it.counter--

                        activationRepository.save(it).awaitLast()
                    }
                    return Pair(HttpStatus.NOT_FOUND, null)

                } else {
                    // if provisional_id + activation_code ok -> return ValidateDTO + 201
                    val userRow = userRepository.findById(activationDTO.userId!!).awaitLast()
                    userRow.active = true

                    userRepository.save(userRow).awaitLast()

                    activationRepository.deleteById(UUID.fromString(provisional_id)).thenReturn(provisional_id)
                        .awaitLast()

                    val userDTO = userRow.toDTO()

                    /**
                     * Once the user is correctly validated, we send the information to the TravelService through Kafka
                     * in a way that the profile of the user will be created automatically.
                     */
                    try {
                        val userRegister = UserRegister(userRow.username)
                        log.info("Sending message to Kafka {}", userRegister)
                        val message: Message<UserRegister> = MessageBuilder
                            .withPayload(userRegister)
                            .setHeader(KafkaHeaders.TOPIC, topic)
                            .build()
                        kafkaTemplate.send(message)

                        log.info("Message sent with success")
                    } catch (e: Exception) {
                        log.error("Exception: $e", e)
                    }

                    return Pair(HttpStatus.CREATED, ValidateDTO(userDTO.id!!, userDTO.username, userDTO.email))
                }
            } ?: return Pair(HttpStatus.NOT_FOUND, null)

        } catch (e: Exception) {
            return Pair(HttpStatus.NOT_FOUND, null)
        }

    }

    /**
     * Scheduled task to check periodically if there are users with expired date that still not activate the account.
     * In this case, the account will be eliminated to the database, as well as the row on the activation table.
     */
    @Scheduled(fixedDelay = 20000)
    fun registrationScheduledCleanup() {
        runBlocking {
            activationRepository.findAllExpired()
                .mapNotNull {
                    it.userId?.let { it1 ->
                        userRepository.deleteById(it1).subscribe()
                    }
                    println("Removed expired user")
                    //}.doFinally {
                    //    println("select * from activation a where a.deadline < now()")
                }
                .subscribe()
        }
    }
}



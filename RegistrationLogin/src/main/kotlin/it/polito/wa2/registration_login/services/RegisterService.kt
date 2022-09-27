package it.polito.wa2.registration_login.services

import it.polito.wa2.registration_login.dtos.*
import it.polito.wa2.registration_login.entities.Activation
import it.polito.wa2.registration_login.entities.Device
import it.polito.wa2.registration_login.entities.User
import it.polito.wa2.registration_login.repositories.ActivationRepository
import it.polito.wa2.registration_login.repositories.DeviceRepository
import it.polito.wa2.registration_login.repositories.UserRepository
import it.polito.wa2.registration_login.security.Role
import it.polito.wa2.registration_login.security.WebSecurityConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
class RegisterService {

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

    private val specialChar = "[!\"#$%&'()*+,-./:;\\\\<=>?@\\[\\]^_`{|}~]"
    private val mailChar =
        "^(?=.{1,64}@)[A-Za-z\\d_-]+(\\.[A-Za-z\\d_-]+)*@[^-][A-Za-z\\d-]+(\\.[A-Za-z\\d-]+)*(\\.[A-Za-z]{2,})$"
    private val min = 100000
    private val max = 999999

    suspend fun registerUser(user: UserRegistrationDTO): Pair<HttpStatus, RegistrationToValidateDTO?> {
        /**
         * 1. username, password, and email address cannot be empty;
         * 2. username and email address must be unique system-wide;
         * 3. password must be reasonably strong (it must not contain any whitespace, it must be at least 8 characters long,
        it must contain at least one digit, one uppercase letter, one lowercase letter, one non-alphanumeric character);
         * 4. email address must be valid.
         */
        // TODO: migliorabile togliendo awaitLast e usando and ecc..
        return when {
            user.nickname.isEmpty() || user.email.isEmpty() -> Pair(HttpStatus.BAD_REQUEST, null)
            userRepository.findByNickname(user.nickname).awaitFirstOrNull()?.nickname?.isNotEmpty() == true -> Pair(
                HttpStatus.BAD_REQUEST,
                null
            )

            userRepository.findByEmail(user.email)
                .awaitFirstOrNull()?.email?.isNotEmpty() == true -> Pair(HttpStatus.BAD_REQUEST, null)

            !validatePassword(user.password) -> Pair(HttpStatus.BAD_REQUEST, null)
            !validateEmail(user.email) -> Pair(HttpStatus.BAD_REQUEST, null)

            else -> {
                try {
                    // TODO: it's better to do it in another way transactionally?
                    val userToDB = userRepository.save(
                        User(
                            null,
                            user.nickname,
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

    @Transactional
    suspend fun registerDevice(device: DeviceRegistrationDTO): Pair<HttpStatus, String?> {
        /**
         * 1. username, password, and email address cannot be empty;
         * 2. username and email address must be unique system-wide;
         * 3. password must be reasonably strong (it must not contain any whitespace, it must be at least 8 characters long,
        it must contain at least one digit, one uppercase letter, one lowercase letter, one non-alphanumeric character);
         * 4. email address must be valid.
         */
        return when {
            device.name.isEmpty() -> Pair(HttpStatus.BAD_REQUEST, null)
            userRepository.findByNickname(device.name).awaitLast()?.nickname?.isNotEmpty() == true -> Pair(
                HttpStatus.BAD_REQUEST,
                null
            )

            !withContext(Dispatchers.IO) {
                validatePassword(device.password)
            } -> Pair(HttpStatus.BAD_REQUEST, null)

            else -> {
                try {
                    deviceRepository.save(
                        Device(
                            null,
                            device.name,
                            webSecurityConfig.passwordEncoder().encode(device.password),
                            device.zone,
                            Role.DEVICE
                        )
                    ).awaitLast()

                    Pair(HttpStatus.ACCEPTED, device.name)
                } catch (e: Exception) {
                    Pair(HttpStatus.BAD_REQUEST, null)
                }
            }
        }
    }

    suspend fun validate(provisional_id: String, activation_code: Int): Pair<HttpStatus, ValidateDTO?> {

        try {
            // if provisional_id exist, fetch the row from db
            activationRepository.findById(UUID.fromString(provisional_id)).awaitFirstOrNull()?.let {
                val activationDTO = it.toDTO()

                // if expiration < now() -> delete rows from db + 404
                if (activationDTO.deadline.isBefore(LocalDateTime.now())) {
                    activationRepository.deleteById(UUID.fromString(provisional_id)).awaitLast()
                    activationDTO.userId.let { user ->
                        if (user != null) {
                            userRepository.deleteById(user).block()
                        }
                    }
                    return Pair(HttpStatus.NOT_FOUND, null)
                } else if (activation_code != activationDTO.activationCode) {
                    // if provisional_id ok but wrong activation_code -> counter-1 + 404
                    if (activationDTO.counter == 1) {
                        // if counter == 0 -> 404 + delete rows from db
                        activationRepository.deleteById(UUID.fromString(provisional_id)).awaitLast()
                        activationDTO.userId?.let { user -> userRepository.deleteById(user).block() }

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
                    return Pair(HttpStatus.CREATED, ValidateDTO(userDTO.id!!, userDTO.nickname, userDTO.email))
                }
            } ?: return Pair(HttpStatus.NOT_FOUND, null)

        } catch (e: Exception) {
            return Pair(HttpStatus.NOT_FOUND, null)
        }

    }

    //TODO: Verificare se funziona
    @Scheduled(fixedDelay = 20000)
    fun registrationScheduledCleanup() {
        runBlocking {
            activationRepository.findAllExpired().mapNotNull {
                println("ciao")
                it.userId?.let { it1 ->

                    userRepository.deleteById(it1).thenReturn(it1).awaitLast() //TODO il problema è qui
                    println("ciao4")

                }
            }.awaitLast()
            println("ciao2")
        }
        println("ciao3")
    }

}



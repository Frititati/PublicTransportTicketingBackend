package it.polito.wa2.registration_login.services

import it.polito.wa2.registration_login.dtos.DeviceRegistrationDTO
import it.polito.wa2.registration_login.dtos.RegistrationDTO
import it.polito.wa2.registration_login.dtos.ValidateDTO
import it.polito.wa2.registration_login.dtos.toDTO
import it.polito.wa2.registration_login.entities.Activation
import it.polito.wa2.registration_login.entities.User
import it.polito.wa2.registration_login.repositories.UserRepository
import it.polito.wa2.registration_login.security.Role
import it.polito.wa2.registration_login.security.SecurityConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class DeviceService {

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var securityConfiguration: SecurityConfiguration

    private val specialChar = "[!\"#$%&'()*+,-./:;\\\\<=>?@\\[\\]^_`{|}~]"

    fun registerDevice(device: DeviceRegistrationDTO): Pair<HttpStatus, UUID?> {
        /**
         * 1. username, password, and email address cannot be empty;
         * 2. username and email address must be unique system-wide;
         * 3. password must be reasonably strong (it must not contain any whitespace, it must be at least 8 characters long,
        it must contain at least one digit, one uppercase letter, one lowercase letter, one non-alphanumeric character);
         * 4. email address must be valid.
         */
        return when {
            device.nickname.isEmpty() -> Pair(HttpStatus.BAD_REQUEST, null)
            userRepository.findByNickname(device.nickname)?.nickname?.isNotEmpty() == true -> Pair(HttpStatus.BAD_REQUEST, null)
            !validatePassword(device.password) -> Pair(HttpStatus.BAD_REQUEST, null)
            else -> {
                try {
                        userRepository.save(
                            User(
                                null,
                                device.nickname,
                                securityConfiguration.passwordEncoder().encode(device.password),
                                "",
                                Role.DEVICE,
                                true,
                                null
                            )
                        )

                    Pair(HttpStatus.ACCEPTED, null)
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

}
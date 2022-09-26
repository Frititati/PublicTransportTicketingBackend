package it.polito.wa2.registration_login.services

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import it.polito.wa2.registration_login.dtos.LoginDTO
import it.polito.wa2.registration_login.repositories.DeviceRepository
import it.polito.wa2.registration_login.repositories.UserRepository
import it.polito.wa2.registration_login.security.Role
import it.polito.wa2.registration_login.security.SecurityConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import javax.crypto.SecretKey

@Service
class LoginService {

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var deviceRepository: DeviceRepository

    @Autowired
    lateinit var securityConfiguration: SecurityConfiguration

    @Value("\${application.registration_loginKey}")
    lateinit var secretString: String

    fun loginUser(credentials: LoginDTO): Pair<HttpStatus, String?> {

        val user = userRepository.findByNickname(credentials.username)

        return if (user?.nickname?.isNotEmpty() == true) {
            val userPassword = user.password
            val userActive = user.active
            if (securityConfiguration.passwordEncoder().matches(credentials.password, userPassword) && userActive) {


                val generatedKey: SecretKey = Keys.hmacShaKeyFor(secretString.toByteArray(StandardCharsets.UTF_8))

                val jwt = Jwts.builder()
                    .setSubject(user.nickname)
                    .setExpiration(
                        Date.from(
                            LocalDateTime.now().plusHours(1).atZone(ZoneId.systemDefault()).toInstant()
                        )
                    )
                    .setIssuedAt(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()))
                    .claim("role", user.role)
                    .signWith(generatedKey)
                    .compact()

                Pair(HttpStatus.OK, jwt)
            } else Pair(HttpStatus.BAD_REQUEST, null)
        } else Pair(HttpStatus.BAD_REQUEST, null)
    }

    fun loginDevice(credentials: LoginDTO): Pair<HttpStatus, String?> {

        val device = deviceRepository.findByName(credentials.username)

        return if (device?.name?.isNotEmpty() == true) {
            val userPassword = device.password
            if (securityConfiguration.passwordEncoder().matches(credentials.password, userPassword)) {

                val generatedKey: SecretKey = Keys.hmacShaKeyFor(secretString.toByteArray(StandardCharsets.UTF_8))

                val jwt = Jwts.builder()
                    .setSubject(device.name)
                    .setExpiration(
                        Date.from(
                            LocalDateTime.now().plusHours(24).atZone(ZoneId.systemDefault()).toInstant()
                        )
                    )
                    .setIssuedAt(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()))
                    .claim("role", Role.DEVICE)
                    .claim("zone", device.zone)
                    .signWith(generatedKey)
                    .compact()

                Pair(HttpStatus.OK, jwt)
            } else Pair(HttpStatus.BAD_REQUEST, null)
        } else Pair(HttpStatus.BAD_REQUEST, null)
    }
}
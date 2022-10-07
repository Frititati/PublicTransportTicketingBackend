package it.polito.wa2.registration_login.services

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import it.polito.wa2.registration_login.dtos.LoginDTO
import it.polito.wa2.registration_login.dtos.LoginJWTDTO
import it.polito.wa2.registration_login.repositories.DeviceRepository
import it.polito.wa2.registration_login.repositories.UserRepository
import it.polito.wa2.registration_login.security.Role
import it.polito.wa2.registration_login.security.WebSecurityConfig
import kotlinx.coroutines.reactive.awaitFirstOrNull
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
    lateinit var webSecurityConfig: WebSecurityConfig

    @Value("\${application.registration_loginKey}")
    lateinit var secretString: String

    /**
     * @param credentials {
     *                      username: String
     *                      password: String
     *                    }
     *
     * @return HttpStatus 200 OK or 400 error
     *         jwt for user if everything is ok, otherwise null
     */
    suspend fun loginUser(credentials: LoginDTO): Pair<HttpStatus, LoginJWTDTO?> {

        val user = userRepository.findByUsername(credentials.username).awaitFirstOrNull()

        return if (user?.username?.isNotEmpty() == true) {
            val userPassword = user.password
            val userActive = user.active
            if (webSecurityConfig.passwordEncoder().matches(credentials.password, userPassword) && userActive) {


                val generatedKey: SecretKey = Keys.hmacShaKeyFor(secretString.toByteArray(StandardCharsets.UTF_8))

                val jwt = Jwts.builder()
                    .setSubject(user.username)
                    .setExpiration(
                        Date.from(
                            LocalDateTime.now().plusHours(1).atZone(ZoneId.systemDefault()).toInstant()
                        )
                    )
                    .setIssuedAt(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()))
                    .claim("role", Role.values()[user.role])
                    .signWith(generatedKey)
                    .compact()

                Pair(HttpStatus.OK, LoginJWTDTO(jwt))
            } else Pair(HttpStatus.BAD_REQUEST, null)
        } else Pair(HttpStatus.BAD_REQUEST, null)
    }

    /**
     * @param credentials {
     *                      username: String
     *                      password: String
     *                    }
     *
     * @return HttpStatus 200 OK or 400 error
     *         jwt for device if everything is ok, otherwise null
     */
    suspend fun loginDevice(credentials: LoginDTO): Pair<HttpStatus, LoginJWTDTO?> {

        val device = deviceRepository.findByName(credentials.username).awaitFirstOrNull()

        return if (device?.name?.isNotEmpty() == true) {
            val userPassword = device.password
            if (webSecurityConfig.passwordEncoder().matches(credentials.password, userPassword)) {

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

                Pair(HttpStatus.OK, LoginJWTDTO(jwt))
            } else Pair(HttpStatus.BAD_REQUEST, null)
        } else Pair(HttpStatus.BAD_REQUEST, null)
    }
}
package it.polito.wa2.transit

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import it.polito.wa2.transit.dtos.PrincipalUserDTO
import it.polito.wa2.transit.dtos.TicketToValidateDTO
import it.polito.wa2.transit.dtos.TimeReportDTO
import it.polito.wa2.transit.services.TransitService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithSecurityContext
import org.springframework.security.test.context.support.WithSecurityContextFactory
import org.springframework.test.context.ContextConfiguration
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import javax.crypto.SecretKey

@SpringBootTest
@ContextConfiguration
class TransitUnitTest {
    @Autowired
    lateinit var transitService: TransitService

    @Value("\${application.ticketKey}")
    lateinit var secretString: String

    @Value("\${application.tokenPrefix}")
    lateinit var prefix: String

    @Retention(AnnotationRetention.RUNTIME)
    @WithSecurityContext(factory = WithMockCustomUserSecurityContextFactory::class)
    annotation class WithMockCustomUser(val username: String, val role : String)

    class WithMockCustomUserSecurityContextFactory : WithSecurityContextFactory<WithMockCustomUser> {
        override fun createSecurityContext(customUser: WithMockCustomUser): SecurityContext {
            val context = SecurityContextHolder.createEmptyContext()
            val principal = PrincipalUserDTO(customUser.username, "A", "")
            val auth: Authentication =
                UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    mutableListOf(SimpleGrantedAuthority("ROLE_${customUser.role}"))
                )
            context.authentication = auth
            return context
        }
    }

    fun createJWSTicket(uuid: String, expiresAt: LocalDateTime, issuedAt: LocalDateTime, type:String, zones:String, username:String): String {

        val generatedKey: SecretKey = Keys.hmacShaKeyFor(secretString.toByteArray(StandardCharsets.UTF_8))

        val jwt = Jwts.builder().setSubject(uuid)
            .setExpiration(Date.from(expiresAt.atZone(ZoneId.systemDefault()).toInstant()))
            .setIssuedAt(Date.from(issuedAt.atZone(ZoneId.systemDefault()).toInstant()))
            .claim("type", type)
            .claim("zid", zones)
            .claim("username", username).signWith(generatedKey).compact()

        return jwt
    }

    /** TICKET VALIDATION TEST */
    @Test
    @WithMockCustomUser("Device1", "DEVICE")
    fun validateDailyTicket() = runBlocking {
        val uuid  = UUID.randomUUID()
        val jws = createJWSTicket(uuid.toString(), LocalDateTime.now().plusHours(1), LocalDateTime.now(), "DAILY", "A", "prova1")
        val ticketToValidate = TicketToValidateDTO(jws)
        Assertions.assertEquals(HttpStatus.ACCEPTED, transitService.validateTicket(ticketToValidate).first)
        transitService.deleteTicketByTicketId(uuid)
    }

    @Test
    @WithMockCustomUser("Device1", "DEVICE")
    fun validateTicketExpired() = runBlocking {
        val jws = createJWSTicket(UUID.randomUUID().toString(), LocalDateTime.now().minusHours(1), LocalDateTime.now(), "DAILY", "A", "prova1")
        val ticketToValidate = TicketToValidateDTO(jws)
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, transitService.validateTicket(ticketToValidate).first)
    }

    @Test
    @WithMockCustomUser("Device1", "DEVICE")
    fun validateDailyTicketMultipleTimes() = runBlocking {
        val uuid = UUID.randomUUID()
        val jws = createJWSTicket(uuid.toString(), LocalDateTime.now().plusHours(1), LocalDateTime.now(), "DAILY", "A", "prova1")
        val ticketToValidate = TicketToValidateDTO(jws)
        Assertions.assertEquals(HttpStatus.ACCEPTED, transitService.validateTicket(ticketToValidate).first)
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, transitService.validateTicket(ticketToValidate).first)
        transitService.deleteTicketByTicketId(uuid)
    }


    @Test
    @WithMockCustomUser("Device1", "DEVICE")
    fun validateNotDailyTicketMultipleTimes() = runBlocking {
        val uuid = UUID.randomUUID()
        val jws = createJWSTicket(uuid.toString(), LocalDateTime.now().plusHours(1), LocalDateTime.now(), "MONTHLY", "A", "prova1")
        val ticketToValidate = TicketToValidateDTO(jws)
        Assertions.assertEquals(HttpStatus.ACCEPTED, transitService.validateTicket(ticketToValidate).first)
        Assertions.assertEquals(HttpStatus.ACCEPTED, transitService.validateTicket(ticketToValidate).first)
        transitService.deleteTicketByTicketId(uuid)
    }

    /** TRANSIT REPORT TEST */
    @Test
    @WithMockCustomUser("Admin", "Admin")
    fun getAllTransit() = runBlocking {
        Assertions.assertEquals(HttpStatus.OK, transitService.getAllTransit().first)
    }

    @Test
    @WithMockCustomUser("Admin", "Admin")
    fun getAllTransitByZone() = runBlocking {
        Assertions.assertEquals(HttpStatus.OK, transitService.getAllTransitByZone("A").first)
    }

    @Test
    @WithMockCustomUser("Admin", "Admin")
    fun getAllTransitByZoneWrong() = runBlocking {
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, transitService.getAllTransitByZone("-1").first)
    }

    @Test
    @WithMockCustomUser("Admin", "Admin")
    fun getAllTransitByTimePeriod() = runBlocking {
        val timeReport = TimeReportDTO("2022-10-18", "2022-10-20")
        Assertions.assertEquals(HttpStatus.OK, transitService.getAllTransitByTimePeriod(timeReport).first)
    }

    @Test
    @WithMockCustomUser("Admin", "Admin")
    fun getAllTransitByTimePeriodWrong() = runBlocking {
        val timeReport = TimeReportDTO("2022-10-18", "")
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, transitService.getAllTransitByTimePeriod(timeReport).first)
    }

    @Test
    @WithMockCustomUser("Admin", "Admin")
    fun getAllTransitByNicknameTimePeriod() = runBlocking {
        val timeReport = TimeReportDTO("2022-10-18", "2022-10-20")
        val nickname = "prova1"
        Assertions.assertEquals(HttpStatus.OK, transitService.getAllTransitByNicknameAndTimePeriod(nickname, timeReport).first)
    }

    @Test
    @WithMockCustomUser("Admin", "Admin")
    fun getAllTransitByEmptyNicknameTimePeriod() = runBlocking {
        val timeReport = TimeReportDTO("2022-10-18", "2022-10-20")
        val nickname = ""
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, transitService.getAllTransitByNicknameAndTimePeriod(nickname, timeReport).first)
    }
}
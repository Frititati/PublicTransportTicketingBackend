package it.polito.wa2.transit.services

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import io.jsonwebtoken.security.*
import it.polito.wa2.transit.dtos.*
import it.polito.wa2.transit.entities.TicketValidated
import it.polito.wa2.transit.repositories.TicketValidatedRepository
import kotlinx.coroutines.reactive.awaitLast
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import reactor.core.publisher.Flux
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.crypto.SecretKey

@Service
class TransitService {
    @Autowired
    lateinit var ticketValidatedRepository: TicketValidatedRepository

    @Value("\${application.ticketKey}")
    lateinit var ticketKey: String

    /**
     * @param ticket contains the jwt of the ticket that the device has to validate
     *
     * First of all, check if the jwt is correctly formatted and signed, then check if the zone of the ticket is the
     * same of the zone of the device, then if everything is correct, it validates it.
     *
     * @return HttpStatus 200 OK or 400 error
     *         information about the validated ticket like ticketId and validation date or null
     */
    suspend fun validateTicket(ticket: TicketToValidateDTO): Pair<HttpStatus, TicketValidatedDTO?> {
        if (ticket.jws.isEmpty()) return Pair(HttpStatus.BAD_REQUEST, null)
        else
            try {
                val generatedKey: SecretKey = Keys.hmacShaKeyFor(ticketKey.toByteArray(StandardCharsets.UTF_8))
                Jwts.parserBuilder().setSigningKey(generatedKey).build().parseClaimsJws(ticket.jws)

                // we know ticket is valid
                val zidTicket = Jwts.parserBuilder().setSigningKey(generatedKey).build()
                    .parseClaimsJws(ticket.jws).body["zid"].toString()
                val type = Jwts.parserBuilder().setSigningKey(generatedKey).build()
                    .parseClaimsJws(ticket.jws).body["type"].toString()
                val nickname = Jwts.parserBuilder().setSigningKey(generatedKey).build()
                    .parseClaimsJws(ticket.jws).body["nickname"].toString()
                val zidMachine = ReactiveSecurityContextHolder.getContext()
                    .map { obj: SecurityContext -> obj.authentication.principal as PrincipalUserDTO }.awaitLast().zone!!
                val ticketId = UUID.fromString(
                    Jwts.parserBuilder().setSigningKey(generatedKey).build()
                        .parseClaimsJws(ticket.jws).body.subject.toString()
                )

                //check match gate and ticket zone
                return if (zidTicket.contains(zidMachine)) {
                    // check if ticket already exists inside db
                    // if false the ticket is daily and it already exists in the DB, so it's no longer valid.
                    if (!type.equals("DAILY")){
                        val entity = TicketValidated(null, ticketId, LocalDateTime.now(), zidTicket, nickname)
                        ticketValidatedRepository.save(entity).awaitLast()

                        val dto: TicketValidatedDTO = entity.toDTO()
                        // allow passage
                        Pair(HttpStatus.ACCEPTED, dto)
                    } else if (type.equals("DAILY") && !ticketValidatedRepository.existsByTicketId(ticketId).awaitLast()) {
                        val entity = TicketValidated(null, ticketId, LocalDateTime.now(), zidTicket, nickname)
                        ticketValidatedRepository.save(entity).awaitLast()

                        val dto: TicketValidatedDTO = entity.toDTO()
                        // allow passage
                        Pair(HttpStatus.ACCEPTED, dto)
                    } else Pair(HttpStatus.BAD_REQUEST, null)

                } else Pair(HttpStatus.BAD_REQUEST, null)

            } catch (ex: SignatureException) {
                println("Invalid signature")
            } catch (ex: ExpiredJwtException) {
                println("JWT Expired")
            } catch (ex: MalformedJwtException) {
                println("Malformed JSON")
            } catch (ex: UnsupportedJwtException) {
                println("Unsupported token")
            } catch (ex: Exception) {
                println(ex.message)
            }

        return Pair(HttpStatus.BAD_REQUEST, null)
    }

    /**
     * @return HttpStatus 200 OK or 400 error
     *         List of all the validated tickets inside the db or null
     */
    suspend fun getAllTransit(): Pair<HttpStatus, Flux<TicketValidatedDTO>> {
        return try {
            Pair(HttpStatus.OK, ticketValidatedRepository.findAll().map { it.toDTO() })
        } catch (e: Exception) {
            Pair(HttpStatus.BAD_REQUEST, Flux.empty())
        }
    }

    /**
     * @param zid zone that the admin want to check
     *
     * @return HttpStatus 200 OK or 400 error
     *         List of all the validated tickets for a specific zone or null
     */
    suspend fun getAllTransitByZone(zid: String): Pair<HttpStatus, Flux<TicketValidatedDTO>> {
        return if (zid.matches("[a-zA-Z]+".toRegex())){
            try {
                Pair(HttpStatus.OK, ticketValidatedRepository.findTicketValidatedByZid(zid).map { it.toDTO() })
            } catch (e: Exception) {
                Pair(HttpStatus.BAD_REQUEST, Flux.empty())
            }
        } else Pair(HttpStatus.BAD_REQUEST, Flux.empty())
    }

    /**
     * @param timeReport contains initialDate and finalDate inserted by the user in the yyyy-MM-dd format
     *
     * @return HttpStatus 200 OK or 400 error
     *         List of all the validated tickets in a selected time period or null
     */
    suspend fun getAllTransitByTimePeriod(timeReport: TimeReportDTO): Pair<HttpStatus, Flux<TicketValidatedDTO>> {
        return try {
            val formatter = formatDate(timeReport)
            Pair(
                HttpStatus.OK,
                ticketValidatedRepository.findTicketValidatedByValidationDateGreaterThanEqualAndValidationDateLessThanEqual(
                    formatter.first,
                    formatter.second
                ).map { it.toDTO() })
        } catch (e: Exception) {
            Pair(HttpStatus.BAD_REQUEST, Flux.empty())
        }
    }

    /**
     * @param nickname username of the user for which the admin wants to check the validated tickets
     * @param timeReport contains initialDate and finalDate inserted by the user in the yyyy-MM-dd format
     *
     * @return HttpStatus 200 OK or 400 error
     *         List of all the validated tickets in a selected time period for a specific user or null
     */
    suspend fun getAllTransitByNicknameAndTimePeriod(
        nickname: String,
        timeReport: TimeReportDTO,
    ): Pair<HttpStatus, Flux<TicketValidatedDTO>> {
        return if (!nickname.isEmpty()){
            try {
                val formatter = formatDate(timeReport)
                Pair(
                    HttpStatus.OK,
                    ticketValidatedRepository.findTicketValidatedByValidationDateGreaterThanEqualAndValidationDateLessThanEqualAndNickname(
                        formatter.first,
                        formatter.second,
                        nickname
                    ).map { it.toDTO() })
            } catch (e: Exception) {
                Pair(HttpStatus.BAD_REQUEST, Flux.empty())
            }
        } else Pair(HttpStatus.BAD_REQUEST, Flux.empty())
    }


    /**
     * @param timeReport contains initialDate and finalDate inserted by the user in the yyyy-MM-dd format
     *
     * @return Pair with initialDate and finalDate in LocalDateTime format
     */
    suspend fun formatDate(timeReport: TimeReportDTO): Pair<LocalDateTime, LocalDateTime> {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val startDate = LocalDate.parse(timeReport.initialDate, dateFormatter)
        var startDateTime = LocalDateTime.of(startDate, LocalTime.of(0, 0))
        val endDate = LocalDate.parse(timeReport.finalDate, dateFormatter)
        var endDateTime = LocalDateTime.of(endDate, LocalTime.of(23, 59))
        if (startDateTime.isAfter(endDateTime)) {
            val a = startDateTime
            startDateTime = endDateTime
            endDateTime = a
        }
        return Pair(startDateTime, endDateTime)
    }


}
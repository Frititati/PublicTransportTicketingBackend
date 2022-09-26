package it.polito.wa2.transit.services

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import it.polito.wa2.transit.dtos.TicketToValidateDTO
import it.polito.wa2.transit.dtos.TicketValidatedDTO
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import io.jsonwebtoken.security.*
import it.polito.wa2.transit.dtos.toDTO
import it.polito.wa2.transit.entities.TicketValidated
import it.polito.wa2.transit.repositories.TicketValidatedRepository
import kotlinx.coroutines.reactive.awaitLast
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import reactor.core.publisher.Flux
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.util.*
import javax.crypto.SecretKey

@Service
class TransitService {
    @Autowired
    lateinit var ticketValidatedRepository: TicketValidatedRepository

    @Value("\${application.ticketKey}")
    lateinit var ticketKey: String

    suspend fun validateTicket(ticket: TicketToValidateDTO): Pair<HttpStatus, TicketValidatedDTO?> {
        // TODO add into security context the zone for the embedded device

        if (ticket.jws.isEmpty()) return Pair(HttpStatus.BAD_REQUEST, null)
        else
            try {
                val generatedKey: SecretKey = Keys.hmacShaKeyFor(ticketKey.toByteArray(StandardCharsets.UTF_8))
                Jwts.parserBuilder().setSigningKey(generatedKey).build().parseClaimsJws(ticket.jws)

                // we know ticket is valid

                val zidTicket = Jwts.parserBuilder().setSigningKey(generatedKey).build()
                    .parseClaimsJws(ticket.jws).body["zid"].toString()
                val zidMachine = ticket.zid
                val ticketId = UUID.fromString(Jwts.parserBuilder().setSigningKey(generatedKey).build()
                    .parseClaimsJws(ticket.jws).body.subject.toString())

                //check match gate and ticket zone
                return if (zidTicket.contains(zidMachine)) {
                    // check if ticket already exists inside db
                    // if false there is no ticket with the same UUID, so the ticket is never used
                    if(!ticketValidatedRepository.existsByTicketId(ticketId).awaitLast()){
                        // save in repo
                        val entity = TicketValidated(null, ticketId, LocalDateTime.now(), zidTicket)
                        ticketValidatedRepository.save(entity).awaitLast()
                        // TODO test await last

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
}
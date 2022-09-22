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
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.*

@Service
class TransitService {
    @Autowired
    lateinit var ticketValidatedRepository : TicketValidatedRepository

    @Value("\${application.ticketKey}")
    lateinit var ticketKey: String

    suspend fun validateTicket(ticket: TicketToValidateDTO) : Pair<HttpStatus, TicketValidatedDTO?>{
        // TODO add into security context the zone for the embedded device

        if(ticket.jws.isEmpty()) return Pair(HttpStatus.BAD_REQUEST, null)
        else
            try {
                Jwts.parserBuilder().setSigningKey(ticketKey).build().parseClaimsJws(ticket.jws)

                // we know ticket is valid

                val zidTicket = Jwts.parserBuilder().setSigningKey(ticketKey).build().parseClaimsJws(ticket.jws).body["zid"].toString()
                val zidMachine = ticket.zid
                val id = Jwts.parserBuilder().setSigningKey(ticketKey).build().parseClaimsJws(ticket.jws).body.subject.toString()

                //check match gate and ticket zone
                return if (zidTicket === zidMachine){
                    // save in repo
                    val entity = TicketValidated(UUID.fromString(id), LocalDateTime.now(), zidTicket)
                    ticketValidatedRepository.save(entity)

                    val dto: TicketValidatedDTO = entity.toDTO()

                    // allow passage
                    Pair(HttpStatus.ACCEPTED, dto)

                }else Pair(HttpStatus.BAD_REQUEST, null)

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
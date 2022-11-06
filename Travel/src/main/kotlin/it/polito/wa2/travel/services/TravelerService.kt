package it.polito.wa2.travel.services

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import it.polito.wa2.travel.dtos.*
import it.polito.wa2.travel.entities.TicketAddition
import it.polito.wa2.travel.entities.TicketPurchased
import it.polito.wa2.travel.entities.UserDetails
import it.polito.wa2.travel.entities.UserRegister
import it.polito.wa2.travel.repositories.TicketPurchasedRepository
import it.polito.wa2.travel.repositories.UserDetailsRepository
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import javax.crypto.SecretKey

@Service
class TravelerService {

    @Autowired
    lateinit var ticketPurchasedRepo: TicketPurchasedRepository

    @Autowired
    lateinit var userDetailsRepo: UserDetailsRepository

    @Value("\${application.ticketKey}")
    lateinit var ticketKey: String

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * @param username Name of the selected user
     *
     * @return HttpStatus 200 OK or 400 error
     *         user's info if everything is ok, otherwise null
     */
    suspend fun getUserProfile(username: String): Pair<HttpStatus, Mono<UserDetailsDTO>> {
        return try {
            if (userDetailsRepo.existsUserDetailsByUsername(username).awaitSingle()) {
                Pair(HttpStatus.OK, userDetailsRepo.findOneByUsername(username).map { it.toDTO() })
            } else {
                Pair(HttpStatus.BAD_REQUEST, Mono.empty())
            }
        } catch (e: Exception) {
            log.error(e.message)
            Pair(HttpStatus.BAD_REQUEST, Mono.empty())
        }
    }

    /**
     * @param userDetails {
     *                      name: String
     *                      address: String
     *                      dateOfBirth: String in the yyyy-MM-dd format
     *                      telephoneNumber: Long
     *                    }
     *
     * @return HttpStatus 202 ACCEPTED or 400 error
     */
    suspend fun userUpdate(userDetails: UserDetailsDTO): HttpStatus {
        val username = ReactiveSecurityContextHolder.getContext()
            .map { obj: SecurityContext -> obj.authentication.principal as String }.awaitLast()
        return try {
            if (userDetailsRepo.existsUserDetailsByUsername(username).awaitSingle()) {
                val user = userDetailsRepo.findOneByUsername(username).awaitSingle()
                user.address = userDetails.address

                val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val startDate = LocalDate.parse(userDetails.dateOfBirth, dateFormatter)
                val finalDate = LocalDateTime.of(startDate, LocalTime.of(0, 0))


                user.dateOfBirth = finalDate
                user.name = userDetails.name
                user.telephoneNumber = userDetails.telephoneNumber
                userDetailsRepo.save(user).awaitLast()

                HttpStatus.ACCEPTED
            } else {
                log.info("No user for that Username")
                HttpStatus.BAD_REQUEST
            }
        } catch (e: Exception) {
            log.error(e.message)
            HttpStatus.BAD_REQUEST
        }
    }

    /**
     * @param username Name of the selected user
     *
     * @return HttpStatus 200 OK or 400 error
     *         List of all tickets purchased by the selected user if everything is ok, otherwise null
     */
    suspend fun getUserTickets(username: String): Pair<HttpStatus, Flux<TicketPurchasedDTO>> {
        return try {
            if (userDetailsRepo.existsUserDetailsByUsername(username).awaitFirst()) {
                val user = userDetailsRepo.findOneByUsername(username).awaitSingle()
                val tickets = ticketPurchasedRepo.findAllByUserID(user.id!!).map { it.toDTO() }
                Pair(HttpStatus.OK, tickets)
            } else {
                Pair(HttpStatus.BAD_REQUEST, Flux.empty())
            }
        } catch (e: Exception) {
            log.error(e.message)
            Pair(HttpStatus.BAD_REQUEST, Flux.empty())
        }
    }

    /**
     * @param ticketAddition {
     *                          quantity: Int
     *                          zones: String
     *                          type: TicketType
     *                          username: String
     *                       }
     *
     * Take information of bought tickets from Kafka and save them with their JWT on the db
     *
     * @return true if everything works correctly, otherwise false
     */
    suspend fun processTicketAddition(ticketAddition: TicketAddition): Boolean {
        try {
            val user = userDetailsRepo.findOneByUsername(ticketAddition.username).awaitSingle()
            val generatedKey: SecretKey = Keys.hmacShaKeyFor(ticketKey.toByteArray(StandardCharsets.UTF_8))

            val tickets: MutableList<TicketPurchasedDTO> = mutableListOf()
            for (i in 1..ticketAddition.quantity) {
                val uuid = UUID.randomUUID()
                val expiresAt = ticketAddition.type.exp
                val issuedAt = LocalDateTime.now()

                val jwt = Jwts.builder().setSubject(uuid.toString())
                    .setExpiration(Date.from(expiresAt.atZone(ZoneId.systemDefault()).toInstant()))
                    .setIssuedAt(Date.from(issuedAt.atZone(ZoneId.systemDefault()).toInstant()))
                    .claim("type", ticketAddition.type)
                    .claim("zid", ticketAddition.zones)
                    .claim("username", ticketAddition.username).signWith(generatedKey).compact()

                val ticket = ticketPurchasedRepo.save(
                    TicketPurchased(
                        null, uuid, issuedAt, expiresAt, ticketAddition.zones, ticketAddition.type.name, jwt, user.id!!
                    )
                ).awaitLast()

                tickets.add(ticket.toDTO())
                return true
            }
        } catch (e: Exception) {
            log.error(e.message)
            return false
        }
        return false
    }


    /**
     * @return HttpStatus 200 OK or 400 error
     *         List of all customers' name if everything is ok, otherwise null
     */
    suspend fun getTravelers(): Pair<HttpStatus, Flux<UsernameDTO>> {
        return try {
            val users = userDetailsRepo.findAll()
            Pair(HttpStatus.OK, users.map { it.toUsernameDTO() })
        } catch (e: Exception) {
            Pair(HttpStatus.BAD_REQUEST, Flux.empty())
        }
    }

    /**
     * @param userRegister {
     *                        username: String
     *                     }
     *
     * Take username from Kafka when the user validate his profile on the RegistrationLogin service
     * and create a new empty profile for him
     *
     * @return true if everything works correctly, otherwise false
     */
    suspend fun processUserRegister(userRegister: UserRegister): Boolean {
        return try {
            userDetailsRepo.save(
                UserDetails(null, userRegister.username, null, null, null, null)
            ).awaitLast()
            true
        } catch (e: Exception) {
            println("Process User Register Exception ${e.message}")
            false
        }
    }
}

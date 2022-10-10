package it.polito.wa2.travel.services

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import it.polito.wa2.travel.controllers.TicketPurchase
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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneId
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

    suspend fun getUserByNickname(): Pair<HttpStatus, Mono<UserDetailsDTO>> {

        val nickname = ReactiveSecurityContextHolder.getContext()
            .map { obj: SecurityContext -> obj.authentication.principal as String }.awaitLast()
        return try {
            if (userDetailsRepo.existsUserDetailsByNickname(nickname).awaitSingle()) {
                Pair(HttpStatus.OK, userDetailsRepo.findOneByNickname(nickname).map { it.toDTO() })
            } else {
                Pair(HttpStatus.OK, userDetailsRepo.save(
                    UserDetails(
                        null, nickname, null, null, null, null
                    )
                ).map { it.toDTO() })
            }
        } catch (e: Exception) {
            Pair(HttpStatus.BAD_REQUEST, Mono.empty())
        }
    }

    suspend fun userUpdate(userDetails: UserDetailsDTO): HttpStatus {
        val nickname = ReactiveSecurityContextHolder.getContext()
            .map { obj: SecurityContext -> obj.authentication.principal as String }.awaitLast()
        return try {
            if (userDetailsRepo.existsUserDetailsByNickname(nickname).awaitSingle()) {
                val user = userDetailsRepo.findOneByNickname(nickname).awaitSingle()
                user.address = userDetails.address
                user.dateOfBirth = userDetails.dateOfBirth
                user.name = userDetails.name
                user.telephoneNumber = userDetails.telephoneNumber

                userDetailsRepo.save(user).awaitLast()
            } else {
                userDetailsRepo.save(
                    UserDetails(
                        null,
                        nickname,
                        userDetails.name,
                        userDetails.address,
                        userDetails.dateOfBirth,
                        userDetails.telephoneNumber
                    )
                ).awaitLast()
            }
            HttpStatus.ACCEPTED
        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST
        }
    }

    suspend fun getUserTickets(nickname: String): Pair<HttpStatus, Flux<TicketPurchasedDTO>> {
        // TODO check what happens if the user doesn't exist (fili)
        return try {
            if (!userDetailsRepo.existsUserDetailsByNickname(nickname).awaitFirst()) {
                return Pair(HttpStatus.BAD_REQUEST, Flux.empty())
            }
            val user = getUserDetailsEntity(nickname).awaitSingle()
            val tickets = ticketPurchasedRepo.findAllByUserID(user.id!!).map { it.toDTO() }
            Pair(HttpStatus.OK, tickets)
        } catch (e: Exception) {
            Pair(HttpStatus.BAD_REQUEST, Flux.empty())
        }
    }

//    suspend fun createTickets(nickname: String, quantity: Int, zone: String): List<TicketPurchasedDTO> {
//        val user = getUserDetailsEntity(nickname)
//
//        val generatedKey: SecretKey = Keys.hmacShaKeyFor(ticketKey.toByteArray(StandardCharsets.UTF_8))
//
//        val tickets: MutableList<TicketPurchasedDTO> = mutableListOf()
//
//        for (i in 1..quantity) {
//            val uuid: UUID = UUID.randomUUID()
//            val expiresAt = LocalDateTime.now().plusHours(1)
//            val issuedAt = LocalDateTime.now()
//            val zoneSet: Set<String> = zone.trim().split("").filter { it.isNotEmpty() }.toSet()
//
//            val jwt = Jwts.builder()
//                .setSubject(uuid.toString())
//                .setExpiration(Date.from(expiresAt.atZone(ZoneId.systemDefault()).toInstant()))
//                .setIssuedAt(Date.from(issuedAt.atZone(ZoneId.systemDefault()).toInstant()))
//                .claim("zid", zoneSet)
//                .signWith(generatedKey)
//                .compact()
//
//            val type = "DAILY"
//
//            val ticket = ticketPurchasedRepo.save(
//                TicketPurchased(
//                    uuid,
//                    issuedAt,
//                    expiresAt,
//                    zoneSet,
//                    type,
//                    jwt,
//                    user
//                )
//            )
//
//            tickets.add(ticket.toDTO())
//        }
//        return tickets
//    }

    suspend fun addTickets(
        quantity: Int,
        zones: String,
        type: String,
        exp: LocalDateTime?
    ): Pair<HttpStatus, List<TicketPurchasedDTO>> {
        val nickname = ReactiveSecurityContextHolder.getContext()
            .map { obj: SecurityContext -> obj.authentication.principal.toString() }.awaitLast()
        return try {
            val user = getUserDetailsEntity(nickname).awaitSingle()
            val generatedKey: SecretKey = Keys.hmacShaKeyFor(ticketKey.toByteArray(StandardCharsets.UTF_8))

            val tickets: MutableList<TicketPurchasedDTO> = mutableListOf()
            for (i in 1..quantity) {
                val uuid = UUID.randomUUID()
                val expiresAt = exp ?: LocalDateTime.now().plusHours(1)
                val issuedAt = LocalDateTime.now()

                val jwt = Jwts.builder().setSubject(uuid.toString())
                    .setExpiration(Date.from(expiresAt.atZone(ZoneId.systemDefault()).toInstant()))
                    .setIssuedAt(Date.from(issuedAt.atZone(ZoneId.systemDefault()).toInstant())).claim("type", type)
                    .claim("zid", zones)
                    .claim("nickname", nickname)
                    .signWith(generatedKey)
                    .compact()

                val ticket = ticketPurchasedRepo.save(
                    TicketPurchased(
                        null, uuid, issuedAt, expiresAt, zones, type, jwt, user.id!!
                    )
                ).awaitLast()

                tickets.add(ticket.toDTO())
            }
            Pair(HttpStatus.CREATED, tickets)
        } catch (e: Exception) {
            Pair(HttpStatus.BAD_REQUEST, emptyList())
        }
    }

    suspend fun processTicketAddition(ticketAddition: TicketAddition) {
        try {
            val user = getUserDetailsEntity(ticketAddition.userNickName).awaitSingle()
            val generatedKey: SecretKey = Keys.hmacShaKeyFor(ticketKey.toByteArray(StandardCharsets.UTF_8))

            val tickets: MutableList<TicketPurchasedDTO> = mutableListOf()
            for (i in 1..ticketAddition.quantity) {
                val uuid = UUID.randomUUID()
                // TODO this time is wrong (fili)
                val expiresAt = LocalDateTime.now().plusHours(1)
                val issuedAt = LocalDateTime.now()

                val jwt = Jwts.builder().setSubject(uuid.toString())
                    .setExpiration(Date.from(expiresAt.atZone(ZoneId.systemDefault()).toInstant()))
                    .setIssuedAt(Date.from(issuedAt.atZone(ZoneId.systemDefault()).toInstant()))
                    .claim("type", ticketAddition.type)
                    .claim("zid", ticketAddition.zones)
                    .claim("nickname", ticketAddition.userNickName)
                    .signWith(generatedKey)
                    .compact()

                val ticket = ticketPurchasedRepo.save(
                    TicketPurchased(
                        null, uuid, issuedAt, expiresAt, ticketAddition.zones, ticketAddition.type.name, jwt, user.id!!
                    )
                ).awaitLast()

                tickets.add(ticket.toDTO())
            }
        } catch (e: Exception) {
            println(e.message)
        }
    }


    suspend fun getTravelers(): Pair<HttpStatus, Flux<UserNicknameDTO>> {
        return try {
            val users = userDetailsRepo.findAll()
            Pair(HttpStatus.OK, users.map { it.toUserNicknameDTO() })
        } catch (e: Exception) {
            Pair(HttpStatus.BAD_REQUEST, Flux.empty())
        }
    }

    suspend fun getUserProfile(nickname: String): Pair<HttpStatus, Mono<UserDetailsDTO>> {
        return try {
            val user = userDetailsRepo.findOneByNickname(nickname)
            Pair(HttpStatus.OK, user.map { it.toDTO() })
        } catch (e: Exception) {
            Pair(HttpStatus.BAD_REQUEST, Mono.empty())
        }
    }

    private suspend fun getUserDetailsEntity(nickname: String): Mono<UserDetails> {
        return if (userDetailsRepo.existsUserDetailsByNickname(nickname).awaitFirst()) {
            userDetailsRepo.findOneByNickname(nickname)
        } else {
            Mono.empty()
        }
    }

    fun validateUserDetailsDTO(userDetailsDTO: UserDetailsDTO): Boolean {

        // check if null values
        return if (userDetailsDTO.name == null || userDetailsDTO.address == null || userDetailsDTO.dateOfBirth == null || userDetailsDTO.telephoneNumber == null) {
            false
        }

        // check if empty
        else if (userDetailsDTO.name.isEmpty() || userDetailsDTO.address.isEmpty()) {
            false
        }

        // check if phone number
        else if (userDetailsDTO.telephoneNumber.toString().length > 15 || 10 > userDetailsDTO.telephoneNumber.toString().length) {
            false
        }

        // check if date of birth before now
        else !userDetailsDTO.dateOfBirth.isAfter(LocalDateTime.now())
    }

    fun validatePurchaseTicket(ticketPurchase: TicketPurchase): Boolean {

        // check if empty
        return if (ticketPurchase.cmd.isEmpty() || ticketPurchase.zones.isEmpty() || ticketPurchase.cmd != "buy_tickets") {
            false
        }

        // check quantity
        else ticketPurchase.quantity >= 1
    }

    suspend fun processUserRegister(userRegister: UserRegister) {
        userDetailsRepo.save(
            UserDetails(null, userRegister.nickname, null, null, null, null)
        ).awaitLast()
    }
}

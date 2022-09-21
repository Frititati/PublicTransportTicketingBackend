package it.polito.wa2.travel.services

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import it.polito.wa2.travel.controllers.TicketPurchase
import it.polito.wa2.travel.dtos.*
import it.polito.wa2.travel.entities.TicketPurchased
import it.polito.wa2.travel.entities.UserDetails
import it.polito.wa2.travel.repositories.TicketPurchasedRepository
import it.polito.wa2.travel.repositories.UserDetailsRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
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

    fun getUserByNickname(nickname: String): UserDetailsDTO {
        return if (userDetailsRepo.existsUserDetailsByNickname(nickname)) {
            userDetailsRepo.findOneByNickname(nickname).toDTO()
        } else {
            userDetailsRepo.save(
                UserDetails(
                    null, nickname, null, null, null, null, mutableSetOf()
                )
            ).toDTO()
        }
    }

    fun userUpdate(nickname: String, userDetails: UserDetailsDTO) {
        if (userDetailsRepo.existsUserDetailsByNickname(nickname)) {
            val user = userDetailsRepo.findOneByNickname(nickname)
            user.address = userDetails.address
            user.dateOfBirth = userDetails.dateOfBirth
            user.name = userDetails.name
            user.telephoneNumber = userDetails.telephoneNumber

            userDetailsRepo.save(user)
        } else {
            userDetailsRepo.save(
                UserDetails(
                    null,
                    nickname,
                    userDetails.name,
                    userDetails.address,
                    userDetails.dateOfBirth,
                    userDetails.telephoneNumber,
                    mutableSetOf()
                )
            )
        }
    }

    fun getUserTickets(nickname: String): List<TicketPurchasedDTO> {
        val user = getUserDetailsEntity(nickname)
        return user.ticketPurchased.toList().map { it.toDTO() }

    }

    fun createTickets(nickname: String, quantity: Int, zone: String): List<TicketPurchasedDTO> {
        val user = getUserDetailsEntity(nickname)

        val generatedKey: SecretKey = Keys.hmacShaKeyFor(ticketKey.toByteArray(StandardCharsets.UTF_8))

        val tickets: MutableList<TicketPurchasedDTO> = mutableListOf()

        for (i in 1..quantity) {
            val uuid: UUID = UUID.randomUUID()
            val expiresAt = LocalDateTime.now().plusHours(1)
            val issuedAt = LocalDateTime.now()
            val zoneSet: Set<String> = zone.trim().split("").filter { it.isNotEmpty() }.toSet()

            val jwt = Jwts.builder()
                .setSubject(uuid.toString())
                .setExpiration(Date.from(expiresAt.atZone(ZoneId.systemDefault()).toInstant()))
                .setIssuedAt(Date.from(issuedAt.atZone(ZoneId.systemDefault()).toInstant()))
                .claim("zid", zoneSet)
                .signWith(generatedKey)
                .compact()

            val type = "DAILY"

            val ticket = ticketPurchasedRepo.save(
                TicketPurchased(
                    uuid,
                    issuedAt,
                    expiresAt,
                    zoneSet,
                    type,
                    jwt,
                    user
                )
            )

            tickets.add(ticket.toDTO())
        }
        return tickets
    }

    fun addTickets(nickname: String, quantity: Int, zone: String, type : String, exp : LocalDateTime?): List<TicketPurchasedDTO> {
        val user = getUserDetailsEntity(nickname)

        val generatedKey: SecretKey = Keys.hmacShaKeyFor(ticketKey.toByteArray(StandardCharsets.UTF_8))

        val tickets: MutableList<TicketPurchasedDTO> = mutableListOf()

        for (i in 1..quantity) {
            val uuid: UUID = UUID.randomUUID()
            val expiresAt = exp ?: LocalDateTime.now().plusHours(1)
            val issuedAt = LocalDateTime.now()
            val zoneSet: Set<String> = zone.trim().split("").filter { it.isNotEmpty() }.toSet()

            val jwt = Jwts.builder()
                .setSubject(uuid.toString())
                .setExpiration(Date.from(expiresAt.atZone(ZoneId.systemDefault()).toInstant()))
                .setIssuedAt(Date.from(issuedAt.atZone(ZoneId.systemDefault()).toInstant()))
                .claim("type", type)
                .claim("zid", zoneSet)
                .signWith(generatedKey)
                .compact()


            val ticket = ticketPurchasedRepo.save(
                TicketPurchased(
                    uuid,
                    issuedAt,
                    expiresAt,
                    zoneSet,
                    type,
                    jwt,
                    user
                )
            )

            tickets.add(ticket.toDTO())
        }
        return tickets
    }



    fun getTravelers(): List<String?> {
        val users = userDetailsRepo.findAll()
        return users.map { it.nickname }
    }

    fun getUserProfile(nickname: String): UserDetailsDTO {
        val user = userDetailsRepo.findOneByNickname(nickname)
        return user.toDTO()
    }

    fun getProfileTickets(nickname: String): List<TicketPurchasedDTO> {
        val profileTickets = ticketPurchasedRepo.findAllByUserDetails_Nickname(nickname)
        return profileTickets.map { it.toDTO() }
    }

    fun doesUserExist(nickname: String): Boolean {
        return userDetailsRepo.existsUserDetailsByNickname(nickname)
    }

    private fun getUserDetailsEntity(nickname: String): UserDetails {
        return if (userDetailsRepo.existsUserDetailsByNickname(nickname)) {
            userDetailsRepo.findOneByNickname(nickname)
        } else {
            userDetailsRepo.save(
                UserDetails(
                    null, nickname, null, null, null, null, mutableSetOf()
                )
            )
        }
    }

    fun validateUserDetailsDTO(userDetailsDTO: UserDetailsDTO): Boolean {

        // check if null values
        if (userDetailsDTO.name == null || userDetailsDTO.address == null || userDetailsDTO.dateOfBirth == null || userDetailsDTO.telephoneNumber == null) {
            return false
        }

        // check if empty
        if (userDetailsDTO.name.isEmpty() || userDetailsDTO.address.isEmpty()) {
            return false
        }

        // check if phone number
        if (userDetailsDTO.telephoneNumber.toString().length > 15 || 10 > userDetailsDTO.telephoneNumber.toString().length) {
            return false
        }

        // check if date of birth before now
        if (userDetailsDTO.dateOfBirth.isAfter(LocalDateTime.now())) {
            return false
        }

        return true
    }

    fun validatePurchaseTicket(ticketPurchase: TicketPurchase): Boolean {

        // check if empty
        if (ticketPurchase.cmd.isEmpty() || ticketPurchase.zones.isEmpty() || ticketPurchase.cmd != "buy_tickets") {
            return false
        }

        // check quantity
        if (ticketPurchase.quantity < 1) {
            return false
        }

        return true
    }
}

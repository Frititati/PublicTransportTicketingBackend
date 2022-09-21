package it.polito.wa2.ticketcatalogue.services

import com.fasterxml.jackson.databind.ObjectMapper
import it.polito.wa2.ticketcatalogue.dtos.*
import it.polito.wa2.ticketcatalogue.entities.AvailableTicket
import it.polito.wa2.ticketcatalogue.entities.TicketType
import it.polito.wa2.ticketcatalogue.repositories.AvailableTicketsRepository
import it.polito.wa2.ticketcatalogue.repositories.OrdersRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*
import java.util.stream.Collectors

@Service
class AdminService(
    @Value("\${application.travelerUri}")
    var travelerUri: String,
) {

    @Autowired
    lateinit var availableTicketsRepository: AvailableTicketsRepository

    @Autowired
    lateinit var ordersRepository: OrdersRepository

    @Value("\${application.tokenPrefix}")
    lateinit var prefixHeader: String


    private val webClient = WebClient.create(travelerUri)

    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun addTicket(ticket: AvailableTicketCreationDTO): Pair<HttpStatus, AvailableTicketDTO?> {

        if (checkFields(ticket)) {

            return try {
                Pair(
                    HttpStatus.OK, availableTicketsRepository.save(
                        AvailableTicket(
                            null,
                            ticket.price,
                            TicketType.valueOf(ticket.type),
                            ticket.minAge ?: 0,
                            ticket.maxAge ?: 99
                        )
                    ).map { it.toDTO() }.awaitLast()
                )
            } catch (e: Exception) {
                log.error("Exception: $e", e)
                Pair(HttpStatus.BAD_REQUEST, null)
            }
        } else return Pair(HttpStatus.BAD_REQUEST, null)
    }

    suspend fun retrieveAllOrders(): Pair<HttpStatus, Flux<OrderDTO>> {
        return try {
            Pair(HttpStatus.OK, ordersRepository.findAll().map { it.toDTO() })
        } catch (e: Exception) {
            log.error("Exception: $e", e)
            Pair(HttpStatus.BAD_REQUEST, Flux.empty())
        }
    }

    suspend fun usersWithOrders(): Pair<HttpStatus, List<UserOrdersDTO?>> {
        val jwt = ReactiveSecurityContextHolder.getContext()
            .map { obj: SecurityContext -> obj.authentication.principal as PrincipalUserDTO }.awaitLast().jwt!!

        return try {

            val orders = ordersRepository.findAll().collectList().awaitLast()


            val client = webClient.get()
                .uri("/admin/travelers")
                .header("Authorization", "$prefixHeader$jwt")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Array<Any>::class.java).log()


            val objects: Array<Any>? = withContext(Dispatchers.IO) {
                client.block()
            }

            val mapper = ObjectMapper()

            val result = Arrays.stream(objects)
                .map { mapper.convertValue(it, String::class.java) }
                .map {
                    val userOrders = orders.filter { user -> user.nickname == it }
                    return@map UserOrdersDTO(it, userOrders)
                }
                .collect(Collectors.toList())

            Pair(HttpStatus.OK, result)

        } catch (e: Exception) {
            Pair(HttpStatus.BAD_REQUEST, emptyList())
        }
    }

    suspend fun retrieveUserInfo(userId: String): Pair<HttpStatus, UserDetailsDTO?> {
        val jwt = ReactiveSecurityContextHolder.getContext()
            .map { obj: SecurityContext -> obj.authentication.principal as PrincipalUserDTO }.awaitLast().jwt!!


        return try {
            val client = webClient.get()
                .uri("/admin/traveler/{userId}/profile", userId)
                .header("Authorization", "$prefixHeader$jwt")
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono { response ->
                    if (response.statusCode() == HttpStatus.OK) {
                        response.bodyToMono(UserDetailsDTO::class.java)
                    } else {
                        println("error ${response.statusCode()}")
                        Mono.empty()
                    }
                }.awaitLast()
            Pair(HttpStatus.OK, client)
        } catch (e: Exception) {
            Pair(HttpStatus.BAD_REQUEST, null)
        }
    }

    suspend fun getUserOrders(userId: String): Pair<HttpStatus, Flux<OrderDTO>> {
        return try {
            Pair(HttpStatus.OK, ordersRepository.findAllByNickname(userId).map { it.toDTO() })
        } catch (e: Exception) {
            Pair(HttpStatus.BAD_REQUEST, Flux.empty())
        }

    }

    fun checkFields(ticket: AvailableTicketCreationDTO): Boolean {
        return when {
            ticket.minAge != null && ticket.minAge < 0L -> false
            ticket.minAge != null && ticket.minAge > 99L -> false
            ticket.maxAge != null && ticket.maxAge < 0L -> false
            ticket.maxAge != null && ticket.maxAge > 99L -> false
            ticket.type != TicketType.DAILY.toString() &&
                    ticket.type != TicketType.SINGLE.toString() &&
                    ticket.type != TicketType.MONTHLY.toString() &&
                    ticket.type != TicketType.WEEKLY.toString() &&
                    ticket.type != TicketType.YEARLY.toString()-> false
            else -> true

        }
    }
}
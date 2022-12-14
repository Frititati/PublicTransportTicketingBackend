package it.polito.wa2.ticketcatalogue.services

import com.fasterxml.jackson.databind.ObjectMapper
import it.polito.wa2.ticketcatalogue.dtos.*
import it.polito.wa2.ticketcatalogue.entities.AvailableTicket
import it.polito.wa2.ticketcatalogue.entities.TicketType
import it.polito.wa2.ticketcatalogue.repositories.AvailableTicketsRepository
import it.polito.wa2.ticketcatalogue.repositories.OrdersRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactive.awaitFirstOrNull
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.HashMap

@Service
class AdminService(
    @Value("\${application.travelerUri}") var travelerUri: String,
) {

    @Autowired
    lateinit var availableTicketsRepository: AvailableTicketsRepository

    @Autowired
    lateinit var ordersRepository: OrdersRepository

    @Value("\${application.tokenPrefix}")
    lateinit var prefixHeader: String


    private val webClient = WebClient.create(travelerUri)

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * @param ticket {
     *                  price: Double
     *                  type: String
     *                  minAge: Long
     *                  maxAge: Long
     *                  zones: String
     *               }
     *
     * @return HttpStatus 200 OK or 400 error
     *         ticket admin just created if everything is ok, otherwise null
     */
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
                            ticket.maxAge ?: 99,
                            ticket.zones
                        )
                    ).map { it.toDTO() }.awaitLast()
                )
            } catch (e: Exception) {
                log.error("Exception: $e", e)
                Pair(HttpStatus.BAD_REQUEST, null)
            }
        } else return Pair(HttpStatus.BAD_REQUEST, null)
    }

    /**
     * @return HttpStatus 200 OK or 400 error
     *         list of all orders if everything is ok, otherwise null
     */
    suspend fun retrieveAllOrders(): Pair<HttpStatus, Flux<OrderDTO>> {
        return try {
            Pair(HttpStatus.OK, ordersRepository.findAll().map { it.toDTO() })
        } catch (e: Exception) {
            log.error("Exception: $e", e)
            Pair(HttpStatus.BAD_REQUEST, Flux.empty())
        }
    }

    /**
     * @param timeReport {
     *                      initialDate: String in yyyy-MM-dd format
     *                      finalDate: String in yyyy-MM-dd format
     *                   }
     *
     * If timeReport is null, it returns all the orders saved in the db, otherwise filter them by the time period
     *
     * @return HttpStatus 200 OK or 400 error
     *         list of orders if everything is ok, otherwise null
     */
    suspend fun usersWithOrders(timeReport: TimeReportDTO?): Pair<HttpStatus, List<UserOrdersDTO>> {
        val jwt = ReactiveSecurityContextHolder.getContext()
            .map { obj: SecurityContext -> obj.authentication.principal as PrincipalUserDTO }.awaitFirstOrNull()?.jwt

        if (jwt == null) return Pair(HttpStatus.BAD_REQUEST, emptyList())
        else {
            return try {

                val orders = if (timeReport === null) ordersRepository.findAll().collectList().awaitFirstOrNull()
                else {
                    val formattedDate = formatDate(timeReport)
                    ordersRepository.findOrderByPurchaseDateGreaterThanEqualAndPurchaseDateLessThanEqual(
                        formattedDate.first, formattedDate.second
                    ).collectList().awaitLast()
                }
                val client = webClient.get().uri("/admin/travelers").header("Authorization", "$prefixHeader$jwt")
                    .accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(Array<Any>::class.java).log()


                val objects: Array<Any>? = withContext(Dispatchers.IO) {
                    client.block()
                }

                val mapper = ObjectMapper()

                val result = Arrays.stream(objects).map {
                    mapper.convertValue(it, HashMap::class.java)
                }.map {
                    val userOrders = orders?.filter { user -> user.username == it.values.first().toString() }
                    return@map userOrders?.let { it1 -> UserOrdersDTO(it.values.first().toString(), it1) }
                }.collect(Collectors.toList())

                Pair(HttpStatus.OK, result)

            } catch (e: Exception) {
                println("$e")
                Pair(HttpStatus.BAD_REQUEST, emptyList())
            }
        }
    }

    /**
     * @param userId username of the selected user
     *
     * @return HttpStatus 200 OK or 400 error
     *         information of the user profile if everything is ok, otherwise null
     */
    suspend fun retrieveUserInfo(userId: String): Pair<HttpStatus, UserDetailsDTO?> {
        val jwt = ReactiveSecurityContextHolder.getContext()
            .map { obj: SecurityContext -> obj.authentication.principal as PrincipalUserDTO }.awaitLast().jwt!!


        return try {
            val client = webClient.get().uri("/admin/traveler/{userId}/profile", userId)
                .header("Authorization", "$prefixHeader$jwt").accept(MediaType.APPLICATION_JSON)
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

    /**
     * @param userId username of the selected user
     * @param timeReport {
     *                      initialDate: String in yyyy-MM-dd format
     *                      finalDate: String in yyyy-MM-dd format
     *                   }
     *
     * If timeReport is null, it returns all the orders of the selected user, otherwise filter them also by the time period
     *
     * @return HttpStatus 200 OK or 400 error
     *         list of orders for the selected user if everything is ok, otherwise null
     */
    suspend fun getUserOrders(userId: String, timeReport: TimeReportDTO?): Pair<HttpStatus, Flux<OrderDTO>> {
        return try {
            val result = if (timeReport === null) ordersRepository.findAllByUsername(userId).map { it.toDTO() }
            else {
                val formattedDate = formatDate(timeReport)
                ordersRepository.findOrderByPurchaseDateGreaterThanEqualAndPurchaseDateLessThanEqualAndUsername(
                    formattedDate.first, formattedDate.second, userId
                ).map { it.toDTO() }
            }
            Pair(HttpStatus.OK, result)
        } catch (e: Exception) {
            Pair(HttpStatus.BAD_REQUEST, Flux.empty())
        }

    }

    /**
     * @param ticket {
     *                  price: Double
     *                  type: String
     *                  minAge: Long
     *                  maxAge: Long
     *                  zones: String
     *               }
     *
     * Check if all the data the admin passed when create a new ticket are ok, or if there is some problem in them
     *
     * @return true if everything is ok, otherwise fale
     */
    fun checkFields(ticket: AvailableTicketCreationDTO): Boolean {
        return when {
            ticket.minAge === null -> false
            ticket.maxAge === null -> false
            ticket.minAge < 0L -> false
            ticket.minAge > 99L -> false
            ticket.maxAge < 0L -> false
            ticket.maxAge > 99L -> false
            ticket.type != TicketType.DAILY.toString() && ticket.type != TicketType.SINGLE.toString() && ticket.type != TicketType.MONTHLY.toString() && ticket.type != TicketType.WEEKLY.toString() && ticket.type != TicketType.YEARLY.toString() -> false

            else -> true

        }
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
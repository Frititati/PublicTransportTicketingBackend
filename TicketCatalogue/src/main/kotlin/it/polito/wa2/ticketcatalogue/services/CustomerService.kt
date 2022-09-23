package it.polito.wa2.ticketcatalogue.services

import it.polito.wa2.ticketcatalogue.dtos.*
import it.polito.wa2.ticketcatalogue.entities.*
import it.polito.wa2.ticketcatalogue.repositories.AvailableTicketsRepository
import it.polito.wa2.ticketcatalogue.repositories.OrdersRepository
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.suspendCancellableCoroutine
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.stereotype.Service
import org.springframework.util.concurrent.ListenableFuture
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Service
class CustomerService(
    @Value("\${kafka.topics.purchaseOrder}") val topic: String,
    @Autowired
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    @Value("\${application.travelerUri}")
    var travelerUri: String
) {

    @Autowired
    lateinit var availableTicketsRepository: AvailableTicketsRepository

    @Autowired
    lateinit var ordersRepository: OrdersRepository

    @Value("\${application.tokenPrefix}")
    lateinit var prefixHeader: String

    private val webClient = WebClient.create(travelerUri)

    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun purchaseTicket(ticketId: Long, purchaseRequest: PurchaseRequestDTO): Pair<HttpStatus, OrderDTO?> {

        try {
            val ticket = availableTicketsRepository.findById(ticketId).awaitLast()

            val principalObj = ReactiveSecurityContextHolder.getContext()
                .map { obj: SecurityContext -> obj.authentication.principal as PrincipalUserDTO }.awaitLast()

            val jwt = principalObj.jwt!!

            // In this case the ticket doesn't need to check the age of the user
            if(ticket.minAge == 0L && ticket.maxAge == 99L) {
                return buyTickets(ticket, jwt, purchaseRequest, ticketId)

                // In this case it means that the ticket needs to check the age of the user
            } else if (ticket.minAge > 0L || ticket.maxAge < 99L) {

                val dateOfBirth = retrieveUserInfo(jwt)?.dateOfBirth

                // If dateOfBirth is null it means that there is an error when connecting with the other service
                // or that the user doesn't set it in his profile, so I have to return an error because I can't check it
                return if (dateOfBirth != null && ((ticket.minAge > 0 && LocalDateTime.now().minusYears(ticket.minAge).isAfter(dateOfBirth))
                    || (ticket.maxAge < 99 && LocalDateTime.now().minusYears(ticket.maxAge).isBefore(dateOfBirth)))
                ) {


                    // In this case the age of the user is ok to buy the ticket
                    buyTickets(ticket, jwt, purchaseRequest, ticketId)


                } else {
                    Pair(HttpStatus.BAD_REQUEST, null)
                }

            } else
                return Pair(HttpStatus.BAD_REQUEST, null)


        } catch (e: Exception) {
            log.error("Exception: $e", e)
            return Pair(HttpStatus.BAD_REQUEST, null)
        }

    }

    private suspend fun rollbackDb(purchase: Order) {
        try {
            purchase.status = PaymentStatus.REJECTED
            ordersRepository.save(purchase).awaitLast()
        } catch (e: Exception) {
            log.error("Exception: $e", e)
        }
    }

    suspend fun updateOrder(purchaseOutcome: PurchaseOutcome) {
        try {
            val order = ordersRepository.findById(purchaseOutcome.transactionId).awaitLast()

            order.status = purchaseOutcome.status

            ordersRepository.save(order).awaitLast()

        } catch (e: Exception) {
            log.error("Exception: $e", e)
        }
    }

    suspend fun getOrders(): Pair<HttpStatus, List<OrderDTO>?> {

        return try {
            val nickname = ReactiveSecurityContextHolder.getContext()
                .map { obj: SecurityContext -> obj.authentication.principal as PrincipalUserDTO }.awaitLast().nickname!!

            val orders = ordersRepository.findAllByNickname(nickname).map { it.toDTO() }.collectList().awaitLast()

            return Pair(HttpStatus.OK, orders)
        } catch (e: Exception) {
            log.error("Exception: $e", e)
            Pair(HttpStatus.BAD_REQUEST, null)
        }

    }

    suspend fun getSingleOrder(orderId: Long): Pair<HttpStatus, OrderDTO?> {
        return try {
            val order = ordersRepository.findById(orderId).awaitLast()

            val nickName = ReactiveSecurityContextHolder.getContext()
                .map { obj: SecurityContext -> obj.authentication.principal as PrincipalUserDTO }.awaitLast().nickname!!

            if (order.nickname == nickName)
                Pair(HttpStatus.OK, order.toDTO())
            else Pair(HttpStatus.UNAUTHORIZED, null)
        } catch (e: Exception) {
            log.error("Exception: $e", e)
            Pair(HttpStatus.BAD_REQUEST, null)
        }
    }


    suspend fun retrieveUserInfo(jwt: String): UserDetailsDTO? {


        return try {
            val client = webClient.get()
                    .uri("/my/profile")
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

            client
        } catch (e: Exception) {
            null
        }
    }

    suspend fun contactTicketService(ticket: AvailableTicket, jwt: String, purchaseRequest: PurchaseRequestDTO): List<TicketDTO>? {
        // TODO: make zone dynamic (not always ABC)
        val body = TicketPurchaseDTO("buy_tickets", purchaseRequest.numberOfTickets, "ABC", ticket.type, ticket.type.exp)

        return try {
            val client = webClient.post()
                .uri("/my/tickets")
                .header("Authorization", "$prefixHeader$jwt")
                .bodyValue(body)
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono { response ->
                    if (response.statusCode() == HttpStatus.CREATED) {
                        response.bodyToMono(mutableListOf<TicketDTO>()::class.java)
                    } else {
                        println("error ${response.statusCode()}")
                        Mono.empty()
                    }
                }.awaitLast()

            client


        } catch (e: Exception) {
            null
        }
    }

    suspend fun buyTickets(ticket : AvailableTicket, jwt : String, purchaseRequest: PurchaseRequestDTO, ticketId : Long) : Pair<HttpStatus, OrderDTO?> {
        val travelerTickets = contactTicketService(ticket, jwt, purchaseRequest)

        if (travelerTickets != null) {

            try {
                val ticketPrice = ticket.price * purchaseRequest.numberOfTickets

                val nickName = ReactiveSecurityContextHolder.getContext()
                    .map { obj: SecurityContext -> obj.authentication.principal as PrincipalUserDTO }
                    .awaitLast().nickname!!

                val orderRequest = ordersRepository.save(
                    Order(
                        null,
                        nickName,
                        purchaseRequest.numberOfTickets,
                        ticketId,
                        PaymentStatus.PENDING,
                        ticketPrice
                    )
                ).awaitLast()


                val purchaseOrder = PurchaseOrder(
                    nickName,
                    orderRequest.id!!,
                    ticketPrice,
                    purchaseRequest.creditCard,
                    purchaseRequest.expirationDate,
                    purchaseRequest.cvv,
                    purchaseRequest.cardHolder
                )

                try {
                    log.info("Sending message to Kafka {}", purchaseOrder)
                    val message: Message<PurchaseOrder> = MessageBuilder
                        .withPayload(purchaseOrder)
                        .setHeader(KafkaHeaders.TOPIC, topic)
                        .build()
                    kafkaTemplate.send(message).await()

                    log.info("Message sent with success")
                    return Pair(HttpStatus.OK, orderRequest.toDTO())
                } catch (e: Exception) {
                    log.error("Exception: $e", e)
                    rollbackDb(orderRequest)
                    //ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error to send message")
                    return Pair(HttpStatus.BAD_REQUEST, null)
                }

            } catch (e: Exception) {
                log.error("Exception: $e", e)
                return Pair(HttpStatus.BAD_REQUEST, null)
            }
        } else
            return Pair(HttpStatus.BAD_REQUEST, null)
    }

}

suspend fun <T> ListenableFuture<T>.await() {
    return suspendCancellableCoroutine {
        it.invokeOnCancellation {
            this.cancel(true)
        }
        this.addCallback({ _ ->
            it.resume(Unit, null)
        }, { a ->
            it.cancel(a)
        })
    }
}
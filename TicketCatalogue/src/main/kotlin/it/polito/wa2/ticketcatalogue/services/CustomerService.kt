package it.polito.wa2.ticketcatalogue.services

import it.polito.wa2.ticketcatalogue.dtos.*
import it.polito.wa2.ticketcatalogue.entities.*
import it.polito.wa2.ticketcatalogue.repositories.AvailableTicketsRepository
import it.polito.wa2.ticketcatalogue.repositories.OrdersRepository
import kotlinx.coroutines.reactive.awaitFirstOrNull
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
    @Value("\${kafka.topics.purchaseOrder}") val purchaseOrderTopic: String,
    @Value("\${kafka.topics.addTickets}") val addTicketTopic: String,
    @Autowired
    private val purchaseOrderKafkaTemplate: KafkaTemplate<String, PurchaseOrder>,
    @Autowired
    private val ticketAdditionKafkaTemplate: KafkaTemplate<String, TicketAddition>,
    @Value("\${application.travelerUri}")
    val travelerUri: String,
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
     * @param ticketId id of the ticket the user want to buy
     * @param purchaseRequest {
     *                          numberOfTickets: Int
     *                          creditCard: Long
     *                          expirationDate: String
     *                          cvv: Int
     *                          cardHolder: String
     *                        }
     *
     * First it checks all the data passed by the user, then if everything is correct save the transaction
     * inside the database and pass it through kafka to the Payment service that check the payment's data and
     * accepted or rejected the payment
     *
     * @return HttpStatus 200 OK or 400 error
     *         data of the order if everything is ok, otherwise null
     */
    suspend fun purchaseTicket(ticketId: Long, purchaseRequest: PurchaseRequestDTO): Pair<HttpStatus, OrderDTO?> {

        try {
            val ticket = availableTicketsRepository.findById(ticketId).awaitFirstOrNull()

            if (ticket?.ticketId == null) {
                return Pair(HttpStatus.BAD_REQUEST, null)
            } else {
                val principalObj = ReactiveSecurityContextHolder.getContext()
                    .map { obj: SecurityContext -> obj.authentication.principal as PrincipalUserDTO }.awaitLast()

                val userJWT = principalObj.jwt!!

                // In this case the ticket doesn't need to check the age of the user
                if (ticket.minAge == 0L && ticket.maxAge == 99L) {
                    return startPurchaseOrder(ticket, purchaseRequest, ticketId)

                    // In this case it means that the ticket needs to check the age of the user
                } else if (ticket.minAge > 0L || ticket.maxAge < 99L) {

                    val dateOfBirth = retrieveUserInfo(userJWT)?.dateOfBirth

                    // If dateOfBirth is null it means that there is an error when connecting with the other service
                    // or that the user doesn't set it in his profile, so I have to return an error because I can't check it
                    return if (dateOfBirth != null && ((ticket.minAge > 0 && LocalDateTime.now()
                            .minusYears(ticket.minAge)
                            .isAfter(dateOfBirth))
                                || (ticket.maxAge < 99 && LocalDateTime.now().minusYears(ticket.maxAge)
                            .isBefore(dateOfBirth)))
                    ) {


                        // In this case the age of the user is ok to buy the ticket
                        startPurchaseOrder(ticket, purchaseRequest, ticketId)


                    } else {
                        Pair(HttpStatus.BAD_REQUEST, null)
                    }

                } else
                    return Pair(HttpStatus.BAD_REQUEST, null)
            }


        } catch (e: Exception) {
            log.error("Exception: $e", e)
            return Pair(HttpStatus.BAD_REQUEST, null)
        }

    }

    /**
     * @param purchase {
     *                   id: Long
     *                   username: String
     *                   numberTickets: Int
     *                   ticketId: Long
     *                   status: PaymentStatus
     *                   price: Double
     *                   purchaseDate: LocalDateTime
     *                 }
     *
     * If there is some problem sending the order through Kafka, we rejected it
     * otherwise it remains pending forever
     */
    private suspend fun rollbackDb(purchase: Order) {
        // here we rollback the order just to make sure the db is clean
        try {
            purchase.status = PaymentStatus.REJECTED
            ordersRepository.save(purchase).awaitLast()
        } catch (e: Exception) {
            log.error("Exception: $e", e)
        }
    }


    /**
     * @return HttpStatus 200 OK or 400 error
     *         list of all orders if everything is ok, otherwise null
     */
    suspend fun getOrders(): Pair<HttpStatus, List<OrderDTO>?> {

        return try {
            val username = ReactiveSecurityContextHolder.getContext()
                .map { obj: SecurityContext -> obj.authentication.principal as PrincipalUserDTO }.awaitFirstOrNull()?.username
                ?: return Pair(HttpStatus.BAD_REQUEST, null)

            val orders = ordersRepository.findAllByUsername(username).map { it.toDTO() }.collectList().awaitFirstOrNull()

            return Pair(HttpStatus.OK, orders)
        } catch (e: Exception) {
            log.error("Exception: $e", e)
            Pair(HttpStatus.BAD_REQUEST, null)
        }

    }

    /**
     * @param orderId id of the selected order
     *
     * @return HttpStatus 200 OK or 400 error
     *         selected order if everything is ok, otherwise null
     */
    suspend fun getSingleOrder(orderId: Long): Pair<HttpStatus, OrderDTO?> {
        return try {
            val order = ordersRepository.findById(orderId).awaitFirstOrNull()

            val username = ReactiveSecurityContextHolder.getContext()
                .map { obj: SecurityContext -> obj.authentication.principal as PrincipalUserDTO }.awaitFirstOrNull()?.username
                ?: return Pair(HttpStatus.BAD_REQUEST, null)
            if (order?.username == username)
                Pair(HttpStatus.OK, order.toDTO())
            else Pair(HttpStatus.UNAUTHORIZED, null)
        } catch (e: Exception) {
            log.error("Exception: $e", e)
            Pair(HttpStatus.BAD_REQUEST, null)
        }
    }


    /**
     * @param jwt JWT of the user wants to retrieve its profile
     *
     * We contact the Travel microservice that contains the user's info and retrieve them
     *
     * @return profile of the user if everything is ok, otherwise null
     */
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

    /**
     * @param ticket {
     *                  ticketId: Long
     *                  price: Double
     *                  type: TicketType
     *                  minAge: Long
     *                  maxAge: Long
     *                  zones: String
     *               }
     * @param purchaseRequest {
     *                          numberOfTickets: Int
     *                          creditCard: Long
     *                          expirationDate: String
     *                          cvv: Int
     *                          cardHolder: String
     *                        }
     * @param ticketId id of the selected ticket
     *
     * We generated the order and send it through Kafka to the Payment microservice
     *
     * @return HttpStatus 200 OK or 400 error
     *         order's info if everything is ok, otherwise null
     */
    suspend fun startPurchaseOrder(
        ticket: AvailableTicket,
        purchaseRequest: PurchaseRequestDTO,
        ticketId: Long,
    ): Pair<HttpStatus, OrderDTO?> {
        // generate order
        // send purchaseRequest to Payment
        // payment decides the outcome of the Payment
        // when payment replies add (if outcome is positive) to Travel
        try {

            val totalPrice = ticket.price * purchaseRequest.numberOfTickets
            val userNickName = ReactiveSecurityContextHolder.getContext()
                .map { obj: SecurityContext -> obj.authentication.principal as PrincipalUserDTO }
                .awaitLast().username!!

            // save order details to db
            val generatedOrder = ordersRepository.save(
                Order(
                    null,
                    userNickName,
                    purchaseRequest.numberOfTickets,
                    ticketId,
                    PaymentStatus.PENDING,
                    totalPrice,
                    LocalDateTime.now()
                )
            ).awaitLast()

            // prepare object to send to Payment microservice
            val purchaseOrder = PurchaseOrder(
                userNickName,
                generatedOrder.id!!,
                totalPrice,
                purchaseRequest.creditCard,
                purchaseRequest.expirationDate,
                purchaseRequest.cvv,
                purchaseRequest.cardHolder
            )

            // send the purchaseOrder to the Payment Microservice
            return try {
                log.info("Sending message via Kafka {}", purchaseOrder)
                val message: Message<PurchaseOrder> = MessageBuilder
                    .withPayload(purchaseOrder)
                    .setHeader(KafkaHeaders.TOPIC, purchaseOrderTopic)
                    .build()
                purchaseOrderKafkaTemplate.send(message).await()

                log.info("Message sent with success")
                Pair(HttpStatus.OK, generatedOrder.toDTO())
            } catch (e: Exception) {
                log.error("Exception: $e", e)
                // rollback the db to make sure it's clean of the generatedOrder
                rollbackDb(generatedOrder)
                Pair(HttpStatus.BAD_REQUEST, null)
            }
        } catch (e: Exception) {
            log.error("Exception: $e", e)
            return Pair(HttpStatus.BAD_REQUEST, null)
        }
    }

    /**
     * @param purchaseOutcome {
     *                          transactionId: Long
     *                          status: PaymentStatus
     *                        }
     *
     * If the payment will be accepted, we send the ticket's info to the Travel microservice through Kafka
     */
    suspend fun processPurchaseOutcome(purchaseOutcome: PurchaseOutcome) {
        try {
            // initially we update the order
            val orderInfo = ordersRepository.findById(purchaseOutcome.transactionId).awaitFirstOrNull()
            if (orderInfo != null) {
                orderInfo.status = purchaseOutcome.status
                ordersRepository.save(orderInfo).awaitLast()

                if (purchaseOutcome.status == PaymentStatus.ACCEPTED) {
                    // then we ask Travel Microservice to generate the tickets
                    val ticketAvailable = availableTicketsRepository.findById(orderInfo.ticketId).awaitLast()
                    val ticketsToAdd = TicketAddition(
                        orderInfo.numberTickets,
                        ticketAvailable.zones,
                        ticketAvailable.type,
                        orderInfo.username
                    )
                    log.info("Sending message via Kafka {}", ticketsToAdd)
                    val message: Message<TicketAddition> = MessageBuilder
                        .withPayload(ticketsToAdd)
                        .setHeader(KafkaHeaders.TOPIC, addTicketTopic)
                        .build()
                    ticketAdditionKafkaTemplate.send(message).await()
                    log.info("Message sent with success")
                }
            }

        } catch (e: Exception) {
            log.error("Exception: $e", e)
        }
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
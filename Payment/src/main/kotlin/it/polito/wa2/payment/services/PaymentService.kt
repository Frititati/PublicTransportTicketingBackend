package it.polito.wa2.payment.services

import it.polito.wa2.payment.dtos.PrincipalUserDTO
import it.polito.wa2.payment.dtos.TransactionDTO
import it.polito.wa2.payment.dtos.toDTO
import it.polito.wa2.payment.entities.PaymentStatus
import it.polito.wa2.payment.entities.PurchaseOrder
import it.polito.wa2.payment.entities.PurchaseOutcome
import it.polito.wa2.payment.entities.Transaction
import it.polito.wa2.payment.repositories.TransactionsRepository
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.suspendCancellableCoroutine
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.stereotype.Service
import org.springframework.util.concurrent.ListenableFuture
import reactor.core.publisher.Flux

@Service
class PaymentService(
    @Value("\${kafka.topics.purchaseOutcome}") val topic: String,
    @Autowired
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {


    @Autowired
    lateinit var transactionsRepository: TransactionsRepository
    

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * @param purchaseOrder {
     *                          username: String
     *                          transactionId: Long
     *                          price: Double
     *                          creditCard: Long
     *                          expirationDate: String
     *                          cvv: Int
     *                          cardHolder: String
     *                      }
     *
     *  @return Randomly decide if the transaction will be ACCEPTED or REJECTED and then sends information back
     *  through Kafka to the TicketCatalogueService and save it on its database
     */
    suspend fun validatePurchase(purchaseOrder: PurchaseOrder) {
        try {
            val random = (0..1).random()
            val status = PaymentStatus.values()[random]

            val transaction = transactionsRepository.save(
                Transaction(
                    null,
                    purchaseOrder.transactionId,
                    purchaseOrder.price,
                    purchaseOrder.username,
                    status
                )
            ).awaitLast()

            val outcome = PurchaseOutcome(purchaseOrder.transactionId, status)

            try {
                log.info("Sending message to Kafka {}", transaction)
                val message: Message<PurchaseOutcome> = MessageBuilder
                    .withPayload(outcome)
                    .setHeader(KafkaHeaders.TOPIC, topic)
                    .build()
                kafkaTemplate.send(message).await()

                log.info("Message sent with success")
            } catch (e: Exception) {
                log.error("Exception: $e", e)
            }
        } catch (e: Exception) {
            log.error("Exception: $e", e)
        }
    }

    /**
     * It takes information about user through the jwt in the Authorization header and extract the username
     *
     * @return HttpStatus 200 OK or 400 error
     *         List of all the transactions made by the single user or null
     */
    suspend fun userTransactions() : Pair<HttpStatus, Flux<TransactionDTO>> {

        return try {
            val username = ReactiveSecurityContextHolder.getContext()
                .map { obj: SecurityContext -> obj.authentication.principal as PrincipalUserDTO }.awaitLast().username!!

            Pair(HttpStatus.OK, transactionsRepository.findAllByUsername(username).map { it.toDTO() })

        } catch(e: Exception) {
            log.error("Exception: $e", e)
            Pair(HttpStatus.BAD_REQUEST, Flux.empty())
        }
    }

    /**
     * @return HttpStatus 200 OK or 400 error
     *         List of all the transactions made by the all the users or null
     */
    suspend fun allTransactions() : Pair<HttpStatus, Flux<TransactionDTO>?> {
        return try {

            Pair(HttpStatus.OK, transactionsRepository.findAll().map { it.toDTO() })

        } catch (e: Exception) {
            log.error("Exception: $e", e)
            Pair(HttpStatus.BAD_REQUEST, Flux.empty())
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
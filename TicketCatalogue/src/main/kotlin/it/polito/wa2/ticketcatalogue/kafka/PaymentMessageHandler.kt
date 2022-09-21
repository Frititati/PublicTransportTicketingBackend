package it.polito.wa2.ticketcatalogue.kafka

import it.polito.wa2.ticketcatalogue.entities.PurchaseOrder
import it.polito.wa2.ticketcatalogue.entities.PurchaseOutcome
import it.polito.wa2.ticketcatalogue.services.CustomerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class PaymentMessageHandler(val customerService: CustomerService) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["\${kafka.topics.purchaseOutcome}"], groupId = "ppr")
    fun listenGroupFoo(consumerRecord: ConsumerRecord<Any, Any>, ack: Acknowledgment) {
        logger.info("Message received {}", consumerRecord)
        ack.acknowledge()

        CoroutineScope(Dispatchers.Default).launch {
            customerService.updateOrder(consumerRecord.value() as PurchaseOutcome)
        }

    }
}
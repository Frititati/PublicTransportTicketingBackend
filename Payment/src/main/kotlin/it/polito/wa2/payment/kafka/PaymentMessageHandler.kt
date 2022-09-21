package it.polito.wa2.payment.kafka

import it.polito.wa2.payment.entities.PurchaseOrder
import it.polito.wa2.payment.services.PaymentService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class PaymentMessageHandler(
    val paymentService: PaymentService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["\${kafka.topics.purchaseOrder}"], groupId = "ppr")
    fun purchaseOrderListener(consumerRecord: ConsumerRecord<Any, Any>, ack: Acknowledgment) {
        logger.info("Message received {}", consumerRecord)
        ack.acknowledge()

        CoroutineScope(Dispatchers.Default).launch {
            paymentService.validatePurchase(consumerRecord.value() as PurchaseOrder)
        }


    }
}
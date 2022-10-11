package it.polito.wa2.travel.kafka

import it.polito.wa2.travel.entities.TicketAddition
import it.polito.wa2.travel.services.TravelerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class TicketMessageHandler(val travelerService: TravelerService) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["\${kafka.topics.addTickets}"], groupId = "ppr", containerFactory = "kafkaListenerContainerFactory"
    )
    fun listenPurchaseOutcome(consumerRecord: ConsumerRecord<Any, Any>, ack: Acknowledgment) {
        logger.info("Message received ticket {}", consumerRecord)
        ack.acknowledge()

        CoroutineScope(Dispatchers.Default).launch {
            travelerService.processTicketAddition(consumerRecord.value() as TicketAddition)
        }
    }
}
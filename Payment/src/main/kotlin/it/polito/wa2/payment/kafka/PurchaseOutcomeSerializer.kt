package it.polito.wa2.payment.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import it.polito.wa2.payment.entities.PurchaseOutcome
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Serializer
import org.slf4j.LoggerFactory

class PurchaseOutcomeSerializer : Serializer<PurchaseOutcome> {
    private val objectMapper = ObjectMapper()
    private val log = LoggerFactory.getLogger(javaClass)

    override fun serialize(topic: String?, data: PurchaseOutcome?): ByteArray? {
        log.info("Serializing...")
        return objectMapper.writeValueAsBytes(
            data ?: throw SerializationException("Error when serializing PurchaseOrder to ByteArray[]")
        )
    }
    override fun close() {}
}
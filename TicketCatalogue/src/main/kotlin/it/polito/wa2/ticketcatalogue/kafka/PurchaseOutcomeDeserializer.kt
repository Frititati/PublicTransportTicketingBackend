package it.polito.wa2.ticketcatalogue.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.common.serialization.Deserializer
import it.polito.wa2.ticketcatalogue.entities.PurchaseOutcome
import org.apache.kafka.common.errors.SerializationException
import org.slf4j.LoggerFactory

class PurchaseOutcomeDeserializer : Deserializer<PurchaseOutcome> {
    private val objectMapper = ObjectMapper()
    private val log = LoggerFactory.getLogger(javaClass)

    override fun deserialize(topic: String?, data: ByteArray?): PurchaseOutcome? {
        log.info("Deserializing...")
        return objectMapper.readValue(
            String(
                data ?: throw SerializationException("Error when deserializing byte[] to PurchaseOutcome"), Charsets.UTF_8
            ), PurchaseOutcome::class.java
        )
    }

    override fun close() {}
}
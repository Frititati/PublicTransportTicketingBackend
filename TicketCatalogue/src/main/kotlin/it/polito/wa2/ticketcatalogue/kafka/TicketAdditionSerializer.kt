package it.polito.wa2.ticketcatalogue.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.common.serialization.Serializer
import it.polito.wa2.ticketcatalogue.entities.TicketAddition
import org.apache.kafka.common.errors.SerializationException
import org.slf4j.LoggerFactory

class TicketAdditionSerializer : Serializer<TicketAddition> {
    private val objectMapper = ObjectMapper()
    private val log = LoggerFactory.getLogger(javaClass)

    override fun serialize(topic: String?, data: TicketAddition?): ByteArray {
        log.info("Serializing...")
        return objectMapper.writeValueAsBytes(
            data ?: throw SerializationException("Error when serializing TicketAddition to ByteArray[]")
        )
    }

    override fun close() {}
}
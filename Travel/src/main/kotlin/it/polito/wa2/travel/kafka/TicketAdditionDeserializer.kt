package it.polito.wa2.travel.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import it.polito.wa2.travel.entities.TicketAddition
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Deserializer
import org.slf4j.LoggerFactory

class TicketAdditionDeserializer : Deserializer<TicketAddition> {
    private val objectMapper = ObjectMapper()
    private val log = LoggerFactory.getLogger(javaClass)

    override fun deserialize(topic: String?, data: ByteArray?): TicketAddition? {
        log.info("Deserializing... ticket")
        return objectMapper.readValue(
            String(
                data ?: throw SerializationException("Error when deserializing byte[] to TicketAddition"),
                Charsets.UTF_8
            ), TicketAddition::class.java
        )
    }

    override fun close() {}
}
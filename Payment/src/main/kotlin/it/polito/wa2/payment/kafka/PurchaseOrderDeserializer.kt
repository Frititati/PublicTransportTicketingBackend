package it.polito.wa2.payment.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import it.polito.wa2.payment.entities.PurchaseOrder
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Deserializer
import org.slf4j.LoggerFactory
import kotlin.text.Charsets.UTF_8


class PurchaseOrderDeserializer : Deserializer<PurchaseOrder> {
    private val objectMapper = ObjectMapper()
    private val log = LoggerFactory.getLogger(javaClass)

    override fun deserialize(topic: String?, data: ByteArray?): PurchaseOrder? {
        log.info("Deserializing...")
        return objectMapper.readValue(
            String(
                data ?: throw SerializationException("Error when deserializing byte[] to PurchaseOrder"), UTF_8
            ), PurchaseOrder::class.java
        )
    }

    override fun close() {}

}
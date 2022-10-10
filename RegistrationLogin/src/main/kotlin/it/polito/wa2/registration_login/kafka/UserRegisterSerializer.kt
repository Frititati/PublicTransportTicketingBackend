package it.polito.wa2.registration_login.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import it.polito.wa2.registration_login.entities.UserRegister
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Serializer
import org.slf4j.LoggerFactory

class UserRegisterSerializer : Serializer<UserRegister> {
    private val objectMapper = ObjectMapper()
    private val log = LoggerFactory.getLogger(javaClass)

    override fun serialize(topic: String?, data: UserRegister?): ByteArray? {
        log.info("Serializing...")
        return objectMapper.writeValueAsBytes(
            data ?: throw SerializationException("Error when serializing PurchaseOrder to ByteArray[]")
        )
    }

    override fun close() {}
}
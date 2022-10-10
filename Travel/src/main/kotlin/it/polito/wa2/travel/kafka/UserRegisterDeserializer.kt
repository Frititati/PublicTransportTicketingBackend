package it.polito.wa2.travel.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import it.polito.wa2.travel.entities.UserRegister
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Deserializer
import org.slf4j.LoggerFactory

class UserRegisterDeserializer : Deserializer<UserRegister> {
    private val objectMapper = ObjectMapper()
    private val log = LoggerFactory.getLogger(javaClass)

    override fun deserialize(topic: String?, data: ByteArray?): UserRegister? {
        log.info("Deserializing...")
        return objectMapper.readValue(
            String(
                data ?: throw SerializationException("Error when deserializing byte[] to UserRegister"), Charsets.UTF_8
            ), UserRegister::class.java
        )
    }

    override fun close() {}
}
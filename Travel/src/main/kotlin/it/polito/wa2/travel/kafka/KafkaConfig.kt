package it.polito.wa2.travel.kafka

import it.polito.wa2.travel.entities.TicketAddition
import it.polito.wa2.travel.entities.UserRegister
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties

@Configuration
class KafkaConfig(
    @Value("\${kafka.bootstrapAddress}") private val servers: String
) {

    @Bean
    fun consumerTicketAdditionFactory(): ConsumerFactory<String?, TicketAddition?> {
        val props: MutableMap<String, Any> = HashMap()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = servers
        props[ConsumerConfig.GROUP_ID_CONFIG] = "ppr"
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = TicketAdditionDeserializer::class.java
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        return DefaultKafkaConsumerFactory(props)
    }

    @Bean("kafkaListenerContainerFactory")
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, TicketAddition>? {
        val factory = ConcurrentKafkaListenerContainerFactory<String, TicketAddition>()
        factory.consumerFactory = consumerTicketAdditionFactory()
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        factory.containerProperties.isSyncCommits = true
        return factory
    }

    @Bean
    fun consumerUserRegisterFactory(): ConsumerFactory<String?, UserRegister?> {
        val props: MutableMap<String, Any> = HashMap()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = servers
        props[ConsumerConfig.GROUP_ID_CONFIG] = "ppr1"
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = UserRegisterDeserializer::class.java
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        return DefaultKafkaConsumerFactory(props)
    }

    @Bean("userKafkaListenerContainerFactory")
    fun userKafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, UserRegister>? {
        val factory = ConcurrentKafkaListenerContainerFactory<String, UserRegister>()
        factory.consumerFactory = consumerUserRegisterFactory()
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        factory.containerProperties.isSyncCommits = true
        return factory
    }
}
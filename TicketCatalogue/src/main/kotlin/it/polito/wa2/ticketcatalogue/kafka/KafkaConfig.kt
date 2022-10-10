package it.polito.wa2.ticketcatalogue.kafka

import it.polito.wa2.ticketcatalogue.entities.PurchaseOrder
import it.polito.wa2.ticketcatalogue.entities.PurchaseOutcome
import it.polito.wa2.ticketcatalogue.entities.TicketAddition
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.*
import org.springframework.kafka.listener.ContainerProperties

@Configuration
class KafkaConfig(
    @Value("\${kafka.bootstrapAddress}")
    private val servers: String,
    @Value("\${kafka.topics.purchaseOrder}")
    private val purchaseOrderTopic: String,
    @Value("\${kafka.topics.addTickets}")
    private val addTicketsTopic: String
) {

    @Bean
    fun kafkaAdmin(): KafkaAdmin {
        val configs: MutableMap<String, Any?> = HashMap()
        configs[AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG] = servers
        return KafkaAdmin(configs)
    }

    @Bean
    fun createPurchaseTopic(): NewTopic {
        return NewTopic(purchaseOrderTopic, 1, 1.toShort())
    }
    @Bean
    fun createTicketTopic(): NewTopic {
        return NewTopic(addTicketsTopic, 1, 1.toShort())
    }

    @Bean
    fun consumerPurchaseOutcomeFactory(): ConsumerFactory<String?, PurchaseOutcome?> {
        val props: MutableMap<String, Any> = HashMap()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = servers
        props[ConsumerConfig.GROUP_ID_CONFIG] = "ppr"
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = PurchaseOutcomeDeserializer::class.java
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        return DefaultKafkaConsumerFactory(props)
    }

    @Bean
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, PurchaseOutcome>? {
        val factory = ConcurrentKafkaListenerContainerFactory<String, PurchaseOutcome>()
        factory.consumerFactory = consumerPurchaseOutcomeFactory()
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        factory.containerProperties.isSyncCommits = true
        return factory
    }

    @Bean
    fun producerPurchaseOrderFactory(): ProducerFactory<String, PurchaseOrder> {
        val configProps: MutableMap<String, Any> = HashMap()
        configProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = servers
        configProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        configProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = PurchaseOrderSerializer::class.java
        return DefaultKafkaProducerFactory(configProps)
    }

    @Bean
    fun purchaseOrderTemplate(): KafkaTemplate<String, PurchaseOrder> {
        return KafkaTemplate(producerPurchaseOrderFactory())
    }

    @Bean
    fun producerTicketAdditionFactory(): ProducerFactory<String, TicketAddition> {
        val configProps: MutableMap<String, Any> = HashMap()
        configProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = servers
        configProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        configProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = TicketAdditionSerializer::class.java
        return DefaultKafkaProducerFactory(configProps)
    }

    @Bean
    fun ticketAdditionTemplate(): KafkaTemplate<String, TicketAddition> {
        return KafkaTemplate(producerTicketAdditionFactory())
    }
}
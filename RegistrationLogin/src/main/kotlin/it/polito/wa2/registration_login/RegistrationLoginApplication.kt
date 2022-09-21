package it.polito.wa2.registration_login

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSenderImpl
import java.util.*


@SpringBootApplication
class RegistrationLoginApplication {

    @Value("\${spring.mail.host}")
    lateinit var host : String

    @Value("\${spring.mail.port}")
    lateinit var port : String

    @Value("\${spring.mail.username}")
    lateinit var username : String

    @Value("\${spring.mail.password}")
    lateinit var password : String

    @Value("\${spring.mail.properties.mail.smtp.auth}")
    lateinit var authProperty : String

    @Value("\${spring.mail.properties.mail.smtp.starttls.enable}")
    lateinit var startTLSProperty : String

    @Value("\${spring.mail.properties.mail.debug}")
    lateinit var debugProperty : String

    @Bean
    fun getJavaMailSender() {
        val mailSender = JavaMailSenderImpl()
        mailSender.host = host
        mailSender.port = port.toInt()
        mailSender.username = username
        mailSender.password = password
        val props: Properties = mailSender.javaMailProperties
        props["mail.smtp.auth"] = authProperty.toBoolean()
        props["mail.smtp.starttls.enable"] = startTLSProperty.toBoolean()
        props["mail.debug"] = debugProperty.toBoolean()
    }

    @Bean
    fun getMailMessage() : SimpleMailMessage {
        return SimpleMailMessage()
    }
}

fun main(args: Array<String>) {
    runApplication<RegistrationLoginApplication>(*args)
}

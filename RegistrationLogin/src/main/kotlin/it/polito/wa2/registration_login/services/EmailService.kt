package it.polito.wa2.registration_login.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
class EmailService {

    @Autowired
    lateinit var emailSender : JavaMailSender

    @Autowired
    lateinit var message: SimpleMailMessage

    /**
     * @param email String: email of the user
     * @param activationCode Int: code of 6 digits randomly generated
     *
     * It sends an email to the user with the activation code to activate his account
     *
     * @return True if email is sent successfully, otherwise false
     */
    fun sendMessage(email: String, activationCode : Int) : Boolean {
        message.setSubject("Confirm your email address")
        message.setText("Your activation code is $activationCode")
        message.setTo(email)

        return try {
            emailSender.send(message)
            true
        } catch (e: Exception) {
            println(e)
            false
        }
    }
}
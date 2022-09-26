package it.polito.wa2.registration_login.dtos

import java.util.UUID
import it.polito.wa2.registration_login.entities.User
import it.polito.wa2.registration_login.entities.Activation
import java.time.LocalDateTime

data class ActivationDTO(
    val id: UUID?,
    val activationCode: Int,
    val deadline: LocalDateTime,
    var counter: Int,
    val user: User
)

fun Activation.toDTO(): ActivationDTO {
    return ActivationDTO(id, activationCode, deadline, counter, user)
}

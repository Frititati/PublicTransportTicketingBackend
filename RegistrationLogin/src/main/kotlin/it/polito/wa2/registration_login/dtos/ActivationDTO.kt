package it.polito.wa2.registration_login.dtos

import java.util.UUID
import it.polito.wa2.registration_login.entities.Activation
import java.time.LocalDateTime

data class ActivationDTO(
    var id: UUID?,
    val activationCode: Int,
    val deadline: LocalDateTime,
    var counter: Int,
    val userId: Long?
)

fun Activation.toDTO(): ActivationDTO {
    return ActivationDTO(id, activationCode, deadline, counter, userId)
}

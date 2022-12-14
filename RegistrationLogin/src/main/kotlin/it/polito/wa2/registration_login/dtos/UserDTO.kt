package it.polito.wa2.registration_login.dtos

import it.polito.wa2.registration_login.entities.User
import it.polito.wa2.registration_login.security.Role

data class UserDTO(
    var id: Long?,
    val username: String,
    val password: String,
    val email: String,

    val role: Role,

    val active: Boolean
)

fun User.toDTO(): UserDTO {
    return UserDTO(id, username, password, email, Role.values()[role], false)
}
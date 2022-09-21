package it.polito.wa2.registration_login.dtos

import it.polito.wa2.registration_login.entities.User
import it.polito.wa2.registration_login.security.Role

data class UserDTO(
    val id:Long?,
    val nickname: String,
    val password : String,
    val email : String,

    val role : Role,

    val active: Boolean
)

fun User.toDTO():UserDTO{
    return UserDTO(id, nickname, password, email, role,false)
}
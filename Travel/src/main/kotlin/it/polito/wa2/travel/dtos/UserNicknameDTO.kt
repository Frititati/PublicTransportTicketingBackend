package it.polito.wa2.travel.dtos

import it.polito.wa2.travel.entities.UserDetails

data class UserNicknameDTO(val nickname: String)

fun UserDetails.toUserNicknameDTO(): UserNicknameDTO{
    return UserNicknameDTO(nickname)
}
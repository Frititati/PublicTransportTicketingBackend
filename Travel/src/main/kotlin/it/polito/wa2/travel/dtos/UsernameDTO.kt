package it.polito.wa2.travel.dtos

import it.polito.wa2.travel.entities.UserDetails

data class UsernameDTO(val username: String)

fun UserDetails.toUsernameDTO(): UsernameDTO{
    return UsernameDTO(username)
}
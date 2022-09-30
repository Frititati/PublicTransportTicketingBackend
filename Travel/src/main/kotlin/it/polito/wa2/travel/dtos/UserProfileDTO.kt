package it.polito.wa2.travel.dtos

import it.polito.wa2.travel.entities.UserDetails
import java.time.LocalDateTime

data class UserProfileDTO(val name : String?, val address : String?, val dateOfBirth : LocalDateTime?, val telephoneNumber : Long?)

fun UserDetails.toUserProfileDTO(): UserProfileDTO{
    return UserProfileDTO(name, address, dateOfBirth, telephoneNumber)
}
package it.polito.wa2.travel.dtos

import it.polito.wa2.travel.entities.UserDetails
import java.time.LocalDateTime
import javax.validation.constraints.*

data class UserDetailsDTO(
    @get:NotEmpty
    @get:NotBlank
    @get:NotNull
    @get:Size(min = 1, max = 50)
    val name: String?,
    @get:NotEmpty
    @get:NotBlank
    @get:NotNull
    @get:Size(min = 1, max = 100)
    val address: String?,
    @get:NotNull
    @get:PastOrPresent
    val dateOfBirth: String?,
    @get:NotNull
    @get:Min(1000000000)
    @get:Max(999999999999999)
    val telephoneNumber: Long?
)

fun UserDetails.toDTO(): UserDetailsDTO {
    return UserDetailsDTO(name, address, dateToString(dateOfBirth), telephoneNumber)
}

fun dateToString(dateOfBirth: LocalDateTime?) : String? {
    return if (dateOfBirth == null) {
        null
    } else {
        "${dateOfBirth.year}-${dateOfBirth.monthValue}-${dateOfBirth.dayOfMonth}"
    }
}
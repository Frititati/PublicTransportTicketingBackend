package it.polito.wa2.registration_login.dtos

import java.util.*

data class RegistrationToValidateDTO(val provisional_id: UUID?, val email: String)

package it.polito.wa2.registration_login.entities

import it.polito.wa2.registration_login.security.Role
import javax.persistence.*

@Entity
@Table(name = "devices")
class Device(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    val id: Long?,
    val name: String,
    val password: String,
    val zone: String,
    val role: Role
    // TODO check if role is necessary
)

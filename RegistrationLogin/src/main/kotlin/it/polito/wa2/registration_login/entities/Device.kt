package it.polito.wa2.registration_login.entities

import it.polito.wa2.registration_login.security.Role
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("devices")
data class Device(
    @Id
    @Column("id")
    val id: Long?,
    @Column("name")
    val name: String,
    @Column("password")
    val password: String,
    @Column("zone")
    val zone: String,
    @Column("role")
    val role: Role
    // TODO check if role is necessary
)

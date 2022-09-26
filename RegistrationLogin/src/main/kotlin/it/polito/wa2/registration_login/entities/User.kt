package it.polito.wa2.registration_login.entities

import it.polito.wa2.registration_login.security.Role
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("users")
class User(
    @Id
    @Column("id")
    val id: Long?,
    @Column("nickname")
    val nickname: String,
    @Column("password")
    val password: String,
    @Column("email")
    val email: String,
    @Column("role")
    var role: Role,
    @Column("active")
    var active: Boolean,
)
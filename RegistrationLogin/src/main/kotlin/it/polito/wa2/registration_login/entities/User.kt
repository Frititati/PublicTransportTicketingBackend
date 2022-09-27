package it.polito.wa2.registration_login.entities

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("users")
data class User(
    @Id
    @Column("id")
    var id: Long?,
    @Column("nickname")
    val nickname: String,
    @Column("password")
    val password: String,
    @Column("email")
    val email: String,
    @Column("role")
    var role: Int,
    @Column("active")
    var active: Boolean,
)
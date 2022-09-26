package it.polito.wa2.registration_login.entities

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.UUID

@Table("activation")
class Activation(
    @Id
    @Column("id")
    val id: UUID?,
    @Column("activation_code")
    val activationCode: Int,
    @Column("deadline")
    var deadline: LocalDateTime,
    @Column("counter")
    var counter: Int,
    @Column("user_id")
    val userId: Long?
    //TODO: use @ReadingConverter on OneToOne relationship using
    // val user : User
)
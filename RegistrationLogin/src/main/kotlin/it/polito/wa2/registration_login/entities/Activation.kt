package it.polito.wa2.registration_login.entities

import org.hibernate.annotations.GenericGenerator
import java.time.LocalDateTime
import java.util.UUID
import javax.persistence.*

@Entity
class Activation(
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    val id: UUID?,
    val activationCode: Int,
    var deadline: LocalDateTime,
    var counter: Int,

    @OneToOne
    val user: User
)
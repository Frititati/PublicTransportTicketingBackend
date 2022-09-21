package it.polito.wa2.registration_login.entities

import it.polito.wa2.registration_login.security.Role
import javax.persistence.*

@Entity
@Table(name= "users")
class User(
    @Id
    @GeneratedValue(strategy= GenerationType.SEQUENCE)
    val id : Long?,
    val nickname : String,
    val password : String,
    val email : String,
    val role : Role,
    var active : Boolean,

    @OneToOne(mappedBy = "user", cascade = [CascadeType.ALL])
    var activation : Activation?
)
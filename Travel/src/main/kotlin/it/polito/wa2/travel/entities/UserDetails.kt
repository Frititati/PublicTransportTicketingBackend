package it.polito.wa2.travel.entities

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("user_details")
class UserDetails (
    @Id
    @Column("id")
    var id : Long?,
    @Column("username")
    val username: String,
    @Column("name")
    var name : String?,
    @Column("address")
    var address: String?,
    @Column("date_of_birth")
    var dateOfBirth : LocalDateTime?,
    @Column("telephone_number")
    var telephoneNumber : Long?
)
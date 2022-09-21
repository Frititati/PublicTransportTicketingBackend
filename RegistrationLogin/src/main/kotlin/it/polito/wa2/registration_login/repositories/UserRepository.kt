package it.polito.wa2.registration_login.repositories

import it.polito.wa2.registration_login.entities.User
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : CrudRepository <User, Long> {

    fun findByNickname(nickname: String) : User?
    fun findByEmail(email: String) : User?
}
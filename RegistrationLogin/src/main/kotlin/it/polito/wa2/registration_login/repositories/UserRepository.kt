package it.polito.wa2.registration_login.repositories

import it.polito.wa2.registration_login.entities.User
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Mono

interface UserRepository : R2dbcRepository<User, Long> {

    fun findByNickname(nickname: String) : Mono<User?>
    fun findByEmail(email: String) : Mono<User?>
}
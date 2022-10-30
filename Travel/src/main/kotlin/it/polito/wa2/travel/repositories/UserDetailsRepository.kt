package it.polito.wa2.travel.repositories

import it.polito.wa2.travel.entities.UserDetails
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

@Repository
interface UserDetailsRepository : R2dbcRepository<UserDetails, Long> {
    fun findOneByUsername(username: String): Mono<UserDetails>
    fun deleteAllByUsername(username: String): Mono<Void>
    fun existsUserDetailsByUsername(username: String): Mono<Boolean>
}
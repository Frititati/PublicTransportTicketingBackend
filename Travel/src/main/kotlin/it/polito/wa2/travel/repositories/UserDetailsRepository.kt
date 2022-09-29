package it.polito.wa2.travel.repositories

import it.polito.wa2.travel.entities.UserDetails
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

@Repository
interface UserDetailsRepository : R2dbcRepository<UserDetails, Long> {
    fun findOneByNickname(nickname: String): Mono<UserDetails>

    fun existsUserDetailsByNickname(nickname: String) : Mono<Boolean>
}
package it.polito.wa2.travel.repositories

import it.polito.wa2.travel.entities.UserDetails
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface UserDetailsRepository : CrudRepository<UserDetails, Long> {
    fun findOneByNickname(nickname: String): UserDetails

    fun existsUserDetailsByNickname(nickname: String) : Boolean
}
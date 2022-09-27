package it.polito.wa2.registration_login.services

import it.polito.wa2.registration_login.dtos.UpdatedUserDTO
import it.polito.wa2.registration_login.repositories.UserRepository
import it.polito.wa2.registration_login.security.Role
import kotlinx.coroutines.reactive.awaitLast
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class AdminService {

    @Autowired
    lateinit var userRepository: UserRepository

    suspend fun updateUser(nickname: String): Pair<HttpStatus, UpdatedUserDTO?> {
        val userToUpdate = userRepository.findByNickname(nickname).awaitLast()

        return if (userToUpdate != null) {


            if(!userToUpdate.active) {
                println("User is not active")
                Pair(HttpStatus.BAD_REQUEST, null)
            }

            userToUpdate.role = Role.ADMIN.ordinal
            try {

                userRepository.save(
                    userToUpdate
                ).awaitLast()

                Pair(HttpStatus.ACCEPTED, (UpdatedUserDTO(userToUpdate.email)))

            } catch (e: Exception) {
                Pair(HttpStatus.BAD_REQUEST, null)
            }

        } else Pair(HttpStatus.BAD_REQUEST, null)

    }
}
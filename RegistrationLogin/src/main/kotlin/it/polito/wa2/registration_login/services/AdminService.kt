package it.polito.wa2.registration_login.services

import it.polito.wa2.registration_login.dtos.UpdatedUserDTO
import it.polito.wa2.registration_login.repositories.UserRepository
import it.polito.wa2.registration_login.security.Role
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitLast
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class AdminService {

    @Autowired
    lateinit var userRepository: UserRepository

    /**
     * @param username String: name of the user
     *
     * If the user is active, it changes the role of the selected user from CUSTOMER to ADMIN
     *
     * @return HttpStatus 200 OK or 400 error
     *         email of the user if everything is ok, otherwise null
     */
    suspend fun updateUser(username: String): Pair<HttpStatus, UpdatedUserDTO?> {
        val userToUpdate = userRepository.findByUsername(username).awaitFirstOrNull()

        if (userToUpdate != null) {


            if(!userToUpdate.active) {
                println("User is not active")
                return Pair(HttpStatus.BAD_REQUEST, null)
            }

            userToUpdate.role = Role.ADMIN.ordinal
            return try {

                userRepository.save(
                    userToUpdate
                ).awaitLast()

                Pair(HttpStatus.ACCEPTED, (UpdatedUserDTO(userToUpdate.email)))

            } catch (e: Exception) {
                Pair(HttpStatus.BAD_REQUEST, null)
            }

        } else return Pair(HttpStatus.BAD_REQUEST, null)

    }
}
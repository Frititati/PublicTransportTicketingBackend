package it.polito.wa2.registration_login.services

import it.polito.wa2.registration_login.repositories.UserRepository
import it.polito.wa2.registration_login.security.Role
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class AdminService {

    @Autowired
    lateinit var userRepository: UserRepository

    fun updateUser(nickname: String): Pair<HttpStatus, String?> {
        val userToUpdate = userRepository.findByNickname(nickname)

        return if (userToUpdate != null) {

            if(!userToUpdate.active) {
                println("User is not active")
                Pair(HttpStatus.BAD_REQUEST, null)
            }

            userToUpdate.role = Role.ADMIN
            try {
                userRepository.save(
                    userToUpdate
                )
                Pair(HttpStatus.ACCEPTED, userToUpdate.email)
            } catch (e: Exception) {
                Pair(HttpStatus.BAD_REQUEST, null)
            }

        } else Pair(HttpStatus.BAD_REQUEST, null)

    }
}
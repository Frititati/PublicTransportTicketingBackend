package it.polito.wa2.registration_login.controllers

import it.polito.wa2.registration_login.dtos.UpdatedUserDTO
import it.polito.wa2.registration_login.services.AdminService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class AdminEndpoint (val adminService: AdminService){

    @PostMapping("/admin/{nickname}/update")
    suspend fun makeAdmin(@PathVariable nickname: String) : ResponseEntity<UpdatedUserDTO?> {

        val result = adminService.updateUser(nickname)

        return ResponseEntity(result.second, result.first)

    }
}
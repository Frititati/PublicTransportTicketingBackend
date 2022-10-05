package it.polito.wa2.registration_login.controllers

import it.polito.wa2.registration_login.dtos.UpdatedUserDTO
import it.polito.wa2.registration_login.services.AdminService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class AdminEndpoint (val adminService: AdminService){

    /**
     * @param username : String
     *
     * @return If username exist, it makes the user administrator changing its role
     */
    @PostMapping("/admin/{username}/update")
    suspend fun makeAdmin(@PathVariable username: String) : ResponseEntity<UpdatedUserDTO?> {

        val result = adminService.updateUser(username)

        return ResponseEntity(result.second, result.first)

    }
}
package it.polito.wa2.registration_login.controllers

import it.polito.wa2.registration_login.services.AdminService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class AdminEndpoint (val adminService: AdminService){

    @PostMapping("/admin/{nickname}/update")
    fun makeAdmin(@PathVariable nickname: String) : ResponseEntity<UpdatedUser> {

        val result = adminService.updateUser(nickname)

        return ResponseEntity(UpdatedUser(result.second), result.first)

    }
}

data class UpdatedUser(val email : String?)
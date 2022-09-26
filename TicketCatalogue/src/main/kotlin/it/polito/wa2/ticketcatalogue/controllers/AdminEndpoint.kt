package it.polito.wa2.ticketcatalogue.controllers

import it.polito.wa2.ticketcatalogue.dtos.*
import it.polito.wa2.ticketcatalogue.services.AdminService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
class AdminEndpoint(val adminService: AdminService) {

    /**
     * @param ticket : body of the request
     *                  {
     *                      "price": 7.0        // Double, not null
     *                      "type":  "DAILY"   // String not null, it can be SINGLE, DAILY, WEEKLY, MONTHLY, YEARLY
     *                      "minAge": 12      // Int, if not set it will be 0
     *                      "maxAge": 85     // Int, if not set wil will be 99
     *                  }
     */
    @PostMapping("/admin/tickets")
    suspend fun addTickets(@RequestBody ticket: AvailableTicketCreationDTO): ResponseEntity<AvailableTicketDTO?> {
        val result = adminService.addTicket(ticket)
        return ResponseEntity(result.second, result.first)
    }

    @GetMapping("/admin/orders")
    suspend fun allOrders(): ResponseEntity<Flux<OrderDTO>> {
        val result = adminService.retrieveAllOrders()
        return ResponseEntity(result.second, result.first)
    }

    /**
     * @param userId : id of the user you want to see
     */
    @GetMapping("/admin/users/{userId}")
    suspend fun getSpecificUser(@PathVariable userId: String): ResponseEntity<UserDetailsDTO?> {

        val result = adminService.retrieveUserInfo(userId)
        return ResponseEntity(result.second, result.first)
    }

    /**
     * Get list of users with their orders
     */
    @GetMapping("/admin/users")
    suspend fun getUsersWithOrders(): ResponseEntity<List<UserOrdersDTO?>> {
        val result = adminService.usersWithOrders(null)
        return ResponseEntity(result.second, result.first)
    }

    /**
     * @param userId : id of the user you want to see
     * Get orders of a specific user
     */
    @GetMapping("/admin/users/{userId}/orders")
    suspend fun getUserOrders(@PathVariable userId: String): ResponseEntity<Flux<OrderDTO>> {
        val result = adminService.getUserOrders(userId, null)
        return ResponseEntity(result.second, result.first)
    }

    /**
     * Get list of users with their orders on a selectable time period
     */
    @PostMapping("/admin/users")
    suspend fun getUsersWithOrdersTimePeriod(@RequestBody timeReport : TimeReportDTO) : ResponseEntity<List<UserOrdersDTO?>> {

        val result = adminService.usersWithOrders(timeReport)
        return ResponseEntity(result.second, result.first)
    }

    /**
     * @param userId : id of the user you want to see
     * Get orders of a specific user on a selectable time period
     */
    @PostMapping("/admin/users/{userId}/orders")
    suspend fun getUserOrdersTimePeriod(@PathVariable userId: String, @RequestBody timeReport: TimeReportDTO) : ResponseEntity<Flux<OrderDTO>> {
        val result = adminService.getUserOrders(userId, timeReport)
        return ResponseEntity(result.second, result.first)
    }
}


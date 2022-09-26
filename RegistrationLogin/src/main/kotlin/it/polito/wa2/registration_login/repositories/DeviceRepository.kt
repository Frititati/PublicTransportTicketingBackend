package it.polito.wa2.registration_login.repositories

import it.polito.wa2.registration_login.entities.Device
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Mono

interface DeviceRepository : R2dbcRepository<Device, Long> {

    fun findByName(name: String): Mono<Device?>
}
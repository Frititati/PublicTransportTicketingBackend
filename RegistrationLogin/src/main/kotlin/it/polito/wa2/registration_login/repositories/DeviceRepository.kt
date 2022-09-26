package it.polito.wa2.registration_login.repositories

import it.polito.wa2.registration_login.entities.Device
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface DeviceRepository : CrudRepository<Device, Long> {

    fun findByDevice(device: String): Device?
}
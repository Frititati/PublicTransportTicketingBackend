package it.polito.wa2.registration_login.dtos

import it.polito.wa2.registration_login.entities.Device

data class DeviceDTO(
    var id: Long?,
    val name: String,
    val password: String,
    val zone: String
)

fun Device.toDTO(): DeviceDTO {
    return DeviceDTO(id, name, password, zone)
}
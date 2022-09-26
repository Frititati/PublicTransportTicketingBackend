package it.polito.wa2.registration_login.repositories

import it.polito.wa2.registration_login.entities.Activation
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import java.util.UUID

interface ActivationRepository : R2dbcRepository<Activation, UUID> {

    @Query("select * from activation a where a.deadline < now()")
    fun findAllExpired() : Flux<List<Activation>>
}
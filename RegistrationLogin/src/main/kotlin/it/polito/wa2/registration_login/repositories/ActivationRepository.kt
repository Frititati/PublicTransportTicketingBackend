package it.polito.wa2.registration_login.repositories

import it.polito.wa2.registration_login.entities.Activation
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

@Repository
interface ActivationRepository : CrudRepository<Activation, UUID>{

    @Query(value = "select * from activation a where a.deadline < now()", nativeQuery = true)
    fun findAllExpired() : List<Activation>
}
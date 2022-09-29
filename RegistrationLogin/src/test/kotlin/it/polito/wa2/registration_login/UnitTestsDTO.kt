package it.polito.wa2.registration_login

import it.polito.wa2.registration_login.dtos.ActivationDTO
import it.polito.wa2.registration_login.dtos.UserDTO
import it.polito.wa2.registration_login.dtos.toDTO
import it.polito.wa2.registration_login.entities.Activation
import it.polito.wa2.registration_login.entities.User
import it.polito.wa2.registration_login.security.Role
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime
import java.util.*

@SpringBootTest
class UnitTestsDTO {
    @Test
    fun sameActivationTest() {
        val u = User(1, "TestMe", "Abc&1234", "testme@gmail.com", Role.CUSTOMER.ordinal,false)
        val a = ActivationDTO(UUID.fromString("eda6bff4-cc1e-46be-80fe-b5a59fcc75e3"), 1, LocalDateTime.of(2022, 5, 8, 10, 0, 0), 5, u.id)
        val a2 = Activation(UUID.fromString("eda6bff4-cc1e-46be-80fe-b5a59fcc75e3"), 1, LocalDateTime.of(2022, 5, 8, 10, 0, 0), 5, u.id)
        assert(a == a2.toDTO())
    }

    @Test
    fun differentActivationTest() {
        val u = User(1, "TestMe2", "Abc&1234", "testme2@gmail.com", Role.CUSTOMER.ordinal, false)
        val a = ActivationDTO(UUID.fromString("eda6bff4-cc1e-46be-80fe-b5a59fcc75e3"), 1, LocalDateTime.of(2022, 5, 8, 10, 0, 0), 5, u.id)
        val a2 = Activation(UUID.fromString("eda6bff4-cc1e-46be-80fe-b5a59fcc75e3"), 2, LocalDateTime.of(2022, 5, 8, 10, 0, 0), 5, u.id)
        assert(a != a2.toDTO())
    }
    @Test
    fun sameUserTest(){
        val u = UserDTO(1, "TestMe", "Abc&1234", "testme@gmail.com",Role.CUSTOMER,false)
        val u2 = User(1, "TestMe", "Abc&1234", "testme@gmail.com",Role.CUSTOMER.ordinal, false)
        assert (u == u2.toDTO())
    }
    @Test
    fun differentUserTest(){
        val u = UserDTO(1, "TestMe", "Abc&1234", "testme@gmail.com",Role.CUSTOMER, false)
        val u2 = User(2, "TestMe2", "Abc&1234", "testme2@gmail.com",Role.CUSTOMER.ordinal,false)
        assert (u != u2.toDTO())
    }
}
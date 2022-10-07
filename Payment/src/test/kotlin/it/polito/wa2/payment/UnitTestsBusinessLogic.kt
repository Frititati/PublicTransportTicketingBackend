package it.polito.wa2.payment

import it.polito.wa2.payment.dtos.PrincipalUserDTO
import it.polito.wa2.payment.services.PaymentService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithSecurityContext
import org.springframework.security.test.context.support.WithSecurityContextFactory
import org.springframework.test.context.ContextConfiguration


@SpringBootTest
@ContextConfiguration
class UnitTestsBusinessLogic {

    @Autowired
    lateinit var paymentService: PaymentService

    @Retention(AnnotationRetention.RUNTIME)
    @WithSecurityContext(factory = WithMockCustomUserSecurityContextFactory::class)
    annotation class WithMockCustomUser(val username: String, val role : String)

    class WithMockCustomUserSecurityContextFactory : WithSecurityContextFactory<WithMockCustomUser> {
        override fun createSecurityContext(customUser: WithMockCustomUser): SecurityContext {
            val context = SecurityContextHolder.createEmptyContext()
            val principal = PrincipalUserDTO(customUser.username, "")
            val auth: Authentication =
                UsernamePasswordAuthenticationToken(principal, null, mutableListOf(SimpleGrantedAuthority("ROLE_${customUser.role}")))
            context.authentication = auth
            return context
        }
    }

    @Test
    fun retrieveUserTransactionsWithoutLogin() = runBlocking {
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, paymentService.userTransactions().first)
    }

    @Test
    @WithMockCustomUser(username = "TestUser", role = "CUSTOMER")
    fun retrieveUserTransactionsCorrectly() = runBlocking {
        Assertions.assertEquals(HttpStatus.OK, paymentService.userTransactions().first)
    }

    @Test
    fun retrieveAdminTransactions() = runBlocking {
        Assertions.assertEquals(HttpStatus.OK, paymentService.allTransactions().first)
    }

}
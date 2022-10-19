package it.polito.wa2.transit

import it.polito.wa2.transit.dtos.PrincipalUserDTO
import it.polito.wa2.transit.services.TransitService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
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
class TransitUnitTest {
    @Autowired
    lateinit var transitService: TransitService

    @Retention(AnnotationRetention.RUNTIME)
    @WithSecurityContext(factory = WithMockCustomUserSecurityContextFactory::class)
    annotation class WithMockCustomUser(val username: String, val role : String)

    class WithMockCustomUserSecurityContextFactory : WithSecurityContextFactory<WithMockCustomUser> {
        override fun createSecurityContext(customUser: WithMockCustomUser): SecurityContext {
            val context = SecurityContextHolder.createEmptyContext()
            val principal = PrincipalUserDTO(customUser.username, "A", "")
            val auth: Authentication =
                UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    mutableListOf(SimpleGrantedAuthority("ROLE_${customUser.role}"))
                )
            context.authentication = auth
            return context
        }
    }
}
package it.polito.wa2.travel.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class JwtAuthenticationTokenFilter(
    authManager: AuthenticationManager,
    private val jwtUtils: JwtUtils
) : BasicAuthenticationFilter(authManager) {

    @Value("\${application.headerString}")
    lateinit var headerString: String

    @Value("\${application.tokenPrefix}")
    lateinit var tokenPrefix: String

    override fun doFilterInternal(
        req: HttpServletRequest,
        res: HttpServletResponse,
        chain: FilterChain
    ) {
        try {
            val jwt: String? = parseJwt(req)

            if ((jwt != null) && jwtUtils.validateJwt(jwt)) {

                jwtUtils.getDetailsJwt(jwt)?.also {
                    val role: ArrayList<GrantedAuthority?> = ArrayList()
                    role.add(SimpleGrantedAuthority("ROLE_${it.role}"))
                    val user = UsernamePasswordAuthenticationToken(it.username, null, role)
                    SecurityContextHolder.getContext().authentication = user
                }
            }
        } catch (e: Exception) {
            logger.error("Cannot set user authentication: {}", e)
        }

        chain.doFilter(req, res)
    }

    private fun parseJwt(request: HttpServletRequest): String? {
        val headerAuth = request.getHeader(headerString)
        return if (StringUtils.hasText(headerAuth) && headerAuth.startsWith(tokenPrefix)) {
            headerAuth.substring(7, headerAuth.length)
        } else null
    }
}
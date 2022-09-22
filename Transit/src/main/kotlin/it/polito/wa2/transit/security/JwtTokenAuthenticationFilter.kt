package it.polito.wa2.transit.security

import it.polito.wa2.transit.dtos.PrincipalUserDTO
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.util.StringUtils
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono


class JwtTokenAuthenticationFilter(private val tokenProvider : JwtUtils) : WebFilter {

    @Value("\${application.tokenPrefix}")
    lateinit var tokenPrefix: String

    @Value("\${application.headerString}")
    lateinit var headerString: String

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val token = exchange.request.headers.getFirst(headerString)?.let { parseJwt(it) }
        if (StringUtils.hasText(token) && tokenProvider.validateJwt(token) && !token.isNullOrEmpty()) {
            val user = tokenProvider.getDetailsJwt(token)
            val authentication = UsernamePasswordAuthenticationToken(
                PrincipalUserDTO(user?.username, token),
                null,
                mutableListOf(SimpleGrantedAuthority("ROLE_${user?.role}"))
            )
            return chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
        }
        return chain.filter(exchange)
    }


    private fun parseJwt(headerAuth: String): String? {

        return if (StringUtils.hasText(headerAuth) && headerAuth.startsWith(tokenPrefix)) {
            headerAuth.substring(7, headerAuth.length)
        } else null
    }

}
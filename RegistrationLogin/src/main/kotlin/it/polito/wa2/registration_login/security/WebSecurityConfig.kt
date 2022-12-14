package it.polito.wa2.registration_login.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
class WebSecurityConfig(private val jwtUtils: JwtUtils) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity) : SecurityWebFilterChain {

        return http.authorizeExchange()
            .pathMatchers("/device/register", "/admin/**").hasRole(Role.ADMIN.toString())
            .anyExchange().permitAll()
            .and()
            .addFilterAt(jwtAuthTokenFilter(), SecurityWebFiltersOrder.AUTHORIZATION)
            .csrf().disable()
            .build()
    }

    @Bean
    fun jwtAuthTokenFilter() : JwtTokenAuthenticationFilter {
        return JwtTokenAuthenticationFilter(jwtUtils)
    }

}
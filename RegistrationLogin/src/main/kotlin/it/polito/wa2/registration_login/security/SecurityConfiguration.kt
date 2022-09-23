package it.polito.wa2.registration_login.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
@EnableWebSecurity
class SecurityConfiguration(val jwtUtils: JwtUtils) : WebSecurityConfigurerAdapter() {

    override fun configure(web: WebSecurity) {
        super.configure(web)
    }

    override fun configure(auth: AuthenticationManagerBuilder) {
        super.configure(auth)
    }

    // TODO: /device/register 403 sempre
    override fun configure(http: HttpSecurity) {
        http.csrf().disable().authorizeRequests().anyRequest().permitAll()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
//PasswordEncoderFactories.createDelegatingPasswordEncoder()

    @Bean
    fun jwtAuthTokenFilter() : JwtAuthenticationTokenFilter {
        return JwtAuthenticationTokenFilter(authenticationManager(), jwtUtils)
    }

    @Bean
    override fun authenticationManager(): AuthenticationManager {
        return super.authenticationManager()
    }

}
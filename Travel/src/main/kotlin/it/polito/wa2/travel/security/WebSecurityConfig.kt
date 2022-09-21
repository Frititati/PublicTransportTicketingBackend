package it.polito.wa2.travel.security

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
class WebSecurityConfig(val jwtUtils: JwtUtils) : WebSecurityConfigurerAdapter() {

    override fun configure(web: WebSecurity) {
        super.configure(web)
    }

    override fun configure(auth: AuthenticationManagerBuilder) {
        super.configure(auth)
    }

    override fun configure(http: HttpSecurity) {
        http
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
            .mvcMatchers("/admin/**").hasRole(Role.ADMIN.toString())
            .mvcMatchers("/my/**").hasRole(Role.CUSTOMER.toString())
            .and()
            .addFilter(jwtAuthTokenFilter())
    }

    @Bean
    fun jwtAuthTokenFilter() : JwtAuthenticationTokenFilter {
        return JwtAuthenticationTokenFilter(authenticationManager(), jwtUtils)
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    override fun authenticationManager(): AuthenticationManager {
        return super.authenticationManager()
    }
}

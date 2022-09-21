package it.polito.wa2.registration_login.interceptor

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class BucketConfig : WebMvcConfigurer {

    @Autowired
    private lateinit var interceptor: RateLimiterInterceptor

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(interceptor)
            .addPathPatterns("/user/**")

    }

}

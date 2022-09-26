package it.polito.wa2.registration_login.interceptor

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.WebFluxConfigurer

@Configuration
class BucketConfig : WebFluxConfigurer {

    @Autowired
    private lateinit var interceptor: RateLimiterInterceptor

    //TODO: fix
    //fun addInterceptors(registry: InterceptorRegistry) {
    //    registry.addInterceptor(interceptor)
    //        .addPathPatterns("/user/**")
    //}


}

package it.polito.wa2.registration_login.interceptor

import io.github.bucket4j.*
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import java.time.Duration
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class RateLimiterInterceptor : HandlerInterceptorAdapter() {

    private final val limit : Bandwidth = Bandwidth.classic(10, Refill.greedy(10, Duration.ofSeconds(1)))
    val tokenBucket: Bucket = Bucket.builder().addLimit(limit).build()

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {

        val probe: ConsumptionProbe = tokenBucket.tryConsumeAndReturnRemaining(1)

        return if (probe.isConsumed) {
            response.addHeader("X-Rate-Limit-Remaining", probe.remainingTokens.toString())
            true
        } else {
            response.sendError(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "You have exhausted your API Request Quota"
            )
            false
        }
    }
}

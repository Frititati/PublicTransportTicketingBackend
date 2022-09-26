package it.polito.wa2.registration_login.interceptor

import io.github.bucket4j.*
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.time.Duration

@Component
class RateLimiterInterceptor : WebFilter {

    private final val limit : Bandwidth = Bandwidth.classic(10, Refill.greedy(10, Duration.ofSeconds(1)))
    val tokenBucket: Bucket = Bucket.builder().addLimit(limit).build()

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val probe: ConsumptionProbe = tokenBucket.tryConsumeAndReturnRemaining(1)

        return if (probe.isConsumed) {
            exchange.response.headers.add("X-Rate-Limit-Remaining", probe.remainingTokens.toString())
            chain.filter(exchange)
        } else {
            //TODO: check if work correctly
            exchange.response.statusCode = HttpStatus.TOO_MANY_REQUESTS
            chain.filter(exchange)
        }
    }
}

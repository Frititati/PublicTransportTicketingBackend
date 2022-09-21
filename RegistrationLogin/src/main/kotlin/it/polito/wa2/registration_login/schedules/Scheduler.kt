package it.polito.wa2.registration_login.schedules

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = ["scheduler.enabled"], matchIfMissing = true)
class Scheduler
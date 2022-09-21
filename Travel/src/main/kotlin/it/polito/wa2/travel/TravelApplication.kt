package it.polito.wa2.travel

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TravelApplication

fun main(args: Array<String>) {
    runApplication<TravelApplication>(*args)
}

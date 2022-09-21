package it.polito.wa2.ticketcatalogue.entities

import java.time.LocalDateTime

enum class TicketType (val exp : LocalDateTime) {
    SINGLE(LocalDateTime.now().plusHours(1)),
    DAILY(LocalDateTime.now().plusDays(1)),
    WEEKLY(LocalDateTime.now().plusWeeks(1)),
    MONTHLY(LocalDateTime.now().plusMonths(1)),
    YEARLY(LocalDateTime.now().plusYears(1))
}
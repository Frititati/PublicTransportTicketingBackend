package it.polito.wa2.ticketcatalogue.dtos

import javax.validation.constraints.*
data class PurchaseRequestDTO(
    @get:NotNull(message = "Number of tickets cannot be null")
    @get:Min(1, message = "You have to buy at least one ticket")
    val numberOfTickets: Int,
    @get:NotNull(message = "Credit card number cannot be null")
    @get:Min(10000000000000, message = "Credit card number needs at least 14 characters") // 14
    @get:Max(9999999999999999, message = "Credit card number needs at most 16 characters") // 16
    val creditCard: Long,
    @get:NotEmpty(message = "Expiration date cannot be null")
    @get:NotBlank(message = "Expiration date cannot be null")
    @get:NotNull(message = "Expiration date cannot be null")
    @get:Size(min = 7, max = 7, message = "Expiration date have 7 characters")
    @get:Pattern(regexp = """[0-1]\d-\d{4}""", message = "Expiration date have the following pattern: MM-YYYY")
    val expirationDate: String,
    @get:NotNull(message = "cvv cannot be null")
    @get:Min(100, message = "cvv have 3 characters")
    @get:Max(999, message = "cvv have 3 characters")
    val cvv: Int,
    @get:NotEmpty(message = "Card Holder cannot be null")
    @get:NotBlank(message = "Card Holder cannot be null")
    @get:NotNull(message = "Card Holder cannot be null")
    @get:Size(min = 1, max = 40, message = "Card holder have at least 1 character and at most 40 characters")
    val cardHolder: String
)
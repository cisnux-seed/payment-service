package id.co.bni.payment.domains.dtos

import java.math.BigDecimal
import java.time.ZonedDateTime

data class EWalletBalanceResponse(
    val provider: String,
    val balance: BigDecimal = BigDecimal("0.00"),
    val currency: String = "IDR",
    val accountNumber: String,
    val lastUpdated: ZonedDateTime
)
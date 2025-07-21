package id.co.bni.payment.domains.dtos

import java.math.BigDecimal

data class BalanceResponse(
    val balance: BigDecimal = BigDecimal("0.00"),
    val currency: String = "IDR",
)
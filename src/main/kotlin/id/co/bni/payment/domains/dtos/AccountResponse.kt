package id.co.bni.payment.domains.dtos

import id.co.bni.payment.commons.constants.AccountStatus
import java.math.BigDecimal
import java.time.LocalDateTime

data class AccountResponse(
    val id: String,
    val userId: Long,
    val balance: BigDecimal,
    val currency: String,
    val accountStatus: AccountStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
package id.co.bni.payment.domains.dtos

import id.co.bni.payment.commons.constants.PaymentMethod
import id.co.bni.payment.commons.constants.TransactionStatus
import id.co.bni.payment.commons.constants.TransactionType
import java.math.BigDecimal
import java.time.LocalDateTime

data class TransactionResponse(
    val id: String,
    val transactionId: String,
    val transactionType: TransactionType,
    val transactionStatus: TransactionStatus,
    val amount: BigDecimal,
    val balanceBefore: BigDecimal,
    val balanceAfter: BigDecimal,
    val currency: String,
    val paymentMethod: PaymentMethod? = null,
    val description: String? = null,
    val createdAt: LocalDateTime
)
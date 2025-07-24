package id.co.bni.payment.domains.entities

import id.co.bni.payment.commons.constants.PaymentMethod
import id.co.bni.payment.commons.constants.TransactionStatus
import id.co.bni.payment.commons.constants.TransactionType
import java.math.BigDecimal
import java.time.LocalDateTime

data class Transaction(
    val id: String,
    val userId: Long,
    val accountId: String,
    val transactionId: String,
    val transactionType: TransactionType,
    val transactionStatus: TransactionStatus,
    val amount: BigDecimal,
    val balanceBefore: BigDecimal,
    val balanceAfter: BigDecimal,
    val currency: String,
    val description: String? = null,
    val externalReference: String? = null,
    val paymentMethod: PaymentMethod? = null,
    val metadata: String? = null,
    val isAccessibleFromExternal: Boolean = listOf(true, false).random(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
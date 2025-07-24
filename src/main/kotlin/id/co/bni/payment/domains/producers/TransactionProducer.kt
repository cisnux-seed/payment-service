package id.co.bni.payment.domains.producers

import id.co.bni.payment.domains.dtos.TransactionResponse
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration

interface TransactionProducer {
    suspend fun publishTransactionEvent(transaction: TransactionResponse)
    suspend fun publishTransactionEventWithRetry(
        transaction: TransactionResponse,
        maxRetries: Int = 3,
        initialDelay: Duration = 100.milliseconds,
        maxDelay: Duration = 2.seconds,
        factor: Double = 2.0
    )
}
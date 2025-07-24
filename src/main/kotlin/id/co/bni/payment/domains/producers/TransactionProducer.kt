package id.co.bni.payment.domains.producers

import id.co.bni.payment.domains.entities.Transaction
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration

interface TransactionProducer {
    suspend fun publishTransactionEvent(transaction: Transaction)
    suspend fun publishTransactionEventWithRetry(
        transaction: Transaction,
        maxRetries: Int = 3,
        initialDelay: Duration = 100.milliseconds,
        maxDelay: Duration = 2.seconds,
        factor: Double = 2.0
    )
}
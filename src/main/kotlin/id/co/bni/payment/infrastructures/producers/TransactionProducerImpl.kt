package id.co.bni.payment.infrastructures.producers

import id.co.bni.payment.commons.loggable.Loggable
import id.co.bni.payment.domains.dtos.TransactionResponse
import id.co.bni.payment.domains.producers.TransactionProducer
import io.ktor.client.plugins.logging.MDCContext
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

@Component
class TransactionProducerImpl(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) : TransactionProducer, Loggable {
    @Value("\${app.kafka.transaction-topic:transaction-events}")
    private lateinit var transactionTopic: String

    @OptIn(InternalAPI::class)
    private val kafkaContext: CoroutineContext = Dispatchers.IO +
            SupervisorJob() +
            CoroutineName("KafkaProducer") +
            MDCContext()

    override suspend fun publishTransactionEvent(transaction: TransactionResponse): Unit =
        withContext(kafkaContext) {
            try {
                val record = createProducerRecord(transaction)

                val sendResult = kafkaTemplate.send(record).await()
                val metadata = sendResult.recordMetadata

                log.info(
                    "Published transaction event: ${transaction.id} " +
                            "to topic: ${metadata.topic()}, partition: ${metadata.partition()}, offset: ${metadata.offset()}"
                )
            } catch (exception: Exception) {
                log.error("Failed to publish transaction event: ${transaction.id}", exception)
                throw exception
            }
        }


    override suspend fun publishTransactionEventWithRetry(
        transaction: TransactionResponse,
        maxRetries: Int,
        initialDelay: Duration,
        maxDelay: Duration,
        factor: Double
    ) {
        var currentDelay = initialDelay
        var lastException: Exception? = null

        repeat(maxRetries + 1) { attempt ->
            try {
                publishTransactionEvent(transaction)
                return // Success, exit retry loop
            } catch (exception: Exception) {
                lastException = exception

                if (attempt < maxRetries) {
                    log.warn(
                        "Retrying transaction event publication for ${transaction.id}, " +
                                "attempt: ${attempt + 1}/${maxRetries + 1}, error: ${exception.message}"
                    )
                    delay(currentDelay)
                    currentDelay = minOf(currentDelay * factor, maxDelay)
                } else {
                    log.error("Failed to publish transaction event after $maxRetries retries: ${transaction.id}", exception)
                }
            }
        }

        throw lastException ?: RuntimeException("Unknown error during transaction event publication")
    }

    private fun createProducerRecord(transaction: TransactionResponse): ProducerRecord<String, Any> {
        val headers = mapOf(
            "event_type" to "transaction_completed",
            "event_version" to "1.0",
            "transaction_type" to transaction.transactionType.name,
            "payment_method" to transaction.paymentMethod?.name,
            "currency" to transaction.currency
        ).map { (key, value) ->
            RecordHeader(key, value?.toByteArray())
        }

        return ProducerRecord(
            transactionTopic,
            null,
            transaction.transactionId,
            transaction,
            headers
        )
    }
}
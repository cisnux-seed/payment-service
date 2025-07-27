package id.co.bni.payment.infrastructures.producers

import id.co.bni.payment.commons.constants.PaymentMethod
import id.co.bni.payment.commons.constants.TransactionStatus
import id.co.bni.payment.commons.constants.TransactionType
import id.co.bni.payment.domains.entities.Transaction
import id.co.bni.payment.domains.producers.TransactionProducer
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@ExtendWith(MockKExtension::class)
class TransactionProducerTest {

    private lateinit var transactionProducer: TransactionProducer
    @MockK
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>
    private val transactionTopic = "transaction-events"

    private val dummyTrx = Transaction(
        id = "trans-123",
        userId = 12345L,
        accountId = "acc-456",
        transactionId = "txn-456",
        transactionType = TransactionType.TOPUP,
        transactionStatus = TransactionStatus.SUCCESS,
        amount = BigDecimal("50000.00"),
        balanceBefore = BigDecimal("100000.00"),
        balanceAfter = BigDecimal("150000.00"),
        currency = "IDR",
        paymentMethod = PaymentMethod.GOPAY,
        description = "Top up via Gopay",
        externalReference = "ext-ref-123",
        metadata = """{"source": "mobile_app"}""",
        isAccessibleFromExternal = true,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    @BeforeEach
    fun setUp() {
        transactionProducer = TransactionProducerImpl(kafkaTemplate)
        ReflectionTestUtils.setField(transactionProducer, "transactionTopic", transactionTopic)
    }

    @Test
    fun `publishTransactionEvent with valid transaction should publish successfully`() = runTest {
        // arrange
        val mockSendResult = mockk<SendResult<String, Any>>()
        val mockRecordMetadata = mockk<RecordMetadata>()
        val mockFuture = mockk<CompletableFuture<SendResult<String, Any>>>()
        val producerRecordSlot = slot<ProducerRecord<String, Any>>()

        every { mockRecordMetadata.topic() } returns transactionTopic
        every { mockRecordMetadata.partition() } returns 0
        every { mockRecordMetadata.offset() } returns 12345L
        every { mockSendResult.recordMetadata } returns mockRecordMetadata
        every { mockFuture.get() } returns mockSendResult
        every { mockFuture.isDone } returns true
        every { mockFuture.toCompletableFuture() } returns mockFuture
        every { mockFuture.toCompletableFuture() } returns mockFuture
        every { mockFuture.toCompletableFuture() } returns mockFuture
        every { mockFuture.toCompletableFuture() } returns mockFuture
        every { kafkaTemplate.send(capture(producerRecordSlot)) } returns mockFuture

        // act
        transactionProducer.publishTransactionEvent(dummyTrx)

        // assert
        verify(exactly = 1) { kafkaTemplate.send(any<ProducerRecord<String, Any>>()) }

        val capturedRecord = producerRecordSlot.captured
        assertEquals(transactionTopic, capturedRecord.topic())
        assertEquals(dummyTrx.transactionId, capturedRecord.key())
        assertEquals(dummyTrx, capturedRecord.value())

        // Verify headers
        val headers = capturedRecord.headers().toList()
        assertTrue(headers.any { it.key() == "event_type" })
        assertTrue(headers.any { it.key() == "event_version" })
        assertTrue(headers.any { it.key() == "transaction_type" })
        assertTrue(headers.any { it.key() == "payment_method" })
        assertTrue(headers.any { it.key() == "currency" })
    }

    @Test
    fun `publishTransactionEvent with kafka failure should throw exception`() = runTest {
        // arrange
        val mockFuture = mockk<CompletableFuture<SendResult<String, Any>>>()
        val kafkaException = RuntimeException("Kafka connection failed")

        every { mockFuture.get() } throws ExecutionException(kafkaException)
        every { mockFuture.isDone } returns true
        every { mockFuture.toCompletableFuture() } returns mockFuture
        every { mockFuture.toCompletableFuture() } returns mockFuture
        every { mockFuture.toCompletableFuture() } returns mockFuture
        every { kafkaTemplate.send(any<ProducerRecord<String, Any>>()) } returns mockFuture

        // act & assert
        val exception = assertThrows(RuntimeException::class.java) {
            runBlocking {
                transactionProducer.publishTransactionEvent(dummyTrx)
            }
        }

        assertEquals("Kafka connection failed", exception.message)
        verify(exactly = 1) { kafkaTemplate.send(any<ProducerRecord<String, Any>>()) }
    }

    @Test
    fun `publishTransactionEventWithRetry with failure then success should succeed`() = runTest {
        // arrange
        val mockSendResult = mockk<SendResult<String, Any>>()
        val mockRecordMetadata = mockk<RecordMetadata>()
        val mockFuture1 = mockk<CompletableFuture<SendResult<String, Any>>>()
        val mockFuture2 = mockk<CompletableFuture<SendResult<String, Any>>>()
        val kafkaException = RuntimeException("Temporary failure")

        every { mockRecordMetadata.topic() } returns transactionTopic
        every { mockRecordMetadata.partition() } returns 0
        every { mockRecordMetadata.offset() } returns 12345L
        every { mockSendResult.recordMetadata } returns mockRecordMetadata

        every { mockFuture1.get() } throws ExecutionException(kafkaException)
        every { mockFuture1.isDone } returns true
        every { mockFuture1.toCompletableFuture() } returns mockFuture1
        every { mockFuture2.get() } returns mockSendResult
        every { mockFuture2.isDone } returns true
        every { mockFuture2.toCompletableFuture() } returns mockFuture2
        every { kafkaTemplate.send(any<ProducerRecord<String, Any>>()) } returnsMany listOf(mockFuture1, mockFuture2)

        // act
        transactionProducer.publishTransactionEventWithRetry(
            transaction = dummyTrx,
            maxRetries = 3,
            initialDelay = 10.milliseconds, // Short delay for test
            maxDelay = 1.seconds,
            factor = 2.0
        )

        // assert
        verify(exactly = 2) { kafkaTemplate.send(any<ProducerRecord<String, Any>>()) }
    }
}
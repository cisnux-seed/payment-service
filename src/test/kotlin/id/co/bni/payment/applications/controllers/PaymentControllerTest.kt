package id.co.bni.payment.applications.controllers

import com.ninjasquad.springmockk.MockkBean
import id.co.bni.payment.applications.resolvers.SubjectArgumentResolver
import id.co.bni.payment.commons.constants.AccountStatus
import id.co.bni.payment.commons.constants.PaymentMethod
import id.co.bni.payment.commons.constants.TransactionStatus
import id.co.bni.payment.commons.constants.TransactionType
import id.co.bni.payment.domains.dtos.AccountResponse
import id.co.bni.payment.domains.dtos.BalanceResponse
import id.co.bni.payment.domains.dtos.EWalletBalanceResponse
import id.co.bni.payment.domains.dtos.TransactionResponse
import id.co.bni.payment.domains.services.AccountService
import id.co.bni.payment.domains.services.PaymentService
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZonedDateTime

@TestConfiguration
class TestConfig {
    @Bean
    fun subjectArgumentResolver(): SubjectArgumentResolver {
        return SubjectArgumentResolver()
    }
}

@WebFluxTest(controllers = [PaymentController::class])
@Import(TestConfig::class)
@ActiveProfiles("test")
@TestPropertySource(properties = [
    "spring.r2dbc.url=r2dbc:h2:mem:///testdb",
    "spring.r2dbc.username=sa",
    "spring.r2dbc.password=",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379",
    "spring.kafka.bootstrap-servers=localhost:9092",
    "app.kafka.transaction-topic=test-transaction-events",
    "server.port=8080"
])
class PaymentControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var accountService: AccountService

    @MockkBean
    private lateinit var paymentService: PaymentService

    // Dummy data
    private val dummyUsername = "johndoe"
    private val dummyUserId = 12345L
    private val dummyAccountId = "acc-123"
    private val dummyBalance = BigDecimal("150000.00")
    private val dummyCurrency = "IDR"
    private val dummyWalletType = PaymentMethod.GOPAY.value
    private val dummyTopUpAmount = BigDecimal("50000.00")
    private val dummyDescription = "Top up for shopping"

    private val dummyAccountResponse = AccountResponse(
        id = dummyAccountId,
        userId = dummyUserId,
        balance = dummyBalance,
        currency = dummyCurrency,
        accountStatus = AccountStatus.ACTIVE,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    private val dummyBalanceResponse = BalanceResponse(
        balance = dummyBalance,
        currency = dummyCurrency
    )

    private val dummyEWalletBalanceResponse = EWalletBalanceResponse(
        provider = dummyWalletType,
        balance = BigDecimal("75000.00"),
        currency = dummyCurrency,
        accountNumber = dummyAccountId,
        lastUpdated = ZonedDateTime.now()
    )

    private val dummyTransactionResponse = TransactionResponse(
        id = "txn-123",
        transactionId = "TRX-123",
        transactionType = TransactionType.TOPUP,
        transactionStatus = TransactionStatus.SUCCESS,
        amount = dummyTopUpAmount,
        balanceBefore = dummyBalance,
        balanceAfter = dummyBalance - dummyTopUpAmount,
        currency = dummyCurrency,
        paymentMethod = PaymentMethod.GOPAY,
        description = dummyDescription,
        createdAt = LocalDateTime.now()
    )

    // Helper method to create JWT token for testing
    private fun createTestJwtToken(username: String): String {
        val header = """{"alg":"HS256","typ":"JWT"}"""
        val payload = """{"sub":"$username","iat":1516239022,"exp":1999999999}"""

        val encodedHeader = java.util.Base64.getUrlEncoder().encodeToString(header.toByteArray())
        val encodedPayload = java.util.Base64.getUrlEncoder().encodeToString(payload.toByteArray())
        val signature = "test-signature"

        return "$encodedHeader.$encodedPayload.$signature"
    }

    @Test
    fun `getAccount should return account successfully`() = runTest {
        // arrange
        coEvery { accountService.getAccountByUsername(dummyUsername) } returns dummyAccountResponse

        // act & assert
        webTestClient
            .get()
            .uri("/api/payment/account")
            .header("Authorization", "Bearer ${createTestJwtToken(dummyUsername)}")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.meta.code").isEqualTo("200")
            .jsonPath("$.meta.message").isEqualTo("account retrieved successfully")
            .jsonPath("$.data.id").isEqualTo(dummyAccountId)
            .jsonPath("$.data.user_id").isEqualTo(dummyUserId)
            .jsonPath("$.data.balance").isEqualTo(dummyBalance.toDouble())
            .jsonPath("$.data.currency").isEqualTo(dummyCurrency)
            .jsonPath("$.data.account_status").isEqualTo(AccountStatus.ACTIVE.name)

        coVerify(exactly = 1) { accountService.getAccountByUsername(dummyUsername) }
    }

    @Test
    fun `getAccount should throw NotFoundResourceException when account not found`() = runTest {
        // arrange
        val dummyNonExistentUsername = "nonexistent"
        coEvery { accountService.getAccountByUsername(dummyNonExistentUsername) } returns null

        // act & assert
        webTestClient
            .get()
            .uri("/api/payment/account")
            .header("Authorization", "Bearer ${createTestJwtToken(dummyNonExistentUsername)}")
            .exchange()
            .expectStatus().isNotFound
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.meta.code").isEqualTo("404")
            .jsonPath("$.meta.message").isEqualTo("account not found for user: $dummyNonExistentUsername")

        coVerify(exactly = 1) { accountService.getAccountByUsername(dummyNonExistentUsername) }
    }

    @Test
    fun `getBalance should return balance successfully`() = runTest {
        // arrange
        coEvery { accountService.getBalanceByUsername(dummyUsername) } returns dummyBalanceResponse

        // act & assert
        webTestClient
            .get()
            .uri("/api/payment/balance")
            .header("Authorization", "Bearer ${createTestJwtToken(dummyUsername)}")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.meta.code").isEqualTo("200")
            .jsonPath("$.meta.message").isEqualTo("balance retrieved successfully")
            .jsonPath("$.data.balance").isEqualTo(dummyBalance.toDouble())
            .jsonPath("$.data.currency").isEqualTo(dummyCurrency)

        coVerify(exactly = 1) { accountService.getBalanceByUsername(dummyUsername) }
    }

    @Test
    fun `getBalance should throw NotFoundResourceException when account not found`() = runTest {
        // arrange
        val dummyNonExistentUsername = "nonexistent"
        coEvery { accountService.getBalanceByUsername(dummyNonExistentUsername) } returns null

        // act & assert
        webTestClient
            .get()
            .uri("/api/payment/balance")
            .header("Authorization", "Bearer ${createTestJwtToken(dummyNonExistentUsername)}")
            .exchange()
            .expectStatus().isNotFound
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.meta.code").isEqualTo("404")
            .jsonPath("$.meta.message").isEqualTo("account not found for user: $dummyNonExistentUsername")

        coVerify(exactly = 1) { accountService.getBalanceByUsername(dummyNonExistentUsername) }
    }

    @Test
    fun `getWalletBalance should return ewallet balance successfully`() = runTest {
        // arrange
        coEvery { paymentService.getEWalletBalanceByUsername(dummyUsername, dummyWalletType) } returns dummyEWalletBalanceResponse

        // act & assert
        webTestClient
            .get()
            .uri("/api/payment/wallet?ewallet=$dummyWalletType")
            .header("Authorization", "Bearer ${createTestJwtToken(dummyUsername)}")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.meta.code").isEqualTo("200")
            .jsonPath("$.meta.message").isEqualTo("account retrieved successfully")
            .jsonPath("$.data.provider").isEqualTo(dummyWalletType)
            .jsonPath("$.data.balance").isEqualTo(75000.0)
            .jsonPath("$.data.currency").isEqualTo(dummyCurrency)
            .jsonPath("$.data.account_number").isEqualTo(dummyAccountId)

        coVerify(exactly = 1) { paymentService.getEWalletBalanceByUsername(dummyUsername, dummyWalletType) }
    }

    @Test
    fun `getWalletBalance should throw NotFoundResourceException when wallet not found`() = runTest {
        // arrange
        val dummyNonExistentUsername = "nonexistent"
        coEvery { paymentService.getEWalletBalanceByUsername(dummyNonExistentUsername, dummyWalletType) } returns null

        // act & assert
        webTestClient
            .get()
            .uri("/api/payment/wallet?ewallet=$dummyWalletType")
            .header("Authorization", "Bearer ${createTestJwtToken(dummyNonExistentUsername)}")
            .exchange()
            .expectStatus().isNotFound
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.meta.code").isEqualTo("404")
            .jsonPath("$.meta.message").isEqualTo("account not found for user: $dummyNonExistentUsername")

        coVerify(exactly = 1) { paymentService.getEWalletBalanceByUsername(dummyNonExistentUsername, dummyWalletType) }
    }

    @Test
    fun `topUpEWallet should return transaction response successfully`() = runTest {
        // arrange
        val dummyTopUpRequest = """
            {
                "amount": ${dummyTopUpAmount.toDouble()},
                "payment_method": "${PaymentMethod.GOPAY.value}",
                "phone_number": "+628123456789",
                "description": "$dummyDescription"
            }
        """.trimIndent()

        coEvery { paymentService.topUpEWallet(eq(dummyUsername), any()) } returns dummyTransactionResponse

        // act & assert
        webTestClient
            .post()
            .uri("/api/payment/wallet/topup")
            .header("Authorization", "Bearer ${createTestJwtToken(dummyUsername)}")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(dummyTopUpRequest)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.meta.code").isEqualTo("200")
            .jsonPath("$.meta.message").isEqualTo("top up successful")
            .jsonPath("$.data.id").isEqualTo("txn-123")
            .jsonPath("$.data.transaction_id").isEqualTo("TRX-123")
            .jsonPath("$.data.transaction_type").isEqualTo(TransactionType.TOPUP.name)
            .jsonPath("$.data.transaction_status").isEqualTo(TransactionStatus.SUCCESS.name)
            .jsonPath("$.data.amount").isEqualTo(dummyTopUpAmount.toDouble())
            .jsonPath("$.data.currency").isEqualTo(dummyCurrency)
            .jsonPath("$.data.payment_method").isEqualTo(PaymentMethod.GOPAY.name)
            .jsonPath("$.data.description").isEqualTo(dummyDescription)

        coVerify(exactly = 1) { paymentService.topUpEWallet(eq(dummyUsername), any()) }
    }

    @Test
    fun `topUpEWallet should return validation error for invalid request`() = runTest {
        // arrange - Invalid request with negative amount
        val dummyInvalidRequest = """
            {
                "amount": -5000,
                "payment_method": "${PaymentMethod.GOPAY.value}",
                "phone_number": "+628123456789",
                "description": "$dummyDescription"
            }
        """.trimIndent()

        // act & assert
        webTestClient
            .post()
            .uri("/api/payment/wallet/topup")
            .header("Authorization", "Bearer ${createTestJwtToken(dummyUsername)}")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(dummyInvalidRequest)
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.meta.code").isEqualTo("400")

        // No service method should be called for validation errors
        coVerify(exactly = 0) { paymentService.topUpEWallet(any(), any()) }
    }

    @Test
    fun `topUpEWallet should return validation error for blank payment method`() = runTest {
        // arrange - Invalid request with blank payment method
        val dummyInvalidRequest = """
            {
                "amount": ${dummyTopUpAmount.toDouble()},
                "payment_method": "",
                "phone_number": "+628123456789",
                "description": "$dummyDescription"
            }
        """.trimIndent()

        // act & assert
        webTestClient
            .post()
            .uri("/api/payment/wallet/topup")
            .header("Authorization", "Bearer ${createTestJwtToken(dummyUsername)}")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(dummyInvalidRequest)
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.meta.code").isEqualTo("400")

        coVerify(exactly = 0) { paymentService.topUpEWallet(any(), any()) }
    }

    @Test
    fun `topUpEWallet should return validation error for blank phone number`() = runTest {
        // arrange - Invalid request with blank phone number
        val dummyInvalidRequest = """
            {
                "amount": ${dummyTopUpAmount.toDouble()},
                "payment_method": "${PaymentMethod.GOPAY.value}",
                "phone_number": "",
                "description": "$dummyDescription"
            }
        """.trimIndent()

        // act & assert
        webTestClient
            .post()
            .uri("/api/payment/wallet/topup")
            .header("Authorization", "Bearer ${createTestJwtToken(dummyUsername)}")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(dummyInvalidRequest)
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.meta.code").isEqualTo("400")

        coVerify(exactly = 0) { paymentService.topUpEWallet(any(), any()) }
    }

    @Test
    fun `getWalletBalance should return bad request for missing ewallet parameter`() = runTest {
        // act & assert
        webTestClient
            .get()
            .uri("/api/payment/wallet")
            .header("Authorization", "Bearer ${createTestJwtToken(dummyUsername)}")
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `topUpEWallet should return bad request for missing request body`() = runTest {
        // act & assert
        webTestClient
            .post()
            .uri("/api/payment/wallet/topup")
            .header("Authorization", "Bearer ${createTestJwtToken(dummyUsername)}")
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isBadRequest
    }
}
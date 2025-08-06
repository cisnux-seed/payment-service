package id.co.bni.payment.domains.services

import id.co.bni.payment.commons.constants.AccountStatus
import id.co.bni.payment.commons.constants.PaymentMethod
import id.co.bni.payment.commons.constants.TransactionType
import id.co.bni.payment.commons.exceptions.APIException
import id.co.bni.payment.domains.dtos.BalanceInfo
import id.co.bni.payment.domains.dtos.CommonDtoResp
import id.co.bni.payment.domains.dtos.GetShopeePayWalletResp
import id.co.bni.payment.domains.dtos.GopayTopUpResp
import id.co.bni.payment.domains.dtos.GopayWalletResp
import id.co.bni.payment.domains.dtos.ShopeePayTopUpResp
import id.co.bni.payment.domains.dtos.TopUpDetails
import id.co.bni.payment.domains.dtos.TopUpEWalletRequest
import id.co.bni.payment.domains.entities.Account
import id.co.bni.payment.domains.entities.Transaction
import id.co.bni.payment.domains.entities.User
import id.co.bni.payment.domains.producers.TransactionProducer
import id.co.bni.payment.domains.repositories.AccountRepository
import id.co.bni.payment.domains.repositories.GopayRepository
import id.co.bni.payment.domains.repositories.ShopeePayRepository
import id.co.bni.payment.domains.repositories.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.Instant

@ExtendWith(MockKExtension::class)
@DisplayName("Payment Service")
class PaymentServiceTest {

    @MockK
    private lateinit var gopayRepository: GopayRepository

    @MockK
    private lateinit var shopeePayRepository: ShopeePayRepository

    @MockK
    private lateinit var userRepository: UserRepository

    @MockK
    private lateinit var accountRepository: AccountRepository

    @MockK
    private lateinit var transactionProducer: TransactionProducer

    @MockK
    private lateinit var cacheInvalidationService: CacheInvalidationService

    @InjectMockKs
    private lateinit var paymentService: PaymentServiceImpl

    // Dummy data
    private val dummyUserId = 12345L
    private val dummyUsername = "johndoe"
    private val dummyAccountId = "acc-123"
    private val dummyEmail = "john@example.com"
    private val dummyPhone = "+628123456789"
    private val dummyPassword = "hashedPassword123"
    private val dummyBalance = BigDecimal("150000.00")
    private val dummyCurrency = "IDR"
    private val dummyCreatedAt = Instant.parse("2024-01-01T10:00:00Z")
    private val dummyUpdatedAt = Instant.parse("2024-01-01T12:00:00Z")
    private val dummyTopUpAmount = BigDecimal("50000.00")
    private val dummyDescription = "Top up for shopping"

    private val dummyUser = User(
        id = dummyUserId,
        username = dummyUsername,
        phone = dummyPhone,
        email = dummyEmail,
        password = dummyPassword,
        createdAt = dummyCreatedAt,
        updatedAt = dummyUpdatedAt
    )

    private val dummyAccount = Account(
        id = dummyAccountId,
        userId = dummyUserId,
        balance = dummyBalance,
        currency = dummyCurrency,
        accountStatus = AccountStatus.ACTIVE,
        createdAt = dummyCreatedAt,
        updatedAt = dummyUpdatedAt
    )

    private val dummyTopUpRequest = TopUpEWalletRequest(
        amount = dummyTopUpAmount,
        paymentMethod = PaymentMethod.GOPAY.value,
        phoneNumber = dummyPhone,
        description = dummyDescription
    )

    private val dummyGopayWalletResp = GopayWalletResp(
        walletId = "898$dummyPhone",
        balance = 75000.0,
        currency = dummyCurrency,
        lastUpdated = "2024-01-01T12:00:00Z"
    )

    private val dummyGopayBalanceResp = CommonDtoResp(
        status = "success",
        data = dummyGopayWalletResp
    )

    private val dummyBalanceInfo = BalanceInfo(
        userId = "897$dummyPhone",
        availableBalance = 75000.0,
        pendingBalance = 0.0,
        currencyCode = dummyCurrency,
        lastTransactionDate = "2024-01-01T12:00:00Z"
    )

    private val dummyShopeePayBalanceResp = GetShopeePayWalletResp(
        result = "SUCCESS",
        requestId = "req-123",
        balanceInfo = dummyBalanceInfo
    )

    private val dummyGopayTopUpResp = GopayTopUpResp(
        status = "SUCCESS",
        transactionId = "txn-123",
        walletId = "898$dummyPhone",
        amount = dummyTopUpAmount.toDouble(),
        newBalance = 125000.0,
        transactionTime = "2024-01-01T12:30:00Z"
    )

    private val dummyShopeePayTopUpResp = ShopeePayTopUpResp(
        result = "SUCCESS",
        requestId = "req-456",
        topupDetails = TopUpDetails(
            transactionId = "txn-456",
            userId = "897$dummyPhone",
            amount = dummyTopUpAmount.toDouble(),
            status = "COMPLETED",
            createdAt = "2024-01-01T12:30:00Z"
        )
    )

    @Test
    fun `getEWalletBalanceByUsername with valid username and GOPAY should return balance response`() = runTest {
        // arrange
        val dummyWalletType = PaymentMethod.GOPAY.value
        coEvery { userRepository.findByUsername(dummyUsername) } returns dummyUser
        coEvery { gopayRepository.getBalanceByWalletId("898$dummyPhone") } returns dummyGopayBalanceResp
        coEvery { accountRepository.getAccountByUserId(dummyUserId) } returns dummyAccount

        // act
        val result = paymentService.getEWalletBalanceByUsername(dummyUsername, dummyWalletType)

        // assert
        assertNotNull(result)
        assertEquals(dummyWalletType, result?.provider)
        assertEquals(BigDecimal("75000"), result?.balance)
        assertEquals(dummyCurrency, result?.currency)
        assertEquals(dummyAccountId, result?.accountNumber)

        coVerify(exactly = 1) { userRepository.findByUsername(dummyUsername) }
        coVerify(exactly = 1) { gopayRepository.getBalanceByWalletId("898$dummyPhone") }
        coVerify(exactly = 1) { accountRepository.getAccountByUserId(dummyUserId) }
    }

    @Test
    fun `getEWalletBalanceByUsername with valid username and SHOPEE_PAY should return balance response`() = runTest {
        // arrange
        val dummyWalletType = PaymentMethod.SHOPEE_PAY.value
        coEvery { userRepository.findByUsername(dummyUsername) } returns dummyUser
        coEvery { shopeePayRepository.getShopeePayBalance(any()) } returns dummyShopeePayBalanceResp
        coEvery { accountRepository.getAccountByUserId(dummyUserId) } returns dummyAccount

        // act
        val result = paymentService.getEWalletBalanceByUsername(dummyUsername, dummyWalletType)

        // assert
        assertNotNull(result)
        assertEquals(dummyWalletType, result?.provider)
        assertEquals(BigDecimal("75000"), result?.balance)
        assertEquals(dummyCurrency, result?.currency)
        assertEquals(dummyAccountId, result?.accountNumber)

        coVerify(exactly = 1) { userRepository.findByUsername(dummyUsername) }
        coVerify(exactly = 1) { shopeePayRepository.getShopeePayBalance(any()) }
        coVerify(exactly = 1) { accountRepository.getAccountByUserId(dummyUserId) }
    }

    @Test
    fun `getEWalletBalanceByUsername with non-existent username should throw NotFoundResourceException`() = runTest {
        // arrange
        val dummyNonExistentUsername = "nonexistent"
        val dummyWalletType = PaymentMethod.GOPAY.value
        coEvery { userRepository.findByUsername(dummyNonExistentUsername) } returns null

        // act & assert
        val exception = assertThrows(APIException.NotFoundResourceException::class.java) {
            runBlocking {
                paymentService.getEWalletBalanceByUsername(dummyNonExistentUsername, dummyWalletType)
            }
        }

        assertEquals("user not found", exception.message)
        assertEquals(404, exception.statusCode)
        coVerify(exactly = 1) { userRepository.findByUsername(dummyNonExistentUsername) }
    }

    @Test
    fun `getEWalletBalanceByUsername with invalid wallet type should throw IllegalParameterException`() = runTest {
        // arrange
        val dummyInvalidWalletType = "INVALID_WALLET"
        coEvery { userRepository.findByUsername(dummyUsername) } returns dummyUser

        // act & assert
        val exception = assertThrows(APIException.IllegalParameterException::class.java) {
            runBlocking {
                paymentService.getEWalletBalanceByUsername(dummyUsername, dummyInvalidWalletType)
            }
        }

        assertEquals("invalid payment method: $dummyInvalidWalletType", exception.message)
        assertEquals(400, exception.statusCode)
        coVerify(exactly = 1) { userRepository.findByUsername(dummyUsername) }
    }

    @Test
    fun `topUpEWallet with valid GOPAY request should return transaction response and invalidate caches`() = runTest {
        // arrange
        val dummyUpdatedAccount = dummyAccount.copy(balance = dummyBalance - dummyTopUpAmount)
        val dummyAffectedRows = 1
        val transactionSlot = slot<Transaction>()

        coEvery { userRepository.findByUsername(dummyUsername) } returns dummyUser
        coEvery { accountRepository.getAccountByUserId(dummyUserId) } returns dummyAccount
        coEvery { gopayRepository.topUp(any()) } returns dummyGopayTopUpResp
        coEvery { accountRepository.updateAccountBalance(any()) } returns dummyAffectedRows
        coEvery { accountRepository.getAccountByUserId(dummyUserId) } returns dummyUpdatedAccount
        coEvery { transactionProducer.publishTransactionEvent(capture(transactionSlot)) } returns Unit
        coEvery { cacheInvalidationService.invalidateTransactionAndUserCaches(dummyUsername) } returns Unit

        // act
        val result = paymentService.topUpEWallet(dummyUsername, dummyTopUpRequest)

        // assert
        assertNotNull(result)
        assertEquals(TransactionType.TOPUP, result.transactionType)
        assertEquals(dummyTopUpAmount, result.amount)
        assertEquals(dummyCurrency, result.currency)
        assertEquals(dummyUpdatedAccount.balance, result.balanceAfter)
        assertEquals(PaymentMethod.GOPAY, result.paymentMethod)
        assertEquals(dummyDescription, result.description)

        // Verify captured transaction
        val capturedTransaction = transactionSlot.captured
        assertEquals(dummyUserId, capturedTransaction.userId)
        assertEquals(dummyUpdatedAccount.id, capturedTransaction.accountId)
        assertEquals(TransactionType.TOPUP, capturedTransaction.transactionType)

        coVerify(exactly = 1) { userRepository.findByUsername(dummyUsername) }
        coVerify(exactly = 2) { accountRepository.getAccountByUserId(dummyUserId) }
        coVerify(exactly = 1) { gopayRepository.topUp(any()) }
        coVerify(exactly = 1) { accountRepository.updateAccountBalance(any()) }
        coVerify(exactly = 1) { transactionProducer.publishTransactionEvent(any()) }
        coVerify(exactly = 1) { cacheInvalidationService.invalidateTransactionAndUserCaches(dummyUsername) }
    }

    @Test
        fun `topUpEWallet with valid SHOPEE_PAY request should return transaction response and invalidate caches`() = runTest {
        // arrange
        val dummyShopeePayRequest = dummyTopUpRequest.copy(paymentMethod = PaymentMethod.SHOPEE_PAY.value)
        val dummyUpdatedAccount = dummyAccount.copy(balance = dummyBalance - dummyTopUpAmount)
        val dummyAffectedRows = 1
        val transactionSlot = slot<Transaction>()

        coEvery { userRepository.findByUsername(dummyUsername) } returns dummyUser
        coEvery { accountRepository.getAccountByUserId(dummyUserId) } returns dummyAccount
        coEvery { shopeePayRepository.topUp(any()) } returns dummyShopeePayTopUpResp
        coEvery { accountRepository.updateAccountBalance(any()) } returns dummyAffectedRows
        coEvery { accountRepository.getAccountByUserId(dummyUserId) } returns dummyUpdatedAccount
        coEvery { transactionProducer.publishTransactionEvent(capture(transactionSlot)) } returns Unit
        coEvery { cacheInvalidationService.invalidateTransactionAndUserCaches(dummyUsername) } returns Unit

        // act
        val result = paymentService.topUpEWallet(dummyUsername, dummyShopeePayRequest)

        // assert
        assertNotNull(result)
        assertEquals(TransactionType.TOPUP, result.transactionType)
        assertEquals(dummyTopUpAmount, result.amount)
        assertEquals(dummyCurrency, result.currency)
        assertEquals(dummyUpdatedAccount.balance, result.balanceAfter)
        assertEquals(PaymentMethod.SHOPEE_PAY, result.paymentMethod)
        assertEquals(dummyDescription, result.description)

        coVerify(exactly = 1) { userRepository.findByUsername(dummyUsername) }
        coVerify(exactly = 2) { accountRepository.getAccountByUserId(dummyUserId) }
        coVerify(exactly = 1) { shopeePayRepository.topUp(any()) }
        coVerify(exactly = 1) { accountRepository.updateAccountBalance(any()) }
        coVerify(exactly = 1) { transactionProducer.publishTransactionEvent(any()) }
        coVerify(exactly = 1) { cacheInvalidationService.invalidateTransactionAndUserCaches(dummyUsername) }
    }

    @Test
    fun `topUpEWallet should continue successfully even if cache invalidation fails`() = runTest {
        // arrange
        val dummyUpdatedAccount = dummyAccount.copy(balance = dummyBalance - dummyTopUpAmount)
        val dummyAffectedRows = 1

        coEvery { userRepository.findByUsername(dummyUsername) } returns dummyUser
        coEvery { accountRepository.getAccountByUserId(dummyUserId) } returns dummyAccount
        coEvery { gopayRepository.topUp(any()) } returns dummyGopayTopUpResp
        coEvery { accountRepository.updateAccountBalance(any()) } returns dummyAffectedRows
        coEvery { accountRepository.getAccountByUserId(dummyUserId) } returns dummyUpdatedAccount
        coEvery { transactionProducer.publishTransactionEvent(any()) } returns Unit
        coEvery { cacheInvalidationService.invalidateTransactionAndUserCaches(dummyUsername) } throws RuntimeException("Cache service unavailable")

        // act - should not throw exception despite cache invalidation failure
        val result = paymentService.topUpEWallet(dummyUsername, dummyTopUpRequest)

        // assert
        assertNotNull(result)
        assertEquals(TransactionType.TOPUP, result.transactionType)
        assertEquals(dummyTopUpAmount, result.amount)

        // Verify that all business operations completed successfully
        coVerify(exactly = 1) { userRepository.findByUsername(dummyUsername) }
        coVerify(exactly = 2) { accountRepository.getAccountByUserId(dummyUserId) }
        coVerify(exactly = 1) { gopayRepository.topUp(any()) }
        coVerify(exactly = 1) { accountRepository.updateAccountBalance(any()) }
        coVerify(exactly = 1) { transactionProducer.publishTransactionEvent(any()) }
        coVerify(exactly = 1) { cacheInvalidationService.invalidateTransactionAndUserCaches(dummyUsername) }
    }

    @Test
    fun `topUpEWallet with non-existent user should throw NotFoundResourceException`() = runTest {
        // arrange
        val dummyNonExistentUsername = "nonexistent"
        coEvery { userRepository.findByUsername(dummyNonExistentUsername) } returns null

        // act & assert
        val exception = assertThrows(APIException.NotFoundResourceException::class.java) {
            runBlocking {
                paymentService.topUpEWallet(dummyNonExistentUsername, dummyTopUpRequest)
            }
        }

        assertEquals("user not found", exception.message)
        assertEquals(404, exception.statusCode)
        coVerify(exactly = 1) { userRepository.findByUsername(dummyNonExistentUsername) }
        coVerify(exactly = 0) { cacheInvalidationService.invalidateTransactionAndUserCaches(any()) }
    }

    @Test
    fun `topUpEWallet with inactive account should throw ForbiddenException`() = runTest {
        // arrange
        val dummyInactiveAccount = dummyAccount.copy(accountStatus = AccountStatus.SUSPENDED)
        coEvery { userRepository.findByUsername(dummyUsername) } returns dummyUser
        coEvery { accountRepository.getAccountByUserId(dummyUserId) } returns dummyInactiveAccount

        // act & assert
        val exception = assertThrows(APIException.ForbiddenException::class.java) {
            runBlocking {
                paymentService.topUpEWallet(dummyUsername, dummyTopUpRequest)
            }
        }

        assertEquals("account is not active, cannot perform top up", exception.message)
        assertEquals(403, exception.statusCode)
        coVerify(exactly = 1) { userRepository.findByUsername(dummyUsername) }
        coVerify(exactly = 1) { accountRepository.getAccountByUserId(dummyUserId) }
        coVerify(exactly = 0) { cacheInvalidationService.invalidateTransactionAndUserCaches(any()) }
    }

    @Test
    fun `topUpEWallet with insufficient balance should throw IllegalParameterException`() = runTest {
        // arrange
        val dummyInsufficientBalanceAccount = dummyAccount.copy(balance = BigDecimal("10000.00"))
        coEvery { userRepository.findByUsername(dummyUsername) } returns dummyUser
        coEvery { accountRepository.getAccountByUserId(dummyUserId) } returns dummyInsufficientBalanceAccount

        // act & assert
        val exception = assertThrows(APIException.IllegalParameterException::class.java) {
            runBlocking {
                paymentService.topUpEWallet(dummyUsername, dummyTopUpRequest)
            }
        }

        assertEquals("insufficient balance for top up", exception.message)
        assertEquals(400, exception.statusCode)
        coVerify(exactly = 1) { userRepository.findByUsername(dummyUsername) }
        coVerify(exactly = 1) { accountRepository.getAccountByUserId(dummyUserId) }
        coVerify(exactly = 0) { cacheInvalidationService.invalidateTransactionAndUserCaches(any()) }
    }

    @Test
    fun `topUpEWallet with invalid payment method should throw IllegalParameterException`() = runTest {
        // arrange
        val dummyInvalidRequest = dummyTopUpRequest.copy(paymentMethod = "INVALID_METHOD")
        coEvery { userRepository.findByUsername(dummyUsername) } returns dummyUser
        coEvery { accountRepository.getAccountByUserId(dummyUserId) } returns dummyAccount

        // act & assert
        val exception = assertThrows(APIException.IllegalParameterException::class.java) {
            runBlocking {
                paymentService.topUpEWallet(dummyUsername, dummyInvalidRequest)
            }
        }

        assertEquals("invalid payment method: INVALID_METHOD", exception.message)
        assertEquals(400, exception.statusCode)
        coVerify(exactly = 1) { userRepository.findByUsername(dummyUsername) }
        coVerify(exactly = 1) { accountRepository.getAccountByUserId(dummyUserId) }
        coVerify(exactly = 0) { cacheInvalidationService.invalidateTransactionAndUserCaches(any()) }
    }

    @Test
    fun `topUpEWallet with GOPAY top up failure should throw NotFoundResourceException`() = runTest {
        // arrange
        coEvery { userRepository.findByUsername(dummyUsername) } returns dummyUser
        coEvery { accountRepository.getAccountByUserId(dummyUserId) } returns dummyAccount
        coEvery { gopayRepository.topUp(any()) } returns null

        // act & assert
        val exception = assertThrows(APIException.NotFoundResourceException::class.java) {
            runBlocking {
                paymentService.topUpEWallet(dummyUsername, dummyTopUpRequest)
            }
        }

        assertEquals("top up failed for walletId: 898$dummyPhone", exception.message)
        assertEquals(404, exception.statusCode)
        coVerify(exactly = 1) { userRepository.findByUsername(dummyUsername) }
        coVerify(exactly = 1) { accountRepository.getAccountByUserId(dummyUserId) }
        coVerify(exactly = 1) { gopayRepository.topUp(any()) }
        coVerify(exactly = 0) { cacheInvalidationService.invalidateTransactionAndUserCaches(any()) }
    }
}
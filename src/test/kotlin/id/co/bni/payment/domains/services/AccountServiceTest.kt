package id.co.bni.payment.domains.services

import id.co.bni.payment.commons.constants.AccountStatus
import id.co.bni.payment.commons.exceptions.APIException
import id.co.bni.payment.domains.entities.Account
import id.co.bni.payment.domains.entities.User
import id.co.bni.payment.domains.repositories.AccountRepository
import id.co.bni.payment.domains.repositories.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@ExtendWith(MockKExtension::class)
class AccountServiceTest {
    private lateinit var accountService: AccountServiceImpl
    @MockK
    private lateinit var accountRepository: AccountRepository
    @MockK
    private lateinit var userRepository: UserRepository
    @MockK
    private lateinit var cacheService: CacheService

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

    @BeforeEach
    fun setUp() {
        accountService = AccountServiceImpl(accountRepository, userRepository, cacheService)
    }

    @Test
    fun `updateAccountBalance with valid account should return updated rows count`() = runTest {
        // arrange
        val dummyUpdatedRowsCount = 1
        coEvery { accountRepository.updateAccountBalance(dummyAccount) } returns dummyUpdatedRowsCount

        // act
        val result = accountService.updateAccountBalance(dummyAccount)

        // assert
        assertEquals(dummyUpdatedRowsCount, result)
        coVerify(exactly = 1) { accountRepository.updateAccountBalance(dummyAccount) }
    }

    @Test
    fun `updateAccountBalance with account update failure should return zero rows count`() = runTest {
        // arrange
        val dummyZeroRowsCount = 0
        coEvery { accountRepository.updateAccountBalance(dummyAccount) } returns dummyZeroRowsCount

        // act
        val result = accountService.updateAccountBalance(dummyAccount)

        // assert
        assertEquals(dummyZeroRowsCount, result)
        coVerify(exactly = 1) { accountRepository.updateAccountBalance(dummyAccount) }
    }

    @Test
    fun `getAccountByUsername with valid username should return account response`() = runTest {
        // arrange
        coEvery { userRepository.findByUsername(dummyUsername) } returns dummyUser
        coEvery { accountRepository.getAccountByUserId(dummyUserId) } returns dummyAccount

        // act
        val result = accountService.getAccountByUsername(dummyUsername)

        // assert
        assertNotNull(result)
        assertEquals(dummyAccountId, result?.id)
        assertEquals(dummyUserId, result?.userId)
        assertEquals(dummyBalance, result?.balance)
        assertEquals(dummyCurrency, result?.currency)
        assertEquals(AccountStatus.ACTIVE, result?.accountStatus)
        assertEquals(LocalDateTime.ofInstant(dummyCreatedAt, ZoneOffset.UTC), result?.createdAt)
        assertEquals(LocalDateTime.ofInstant(dummyUpdatedAt, ZoneOffset.UTC), result?.updatedAt)

        coVerify(exactly = 1) { userRepository.findByUsername(dummyUsername) }
        coVerify(exactly = 1) { accountRepository.getAccountByUserId(dummyUserId) }
    }

    @Test
    fun `getAccountByUsername with non-existent username should throw NotFoundResourceException`() = runTest {
        // arrange
        val dummyNonExistentUsername = "nonexistent"
        coEvery { userRepository.findByUsername(dummyNonExistentUsername) } returns null

        // act & assert
        val exception = assertThrows(APIException.NotFoundResourceException::class.java) {
            runBlocking {
                accountService.getAccountByUsername(dummyNonExistentUsername)
            }
        }

        assertEquals("user not found", exception.message)
        assertEquals(404, exception.statusCode)
        coVerify(exactly = 1) { userRepository.findByUsername(dummyNonExistentUsername) }
        coVerify(exactly = 0) { accountRepository.getAccountByUserId(any()) }
    }

    @Test
    fun `getAccountByUsername with user without account should throw NotFoundResourceException`() = runTest {
        // arrange
        coEvery { userRepository.findByUsername(dummyUsername) } returns dummyUser
        coEvery { accountRepository.getAccountByUserId(dummyUserId) } returns null

        // act & assert
        val exception = assertThrows(APIException.NotFoundResourceException::class.java) {
            runBlocking {
                accountService.getAccountByUsername(dummyUsername)
            }
        }

        assertEquals("account not found", exception.message)
        assertEquals(404, exception.statusCode)
        coVerify(exactly = 1) { userRepository.findByUsername(dummyUsername) }
        coVerify(exactly = 1) { accountRepository.getAccountByUserId(dummyUserId) }
    }

    @Test
    fun `getBalanceByUsername with valid username should return balance response`() = runTest {
        // arrange
        coEvery { userRepository.findByUsername(dummyUsername) } returns dummyUser
        coEvery { accountRepository.getAccountByUserId(dummyUserId) } returns dummyAccount

        // act
        val result = accountService.getBalanceByUsername(dummyUsername)

        // assert
        assertNotNull(result)
        assertEquals(dummyBalance, result?.balance)
        assertEquals(dummyCurrency, result?.currency)

        coVerify(exactly = 1) { userRepository.findByUsername(dummyUsername) }
        coVerify(exactly = 1) { accountRepository.getAccountByUserId(dummyUserId) }
    }

    @Test
    fun `getBalanceByUsername with non-existent username should throw NotFoundResourceException`() = runTest {
        // arrange
        val dummyNonExistentUsername = "nonexistent"
        coEvery { userRepository.findByUsername(dummyNonExistentUsername) } returns null

        // act & assert
        val exception = assertThrows(APIException.NotFoundResourceException::class.java) {
            runBlocking {
                accountService.getBalanceByUsername(dummyNonExistentUsername)
            }
        }

        assertEquals("user not found", exception.message)
        assertEquals(404, exception.statusCode)
        coVerify(exactly = 1) { userRepository.findByUsername(dummyNonExistentUsername) }
        coVerify(exactly = 0) { accountRepository.getAccountByUserId(any()) }
    }

    @Test
    fun `getBalanceByUsername with user without account should throw NotFoundResourceException`() = runTest {
        // arrange
        coEvery { userRepository.findByUsername(dummyUsername) } returns dummyUser
        coEvery { accountRepository.getAccountByUserId(dummyUserId) } returns null

        // act & assert
        val exception = assertThrows(APIException.NotFoundResourceException::class.java) {
            runBlocking {
                accountService.getBalanceByUsername(dummyUsername)
            }
        }

        assertEquals("account not found", exception.message)
        assertEquals(404, exception.statusCode)
        coVerify(exactly = 1) { userRepository.findByUsername(dummyUsername) }
        coVerify(exactly = 1) { accountRepository.getAccountByUserId(dummyUserId) }
    }

    @Test
    fun `getAccountByUsername with user having null id should throw exception`() = runTest {
        // arrange
        val dummyUserWithNullId = dummyUser.copy(id = null)
        coEvery { userRepository.findByUsername(dummyUsername) } returns dummyUserWithNullId

        // act & assert
        assertThrows(NullPointerException::class.java) {
            runBlocking {
                accountService.getAccountByUsername(dummyUsername)
            }
        }

        coVerify(exactly = 1) { userRepository.findByUsername(dummyUsername) }
    }

    @Test
    fun `getBalanceByUsername with user having null id should throw exception`() = runTest {
        // arrange
        val dummyUserWithNullId = dummyUser.copy(id = null)
        coEvery { userRepository.findByUsername(dummyUsername) } returns dummyUserWithNullId

        // act & assert
        assertThrows(NullPointerException::class.java) {
            runBlocking {
                accountService.getBalanceByUsername(dummyUsername)
            }
        }

        coVerify(exactly = 1) { userRepository.findByUsername(dummyUsername) }
    }
}
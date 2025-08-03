package id.co.bni.payment.domains.services

import id.co.bni.payment.commons.constants.AccountStatus
import id.co.bni.payment.commons.constants.CacheKeys
import id.co.bni.payment.commons.exceptions.APIException
import id.co.bni.payment.domains.dtos.AccountResponse
import id.co.bni.payment.domains.dtos.BalanceResponse
import id.co.bni.payment.domains.entities.Account
import id.co.bni.payment.domains.entities.User
import id.co.bni.payment.domains.repositories.AccountRepository
import id.co.bni.payment.domains.repositories.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
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

    private val dummyAccountResponse = AccountResponse(
        id = dummyAccountId,
        userId = dummyUserId,
        balance = dummyBalance,
        currency = dummyCurrency,
        accountStatus = AccountStatus.ACTIVE,
        createdAt = LocalDateTime.ofInstant(dummyCreatedAt, ZoneOffset.UTC),
        updatedAt = LocalDateTime.ofInstant(dummyUpdatedAt, ZoneOffset.UTC)
    )

    private val dummyBalanceResponse = BalanceResponse(
        balance = dummyBalance,
        currency = dummyCurrency
    )

    @BeforeEach
    fun setUp() {
        accountService = AccountServiceImpl(accountRepository, userRepository, cacheService)
    }

    @Test
    fun `getAccountByUsername with cache hit should return cached response`() = runTest {
        // arrange
        val cacheKey = CacheKeys.accountKey(dummyUsername)
        coEvery { cacheService.get(cacheKey, AccountResponse::class.java) } returns dummyAccountResponse

        // act
        val result = accountService.getAccountByUsername(dummyUsername)

        // assert
        assertNotNull(result)
        assertEquals(dummyAccountResponse, result)
        coVerify(exactly = 1) { cacheService.get(cacheKey, AccountResponse::class.java) }
        coVerify(exactly = 0) { userRepository.findByUsername(any()) }
        coVerify(exactly = 0) { accountRepository.getAccountByUserId(any()) }
        coVerify(exactly = 0) { cacheService.set(any(), any(), any()) }
    }

    @Test
    fun `getAccountByUsername with cache miss should fetch from database and cache result`() = runTest {
        // arrange
        val cacheKey = CacheKeys.accountKey(dummyUsername)
        val accountResponseSlot = slot<AccountResponse>()

        coEvery { cacheService.get(cacheKey, AccountResponse::class.java) } returns null
        coEvery { userRepository.findByUsername(dummyUsername) } returns dummyUser
        coEvery { accountRepository.getAccountByUserId(dummyUserId) } returns dummyAccount
        coEvery { cacheService.set(cacheKey, capture(accountResponseSlot), 15) } returns Unit

        // act
        val result = accountService.getAccountByUsername(dummyUsername)

        // assert
        assertNotNull(result)
        assertEquals(dummyAccountId, result?.id)
        assertEquals(dummyUserId, result?.userId)
        assertEquals(dummyBalance, result?.balance)
        assertEquals(dummyCurrency, result?.currency)
        assertEquals(AccountStatus.ACTIVE, result?.accountStatus)

        // Verify cached value
        val cachedValue = accountResponseSlot.captured
        assertEquals(dummyAccountId, cachedValue.id)
        assertEquals(dummyUserId, cachedValue.userId)
        assertEquals(dummyBalance, cachedValue.balance)

        coVerify(exactly = 1) { cacheService.get(cacheKey, AccountResponse::class.java) }
        coVerify(exactly = 1) { userRepository.findByUsername(dummyUsername) }
        coVerify(exactly = 1) { accountRepository.getAccountByUserId(dummyUserId) }
        coVerify(exactly = 1) { cacheService.set(cacheKey, any(), 15) }
    }

    @Test
    fun `getAccountByUsername with non-existent username should throw NotFoundResourceException`() = runTest {
        // arrange
        val dummyNonExistentUsername = "nonexistent"
        val cacheKey = CacheKeys.accountKey(dummyNonExistentUsername)
        coEvery { cacheService.get(cacheKey, AccountResponse::class.java) } returns null
        coEvery { userRepository.findByUsername(dummyNonExistentUsername) } returns null

        // act & assert
        val exception = assertThrows(APIException.NotFoundResourceException::class.java) {
            runBlocking {
                accountService.getAccountByUsername(dummyNonExistentUsername)
            }
        }

        assertEquals("user not found", exception.message)
        assertEquals(404, exception.statusCode)
        coVerify(exactly = 1) { cacheService.get(cacheKey, AccountResponse::class.java) }
        coVerify(exactly = 1) { userRepository.findByUsername(dummyNonExistentUsername) }
        coVerify(exactly = 0) { accountRepository.getAccountByUserId(any()) }
        coVerify(exactly = 0) { cacheService.set(any(), any(), any()) }
    }

    @Test
    fun `getAccountByUsername with user without account should throw NotFoundResourceException`() = runTest {
        // arrange
        val cacheKey = CacheKeys.accountKey(dummyUsername)
        coEvery { cacheService.get(cacheKey, AccountResponse::class.java) } returns null
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
        coVerify(exactly = 1) { cacheService.get(cacheKey, AccountResponse::class.java) }
        coVerify(exactly = 1) { userRepository.findByUsername(dummyUsername) }
        coVerify(exactly = 1) { accountRepository.getAccountByUserId(dummyUserId) }
        coVerify(exactly = 0) { cacheService.set(any(), any(), any()) }
    }

    @Test
    fun `getAccountByUsername with user having null id should throw exception`() = runTest {
        // arrange
        val dummyUserWithNullId = dummyUser.copy(id = null)
        val cacheKey = CacheKeys.accountKey(dummyUsername)
        coEvery { cacheService.get(cacheKey, AccountResponse::class.java) } returns null
        coEvery { userRepository.findByUsername(dummyUsername) } returns dummyUserWithNullId

        // act & assert
        assertThrows(NullPointerException::class.java) {
            runBlocking {
                accountService.getAccountByUsername(dummyUsername)
            }
        }

        coVerify(exactly = 1) { cacheService.get(cacheKey, AccountResponse::class.java) }
        coVerify(exactly = 1) { userRepository.findByUsername(dummyUsername) }
    }

    @Test
    fun `getBalanceByUsername with cache hit should return cached response`() = runTest {
        // arrange
        val cacheKey = CacheKeys.balanceKey(dummyUsername)
        coEvery { cacheService.get(cacheKey, BalanceResponse::class.java) } returns dummyBalanceResponse

        // act
        val result = accountService.getBalanceByUsername(dummyUsername)

        // assert
        assertNotNull(result)
        assertEquals(dummyBalanceResponse, result)
        coVerify(exactly = 1) { cacheService.get(cacheKey, BalanceResponse::class.java) }
        coVerify(exactly = 0) { userRepository.findByUsername(any()) }
        coVerify(exactly = 0) { accountRepository.getAccountByUserId(any()) }
        coVerify(exactly = 0) { cacheService.set(any(), any(), any()) }
    }

    @Test
    fun `getBalanceByUsername with cache miss should fetch from database and cache result`() = runTest {
        // arrange
        val cacheKey = CacheKeys.balanceKey(dummyUsername)
        val balanceResponseSlot = slot<BalanceResponse>()

        coEvery { cacheService.get(cacheKey, BalanceResponse::class.java) } returns null
        coEvery { userRepository.findByUsername(dummyUsername) } returns dummyUser
        coEvery { accountRepository.getAccountByUserId(dummyUserId) } returns dummyAccount
        coEvery { cacheService.set(cacheKey, capture(balanceResponseSlot), 10) } returns Unit

        // act
        val result = accountService.getBalanceByUsername(dummyUsername)

        // assert
        assertNotNull(result)
        assertEquals(dummyBalance, result?.balance)
        assertEquals(dummyCurrency, result?.currency)

        // Verify cached value
        val cachedValue = balanceResponseSlot.captured
        assertEquals(dummyBalance, cachedValue.balance)
        assertEquals(dummyCurrency, cachedValue.currency)

        coVerify(exactly = 1) { cacheService.get(cacheKey, BalanceResponse::class.java) }
        coVerify(exactly = 1) { userRepository.findByUsername(dummyUsername) }
        coVerify(exactly = 1) { accountRepository.getAccountByUserId(dummyUserId) }
        coVerify(exactly = 1) { cacheService.set(cacheKey, any(), 10) }
    }

    @Test
    fun `getBalanceByUsername with non-existent username should throw NotFoundResourceException`() = runTest {
        // arrange
        val dummyNonExistentUsername = "nonexistent"
        val cacheKey = CacheKeys.balanceKey(dummyNonExistentUsername)
        coEvery { cacheService.get(cacheKey, BalanceResponse::class.java) } returns null
        coEvery { userRepository.findByUsername(dummyNonExistentUsername) } returns null

        // act & assert
        val exception = assertThrows(APIException.NotFoundResourceException::class.java) {
            runBlocking {
                accountService.getBalanceByUsername(dummyNonExistentUsername)
            }
        }

        assertEquals("user not found", exception.message)
        assertEquals(404, exception.statusCode)
        coVerify(exactly = 1) { cacheService.get(cacheKey, BalanceResponse::class.java) }
        coVerify(exactly = 1) { userRepository.findByUsername(dummyNonExistentUsername) }
        coVerify(exactly = 0) { accountRepository.getAccountByUserId(any()) }
        coVerify(exactly = 0) { cacheService.set(any(), any(), any()) }
    }

    @Test
    fun `getBalanceByUsername with user without account should throw NotFoundResourceException`() = runTest {
        // arrange
        val cacheKey = CacheKeys.balanceKey(dummyUsername)
        coEvery { cacheService.get(cacheKey, BalanceResponse::class.java) } returns null
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
        coVerify(exactly = 1) { cacheService.get(cacheKey, BalanceResponse::class.java) }
        coVerify(exactly = 1) { userRepository.findByUsername(dummyUsername) }
        coVerify(exactly = 1) { accountRepository.getAccountByUserId(dummyUserId) }
        coVerify(exactly = 0) { cacheService.set(any(), any(), any()) }
    }

    @Test
    fun `getBalanceByUsername with user having null id should throw exception`() = runTest {
        // arrange
        val dummyUserWithNullId = dummyUser.copy(id = null)
        val cacheKey = CacheKeys.balanceKey(dummyUsername)
        coEvery { cacheService.get(cacheKey, BalanceResponse::class.java) } returns null
        coEvery { userRepository.findByUsername(dummyUsername) } returns dummyUserWithNullId

        // act & assert
        assertThrows(NullPointerException::class.java) {
            runBlocking {
                accountService.getBalanceByUsername(dummyUsername)
            }
        }

        coVerify(exactly = 1) { cacheService.get(cacheKey, BalanceResponse::class.java) }
        coVerify(exactly = 1) { userRepository.findByUsername(dummyUsername) }
    }

    // ============== Cache Management Tests ==============

    @Test
    fun `invalidateUserCache should delete both account and balance cache keys`() = runTest {
        // arrange
        val accountCacheKey = CacheKeys.accountKey(dummyUsername)
        val balanceCacheKey = CacheKeys.balanceKey(dummyUsername)
        coEvery { cacheService.delete(accountCacheKey) } returns true
        coEvery { cacheService.delete(balanceCacheKey) } returns true

        // act
        accountService.invalidateUserCache(dummyUsername)

        // assert
        coVerify(exactly = 1) { cacheService.delete(accountCacheKey) }
        coVerify(exactly = 1) { cacheService.delete(balanceCacheKey) }
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

    // ============== Different Account Status Tests ==============

    @Test
    fun `getAccountByUsername with suspended account should return account response with suspended status`() = runTest {
        // arrange
        val suspendedAccount = dummyAccount.copy(accountStatus = AccountStatus.SUSPENDED)
        val cacheKey = CacheKeys.accountKey(dummyUsername)

        coEvery { cacheService.get(cacheKey, AccountResponse::class.java) } returns null
        coEvery { userRepository.findByUsername(dummyUsername) } returns dummyUser
        coEvery { accountRepository.getAccountByUserId(dummyUserId) } returns suspendedAccount
        coEvery { cacheService.set(cacheKey, any(), 15) } returns Unit

        // act
        val result = accountService.getAccountByUsername(dummyUsername)

        // assert
        assertNotNull(result)
        assertEquals(AccountStatus.SUSPENDED, result?.accountStatus)
        assertEquals(dummyAccountId, result?.id)
        assertEquals(dummyUserId, result?.userId)

        coVerify(exactly = 1) { userRepository.findByUsername(dummyUsername) }
        coVerify(exactly = 1) { accountRepository.getAccountByUserId(dummyUserId) }
        coVerify(exactly = 1) { cacheService.set(cacheKey, any(), 15) }
    }

    @Test
    fun `getBalanceByUsername with different currency should return correct currency`() = runTest {
        // arrange
        val usdAccount = dummyAccount.copy(currency = "USD", balance = BigDecimal("1000.00"))
        val cacheKey = CacheKeys.balanceKey(dummyUsername)

        coEvery { cacheService.get(cacheKey, BalanceResponse::class.java) } returns null
        coEvery { userRepository.findByUsername(dummyUsername) } returns dummyUser
        coEvery { accountRepository.getAccountByUserId(dummyUserId) } returns usdAccount
        coEvery { cacheService.set(cacheKey, any(), 10) } returns Unit

        // act
        val result = accountService.getBalanceByUsername(dummyUsername)

        // assert
        assertNotNull(result)
        assertEquals(BigDecimal("1000.00"), result?.balance)
        assertEquals("USD", result?.currency)

        coVerify(exactly = 1) { userRepository.findByUsername(dummyUsername) }
        coVerify(exactly = 1) { accountRepository.getAccountByUserId(dummyUserId) }
        coVerify(exactly = 1) { cacheService.set(cacheKey, any(), 10) }
    }

    // ============== Cache Error Handling Tests ==============

    @Test
    fun `getAccountByUsername should throw exception when cache get fails`() = runTest {
        // arrange
        val cacheKey = CacheKeys.accountKey(dummyUsername)
        coEvery { cacheService.get(cacheKey, AccountResponse::class.java) } throws RuntimeException("Cache read error")

        // act & assert - should throw the cache exception
        assertThrows(RuntimeException::class.java) {
            runBlocking {
                accountService.getAccountByUsername(dummyUsername)
            }
        }

        coVerify(exactly = 1) { cacheService.get(cacheKey, AccountResponse::class.java) }
        coVerify(exactly = 0) { userRepository.findByUsername(any()) }
        coVerify(exactly = 0) { accountRepository.getAccountByUserId(any()) }
    }

    @Test
    fun `getAccountByUsername should throw exception when cache set fails`() = runTest {
        // arrange
        val cacheKey = CacheKeys.accountKey(dummyUsername)
        coEvery { cacheService.get(cacheKey, AccountResponse::class.java) } returns null
        coEvery { userRepository.findByUsername(dummyUsername) } returns dummyUser
        coEvery { accountRepository.getAccountByUserId(dummyUserId) } returns dummyAccount
        coEvery { cacheService.set(cacheKey, any(), 15) } throws RuntimeException("Cache write error")

        // act & assert - should throw the cache exception
        assertThrows(RuntimeException::class.java) {
            runBlocking {
                accountService.getAccountByUsername(dummyUsername)
            }
        }

        coVerify(exactly = 1) { cacheService.get(cacheKey, AccountResponse::class.java) }
        coVerify(exactly = 1) { userRepository.findByUsername(dummyUsername) }
        coVerify(exactly = 1) { accountRepository.getAccountByUserId(dummyUserId) }
        coVerify(exactly = 1) { cacheService.set(cacheKey, any(), 15) }
    }

    @Test
    fun `getBalanceByUsername should throw exception when cache get fails`() = runTest {
        // arrange
        val cacheKey = CacheKeys.balanceKey(dummyUsername)
        coEvery { cacheService.get(cacheKey, BalanceResponse::class.java) } throws RuntimeException("Cache read error")

        // act & assert - should throw the cache exception
        assertThrows(RuntimeException::class.java) {
            runBlocking {
                accountService.getBalanceByUsername(dummyUsername)
            }
        }

        coVerify(exactly = 1) { cacheService.get(cacheKey, BalanceResponse::class.java) }
        coVerify(exactly = 0) { userRepository.findByUsername(any()) }
        coVerify(exactly = 0) { accountRepository.getAccountByUserId(any()) }
    }

    @Test
    fun `getBalanceByUsername should throw exception when cache set fails`() = runTest {
        // arrange
        val cacheKey = CacheKeys.balanceKey(dummyUsername)
        coEvery { cacheService.get(cacheKey, BalanceResponse::class.java) } returns null
        coEvery { userRepository.findByUsername(dummyUsername) } returns dummyUser
        coEvery { accountRepository.getAccountByUserId(dummyUserId) } returns dummyAccount
        coEvery { cacheService.set(cacheKey, any(), 10) } throws RuntimeException("Cache write error")

        // act & assert - should throw the cache exception
        assertThrows(RuntimeException::class.java) {
            runBlocking {
                accountService.getBalanceByUsername(dummyUsername)
            }
        }

        coVerify(exactly = 1) { cacheService.get(cacheKey, BalanceResponse::class.java) }
        coVerify(exactly = 1) { userRepository.findByUsername(dummyUsername) }
        coVerify(exactly = 1) { accountRepository.getAccountByUserId(dummyUserId) }
        coVerify(exactly = 1) { cacheService.set(cacheKey, any(), 10) }
    }

    // ============== Edge Cases and Boundary Tests ==============

    @Test
    fun `getAccountByUsername with zero balance should work correctly`() = runTest {
        // arrange
        val zeroBalanceAccount = dummyAccount.copy(balance = BigDecimal.ZERO)
        val cacheKey = CacheKeys.accountKey(dummyUsername)

        coEvery { cacheService.get(cacheKey, AccountResponse::class.java) } returns null
        coEvery { userRepository.findByUsername(dummyUsername) } returns dummyUser
        coEvery { accountRepository.getAccountByUserId(dummyUserId) } returns zeroBalanceAccount
        coEvery { cacheService.set(cacheKey, any(), 15) } returns Unit

        // act
        val result = accountService.getAccountByUsername(dummyUsername)

        // assert
        assertNotNull(result)
        assertEquals(BigDecimal.ZERO, result?.balance)
        assertEquals(dummyAccountId, result?.id)

        coVerify(exactly = 1) { userRepository.findByUsername(dummyUsername) }
        coVerify(exactly = 1) { accountRepository.getAccountByUserId(dummyUserId) }
        coVerify(exactly = 1) { cacheService.set(cacheKey, any(), 15) }
    }

    @Test
    fun `getBalanceByUsername with large balance should work correctly`() = runTest {
        // arrange
        val largeBalance = BigDecimal("999999999.99")
        val largeBalanceAccount = dummyAccount.copy(balance = largeBalance)
        val cacheKey = CacheKeys.balanceKey(dummyUsername)

        coEvery { cacheService.get(cacheKey, BalanceResponse::class.java) } returns null
        coEvery { userRepository.findByUsername(dummyUsername) } returns dummyUser
        coEvery { accountRepository.getAccountByUserId(dummyUserId) } returns largeBalanceAccount
        coEvery { cacheService.set(cacheKey, any(), 10) } returns Unit

        // act
        val result = accountService.getBalanceByUsername(dummyUsername)

        // assert
        assertNotNull(result)
        assertEquals(largeBalance, result?.balance)
        assertEquals(dummyCurrency, result?.currency)

        coVerify(exactly = 1) { userRepository.findByUsername(dummyUsername) }
        coVerify(exactly = 1) { accountRepository.getAccountByUserId(dummyUserId) }
        coVerify(exactly = 1) { cacheService.set(cacheKey, any(), 10) }
    }

    @Test
    fun `getAccountByUsername with special characters in username should work correctly`() = runTest {
        // arrange
        val specialUsername = "user@test-123_456"
        val cacheKey = CacheKeys.accountKey(specialUsername)
        val specialUser = dummyUser.copy(username = specialUsername)

        coEvery { cacheService.get(cacheKey, AccountResponse::class.java) } returns null
        coEvery { userRepository.findByUsername(specialUsername) } returns specialUser
        coEvery { accountRepository.getAccountByUserId(dummyUserId) } returns dummyAccount
        coEvery { cacheService.set(cacheKey, any(), 15) } returns Unit

        // act
        val result = accountService.getAccountByUsername(specialUsername)

        // assert
        assertNotNull(result)
        assertEquals(dummyAccountId, result?.id)
        assertEquals(dummyUserId, result?.userId)

        coVerify(exactly = 1) { cacheService.get(cacheKey, AccountResponse::class.java) }
        coVerify(exactly = 1) { userRepository.findByUsername(specialUsername) }
        coVerify(exactly = 1) { accountRepository.getAccountByUserId(dummyUserId) }
        coVerify(exactly = 1) { cacheService.set(cacheKey, any(), 15) }
    }

    @Test
    fun `invalidateUserCache with special characters in username should work correctly`() = runTest {
        // arrange
        val specialUsername = "user@test-123_456"
        val accountCacheKey = CacheKeys.accountKey(specialUsername)
        val balanceCacheKey = CacheKeys.balanceKey(specialUsername)
        coEvery { cacheService.delete(accountCacheKey) } returns true
        coEvery { cacheService.delete(balanceCacheKey) } returns true

        // act
        accountService.invalidateUserCache(specialUsername)

        // assert
        coVerify(exactly = 1) { cacheService.delete(accountCacheKey) }
        coVerify(exactly = 1) { cacheService.delete(balanceCacheKey) }
    }
}
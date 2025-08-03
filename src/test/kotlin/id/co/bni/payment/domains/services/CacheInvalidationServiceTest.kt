package id.co.bni.payment.domains.services

import id.co.bni.payment.commons.constants.CacheKeys
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class CacheInvalidationServiceTest {

    @MockK
    private lateinit var cacheService: CacheService

    @MockK
    private lateinit var accountService: AccountService

    @InjectMockKs
    private lateinit var cacheInvalidationService: CacheInvalidationService

    @Test
    fun `invalidateTransactionAndUserCaches should invalidate all relevant caches successfully`() = runTest {
        // arrange
        val username = "testuser"
        coEvery { cacheService.deletePattern(CacheKeys.ALL_TRANSACTION_LISTS_PATTERN) } returns 5L
        coEvery { cacheService.deletePattern(CacheKeys.ALL_TRANSACTIONS_PATTERN) } returns 10L
        coEvery { cacheService.delete(CacheKeys.TRANSACTION_COUNT) } returns true
        coEvery { accountService.invalidateUserCache(username) } returns Unit

        // act
        cacheInvalidationService.invalidateTransactionAndUserCaches(username)

        // assert
        coVerify(exactly = 1) { cacheService.deletePattern(CacheKeys.ALL_TRANSACTION_LISTS_PATTERN) }
        coVerify(exactly = 1) { cacheService.deletePattern(CacheKeys.ALL_TRANSACTIONS_PATTERN) }
        coVerify(exactly = 1) { cacheService.delete(CacheKeys.TRANSACTION_COUNT) }
        coVerify(exactly = 1) { accountService.invalidateUserCache(username) }
    }

    @Test
    fun `invalidateTransactionAndUserCaches with empty username should handle gracefully`() = runTest {
        // arrange
        val username = ""
        coEvery { cacheService.deletePattern(CacheKeys.ALL_TRANSACTION_LISTS_PATTERN) } returns 0L
        coEvery { cacheService.deletePattern(CacheKeys.ALL_TRANSACTIONS_PATTERN) } returns 0L
        coEvery { cacheService.delete(CacheKeys.TRANSACTION_COUNT) } returns true
        coEvery { accountService.invalidateUserCache(username) } returns Unit

        // act
        cacheInvalidationService.invalidateTransactionAndUserCaches(username)

        // assert
        coVerify(exactly = 1) { cacheService.deletePattern(CacheKeys.ALL_TRANSACTION_LISTS_PATTERN) }
        coVerify(exactly = 1) { cacheService.deletePattern(CacheKeys.ALL_TRANSACTIONS_PATTERN) }
        coVerify(exactly = 1) { cacheService.delete(CacheKeys.TRANSACTION_COUNT) }
        coVerify(exactly = 1) { accountService.invalidateUserCache(username) }
    }

    @Test
    fun `invalidateTransactionAndUserCaches should succeed when some caches return zero deletions`() = runTest {
        // arrange
        val username = "testuser"
        coEvery { cacheService.deletePattern(CacheKeys.ALL_TRANSACTION_LISTS_PATTERN) } returns 0L // No transaction lists found
        coEvery { cacheService.deletePattern(CacheKeys.ALL_TRANSACTIONS_PATTERN) } returns 0L // No transactions found
        coEvery { cacheService.delete(CacheKeys.TRANSACTION_COUNT) } returns false // Count cache doesn't exist
        coEvery { accountService.invalidateUserCache(username) } returns Unit

        // act
        cacheInvalidationService.invalidateTransactionAndUserCaches(username)

        // assert
        coVerify(exactly = 1) { cacheService.deletePattern(CacheKeys.ALL_TRANSACTION_LISTS_PATTERN) }
        coVerify(exactly = 1) { cacheService.deletePattern(CacheKeys.ALL_TRANSACTIONS_PATTERN) }
        coVerify(exactly = 1) { cacheService.delete(CacheKeys.TRANSACTION_COUNT) }
        coVerify(exactly = 1) { accountService.invalidateUserCache(username) }
    }
}
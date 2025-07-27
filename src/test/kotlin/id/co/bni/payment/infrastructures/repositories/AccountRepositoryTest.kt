package id.co.bni.payment.infrastructures.repositories

import id.co.bni.payment.commons.constants.AccountStatus
import id.co.bni.payment.domains.entities.Account
import id.co.bni.payment.infrastructures.repositories.dao.AccountDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.*

@ExtendWith(MockKExtension::class)
class AccountRepositoryTest {

    @MockK
    private lateinit var accountDao: AccountDao

    @InjectMockKs
    private lateinit var accountRepository: AccountRepositoryImpl

    @Test
    fun `updateAccountBalance should return update count when successful`() = runTest {
        // Given
        val account = Account(
            id = "account-123",
            userId = 1L,
            balance = BigDecimal("1500.50"),
            currency = "IDR",
            accountStatus = AccountStatus.ACTIVE,
            createdAt = Instant.parse("2023-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2023-01-02T00:00:00Z")
        )
        
        coEvery { 
            accountDao.updateAccount(
                id = "account-123",
                userId = 1L,
                balance = BigDecimal("1500.50"),
                currency = "IDR",
                accountStatus = "ACTIVE",
                updatedAt = account.updatedAt
            ) 
        } returns 1

        // When
        val result = accountRepository.updateAccountBalance(account)

        // Then
        assertEquals(1, result)
        coVerify(exactly = 1) { 
            accountDao.updateAccount(
                id = "account-123",
                userId = 1L,
                balance = BigDecimal("1500.50"),
                currency = "IDR",
                accountStatus = "ACTIVE",
                updatedAt = account.updatedAt
            ) 
        }
    }

    @Test
    fun `updateAccountBalance should return zero when no rows updated`() = runTest {
        // Given
        val account = Account(
            id = "nonexistent-123",
            userId = 999L,
            balance = BigDecimal("100.00"),
            currency = "USD",
            accountStatus = AccountStatus.SUSPENDED,
            updatedAt = Instant.parse("2023-01-03T00:00:00Z")
        )
        
        coEvery { 
            accountDao.updateAccount(
                id = "nonexistent-123",
                userId = 999L,
                balance = BigDecimal("100.00"),
                currency = "USD",
                accountStatus = "SUSPENDED",
                updatedAt = account.updatedAt
            ) 
        } returns 0

        // When
        val result = accountRepository.updateAccountBalance(account)

        // Then
        assertEquals(0, result)
    }

    @Test
    fun `updateAccountBalance should handle different account statuses`() = runTest {
        // Given
        val suspendedAccount = Account(
            id = "account-456",
            userId = 2L,
            balance = BigDecimal("500.00"),
            currency = "IDR",
            accountStatus = AccountStatus.SUSPENDED,
            updatedAt = Instant.now()
        )
        
        coEvery { 
            accountDao.updateAccount(
                id = "account-456",
                userId = 2L,
                balance = BigDecimal("500.00"),
                currency = "IDR",
                accountStatus = "SUSPENDED",
                updatedAt = suspendedAccount.updatedAt
            ) 
        } returns 1

        // When
        val result = accountRepository.updateAccountBalance(suspendedAccount)

        // Then
        assertEquals(1, result)
        coVerify(exactly = 1) { 
            accountDao.updateAccount(
                id = "account-456",
                userId = 2L,
                balance = BigDecimal("500.00"),
                currency = "IDR",
                accountStatus = "SUSPENDED",
                updatedAt = suspendedAccount.updatedAt
            ) 
        }
    }

    @Test
    fun `updateAccountBalance should handle zero balance`() = runTest {
        // Given
        val zeroBalanceAccount = Account(
            id = "account-789",
            userId = 3L,
            balance = BigDecimal("0.00"),
            currency = "IDR",
            accountStatus = AccountStatus.ACTIVE,
            updatedAt = Instant.now()
        )
        
        coEvery { 
            accountDao.updateAccount(
                id = "account-789",
                userId = 3L,
                balance = BigDecimal("0.00"),
                currency = "IDR",
                accountStatus = "ACTIVE",
                updatedAt = zeroBalanceAccount.updatedAt
            ) 
        } returns 1

        // When
        val result = accountRepository.updateAccountBalance(zeroBalanceAccount)

        // Then
        assertEquals(1, result)
    }

    @Test
    fun `getAccountByUserId should return account when found`() = runTest {
        // Given
        val userId = 1L
        val expectedAccount = Account(
            id = "account-123",
            userId = userId,
            balance = BigDecimal("2500.75"),
            currency = "IDR",
            accountStatus = AccountStatus.ACTIVE,
            createdAt = Instant.parse("2023-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2023-01-02T00:00:00Z")
        )
        
        coEvery { accountDao.findByUserId(userId) } returns expectedAccount

        // When
        val result = accountRepository.getAccountByUserId(userId)

        // Then
        assertEquals("account-123", result?.id)
        assertEquals(1L, result?.userId)
        assertEquals(BigDecimal("2500.75"), result?.balance)
        assertEquals("IDR", result?.currency)
        assertEquals(AccountStatus.ACTIVE, result?.accountStatus)
        assertEquals(Instant.parse("2023-01-01T00:00:00Z"), result?.createdAt)
        assertEquals(Instant.parse("2023-01-02T00:00:00Z"), result?.updatedAt)
        
        coVerify(exactly = 1) { accountDao.findByUserId(userId) }
    }

    @Test
    fun `getAccountByUserId should return null when not found`() = runTest {
        // Given
        val userId = 999L
        coEvery { accountDao.findByUserId(userId) } returns null

        // When
        val result = accountRepository.getAccountByUserId(userId)

        // Then
        assertNull(result)
        coVerify(exactly = 1) { accountDao.findByUserId(userId) }
    }

    @Test
    fun `getAccountByUserId should handle different currencies`() = runTest {
        // Given
        val userId = 2L
        val usdAccount = Account(
            id = "account-usd",
            userId = userId,
            balance = BigDecimal("1000.00"),
            currency = "USD",
            accountStatus = AccountStatus.ACTIVE,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        coEvery { accountDao.findByUserId(userId) } returns usdAccount

        // When
        val result = accountRepository.getAccountByUserId(userId)

        // Then
        assertEquals("USD", result?.currency)
        assertEquals(BigDecimal("1000.00"), result?.balance)
    }

    @Test
    fun `getAccountByUserId should handle suspended accounts`() = runTest {
        // Given
        val userId = 3L
        val suspendedAccount = Account(
            id = "account-suspended",
            userId = userId,
            balance = BigDecimal("750.25"),
            currency = "IDR",
            accountStatus = AccountStatus.SUSPENDED,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        coEvery { accountDao.findByUserId(userId) } returns suspendedAccount

        // When
        val result = accountRepository.getAccountByUserId(userId)

        // Then
        assertEquals(AccountStatus.SUSPENDED, result?.accountStatus)
        assertEquals("account-suspended", result?.id)
        assertEquals(3L, result?.userId)
    }
}
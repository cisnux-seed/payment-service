package id.co.bni.payment.infrastructures.repositories.dao

import id.co.bni.payment.domains.entities.Account
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.relational.core.sql.LockMode
import org.springframework.data.relational.repository.Lock
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant

@Component
interface AccountDao : CoroutineCrudRepository<Account, String> {
    @Lock(LockMode.PESSIMISTIC_WRITE)
    suspend fun findByUserId(userId: Long): Account?

    @Modifying
    @Query("""
        UPDATE accounts 
        SET user_id = :userId, 
            balance = :balance, 
            currency = :currency, 
            account_status = :accountStatus::account_status_enum,
            updated_at = :updatedAt 
        WHERE id = :id
    """)
    suspend fun updateAccount(
        id: String,
        userId: Long,
        balance: BigDecimal,
        currency: String,
        accountStatus: String,
        updatedAt: Instant
    ): Int
}
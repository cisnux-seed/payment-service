package id.co.bni.payment.infrastructures.repositories.dao

import id.co.bni.payment.domains.entities.Account
import org.springframework.data.relational.core.sql.LockMode
import org.springframework.data.relational.repository.Lock
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Component

@Component
interface AccountDao : CoroutineCrudRepository<Account, String> {
    @Lock(LockMode.PESSIMISTIC_WRITE)
    suspend fun findByUserId(userId: Long): Account?
}
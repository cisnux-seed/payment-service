package id.co.bni.payment.infrastructures.repositories.dao

import id.co.bni.payment.domains.entities.User
import org.springframework.data.relational.core.sql.LockMode
import org.springframework.data.relational.repository.Lock
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Component

@Component
interface UserDao : CoroutineCrudRepository<User, Long> {
    @Lock(LockMode.PESSIMISTIC_WRITE)
    suspend fun findByUsername(username: String): User?
}
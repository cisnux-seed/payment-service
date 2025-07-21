package id.co.bni.payment.infrastructures.repositories

import id.co.bni.payment.domains.entities.User
import id.co.bni.payment.domains.repositories.UserRepository
import id.co.bni.payment.infrastructures.repositories.dao.UserDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryImpl(
    private val userDAO: UserDao
) : UserRepository {
    override suspend fun findByUsername(username: String): User? = withContext(Dispatchers.IO) {
        userDAO.findByUsername(username)
    }
}
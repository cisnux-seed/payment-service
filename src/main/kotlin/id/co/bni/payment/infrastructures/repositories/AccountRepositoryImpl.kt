package id.co.bni.payment.infrastructures.repositories

import id.co.bni.payment.domains.entities.Account
import id.co.bni.payment.domains.repositories.AccountRepository
import id.co.bni.payment.infrastructures.repositories.dao.AccountDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Repository

@Repository
class AccountRepositoryImpl(
    private val accountDao: AccountDao
) : AccountRepository {
    override suspend fun updateAccountBalance(account: Account): Int = withContext(Dispatchers.IO) {
        accountDao.updateAccount(
            id = account.id,
            userId = account.userId,
            balance = account.balance,
            currency = account.currency,
            accountStatus = account.accountStatus.name,
            updatedAt = account.updatedAt
        )
    }


    override suspend fun getAccountByUserId(userId: Long): Account? = withContext(Dispatchers.IO) {
        accountDao.findByUserId(userId)
    }
}
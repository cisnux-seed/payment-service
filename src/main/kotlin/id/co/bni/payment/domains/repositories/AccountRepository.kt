package id.co.bni.payment.domains.repositories

import id.co.bni.payment.domains.entities.Account

interface AccountRepository {
    suspend fun updateAccountBalance(account: Account): Int
    suspend fun getAccountByUserId(userId: Long): Account?
}
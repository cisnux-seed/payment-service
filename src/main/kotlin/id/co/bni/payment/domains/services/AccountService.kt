package id.co.bni.payment.domains.services

import id.co.bni.payment.domains.dtos.AccountResponse
import id.co.bni.payment.domains.dtos.BalanceResponse
import id.co.bni.payment.domains.entities.Account

interface AccountService {
    suspend fun updateAccountBalance(account: Account): Int
    suspend fun getAccountByUsername(username: String): AccountResponse?
    suspend fun getBalanceByUsername(username: String): BalanceResponse?
}
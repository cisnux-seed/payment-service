package id.co.bni.payment.domains.services

import id.co.bni.payment.domains.dtos.AccountResponse
import id.co.bni.payment.domains.dtos.BalanceResponse

interface AccountService {
    suspend fun getAccountByUsername(username: String): AccountResponse?
    suspend fun getBalanceByUsername(username: String): BalanceResponse?
}
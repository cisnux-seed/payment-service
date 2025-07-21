package id.co.bni.payment.domains.repositories

import id.co.bni.payment.domains.dtos.EWalletBalanceResponse
import id.co.bni.payment.domains.dtos.TopUpEWalletRequest
import id.co.bni.payment.domains.dtos.TransactionResponse

interface EWalletRepository {
    suspend fun getEWalletBalanceByUsername(username: String): EWalletBalanceResponse?
    suspend fun topUpEWallet(topUpEWalletRequest: TopUpEWalletRequest): TransactionResponse
}
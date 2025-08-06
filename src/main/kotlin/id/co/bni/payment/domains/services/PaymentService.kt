package id.co.bni.payment.domains.services

import id.co.bni.payment.domains.dtos.EWalletBalanceResponse
import id.co.bni.payment.domains.dtos.TopUpEWalletRequest
import id.co.bni.payment.domains.dtos.TransactionResponse

interface PaymentService {
    suspend fun getEWalletBalanceByUsername(username: String, walletType: String): EWalletBalanceResponse?
    suspend fun topUpEWallet(username: String, topUpEWalletRequest: TopUpEWalletRequest, onFailedTrx: ()->Unit = {}): TransactionResponse
}
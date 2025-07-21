package id.co.bni.payment.infrastructures.repositories

import id.co.bni.payment.commons.constants.PaymentMethod
import id.co.bni.payment.commons.constants.TransactionStatus
import id.co.bni.payment.commons.constants.TransactionType
import id.co.bni.payment.domains.dtos.EWalletBalanceResponse
import id.co.bni.payment.domains.dtos.TopUpEWalletRequest
import id.co.bni.payment.domains.dtos.TransactionResponse
import id.co.bni.payment.domains.repositories.EWalletRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
class EWalletRepositoryImpl : EWalletRepository {
    override suspend fun getEWalletBalanceByUsername(username: String): EWalletBalanceResponse? {
        return EWalletBalanceResponse(
            provider = "GOPAY",
            balance = BigDecimal("100000.00"),
            currency = "IDR",
            accountNumber = "1234567890",
            lastUpdated = LocalDateTime.now()
        )
    }

    override suspend fun topUpEWallet(topUpEWalletRequest: TopUpEWalletRequest): TransactionResponse {
        return TransactionResponse(
            id = "trmcckcococo123123123",
            transactionId = "TXN123456",
            transactionType = TransactionType.TOPUP,
            transactionStatus = TransactionStatus.SUCCESS,
            amount = BigDecimal("40000.00"),
            balanceBefore = BigDecimal("100000.00"),
            balanceAfter = BigDecimal("60000.00"),
            currency = "IDR",
            paymentMethod = PaymentMethod.GOPAY,
            description = "Top up e-wallet gopay is success",
            createdAt = LocalDateTime.now()
        )
    }
}
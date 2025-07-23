package id.co.bni.payment.domains.dtos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GopayTopUpResp(
    val status: String,
    @SerialName("transaction_id")
    val transactionId: String,
    @SerialName("wallet_id")
    val walletId: String,
    val amount: Double,
    @SerialName("new_balance")
    val newBalance: Double,
    @SerialName("transaction_time")
    val transactionTime: String
)
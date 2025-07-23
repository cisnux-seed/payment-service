package id.co.bni.payment.domains.dtos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetShopeePayWalletResp(
    val result: String,
    @SerialName("request_id")
    val requestId: String,
    @SerialName("balance_info")
    val balanceInfo: BalanceInfo
)

@Serializable
data class BalanceInfo(
    @SerialName("user_id")
    val userId: String,
    @SerialName("available_balance")
    val availableBalance: Double,
    @SerialName("pending_balance")
    val pendingBalance: Double,
    @SerialName("currency_code")
    val currencyCode: String,
    @SerialName("last_transaction_date")
    val lastTransactionDate: String
)

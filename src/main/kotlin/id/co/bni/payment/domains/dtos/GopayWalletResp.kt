package id.co.bni.payment.domains.dtos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GopayWalletResp(
    @SerialName("wallet_id")
    val walletId: String,
    val balance: Double,
    val currency: String = "IDR",
    @SerialName("last_updated")
    val lastUpdated: String
)

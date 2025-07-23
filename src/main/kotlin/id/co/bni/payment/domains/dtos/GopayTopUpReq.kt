package id.co.bni.payment.domains.dtos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GopayTopUpReq(
    @SerialName("wallet_id")
    val walletId: String,
    val amount: Double,
    @SerialName("payment_method")
    val paymentMethod: String,
    @SerialName("reference_id")
    val referenceId: String
)
package id.co.bni.payment.domains.dtos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShopeePayTopUpResp(
    val result: String,
    @SerialName("request_id")
    val requestId: String,
    @SerialName("topup_details")
    val topupDetails: TopUpDetails
)

@Serializable
data class TopUpDetails(
    @SerialName("transaction_id")
    val transactionId: String,
    @SerialName("user_id")
    val userId: String,
    val amount: Double,
    val status: String,
    @SerialName("created_at")
    val createdAt: String
)
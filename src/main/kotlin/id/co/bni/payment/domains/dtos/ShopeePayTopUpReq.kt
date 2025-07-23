package id.co.bni.payment.domains.dtos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShopeePayTopUpReq(
    @SerialName("user_id")
    val userId: String,
    @SerialName("topup_amount")
    val topupAmount: Long,
    @SerialName("source_type")
    val sourceType: String,
    @SerialName("source_details")
    val sourceDetails: SourceDetails,
    @SerialName("request_id")
    val requestId: String
)

@Serializable
data class SourceDetails(
    @SerialName("bank_code")
    val bankCode: String,
    @SerialName("account_number")
    val accountNumber: String
)

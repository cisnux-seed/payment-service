package id.co.bni.payment.domains.dtos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetShopeePayWalletReq(
    @SerialName("user_id")
    val userId: String,
    @SerialName("request_id")
    val requestId: String,
    @SerialName("include_details")
    val includeDetails: Boolean = true
)

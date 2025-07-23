package id.co.bni.payment.domains.dtos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShopeePayAuthReq(
    @SerialName("merchant_id")
    val merchantId: String,
    @SerialName("api_key")
    val apiKey: String,
    val signature: String,
    val timestamp: Long
)
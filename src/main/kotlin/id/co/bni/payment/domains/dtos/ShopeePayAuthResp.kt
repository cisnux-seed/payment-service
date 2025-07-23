package id.co.bni.payment.domains.dtos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShopeePayAuthResp(
    val success: Boolean,
    val token: String,
    @SerialName("valid_until")
    val validUntil: Long,
    @SerialName("merchant_info")
    val merchantInfo: MerchantInfo
)

@Serializable
data class MerchantInfo(
    @SerialName("merchant_id")
    val merchantId: String,
    @SerialName("merchant_name")
    val merchantName: String
)
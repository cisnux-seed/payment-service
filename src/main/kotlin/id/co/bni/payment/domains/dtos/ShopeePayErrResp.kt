package id.co.bni.payment.domains.dtos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShopeePayErrResp(
    @SerialName("error_code")
    val errorCode: String,
    @SerialName("error_message")
    val errorMessage: String,
    val success: Boolean,
    val timestamp: Long
)

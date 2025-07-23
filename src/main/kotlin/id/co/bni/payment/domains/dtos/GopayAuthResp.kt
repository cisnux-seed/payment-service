package id.co.bni.payment.domains.dtos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GopayAuthResp(
    val success: Boolean,
    val token: String,
    @SerialName("valid_until")
    val validUntil: Long,
    @SerialName("client_info")
    val clientInfo: ClientInfo
)

@Serializable
data class ClientInfo(
    @SerialName("client_id")
    val clientId: String,
    @SerialName("client_name")
    val clientName: String
)

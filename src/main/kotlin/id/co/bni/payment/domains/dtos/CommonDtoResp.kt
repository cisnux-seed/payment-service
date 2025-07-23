package id.co.bni.payment.domains.dtos

import kotlinx.serialization.Serializable

@Serializable
data class CommonDtoResp<out T>(
    val status: String,
    val data: T
)
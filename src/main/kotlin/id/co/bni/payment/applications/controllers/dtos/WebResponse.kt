package id.co.bni.payment.applications.controllers.dtos

data class WebResponse<out T>(
    val meta: MetaResponse,
    val data: T? = null,
)

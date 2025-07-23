package id.co.bni.payment.infrastructures.repositories

import id.co.bni.payment.commons.exceptions.APIException
import id.co.bni.payment.commons.loggable.Loggable
import id.co.bni.payment.domains.dtos.CommonDtoResp
import id.co.bni.payment.domains.dtos.GopayTopUpReq
import id.co.bni.payment.domains.dtos.GopayTopUpResp
import id.co.bni.payment.domains.dtos.GopayWalletResp
import id.co.bni.payment.domains.repositories.GopayRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Repository

@Repository
class GopayRepositoryImpl(
    @Qualifier("gopayHttpClient")
    private val httpClient: HttpClient,
) : GopayRepository, Loggable {
    @Value("\${GOPAY_BASE_URL}")
    private lateinit var gopayBaseUrl: String

    override suspend fun getBalanceByWalletId(walletId: String): CommonDtoResp<GopayWalletResp> = withContext(
        Dispatchers.IO
    ) {
        try {
            val response = httpClient.get("$gopayBaseUrl/wallet/balance") {
                url {
                    parameter("wallet_id", walletId)
                }
            }
            if (response.status.isSuccess()) {
                val gopayWalletResp = response.body<CommonDtoResp<GopayWalletResp>>()
                gopayWalletResp
            } else {
                throw APIException.HTTP_CLIENT_FAILURES[response.status] ?: APIException.InternalServerException(
                    statusCode = response.status.value,
                    message = "Failed to retrieve balance for walletId: $walletId"
                )
            }
        } catch (e: Exception) {
            log.error(e.message, e)
            throw APIException.InternalServerException(
                statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                message = "Error retrieving balance for walletId: $walletId",
            )
        }
    }

    override suspend fun topUp(topUpReq: GopayTopUpReq): GopayTopUpResp? = withContext(
        Dispatchers.IO
    ) {
        try {
            val response = httpClient.post("$gopayBaseUrl/wallet/topup"){
                contentType(ContentType.Application.Json)
                setBody(topUpReq)
            }
            if (response.status.isSuccess()) {
                val gopayWalletResp = response.body<GopayTopUpResp>()
                gopayWalletResp
            } else {
                null
            }
        } catch (e: Exception) {
            log.error(e.message, e)
            throw APIException.InternalServerException(
                statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                message = "Error top up for walletId: ${topUpReq.walletId}",
            )
        }
    }
}
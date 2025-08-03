package id.co.bni.payment.infrastructures.repositories

import id.co.bni.payment.commons.exceptions.APIException
import id.co.bni.payment.commons.loggable.Loggable
import id.co.bni.payment.domains.dtos.GetShopeePayWalletReq
import id.co.bni.payment.domains.dtos.GetShopeePayWalletResp
import id.co.bni.payment.domains.dtos.ShopeePayErrResp
import id.co.bni.payment.domains.dtos.ShopeePayTopUpReq
import id.co.bni.payment.domains.dtos.ShopeePayTopUpResp
import id.co.bni.payment.domains.repositories.ShopeePayRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
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
class ShopeePayRepositoryImpl(
    @Qualifier("shopeePayHttpClient")
    private val httpClient: HttpClient,
) : ShopeePayRepository, Loggable {
    @Value("\${SHOPEE_PAY_BASE_URL}")
    private lateinit var shopeePayBaseUrl: String

    override suspend fun getShopeePayBalance(req: GetShopeePayWalletReq): GetShopeePayWalletResp = withContext(
        Dispatchers.IO
    ) {
        try {
            val response = httpClient.post("$shopeePayBaseUrl/account/balance") {
                contentType(ContentType.Application.Json)
                setBody(req)
            }
            if (response.status.isSuccess()) {
                val gopayWalletResp = response.body<GetShopeePayWalletResp>()
                gopayWalletResp
            } else {
                throw APIException.HTTP_CLIENT_FAILURES[response.status]?.apply {
                    val shopeePayErrResp = response.body<ShopeePayErrResp>()
                    message = shopeePayErrResp.errorMessage
                } ?: APIException.InternalServerException(
                    statusCode = response.status.value,
                    message = "Failed to retrieve balance for wallet: $req"
                )
            }
        } catch (e: Exception) {
            log.error(e.message, e)
            throw APIException.InternalServerException(
                statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                message = "Error retrieving balance for wallet: $req",
            )
        }
    }


    override suspend fun topUp(req: ShopeePayTopUpReq): ShopeePayTopUpResp = withContext(
        Dispatchers.IO
    ) {
        try {
            val response = httpClient.post("$shopeePayBaseUrl/wallet/topup") {
                contentType(ContentType.Application.Json)
                setBody(req)
            }
            if (response.status.isSuccess()) {
                val gopayWalletResp = response.body<ShopeePayTopUpResp>()
                gopayWalletResp
            } else {
                throw APIException.HTTP_CLIENT_FAILURES[response.status]?.apply {
                    val shopeePayErrResp = response.body<ShopeePayErrResp>()
                    message = shopeePayErrResp.errorMessage
                } ?: APIException.InternalServerException(
                    statusCode = response.status.value,
                    message = "Failed to top up: $req"
                )
            }
        } catch (e: Exception) {
            log.error(e.message, e)
            throw APIException.InternalServerException(
                statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                message = "Error top up balance for wallet: $req",
            )
        }
    }
}
package id.co.bni.payment.commons.configs

import id.co.bni.payment.commons.loggable.Loggable
import id.co.bni.payment.domains.dtos.GopayAuthRequest
import id.co.bni.payment.domains.dtos.GopayAuthResp
import id.co.bni.payment.domains.dtos.ShopeePayAuthReq
import id.co.bni.payment.domains.dtos.ShopeePayAuthResp
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.DEFAULT
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.springframework.beans.factory.annotation.Value
import java.time.Instant

@Configuration
class HttpClientConfig : Loggable {
    private val gopayBearerTokenStorage = mutableListOf<BearerTokens>()
    private val shopeePayBearerTokenStorage = mutableListOf<BearerTokens>()

    @Value("\${GOPAY_BASE_URL}")
    private lateinit var gopayBaseUrl: String

    @Value("\${GOPAY_CLIENT_ID}")
    private lateinit var gopayClientId: String

    @Value("\${GOPAY_CLIENT_SECRET}")
    private lateinit var gopayClientSecret: String

    @Value("\${GOPAY_SIGNATURE}")
    private lateinit var gopaySignature: String

    @Value("\${SHOPEE_PAY_BASE_URL}")
    private lateinit var shopePayBaseUrl: String

    @Value("\${SHOPEE_PAY_MERCHANT_ID}")
    private lateinit var shopeePayMerchantId: String

    @Value("\${SHOPEE_PAY_API_KEY}")
    private lateinit var shopeePayApiKey: String

    @Value("\${SHOPEE_PAY_SIGNATURE}")
    private lateinit var shopeePaySignature: String

    @Bean("shopeePayHttpClient")
    fun shopeePayHttpClient(): HttpClient = HttpClient(CIO) {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }

        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 2)
            exponentialDelay()

            retryIf(maxRetries = 1) { request, response ->
                response.status == HttpStatusCode.Unauthorized &&
                        !request.url.toString().contains("/authentication")
            }

            modifyRequest { request ->
                if (response?.status == HttpStatusCode.Unauthorized) {
                    log.info("shopee pay refreshing token due to 401 response")
                    // Refresh token and update the request
                    runBlocking {
                        val newToken = fetchShopeePayNewToken()
                        request.headers["Authorization"] = "Bearer ${newToken.accessToken}"
                    }
                }
            }
        }

        install(DefaultRequest) {
            if (!url.toString().contains("/authentication")) {
                runBlocking {
                    val token = getShopeePayCurrentToken()
                    headers["Authorization"] = "Bearer $token"
                }
            }
        }

        HttpResponseValidator {
            handleResponseExceptionWithRequest { exception, request ->
                if (exception is ClientRequestException &&
                    exception.response.status == HttpStatusCode.Unauthorized &&
                    !request.url.toString().contains("/authentication")
                ) {
                    log.warn("shopee pay got 401 for ${request.url}, clearing tokens for retry")
                    shopeePayBearerTokenStorage.clear()
                    throw exception
                }
            }
        }
    }

    @Bean("gopayHttpClient")
    fun gopayHttpClient(): HttpClient = HttpClient(CIO) {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }

        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 2)
            exponentialDelay()

            retryIf(maxRetries = 1) { request, response ->
                response.status == HttpStatusCode.Unauthorized &&
                        !request.url.toString().contains("/authentication")
            }

            modifyRequest { request ->
                if (response?.status == HttpStatusCode.Unauthorized) {
                    log.info("gopay refreshing token due to 401 response")
                    runBlocking {
                        val newToken = fetchGopayNewToken()
                        request.headers["Authorization"] = "Bearer ${newToken.accessToken}"
                    }
                } else if (response?.status == HttpStatusCode.Forbidden) {
                    log.info("gopay refreshing token due to 403 response")
                    runBlocking {
                        val newToken = fetchGopayNewToken()
                        request.headers["Authorization"] = "Bearer ${newToken.accessToken}"
                    }
                }
            }
        }

        install(DefaultRequest) {
            if (!url.toString().contains("/authentication")) {
                runBlocking {
                    val token = getGopayCurrentToken()
                    headers["Authorization"] = "Bearer $token"
                }
            }
        }

        HttpResponseValidator {
            handleResponseExceptionWithRequest { exception, request ->
                if (exception is ClientRequestException &&
                    exception.response.status == HttpStatusCode.Unauthorized &&
                    !request.url.toString().contains("/authentication")
                ) {
                    log.warn("gopay got 401 for ${request.url}, clearing tokens for retry")
                    gopayBearerTokenStorage.clear()
                    throw exception
                }
            }
        }
    }

    private suspend fun fetchGopayNewToken(): BearerTokens {
        val authClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                    }
                )
            }
        }

        try {
            val response = authClient.post("$gopayBaseUrl/auth/token") {
                contentType(ContentType.Application.Json)
                setBody(
                    GopayAuthRequest(
                        clientId = gopayClientId,
                        clientSecret = gopayClientSecret,
                        signature = gopaySignature,
                        timestamp = Instant.now().toEpochMilli()
                    )
                )
            }

            if (response.status.isSuccess()) {
                val authResponse = response.body<GopayAuthResp>()
                val bearerTokens = BearerTokens(authResponse.token, null)

                // Store the new token
                gopayBearerTokenStorage.clear()
                gopayBearerTokenStorage.add(bearerTokens)

                return bearerTokens
            } else {
                throw Exception("Failed to authenticate: ${response.status}")
            }
        } catch (e: Exception) {
            throw Exception("Error fetching auth token", e)
        } finally {
            authClient.close()
        }
    }

    private suspend fun fetchShopeePayNewToken(): BearerTokens {
        val authClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                    }
                )
            }
        }

        try {
            val response = authClient.post("$shopePayBaseUrl/authentication") {
                contentType(ContentType.Application.Json)
                setBody(
                    ShopeePayAuthReq(
                        merchantId = shopeePayMerchantId,
                        apiKey = shopeePayApiKey,
                        signature = shopeePaySignature,
                        timestamp = Instant.now().toEpochMilli()
                    )
                )
            }

            if (response.status.isSuccess()) {
                val authResponse = response.body<ShopeePayAuthResp>()
                val bearerTokens = BearerTokens(authResponse.token, null)

                // Store the new token
                shopeePayBearerTokenStorage.clear()
                shopeePayBearerTokenStorage.add(bearerTokens)

                return bearerTokens
            } else {
                throw Exception("Failed to authenticate: ${response.status}")
            }
        } catch (e: Exception) {
            throw Exception("Error fetching auth token", e)
        } finally {
            authClient.close()
        }
    }

    private suspend fun getShopeePayCurrentToken(): String {
        return shopeePayBearerTokenStorage.firstOrNull()?.accessToken
            ?: fetchShopeePayNewToken().accessToken
    }

    private suspend fun getGopayCurrentToken(): String {
        return shopeePayBearerTokenStorage.firstOrNull()?.accessToken
            ?: fetchGopayNewToken().accessToken
    }
}
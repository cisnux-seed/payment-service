package id.co.bni.payment.commons.configs

import id.co.bni.payment.commons.loggable.Loggable
import id.co.bni.payment.domains.dtos.GopayAuthRequest
import id.co.bni.payment.domains.dtos.GopayAuthResp
import id.co.bni.payment.domains.dtos.ShopeePayAuthReq
import id.co.bni.payment.domains.dtos.ShopeePayAuthResp
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
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
import jakarta.annotation.PostConstruct
import kotlinx.serialization.json.Json
import org.springframework.beans.factory.annotation.Value
import java.time.Instant

@Configuration
class HttpClientConfig: Loggable {
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

    @PostConstruct
    fun init() {
        // Initialize the token storage
        log.info("gopay client initialized with clientId: $gopayClientId")
        log.info("gopay client initialized with clientSecret: $gopayClientSecret")
        log.info("gopay client initialized with signature: $gopaySignature")
        log.info("gopay base URL: $gopayBaseUrl")
        log.info("shopeePay client initialized with merchantId: $shopeePayMerchantId")
        log.info("shopeePay client initialized with apiKey: $shopeePayApiKey")
        log.info("shopeePay client initialized with signature: $shopeePaySignature")
        log.info("shopeePay base URL: $shopePayBaseUrl")
    }

    @Bean("shopeePayHttpClient")
    fun shopeePayHttpClient(): HttpClient = HttpClient(CIO) {
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
        install(Auth) {
            bearer {
                loadTokens {
                    shopeePayBearerTokenStorage.firstOrNull() ?: fetchShopeePayNewToken()
                }
                refreshTokens {
                    fetchShopeePayNewToken()
                }
                sendWithoutRequest { request ->
                    // Send credentials with requests to the auth endpoint
                    request.url.host.contains("shopeepay") && !request.url.toString().contains("/authentication")
                }
            }
        }
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 2)
            retryIf(maxRetries = 1) { request, response ->
                response.status == HttpStatusCode.Unauthorized
            }
            modifyRequest { request ->
                // Clear stored token on 401 so it will be refreshed
                if (response?.status == HttpStatusCode.Unauthorized) {
                    shopeePayBearerTokenStorage.clear()
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
        install(Auth) {
            bearer {
                loadTokens {
                    gopayBearerTokenStorage.firstOrNull() ?: fetchGopayNewToken()
                }
                refreshTokens {
                    fetchGopayNewToken()
                }
                sendWithoutRequest { request ->
                    // Send credentials with requests to the auth endpoint
                    request.url.host.contains("gopay") && !request.url.toString().contains("/auth/token")
                }
            }
        }
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 2)
            retryIf(maxRetries = 1) { request, response ->
                response.status == HttpStatusCode.Unauthorized
            }
            modifyRequest { request ->
                // Clear stored token on 401 so it will be refreshed
                if (response?.status == HttpStatusCode.Unauthorized) {
                    gopayBearerTokenStorage.clear()
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
}
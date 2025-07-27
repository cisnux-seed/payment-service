package id.co.bni.payment.infrastructures.repositories

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

open class BaseRepositoryTest {
    private val mockConfig = MockEngineConfig()

    fun mockHandler(handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): HttpClient {
        mockConfig.addHandler(handler)
        val mockEngine = MockEngine(config = mockConfig)
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                    }
                )
            }
        }
        return client
    }
}
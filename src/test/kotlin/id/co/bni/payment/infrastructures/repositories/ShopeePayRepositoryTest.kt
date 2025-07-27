package id.co.bni.payment.infrastructures.repositories

import id.co.bni.payment.domains.dtos.GetShopeePayWalletReq
import id.co.bni.payment.domains.dtos.ShopeePayTopUpReq
import id.co.bni.payment.domains.dtos.SourceDetails
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.util.ReflectionTestUtils

@ExtendWith(MockKExtension::class)
class ShopeePayRepositoryTest : BaseRepositoryTest(){
    private val shopeePayBaseUrl = "https://api.shopeepay.com"

    // Mock request data
    private val dummyUserId = "user123"
    private val dummyRequestId = "req123"
    
    private val dummyGetBalanceReq = GetShopeePayWalletReq(
        userId = dummyUserId,
        requestId = dummyRequestId,
        includeDetails = true
    )

    private val dummyTopUpReq = ShopeePayTopUpReq(
        userId = dummyUserId,
        topupAmount = 50000L,
        sourceType = "BANK_TRANSFER",
        sourceDetails = SourceDetails(
            bankCode = "BNI",
            accountNumber = "1234567890"
        ),
        requestId = dummyRequestId
    )

    // JSON responses
    private val successfulBalanceResponseJson = """
        {
            "result": "SUCCESS",
            "request_id": "$dummyRequestId",
            "balance_info": {
                "user_id": "$dummyUserId",
                "available_balance": 100000.0,
                "pending_balance": 5000.0,
                "currency_code": "IDR",
                "last_transaction_date": "2024-01-01T10:00:00Z"
            }
        }
    """.trimIndent()

    private val successfulTopUpResponseJson = """
        {
            "result": "SUCCESS",
            "request_id": "$dummyRequestId",
            "topup_details": {
                "transaction_id": "txn123",
                "user_id": "$dummyUserId",
                "amount": 50000.0,
                "status": "COMPLETED",
                "created_at": "2024-01-01T10:30:00Z"
            }
        }
    """.trimIndent()

    private val errorResponseJson = """
        {
            "error_code": "WALLET_NOT_FOUND",
            "error_message": "The specified wallet does not exist",
            "success": false,
            "timestamp": 1704110400000
        }
    """.trimIndent()

    @Nested
    @DisplayName("when get ShopeePay balance")
    inner class GetShopeePayBalance {

        @Test
        fun `with valid request then should return success response`() = runTest {
            // arrange
            val shopeePayRepository = ShopeePayRepositoryImpl(
                httpClient = mockHandler {
                    respond(
                        content = successfulBalanceResponseJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            )
            ReflectionTestUtils.setField(shopeePayRepository, "shopeePayBaseUrl", shopeePayBaseUrl)

            // act
            val result = shopeePayRepository.getShopeePayBalance(dummyGetBalanceReq)

            // assert
            assertEquals("SUCCESS", result.result)
            assertEquals(dummyRequestId, result.requestId)
            assertEquals(dummyUserId, result.balanceInfo.userId)
            assertEquals(100000.0, result.balanceInfo.availableBalance)
            assertEquals(5000.0, result.balanceInfo.pendingBalance)
            assertEquals("IDR", result.balanceInfo.currencyCode)
        }

        @Test
        fun `with invalid request then should throw APIException`() = runTest {
            // arrange
            val shopeePayRepository = ShopeePayRepositoryImpl(
                httpClient = mockHandler {
                    respond(
                        content = errorResponseJson,
                        status = HttpStatusCode.BadRequest,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            )
            ReflectionTestUtils.setField(shopeePayRepository, "shopeePayBaseUrl", shopeePayBaseUrl)

            // act & assert
            assertThrows(IllegalStateException::class.java) {
                runTest {
                    shopeePayRepository.getShopeePayBalance(dummyGetBalanceReq)
                }
            }
        }

        @Test
        fun `with valid request but server error then should throw InternalServerException`() = runTest {
            // arrange
            val shopeePayRepository = ShopeePayRepositoryImpl(
                httpClient = mockHandler {
                    respond(
                        content = "Internal Server Error",
                        status = HttpStatusCode.InternalServerError,
                        headers = headersOf(HttpHeaders.ContentType, "text/plain")
                    )
                }
            )
            ReflectionTestUtils.setField(shopeePayRepository, "shopeePayBaseUrl", shopeePayBaseUrl)

            // act & assert
            assertThrows(IllegalStateException::class.java) {
                runTest {
                    shopeePayRepository.getShopeePayBalance(dummyGetBalanceReq)
                }
            }
        }

        @Test
        fun `with network exception then should throw InternalServerException`() = runTest {
            // arrange
            val shopeePayRepository = ShopeePayRepositoryImpl(
                httpClient = mockHandler {
                    throw RuntimeException("Network error")
                }
            )
            ReflectionTestUtils.setField(shopeePayRepository, "shopeePayBaseUrl", shopeePayBaseUrl)

            // act & assert
            assertThrows(IllegalStateException::class.java) {
                runTest {
                    shopeePayRepository.getShopeePayBalance(dummyGetBalanceReq)
                }
            }
        }
    }

    @Nested
    @DisplayName("when top up ShopeePay wallet")
    inner class TopUpShopeePayWallet {

        @Test
        fun `with valid request then should return success response`() = runTest {
            // arrange
            val shopeePayRepository = ShopeePayRepositoryImpl(
                httpClient = mockHandler {
                    respond(
                        content = successfulTopUpResponseJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            )
            ReflectionTestUtils.setField(shopeePayRepository, "shopeePayBaseUrl", shopeePayBaseUrl)

            // act
            val result = shopeePayRepository.topUp(dummyTopUpReq)

            // assert
            assertEquals("SUCCESS", result.result)
            assertEquals(dummyRequestId, result.requestId)
            assertEquals("txn123", result.topupDetails.transactionId)
            assertEquals(dummyUserId, result.topupDetails.userId)
            assertEquals(50000.0, result.topupDetails.amount)
            assertEquals("COMPLETED", result.topupDetails.status)
        }

        @Test
        fun `with invalid request then should throw APIException`() = runTest {
            // arrange
            val shopeePayRepository = ShopeePayRepositoryImpl(
                httpClient = mockHandler {
                    respond(
                        content = errorResponseJson,
                        status = HttpStatusCode.BadRequest,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            )
            ReflectionTestUtils.setField(shopeePayRepository, "shopeePayBaseUrl", shopeePayBaseUrl)

            // act & assert
            assertThrows(IllegalStateException::class.java) {
                runTest {
                    shopeePayRepository.topUp(dummyTopUpReq)
                }
            }
        }

        @Test
        fun `with valid request but unauthorized then should throw APIException`() = runTest {
            // arrange
            val shopeePayRepository = ShopeePayRepositoryImpl(
                httpClient = mockHandler {
                    respond(
                        content = errorResponseJson,
                        status = HttpStatusCode.Unauthorized,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            )
            ReflectionTestUtils.setField(shopeePayRepository, "shopeePayBaseUrl", shopeePayBaseUrl)

            // act & assert
            assertThrows(IllegalStateException::class.java) {
                runTest {
                    shopeePayRepository.topUp(dummyTopUpReq)
                }
            }
        }

        @Test
        fun `with valid request but server error then should throw InternalServerException`() = runTest {
            // arrange
            val shopeePayRepository = ShopeePayRepositoryImpl(
                httpClient = mockHandler {
                    respond(
                        content = "Internal Server Error",
                        status = HttpStatusCode.InternalServerError,
                        headers = headersOf(HttpHeaders.ContentType, "text/plain")
                    )
                }
            )
            ReflectionTestUtils.setField(shopeePayRepository, "shopeePayBaseUrl", shopeePayBaseUrl)

            // act & assert
            assertThrows(IllegalStateException::class.java) {
                runTest {
                    shopeePayRepository.topUp(dummyTopUpReq)
                }
            }
        }

        @Test
        fun `with network exception then should throw InternalServerException`() = runTest {
            // arrange
            val shopeePayRepository = ShopeePayRepositoryImpl(
                httpClient = mockHandler {
                    throw RuntimeException("Network connection failed")
                }
            )
            ReflectionTestUtils.setField(shopeePayRepository, "shopeePayBaseUrl", shopeePayBaseUrl)

            // act & assert
            assertThrows(IllegalStateException::class.java) {
                runTest {
                    shopeePayRepository.topUp(dummyTopUpReq)
                }
            }
        }
    }
}
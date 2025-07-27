package id.co.bni.payment.infrastructures.repositories

import id.co.bni.payment.domains.dtos.GopayTopUpReq
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.util.ReflectionTestUtils
import kotlin.test.*


@ExtendWith(MockKExtension::class)
class GopayRepositoryTest: BaseRepositoryTest() {
    private val gopayBaseUrl = "https://api.gopay.com"

    private val dummyWalletId = "wallet123"
    private val dummyTopUpReq = GopayTopUpReq(
        walletId = dummyWalletId,
        amount = 50000.0,
        paymentMethod = "BANK_TRANSFER",
        referenceId = "ref123"
    )

    private val successfulBalanceResponseJson = """
        {
            "status": "success",
            "data": {
                "wallet_id": "$dummyWalletId",
                "balance": 100000.0,
                "currency": "IDR",
                "last_updated": "2024-01-01T10:00:00Z"
            }
        }
    """.trimIndent()

    private val successfulTopUpResponseJson = """
        {
            "status": "SUCCESS",
            "transaction_id": "txn123",
            "wallet_id": "$dummyWalletId",
            "amount": 50000.0,
            "new_balance": 150000.0,
            "transaction_time": "2024-01-01T10:30:00Z"
        }
    """.trimIndent()

    private val errorResponseJson = """
        {
            "error": "Wallet not found",
            "message": "The specified wallet ID does not exist"
        }
    """.trimIndent()

    @Nested
    @DisplayName("when get balance by wallet ID")
    inner class GetBalanceByWalletId {

        @Test
        fun `with valid wallet ID then should return success response`() = runTest {
            // arrange
            val gopayRepository = GopayRepositoryImpl(
                httpClient = mockHandler {
                    respond(
                        content = successfulBalanceResponseJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            )
            ReflectionTestUtils.setField(gopayRepository, "gopayBaseUrl", gopayBaseUrl)

            // act
            val result = gopayRepository.getBalanceByWalletId(dummyWalletId)

            // assert
            assertEquals("success", result.status)
            assertEquals(dummyWalletId, result.data.walletId)
            assertEquals(100000.0, result.data.balance)
            assertEquals("IDR", result.data.currency)
        }

        @Test
        fun `with invalid wallet ID then should return 4xx response`() = runTest {
            // arrange
            val gopayRepository = GopayRepositoryImpl(
                httpClient = mockHandler {
                    respond(
                        content = successfulBalanceResponseJson,
                        status = HttpStatusCode.NotFound,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            )
            ReflectionTestUtils.setField(gopayRepository, "gopayBaseUrl", gopayBaseUrl)

            // assert
            assertThrows(IllegalStateException::class.java) {
                runTest {
                    gopayRepository.getBalanceByWalletId(dummyWalletId)
                }
            }
        }

        @Test
        fun `with invalid wallet ID then should throw APIException`() = runTest {
            // arrange
            val gopayRepository = GopayRepositoryImpl(
                httpClient = mockHandler {
                    respond(
                        content = errorResponseJson,
                        status = HttpStatusCode.NotFound,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            )
            ReflectionTestUtils.setField(gopayRepository, "gopayBaseUrl", gopayBaseUrl)

            // act & assert
            assertThrows(IllegalStateException::class.java) {
                runTest {
                    gopayRepository.getBalanceByWalletId("invalid_wallet")
                }
            }
        }

        @Test
        fun `with valid wallet ID but server error then should throw InternalServerException`() = runTest {
            // arrange
            val gopayRepository = GopayRepositoryImpl(
                httpClient = mockHandler {
                    respond(
                        content = "Internal Server Error",
                        status = HttpStatusCode.InternalServerError,
                        headers = headersOf(HttpHeaders.ContentType, "text/plain")
                    )
                }
            )
            ReflectionTestUtils.setField(gopayRepository, "gopayBaseUrl", gopayBaseUrl)

            // act & assert
            assertThrows(IllegalStateException::class.java) {
                runTest {
                    gopayRepository.getBalanceByWalletId(dummyWalletId)
                }
            }
        }

        @Test
        fun `with network exception then should throw InternalServerException`() = runTest {
            // arrange
            val gopayRepository = GopayRepositoryImpl(
                httpClient = mockHandler {
                    throw RuntimeException("Network error")
                }
            )
            ReflectionTestUtils.setField(gopayRepository, "gopayBaseUrl", gopayBaseUrl)

            // act & assert
            assertThrows(IllegalStateException::class.java) {
                runTest {
                    gopayRepository.getBalanceByWalletId(dummyWalletId)
                }
            }
        }
    }

    @Nested
    @DisplayName("when top up wallet")
    inner class TopUpWallet {

        @Test
        fun `with valid request then should return success response`() = runTest {
            // arrange
            val gopayRepository = GopayRepositoryImpl(
                httpClient = mockHandler {
                    respond(
                        content = successfulTopUpResponseJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            )
            ReflectionTestUtils.setField(gopayRepository, "gopayBaseUrl", gopayBaseUrl)

            // act
            val result = gopayRepository.topUp(dummyTopUpReq)

            // assert
            assertNotNull(result)
            assertEquals("SUCCESS", result.status)
            assertEquals("txn123", result.transactionId)
            assertEquals(dummyWalletId, result.walletId)
            assertEquals(50000.0, result.amount)
            assertEquals(150000.0, result.newBalance)
        }

        @Test
        fun `with invalid request then should return 4xx response`() = runTest {
            // arrange
            val gopayRepository = GopayRepositoryImpl(
                httpClient = mockHandler {
                    respond(
                        content = successfulTopUpResponseJson,
                        status = HttpStatusCode.NotFound,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            )
            ReflectionTestUtils.setField(gopayRepository, "gopayBaseUrl", gopayBaseUrl)

            // act & assert
            assertThrows(IllegalStateException::class.java) {
                runTest {
                    gopayRepository.topUp(dummyTopUpReq)
                }
            }
        }

        @Test
        fun `with valid request but bad request response then should return null`() = runTest {
            // arrange
            val gopayRepository = GopayRepositoryImpl(
                httpClient = mockHandler {
                    respond(
                        content = errorResponseJson,
                        status = HttpStatusCode.BadRequest,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            )
            ReflectionTestUtils.setField(gopayRepository, "gopayBaseUrl", gopayBaseUrl)

            // act
            val result = gopayRepository.topUp(dummyTopUpReq)

            // assert
            assertNull(result)
        }

        @Test
        fun `with valid request but server error then should return null`() = runTest {
            // arrange
            val gopayRepository = GopayRepositoryImpl(
                httpClient = mockHandler {
                    respond(
                        content = "Internal Server Error",
                        status = HttpStatusCode.InternalServerError,
                        headers = headersOf(HttpHeaders.ContentType, "text/plain")
                    )
                }
            )
            ReflectionTestUtils.setField(gopayRepository, "gopayBaseUrl", gopayBaseUrl)

            // act
            val result = gopayRepository.topUp(dummyTopUpReq)

            // assert
            assertNull(result)
        }

        @Test
        fun `with network exception then should throw InternalServerException`() = runTest {
            // arrange
            val gopayRepository = GopayRepositoryImpl(
                httpClient = mockHandler {
                    throw RuntimeException("Network connection failed")
                }
            )
            ReflectionTestUtils.setField(gopayRepository, "gopayBaseUrl", gopayBaseUrl)

            // act & assert
            assertThrows(IllegalStateException::class.java) {
                runTest {
                    gopayRepository.topUp(dummyTopUpReq)
                }
            }
        }
    }
}
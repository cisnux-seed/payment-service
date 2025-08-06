package id.co.bni.payment.applications.controllers

import id.co.bni.payment.applications.controllers.dtos.MetaResponse
import id.co.bni.payment.applications.controllers.dtos.WebResponse
import id.co.bni.payment.applications.resolvers.Subject
import id.co.bni.payment.commons.exceptions.APIException
import id.co.bni.payment.commons.loggable.Loggable
import id.co.bni.payment.domains.dtos.AccountResponse
import id.co.bni.payment.domains.dtos.BalanceResponse
import id.co.bni.payment.domains.dtos.EWalletBalanceResponse
import id.co.bni.payment.domains.dtos.TopUpEWalletRequest
import id.co.bni.payment.domains.dtos.TransactionResponse
import id.co.bni.payment.domains.services.AccountService
import id.co.bni.payment.domains.services.PaymentService
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.util.UUID

@RestController
@RequestMapping("/api/payment")
class PaymentController(
    private val accountService: AccountService,
    private val paymentService: PaymentService
) : Loggable {
    @GetMapping("/account", produces = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun getAccount(@Subject username: String): WebResponse<AccountResponse> {
        val traceId = UUID.randomUUID().toString()

        return withContext(
            MDCContext(mapOf("traceId" to traceId))
        ) {
            log.info("getting account information for user {}", username)
            val accountResponse = accountService.getAccountByUsername(username)
                ?: throw APIException.NotFoundResourceException(
                    statusCode = HttpStatus.NOT_FOUND.value(),
                    message = "account not found for user: $username"
                )
            WebResponse(
                meta = MetaResponse(
                    code = HttpStatus.OK.value().toString(),
                    message = "account retrieved successfully"
                ),
                data = accountResponse
            )
        }
    }

    @GetMapping("/balance", produces = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun getBalance(@Subject username: String): WebResponse<BalanceResponse> {
        val traceId = UUID.randomUUID().toString()

        return withContext(MDCContext(mapOf("traceId" to traceId))) {
            log.info("getting balance for user {}", username)
            val balanceResp = accountService.getBalanceByUsername(username)
                ?: throw APIException.NotFoundResourceException(
                    statusCode = HttpStatus.NOT_FOUND.value(),
                    message = "account not found for user: $username"
                )
            WebResponse(
                meta = MetaResponse(
                    code = HttpStatus.OK.value().toString(),
                    message = "balance retrieved successfully"
                ),
                data = balanceResp
            )
        }
    }

    @GetMapping("/wallet", produces = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun getBalance(
        @Subject username: String,
        @RequestParam("ewallet") ewalllet: String
    ): WebResponse<EWalletBalanceResponse> {
        val traceId = UUID.randomUUID().toString()
        return withContext(MDCContext(mapOf("traceId" to traceId))) {
            log.info("getting ewallet balance for user {} from {}", username, ewalllet)

            val walletResp =
                paymentService.getEWalletBalanceByUsername(username, ewalllet)
                    ?: throw APIException.NotFoundResourceException(
                        statusCode = HttpStatus.NOT_FOUND.value(),
                        message = "account not found for user: $username"
                    )
            WebResponse(
                meta = MetaResponse(
                    code = HttpStatus.OK.value().toString(),
                    message = "account retrieved successfully"
                ),
                data = walletResp
            )
        }
    }

    @PostMapping(
        "/wallet/topup",
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    suspend fun topUpEWallet(
        @Subject username: String,
        @RequestBody
        @Validated request: TopUpEWalletRequest,
    ): WebResponse<TransactionResponse> {
        val traceId = UUID.randomUUID().toString()
        return withContext(MDCContext(mapOf("traceId" to traceId))) {
            log.info("getting topup information for user {}", username)

            val topUpResp = paymentService.topUpEWallet(username, request) {
                throw APIException.InternalServerException(
                    statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    message = "Top up transaction failed, please try again later"
                )
            }

            WebResponse(
                meta = MetaResponse(
                    code = HttpStatus.OK.value().toString(),
                    message = "top up successful"
                ),
                data = topUpResp
            )
        }
    }
}

package id.co.bni.payment.applications.controllers

import id.co.bni.payment.applications.controllers.dtos.MetaResponse
import id.co.bni.payment.applications.controllers.dtos.WebResponse
import id.co.bni.payment.applications.resolvers.Subject
import id.co.bni.payment.commons.exceptions.APIException
import id.co.bni.payment.domains.dtos.AccountResponse
import id.co.bni.payment.domains.dtos.BalanceResponse
import id.co.bni.payment.domains.dtos.EWalletBalanceResponse
import id.co.bni.payment.domains.dtos.TopUpEWalletRequest
import id.co.bni.payment.domains.dtos.TransactionResponse
import id.co.bni.payment.domains.services.AccountService
import id.co.bni.payment.domains.services.PaymentService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@RestController
@RequestMapping("/api/payment")
class PaymentController(
    private val accountService: AccountService,
    private val paymentService: PaymentService
) {
    @GetMapping("/account", produces = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun getAccount(@Subject username: String): WebResponse<AccountResponse> {
        val accountResponse = accountService.getAccountByUsername(username)
            ?: throw APIException.NotFoundResourceException(
                statusCode = HttpStatus.NOT_FOUND.value(),
                message = "account not found for user: $username"
            )
        return WebResponse(
            meta = MetaResponse(
                code = HttpStatus.OK.value().toString(),
                message = "account retrieved successfully"
            ),
            data = accountResponse
        )
    }

    @GetMapping("/balance", produces = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun getBalance(@Subject username: String): WebResponse<BalanceResponse> {
        val balanceResp = accountService.getBalanceByUsername(username)
            ?: throw APIException.NotFoundResourceException(
                statusCode = HttpStatus.NOT_FOUND.value(),
                message = "account not found for user: $username"
            )
        return WebResponse(
            meta = MetaResponse(
                code = HttpStatus.OK.value().toString(),
                message = "balance retrieved successfully"
            ),
            data = balanceResp
        )
    }

    @GetMapping("/wallet", produces = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun getBalance(
        @Subject username: String,
        @RequestParam("ewallet") ewalllet: String
    ): WebResponse<EWalletBalanceResponse> {
        val walletResp =
            paymentService.getEWalletBalanceByUsername(username, ewalllet)
                ?: throw APIException.NotFoundResourceException(
                    statusCode = HttpStatus.NOT_FOUND.value(),
                    message = "account not found for user: $username"
                )
        return WebResponse(
            meta = MetaResponse(
                code = HttpStatus.OK.value().toString(),
                message = "account retrieved successfully"
            ),
            data = walletResp
        )
    }

    @PostMapping("/wallet/topup", produces = [MediaType.APPLICATION_JSON_VALUE], consumes = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun topUpEWallet(
        @Subject username: String,
        @RequestBody
        @Validated request: TopUpEWalletRequest,
    ): WebResponse<TransactionResponse> {
        val topUpResp = paymentService.topUpEWallet(username, request)
        return WebResponse(
            meta = MetaResponse(
                code = HttpStatus.OK.value().toString(),
                message = "top up successful"
            ),
            data = topUpResp
        )
    }
}

package id.co.bni.payment.applications.controllers

import id.co.bni.payment.applications.controllers.dtos.MetaResponse
import id.co.bni.payment.applications.controllers.dtos.WebResponse
import id.co.bni.payment.applications.resolvers.Subject
import id.co.bni.payment.commons.exceptions.APIException
import id.co.bni.payment.domains.dtos.AccountResponse
import id.co.bni.payment.domains.services.AccountService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping

@RestController
@RequestMapping("/api/payment")
class PaymentController(
    private val accountService: AccountService
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
}

package id.co.bni.payment.commons.exceptions

import io.ktor.http.HttpStatusCode

sealed class APIException(
    open var statusCode: Int, override var message: String
) : Exception(message) {
    data class NotFoundResourceException(override var statusCode: Int = 404, override var message: String) :
        APIException(
            statusCode = statusCode, message = message
        )

    data class UnauthenticatedException(override var statusCode: Int = 401, override var message: String) :
        APIException(
            statusCode = statusCode, message = message
        )

    data class ForbiddenException(override var statusCode: Int = 403, override var message: String) : APIException(
        statusCode = statusCode, message = message
    )

    data class InternalServerException(override var statusCode: Int = 500, override var message: String) : APIException(
        statusCode = statusCode, message = message
    )

    data class IllegalParameterException(override var statusCode: Int = 400, override var message: String) :
        APIException(
            statusCode = statusCode, message = message
        )

    data class ConflictResourceException(override var statusCode: Int = 409, override var message: String) :
        APIException(
            statusCode = statusCode, message = message
        )

    companion object {
        val HTTP_CLIENT_FAILURES = mapOf(
            HttpStatusCode.Conflict to ConflictResourceException(message = ""),
            HttpStatusCode.BadRequest to IllegalParameterException(message = ""),
            HttpStatusCode.Unauthorized to UnauthenticatedException(message = ""),
            HttpStatusCode.NotFound to NotFoundResourceException(message = ""),
            HttpStatusCode.Forbidden to ForbiddenException(message = ""),
            HttpStatusCode.InternalServerError to InternalServerException(message = ""),
            HttpStatusCode.InternalServerError to InternalServerException(message = ""),
            HttpStatusCode.NotImplemented to InternalServerException(message = ""),
            HttpStatusCode.BadGateway to InternalServerException(message = ""),
            HttpStatusCode.ServiceUnavailable to InternalServerException(message = ""),
            HttpStatusCode.GatewayTimeout to InternalServerException(message = ""),
            HttpStatusCode.VersionNotSupported to InternalServerException(message = ""),
            HttpStatusCode.VariantAlsoNegotiates to InternalServerException(message = ""),
            HttpStatusCode.InsufficientStorage to InternalServerException(message = "")
        )
    }
}

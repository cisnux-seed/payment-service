package id.co.bni.payment.commons.exceptions

sealed class APIException(
    open var statusCode: Int, override var message: String
) : Exception(message) {
    data class NotFoundResourceException(override var statusCode: Int = 404, override var message: String) :
        APIException(
            statusCode = statusCode, message = message
        )

    data class UnauthenticatedException(override var statusCode: Int = 401, override var message: String) : APIException(
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
}

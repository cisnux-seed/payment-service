package id.co.bni.payment.applications.resolvers

import com.fasterxml.jackson.databind.ObjectMapper
import id.co.bni.payment.commons.exceptions.APIException
import kotlinx.coroutines.reactor.mono
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.reactive.BindingContext
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.Base64

@Component
class SubjectArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(Subject::class.java) && parameter.parameterType == String::class.java

    override fun resolveArgument(
        parameter: MethodParameter, bindingContext: BindingContext, exchange: ServerWebExchange
    ): Mono<Any> = mono {
        extractSubjectFromJWT(exchange.request)
    }

    private fun extractTokenFromJWT(request: ServerHttpRequest): String{
        val authHeader = request.headers.getFirst("Authorization")
        if (authHeader?.startsWith("Bearer ") == true){
            return authHeader.substring(7)
        }

        val cookieToken = request.cookies["auth-token"]?.firstOrNull()?.value
        if (cookieToken != null) {
            return cookieToken
        }

        throw APIException.UnauthenticatedException(HttpStatus.UNAUTHORIZED.value(), "no JWT found")
    }

    private fun extractSubjectFromJWT(request: ServerHttpRequest): String {
        val token = extractTokenFromJWT(request)

        return try {
            val payload = token.split(".")[1]
            val decodedBytes = Base64.getUrlDecoder().decode(payload)
            val objectMapper = ObjectMapper()
            val claims = objectMapper.readValue(decodedBytes, Map::class.java)
            claims["sub"] as? String
                ?: throw APIException.UnauthenticatedException(HttpStatus.UNAUTHORIZED.value(), "No subject in JWT")
        } catch (_: Exception) {
            throw APIException.UnauthenticatedException(HttpStatus.UNAUTHORIZED.value(), "Invalid JWT")
        }
    }
}
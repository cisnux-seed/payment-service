package id.co.bni.payment.domains.services

import com.fasterxml.jackson.databind.ObjectMapper
import id.co.bni.payment.commons.exceptions.APIException
import id.co.bni.payment.commons.loggable.Loggable
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class CacheServiceImpl(
    private val redisTemplate: ReactiveRedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper
) : CacheService, Loggable {

    override suspend fun <T> get(key: String, type: Class<T>): T? {
        return try {
            val value = redisTemplate.opsForValue().get(key).awaitFirstOrNull()
            when {
                value == null -> null
                type.isAssignableFrom(value::class.java) -> value as T
                value is String -> objectMapper.readValue(value, type)
                else -> objectMapper.convertValue(value, type)
            }
        } catch (e: Exception) {
            log.error("Error getting cache key: $key", e)
            throw APIException.InternalServerException(
                message = "internal server error",
                statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value()
            )
        }
    }

    override suspend fun set(key: String, value: Any, ttlMinutes: Long) {
        try {
            redisTemplate.opsForValue()
                .set(key, value, Duration.ofMinutes(ttlMinutes))
                .awaitFirstOrNull()
        } catch (e: Exception) {
            log.error("Error setting cache key: $key", e)
            throw APIException.InternalServerException(
                message = "internal server error",
                statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value()
            )
        }
    }

    override suspend fun delete(key: String): Boolean {
        return try {
            redisTemplate.delete(key).awaitSingle() > 0
        } catch (e: Exception) {
            log.error("Error deleting cache key: $key", e)
            false
        }
    }

    override suspend fun deletePattern(pattern: String): Long {
        return try {
            val keys = redisTemplate.keys(pattern).collectList().awaitSingle()
            if (keys.isNotEmpty()) {
                redisTemplate.delete(*keys.toTypedArray()).awaitSingle()
            } else {
                0L
            }
        } catch (e: Exception) {
            log.error("Error deleting cache pattern: $pattern", e)
            0L
        }
    }
}
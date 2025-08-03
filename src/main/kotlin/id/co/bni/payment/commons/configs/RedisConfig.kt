package id.co.bni.payment.commons.configs

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import kotlin.apply

@Configuration
class RedisConfig {

    @Bean
    fun redisObjectMapper(): ObjectMapper {
        return ObjectMapper().apply {
            registerModule(JavaTimeModule())
            registerKotlinModule()
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }

    @Bean
    fun reactiveRedisTemplate(
        connectionFactory: ReactiveRedisConnectionFactory,
        redisObjectMapper: ObjectMapper
    ): ReactiveRedisTemplate<String, Any> {
        val keySerializer = StringRedisSerializer()
        val valueSerializer = GenericJackson2JsonRedisSerializer(redisObjectMapper)

        val serializationContext = RedisSerializationContext
            .newSerializationContext<String, Any>()
            .key(keySerializer)
            .value(valueSerializer)
            .hashKey(keySerializer)
            .hashValue(valueSerializer)
            .build()

        return ReactiveRedisTemplate(connectionFactory, serializationContext)
    }
}
package id.co.bni.payment.domains.services

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ReactiveValueOperations
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

@ExtendWith(MockKExtension::class)
class CacheServiceTest {

    @MockK
    private lateinit var redisTemplate: ReactiveRedisTemplate<String, Any>

    @MockK
    private lateinit var objectMapper: ObjectMapper

    @MockK
    private lateinit var valueOps: ReactiveValueOperations<String, Any>

    @InjectMockKs
    private lateinit var cacheService: CacheServiceImpl

    // Test data class for ObjectMapper conversion tests
    data class TestData(
        val name: String,
        val value: Int
    )

    @Test
    fun `get should return cached value when key exists and type matches`() = runTest {
        // arrange
        val key = "test:key"
        val value = "test value"
        coEvery { redisTemplate.opsForValue() } returns valueOps
        coEvery { valueOps.get(key) } returns Mono.just(value)

        // act
        val result = cacheService.get(key, String::class.java)

        // assert
        assertEquals(value, result)
        coVerify(exactly = 1) { redisTemplate.opsForValue() }
        coVerify(exactly = 1) { valueOps.get(key) }
    }

    @Test
    fun `get should return null when key does not exist`() = runTest {
        // arrange
        val key = "nonexistent:key"
        coEvery { redisTemplate.opsForValue() } returns valueOps
        coEvery { valueOps.get(key) } returns Mono.empty()

        // act
        val result = cacheService.get(key, String::class.java)

        // assert
        assertNull(result)
        coVerify(exactly = 1) { redisTemplate.opsForValue() }
        coVerify(exactly = 1) { valueOps.get(key) }
    }

    @Test
    fun `get should use ObjectMapper when value is String and type conversion needed`() = runTest {
        // arrange
        val key = "test:key"
        val jsonValue = """{"name":"test","value":123}"""
        val expectedObject = TestData("test", 123)
        
        coEvery { redisTemplate.opsForValue() } returns valueOps
        coEvery { valueOps.get(key) } returns Mono.just(jsonValue)
        coEvery { objectMapper.readValue(jsonValue, TestData::class.java) } returns expectedObject

        // act
        val result = cacheService.get(key, TestData::class.java)

        // assert
        assertEquals(expectedObject, result)
        coVerify(exactly = 1) { redisTemplate.opsForValue() }
        coVerify(exactly = 1) { valueOps.get(key) }
        coVerify(exactly = 1) { objectMapper.readValue(jsonValue, TestData::class.java) }
    }

    @Test
    fun `get should use ObjectMapper convertValue when types don't match directly`() = runTest {
        // arrange
        val key = "test:key"
        val mapValue = mapOf("name" to "test", "value" to 123)
        val expectedObject = TestData("test", 123)
        
        coEvery { redisTemplate.opsForValue() } returns valueOps
        coEvery { valueOps.get(key) } returns Mono.just(mapValue)
        coEvery { objectMapper.convertValue(mapValue, TestData::class.java) } returns expectedObject

        // act
        val result = cacheService.get(key, TestData::class.java)

        // assert
        assertEquals(expectedObject, result)
        coVerify(exactly = 1) { redisTemplate.opsForValue() }
        coVerify(exactly = 1) { valueOps.get(key) }
        coVerify(exactly = 1) { objectMapper.convertValue(mapValue, TestData::class.java) }
    }

    @Test
    fun `set should store value with default TTL`() = runTest {
        // arrange
        val key = "test:key"
        val value = "test value"
        
        coEvery { redisTemplate.opsForValue() } returns valueOps
        coEvery { valueOps.set(key, value, Duration.ofMinutes(30)) } returns Mono.just(true)

        // act
        cacheService.set(key, value)

        // assert
        coVerify(exactly = 1) { redisTemplate.opsForValue() }
        coVerify(exactly = 1) { valueOps.set(key, value, Duration.ofMinutes(30)) }
    }

    @Test
    fun `set should store value with custom TTL`() = runTest {
        // arrange
        val key = "test:key"
        val value = "test value"
        val ttlMinutes = 60L
        
        coEvery { redisTemplate.opsForValue() } returns valueOps
        coEvery { valueOps.set(key, value, Duration.ofMinutes(ttlMinutes)) } returns Mono.just(true)

        // act
        cacheService.set(key, value, ttlMinutes)

        // assert
        coVerify(exactly = 1) { redisTemplate.opsForValue() }
        coVerify(exactly = 1) { valueOps.set(key, value, Duration.ofMinutes(ttlMinutes)) }
    }

    @Test
    fun `set should store complex object`() = runTest {
        // arrange
        val key = "test:key"
        val value = TestData("test", 123)
        
        coEvery { redisTemplate.opsForValue() } returns valueOps
        coEvery { valueOps.set(key, value, Duration.ofMinutes(30)) } returns Mono.just(true)

        // act
        cacheService.set(key, value)

        // assert
        coVerify(exactly = 1) { redisTemplate.opsForValue() }
        coVerify(exactly = 1) { valueOps.set(key, value, Duration.ofMinutes(30)) }
    }

    @Test
    fun `delete should return true when key is deleted successfully`() = runTest {
        // arrange
        val key = "test:key"
        coEvery { redisTemplate.delete(key) } returns Mono.just(1L)

        // act
        val result = cacheService.delete(key)

        // assert
        assertTrue(result)
        coVerify(exactly = 1) { redisTemplate.delete(key) }
    }

    @Test
    fun `delete should return false when key does not exist`() = runTest {
        // arrange
        val key = "nonexistent:key"
        coEvery { redisTemplate.delete(key) } returns Mono.just(0L)

        // act
        val result = cacheService.delete(key)

        // assert
        assertFalse(result)
        coVerify(exactly = 1) { redisTemplate.delete(key) }
    }

    @Test
    fun `delete should return false when Redis operation fails`() = runTest {
        // arrange
        val key = "test:key"
        coEvery { redisTemplate.delete(key) } throws RuntimeException("Redis delete failed")

        // act
        val result = cacheService.delete(key)

        // assert
        assertFalse(result)
        coVerify(exactly = 1) { redisTemplate.delete(key) }
    }

    @Test
    fun `deletePattern should delete multiple keys matching pattern`() = runTest {
        // arrange
        val pattern = "test:*"
        val keys = listOf("test:key1", "test:key2", "test:key3")
        val mockFlux = mockk<Flux<String>>()
        
        coEvery { redisTemplate.keys(pattern) } returns mockFlux
        coEvery { mockFlux.collectList() } returns Mono.just(keys)
        coEvery { redisTemplate.delete(*keys.toTypedArray()) } returns Mono.just(3L)

        // act
        val result = cacheService.deletePattern(pattern)

        // assert
        assertEquals(3L, result)
        coVerify(exactly = 1) { redisTemplate.keys(pattern) }
        coVerify(exactly = 1) { mockFlux.collectList() }
        coVerify(exactly = 1) { redisTemplate.delete(*keys.toTypedArray()) }
    }

    @Test
    fun `deletePattern should return 0 when no keys match pattern`() = runTest {
        // arrange
        val pattern = "nonexistent:*"
        val mockFlux = mockk<Flux<String>>()
        
        coEvery { redisTemplate.keys(pattern) } returns mockFlux
        coEvery { mockFlux.collectList() } returns Mono.just(emptyList())

        // act
        val result = cacheService.deletePattern(pattern)

        // assert
        assertEquals(0L, result)
        coVerify(exactly = 1) { redisTemplate.keys(pattern) }
        coVerify(exactly = 1) { mockFlux.collectList() }
        coVerify(exactly = 0) { redisTemplate.delete(any<String>()) }
    }

    @Test
    fun `deletePattern should handle single key pattern`() = runTest {
        // arrange
        val pattern = "test:single"
        val keys = listOf("test:single")
        val mockFlux = mockk<Flux<String>>()
        
        coEvery { redisTemplate.keys(pattern) } returns mockFlux
        coEvery { mockFlux.collectList() } returns Mono.just(keys)
        coEvery { redisTemplate.delete(*keys.toTypedArray()) } returns Mono.just(1L)

        // act
        val result = cacheService.deletePattern(pattern)

        // assert
        assertEquals(1L, result)
        coVerify(exactly = 1) { redisTemplate.keys(pattern) }
        coVerify(exactly = 1) { mockFlux.collectList() }
        coVerify(exactly = 1) { redisTemplate.delete(*keys.toTypedArray()) }
    }

    @Test
    fun `deletePattern should return 0 when Redis keys operation fails`() = runTest {
        // arrange
        val pattern = "test:*"
        coEvery { redisTemplate.keys(pattern) } throws RuntimeException("Redis keys operation failed")

        // act
        val result = cacheService.deletePattern(pattern)

        // assert
        assertEquals(0L, result)
        coVerify(exactly = 1) { redisTemplate.keys(pattern) }
    }

    @Test
    fun `deletePattern should return 0 when Redis delete operation fails`() = runTest {
        // arrange
        val pattern = "test:*"
        val keys = listOf("test:key1", "test:key2")
        val mockFlux = mockk<Flux<String>>()
        
        coEvery { redisTemplate.keys(pattern) } returns mockFlux
        coEvery { mockFlux.collectList() } returns Mono.just(keys)
        coEvery { redisTemplate.delete(*keys.toTypedArray()) } throws RuntimeException("Redis delete failed")

        // act
        val result = cacheService.deletePattern(pattern)

        // assert
        assertEquals(0L, result)
        coVerify(exactly = 1) { redisTemplate.keys(pattern) }
        coVerify(exactly = 1) { mockFlux.collectList() }
        coVerify(exactly = 1) { redisTemplate.delete(*keys.toTypedArray()) }
    }

    @Test
    fun `deletePattern should handle empty pattern gracefully`() = runTest {
        // arrange
        val pattern = ""
        val mockFlux = mockk<Flux<String>>()
        
        coEvery { redisTemplate.keys(pattern) } returns mockFlux
        coEvery { mockFlux.collectList() } returns Mono.just(emptyList())

        // act
        val result = cacheService.deletePattern(pattern)

        // assert
        assertEquals(0L, result)
        coVerify(exactly = 1) { redisTemplate.keys(pattern) }
        coVerify(exactly = 1) { mockFlux.collectList() }
        coVerify(exactly = 0) { redisTemplate.delete(any<String>()) }
    }
}
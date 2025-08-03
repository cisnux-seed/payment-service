package id.co.bni.payment.domains.services

interface CacheService {
    suspend fun <T> get(key: String, type: Class<T>): T?
    suspend fun set(key: String, value: Any, ttlMinutes: Long = 30)
    suspend fun delete(key: String): Boolean
    suspend fun deletePattern(pattern: String): Long
}
package id.co.bni.payment.domains.services

interface CacheInvalidationService {
    suspend fun invalidateTransactionAndUserCaches(username: String)
}
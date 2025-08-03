package id.co.bni.payment.domains.services

import id.co.bni.payment.commons.constants.CacheKeys
import id.co.bni.payment.commons.loggable.Loggable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Service

@Service
class CacheInvalidationService(
    private val cacheService: CacheService,
    private val accountService: AccountService
) : Loggable {
    suspend fun invalidateTransactionAndUserCaches(username: String) {
        coroutineScope {
            val invalidationTasks = listOf(
                // Invalidate transaction caches
                async { 
                    log.info("Invalidating transaction list caches")
                    cacheService.deletePattern(CacheKeys.ALL_TRANSACTION_LISTS_PATTERN) 
                },
                async { 
                    log.info("Invalidating individual transaction caches")
                    cacheService.deletePattern(CacheKeys.ALL_TRANSACTIONS_PATTERN)
                },
                async { 
                    log.info("Invalidating transaction count cache")
                    cacheService.delete(CacheKeys.TRANSACTION_COUNT) 
                },
                // Invalidate user's account and balance caches
                async { 
                    log.info("Invalidating user account and balance caches for user: $username")
                    accountService.invalidateUserCache(username) 
                }
            )

            try {
                val results = invalidationTasks.awaitAll()
                val totalInvalidated = results.sumOf { 
                    when (it) {
                        is Long -> it
                        is Boolean -> if (it) 1L else 0L
                        else -> 0L
                    }
                }
                log.info("Successfully invalidated caches after top-up. Total keys invalidated: $totalInvalidated")
            } catch (e: Exception) {
                log.error("Error during cache invalidation for user: $username", e)
            }
        }
    }
}
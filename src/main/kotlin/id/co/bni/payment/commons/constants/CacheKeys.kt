package id.co.bni.payment.commons.constants

object CacheKeys {
    fun accountKey(username: String) = "account:$username"
    fun balanceKey(username: String) = "balance:$username"
    
    // Transaction service cache keys to invalidate
    fun transactionKey(id: String) = "trx:$id"
    fun transactionListKey(page: Int, size: Int) = "trx_list:$page:$size"
    const val TRANSACTION_COUNT = "trx_count"
    
    // Cache patterns for bulk deletion
    const val ALL_TRANSACTIONS_PATTERN = "trx:*"
    const val ALL_TRANSACTION_LISTS_PATTERN = "trx_list:*"
}
package id.co.bni.payment.domains.services

import id.co.bni.payment.commons.constants.AccountStatus
import id.co.bni.payment.commons.constants.PaymentMethod
import id.co.bni.payment.commons.constants.TransactionStatus
import id.co.bni.payment.commons.constants.TransactionType
import id.co.bni.payment.commons.exceptions.APIException
import id.co.bni.payment.commons.loggable.Loggable
import id.co.bni.payment.domains.dtos.EWalletBalanceResponse
import id.co.bni.payment.domains.dtos.GetShopeePayWalletReq
import id.co.bni.payment.domains.dtos.GopayTopUpReq
import id.co.bni.payment.domains.dtos.ShopeePayTopUpReq
import id.co.bni.payment.domains.dtos.SourceDetails
import id.co.bni.payment.domains.dtos.TopUpEWalletRequest
import id.co.bni.payment.domains.dtos.TransactionResponse
import id.co.bni.payment.domains.entities.Transaction
import id.co.bni.payment.domains.producers.TransactionProducer
import id.co.bni.payment.domains.repositories.AccountRepository
import id.co.bni.payment.domains.repositories.GopayRepository
import id.co.bni.payment.domains.repositories.ShopeePayRepository
import id.co.bni.payment.domains.repositories.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.UUID

@Service
class PaymentServiceImpl(
    private val gopayRepository: GopayRepository,
    private val shopeePayRepository: ShopeePayRepository,
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository,
    private val transactionProducer: TransactionProducer,
    private val cacheInvalidationService: CacheInvalidationService
) : PaymentService, Loggable {

    override suspend fun getEWalletBalanceByUsername(username: String, walletType: String): EWalletBalanceResponse? {
        val user = userRepository.findByUsername(username) ?: throw APIException.NotFoundResourceException(
            statusCode = HttpStatus.NOT_FOUND.value(),
            message = "user not found"
        )
        if (walletType !in listOf(PaymentMethod.GOPAY.value, PaymentMethod.SHOPEE_PAY.value)) {
            throw APIException.IllegalParameterException(
                statusCode = HttpStatus.BAD_REQUEST.value(),
                message = "invalid payment method: $walletType"
            )
        }

        val walletId = "${
            when (walletType) {
                PaymentMethod.GOPAY.value -> "898"
                else -> "897"
            }
        }${user.phone}"

        return when (walletType) {
            PaymentMethod.GOPAY.value -> {
                val gopayResp = gopayRepository.getBalanceByWalletId(walletId)
                val account =
                    accountRepository.getAccountByUserId(user.id!!) ?: throw APIException.NotFoundResourceException(
                        statusCode = HttpStatus.NOT_FOUND.value(),
                        message = "account not found for user: $username"
                    )
                val walletResp = EWalletBalanceResponse(
                    provider = walletType,
                    balance = BigDecimal(gopayResp.data.balance),
                    currency = "IDR",
                    accountNumber = account.id,
                    lastUpdated = ZonedDateTime.parse(gopayResp.data.lastUpdated)
                )
                walletResp
            }

            else -> {
                val req = GetShopeePayWalletReq(
                    userId = walletId,
                    requestId = UUID.randomUUID().toString()
                )
                val shopeePayResp = shopeePayRepository.getShopeePayBalance(req)
                val account =
                    accountRepository.getAccountByUserId(user.id!!) ?: throw APIException.NotFoundResourceException(
                        statusCode = HttpStatus.NOT_FOUND.value(),
                        message = "account not found for user: $username"
                    )
                val walletResp = EWalletBalanceResponse(
                    provider = walletType,
                    balance = BigDecimal(shopeePayResp.balanceInfo.availableBalance),
                    currency = "IDR",
                    accountNumber = account.id,
                    lastUpdated = ZonedDateTime.now()
                )
                walletResp
            }
        }
    }

    @Transactional
    override suspend fun topUpEWallet(username: String, topUpEWalletRequest: TopUpEWalletRequest): TransactionResponse {
        val user = userRepository.findByUsername(username) ?: throw APIException.NotFoundResourceException(
            statusCode = HttpStatus.NOT_FOUND.value(),
            message = "user not found"
        )
        val account = accountRepository.getAccountByUserId(user.id!!) ?: throw APIException.NotFoundResourceException(
            statusCode = HttpStatus.NOT_FOUND.value(),
            message = "account not found for user: $username"
        )

        if (account.accountStatus != AccountStatus.ACTIVE) {
            throw APIException.ForbiddenException(
                statusCode = HttpStatus.FORBIDDEN.value(),
                message = "account is not active, cannot perform top up"
            )
        }

        if (account.balance < topUpEWalletRequest.amount) {
            throw APIException.IllegalParameterException(
                statusCode = HttpStatus.BAD_REQUEST.value(),
                message = "insufficient balance for top up"
            )
        }

        if (topUpEWalletRequest.paymentMethod != PaymentMethod.GOPAY.value &&
            topUpEWalletRequest.paymentMethod != PaymentMethod.SHOPEE_PAY.value
        ) {
            throw APIException.IllegalParameterException(
                statusCode = HttpStatus.BAD_REQUEST.value(),
                message = "invalid payment method: ${topUpEWalletRequest.paymentMethod}"
            )
        }

        val walletId = "${
            when (topUpEWalletRequest.paymentMethod) {
                PaymentMethod.GOPAY.value -> "898"
                else -> "897"
            }
        }${topUpEWalletRequest.phoneNumber}"

        val transactionResponse = when (topUpEWalletRequest.paymentMethod) {
            PaymentMethod.GOPAY.value -> {
                processGopayTopUp(user.id, account, walletId, topUpEWalletRequest, username)
            }
            else -> {
                processShopeePayTopUp(user.id, account, walletId, topUpEWalletRequest, username)
            }
        }

        withContext(Dispatchers.IO){
            async {
                try {
                    cacheInvalidationService.invalidateTransactionAndUserCaches(username)
                } catch (e: Exception) {
                    log.error("Cache invalidation failed for user: $username", e)
                }
            }
        }

        return transactionResponse
    }

    private suspend fun processGopayTopUp(
        userId: Long,
        account: id.co.bni.payment.domains.entities.Account,
        walletId: String,
        topUpEWalletRequest: TopUpEWalletRequest,
        username: String
    ): TransactionResponse {
        val gopayTopUpReq = GopayTopUpReq(
            walletId = walletId,
            amount = topUpEWalletRequest.amount.toDouble(),
            paymentMethod = "BANK_TRANSFER",
            referenceId = "TRX-${Instant.now().epochSecond}-GPX",
        )

        val gopayTopUpResp = gopayRepository.topUp(gopayTopUpReq) ?: throw APIException.NotFoundResourceException(
            statusCode = HttpStatus.NOT_FOUND.value(),
            message = "top up failed for walletId: $walletId"
        )

        log.info("Gopay - top up successful, response: $gopayTopUpResp")

        val affectedRows = accountRepository.updateAccountBalance(
            account.copy(
                balance = account.balance - topUpEWalletRequest.amount,
                updatedAt = Instant.now()
            )
        )
        log.info("Gopay - account balance updated, affected rows: $affectedRows")

        val updatedAccount = accountRepository.getAccountByUserId(userId)
            ?: throw APIException.NotFoundResourceException(
                statusCode = HttpStatus.NOT_FOUND.value(),
                message = "account not found after update for user: $username"
            )

        val trxResp = TransactionResponse(
            id = UUID.randomUUID().toString(),
            transactionId = "TRX-${Instant.now().epochSecond}-GPX",
            transactionType = TransactionType.TOPUP,
            amount = topUpEWalletRequest.amount,
            currency = "IDR",
            transactionStatus = TransactionStatus.entries.toTypedArray().random(),
            balanceBefore = account.balance,
            balanceAfter = updatedAccount.balance,
            paymentMethod = PaymentMethod.GOPAY,
            description = topUpEWalletRequest.description,
            createdAt = LocalDateTime.now()
        )

        val trx = Transaction(
            id = trxResp.id,
            userId = userId,
            accountId = updatedAccount.id,
            transactionId = trxResp.transactionId,
            transactionType = TransactionType.TOPUP,
            transactionStatus = trxResp.transactionStatus,
            amount = topUpEWalletRequest.amount,
            balanceBefore = trxResp.balanceBefore,
            balanceAfter = trxResp.balanceAfter,
            currency = trxResp.currency,
            description = trxResp.description,
            paymentMethod = PaymentMethod.GOPAY,
            createdAt = trxResp.createdAt,
            updatedAt = trxResp.createdAt
        )

        withContext(Dispatchers.IO){
            async {
                try {
                    transactionProducer.publishTransactionEvent(trx)
                    log.info("gopay top up event published successfully for transaction: ${trx.transactionId}")
                } catch (e: Exception) {
                    log.error("Failed to publish transaction event for transaction: ${trx.transactionId}", e)
                }
            }
        }

        return trxResp
    }

    private suspend fun processShopeePayTopUp(
        userId: Long,
        account: id.co.bni.payment.domains.entities.Account,
        walletId: String,
        topUpEWalletRequest: TopUpEWalletRequest,
        username: String
    ): TransactionResponse {
        val shopeePayTopUpReq = ShopeePayTopUpReq(
            userId = walletId,
            topupAmount = topUpEWalletRequest.amount.toLong(),
            sourceType = "BANK_ACCOUNT",
            requestId = "TRX-${Instant.now().epochSecond}-SPX",
            sourceDetails = SourceDetails(
                bankCode = "BNI",
                accountNumber = account.id
            )
        )

        val shopeePayTopUpResp = shopeePayRepository.topUp(shopeePayTopUpReq)

        log.info("ShopeePay - shopeePayTopUpResp: $shopeePayTopUpResp")

        val affectedRows = accountRepository.updateAccountBalance(
            account.copy(
                balance = account.balance - topUpEWalletRequest.amount,
                updatedAt = Instant.now()
            )
        )

        log.info("Shopee Pay - account balance updated, affected rows: $affectedRows")

        val updatedAccount = accountRepository.getAccountByUserId(userId)
            ?: throw APIException.NotFoundResourceException(
                statusCode = HttpStatus.NOT_FOUND.value(),
                message = "account not found after update for user: $username"
            )

        val trxResp = TransactionResponse(
            id = UUID.randomUUID().toString(),
            transactionId = "TRX-${Instant.now().epochSecond}-SPX",
            transactionType = TransactionType.TOPUP,
            amount = topUpEWalletRequest.amount,
            currency = "IDR",
            transactionStatus = TransactionStatus.entries.toTypedArray().random(),
            balanceBefore = account.balance,
            balanceAfter = updatedAccount.balance,
            paymentMethod = PaymentMethod.SHOPEE_PAY,
            description = topUpEWalletRequest.description,
            createdAt = LocalDateTime.now()
        )

        val trx = Transaction(
            id = trxResp.id,
            userId = userId,
            accountId = updatedAccount.id,
            transactionId = trxResp.transactionId,
            transactionType = TransactionType.TOPUP,
            transactionStatus = trxResp.transactionStatus,
            amount = topUpEWalletRequest.amount,
            balanceBefore = trxResp.balanceBefore,
            balanceAfter = trxResp.balanceAfter,
            currency = trxResp.currency,
            description = trxResp.description,
            paymentMethod = PaymentMethod.SHOPEE_PAY,
            createdAt = trxResp.createdAt,
            updatedAt = trxResp.createdAt
        )

        withContext(Dispatchers.IO){
            async {
                try {
                    transactionProducer.publishTransactionEvent(trx)
                    log.info("shopee pay top up event published successfully for transaction: ${trx.transactionId}")
                } catch (e: Exception) {
                    log.error("Failed to publish transaction event for transaction: ${trx.transactionId}", e)
                }
            }
        }

        return trxResp
    }
}
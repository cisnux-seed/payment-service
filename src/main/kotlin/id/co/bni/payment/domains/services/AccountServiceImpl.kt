package id.co.bni.payment.domains.services

import id.co.bni.payment.commons.exceptions.APIException
import id.co.bni.payment.domains.dtos.AccountResponse
import id.co.bni.payment.domains.dtos.BalanceResponse
import id.co.bni.payment.domains.entities.Account
import id.co.bni.payment.domains.repositories.AccountRepository
import id.co.bni.payment.domains.repositories.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class AccountServiceImpl(
    private val accountRepository: AccountRepository,
    private val userRepository: UserRepository
) : AccountService {

    override suspend fun updateAccountBalance(
        account: Account
    ): Int = accountRepository.updateAccountBalance(account)

    override suspend fun getAccountByUsername(username: String): AccountResponse? {
        val user = userRepository.findByUsername(username) ?: throw APIException.NotFoundResourceException(
            statusCode = HttpStatus.NOT_FOUND.value(),
            message = "user not found"
        )
        val account = accountRepository.getAccountByUserId(user.id!!) ?: throw APIException.NotFoundResourceException(
            statusCode = HttpStatus.NOT_FOUND.value(),
            message = "account not found"
        )
        return AccountResponse(
            id = account.id,
            userId = account.userId,
            balance = account.balance,
            currency = account.currency,
            accountStatus = account.accountStatus,
            createdAt = LocalDateTime.ofInstant(account.createdAt, ZoneOffset.UTC),
            updatedAt = LocalDateTime.ofInstant(account.updatedAt, ZoneOffset.UTC)
        )
    }

    override suspend fun getBalanceByUsername(username: String): BalanceResponse? {
        val user = userRepository.findByUsername(username) ?: throw APIException.NotFoundResourceException(
            statusCode = HttpStatus.NOT_FOUND.value(),
            message = "user not found"
        )
        val account = accountRepository.getAccountByUserId(user.id!!) ?: throw APIException.NotFoundResourceException(
            statusCode = HttpStatus.NOT_FOUND.value(),
            message = "account not found"
        )
        return BalanceResponse(
            balance = account.balance,
            currency = account.currency
        )
    }
}
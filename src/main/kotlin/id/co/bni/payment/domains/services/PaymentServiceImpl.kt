package id.co.bni.payment.domains.services

import id.co.bni.payment.commons.exceptions.APIException
import id.co.bni.payment.domains.dtos.EWalletBalanceResponse
import id.co.bni.payment.domains.dtos.TopUpEWalletRequest
import id.co.bni.payment.domains.dtos.TransactionResponse
import id.co.bni.payment.domains.repositories.EWalletRepository
import id.co.bni.payment.domains.repositories.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class PaymentServiceImpl(
    private val eWalletRepository: EWalletRepository,
    private val userRepository: UserRepository
) : PaymentService {
    override suspend fun getEWalletBalanceByUsername(username: String): EWalletBalanceResponse? {
        val user = userRepository.findByUsername(username) ?: throw APIException.NotFoundResourceException(
            statusCode = HttpStatus.NOT_FOUND.value(),
            message = "user not found"
        )
        return eWalletRepository.getEWalletBalanceByUsername(user.username)
            ?: throw APIException.NotFoundResourceException(
                statusCode = HttpStatus.NOT_FOUND.value(),
                message = "e-wallet balance not found"
            )
    }

    override suspend fun topUpEWallet(topUpEWalletRequest: TopUpEWalletRequest): TransactionResponse {
        return eWalletRepository.topUpEWallet(topUpEWalletRequest)
    }
}
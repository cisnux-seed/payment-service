package id.co.bni.payment.domains.dtos

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class TopUpEWalletRequest(
    @field:NotNull(message = "amount cannot be null")
    @field:Min(value = 10_000, message = "amount can't be less than 10000")
    val amount: BigDecimal = BigDecimal("0.00"),
    @field:NotBlank(message = "payment method cannot be blank")
    val paymentMethod: String,
    @field:NotBlank(message = "phone number cannot be blank")
    val phoneNumber: String,
    val description: String? = null
)
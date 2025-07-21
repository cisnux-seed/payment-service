package id.co.bni.payment.domains.entities

import id.co.bni.payment.commons.constants.AccountStatus
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.Instant

@Table("accounts")
data class Account(
    @Id
    val id: String,
    @Column("user_id")
    val userId: Long,
    val balance: BigDecimal = BigDecimal("0.00"),
    val currency: String = "IDR",
    @Column("account_status")
    val accountStatus: AccountStatus = AccountStatus.ACTIVE,
    @Column("created_at")
    val createdAt: Instant = Instant.now(),
    @Column("updated_at")
    val updatedAt: Instant = Instant.now()
)
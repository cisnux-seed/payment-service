package id.co.bni.payment.commons.configs

import id.co.bni.payment.commons.constants.AccountStatus
import io.r2dbc.spi.ConnectionFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions
import org.springframework.data.r2dbc.dialect.DialectResolver

//@Configuration
//class DatabaseConfig(private val connectionFactory: ConnectionFactory){
//    @Bean
//    fun r2dbcCustomConversions(connectionFactory: ConnectionFactory): R2dbcCustomConversions {
//        val dialect = DialectResolver.getDialect(connectionFactory)
//        val converters = listOf(
//            AccountStatusTypeConverter()
//        )
//        return R2dbcCustomConversions.of(dialect, converters)
//    }
//}
//
//@WritingConverter
//class AccountStatusTypeConverter : Converter<AccountStatus, AccountStatus> {
//    override fun convert(source: AccountStatus): AccountStatus? = source
//}

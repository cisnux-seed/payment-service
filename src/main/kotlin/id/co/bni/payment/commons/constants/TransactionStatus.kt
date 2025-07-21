package id.co.bni.payment.commons.constants

enum class TransactionStatus(val status: String) {
    PENDING("PENDING"),
    SUCCESS("SUCCESS"),
    FAILED("FAILED"),
    CANCELLED("CANCELLED"),
}
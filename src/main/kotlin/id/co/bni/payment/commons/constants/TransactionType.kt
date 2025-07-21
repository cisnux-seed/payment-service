package id.co.bni.payment.commons.constants

enum class TransactionType(val type: String) {
    TOPUP("TOPUP"),
    PAYMENT("PAYMENT"),
    REFUND("REFUND"),
    TRANSFER("TRANSFER"),
}
package com.iti.domain.payment.model

enum class PaymentMethodType {
    MOBILE_WALLET,
    CARD,
}

enum class WalletProvider(val egyptianPrefix: String) {
    VODAFONE_CASH("010"),
    ORANGE_CASH("012"),
    ETISALAT_CASH("011"),
    WE_PAY("015"),
}

enum class CardBrand {
    VISA,
    MASTERCARD,
}

data class PaymentIntention(
    val intentionId: String,
    val clientSecret: String,
    val amountMinorUnits: Long,
    val currencyCode: String,
)

enum class PaymentStatus {
    SUCCESS,
    FAILED,
    PENDING,
}

data class PaymentOutcome(
    val transactionId: String,
    val status: PaymentStatus,
    val failureReasonCode: String? = null,
)

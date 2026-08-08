package com.iti.data.payment

import com.iti.data.payment.dto.PaymentIntentionDto
import com.iti.data.payment.dto.PaymentOutcomeDto
import kotlinx.coroutines.delay
import java.util.UUID


class PaymentFakeDataSource : PaymentDataSource {

    override suspend fun createIntention(packageId: String, method: String): PaymentIntentionDto {
        delay(CREATE_INTENTION_DELAY_MS)
        return PaymentIntentionDto(
            intentionId = "fake_intent_${UUID.randomUUID()}",
            clientSecret = "fake_secret_${UUID.randomUUID()}",
            amountMinorUnits = FAKE_PACKAGE_PRICES[packageId] ?: DEFAULT_AMOUNT_MINOR_UNITS,
            currencyCode = "EGP",
        )
    }

    override suspend fun confirmWalletPayment(
        intentionId: String,
        walletProvider: String,
        walletNumber: String,
    ): PaymentOutcomeDto {
        delay(CONFIRM_PAYMENT_DELAY_MS)
        return outcomeFor(walletNumber)
    }

    override suspend fun confirmCardPayment(
        intentionId: String,
        cardBrand: String,
        cardNumber: String,
        expiry: String,
        cvv: String,
        cardholderName: String,
    ): PaymentOutcomeDto {
        delay(CONFIRM_PAYMENT_DELAY_MS)
        return outcomeFor(cardNumber)
    }

    /**
     * Last digit 0-7 => SUCCESS, 8 => PENDING, 9 => FAILED. Lets QA/demo force each outcome
     * branch on demand (e.g. end a wallet number in 9 to see the failure path) without a
     * separate debug flag, matching the "realistic fake" spirit of AlmahirFakeDataSource.
     */
    private fun outcomeFor(rawInput: String): PaymentOutcomeDto {
        val lastDigit = rawInput.filterNot { it.isWhitespace() }.lastOrNull { it.isDigit() }
            ?.digitToIntOrNull() ?: 0
        return when (lastDigit) {
            8 -> PaymentOutcomeDto(transactionId = "fake_txn_${UUID.randomUUID()}", status = "PENDING")
            9 -> PaymentOutcomeDto(
                transactionId = "fake_txn_${UUID.randomUUID()}",
                status = "FAILED",
                failureReasonCode = "fake_declined",
            )
            else -> PaymentOutcomeDto(transactionId = "fake_txn_${UUID.randomUUID()}", status = "SUCCESS")
        }
    }

    private companion object {
        const val CREATE_INTENTION_DELAY_MS = 500L
        const val CONFIRM_PAYMENT_DELAY_MS = 1_500L
        const val DEFAULT_AMOUNT_MINOR_UNITS = 4_000L

        // Mirrors the package seed prices in AlmahirFakeDataSource.packagesFor(...) so the
        // amount confirmed here agrees with what PackagesScreen/Checkout already displayed.
        val FAKE_PACKAGE_PRICES = mapOf(
            "pkg-light" to 4_000L,
            "pkg-intensive" to 6_500L,
            "pkg-elite" to 12_000L,
        )
    }
}

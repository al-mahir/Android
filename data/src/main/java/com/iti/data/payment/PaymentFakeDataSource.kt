package com.iti.data.payment

import com.iti.data.payment.dto.PaymentIntentionDto
import com.iti.data.payment.dto.PaymentOutcomeDto
import kotlinx.coroutines.delay
import java.util.UUID


/**
 * Fake payment data source used during development and for Compose Previews.
 *
 * Deterministic outcome rules (based on last digit of packageId or intentionId):
 *   0-7 → SUCCESS, 8 → PENDING, 9 → FAILED
 *
 * Keep this in the codebase — it is useful for previews, tests, and as a quick
 * rollback binding if the backend has an outage. To revert: swap the DI binding
 * in PaymentDataModule back to PaymentFakeDataSource().
 */
class PaymentFakeDataSource : PaymentDataSource {

    override suspend fun createIntention(
        packageId: String,
        method: String,
        idempotencyKey: String,
    ): PaymentIntentionDto {
        delay(CREATE_INTENTION_DELAY_MS)
        return PaymentIntentionDto(
            intentionId = "fake_intent_${UUID.randomUUID()}",
            clientSecret = "fake_secret_${UUID.randomUUID()}",
            publicKey = "fake_public_key",
            amountMinorUnits = FAKE_PACKAGE_PRICES[packageId] ?: DEFAULT_AMOUNT_MINOR_UNITS,
            currencyCode = "EGP",
        )
    }

    override suspend fun getPaymentStatus(intentionId: String): PaymentOutcomeDto {
        delay(CONFIRM_PAYMENT_DELAY_MS)
        return outcomeFor(intentionId)
    }

    /**
     * Last digit 0-7 => SUCCESS, 8 => PENDING, 9 => FAILED. Lets QA/demo force each outcome
     * branch on demand without a separate debug flag.
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

        val FAKE_PACKAGE_PRICES = mapOf(
            "pkg-light" to 4_000L,
            "pkg-intensive" to 6_500L,
            "pkg-elite" to 12_000L,
        )
    }
}

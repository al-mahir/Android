package com.iti.data.payment

import com.iti.data.payment.dto.PaymentIntentionDto
import com.iti.data.payment.dto.PaymentOutcomeDto
import com.iti.data.payment.dto.SubscriptionMinutesDto
import com.iti.data.payment.dto.SubscriptionPackageDto
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

    override suspend fun getPackages(): List<SubscriptionPackageDto> {
        delay(GET_PACKAGES_DELAY_MS)
        return FAKE_PACKAGES
    }

    override suspend fun getSubscriptionMinutes(): SubscriptionMinutesDto? {
        delay(GET_PACKAGES_DELAY_MS)
        return FAKE_SUBSCRIPTION_MINUTES
    }

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
            amountMinorUnits = FAKE_PACKAGES.firstOrNull { it.code == packageId }?.priceMinorUnits
                ?: DEFAULT_AMOUNT_MINOR_UNITS,
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
        const val GET_PACKAGES_DELAY_MS = 500L
        const val CREATE_INTENTION_DELAY_MS = 500L
        const val CONFIRM_PAYMENT_DELAY_MS = 1_500L
        const val DEFAULT_AMOUNT_MINOR_UNITS = 4_000L

        /** Set to `null` to exercise the "no subscription" branch of the profile/meeting flows. */
        val FAKE_SUBSCRIPTION_MINUTES = SubscriptionMinutesDto(
            packageName = "Intensive",
            totalMinutes = 600,
            remainingMinutes = 145,
            startedAt = "2026-08-01T09:00:00Z",
            expiresAt = "2026-08-31T09:00:00Z",
        )

        /** Mirrors the shape of `GET /api/payment/packages`, not any specific backend row. */
        val FAKE_PACKAGES = listOf(
            SubscriptionPackageDto(
                code = "LIGHT",
                name = "Light",
                description = "A gentle start for weekly revision.",
                priceMinorUnits = 4_000,
                currencyCode = "EGP",
                meetingMinutesAllowed = 240,
                durationDays = 30,
                features = listOf(
                    "30-minute sessions",
                    "Weekly progress report",
                    "Group correction sessions",
                ),
            ),
            SubscriptionPackageDto(
                code = "INTENSIVE",
                name = "Intensive",
                description = "Daily practice with direct feedback.",
                priceMinorUnits = 6_500,
                currencyCode = "EGP",
                meetingMinutesAllowed = 600,
                durationDays = 30,
                features = listOf(
                    "45-minute sessions",
                    "Personalized revision plan",
                    "Direct feedback + recordings",
                    "Priority scheduling",
                ),
            ),
            SubscriptionPackageDto(
                code = "ELITE",
                name = "Elite",
                description = "One-on-one guidance all year round.",
                priceMinorUnits = 12_000,
                currencyCode = "EGP",
                meetingMinutesAllowed = 1_200,
                durationDays = 365,
                features = listOf(
                    "60-minute sessions",
                    "1-on-1 dedicated sheikh",
                    "Unlimited feedback + recordings",
                    "Priority scheduling",
                ),
            ),
        )
    }
}

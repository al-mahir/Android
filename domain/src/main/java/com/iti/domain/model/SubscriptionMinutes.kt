package com.iti.domain.model

/**
 * The student's live subscription entitlement, as reported by
 * `GET /api/students/me/subscription-minutes`.
 *
 * This — not [Subscription] — is the source of truth for "can this student book a meeting":
 * a subscription can be present yet unusable because it expired or its minutes ran out.
 */
data class SubscriptionMinutes(
    val packageName: String?,
    val totalMinutes: Int,
    val remainingMinutes: Int,
    val startedAtEpochMillis: Long?,
    val expiresAtEpochMillis: Long?,
) {
    val usedMinutes: Int get() = (totalMinutes - remainingMinutes).coerceAtLeast(0)

    /** 0f..1f; 0f when the package reports no total, so the UI never divides by zero. */
    val usedFraction: Float
        get() = if (totalMinutes <= 0) 0f else (usedMinutes.toFloat() / totalMinutes).coerceIn(0f, 1f)

    fun isExpiredAt(nowEpochMillis: Long): Boolean =
        expiresAtEpochMillis != null && expiresAtEpochMillis <= nowEpochMillis

    fun isUsable(nowEpochMillis: Long): Boolean =
        remainingMinutes > 0 && !isExpiredAt(nowEpochMillis)
}

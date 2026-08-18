package com.iti.domain.model

/**
 * Whether the student may start a meeting request right now.
 *
 * [Unverified] is deliberately *not* a refusal: when the entitlement check itself fails (offline,
 * 5xx), blocking would lock a paying student out over a transient error. The backend enforces the
 * quota authoritatively on the request itself, so the client lets the attempt through and lets the
 * server reject it if it must.
 */
sealed interface MeetingEligibility {

    data class Allowed(val remainingMinutes: Int) : MeetingEligibility

    data object NoSubscription : MeetingEligibility

    data class Expired(val packageName: String?) : MeetingEligibility

    data class NoMinutesLeft(val packageName: String?) : MeetingEligibility

    data class NotEnoughMinutes(
        val remainingMinutes: Int,
        val requiredMinutes: Int,
    ) : MeetingEligibility

    data object Unverified : MeetingEligibility

    val allowsRequest: Boolean get() = this is Allowed || this is Unverified
}

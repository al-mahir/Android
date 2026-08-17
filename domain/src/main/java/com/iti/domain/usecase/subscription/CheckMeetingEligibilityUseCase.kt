package com.iti.domain.usecase.subscription

import com.iti.domain.core.Result
import com.iti.domain.model.MeetingEligibility
import com.iti.domain.model.SubscriptionMinutes

/**
 * Decides whether the student can request a meeting, based on their live minute entitlement.
 *
 * Ordering matters: an expired package is reported as expired even if minutes remain, because
 * "renew" and "top up" are different actions for the student.
 *
 * [nowProvider] is injected so the expiry boundary is testable.
 */
class CheckMeetingEligibilityUseCase(
    private val getSubscriptionMinutes: GetSubscriptionMinutesUseCase,
    private val nowProvider: () -> Long = System::currentTimeMillis,
) {
    suspend operator fun invoke(requiredMinutes: Int = DEFAULT_REQUIRED_MINUTES): MeetingEligibility =
        when (val result = getSubscriptionMinutes()) {
            is Result.Error -> MeetingEligibility.Unverified
            is Result.Success -> evaluate(result.data, requiredMinutes)
        }

    private fun evaluate(minutes: SubscriptionMinutes?, requiredMinutes: Int): MeetingEligibility {
        if (minutes == null) return MeetingEligibility.NoSubscription

        val now = nowProvider()
        return when {
            minutes.isExpiredAt(now) -> MeetingEligibility.Expired(minutes.packageName)
            minutes.remainingMinutes <= 0 -> MeetingEligibility.NoMinutesLeft(minutes.packageName)
            minutes.remainingMinutes < requiredMinutes -> MeetingEligibility.NotEnoughMinutes(
                remainingMinutes = minutes.remainingMinutes,
                requiredMinutes = requiredMinutes,
            )
            else -> MeetingEligibility.Allowed(minutes.remainingMinutes)
        }
    }

    companion object {
        /** Shortest slot the sheikh side will accept, so anything less can never be booked. */
        const val DEFAULT_REQUIRED_MINUTES = 15
    }
}

package com.iti.presentation.profile.state

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.iti.domain.model.Subscription
import com.iti.domain.model.SubscriptionMinutes
import com.iti.domain.model.SubscriptionPlan
import com.iti.domain.model.User
import com.iti.meeting.domain.model.circle.Circle

enum class ProfileDialog { LOGOUT, DELETE_ACCOUNT }

@Immutable
data class ProfileUiState(
    val isLoading: Boolean = true,
    @StringRes val errorMessageRes: Int? = null,
    val user: User? = null,
    val subscription: Subscription? = null,
    val subscriptionMinutes: SubscriptionMinutes? = null,
    /**
     * False in apps with no payment graph (the sheikh app, whose users earn rather than pay).
     * The whole subscription section — status, balance card and packages CTA — is then omitted.
     */
    val isSubscriptionSupported: Boolean = true,
    val isLoadingMinutes: Boolean = true,
    val nowEpochMillis: Long = 0L,
    val dialog: ProfileDialog? = null,
    val isProcessingDialogAction: Boolean = false,
    val isOffline: Boolean = false,
    val myCircles: List<Circle> = emptyList(),
    val availableCircles: List<Circle> = emptyList(),
) {
    val hasError: Boolean get() = errorMessageRes != null && user == null

    /**
     * Subscribed means "has a live entitlement", not merely "has a row" — an expired or fully
     * consumed package must send the student back to the packages list, not to a status screen
     * they can do nothing with. Falls back to the plan flag only while the minutes call is still
     * in flight, so the UI does not flicker from premium to free and back.
     */
    val isSubscribed: Boolean
        get() = subscriptionMinutes?.isUsable(nowEpochMillis)
            ?: (isLoadingMinutes && subscription?.plan == SubscriptionPlan.PREMIUM)

    /** Has a package, but it is expired or out of minutes — prompt to renew rather than to buy. */
    val needsRenewal: Boolean
        get() = subscriptionMinutes != null && !subscriptionMinutes.isUsable(nowEpochMillis)

    val isPremium: Boolean get() = isSubscribed
}

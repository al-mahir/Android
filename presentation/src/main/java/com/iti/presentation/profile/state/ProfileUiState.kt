package com.iti.presentation.profile.state

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.iti.domain.model.Subscription
import com.iti.domain.model.SubscriptionPlan
import com.iti.domain.model.User

enum class ProfileDialog { LOGOUT, DELETE_ACCOUNT }

@Immutable
data class ProfileUiState(
    val isLoading: Boolean = true,
    @StringRes val errorMessageRes: Int? = null,
    val user: User? = null,
    val subscription: Subscription? = null,
    val dialog: ProfileDialog? = null,
    val isProcessingDialogAction: Boolean = false,
    val isOffline: Boolean = false,
) {
    val hasError: Boolean get() = errorMessageRes != null && user == null

    val isPremium: Boolean get() = subscription?.plan == SubscriptionPlan.PREMIUM
}

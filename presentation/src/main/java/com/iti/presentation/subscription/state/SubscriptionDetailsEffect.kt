package com.iti.presentation.subscription.state

import androidx.annotation.StringRes

sealed interface SubscriptionDetailsEffect {

    data object NavigateBack : SubscriptionDetailsEffect

    data class ShowMessage(@StringRes val messageRes: Int) : SubscriptionDetailsEffect
}

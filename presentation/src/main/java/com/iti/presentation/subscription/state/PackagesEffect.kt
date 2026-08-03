package com.iti.presentation.subscription.state

import androidx.annotation.StringRes

sealed interface PackagesEffect {

    data object PurchaseCompleted : PackagesEffect

    data class ShowMessage(@StringRes val messageRes: Int) : PackagesEffect
}

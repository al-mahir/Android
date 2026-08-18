package com.iti.presentation.subscription.state

import androidx.annotation.StringRes

sealed interface PackagesEffect {

    data class NavigateToCheckout(val packageId: String) : PackagesEffect

    data class ShowMessage(@StringRes val messageRes: Int) : PackagesEffect
}

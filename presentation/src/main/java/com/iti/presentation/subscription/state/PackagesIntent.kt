package com.iti.presentation.subscription.state

sealed interface PackagesIntent {

    data object Retry : PackagesIntent

    data class SelectPackageClicked(val packageId: String) : PackagesIntent

    data object StartFreeTrialClicked : PackagesIntent
}

package com.iti.presentation.subscription.state

sealed interface SubscriptionDetailsIntent {

    data object Retry : SubscriptionDetailsIntent

    data object BackClicked : SubscriptionDetailsIntent

    data object ReturnSubscriptionClicked : SubscriptionDetailsIntent

    data object ReturnSheetDismissed : SubscriptionDetailsIntent

    data class CancellationMessageChanged(val message: String) : SubscriptionDetailsIntent

    data object SendCancellationMessageClicked : SubscriptionDetailsIntent
}

package com.iti.presentation.subscription.state

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.iti.domain.model.Subscription
import com.iti.domain.model.SubscriptionPackage

@Immutable
data class SubscriptionDetailsUiState(
    val isLoading: Boolean = true,
    @StringRes val errorMessageRes: Int? = null,
    val subscription: Subscription? = null,
    val activePackage: SubscriptionPackage? = null,
    val isReturnSheetVisible: Boolean = false,
    val cancellationMessage: String = "",
    val isSendingCancellationMessage: Boolean = false,
    val cancellationMessageSent: Boolean = false,
) {
    val hasError: Boolean get() = errorMessageRes != null && subscription == null
}

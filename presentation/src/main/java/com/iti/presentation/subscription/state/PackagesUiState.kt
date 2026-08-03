package com.iti.presentation.subscription.state

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.iti.domain.model.SubscriptionPackage

@Immutable
data class PackagesUiState(
    val isLoading: Boolean = true,
    @StringRes val errorMessageRes: Int? = null,
    val packages: List<SubscriptionPackage> = emptyList(),
    val processingPackageId: String? = null,
    val isStartingTrial: Boolean = false,
) {
    val hasError: Boolean get() = errorMessageRes != null && packages.isEmpty()
}

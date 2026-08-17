package com.iti.presentation.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.domain.core.fold
import com.iti.domain.usecase.subscription.GetSubscriptionPackagesUseCase
import com.iti.presentation.R
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import com.iti.presentation.subscription.state.PackagesEffect
import com.iti.presentation.subscription.state.PackagesIntent
import com.iti.presentation.subscription.state.PackagesUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PackagesViewModel(
    private val getSubscriptionPackages: GetSubscriptionPackagesUseCase,
) : ViewModel(),
    StateHolder<PackagesUiState> by DefaultStateHolder(PackagesUiState()),
    EffectPublisher<PackagesEffect> by DefaultEffectPublisher() {

    private var loadJob: Job? = null

    init {
        loadPackages()
    }

    fun onIntent(intent: PackagesIntent) {
        when (intent) {
            PackagesIntent.Retry -> loadPackages()
            is PackagesIntent.SelectPackageClicked -> selectPackage(intent.packageId)
        }
    }

    private fun loadPackages() {
        // Retry taps while a fetch is already in flight would otherwise race, and the loser's
        // result would overwrite the winner's.
        loadJob?.cancel()
        updateState { copy(isLoading = true, errorMessageRes = null) }

        loadJob = viewModelScope.launch {
            getSubscriptionPackages().fold(
                onSuccess = { packages ->
                    updateState { copy(isLoading = false, errorMessageRes = null, packages = packages) }
                },
                onError = {
                    updateState { copy(isLoading = false, errorMessageRes = R.string.packages_error_generic) }
                },
            )
        }
    }

    private fun selectPackage(packageId: String) {
        if (currentState.processingPackageId != null) return
        sendEffect(PackagesEffect.NavigateToCheckout(packageId))
    }
}

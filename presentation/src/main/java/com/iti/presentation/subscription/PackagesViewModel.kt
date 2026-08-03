package com.iti.presentation.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.domain.core.Result
import com.iti.domain.core.fold
import com.iti.domain.usecase.subscription.GetSubscriptionPackagesUseCase
import com.iti.domain.usecase.subscription.SelectSubscriptionPackageUseCase
import com.iti.domain.usecase.subscription.StartFreeTrialUseCase
import com.iti.presentation.R
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import com.iti.presentation.subscription.state.PackagesEffect
import com.iti.presentation.subscription.state.PackagesIntent
import com.iti.presentation.subscription.state.PackagesUiState
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class PackagesViewModel(
    private val getSubscriptionPackages: GetSubscriptionPackagesUseCase,
    private val selectSubscriptionPackage: SelectSubscriptionPackageUseCase,
    private val startFreeTrial: StartFreeTrialUseCase,
) : ViewModel(),
    StateHolder<PackagesUiState> by DefaultStateHolder(PackagesUiState()),
    EffectPublisher<PackagesEffect> by DefaultEffectPublisher() {

    init {
        loadPackages()
    }

    fun onIntent(intent: PackagesIntent) {
        when (intent) {
            PackagesIntent.Retry -> loadPackages()
            is PackagesIntent.SelectPackageClicked -> selectPackage(intent.packageId)
            PackagesIntent.StartFreeTrialClicked -> startTrial()
        }
    }

    private fun loadPackages() {
        updateState { copy(isLoading = true, errorMessageRes = null) }

        getSubscriptionPackages()
            .catch {
                updateState { copy(isLoading = false, errorMessageRes = R.string.packages_error_generic) }
            }
            .onEach { result ->
                result.fold(
                    onSuccess = { packages ->
                        updateState { copy(isLoading = false, errorMessageRes = null, packages = packages) }
                    },
                    onError = {
                        updateState { copy(isLoading = false, errorMessageRes = R.string.packages_error_generic) }
                    },
                )
            }
            .launchIn(viewModelScope)
    }

    private fun selectPackage(packageId: String) {
        if (currentState.processingPackageId != null || currentState.isStartingTrial) return
        updateState { copy(processingPackageId = packageId) }

        viewModelScope.launch {
            val succeeded = selectSubscriptionPackage(packageId) is Result.Success
            updateState { copy(processingPackageId = null) }
            completePurchase(succeeded)
        }
    }

    private fun startTrial() {
        if (currentState.processingPackageId != null || currentState.isStartingTrial) return
        updateState { copy(isStartingTrial = true) }

        viewModelScope.launch {
            val succeeded = startFreeTrial() is Result.Success
            updateState { copy(isStartingTrial = false) }
            completePurchase(succeeded, isTrial = true)
        }
    }

    private fun completePurchase(succeeded: Boolean, isTrial: Boolean = false) {
        if (succeeded) {
            sendEffect(
                PackagesEffect.ShowMessage(
                    if (isTrial) R.string.packages_trial_started else R.string.packages_purchase_succeeded
                )
            )
            sendEffect(PackagesEffect.PurchaseCompleted)
        } else {
            sendEffect(PackagesEffect.ShowMessage(R.string.packages_purchase_failed))
        }
    }
}

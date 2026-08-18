package com.iti.presentation.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.domain.core.Result
import com.iti.domain.core.getOrNull
import com.iti.domain.usecase.subscription.GetSubscriptionPackagesUseCase
import com.iti.domain.usecase.subscription.GetSubscriptionUseCase
import com.iti.domain.usecase.subscription.RequestSubscriptionCancellationUseCase
import com.iti.presentation.R
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import com.iti.presentation.subscription.state.SubscriptionDetailsEffect
import com.iti.presentation.subscription.state.SubscriptionDetailsIntent
import com.iti.presentation.subscription.state.SubscriptionDetailsUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SubscriptionDetailsViewModel(
    private val getSubscription: GetSubscriptionUseCase,
    private val getSubscriptionPackages: GetSubscriptionPackagesUseCase,
    private val requestSubscriptionCancellation: RequestSubscriptionCancellationUseCase,
) : ViewModel(),
    StateHolder<SubscriptionDetailsUiState> by DefaultStateHolder(SubscriptionDetailsUiState()),
    EffectPublisher<SubscriptionDetailsEffect> by DefaultEffectPublisher() {

    private var observeJob: Job? = null

    init {
        observeSubscription()
    }

    fun onIntent(intent: SubscriptionDetailsIntent) {
        when (intent) {
            SubscriptionDetailsIntent.Retry -> observeSubscription()
            SubscriptionDetailsIntent.BackClicked -> sendEffect(SubscriptionDetailsEffect.NavigateBack)
            SubscriptionDetailsIntent.ReturnSubscriptionClicked -> openReturnSheet()
            SubscriptionDetailsIntent.ReturnSheetDismissed -> dismissReturnSheet()
            is SubscriptionDetailsIntent.CancellationMessageChanged ->
                updateState { copy(cancellationMessage = intent.message) }
            SubscriptionDetailsIntent.SendCancellationMessageClicked -> sendCancellationMessage()
        }
    }

    private fun observeSubscription() {
        updateState { copy(isLoading = true, errorMessageRes = null) }
        observeJob?.cancel()

        observeJob = viewModelScope.launch {
            // The catalogue is a one-shot GET, so it is fetched once here rather than re-read on
            // every subscription emission. A failure to load it is not fatal: the subscription
            // itself still renders, just without the package details.
            val packages = getSubscriptionPackages().getOrNull().orEmpty()

            getSubscription()
                .catch {
                    updateState {
                        copy(isLoading = false, errorMessageRes = R.string.subscription_details_error_generic)
                    }
                }
                .onEach { subscriptionResult ->
                    val subscription = subscriptionResult.getOrNull()
                    if (subscription == null) {
                        updateState {
                            copy(isLoading = false, errorMessageRes = R.string.subscription_details_error_generic)
                        }
                        return@onEach
                    }
                    updateState {
                        copy(
                            isLoading = false,
                            errorMessageRes = null,
                            subscription = subscription,
                            activePackage = packages.firstOrNull { it.code == subscription.activePackageId },
                        )
                    }
                }
                .collect()
        }
    }

    private fun openReturnSheet() {
        updateState {
            copy(isReturnSheetVisible = true, cancellationMessage = "", cancellationMessageSent = false)
        }
    }

    private fun dismissReturnSheet() {
        if (currentState.isSendingCancellationMessage) return
        updateState {
            copy(isReturnSheetVisible = false, cancellationMessage = "", cancellationMessageSent = false)
        }
    }

    private fun sendCancellationMessage() {
        val message = currentState.cancellationMessage
        if (message.isBlank() || currentState.isSendingCancellationMessage) return
        updateState { copy(isSendingCancellationMessage = true) }

        viewModelScope.launch {
            val succeeded = requestSubscriptionCancellation(message) is Result.Success
            if (succeeded) {
                updateState { copy(isSendingCancellationMessage = false, cancellationMessageSent = true) }
            } else {
                updateState { copy(isSendingCancellationMessage = false) }
                sendEffect(SubscriptionDetailsEffect.ShowMessage(R.string.subscription_cancellation_error))
            }
        }
    }
}

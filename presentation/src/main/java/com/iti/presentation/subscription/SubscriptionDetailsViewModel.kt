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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SubscriptionDetailsViewModel(
    private val getSubscription: GetSubscriptionUseCase,
    private val getSubscriptionPackages: GetSubscriptionPackagesUseCase,
    private val requestSubscriptionCancellation: RequestSubscriptionCancellationUseCase,
) : ViewModel(),
    StateHolder<SubscriptionDetailsUiState> by DefaultStateHolder(SubscriptionDetailsUiState()),
    EffectPublisher<SubscriptionDetailsEffect> by DefaultEffectPublisher() {

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

        combine(getSubscription(), getSubscriptionPackages()) { subscriptionResult, packagesResult ->
            val subscription = subscriptionResult.getOrNull() ?: error("Failed to load subscription")
            val packages = packagesResult.getOrNull().orEmpty()
            subscription to packages.firstOrNull { it.id == subscription.activePackageId }
        }
            .catch {
                updateState {
                    copy(isLoading = false, errorMessageRes = R.string.subscription_details_error_generic)
                }
            }
            .onEach { (subscription, activePackage) ->
                updateState {
                    copy(
                        isLoading = false,
                        errorMessageRes = null,
                        subscription = subscription,
                        activePackage = activePackage,
                    )
                }
            }
            .launchIn(viewModelScope)
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

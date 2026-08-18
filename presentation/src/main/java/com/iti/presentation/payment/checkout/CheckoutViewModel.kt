package com.iti.presentation.payment.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.designsystem.text.UiText
import com.iti.domain.core.Result
import com.iti.domain.core.fold
import com.iti.domain.core.onSuccess
import com.iti.domain.payment.model.PaymentStatus
import com.iti.domain.payment.usecase.ActivateSubscriptionAfterPaymentUseCase
import com.iti.domain.payment.usecase.CreatePaymentIntentionUseCase
import com.iti.domain.payment.usecase.GetPaymentStatusUseCase
import com.iti.domain.usecase.subscription.GetSubscriptionPackagesUseCase
import com.iti.domain.usecase.user.GetCurrentUserUseCase
import com.iti.presentation.R
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID

class CheckoutViewModel(
    private val packageId: String,
    private val getSubscriptionPackages: GetSubscriptionPackagesUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val createPaymentIntention: CreatePaymentIntentionUseCase,
    private val getPaymentStatus: GetPaymentStatusUseCase,
    private val activateSubscriptionAfterPayment: ActivateSubscriptionAfterPaymentUseCase,
) : ViewModel(),
    StateHolder<CheckoutUiState> by DefaultStateHolder(CheckoutUiState()),
    EffectPublisher<CheckoutEffect> by DefaultEffectPublisher() {

    private var loadJob: Job? = null

    init {
        loadPackage()
        loadUser()
    }

    fun onIntent(intent: CheckoutIntent) {
        when (intent) {
            is CheckoutIntent.TabSelected -> updateState {
                copy(selectedMethod = CheckoutUiState.methodForTab(intent.index))
            }
            CheckoutIntent.PayClicked -> submitPayment()
            CheckoutIntent.OverlayDismissed -> handleOverlayDismissed()
            CheckoutIntent.RetryLoadClicked -> loadPackage()
            is CheckoutIntent.PaymobSdkResult -> handleSdkResult(intent.outcome)
        }
    }

    private fun loadPackage() {
        loadJob?.cancel()
        updateState { copy(isLoadingPackage = true, loadError = null) }

        loadJob = viewModelScope.launch {
            getSubscriptionPackages().fold(
                onSuccess = { packages ->
                    val pkg = packages.firstOrNull { it.code == packageId }
                    if (pkg != null) {
                        updateState { copy(isLoadingPackage = false, loadError = null, pkg = pkg) }
                    } else {
                        updateState {
                            copy(isLoadingPackage = false, loadError = UiText.Resource(R.string.checkout_package_unavailable))
                        }
                    }
                },
                onError = {
                    updateState {
                        copy(isLoadingPackage = false, loadError = UiText.Resource(R.string.checkout_load_error))
                    }
                },
            )
        }
    }

    private fun loadUser() {
        getCurrentUser()
            .onEach { result -> result.onSuccess { user -> updateState { copy(userDisplayName = user.displayName) } } }
            .launchIn(viewModelScope)
    }

    private fun submitPayment() {
        if (currentState.isProcessing) return
        currentState.pkg ?: return

        val idempotencyKey = UUID.randomUUID().toString()
        updateState { copy(overlay = CheckoutOverlay.Loading, pendingIntentionId = null) }

        viewModelScope.launch {
            when (val intentionResult = createPaymentIntention(packageId, currentState.selectedMethod, idempotencyKey)) {
                is Result.Error -> updateState {
                    copy(overlay = CheckoutOverlay.Error(UiText.Resource(R.string.checkout_error_intention_failed)))
                }
                is Result.Success -> {
                    val intention = intentionResult.data
                    // Dismiss the loading overlay before the SDK launches its own UI, otherwise
                    // our overlay sits on top of the Paymob sheet.
                    updateState { copy(pendingIntentionId = intention.intentionId, overlay = null) }
                    sendEffect(CheckoutEffect.LaunchPaymobSdk(intention.clientSecret, intention.publicKey))
                }
            }
        }
    }

    /**
     * Called when the Paymob SDK reports back. We never trust the SDK callback alone — we always
     * confirm with the backend status endpoint before activating the subscription.
     *
     * On FAILURE the SDK callback is definitive; no need to poll. On CANCELLED the user backed
     * out deliberately, so we return them to the form silently rather than showing an error.
     * On SUCCESS or PENDING we poll up to [MAX_STATUS_POLLS] times with [STATUS_POLL_DELAY_MS]
     * between each attempt.
     */
    private fun handleSdkResult(outcome: PaymobSdkOutcome) {
        when (outcome) {
            PaymobSdkOutcome.Cancelled -> {
                updateState { copy(overlay = null, pendingIntentionId = null) }
                return
            }
            is PaymobSdkOutcome.Failure -> {
                updateState {
                    copy(
                        overlay = CheckoutOverlay.Error(UiText.Resource(R.string.checkout_payment_failed)),
                        pendingIntentionId = null,
                    )
                }
                return
            }
            else -> Unit
        }

        val intentionId = currentState.pendingIntentionId
        if (intentionId == null) {
            updateState {
                copy(overlay = CheckoutOverlay.Error(UiText.Resource(R.string.checkout_payment_failed)))
            }
            return
        }

        updateState { copy(overlay = CheckoutOverlay.Loading) }
        viewModelScope.launch {
            pollStatus(intentionId)
        }
    }

    private suspend fun pollStatus(intentionId: String) {
        repeat(MAX_STATUS_POLLS) { attempt ->
            val isLastAttempt = attempt == MAX_STATUS_POLLS - 1
            when (val statusResult = getPaymentStatus(intentionId)) {
                is Result.Error -> {
                    if (isLastAttempt) {
                        updateState {
                            copy(overlay = CheckoutOverlay.Error(UiText.Resource(R.string.checkout_payment_failed)))
                        }
                        return
                    }
                    delay(STATUS_POLL_DELAY_MS)
                }
                is Result.Success -> when (statusResult.data.status) {
                    PaymentStatus.SUCCESS -> {
                        activateSubscriptionAfterPayment(packageId)
                        updateState {
                            copy(
                                overlay = CheckoutOverlay.Success(UiText.Resource(R.string.checkout_payment_success)),
                                pendingIntentionId = null,
                            )
                        }
                        return
                    }
                    PaymentStatus.FAILED -> {
                        updateState {
                            copy(
                                overlay = CheckoutOverlay.Error(UiText.Resource(R.string.checkout_payment_failed)),
                                pendingIntentionId = null,
                            )
                        }
                        return
                    }
                    PaymentStatus.PENDING -> {
                        if (isLastAttempt) {
                            // Exhausted retries while still PENDING — tell user payment is processing
                            updateState {
                                copy(
                                    overlay = CheckoutOverlay.Success(UiText.Resource(R.string.checkout_payment_pending)),
                                    pendingIntentionId = null,
                                )
                            }
                        } else {
                            delay(STATUS_POLL_DELAY_MS)
                        }
                    }
                }
            }
        }
    }

    private fun handleOverlayDismissed() {
        val wasSuccess = currentState.overlay is CheckoutOverlay.Success
        updateState { copy(overlay = null) }
        if (wasSuccess) sendEffect(CheckoutEffect.NavigateBack)
    }

    private companion object {
        const val MAX_STATUS_POLLS = 3

        const val STATUS_POLL_DELAY_MS = 2_000L
    }
}

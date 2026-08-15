package com.iti.presentation.payment.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.designsystem.text.UiText
import com.iti.domain.core.Result
import com.iti.domain.core.fold
import com.iti.domain.core.onSuccess
import com.iti.domain.payment.model.PaymentStatus
import com.iti.domain.payment.model.PaymentMethodType
import com.iti.domain.payment.model.WalletProvider
import com.iti.domain.payment.model.CardBrand
import com.iti.domain.payment.usecase.ActivateSubscriptionAfterPaymentUseCase
import com.iti.domain.payment.usecase.CreatePaymentIntentionUseCase
import com.iti.domain.payment.usecase.GetPaymentStatusUseCase
import com.iti.domain.payment.usecase.validation.CardValidators
import com.iti.domain.payment.usecase.validation.WalletNumberValidator
import com.iti.domain.usecase.subscription.GetSubscriptionPackagesUseCase
import com.iti.domain.usecase.user.GetCurrentUserUseCase
import com.iti.presentation.R
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
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

    init {
        loadPackage()
        loadUser()
    }

    fun onIntent(intent: CheckoutIntent) {
        when (intent) {
            is CheckoutIntent.TabSelected -> updateState { copy(selectedTabIndex = intent.index) }

            is CheckoutIntent.WalletProviderSelected -> updateState {
                copy(
                    selectedWalletProvider = intent.provider,
                    walletNumberError = if (walletNumber.isBlank()) null else validateWalletNumber(walletNumber, intent.provider),
                )
            }

            is CheckoutIntent.WalletNumberChanged -> updateState {
                copy(walletNumber = intent.value, walletNumberError = null)
            }

            is CheckoutIntent.CardBrandSelected -> updateState {
                copy(
                    selectedCardBrand = intent.brand,
                    cardNumberError = if (cardNumber.isBlank()) null else validateCardNumber(cardNumber, intent.brand),
                )
            }

            is CheckoutIntent.CardNumberChanged -> updateState {
                copy(cardNumber = intent.value.digitsOnly(CARD_NUMBER_MAX_DIGITS), cardNumberError = null)
            }
            is CheckoutIntent.ExpiryChanged -> updateState {
                copy(expiry = intent.value.digitsOnly(EXPIRY_MAX_DIGITS), expiryError = null)
            }
            is CheckoutIntent.CvvChanged -> updateState {
                copy(cvv = intent.value.digitsOnly(CVV_MAX_DIGITS), cvvError = null)
            }
            is CheckoutIntent.CardholderNameChanged -> updateState {
                copy(cardholderName = intent.value, cardholderNameError = null)
            }

            CheckoutIntent.PayClicked -> submitPayment()
            CheckoutIntent.OverlayDismissed -> handleOverlayDismissed()
            CheckoutIntent.RetryLoadClicked -> loadPackage()
            is CheckoutIntent.PaymobSdkResult -> handleSdkResult(intent.outcome)
        }
    }

    private fun loadPackage() {
        updateState { copy(isLoadingPackage = true, loadError = null) }

        getSubscriptionPackages()
            .catch { updateState { copy(isLoadingPackage = false, loadError = UiText.Resource(R.string.checkout_load_error)) } }
            .onEach { result ->
                result.fold(
                    onSuccess = { packages ->
                        val pkg = packages.firstOrNull { it.id == packageId }
                        if (pkg != null) {
                            updateState { copy(isLoadingPackage = false, loadError = null, pkg = pkg) }
                        } else {
                            updateState { copy(isLoadingPackage = false, loadError = UiText.Resource(R.string.checkout_load_error)) }
                        }
                    },
                    onError = {
                        updateState { copy(isLoadingPackage = false, loadError = UiText.Resource(R.string.checkout_load_error)) }
                    },
                )
            }
            .launchIn(viewModelScope)
    }

    private fun loadUser() {
        getCurrentUser()
            .onEach { result -> result.onSuccess { user -> updateState { copy(userDisplayName = user.displayName) } } }
            .launchIn(viewModelScope)
    }

    private fun submitPayment() {
        if (currentState.isProcessing) return
        currentState.pkg ?: return
        val method = methodForSelectedTab()

        val isValid = if (method == PaymentMethodType.MOBILE_WALLET) validateWalletForm() else validateCardForm()
        if (!isValid) return

        val idempotencyKey = UUID.randomUUID().toString()
        updateState { copy(overlay = CheckoutOverlay.Loading, pendingIntentionId = null) }

        viewModelScope.launch {
            when (val intentionResult = createPaymentIntention(packageId, method, idempotencyKey)) {
                is Result.Error -> updateState {
                    copy(overlay = CheckoutOverlay.Error(UiText.Resource(R.string.checkout_error_intention_failed)))
                }
                is Result.Success -> {
                    val intention = intentionResult.data
                    updateState { copy(pendingIntentionId = intention.intentionId) }
                    // Dismiss loading overlay before SDK launches its own UI
                    updateState { copy(overlay = null) }
                    sendEffect(CheckoutEffect.LaunchPaymobSdk(intention.clientSecret, intention.publicKey))
                }
            }
        }
    }

    /**
     * Called when the Paymob SDK reports back. We never trust the SDK callback alone — we always
     * confirm with the backend status endpoint before activating the subscription.
     *
     * On FAILURE the SDK callback is definitive; no need to poll.
     * On SUCCESS or PENDING we poll up to [MAX_STATUS_POLLS] times with [STATUS_POLL_DELAY_MS]
     * between each attempt.
     */
    private fun handleSdkResult(outcome: PaymobSdkOutcome) {
        if (outcome is PaymobSdkOutcome.Failure) {
            updateState {
                copy(overlay = CheckoutOverlay.Error(UiText.Resource(R.string.checkout_payment_failed)))
            }
            return
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
            when (val statusResult = getPaymentStatus(intentionId)) {
                is Result.Error -> {
                    if (attempt == MAX_STATUS_POLLS - 1) {
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
                        if (attempt < MAX_STATUS_POLLS - 1) {
                            delay(STATUS_POLL_DELAY_MS)
                        } else {
                            // Exhausted retries while still PENDING — tell user payment is processing
                            updateState {
                                copy(
                                    overlay = CheckoutOverlay.Success(UiText.Resource(R.string.checkout_payment_pending)),
                                    pendingIntentionId = null,
                                )
                            }
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

    private fun methodForSelectedTab(): PaymentMethodType =
        if (currentState.selectedTabIndex == 0) PaymentMethodType.MOBILE_WALLET else PaymentMethodType.CARD

    private fun validateWalletForm(): Boolean {
        val provider = currentState.selectedWalletProvider ?: return false
        val error = validateWalletNumber(currentState.walletNumber, provider)
        updateState { copy(walletNumberError = error) }
        return error == null
    }

    private fun validateWalletNumber(number: String, provider: WalletProvider): UiText? = when {
        !WalletNumberValidator.isValidEgyptianMobile(number) -> UiText.Resource(R.string.checkout_error_wallet_number_invalid)
        !WalletNumberValidator.matchesCarrierPrefix(number, provider) -> UiText.Resource(provider.prefixErrorRes())
        else -> null
    }

    private fun validateCardForm(): Boolean {
        val brand = currentState.selectedCardBrand
        val numberError = if (brand == null) {
            UiText.Resource(R.string.checkout_error_card_number_invalid)
        } else {
            validateCardNumber(currentState.cardNumber, brand)
        }
        val expiryError = if (CardValidators.isValidExpiry(currentState.expiry.asMonthYear())) {
            null
        } else {
            UiText.Resource(R.string.checkout_error_expiry_invalid)
        }
        val cvvError = if (CardValidators.isValidCvv(currentState.cvv)) null else UiText.Resource(R.string.checkout_error_cvv_invalid)
        val nameError = if (CardValidators.isNonBlankName(currentState.cardholderName)) {
            null
        } else {
            UiText.Resource(R.string.checkout_error_cardholder_name_required)
        }

        updateState {
            copy(
                cardNumberError = numberError,
                expiryError = expiryError,
                cvvError = cvvError,
                cardholderNameError = nameError,
            )
        }
        return brand != null && numberError == null && expiryError == null && cvvError == null && nameError == null
    }

    private fun validateCardNumber(number: String, brand: CardBrand): UiText? = when {
        !CardValidators.luhnCheck(number) -> UiText.Resource(R.string.checkout_error_card_number_invalid)
        !CardValidators.brandMatches(number, brand) -> UiText.Resource(R.string.checkout_error_card_brand_mismatch)
        else -> null
    }

    private companion object {
        const val CARD_NUMBER_MAX_DIGITS = 16
        const val EXPIRY_MAX_DIGITS = 4
        const val CVV_MAX_DIGITS = 3

        const val MAX_STATUS_POLLS = 3

        const val STATUS_POLL_DELAY_MS = 2_000L

        fun String.digitsOnly(maxLength: Int): String = filter { it.isDigit() }.take(maxLength)

        /** Converts "MMYY" raw digits to "MM/YY" string for validation. */
        fun String.asMonthYear(): String = if (length > 2) "${take(2)}/${drop(2)}" else this
    }
}

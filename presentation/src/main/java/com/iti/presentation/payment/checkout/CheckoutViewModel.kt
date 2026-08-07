package com.iti.presentation.payment.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.designsystem.text.UiText
import com.iti.domain.core.Result
import com.iti.domain.core.fold
import com.iti.domain.core.onSuccess
import com.iti.domain.payment.model.CardBrand
import com.iti.domain.payment.model.PaymentIntention
import com.iti.domain.payment.model.PaymentMethodType
import com.iti.domain.payment.model.PaymentStatus
import com.iti.domain.payment.model.WalletProvider
import com.iti.domain.payment.usecase.ActivateSubscriptionAfterPaymentUseCase
import com.iti.domain.payment.usecase.ConfirmCardPaymentUseCase
import com.iti.domain.payment.usecase.ConfirmWalletPaymentUseCase
import com.iti.domain.payment.usecase.CreatePaymentIntentionUseCase
import com.iti.domain.payment.usecase.validation.CardValidators
import com.iti.domain.payment.usecase.validation.WalletNumberValidator
import com.iti.domain.usecase.subscription.GetSubscriptionPackagesUseCase
import com.iti.domain.usecase.user.GetCurrentUserUseCase
import com.iti.presentation.R
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class CheckoutViewModel(
    private val packageId: String,
    private val getSubscriptionPackages: GetSubscriptionPackagesUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val createPaymentIntention: CreatePaymentIntentionUseCase,
    private val confirmWalletPayment: ConfirmWalletPaymentUseCase,
    private val confirmCardPayment: ConfirmCardPaymentUseCase,
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
        val pkg = currentState.pkg ?: return
        val method = methodForSelectedTab()

        val isValid = if (method == PaymentMethodType.MOBILE_WALLET) validateWalletForm() else validateCardForm()
        if (!isValid) return

        updateState { copy(overlay = CheckoutOverlay.Loading) }
        viewModelScope.launch {
            when (val intentionResult = createPaymentIntention(pkg.id, method)) {
                is Result.Error -> updateState {
                    copy(overlay = CheckoutOverlay.Error(UiText.Resource(R.string.checkout_error_intention_failed)))
                }
                is Result.Success -> confirmAndActivate(pkg.id, intentionResult.data, method)
            }
        }
    }

    private suspend fun confirmAndActivate(packageId: String, intention: PaymentIntention, method: PaymentMethodType) {
        val outcomeResult = if (method == PaymentMethodType.MOBILE_WALLET) {
            confirmWalletPayment(
                intention.intentionId,
                currentState.selectedWalletProvider ?: return,
                currentState.walletNumber,
            )
        } else {
            confirmCardPayment(
                intention.intentionId,
                currentState.selectedCardBrand ?: return,
                currentState.cardNumber,
                currentState.expiry.asMonthYear(),
                currentState.cvv,
                currentState.cardholderName,
            )
        }

        when (outcomeResult) {
            is Result.Error -> updateState {
                copy(overlay = CheckoutOverlay.Error(UiText.Resource(R.string.checkout_payment_failed)))
            }
            is Result.Success -> when (outcomeResult.data.status) {
                PaymentStatus.SUCCESS -> {
                    activateSubscriptionAfterPayment(packageId)
                    updateState { copy(overlay = CheckoutOverlay.Success(UiText.Resource(R.string.checkout_payment_success))) }
                }
                PaymentStatus.PENDING -> updateState {
                    copy(overlay = CheckoutOverlay.Success(UiText.Resource(R.string.checkout_payment_pending)))
                }
                PaymentStatus.FAILED -> updateState {
                    copy(overlay = CheckoutOverlay.Error(UiText.Resource(R.string.checkout_payment_failed)))
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

        fun String.digitsOnly(maxLength: Int): String = filter { it.isDigit() }.take(maxLength)

        /** Converts the raw "MMYY" digits the state holds (display-formatted to "MM/YY" only via
         * `ExpiryDateVisualTransformation`) to the "MM/YY" string [CardValidators.isValidExpiry]
         * and the payment confirmation call expect. */
        fun String.asMonthYear(): String = if (length > 2) "${take(2)}/${drop(2)}" else this
    }
}

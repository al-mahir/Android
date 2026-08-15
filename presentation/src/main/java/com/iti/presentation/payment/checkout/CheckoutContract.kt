package com.iti.presentation.payment.checkout

import com.example.designsystem.text.UiText
import com.iti.domain.model.SubscriptionPackage
import com.iti.domain.payment.model.CardBrand
import com.iti.domain.payment.model.WalletProvider


sealed interface CheckoutOverlay {
    data object Loading : CheckoutOverlay
    data class Success(val message: UiText? = null) : CheckoutOverlay
    data class Error(val message: UiText? = null) : CheckoutOverlay
}

data class CheckoutUiState(
    val isLoadingPackage: Boolean = true,
    val loadError: UiText? = null,
    val pkg: SubscriptionPackage? = null,
    val userDisplayName: String = "",
    val selectedTabIndex: Int = 0,

    val pendingIntentionId: String? = null,

    val selectedWalletProvider: WalletProvider? = null,
    val walletNumber: String = "",
    val walletNumberError: UiText? = null,

    val selectedCardBrand: CardBrand? = null,
    val cardNumber: String = "",
    val cardNumberError: UiText? = null,
    val expiry: String = "",
    val expiryError: UiText? = null,
    val cvv: String = "",
    val cvvError: UiText? = null,
    val cardholderName: String = "",
    val cardholderNameError: UiText? = null,

    val overlay: CheckoutOverlay? = null,
) {
    val isProcessing: Boolean get() = overlay is CheckoutOverlay.Loading

    val canSubmitWallet: Boolean
        get() = !isProcessing && selectedWalletProvider != null && walletNumber.isNotBlank()

    val canSubmitCard: Boolean
        get() = !isProcessing && selectedCardBrand != null && cardNumber.isNotBlank() &&
            expiry.isNotBlank() && cvv.isNotBlank() && cardholderName.isNotBlank()
}

sealed interface CheckoutIntent {
    data class TabSelected(val index: Int) : CheckoutIntent
    data class WalletProviderSelected(val provider: WalletProvider) : CheckoutIntent
    data class WalletNumberChanged(val value: String) : CheckoutIntent
    data class CardBrandSelected(val brand: CardBrand) : CheckoutIntent
    data class CardNumberChanged(val value: String) : CheckoutIntent
    data class ExpiryChanged(val value: String) : CheckoutIntent
    data class CvvChanged(val value: String) : CheckoutIntent
    data class CardholderNameChanged(val value: String) : CheckoutIntent
    data object PayClicked : CheckoutIntent
    data object OverlayDismissed : CheckoutIntent
    data object RetryLoadClicked : CheckoutIntent
    /** Fired by CheckoutScreen when the Paymob SDK finishes (success / failure / pending). */
    data class PaymobSdkResult(val outcome: PaymobSdkOutcome) : CheckoutIntent
}

sealed interface PaymobSdkOutcome {
    data class Success(val result: HashMap<String, String?>) : PaymobSdkOutcome
    data class Failure(val message: String?) : PaymobSdkOutcome
    data object Pending : PaymobSdkOutcome
}

sealed interface CheckoutEffect {
    /** Fired after intention creation; CheckoutScreen launches the real Paymob SDK. */
    data class LaunchPaymobSdk(val clientSecret: String, val publicKey: String) : CheckoutEffect
    data object NavigateBack : CheckoutEffect
    data class ShowMessage(val message: UiText) : CheckoutEffect
}

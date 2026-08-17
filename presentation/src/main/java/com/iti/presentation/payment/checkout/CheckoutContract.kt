package com.iti.presentation.payment.checkout

import com.example.designsystem.text.UiText
import com.iti.domain.model.SubscriptionPackage
import com.iti.domain.payment.model.PaymentMethodType


sealed interface CheckoutOverlay {
    data object Loading : CheckoutOverlay
    data class Success(val message: UiText? = null) : CheckoutOverlay
    data class Error(val message: UiText? = null) : CheckoutOverlay
}

/**
 * Checkout deliberately collects no card or wallet credentials of its own.
 *
 * The Paymob SDK owns credential entry and exposes no API to pre-fill it (see
 * `PaymobSdk.Builder`), so any field we rendered here would have to be typed a second time in
 * the SDK sheet — and would put a raw PAN in our process for no benefit. This screen therefore
 * only picks the *method*, which is all `createIntention` sends.
 */
data class CheckoutUiState(
    val isLoadingPackage: Boolean = true,
    val loadError: UiText? = null,
    val pkg: SubscriptionPackage? = null,
    val userDisplayName: String = "",
    val selectedMethod: PaymentMethodType = PaymentMethodType.MOBILE_WALLET,

    val pendingIntentionId: String? = null,

    val overlay: CheckoutOverlay? = null,
) {
    val isProcessing: Boolean get() = overlay is CheckoutOverlay.Loading

    val selectedTabIndex: Int
        get() = if (selectedMethod == PaymentMethodType.MOBILE_WALLET) WALLET_TAB_INDEX else CARD_TAB_INDEX

    val canSubmit: Boolean get() = !isProcessing && pkg != null

    companion object {
        const val WALLET_TAB_INDEX = 0
        const val CARD_TAB_INDEX = 1

        fun methodForTab(index: Int): PaymentMethodType =
            if (index == CARD_TAB_INDEX) PaymentMethodType.CARD else PaymentMethodType.MOBILE_WALLET
    }
}

sealed interface CheckoutIntent {
    data class TabSelected(val index: Int) : CheckoutIntent
    data object PayClicked : CheckoutIntent
    data object OverlayDismissed : CheckoutIntent
    data object RetryLoadClicked : CheckoutIntent
    /** Fired by CheckoutScreen when the Paymob SDK finishes (success / failure / pending). */
    data class PaymobSdkResult(val outcome: PaymobSdkOutcome) : CheckoutIntent
}

sealed interface PaymobSdkOutcome {
    data class Success(val result: HashMap<String, String?>) : PaymobSdkOutcome
    data class Failure(val message: String?) : PaymobSdkOutcome
    data object Cancelled : PaymobSdkOutcome
    data object Pending : PaymobSdkOutcome
}

sealed interface CheckoutEffect {
    /** Fired after intention creation; CheckoutScreen launches the real Paymob SDK. */
    data class LaunchPaymobSdk(val clientSecret: String, val publicKey: String) : CheckoutEffect
    data object NavigateBack : CheckoutEffect
    data class ShowMessage(val message: UiText) : CheckoutEffect
}

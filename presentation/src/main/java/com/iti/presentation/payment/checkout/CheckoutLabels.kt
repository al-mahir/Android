package com.iti.presentation.payment.checkout

import androidx.annotation.StringRes
import com.iti.domain.payment.model.CardBrand
import com.iti.domain.payment.model.WalletProvider
import com.iti.presentation.R

@StringRes
internal fun WalletProvider.labelRes(): Int = when (this) {
    WalletProvider.VODAFONE_CASH -> R.string.checkout_wallet_vodafone_cash
    WalletProvider.ORANGE_CASH -> R.string.checkout_wallet_orange_cash
    WalletProvider.ETISALAT_CASH -> R.string.checkout_wallet_etisalat_cash
    WalletProvider.WE_PAY -> R.string.checkout_wallet_we_pay
}

@StringRes
internal fun WalletProvider.prefixErrorRes(): Int = when (this) {
    WalletProvider.VODAFONE_CASH -> R.string.checkout_error_wallet_prefix_vodafone
    WalletProvider.ORANGE_CASH -> R.string.checkout_error_wallet_prefix_orange
    WalletProvider.ETISALAT_CASH -> R.string.checkout_error_wallet_prefix_etisalat
    WalletProvider.WE_PAY -> R.string.checkout_error_wallet_prefix_we
}

@StringRes
internal fun CardBrand.labelRes(): Int = when (this) {
    CardBrand.VISA -> R.string.checkout_card_visa
    CardBrand.MASTERCARD -> R.string.checkout_card_mastercard
}

package com.iti.presentation.payment.checkout.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.designsystem.R as DesignSystemR
import com.example.designsystem.components.card.InnerContentCard
import com.example.designsystem.theme.Theme
import com.iti.domain.payment.model.CardBrand
import com.iti.domain.payment.model.PaymentMethodType
import com.iti.domain.payment.model.WalletProvider
import com.iti.presentation.R
import com.iti.presentation.payment.checkout.labelRes

/**
 * Explains what happens after "Pay" for the selected method, and shows which brands are
 * accepted.
 *
 * Deliberately collects nothing: the Paymob SDK asks for the card number / wallet number on its
 * own sheet and cannot be pre-filled, so any field here would just be typed twice.
 */
@Composable
internal fun PaymentMethodSection(
    method: PaymentMethodType,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        InnerContentCard {
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    painter = painterResource(DesignSystemR.drawable.ic_lock),
                    contentDescription = null,
                    tint = Theme.colors.primary,
                    modifier = Modifier
                        .padding(end = Theme.spacing.small)
                        .size(Theme.size.iconSemiMedium),
                )
                Column {
                    Text(
                        text = stringResource(R.string.checkout_secure_entry_title),
                        style = Theme.typography.body.medium,
                        color = Theme.colors.primaryFont,
                    )
                    Text(
                        text = stringResource(
                            when (method) {
                                PaymentMethodType.CARD -> R.string.checkout_secure_entry_card_note
                                PaymentMethodType.MOBILE_WALLET -> R.string.checkout_secure_entry_wallet_note
                            }
                        ),
                        style = Theme.typography.body.small,
                        color = Theme.colors.secondaryFont,
                        modifier = Modifier.padding(top = Theme.spacing.extraSmall),
                    )
                }
            }
        }

        Text(
            text = stringResource(
                when (method) {
                    PaymentMethodType.CARD -> R.string.checkout_accepted_cards
                    PaymentMethodType.MOBILE_WALLET -> R.string.checkout_accepted_wallets
                }
            ),
            style = Theme.typography.body.small,
            color = Theme.colors.secondaryFont,
            modifier = Modifier.padding(top = Theme.spacing.medium, bottom = Theme.spacing.small),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
            when (method) {
                PaymentMethodType.CARD -> CardBrand.entries.forEach { brand ->
                    BrandBadge(iconRes = brand.iconRes(), label = stringResource(brand.labelRes()))
                }
                PaymentMethodType.MOBILE_WALLET -> WalletProvider.entries.forEach { provider ->
                    BrandBadge(
                        iconRes = provider.iconRes(),
                        label = stringResource(provider.labelRes()),
                        background = provider.brandColor(),
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandBadge(
    iconRes: Int,
    label: String,
    modifier: Modifier = Modifier,
    background: Color = Color.White,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(Theme.shapes.medium)
            .background(background)
            .padding(Theme.spacing.small),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = Color.Unspecified,
            modifier = Modifier.size(Theme.size.iconMedium),
        )
    }
}

private fun CardBrand.iconRes(): Int = when (this) {
    CardBrand.VISA -> DesignSystemR.drawable.ic_card_visa
    CardBrand.MASTERCARD -> DesignSystemR.drawable.ic_card_mastercard
}

private fun WalletProvider.iconRes(): Int = when (this) {
    WalletProvider.VODAFONE_CASH -> DesignSystemR.drawable.ic_wallet_vodafone_cash
    WalletProvider.ORANGE_CASH -> DesignSystemR.drawable.ic_wallet_orange_cash
    WalletProvider.ETISALAT_CASH -> DesignSystemR.drawable.ic_wallet_etisalat_cash
    WalletProvider.WE_PAY -> DesignSystemR.drawable.ic_wallet_we_pay
}

// Fixed third-party brand colors (not part of the app's own palette, so Theme.colors has no
// token for them) — the one deliberate exception to "no raw Color(...) in feature code".
private fun WalletProvider.brandColor(): Color = when (this) {
    WalletProvider.VODAFONE_CASH -> Color(0xFFE60000)
    WalletProvider.ORANGE_CASH -> Color(0xFFFF7900)
    WalletProvider.ETISALAT_CASH -> Color(0xFF000000)
    WalletProvider.WE_PAY -> Color(0xFF0077C8)
}

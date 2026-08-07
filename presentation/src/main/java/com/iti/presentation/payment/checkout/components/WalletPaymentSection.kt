package com.iti.presentation.payment.checkout.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.example.designsystem.R as DesignSystemR
import com.example.designsystem.components.selection.SelectableIconCardGrid
import com.example.designsystem.components.selection.SelectableIconCardItem
import com.example.designsystem.components.textfield.TextField
import com.example.designsystem.text.asString
import com.example.designsystem.theme.Theme
import com.iti.domain.payment.model.WalletProvider
import com.iti.presentation.R
import com.iti.presentation.payment.checkout.CheckoutIntent
import com.iti.presentation.payment.checkout.CheckoutUiState
import com.iti.presentation.payment.checkout.labelRes

@Composable
internal fun WalletPaymentSection(
    state: CheckoutUiState,
    onIntent: (CheckoutIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.checkout_select_payment_method),
            style = Theme.typography.body.medium,
            modifier = Modifier.padding(bottom = Theme.spacing.small),
        )

        SelectableIconCardGrid(
            items = WalletProvider.entries.map { provider ->
                SelectableIconCardItem(
                    id = provider.name,
                    label = stringResource(provider.labelRes()),
                    iconPainter = painterResource(provider.iconRes()),
                    iconBackgroundColor = provider.brandColor(),
                )
            },
            selectedId = state.selectedWalletProvider?.name,
            onItemSelected = { id ->
                onIntent(CheckoutIntent.WalletProviderSelected(WalletProvider.valueOf(id)))
            },
        )

        state.selectedWalletProvider?.let { provider ->
            Text(
                text = stringResource(R.string.checkout_wallet_hint_pattern, stringResource(provider.labelRes())),
                style = Theme.typography.body.small,
                color = Theme.colors.secondaryFont,
                modifier = Modifier.padding(top = Theme.spacing.medium, bottom = Theme.spacing.small),
            )
        }

        TextField(
            text = state.walletNumber,
            onTextChange = { onIntent(CheckoutIntent.WalletNumberChanged(it)) },
            title = stringResource(R.string.checkout_wallet_number_label),
            hint = stringResource(R.string.checkout_wallet_number_hint),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = state.walletNumberError != null,
            errorMessage = state.walletNumberError?.asString(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Theme.spacing.medium),
        )

        Text(
            text = stringResource(R.string.checkout_security_note),
            style = Theme.typography.body.small,
            color = Theme.colors.secondaryFont,
        )
    }
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

package com.iti.presentation.payment.checkout.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.iti.domain.payment.model.CardBrand
import com.iti.presentation.R
import com.iti.presentation.payment.checkout.CheckoutIntent
import com.iti.presentation.payment.checkout.CheckoutUiState
import com.iti.presentation.payment.checkout.labelRes

@Composable
internal fun CardPaymentSection(
    state: CheckoutUiState,
    onIntent: (CheckoutIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.checkout_select_card_brand),
            style = Theme.typography.body.medium,
            modifier = Modifier.padding(bottom = Theme.spacing.small),
        )

        SelectableIconCardGrid(
            items = CardBrand.entries.map { brand ->
                SelectableIconCardItem(
                    id = brand.name,
                    label = stringResource(brand.labelRes()),
                    iconPainter = painterResource(brand.iconRes()),
                    iconBackgroundColor = Color.White,
                )
            },
            selectedId = state.selectedCardBrand?.name,
            onItemSelected = { id -> onIntent(CheckoutIntent.CardBrandSelected(CardBrand.valueOf(id))) },
        )

        Text(
            text = stringResource(R.string.checkout_card_details_title),
            style = Theme.typography.body.medium,
            modifier = Modifier.padding(top = Theme.spacing.medium, bottom = Theme.spacing.small),
        )

        TextField(
            text = state.cardNumber,
            onTextChange = { onIntent(CheckoutIntent.CardNumberChanged(it)) },
            title = stringResource(R.string.checkout_card_number_label),
            hint = stringResource(R.string.checkout_card_number_hint),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = CardNumberVisualTransformation(),
            singleLine = true,
            isError = state.cardNumberError != null,
            errorMessage = state.cardNumberError?.asString(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Theme.spacing.medium),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Theme.spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
        ) {
            TextField(
                text = state.expiry,
                onTextChange = { onIntent(CheckoutIntent.ExpiryChanged(it)) },
                title = stringResource(R.string.checkout_expiry_label),
                hint = stringResource(R.string.checkout_expiry_hint),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = ExpiryDateVisualTransformation(),
                singleLine = true,
                isError = state.expiryError != null,
                errorMessage = state.expiryError?.asString(),
                modifier = Modifier.weight(1f),
            )
            TextField(
                text = state.cvv,
                onTextChange = { onIntent(CheckoutIntent.CvvChanged(it)) },
                title = stringResource(R.string.checkout_cvv_label),
                hint = stringResource(R.string.checkout_cvv_hint),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                isError = state.cvvError != null,
                errorMessage = state.cvvError?.asString(),
                modifier = Modifier.weight(1f),
            )
        }

        TextField(
            text = state.cardholderName,
            onTextChange = { onIntent(CheckoutIntent.CardholderNameChanged(it)) },
            title = stringResource(R.string.checkout_cardholder_name_label),
            hint = stringResource(R.string.checkout_cardholder_name_hint),
            singleLine = true,
            isError = state.cardholderNameError != null,
            errorMessage = state.cardholderNameError?.asString(),
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

private fun CardBrand.iconRes(): Int = when (this) {
    CardBrand.VISA -> DesignSystemR.drawable.ic_card_visa
    CardBrand.MASTERCARD -> DesignSystemR.drawable.ic_card_mastercard
}

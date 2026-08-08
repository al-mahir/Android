package com.iti.presentation.payment.checkout.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.designsystem.components.tabs.TabSelector
import com.iti.presentation.R

@Composable
internal fun CheckoutTabs(selectedIndex: Int, onTabSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    TabSelector(
        tabs = listOf(
            stringResource(R.string.checkout_tab_wallet),
            stringResource(R.string.checkout_tab_card),
        ),
        selectedIndex = selectedIndex,
        onTabSelected = onTabSelected,
        modifier = modifier.fillMaxWidth(),
    )
}

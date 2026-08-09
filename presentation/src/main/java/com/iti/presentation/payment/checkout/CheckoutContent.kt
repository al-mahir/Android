package com.iti.presentation.payment.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.designsystem.R as DesignSystemR
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.components.loading.ShimmerBox
import com.example.designsystem.components.placeholderscreens.NetworkErrorScreen
import com.example.designsystem.components.topbar.BackTitleTopBar
import com.example.designsystem.text.asString
import com.example.designsystem.theme.Theme
import com.iti.presentation.R
import com.iti.presentation.payment.checkout.components.PackageSummaryCard
import com.iti.presentation.payment.checkout.components.CardPaymentSection
import com.iti.presentation.payment.checkout.components.CheckoutTabs
import com.iti.presentation.payment.checkout.components.WalletPaymentSection
import com.iti.presentation.payment.checkout.components.rememberFormattedWholePrice

@Composable
fun CheckoutContent(
    state: CheckoutUiState,
    onIntent: (CheckoutIntent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.backGround),
    ) {
        BackTitleTopBar(
            title = stringResource(R.string.checkout_title),
            onBackClick = onBackClick,
        )

        val pkg = state.pkg
        when {
            state.loadError != null -> NetworkErrorScreen(
                modifier = Modifier.fillMaxSize(),
                description = state.loadError.asString(),
                onRetry = { onIntent(CheckoutIntent.RetryLoadClicked) },
            )

            state.isLoadingPackage || pkg == null -> CheckoutSkeleton()

            else -> {
                val payAmountLabel = stringResource(
                    R.string.checkout_pay_button_pattern,
                    rememberFormattedWholePrice(pkg.monthlyPriceMinorUnits, pkg.currencyCode),
                )
                val canSubmit = if (state.selectedTabIndex == 0) state.canSubmitWallet else state.canSubmitCard

                // The Pay button is pinned outside the scrollable list (not the last LazyColumn
                // item) so it stays reachable above the keyboard instead of requiring a scroll
                // past the open IME to find it.
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(
                            horizontal = Theme.spacing.medium,
                            vertical = Theme.spacing.large,
                        ),
                    ) {
                        item {
                            PackageSummaryCard(pkg = pkg, modifier = Modifier.padding(bottom = Theme.spacing.medium))
                        }

                        if (state.userDisplayName.isNotBlank()) {
                            item {
                                Text(
                                    text = stringResource(R.string.checkout_purchasing_as_pattern, state.userDisplayName),
                                    style = Theme.typography.body.small,
                                    color = Theme.colors.secondaryFont,
                                    modifier = Modifier.padding(bottom = Theme.spacing.medium),
                                )
                            }
                        }

                        item {
                            CheckoutTabs(
                                selectedIndex = state.selectedTabIndex,
                                onTabSelected = { onIntent(CheckoutIntent.TabSelected(it)) },
                                modifier = Modifier.padding(bottom = Theme.spacing.medium),
                            )
                        }

                        item {
                            if (state.selectedTabIndex == 0) {
                                WalletPaymentSection(state = state, onIntent = onIntent)
                            } else {
                                CardPaymentSection(state = state, onIntent = onIntent)
                            }
                        }
                    }

                    PrimaryButton(
                        caption = payAmountLabel,
                        onClick = { onIntent(CheckoutIntent.PayClicked) },
                        isDisabled = !canSubmit,
                        isLoading = state.isProcessing,
                        iconPainter = painterResource(DesignSystemR.drawable.ic_check),
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding()
                            .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.medium),
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckoutSkeleton(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.large),
    ) {
        ShimmerBox(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(Theme.shapes.large),
        )
        ShimmerBox(
            Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(Theme.shapes.large),
        )
    }
}

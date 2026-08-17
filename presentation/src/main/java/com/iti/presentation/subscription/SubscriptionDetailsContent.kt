package com.iti.presentation.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.designsystem.R as DesignSystemR
import com.example.designsystem.components.button.SecondaryButton
import com.example.designsystem.components.card.InnerContentCard
import com.example.designsystem.components.loading.ShimmerBox
import com.example.designsystem.components.placeholderscreens.NetworkErrorScreen
import com.example.designsystem.components.section.SectionHeader
import com.example.designsystem.components.topbar.BackTitleTopBar
import com.example.designsystem.theme.Theme
import com.iti.presentation.R
import com.iti.presentation.subscription.components.ReturnSubscriptionSheet
import com.iti.presentation.subscription.components.billingPeriodLabel
import com.iti.presentation.subscription.components.rememberFormattedDate
import com.iti.presentation.subscription.components.rememberFormattedWholePrice
import com.iti.presentation.subscription.state.SubscriptionDetailsUiState

@Composable
fun SubscriptionDetailsContent(
    state: SubscriptionDetailsUiState,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onReturnSubscriptionClick: () -> Unit,
    onReturnSheetDismiss: () -> Unit,
    onCancellationMessageChange: (String) -> Unit,
    onSendCancellationMessageClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.backGround),
    ) {
        BackTitleTopBar(
            title = stringResource(R.string.subscription_details_title),
            onBackClick = onBackClick,
        )

        when {
            state.hasError -> NetworkErrorScreen(
                modifier = Modifier.fillMaxSize(),
                description = stringResource(
                    state.errorMessageRes ?: R.string.subscription_details_error_generic
                ),
                onRetry = onRetryClick,
            )

            state.isLoading && state.subscription == null -> SubscriptionDetailsSkeleton()

            else -> {
                val subscription = state.subscription
                val activePackage = state.activePackage

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.large),
                    verticalArrangement = Arrangement.spacedBy(Theme.spacing.large),
                ) {
                    PlanHeroCard(
                        planName = activePackage?.name ?: stringResource(R.string.profile_plan_premium),
                        priceText = activePackage?.let {
                            rememberFormattedWholePrice(it.priceAmount, it.currencyCode)
                        },
                        pricePeriodText = billingPeriodLabel(activePackage?.durationDays ?: 0),
                        renewsAtText = subscription?.renewsAtEpochMillis?.let { rememberFormattedDate(it) },
                    )

                    val features = activePackage?.features.orEmpty()
                    if (features.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
                            SectionHeader(title = stringResource(R.string.subscription_details_features_label))
                            InnerContentCard {
                                Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
                                    features.forEach { feature -> FeatureRow(text = feature) }
                                }
                            }
                        }
                    }

                    SecondaryButton(
                        caption = stringResource(R.string.subscription_details_return_button),
                        onClick = onReturnSubscriptionClick,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (state.isReturnSheetVisible) {
        ReturnSubscriptionSheet(
            message = state.cancellationMessage,
            onMessageChange = onCancellationMessageChange,
            isSending = state.isSendingCancellationMessage,
            isSent = state.cancellationMessageSent,
            onSendClick = onSendCancellationMessageClick,
            onDismiss = onReturnSheetDismiss,
        )
    }
}

@Composable
private fun PlanHeroCard(
    planName: String,
    priceText: String?,
    pricePeriodText: String,
    renewsAtText: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Theme.shapes.large)
            .background(Theme.colors.primary.copy(alpha = 0.08f))
            .border(1.dp, Theme.colors.primary, Theme.shapes.large)
            .padding(Theme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall)) {
                EyebrowLabel(text = stringResource(R.string.subscription_details_plan_label))
                BasicText(
                    text = planName,
                    style = Theme.typography.title.copy(
                        color = Theme.colors.primaryFont,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }

            if (priceText != null) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
                ) {
                    EyebrowLabel(text = stringResource(R.string.subscription_details_price_label))
                    Row(verticalAlignment = Alignment.Bottom) {
                        BasicText(
                            text = priceText,
                            style = Theme.typography.h4.copy(
                                color = Theme.colors.primary,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        BasicText(
                            text = pricePeriodText,
                            style = Theme.typography.body.small.copy(color = Theme.colors.secondaryFont),
                            modifier = Modifier.padding(start = Theme.spacing.extraSmall),
                        )
                    }
                }
            }
        }

        if (renewsAtText != null) {
            HorizontalDivider(thickness = 1.dp, color = Theme.colors.outline)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                EyebrowLabel(text = stringResource(R.string.subscription_details_renews_label))
                BasicText(
                    text = renewsAtText,
                    style = Theme.typography.body.medium.copy(color = Theme.colors.primaryFont),
                )
            }
        }
    }
}

@Composable
private fun EyebrowLabel(text: String, modifier: Modifier = Modifier) {
    BasicText(
        text = text,
        style = Theme.typography.body.small.copy(
            color = Theme.colors.secondaryFont,
            fontWeight = FontWeight.SemiBold,
        ),
        modifier = modifier,
    )
}

@Composable
private fun FeatureRow(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(DesignSystemR.drawable.ic_check),
            contentDescription = null,
            tint = Theme.colors.primary,
            modifier = Modifier.size(Theme.size.iconSemiMedium),
        )
        BasicText(
            text = text,
            style = Theme.typography.body.small.copy(color = Theme.colors.primaryFont),
        )
    }
}

@Composable
private fun SubscriptionDetailsSkeleton(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.large),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.large),
    ) {
        ShimmerBox(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(Theme.shapes.large),
        )
        repeat(3) {
            ShimmerBox(Modifier.width(200.dp).height(18.dp).clip(Theme.shapes.small))
        }
    }
}

package com.iti.presentation.subscription.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.designsystem.components.pricing.PackageCard
import com.iti.domain.model.SubscriptionPackage
import com.iti.presentation.R

@Composable
internal fun PackageListItem(
    pkg: SubscriptionPackage,
    isProcessing: Boolean,
    onSelectClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val priceText = rememberFormattedWholePrice(pkg.monthlyPriceMinorUnits, pkg.currencyCode)

    PackageCard(
        title = pkg.name,
        priceText = priceText,
        pricePeriodText = stringResource(R.string.packages_price_period_month),
        features = pkg.features,
        selectCaption = stringResource(
            if (isProcessing) R.string.packages_selecting_button else R.string.packages_select_button
        ),
        onSelectClick = onSelectClick,
        isRecommended = pkg.isRecommended,
        recommendedLabel = stringResource(R.string.packages_recommended_badge),
        isLoading = isProcessing,
        modifier = modifier,
    )
}

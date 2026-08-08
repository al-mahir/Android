package com.example.designsystem.components.pricing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.designsystem.theme.AlMahirTheme
import com.example.designsystem.theme.Theme
import java.util.Locale

@Preview(name = "PackageCard – Light LTR", showBackground = true, group = "PackageCard")
@Composable
private fun PackageCardLightLtr() {
    AlMahirTheme(isDarkTheme = false, locale = Locale.ENGLISH) { PackageCardSamples() }
}

@Preview(name = "PackageCard – Light RTL", showBackground = true, group = "PackageCard")
@Composable
private fun PackageCardLightRtl() {
    AlMahirTheme(isDarkTheme = false, locale = Locale("ar")) { PackageCardSamples() }
}

@Preview(name = "PackageCard – Dark LTR", showBackground = true, group = "PackageCard")
@Composable
private fun PackageCardDarkLtr() {
    AlMahirTheme(isDarkTheme = true, locale = Locale.ENGLISH) { PackageCardSamples() }
}

@Preview(name = "PackageCard – Dark RTL", showBackground = true, group = "PackageCard")
@Composable
private fun PackageCardDarkRtl() {
    AlMahirTheme(isDarkTheme = true, locale = Locale("ar")) { PackageCardSamples() }
}

@Composable
private fun PackageCardSamples() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme.colors.backGround)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PackageCard(
            title = "Intensive",
            priceText = "$65",
            pricePeriodText = "/month",
            features = listOf(
                "45-minute sessions",
                "Personalized revision plan",
                "Direct feedback + recordings",
                "Priority scheduling",
            ),
            selectCaption = "Select Package",
            onSelectClick = {},
            isRecommended = true,
            recommendedLabel = "RECOMMENDED",
        )
        PackageCard(
            title = "Light",
            priceText = "$40",
            pricePeriodText = "/month",
            features = listOf(
                "30-minute sessions",
                "Weekly progress report",
                "Group correction sessions",
            ),
            selectCaption = "Select Package",
            onSelectClick = {},
        )
    }
}

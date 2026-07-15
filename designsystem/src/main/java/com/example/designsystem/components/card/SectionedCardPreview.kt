package com.example.designsystem.components.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.designsystem.R
import com.example.designsystem.theme.AlMahirTheme
import com.example.designsystem.theme.Theme
import java.util.Locale

@Preview(name = "SectionedCard – Light LTR", showBackground = true, group = "SectionedCard")
@Composable
private fun SectionedCardLightLtr() {
    AlMahirTheme(isDarkTheme = false, locale = Locale.ENGLISH) { SectionedCardSamples() }
}

@Preview(name = "SectionedCard – Light RTL", showBackground = true, group = "SectionedCard")
@Composable
private fun SectionedCardLightRtl() {
    AlMahirTheme(isDarkTheme = false, locale = Locale("ar")) { SectionedCardSamples() }
}

@Preview(name = "SectionedCard – Dark LTR", showBackground = true, group = "SectionedCard")
@Composable
private fun SectionedCardDarkLtr() {
    AlMahirTheme(isDarkTheme = true, locale = Locale.ENGLISH) { SectionedCardSamples() }
}

@Preview(name = "SectionedCard – Dark RTL", showBackground = true, group = "SectionedCard")
@Composable
private fun SectionedCardDarkRtl() {
    AlMahirTheme(isDarkTheme = true, locale = Locale("ar")) { SectionedCardSamples() }
}

/** Base (header + body) and the footer overload, both with a faint body watermark. */
@Composable
private fun SectionedCardSamples() {
    val watermark = painterResource(R.drawable.ic_profile)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme.colors.backGround)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Base — header + body, watermark behind the body band.
        SectionedCard(
            header = { SampleSection("Header") },
            bodyWatermark = watermark,
            body = { SampleSection("Body — white band with a faint watermark") },
        )
        // Footer overload — the doctor-card shape (header / body / rate footer).
        SectionedCard(
            header = { SampleSection("بيانات الطبيب") },
            footer = { SampleSection("قيم الطبيب") },
            bodyWatermark = watermark,
            body = { SampleSection("د. ليلي فهد · 4.8") },
        )
    }
}

@Composable
private fun SampleSection(text: String) {
    Text(
        text = text,
        style = Theme.typography.body.medium.copy(color = Theme.colors.primaryFont),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    )
}

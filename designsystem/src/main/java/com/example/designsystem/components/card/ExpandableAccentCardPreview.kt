package com.example.designsystem.components.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.designsystem.theme.AlMahirTheme
import com.example.designsystem.theme.Theme
import java.util.Locale

@Preview(name = "ExpandableAccentCard – RTL", showBackground = true, group = "ExpandableAccentCard")
@Composable
private fun ExpandableAccentCardRtl() {
    AlMahirTheme(locale = Locale("ar")) { ExpandableAccentCardSamples() }
}

@Preview(name = "ExpandableAccentCard – LTR", showBackground = true, group = "ExpandableAccentCard")
@Composable
private fun ExpandableAccentCardLtr() {
    AlMahirTheme(locale = Locale.ENGLISH) { ExpandableAccentCardSamples() }
}

@Composable
private fun ExpandableAccentCardSamples() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme.colors.surfaceVariant)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        var firstExpanded by remember { mutableStateOf(true) }
        var secondExpanded by remember { mutableStateOf(false) }
        ExpandableAccentCard(
            expanded = firstExpanded,
            onToggle = { firstExpanded = !firstExpanded },
            accentColor = SettingsCardAccent.Amber,
            header = {
                Text(
                    text = "إفطار",
                    style = Theme.typography.body.large.copy(
                        color = Theme.colors.primaryFont,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            },
            body = {
                Text(
                    text = "Body content visible while expanded",
                    style = Theme.typography.body.medium.copy(color = Theme.colors.secondaryFont),
                    modifier = Modifier.padding(16.dp),
                )
            },
        )
        ExpandableAccentCard(
            expanded = secondExpanded,
            onToggle = { secondExpanded = !secondExpanded },
            accentColor = SettingsCardAccent.Purple,
            header = {
                Text(
                    text = "وجبة خفيفة 1",
                    style = Theme.typography.body.large.copy(
                        color = Theme.colors.primaryFont,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            },
            body = {
                Text(
                    text = "Hidden body",
                    modifier = Modifier.padding(16.dp),
                )
            },
        )
    }
}

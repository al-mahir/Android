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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.designsystem.theme.AlMahirTheme
import com.example.designsystem.theme.Theme
import java.util.Locale

@Preview(name = "ExpandableSection – Light LTR", showBackground = true, group = "ExpandableSection")
@Composable
private fun ExpandableSectionLightLtr() {
    AlMahirTheme(isDarkTheme = false, locale = Locale.ENGLISH) { ExpandableSectionSamples() }
}

@Preview(name = "ExpandableSection – Light RTL", showBackground = true, group = "ExpandableSection")
@Composable
private fun ExpandableSectionLightRtl() {
    AlMahirTheme(isDarkTheme = false, locale = Locale("ar")) { ExpandableSectionSamples() }
}

@Preview(name = "ExpandableSection – Dark LTR", showBackground = true, group = "ExpandableSection")
@Composable
private fun ExpandableSectionDarkLtr() {
    AlMahirTheme(isDarkTheme = true, locale = Locale.ENGLISH) { ExpandableSectionSamples() }
}

@Preview(name = "ExpandableSection – Dark RTL", showBackground = true, group = "ExpandableSection")
@Composable
private fun ExpandableSectionDarkRtl() {
    AlMahirTheme(isDarkTheme = true, locale = Locale("ar")) { ExpandableSectionSamples() }
}

/** One section expanded by default, one collapsed and toggleable. */
@Composable
private fun ExpandableSectionSamples() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme.colors.backGround)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        var firstExpanded by remember { mutableStateOf(true) }
        var secondExpanded by remember { mutableStateOf(false) }
        ExpandableSection(
            title = "معلوماتي",
            expanded = firstExpanded,
            onToggle = { firstExpanded = !firstExpanded },
        ) {
            SampleBody("Body content — visible while expanded")
        }
        ExpandableSection(
            title = "الجدول الغذائي",
            expanded = secondExpanded,
            onToggle = { secondExpanded = !secondExpanded },
        ) {
            SampleBody("Hidden body")
        }
    }
}

@Composable
private fun SampleBody(text: String) {
    Text(
        text = text,
        style = Theme.typography.body.medium.copy(color = Theme.colors.primaryFont),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    )
}
